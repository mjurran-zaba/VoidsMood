// meticulous and honest
package moe.rukamori.voidsmood.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Lucide Icon Set - Custom Vector Composables
// Avoids heavy icon library dependency footprint on ARMv7 devices

@Composable
fun LucideMenu(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // 3 parallel clean lines (Lucide menu style)
        drawLine(tint, Offset(w * 0.15f, h * 0.28f), Offset(w * 0.85f, h * 0.28f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, h * 0.50f), Offset(w * 0.85f, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, h * 0.72f), Offset(w * 0.85f, h * 0.72f), stroke, StrokeCap.Round)
    }
}

@Composable
fun LucideHome(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.40f)
            lineTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.40f)
            lineTo(w * 0.85f, h * 0.88f)
            lineTo(w * 0.58f, h * 0.88f)
            lineTo(w * 0.58f, h * 0.60f)
            lineTo(w * 0.42f, h * 0.60f)
            lineTo(w * 0.42f, h * 0.88f)
            lineTo(w * 0.15f, h * 0.88f)
            close()
        }
        drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LucideCompass(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // Outer circle
        drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))

        // Compass needle diamond
        val needle = Path().apply {
            moveTo(w * 0.62f, h * 0.38f)
            lineTo(w * 0.45f, h * 0.45f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.55f, h * 0.55f)
            close()
        }
        drawPath(needle, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LucideSubscriptions(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // TV / Screen display
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.30f),
            size = Size(w * 0.76f, h * 0.52f),
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Antenna
        drawLine(tint, Offset(w * 0.35f, h * 0.14f), Offset(w * 0.50f, h * 0.30f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.65f, h * 0.14f), Offset(w * 0.50f, h * 0.30f), stroke, StrokeCap.Round)
    }
}

@Composable
fun LucideLibrary(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // Folder outline
        val folder = Path().apply {
            moveTo(w * 0.12f, h * 0.28f)
            lineTo(w * 0.40f, h * 0.28f)
            lineTo(w * 0.50f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.82f)
            lineTo(w * 0.12f, h * 0.82f)
            close()
        }
        drawPath(folder, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LucideHistory(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
        // Clock hands (hour & minute)
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.28f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), stroke, StrokeCap.Round)
    }
}

@Composable
fun LucideSettings(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))
    }
}
