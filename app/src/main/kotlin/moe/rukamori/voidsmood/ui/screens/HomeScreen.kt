// meticulous and honest
// READ AND IMPLEMENT AGENTS.md BEFORE YOU DO
package moe.rukamori.voidsmood.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.ui.components.GlassmorphicCard
import moe.rukamori.voidsmood.ui.components.LucideShorts
import moe.rukamori.voidsmood.ui.components.VideoCard
import moe.rukamori.voidsmood.ui.theme.GlassBorder
import moe.rukamori.voidsmood.ui.theme.GlassBorderSubtle
import moe.rukamori.voidsmood.ui.theme.GlassSurface
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted
import moe.rukamori.voidsmood.ui.viewmodel.HomeUiState
import moe.rukamori.voidsmood.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Pull-to-Refresh native Material 3: tarik ke bawah untuk refresh konten baru
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshTrending() },
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingFeedView()
            }
            is HomeUiState.Error -> {
                ErrorFeedView(
                    message = state.message,
                    onRetry = { viewModel.loadTrending() }
                )
            }
            is HomeUiState.Success -> {
                SuccessFeedView(
                    videos = state.videos,
                    isSearching = state.isSearching,
                    query = state.query,
                    viewModel = viewModel,
                    onVideoClick = onVideoClick,
                    onClearSearch = { viewModel.loadTrending() }
                )
            }
        }
    }
}

// BARU: SuccessFeedView tanpa tulisan 'Sedang Tren' (murni feed video langsung mengalir dari atas)
// Dilengkapi deteksi scroll untuk infinite scrolling pagination berkelanjutan
@Composable
private fun SuccessFeedView(
    videos: List<VideoItem>,
    isSearching: Boolean,
    query: String,
    viewModel: HomeViewModel,
    onVideoClick: (VideoItem) -> Unit,
    onClearSearch: () -> Unit
) {
    val listState = rememberLazyListState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val homeShorts by viewModel.homeShorts.collectAsState()

    var selectedFilter by remember { mutableStateOf("Semua") }
    val filterOptions = listOf("Semua", "Video", "Shorts")

    val categoryTopics = remember {
        listOf("Semua", "Musik", "Gaming", "Podcast", "Sains & Teknologi", "Berita", "Animasi", "Otomotif")
    }

    val displayedVideos = remember(videos, isSearching, selectedFilter) {
        if (!isSearching) videos
        else {
            when (selectedFilter) {
                "Video" -> videos.filter { it.durationSeconds > 90L || it.durationSeconds <= 0L }
                "Shorts" -> videos.filter { it.durationSeconds in 1..90L }
                else -> videos
            }
        }
    }

    // DINONAKTIFKAN: LaunchedEffect(shouldLoadMore) yang terkunci (deadlock) jika sempat gagal atau pengguna tetap di dasar list
    // val shouldLoadMore by remember {
    //     derivedStateOf {
    //         val totalItems = listState.layoutInfo.totalItemsCount
    //         val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    //         totalItems > 0 && lastVisibleItem >= totalItems - 4
    //     }
    // }
    // LaunchedEffect(shouldLoadMore) {
    //     if (shouldLoadMore && !isLoadingMore) {
    //         viewModel.loadMore()
    //     }
    // }
    // BARU: snapshotFlow dinamis yang memantau interaksi scroll di dekat ujung daftar.
    // Tidak akan macet jika terjadi transient delay; setiap kali pengguna menggulir mendekati akhir,
    // sistem akan otomatis mencoba memuat video berikutnya tanpa harus scroll balik ke atas untuk refresh.
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearEnd = total > 0 && lastIndex >= (total - 5)
            isNearEnd to listState.firstVisibleItemScrollOffset
        }.collect { (isNearEnd, _) ->
            if (isNearEnd && !viewModel.isLoadingMore.value) {
                viewModel.loadMore()
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // BARU: Bilah Kapsul Topik Kategori di Beranda (Hanya tampil saat mode beranda normal / tidak searching)
        if (!isSearching) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryTopics) { topic ->
                        val isSelected = selectedTopic == topic
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) PolarCyan else Color(0xFF1B1E29))
                                .border(1.dp, if (isSelected) PolarCyan else GlassBorder, RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectTopic(topic) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = topic,
                                color = if (isSelected) PureBlack else IceWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Hanya tampilkan baris pencarian jika sedang dalam mode search
        if (isSearching) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hasil: \"$query\"",
                            color = IceWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E222D))
                                .clickable(onClick = onClearSearch)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Kembali ke Beranda",
                                color = PolarCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // BARU: Bilah Kapsul Filter (Semua, Video, Shorts) tanpa emoji
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterOptions.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PolarCyan else Color(0xFF1B1E29))
                                    .border(1.dp, if (isSelected) PolarCyan else GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) PureBlack else IceWhite,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (displayedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada video ditemukan.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (!isSearching && homeShorts.isNotEmpty() && displayedVideos.size >= 4) {
            val firstBatch = displayedVideos.take(4)
            val remainingBatch = displayedVideos.drop(4)

            items(
                items = firstBatch,
                key = { it.id }
            ) { video ->
                VideoCard(
                    video = video,
                    onVideoClick = onVideoClick
                )
            }

            // BARU: Rak Shorts Interleaved di tengah Home feed (Jembatan Retensi Doomscrolling YouTube)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LucideShorts(
                            tint = Color(0xFFFF0033),
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shorts",
                            color = IceWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = homeShorts,
                            key = { "shelf_${it.id}" }
                        ) { shortItem ->
                            ShortsShelfCard(
                                short = shortItem,
                                onClick = { onVideoClick(shortItem) }
                            )
                        }
                    }
                }
            }

            items(
                items = remainingBatch,
                key = { it.id }
            ) { video ->
                VideoCard(
                    video = video,
                    onVideoClick = onVideoClick
                )
            }
        } else {
            items(
                items = displayedVideos,
                key = { it.id }
            ) { video ->
                VideoCard(
                    video = video,
                    onVideoClick = onVideoClick
                )
            }
        }

        // Indikator memuat video tambahan saat infinite scrolling
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PolarCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Bottom Spacer untuk clearance Floating AppleBottomNavigationBar & MiniPlayerBar
        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}

@Composable
private fun LoadingFeedView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color(0xFF13141C))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1C1D28))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1C1D28))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1C1D28))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorFeedView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Gagal Menghubungkan ke YouTube",
                    color = IceWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PolarCyan)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Coba Lagi",
                        color = PureBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// BARU: Kartu Shorts Vertikal 9:16 untuk Rak Shorts di Beranda (Doomscrolling Interleaving)
@Composable
private fun ShortsShelfCard(
    short: VideoItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(235.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141721))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = short.thumbnailUrl,
            contentDescription = short.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Vignette gradasi hitam di bawah untuk keterbacaan teks judul
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = short.title,
                color = IceWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            if (short.viewCount > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${VideoItem.formatCount(short.viewCount)} tayangan",
                    color = IceWhite.copy(alpha = 0.70f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
