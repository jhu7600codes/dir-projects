package com.vanbank.app.data.repository

import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val db: VanBankDatabase) {
    private val accountDao = db.accountDao()

    fun observeForUser(userId: Long): Flow<List<AccountEntity>> = accountDao.observeForUser(userId)

    fun observeById(accountId: Long): Flow<AccountEntity?> = accountDao.observeById(accountId)

    suspend fun getById(accountId: Long): AccountEntity? = accountDao.getById(accountId)

    suspend fun getByAccountNumber(accountNumber: String): AccountEntity? =
        accountDao.getByAccountNumber(accountNumber)

    suspend fun getForUser(userId: Long): List<AccountEntity> = accountDao.getForUser(userId)
}
