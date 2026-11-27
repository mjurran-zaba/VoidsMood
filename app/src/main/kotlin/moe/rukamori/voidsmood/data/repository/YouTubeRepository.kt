// meticulous and honest
package moe.rukamori.voidsmood.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
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
                    val downloader = NewPipeDownloader()
                    NewPipe.init(
                        downloader,
                        Localization("id", "ID"),
                        ContentCountry("ID")
                    )
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
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(seed)
            searchExtractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(searchExtractor)

            val videoItems = searchInfo.relatedItems
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

            if (videoItems.isNotEmpty()) {
                Result.success(videoItems)
            } else {
                // Fallback ke Kiosk Trending bawaan
                val kioskList = service.kioskList
                val defaultKiosk = kioskList.defaultKioskId ?: "Trending"
                val extractor = kioskList.getExtractorById(defaultKiosk, null)
                extractor.fetchPage()
                val kioskInfo = KioskInfo.getInfo(extractor)
                val fallbackItems = kioskInfo.relatedItems
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
                Result.success(fallbackItems)
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

            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(nextSeed)
            searchExtractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(searchExtractor)

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
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(searchExtractor)

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

    // BARU: Mengambil daftar video Shorts YouTube (#shorts viral / trending) untuk ShortsScreen
    suspend fun getShortsFeed(refresh: Boolean = false): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val query = if (refresh) "#shorts trending" else "#shorts viral indonesia"
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(searchExtractor)

            val shorts = searchInfo.relatedItems
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

            Result.success(shorts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BARU: Mengambil daftar video kategori Explore / Jelajah (Trending, Musik, Gaming, Berita, Animasi)
    suspend fun getExploreVideos(category: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val query = when (category) {
                "Musik" -> "Trending Musik Indonesia"
                "Gaming" -> "Trending Game Indonesia"
                "Berita" -> "Berita Hari Ini Indonesia"
                "Animasi" -> "Animasi Populer Indonesia"
                else -> "Trending Indonesia Hari Ini"
            }
            searchVideos(query)
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
                val streamUrl = s.url
                val r = s.resolution?.trim() ?: if (s.height > 0) "${s.height}p" else null
                if (!r.isNullOrBlank() && !streamUrl.isNullOrBlank() && !resMap.containsKey(r)) {
                    resMap[r] = streamUrl
                    rawResolutions.add(r)
                }
            }

            info.videoOnlyStreams?.forEach { s ->
                val streamUrl = s.url
                val r = s.resolution?.trim() ?: if (s.height > 0) "${s.height}p" else null
                if (!r.isNullOrBlank() && !streamUrl.isNullOrBlank() && !resMap.containsKey(r)) {
                    resMap[r] = streamUrl
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

            // DINONAKTIFKAN: Pemilihan stream videoOnly dan audio lama yang rentan memilih AV1 / 4K pada kompilasi video panjang
            // // Cari stream video progressive (video + audio muxed) jika tersedia
            // val muxedProgressive = info.videoStreams
            //     ?.filter { !it.isVideoOnly }
            //     ?.maxByOrNull { it.height }
            //
            // // Cari video-only stream terbaik (720p atau 480p ideal untuk hardware ARMv7 Infinix)
            // val videoOnlyStream = info.videoOnlyStreams
            //     ?.firstOrNull { it.resolution == "720p" }
            //     ?: info.videoOnlyStreams?.firstOrNull { it.resolution == "480p" }
            //     ?: info.videoOnlyStreams?.firstOrNull { it.resolution == "360p" }
            //     ?: info.videoOnlyStreams?.firstOrNull()
            //
            // val selectedVideoUrl = muxedProgressive?.url ?: videoOnlyStream?.url
            //
            // // Cari stream audio dengan bitrate optimal
            // val selectedAudioUrl = info.audioStreams
            //     ?.maxByOrNull { it.averageBitrate }
            //     ?.url
            //     ?: info.audioStreams?.firstOrNull()?.url
            // BARU: Ekstraksi stream video dan audio yang dioptimalkan untuk kompatibilitas perangkat ARMv7 (Infinix dsb.)
            // Memprioritaskan MP4/AVC (H.264) untuk video dan M4A/AAC untuk audio, mencegah error decoder pada video panjang/kompilasi
            val muxedProgressive = info.videoStreams
                ?.filter { !it.isVideoOnly }
                ?.maxByOrNull { it.height }

            val mp4VideoOnlyStreams = info.videoOnlyStreams?.filter { s ->
                s.format == MediaFormat.MPEG_4 || s.codec?.startsWith("avc", ignoreCase = true) == true
            }

            val videoOnlyStream = mp4VideoOnlyStreams?.firstOrNull { it.height in 700..750 || it.resolution?.startsWith("720") == true }
                ?: mp4VideoOnlyStreams?.firstOrNull { it.height in 460..500 || it.resolution?.startsWith("480") == true }
                ?: mp4VideoOnlyStreams?.firstOrNull { it.height in 340..380 || it.resolution?.startsWith("360") == true }
                ?: mp4VideoOnlyStreams?.firstOrNull { it.height in 1000..1100 || it.resolution?.startsWith("1080") == true }
                ?: mp4VideoOnlyStreams?.firstOrNull()
                ?: info.videoOnlyStreams?.firstOrNull { it.height in 700..750 || it.resolution?.startsWith("720") == true }
                ?: info.videoOnlyStreams?.firstOrNull { it.height in 460..500 || it.resolution?.startsWith("480") == true }
                ?: info.videoOnlyStreams?.firstOrNull { it.height in 340..380 || it.resolution?.startsWith("360") == true }
                ?: info.videoOnlyStreams?.firstOrNull()

            val selectedVideoUrl = muxedProgressive?.url ?: videoOnlyStream?.url

            // Cari stream audio dengan bitrate optimal (prioritaskan format M4A/AAC untuk hardware decoder Android universal)
            val selectedAudioUrl = info.audioStreams
                ?.filter { it.format == MediaFormat.M4A || it.codec?.contains("mp4a", ignoreCase = true) == true }
                ?.maxByOrNull { it.averageBitrate }
                ?.url
                ?: info.audioStreams?.maxByOrNull { it.averageBitrate }?.url
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

            // DINONAKTIFKAN: isLive = !info.hlsUrl.isNullOrBlank()
            // Menandai video sebagai live hanya berdasarkan keberadaan hlsUrl keliru karena YouTube juga menyediakannya pada VOD,
            // sehingga memaksa player memutar HLS yang ditolak 403 oleh YouTube.
            // isLive = !info.hlsUrl.isNullOrBlank(),
            // BARU: Penentuan status Live streaming sejati berdasarkan StreamType dari NewPipeExtractor
            val isLiveStream = info.streamType == StreamType.LIVE_STREAM

            val details = VideoPlaybackDetails(
                video = baseVideo,
                hlsUrl = cleanHls,
                videoUrl = selectedVideoUrl,
                audioUrl = selectedAudioUrl,
                isProgressiveMuxed = (muxedProgressive != null),
                isLive = isLiveStream,
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
