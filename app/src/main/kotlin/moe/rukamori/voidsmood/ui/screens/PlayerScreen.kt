// meticulous and honest
package moe.rukamori.voidsmood.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import moe.rukamori.voidsmood.ui.components.LucideArrowLeft
import moe.rukamori.voidsmood.ui.components.LucideDownload
import moe.rukamori.voidsmood.ui.components.LucideShare2
import moe.rukamori.voidsmood.ui.components.LucideThumbsUp
import moe.rukamori.voidsmood.ui.components.VideoCard
import moe.rukamori.voidsmood.ui.theme.DarkSurface
import moe.rukamori.voidsmood.ui.theme.GlassBorder
import moe.rukamori.voidsmood.ui.theme.GlassSurface
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PolarCyan
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted
import moe.rukamori.voidsmood.ui.viewmodel.PlayerUiState
import moe.rukamori.voidsmood.ui.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
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

    // Tangani gesture/tombol back sistem Android untuk kembali ke feed
    BackHandler(onBack = onBack)

    LaunchedEffect(video.id) {
        viewModel.loadVideo(video)
    }

    // Media3 ExoPlayer instance dengan Lifecycle DisposableEffect
    val exoPlayer = remember(context) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // AREA 1: 16:9 Video Player Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            when (val state = uiState) {
                is PlayerUiState.Success -> {
                    LaunchedEffect(state.details.streamUrl) {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        val mediaItem = MediaItem.fromUri(state.details.streamUrl)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                setShowNextButton(false)
                                setShowPreviousButton(false)
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

                is PlayerUiState.Loading -> {
                    // Tampilkan thumbnail langsung agar layar tidak pernah blank
                    AsyncImage(
                        model = state.video.thumbnailUrl,
                        contentDescription = state.video.title,
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
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                is PlayerUiState.Error -> {
                    AsyncImage(
                        model = state.video.thumbnailUrl,
                        contentDescription = state.video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = IceWhite,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PolarCyan)
                                    .clickable { viewModel.loadVideo(state.video) }
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

                PlayerUiState.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }

            // Tombol Back di Pojok Kiri Atas Player (YouTube Style)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                LucideArrowLeft(
                    tint = IceWhite,
                    size = 20.dp
                )
            }
        }

        // AREA 2: Video Metadata & Daftar Video Terkait (YouTube Mobile Layout)
        val details = (uiState as? PlayerUiState.Success)?.details
        val currentVideo = details?.video ?: video

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

                // Baris Channel (Avatar + Nama + Subs + Tombol Subscribe Putih YouTube)
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
                        if (details?.subscriberCountText?.isNotEmpty() == true) {
                            Text(
                                text = details.subscriberCountText,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Tombol Subscribe (Gaya YouTube: Pill Putih Text Hitam)
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
                            text = details?.likeCountText ?: "Suka",
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
                if (details?.description?.isNotEmpty() == true) {
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
                                text = details.description,
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
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
            val relatedList = details?.relatedVideos ?: emptyList()
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
