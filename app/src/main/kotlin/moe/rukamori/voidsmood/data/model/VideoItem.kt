// meticulous and honest
package moe.rukamori.voidsmood.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoItem(
    val id: String,
    val url: String,
    val title: String,
    val uploaderName: String,
    val uploaderAvatarUrl: String? = null,
    val thumbnailUrl: String,
    val durationSeconds: Long = 0,
    val durationText: String = "",
    val viewCount: Long = 0,
    val viewCountText: String = "",
    val uploadDateText: String = ""
) {
    companion object {
        fun formatDuration(seconds: Long): String {
            if (seconds <= 0) return ""
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) {
                String.format("%d:%02d:%02d", h, m, s)
            } else {
                String.format("%d:%02d", m, s)
            }
        }

        fun formatViews(count: Long): String {
            if (count < 0) return ""
            return when {
                count >= 1_000_000_000 -> String.format("%.1fB x ditonton", count / 1_000_000_000.0)
                count >= 1_000_000 -> String.format("%.1fM x ditonton", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK x ditonton", count / 1_000.0)
                else -> "$count x ditonton"
            }
        }

        fun formatCount(count: Long): String {
            if (count <= 0) return ""
            return when {
                count >= 1_000_000_000 -> String.format("%.1fM", count / 1_000_000_000.0)
                count >= 1_000_000 -> String.format("%.1f jt", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1f rb", count / 1_000.0)
                else -> "$count"
            }
        }
    }
}
