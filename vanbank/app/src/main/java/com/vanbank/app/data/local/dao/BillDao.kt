package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Delete
    suspend fun delete(bill: BillEntity)

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY nextDueAtEpochMillis ASC")
    fun observeForUser(userId: Long): Flow<List<BillEntity>>

    /** Every active bill due at or before [nowMillis], across all users -- what BillPayWorker sweeps. */
    @Query("SELECT * FROM bills WHERE isActive = 1 AND nextDueAtEpochMillis <= :nowMillis")
    suspend fun getDueBills(nowMillis: Long): List<BillEntity>
}
