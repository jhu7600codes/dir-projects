package com.vanbank.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbOnAccent
import com.vanbank.app.ui.theme.VbPanelElevated
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary

@Composable
fun VbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VbAccent, contentColor = VbOnAccent, disabledContainerColor = VbAccent.copy(alpha = 0.4f)),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), color = VbOnAccent, strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}

@Composable
fun VbSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = VbTextPrimary),
    ) {
        Text(text)
    }
}

@Composable
fun VbDangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VbNegative, contentColor = Color.White),
    ) {
        Text(text)
    }
}

@Composable
fun VbTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = VbTextSecondary)
    }
}
