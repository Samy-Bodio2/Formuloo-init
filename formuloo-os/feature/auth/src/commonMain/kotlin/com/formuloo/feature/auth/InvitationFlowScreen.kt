package com.formuloo.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.network.dto.auth.InvitePreviewDto
import org.koin.compose.viewmodel.koinViewModel

private sealed interface InvitationStep {
    data object Preview : InvitationStep
    data class Accept(val preview: InvitePreviewDto) : InvitationStep
    data class Success(val preview: InvitePreviewDto) : InvitationStep
}

/**
 * Orchestre les 3 étapes du flux d'invitation (aperçu → création de compte → succès)
 * avec une seule instance de [InvitationViewModel] partagée — évite de devoir faire
 * transiter le DTO complet à travers la navigation typée.
 */
@Composable
fun InvitationFlowScreen(
    code: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val viewModel: InvitationViewModel = koinViewModel()
    var step by remember { mutableStateOf<InvitationStep>(InvitationStep.Preview) }

    when (val current = step) {
        is InvitationStep.Preview -> {
            if (code.length != 6) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Code d'invitation invalide.", color = FormulooError)
                        Spacer(Modifier.height(16.dp))
                        FormulooButton(text = "Retour", onClick = onBack)
                    }
                }
            } else {
                InvitationPreviewScreen(
                    code = code,
                    viewModel = viewModel,
                    onAccept = { preview -> step = InvitationStep.Accept(preview) },
                )
            }
        }
        is InvitationStep.Accept -> {
            InvitationAcceptScreen(
                code = code,
                preview = current.preview,
                viewModel = viewModel,
                onBack = { step = InvitationStep.Preview },
                onAccepted = { step = InvitationStep.Success(current.preview) },
            )
        }
        is InvitationStep.Success -> {
            InvitationSuccessScreen(
                organisationName = current.preview.organisation.name,
                role = current.preview.role,
                onAccessSpace = onDone,
            )
        }
    }
}
