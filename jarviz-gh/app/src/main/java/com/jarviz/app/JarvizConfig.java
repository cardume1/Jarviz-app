package com.jarviz.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class JarvizConfig {
    private static final String CHAVE_GROQ = "groq_api_key";
    private static final String CHAVE_GEMINI = "gemini_api_key";
    private static final String CHAVE_PROVEDOR = "ai_provider";
    private static final String CHAVE_VOZ_ATIVA = "voz_ativa";
    private static final String CHAVE_NOME_USUARIO = "nome_usuario";
    private static final String CHAVE_MODO_CONTINUO = "modo_continuo";
    private static final String CHAVE_PERSONALIDADE = "personalidade";

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }

    public static String getGroqApiKey(Context ctx) { return prefs(ctx).getString(CHAVE_GROQ, ""); }
    public static void setGroqApiKey(Context ctx, String key) { prefs(ctx).edit().putString(CHAVE_GROQ, key == null ? "" : key.trim()).apply(); }

    public static String getGeminiApiKey(Context ctx) { return prefs(ctx).getString(CHAVE_GEMINI, ""); }
    public static void setGeminiApiKey(Context ctx, String key) { prefs(ctx).edit().putString(CHAVE_GEMINI, key == null ? "" : key.trim()).apply(); }

    public static String getProvedor(Context ctx) { return prefs(ctx).getString(CHAVE_PROVEDOR, "groq"); }
    public static void setProvedor(Context ctx, String provider) { prefs(ctx).edit().putString(CHAVE_PROVEDOR, provider == null ? "groq" : provider).apply(); }

    public static boolean temChaveConfigurada(Context ctx) {
        return getProvedor(ctx).equals("gemini") ? !getGeminiApiKey(ctx).isEmpty() : !getGroqApiKey(ctx).isEmpty();
    }

    public static boolean isVozAtiva(Context ctx) { return prefs(ctx).getBoolean(CHAVE_VOZ_ATIVA, true); }
    public static void setVozAtiva(Context ctx, boolean ativa) { prefs(ctx).edit().putBoolean(CHAVE_VOZ_ATIVA, ativa).apply(); }

    public static boolean isModoContinuo(Context ctx) { return prefs(ctx).getBoolean(CHAVE_MODO_CONTINUO, false); }
    public static void setModoContinuo(Context ctx, boolean ativo) { prefs(ctx).edit().putBoolean(CHAVE_MODO_CONTINUO, ativo).apply(); }

    public static String getNomeUsuario(Context ctx) { return prefs(ctx).getString(CHAVE_NOME_USUARIO, ""); }
    public static void setNomeUsuario(Context ctx, String nome) { prefs(ctx).edit().putString(CHAVE_NOME_USUARIO, nome == null ? "" : nome.trim()).apply(); }

    public static String getPersonalidade(Context ctx) { return prefs(ctx).getString(CHAVE_PERSONALIDADE, "equilibrado"); }
    public static void setPersonalidade(Context ctx, String estilo) { prefs(ctx).edit().putString(CHAVE_PERSONALIDADE, estilo == null ? "equilibrado" : estilo).apply(); }
}
