// meticulous and honest
package moe.rukamori.voidsmood.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.rukamori.voidsmood.data.model.VideoItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository lokal untuk menyimpan Riwayat Tontonan, Video yang Disukai,
 * dan Tonton Nanti secara 100% lokal di HP tanpa memerlukan login akun Google.
 */
object LocalLibraryRepository {
    private const val PREFS_NAME = "voidsmood_library_prefs"
    private const val KEY_HISTORY = "history_videos"
    private const val KEY_LIKED = "liked_videos"
    private const val KEY_WATCH_LATER = "watch_later_videos"
    private const val KEY_SEARCH_HISTORY = "search_history_queries"

    private var prefs: SharedPreferences? = null

    private val _history = MutableStateFlow<List<VideoItem>>(emptyList())
    val history: StateFlow<List<VideoItem>> = _history.asStateFlow()

    private val _liked = MutableStateFlow<List<VideoItem>>(emptyList())
    val liked: StateFlow<List<VideoItem>> = _liked.asStateFlow()

    private val _watchLater = MutableStateFlow<List<VideoItem>>(emptyList())
    val watchLater: StateFlow<List<VideoItem>> = _watchLater.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadAll()
        }
    }

    private fun loadAll() {
        val p = prefs ?: return
        _history.value = loadList(p.getString(KEY_HISTORY, null))
        _liked.value = loadList(p.getString(KEY_LIKED, null))
        _watchLater.value = loadList(p.getString(KEY_WATCH_LATER, null))
        _searchHistory.value = loadStringList(p.getString(KEY_SEARCH_HISTORY, null))
    }

    // BARU: Menyimpan kueri pencarian ke riwayat lokal (maksimal 20 item, deduplikasi, urutan terbaru di atas)
    fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(20)
        _searchHistory.value = capped
        saveStringList(KEY_SEARCH_HISTORY, capped)
    }

    // BARU: Menghapus satu entri dari riwayat pencarian
    fun deleteSearchQuery(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        _searchHistory.value = current
        saveStringList(KEY_SEARCH_HISTORY, current)
    }

    // BARU: Menghapus seluruh riwayat pencarian
    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        prefs?.edit()?.remove(KEY_SEARCH_HISTORY)?.apply()
    }

    fun addToHistory(video: VideoItem) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        val trimmed = current.take(50)
        _history.value = trimmed
        saveList(KEY_HISTORY, trimmed)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs?.edit()?.remove(KEY_HISTORY)?.apply()
    }

    fun toggleLike(video: VideoItem): Boolean {
        val current = _liked.value.toMutableList()
        val exists = current.any { it.id == video.id }
        if (exists) {
            current.removeAll { it.id == video.id }
        } else {
            current.add(0, video)
        }
        _liked.value = current
        saveList(KEY_LIKED, current)
        return !exists
    }

    fun isLiked(videoId: String): Boolean {
        return _liked.value.any { it.id == videoId }
    }

    fun toggleWatchLater(video: VideoItem): Boolean {
        val current = _watchLater.value.toMutableList()
        val exists = current.any { it.id == video.id }
        if (exists) {
            current.removeAll { it.id == video.id }
        } else {
            current.add(0, video)
        }
        _watchLater.value = current
        saveList(KEY_WATCH_LATER, current)
        return !exists
    }

    fun isWatchLater(videoId: String): Boolean {
        return _watchLater.value.any { it.id == videoId }
    }

    private fun saveList(key: String, list: List<VideoItem>) {
        val p = prefs ?: return
        val array = JSONArray()
        list.forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("url", v.url)
                put("title", v.title)
                put("uploaderName", v.uploaderName)
                put("uploaderAvatarUrl", v.uploaderAvatarUrl ?: "")
                put("thumbnailUrl", v.thumbnailUrl)
                put("durationSeconds", v.durationSeconds)
                put("durationText", v.durationText)
                put("viewCount", v.viewCount)
                put("viewCountText", v.viewCountText)
                put("uploadDateText", v.uploadDateText)
            }
            array.put(obj)
        }
        p.edit().putString(key, array.toString()).apply()
    }

    private fun loadList(rawJson: String?): List<VideoItem> {
        if (rawJson.isNullOrBlank()) return emptyList()
        val list = mutableListOf<VideoItem>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VideoItem(
                        id = obj.optString("id"),
                        url = obj.optString("url"),
                        title = obj.optString("title"),
                        uploaderName = obj.optString("uploaderName"),
                        uploaderAvatarUrl = obj.optString("uploaderAvatarUrl").takeIf { it.isNotBlank() },
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        durationSeconds = obj.optLong("durationSeconds"),
                        durationText = obj.optString("durationText"),
                        viewCount = obj.optLong("viewCount"),
                        viewCountText = obj.optString("viewCountText"),
                        uploadDateText = obj.optString("uploadDateText")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveStringList(key: String, list: List<String>) {
        val p = prefs ?: return
        val array = JSONArray()
        list.forEach { array.put(it) }
        p.edit().putString(key, array.toString()).apply()
    }

    private fun loadStringList(rawJson: String?): List<String> {
        if (rawJson.isNullOrBlank()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
        } catch (_: Exception) {}
        return list
    }
}
