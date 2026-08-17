package com.androdrop.app.data

/** Mirrors src/lib/types.ts on the server — keep these in sync by hand. */

data class PairRequest(
    val deviceId: String? = null,
    val name: String? = null,
    val platform: String = "android",
)

data class PairResponse(
    val deviceId: String,
    val name: String,
    val platform: String,
    val pairCode: String,
    val createdAt: String,
)

data class ResolveCodeResponse(
    val deviceId: String,
    val name: String,
    val platform: String,
)

data class CreateTransferResponse(
    val transferId: String,
    val status: String,
    val expiresAt: String,
)

data class TransferFileView(
    val name: String,
    val size: Long,
    val type: String,
    val url: String? = null,
)

data class TransferDetailResponse(
    val id: String,
    val status: String,
    val senderName: String,
    val senderDeviceId: String,
    val targetName: String,
    val targetDeviceId: String,
    val files: List<TransferFileView>,
    val createdAt: String,
    val respondedAt: String?,
    val expiresAt: String,
)

data class RespondRequest(
    val deviceId: String,
    val action: String, // "accept" | "decline"
)

data class RespondResponse(
    val id: String,
    val status: String,
    val files: List<TransferFileView>? = null,
)

data class IncomingTransferSummary(
    val id: String,
    val senderName: String,
    val files: List<TransferFileSummary>,
    val createdAt: String,
)

data class TransferFileSummary(
    val name: String,
    val size: Long,
    val type: String,
)

data class ApiErrorBody(
    val error: String? = null,
)
