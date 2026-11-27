// meticulous and honest
package moe.rukamori.voidsmood.ui.drawer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rukamori.voidsmood.ui.components.LucideCompass
import moe.rukamori.voidsmood.ui.components.LucideHistory
import moe.rukamori.voidsmood.ui.components.LucideHome
import moe.rukamori.voidsmood.ui.components.LucideLibrary
import moe.rukamori.voidsmood.ui.components.LucideSettings
import moe.rukamori.voidsmood.ui.components.LucideSubscriptions
import moe.rukamori.voidsmood.ui.theme.DarkBlueOceanAura
import moe.rukamori.voidsmood.ui.theme.DarkSurfaceVariant
import moe.rukamori.voidsmood.ui.theme.FadedGreyCharcoalAura
import moe.rukamori.voidsmood.ui.theme.GlassBorderHighlight
import moe.rukamori.voidsmood.ui.theme.GlassBorderSubtle
import moe.rukamori.voidsmood.ui.theme.GlassSurface35
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted

data class NavDrawerItem(
    val id: String,
    val title: String,
    val icon: @Composable (tint: Color) -> Unit
)

@Composable
fun VoidsDrawerContent(
    selectedId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavDrawerItem("home", "Home") { tint -> LucideHome(tint = tint) },
        NavDrawerItem("explore", "Explore") { tint -> LucideCompass(tint = tint) },
        NavDrawerItem("subscriptions", "Subscriptions") { tint -> LucideSubscriptions(tint = tint) },
        NavDrawerItem("library", "Library") { tint -> LucideLibrary(tint = tint) },
        NavDrawerItem("history", "History") { tint -> LucideHistory(tint = tint) },
        NavDrawerItem("settings", "Settings") { tint -> LucideSettings(tint = tint) }
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .background(PureBlack)
            // Auramorphism Mesh Backdrop: Faded grey charcoal 45% & Dark blue ocean 50%
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DarkBlueOceanAura,
                        FadedGreyCharcoalAura,
                        PureBlack.copy(alpha = 0.90f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1800f)
                )
            )
            .border(
                1.dp,
                GlassBorderSubtle,
                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Drawer Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurface35)
                        .border(1.dp, GlassBorderHighlight, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(PolarCyan)
                    )
                }

                Column {
                    Text(
                        text = "VoidsMood",
                        color = IceWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "The Silent Horizon",
                        color = PolarCyan.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Items List
            navItems.forEach { item ->
                val isSelected = item.id == selectedId
                val itemBg = if (isSelected) GlassSurface35 else Color.Transparent
                val itemBorder = if (isSelected) GlassBorderHighlight else Color.Transparent
                val itemText = if (isSelected) PolarCyan else TextMuted

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(16.dp))
                        .clickable { onItemSelected(item.id) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item.icon(if (isSelected) PolarCyan else TextMuted)
                    Text(
                        text = item.title,
                        color = itemText,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurface35)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "AMOLED • Pure Black • 120Hz",
                    color = TextMuted.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
