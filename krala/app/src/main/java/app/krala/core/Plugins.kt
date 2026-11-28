package app.krala.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** slash commands and plugins: parsing only, pure and tested */
object Commands {
    val builtIn = listOf("help", "web", "search", "new", "incognito", "talk", "model", "prompt", "rename", "export", "plugin", "plugins", "recall")

    /** "/todo buy milk" → ("todo", "buy milk"); null when the text isn't a command */
    fun parse(text: String): Pair<String, String>? {
        val m = Regex("""^/([\p{L}\p{N}_-]{1,32})(?:\s+([\s\S]*))?$""").find(text.trim()) ?: return null
        return m.groupValues[1].lowercase() to m.groupValues[2].trim()
    }

    fun slug(raw: String): String =
        raw.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), "-").trim('-').take(24).ifBlank { "plugin" }
}

data class PluginSpec(val name: String, val description: String, val instructions: String, val web: Boolean)

object PluginText {
    /** reads the json the model wrote for /plugin, tolerating code fences and chatter */
    fun parseSpec(reply: String): PluginSpec? {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val o = runCatching { Json.parseToJsonElement(reply.substring(start, end + 1)) as JsonObject }.getOrNull() ?: return null
        fun str(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        val name = Commands.slug(str("name"))
        val instructions = str("instructions")
        if (instructions.isBlank()) return null
        return PluginSpec(
            name = if (name in Commands.builtIn) "$name-2" else name,
            description = str("description").ifBlank { instructions.take(80) },
            instructions = instructions,
            web = (o["web"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }

    /** krala can call a plugin herself with <plugin name>input</plugin> */
    private val tag = Regex("""<\s*plugin\s+([\p{L}\p{N}_-]+)\s*>(.*?)(<\s*/\s*plugin\s*>|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun findCall(text: String): Pair<String, String>? = tag.find(text)?.let { it.groupValues[1].lowercase() to it.groupValues[2].trim() }

    fun stripCall(text: String): String {
        var t = tag.replace(text, "")
        // a half streamed "<plu" at the end
        val open = "<plugin"
        for (n in open.length downTo 2) if (t.lowercase().endsWith(open.take(n))) { t = t.dropLast(n); break }
        return t.trim()
    }

    fun listSection(plugins: List<PluginSpec>): String = buildString {
        append("plugins you can use. to use one, reply with only <plugin name>what to do it on</plugin> and nothing else; the app runs it and asks you again. the user can also type /name.\n")
        plugins.forEach { append("/").append(it.name).append(": ").append(it.description).append('\n') }
    }.trim()

    fun runSection(p: PluginSpec, input: String): String =
        "use the plugin /${p.name} now.\ninstructions: ${p.instructions.trim()}" + (if (input.isNotBlank()) "\ninput: $input" else "")
}
