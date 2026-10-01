package com.aurora.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE source = :source ORDER BY id LIMIT 1")
    suspend fun findBySource(source: String): ConversationEntity?

    @Query("UPDATE conversations SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, title: String, now: Long)

    @Query("UPDATE conversations SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity): Long

    /** Retorna -1 para cada linha ignorada por conflito de [MessageEntity.legacyKey]. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(messages: List<MessageEntity>): List<Long>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observe(conversationId: Long): Flow<List<MessageEntity>>

    /** Mais recentes primeiro. */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun recent(conversationId: Long, limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)
}

@Dao
interface MemoryDao {
    /** Retorna -1 para cada linha ignorada por conflito de [MemoryEntity.legacyId]. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(memories: List<MemoryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(memory: MemoryEntity): Long

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    suspend fun all(): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: Long)
}

/** AINDA NÃO USADO (ver [TaskEntity]). */
@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Query("SELECT * FROM scheduled_tasks WHERE status = 'PENDING' ORDER BY triggerTimeMs")
    suspend fun pending(): List<TaskEntity>

    @Query("UPDATE scheduled_tasks SET status = :status WHERE taskId = :id")
    suspend fun setStatus(id: String, status: String)
}
