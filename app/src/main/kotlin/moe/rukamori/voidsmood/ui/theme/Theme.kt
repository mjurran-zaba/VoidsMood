// meticulous and honest
package moe.rukamori.voidsmood.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PolarCyan,
    onPrimary = PureBlack,
    primaryContainer = PolarCyanDim,
    onPrimaryContainer = IceWhite,
    secondary = DarkBlueOcean,
    onSecondary = IceWhite,
    background = PureBlack,
    onBackground = IceWhite,
    surface = DarkSurface,
    onSurface = IceWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
)

@Composable
fun VoidsMoodTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = {
            // Auramorphism Mesh Backdrop: Faded grey charcoal 45% & Dark blue ocean 50%
            // Low GPU overhead radial/linear gradients optimized for ARMv7
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DarkBlueOceanAura,
                                FadedGreyCharcoalAura,
                                Color.Transparent
                            ),
                            center = Offset(200f, 300f),
                            radius = 1200f
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DeepOceanAbyss.copy(alpha = 0.35f),
                                PureBlack
                            )
                        )
                    )
            ) {
                content()
            }
        }
    )
}
