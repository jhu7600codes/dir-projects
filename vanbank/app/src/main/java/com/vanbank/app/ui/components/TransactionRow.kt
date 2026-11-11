package com.vanbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.app.data.repository.label
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbPanelElevated
import com.vanbank.app.ui.theme.VbPositive
import com.vanbank.app.ui.theme.VbTextMuted
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbWarning
import com.vanbank.core.model.Money
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

@Composable
fun TransactionRow(transaction: TransactionEntity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).background(VbPanelElevated, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(categoryEmoji(transaction.category), fontSize = 16.sp)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    transaction.description,
                    color = VbTextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Row {
                    Text(
                        text = Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault()).format(timeFormatter),
                        color = VbTextMuted,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "  •  ${transaction.category.label()}",
                        color = VbTextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.formatSigned(transaction.amountMinor, transaction.direction),
                style = VbNumeralStyles.amountMedium,
                color = when {
                    transaction.status == TransactionStatus.DECLINED -> VbTextMuted
                    transaction.direction == TransactionDirection.IN -> VbPositive
                    else -> VbTextPrimary
                },
            )
            if (transaction.status != TransactionStatus.COMPLETED) {
                Text(
                    text = transaction.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (transaction.status == TransactionStatus.DECLINED) VbNegative else VbWarning,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private fun categoryEmoji(category: TransactionCategory): String = when (category) {
    TransactionCategory.GROCERIES -> "🛒"
    TransactionCategory.INCOME -> "💰"
    TransactionCategory.SUBSCRIPTIONS -> "🔁"
    TransactionCategory.DINING -> "🍽️"
    TransactionCategory.TRANSPORT -> "🚕"
    TransactionCategory.SHOPPING -> "🛍️"
    TransactionCategory.UTILITIES -> "💡"
    TransactionCategory.ENTERTAINMENT -> "🎬"
    TransactionCategory.RENT_MORTGAGE -> "🏠"
    TransactionCategory.TRANSFER -> "↔️"
    TransactionCategory.LOAN -> "🏦"
    TransactionCategory.SAVINGS -> "🎯"
    TransactionCategory.AI_SERVICES -> "🤖"
    TransactionCategory.FEES -> "⚠️"
    TransactionCategory.OTHER -> "•"
}
