package com.vanbank.app.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vanbank.app.ui.components.SectionHeader
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.components.VbPrimaryButton
import com.vanbank.app.ui.components.VbTextField
import com.vanbank.app.ui.theme.VbAccent as ThemeAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbPanel as ThemePanel
import com.vanbank.app.ui.theme.VbPanelBorder
import com.vanbank.app.ui.theme.VbPositive
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.AccountType
import com.vanbank.core.model.Money

@Composable
fun TransferScreen(viewModel: TransferViewModel, onBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var mode by rememberSaveable { mutableStateOf(TransferMode.INTERNAL) }
    var fromAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recipient by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(accounts) {
        if (fromAccountId == null) fromAccountId = accounts.firstOrNull { it.type == AccountType.CHECKING }?.id ?: accounts.firstOrNull()?.id
        if (toAccountId == null) toAccountId = accounts.firstOrNull { it.id != fromAccountId }?.id
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            amount = ""
            note = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VbBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VbTextPrimary)
            }
            Text("Transfer", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            ModeChip("Own accounts", mode == TransferMode.INTERNAL) { mode = TransferMode.INTERNAL; viewModel.clearMessages() }
            ModeChip("VANBank user", mode == TransferMode.TO_USER) { mode = TransferMode.TO_USER; viewModel.clearMessages() }
            ModeChip("Card number", mode == TransferMode.BY_CARD) { mode = TransferMode.BY_CARD; viewModel.clearMessages() }
        }

        VbPanel {
            SectionHeader("From", modifier = Modifier.padding(bottom = 10.dp))
            Column {
                accounts.forEach { account ->
                    AccountOptionRow(
                        label = account.nickname,
                        detail = Money.format(account.balanceMinor),
                        selected = account.id == fromAccountId,
                        onSelect = { fromAccountId = account.id },
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        VbPanel {
            when (mode) {
                TransferMode.INTERNAL -> {
                    SectionHeader("To", modifier = Modifier.padding(bottom = 10.dp))
                    Column {
                        accounts.filter { it.id != fromAccountId }.forEach { account ->
                            AccountOptionRow(
                                label = account.nickname,
                                detail = Money.format(account.balanceMinor),
                                selected = account.id == toAccountId,
                                onSelect = { toAccountId = account.id },
                            )
                        }
                    }
                }
                TransferMode.TO_USER -> {
                    VbTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = "Username or account number",
                        placeholder = "@username or 10-digit account #",
                    )
                }
                TransferMode.BY_CARD -> {
                    VbTextField(
                        value = recipient,
                        onValueChange = { recipient = it.filter(Char::isDigit).take(16) },
                        label = "16-digit DIR card number",
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        VbPanel {
            VbTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Amount (₽)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            VbTextField(
                value = note,
                onValueChange = { note = it },
                label = "Note (optional)",
            )
        }

        if (uiState.errorMessage != null) {
            Text(uiState.errorMessage!!, color = VbNegative, modifier = Modifier.padding(top = 12.dp))
        }
        if (uiState.successMessage != null) {
            Text(uiState.successMessage!!, color = VbPositive, modifier = Modifier.padding(top = 12.dp))
        }

        VbPrimaryButton(
            text = "Send",
            onClick = {
                fromAccountId?.let { from ->
                    viewModel.submit(mode, from, recipient, toAccountId, amount, note)
                }
            },
            enabled = fromAccountId != null && amount.isNotBlank() && !uiState.isSubmitting &&
                (mode != TransferMode.INTERNAL || toAccountId != null) &&
                (mode == TransferMode.INTERNAL || recipient.isNotBlank()),
            loading = uiState.isSubmitting,
            modifier = Modifier.padding(vertical = 20.dp),
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ThemeAccent,
            selectedLabelColor = VbOnAccent,
            containerColor = ThemePanel,
            labelColor = VbTextSecondary,
        ),
    )
}

@Composable
private fun AccountOptionRow(label: String, detail: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onSelect),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = ThemeAccent, unselectedColor = VbPanelBorder),
            )
            Text(label, color = VbTextPrimary)
        }
        Text(detail, color = VbTextSecondary)
    }
}
