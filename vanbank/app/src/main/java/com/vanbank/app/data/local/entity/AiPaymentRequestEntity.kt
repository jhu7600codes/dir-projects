package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vanbank.core.model.AiRequestStatus

/**
 * A legitimate-looking payment request from the DIR AI Assistant for a
 * completed task/service -- an invoice/approval flow, not a random prompt.
 * The user approves (deducts balance, logs a transaction) or declines (no
 * deduction, logged as declined).
 */
@Entity(
    tableName = "ai_payment_requests",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("userId")],
)
data class AiPaymentRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val accountId: Long,
    /** Short line item, e.g. "Research summary compilation". */
    val title: String,
    /** Longer invoice detail, e.g. "3-source competitive analysis, delivered 14:02". */
    val detail: String,
    val amountMinor: Long,
    val status: AiRequestStatus = AiRequestStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
)
