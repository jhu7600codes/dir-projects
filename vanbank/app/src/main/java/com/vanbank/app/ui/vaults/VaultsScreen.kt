package com.vanbank.app.ui.vaults

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.data.local.entity.SavingsVaultEntity
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.components.VbPrimaryButton
import com.vanbank.app.ui.components.VbTextField
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbPanel as VbPanelColor
import com.vanbank.app.ui.theme.VbPanelBorder
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.Money

@Composable
fun VaultsScreen(viewModel: VaultsViewModel, onBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val vaults by viewModel.vaults.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var activeVault by remember { mutableStateOf<SavingsVaultEntity?>(null) }
    var activeMode by remember { mutableStateOf(VaultAction.CONTRIBUTE) }

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
                    Text("Savings vaults", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New vault", tint = VbAccent)
                }
            }
        }

        if (vaults.isEmpty()) {
            item {
                EmptyState(title = "No goals yet", subtitle = "Tap + to start a savings vault.")
            }
        } else {
            items(vaults, key = { it.id }) { vault ->
                VaultCard(
                    vault = vault,
                    onContribute = { activeVault = vault; activeMode = VaultAction.CONTRIBUTE },
                    onWithdraw = { activeVault = vault; activeMode = VaultAction.WITHDRAW },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreateVaultDialog(
            accountIds = accounts.map { it.id to it.nickname },
            onDismiss = { showCreateDialog = false },
            onCreate = { accountId, name, emoji, targetMinor ->
                viewModel.createVault(accountId, name, emoji, targetMinor) {}
                showCreateDialog = false
            },
        )
    }

    activeVault?.let { vault ->
        VaultActionDialog(
            vault = vault,
            mode = activeMode,
            accountIds = accounts.map { it.id to it.nickname },
            onDismiss = { activeVault = null },
            onConfirm = { accountId, amountMinor ->
                if (activeMode == VaultAction.CONTRIBUTE) {
                    viewModel.contribute(vault.id, accountId, amountMinor) {}
                } else {
                    viewModel.withdraw(vault.id, accountId, amountMinor) {}
                }
                activeVault = null
            },
        )
    }
}

private enum class VaultAction { CONTRIBUTE, WITHDRAW }

@Composable
private fun VaultCard(vault: SavingsVaultEntity, onContribute: () -> Unit, onWithdraw: () -> Unit) {
    VbPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(vault.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(vault.name, color = VbTextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    "${Money.format(vault.currentAmountMinor)} of ${Money.format(vault.targetAmountMinor)}",
                    color = VbTextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val progress = if (vault.targetAmountMinor > 0) {
            (vault.currentAmountMinor.toFloat() / vault.targetAmountMinor.toFloat()).coerceIn(0f, 1f)
        } else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = VbAccent,
            trackColor = VbPanelBorder,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onContribute) { Text("Add funds", color = VbAccent) }
            TextButton(onClick = onWithdraw) { Text("Withdraw", color = VbTextSecondary) }
        }
    }
}

@Composable
private fun CreateVaultDialog(
    accountIds: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onCreate: (accountId: Long, name: String, emoji: String, targetMinor: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VbPanelColor,
        title = { Text("New savings goal", color = VbTextPrimary) },
        text = {
            Column {
                VbTextField(value = emoji, onValueChange = { emoji = it.take(2) }, label = "Emoji", modifier = Modifier.padding(bottom = 10.dp))
                VbTextField(value = name, onValueChange = { name = it }, label = "Goal name", modifier = Modifier.padding(bottom = 10.dp))
                VbTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit) },
                    label = "Target amount (₽)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val accountId = accountIds.firstOrNull()?.first ?: return@TextButton
                    val targetMinor = (target.toLongOrNull() ?: 0L) * 100
                    if (name.isNotBlank() && targetMinor > 0) onCreate(accountId, name, emoji, targetMinor)
                },
            ) { Text("Create", color = VbAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = VbTextSecondary) } },
    )
}

@Composable
private fun VaultActionDialog(
    vault: SavingsVaultEntity,
    mode: VaultAction,
    accountIds: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amountMinor: Long) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VbPanelColor,
        title = {
            Text(if (mode == VaultAction.CONTRIBUTE) "Add to ${vault.name}" else "Withdraw from ${vault.name}", color = VbTextPrimary)
        },
        text = {
            VbTextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit) },
                label = "Amount (₽)",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val accountId = accountIds.firstOrNull()?.first ?: return@TextButton
                    val amountMinor = (amount.toLongOrNull() ?: 0L) * 100
                    if (amountMinor > 0) onConfirm(accountId, amountMinor)
                },
            ) { Text("Confirm", color = VbAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = VbTextSecondary) } },
    )
}
