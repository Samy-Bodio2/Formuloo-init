package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooTextField
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PasswordResetRequestScreen(
    onBack: () -> Unit,
    onLinkSent: () -> Unit,
    viewModel: PasswordResetViewModel = koinViewModel(),
) {
    val state by viewModel.requestState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val emailError = if (submitted && !isValidEmail(email)) "Adresse e-mail invalide" else null

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
            Text("Connexion", color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(FormulooSecondaryBg, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = FormulooPrimary)
        }
        Spacer(Modifier.height(16.dp))

        Text("Mot de passe oublié ?", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Saisissez l'adresse e-mail de votre compte. Nous vous enverrons un lien sécurisé pour le réinitialiser.",
            style = MaterialTheme.typography.bodyLarge,
            color = FormulooOnSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        FormulooTextField(
            value = email,
            onValueChange = { email = it },
            label = "Adresse e-mail",
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            isError = emailError != null,
            errorMessage = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
            text = "Envoyer le lien",
            loading = current is UiState.Loading,
            onClick = {
                submitted = true
                if (isValidEmail(email)) viewModel.requestReset(email)
            },
        )

        if (current is UiState.Success) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Si un compte existe pour cette adresse, un code à 6 chiffres valable 2h vient d'être envoyé.",
                color = FormulooOnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            FormulooButton(text = "J'ai reçu mon code", onClick = onLinkSent)
        }

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                "← Retour à la connexion",
                color = FormulooPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun isValidEmail(email: String): Boolean {
    val pattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return pattern.matches(email)
}
