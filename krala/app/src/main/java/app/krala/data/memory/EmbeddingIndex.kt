package app.krala.data.memory

import app.krala.data.db.ConversationDao
import app.krala.data.db.EmbeddingDao
import app.krala.data.db.EmbeddingEntity
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageHit
import app.krala.data.provider.OpenAiClient
import app.krala.data.provider.ProviderRepository
import app.krala.data.settings.SettingsRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Optional semantic recall. When turned on, finished messages are embedded with the chosen
 * provider's /embeddings endpoint and kept locally; recall then also looks at the nearest ones.
 * Brute force cosine is plenty for a personal chat history.
 */
@Singleton
class EmbeddingIndex @Inject constructor(
    private val dao: EmbeddingDao,
    private val messages: MessageDao,
    private val conversations: ConversationDao,
    private val providers: ProviderRepository,
    private val client: OpenAiClient,
    private val settings: SettingsRepository,
) {
    private suspend fun target(): Pair<app.krala.data.provider.ProviderConfig, String>? {
        val s = settings.current()
        if (!s.embeddingsEnabled) return null
        val model = s.embeddingModel?.takeIf { it.isNotBlank() } ?: return null
        val p = s.embeddingProviderId?.let { providers.get(it) } ?: return null
        return providers.config(p) to model
    }

    /** embeds up to [max] messages that have no vector yet. returns how many were added. */
    suspend fun indexPending(max: Int = 256): Int {
        val (cfg, model) = target() ?: return 0
        var added = 0
        while (added < max) {
            val batch = messages.withoutEmbedding(model, 32).filter { it.content.isNotBlank() }
            if (batch.isEmpty()) break
            val vectors = client.embed(cfg, model, batch.map { it.content.take(4_000) })
            if (vectors.size != batch.size) break
            val now = System.currentTimeMillis()
            dao.upsertAll(batch.zip(vectors).map { (m, v) ->
                EmbeddingEntity(m.id, m.conversationId, model, v.size, encode(normalize(v)), now)
            })
            added += batch.size
        }
        return added
    }

    suspend fun nearest(text: String, excludeConversation: String?, k: Int, minScore: Float = 0.45f): List<MessageHit> {
        val (cfg, model) = target() ?: return emptyList()
        val q = normalize(client.embed(cfg, model, listOf(text.take(4_000))).firstOrNull() ?: return emptyList())
        val scored = dao.allFor(model)
            .filter { it.conversationId != excludeConversation && it.dims == q.size }
            .map { it to dot(q, decode(it.vector)) }
            .filter { it.second >= minScore }
            .sortedByDescending { it.second }
            .take(k)
        return scored.mapNotNull { (e, score) ->
            val m = messages.get(e.messageId)?.takeIf { !it.deleted } ?: return@mapNotNull null
            val c = conversations.get(m.conversationId)?.takeIf { !it.deleted && !it.incognito } ?: return@mapNotNull null
            MessageHit(m.id, c.id, c.title, m.role, m.content.take(280), m.content, m.createdAt, score.toDouble())
        }
    }

    suspend fun clear() = dao.clear()

    private fun normalize(v: FloatArray): FloatArray {
        var n = 0f
        for (x in v) n += x * x
        val len = sqrt(n).takeIf { it > 0f } ?: 1f
        return FloatArray(v.size) { v[it] / len }
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in a.indices) s += a[i] * b[i]
        return s
    }

    private fun encode(v: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        v.forEach { buf.putFloat(it) }
        return buf.array()
    }

    private fun decode(b: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(b.size / 4) { buf.getFloat() }
    }
}
