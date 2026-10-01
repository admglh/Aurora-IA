package com.aurora.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.AuroraApplication
import com.aurora.core.database.ConversationEntity
import com.aurora.core.database.MessageEntity
import com.aurora.core.database.Roles
import com.aurora.core.database.MemoryEntity
import com.aurora.core.tools.DeviceInfoTool
import com.aurora.core.model.ChatMessage
import com.aurora.core.model.ModelState
import com.aurora.core.settings.SecretStore
import com.aurora.core.voice.AuroraVoice
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(
    val id: Long,
    val role: String,
    val text: String,
    /** true enquanto a resposta ainda está sendo gerada (ainda não gravada no banco). */
    val streaming: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AuroraViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as AuroraApplication).container
    private val chat = container.chat
    private val agent = container.agent
    private val settings = container.settings
    private val voice = AuroraVoice(app.applicationContext)

    private val _animationEnabled = MutableStateFlow(settings.animationEnabled)
    val animationEnabled: StateFlow<Boolean> = _animationEnabled.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(settings.voiceEnabled)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()
    private val _voicePitch = MutableStateFlow(settings.voicePitch)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()
    private val _voiceRate = MutableStateFlow(settings.voiceRate)
    val voiceRate: StateFlow<Float> = _voiceRate.asStateFlow()
    private val _localModelName = MutableStateFlow(settings.localModelFileName)
    val localModelName: StateFlow<String> = _localModelName.asStateFlow()

    /** Estado REAL do modelo (não existe estado "fingido" na UI). */
    val modelState: StateFlow<ModelState> = container.models.state

    val conversations: StateFlow<List<ConversationEntity>> =
        chat.observeConversations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val memories: StateFlow<List<MemoryEntity>> =
        container.db.memories().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val deviceTool = DeviceInfoTool(app.applicationContext)
    private val _deviceInfo = MutableStateFlow("Consultando…")
    val deviceInfo: StateFlow<String> = _deviceInfo.asStateFlow()

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    /** null = conversa nova ainda não criada (é criada ao enviar a primeira mensagem). */
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val streaming = MutableStateFlow<String?>(null)

    val messages: StateFlow<List<UiMessage>> = combine(
        _activeConversationId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList<MessageEntity>()) else chat.observeMessages(id)
        },
        streaming
    ) { persisted, partial ->
        persisted.map { UiMessage(it.id, it.role, it.content) } +
            (partial?.let { listOf(UiMessage(STREAMING_ID, Roles.ASSISTANT, it, streaming = true)) }
                ?: emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isGenerating: StateFlow<Boolean> =
        streaming.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var generationJob: Job? = null

    init {
        viewModelScope.launch { _deviceInfo.value = deviceTool.run(emptyMap()) }
        viewModelScope.launch {
            container.startup.join()
            val last = settings.lastConversationId
            if (last > 0 && chat.conversationExists(last)) _activeConversationId.value = last
        }
    }

    /** @return true se a mensagem foi aceita (a UI só limpa o campo de texto nesse caso). */
    fun send(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty()) return false
        if (generationJob?.isActive == true) return false
        if (modelState.value !is ModelState.Ready) {
            _error.value = "Nenhum modelo está pronto. Configure um modelo antes de conversar."
            return false
        }
        _error.value = null

        generationJob = viewModelScope.launch {
            var conversationId: Long? = null
            val buffer = StringBuilder()
            try {
                val id = _activeConversationId.value ?: chat.createConversation().also {
                    _activeConversationId.value = it
                    settings.lastConversationId = it
                }
                conversationId = id
                val history = chat.recentHistory(id, HISTORY_MESSAGES)
                    .map { ChatMessage(it.role, it.content) }
                chat.addMessage(id, Roles.USER, clean)

                streaming.value = ""
                agent.respond(history, clean).collect { piece ->
                    buffer.append(piece)
                    streaming.value = buffer.toString()
                }
                if (buffer.isBlank()) _error.value = "O modelo não retornou texto."
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _error.value = "Falha ao gerar a resposta: " + (t.message ?: t.javaClass.simpleName)
            } finally {
                withContext(NonCancellable) {
                    val id = conversationId
                    if (id != null && buffer.isNotBlank()) chat.addMessage(id, Roles.ASSISTANT, buffer.toString())
                    streaming.value = null
                }
            }
        }
        return true
    }

    /** Interrompe a geração; o texto parcial já recebido é gravado como resposta. */
    fun stop() {
        agent.cancel()
        generationJob?.cancel()
    }

    fun dismissError() { _error.value = null }

    fun newConversation() {
        stop()
        _activeConversationId.value = null
        settings.lastConversationId = -1L
    }

    fun openConversation(id: Long) {
        stop()
        _activeConversationId.value = id
        settings.lastConversationId = id
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            if (_activeConversationId.value == id) {
                stop()
                _activeConversationId.value = null
                settings.lastConversationId = -1L
            }
            chat.deleteConversation(id)
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch { chat.deleteMessage(id) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { container.db.memories().delete(id) }
    }

    // --- Configuração PROVISÓRIA do endpoint remoto (a tela de Configurações definitiva vem depois) ---

    fun remoteEndpoint(): String = settings.remoteEndpoint
    fun remoteModel(): String = settings.remoteModel
    fun hasRemoteKey(): Boolean = container.secrets.has(SecretStore.KEY_REMOTE_API)

    /** [apiKey] em branco mantém a chave atual. */
    fun saveRemoteConfig(endpoint: String, model: String, apiKey: String) {
        settings.remoteEndpoint = endpoint
        settings.remoteModel = model
        if (apiKey.isNotBlank()) container.secrets.put(SecretStore.KEY_REMOTE_API, apiKey.trim())
        container.models.refreshAsync()
    }

    fun clearRemoteConfig() {
        settings.remoteEndpoint = ""
        settings.remoteModel = ""
        container.secrets.put(SecretStore.KEY_REMOTE_API, null)
        container.models.refreshAsync()
    }

    fun setAnimationEnabled(enabled: Boolean) {
        settings.animationEnabled = enabled
        _animationEnabled.value = enabled
    }

    fun setVoiceEnabled(enabled: Boolean) {
        settings.voiceEnabled = enabled
        _voiceEnabled.value = enabled
        if (!enabled) voice.stop()
    }

    fun setVoicePitch(value: Float) {
        settings.voicePitch = value
        _voicePitch.value = value
        voice.configure(value, _voiceRate.value)
    }

    fun setVoiceRate(value: Float) {
        settings.voiceRate = value
        _voiceRate.value = value
        voice.configure(_voicePitch.value, value)
    }

    fun speakLast() {
        if (!_voiceEnabled.value) return
        val last = messages.value.lastOrNull { it.role == Roles.ASSISTANT && !it.streaming }?.text.orEmpty()
        if (last.isNotBlank()) voice.speak(last)
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            _error.value = null
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Qwen-model.gguf"
            val result = container.models.importLocalModel(uri, name)
            result.onSuccess { file ->
                _localModelName.value = file.name
            }.onFailure { t ->
                _error.value = "Não foi possível importar o modelo: ${t.message ?: "erro desconhecido"}"
            }
        }
    }

    override fun onCleared() {
        voice.shutdown()
        super.onCleared()
    }

    private companion object {
        const val STREAMING_ID = -1L
        const val HISTORY_MESSAGES = 40
    }
}
