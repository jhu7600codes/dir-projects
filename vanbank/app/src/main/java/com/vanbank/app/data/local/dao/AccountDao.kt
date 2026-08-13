package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE accountNumber = :accountNumber")
    suspend fun getByAccountNumber(accountNumber: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAt ASC")
    fun observeForUser(userId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getForUser(userId: Long): List<AccountEntity>

    @Query("UPDATE accounts SET balanceMinor = balanceMinor + :deltaMinor WHERE id = :accountId")
    suspend fun adjustBalance(accountId: Long, deltaMinor: Long)

    @Query("UPDATE accounts SET balanceMinor = :balanceMinor WHERE id = :accountId")
    suspend fun setBalance(accountId: Long, balanceMinor: Long)

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>
}
