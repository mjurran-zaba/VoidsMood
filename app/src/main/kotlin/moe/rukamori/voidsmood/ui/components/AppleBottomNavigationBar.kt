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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.TextMuted

// Definisi 4 Tab Navigasi Utama VoidsMood
enum class NavigationTab(val id: String, val title: String) {
    HOME("home", "Beranda"),
    EXPLORE("explore", "Eksplorasi"),
    SUBSCRIPTIONS("subscriptions", "Langganan"),
    LIBRARY("library", "Pustaka")
}

// BARU: Floating Apple-Style Capsule Navigation Toolbar (Terinspirasi dari AppleMusicNavigationToolbar VoidTune)
// Mematuhi standar anti-slop: tanpa dots ornamen palsu, desain kapsul 50% murni fungsional, kontras tinggi.
@Composable
fun AppleBottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val capsuleShape = remember { RoundedCornerShape(percent = 50) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(64.dp)
                .clip(capsuleShape)
                .background(Color(0xFF090C12).copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), capsuleShape)
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabScale"
                    )

                    val tintColor by animateColorAsState(
                        targetValue = if (isSelected) IceWhite else TextMuted,
                        label = "tabTint"
                    )

                    val backgroundPillColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
                        label = "pillBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(capsuleShape)
                            .background(backgroundPillColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        ) {
                            when (tab) {
                                NavigationTab.HOME -> LucideHome(tint = tintColor, size = 20.dp)
                                NavigationTab.EXPLORE -> LucideCompass(tint = tintColor, size = 20.dp)
                                NavigationTab.SUBSCRIPTIONS -> LucideSubscriptions(tint = tintColor, size = 20.dp)
                                NavigationTab.LIBRARY -> LucideLibrary(tint = tintColor, size = 20.dp)
                            }

                            Text(
                                text = tab.title,
                                color = tintColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
