package com.vanbank.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A named savings goal ("Emergency fund", "Trip to Sochi") with a target and running progress. */
@Entity(
    tableName = "savings_vaults",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class SavingsVaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The savings account this vault's funds are held in/drawn from. */
    val accountId: Long,
    val name: String,
    val emoji: String = "🎯",
    val targetAmountMinor: Long,
    val currentAmountMinor: Long = 0,
    val targetDateEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
