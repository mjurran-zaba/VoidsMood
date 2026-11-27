// meticulous and honest
package moe.rukamori.voidsmood.data.network

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Singleton OkHttpClient terpusat yang digunakan oleh seluruh modul aplikasi:
 * 1. NewPipeExtractor (ekstraksi metadata & video info)
 * 2. Media3 ExoPlayer OkHttpDataSource (streaming chunk video & audio)
 * 3. Search suggestions & HTTP request lainnya
 *
 * Menggunakan ConnectionPool persisten (16 koneksi, 5 menit keep-alive)
 * agar koneksi TCP & handshake TLS 1.3 ke googlevideo.com / youtube.com
 * tidak perlu diulang pada setiap request chunk media, melipatgandakan kecepatan transfer.
 */
object NetworkClient {
    val connectionPool = ConnectionPool(16, 5, TimeUnit.MINUTES)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(connectionPool)
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
}
