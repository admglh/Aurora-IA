package com.aurora.core.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente de endpoint compatível com a API de chat da OpenAI (OpenAI, llama-server, Ollama, etc.).
 * [endpoint] é a URL base incluindo /v1 (ex.: https://host/v1); o app acrescenta /chat/completions.
 * [apiKey] é opcional (servidores locais não exigem).
 *
 * STATUS: implementado (streaming SSE + fallback para resposta JSON completa), AINDA NÃO TESTADO contra
 * um servidor real. "Ready" significa apenas "configurado": a conexão só é exercitada ao gerar.
 */
class OpenAICompatibleProvider(
    private val endpoint: String,
    private val model: String,
    private val apiKey: String?
) : ModelProvider {

    override val id = "openai-compatible"
    override val label = "Endpoint remoto ($model)"

    private val configured = endpoint.isNotBlank() && model.isNotBlank()
    private val idleState: ModelState =
        if (configured) ModelState.Ready(label)
        else ModelState.NotConfigured("Endpoint remoto não configurado.")

    private val _state = MutableStateFlow(idleState)
    override val state: StateFlow<ModelState> = _state.asStateFlow()

    @Volatile private var cancelled = false
    @Volatile private var connection: HttpURLConnection? = null

    override suspend fun load() { /* nada a carregar; a configuração já define o estado */ }

    override fun generate(request: GenerationRequest): Flow<String> = flow {
        check(configured) { "Endpoint remoto não configurado." }
        cancelled = false
        _state.value = ModelState.Generating
        try {
            val conn = openConnection(request)
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(300).orEmpty()
                throw IOException("HTTP $code" + if (err.isNotBlank()) ": $err" else "")
            }
            var emitted = false
            val nonSse = StringBuilder()
            conn.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) {
                        if (nonSse.length < MAX_NON_SSE_CHARS) nonSse.append(line).append('\n')
                        continue
                    }
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    val piece = parseDelta(payload)
                    if (piece.isNotEmpty()) {
                        emitted = true
                        emit(piece)
                    }
                }
            }
            if (!emitted && !cancelled) {
                // Alguns servidores ignoram stream=true e devolvem o JSON completo.
                val full = parseFullMessage(nonSse.toString())
                if (full.isNotEmpty()) emit(full)
                else if (nonSse.isNotBlank()) {
                    throw IOException("Resposta inesperada do servidor: " + nonSse.toString().trim().take(200))
                }
            }
        } catch (e: IOException) {
            if (!cancelled) throw e
        } finally {
            connection?.disconnect()
            connection = null
            _state.value = idleState
        }
    }.flowOn(Dispatchers.IO)

    override fun cancel() {
        cancelled = true
        connection?.disconnect()
    }

    override fun release() = cancel()

    private fun openConnection(request: GenerationRequest): HttpURLConnection {
        val conn = URL(endpoint.trimEnd('/') + "/chat/completions").openConnection() as HttpURLConnection
        connection = conn
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 120_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "text/event-stream")
        if (!apiKey.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $apiKey")

        val msgs = JSONArray()
        request.messages.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject()
            .put("model", model)
            .put("messages", msgs)
            .put("stream", true)
            .put("temperature", request.temperature.toDouble())
            .put("max_tokens", request.maxTokens)
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        return conn
    }

    private fun parseDelta(payload: String): String = try {
        val delta = JSONObject(payload)
            .optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
        if (delta != null && !delta.isNull("content")) delta.getString("content") else ""
    } catch (e: Exception) {
        ""
    }

    private fun parseFullMessage(raw: String): String = try {
        val msg = JSONObject(raw.trim())
            .optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
        if (msg != null && !msg.isNull("content")) msg.getString("content") else ""
    } catch (e: Exception) {
        ""
    }

    private companion object {
        const val MAX_NON_SSE_CHARS = 20_000
    }
}
