// meticulous and honest
package moe.rukamori.voidsmood.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import moe.rukamori.voidsmood.data.repository.YouTubeRepository
import moe.rukamori.voidsmood.ui.components.LucideArrowLeft
import moe.rukamori.voidsmood.ui.components.LucideArrowUpLeft
import moe.rukamori.voidsmood.ui.components.LucideSearch
import moe.rukamori.voidsmood.ui.components.LucideX
import moe.rukamori.voidsmood.ui.theme.GlassBorder
import moe.rukamori.voidsmood.ui.theme.GlassBorderSubtle
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted

// BARU: Layer Pencarian Penuh YouTube Style dengan animasi slide-to-left mulus
// Menyediakan live search suggestions real-time dari YouTube saat pengguna mengetik
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onClose: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Tangani gesture / tombol back Android agar layer pencarian menutup dengan halus
    BackHandler(onBack = onClose)

    // Fokuskan input pencarian dan munculkan keyboard secara otomatis saat layer dibuka
    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Ambil saran pencarian real-time dari YouTube (debounced 100ms)
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(100)
            suggestions = YouTubeRepository.getSearchSuggestions(query)
        } else {
            suggestions = emptyList()
        }
    }

    val trendingTopics = remember {
        listOf(
            "Video Viral Indonesia",
            "Musik Indonesia Populer",
            "Trending Gaming",
            "Podcast Indonesia",
            "Berita Hari Ini",
            "Gameplay Terbaru",
            "Film Pendek",
            "Komedi Lucu"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Safe inset status bar
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // Baris Input Pencarian Atas (YouTube style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Kembali
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        keyboardController?.hide()
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                LucideArrowLeft(
                    tint = IceWhite,
                    size = 22.dp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Kotak Input Pencarian
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color(0xFF1B1E29))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(21.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Telusuri YouTube...",
                        color = Color(0xFF7A8096),
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = IceWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(PolarCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) {
                            keyboardController?.hide()
                            onSearch(query.trim())
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }

            // Tombol Hapus Teks (tampil saat query tidak kosong)
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { query = "" },
                    contentAlignment = Alignment.Center
                ) {
                    LucideX(
                        tint = IceWhite,
                        size = 20.dp
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // Garis Pembatas Halus
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1E212E))
        )

        // Konten Hasil Saran Pencarian (Suggestions) atau Topik Tren Populer
        if (query.isNotBlank() && suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = suggestions,
                    key = { it }
                ) { suggestionText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keyboardController?.hide()
                                onSearch(suggestionText)
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LucideSearch(
                            tint = Color(0xFF868C9F),
                            size = 18.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = suggestionText,
                            color = IceWhite,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Tombol Panah Diagonal (Menyisipkan teks saran ke dalam field input seperti YouTube)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    query = suggestionText
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideArrowUpLeft(
                                tint = Color(0xFF868C9F),
                                size = 18.dp
                            )
                        }
                    }
                }
            }
        } else {
            // Tampilan saat belum ada teks pencarian: Topik Populer yang sering dicari
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Topik Populer",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    trendingTopics.forEach { topic ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1A1D27))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    keyboardController?.hide()
                                    onSearch(topic)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = topic,
                                color = IceWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
