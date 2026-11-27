// meticulous and honest
package moe.rukamori.voidsmood.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object YouTubeRepository {

    @Volatile
    private var isInitialized = false

    // BARU: Kumpulan seed topik dinamis agar feed YouTube kaya, beragam (50+ video),
    // dan selalu berganti konten baru setiap kali ditarik untuk refresh (Pull-to-Refresh)
    private val FEED_SEEDS = listOf(
        "trending_gaming",
        "video viral indonesia",
        "trending_podcasts_episodes",
        "musik indonesia populer",
        "live",
        "berita terkini terpopuler",
        "gameplay indonesia"
    )

    private var currentSeedIndex = 0

    fun initIfNeeded() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    NewPipe.init(NewPipeDownloader.getInstance())
                    isInitialized = true
                }
            }
        }
    }

    // Mengambil daftar video beranda secara dinamis dengan rotasi seed saat refresh
    suspend fun getTrendingVideos(refresh: Boolean = false): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()

            if (refresh) {
                currentSeedIndex = (currentSeedIndex + 1) % FEED_SEEDS.size
            }

            val seed = FEED_SEEDS[currentSeedIndex]
            val allVideos = mutableListOf<VideoItem>()

            // 1. Eksekusi pencarian berdasarkan topik seed aktif untuk mendapatkan konten segar
            try {
                val searchInfo = SearchInfo.getInfo(
                    ServiceList.YouTube,
                    ServiceList.YouTube.searchQHFactory.fromQuery(seed),
                    ContentCountry("ID"),
                    Localization("id", "ID")
                )
                val searchItems = searchInfo.relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { item ->
                        val videoId = item.url.substringAfter("v=").substringBefore("&")
                        val thumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val avatar = item.uploaderAvatars.firstOrNull()?.url
                        VideoItem(
                            id = videoId,
                            url = item.url,
                            title = item.name ?: "Untitled",
                            uploaderName = item.uploaderName ?: "Unknown Creator",
                            uploaderAvatarUrl = avatar,
                            thumbnailUrl = thumb,
                            durationSeconds = item.duration,
                            durationText = VideoItem.formatDuration(item.duration),
                            viewCount = item.viewCount,
                            viewCountText = VideoItem.formatViews(item.viewCount),
                            uploadDateText = item.textualUploadDate ?: ""
                        )
                    }
                allVideos.addAll(searchItems)
            } catch (_: Exception) {}

            // 2. Ambil juga feed kiosk Trending bawaan sebagai pengayaan
            try {
                val defaultKiosk = ServiceList.YouTube.kioskList.defaultKioskId
                val kioskInfo = KioskInfo.getInfo(
                    ServiceList.YouTube,
                    ServiceList.YouTube.kioskList.getListFor(defaultKiosk).contentFilter[0],
                    ContentCountry("ID"),
                    Localization("id", "ID")
                )
                val kioskVideos = kioskInfo.relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { item ->
                        val videoId = item.url.substringAfter("v=").substringBefore("&")
                        val thumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val avatar = item.uploaderAvatars.firstOrNull()?.url
                        VideoItem(
                            id = videoId,
                            url = item.url,
                            title = item.name ?: "Untitled",
                            uploaderName = item.uploaderName ?: "Unknown Creator",
                            uploaderAvatarUrl = avatar,
                            thumbnailUrl = thumb,
                            durationSeconds = item.duration,
                            durationText = VideoItem.formatDuration(item.duration),
                            viewCount = item.viewCount,
                            viewCountText = VideoItem.formatViews(item.viewCount),
                            uploadDateText = item.textualUploadDate ?: ""
                        )
                    }
                allVideos.addAll(kioskVideos)
            } catch (_: Exception) {}

            val distinctVideos = allVideos.distinctBy { it.id }
            if (distinctVideos.isNotEmpty()) {
                Result.success(distinctVideos)
            } else {
                Result.failure(Exception("Tidak dapat memuat video saat ini. Periksa koneksi internet."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BARU: Fungsi memuat batch video berikutnya secara berkelanjutan (Infinite Scrolling)
    suspend fun loadMoreVideos(pageOffset: Int): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val seedIndex = (currentSeedIndex + pageOffset) % FEED_SEEDS.size
            val nextSeed = FEED_SEEDS[seedIndex]

            val searchInfo = SearchInfo.getInfo(
                ServiceList.YouTube,
                ServiceList.YouTube.searchQHFactory.fromQuery(nextSeed),
                ContentCountry("ID"),
                Localization("id", "ID")
            )

            val newVideos = searchInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = item.url.substringAfter("v=").substringBefore("&")
                    val thumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    val avatar = item.uploaderAvatars.firstOrNull()?.url
                    VideoItem(
                        id = videoId,
                        url = item.url,
                        title = item.name ?: "Untitled",
                        uploaderName = item.uploaderName ?: "Unknown Creator",
                        uploaderAvatarUrl = avatar,
                        thumbnailUrl = thumb,
                        durationSeconds = item.duration,
                        durationText = VideoItem.formatDuration(item.duration),
                        viewCount = item.viewCount,
                        viewCountText = VideoItem.formatViews(item.viewCount),
                        uploadDateText = item.textualUploadDate ?: ""
                    )
                }
                .distinctBy { it.id }

            Result.success(newVideos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchVideos(query: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val searchInfo = SearchInfo.getInfo(
                ServiceList.YouTube,
                ServiceList.YouTube.searchQHFactory.fromQuery(query),
                ContentCountry("ID"),
                Localization("id", "ID")
            )

            val videos = searchInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = item.url.substringAfter("v=").substringBefore("&")
                    val thumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    val avatar = item.uploaderAvatars.firstOrNull()?.url
                    VideoItem(
                        id = videoId,
                        url = item.url,
                        title = item.name ?: "Untitled",
                        uploaderName = item.uploaderName ?: "Unknown Creator",
                        uploaderAvatarUrl = avatar,
                        thumbnailUrl = thumb,
                        durationSeconds = item.duration,
                        durationText = VideoItem.formatDuration(item.duration),
                        viewCount = item.viewCount,
                        viewCountText = VideoItem.formatViews(item.viewCount),
                        uploadDateText = item.textualUploadDate ?: ""
                    )
                }
                .distinctBy { it.id }

            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BARU: Ambil saran pencarian real-time dari YouTube (Search Suggestions)
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            initIfNeeded()
            val service = ServiceList.YouTube
            val suggestions = service.suggestionExtractor?.suggestionList(query)
            if (!suggestions.isNullOrEmpty()) {
                return@withContext suggestions
            }
        } catch (_: Exception) {}

        // Fallback langsung ke endpoint Google YouTube Suggest queries via HTTP
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=youtube&jsonp=JP&ds=yt&gl=ID&hl=id&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NewPipeDownloader.DEFAULT_USER_AGENT)
                .build()
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (body.startsWith("JP(") && body.endsWith(")")) {
                val jsonStr = body.substring(3, body.length - 1)
                val jsonArray = org.json.JSONArray(jsonStr)
                val collection = jsonArray.getJSONArray(1)
                val results = mutableListOf<String>()
                for (i in 0 until collection.length()) {
                    val item = collection.getJSONArray(i)
                    val text = item.getString(0)
                    if (!text.isNullOrBlank()) {
                        results.add(text)
                    }
                }
                return@withContext results
            }
        } catch (_: Exception) {}

        emptyList()
    }

    suspend fun getVideoPlaybackDetails(videoIdOrUrl: String): Result<VideoPlaybackDetails> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val url = if (videoIdOrUrl.startsWith("http")) videoIdOrUrl else "https://www.youtube.com/watch?v=$videoIdOrUrl"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            val videoId = if (videoIdOrUrl.contains("v=")) {
                videoIdOrUrl.substringAfter("v=").substringBefore("&")
            } else {
                videoIdOrUrl.substringAfterLast("/")
            }

            val thumb = info.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val avatar = info.uploaderAvatars.firstOrNull()?.url

            val baseVideo = VideoItem(
                id = videoId,
                url = url,
                title = info.name ?: "Untitled",
                uploaderName = info.uploaderName ?: "Unknown Creator",
                uploaderAvatarUrl = avatar,
                thumbnailUrl = thumb,
                durationSeconds = info.duration,
                durationText = VideoItem.formatDuration(info.duration),
                viewCount = info.viewCount,
                viewCountText = VideoItem.formatViews(info.viewCount),
                uploadDateText = info.textualUploadDate ?: ""
            )

            // Ekstraksi HLS Master Playlist (Menyediakan semua resolusi adaptif + audio secara terpadu)
            val rawHls = info.hlsUrl?.takeIf { it.isNotBlank() }
            val cleanHls = rawHls?.removeSuffix("?")

            // BARU: Ekstraksi seluruh pilihan resolusi video yang tersedia beserta URL stream-nya
            val resMap = mutableMapOf<String, String>()
            val rawResolutions = mutableListOf<String>()

            info.videoStreams?.forEach { s ->
                val r = s.resolution?.trim() ?: if (s.height > 0) "${s.height}p" else null
                if (!r.isNullOrBlank() && !resMap.containsKey(r) && !s.url.isNullOrBlank()) {
                    resMap[r] = s.url
                    rawResolutions.add(r)
                }
            }

            info.videoOnlyStreams?.forEach { s ->
                val r = s.resolution?.trim() ?: if (s.height > 0) "${s.height}p" else null
                if (!r.isNullOrBlank() && !resMap.containsKey(r) && !s.url.isNullOrBlank()) {
                    resMap[r] = s.url
                    rawResolutions.add(r)
                }
            }

            val sortedRes = rawResolutions.distinct().sortedByDescending { r ->
                r.replace("p", "").replace("60", "").trim().toIntOrNull() ?: 0
            }

            val finalResolutions = if (sortedRes.isNotEmpty()) {
                listOf("Auto") + sortedRes
            } else {
                listOf("Auto", "1080p", "720p", "480p", "360p", "240p", "144p")
            }

            // Cari stream video progressive (video + audio muxed) jika tersedia
            val muxedProgressive = info.videoStreams
                ?.filter { !it.isVideoOnly }
                ?.maxByOrNull { it.height }

            // Cari video-only stream terbaik (720p atau 480p ideal untuk hardware ARMv7 Infinix)
            val videoOnlyStream = info.videoOnlyStreams
                ?.firstOrNull { it.resolution == "720p" }
                ?: info.videoOnlyStreams?.firstOrNull { it.resolution == "480p" }
                ?: info.videoOnlyStreams?.firstOrNull { it.resolution == "360p" }
                ?: info.videoOnlyStreams?.firstOrNull()

            val selectedVideoUrl = muxedProgressive?.url ?: videoOnlyStream?.url

            // Cari stream audio dengan bitrate optimal
            val selectedAudioUrl = info.audioStreams
                ?.maxByOrNull { it.averageBitrate }
                ?.url
                ?: info.audioStreams?.firstOrNull()?.url

            val related = info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val relId = item.url.substringAfter("v=").substringBefore("&")
                    val relThumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$relId/hqdefault.jpg"
                    val relAvatar = item.uploaderAvatars.firstOrNull()?.url
                    VideoItem(
                        id = relId,
                        url = item.url,
                        title = item.name ?: "Untitled",
                        uploaderName = item.uploaderName ?: "Unknown Creator",
                        uploaderAvatarUrl = relAvatar,
                        thumbnailUrl = relThumb,
                        durationSeconds = item.duration,
                        durationText = VideoItem.formatDuration(item.duration),
                        viewCount = item.viewCount,
                        viewCountText = VideoItem.formatViews(item.viewCount),
                        uploadDateText = item.textualUploadDate ?: ""
                    )
                }

            // BARU: Pembersihan teks deskripsi video secara menyeluruh dari tag HTML <br>, <p>, anchor, dan entitas HTML
            val cleanDescription = cleanHtmlDescription(info.description?.content ?: "")

            val details = VideoPlaybackDetails(
                video = baseVideo,
                hlsUrl = cleanHls,
                videoUrl = selectedVideoUrl,
                audioUrl = selectedAudioUrl,
                isProgressiveMuxed = (muxedProgressive != null),
                isLive = !info.hlsUrl.isNullOrBlank(),
                description = cleanDescription,
                subscriberCountText = if (info.uploaderSubscriberCount > 0) "${VideoItem.formatCount(info.uploaderSubscriberCount)} subscriber" else "",
                likeCountText = if (info.likeCount > 0) VideoItem.formatCount(info.likeCount) else "Suka",
                dislikeCountText = if (info.dislikeCount > 0) VideoItem.formatCount(info.dislikeCount) else "",
                relatedVideos = related,
                availableResolutions = finalResolutions,
                resolutionStreams = resMap
            )

            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BARU: Fungsi pembersih tag HTML dan pemformat teks deskripsi YouTube
    // Mengubah <br> menjadi newline dan membersihkan entitas &amp;, &quot;, dsb.
    fun cleanHtmlDescription(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</?p\\s*>"), "\n\n")
            .replace(Regex("(?i)<a\\s+[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>"), "$2 ($1)")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
