package mega.privacy.android.app.mediaplayer.videoplayer.view

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.isVisible
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.VideoPlayerPlayerViewBinding
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerController
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialNavigationApi::class)
@Composable
internal fun VideoPlayerScreen(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val density = LocalDensity.current

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    val systemUiController = rememberSystemUiController()
    var isControllerViewVisible by rememberSaveable { mutableStateOf(true) }

    val navigationBarHeight =
        getNavigationBarHeight(orientation, density, LocalLayoutDirection.current)
    val navigationBarHeightPx = with(density) { navigationBarHeight.toPx().toInt() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var playbackState by rememberSaveable { mutableIntStateOf(STATE_IDLE) }
    val playerEventListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            playbackState = state
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(uiState.snackBarMessage) {
        uiState.snackBarMessage?.let { message ->
            scaffoldState.snackbarHostState.showSnackbar(message)
            viewModel.updateSnackBarMessage(null)
        }
    }

    DisposableEffect(Unit) {
        player?.addListener(playerEventListener)
        onDispose {
            player?.removeListener(playerEventListener)
        }
    }

    MegaBottomSheetLayout(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        MegaScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
        ) { paddingValues ->
            key(orientation) {
                AndroidViewBinding(
                    modifier = Modifier.fillMaxSize(),
                    factory = { inflater, parent, attachToParent ->
                        VideoPlayerPlayerViewBinding.inflate(inflater, parent, attachToParent)
                            .apply {
                                VideoPlayerController(context, viewModel, root).let {
                                    lifecycle.addObserver(it)
                                }

                                fun updateControllerView(isVisible: Boolean) {
                                    systemUiController.isSystemBarsVisible = isVisible
                                    if (isVisible) {
                                        playerComposeView.showController()
                                    } else {
                                        playerComposeView.hideController()
                                    }
                                }

                                fun toggleControllerVisibility(visible: Boolean) {
                                    autoHideJob?.cancel()
                                    isControllerViewVisible = visible
                                    updateControllerView(visible)

                                    if (visible) {
                                        autoHideJob = coroutineScope.launch {
                                            delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                            isControllerViewVisible = false
                                            updateControllerView(false)
                                        }
                                    }
                                }

                                playerComposeView.setOnClickListener {
                                    toggleControllerVisibility(!isControllerViewVisible)
                                }

                                playerComposeView.setControllerVisibilityListener(
                                    object : PlayerView.ControllerVisibilityListener {
                                        override fun onVisibilityChanged(visibility: Int) {
                                            if (visibility == View.VISIBLE && !isControllerViewVisible) {
                                                autoHideJob?.cancel()
                                                autoHideJob = coroutineScope.launch {
                                                    delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                                    playerComposeView.hideController()
                                                }
                                            }
                                        }
                                    }
                                )

                                playerComposeView.player = player
                                toggleControllerVisibility(true)
                            }
                    },
                ) {
                    val controllerView = root.findViewById<View>(R.id.controls_view)
                    playerComposeView.keepScreenOn =
                        uiState.mediaPlaybackState == MediaPlaybackState.Playing
                    val layoutParams =
                        controllerView.layoutParams as ViewGroup.MarginLayoutParams
                    if (orientation == ORIENTATION_PORTRAIT)
                        layoutParams.bottomMargin = navigationBarHeightPx
                    else
                        layoutParams.marginEnd = navigationBarHeightPx
                    controllerView.layoutParams = layoutParams

                    root.findViewById<ProgressBar>(R.id.loading_video_player_controller_view).isVisible =
                        playbackState <= STATE_BUFFERING

                    root.findViewById<View>(R.id.track_name).isVisible =
                        orientation == ORIENTATION_PORTRAIT
                }
            }
        }
    }
}

@Composable
private fun getNavigationBarHeight(
    orientation: Int,
    density: Density,
    layoutDirection: LayoutDirection,
): Dp {
    return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        with(density) { WindowInsets.navigationBars.getRight(density, layoutDirection).toDp() }
    } else {
        with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    }
}