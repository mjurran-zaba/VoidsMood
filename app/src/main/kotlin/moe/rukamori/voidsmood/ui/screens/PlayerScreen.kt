// meticulous and honest
package moe.rukamori.voidsmood.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import moe.rukamori.voidsmood.ui.components.LucideArrowLeft
import moe.rukamori.voidsmood.ui.components.LucideCheck
import moe.rukamori.voidsmood.ui.components.LucideDownload
import moe.rukamori.voidsmood.ui.components.LucideMaximize
import moe.rukamori.voidsmood.ui.components.LucideMinimize
import moe.rukamori.voidsmood.ui.components.LucidePause
import moe.rukamori.voidsmood.ui.components.LucidePlay
import moe.rukamori.voidsmood.ui.components.LucideRotateCcw10
import moe.rukamori.voidsmood.ui.components.LucideRotateCw10
import moe.rukamori.voidsmood.ui.components.LucideSettings
import moe.rukamori.voidsmood.ui.components.LucideShare2
import moe.rukamori.voidsmood.ui.components.LucideThumbsUp
import moe.rukamori.voidsmood.ui.components.VideoCard
import moe.rukamori.voidsmood.ui.theme.GlassBorder
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted
import moe.rukamori.voidsmood.ui.viewmodel.PlayerUiState
import moe.rukamori.voidsmood.ui.viewmodel.PlayerViewModel
import kotlin.OptIn

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// BARU: Komponen Seekbar YouTube Asli (Garis merah + buffer putih + thumb titik merah)
@Composable
fun YouTubeProgressBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    onSeek: (Long) -> Unit,
    onSeekingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSafe = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val playedFraction = if (isDragging) dragFraction else (positionMs.toFloat() / durationSafe).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedMs.toFloat() / durationSafe).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(durationSafe) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        onSeekingChange(true)
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val success = tryAwaitRelease()
                        if (success) {
                            onSeek((dragFraction * durationSafe).toLong())
                        }
                        isDragging = false
                        onSeekingChange(false)
                    }
                )
            }
            .pointerInput(durationSafe) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onSeekingChange(true)
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek((dragFraction * durationSafe).toLong())
                        isDragging = false
                        onSeekingChange(false)
                    },
                    onDragCancel = {
                        isDragging = false
                        onSeekingChange(false)
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val deltaFraction = dragAmount / size.width
                        dragFraction = (dragFraction + deltaFraction).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isDragging) 4.5.dp else 3.dp)
        ) {
            val w = size.width
            val h = size.height

            // 1. Unplayed line (abu-abu transparan)
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = h,
                cap = StrokeCap.Round
            )

            // 2. Buffer progress line (putih transparan)
            if (bufferedFraction > 0f) {
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(0f, h / 2),
                    end = Offset(w * bufferedFraction, h / 2),
                    strokeWidth = h,
                    cap = StrokeCap.Round
                )
            }

            // 3. Played progress line (YouTube Red: #FF0000)
            drawLine(
                color = Color(0xFFFF0000),
                start = Offset(0f, h / 2),
                end = Offset(w * playedFraction, h / 2),
                strokeWidth = h,
                cap = StrokeCap.Round
            )

            // 4. Scrubber Thumb (Titik bulat merah YouTube di ujung pemutaran)
            val thumbRadius = if (isDragging) 6.5.dp.toPx() else 5.dp.toPx()
            drawCircle(
                color = Color(0xFFFF0000),
                radius = thumbRadius,
                center = Offset(w * playedFraction, h / 2)
            )
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onVideoSelect: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // State kontrol Fullscreen & Mode Landscape
    var isFullscreen by remember { mutableStateOf(false) }

    // State kontrol pemutaran video
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    // BARU: Logika Auto-Hide dan Visibility Kontrol 100% YouTube Style
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // State pemilihan resolusi kualitas video
    var selectedQuality by remember { mutableStateOf("Auto") }
    var showQualityDialog by remember { mutableStateOf(false) }

    fun toggleFullscreen() {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            isFullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Tangani gesture / tombol back sistem Android (prioritaskan keluar fullscreen terlebih dahulu)
    BackHandler {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            onBack()
        }
    }

    // Reset orientasi dan status bar saat keluar dari PlayerScreen
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(video.id) {
        viewModel.loadVideo(video)
        selectedQuality = "Auto"
        areControlsVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // Factory HTTP DataSource dengan User-Agent & Referer YouTube resmi anti-403
    val httpDataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.youtube.com/",
                    "Origin" to "https://www.youtube.com"
                )
            )
    }

    // ExoPlayer Lifecycle instance dengan Listener
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    lastInteractionTime = System.currentTimeMillis()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        playbackError = null
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        areControlsVisible = true
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = error.message ?: "Koneksi video terputus. Ketuk untuk mencoba lagi."
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Polling durasi dan posisi pemutaran saat video aktif
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (!isSeeking && isPlaying) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    // Logika Auto-Hide: kontrol memudar dan menghilang otomatis setelah 3 detik pemutaran tanpa sentuhan
    LaunchedEffect(areControlsVisible, isPlaying, lastInteractionTime) {
        if (areControlsVisible && isPlaying && !isSeeking) {
            delay(3000)
            areControlsVisible = false
        }
    }

    // Eksekusi pemutaran saat detail stream berhasil diekstrak
    val successDetails = (uiState as? PlayerUiState.Success)?.details
    LaunchedEffect(successDetails) {
        if (successDetails != null) {
            playbackError = null
            isBuffering = true

            val mediaSource: MediaSource? = try {
                when {
                    !successDetails.hlsUrl.isNullOrBlank() -> {
                        val uri = successDetails.hlsUrl.removeSuffix("?")
                        HlsMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(uri))
                    }
                    successDetails.isProgressiveMuxed && !successDetails.videoUrl.isNullOrBlank() -> {
                        ProgressiveMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(successDetails.videoUrl))
                    }
                    !successDetails.videoUrl.isNullOrBlank() && !successDetails.audioUrl.isNullOrBlank() -> {
                        val vSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(successDetails.videoUrl))
                        val aSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(successDetails.audioUrl))
                        MergingMediaSource(vSource, aSource)
                    }
                    !successDetails.videoUrl.isNullOrBlank() -> {
                        ProgressiveMediaSource.Factory(httpDataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(successDetails.videoUrl))
                    }
                    else -> null
                }
            } catch (e: Exception) {
                playbackError = e.localizedMessage ?: "Gagal memproses stream video."
                null
            }

            if (mediaSource != null) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.play()
            } else if (playbackError == null) {
                playbackError = "Tidak ada stream pemutaran yang kompatibel."
                isBuffering = false
            }
        }
    }

    // Fungsi terapan penggantian resolusi kualitas video
    fun applyResolution(quality: String) {
        selectedQuality = quality

        if (quality == "Auto") {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .build()
            return
        }

        val resNumber = quality.replace("p", "").replace("60", "").trim().toIntOrNull() ?: return
        val (width, height) = when (resNumber) {
            1080 -> 1920 to 1080
            720 -> 1280 to 720
            480 -> 854 to 480
            360 -> 640 to 360
            240 -> 426 to 240
            144 -> 256 to 144
            else -> (resNumber * 16 / 9) to resNumber
        }

        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(width, height)
            .setMinVideoSize(width, height)
            .build()

        val directStreamUrl = successDetails?.resolutionStreams?.get(quality)
            ?: successDetails?.resolutionStreams?.get("${resNumber}p")

        if (directStreamUrl != null && successDetails?.hlsUrl.isNullOrBlank()) {
            try {
                val curPos = exoPlayer.currentPosition
                val wasPlaying = exoPlayer.isPlaying
                val vSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(directStreamUrl))
                val newSource = if (!successDetails?.audioUrl.isNullOrBlank()) {
                    val aSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(successDetails.audioUrl))
                    MergingMediaSource(vSource, aSource)
                } else {
                    vSource
                }
                exoPlayer.setMediaSource(newSource)
                exoPlayer.prepare()
                exoPlayer.seekTo(curPos)
                if (wasPlaying) exoPlayer.play()
            } catch (_: Exception) {}
        }
    }

    val currentVideo = successDetails?.video ?: video

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Spacer pengaman status bar pada mode portrait
        if (!isFullscreen) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        }

        // AREA 1: Video Player Surface
        Box(
            modifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }
        ) {
            // DINONAKTIFKAN: useController = true bawaan ExoPlayer yang menyebabkan duplikasi tombol dan stiker permanen
            // BARU: useController = false murni rendering video hardware, seluruh kontrol dikendalikan 100% Compose YouTube style
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        keepScreenOn = true
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

            // Tampilkan thumbnail poster saat buffering awal atau loading
            if (isBuffering || uiState is PlayerUiState.Loading) {
                AsyncImage(
                    model = currentVideo.thumbnailUrl,
                    contentDescription = currentVideo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PolarCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Tampilan Error jika pemutaran terkendala
            if (playbackError != null || uiState is PlayerUiState.Error) {
                val errorMsg = playbackError
                    ?: (uiState as? PlayerUiState.Error)?.message
                    ?: "Koneksi video gagal."

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMsg,
                            color = IceWhite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PolarCyan)
                                .clickable {
                                    playbackError = null
                                    viewModel.loadVideo(currentVideo)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
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

            // Lapisan penangkap sentuhan untuk memunculkan / menyembunyikan kontrol video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        areControlsVisible = !areControlsVisible
                        if (areControlsVisible) {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
            )

            // BARU: Kontrol Video 100% Persis Plek Ketiplek YouTube Mobile
            // Muncul bersamaan saat diketuk, dan menghilang total (fade-out) bersamaan tanpa ada tombol tertinggal
            androidx.compose.animation.AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.48f))
                ) {
                    // BARIS ATAS: Back Button di kiri, Settings Gear di kanan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Back (Keluar fullscreen jika landscape, kembali ke beranda jika portrait)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    if (isFullscreen) toggleFullscreen() else onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideArrowLeft(
                                tint = IceWhite,
                                size = 22.dp
                            )
                        }

                        // Tombol Settings Gear Tunggal di Kanan Atas (Buka Bottom Sheet Resolusi Video)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showQualityDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideSettings(
                                tint = IceWhite,
                                size = 22.dp
                            )
                        }
                    }

                    // KONTROL TENGAH: Rewind 10 Detik, Play / Pause Lingkaran, Forward 10 Detik
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                    currentPosition = exoPlayer.currentPosition
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideRotateCcw10(
                                tint = IceWhite,
                                size = 36.dp
                            )
                        }

                        // Play / Pause Circle
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying) {
                                LucidePause(
                                    tint = IceWhite,
                                    size = 28.dp
                                )
                            } else {
                                LucidePlay(
                                    tint = IceWhite,
                                    size = 28.dp
                                )
                            }
                        }

                        // Forward 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(duration))
                                    currentPosition = exoPlayer.currentPosition
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideRotateCw10(
                                tint = IceWhite,
                                size = 36.dp
                            )
                        }
                    }

                    // BARIS BAWAH: Seekbar Merah YouTube + Waktu + SATU Tombol Fullscreen Tunggal
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    ) {
                        // YouTube Red Seekbar
                        YouTubeProgressBar(
                            positionMs = currentPosition,
                            durationMs = duration,
                            bufferedMs = bufferedPosition,
                            onSeek = { newPos ->
                                lastInteractionTime = System.currentTimeMillis()
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            onSeekingChange = { seeking ->
                                isSeeking = seeking
                                if (seeking) {
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Baris Info Durasi Waktu & Tombol Fullscreen Tunggal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 8.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Penunjuk Waktu: misal "00:33 / 02:22"
                            Text(
                                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                color = IceWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // SATU Tombol Fullscreen Tunggal di Kanan Bawah
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        lastInteractionTime = System.currentTimeMillis()
                                        toggleFullscreen()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFullscreen) {
                                    LucideMinimize(
                                        tint = IceWhite,
                                        size = 20.dp
                                    )
                                } else {
                                    LucideMaximize(
                                        tint = IceWhite,
                                        size = 20.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // AREA 2: Video Metadata & Daftar Video Terkait (Hanya tampil saat mode portrait / bukan fullscreen)
        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Judul Video
                item {
                    Text(
                        text = currentVideo.title,
                        color = IceWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                    )

                    // Stats: Views & Waktu Upload
                    val statsText = buildString {
                        if (currentVideo.viewCountText.isNotEmpty()) append(currentVideo.viewCountText)
                        if (currentVideo.uploadDateText.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append(currentVideo.uploadDateText)
                        }
                    }
                    if (statsText.isNotEmpty()) {
                        Text(
                            text = statsText,
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Baris Channel (Avatar + Nama + Subs + Tombol Subscribe Putih)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22242E)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentVideo.uploaderAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentVideo.uploaderAvatarUrl,
                                    contentDescription = currentVideo.uploaderName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = currentVideo.uploaderName.firstOrNull()?.uppercase() ?: "Y",
                                    color = TextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentVideo.uploaderName,
                                color = IceWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (successDetails?.subscriberCountText?.isNotEmpty() == true) {
                                Text(
                                    text = successDetails.subscriberCountText,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Tombol Subscribe (Pill Putih khas YouTube)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(IceWhite)
                                .clickable { /* Subscribe toggle */ }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Subscribe",
                                color = PureBlack,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Baris Tombol Aksi (Like, Share, Download)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1F222B))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LucideThumbsUp(tint = IceWhite, size = 16.dp)
                            Text(
                                text = successDetails?.likeCountText ?: "Suka",
                                color = IceWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Share Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1F222B))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LucideShare2(tint = IceWhite, size = 16.dp)
                            Text(
                                text = "Bagikan",
                                color = IceWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Download Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1F222B))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LucideDownload(tint = IceWhite, size = 16.dp)
                            Text(
                                text = "Download",
                                color = IceWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Kotak Deskripsi Video (Expandable)
                    val descriptionText = successDetails?.description ?: ""
                    if (descriptionText.isNotEmpty()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141720))
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .clickable { isExpanded = !isExpanded }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Deskripsi",
                                    color = IceWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = descriptionText,
                                    color = Color(0xFFC7CBD8),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isExpanded) "Sembunyikan" else "...selengkapnya",
                                    color = PolarCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Section Header: Video Terkait
                    Text(
                        text = "Video Terkait",
                        color = IceWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                    )
                }

                // Daftar Video Terkait
                val relatedList = successDetails?.relatedVideos ?: emptyList()
                if (relatedList.isNotEmpty()) {
                    items(
                        items = relatedList,
                        key = { it.id }
                    ) { relVideo ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            VideoCard(
                                video = relVideo,
                                onVideoClick = { selected ->
                                    onVideoSelect(selected)
                                }
                            )
                        }
                    }
                } else if (uiState is PlayerUiState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
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
            }
        }
    }

    // Modal Bottom Sheet Pengaturan Resolusi / Kualitas Video
    if (showQualityDialog) {
        ModalBottomSheet(
            onDismissRequest = { showQualityDialog = false },
            containerColor = Color(0xFF161822),
            contentColor = IceWhite,
            scrimColor = Color.Black.copy(alpha = 0.65f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3E4357))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Kualitas Video",
                    color = IceWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih resolusi pemutaran video untuk menghemat kuota atau kualitas tinggi",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val qualityOptions = successDetails?.availableResolutions?.ifEmpty {
                    listOf("Auto", "1080p", "720p", "480p", "360p", "240p", "144p")
                } ?: listOf("Auto", "1080p", "720p", "480p", "360p", "240p", "144p")

                qualityOptions.forEach { quality ->
                    val isSelected = (selectedQuality == quality)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                applyResolution(quality)
                                showQualityDialog = false
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val label = when (quality) {
                            "Auto" -> "Otomatis (Disarankan)"
                            "1080p", "1080p60" -> "$quality • Full HD"
                            "720p", "720p60" -> "$quality • HD"
                            "480p" -> "480p • Standar"
                            "360p" -> "360p • Hemat Kuota"
                            "240p" -> "240p • Cepat"
                            "144p" -> "144p • Paling Hemat"
                            else -> quality
                        }

                        Text(
                            text = label,
                            color = if (isSelected) PolarCyan else IceWhite,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        if (isSelected) {
                            LucideCheck(tint = PolarCyan, size = 18.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
