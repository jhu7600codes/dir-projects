package app.krala.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteConversation(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("title_auto") val titleAuto: Boolean,
    val pinned: Boolean,
    @SerialName("provider_id") val providerId: String? = null,
    val model: String? = null,
    @SerialName("system_prompt") val systemPrompt: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("last_message_at") val lastMessageAt: Long,
    val deleted: Boolean,
)

@Serializable
data class RemoteMessage(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("conversation_id") val conversationId: String,
    val role: String,
    val content: String,
    @SerialName("hidden_prefix") val hiddenPrefix: String? = null,
    val attachments: String = "[]",
    val status: String,
    val error: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
    val model: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val deleted: Boolean,
    val sources: String? = null,
)

@Serializable
data class RemoteMemory(
    val id: String,
    @SerialName("user_id") val userId: String,
    val kind: String,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("chat_name") val chatName: String,
    val notes: String,
    @SerialName("archived_into") val archivedInto: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val deleted: Boolean,
)

@Serializable
data class RemoteProvider(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("base_url") val baseUrl: String,
    val models: String,
    @SerialName("default_model") val defaultModel: String? = null,
    @SerialName("context_size") val contextSize: Int,
    val priority: Int,
    val enabled: Boolean,
    @SerialName("preset_id") val presetId: String? = null,
    @SerialName("supports_vision") val supportsVision: Boolean,
    /** api key + headers, encrypted on the device with the sync passphrase. null = keys not synced */
    @SerialName("key_cipher") val keyCipher: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val deleted: Boolean,
)

@Serializable
data class RemoteSettings(
    @SerialName("user_id") val userId: String,
    val data: String,
    @SerialName("updated_at") val updatedAt: Long,
)
