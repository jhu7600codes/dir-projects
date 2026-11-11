package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vanbank.core.model.BillFrequency
import com.vanbank.core.model.TransactionCategory

/** A recurring fake bill that auto-deducts on schedule via [com.vanbank.app.work.BillPayWorker]. */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("nextDueAtEpochMillis")],
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val accountId: Long,
    val name: String,
    val category: TransactionCategory,
    val amountMinor: Long,
    val frequency: BillFrequency,
    val nextDueAtEpochMillis: Long,
    val isActive: Boolean = true,
    val lastPaidAt: Long? = null,
    val lastPaymentFailed: Boolean = false,
)
