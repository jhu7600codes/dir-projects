package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vanbank.core.model.CardStatus
import com.vanbank.core.model.CardType

/** A debit or credit card, always issued on VANBank's own DIR network (number prefix '8'). */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index(value = ["cardNumber"], unique = true)],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val cardNumber: String,
    val cvv: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cardholderName: String,
    val cardType: CardType,
    /** Only set for CREDIT cards; null for DEBIT. */
    val creditLimitMinor: Long? = null,
    /** Outstanding balance owed on a CREDIT card; null for DEBIT. Starts at 0. */
    val creditBalanceMinor: Long? = null,
    val status: CardStatus = CardStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
)
