// meticulous and honest
// READ AND IMPLEMENT AGENTS.md BEFORE YOU DO
package moe.rukamori.voidsmood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.repository.LocalLibraryRepository
import moe.rukamori.voidsmood.data.repository.YouTubeRepository
import moe.rukamori.voidsmood.ui.components.AppleBottomNavigationBar
import moe.rukamori.voidsmood.ui.components.MiniPlayerBar
import moe.rukamori.voidsmood.ui.components.NavigationTab
import moe.rukamori.voidsmood.ui.components.VoidsTopAppBar
import moe.rukamori.voidsmood.ui.screens.ExploreScreen
import moe.rukamori.voidsmood.ui.screens.HomeScreen
import moe.rukamori.voidsmood.ui.screens.LibraryScreen
import moe.rukamori.voidsmood.ui.screens.PlayerScreen
import moe.rukamori.voidsmood.ui.screens.SearchScreen
import moe.rukamori.voidsmood.ui.screens.ShortsScreen
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted
import moe.rukamori.voidsmood.ui.theme.VoidsMoodTheme
import moe.rukamori.voidsmood.ui.viewmodel.HomeUiState
import moe.rukamori.voidsmood.ui.viewmodel.HomeViewModel
import moe.rukamori.voidsmood.ui.viewmodel.PlayerMode
import moe.rukamori.voidsmood.ui.viewmodel.PlayerViewModel
import moe.rukamori.voidsmood.ui.viewmodel.ShortsViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val shortsViewModel: ShortsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalLibraryRepository.init(this)
        // BARU: Inisialisasi YouTubeRepository dengan Context aplikasi untuk deteksi lokasi negara SIM/jaringan pengguna
        YouTubeRepository.init(this)

        setContent {
            VoidsMoodTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

                // State reaktif pemutar video & miniplayer dari PlayerViewModel
                val playerMode by playerViewModel.playerMode.collectAsState()
                val activeVideo by playerViewModel.activeVideo.collectAsState()
                val isPlaying by playerViewModel.isPlaying.collectAsState()
                val currentPosition by playerViewModel.currentPosition.collectAsState()
                val duration by playerViewModel.duration.collectAsState()

                // BARU: Logika Scroll-Aware (Opsi 2) untuk Bottom Navigation dan MiniPlayer
                // Menyembunyikan bar saat scroll ke bawah (membaca feed) dan memunculkan kembali saat scroll ke atas
                // DINONAKTIFKAN: Auto-dismiss bottom navigation bar saat scroll konten feed ke bawah
                // val nestedScrollConnection = remember {
                //     object : NestedScrollConnection {
                //         override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                //             val delta = available.y
                //             if (delta < -10f) {
                //                 isNavigationVisible = false
                //             } else if (delta > 10f) {
                //                 isNavigationVisible = true
                //             }
                //             return Offset.Zero
                //         }
                //     }
                // }

                // BARU: Bottom navigation bar TIDAK BOLEH auto-dismiss saat scroll feed;
                // Hanya boleh disembunyikan jika pengguna secara sengaja melakukan swipe down pada bar navigasi itu sendiri
                var isNavigationVisible by remember { mutableStateOf(true) }

                val navOffsetY by animateDpAsState(
                    targetValue = if (isNavigationVisible) 0.dp else 100.dp,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    ),
                    label = "navOffsetY"
                )

                val miniPlayerOffsetY by animateDpAsState(
                    targetValue = if (isNavigationVisible) 0.dp else 66.dp,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    ),
                    label = "miniPlayerOffsetY"
                )

                // Jeda pemutaran miniplayer jika pengguna berpindah ke tab Shorts, atau jeda player Shorts jika keluar dari tab Shorts
                LaunchedEffect(currentTab) {
                    if (currentTab == NavigationTab.SHORTS) {
                        if (playerMode == PlayerMode.MINI && isPlaying) {
                            playerViewModel.togglePlayPause()
                        }
                        shortsViewModel.resumePlayer()
                    } else {
                        shortsViewModel.pausePlayer()
                    }
                }

                // State kontrol kemunculan layer pencarian penuh dengan animasi slide-to-left
                var isSearchActive by remember { mutableStateOf(false) }

                // State reaktif pencarian beranda
                val homeState by homeViewModel.uiState.collectAsState()
                val isHomeSearching = (homeState as? HomeUiState.Success)?.isSearching == true
                val currentSearchQuery = if (isHomeSearching) (homeState as? HomeUiState.Success)?.query.orEmpty() else ""

                // BARU: Jika bottom navigation sedang disembunyikan (isNavigationVisible == false),
                // gestur/tombol Back fisik pertama akan memunculkan kembali bottom navigation bar terlebih dahulu,
                // sehingga pengguna tidak keluar atau berganti tab tanpa sengaja
                BackHandler(enabled = !isNavigationVisible && !isSearchActive && playerMode != PlayerMode.FULL) {
                    isNavigationVisible = true
                }

                // BARU: Jika pengguna sedang melihat hasil pencarian di tab HOME, tombol Back mengembalikan ke feed Beranda terlebih dahulu
                BackHandler(enabled = currentTab == NavigationTab.HOME && isHomeSearching && !isSearchActive && playerMode != PlayerMode.FULL) {
                    homeViewModel.loadTrending(forceReload = false)
                }

                // DINONAKTIFKAN: Belum ada penanganan tombol Back saat berada di luar tab HOME sehingga aplikasi langsung keluar
                // BARU: Tangani tombol Back fisik/gesture di tab Shorts, Explore, atau Library agar kembali ke tab HOME terlebih dahulu
                BackHandler(enabled = currentTab != NavigationTab.HOME && !isSearchActive && playerMode != PlayerMode.FULL) {
                    currentTab = NavigationTab.HOME
                }

                if (playerMode == PlayerMode.FULL && activeVideo != null) {
                    // MODE 1: Layar Penuh Pemutar Video (PlayerScreen YouTube Style)
                    PlayerScreen(
                        video = activeVideo!!,
                        viewModel = playerViewModel,
                        onBack = {
                            // Menekan tombol back di full player meminimalisir video ke Miniplayer
                            playerViewModel.minimizePlayer()
                        },
                        onVideoSelect = { newVideo ->
                            playerViewModel.openVideo(newVideo)
                        }
                    )
                } else {
                    // MODE 2: Beranda Scaffold Navigasi dengan Miniplayer Melayang di Atas Bottom Bar
                    Box(modifier = Modifier.fillMaxSize()) {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            scrimColor = Color.Black.copy(alpha = 0.50f),
                            drawerContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.70f)
                                        .fillMaxHeight()
                                        .background(PureBlack)
                                )
                            }
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Color.Transparent,
                                topBar = {
                                    // Sembunyikan top bar bawaan pada tab Shorts agar pengalaman video reels vertikal 9:16 murni layar penuh
                                    if (currentTab != NavigationTab.SHORTS) {
                                        VoidsTopAppBar(
                                            onMenuClick = {
                                                scope.launch {
                                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                }
                                            },
                                            onSearchClick = {
                                                isSearchActive = true
                                            }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (currentTab == NavigationTab.SHORTS) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                                        // DINONAKTIFKAN: .nestedScroll(nestedScrollConnection) yang menyembunyikan bottom navigation saat pengguna scroll konten feed
                                ) {
                                    // DINONAKTIFKAN: 4 tab lama dengan SUBSCRIPTIONS placeholder
                                    // when (currentTab) {
                                    //     NavigationTab.HOME -> { ... }
                                    //     NavigationTab.EXPLORE -> { ... }
                                    //     NavigationTab.SUBSCRIPTIONS -> { ... }
                                    //     NavigationTab.LIBRARY -> { ... }
                                    // }
                                    // BARU: 4 Tab pilihan user aktif penuh: Home, Shorts (Reels/vertikal), Explore, dan Pustaka & Riwayat
                                    when (currentTab) {
                                        NavigationTab.HOME -> {
                                            HomeScreen(
                                                viewModel = homeViewModel,
                                                onVideoClick = { video ->
                                                    LocalLibraryRepository.addToHistory(video)
                                                    playerViewModel.openVideo(video)
                                                }
                                            )
                                        }
                                        NavigationTab.SHORTS -> {
                                            ShortsScreen(
                                                viewModel = shortsViewModel,
                                                onVideoSelect = { video ->
                                                    LocalLibraryRepository.addToHistory(video)
                                                    playerViewModel.openVideo(video)
                                                }
                                            )
                                        }
                                        NavigationTab.EXPLORE -> {
                                            ExploreScreen(
                                                onVideoClick = { video ->
                                                    LocalLibraryRepository.addToHistory(video)
                                                    playerViewModel.openVideo(video)
                                                }
                                            )
                                        }
                                        NavigationTab.LIBRARY -> {
                                            LibraryScreen(
                                                onVideoClick = { video ->
                                                    LocalLibraryRepository.addToHistory(video)
                                                    playerViewModel.openVideo(video)
                                                }
                                            )
                                        }
                                    }

                                    // BARU: MiniPlayer YouTube Standar (Opsi 1)
                                    // Muncul tepat di atas AppleBottomNavigationBar saat video diminimalkan
                                    // Disembunyikan saat di tab Shorts agar tidak menutupi tampilan video reels
                                    if (playerMode == PlayerMode.MINI && activeVideo != null && currentTab != NavigationTab.SHORTS) {
                                        MiniPlayerBar(
                                            video = activeVideo!!,
                                            isPlaying = isPlaying,
                                            currentPosition = currentPosition,
                                            duration = duration,
                                            onExpand = { playerViewModel.expandPlayer() },
                                            onPlayPauseToggle = { playerViewModel.togglePlayPause() },
                                            onClose = { playerViewModel.closePlayer() },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 76.dp)
                                                .offset(y = miniPlayerOffsetY)
                                        )
                                    }

                                    // Floating Apple-Style Capsule Navigation Toolbar (4 Tab) dengan Scroll-Aware Offset
                                    AppleBottomNavigationBar(
                                        currentTab = currentTab,
                                        onTabSelected = { tab ->
                                            val wasAlreadyHome = (currentTab == NavigationTab.HOME)
                                            currentTab = tab
                                            if (tab == NavigationTab.HOME) {
                                                // DINONAKTIFKAN: homeViewModel.loadTrending() setiap kali tab HOME dipilih yang mereset scroll dan feed ke kosong
                                                // BARU: Pertahankan feed video yang ada saat beralih tab, hanya refresh jika user men-tap tab HOME kedua kalinya
                                                if (wasAlreadyHome) {
                                                    homeViewModel.refreshTrending()
                                                } else {
                                                    homeViewModel.loadTrending(forceReload = false)
                                                }
                                            } else if (tab == NavigationTab.SHORTS) {
                                                // BARU: Saat pengguna masuk ke tab Shorts (misal setelah menekan tombol back ke Home lalu masuk Shorts kembali),
                                                // selalu muat kumpulan video Shorts baru dengan rotasi topik agar kontennya selalu berubah dan segar
                                                shortsViewModel.loadShorts(refresh = true)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(y = navOffsetY)
                                            // BARU: Hanya dapat disembunyikan jika pengguna secara sengaja melakukan swipe down pada bar navigasi itu sendiri
                                            .pointerInput(Unit) {
                                                detectVerticalDragGestures { _, dragAmount ->
                                                    if (dragAmount > 15f) {
                                                        isNavigationVisible = false
                                                    }
                                                }
                                            }
                                    )

                                    // DINONAKTIFKAN: Area sentuh 28.dp di bagian paling bawah layar tanpa windowInsetsPadding yang bertabrakan dengan gestur Back to Home bawaan sistem operasi Android
                                    // if (!isNavigationVisible) {
                                    //     Box(
                                    //         modifier = Modifier
                                    //             .align(Alignment.BottomCenter)
                                    //             .fillMaxWidth()
                                    //             .height(28.dp)
                                    //             .pointerInput(Unit) {
                                    //                 detectVerticalDragGestures { _, dragAmount ->
                                    //                     if (dragAmount < -10f) {
                                    //                         isNavigationVisible = true
                                    //                     }
                                    //                 }
                                    //             }
                                    //             .clickable { isNavigationVisible = true }
                                    //     )
                                    // }

                                    // BARU: Floating Capsule Handle elegan yang ditempatkan DI ATAS inset navigationBars sistem Android.
                                    // Mengeliminasi total tabrakan dengan gestur Back to Home OS:
                                    // 1. Memiliki bantalan insets (windowInsetsPadding(WindowInsets.navigationBars)) sehingga posisinya tidak di tepi bezel bawah layar.
                                    // 2. Mendukung ketukan sekali sentuh (single-tap) yang tidak akan pernah memicu gestur usap home OS.
                                    // 3. Mendukung usap ke atas pada kapsul itu sendiri untuk mengembalikan navigasi dengan mulus.
                                    AnimatedVisibility(
                                        visible = !isNavigationVisible,
                                        enter = fadeIn(animationSpec = tween(200)),
                                        exit = fadeOut(animationSpec = tween(150)),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF0A0D14).copy(alpha = 0.88f))
                                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                                .clickable { isNavigationVisible = true }
                                                .pointerInput(Unit) {
                                                    detectVerticalDragGestures { _, dragAmount ->
                                                        if (dragAmount < -8f) {
                                                            isNavigationVisible = true
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 28.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(36.dp)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.70f))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Layer Pencarian Baru yang meluncur mulus dari kanan ke kiri (slide to left)
                        AnimatedVisibility(
                            visible = isSearchActive,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(200)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(150))
                        ) {
                            SearchScreen(
                                onClose = { isSearchActive = false },
                                initialQuery = currentSearchQuery,
                                onSearch = { query ->
                                    isSearchActive = false
                                    currentTab = NavigationTab.HOME
                                    homeViewModel.search(query)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
