package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("timestamp")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val cardId: Long? = null,
    val category: TransactionCategory,
    val description: String,
    val amountMinor: Long,
    val direction: TransactionDirection,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val counterparty: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
