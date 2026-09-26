package com.jarviz.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private LinearLayout chat;
    private ScrollView scroll;
    private TextView status;
    private EditText campoMsg;
    private Button btnMic;

    private boolean ouvindoContinuo = false;
    private boolean processandoResposta = false;

    private static final String[] PALAVRAS_ATIVACAO = {"jarviz", "ok jarviz", "e ai jarviz", "jarves"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);
        chat = findViewById(R.id.chat);
        scroll = findViewById(R.id.scroll);
        status = findViewById(R.id.status);
        campoMsg = findViewById(R.id.campoMsg);
        btnMic = findViewById(R.id.btnMic);

        tts = new TextToSpeech(this, st -> {
            if (st == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("pt", "BR"));
                tts.setPitch(1.0f);
                tts.setSpeechRate(1.02f);
            }
        });

        btnMic.setOnClickListener(v -> {
            if (ouvindoContinuo) {
                pararModoContinuo();
            } else {
                iniciarOuvir(false);
            }
        });

        btnMic.setOnLongClickListener(v -> {
            // Toque longo no microfone liga/desliga o "modo sempre ouvindo"
            if (ouvindoContinuo) pararModoContinuo();
            else iniciarModoContinuo();
            return true;
        });

        findViewById(R.id.btnEnviar).setOnClickListener(v -> enviarMsg());

        pedirPermissoes();

        String nome = JarvizConfig.getNomeUsuario(this);
        String saudacao = nome.isEmpty()
            ? "Olá! Eu sou o Jarviz. Toque no microfone pra falar comigo, ou segure ele pra eu ficar sempre ouvindo."
            : "E aí, " + nome + "! Bom te ver de novo. Tô aqui, pode falar.";
        adicionarMsg("Jarviz", saudacao, false);

        if (!JarvizConfig.temChaveConfigurada(this)) {
            adicionarMsg("Jarviz", "⚠️ Antes de começarmos: vá em ⋮ > Configurações e cole sua chave da Groq (é grátis, pega em console.groq.com/keys).", false);
        }
    }

    private void pedirPermissoes() {
        String[] perms = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS
        };
        ArrayList<String> faltando = new ArrayList<>();
        for (String p : perms)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) faltando.add(p);
        if (!faltando.isEmpty())
            requestPermissions(faltando.toArray(new String[0]), 100);
    }

    private void falar(String texto) {
        if (!JarvizConfig.isVozAtiva(this)) return;
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // ---------- Escuta ----------

    private void iniciarOuvir(boolean silencioso) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != 0) {
            Toast.makeText(this, "Dê permissão de microfone nas configurações do app", Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText("● Ouvindo...");
        animarMic(true);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        try {
            startActivityForResult(i, 101);
        } catch (Exception e) {
            status.setText("● Online");
            animarMic(false);
            Toast.makeText(this, "Reconhecimento de voz indisponível neste aparelho", Toast.LENGTH_SHORT).show();
        }
    }

    private void iniciarModoContinuo() {
        ouvindoContinuo = true;
        btnMic.setText("🛑");
        adicionarMsg("Jarviz", "🎙️ Modo contínuo ativado. Diga \"Jarviz\" antes do que quiser me pedir. Segure o microfone de novo pra desligar.", false);
        iniciarOuvir(true);
    }

    private void pararModoContinuo() {
        ouvindoContinuo = false;
        btnMic.setText("🎤");
        status.setText("● Online");
        animarMic(false);
    }

    @Override
    protected void onActivityResult(int cod, int res, Intent d) {
        super.onActivityResult(cod, res, d);
        status.setText("● Online");
        animarMic(false);

        if (cod == 101) {
            if (res == RESULT_OK && d != null) {
                ArrayList<String> frases = d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (frases != null && !frases.isEmpty()) {
                    String dito = frases.get(0);
                    tratarFalaReconhecida(dito);
                    return;
                }
            }
            // Nada reconhecido — se estiver em modo contínuo, continua ouvindo
            if (ouvindoContinuo) iniciarOuvir(true);
        }
    }

    private void tratarFalaReconhecida(String dito) {
        String ditoLower = dito.toLowerCase(new Locale("pt", "BR"));

        if (ouvindoContinuo) {
            boolean ativou = false;
            String resto = ditoLower;
            for (String palavra : PALAVRAS_ATIVACAO) {
                if (ditoLower.contains(palavra)) {
                    ativou = true;
                    resto = ditoLower.replaceFirst(palavra, "").trim();
                    break;
                }
            }
            if (!ativou) {
                // Não disse a palavra de ativação: ignora e continua ouvindo em silêncio
                iniciarOuvir(true);
                return;
            }
            if (resto.isEmpty()) {
                falar("Diga.");
                adicionarMsg("Jarviz", "🙂 Tô ouvindo, pode falar.", false);
                iniciarOuvir(true);
                return;
            }
            adicionarMsg("Você", dito, true);
            processar(resto);
            // volta a ouvir depois de processar (o próprio processar/callback já cuida do fluxo)
        } else {
            adicionarMsg("Você", dito, true);
            processar(ditoLower);
        }
    }

    // ---------- Envio por texto ----------

    private void enviarMsg() {
        String txt = campoMsg.getText().toString().trim();
        if (txt.isEmpty()) return;
        campoMsg.setText("");
        adicionarMsg("Você", txt, true);
        processar(txt.toLowerCase(new Locale("pt", "BR")));
    }

    // ---------- Comandos ----------

    private void processar(String m) {
        if (m.contains("ligar para")) {
            String alvo = m.replaceAll(".*ligar para", "").trim();
            String numero = resolverNumeroContato(alvo);
            if (numero == null) numero = alvo;
            adicionarMsg("Jarviz", "Ligando para " + alvo + "...", false);
            falar("Ligando para " + alvo);
            try {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + numero)));
            } catch (SecurityException e) {
                adicionarMsg("Jarviz", "Preciso da permissão de ligação pra fazer isso.", false);
            }
            retomarEscutaSeNecessario();
            return;
        }

        if (m.contains("mandar mensagem para") || m.contains("manda mensagem para") || m.contains("mandar sms para")) {
            String alvo = m.replaceAll(".*(mandar mensagem para|manda mensagem para|mandar sms para)", "").trim();
            adicionarMsg("Jarviz", "Abrindo mensagem para " + alvo + ". O que você quer escrever?", false);
            falar("Abrindo mensagem para " + alvo);
            String numero = resolverNumeroContato(alvo);
            Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + (numero != null ? numero : "")));
            try { startActivity(smsIntent); } catch (Exception ignored) {}
            retomarEscutaSeNecessario();
            return;
        }

        if (m.contains("abrir ")) {
            String nomeApp = m.replaceAll(".*abrir ", "").trim();
            boolean abriu = abrirAppPorNome(nomeApp);
            adicionarMsg("Jarviz", abriu ? "Abrindo " + nomeApp + "..." : "Não achei um app chamado \"" + nomeApp + "\" no aparelho.", false);
            falar(abriu ? "Abrindo " + nomeApp : "Não encontrei esse aplicativo");
            retomarEscutaSeNecessario();
            return;
        }

        if (m.contains("desligar voz")) {
            JarvizConfig.setVozAtiva(this, false);
            adicionarMsg("Jarviz", "🔇 Voz desligada.", false);
            retomarEscutaSeNecessario();
            return;
        }
        if (m.contains("ligar voz")) {
            JarvizConfig.setVozAtiva(this, true);
            adicionarMsg("Jarviz", "🔊 Voz ligada.", false);
            falar("Voz ligada");
            retomarEscutaSeNecessario();
            return;
        }
        if (m.contains("esquece o que a gente falou") || m.contains("limpar conversa") || m.contains("apaga o historico")) {
            JarvizAI.limparHistorico();
            adicionarMsg("Jarviz", "🧹 Prontinho, esqueci nossa conversa anterior.", false);
            falar("Memória limpa");
            retomarEscutaSeNecessario();
            return;
        }

        // Pergunta livre -> IA
        processandoResposta = true;
        status.setText("● Pensando...");
        JarvizAI.perguntar(this, m, new JarvizAI.Callback() {
            public void resposta(String t) {
                adicionarMsg("Jarviz", t, false);
                falar(t);
                processandoResposta = false;
                status.setText("● Online");
                retomarEscutaSeNecessario();
            }
            public void erro(String e) {
                adicionarMsg("Jarviz", e, false);
                processandoResposta = false;
                status.setText("● Online");
                retomarEscutaSeNecessario();
            }
        });
    }

    private void retomarEscutaSeNecessario() {
        if (ouvindoContinuo && !processandoResposta) {
            chat.postDelayed(() -> { if (ouvindoContinuo) iniciarOuvir(true); }, 400);
        }
    }

    private String resolverNumeroContato(String nome) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return null;
        String numero = null;
        android.database.Cursor c = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            new String[]{"%" + nome + "%"}, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (idx >= 0) numero = c.getString(idx);
            }
            c.close();
        }
        return numero;
    }

    private boolean abrirAppPorNome(String nomeApp) {
        PackageManager pm = getPackageManager();
        List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (android.content.pm.ApplicationInfo app : apps) {
            String label = pm.getApplicationLabel(app).toString();
            if (label.toLowerCase(new Locale("pt", "BR")).contains(nomeApp)) {
                Intent launch = pm.getLaunchIntentForPackage(app.packageName);
                if (launch != null) {
                    startActivity(launch);
                    return true;
                }
            }
        }
        return false;
    }

    // ---------- UI ----------

    private void adicionarMsg(String quem, String texto, boolean usuario) {
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(usuario ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams linhaParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linhaParams.setMargins(0, 8, 0, 8);
        linha.setLayoutParams(linhaParams);

        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(15f);
        tv.setPadding(28, 20, 28, 20);
        tv.setBackgroundResource(usuario ? R.drawable.bubble_user : R.drawable.bubble_ai);

        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvParams.width = Math.min(tvParams.width, (int) (getResources().getDisplayMetrics().widthPixels * 0.78));
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78));

        linha.addView(tv, tvParams);
        chat.addView(linha, linhaParams);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void animarMic(boolean ativo) {
        if (ativo) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(btnMic, "alpha", 1f, 0.4f, 1f);
            anim.setDuration(900);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            btnMic.setTag(anim);
            anim.start();
        } else {
            Object tag = btnMic.getTag();
            if (tag instanceof ObjectAnimator) {
                ((ObjectAnimator) tag).cancel();
                btnMic.setAlpha(1f);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, JarvizConfig.isVozAtiva(this) ? "🔇 Desligar Voz" : "🔊 Ligar Voz");
        menu.add(0, 2, 1, "⚙️ Configurações");
        menu.add(0, 3, 2, "🧹 Limpar conversa");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                JarvizConfig.setVozAtiva(this, !JarvizConfig.isVozAtiva(this));
                falar(JarvizConfig.isVozAtiva(this) ? "Voz ligada" : "Voz desligada");
                invalidateOptionsMenu();
                return true;
            case 2:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case 3:
                JarvizAI.limparHistorico();
                chat.removeAllViews();
                adicionarMsg("Jarviz", "🧹 Conversa limpa. Bora recomeçar!", false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
