package com.vanbank.app.ui.loans

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.data.local.entity.LoanEntity
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.SectionHeader
import com.vanbank.app.ui.components.StatusPill
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.components.VbPrimaryButton
import com.vanbank.app.ui.components.VbTextField
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbPositive
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.app.ui.theme.VbWarning
import com.vanbank.core.model.LoanStatus
import com.vanbank.core.model.Money

@Composable
fun LoansScreen(viewModel: LoansViewModel, onOpenLoan: (Long) -> Unit, onBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val requestState by viewModel.requestState.collectAsState()

    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var purpose by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("100000") }
    var termMonths by remember { mutableFloatStateOf(24f) }

    LaunchedEffect(accounts) {
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }

    val principalMinor = (amountText.toDoubleOrNull() ?: 0.0).times(100).toLong()
    val quotedRate = if (principalMinor > 0) viewModel.quoteRate(principalMinor, termMonths.toInt()) else 0.0

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
            Text("Loans", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
        }

        if (loans.isNotEmpty()) {
            SectionHeader("Your loans", modifier = Modifier.padding(bottom = 10.dp))
            loans.forEach { loan ->
                LoanSummaryRow(loan, onClick = { onOpenLoan(loan.id) })
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(10.dp))
        }

        SectionHeader("Request a loan", modifier = Modifier.padding(bottom = 10.dp))
        VbPanel {
            VbTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = "Purpose",
                placeholder = "e.g. Home renovation",
                modifier = Modifier.padding(bottom = 12.dp),
            )
            VbTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = "Amount (₽)",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text("Term: ${termMonths.toInt()} months", color = VbTextSecondary, modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = termMonths,
                onValueChange = { termMonths = it },
                valueRange = 3f..60f,
                steps = 56,
                colors = SliderDefaults.colors(thumbColor = VbAccent, activeTrackColor = VbAccent),
            )

            if (principalMinor > 0) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Quoted rate", color = VbTextSecondary)
                    Text("${"%.2f".format(quotedRate)}% APR", color = VbTextPrimary, fontWeight = FontWeight.Medium)
                }
            }

            if (requestState.errorMessage != null) {
                Text(requestState.errorMessage!!, color = VbNegative, modifier = Modifier.padding(top = 10.dp))
            }
            if (requestState.successMessage != null) {
                Text(requestState.successMessage!!, color = VbPositive, modifier = Modifier.padding(top = 10.dp))
            }

            VbPrimaryButton(
                text = "Request loan",
                onClick = {
                    accountId?.let { id ->
                        viewModel.requestLoan(id, purpose, principalMinor, termMonths.toInt())
                    }
                },
                enabled = accountId != null && purpose.isNotBlank() && principalMinor > 0 && !requestState.isSubmitting,
                loading = requestState.isSubmitting,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LoanSummaryRow(loan: LoanEntity, onClick: () -> Unit) {
    VbPanel(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(loan.purpose, color = VbTextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    "${Money.format(loan.principalMinor)} · ${loan.termMonths} mo · ${"%.2f".format(loan.annualRatePercent)}% APR",
                    color = VbTextSecondary,
                    fontSize = 12.sp,
                )
            }
            StatusPill(
                text = loan.status.name.replace('_', ' '),
                color = when (loan.status) {
                    LoanStatus.ACTIVE -> VbAccent
                    LoanStatus.PAID_OFF -> VbPositive
                    LoanStatus.REJECTED -> VbNegative
                    LoanStatus.PENDING -> VbWarning
                },
            )
        }
    }
}
