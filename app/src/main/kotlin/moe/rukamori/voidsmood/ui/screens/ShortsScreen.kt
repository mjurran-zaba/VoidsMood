// meticulous and honest
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Text
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
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import moe.rukamori.voidsmood.data.repository.LocalLibraryRepository
import moe.rukamori.voidsmood.data.repository.YouTubeRepository
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
// Menampilkan pengalaman swipe vertikal layar penuh 9:16 dengan Single Shared ExoPlayer & Non-blocking Clickable Gesture
@OptIn(UnstableApi::class)
@Composable
fun ShortsScreen(
    onVideoSelect: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shortsList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // BARU: Single Shared ExoPlayer instance di level ShortsScreen agar tidak terjadi decoder exhaustion dan saturasi bandwidth jaringan
    val sharedExoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    val httpDataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))
    }

    DisposableEffect(sharedExoPlayer) {
        onDispose {
            sharedExoPlayer.stop()
            sharedExoPlayer.release()
        }
    }

    fun loadShorts(refresh: Boolean = false) {
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = YouTubeRepository.getShortsFeed(refresh = refresh)
            result.onSuccess { list ->
                shortsList = list
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Gagal memuat YouTube Shorts."
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadShorts(refresh = false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        if (isLoading && shortsList.isEmpty()) {
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
        } else if (errorMessage != null && shortsList.isEmpty()) {
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
                            .clickable { loadShorts(refresh = true) }
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
        } else if (shortsList.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { shortsList.size })

            // BARU: Infinite Scroll - Muat lebih banyak shorts saat pengguna mendekati 4 item terakhir di pager
            LaunchedEffect(pagerState.currentPage, shortsList.size) {
                if (shortsList.isNotEmpty() && pagerState.currentPage >= shortsList.size - 4 && !isLoadingMore) {
                    isLoadingMore = true
                    val moreResult = YouTubeRepository.getShortsFeed(refresh = true)
                    moreResult.onSuccess { newShorts ->
                        if (newShorts.isNotEmpty()) {
                            shortsList = (shortsList + newShorts).distinctBy { it.id }
                        }
                    }
                    isLoadingMore = false
                }
            }

            // BARU: Kontrol pemutaran pada video yang sedang aktif di pager
            var isCurrentPlaying by remember { mutableStateOf(true) }
            var isCurrentBuffering by remember { mutableStateOf(true) }

            LaunchedEffect(pagerState.currentPage, shortsList) {
                val currentVideo = shortsList.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
                LocalLibraryRepository.addToHistory(currentVideo)
                isCurrentBuffering = true
                sharedExoPlayer.pause()
                sharedExoPlayer.stop()
                sharedExoPlayer.clearMediaItems()

                val result = YouTubeRepository.getVideoPlaybackDetails(currentVideo.id)
                result.onSuccess { details ->
                    val mediaSource: MediaSource? = when {
                        details.isProgressiveMuxed && !details.videoUrl.isNullOrBlank() -> {
                            ProgressiveMediaSource.Factory(httpDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(details.videoUrl))
                        }
                        !details.videoUrl.isNullOrBlank() && !details.audioUrl.isNullOrBlank() -> {
                            val vSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(details.videoUrl))
                            val aSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(details.audioUrl))
                            MergingMediaSource(vSource, aSource)
                        }
                        !details.videoUrl.isNullOrBlank() -> {
                            ProgressiveMediaSource.Factory(httpDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(details.videoUrl))
                        }
                        !details.hlsUrl.isNullOrBlank() -> {
                            HlsMediaSource.Factory(httpDataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(details.hlsUrl))
                        }
                        else -> null
                    }
                    if (mediaSource != null) {
                        sharedExoPlayer.setMediaSource(mediaSource)
                        sharedExoPlayer.prepare()
                        sharedExoPlayer.play()
                        isCurrentPlaying = true
                    }
                }
            }

            DisposableEffect(sharedExoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> isCurrentBuffering = true
                            Player.STATE_READY -> isCurrentBuffering = false
                            Player.STATE_ENDED -> isCurrentBuffering = false
                            else -> {}
                        }
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isCurrentPlaying = playing
                    }
                }
                sharedExoPlayer.addListener(listener)
                onDispose {
                    sharedExoPlayer.removeListener(listener)
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val shortItem = shortsList[page]
                val isCurrentPage = pagerState.currentPage == page

                ShortsPlayerPage(
                    video = shortItem,
                    isActive = isCurrentPage,
                    exoPlayer = sharedExoPlayer,
                    isPlaying = isCurrentPlaying,
                    isBuffering = isCurrentBuffering,
                    onTogglePlayPause = {
                        if (isCurrentPlaying) {
                            sharedExoPlayer.pause()
                        } else {
                            sharedExoPlayer.play()
                        }
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
    var isLiked by remember(video.id) { mutableStateOf(LocalLibraryRepository.isLiked(video.id)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            // BARU: Gunakan clickable dengan indication null agar sentuhan tap dapat direspon TANPA menelan event drag vertikal VerticalPager
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onTogglePlayPause()
                    showPlayPauseIndicator = true
                }
            )
    ) {
        // Lapisan Video: Hanya pasang PlayerView pada halaman yang sedang aktif di layar
        if (isActive) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
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

        // Thumbnail Poster saat buffering atau pada halaman offscreen
        if (isBuffering || !isActive) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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

            // Judul Shorts
            Text(
                text = video.title,
                color = IceWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
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
    }
}
