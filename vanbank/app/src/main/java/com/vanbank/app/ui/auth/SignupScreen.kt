package com.vanbank.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vanbank.app.ui.components.VanBankWordmark
import com.vanbank.app.ui.components.VbPrimaryButton
import com.vanbank.app.ui.components.VbTextField
import com.vanbank.app.ui.components.VbTextLink
import com.vanbank.app.ui.theme.VbBackground
import com.vanbank.app.ui.theme.VbNegative
import com.vanbank.app.ui.theme.VbTextPrimary
import com.vanbank.app.ui.theme.VbTextSecondary

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VbBackground)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            VanBankWordmark()
            Spacer(Modifier.height(32.dp))

            Text("Create your account", style = MaterialTheme.typography.headlineMedium, color = VbTextPrimary)
            Text(
                "You'll get a checking + savings account and a DIR debit card instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = VbTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            VbTextField(
                value = fullName,
                onValueChange = { fullName = it; viewModel.clearError() },
                label = "Full name",
                modifier = Modifier.padding(bottom = 12.dp),
            )
            VbTextField(
                value = username,
                onValueChange = { username = it; viewModel.clearError() },
                label = "Username",
                modifier = Modifier.padding(bottom = 12.dp),
            )
            VbTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                label = "Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(bottom = 12.dp),
            )
            VbTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; viewModel.clearError() },
                label = "Confirm password",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = VbNegative,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            VbPrimaryButton(
                text = "Create account",
                onClick = {
                    viewModel.signUp(username, password, confirmPassword, fullName, onSignupSuccess)
                },
                enabled = listOf(fullName, username, password, confirmPassword).all { it.isNotBlank() },
                loading = uiState.isLoading,
                modifier = Modifier.padding(top = 12.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Already have an account?", color = VbTextSecondary, style = MaterialTheme.typography.bodySmall)
                VbTextLink(text = "Log in", onClick = onNavigateToLogin)
            }
        }
    }
}
