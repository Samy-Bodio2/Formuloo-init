package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.OrganizationCard
import com.formuloo.core.network.dto.auth.InvitePreviewDto

@Composable
fun InvitationPreviewScreen(
    code: String,
    viewModel: InvitationViewModel,
    onAccept: (InvitePreviewDto) -> Unit,
) {
    val state by viewModel.previewState.collectAsStateWithLifecycle()

    LaunchedEffect(code) {
        viewModel.loadPreview(code)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooPrimary),
    ) {
        Spacer(Modifier.height(80.dp))
        Box(modifier = Modifier.fillMaxSize().padding(top = 0.dp)) {
            when (val current = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                is UiState.Success -> InvitationPreviewContent(current.data, onAccept = { onAccept(current.data) })
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(current.message, color = Color.White)
                }
                is UiState.Empty -> Unit
            }
        }
    }
}

@Composable
private fun InvitationPreviewContent(preview: InvitePreviewDto, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrganizationCard(
                initials = preview.organisation.initials,
                name = preview.invitedBy?.let { "$it vous invite à rejoindre" } ?: "Vous êtes invité(e) à rejoindre",
                subtitle = preview.organisation.name,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Vous avez été invité(e) à collaborer sur l'espace Formuloo OS de ${preview.organisation.name}. " +
                    "Acceptez pour créer votre compte et démarrer.",
                style = MaterialTheme.typography.bodyMedium,
                color = FormulooOnSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val role = preview.role
            if (role != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RÔLE ASSIGNÉ  ", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                    Text(role, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.modules.forEach { module ->
                    Box(
                        modifier = Modifier
                            .background(FormulooMint, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(module, fontSize = 11.sp, color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            FormulooButton(text = "Accepter l'invitation", onClick = onAccept)
            Spacer(Modifier.height(12.dp))
            Text(
                "Invitation envoyée à ${preview.email} · expire le ${preview.expiresAt.take(10)}",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
            )
        }
    }
}
