// meticulous and honest
package moe.rukamori.voidsmood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import moe.rukamori.voidsmood.data.model.VideoItem
import moe.rukamori.voidsmood.ui.components.VoidsTopAppBar
import moe.rukamori.voidsmood.ui.drawer.VoidsDrawerContent
import moe.rukamori.voidsmood.ui.screens.HomeScreen
import moe.rukamori.voidsmood.ui.screens.PlayerScreen
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
                var currentNavId by remember { mutableStateOf("home") }
                var activeVideo by remember { mutableStateOf<VideoItem?>(null) }

                if (activeVideo != null) {
                    // Layar Pemutar Video Media3 YouTube (Zero Ad, On-Device, Video Terkait)
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
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        scrimColor = Color.Black.copy(alpha = 0.65f),
                        drawerContent = {
                            VoidsDrawerContent(
                                selectedId = currentNavId,
                                onItemSelected = { id ->
                                    currentNavId = id
                                    if (id == "home") {
                                        homeViewModel.loadTrending()
                                    }
                                    scope.launch { drawerState.close() }
                                }
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
                                    onSearch = { query ->
                                        homeViewModel.search(query)
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onVideoClick = { video ->
                                        activeVideo = video
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
