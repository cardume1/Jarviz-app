package com.jarviz.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

public class SettingsActivity extends Activity {

    private EditText campoChave;
    private EditText campoNome;
    private Spinner spinnerPersonalidade;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        campoChave = findViewById(R.id.campoChave);
        campoNome = findViewById(R.id.campoNome);
        spinnerPersonalidade = findViewById(R.id.spinnerPersonalidade);
        Button btnSalvar = findViewById(R.id.btnSalvar);
        TextView link = findViewById(R.id.linkGroq);

        campoChave.setText(JarvizConfig.getGroqApiKey(this));
        campoNome.setText(JarvizConfig.getNomeUsuario(this));

        String[] opcoes = {"equilibrado", "engracado", "formal", "direto"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcoes);
        spinnerPersonalidade.setAdapter(adapter);
        String atual = JarvizConfig.getPersonalidade(this);
        for (int i = 0; i < opcoes.length; i++) if (opcoes[i].equals(atual)) spinnerPersonalidade.setSelection(i);

        link.setText("Pegue sua chave grátis em: console.groq.com/keys");

        btnSalvar.setOnClickListener(v -> {
            JarvizConfig.setGroqApiKey(this, campoChave.getText().toString());
            JarvizConfig.setNomeUsuario(this, campoNome.getText().toString());
            JarvizConfig.setPersonalidade(this, opcoes[spinnerPersonalidade.getSelectedItemPosition()]);
            Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
