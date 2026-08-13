package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiRequestDao {
    @Insert
    suspend fun insert(request: AiPaymentRequestEntity): Long

    @Update
    suspend fun update(request: AiPaymentRequestEntity)

    @Query("SELECT * FROM ai_payment_requests WHERE id = :id")
    suspend fun getById(id: Long): AiPaymentRequestEntity?

    @Query("SELECT * FROM ai_payment_requests WHERE userId = :userId ORDER BY requestedAt DESC")
    fun observeForUser(userId: Long): Flow<List<AiPaymentRequestEntity>>

    @Query("SELECT * FROM ai_payment_requests WHERE userId = :userId AND status = 'PENDING' ORDER BY requestedAt DESC")
    fun observePendingForUser(userId: Long): Flow<List<AiPaymentRequestEntity>>
}
