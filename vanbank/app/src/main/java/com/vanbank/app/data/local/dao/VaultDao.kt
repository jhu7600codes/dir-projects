package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.SavingsVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Insert
    suspend fun insert(vault: SavingsVaultEntity): Long

    @Update
    suspend fun update(vault: SavingsVaultEntity)

    @Delete
    suspend fun delete(vault: SavingsVaultEntity)

    @Query("SELECT * FROM savings_vaults WHERE id = :id")
    suspend fun getById(id: Long): SavingsVaultEntity?

    @Query("SELECT * FROM savings_vaults WHERE id = :id")
    fun observeById(id: Long): Flow<SavingsVaultEntity?>

    @Query("SELECT * FROM savings_vaults WHERE accountId = :accountId ORDER BY createdAt ASC")
    fun observeForAccount(accountId: Long): Flow<List<SavingsVaultEntity>>

    @Query(
        """
        SELECT savings_vaults.* FROM savings_vaults
        INNER JOIN accounts ON accounts.id = savings_vaults.accountId
        WHERE accounts.userId = :userId
        ORDER BY savings_vaults.createdAt ASC
        """,
    )
    fun observeForUser(userId: Long): Flow<List<SavingsVaultEntity>>

    @Query("UPDATE savings_vaults SET currentAmountMinor = currentAmountMinor + :deltaMinor WHERE id = :vaultId")
    suspend fun adjustAmount(vaultId: Long, deltaMinor: Long)
}
