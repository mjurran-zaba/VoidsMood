// meticulous and honest
package moe.rukamori.voidsmood.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.data.model.VideoPlaybackDetails
import moe.rukamori.voidsmood.data.network.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object YouTubeRepository {

    @Volatile
    private var isInitialized = false

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

    suspend fun getTrendingVideos(): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val service = ServiceList.YouTube
            val kioskList = service.kioskList
            val defaultKiosk = kioskList.defaultKioskId ?: "Trending"
            val extractor = kioskList.getExtractorById(defaultKiosk, null)
            extractor.fetchPage()
            val kioskInfo = KioskInfo.getInfo(extractor)

            val videoItems = kioskInfo.relatedItems
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
            Result.success(videoItems)
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
            Result.success(videoItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

            // DINONAKTIFKAN: Raw description yang meloloskan tag HTML <br> dan entitas HTML
            // description = info.description?.content ?: "",
            // BARU: Pembersihan teks deskripsi video secara menyeluruh dari tag HTML <br>, <p>, anchor, dan entitas HTML
            description = cleanHtmlDescription(info.description?.content ?: ""),
                subscriberCountText = if (info.uploaderSubscriberCount > 0) "${VideoItem.formatCount(info.uploaderSubscriberCount)} subscriber" else "",
                likeCountText = if (info.likeCount > 0) VideoItem.formatCount(info.likeCount) else "Suka",
                dislikeCountText = if (info.dislikeCount > 0) VideoItem.formatCount(info.dislikeCount) else "",
                relatedVideos = related
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
