// meticulous and honest
package moe.rukamori.voidsmood.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoPlaybackDetails(
    val video: VideoItem,
    val streamUrl: String,
    val isLive: Boolean = false,
    val description: String = "",
    val subscriberCountText: String = "",
    val likeCountText: String = "",
    val dislikeCountText: String = "",
    val relatedVideos: List<VideoItem> = emptyList()
)
