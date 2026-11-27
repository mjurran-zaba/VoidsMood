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

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // BARU: State topik kategori terpilih di bilah atas Home
    private val _selectedTopic = MutableStateFlow("Semua")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    // BARU: Daftar Shorts untuk disisipkan (interleaved) di rak Shorts Home feed
    private val _homeShorts = MutableStateFlow<List<VideoItem>>(emptyList())
    val homeShorts: StateFlow<List<VideoItem>> = _homeShorts.asStateFlow()

    private var pageOffset = 0
    private var currentVideos = mutableListOf<VideoItem>()

    init {
        loadTrending()
        loadHomeShorts()
    }

    private fun loadHomeShorts() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.getShortsFeed(refresh = false)
            result.onSuccess { list ->
                _homeShorts.value = list.take(12)
            }
        }
    }

    fun loadTrending(forceReload: Boolean = false) {
        if (!forceReload && currentVideos.isNotEmpty() && _selectedTopic.value == "Semua") {
            _uiState.value = HomeUiState.Success(videos = currentVideos.toList(), isSearching = false)
            return
        }
        _uiState.value = HomeUiState.Loading
        pageOffset = 0
        _selectedTopic.value = "Semua"
        viewModelScope.launch(Dispatchers.IO) {
            val result = YouTubeRepository.getTrendingVideos(refresh = false)
            result.onSuccess { items ->
                currentVideos = items.toMutableList()
                _uiState.value = HomeUiState.Success(videos = currentVideos.toList(), isSearching = false)
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(
                    error.localizedMessage ?: "Gagal memuat feed YouTube. Cek koneksi internet."
                )
            }
        }
    }

    // BARU: Pemilihan topik kapsul filter di Beranda (Semua, Musik, Gaming, Podcast, Sains & Teknologi, Berita, Animasi, Otomotif)
    fun selectTopic(topic: String) {
        if (_selectedTopic.value == topic && _uiState.value is HomeUiState.Success) return
        _selectedTopic.value = topic
        if (topic == "Semua") {
            loadTrending(forceReload = true)
        } else {
            _uiState.value = HomeUiState.Loading
            pageOffset = 0
            viewModelScope.launch(Dispatchers.IO) {
                val query = when (topic) {
                    "Musik" -> "musik indonesia trending hits"
                    "Gaming" -> "gameplay indonesia gaming trending"
                    "Podcast" -> "podcast indonesia terbaru"
                    "Sains & Teknologi" -> "sains teknologi gadget review"
                    "Berita" -> "berita terkini terpopuler"
                    "Animasi" -> "animasi keren kartun"
                    "Otomotif" -> "otomotif mobil motor modifikasi"
                    else -> topic
                }
                val result = YouTubeRepository.searchVideos(query)
                result.onSuccess { items ->
                    currentVideos = items.distinctBy { it.id }.toMutableList()
                    _uiState.value = HomeUiState.Success(videos = currentVideos.toList(), isSearching = false)
                }.onFailure { err ->
                    _uiState.value = HomeUiState.Error(err.localizedMessage ?: "Gagal memuat kategori $topic.")
                }
            }
        }
    }

    // BARU: Refresh cerdas yang mengambil pool video baru yang segar setiap kali ditarik
    fun refreshTrending() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            pageOffset = 0
            if (_selectedTopic.value == "Semua") {
                val result = YouTubeRepository.getTrendingVideos(refresh = true)
                result.onSuccess { items ->
                    currentVideos = items.distinctBy { it.id }.toMutableList()
                    _uiState.value = HomeUiState.Success(videos = currentVideos.toList(), isSearching = false)
                }
                val shortsResult = YouTubeRepository.getShortsFeed(refresh = true)
                shortsResult.onSuccess { list ->
                    _homeShorts.value = list.take(12)
                }
            } else {
                val topic = _selectedTopic.value
                val query = when (topic) {
                    "Musik" -> "musik indonesia trending hits"
                    "Gaming" -> "gameplay indonesia gaming trending"
                    "Podcast" -> "podcast indonesia terbaru"
                    "Sains & Teknologi" -> "sains teknologi gadget review"
                    "Berita" -> "berita terkini terpopuler"
                    "Animasi" -> "animasi keren kartun"
                    "Otomotif" -> "otomotif mobil motor modifikasi"
                    else -> topic
                }
                val result = YouTubeRepository.searchVideos(query)
                result.onSuccess { items ->
                    currentVideos = items.distinctBy { it.id }.toMutableList()
                    _uiState.value = HomeUiState.Success(videos = currentVideos.toList(), isSearching = false)
                }
            }
            _isRefreshing.value = false
        }
    }

    // BARU: Infinite Scroll pagination yang memuat 30-50 video tambahan saat scroll ke bawah
    fun loadMore() {
        if (_isLoadingMore.value || _uiState.value !is HomeUiState.Success) return
        val currentState = _uiState.value as HomeUiState.Success
        if (currentState.isSearching) return

        _isLoadingMore.value = true
        pageOffset++
        viewModelScope.launch(Dispatchers.IO) {
            val currentTopic = _selectedTopic.value
            val result = if (currentTopic == "Semua") {
                YouTubeRepository.loadMoreVideos(pageOffset)
            } else {
                val query = when (currentTopic) {
                    "Musik" -> "musik trending"
                    "Gaming" -> "gameplay trending"
                    "Podcast" -> "podcast populer"
                    "Sains & Teknologi" -> "teknologi terpopuler"
                    "Berita" -> "berita dunia indonesia"
                    "Animasi" -> "animasi terbaik"
                    "Otomotif" -> "review otomotif mobil"
                    else -> currentTopic
                }
                YouTubeRepository.searchVideos("$query $pageOffset")
            }
            result.onSuccess { newItems ->
                if (newItems.isNotEmpty()) {
                    // DINONAKTIFKAN: currentVideos.addAll(newItems) tanpa deduplikasi ID
                    // BARU: Gabungkan dan filter unik distinctBy { it.id } agar tidak ada benturan key di Jetpack Compose
                    val combined = (currentVideos + newItems).distinctBy { it.id }
                    currentVideos = combined.toMutableList()
                    _uiState.value = currentState.copy(videos = currentVideos.toList())
                }
            }
            _isLoadingMore.value = false
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
                currentVideos = items.toMutableList()
                _uiState.value = HomeUiState.Success(videos = currentVideos.toList(), isSearching = true, query = query)
            }.onFailure { error ->
                _uiState.value = HomeUiState.Error(
                    error.localizedMessage ?: "Pencarian '$query' gagal. Coba lagi."
                )
            }
        }
    }
}
