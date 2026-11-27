// meticulous and honest
package moe.rukamori.voidsmood.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.repository.YouTubeRepository
import moe.rukamori.voidsmood.ui.components.VideoCard
import moe.rukamori.voidsmood.ui.theme.DarkSurfaceVariant
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted

// BARU: Layar Jelajah / Explore Kategori YouTube (Trending, Musik, Game, Berita, Animasi)
// Mendukung Pull to Refresh, Auto Scroll ke Paling Atas saat Ganti Kategori, dan Infinite Scrolling
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val categories = remember {
        listOf("Trending", "Musik", "Gaming", "Berita", "Animasi")
    }
    var selectedCategory by remember { mutableStateOf("Trending") }

    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var pageOffset by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()

    fun loadCategory(category: String, isRefresh: Boolean = false) {
        selectedCategory = category
        if (!isRefresh) {
            isLoading = true
        }
        errorMessage = null
        pageOffset = 1

        // BARU: Reset scroll ke thumbnails paling atas (indeks 0) saat berganti kategori
        scope.launch {
            listState.scrollToItem(0)
        }

        scope.launch {
            // DINONAKTIFKAN: getExploreVideos lama tanpa parameter refresh sehingga setiap pull-to-refresh memuat query yang sama persis
            // val result = YouTubeRepository.getExploreVideos(category)
            // BARU: getExploreVideos dengan rotasi seed global dinamis setiap kali pull-to-refresh (isRefresh = true)
            val result = YouTubeRepository.getExploreVideos(category, refresh = isRefresh)
            result.onSuccess { list ->
                videos = list
                isLoading = false
                isRefreshing = false
                // Pastikan posisi tetap di atas setelah konten baru dirender
                listState.scrollToItem(0)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Gagal memuat video kategori $category."
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCategory("Trending")
    }

    // BARU: Infinite Scroll untuk memuat lebih banyak thumbnails saat pengguna mendekati akhir daftar
    LaunchedEffect(listState.canScrollForward, videos.size) {
        if (!listState.canScrollForward && videos.isNotEmpty() && !isLoading && !isLoadingMore && !isRefreshing) {
            isLoadingMore = true
            val moreResult = YouTubeRepository.loadMoreExploreVideos(selectedCategory, pageOffset)
            moreResult.onSuccess { newVideos ->
                if (newVideos.isNotEmpty()) {
                    videos = (videos + newVideos).distinctBy { it.id }
                    pageOffset += 1
                }
            }
            isLoadingMore = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Deretan Chip Kategori Horizontal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) IceWhite else DarkSurfaceVariant)
                        .clickable {
                            if (selectedCategory != category) {
                                loadCategory(category)
                            } else {
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) PureBlack else IceWhite,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // BARU: Expressive PullToRefreshBox mengelilingi konten Explore
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadCategory(selectedCategory, isRefresh = true)
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = DarkSurfaceVariant,
                    color = PolarCyan
                )
            }
        ) {
            if (isLoading && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = PolarCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memuat video $selectedCategory...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (errorMessage != null && videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "Koneksi terkendala.",
                            color = IceWhite,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PolarCyan)
                                .clickable { loadCategory(selectedCategory) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Coba Lagi",
                                color = PureBlack,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = videos,
                        key = { it.id }
                    ) { video ->
                        VideoCard(
                            video = video,
                            onVideoClick = onVideoClick
                        )
                    }

                    // Indikator loading saat memuat batch video tambahan
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

                    // Spacer pengaman agar konten terbawah tidak terpotong bottom navigation bar
                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        }
    }
}
