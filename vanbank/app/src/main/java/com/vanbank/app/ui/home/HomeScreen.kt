package com.vanbank.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.vanbank.app.data.local.entity.CardEntity
import com.vanbank.app.ui.components.AiRequestCard
import com.vanbank.app.ui.components.BankCardVisual
import com.vanbank.app.ui.components.EmptyState
import com.vanbank.app.ui.components.QuickAction
import com.vanbank.app.ui.components.QuickActionsGrid
import com.vanbank.app.ui.components.SectionHeader
import com.vanbank.app.ui.components.TransactionRow
import com.vanbank.app.ui.components.VbTextLink
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbPanelBorder
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.CardStatus
import com.vanbank.core.model.Money

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransfer: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToStatements: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToVaults: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToBillPay: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToAiRequests: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var loggingOut by remember { mutableStateOf(false) }

    LaunchedEffect(loggingOut) {
        if (loggingOut) onLoggedOut()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VbBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Welcome back,", color = VbTextSecondary, fontSize = 13.sp)
                    Text(
                        uiState.fullName.ifBlank { "..." },
                        color = VbTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
                IconButton(onClick = {
                    viewModel.logout()
                    loggingOut = true
                }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Log out", tint = VbTextSecondary)
                }
            }
        }

        if (uiState.cards.isNotEmpty()) {
            item {
                CardCarousel(cards = uiState.cards, onToggleFreeze = viewModel::toggleCardFreeze)
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Total balance", color = VbTextSecondary, fontSize = 13.sp)
                Text(
                    Money.format(uiState.totalBalanceMinor),
                    style = VbNumeralStyles.balanceLarge,
                    color = VbTextPrimary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(modifier = Modifier.padding(top = 10.dp)) {
                    uiState.accounts.forEach { account ->
                        Column(modifier = Modifier.padding(end = 24.dp)) {
                            Text(account.nickname, color = VbTextSecondary, fontSize = 12.sp)
                            Text(
                                Money.format(account.balanceMinor),
                                style = VbNumeralStyles.amountMedium,
                                color = VbTextPrimary,
                            )
                        }
                    }
                }
            }
        }

        item {
            QuickActionsGrid(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                actions = listOf(
                    QuickAction("Transfer", Icons.Filled.SwapHoriz, onNavigateToTransfer),
                    QuickAction("Pay Bills", Icons.Filled.Receipt, onNavigateToBillPay),
                    QuickAction("Loans", Icons.Filled.AccountBalance, onNavigateToLoans),
                    QuickAction("Vaults", Icons.Filled.Savings, onNavigateToVaults),
                    QuickAction("Budget", Icons.Filled.PieChart, onNavigateToBudget),
                    QuickAction("Statements", Icons.Filled.Description, onNavigateToStatements),
                    QuickAction("AI Assistant", Icons.Filled.SmartToy, onNavigateToAiRequests),
                    QuickAction("Admin", Icons.Filled.AdminPanelSettings, onNavigateToAdmin),
                ),
            )
        }

        if (uiState.pendingAiRequests.isNotEmpty()) {
            items(uiState.pendingAiRequests, key = { it.id }) { request ->
                AiRequestCard(
                    request = request,
                    onApprove = { viewModel.approveAiRequest(request.id) {} },
                    onDecline = { viewModel.declineAiRequest(request.id) {} },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        item {
            SectionHeader(
                title = "Recent activity",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                action = { VbTextLink(text = "See all", onClick = onNavigateToTransactions) },
            )
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = "No activity yet",
                    subtitle = "Your transactions will show up here.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(uiState.recentTransactions, key = { it.id }) { tx ->
                TransactionRow(tx, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
private fun CardCarousel(
    cards: List<CardEntity>,
    onToggleFreeze: (Long, Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { cards.size })
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 14.dp,
        ) { page ->
            val card = cards[page]
            var revealed by remember(card.id) { mutableStateOf(false) }
            BankCardVisual(
                cardNumber = card.cardNumber,
                cardholderName = card.cardholderName,
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                cardType = card.cardType,
                isFrozen = card.status == CardStatus.FROZEN,
                revealed = revealed,
                onToggleReveal = { revealed = !revealed },
                subtitle = card.creditLimitMinor?.let { "Limit ${Money.format(it)}" },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(cards.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (active) 18.dp else 6.dp)
                        .background(if (active) VbAccent else VbPanelBorder, RoundedCornerShape(3.dp)),
                )
            }
        }

        val currentCard = cards.getOrNull(pagerState.currentPage)
        if (currentCard != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                val frozen = currentCard.status == CardStatus.FROZEN
                VbTextLink(
                    text = if (frozen) "Unfreeze card" else "Freeze card",
                    onClick = { onToggleFreeze(currentCard.id, frozen) },
                )
            }
        }
    }
}
