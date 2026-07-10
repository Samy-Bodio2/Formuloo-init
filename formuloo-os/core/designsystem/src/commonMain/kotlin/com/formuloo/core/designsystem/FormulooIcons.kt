package com.formuloo.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icones dessinees au Canvas, en complement de material-icons-core (qui ne
 * fournit pas Visibility, Fingerprint, Business, People, Calculate,
 * TrendingUp, Dashboard, AccountTree, Inventory, etc.).
 */
private val DefaultIconSize = 24.dp

@Composable
fun VisibilityIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val eyePath = Path().apply {
            moveTo(w * 0.05f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.1f, w * 0.95f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.9f, w * 0.05f, h * 0.5f)
            close()
        }
        drawPath(eyePath, color = tint, style = Stroke(width = w * 0.07f))
        drawCircle(color = tint, radius = w * 0.13f, center = Offset(w / 2f, h / 2f))
    }
}

@Composable
fun VisibilityOffIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val eyePath = Path().apply {
            moveTo(w * 0.05f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.1f, w * 0.95f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.9f, w * 0.05f, h * 0.5f)
            close()
        }
        drawPath(eyePath, color = tint, style = Stroke(width = w * 0.07f))
        drawCircle(color = tint, radius = w * 0.13f, center = Offset(w / 2f, h / 2f))
        drawLine(
            color = tint,
            start = Offset(w * 0.08f, h * 0.08f),
            end = Offset(w * 0.92f, h * 0.92f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun FingerprintIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.42f)
        val stroke = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        for (i in 0..2) {
            val r = w * (0.18f + i * 0.13f)
            drawArc(
                color = tint,
                startAngle = 200f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = stroke,
            )
        }
        drawLine(
            color = tint,
            start = Offset(center.x, center.y + w * 0.1f),
            end = Offset(center.x, center.y + w * 0.46f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun BusinessIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.12f),
            size = Size(w * 0.7f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.04f),
            style = Stroke(width = w * 0.07f),
        )
        for (r in 0 until 3) {
            for (c in 0 until 2) {
                drawRect(
                    color = tint,
                    topLeft = Offset(w * (0.26f + c * 0.28f), h * (0.24f + r * 0.2f)),
                    size = Size(w * 0.12f, h * 0.12f),
                )
            }
        }
    }
}

@Composable
fun PeopleIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        drawCircle(tint.copy(alpha = 0.45f), radius = w * 0.14f, center = Offset(w * 0.68f, h * 0.32f))
        drawArc(
            color = tint.copy(alpha = 0.45f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.48f, h * 0.48f),
            size = Size(w * 0.4f, h * 0.36f),
        )
        drawCircle(tint, radius = w * 0.17f, center = Offset(w * 0.36f, h * 0.34f))
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.12f, h * 0.5f),
            size = Size(w * 0.48f, h * 0.4f),
        )
    }
}

@Composable
fun CalculateIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.08f),
            size = Size(w * 0.64f, h * 0.84f),
            cornerRadius = CornerRadius(w * 0.06f),
            style = Stroke(width = w * 0.07f),
        )
        drawRect(tint, topLeft = Offset(w * 0.27f, h * 0.18f), size = Size(w * 0.46f, h * 0.16f))
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                drawCircle(
                    color = tint,
                    radius = w * 0.045f,
                    center = Offset(w * (0.32f + c * 0.18f), h * (0.52f + r * 0.15f)),
                )
            }
        }
    }
}

@Composable
fun TrendingUpIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val line = Path().apply {
            moveTo(w * 0.08f, h * 0.78f)
            lineTo(w * 0.36f, h * 0.5f)
            lineTo(w * 0.54f, h * 0.66f)
            lineTo(w * 0.92f, h * 0.22f)
        }
        drawPath(line, color = tint, style = stroke)
        val arrow = Path().apply {
            moveTo(w * 0.66f, h * 0.22f)
            lineTo(w * 0.92f, h * 0.22f)
            lineTo(w * 0.92f, h * 0.48f)
        }
        drawPath(arrow, color = tint, style = stroke)
    }
}

@Composable
fun DashboardIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val cell = w * 0.38f
        val gap = w * 0.08f
        val corner = CornerRadius(w * 0.06f)
        listOf(
            Offset(w * 0.08f, h * 0.08f),
            Offset(w * 0.08f + cell + gap, h * 0.08f),
            Offset(w * 0.08f, h * 0.08f + cell + gap),
            Offset(w * 0.08f + cell + gap, h * 0.08f + cell + gap),
        ).forEach { topLeft ->
            drawRoundRect(color = tint, topLeft = topLeft, size = Size(cell, cell), cornerRadius = corner)
        }
    }
}

@Composable
fun ProjectsIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val barHeight = h * 0.14f
        val corner = CornerRadius(barHeight / 2)
        listOf(
            Triple(0.08f, 0.16f, 0.55f),
            Triple(0.08f, 0.42f, 0.85f),
            Triple(0.08f, 0.68f, 0.4f),
        ).forEach { (x, y, lenFraction) ->
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * x, h * y),
                size = Size(w * lenFraction, barHeight),
                cornerRadius = corner,
            )
        }
    }
}

@Composable
fun InventoryIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.07f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.1f, h * 0.28f),
            size = Size(w * 0.8f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.04f),
            style = stroke,
        )
        drawLine(tint, Offset(w * 0.1f, h * 0.46f), Offset(w * 0.9f, h * 0.46f), strokeWidth = w * 0.06f)
        val flap = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.16f, h * 0.28f)
            lineTo(w * 0.84f, h * 0.28f)
            close()
        }
        drawPath(flap, color = tint, style = stroke)
    }
}

@Composable
fun TargetIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        drawCircle(tint, radius = w * 0.42f, center = center, style = Stroke(width = w * 0.07f))
        drawCircle(tint, radius = w * 0.26f, center = center, style = Stroke(width = w * 0.07f))
        drawCircle(tint, radius = w * 0.09f, center = center)
    }
}

@Composable
fun FolderIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.24f)
            lineTo(w * 0.4f, h * 0.24f)
            lineTo(w * 0.48f, h * 0.36f)
            lineTo(w * 0.9f, h * 0.36f)
            lineTo(w * 0.9f, h * 0.82f)
            lineTo(w * 0.1f, h * 0.82f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun KeycloakIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF4D4D4D)) {
    Canvas(modifier = modifier.size(DefaultIconSize)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.32f, h * 0.32f), style = stroke)
        drawLine(tint, Offset(w * 0.44f, h * 0.44f), Offset(w * 0.85f, h * 0.85f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.7f, h * 0.7f), Offset(w * 0.82f, h * 0.58f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.78f), Offset(w * 0.9f, h * 0.66f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
    }
}

/** Logo Formuloo : double anneau orange/vert. */
@Composable
fun FormulooLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.16f
        drawCircle(
            color = FormulooSecondary,
            radius = w * 0.42f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = FormulooPrimary,
            radius = w * 0.42f,
            center = Offset(w * 0.58f, h * 0.58f),
            style = Stroke(width = strokeWidth),
        )
    }
}
