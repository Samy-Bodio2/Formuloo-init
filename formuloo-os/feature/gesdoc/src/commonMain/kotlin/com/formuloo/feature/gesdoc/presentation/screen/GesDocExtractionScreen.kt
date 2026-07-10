package com.formuloo.feature.gesdoc.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.StepProgressBar
import com.formuloo.feature.gesdoc.presentation.components.GesDocTabBar
import com.formuloo.feature.gesdoc.presentation.viewmodel.ExtractionStepItem
import com.formuloo.feature.gesdoc.presentation.viewmodel.ExtractionStepState
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocExtractionViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesDocExtractionScreen(
    documentId: String,
    onBack: () -> Unit,
    onNavigateToAudit: () -> Unit = {},
    onExtractionComplete: (String) -> Unit,
    viewModel: GesDocExtractionViewModel = koinViewModel(parameters = { parametersOf(documentId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hasAutoAdvanced by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete && !hasAutoAdvanced) {
            hasAutoAdvanced = true
            onExtractionComplete(state.documentId)
        }
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Gestion documentaire",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            "OCR · certification Blockchain",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = FormulooTextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filtrer",
                            tint = FormulooOnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooSurface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GesDocTabBar(
                activeTabIndex = 1,
                onNavigateToDashboard = onBack,
                onNavigateToUpload = {},
                onNavigateToAudit = onNavigateToAudit,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Step progress bar + label
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        StepProgressBar(totalSteps = 4, currentStep = 2)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("Étape ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("2") }
                                append(" sur 4 · Extraction")
                            },
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }

                // Circular spinner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExtractionSpinner()
                    }
                }

                // Title + subtitle
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Extraction en cours...",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = FormulooTextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Le document est traité en arrière-plan.\nAucune action requise — cette page se met à jour automatiquement.",
                            fontSize = 13.sp,
                            color = FormulooOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Processing steps card
                item {
                    ExtractionStepsCard(
                        steps = state.steps,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // Error banner
                if (state.error != null) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(FormulooErrorBg, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = FormulooError,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(state.error!!, fontSize = 13.sp, color = FormulooError)
                        }
                    }
                }
            }
        }
    }
}

// ── Spinner circulaire animé ──────────────────────────────────────────────────

@Composable
private fun ExtractionSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "ocr_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = FormulooMint)
            drawArc(
                color = FormulooPrimary,
                startAngle = rotation - 90f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            tint = FormulooPrimary,
            modifier = Modifier.size(44.dp),
        )
    }
}

// ── Carte des étapes de traitement ────────────────────────────────────────────

@Composable
private fun ExtractionStepsCard(steps: List<ExtractionStepItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FormulooSurface),
    ) {
        steps.forEachIndexed { index, step ->
            ExtractionStepRow(step = step, stepIndex = index)
            if (index < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = FormulooOutline.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                )
            }
        }
    }
}

@Composable
private fun ExtractionStepRow(step: ExtractionStepItem, stepIndex: Int) {
    val stepIcon: ImageVector = when {
        step.state == ExtractionStepState.DONE -> Icons.Filled.Check
        stepIndex == 0 -> Icons.Filled.Image
        stepIndex == 1 -> Icons.Filled.Description
        else -> Icons.Filled.AutoAwesome
    }
    val badgeBg = when (step.state) {
        ExtractionStepState.DONE, ExtractionStepState.IN_PROGRESS -> FormulooMint
        ExtractionStepState.PENDING -> Color(0xFFF0F0F0)
    }
    val iconTint = when (step.state) {
        ExtractionStepState.DONE, ExtractionStepState.IN_PROGRESS -> FormulooPrimary
        ExtractionStepState.PENDING -> FormulooOnSurfaceVariant
    }
    val labelColor = when (step.state) {
        ExtractionStepState.PENDING -> FormulooOnSurfaceVariant
        else -> FormulooTextPrimary
    }
    val labelWeight = if (step.state == ExtractionStepState.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal
    val statusText = when (step.state) {
        ExtractionStepState.DONE -> "Terminé"
        ExtractionStepState.IN_PROGRESS -> "En cours..."
        ExtractionStepState.PENDING -> "En attente"
    }
    val statusColor = when (step.state) {
        ExtractionStepState.DONE -> FormulooOnSurfaceVariant
        ExtractionStepState.IN_PROGRESS -> FormulooPrimary
        ExtractionStepState.PENDING -> FormulooOutline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = stepIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = step.label,
            fontSize = 14.sp,
            fontWeight = labelWeight,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = statusText,
            fontSize = 13.sp,
            color = statusColor,
        )
    }
}
