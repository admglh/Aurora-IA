package com.aurora.core.agent

import android.content.Context
import android.util.Log
import java.io.IOException

/** Carrega o system prompt da Aurora 5.0 de assets/persona/aurora_5_0.md. */
class PersonaProvider(private val context: Context) {
    @Volatile private var cached: String? = null

    fun systemPrompt(): String = cached ?: load().also { cached = it }

    private fun load(): String = try {
        context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .trim()
    } catch (e: IOException) {
        Log.e(TAG, "Persona não encontrada em assets/$ASSET; usando prompt mínimo.", e)
        FALLBACK
    }

    private companion object {
        const val TAG = "PersonaProvider"
        const val ASSET = "persona/aurora_5_0.md"
        const val FALLBACK = "Você é a Aurora 5.0, assistente pessoal. Responda em português do Brasil. " +
            "Não afirme ter acesso a ferramentas ou dados que você não tem."
    }
}
