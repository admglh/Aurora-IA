package com.aurora.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2.
 *
 * v1 tinha: memories(key,value,category,timestamp), conversations(role,content,timestamp) [na verdade MENSAGENS],
 * accounts(apiKey em texto puro) e scheduled_tasks.
 *
 * v2: conversations (threads) + messages + memories (nova) ; accounts REMOVIDA (chaves agora ficam no Keystore).
 * scheduled_tasks não muda.
 *
 * Dados preservados:
 *  - mensagens da v1 vão para uma conversa "Conversa anterior (v1)", EXCETO a resposta fixa fake
 *    ("Aurora 5.0 está instalada...") que nunca foi uma resposta real;
 *  - memórias da v1 (se houver) são copiadas.
 *
 * O DDL abaixo precisa bater EXATAMENTE com as entidades (Room valida na abertura).
 * Os statements ficam entre os marcadores SQL-BEGIN/SQL-END porque são testados por script externo.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (sql in MIGRATION_1_2_SQL) db.execSQL(sql)
    }
}

// SQL-BEGIN
private val MIGRATION_1_2_SQL = listOf(
    "ALTER TABLE `conversations` RENAME TO `v1_messages`",
    "ALTER TABLE `memories` RENAME TO `v1_memories`",
    "CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `source` TEXT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `legacyKey` TEXT, FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_legacyKey` ON `messages` (`legacyKey`)",
    "CREATE TABLE IF NOT EXISTS `memories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `legacyId` TEXT, `content` TEXT NOT NULL, `kind` TEXT NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_memories_legacyId` ON `memories` (`legacyId`)",
    "INSERT INTO `conversations` (`title`, `createdAt`, `updatedAt`, `source`) SELECT 'Conversa anterior (v1)', MIN(`timestamp`), MAX(`timestamp`), 'v1_migrated' FROM `v1_messages` WHERE NOT (`role` = 'assistant' AND `content` LIKE 'Aurora 5.0 est% instalada. O pr%') HAVING COUNT(*) > 0",
    "INSERT INTO `messages` (`conversationId`, `role`, `content`, `timestamp`, `legacyKey`) SELECT (SELECT `id` FROM `conversations` WHERE `source` = 'v1_migrated' LIMIT 1), `role`, `content`, `timestamp`, NULL FROM `v1_messages` WHERE NOT (`role` = 'assistant' AND `content` LIKE 'Aurora 5.0 est% instalada. O pr%') ORDER BY `timestamp` ASC, `id` ASC",
    "INSERT INTO `memories` (`legacyId`, `content`, `kind`, `source`, `createdAt`, `updatedAt`) SELECT NULL, CASE WHEN `key` LIKE 'legacy_%' THEN `value` ELSE `key` || ': ' || `value` END, `category`, 'v1', `timestamp`, `timestamp` FROM `v1_memories`",
    "DROP TABLE `v1_messages`",
    "DROP TABLE `v1_memories`",
    "DROP TABLE IF EXISTS `accounts`"
)
// SQL-END
