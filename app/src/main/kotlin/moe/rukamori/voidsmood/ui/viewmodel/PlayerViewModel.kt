// meticulous and honest
package moe.rukamori.voidsmood.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.repository.YouTubeRepository

sealed interface PlayerUiState {
    data object Idle : PlayerUiState
    data class Loading(val video: VideoItem) : PlayerUiState
    data class Success(val details: VideoPlaybackDetails) : PlayerUiState
    data class Error(val video: VideoItem, val message: String) : PlayerUiState
}

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

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
        _uiState.value = PlayerUiState.Idle
    }
}
