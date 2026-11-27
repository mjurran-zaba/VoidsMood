// meticulous and honest
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
    private var currentPlayingVideoId: String? = null
    private var pageOffset = 1

    init {
        loadShorts(refresh = false)
    }

    fun getOrCreateShortsPlayer(context: Context): ExoPlayer {
        if (exoPlayerInstance == null) {
            val shortsLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 8_000,
                    /* maxBufferMs = */ 20_000,
                    /* bufferForPlaybackMs = */ 600,
                    /* bufferForPlaybackAfterRebufferMs = */ 1_000
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

            if (currentPlayingVideoId == video.id && player.playbackState == Player.STATE_READY) {
                player.play()
                return@launch
            }

            currentPlayingVideoId = video.id
            LocalLibraryRepository.addToHistory(video)
            _isBuffering.value = true

            player.pause()
            player.stop()
            player.clearMediaItems()

            val detailsResult = withContext(Dispatchers.IO) {
                YouTubeRepository.getVideoPlaybackDetails(video.id)
            }

            detailsResult.onSuccess { details ->
                val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkClient.mediaStreamingOkHttpClient)
                    .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
                    .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))

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
                    else -> null
                }

                if (mediaSource != null) {
                    player.setMediaSource(mediaSource)
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    // BARU: Pre-fetching video Shorts berikutnya ke LRU Cache di background (0 ms jeda saat swipe)
    private fun prefetchUpcomingShorts(currentIndex: Int) {
        val list = _shorts.value
        list.getOrNull(currentIndex + 1)?.let { nextVid ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    YouTubeRepository.getVideoPlaybackDetails(nextVid.id)
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
