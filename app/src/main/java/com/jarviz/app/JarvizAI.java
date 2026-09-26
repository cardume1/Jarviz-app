package com.jarviz.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JarvizAI {

    public interface Callback {
        void resposta(String texto);
        void erro(String mensagem);
    }

    private static final String[] MODELOS = {
        "llama-3.3-70b-versatile", "llama-3.1-8b-instant"
    };

    // Histórico de conversa em memória (dá contexto real, tipo gente de verdade
    // que lembra o que você falou 2 minutos atrás).
    private static final List<JSONObject> historico = new ArrayList<>();
    private static final int MAX_TURNOS = 12; // limite pra não estourar o payload

    public static void limparHistorico() {
        historico.clear();
    }

    private static String montarSystemPrompt(Context ctx) {
        String nome = JarvizConfig.getNomeUsuario(ctx);
        String personalidade = JarvizConfig.getPersonalidade(ctx);

        StringBuilder sp = new StringBuilder();
        sp.append("Você é Jarviz, um assistente pessoal por voz e texto, parecido com uma ")
          .append("pessoa real e não com um robô de central de atendimento. ")
          .append("Fale sempre em português brasileiro, de forma natural, com contrações ")
          .append("do dia a dia (\"tá\", \"pra\", \"cê\") quando fizer sentido. ")
          .append("Seja caloroso, tenha opiniões leves quando perguntado, use humor sutil ")
          .append("quando apropriado, e NUNCA se refira a si mesmo como 'modelo de linguagem' ")
          .append("ou 'IA da Groq/Meta'. Você é só o Jarviz. ")
          .append("Respostas por voz devem ser curtas e diretas (1 a 3 frases), a não ser que ")
          .append("o usuário peça detalhes. Evite listas longas em respostas faladas.");

        switch (personalidade) {
            case "engracado":
                sp.append(" Seu estilo é bem-humorado, gosta de brincar e soltar piadas leves, ")
                  .append("mas sem exagerar a ponto de atrapalhar quando o assunto é sério.");
                break;
            case "formal":
                sp.append(" Seu estilo é educado, objetivo e mais formal, como um assistente executivo.");
                break;
            case "direto":
                sp.append(" Seu estilo é direto ao ponto, sem enrolação, frases curtas.");
                break;
            default:
                sp.append(" Seu estilo é equilibrado: caloroso, mas objetivo.");
        }

        if (nome != null && !nome.isEmpty()) {
            sp.append(" O nome da pessoa com quem você está falando é ").append(nome)
              .append("; chame-a pelo nome de vez em quando, sem exagerar.");
        }

        sp.append(" Comandos que você pode reconhecer e confirmar naturalmente quando o ")
          .append("usuário pedir: ligar para [nome/número], mandar mensagem para [nome], ")
          .append("abrir [nome do app], ligar/desligar a voz. Quando um desses comandos for ")
          .append("executado pelo app (isso é informado a você separadamente), não é você quem ")
          .append("decide — apenas responda perguntas normais de forma conversacional.");

        return sp.toString();
    }

    public static void perguntar(Context ctx, String pergunta, Callback cb) {
        final String chave = JarvizConfig.getGroqApiKey(ctx);
        final String systemPrompt = montarSystemPrompt(ctx);

        new Thread(() -> {
            try {
                if (chave == null || chave.isEmpty()) {
                    postErro(cb, "🔑 Nenhuma chave da Groq configurada. Abra as Configurações e cole sua chave (é grátis em console.groq.com).");
                    return;
                }

                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + chave);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("model", MODELOS[0]);

                JSONArray msgs = new JSONArray();
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
                msgs.put(sys);

                synchronized (historico) {
                    for (JSONObject m : historico) msgs.put(m);
                }

                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", pergunta);
                msgs.put(user);

                body.put("messages", msgs);
                body.put("temperature", 0.85);
                body.put("max_tokens", 600);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int cod = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                    cod >= 200 && cod < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
                StringBuilder res = new StringBuilder();
                String linha;
                while ((linha = br.readLine()) != null) res.append(linha);
                br.close();

                if (cod >= 200 && cod < 300) {
                    JSONObject resp = new JSONObject(res.toString());
                    String texto = resp.getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message")
                        .getString("content");

                    synchronized (historico) {
                        JSONObject uMsg = new JSONObject().put("role", "user").put("content", pergunta);
                        JSONObject aMsg = new JSONObject().put("role", "assistant").put("content", texto);
                        historico.add(uMsg);
                        historico.add(aMsg);
                        while (historico.size() > MAX_TURNOS * 2) historico.remove(0);
                    }

                    postResposta(cb, texto);
                } else if (cod == 401) {
                    postErro(cb, "🔑 Chave inválida ou expirada. Confira em Configurações.");
                } else if (cod == 429) {
                    postErro(cb, "⏳ Muitas perguntas em pouco tempo. Espera uns segundos e tenta de novo.");
                } else {
                    postErro(cb, "❌ Erro do servidor (" + cod + "). Tenta de novo em instantes.");
                }
                conn.disconnect();
            } catch (java.net.UnknownHostException | java.net.SocketTimeoutException e) {
                postErro(cb, "📶 Sem internet — modo offline.");
            } catch (Exception e) {
                postErro(cb, "⚠️ Algo deu errado: " + e.getMessage());
            }
        }).start();
    }

    private static void postResposta(Callback cb, String texto) {
        new Handler(Looper.getMainLooper()).post(() -> cb.resposta(texto));
    }

    private static void postErro(Callback cb, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> cb.erro(msg));
    }
}
