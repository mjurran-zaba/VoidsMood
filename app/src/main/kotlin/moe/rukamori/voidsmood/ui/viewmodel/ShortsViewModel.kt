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

    fun loadMore() {
        if (_isLoadingMore.value || _shorts.value.isEmpty()) return
        _isLoadingMore.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.loadMoreShortsFeed(pageOffset)
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

    fun playShortAt(index: Int) {
        val list = _shorts.value
        val video = list.getOrNull(index) ?: return
        _currentPage.value = index

        if (currentPlayingVideoId == video.id && exoPlayerInstance?.playbackState == Player.STATE_READY) {
            exoPlayerInstance?.play()
            return
        }

        currentPlayingVideoId = video.id
        LocalLibraryRepository.addToHistory(video)
        _isBuffering.value = true

        val player = exoPlayerInstance ?: return
        player.pause()
        player.stop()
        player.clearMediaItems()

        viewModelScope.launch(Dispatchers.IO) {
            val detailsResult = YouTubeRepository.getVideoPlaybackDetails(video.id)
            detailsResult.onSuccess { details ->
                val httpDataSourceFactory = OkHttpDataSource.Factory(NetworkClient.okHttpClient)
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
                    launch(Dispatchers.Main) {
                        player.setMediaSource(mediaSource)
                        player.prepare()
                        player.play()
                    }
                }
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
