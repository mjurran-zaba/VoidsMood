// meticulous and honest
// READ AND IMPLEMENT AGENTS.md BEFORE YOU DO
package moe.rukamori.voidsmood.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.network.MediaCacheManager
import moe.rukamori.voidsmood.data.network.NetworkClient
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import moe.rukamori.voidsmood.data.repository.LocalLibraryRepository
import moe.rukamori.voidsmood.data.repository.YouTubeRepository

sealed interface ShortsUiState {
    data object Loading : ShortsUiState
    data class Success(val shorts: List<VideoItem>) : ShortsUiState
    data class Error(val message: String) : ShortsUiState
}

// BARU: ShortsViewModel untuk mempertahankan data Shorts dan siklus pemutar video (State Hoisting)
// Mencegah layar blank saat pengguna berpindah tab ke Home dan kembali ke Shorts,
// serta mendukung infinite vertical scrolling tanpa kehilangan posisi video saat ini.
class ShortsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ShortsUiState>(ShortsUiState.Loading)
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val _shorts = MutableStateFlow<List<VideoItem>>(emptyList())
    val shorts: StateFlow<List<VideoItem>> = _shorts.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var exoPlayerInstance: ExoPlayer? = null
    // DINONAKTIFKAN: private var currentPlayingVideoId: String? = null yang tidak reaktif terhadap recomposition Compose
    // private var currentPlayingVideoId: String? = null
    // BARU: StateFlow reaktif ID video yang sedang dimuat dan diputar di ExoPlayer,
    // memungkinkan PlayerView hanya terpasang jika mediaId ExoPlayer sudah cocok dengan halaman yang aktif
    private val _currentPlayingVideoId = MutableStateFlow<String?>(null)
    val currentPlayingVideoId: StateFlow<String?> = _currentPlayingVideoId.asStateFlow()

    // BARU: State target scroll page untuk mengarahkan pager saat Shorts dibuka dari Beranda / Pencarian
    private val _targetScrollPage = MutableStateFlow<Int?>(null)
    val targetScrollPage: StateFlow<Int?> = _targetScrollPage.asStateFlow()

    fun clearTargetScrollPage() {
        _targetScrollPage.value = null
    }
    private var pageOffset = 1
    private var appContext: Context? = null

    init {
        loadShorts(refresh = false)
    }

    fun getOrCreateShortsPlayer(context: Context): ExoPlayer {
        appContext = context.applicationContext
        if (exoPlayerInstance == null) {
            // DINONAKTIFKAN: Konfigurasi buffer lama (bufferForPlaybackMs = 600ms, minBufferMs = 8000ms) yang lambat untuk feed Shorts
            // val shortsLoadControl = DefaultLoadControl.Builder()
            //     .setBufferDurationsMs(
            //         /* minBufferMs = */ 8_000,
            //         /* maxBufferMs = */ 20_000,
            //         /* bufferForPlaybackMs = */ 600,
            //         /* bufferForPlaybackAfterRebufferMs = */ 1_000
            //     )
            //     .setPrioritizeTimeOverSizeThresholds(true)
            //     .build()

            // BARU: Fast-Start LoadControl ala TikTok/Instagram Reels:
            // bufferForPlaybackMs diturunkan ke 250ms agar pemutaran video langsung dimulai seketika saat segmen data awal tiba
            val shortsLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 3_000,
                    /* maxBufferMs = */ 15_000,
                    /* bufferForPlaybackMs = */ 250,
                    /* bufferForPlaybackAfterRebufferMs = */ 500
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            exoPlayerInstance = ExoPlayer.Builder(context.applicationContext)
                .setLoadControl(shortsLoadControl)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            _isPlaying.value = playing
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                        }
                    })
                }
        }
        return exoPlayerInstance!!
    }

    fun loadShorts(refresh: Boolean = false) {
        if (!refresh && _shorts.value.isNotEmpty()) {
            _uiState.value = ShortsUiState.Success(_shorts.value)
            return
        }

        if (refresh) {
            _isRefreshing.value = true
        } else {
            _uiState.value = ShortsUiState.Loading
        }
        pageOffset = 1

        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.getShortsFeed(refresh = refresh)
            // DINONAKTIFKAN: Eksekusi player dan mutasi state di worker thread Dispatchers.IO yang menyebabkan force close (IllegalStateException: Player is accessed on the wrong thread)
            // result.onSuccess { list ->
            //     _shorts.value = list
            //     _uiState.value = ShortsUiState.Success(list)
            //     _isRefreshing.value = false
            //     if (list.isNotEmpty()) {
            //         playShortAt(0)
            //     }
            // }
            // BARU: Alihkan kembali ke Dispatchers.Main sebelum memperbarui state dan memulai pemutaran player
            withContext(Dispatchers.Main) {
                result.onSuccess { list ->
                    _shorts.value = list
                    _uiState.value = ShortsUiState.Success(list)
                    _isRefreshing.value = false
                    if (list.isNotEmpty()) {
                        playShortAt(0)
                    }
                }.onFailure { err ->
                    _isRefreshing.value = false
                    if (_shorts.value.isEmpty()) {
                        _uiState.value = ShortsUiState.Error(err.localizedMessage ?: "Gagal memuat YouTube Shorts.")
                    }
                }
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || _shorts.value.isEmpty()) return
        _isLoadingMore.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.loadMoreShortsFeed(pageOffset)
            withContext(Dispatchers.Main) {
                result.onSuccess { newItems ->
                    if (newItems.isNotEmpty()) {
                        val combined = (_shorts.value + newItems).distinctBy { it.id }
                        _shorts.value = combined
                        _uiState.value = ShortsUiState.Success(combined)
                        pageOffset++
                    }
                }
                _isLoadingMore.value = false
            }
        }
    }

    // BARU: Membuka dan memutar video Shorts spesifik (misal dari rak Shorts Beranda atau hasil Pencarian)
    // dan mengarahkan Pager ke video tersebut di layar penuh
    fun playShortVideo(video: VideoItem, contextList: List<VideoItem> = emptyList()) {
        val currentList = _shorts.value.toMutableList()
        var targetIndex = currentList.indexOfFirst { it.id == video.id }

        if (targetIndex == -1) {
            if (contextList.isNotEmpty()) {
                val combined = (listOf(video) + contextList + currentList).distinctBy { it.id }
                _shorts.value = combined
                _uiState.value = ShortsUiState.Success(combined)
                targetIndex = 0
            } else {
                currentList.add(0, video)
                _shorts.value = currentList
                _uiState.value = ShortsUiState.Success(currentList)
                targetIndex = 0
            }
        }

        _currentPage.value = targetIndex
        _targetScrollPage.value = targetIndex
        playShortAt(targetIndex)
    }

    fun playShortAt(index: Int) {
        val list = _shorts.value
        val video = list.getOrNull(index) ?: return
        _currentPage.value = index

        // BARU: Prefetch detail video berikutnya (index + 1 dan index + 2) di background
        // sehingga saat pengguna swipe ke bawah, pemutaran langsung berputar seketika tanpa buffering
        prefetchUpcomingShorts(index)

        // DINONAKTIFKAN: Manipulasi player.pause(), player.stop() di thread pemanggil (bisa Dispatchers.IO) yang memicu crash
        // BARU: Garansi 100% semua interaksi dengan ExoPlayer dijalankan di Main Thread (Dispatchers.Main)
        viewModelScope.launch(Dispatchers.Main) {
            val player = exoPlayerInstance ?: return@launch

            if (_currentPlayingVideoId.value == video.id && player.playbackState == Player.STATE_READY) {
                player.play()
                return@launch
            }

            LocalLibraryRepository.addToHistory(video)
            _isBuffering.value = true

            // BARU: Reset _currentPlayingVideoId ke null seketika saat pergantian video dimulai
            // sehingga PlayerView pada halaman baru TIDAK menempel ke player sebelum media barunya selesai disiapkan
            _currentPlayingVideoId.value = null

            player.pause()
            player.stop()
            player.clearMediaItems()
            // DINONAKTIFKAN: player.clearVideoSurface() yang melepaskan (detach) SurfaceView dari ExoPlayer sehingga layar menjadi blank permanen pada video berikutnya
            // player.clearVideoSurface()

            val detailsResult = withContext(Dispatchers.IO) {
                YouTubeRepository.getVideoPlaybackDetails(video.id)
            }

            detailsResult.onSuccess { details ->
                // BARU: Perbarui nama uploader asli dan avatar dari metadata NewPipe yang akurat
                if (details.video.uploaderName.isNotBlank() &&
                    details.video.uploaderName != "YouTube Shorts" &&
                    details.video.uploaderName != "YouTube Creator"
                ) {
                    val updated = _shorts.value.map { item ->
                        if (item.id == video.id) {
                            item.copy(
                                uploaderName = details.video.uploaderName,
                                uploaderAvatarUrl = details.video.uploaderAvatarUrl ?: item.uploaderAvatarUrl
                            )
                        } else item
                    }
                    _shorts.value = updated
                }

                // DINONAKTIFKAN: httpDataSourceFactory tanpa cache disk lokal yang menyebabkan re-download setiap kali loop atau swipe kembali
                // val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkClient.mediaStreamingOkHttpClient)
                //     .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
                //     .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))
                // BARU: DataSourceFactory dengan lapisan SimpleCache 250 MB LRU disk storage (mengadopsi VoidTune & PipePipe).
                // Video yang sudah diputar atau di-looping langsung dibaca dari disk dengan latensi 0 ms tanpa memakan kuota internet
                val dataSourceFactory = appContext?.let { ctx ->
                    MediaCacheManager.createCacheDataSourceFactory(ctx)
                } ?: OkHttpDataSource.Factory(NetworkClient.mediaStreamingOkHttpClient)
                    .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
                    .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))

                // DINONAKTIFKAN: Pembuatan MediaItem tanpa pelacakan mediaId eksplisit
                // val mediaSource: MediaSource? = when {
                //     details.isProgressiveMuxed && !details.videoUrl.isNullOrBlank() -> {
                //         ProgressiveMediaSource.Factory(httpDataSourceFactory)
                //             .createMediaSource(MediaItem.fromUri(details.videoUrl))
                //     }
                //     !details.videoUrl.isNullOrBlank() && !details.audioUrl.isNullOrBlank() -> {
                //         val vSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                //             .createMediaSource(MediaItem.fromUri(details.videoUrl))
                //         val aSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                //             .createMediaSource(MediaItem.fromUri(details.audioUrl))
                //         MergingMediaSource(vSource, aSource)
                //     }
                //     !details.videoUrl.isNullOrBlank() -> {
                //         ProgressiveMediaSource.Factory(httpDataSourceFactory)
                //             .createMediaSource(MediaItem.fromUri(details.videoUrl))
                //     }
                //     else -> null
                // }
                // BARU: MediaItem dengan setMediaId(video.id) eksplisit untuk pelacakan identitas media yang akurat
                val mediaSource: MediaSource? = when {
                    details.isProgressiveMuxed && !details.videoUrl.isNullOrBlank() -> {
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.Builder().setUri(details.videoUrl).setMediaId(video.id).build())
                    }
                    !details.videoUrl.isNullOrBlank() && !details.audioUrl.isNullOrBlank() -> {
                        val vSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.Builder().setUri(details.videoUrl).setMediaId(video.id).build())
                        val aSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.Builder().setUri(details.audioUrl).setMediaId(video.id).build())
                        MergingMediaSource(vSource, aSource)
                    }
                    !details.videoUrl.isNullOrBlank() -> {
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.Builder().setUri(details.videoUrl).setMediaId(video.id).build())
                    }
                    else -> null
                }

                if (mediaSource != null) {
                    player.setMediaSource(mediaSource)
                    player.prepare()
                    player.play()
                    // BARU: Set ID video yang sedang diputar setelah media source baru berhasil dipasang ke player
                    _currentPlayingVideoId.value = video.id
                }
            }
        }
    }

    // DINONAKTIFKAN: prefetchUpcomingShorts lama yang hanya mengambil metadata tanpa pra-unduh data stream video
    // private fun prefetchUpcomingShorts(currentIndex: Int) {
    //     val list = _shorts.value
    //     list.getOrNull(currentIndex + 1)?.let { nextVid ->
    //         viewModelScope.launch(Dispatchers.IO) {
    //             try {
    //                 YouTubeRepository.getVideoPlaybackDetails(nextVid.id)
    //             } catch (_: Exception) {}
    //         }
    //     }
    //     list.getOrNull(currentIndex + 2)?.let { nextNextVid ->
    //         viewModelScope.launch(Dispatchers.IO) {
    //             try {
    //                 YouTubeRepository.getVideoPlaybackDetails(nextNextVid.id)
    //             } catch (_: Exception) {}
    //         }
    //     }
    // }
    // BARU: Pre-caching multi-level ala Instagram Reels & TikTok (Metadata + 512 KB stream head video berikutnya ke disk cache)
    private fun prefetchUpcomingShorts(currentIndex: Int) {
        val list = _shorts.value
        val context = appContext
        list.getOrNull(currentIndex + 1)?.let { nextVid ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = YouTubeRepository.getVideoPlaybackDetails(nextVid.id)
                    result.onSuccess { details ->
                        if (context != null) {
                            details.videoUrl?.let { vUrl ->
                                MediaCacheManager.prefetchStreamHead(context, vUrl, 512 * 1024L)
                            }
                            details.audioUrl?.let { aUrl ->
                                MediaCacheManager.prefetchStreamHead(context, aUrl, 256 * 1024L)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        list.getOrNull(currentIndex + 2)?.let { nextNextVid ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    YouTubeRepository.getVideoPlaybackDetails(nextNextVid.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun pausePlayer() {
        exoPlayerInstance?.pause()
    }

    fun resumePlayer() {
        exoPlayerInstance?.play()
    }

    fun togglePlayPause() {
        val player = exoPlayerInstance ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayerInstance?.release()
        exoPlayerInstance = null
    }
}
