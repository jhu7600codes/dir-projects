package com.vanbank.app.ui.admin

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.ui.components.SectionHeader
import com.vanbank.app.ui.components.VbPanel
import com.vanbank.app.ui.components.VbPrimaryButton
import com.vanbank.app.ui.components.VbSecondaryButton
import com.vanbank.app.ui.components.VbTextField
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbPositive
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.core.model.Money

private data class AiPreset(val title: String, val detail: String, val amountMinor: Long)

private val aiPresets = listOf(
    AiPreset("Research summary compilation", "3-source competitive analysis, delivered as a formatted brief.", 240_00),
    AiPreset("Weekly report generation", "Automated compilation of account activity into a shareable report.", 120_00),
    AiPreset("Data cleanup task", "Deduplicated and normalized a 4,000-row dataset on request.", 380_00),
    AiPreset("Meeting notes transcription", "Transcribed and summarized a 45-minute call.", 95_00),
)

@Composable
fun AdminScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    var selectedUserId by remember { mutableStateOf<Long?>(null) }
    var customTitle by remember { mutableStateOf("") }
    var customDetail by remember { mutableStateOf("") }
    var customAmount by remember { mutableStateOf("") }

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
            Text("Dev panel", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
        }

        if (uiState.message != null) {
            VbPanel(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(uiState.message!!, color = VbPositive)
                uiState.lastSpawnedCredentials?.let { (username, password) ->
                    Text("Username: $username", color = VbTextPrimary, modifier = Modifier.padding(top = 6.dp))
                    Text("Password: $password", color = VbTextPrimary)
                }
            }
        }

        SectionHeader("Test users", modifier = Modifier.padding(bottom = 10.dp))
        VbPanel(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Spawns a new user with a checking + savings account and a debit card.", color = VbTextSecondary)
            VbSecondaryButton(text = "Spawn test user", onClick = viewModel::spawnTestUser, modifier = Modifier.padding(top = 12.dp))
        }

        SectionHeader("Accounts", modifier = Modifier.padding(bottom = 10.dp))
        VbPanel(modifier = Modifier.padding(bottom = 16.dp)) {
            accounts.forEachIndexed { index, account ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                val owner = users.firstOrNull { it.id == account.userId }?.username ?: "user"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("@$owner · ${account.nickname}", color = VbTextPrimary, fontSize = 13.sp)
                        Text(Money.format(account.balanceMinor), color = VbTextSecondary, fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.resetBalance(account.id, account.type) }) {
                        Text("Reset", color = VbAccent)
                    }
                }
            }
        }

        SectionHeader("Trigger AI payment request", modifier = Modifier.padding(bottom = 10.dp))
        VbPanel(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Target user", color = VbTextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                users.forEach { user ->
                    androidx.compose.material3.FilterChip(
                        selected = selectedUserId == user.id,
                        onClick = { selectedUserId = user.id },
                        label = { Text("@${user.username}") },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VbAccent,
                            selectedLabelColor = com.vanbank.app.ui.theme.VbOnAccent,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Quick presets", color = VbTextSecondary, modifier = Modifier.padding(bottom = 6.dp), fontWeight = FontWeight.Medium)
            aiPresets.forEach { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.title, color = VbTextPrimary, fontSize = 13.sp)
                        Text(Money.format(preset.amountMinor), color = VbTextSecondary, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            selectedUserId?.let { viewModel.triggerAiRequest(it, preset.title, preset.detail, preset.amountMinor) }
                        },
                        enabled = selectedUserId != null,
                    ) { Text("Send", color = VbAccent) }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Custom request", color = VbTextSecondary, modifier = Modifier.padding(bottom = 6.dp), fontWeight = FontWeight.Medium)
            VbTextField(value = customTitle, onValueChange = { customTitle = it }, label = "Title", modifier = Modifier.padding(bottom = 10.dp))
            VbTextField(value = customDetail, onValueChange = { customDetail = it }, label = "Detail", modifier = Modifier.padding(bottom = 10.dp))
            VbTextField(
                value = customAmount,
                onValueChange = { customAmount = it.filter(Char::isDigit) },
                label = "Amount (₽)",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            VbPrimaryButton(
                text = "Send custom request",
                onClick = {
                    val amountMinor = (customAmount.toLongOrNull() ?: 0L) * 100
                    selectedUserId?.let {
                        viewModel.triggerAiRequest(it, customTitle, customDetail, amountMinor)
                        customTitle = ""; customDetail = ""; customAmount = ""
                    }
                },
                enabled = selectedUserId != null && customTitle.isNotBlank() && (customAmount.toLongOrNull() ?: 0L) > 0,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
