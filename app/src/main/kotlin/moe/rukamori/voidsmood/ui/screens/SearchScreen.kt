// meticulous and honest
package moe.rukamori.voidsmood.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import moe.rukamori.voidsmood.data.repository.LocalLibraryRepository
import moe.rukamori.voidsmood.data.repository.YouTubeRepository
import moe.rukamori.voidsmood.ui.components.LucideArrowLeft
import moe.rukamori.voidsmood.ui.components.LucideArrowUpLeft
import moe.rukamori.voidsmood.ui.components.LucideHistory
import moe.rukamori.voidsmood.ui.components.LucideSearch
import moe.rukamori.voidsmood.ui.components.LucideX
import moe.rukamori.voidsmood.ui.theme.GlassBorder
import moe.rukamori.voidsmood.ui.theme.GlassBorderSubtle
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted

// BARU: Layer Pencarian Penuh YouTube Style dengan riwayat penelusuran lokal permanen,
// penyorotan teks kelanjutan tebal (highlight bolding), dan tombol salin ↖ tanpa emoji
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onClose: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = ""
) {
    var query by remember { mutableStateOf(initialQuery) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchHistory by LocalLibraryRepository.searchHistory.collectAsState()

    // Tangani gesture / tombol back Android agar layer pencarian menutup dengan halus
    BackHandler(onBack = onClose)

    // Fokuskan input pencarian dan munculkan keyboard secara otomatis saat layer dibuka
    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Ambil saran pencarian real-time dari YouTube (debounced 120ms)
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(120)
            suggestions = YouTubeRepository.getSearchSuggestions(query)
        } else {
            suggestions = emptyList()
        }
    }

    val trendingTopics = remember {
        listOf(
            "Musik Indonesia Populer",
            "Video Viral",
            "Trending Gaming",
            "Podcast Indonesia",
            "Berita Terkini",
            "Gameplay Terbaru",
            "Film Pendek",
            "Teknologi dan Gadget",
            "Sains dan Edukasi",
            "Otomotif"
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
                        val trimmed = query.trim()
                        if (trimmed.isNotBlank()) {
                            LocalLibraryRepository.saveSearchQuery(trimmed)
                            keyboardController?.hide()
                            onSearch(trimmed)
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

        // FASE 3: Konten Hasil Saran Pencarian (Suggestions) saat pengguna sedang mengetik
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
                                LocalLibraryRepository.saveSearchQuery(suggestionText)
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

                        // Penyorotan Teks: Bagian yang diketik redup, kelanjutan saran dicetak tebal
                        HighlightedSuggestionText(
                            fullText = suggestionText,
                            query = query,
                            modifier = Modifier.weight(1f)
                        )

                        // Tombol Panah Serong (Menyalin saran ke dalam teks input tanpa submit)
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
        } else if (query.isEmpty() && searchHistory.isNotEmpty()) {
            // FASE 2A: Tampilan Status Idle saat belum mengetik - Menampilkan Riwayat Penelusuran Terakhir
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Penelusuran",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Hapus Semua",
                            color = PolarCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                LocalLibraryRepository.clearSearchHistory()
                            }
                        )
                    }
                }

                items(
                    items = searchHistory,
                    key = { it }
                ) { historyText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LocalLibraryRepository.saveSearchQuery(historyText)
                                keyboardController?.hide()
                                onSearch(historyText)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LucideHistory(
                            tint = Color(0xFF7E8496),
                            size = 18.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = historyText,
                            color = IceWhite,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Tombol Hapus item spesifik
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    LocalLibraryRepository.deleteSearchQuery(historyText)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideX(
                                tint = Color(0xFF7E8496),
                                size = 16.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Tombol Panah Serong (Salin ke kotak input)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    query = historyText
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideArrowUpLeft(
                                tint = Color(0xFF7E8496),
                                size = 18.dp
                            )
                        }
                    }
                }
            }
        } else {
            // DINONAKTIFKAN: Tampilan deretan chip topik populer saat riwayat kosong
            // Column(
            //     modifier = Modifier
            //         .fillMaxSize()
            //         .padding(16.dp)
            // ) {
            //     Text("Topik Populer", ...)
            //     FlowRow(...) { ... }
            // }
            // BARU: Tipografi bersih minimalis di tengah layar 'FIND YOUR MOOD' tanpa tab/kartu/emoji.
            // Muncul saat riwayat kosong dan query kosong. Otomatis hilang saat mengetik atau memiliki riwayat.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "FIND YOUR MOOD",
                        color = IceWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Telusuri kreator, musik, atau topik yang Anda inginkan",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// BARU: Penyorotan teks cerdas (Highlighting)
// Kata kunci yang diketik pengguna ditampilkan redup, sementara prediksi kelanjutan kata dicetak tebal
@Composable
private fun HighlightedSuggestionText(
    fullText: String,
    query: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(fullText, query) {
        val q = query.trim()
        val index = if (q.isNotEmpty()) fullText.indexOf(q, ignoreCase = true) else -1
        buildAnnotatedString {
            if (index >= 0) {
                val prefix = fullText.substring(0, index)
                val match = fullText.substring(index, index + q.length)
                val suffix = fullText.substring(index + q.length)

                if (prefix.isNotEmpty()) {
                    withStyle(SpanStyle(color = IceWhite.copy(alpha = 0.60f), fontWeight = FontWeight.Normal)) {
                        append(prefix)
                    }
                }
                withStyle(SpanStyle(color = IceWhite.copy(alpha = 0.60f), fontWeight = FontWeight.Normal)) {
                    append(match)
                }
                if (suffix.isNotEmpty()) {
                    withStyle(SpanStyle(color = IceWhite, fontWeight = FontWeight.Bold)) {
                        append(suffix)
                    }
                }
            } else {
                withStyle(SpanStyle(color = IceWhite, fontWeight = FontWeight.Normal)) {
                    append(fullText)
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontSize = 15.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

