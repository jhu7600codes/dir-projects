package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observeById(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE cardNumber = :cardNumber")
    suspend fun getByCardNumber(cardNumber: String): CardEntity?

    @Query("SELECT * FROM cards WHERE accountId = :accountId ORDER BY createdAt ASC")
    fun observeForAccount(accountId: Long): Flow<List<CardEntity>>

    /** All cards across every account the given user owns, newest first -- for the home carousel. */
    @Query(
        """
        SELECT cards.* FROM cards
        INNER JOIN accounts ON accounts.id = cards.accountId
        WHERE accounts.userId = :userId
        ORDER BY cards.createdAt ASC
        """,
    )
    fun observeForUser(userId: Long): Flow<List<CardEntity>>

    @Query("UPDATE cards SET status = :status WHERE id = :cardId")
    suspend fun setStatus(cardId: Long, status: com.vanbank.core.model.CardStatus)

    @Query("UPDATE cards SET creditBalanceMinor = creditBalanceMinor + :deltaMinor WHERE id = :cardId")
    suspend fun adjustCreditBalance(cardId: Long, deltaMinor: Long)
}
