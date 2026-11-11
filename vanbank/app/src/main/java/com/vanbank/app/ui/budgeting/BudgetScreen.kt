package com.vanbank.app.ui.budgeting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.vanbank.app.data.repository.label
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbPanel as VbPanelColor
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.finance.CategorySpend
import com.vanbank.core.model.Money

private val CategoryPalette = listOf(
    Color(0xFF3D5AFE), Color(0xFFDB8A45), Color(0xFF34D399), Color(0xFFF87171),
    Color(0xFFFBBF24), Color(0xFF60A5FA), Color(0xFFA78BFA), Color(0xFFEC4899),
    Color(0xFF2DD4BF), Color(0xFFF97316), Color(0xFF94A3B8), Color(0xFFEF4444),
    Color(0xFF22D3EE), Color(0xFFA3E635), Color(0xFF6366F1),
)

@Composable
fun BudgetScreen(viewModel: BudgetViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

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
            Text("Budget", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            listOf(7L to "7 days", 30L to "30 days", 90L to "90 days").forEach { (days, label) ->
                FilterChip(
                    selected = uiState.selectedDays == days,
                    onClick = { viewModel.loadRange(days) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VbAccent,
                        selectedLabelColor = com.vanbank.app.ui.theme.VbOnAccent,
                        containerColor = VbPanelColor,
                        labelColor = VbTextSecondary,
                    ),
                )
            }
        }

        if (uiState.breakdown.isEmpty()) {
            EmptyState(
                title = "Nothing to show",
                subtitle = "Spend on a few categories and your breakdown will appear here.",
            )
            return@Column
        }

        val slices = uiState.breakdown.mapIndexed { index, spend ->
            PieChartData.Slice(
                spend.category.label(),
                spend.percentage.toFloat(),
                CategoryPalette[index % CategoryPalette.size],
            )
        }
        val pieChartData = PieChartData(slices = slices, plotType = PlotType.Pie)
        val pieChartConfig = PieChartConfig(
            labelVisible = false,
            isAnimationEnable = true,
            activeSliceAlpha = 0.95f,
            strokeWidth = 90f,
            chartPadding = 8,
            backgroundColor = VbBackground,
        )

        VbPanel {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                PieChart(
                    modifier = Modifier.size(200.dp),
                    pieChartData = pieChartData,
                    pieChartConfig = pieChartConfig,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        VbPanel {
            uiState.breakdown.forEachIndexed { index, spend ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                CategoryLegendRow(spend, CategoryPalette[index % CategoryPalette.size])
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryLegendRow(spend: CategorySpend, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(spend.category.label(), color = VbTextPrimary, fontWeight = FontWeight.Medium)
            Text("${"%.1f".format(spend.percentage)}%", color = VbTextSecondary, fontSize = 12.sp)
        }
        Text(Money.format(spend.totalMinor), style = VbNumeralStyles.amountSmall, color = VbTextPrimary)
    }
}
