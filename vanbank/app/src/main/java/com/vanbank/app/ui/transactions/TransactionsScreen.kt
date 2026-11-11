package com.vanbank.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.vanbank.app.data.repository.label
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.TransactionRow
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbPanel
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.TransactionCategory

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val categoriesPresent = uiState.all.map { it.category }.distinct()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(VbBackground).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VbTextPrimary)
                }
                Text("Transactions", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VbAccent,
                            selectedLabelColor = VbOnAccent,
                            containerColor = VbPanel,
                            labelColor = VbTextSecondary,
                        ),
                    )
                }
                items(categoriesPresent) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.label()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VbAccent,
                            selectedLabelColor = VbOnAccent,
                            containerColor = VbPanel,
                            labelColor = VbTextSecondary,
                        ),
                    )
                }
            }
        }

        if (uiState.filtered.isEmpty()) {
            item {
                EmptyState(
                    title = "No transactions",
                    subtitle = "Nothing here yet for this filter.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            }
        } else {
            items(uiState.filtered, key = { it.id }) { tx ->
                TransactionRow(tx, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}
