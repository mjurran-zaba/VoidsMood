// meticulous and honest
// READ AND IMPLEMENT AGENTS.md BEFORE YOU DO
package moe.rukamori.voidsmood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rukamori.voidsmood.ui.theme.IceWhite
import moe.rukamori.voidsmood.ui.theme.PureBlack

// BARU: TopAppBar Edge-to-Edge menyentuh batas layar tanpa margin mengapung,
// tulisan 'VoidsMood' dihapus total, dan tombol pencarian membuka layer pencarian baru (SearchLayer)
@Composable
fun VoidsTopAppBar(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(PureBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sisi Kiri: Ikon Menu Drawer (Touch target >= 48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMenuClick),
                contentAlignment = Alignment.Center
            ) {
                LucideMenu(
                    tint = IceWhite,
                    size = 22.dp
                )
            }

            // Bagian Tengah: Kosong murni (tulisan VoidsMood dihapus sesuai instruksi)
            Box(modifier = Modifier.weight(1f))

            // Sisi Kanan: Tombol Pencarian yang memicu layer pencarian baru meluncur mulus
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                LucideSearch(
                    tint = IceWhite,
                    size = 21.dp
                )
            }
        }
    }
}

// BARU: SearchTopAppBar gaya resmi YouTube saat hasil pencarian aktif:
// Menampilkan tombol kembali (panah kiri), kotak teks yang mempertahankan kata kunci pencarian yang sedang aktif,
// dan tombol hapus (silang) untuk mereset kueri tanpa label teks dekoratif tambahan
@Composable
fun SearchTopAppBar(
    query: String,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(PureBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sisi Kiri: Tombol Panah Kembali ke Beranda
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                LucideArrowLeft(
                    tint = IceWhite,
                    size = 22.dp
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Bagian Tengah: Kotak input pencarian yang tetap menampilkan kueri pencarian aktif
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E222D))
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = query,
                    color = IceWhite,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Sisi Kanan: Tombol Hapus untuk membatalkan pencarian
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClearClick),
                contentAlignment = Alignment.Center
            ) {
                LucideX(
                    tint = IceWhite,
                    size = 18.dp
                )
            }
        }
    }
}
