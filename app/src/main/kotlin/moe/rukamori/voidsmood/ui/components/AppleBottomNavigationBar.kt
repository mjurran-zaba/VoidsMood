// meticulous and honest
package moe.rukamori.voidsmood.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.TextMuted

// Definisi 4 Tab Navigasi Utama VoidsMood
enum class NavigationTab(val id: String) {
    HOME("home"),
    EXPLORE("explore"),
    SUBSCRIPTIONS("subscriptions"),
    LIBRARY("library")
}

// BARU: Floating Apple-Style Navigation Bar Plek VoidTune (AppleMusicNavigationToolbar)
// Satu kapsul tunggal murni, sliding indicator fisik berbasis drawWithCache, TANPA LABEL TEKS (ikon murni 100%)
@Composable
fun AppleBottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { NavigationTab.entries }
    val capsuleShape = remember { RoundedCornerShape(percent = 50) }
    val selectedIndex = tabs.indexOf(currentTab)

    // Animasi pergerakan indikator sliding pill ala Apple Music VoidTune
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "appleIndicatorPos"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(capsuleShape)
                .background(Color(0xFF0A0D14).copy(alpha = 0.94f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), capsuleShape)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val itemWidth = size.width / tabs.size
                        val radius = CornerRadius(size.height / 2f)
                        onDrawBehind {
                            val left = indicatorPosition * itemWidth
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.12f),
                                topLeft = Offset(left + 2.dp.toPx(), 2.dp.toPx()),
                                size = Size(itemWidth - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                cornerRadius = radius
                            )
                        }
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == currentTab

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.10f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabScale"
                    )

                    val tintColor by animateColorAsState(
                        targetValue = if (isSelected) IceWhite else TextMuted.copy(alpha = 0.65f),
                        label = "tabTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(capsuleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            when (tab) {
                                NavigationTab.HOME -> LucideHome(tint = tintColor, size = 22.dp)
                                NavigationTab.EXPLORE -> LucideCompass(tint = tintColor, size = 22.dp)
                                NavigationTab.SUBSCRIPTIONS -> LucideSubscriptions(tint = tintColor, size = 22.dp)
                                NavigationTab.LIBRARY -> LucideLibrary(tint = tintColor, size = 22.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
