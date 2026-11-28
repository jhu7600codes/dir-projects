package app.krala.data.provider

data class Preset(
    val id: String,
    val name: String,
    val baseUrl: String,
    val models: List<String>,
    val contextSize: Int,
    val needsKey: Boolean = true,
    /** where to get a key, shown as plain text */
    val keyHint: String = "",
    val vision: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val fixedKey: String? = null,
)

object Presets {
    const val FREE_ID = "krala-free"
    const val ACCOUNT_PLACEHOLDER = "{account_id}"

    val free = Preset(
        id = FREE_ID,
        name = "krala free",
        baseUrl = "https://api.apisfreellm.com/v1",
        models = listOf("free-ai"),
        contextSize = 32_000,
        needsKey = false,
        fixedKey = "free",
    )

    val all: List<Preset> = listOf(
        free,
        Preset(
            id = "gemini",
            name = "google ai studio",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            models = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite"),
            contextSize = 1_000_000,
            keyHint = "aistudio.google.com → get api key",
            vision = true,
        ),
        Preset(
            id = "groq",
            name = "groq",
            baseUrl = "https://api.groq.com/openai/v1",
            models = listOf("llama-3.3-70b-versatile", "meta-llama/llama-4-scout-17b-16e-instruct", "openai/gpt-oss-120b", "qwen/qwen3-32b"),
            contextSize = 131_072,
            keyHint = "console.groq.com → api keys",
        ),
        Preset(
            id = "cerebras",
            name = "cerebras",
            baseUrl = "https://api.cerebras.ai/v1",
            models = listOf("llama-3.3-70b", "gpt-oss-120b", "qwen-3-32b"),
            contextSize = 65_536,
            keyHint = "cloud.cerebras.ai → api keys",
        ),
        Preset(
            id = "mistral",
            name = "mistral",
            baseUrl = "https://api.mistral.ai/v1",
            models = listOf("mistral-large-latest", "mistral-small-latest", "pixtral-large-latest"),
            contextSize = 128_000,
            keyHint = "console.mistral.ai → api keys",
            vision = true,
        ),
        Preset(
            id = "openrouter",
            name = "openrouter",
            baseUrl = "https://openrouter.ai/api/v1",
            models = listOf("openrouter/auto", "meta-llama/llama-3.3-70b-instruct:free", "deepseek/deepseek-chat-v3-0324:free"),
            contextSize = 128_000,
            keyHint = "openrouter.ai → keys",
            headers = mapOf("X-Title" to "krala"),
        ),
        Preset(
            id = "cloudflare",
            name = "cloudflare workers ai",
            baseUrl = "https://api.cloudflare.com/client/v4/accounts/$ACCOUNT_PLACEHOLDER/ai/v1",
            models = listOf("@cf/meta/llama-3.3-70b-instruct-fp8-fast", "@cf/meta/llama-4-scout-17b-16e-instruct"),
            contextSize = 24_000,
            keyHint = "dash.cloudflare.com → ai → workers ai → use rest api (needs your account id too)",
        ),
        Preset(
            id = "nvidia",
            name = "nvidia",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            models = listOf("meta/llama-3.3-70b-instruct", "deepseek-ai/deepseek-r1", "nvidia/llama-3.1-nemotron-ultra-253b-v1"),
            contextSize = 128_000,
            keyHint = "build.nvidia.com → get api key",
        ),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }
}
