// meticulous and honest
// READ AND IMPLEMENT AGENTS.md BEFORE YOU DO
package moe.rukamori.voidsmood.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.network.NetworkClient
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import moe.rukamori.voidsmood.data.repository.LocalLibraryRepository
import moe.rukamori.voidsmood.data.repository.YouTubeRepository
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.rukamori.voidsmood.ui.viewmodel.ShortsUiState
import moe.rukamori.voidsmood.ui.viewmodel.ShortsViewModel
import moe.rukamori.voidsmood.ui.components.LucidePause
import moe.rukamori.voidsmood.ui.components.LucidePlay
import moe.rukamori.voidsmood.ui.components.LucideShare2
import moe.rukamori.voidsmood.ui.components.LucideThumbsUp
import moe.rukamori.voidsmood.ui.theme.DarkSurfaceVariant
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted

// BARU: Layar Khusus Video Vertikal YouTube Shorts / Reels (Opsi Pilihan User)
// Menampilkan pengalaman swipe vertikal layar penuh 9:16 dengan Single Shared ExoPlayer, Non-blocking Clickable Gesture,
// dan PullToRefreshBox murni spinner ikon (tanpa teks label)
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    onVideoSelect: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShortsViewModel = viewModel()
) {
    val context = LocalContext.current

    // DINONAKTIFKAN: State lokal remember dan DisposableEffect yang merelease ExoPlayer saat berpindah tab ke Home,
    // yang menyebabkan layar menjadi blank hitam saat pengguna kembali dari tab Home ke Shorts
    // var shortsList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    // var isLoading by remember { mutableStateOf(true) }
    // var isLoadingMore by remember { mutableStateOf(false) }
    // var errorMessage by remember { mutableStateOf<String?>(null) }
    // val sharedExoPlayer = remember { ExoPlayer.Builder(context)... }
    // DisposableEffect(sharedExoPlayer) { onDispose { sharedExoPlayer.release() } }

    // BARU: State terpusat dari ShortsViewModel (State Hoisting) yang tersimpan utuh di RAM saat berpindah tab
    val shortsList by viewModel.shorts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val sharedExoPlayer = remember(context) {
        viewModel.getOrCreateShortsPlayer(context)
    }

    // BARU: Jeda pemutaran video saat pengguna beralih keluar dari tab Shorts, dan lanjutkan saat masuk kembali
    DisposableEffect(Unit) {
        viewModel.resumePlayer()
        onDispose {
            viewModel.pausePlayer()
        }
    }

    // DINONAKTIFKAN: Kontainer Box biasa tanpa gestur Pull-to-Refresh
    // Box(
    //     modifier = modifier
    //         .fillMaxSize()
    //         .background(PureBlack)
    // ) {
    // BARU: PullToRefreshBox native Jetpack Compose Material 3 murni indikator spinner circular (tanpa teks label)
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadShorts(refresh = true) },
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when {
            uiState is ShortsUiState.Loading && shortsList.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = PolarCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memuat YouTube Shorts...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            uiState is ShortsUiState.Error && shortsList.isEmpty() -> {
                val errorMsg = (uiState as ShortsUiState.Error).message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = errorMsg,
                            color = IceWhite,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PolarCyan)
                                .clickable { viewModel.loadShorts(refresh = true) }
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
            }
            shortsList.isNotEmpty() -> {
                val pagerState = rememberPagerState(
                    initialPage = viewModel.currentPage.value.coerceIn(0, (shortsList.size - 1).coerceAtLeast(0)),
                    pageCount = { shortsList.size }
                )

                // BARU: Reset scroll ke halaman teratas (page 0) saat shortsList di-refresh dengan batch video baru
                LaunchedEffect(shortsList) {
                    if (shortsList.isNotEmpty() && pagerState.currentPage != 0) {
                        try {
                            pagerState.scrollToPage(0)
                        } catch (_: Exception) {}
                    }
                }

                // BARU: Pindah video di ExoPlayer saat halaman pager digeser ke video berikutnya / sebelumnya
                LaunchedEffect(pagerState.currentPage) {
                    if (shortsList.isNotEmpty()) {
                        viewModel.playShortAt(pagerState.currentPage)
                    }
                }

                // BARU: Infinite Scroll - Muat lebih banyak shorts saat pengguna mendekati 4 item terakhir di pager
                LaunchedEffect(pagerState.currentPage, shortsList.size) {
                    if (shortsList.isNotEmpty() && pagerState.currentPage >= shortsList.size - 4 && !isLoadingMore) {
                        viewModel.loadMore()
                    }
                }

                VerticalPager(
                    state = pagerState,
                    key = { page -> shortsList.getOrNull(page)?.id ?: page },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // DINONAKTIFKAN: Akses indeks langsung shortsList[page] yang berisiko melempar IndexOutOfBoundsException saat paging berubah
                    // val shortItem = shortsList[page]
                    // BARU: Akses aman dengan getOrNull untuk mencegah force close saat transisi batch list
                    val shortItem = shortsList.getOrNull(page) ?: return@VerticalPager
                    val isCurrentPage = pagerState.currentPage == page

                    ShortsPlayerPage(
                        video = shortItem,
                        isActive = isCurrentPage,
                        exoPlayer = sharedExoPlayer,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onTogglePlayPause = {
                            viewModel.togglePlayPause()
                        },
                        onVideoSelect = onVideoSelect
                    )
                }

                // Top Header Bar Transparan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .align(Alignment.TopCenter)
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, start = 18.dp, end = 18.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shorts",
                        color = IceWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            else -> {
                // BARU: State cadangan jika list kosong saat selesai loading (mencegah layar blank hitam tak terjangkau)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Belum ada video Shorts yang dimuat.",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PolarCyan)
                                .clickable { viewModel.loadShorts(refresh = true) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Muat Shorts",
                                color = PureBlack,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// DINONAKTIFKAN: ShortsPlayerPage lama yang menginisialisasi ExoPlayer terpisah di setiap halaman pager dan memakai pointerInput detectTapGestures yang memblokir gestur scroll vertikal
// @Composable
// private fun ShortsPlayerPage(...) { ... val exoPlayer = remember { ExoPlayer.Builder(context).build() } ... pointerInput(Unit) { detectTapGestures(...) } }
// BARU: ShortsPlayerPage yang menggunakan sharedExoPlayer dan non-blocking clickable gesture sehingga VerticalPager bebas di-swipe ke atas/bawah dengan mulus
@OptIn(UnstableApi::class)
@Composable
private fun ShortsPlayerPage(
    video: VideoItem,
    isActive: Boolean,
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onVideoSelect: (VideoItem) -> Unit
) {
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var showDoubleTapLike by remember { mutableStateOf(false) }
    var isTitleExpanded by remember { mutableStateOf(false) }
    var isLiked by remember(video.id) { mutableStateOf(LocalLibraryRepository.isLiked(video.id)) }

    // BARU: Lacak apakah frame pertama video Short sudah berhasil dirender
    var hasRenderedFirstFrame by remember(video.id) {
        mutableStateOf(
            exoPlayer.currentMediaItem?.mediaId == video.id &&
            exoPlayer.playbackState != Player.STATE_IDLE &&
            exoPlayer.playbackState != Player.STATE_BUFFERING
        )
    }

    DisposableEffect(exoPlayer, video.id, isActive) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (isActive) {
                    hasRenderedFirstFrame = true
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && isActive) {
                    hasRenderedFirstFrame = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            // BARU: Gunakan pointerInput detectTapGestures untuk single-tap (play/pause) dan double-tap (like animasi)
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = {
                        onTogglePlayPause()
                        showPlayPauseIndicator = true
                    },
                    onDoubleTap = {
                        if (!isLiked) {
                            isLiked = LocalLibraryRepository.toggleLike(video)
                        }
                        showDoubleTapLike = true
                    }
                )
            }
    ) {
        // Lapisan Video: Hanya pasang PlayerView pada halaman yang sedang aktif di layar
        if (isActive) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // DINONAKTIFKAN: Pengecekan isBuffering yang menyebabkan thumbnail tidak muncul saat swipe ke video baru (akibat isBuffering masih bernilai false dari video sebelumnya) sehingga frame video lama bocor
        // if (!isActive || (!hasRenderedFirstFrame && isBuffering)) {
        //     AsyncImage(
        //         model = video.thumbnailUrl,
        //         contentDescription = video.title,
        //         contentScale = ContentScale.Crop,
        //         modifier = Modifier.fillMaxSize()
        //     )
        // }

        // BARU: UI Buffering YouTube Shorts Anti-Bocor:
        // 1. Thumbnail poster SELALU tampil menutup PlayerView pada halaman offscreen (!isActive) ATAU sebelum frame pertama video baru selesai dirender (!hasRenderedFirstFrame).
        //    Ini menjamin 100% frame dari video sebelumnya tidak akan pernah bocor terlihat saat pengguna swipe ke video berikutnya.
        // 2. Begitu frame pertama video baru berhasil tampil (hasRenderedFirstFrame == true), thumbnail hilang permanen.
        //    Jika terjadi buffering di tengah pemutaran, frame video baru tetap membeku di bawah spinner tanpa tertutup thumbnail.
        if (!isActive || !hasRenderedFirstFrame) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading & Buffering Spinner di tengah layar Shorts (YouTube Style)
        if (isActive && (isBuffering || !hasRenderedFirstFrame)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PolarCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Vignette gradasi bayangan atas dan bawah untuk keterbacaan teks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Indikator Play / Pause di tengah layar saat di-tap
        LaunchedEffect(showPlayPauseIndicator) {
            if (showPlayPauseIndicator) {
                delay(600)
                showPlayPauseIndicator = false
            }
        }
        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.60f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    LucidePause(tint = Color.White, size = 32.dp)
                } else {
                    LucidePlay(tint = Color.White, size = 32.dp)
                }
            }
        }

        // BARU: Indikator Animasi Double-Tap to Like di tengah layar
        LaunchedEffect(showDoubleTapLike) {
            if (showDoubleTapLike) {
                delay(700)
                showDoubleTapLike = false
            }
        }
        AnimatedVisibility(
            visible = showDoubleTapLike,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                LucideThumbsUp(
                    tint = PolarCyan,
                    size = 48.dp
                )
            }
        }

        // Overlay Kanan (Aksi Like, Dislike, Comment, Share)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tombol Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            isLiked = LocalLibraryRepository.toggleLike(video)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LucideThumbsUp(
                        tint = if (isLiked) PolarCyan else IceWhite,
                        size = 22.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (video.viewCount > 0) VideoItem.formatCount(video.viewCount / 10) else "Suka",
                    color = IceWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Tombol Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            // Bagikan link video
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LucideShare2(tint = IceWhite, size = 22.dp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bagikan",
                    color = IceWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Piringan Audio Berputar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.uploaderAvatarUrl ?: video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Overlay Kiri Bawah (Kanal, Judul Shorts, dan Audio)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.80f)
                .padding(start = 16.dp, bottom = 92.dp)
        ) {
            // Baris Kanal + Tombol Subscribe
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = video.uploaderAvatarUrl ?: video.thumbnailUrl,
                    contentDescription = video.uploaderName,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = "@${video.uploaderName.replace(" ", "")}",
                    color = IceWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Subscribe",
                        color = PureBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BARU: Judul Shorts (Dapat diketuk untuk memperluas / ekspansi deskripsi & tagar)
            Text(
                text = video.title,
                color = IceWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = if (isTitleExpanded) 8 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.clickable { isTitleExpanded = !isTitleExpanded }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Baris Audio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "♫  Suara asli - ${video.uploaderName}",
                    color = IceWhite.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // BARU: Garis progres pemutaran tipis interaktif (PolarCyan) di dasar layar video Shorts
        // Responsif terhadap sentuhan geser / tap seeking
        var currentPositionMs by remember { mutableStateOf(0L) }
        var totalDurationMs by remember { mutableStateOf(1L) }

        LaunchedEffect(isActive, isPlaying) {
            if (isActive) {
                while (true) {
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(1L)
                    delay(150)
                }
            }
        }

        val progress = if (totalDurationMs > 0L) {
            (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 74.dp)
                .height(14.dp)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val seekPos = (fraction * totalDurationMs).toLong()
                        exoPlayer.seekTo(seekPos)
                        currentPositionMs = seekPos
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(PolarCyan)
                )
            }
        }
    }
}
