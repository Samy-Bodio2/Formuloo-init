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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.OnboardingIllustrationAnalytics
import com.formuloo.core.designsystem.OnboardingIllustrationCompany
import com.formuloo.core.designsystem.OnboardingIllustrationTeam
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val illustration: @Composable () -> Unit,
    val titleLine1: String,
    val titleLine2: String,
    val description: String,
)

private val onboardingSlides = listOf(
    OnboardingSlide(
        illustration = { OnboardingIllustrationCompany() },
        titleLine1 = "Toute votre entreprise,",
        titleLine2 = "en poche",
        description = "RH, Comptabilité, CRM, Stock, Projets et Analytics réunis dans une seule application native.",
    ),
    OnboardingSlide(
        illustration = { OnboardingIllustrationAnalytics() },
        titleLine1 = "Gérez tout,",
        titleLine2 = "en temps réel",
        description = "Suivez vos indicateurs clés et prenez des décisions éclairées, où que vous soyez.",
    ),
    OnboardingSlide(
        illustration = { OnboardingIllustrationTeam() },
        titleLine1 = "Votre équipe,",
        titleLine2 = "connectée",
        description = "Collaborez en temps réel avec tous les services de votre entreprise.",
    ),
)

@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(FormulooBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onOnboardingComplete) {
                Text("Passer", color = FormulooOnSurfaceVariant)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val slide = onboardingSlides[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                slide.illustration()
                Spacer(Modifier.height(32.dp))
                Text(
                    text = slide.titleLine1,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = slide.titleLine2,
                    style = MaterialTheme.typography.displayLarge,
                    color = FormulooPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(onboardingSlides.size) { index ->
                val isActive = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (isActive) 24.dp else 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) FormulooPrimary else FormulooOutline),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            val isLastPage = pagerState.currentPage == onboardingSlides.lastIndex
            FormulooButton(
                text = if (isLastPage) "Commencer" else "Suivant",
                onClick = {
                    if (isLastPage) {
                        onOnboardingComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )
        }
    }
}
