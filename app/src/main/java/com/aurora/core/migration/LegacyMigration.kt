package com.aurora.core.migration

import androidx.room.withTransaction
import com.aurora.core.database.AuroraDatabase
import com.aurora.core.database.ConversationEntity
import com.aurora.core.database.MemoryEntity
import com.aurora.core.database.MessageEntity
import com.aurora.core.database.Roles
import com.aurora.core.database.Sources
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class LegacyImportResult(
    val memoriesImported: Int = 0,
    val memoriesAlreadyPresent: Int = 0,
    val messagesImported: Int = 0,
    val messagesAlreadyPresent: Int = 0,
    val filesFound: Int = 0,
    val warnings: List<String> = emptyList(),
    /** Preenchido só se a importação inteira falhou (nada foi gravado). */
    val fatalError: String? = null
) {
    fun summary(): String = buildString {
        if (fatalError != null) append("ERRO: ").append(fatalError)
        else append("memórias: +$memoriesImported (já existiam $memoriesAlreadyPresent); ")
            .append("mensagens: +$messagesImported (já existiam $messagesAlreadyPresent)")
        if (warnings.isNotEmpty()) append(" | avisos: ").append(warnings.joinToString("; "))
    }
}

/**
 * Importa memória e conversas da Aurora 2.0.
 *
 * Schema real (ver docs/LEGACY_SCHEMA.md):
 *  - memoria.json   : {"versao":1,"itens":[{"id","texto","tipo","criado_em","atualizado_em"}]}  (tempos em SEGUNDOS, float)
 *  - conversas.jsonl: uma linha por troca {"t":<segundos>,"user":"...","aurora":"..."}  -> vira DUAS mensagens
 *
 * Garantias:
 *  - IDEMPOTENTE: cada item tem chave única (legacyId / legacyKey) com índice UNIQUE e insert IGNORE;
 *    rodar de novo não duplica nada;
 *  - ATÔMICA: tudo em uma transação; falha de parsing do arquivo inteiro não grava nada;
 *  - linhas individuais inválidas de conversas.jsonl são puladas e relatadas, não derrubam a importação;
 *  - NÃO apaga nem altera os arquivos de origem;
 *  - NÃO importa credenciais (accounts.json / sessions.json).
 */
class LegacyMigration(
    private val db: AuroraDatabase,
    private val source: LegacySource
) {
    suspend fun run(): LegacyImportResult {
        val warnings = mutableListOf<String>()
        return try {
            val now = System.currentTimeMillis()
            var files = 0

            val memText = source.readText(MEMORY_FILE)
            val memories = if (memText != null) {
                files++
                parseMemories(memText, now, warnings)
            } else emptyList()

            val convText = source.readText(CONVERSATIONS_FILE)
            val messages = if (convText != null) {
                files++
                parseConversations(convText, now, warnings)
            } else emptyList()

            if (source.readText(CONVERSATIONS_JSON_FILE) != null) {
                warnings += "conversas.json encontrado mas NÃO importado (schema não verificado)"
            }

            var memNew = 0
            var msgNew = 0
            db.withTransaction {
                memNew = db.memories().insertIgnoreAll(memories).count { it != -1L }
                if (messages.isNotEmpty()) {
                    val existing = db.conversations().findBySource(Sources.LEGACY_2_0)
                    val conversationId = existing?.id ?: db.conversations().insert(
                        ConversationEntity(
                            title = LEGACY_TITLE,
                            createdAt = messages.minOf { it.timestamp },
                            updatedAt = messages.maxOf { it.timestamp },
                            source = Sources.LEGACY_2_0
                        )
                    )
                    msgNew = db.messages().insertAllIgnore(
                        messages.map {
                            MessageEntity(
                                conversationId = conversationId,
                                role = it.role,
                                content = it.content,
                                timestamp = it.timestamp,
                                legacyKey = it.legacyKey
                            )
                        }
                    ).count { it != -1L }
                }
            }

            LegacyImportResult(
                memoriesImported = memNew,
                memoriesAlreadyPresent = memories.size - memNew,
                messagesImported = msgNew,
                messagesAlreadyPresent = messages.size - msgNew,
                filesFound = files,
                warnings = warnings
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            LegacyImportResult(warnings = warnings, fatalError = t.message ?: t.javaClass.simpleName)
        }
    }

    private fun parseMemories(text: String, now: Long, warnings: MutableList<String>): List<MemoryEntity> {
        val trimmed = text.trim()
        val items: JSONArray = if (trimmed.startsWith("[")) JSONArray(trimmed)
        else JSONObject(trimmed).optJSONArray("itens") ?: JSONArray().also {
            warnings += "memoria.json sem campo 'itens'"
        }

        val out = mutableListOf<MemoryEntity>()
        var skipped = 0
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i)
            val texto = o?.optString("texto", "")?.trim().orEmpty()
            if (o == null || texto.isEmpty()) {
                skipped++
                continue
            }
            val id = o.optString("id", "").trim()
            out += MemoryEntity(
                legacyId = "mem:" + id.ifEmpty { sha1(texto).take(12) },
                content = texto,
                kind = o.optString("tipo", "geral").ifBlank { "geral" },
                source = Sources.LEGACY_2_0,
                createdAt = toMillis(o.optDouble("criado_em"), now),
                updatedAt = toMillis(o.optDouble("atualizado_em"), now)
            )
        }
        if (skipped > 0) warnings += "memoria.json: $skipped item(ns) vazio(s)/inválido(s) ignorado(s)"
        return out
    }

    private data class LegacyMessage(val role: String, val content: String, val timestamp: Long, val legacyKey: String)

    private fun parseConversations(text: String, now: Long, warnings: MutableList<String>): List<LegacyMessage> {
        val out = mutableListOf<LegacyMessage>()
        var invalid = 0
        text.lineSequence().forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            val o = try { JSONObject(line) } catch (e: Exception) { null }
            if (o == null) {
                invalid++
                return@forEachIndexed
            }
            val tRaw = if (o.has("t")) o.opt("t").toString() else "linha$index"
            val ts = toMillis(o.optDouble("t"), now + index)
            val user = o.optString("user", "").trim()
            val aurora = o.optString("aurora", "").trim()
            if (user.isNotEmpty()) out += LegacyMessage(Roles.USER, user, ts, "conv:$tRaw:u")
            // +1 ms garante a ordem pergunta -> resposta
            if (aurora.isNotEmpty()) out += LegacyMessage(Roles.ASSISTANT, aurora, ts + 1, "conv:$tRaw:a")
        }
        if (invalid > 0) warnings += "conversas.jsonl: $invalid linha(s) inválida(s) ignorada(s)"
        return out
    }

    /** A 2.0 grava segundos (float, ex.: 1790724132.55); o Room da 5.0 usa milissegundos. */
    private fun toMillis(value: Double, fallback: Long): Long = when {
        value.isNaN() || value <= 0.0 -> fallback
        value < 1e11 -> (value * 1000.0).toLong()
        else -> value.toLong() // já está em ms
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MEMORY_FILE = "memoria.json"
        const val CONVERSATIONS_FILE = "conversas.jsonl"
        const val CONVERSATIONS_JSON_FILE = "conversas.json"
        const val LEGACY_TITLE = "Aurora 2.0 — histórico importado"
    }
}
