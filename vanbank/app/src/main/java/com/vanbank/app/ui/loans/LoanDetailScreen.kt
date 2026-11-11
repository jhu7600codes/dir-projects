package com.vanbank.app.ui.loans

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vanbank.app.data.local.entity.LoanInstallmentEntity
import com.vanbank.app.ui.components.StatusPill
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.components.VbPrimaryButton
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
import java.time.LocalDate

@Composable
fun LoanDetailScreen(viewModel: LoansViewModel, loanId: Long, onBack: () -> Unit) {
    val loan by viewModel.loan(loanId).collectAsState(initial = null)
    val installments by viewModel.installments(loanId).collectAsState(initial = emptyList())
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPaying by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(VbBackground).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VbTextPrimary)
                }
                Text("Loan details", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
            }
        }

        val currentLoan = loan
        if (currentLoan != null) {
            item {
                VbPanel(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(currentLoan.purpose, color = VbTextPrimary, fontWeight = FontWeight.SemiBold)
                        StatusPill(
                            text = currentLoan.status.name.replace('_', ' '),
                            color = when (currentLoan.status) {
                                LoanStatus.ACTIVE -> VbAccent
                                LoanStatus.PAID_OFF -> VbPositive
                                LoanStatus.REJECTED -> VbNegative
                                LoanStatus.PENDING -> VbWarning
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    DetailRow("Principal", Money.format(currentLoan.principalMinor))
                    DetailRow("Interest rate", "${"%.2f".format(currentLoan.annualRatePercent)}% APR")
                    DetailRow("Term", "${currentLoan.termMonths} months")

                    val remaining = installments.count { !it.isPaid }
                    DetailRow("Installments remaining", "$remaining of ${installments.size}")

                    if (currentLoan.status == LoanStatus.ACTIVE && remaining > 0) {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = VbNegative, modifier = Modifier.padding(top = 8.dp))
                        }
                        VbPrimaryButton(
                            text = "Pay next installment",
                            onClick = {
                                isPaying = true
                                viewModel.payNextInstallment(loanId) { result ->
                                    isPaying = false
                                    result.onFailure { errorMessage = it.message }
                                }
                            },
                            loading = isPaying,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }

            item {
                Text("Repayment schedule", color = VbTextSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        items(installments, key = { it.id }) { installment ->
            InstallmentRow(installment)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = VbTextSecondary)
        Text(value, color = VbTextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InstallmentRow(installment: LoanInstallmentEntity) {
    VbPanel(modifier = Modifier.padding(bottom = 8.dp), contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("#${installment.number}", color = VbTextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    LocalDate.ofEpochDay(installment.dueDateEpochDay).toString(),
                    color = VbTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(installment.totalDueMinor), style = VbNumeralStyles.amountMedium, color = VbTextPrimary)
                Text(
                    if (installment.isPaid) "Paid" else "Due",
                    color = if (installment.isPaid) VbPositive else VbWarning,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
