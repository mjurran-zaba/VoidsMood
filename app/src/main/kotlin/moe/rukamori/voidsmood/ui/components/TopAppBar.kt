// meticulous and honest
package moe.rukamori.voidsmood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rukamori.voidsmood.ui.theme.GlassBorderSubtle
import moe.rukamori.voidsmood.ui.theme.GlassSurface35
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan

@Composable
fun VoidsTopAppBar(
    onMenuClick: () -> Unit,
    onSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassSurface35)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Drawer Toggle with Lucide Menu Icon (Touch target >= 48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMenuClick),
                contentAlignment = Alignment.Center
            ) {
                LucideMenu(
                    tint = IceWhite,
                    size = 22.dp
                )
            }

            if (isSearchActive) {
                // Search Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari di YouTube...",
                            color = Color(0xFF6E7487),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = IceWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(PolarCyan),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            onSearch(searchQuery)
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // VoidsMood Brand Monolith
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DINONAKTIFKAN: Titik dekoratif cyan 8.dp di samping brand (dilarang oleh aturan anti-slop)
                    // Box(
                    //     modifier = Modifier
                    //         .size(8.dp)
                    //         .clip(CircleShape)
                    //         .background(PolarCyan)
                    // )
                    // BARU: Tipografi brand bersih dan kokoh tanpa dot dekoratif
                    Text(
                        text = "VoidsMood",
                        color = IceWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Search Toggle Button (Touch target >= 48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isSearchActive) {
                            if (searchQuery.isNotBlank()) {
                                onSearch(searchQuery)
                            } else {
                                isSearchActive = false
                            }
                        } else {
                            isSearchActive = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                LucideSearch(
                    tint = if (isSearchActive) PolarCyan else IceWhite,
                    size = 20.dp
                )
            }
        }
    }
}
