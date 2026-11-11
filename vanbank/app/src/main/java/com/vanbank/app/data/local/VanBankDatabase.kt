package com.vanbank.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vanbank.app.data.local.dao.AccountDao
import com.vanbank.app.data.local.dao.AiRequestDao
import com.vanbank.app.data.local.dao.BillDao
import com.vanbank.app.data.local.dao.CardDao
import com.vanbank.app.data.local.dao.LoanDao
import com.vanbank.app.data.local.dao.TransactionDao
import com.vanbank.app.data.local.dao.UserDao
import com.vanbank.app.data.local.dao.VaultDao
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.app.data.local.entity.BillEntity
import com.vanbank.app.data.local.entity.CardEntity
import com.vanbank.app.data.local.entity.LoanEntity
import com.vanbank.app.data.local.entity.LoanInstallmentEntity
import com.vanbank.app.data.local.entity.SavingsVaultEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CardEntity::class,
        TransactionEntity::class,
        LoanEntity::class,
        LoanInstallmentEntity::class,
        SavingsVaultEntity::class,
        BillEntity::class,
        AiPaymentRequestEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class VanBankDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun vaultDao(): VaultDao
    abstract fun billDao(): BillDao
    abstract fun aiRequestDao(): AiRequestDao

    companion object {
        private const val DB_NAME = "vanbank.db"

        @Volatile private var instance: VanBankDatabase? = null

        fun getInstance(context: Context): VanBankDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VanBankDatabase::class.java,
                    DB_NAME,
                ).build().also { instance = it }
            }
    }
}
