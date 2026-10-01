package com.aurora.core.model

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Provedor local de GGUF usando o AAR pré-compilado llama.cpp para Android.
 *
 * O runtime é empacotado pelo Gradle; nenhum servidor, API key ou Termux é necessário
 * para a inferência. O modelo continua sendo importado para filesDir/models/.
 */
class LocalGgufProvider(private val modelFile: File) : ModelProvider {

    override val id = "local-gguf"
    override val label = "Qwen local (GGUF / llama.cpp)"

    private val _state = MutableStateFlow<ModelState>(ModelState.Loading("Verificando modelo local…"))
    override val state: StateFlow<ModelState> = _state.asStateFlow()

    override suspend fun load() {
        _state.value = when {
            !modelFile.exists() || modelFile.length() < 1024L * 1024L ->
                ModelState.NotConfigured("Arquivo do modelo local não encontrado ou inválido (${modelFile.name}).")
            else ->
                ModelState.Ready(label)
        }
    }

    override fun generate(request: GenerationRequest): Flow<String> = flow {
        require(modelFile.exists()) { "Modelo local não encontrado: ${modelFile.name}" }
        _state.value = ModelState.Generating
        val config = LlamaConfig(
            contextSize = 4096,
            threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
        )
        val loaded = Llama.loadModel(modelFile.absolutePath, config)
        try {
            val system = request.messages
                .firstOrNull { it.role == "system" }
                ?.content
                .orEmpty()

            val prompt = request.messages
                .filter { it.role != "system" }
                .joinToString("\n\n") { message ->
                    when (message.role) {
                        "user" -> "Usuário: ${message.content}"
                        "assistant" -> "Aurora: ${message.content}"
                        else -> message.content
                    }
                } + "\n\nAurora:"

            val result = Llama.complete(
                loaded,
                prompt = prompt,
                systemPrompt = system,
                maxTokens = request.maxTokens
            )
            emit(result.text.trim())
            _state.value = ModelState.Ready(label)
        } catch (t: Throwable) {
            _state.value = ModelState.Error(
                t.message?.takeIf { it.isNotBlank() } ?: "Falha ao executar o modelo GGUF."
            )
            throw t
        } finally {
            Llama.releaseModel(loaded)
        }
    }

    override fun cancel() {
        // A API gratuita do AAR expõe complete() como uma operação suspend.
        // O cancelamento do Flow cancela a coroutine que executa a inferência.
    }

    override fun release() {
        _state.value = ModelState.NotConfigured("Modelo local descarregado.")
    }
}
