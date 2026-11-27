// meticulous and honest
package moe.rukamori.voidsmood.data.repository

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.network.NetworkClient
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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

    // BARU: Regex ekstraksi YouTube ID komprehensif (standar PipePipe DeArrowVideoId)
    // Mendukung tautan /shorts/, v=, /embed/, youtu.be, /live/, dan raw 11-char ID
    private val VIDEO_ID_PATTERN = java.util.regex.Pattern.compile(
        "(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)([A-Za-z0-9_-]{11})"
    )

    fun extractVideoId(urlOrId: String?): String {
        if (urlOrId.isNullOrBlank()) return ""
        if (urlOrId.length == 11 && !urlOrId.contains("/") && !urlOrId.contains("?") && !urlOrId.contains("&")) {
            return urlOrId
        }
        val matcher = VIDEO_ID_PATTERN.matcher(urlOrId)
        return if (matcher.find()) {
            matcher.group(1) ?: urlOrId
        } else if (urlOrId.contains("v=")) {
            urlOrId.substringAfter("v=").substringBefore("&").substringBefore("?")
        } else if (urlOrId.contains("/shorts/")) {
            urlOrId.substringAfter("/shorts/").substringBefore("?").substringBefore("&").substringBefore("/")
        } else {
            urlOrId
        }
    }

    // DINONAKTIFKAN: 7 seed topik lama yang hanya mengunci pencarian ke Indonesia sehingga konten sangat sedikit
    // private val FEED_SEEDS = listOf(
    //     "trending_gaming",
    //     "video viral indonesia",
    //     "trending_podcasts_episodes",
    //     "musik indonesia populer",
    //     "live",
    //     "berita terkini terpopuler",
    //     "gameplay indonesia"
    // )
    // BARU: Kumpulan 30 seed topik global dinamis agar feed Home YouTube kaya, melimpah, beragam,
    // dan selalu berganti konten segar setiap kali ditarik untuk refresh (Pull-to-Refresh)
    private val FEED_SEEDS = listOf(
        "trending now",
        "viral videos",
        "top music hits",
        "trending gaming",
        "technology breakthroughs",
        "science discoveries",
        "cinematic 4k nature",
        "popular podcasts highlights",
        "documentary film",
        "gameplay walkthrough",
        "creative engineering",
        "standup comedy best",
        "lofi hip hop radio",
        "action movie scenes",
        "satisfying craftsmanship",
        "animation showcase",
        "space exploration nasa",
        "world street food",
        "esports tournament",
        "viral internet culture",
        "acoustic live session",
        "epic adventure travel",
        "automotive supercar",
        "magic and illusions",
        "fitness motivation",
        "behind the scenes cinema",
        "retro gaming nostalgia",
        "digital art and design",
        "unboxing tech",
        "epic orchestral music"
    )

    private var currentSeedIndex = 0

    // BARU: In-memory LRU Cache untuk menyimpan 50 detail stream video terakhir
    // Memungkinkan pemutaran ulang atau swipe kembali di Shorts/Home/Explore tanpa request jaringan ulang (0 ms latency)
    private val playbackDetailsCache = LruCache<String, VideoPlaybackDetails>(50)

    fun initIfNeeded() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    val downloader = NewPipeDownloader()
                    // DINONAKTIFKAN: Inisialisasi regional sempit ID ("id", "ID") yang membatasi konten ke Indonesia saja
                    // NewPipe.init(
                    //     downloader,
                    //     Localization("id", "ID"),
                    //     ContentCountry("ID")
                    // )
                    // BARU: Inisialisasi dengan standar open-source global (US ContentCountry) agar feed Shorts, Home, dan Jelajah melimpah tanpa limitasi wilayah
                    val defaultLocale = java.util.Locale.getDefault()
                    val lang = defaultLocale.language.ifBlank { "en" }
                    val country = defaultLocale.country.ifBlank { "US" }
                    NewPipe.init(
                        downloader,
                        Localization(lang, country),
                        ContentCountry("US")
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
                    val videoId = extractVideoId(item.url)
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
                .filter { it.id.isNotBlank() && it.id.length == 11 }
                .distinctBy { it.id }
                .toMutableList()

            // BARU: Jika hasil awal kurang dari 30 video, ambil juga dari seed berikutnya
            // agar beranda saat pertama kali dibuka langsung memiliki 35-50 video tanpa cepat mentok di batas bawah
            if (videoItems.size < 30) {
                try {
                    val nextSeed = FEED_SEEDS[(currentSeedIndex + 1) % FEED_SEEDS.size]
                    val nextExtractor = service.getSearchExtractor(nextSeed)
                    nextExtractor.fetchPage()
                    val nextInfo = SearchInfo.getInfo(nextExtractor)
                    val extraItems = nextInfo.relatedItems
                        .filterIsInstance<StreamInfoItem>()
                        .map { item ->
                            val videoId = extractVideoId(item.url)
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
                        .filter { it.id.isNotBlank() && it.id.length == 11 }
                    videoItems.addAll(extraItems)
                } catch (_: Exception) {}
            }

            val finalVideos = videoItems.distinctBy { it.id }

            if (finalVideos.isNotEmpty()) {
                Result.success(finalVideos)
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
                        val videoId = extractVideoId(item.url)
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
                    .filter { it.id.isNotBlank() && it.id.length == 11 }
                    .distinctBy { it.id }
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
                    val videoId = extractVideoId(item.url)
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
                .filter { it.id.isNotBlank() && it.id.length == 11 }
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
                    val videoId = extractVideoId(item.url)
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
                .filter { it.id.isNotBlank() && it.id.length == 11 }
                .distinctBy { it.id }

            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // DINONAKTIFKAN: getShortsFeed lama yang hanya mengambil 1 halaman awal dan terbatas 5-10 video
    // suspend fun getShortsFeed(refresh: Boolean = false): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
    //     try {
    //         initIfNeeded()
    //         val query = if (refresh) "#shorts trending" else "#shorts viral indonesia"
    //         val service = ServiceList.YouTube
    //         val searchExtractor = service.getSearchExtractor(query)
    //         searchExtractor.fetchPage()
    //         val searchInfo = SearchInfo.getInfo(searchExtractor)
    //         val shorts = searchInfo.relatedItems ...
    //     } catch (e: Exception) { Result.failure(e) }
    // }
    // DINONAKTIFKAN: getShortsFeed yang menggunakan query hashtag '#shorts ...' sehingga NewPipeExtractor
    // hanya mengembalikan 1 video (akibat rak reelShelfRenderer di YouTube tidak dibedah) dan substringAfter("v=") gagal pada /shorts/
    // suspend fun getShortsFeed(refresh: Boolean = false): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
    //     try {
    //         initIfNeeded()
    //         val query = if (refresh) "#shorts trending indonesia" else "#shorts viral indonesia"
    //         val service = ServiceList.YouTube
    //         val searchExtractor = service.getSearchExtractor(query)
    //         searchExtractor.fetchPage()
    //         val searchInfo = SearchInfo.getInfo(searchExtractor)
    //         ...
    //     } catch (e: Exception) { Result.failure(e) }
    // }

    // BARU: Kumpulan 20 kueri Shorts global organik (tanpa hashtag #) agar YouTube mengembalikan puluhan videoRenderer penuh
    private val SHORTS_SEEDS = listOf(
        "trending shorts",
        "viral shorts",
        "best shorts compilation",
        "satisfying shorts",
        "funny shorts",
        "amazing talents shorts",
        "gaming highlights shorts",
        "creative animation shorts",
        "science experiments shorts",
        "street food shorts",
        "mind blowing facts shorts",
        "cinematic shorts",
        "diy craft shorts",
        "magic tricks shorts",
        "skate and sports shorts",
        "pets and animals funny shorts",
        "illusions and art shorts",
        "music live acoustic shorts",
        "comedy sketches shorts",
        "inventions and tech shorts"
    )

    private var shortsSeedIndex = 0

    // BARU: Ekstraksi Shorts langsung dari InnerTube API yang membaca wadah 'gridShelfViewModel' & 'reelShelfRenderer'.
    // Ini mengembalikan 25-35 video Shorts vertikal murni per kueri (mengatasi bug NewPipeExtractor 0.27.1
    // yang membuang rak shorts dan hanya menyisakan 2-3 video).
    private fun fetchShortsFromInnerTube(query: String): List<VideoItem> {
        try {
            val jsonContext = JSONObject().apply {
                put("client", JSONObject().apply {
                    put("hl", "en")
                    put("gl", "US")
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240101.00.00")
                })
            }
            val jsonBody = JSONObject().apply {
                put("context", jsonContext)
                put("query", query)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("User-Agent", NewPipeDownloader.DEFAULT_USER_AGENT)
                .post(requestBody)
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val raw = response.body?.string() ?: return emptyList()
            val root = JSONObject(raw)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            val resultList = mutableListOf<VideoItem>()

            for (i in 0 until contents.length()) {
                val sec = contents.optJSONObject(i) ?: continue
                val itemSection = sec.optJSONObject("itemSectionRenderer") ?: continue
                val secContents = itemSection.optJSONArray("contents") ?: continue

                for (j in 0 until secContents.length()) {
                    val it = secContents.optJSONObject(j) ?: continue

                    // 1. Ekstraksi dari gridShelfViewModel (rak Shorts utama di YouTube Web)
                    val gridShelf = it.optJSONObject("gridShelfViewModel")
                    if (gridShelf != null) {
                        val gridContents = gridShelf.optJSONArray("contents")
                        if (gridContents != null) {
                            for (k in 0 until gridContents.length()) {
                                val itemObj = gridContents.optJSONObject(k) ?: continue
                                val sl = itemObj.optJSONObject("shortsLockupViewModel") ?: continue
                                val title = sl.optJSONObject("overlayMetadata")?.optJSONObject("primaryText")?.optString("content", "Untitled Shorts") ?: "Untitled Shorts"
                                val urlPath = sl.optJSONObject("onTap")?.optJSONObject("innertubeCommand")?.optJSONObject("commandMetadata")?.optJSONObject("webCommandMetadata")?.optString("url", "") ?: ""
                                val vidId = extractVideoId(urlPath)
                                val views = sl.optJSONObject("overlayMetadata")?.optJSONObject("secondaryText")?.optString("content", "") ?: ""
                                val sources = sl.optJSONObject("thumbnailViewModel")?.optJSONObject("thumbnailViewModel")?.optJSONObject("image")?.optJSONArray("sources")
                                val thumb = sources?.optJSONObject(0)?.optString("url") ?: "https://i.ytimg.com/vi/$vidId/hqdefault.jpg"

                                if (vidId.length == 11) {
                                    resultList.add(
                                        VideoItem(
                                            id = vidId,
                                            url = "https://www.youtube.com/shorts/$vidId",
                                            title = title,
                                            uploaderName = "YouTube Creator",
                                            uploaderAvatarUrl = null,
                                            thumbnailUrl = thumb,
                                            durationSeconds = 30L,
                                            durationText = "0:30",
                                            viewCount = 0L,
                                            viewCountText = views,
                                            uploadDateText = ""
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. Ekstraksi dari reelShelfRenderer (rak Reel / Shorts format alternatif)
                    val reelShelf = it.optJSONObject("reelShelfRenderer")
                    if (reelShelf != null) {
                        val items = reelShelf.optJSONArray("items")
                        if (items != null) {
                            for (k in 0 until items.length()) {
                                val itemObj = items.optJSONObject(k) ?: continue
                                val reel = itemObj.optJSONObject("reelItemRenderer") ?: continue
                                val vidId = reel.optString("videoId")
                                val title = reel.optJSONObject("headline")?.optString("simpleText", "Untitled Shorts") ?: "Untitled Shorts"
                                val views = reel.optJSONObject("viewCountText")?.optString("simpleText", "") ?: ""
                                val thumbs = reel.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val thumb = thumbs?.optJSONObject(0)?.optString("url") ?: "https://i.ytimg.com/vi/$vidId/hqdefault.jpg"

                                if (vidId.length == 11) {
                                    resultList.add(
                                        VideoItem(
                                            id = vidId,
                                            url = "https://www.youtube.com/shorts/$vidId",
                                            title = title,
                                            uploaderName = "YouTube Creator",
                                            uploaderAvatarUrl = null,
                                            thumbnailUrl = thumb,
                                            durationSeconds = 30L,
                                            durationText = "0:30",
                                            viewCount = 0L,
                                            viewCountText = views,
                                            uploadDateText = ""
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return resultList
        } catch (_: Exception) {
            return emptyList()
        }
    }

    // BARU: Mengambil feed YouTube Shorts organik (25-50 video vertikal) dengan multi-page fetching & regex ID extraction
    suspend fun getShortsFeed(refresh: Boolean = false): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            if (refresh) {
                shortsSeedIndex = (shortsSeedIndex + 1) % SHORTS_SEEDS.size
            }
            val query = SHORTS_SEEDS[shortsSeedIndex]
            val innerTubeShorts = fetchShortsFromInnerTube(query).toMutableList()

            // Jika hasil kueri pertama kurang dari 25, ambil kueri kedua agar daftar terisi 30-50 video
            if (innerTubeShorts.size < 25) {
                val nextQuery = SHORTS_SEEDS[(shortsSeedIndex + 1) % SHORTS_SEEDS.size]
                val extraShorts = fetchShortsFromInnerTube(nextQuery)
                innerTubeShorts.addAll(extraShorts)
            }

            val finalShorts = innerTubeShorts.distinctBy { it.id }

            if (finalShorts.isNotEmpty()) {
                Result.success(finalShorts)
            } else {
                // Fallback ke SearchExtractor bawaan NewPipe jika InnerTube direct request terkendala
                val service = ServiceList.YouTube
                val searchExtractor = service.getSearchExtractor(query)
                searchExtractor.fetchPage()
                val searchInfo = SearchInfo.getInfo(searchExtractor)

                val itemsList = mutableListOf<StreamInfoItem>()
                itemsList.addAll(searchInfo.relatedItems.filterIsInstance<StreamInfoItem>())

                if (searchInfo.hasNextPage() && itemsList.size < 30) {
                    try {
                        val nextPage = searchExtractor.getPage(searchInfo.nextPage)
                        itemsList.addAll(nextPage.items.filterIsInstance<StreamInfoItem>())
                    } catch (_: Exception) {}
                }

                val fallbackShorts = itemsList
                    .map { item ->
                        val videoId = extractVideoId(item.url)
                        val thumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val avatar = item.uploaderAvatars.firstOrNull()?.url
                        VideoItem(
                            id = videoId,
                            url = "https://www.youtube.com/shorts/$videoId",
                            title = item.name ?: "Untitled Shorts",
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
                    .filter { it.id.isNotBlank() && it.id.length == 11 }
                    .distinctBy { it.id }

                Result.success(fallbackShorts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BARU: Memuat batch Shorts berikutnya secara berkelanjutan (Infinite Scroll di VerticalPager)
    suspend fun loadMoreShortsFeed(pageOffset: Int): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val nextIndex = (shortsSeedIndex + pageOffset) % SHORTS_SEEDS.size
            val query = SHORTS_SEEDS[nextIndex]
            val moreShorts = fetchShortsFromInnerTube(query)
            if (moreShorts.isNotEmpty()) {
                Result.success(moreShorts.distinctBy { it.id })
            } else {
                // Fallback ke searchExtractor
                val service = ServiceList.YouTube
                val searchExtractor = service.getSearchExtractor(query)
                searchExtractor.fetchPage()
                val searchInfo = SearchInfo.getInfo(searchExtractor)
                val fallbackItems = searchInfo.relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { item ->
                        val videoId = extractVideoId(item.url)
                        val thumb = item.thumbnails.firstOrNull()?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        VideoItem(
                            id = videoId,
                            url = "https://www.youtube.com/shorts/$videoId",
                            title = item.name ?: "Untitled Shorts",
                            uploaderName = item.uploaderName ?: "Unknown Creator",
                            uploaderAvatarUrl = item.uploaderAvatars.firstOrNull()?.url,
                            thumbnailUrl = thumb,
                            durationSeconds = item.duration,
                            durationText = VideoItem.formatDuration(item.duration),
                            viewCount = item.viewCount,
                            viewCountText = VideoItem.formatViews(item.viewCount),
                            uploadDateText = item.textualUploadDate ?: ""
                        )
                    }
                    .filter { it.id.isNotBlank() && it.id.length == 11 }
                    .distinctBy { it.id }
                Result.success(fallbackItems)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // DINONAKTIFKAN: getExploreVideos lama yang hanya memanggil searchVideos sederhana sehingga hanya menampilkan 5-7 thumbnails
    // suspend fun getExploreVideos(category: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
    //     try {
    //         initIfNeeded()
    //         val query = when (category) {
    //             "Musik" -> "Trending Musik Indonesia"
    //             "Gaming" -> "Trending Game Indonesia"
    //             "Berita" -> "Berita Hari Ini Indonesia"
    //             "Animasi" -> "Animasi Populer Indonesia"
    //             else -> "Trending Indonesia Hari Ini"
    //         }
    //         searchVideos(query)
    //     } catch (e: Exception) {
    //         Result.failure(e)
    //     }
    // }
    // DINONAKTIFKAN: getExploreVideos dengan 1 query statis per kategori yang membuat Pull to Refresh menampilkan hasil sama persis
    // suspend fun getExploreVideos(category: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
    //     ...
    // }

    // BARU: Taksonomi Seed Global Kaya untuk Tab Jelajah / Explore (35+ Variasi Topik Dunia)
    private val EXPLORE_TRENDING_SEEDS = listOf(
        "kiosk_default",
        "trending viral worldwide",
        "top trending videos today",
        "popular entertainment hits",
        "viral moments 2026",
        "breaking trending topics"
    )

    private val EXPLORE_MUSIC_SEEDS = listOf(
        "Top Music Hits Global",
        "Billboard Hot 100",
        "Popular Music Videos 2026",
        "Global Viral Hits Today",
        "Trending Songs Acoustic",
        "Top EDM and Dance Hits",
        "Lofi Chill Beats Radio",
        "Hip Hop and RnB Charts",
        "Rock and Indie Anthems"
    )

    private val EXPLORE_GAMING_SEEDS = listOf(
        "Trending Gaming Gameplay",
        "Popular Games Highlights",
        "Top Esports Moments",
        "New Game Releases 2026",
        "Best Gaming Streamers Clips",
        "Action RPG Gameplay 4K",
        "Minecraft Builds and Survival",
        "Open World Adventure Games",
        "Multiplayer Funny Gaming Moments"
    )

    private val EXPLORE_NEWS_SEEDS = listOf(
        "Top World News Today",
        "Breaking News Live Coverage",
        "Technology and AI Breakthroughs",
        "Science Discoveries News",
        "Global Economy and Markets",
        "Space Exploration Updates",
        "Climate and Nature News"
    )

    private val EXPLORE_ANIMATION_SEEDS = listOf(
        "Trending Animation Shorts",
        "CGI 3D Short Films 4K",
        "Best Animated Short Films",
        "Indie Animation Showcase",
        "Anime Highlights 4K",
        "Stop Motion Animation",
        "Blender 3D CGI Showcase",
        "Visual Storytelling Animation"
    )

    private val exploreCategoryIndices = java.util.concurrent.ConcurrentHashMap<String, Int>()

    // BARU: Mengambil video kategori Explore dengan rotasi seed global dinamis setiap kali Pull to Refresh
    suspend fun getExploreVideos(category: String, refresh: Boolean = false): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val service = ServiceList.YouTube

            val seedList = when (category) {
                "Musik" -> EXPLORE_MUSIC_SEEDS
                "Gaming" -> EXPLORE_GAMING_SEEDS
                "Berita" -> EXPLORE_NEWS_SEEDS
                "Animasi" -> EXPLORE_ANIMATION_SEEDS
                else -> EXPLORE_TRENDING_SEEDS
            }

            val currentIndex = exploreCategoryIndices.getOrDefault(category, 0)
            val nextIndex = if (refresh) (currentIndex + 1) % seedList.size else currentIndex
            exploreCategoryIndices[category] = nextIndex

            val selectedSeed = seedList[nextIndex]

            if (category == "Trending" && selectedSeed == "kiosk_default") {
                try {
                    val kioskList = service.kioskList
                    val defaultKiosk = kioskList.defaultKioskId ?: "Trending"
                    val extractor = kioskList.getExtractorById(defaultKiosk, null)
                    extractor.fetchPage()
                    val kioskInfo = KioskInfo.getInfo(extractor)
                    val items = kioskInfo.relatedItems
                        .filterIsInstance<StreamInfoItem>()
                        .map { item ->
                            val videoId = extractVideoId(item.url)
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
                        .filter { it.id.isNotBlank() && it.id.length == 11 }
                        .distinctBy { it.id }

                    if (items.isNotEmpty()) {
                        return@withContext Result.success(items)
                    }
                } catch (_: Exception) {}
            }

            val query = if (selectedSeed == "kiosk_default") "trending viral worldwide" else selectedSeed
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(searchExtractor)

            val streamItems = mutableListOf<StreamInfoItem>()
            streamItems.addAll(searchInfo.relatedItems.filterIsInstance<StreamInfoItem>())

            if (searchInfo.hasNextPage() && streamItems.size < 25) {
                try {
                    val nextPage = searchExtractor.getPage(searchInfo.nextPage)
                    streamItems.addAll(nextPage.items.filterIsInstance<StreamInfoItem>())
                } catch (_: Exception) {}
            }

            val videos = streamItems
                .map { item ->
                    val videoId = extractVideoId(item.url)
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
                .filter { it.id.isNotBlank() && it.id.length == 11 }
                .distinctBy { it.id }

            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BARU: Memuat batch video tambahan untuk kategori Jelajah (Infinite Scroll)
    suspend fun loadMoreExploreVideos(category: String, pageOffset: Int): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val seedList = when (category) {
                "Musik" -> EXPLORE_MUSIC_SEEDS
                "Gaming" -> EXPLORE_GAMING_SEEDS
                "Berita" -> EXPLORE_NEWS_SEEDS
                "Animasi" -> EXPLORE_ANIMATION_SEEDS
                else -> EXPLORE_TRENDING_SEEDS
            }
            val baseIndex = exploreCategoryIndices.getOrDefault(category, 0)
            val targetSeed = seedList[(baseIndex + pageOffset) % seedList.size]
            val query = if (targetSeed == "kiosk_default") "trending viral worldwide" else targetSeed

            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            val searchInfo = SearchInfo.getInfo(searchExtractor)

            val videos = searchInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = extractVideoId(item.url)
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
                .filter { it.id.isNotBlank() && it.id.length == 11 }
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

        // DINONAKTIFKAN: OkHttpClient.Builder() baru pada setiap request ketikan saran pencarian
        // val client = OkHttpClient.Builder()
        //     .connectTimeout(5, TimeUnit.SECONDS)
        //     .readTimeout(5, TimeUnit.SECONDS)
        //     .build()
        // BARU: Gunakan singleton NetworkClient.okHttpClient bersama dengan connection pool terpadu
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=youtube&jsonp=JP&ds=yt&gl=ID&hl=id&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NewPipeDownloader.DEFAULT_USER_AGENT)
                .build()
            val response = NetworkClient.okHttpClient.newCall(request).execute()
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
            // DINONAKTIFKAN: Ekstraksi ID sederhana yang gagal jika URL berupa /shorts/ atau tautan dengan query params
            // val videoId = if (videoIdOrUrl.contains("v=")) {
            //     videoIdOrUrl.substringAfter("v=").substringBefore("&")
            // } else if (videoIdOrUrl.startsWith("http")) {
            //     videoIdOrUrl.substringAfterLast("/")
            // } else {
            //     videoIdOrUrl
            // }
            // BARU: Ekstraksi ID video universal (standar DeArrow PipePipe) yang menangani semua format URL YouTube
            val videoId = extractVideoId(videoIdOrUrl)

            // BARU: Kembalikan dari cache memori jika detail video sudah pernah diekstrak (0 ms latensi jaringan)
            val cached = playbackDetailsCache.get(videoId)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            val url = if (videoIdOrUrl.startsWith("http")) videoIdOrUrl else "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

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
                    val relId = extractVideoId(item.url)
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

            // Simpan ke in-memory cache untuk akses instan selanjutnya
            playbackDetailsCache.put(videoId, details)

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
