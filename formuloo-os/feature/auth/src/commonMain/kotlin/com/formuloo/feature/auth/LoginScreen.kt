package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.auth.domain.model.AuthState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooCheckbox
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooLogo
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPasswordField
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.SecureConnectionBadge
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onOtpRequired: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToInvite: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val fieldErrors by viewModel.fieldErrors.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val emailError = if (submitted && !isValidEmail(email)) "Adresse e-mail invalide" else null
    val passwordError = if (submitted && password.isBlank()) "Le mot de passe est requis" else null

    LaunchedEffect(state) {
        if (state is AuthState.LoggedIn) {
            val user = currentUser
            if (user != null && !user.isVerified) {
                onOtpRequired(email)
            } else {
                onLoginSuccess()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormulooLogo()
            Spacer(Modifier.width(12.dp))
            Text("Formuloo", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))
        SecureConnectionBadge()
        Spacer(Modifier.height(24.dp))
        Text("Bon retour 👋", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Connectez-vous à votre espace Formuloo OS.",
            style = MaterialTheme.typography.bodyLarge,
            color = FormulooOnSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        FormulooTextField(
            value = email,
            onValueChange = { email = it },
            label = "Adresse e-mail",
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            isError = emailError != null || fieldErrors.containsKey("email"),
            errorMessage = emailError ?: fieldErrors["email"],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(16.dp))
        FormulooPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Mot de passe",
            isError = passwordError != null || fieldErrors.containsKey("password"),
            errorMessage = passwordError ?: fieldErrors["password"],
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword) {
                Text("Mot de passe oublié ?", color = FormulooPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))
        FormulooCheckbox(
            checked = rememberMe,
            onCheckedChange = { rememberMe = it },
            label = "Rester connecté sur cet appareil",
        )
        Spacer(Modifier.height(16.dp))

        val errorState = state
        if (errorState is AuthState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FormulooError.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = errorState.message,
                    color = FormulooError,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        FormulooButton(
            text = "Se connecter",
            loading = state is AuthState.Loading,
            onClick = {
                submitted = true
                if (isValidEmail(email) && password.isNotBlank()) {
                    viewModel.login(email, password)
                }
            },
        )

        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Pas encore de compte ? ", color = FormulooOnSurfaceVariant)
            Text(
                text = "Créer un compte",
                color = FormulooPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onNavigateToRegister),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "J'ai reçu une invitation",
                color = FormulooPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onNavigateToInvite),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun isValidEmail(email: String): Boolean {
    val pattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return pattern.matches(email)
}
