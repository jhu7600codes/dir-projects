package com.vanbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbPanelElevated
import com.vanbank.app.ui.theme.VbTextPrimary

data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** A fixed 4-per-row grid of icon+label actions -- Transfer, Pay bills, Loans, Vaults, Budget, Statements, ... */
@Composable
fun QuickActionsGrid(actions: List<QuickAction>, modifier: Modifier = Modifier, columns: Int = 4) {
    Column(modifier = modifier.fillMaxWidth()) {
        actions.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { action ->
                    QuickActionItem(action, modifier = Modifier.weight(1f))
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun QuickActionItem(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        // Default clickable indication: Material3 supplies the ripple via LocalIndication,
        // so this doesn't need to build one by hand (that API churns a lot between versions).
        modifier = modifier.clickable(onClick = action.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(52.dp)
                .background(VbPanelElevated, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(action.icon, contentDescription = action.label, tint = VbAccent, modifier = Modifier.size(22.dp))
        }
        Text(
            action.label,
            color = VbTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
