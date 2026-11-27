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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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

        drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(stroke))

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

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.30f),
            size = Size(w * 0.76f, h * 0.52f),
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(tint, Offset(w * 0.35f, h * 0.14f), Offset(w * 0.50f, h * 0.30f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.65f, h * 0.14f), Offset(w * 0.50f, h * 0.30f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon YouTube Shorts resmi dengan garis sudut tumpul vertikal dan tombol play di tengahnya
@Composable
fun LucideShorts(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.22f, h * 0.12f),
            size = Size(w * 0.56f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val playPath = Path().apply {
            moveTo(w * 0.44f, h * 0.38f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.44f, h * 0.62f)
            close()
        }
        drawPath(playPath, tint)
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
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.28f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Roda Gigi Settings YouTube resmi (lingkaran tengah dengan gerigi roda)
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
        val cx = w * 0.50f
        val cy = h * 0.50f

        // Lingkaran dalam & luar
        drawCircle(tint, radius = w * 0.16f, center = Offset(cx, cy), style = Stroke(stroke))
        drawCircle(tint, radius = w * 0.34f, center = Offset(cx, cy), style = Stroke(stroke))

        // 6 gerigi gear melingkar
        for (i in 0 until 6) {
            val angle = (i * 60) * (Math.PI / 180.0)
            val cos = Math.cos(angle).toFloat()
            val sin = Math.sin(angle).toFloat()
            val rInner = w * 0.32f
            val rOuter = w * 0.44f
            drawLine(
                color = tint,
                start = Offset(cx + cos * rInner, cy + sin * rInner),
                end = Offset(cx + cos * rOuter, cy + sin * rOuter),
                strokeWidth = stroke * 1.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun LucideSearch(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawCircle(
            color = tint,
            radius = w * 0.28f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(stroke)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.85f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// BARU: Ikon Arrow Left (Kembali dari Player ke Feed)
@Composable
fun LucideArrowLeft(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawLine(tint, Offset(w * 0.82f, h * 0.50f), Offset(w * 0.18f, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.26f), Offset(w * 0.18f, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.18f, h * 0.50f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Thumbs Up (Suka)
@Composable
fun LucideThumbsUp(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.85f)
            lineTo(w * 0.75f, h * 0.85f)
            cubicTo(w * 0.82f, h * 0.85f, w * 0.85f, h * 0.80f, w * 0.85f, h * 0.70f)
            lineTo(w * 0.90f, h * 0.48f)
            cubicTo(w * 0.90f, h * 0.40f, w * 0.82f, h * 0.40f, w * 0.75f, h * 0.40f)
            lineTo(w * 0.58f, h * 0.40f)
            lineTo(w * 0.62f, h * 0.18f)
            cubicTo(w * 0.62f, h * 0.12f, w * 0.55f, h * 0.10f, w * 0.50f, h * 0.15f)
            lineTo(w * 0.32f, h * 0.45f)
            close()
        }
        drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.15f, h * 0.45f), Offset(w * 0.15f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.32f, h * 0.45f), Offset(w * 0.32f, h * 0.85f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Share
@Composable
fun LucideShare2(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.75f, h * 0.25f), style = Stroke(stroke))
        drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.75f, h * 0.75f), style = Stroke(stroke))
        drawCircle(tint, radius = w * 0.25f, center = Offset(w * 0.25f, h * 0.50f), style = Stroke(stroke))

        drawLine(tint, Offset(w * 0.35f, h * 0.44f), Offset(w * 0.65f, h * 0.31f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, h * 0.56f), Offset(w * 0.65f, h * 0.69f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Download
@Composable
fun LucideDownload(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawLine(tint, Offset(w * 0.50f, h * 0.15f), Offset(w * 0.50f, h * 0.65f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.30f, h * 0.45f), Offset(w * 0.50f, h * 0.65f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.70f, h * 0.45f), Offset(w * 0.50f, h * 0.65f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.20f, h * 0.85f), Offset(w * 0.80f, h * 0.85f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Maximize (Masuk mode Fullscreen / Landscape)
@Composable
fun LucideMaximize(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // Sudut kiri atas
        drawLine(tint, Offset(w * 0.15f, h * 0.40f), Offset(w * 0.15f, h * 0.15f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, h * 0.15f), Offset(w * 0.40f, h * 0.15f), stroke, StrokeCap.Round)

        // Sudut kanan atas
        drawLine(tint, Offset(w * 0.60f, h * 0.15f), Offset(w * 0.85f, h * 0.15f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.85f, h * 0.15f), Offset(w * 0.85f, h * 0.40f), stroke, StrokeCap.Round)

        // Sudut kiri bawah
        drawLine(tint, Offset(w * 0.15f, h * 0.60f), Offset(w * 0.15f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.40f, h * 0.85f), stroke, StrokeCap.Round)

        // Sudut kanan bawah
        drawLine(tint, Offset(w * 0.60f, h * 0.85f), Offset(w * 0.85f, h * 0.85f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.85f, h * 0.85f), Offset(w * 0.85f, h * 0.60f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Minimize (Keluar dari Fullscreen ke Portrait)
@Composable
fun LucideMinimize(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // Sudut kiri atas menghadap ke dalam
        drawLine(tint, Offset(w * 0.15f, h * 0.35f), Offset(w * 0.35f, h * 0.35f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, h * 0.35f), Offset(w * 0.35f, h * 0.15f), stroke, StrokeCap.Round)

        // Sudut kanan atas menghadap ke dalam
        drawLine(tint, Offset(w * 0.85f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.65f, h * 0.35f), Offset(w * 0.65f, h * 0.15f), stroke, StrokeCap.Round)

        // Sudut kiri bawah menghadap ke dalam
        drawLine(tint, Offset(w * 0.15f, h * 0.65f), Offset(w * 0.35f, h * 0.65f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, h * 0.65f), Offset(w * 0.35f, h * 0.85f), stroke, StrokeCap.Round)

        // Sudut kanan bawah menghadap ke dalam
        drawLine(tint, Offset(w * 0.85f, h * 0.65f), Offset(w * 0.65f, h * 0.65f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.65f, h * 0.65f), Offset(w * 0.65f, h * 0.85f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Check (Tanda centang kualitas resolusi aktif)
@Composable
fun LucideCheck(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawLine(tint, Offset(w * 0.20f, h * 0.52f), Offset(w * 0.42f, h * 0.74f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.82f, h * 0.28f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon X (Hapus teks pada input pencarian)
@Composable
fun LucideX(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon ArrowUpLeft (Panah diagonal ke kiri atas untuk memasukkan saran pencarian ke search box)
@Composable
fun LucideArrowUpLeft(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        drawLine(tint, Offset(w * 0.75f, h * 0.75f), Offset(w * 0.25f, h * 0.25f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.25f, h * 0.55f), Offset(w * 0.25f, h * 0.25f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.55f, h * 0.25f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Play Segitiga YouTube
@Composable
fun LucidePlay(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.22f)
            lineTo(w * 0.78f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.78f)
            close()
        }
        drawPath(path, tint)
    }
}

// BARU: Ikon Pause Dua Garis Vertikal YouTube
@Composable
fun LucidePause(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 28.dp,
    strokeWidth: Dp = 3.5.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()
        drawLine(tint, Offset(w * 0.36f, h * 0.24f), Offset(w * 0.36f, h * 0.76f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, h * 0.24f), Offset(w * 0.64f, h * 0.76f), stroke, StrokeCap.Round)
    }
}

// BARU: Ikon Rewind 10 Detik khas YouTube Mobile
@Composable
fun LucideRotateCcw10(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 36.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // Busur melingkar berlawanan arah jarum jam (270 derajat)
        drawArc(
            color = tint,
            startAngle = 60f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )

        // Kepala panah di ujung busur kiri atas
        val arrow = Path().apply {
            moveTo(w * 0.08f, h * 0.26f)
            lineTo(w * 0.24f, h * 0.14f)
            lineTo(w * 0.24f, h * 0.38f)
            close()
        }
        drawPath(arrow, tint)

        // Angka 10 di tengah busur
        val paint = android.graphics.Paint().apply {
            color = tint.toArgb()
            textSize = h * 0.30f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("10", w * 0.50f, h * 0.60f, paint)
    }
}

// BARU: Ikon Fast Forward 10 Detik khas YouTube Mobile
@Composable
fun LucideRotateCw10(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 36.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()

        // Busur melingkar searah jarum jam (270 derajat)
        drawArc(
            color = tint,
            startAngle = 120f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )

        // Kepala panah di ujung busur kanan atas
        val arrow = Path().apply {
            moveTo(w * 0.92f, h * 0.26f)
            lineTo(w * 0.76f, h * 0.14f)
            lineTo(w * 0.76f, h * 0.38f)
            close()
        }
        drawPath(arrow, tint)

        // Angka 10 di tengah busur
        val paint = android.graphics.Paint().apply {
            color = tint.toArgb()
            textSize = h * 0.30f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("10", w * 0.50f, h * 0.60f, paint)
    }
}
