// meticulous and honest
package moe.rukamori.voidsmood.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.voidsmood.data.model.VideoItem
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
                    // BARU: Ambil URL dari list thumbnails dan uploaderAvatars bawaan NewPipeExtractor
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
                    // BARU: Ambil URL dari list thumbnails dan uploaderAvatars bawaan NewPipeExtractor
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

    suspend fun getStreamInfo(urlOrId: String): Result<StreamInfo> = withContext(Dispatchers.IO) {
        try {
            initIfNeeded()
            val url = if (urlOrId.startsWith("http")) urlOrId else "https://www.youtube.com/watch?v=$urlOrId"
            val info = StreamInfo.getInfo(url)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
