package com.jarviz.app;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Configurações do Jarviz.
 * IMPORTANTE: a versão anterior tinha uma chave de API da Groq
 * hardcoded no código-fonte (visível para qualquer pessoa que
 * descompilasse o APK). Isso foi removido por segurança.
 * Agora a chave só existe se o próprio usuário colocar na tela
 * de Configurações, e fica guardada localmente no aparelho.
 */
public class JarvizConfig {
    private static final String CHAVE_GROQ = "groq_api_key";
    private static final String CHAVE_VOZ_ATIVA = "voz_ativa";
    private static final String CHAVE_NOME_USUARIO = "nome_usuario";
    private static final String CHAVE_MODO_CONTINUO = "modo_continuo";
    private static final String CHAVE_PERSONALIDADE = "personalidade";

    public static String getGroqApiKey(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(CHAVE_GROQ, "");
    }

    public static void setGroqApiKey(Context ctx, String chave) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putString(CHAVE_GROQ, chave == null ? "" : chave.trim())
            .apply();
    }

    public static boolean temChaveConfigurada(Context ctx) {
        return !getGroqApiKey(ctx).isEmpty();
    }

    public static boolean isVozAtiva(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
            .getBoolean(CHAVE_VOZ_ATIVA, true);
    }

    public static void setVozAtiva(Context ctx, boolean ativa) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putBoolean(CHAVE_VOZ_ATIVA, ativa).apply();
    }

    public static boolean isModoContinuo(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
            .getBoolean(CHAVE_MODO_CONTINUO, false);
    }

    public static void setModoContinuo(Context ctx, boolean ativo) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putBoolean(CHAVE_MODO_CONTINUO, ativo).apply();
    }

    public static String getNomeUsuario(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(CHAVE_NOME_USUARIO, "");
    }

    public static void setNomeUsuario(Context ctx, String nome) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putString(CHAVE_NOME_USUARIO, nome == null ? "" : nome.trim())
            .apply();
    }

    /** Estilo de personalidade: "equilibrado", "engracado", "formal", "direto" */
    public static String getPersonalidade(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(CHAVE_PERSONALIDADE, "equilibrado");
    }

    public static void setPersonalidade(Context ctx, String estilo) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putString(CHAVE_PERSONALIDADE, estilo).apply();
    }
}
