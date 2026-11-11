package com.vanbank.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            VanBankWordmark()

            Spacer(Modifier.height(40.dp))

            Text(
                "Log in",
                style = MaterialTheme.typography.headlineMedium,
                color = VbTextPrimary,
            )
            Text(
                "Welcome back to your simulated bank.",
                style = MaterialTheme.typography.bodyMedium,
                color = VbTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
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
                text = "Log in",
                onClick = { viewModel.login(username, password, onLoginSuccess) },
                enabled = username.isNotBlank() && password.isNotBlank(),
                loading = uiState.isLoading,
                modifier = Modifier.padding(top = 12.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("New to VANBank?", color = VbTextSecondary, style = MaterialTheme.typography.bodySmall)
                VbTextLink(text = "Create an account", onClick = onNavigateToSignup)
            }
        }
    }
}
