package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPasswordField
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.OtpInputField
import com.formuloo.core.designsystem.PasswordStrengthIndicator
import org.koin.compose.viewmodel.koinViewModel

private const val CODE_LENGTH = 6

@Composable
fun PasswordResetConfirmScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: PasswordResetViewModel = koinViewModel(),
) {
    val state by viewModel.confirmState.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val confirmError = if (submitted && password != confirmPassword) {
        "Les mots de passe ne correspondent pas"
    } else {
        null
    }

    LaunchedEffect(state) {
        if (state is UiState.Success) onResetSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onBack),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooPrimary)
            Spacer(Modifier.width(4.dp))
            Text("Retour", color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))

        Text("Nouveau mot de passe", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Saisissez le code à 6 chiffres reçu par email, puis choisissez votre nouveau mot de passe.",
            style = MaterialTheme.typography.bodyLarge,
            color = FormulooOnSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        OtpInputField(
            code = code,
            onCodeChange = { code = it },
            length = CODE_LENGTH,
        )
        Spacer(Modifier.height(16.dp))
        FormulooPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Nouveau mot de passe",
        )
        Spacer(Modifier.height(8.dp))
        PasswordStrengthIndicator(password = password)
        Spacer(Modifier.height(16.dp))
        FormulooPasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmer le mot de passe",
            isError = confirmError != null,
            errorMessage = confirmError,
        )
        Spacer(Modifier.height(16.dp))

        val current = state
        if (current is UiState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = FormulooError.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(current.message, color = FormulooError, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        FormulooButton(
            text = "Réinitialiser le mot de passe",
            loading = current is UiState.Loading,
            enabled = code.length == CODE_LENGTH,
            onClick = {
                submitted = true
                if (password.isNotBlank() && password == confirmPassword) {
                    viewModel.confirmReset(code, password, confirmPassword)
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}
