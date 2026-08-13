package com.vanbank.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.ui.unit.sp
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.app.ui.components.AiRequestCard
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbPositive
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.AiRequestStatus
import com.vanbank.core.model.Money

@Composable
fun AiRequestsScreen(viewModel: AiRequestViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val pending = uiState.requests.filter { it.status == AiRequestStatus.PENDING }
    val resolved = uiState.requests.filter { it.status != AiRequestStatus.PENDING }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(VbBackground).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VbTextPrimary)
                }
                Icon(Icons.Filled.SmartToy, contentDescription = null, tint = VbTextPrimary, modifier = Modifier.padding(end = 8.dp))
                Text("DIR AI Assistant", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
            }
        }

        if (pending.isEmpty() && resolved.isEmpty()) {
            item {
                EmptyState(
                    title = "No requests yet",
                    subtitle = "The DIR AI Assistant will send payment requests here for completed tasks.",
                )
            }
        }

        if (pending.isNotEmpty()) {
            items(pending, key = { it.id }) { request ->
                AiRequestCard(
                    request = request,
                    onApprove = { viewModel.approve(request.id) },
                    onDecline = { viewModel.decline(request.id) },
                    isProcessing = uiState.processingId == request.id,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }

        if (resolved.isNotEmpty()) {
            item {
                Text(
                    "History",
                    color = VbTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(resolved, key = { it.id }) { request ->
                ResolvedRequestRow(request)
            }
        }
    }
}

@Composable
private fun ResolvedRequestRow(request: AiPaymentRequestEntity) {
    VbPanel(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(request.title, color = VbTextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    if (request.status == AiRequestStatus.APPROVED) "Approved" else "Declined",
                    color = if (request.status == AiRequestStatus.APPROVED) VbPositive else VbNegative,
                    fontSize = 12.sp,
                )
            }
            Text(Money.format(request.amountMinor), style = VbNumeralStyles.amountMedium, color = VbTextSecondary)
        }
    }
}
