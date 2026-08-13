package com.vanbank.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vanbank.app.ui.theme.VbAccent
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbPanelBorder
import com.vanbank.app.ui.theme.VbTextMuted
import com.vanbank.app.ui.theme.VbTextPrimary

@Composable
fun VbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = VbTextMuted) } },
        isError = isError,
        supportingText = errorText?.let { { Text(it, color = VbNegative) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VbTextPrimary,
            unfocusedTextColor = VbTextPrimary,
            focusedBorderColor = VbAccent,
            unfocusedBorderColor = VbPanelBorder,
            cursorColor = VbAccent,
            focusedLabelColor = VbAccent,
            unfocusedLabelColor = VbTextMuted,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
