// meticulous and honest
package moe.rukamori.voidsmood.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoPlaybackDetails(
    val video: VideoItem,
    val hlsUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val isProgressiveMuxed: Boolean = false,
    val isLive: Boolean = false,
    val description: String = "",
    val subscriberCountText: String = "",
    val likeCountText: String = "",
    val dislikeCountText: String = "",
    val relatedVideos: List<VideoItem> = emptyList(),
    // BARU: Daftar resolusi yang tersedia dan pemetaan URL resolusi ke stream untuk kontrol kualitas
    val availableResolutions: List<String> = emptyList(),
    val resolutionStreams: Map<String, String> = emptyMap()
)
