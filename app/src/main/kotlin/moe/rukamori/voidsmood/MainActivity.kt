// meticulous and honest
package moe.rukamori.voidsmood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.ui.components.AppleBottomNavigationBar
import moe.rukamori.voidsmood.ui.components.NavigationTab
import moe.rukamori.voidsmood.ui.components.VoidsTopAppBar
import moe.rukamori.voidsmood.ui.screens.HomeScreen
import moe.rukamori.voidsmood.ui.screens.PlayerScreen
import moe.rukamori.voidsmood.ui.screens.SearchScreen
import moe.rukamori.voidsmood.ui.theme.PureBlack
import moe.rukamori.voidsmood.ui.theme.TextMuted
import moe.rukamori.voidsmood.ui.theme.VoidsMoodTheme
import moe.rukamori.voidsmood.ui.viewmodel.HomeViewModel
import moe.rukamori.voidsmood.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VoidsMoodTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
                var activeVideo by remember { mutableStateOf<VideoItem?>(null) }

                // BARU: State kontrol kemunculan layer pencarian penuh dengan animasi slide-to-left
                var isSearchActive by remember { mutableStateOf(false) }

                if (activeVideo != null) {
                    PlayerScreen(
                        video = activeVideo!!,
                        viewModel = playerViewModel,
                        onBack = {
                            playerViewModel.closePlayer()
                            activeVideo = null
                        },
                        onVideoSelect = { newVideo ->
                            activeVideo = newVideo
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // DINONAKTIFKAN: Konten drawer navigasi lama (VoidsDrawerContent)
                        // drawerContent = { VoidsDrawerContent(...) }
                        // BARU: Drawer kosong murni sesuai instruksi user ("di drawer kosong aja")
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
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    // Tampilan Konten Berdasarkan 4 Tab Navigasi
                                    when (currentTab) {
                                        NavigationTab.HOME -> {
                                            HomeScreen(
                                                viewModel = homeViewModel,
                                                onVideoClick = { video ->
                                                    activeVideo = video
                                                }
                                            )
                                        }
                                        NavigationTab.EXPLORE -> {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Eksplorasi Kategori (Segera Hadir)",
                                                    color = TextMuted,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                        NavigationTab.SUBSCRIPTIONS -> {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Langganan Lokal (Segera Hadir)",
                                                    color = TextMuted,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                        NavigationTab.LIBRARY -> {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Pustaka & Riwayat (Segera Hadir)",
                                                    color = TextMuted,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }

                                    // BARU: Floating Apple-Style Capsule Navigation Toolbar (4 Tab)
                                    AppleBottomNavigationBar(
                                        currentTab = currentTab,
                                        onTabSelected = { tab ->
                                            currentTab = tab
                                            if (tab == NavigationTab.HOME) {
                                                homeViewModel.loadTrending()
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
                            }
                        }

                        // BARU: Layer Pencarian Baru yang meluncur mulus dari kanan ke kiri (slide to left)
                        // Menyediakan live suggestions saat mengetik dan saran pencarian YouTube real-time
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
