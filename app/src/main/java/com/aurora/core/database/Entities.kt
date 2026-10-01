package com.aurora.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object Roles {
    const val USER = "user"
    const val ASSISTANT = "assistant"
}

object Sources {
    const val APP = "app"
    const val LEGACY_2_0 = "legacy_2_0"
    const val V1_MIGRATED = "v1_migrated"
}

const val DEFAULT_CONVERSATION_TITLE = "Nova conversa"

/** Uma conversa (thread). Mensagens ficam em [MessageEntity]; apagar a conversa apaga as mensagens (CASCADE). */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val source: String
)

/**
 * Uma mensagem. [legacyKey] só é preenchida em dados importados da Aurora 2.0
 * e tem índice UNIQUE: é o que torna a importação idempotente.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["legacyKey"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
    val legacyKey: String? = null
)

/** Memória de longo prazo. [legacyId] (UNIQUE) evita duplicar memórias importadas da 2.0. */
@Entity(
    tableName = "memories",
    indices = [Index(value = ["legacyId"], unique = true)]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val legacyId: String? = null,
    val content: String,
    val kind: String,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** AINDA NÃO LIGADO: tarefas agendadas serão implementadas em etapa futura (nenhum código as lê/escreve). */
@Entity(tableName = "scheduled_tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val triggerTimeMs: Long,
    val isRecurring: Boolean,
    val intervalMs: Long = 0,
    val status: String
)
