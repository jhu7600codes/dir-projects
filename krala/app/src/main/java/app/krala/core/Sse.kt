package app.krala.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Reads only choices[0].message.content (or choices[0].delta.content when streaming) from an
 * OpenAI style response. Every other field (reasoning, tool calls, usage, ...) is ignored.
 */
object ChoiceContent {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun fromResponse(body: String): String? = runCatching {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return null
        val first = firstChoice(root) ?: return null
        str((first["message"] as? JsonObject)?.get("content"))
    }.getOrNull()

    /** for one streamed chunk; null when the chunk carries no content */
    fun fromChunk(data: String): String? = runCatching {
        val root = json.parseToJsonElement(data) as? JsonObject ?: return null
        val first = firstChoice(root) ?: return null
        str((first["delta"] as? JsonObject)?.get("content"))
            ?: str((first["message"] as? JsonObject)?.get("content"))
    }.getOrNull()

    /** the error message in a json error body, if any */
    fun errorMessage(body: String): String? = runCatching {
        val root = json.parseToJsonElement(body)
        when (root) {
            is JsonObject -> {
                val err = root["error"]
                when (err) {
                    is JsonObject -> str(err["message"]) ?: err.toString()
                    is JsonPrimitive -> err.contentOrNull
                    else -> str(root["message"]) ?: str(root["detail"])
                }
            }
            is JsonArray -> (root.firstOrNull() as? JsonObject)?.let { o ->
                (o["error"] as? JsonObject)?.let { str(it["message"]) }
            }
            else -> null
        }
    }.getOrNull()

    private fun firstChoice(root: JsonObject): JsonObject? =
        (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject

    private fun str(e: JsonElement?): String? = when (e) {
        null, JsonNull -> null
        is JsonPrimitive -> if (e.isString) e.content else null
        else -> null
    }
}

/** Splits a server-sent-events stream into data payloads. Feed it one line at a time. */
class SseLineParser {
    private val data = StringBuilder()

    /** returns a completed event's data when [line] ends an event, else null */
    fun feed(line: String): String? {
        if (line.isEmpty()) return flush()
        if (line.startsWith(":")) return null
        val idx = line.indexOf(':')
        val field = if (idx < 0) line else line.substring(0, idx)
        var value = if (idx < 0) "" else line.substring(idx + 1)
        if (value.startsWith(" ")) value = value.substring(1)
        if (field == "data") {
            if (data.isNotEmpty()) data.append('\n')
            data.append(value)
            // most providers put one json object per data line; emit right away when it parses
            // as a full object so the stream doesn't depend on blank separator lines
            val s = data.toString().trim()
            if (s == "[DONE]" || (s.startsWith("{") && s.endsWith("}") && balanced(s))) return flush()
        }
        return null
    }

    fun flush(): String? {
        if (data.isEmpty()) return null
        val s = data.toString()
        data.clear()
        return s
    }

    private fun balanced(s: String): Boolean {
        var depth = 0
        var inStr = false
        var esc = false
        for (c in s) {
            if (inStr) {
                if (esc) esc = false
                else if (c == '\\') esc = true
                else if (c == '"') inStr = false
                continue
            }
            when (c) {
                '"' -> inStr = true
                '{', '[' -> depth++
                '}', ']' -> depth--
            }
        }
        return depth == 0 && !inStr
    }
}
