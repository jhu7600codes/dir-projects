package com.vanbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbAccentDim
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbTextPrimary

/** The DIR network wordmark -- a small blue mark plus letterspaced "DIR", used anywhere the payment network needs a badge. */
@Composable
fun DirWordmark(modifier: Modifier = Modifier, showMark: Boolean = true) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (showMark) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(VbAccent, VbAccentDim))),
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = VbOnAccent, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = "DIR",
            color = VbTextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

/** VANBank app title lockup, used on auth screens. */
@Composable
fun VanBankWordmark(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "VAN", color = VbTextPrimary, fontWeight = FontWeight.Black, fontSize = 34.sp)
            Text(text = "Bank", color = VbAccent, fontWeight = FontWeight.Black, fontSize = 34.sp)
        }
        Text(
            text = "BANKING ON DIR NETWORK",
            color = VbTextPrimary.copy(alpha = 0.45f),
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
