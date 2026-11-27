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
import moe.rukamori.voidsmood.data.repository.YouTubeRepository

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val videos: List<VideoItem>,
        val isSearching: Boolean = false,
        val query: String = ""
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.getTrendingVideos()
            result.onSuccess { items ->
                _uiState.value = HomeUiState.Success(videos = items, isSearching = false)
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(
                    error.localizedMessage ?: "Gagal memuat feed YouTube. Cek koneksi internet."
                )
            }
        }
    }

    fun refreshTrending() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val result = YouTubeRepository.getTrendingVideos()
            result.onSuccess { items ->
                _uiState.value = HomeUiState.Success(videos = items, isSearching = false)
            }
            _isRefreshing.value = false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            loadTrending()
            return
        }
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.searchVideos(query)
            result.onSuccess { items ->
                _uiState.value = HomeUiState.Success(videos = items, isSearching = true, query = query)
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(
                    error.localizedMessage ?: "Pencarian '$query' gagal. Coba lagi."
                )
            }
        }
    }
}
