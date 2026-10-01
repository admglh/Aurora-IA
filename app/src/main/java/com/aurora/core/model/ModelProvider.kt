package com.aurora.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Estado REAL de um modelo. A UI deve mostrar exatamente isto, nunca um estado inventado. */
sealed class ModelState {
    /** Nada utilizável configurado. [reason] explica por quê, em linguagem para o usuário. */
    data class NotConfigured(val reason: String) : ModelState()
    data class Loading(val detail: String) : ModelState()
    /** Pronto para gerar. [label] descreve a origem (ex.: "Endpoint remoto (modelo X)"). */
    data class Ready(val label: String) : ModelState()
    data object Generating : ModelState()
    /** O modelo não consegue operar (ex.: falha ao carregar). */
    data class Error(val message: String) : ModelState()
}

data class ChatMessage(val role: String, val content: String)

data class GenerationRequest(
    val messages: List<ChatMessage>,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f
)

interface ModelProvider {
    val id: String
    val label: String
    val state: StateFlow<ModelState>

    /** Prepara o provedor (carregar modelo, validar configuração). Não deve lançar: reporta via [state]. */
    suspend fun load()

    /**
     * Gera a resposta em pedaços de texto (streaming). Lança exceção em caso de falha real.
     * Cancelar a coleta do Flow OU chamar [cancel] interrompe a geração.
     */
    fun generate(request: GenerationRequest): Flow<String>

    fun cancel()
    fun release()
}
