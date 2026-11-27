// meticulous and honest
// READ AND IMPLEMENT AGENTS.md BEFORE YOU DO
package moe.rukamori.voidsmood.data.network

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Singleton Manajer Cache Disk Media (Mengadopsi arsitektur VoidTune & PipePipe):
 * 1. SimpleCache + LeastRecentlyUsedCacheEvictor (Kapasitas 250 MB):
 *    Menyimpan fragmen video dan audio langsung ke penyimpanan lokal perangkat.
 *    Saat video melakukan loop (fitur utama Shorts/Reels) atau pengguna swipe balik ke video sebelumnya,
 *    pemutaran dimulai seketika (0 ms) langsung dari storage flash perangkat tanpa menggunakan kuota internet.
 * 2. Pra-unduh Segmen Awal (Prefetch Head 512 KB):
 *    Mengunduh potongan 3-5 detik pertama dari video berikutnya di latar belakang.
 *    Begitu pengguna menggeser layar, video berikutnya langsung tancap gas tanpa jeda buffering jaringan.
 * 3. FLAG_IGNORE_CACHE_ON_ERROR:
 *    Jika terjadi kegagalan baca/tulis disk, ExoPlayer otomatis fallback ke jaringan langsung tanpa freeze.
 */
@OptIn(UnstableApi::class)
object MediaCacheManager {

    private const val CACHE_DIR_NAME = "voids_media_cache"
    private const val MAX_CACHE_BYTES = 250L * 1024L * 1024L // 250 MB LRU disk cache

    @Volatile
    private var simpleCacheInstance: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        return simpleCacheInstance ?: synchronized(this) {
            simpleCacheInstance ?: run {
                val cacheDir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME).apply {
                    if (!exists()) mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCacheInstance = it
                }
            }
        }
    }

    /**
     * Membangun CacheDataSource.Factory untuk ExoPlayer yang menghubungkan disk cache
     * dengan OkHttpDataSource jaringan.
     */
    fun createCacheDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        val upstreamFactory = OkHttpDataSource.Factory(NetworkClient.mediaStreamingOkHttpClient)
            .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheWriteDataSinkFactory(
                CacheDataSink.Factory()
                    .setCache(cache)
                    .setFragmentSize(2L * 1024L * 1024L) // 2 MB per file fragment
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Pra-unduh 256 KB - 512 KB pertama dari URL media di background thread.
     * Mengisi cache awal sehingga Time-To-First-Frame (TTFF) saat swipe menjadi mendekati 0 ms.
     */
    suspend fun prefetchStreamHead(context: Context, url: String, bytesToPrefetch: Long = 512 * 1024L) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        try {
            val cache = getCache(context)
            val upstreamFactory = OkHttpDataSource.Factory(NetworkClient.mediaStreamingOkHttpClient)
                .setUserAgent(NewPipeDownloader.DEFAULT_USER_AGENT)
                .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))

            val cacheDataSource = CacheDataSource(
                cache,
                upstreamFactory.createDataSource(),
                FileDataSource(),
                CacheDataSink(cache, 2L * 1024L * 1024L),
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null
            )

            val uri = Uri.parse(url)
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .setLength(bytesToPrefetch)
                .build()

            val writer = CacheWriter(cacheDataSource, dataSpec, null, null)
            writer.cache()
        } catch (_: Exception) {
            // Kegagalan pra-unduh diabaikan tanpa error, pemutaran reguler akan streaming dari jaringan
        }
    }
}
