package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R.drawable
import mega.privacy.android.app.R.string
import mega.privacy.android.app.presentation.imagepreview.slideshow.SlideshowViewModel
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.ImageResultStatus
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowMenuAction.SettingOptionsMenuAction
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.mobile.analytics.event.SlideShowScreenEvent

@Composable
fun SlideshowScreen(
    onClickSettingMenu: () -> Unit,
    onClickBack: () -> Unit,
    viewModel: SlideshowViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes

    if (viewState.isInitialized && imageNodes.isEmpty()) {
        LaunchedEffect(Unit) {
            onClickBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Analytics.tracker.trackEvent(SlideShowScreenEvent)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentImageNodeIndex = viewState.currentImageNodeIndex
    val currentImageNode = viewState.currentImageNode
    currentImageNode?.let { node ->
        val zoomableStateMap = remember { mutableMapOf<NodeId, ZoomableState?>() }
        val isNodeDownloaded = remember { mutableStateMapOf<NodeId, Boolean>() }

        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()
        val speed = viewState.speed ?: SlideshowSpeed.Normal
        val repeat = viewState.repeat
        val isPlaying = viewState.isPlaying
        val pagerState = rememberPagerState(
            initialPage = currentImageNodeIndex,
            initialPageOffsetFraction = 0f,
            pageCount = { imageNodes.size },
        )

        MegaScaffold(
            scaffoldState = scaffoldState,
            contentWindowInsets = WindowInsets.ime,
        ) { paddingValues ->
            SlideShowContent(
                modifier = Modifier
                    .background(Color.Black)
                    .padding(paddingValues),
                topBar = {
                    if (!isPlaying) {
                        SlideshowTopBar(
                            onClickBack = onClickBack,
                            onClickSettingMenu = onClickSettingMenu,
                        )
                    }
                },
                bottomBar = {
                    if (!isPlaying) {
                        SlideshowBottomBar(
                            onPlayOrPauseSlideshow = {
                                coroutineScope.launch {
                                    val page = pagerState.currentPage
                                    for (candidatePage in page - 1..page + 1) {
                                        viewState.imageNodes.getOrNull(candidatePage)?.let { node ->
                                            zoomableStateMap[node.id]?.resetZoom()
                                        }
                                    }
                                    viewModel.updateIsPlaying(isPlaying = true)
                                }
                            },
                        )
                    }
                },
                pagerState = pagerState,
                imageNodes = imageNodes,
                isPlaying = viewState.isPlaying,
                downloadImage = viewModel::monitorImageResult,
                getImagePath = viewModel::getHighestResolutionImagePath,
                getErrorImagePath = viewModel::getFallbackImagePath,
                onTapImage = { viewModel.updateIsPlaying(false) },
                onImageZooming = { viewModel.updateIsPlaying(false) },
                onCacheImageState = { node, zoomState ->
                    zoomableStateMap[node.id] = zoomState
                },
                onImageDownloadStatus = { node, isDownloaded ->
                    isNodeDownloaded[node.id] = isDownloaded
                },
            )
        }

        LaunchedEffect(pagerState.currentPage) {
            val page = pagerState.currentPage
            viewState.imageNodes.getOrNull(page)?.let { node ->
                viewModel.setCurrentImageNode(node)
                viewModel.setCurrentImageNodeIndex(page)

                repeat(2) { viewModel.preloadImageNode(page + 1 + it) }
            }

            for (candidatePage in page - 1..page + 1) {
                viewState.imageNodes.getOrNull(candidatePage)?.let { node ->
                    coroutineScope.launch {
                        zoomableStateMap[node.id]?.resetZoom()
                    }
                }
            }
        }

        LaunchedEffect(pagerState.canScrollForward) {
            // Not repeat and the last one.
            if (!pagerState.canScrollForward && !repeat && isPlaying) {
                viewModel.updateIsPlaying(false)
            }
        }

        LaunchedEffect(isPlaying, currentImageNodeIndex, speed, isNodeDownloaded[node.id]) {
            if (viewState.imageNodes.getOrNull(currentImageNodeIndex) == node) {
                if (isPlaying && isNodeDownloaded[node.id] == true) {
                    delay(speed.duration.inWholeMilliseconds)

                    var nextIdx = if (pagerState.canScrollForward) currentImageNodeIndex + 1 else 0
                    if (nextIdx >= pagerState.pageCount) nextIdx = 0

                    pagerState.scrollToPage(nextIdx)
                }
            }
        }
    }
}

@Composable
private fun SlideshowBottomBar(
    onPlayOrPauseSlideshow: () -> Unit,
) {
    Box(
        modifier = Modifier.background(MaterialTheme.colors.white_alpha_070_grey_alpha_070)
    ) {
        BottomAppBar(
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            modifier =Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onPlayOrPauseSlideshow) {
                    Icon(
                        painter = painterResource(id = drawable.ic_play_video_recorded),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideshowTopBar(
    onClickBack: () -> Unit,
    onClickSettingMenu: () -> Unit,
) {
    MegaAppBar(
        title = stringResource(string.action_slideshow),
        appBarType = AppBarType.BACK_NAVIGATION,
        elevation = 0.dp,
        onNavigationPressed = {
            onClickBack()
        },
        actions = listOf(
            SettingOptionsMenuAction
        ),
        onActionPressed = {
            onClickSettingMenu()
        }
    )
}

@Composable
private fun SlideShowContent(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    isPlaying: Boolean,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImagePath: suspend (ImageResult?) -> String?,
    getErrorImagePath: suspend (ImageResult?) -> String?,
    onTapImage: () -> Unit,
    onImageZooming: (ZoomableState) -> Unit,
    onCacheImageState: (ImageNode, ZoomableState) -> Unit,
    onImageDownloadStatus: (ImageNode, Boolean) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L },
        ) { index ->
            val imageNode = imageNodes[index]
            val status by produceState(
                initialValue = ImageResultStatus(0, false, null, null),
                key1 = imageNode,
            ) {
                downloadImage(imageNode).collectLatest { imageResult ->
                    value = ImageResultStatus(
                        progress = imageResult.getProgressPercentage() ?: 0,
                        isFullyLoaded = imageResult.isFullyLoaded,
                        imagePath = getImagePath(imageResult),
                        errorImagePath = getErrorImagePath(imageResult)
                    )
                }
            }

            val (progress, isDownloaded, fullSizePath, errorImagePath) = status
            onImageDownloadStatus(imageNode, isDownloaded)

            val zoomableState = rememberZoomableState(
                zoomSpec = ZoomSpec(maxZoomFactor = Int.MAX_VALUE.toFloat())
            )
            val imageState = rememberZoomableImageState(zoomableState)
            onCacheImageState(imageNode, imageState.zoomableState)

            LaunchedEffect(zoomableState.zoomFraction) {
                val fraction = zoomableState.zoomFraction
                if (fraction != null && fraction > 0.0f) {
                    onImageZooming(zoomableState)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                ImageContent(
                    imageState = imageState,
                    onTapImage = onTapImage,
                    fullSizePath = if (imageNode.serializedData == "localFile") imageNode.fullSizePath else fullSizePath,
                    errorImagePath = if (imageNode.serializedData == "localFile") imageNode.fullSizePath else errorImagePath,
                )

                if (isPlaying && progress < 100 && !isDownloaded) {
                    val loadingModifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp)
                        .width(32.dp)

                    if (progress <= 0) {
                        CircularProgressIndicator(
                            modifier = loadingModifier,
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = progress.toFloat() / 100,
                            modifier = loadingModifier,
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.TopCenter),
            content = { topBar() },
        )

        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter),
            content = { bottomBar() },
        )
    }
}

@Composable
private fun ImageContent(
    imageState: ZoomableImageState,
    onTapImage: () -> Unit,
    fullSizePath: String?,
    errorImagePath: String?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        var imagePath by remember(fullSizePath) {
            mutableStateOf(fullSizePath)
        }

        val request = ImageRequest.Builder(LocalContext.current)
            .data(imagePath)
            .listener(
                onError = { _, _ ->
                    // when some image full size picture decoder throw exception, use preview/thumbnail instead
                    // detail see package coil.decode [BitmapFactoryDecoder] 79 line
                    imagePath = errorImagePath
                }
            )
            .crossfade(1000)
            .build()

        ZoomableAsyncImage(
            model = request,
            state = imageState,
            contentDescription = "Image Preview",
            modifier = Modifier.fillMaxSize(),
            onClick = { onTapImage() },
            onDoubleClick = DoubleClickToZoomListener.cycle(maxZoomFactor = 3f),
        )
    }
}