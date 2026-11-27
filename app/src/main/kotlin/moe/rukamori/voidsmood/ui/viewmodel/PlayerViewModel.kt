// meticulous and honest
package moe.rukamori.voidsmood.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.repository.YouTubeRepository

// BARU: Status mode tampilan pemutar video (Dismissed / Mini / Full)
enum class PlayerMode {
    DISMISSED, // Tidak ada video yang sedang diputar
    MINI,      // Miniplayer bilah horizontal YouTube docked di atas navigasi
    FULL       // Tampilan penuh pemutar video (PlayerScreen)
}

sealed interface PlayerUiState {
    data object Idle : PlayerUiState
    data class Loading(val video: VideoItem) : PlayerUiState
    data class Success(val details: VideoPlaybackDetails) : PlayerUiState
    data class Error(val video: VideoItem, val message: String) : PlayerUiState
}

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Status mode pemutar (Dismissed, Mini, Full)
    private val _playerMode = MutableStateFlow(PlayerMode.DISMISSED)
    val playerMode: StateFlow<PlayerMode> = _playerMode.asStateFlow()

    // Video yang sedang aktif
    private val _activeVideo = MutableStateFlow<VideoItem?>(null)
    val activeVideo: StateFlow<VideoItem?> = _activeVideo.asStateFlow()

    // Status pemutaran reaktif untuk sinkronisasi Miniplayer dan Full Player
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private var exoPlayerInstance: ExoPlayer? = null
    private var isPolling = false

    fun getOrCreateExoPlayer(context: Context): ExoPlayer {
        if (exoPlayerInstance == null) {
            // DINONAKTIFKAN: ExoPlayer.Builder bawaan tanpa tuning LoadControl yang menyebabkan buffering lama
            // exoPlayerInstance = ExoPlayer.Builder(context.applicationContext).build()
            // BARU: DefaultLoadControl dengan buffer instan (800ms buffer awal) untuk menghilangkan slowly network
            val fastStartLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 12_000,
                    /* maxBufferMs = */ 30_000,
                    /* bufferForPlaybackMs = */ 800,
                    /* bufferForPlaybackAfterRebufferMs = */ 1_500
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            exoPlayerInstance = ExoPlayer.Builder(context.applicationContext)
                .setLoadControl(fastStartLoadControl)
                .build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = this@apply.duration.coerceAtLeast(0L)
                        } else if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }
                })
            }
            startPositionPolling()
        }
        return exoPlayerInstance!!
    }

    private fun startPositionPolling() {
        if (isPolling) return
        isPolling = true
        viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                exoPlayerInstance?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                        _bufferedPosition.value = player.bufferedPosition.coerceAtLeast(0L)
                        _duration.value = player.duration.coerceAtLeast(0L)
                    }
                }
                delay(250)
            }
        }
    }

    // DINONAKTIFKAN: openVideo yang selalu memanggil loadVideo(video) tanpa mengecek apakah video yang sama sudah aktif
    // fun openVideo(video: VideoItem) {
    //     _activeVideo.value = video
    //     _playerMode.value = PlayerMode.FULL
    //     loadVideo(video)
    // }
    // BARU: Jika video yang dipilih sama dengan video yang sedang aktif dan player tidak IDLE,
    // cukup ubah mode ke FULL tanpa me-reload agar video tidak terulang dari detik 0.
    fun openVideo(video: VideoItem) {
        val isSameActive = _activeVideo.value?.id == video.id &&
            exoPlayerInstance?.playbackState != Player.STATE_IDLE &&
            exoPlayerInstance?.playbackState != null
        _activeVideo.value = video
        _playerMode.value = PlayerMode.FULL
        if (!isSameActive) {
            loadVideo(video)
        }
    }

    fun minimizePlayer() {
        if (_activeVideo.value != null) {
            _playerMode.value = PlayerMode.MINI
        }
    }

    fun expandPlayer() {
        if (_activeVideo.value != null) {
            _playerMode.value = PlayerMode.FULL
        }
    }

    fun togglePlayPause() {
        exoPlayerInstance?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun loadVideo(video: VideoItem) {
        _uiState.value = PlayerUiState.Loading(video)
        viewModelScope.launch(Dispatchers.IO) {
            val target = if (video.url.isNotBlank()) video.url else video.id
            val result = YouTubeRepository.getVideoPlaybackDetails(target)
            result.onSuccess { details ->
                _uiState.value = PlayerUiState.Success(details)
            }.onFailure { error ->
                _uiState.value = PlayerUiState.Error(
                    video = video,
                    message = error.localizedMessage ?: "Gagal memuat video YouTube. Silakan coba lagi."
                )
            }
        }
    }

    fun closePlayer() {
        exoPlayerInstance?.stop()
        exoPlayerInstance?.clearMediaItems()
        _uiState.value = PlayerUiState.Idle
        _activeVideo.value = null
        _playerMode.value = PlayerMode.DISMISSED
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayerInstance?.release()
        exoPlayerInstance = null
        isPolling = false
    }
}
