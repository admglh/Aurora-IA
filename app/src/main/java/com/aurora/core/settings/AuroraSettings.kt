package com.aurora.core.settings

import android.content.Context

/**
 * Configurações NÃO secretas (SharedPreferences privado; o app tem allowBackup=false).
 * Segredos (chaves de API) ficam em [SecretStore], nunca aqui.
 */
class AuroraSettings(context: Context) {
    private val prefs = context.getSharedPreferences("aurora_settings", Context.MODE_PRIVATE)

    var remoteEndpoint: String
        get() = prefs.getString(K_ENDPOINT, "") ?: ""
        set(v) { prefs.edit().putString(K_ENDPOINT, v.trim()).apply() }

    var remoteModel: String
        get() = prefs.getString(K_MODEL, "") ?: ""
        set(v) { prefs.edit().putString(K_MODEL, v.trim()).apply() }

    /** Nome do arquivo GGUF dentro de filesDir/models/. */
    var localModelFileName: String
        get() = prefs.getString(K_LOCAL_FILE, DEFAULT_LOCAL_MODEL_FILE) ?: DEFAULT_LOCAL_MODEL_FILE
        set(v) { prefs.edit().putString(K_LOCAL_FILE, v.trim()).apply() }

    /** Última conversa aberta (-1 = nenhuma). */
    var lastConversationId: Long
        get() = prefs.getLong(K_LAST_CONV, -1L)
        set(v) { prefs.edit().putLong(K_LAST_CONV, v).apply() }

    /** Exibe a presença animada na tela inicial. Desligada por padrão para uma interface mais discreta. */
    var animationEnabled: Boolean
        get() = prefs.getBoolean(K_ANIMATION_ENABLED, false)
        set(v) { prefs.edit().putBoolean(K_ANIMATION_ENABLED, v).apply() }

    var voiceEnabled: Boolean
        get() = prefs.getBoolean(K_VOICE_ENABLED, true)
        set(v) { prefs.edit().putBoolean(K_VOICE_ENABLED, v).apply() }

    var voicePitch: Float
        get() = prefs.getFloat(K_VOICE_PITCH, 0.72f)
        set(v) { prefs.edit().putFloat(K_VOICE_PITCH, v.coerceIn(0.55f, 1.0f)).apply() }

    var voiceRate: Float
        get() = prefs.getFloat(K_VOICE_RATE, 0.88f)
        set(v) { prefs.edit().putFloat(K_VOICE_RATE, v.coerceIn(0.70f, 1.05f)).apply() }

    /** true quando a importação da Aurora 2.0 terminou SEM erro fatal. */
    var legacyImportDone: Boolean
        get() = prefs.getBoolean(K_LEGACY_DONE, false)
        set(v) { prefs.edit().putBoolean(K_LEGACY_DONE, v).apply() }

    /** Relatório da última tentativa de importação (para a futura tela de diagnóstico). */
    var lastImportReport: String
        get() = prefs.getString(K_LEGACY_REPORT, "") ?: ""
        set(v) { prefs.edit().putString(K_LEGACY_REPORT, v).apply() }

    companion object {
        const val DEFAULT_LOCAL_MODEL_FILE = "Qwen3-1.7B-Q8_0.gguf"
        private const val K_ENDPOINT = "remote_endpoint"
        private const val K_ANIMATION_ENABLED = "animation_enabled"
        private const val K_VOICE_ENABLED = "voice_enabled"
        private const val K_VOICE_PITCH = "voice_pitch"
        private const val K_VOICE_RATE = "voice_rate"
        private const val K_MODEL = "remote_model"
        private const val K_LOCAL_FILE = "local_model_file"
        private const val K_LAST_CONV = "last_conversation_id"
        private const val K_LEGACY_DONE = "legacy_import_done"
        private const val K_LEGACY_REPORT = "legacy_import_report"
    }
}
