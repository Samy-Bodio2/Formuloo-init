package com.formuloo.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Illustrations decoratives pour l'onboarding (3 slides).
 * Compositions geometriques au Canvas, dans la palette Formuloo.
 */

@Composable
fun OnboardingIllustrationCompany(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FormulooMint),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val w = size.width
            val h = size.height
            rotate(45f, pivot = Offset(w * 0.55f, h * 0.55f)) {
                drawRoundRect(
                    color = FormulooPrimary.copy(alpha = 0.2f),
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.6f, h * 0.6f),
                    cornerRadius = CornerRadius(16f),
                )
            }
            rotate(45f, pivot = Offset(w * 0.45f, h * 0.45f)) {
                drawRoundRect(
                    color = FormulooPrimary.copy(alpha = 0.5f),
                    topLeft = Offset(w * 0.2f, h * 0.2f),
                    size = Size(w * 0.5f, h * 0.5f),
                    cornerRadius = CornerRadius(16f),
                )
            }
            rotate(45f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                drawRoundRect(
                    color = FormulooPrimary,
                    topLeft = Offset(w * 0.3f, h * 0.3f),
                    size = Size(w * 0.4f, h * 0.4f),
                    cornerRadius = CornerRadius(12f),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(FormulooSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(FormulooSecondary),
            contentAlignment = Alignment.Center,
        ) {
            FolderIcon(tint = Color.White)
        }
    }
}

@Composable
fun OnboardingIllustrationAnalytics(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FormulooMint),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val w = size.width
            val h = size.height
            val bars = listOf(0.3f, 0.5f, 0.4f, 0.75f, 0.55f, 0.9f)
            val barWidth = w * 0.1f
            val gap = w * 0.05f
            bars.forEachIndexed { i, frac ->
                val x = w * 0.05f + i * (barWidth + gap)
                drawRoundRect(
                    color = if (i == bars.lastIndex) FormulooSecondary else FormulooPrimary.copy(alpha = 0.25f + 0.1f * i),
                    topLeft = Offset(x, h * (1f - frac)),
                    size = Size(barWidth, h * frac),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }
            val trend = Path().apply {
                moveTo(w * 0.08f, h * 0.75f)
                lineTo(w * 0.28f, h * 0.5f)
                lineTo(w * 0.48f, h * 0.6f)
                lineTo(w * 0.68f, h * 0.25f)
                lineTo(w * 0.92f, h * 0.1f)
            }
            drawPath(
                trend,
                color = FormulooPrimary,
                style = Stroke(width = w * 0.018f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * Coche de succes animee (cercle puis check, dessines progressivement sur 1s).
 * Utilisee sur l'ecran de confirmation d'inscription.
 */
@Composable
fun SuccessCheckmarkIllustration(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val circleProgress = (progress.value / 0.6f).coerceIn(0f, 1f)
        drawArc(
            color = FormulooPrimary,
            startAngle = -90f,
            sweepAngle = 360f * circleProgress,
            useCenter = false,
            topLeft = Offset(w * 0.05f, h * 0.05f),
            size = Size(w * 0.9f, h * 0.9f),
            style = stroke,
        )

        val checkProgress = ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
        if (checkProgress > 0f) {
            val checkPath = Path().apply {
                moveTo(w * 0.28f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.68f)
                lineTo(w * 0.74f, h * 0.34f)
            }
            val measure = PathMeasure().apply { setPath(checkPath, false) }
            val animatedPath = Path()
            measure.getSegment(0f, measure.length * checkProgress, animatedPath, true)
            drawPath(animatedPath, color = FormulooPrimary, style = stroke)
        }
    }
}

@Composable
fun OnboardingIllustrationTeam(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FormulooMint),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val w = size.width
            val h = size.height
            val centers = listOf(
                Offset(w * 0.5f, h * 0.25f),
                Offset(w * 0.22f, h * 0.65f),
                Offset(w * 0.78f, h * 0.65f),
            )
            drawLine(FormulooPrimary.copy(alpha = 0.35f), centers[0], centers[1], strokeWidth = w * 0.015f)
            drawLine(FormulooPrimary.copy(alpha = 0.35f), centers[0], centers[2], strokeWidth = w * 0.015f)
            drawLine(FormulooPrimary.copy(alpha = 0.35f), centers[1], centers[2], strokeWidth = w * 0.015f)
            val colors = listOf(FormulooPrimary, FormulooSecondary, FormulooTeal)
            centers.forEachIndexed { i, c ->
                drawCircle(colors[i], radius = w * 0.14f, center = c)
                drawCircle(Color.White, radius = w * 0.06f, center = Offset(c.x, c.y - w * 0.02f))
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(c.x - w * 0.09f, c.y),
                    size = Size(w * 0.18f, w * 0.14f),
                )
            }
        }
    }
}
