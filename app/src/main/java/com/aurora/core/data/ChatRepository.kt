package com.aurora.core.data

import androidx.room.withTransaction
import com.aurora.core.database.AuroraDatabase
import com.aurora.core.database.ConversationEntity
import com.aurora.core.database.DEFAULT_CONVERSATION_TITLE
import com.aurora.core.database.MessageEntity
import com.aurora.core.database.Sources
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val db: AuroraDatabase) {

    fun observeConversations(): Flow<List<ConversationEntity>> = db.conversations().observeAll()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        db.messages().observe(conversationId)

    suspend fun conversationExists(id: Long): Boolean = db.conversations().getById(id) != null

    suspend fun createConversation(
        title: String = DEFAULT_CONVERSATION_TITLE,
        source: String = Sources.APP
    ): Long {
        val now = System.currentTimeMillis()
        return db.conversations().insert(
            ConversationEntity(title = title, createdAt = now, updatedAt = now, source = source)
        )
    }

    /** Insere a mensagem e atualiza a conversa; a primeira mensagem do usuário vira o título. */
    suspend fun addMessage(conversationId: Long, role: String, content: String): Long =
        db.withTransaction {
            val now = System.currentTimeMillis()
            val id = db.messages().insert(
                MessageEntity(conversationId = conversationId, role = role, content = content, timestamp = now)
            )
            val conversation = db.conversations().getById(conversationId)
            if (conversation != null && role == "user" && conversation.title == DEFAULT_CONVERSATION_TITLE) {
                db.conversations().rename(conversationId, titleFrom(content), now)
            } else {
                db.conversations().touch(conversationId, now)
            }
            id
        }

    /** Mais antigas primeiro, no máximo [limit] mensagens. */
    suspend fun recentHistory(conversationId: Long, limit: Int): List<MessageEntity> =
        db.messages().recent(conversationId, limit).asReversed()

    suspend fun renameConversation(id: Long, title: String) {
        val clean = title.trim()
        if (clean.isNotEmpty()) db.conversations().rename(id, clean.take(MAX_TITLE), System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: Long) = db.conversations().delete(id)

    suspend fun deleteMessage(id: Long) = db.messages().delete(id)

    private fun titleFrom(text: String): String {
        val line = text.trim().lineSequence().firstOrNull().orEmpty().trim()
        return if (line.length <= MAX_TITLE) line else line.take(MAX_TITLE - 1).trimEnd() + "…"
    }

    private companion object {
        const val MAX_TITLE = 48
    }
}
