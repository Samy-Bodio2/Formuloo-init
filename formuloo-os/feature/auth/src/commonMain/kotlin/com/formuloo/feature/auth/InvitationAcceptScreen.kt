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
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.OrganizationCard
import com.formuloo.core.designsystem.PasswordStrengthIndicator
import com.formuloo.core.network.dto.auth.InvitePreviewDto

@Composable
fun InvitationAcceptScreen(
    code: String,
    preview: InvitePreviewDto,
    viewModel: InvitationViewModel,
    onBack: () -> Unit,
    onAccepted: () -> Unit,
) {
    val state by viewModel.acceptState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val confirmError = if (submitted && password != confirmPassword) {
        "Les mots de passe ne correspondent pas"
    } else {
        null
    }

    LaunchedEffect(state) {
        if (state is UiState.Success) onAccepted()
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
            Text("Invitation", color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))

        OrganizationCard(
            initials = preview.organisation.initials,
            name = preview.organisation.name,
            subtitle = "Rôle : ${preview.role ?: "Membre"}",
        )
        Spacer(Modifier.height(24.dp))

        Text("Création de compte invité", style = MaterialTheme.typography.bodyMedium, color = FormulooOnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("Bienvenue, ${preview.firstName} 👋", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(24.dp))

        ReadOnlyField(label = "Adresse e-mail", value = preview.email)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ReadOnlyField(label = "Prénom", value = preview.firstName, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            ReadOnlyField(label = "Nom", value = preview.lastName, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        FormulooPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Mot de passe",
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
            text = "Créer mon compte & accéder",
            loading = current is UiState.Loading,
            onClick = {
                submitted = true
                if (password.isNotBlank() && password == confirmPassword) {
                    viewModel.accept(code, preview.email, password, confirmPassword)
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = FormulooOnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = FormulooTextPrimary)
    }
}
