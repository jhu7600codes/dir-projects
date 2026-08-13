package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vanbank.core.model.LoanStatus

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId")],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** Account the loan is disbursed into, and repayments are auto-deducted from. */
    val accountId: Long,
    val purpose: String,
    val principalMinor: Long,
    val annualRatePercent: Double,
    val termMonths: Int,
    val status: LoanStatus,
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
)
