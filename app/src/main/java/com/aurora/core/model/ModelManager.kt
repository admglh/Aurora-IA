package com.aurora.core.model

import android.content.Context
import android.net.Uri
import com.aurora.core.settings.AuroraSettings
import com.aurora.core.settings.SecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Escolhe o provedor ativo (local primeiro, depois endpoint remoto) e expõe UM estado real para a UI.
 * Chame [refresh] após qualquer mudança de configuração.
 */
class ModelManager(
    context: Context,
    private val settings: AuroraSettings,
    private val secrets: SecretStore,
    private val scope: CoroutineScope
) {
    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    private val _state = MutableStateFlow<ModelState>(ModelState.Loading("Inicializando…"))
    val state: StateFlow<ModelState> = _state.asStateFlow()

    @Volatile private var provider: ModelProvider? = null
    private var stateJob: Job? = null

    suspend fun refresh() {
        mutex.withLock {
            stateJob?.cancel()
            stateJob = null
            provider?.release()
            provider = null
            _state.value = ModelState.Loading("Verificando modelos…")

            val local = LocalGgufProvider(File(modelsDir, settings.localModelFileName))
            val remote = OpenAICompatibleProvider(
                endpoint = settings.remoteEndpoint,
                model = settings.remoteModel,
                apiKey = secrets.get(SecretStore.KEY_REMOTE_API)
            )
            local.load()
            remote.load()

            val candidates = listOf(local, remote)
            val chosen = candidates.firstOrNull { it.state.value is ModelState.Ready }
            if (chosen == null) {
                candidates.forEach { it.release() }
                val states = candidates.map { it.label to it.state.value }
                val firstError = states.firstNotNullOfOrNull { (it.second as? ModelState.Error)?.message }
                _state.value = if (firstError != null) {
                    ModelState.Error(firstError)
                } else {
                    val lines = states.mapNotNull { (label, s) ->
                        (s as? ModelState.NotConfigured)?.let { "$label: ${it.reason}" }
                    }
                    ModelState.NotConfigured(lines.joinToString("\n"))
                }
            } else {
                candidates.filter { it !== chosen }.forEach { it.release() }
                provider = chosen
                _state.value = chosen.state.value
                stateJob = scope.launch { chosen.state.collect { _state.value = it } }
            }
        }
    }

    fun refreshAsync() {
        scope.launch { refresh() }
    }

    /** Copia um GGUF escolhido pelo usuário para o armazenamento privado do app. */
    suspend fun importLocalModel(uri: Uri, displayName: String): Result<File> = runCatching {
        val safeName = displayName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { AuroraSettings.DEFAULT_LOCAL_MODEL_FILE }
        val target = File(modelsDir, safeName)
        appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Não foi possível abrir o arquivo escolhido." }
            target.outputStream().use { output -> input.copyTo(output, 1024 * 1024) }
        }
        settings.localModelFileName = target.name
        refresh()
        target
    }

    fun localModelFile(): File = File(modelsDir, settings.localModelFileName)

    fun generate(request: GenerationRequest): Flow<String> {
        val p = provider
            ?: return flow { throw IllegalStateException("Nenhum modelo disponível.") }
        return p.generate(request)
    }

    fun cancel() {
        provider?.cancel()
    }
}
