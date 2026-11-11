package com.vanbank.app.ui.statements

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanbank.app.data.repository.label
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.SectionHeader
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbPanel as VbPanelColor
import com.vanbank.app.ui.theme.VbPositive
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.Money
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun StatementsScreen(viewModel: StatementsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary

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
            Text("Statements", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            listOf(7L to "7 days", 30L to "30 days", 90L to "90 days").forEach { (days, label) ->
                FilterChip(
                    selected = uiState.selectedDays == days,
                    onClick = { viewModel.loadRange(days) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VbAccent,
                        selectedLabelColor = VbOnAccent,
                        containerColor = VbPanelColor,
                        labelColor = VbTextSecondary,
                    ),
                )
            }
        }

        if (summary == null) {
            EmptyState(title = "No data", subtitle = "Statement is loading or empty for this range.")
            return@Column
        }

        VbPanel {
            Text(
                "${summary.from.format(dateFormatter)} — ${summary.to.format(dateFormatter)}",
                color = VbTextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SummaryRow("Total in", Money.format(summary.totalInMinor), VbPositive)
            SummaryRow("Total out", "-${Money.format(summary.totalOutMinor)}", VbTextPrimary)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(color = com.vanbank.app.ui.theme.VbPanelBorder)
            Spacer(Modifier.height(8.dp))
            SummaryRow(
                "Net",
                Money.format(summary.netMinor),
                if (summary.netMinor >= 0) VbPositive else com.vanbank.app.ui.theme.VbNegative,
                bold = true,
            )
            Text(
                "${summary.transactionCount} transactions",
                color = VbTextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        if (summary.byCategory.isNotEmpty()) {
            SectionHeader("Spending by category", modifier = Modifier.padding(bottom = 10.dp))
            VbPanel {
                summary.byCategory.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(entry.key.label(), color = VbTextPrimary)
                        Text(Money.format(entry.value), style = VbNumeralStyles.amountSmall, color = VbTextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = VbTextSecondary)
        Text(
            value,
            style = VbNumeralStyles.amountMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
