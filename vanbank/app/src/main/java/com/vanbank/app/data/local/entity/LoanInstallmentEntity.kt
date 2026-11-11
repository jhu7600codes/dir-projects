package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loan_installments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("loanId")],
)
data class LoanInstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val number: Int,
    /** Epoch day (java.time.LocalDate#toEpochDay) the installment is due. */
    val dueDateEpochDay: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val totalDueMinor: Long,
    val remainingBalanceMinor: Long,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
)
