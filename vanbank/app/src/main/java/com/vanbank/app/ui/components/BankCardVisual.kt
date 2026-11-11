package com.vanbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.core.model.CardType
import com.vanbank.core.numbers.CardNumberGenerator
import com.vanbank.app.ui.theme.VbCreditGradientEnd
import com.vanbank.app.ui.theme.VbCreditGradientStart
import com.vanbank.app.ui.theme.VbDebitGradientEnd
import com.vanbank.app.ui.theme.VbDebitGradientStart
import com.vanbank.app.ui.theme.VbFrostOverlay
import com.vanbank.app.ui.theme.VbNumeralStyles

/**
 * The card face: dark navy-to-charcoal for debit, warm orange/rust for
 * credit -- matches the DIR wordmark demo's card carousel look. Numbers
 * always render in the app's tabular monospace style.
 */
@Composable
fun BankCardVisual(
    cardNumber: String,
    cardholderName: String,
    expiryMonth: Int,
    expiryYear: Int,
    cardType: CardType,
    isFrozen: Boolean,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val gradient = if (cardType == CardType.DEBIT) {
        Brush.linearGradient(listOf(VbDebitGradientStart, VbDebitGradientEnd))
    } else {
        Brush.linearGradient(listOf(VbCreditGradientStart, VbCreditGradientEnd))
    }

    Box(
        modifier = modifier
            .aspectRatio(1.586f) // ISO/IEC 7810 ID-1 card ratio
            .clip(RoundedCornerShape(22.dp))
            .background(gradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = if (cardType == CardType.DEBIT) "DEBIT" else "CREDIT",
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
                DirWordmark(showMark = false)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

            Text(
                text = if (revealed) CardNumberGenerator.formatGrouped(cardNumber) else CardNumberGenerator.formatMasked(cardNumber),
                style = VbNumeralStyles.cardNumber,
                color = Color.White,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("CARDHOLDER", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(cardholderName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(
                        "%02d/%02d".format(expiryMonth, expiryYear % 100),
                        style = VbNumeralStyles.cardNumberSmall,
                        color = Color.White,
                    )
                }
            }
        }

        // Tap to reveal/hide the full PAN, top-right corner.
        IconButton(onClick = onToggleReveal, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            Icon(
                imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (revealed) "Hide card number" else "Show card number",
                tint = Color.White.copy(alpha = 0.85f),
            )
        }

        if (isFrozen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(VbFrostOverlay),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AcUnit, contentDescription = null, tint = Color.White)
                    Text(
                        "FROZEN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
