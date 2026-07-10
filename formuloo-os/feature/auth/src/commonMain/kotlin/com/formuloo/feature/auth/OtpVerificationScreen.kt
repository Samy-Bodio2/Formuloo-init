package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.OtpInputField
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val RESEND_DELAY_SECONDS = 60
private const val OTP_LENGTH = 6

@Composable
fun OtpVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: OtpVerificationViewModel = koinViewModel(),
) {
    var code by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableStateOf(RESEND_DELAY_SECONDS) }
    val verifyState by viewModel.verifyState.collectAsStateWithLifecycle()
    val isVerifying = verifyState is UiState.Loading
    val errorMessage = (verifyState as? UiState.Error)?.message

    LaunchedEffect(verifyState) {
        if (verifyState is UiState.Success) onVerified()
    }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("< Précédent", color = FormulooPrimary)
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FormulooMint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MailOutline,
                contentDescription = null,
                tint = FormulooPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Vérifiez votre e-mail",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("Nous avons envoyé un code à 6 chiffres à ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(email)
                }
                append(". Saisissez-le ci-dessous.")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = FormulooOnSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        OtpInputField(
            code = code,
            onCodeChange = { value -> code = value },
            length = OTP_LENGTH,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (remainingSeconds > 0) {
            Text(
                text = "Renvoyer le code dans 0:${remainingSeconds.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                color = FormulooOnSurfaceVariant,
            )
        } else {
            TextButton(
                onClick = {
                    remainingSeconds = RESEND_DELAY_SECONDS
                    code = ""
                    viewModel.resend(email)
                },
            ) {
                Text("Renvoyer le code", color = FormulooPrimary)
            }
        }

        Spacer(Modifier.height(24.dp))

        FormulooButton(
            text = "Vérifier le code",
            enabled = code.length == OTP_LENGTH && !isVerifying,
            loading = isVerifying,
            onClick = { viewModel.verify(code) },
        )
    }
}
