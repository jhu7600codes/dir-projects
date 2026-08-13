package com.vanbank.app.ui.billpay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.data.local.entity.BillEntity
import com.vanbank.app.data.repository.label
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.components.VbTextField
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbPanel as VbPanelColor
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.BillFrequency
import com.vanbank.core.model.Money
import com.vanbank.core.model.TransactionCategory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dueDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneOffset.UTC)

@Composable
fun BillPayScreen(viewModel: BillPayViewModel, onBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val bills by viewModel.bills.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(VbBackground).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VbTextPrimary)
                    }
                    Text("Bill pay", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add bill", tint = VbAccent)
                }
            }
        }

        if (bills.isEmpty()) {
            item { EmptyState(title = "No recurring bills", subtitle = "Tap + to schedule an auto-pay bill.") }
        } else {
            items(bills, key = { it.id }) { bill ->
                BillRow(
                    bill = bill,
                    onToggleActive = { viewModel.setActive(bill, it) },
                    onDelete = { viewModel.delete(bill) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showAddDialog) {
        AddBillDialog(
            accountIds = accounts.map { it.id to it.nickname },
            onDismiss = { showAddDialog = false },
            onCreate = { accountId, name, category, amountMinor, frequency ->
                val firstDue = Instant.now().toEpochMilli()
                viewModel.createBill(accountId, name, category, amountMinor, frequency, firstDue) {}
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun BillRow(bill: BillEntity, onToggleActive: (Boolean) -> Unit, onDelete: () -> Unit) {
    VbPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bill.name, color = VbTextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    "${bill.category.label()} · ${bill.frequency.name.lowercase()}",
                    color = VbTextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    "Next: ${dueDateFormatter.format(Instant.ofEpochMilli(bill.nextDueAtEpochMillis))}",
                    color = if (bill.lastPaymentFailed) VbNegative else VbTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(bill.amountMinor), style = VbNumeralStyles.amountMedium, color = VbTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = bill.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(checkedTrackColor = VbAccent),
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete bill", tint = VbNegative)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBillDialog(
    accountIds: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onCreate: (accountId: Long, name: String, category: TransactionCategory, amountMinor: Long, frequency: BillFrequency) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TransactionCategory.UTILITIES) }
    var frequency by remember { mutableStateOf(BillFrequency.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VbPanelColor,
        title = { Text("New recurring bill", color = VbTextPrimary) },
        text = {
            Column {
                VbTextField(value = name, onValueChange = { name = it }, label = "Bill name", modifier = Modifier.padding(bottom = 10.dp))
                VbTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = "Amount (₽)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Text("Frequency", color = VbTextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BillFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.name.lowercase().replaceFirstChar(Char::uppercase)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VbAccent,
                                selectedLabelColor = VbOnAccent,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val accountId = accountIds.firstOrNull()?.first ?: return@TextButton
                    val amountMinor = (amount.toLongOrNull() ?: 0L) * 100
                    if (name.isNotBlank() && amountMinor > 0) onCreate(accountId, name, category, amountMinor, frequency)
                },
            ) { Text("Create", color = VbAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = VbTextSecondary) } },
    )
}
