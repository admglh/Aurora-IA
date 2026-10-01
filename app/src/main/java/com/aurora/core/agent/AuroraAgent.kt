package com.aurora.core.agent

import com.aurora.core.model.ChatMessage
import com.aurora.core.model.GenerationRequest
import com.aurora.core.model.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Monta o contexto (persona + data/hora + memórias relevantes + histórico recente) e delega a geração
 * ao [ModelManager]. Não conhece UI nem banco de conversas.
 *
 * AINDA NÃO EXISTE: uso de ferramentas (tools). O prompt da persona diz isso ao modelo de forma explícita.
 */
class AuroraAgent(
    private val persona: PersonaProvider,
    private val memory: MemoryRetriever,
    private val models: ModelManager
) {
    /** [history] = mensagens anteriores da conversa, em ordem cronológica, SEM a mensagem atual. */
    suspend fun buildRequest(history: List<ChatMessage>, userText: String): GenerationRequest {
        val memories = memory.relevant(userText)
        val system = buildString {
            append(persona.systemPrompt())
            append("\n\n## Contexto atual\nData e hora: ").append(nowText())
            if (memories.isNotEmpty()) {
                append("\n\n## Memórias sobre o usuário\n")
                memories.forEach { append("- ").append(it.content).append('\n') }
            }
        }.trimEnd()

        return GenerationRequest(
            messages = listOf(ChatMessage("system", system)) +
                trimHistory(history) +
                ChatMessage("user", userText)
        )
    }

    fun respond(history: List<ChatMessage>, userText: String): Flow<String> = flow {
        emitAll(models.generate(buildRequest(history, userText)))
    }.flowOn(Dispatchers.IO)

    fun cancel() = models.cancel()

    /** Orçamento por caracteres (aproximação); um tokenizador real virá com o llama.cpp. */
    private fun trimHistory(history: List<ChatMessage>): List<ChatMessage> {
        var used = 0
        val kept = ArrayDeque<ChatMessage>()
        for (m in history.asReversed()) {
            if (used + m.content.length > MAX_HISTORY_CHARS && kept.isNotEmpty()) break
            used += m.content.length
            kept.addFirst(m)
        }
        return kept.toList()
    }

    private fun nowText(): String =
        ZonedDateTime.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
        )

    private companion object {
        const val MAX_HISTORY_CHARS = 6000
    }
}
