package com.vanbank.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbAccentDim
import com.vanbank.app.ui.theme.VbNumeralStyles
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbPanelBorder
import com.vanbank.app.ui.theme.VbPanelElevated
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary
import com.vanbank.app.ui.theme.VbWarning
import com.vanbank.core.model.Money

/**
 * A DIR AI Assistant payment request, presented as an invoice/approval card
 * -- title, longer detail, amount due, and Approve/Decline actions. This is
 * meant to read as a professional billing prompt, not a joke or a random
 * popup.
 */
@Composable
fun AiRequestCard(
    request: AiPaymentRequestEntity,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
) {
    VbPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(VbAccent, VbAccentDim))),
                contentAlignment = Alignment.Center,
            ) {
                Text("AI", color = VbOnAccent, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("DIR AI Assistant", color = VbTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Payment request", color = VbTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            StatusPill(text = "PENDING", color = VbWarning)
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VbPanelElevated, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text(request.title, color = VbTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                request.detail,
                color = VbTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount due", color = VbTextSecondary, fontSize = 13.sp)
                Text(Money.format(request.amountMinor), style = VbNumeralStyles.amountMedium, color = VbTextPrimary)
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDecline,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VbPanelBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VbTextPrimary),
            ) {
                Text("Decline")
            }
            Button(
                onClick = onApprove,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VbAccent, contentColor = VbOnAccent),
            ) {
                Text("Approve")
            }
        }
    }
}
