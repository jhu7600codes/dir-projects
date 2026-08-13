package com.vanbank.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vanbank.app.data.local.entity.LoanEntity
import com.vanbank.app.data.local.entity.LoanInstallmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeLoanById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE userId = :userId ORDER BY requestedAt DESC")
    fun observeLoansForUser(userId: Long): Flow<List<LoanEntity>>

    @Insert
    suspend fun insertInstallments(installments: List<LoanInstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: LoanInstallmentEntity)

    @Query("SELECT * FROM loan_installments WHERE loanId = :loanId ORDER BY number ASC")
    fun observeInstallments(loanId: Long): Flow<List<LoanInstallmentEntity>>

    @Query("SELECT * FROM loan_installments WHERE loanId = :loanId AND isPaid = 0 ORDER BY number ASC LIMIT 1")
    suspend fun nextUnpaidInstallment(loanId: Long): LoanInstallmentEntity?
}
