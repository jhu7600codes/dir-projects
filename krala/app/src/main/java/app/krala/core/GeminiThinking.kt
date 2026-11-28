package app.krala.core

/**
 * How much gemini thinks before answering. Gemini 2.5 takes a token budget, gemini 3+ a level.
 * "fast" keeps first-token time short; thought summaries are always asked for so they show in
 * the thinking panel.
 */
object GeminiThinking {
    fun version(model: String): Double? =
        Regex("""gemini-(\d+(?:\.\d+)?)""").find(model.lowercase())?.groupValues?.get(1)?.toDoubleOrNull()

    /** fields for google.thinking_config, or null when the model has no thinking to tune */
    fun config(model: String, level: String): Map<String, Any>? {
        val v = version(model) ?: return null
        if (v < 2.5) return null
        val out = linkedMapOf<String, Any>("include_thoughts" to true)
        if (v >= 3.0) {
            when (level) {
                "fast" -> out["thinking_level"] = "low"
                "deep" -> out["thinking_level"] = "high"
            }
        } else {
            val pro = "pro" in model.lowercase()
            out["thinking_budget"] = when (level) {
                "fast" -> if (pro) 128 else 512
                "deep" -> -1
                else -> 4096
            }
        }
        return out
    }
}
