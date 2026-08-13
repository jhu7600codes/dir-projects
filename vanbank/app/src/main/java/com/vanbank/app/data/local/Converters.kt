package com.vanbank.app.data.local

import androidx.room.TypeConverter
import com.vanbank.core.model.AccountType
import com.vanbank.core.model.AiRequestStatus
import com.vanbank.core.model.BillFrequency
import com.vanbank.core.model.CardStatus
import com.vanbank.core.model.CardType
import com.vanbank.core.model.LoanStatus
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus

/** Every enum from :core is stored in Room as its plain name() -- readable in a DB inspector, and stable across reorderings. */
class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromCardType(v: CardType): String = v.name
    @TypeConverter fun toCardType(v: String): CardType = CardType.valueOf(v)

    @TypeConverter fun fromCardStatus(v: CardStatus): String = v.name
    @TypeConverter fun toCardStatus(v: String): CardStatus = CardStatus.valueOf(v)

    @TypeConverter fun fromTransactionCategory(v: TransactionCategory): String = v.name
    @TypeConverter fun toTransactionCategory(v: String): TransactionCategory = TransactionCategory.valueOf(v)

    @TypeConverter fun fromTransactionDirection(v: TransactionDirection): String = v.name
    @TypeConverter fun toTransactionDirection(v: String): TransactionDirection = TransactionDirection.valueOf(v)

    @TypeConverter fun fromTransactionStatus(v: TransactionStatus): String = v.name
    @TypeConverter fun toTransactionStatus(v: String): TransactionStatus = TransactionStatus.valueOf(v)

    @TypeConverter fun fromLoanStatus(v: LoanStatus): String = v.name
    @TypeConverter fun toLoanStatus(v: String): LoanStatus = LoanStatus.valueOf(v)

    @TypeConverter fun fromBillFrequency(v: BillFrequency): String = v.name
    @TypeConverter fun toBillFrequency(v: String): BillFrequency = BillFrequency.valueOf(v)

    @TypeConverter fun fromAiRequestStatus(v: AiRequestStatus): String = v.name
    @TypeConverter fun toAiRequestStatus(v: String): AiRequestStatus = AiRequestStatus.valueOf(v)
}
