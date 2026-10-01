package com.aurora.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        TaskEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun conversations(): ConversationDao
    abstract fun messages(): MessageDao
    abstract fun memories(): MemoryDao
    abstract fun tasks(): TaskDao

    companion object {
        @Volatile
        private var instance: AuroraDatabase? = null

        fun get(context: Context): AuroraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuroraDatabase::class.java,
                    "aurora_v5.db"
                )
                    // SEM fallbackToDestructiveMigration: toda mudança de schema exige Migration explícita.
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
