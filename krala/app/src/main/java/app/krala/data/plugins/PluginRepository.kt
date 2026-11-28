package app.krala.data.plugins

import app.krala.core.Commands
import app.krala.core.Ids
import app.krala.core.PluginSpec
import app.krala.core.PluginText
import app.krala.data.db.PluginDao
import app.krala.data.db.PluginEntity
import app.krala.data.provider.ApiMessage
import app.krala.data.provider.ProviderRouter
import app.krala.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginRepository @Inject constructor(
    private val dao: PluginDao,
    private val router: ProviderRouter,
    private val settings: SettingsRepository,
) {
    val all: Flow<List<PluginEntity>> = dao.observeAll()

    suspend fun enabled(): List<PluginEntity> = dao.all().filter { it.enabled }
    suspend fun byName(name: String): PluginEntity? = dao.byName(name.lowercase())?.takeIf { it.enabled }

    fun spec(p: PluginEntity) = PluginSpec(p.name, p.description, p.instructions, p.web)

    /** turns a plain description ("turn my notes into a todo list") into a plugin, written by the model */
    suspend fun make(request: String): PluginSpec {
        val s = settings.current()
        val prompt = """
            make a plugin (a reusable slash command) for an ai chat app from this request: "$request"
            reply with only json: {"name": "short-command-name", "description": "one line: what it does and when to use it", "instructions": "clear instructions the ai follows every time it's used, in the request's language", "web": true or false}
            web is true only if it needs fresh info from the internet. name: 1 to 2 words, lowercase, no spaces (use -).
        """.trimIndent()
        val (reply, _) = router.complete(s.defaultProviderId, s.defaultModel, listOf(ApiMessage("user", prompt)))
        return PluginText.parseSpec(app.krala.core.Think.answer(reply)) ?: throw IllegalStateException("couldn't make a plugin from that, try saying it differently")
    }

    suspend fun save(spec: PluginSpec, id: String? = null): String {
        val now = System.currentTimeMillis()
        var name = Commands.slug(spec.name)
        if (name in Commands.builtIn) name += "-2"
        val existing = dao.byName(name)
        if (existing != null && existing.id != id) name += "-" + (now % 100)
        val pid = id ?: Ids.new()
        val old = id?.let { dao.all().firstOrNull { p -> p.id == it } }
        dao.upsert(PluginEntity(pid, name, spec.description.trim(), spec.instructions.trim(), spec.web, old?.enabled ?: true, old?.createdAt ?: now, now))
        return name
    }

    suspend fun setEnabled(p: PluginEntity, on: Boolean) = dao.upsert(p.copy(enabled = on, updatedAt = System.currentTimeMillis()))
    suspend fun delete(id: String) = dao.delete(id)
}
