package com.aurora.core.agent

import com.aurora.core.database.MemoryDao
import com.aurora.core.database.MemoryEntity
import java.text.Normalizer

/**
 * Recuperação de memórias por sobreposição léxica (sem embeddings, sem rede).
 *
 *  - memórias de tipo "perfil" são sempre elegíveis (são poucas e definem quem é o usuário);
 *  - as demais entram só se compartilham palavras com a mensagem atual;
 *  - respeita limites de itens e de caracteres para caber no contexto do modelo.
 *
 * LIMITAÇÃO CONHECIDA: é correspondência por palavras/prefixos. Busca semântica (embeddings) é trabalho futuro.
 */
class MemoryRetriever(private val dao: MemoryDao) {

    suspend fun relevant(query: String, maxItems: Int = 12, maxChars: Int = 1800): List<MemoryEntity> {
        val queryTokens = tokens(query)
        val scored = dao.all().mapNotNull { memory ->
            val isProfile = memory.kind.equals("perfil", ignoreCase = true)
            val overlap = overlap(queryTokens, tokens(memory.content))
            if (!isProfile && overlap == 0) null
            else Triple(memory, overlap, if (isProfile) 1 else 0)
        }.sortedWith(
            compareByDescending<Triple<MemoryEntity, Int, Int>> { it.second }
                .thenByDescending { it.third }
                .thenByDescending { it.first.updatedAt }
        )

        val result = mutableListOf<MemoryEntity>()
        var used = 0
        for ((memory, _, _) in scored) {
            if (result.size >= maxItems) break
            if (used + memory.content.length > maxChars) continue
            result += memory
            used += memory.content.length
        }
        return result
    }

    private fun overlap(query: Set<String>, content: Set<String>): Int =
        query.count { q ->
            val stem = if (q.length > 5) q.take(5) else q
            content.any { it == q || (stem.length >= 5 && it.startsWith(stem)) }
        }

    private fun tokens(text: String): Set<String> =
        normalize(text)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOPWORDS }
            .toSet()

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

    private companion object {
        val STOPWORDS = setOf(
            "que", "para", "com", "uma", "uns", "umas", "por", "mais", "como", "mas", "dos", "das",
            "nos", "nas", "seu", "sua", "seus", "suas", "ele", "ela", "eles", "elas", "voce", "voces",
            "isso", "esse", "essa", "este", "esta", "aqui", "tem", "ter", "foi", "sao", "ser", "esta",
            "sobre", "quando", "onde", "qual", "quais", "pode", "posso", "preciso", "quero", "aurora",
            "ola", "oi", "meu", "minha", "meus", "minhas", "mim", "algo", "tudo", "bem"
        )
    }
}
