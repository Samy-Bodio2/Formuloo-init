package com.formuloo.os

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.auth.domain.model.AuthState
import com.formuloo.core.designsystem.FormulooLogo
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.FormulooVersionGray
import com.formuloo.feature.auth.AuthViewModel
import com.formuloo.feature.auth.OnboardingPreferences
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val LOGO_FADE_IN_MS = 800
private const val SPLASH_DURATION_MS = 2500L

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var startAnimation by remember { mutableStateOf(false) }
    var timerElapsed by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = LOGO_FADE_IN_MS),
        label = "splash-logo-alpha",
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(SPLASH_DURATION_MS)
        timerElapsed = true
    }

    LaunchedEffect(state, timerElapsed) {
        if (!timerElapsed) return@LaunchedEffect
        when (state) {
            is AuthState.LoggedIn -> onNavigateToHome()
            is AuthState.LoggedOut, is AuthState.Error -> {
                if (OnboardingPreferences.isOnboardingDone()) {
                    onNavigateToLogin()
                } else {
                    onNavigateToOnboarding()
                }
            }
            is AuthState.Loading -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(logoAlpha),
        ) {
            FormulooLogo(size = 72.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = FormulooTextPrimary,
                        ),
                    ) {
                        append("Formuloo")
                    }
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FormulooPrimary,
                            baselineShift = BaselineShift.Superscript,
                        ),
                    ) {
                        append("OS")
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = FormulooPrimary,
                trackColor = FormulooOutline,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = FormulooVersionGray,
            )
        }
    }
}
