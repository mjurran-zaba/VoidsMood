// meticulous and honest
package moe.rukamori.voidsmood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.ui.theme.GlassBorderSubtle
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.TextMuted
import kotlin.math.roundToInt

// BARU: Komponen MiniPlayer YouTube Standar (Opsi 1)
// Bilah horizontal elegan di atas bottom navigation dengan thumbnail 16:9, judul marquee,
// tombol Play/Pause, tombol Close, dan progress bar tipis YouTube Red. Mendukung swipe to dismiss.
@Composable
fun MiniPlayerBar(
    video: VideoItem,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onExpand: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val safeDuration = duration.coerceAtLeast(1L)
    val progressFraction = (currentPosition.toFloat() / safeDuration).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .height(62.dp)
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffsetX > 140f || dragOffsetX < -140f) {
                            onClose()
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX += dragAmount
                    }
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161824))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sisi Kiri: Cuplikan Thumbnail Video 16:9
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F111A))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Bagian Tengah: Judul Video (Marquee jika panjang) & Nama Channel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.title,
                    color = IceWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.uploaderName,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sisi Kanan: Tombol Play / Pause & Tombol Close (✕)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Play / Pause
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayPauseToggle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        LucidePause(tint = IceWhite, size = 20.dp)
                    } else {
                        LucidePlay(tint = IceWhite, size = 20.dp)
                    }
                }

                // Tombol Close (✕)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    LucideX(tint = IceWhite, size = 20.dp)
                }
            }
        }

        // Garis Merah Progress Bar Tipis Khas YouTube di Bagian Bawah Bilah Miniplayer
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .height(2.dp)
                    .background(Color(0xFFFF0000))
            )
        }
    }
}
