package com.jarviz.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

public class SettingsActivity extends Activity {
    private EditText groq, gemini, nome;
    private Spinner provedor, personalidade;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        groq = findViewById(R.id.campoGroq);
        gemini = findViewById(R.id.campoGemini);
        nome = findViewById(R.id.campoNome);
        provedor = findViewById(R.id.spinnerProvedor);
        personalidade = findViewById(R.id.spinnerPersonalidade);
        Button salvar = findViewById(R.id.btnSalvar);

        groq.setText(JarvizConfig.getGroqApiKey(this));
        gemini.setText(JarvizConfig.getGeminiApiKey(this));
        nome.setText(JarvizConfig.getNomeUsuario(this));

        String[] providers = {"Groq", "Gemini"};
        String[] providerIds = {"groq", "gemini"};
        provedor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, providers));
        String atualProvider = JarvizConfig.getProvedor(this);
        for (int i = 0; i < providerIds.length; i++) if (providerIds[i].equals(atualProvider)) provedor.setSelection(i);

        String[] estilos = {"equilibrado", "engracado", "formal", "direto"};
        personalidade.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, estilos));
        String atual = JarvizConfig.getPersonalidade(this);
        for (int i = 0; i < estilos.length; i++) if (estilos[i].equals(atual)) personalidade.setSelection(i);

        salvar.setOnClickListener(v -> {
            JarvizConfig.setGroqApiKey(this, groq.getText().toString());
            JarvizConfig.setGeminiApiKey(this, gemini.getText().toString());
            JarvizConfig.setProvedor(this, providerIds[provedor.getSelectedItemPosition()]);
            JarvizConfig.setNomeUsuario(this, nome.getText().toString());
            JarvizConfig.setPersonalidade(this, estilos[personalidade.getSelectedItemPosition()]);
            Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
