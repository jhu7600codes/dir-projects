package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vanbank.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentForAccount(accountId: Long, limit: Int): Flow<List<TransactionEntity>>

    /** Every transaction across every account the user owns, newest first -- for the home feed. */
    @Query(
        """
        SELECT transactions.* FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.accountId
        WHERE accounts.userId = :userId
        ORDER BY transactions.timestamp DESC
        """,
    )
    fun observeForUser(userId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT transactions.* FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.accountId
        WHERE accounts.userId = :userId
        ORDER BY transactions.timestamp DESC
        LIMIT :limit
        """,
    )
    fun observeRecentForUser(userId: Long, limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT transactions.* FROM transactions
        INNER JOIN accounts ON accounts.id = transactions.accountId
        WHERE accounts.userId = :userId AND transactions.timestamp BETWEEN :fromMillis AND :toMillis
        ORDER BY transactions.timestamp DESC
        """,
    )
    suspend fun getForUserBetween(userId: Long, fromMillis: Long, toMillis: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun observeForCard(cardId: Long): Flow<List<TransactionEntity>>
}
