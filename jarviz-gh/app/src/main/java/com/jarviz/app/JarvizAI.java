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
import java.util.Locale;

/** Núcleo de conversa do Jarviz. Suporta Groq e Gemini sem colocar chaves no APK. */
public final class JarvizAI {
    public interface Callback { void resposta(String texto); void erro(String mensagem); }

    private static final String[] GROQ_MODELOS = {
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant"
    };
    private static final String GEMINI_MODELO = "gemini-3.8-flash";
    private static final List<JSONObject> historico = new ArrayList<>();
    private static final int MAX_TURNOS = 20;

    private JarvizAI() {}

    public static void limparHistorico() { synchronized (historico) { historico.clear(); } }

    private static String montarSystemPrompt(Context ctx) {
        String nome = JarvizConfig.getNomeUsuario(ctx);
        String personalidade = JarvizConfig.getPersonalidade(ctx);
        StringBuilder p = new StringBuilder();
        p.append("Você é Jarviz, o assistente pessoal do usuário. Converse em português brasileiro natural. ")
         .append("Seu objetivo é ser útil, rápido, educado e humano na conversa, sem fingir ser uma pessoa humana. ")
         .append("Não diga que é Groq, Gemini, Llama ou um modelo de IA, a menos que o usuário pergunte diretamente qual tecnologia está sendo usada. ")
         .append("Entenda contexto, pronomes e referências às mensagens anteriores. Não repita a pergunta do usuário. ")
         .append("Para voz, prefira respostas curtas e naturais; para texto, detalhe quando isso ajudar. ")
         .append("Quando não souber algo, seja transparente. Nunca invente que executou uma ação do Android: ações reais são feitas pelo aplicativo. ");
        switch (personalidade) {
            case "engracado": p.append("Use humor leve quando couber."); break;
            case "formal": p.append("Seja educado, organizado e formal."); break;
            case "direto": p.append("Seja extremamente direto e objetivo."); break;
            default: p.append("Mantenha um equilíbrio entre simpatia e objetividade.");
        }
        if (!nome.isEmpty()) p.append(" O nome do usuário é ").append(nome).append("; use-o ocasionalmente, sem exagerar.");
        return p.toString();
    }

    public static void perguntar(Context ctx, String pergunta, Callback cb) {
        final String provider = JarvizConfig.getProvedor(ctx);
        final String key = provider.equals("gemini") ? JarvizConfig.getGeminiApiKey(ctx) : JarvizConfig.getGroqApiKey(ctx);
        final String prompt = montarSystemPrompt(ctx);
        new Thread(() -> {
            try {
                if (key.isEmpty()) {
                    postErro(cb, provider.equals("gemini") ? "🔑 Configure uma chave Gemini em Configurações." : "🔑 Configure uma chave Groq em Configurações.");
                    return;
                }
                if (provider.equals("gemini") && key.startsWith("gsk_")) {
                    postErro(cb, "🔑 Essa parece ser uma chave Groq. Selecione Groq ou coloque uma chave Gemini.");
                    return;
                }
                if (provider.equals("groq") && key.startsWith("AIza")) {
                    postErro(cb, "🔑 Essa parece ser uma chave Gemini. Selecione Gemini ou coloque uma chave Groq.");
                    return;
                }
                if (provider.equals("gemini")) chamadaGemini(key, prompt, pergunta, cb);
                else chamadaGroq(key, prompt, pergunta, cb);
            } catch (UnknownHostException | SocketTimeoutException e) {
                postErro(cb, "📶 Não consegui acessar a internet. Confira sua conexão e tente novamente.");
            } catch (Exception e) {
                postErro(cb, "⚠️ Jarviz encontrou um problema: " + mensagemSegura(e));
            }
        }).start();
    }

    private static void chamadaGroq(String key, String system, String pergunta, Callback cb) throws Exception {
        Exception ultimo = null;
        for (String modelo : GROQ_MODELOS) {
            try {
                JSONObject body = new JSONObject();
                body.put("model", modelo);
                JSONArray msgs = new JSONArray();
                msgs.put(new JSONObject().put("role", "system").put("content", system));
                synchronized (historico) { for (JSONObject m : historico) msgs.put(m); }
                msgs.put(new JSONObject().put("role", "user").put("content", pergunta));
                body.put("messages", msgs);
                body.put("temperature", 0.7);
                body.put("max_completion_tokens", 900);
                HttpResult r = post("https://api.groq.com/openai/v1/chat/completions", key, body, false);
                if (r.code == 200) {
                    JSONObject resp = new JSONObject(r.body);
                    String texto = resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                    guardar(pergunta, texto);
                    postResposta(cb, texto);
                    return;
                }
                if (r.code == 404 && modelo.equals(GROQ_MODELOS[0])) { ultimo = new IOException(extrairErro(r.body, "Modelo ou rota não encontrado.")); continue; }
                tratarHttpErro(r, cb, "Groq");
                return;
            } catch (Exception e) { ultimo = e; }
        }
        if (ultimo != null) throw ultimo;
    }

    private static void chamadaGemini(String key, String system, String pergunta, Callback cb) throws Exception {
        JSONObject body = new JSONObject();
        body.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", system))));
        JSONArray contents = new JSONArray();
        synchronized (historico) {
            for (JSONObject m : historico) {
                String role = m.optString("role", "user");
                if (role.equals("assistant")) role = "model";
                contents.put(new JSONObject().put("role", role).put("parts", new JSONArray().put(new JSONObject().put("text", m.optString("content", "")))));
            }
        }
        contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(new JSONObject().put("text", pergunta))));
        body.put("contents", contents);
        JSONObject generation = new JSONObject();
        generation.put("temperature", 0.7);
        generation.put("maxOutputTokens", 900);
        body.put("generationConfig", generation);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODELO + ":generateContent?key=" + URLEncoder.encode(key, "UTF-8");
        HttpResult r = post(url, null, body, true);
        if (r.code == 200) {
            JSONObject resp = new JSONObject(r.body);
            String texto = resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text", "").trim();
            if (texto.isEmpty()) throw new IOException("A API não retornou texto.");
            guardar(pergunta, texto);
            postResposta(cb, texto);
            return;
        }
        tratarHttpErro(r, cb, "Gemini");
    }

    private static HttpResult post(String endpoint, String key, JSONObject body, boolean gemini) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (!gemini && key != null) conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String text = read(stream);
        conn.disconnect();
        return new HttpResult(code, text);
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder s = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) s.append(line);
            return s.toString();
        }
    }

    private static void guardar(String pergunta, String resposta) {
        synchronized (historico) {
            try {
                historico.add(new JSONObject().put("role", "user").put("content", pergunta));
                historico.add(new JSONObject().put("role", "assistant").put("content", resposta));
                while (historico.size() > MAX_TURNOS * 2) historico.remove(0);
            } catch (Exception ignored) {
                // O histórico não pode impedir uma resposta válida.
            }
        }
    }

    private static void tratarHttpErro(HttpResult r, Callback cb, String provider) {
        String detalhe = extrairErro(r.body, "Resposta sem detalhes.");
        if (r.code == 400) postErro(cb, "⚠️ Pedido inválido na " + provider + ". " + detalhe);
        else if (r.code == 401 || r.code == 403) postErro(cb, "🔑 A chave da " + provider + " foi recusada. Confira a chave e as permissões.");
        else if (r.code == 404) postErro(cb, "❌ A " + provider + " respondeu 404 (recurso não encontrado). " + detalhe);
        else if (r.code == 429) postErro(cb, "⏳ Limite da " + provider + " atingido. Aguarde um pouco e tente novamente.");
        else postErro(cb, "❌ " + provider + " respondeu HTTP " + r.code + ". " + detalhe);
    }

    private static String extrairErro(String body, String fallback) {
        try {
            JSONObject o = new JSONObject(body);
            if (o.has("error")) {
                JSONObject e = o.getJSONObject("error");
                String msg = e.optString("message", "");
                if (!msg.isEmpty()) return msg;
            }
            if (o.has("message")) return o.optString("message", fallback);
        } catch (Exception ignored) {}
        return body == null || body.isEmpty() ? fallback : body.substring(0, Math.min(body.length(), 220));
    }

    private static String mensagemSegura(Exception e) {
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m.replaceAll("(?i)(AIza|gsk_)[A-Za-z0-9_-]+", "[chave ocultada]");
    }

    private static void postResposta(Callback cb, String texto) { new Handler(Looper.getMainLooper()).post(() -> cb.resposta(texto)); }
    private static void postErro(Callback cb, String msg) { new Handler(Looper.getMainLooper()).post(() -> cb.erro(msg)); }

    private static final class HttpResult {
        final int code; final String body;
        HttpResult(int c, String b) { code = c; body = b; }
    }
}
