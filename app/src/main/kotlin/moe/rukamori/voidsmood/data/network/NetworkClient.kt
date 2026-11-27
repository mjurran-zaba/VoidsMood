// meticulous and honest
package moe.rukamori.voidsmood.data.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Singleton OkHttpClient terpusat yang dioptimasi untuk jaringan internet mobile & broadband Indonesia:
 * 1. Ipv4PreferredDns & Fast Fallback (Happy Eyeballs RFC 8305):
 *    Mengeliminasi jeda timeout 10-15 detik di ISP Indonesia (IndiHome, Telkomsel, XL, Biznet)
 *    yang sering mengalami packet drop / blackhole pada rute IPv6 googlevideo.com / youtubei.
 * 2. Keep-Alive 45 Detik (Mengadopsi VoidTune PlaybackNetwork):
 *    Mencegah "zombie sockets" di mana tabel CGNAT operator seluler menutup koneksi idle
 *    setelah 1-2 menit tanpa mengirim sinyal RST/FIN, menyebabkan request menggantung.
 * 3. Pemisahan Klien Media Streaming (retryOnConnectionFailure = false):
 *    Menghindari loop retry liar pada Range requests ExoPlayer yang dapat menguras kuota saat buffering.
 */
object NetworkClient {

    // BARU: DNS resolver yang memprioritaskan alamat IPv4 daripada IPv6
    // Menghilangkan jeda hang 10 detik saat ISP gagal me-route paket IPv6 ke server YouTube
    object Ipv4PreferredDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            return addresses.sortedBy { if (it is Inet6Address) 1 else 0 }
        }
    }

    // BARU: Dispatcher dengan batas konkurensi lebih tinggi (64 global, 16 per-host)
    // agar pemuatan thumbnail Coil, ekstraksi NewPipe, dan data feed tidak saling mengantre di thread tunggal
    private val apiDispatcher = Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 16
    }

    private val mediaDispatcher = Dispatcher().apply {
        maxRequests = 32
        maxRequestsPerHost = 8
    }

    // DINONAKTIFKAN: ConnectionPool lama dengan keep-alive 5 menit
    // val connectionPool = ConnectionPool(16, 5, TimeUnit.MINUTES)
    // BARU: ConnectionPool 45 detik untuk mencegah zombie half-open socket akibat pemotongan NAT seluler
    val connectionPool = ConnectionPool(32, 45, TimeUnit.SECONDS)
    val mediaConnectionPool = ConnectionPool(16, 45, TimeUnit.SECONDS)

    // Klien HTTP untuk API, pencarian, dan ekstraksi metadata
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(apiDispatcher)
        .connectionPool(connectionPool)
        .dns(Ipv4PreferredDns)
        .fastFallback(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // BARU: OkHttpClient khusus untuk streaming video/audio Media3 ExoPlayer (mengadopsi arsitektur VoidTune)
    // retryOnConnectionFailure dimatikan agar OkHttp tidak mengulang Range request yang gagal secara membabi buta,
    // melainkan menyerahkan kendali penanganan chunk dan failover sepenuhnya kepada Media3
    val mediaStreamingOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(mediaDispatcher)
        .connectionPool(mediaConnectionPool)
        .dns(Ipv4PreferredDns)
        .fastFallback(true)
        .retryOnConnectionFailure(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
}
