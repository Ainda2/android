package mega.privacy.android.app.presentation.videosection.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_PLAYLIST_DETAIL
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_RECENTLY_WATCHED_MODE
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_SECTION_MODE
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VideoRecentlyWatchedView
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.videoRecentlyWatchedRoute
import mega.privacy.mobile.analytics.event.RecentlyWatchedOpenedButtonPressedEvent

@Composable
internal fun VideoSectionFeatureScreen(
    modifier: Modifier,
    videoSectionViewModel: VideoSectionViewModel,
    onAddElementsClicked: () -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity, index: Int) -> Unit,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    retryActionCallback: () -> Unit,
) {
    val navHostController = rememberNavController()

    VideoSectionNavHost(
        modifier = modifier,
        navHostController = navHostController,
        viewModel = videoSectionViewModel,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = onMenuClick,
        onAddElementsClicked = onAddElementsClicked,
        onMenuAction = onMenuAction,
        retryActionCallback = retryActionCallback
    )
}

@Composable
internal fun VideoSectionNavHost(
    navHostController: NavHostController,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity, index: Int) -> Unit,
    onAddElementsClicked: () -> Unit,
    modifier: Modifier,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    retryActionCallback: () -> Unit,
    viewModel: VideoSectionViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    var enableFavouritesPlaylistMenu by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enableFavouritesPlaylistMenu) {
        enableFavouritesPlaylistMenu = viewModel.enableFavouritesPlaylistMenu()
    }

    val onDeleteVideosDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit = { playlist ->
        val removedVideoIDs = state.selectedVideoElementIDs
        viewModel.removeVideosFromPlaylist(playlist.id, removedVideoIDs)
        viewModel.clearAllSelectedVideosOfPlaylist()
    }

    if (state.isVideoPlaylistCreatedSuccessfully) {
        viewModel.setIsVideoPlaylistCreatedSuccessfully(false)
        navHostController.navigate(
            route = videoPlaylistDetailRoute,
        )
    }

    if (state.areVideoPlaylistsRemovedSuccessfully) {
        viewModel.setAreVideoPlaylistsRemovedSuccessfully(false)
        if (navHostController.currentDestination?.route == videoPlaylistDetailRoute) {
            navHostController.popBackStack()
        }
    }

    navHostController.addOnDestinationChangedListener { _, destination, _ ->
        destination.route?.let { route ->
            viewModel.setCurrentDestinationRoute(route)
            if (route != videoPlaylistDetailRoute) {
                viewModel.updateCurrentVideoPlaylist(null)
            }
        }
    }

    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = videoSectionRoute
    ) {
        composable(
            route = videoSectionRoute
        ) {
            VideoSectionComposeView(
                videoSectionViewModel = viewModel,
                onClick = viewModel::onItemClicked,
                onSortOrderClick = onSortOrderClick,
                onMenuClick = { onMenuClick(it, VIDEO_SECTION_MODE) },
                onLongClick = viewModel::onItemLongClicked,
                onPlaylistItemClick = { playlist, index ->
                    if (state.isInSelection) {
                        viewModel.onVideoPlaylistItemClicked(playlist, index)
                    } else {
                        viewModel.updateCurrentVideoPlaylist(playlist)
                        navHostController.navigate(route = videoPlaylistDetailRoute)
                    }
                },
                onPlaylistItemLongClick = viewModel::onVideoPlaylistItemClicked,
                onDeleteDialogButtonClicked = viewModel::clearAllSelectedVideoPlaylists,
                onMenuAction = { action ->
                    if (action is VideoSectionMenuAction.VideoRecentlyWatchedAction) {
                        Analytics.tracker.trackEvent(RecentlyWatchedOpenedButtonPressedEvent)
                        navHostController.navigate(route = videoRecentlyWatchedRoute)
                    } else {
                        onMenuAction(action)
                    }
                },
                retryActionCallback = retryActionCallback
            )
        }
        composable(
            route = videoPlaylistDetailRoute
        ) {
            VideoPlaylistDetailView(
                playlist = state.currentVideoPlaylist,
                selectedSize = state.selectedVideoElementIDs.size,
                shouldApplySensitiveMode = state.hiddenNodeEnabled
                        && state.accountType?.isPaid == true
                        && !state.isBusinessAccountExpired,
                isHideMenuActionVisible = state.isHideMenuActionVisible,
                isUnhideMenuActionVisible = state.isUnhideMenuActionVisible,
                isInputTitleValid = state.isInputTitleValid,
                numberOfAddedVideos = state.numberOfAddedVideos,
                addedMessageShown = viewModel::clearNumberOfAddedVideos,
                numberOfRemovedItems = state.numberOfRemovedItems,
                removedMessageShown = viewModel::clearNumberOfRemovedItems,
                inputPlaceHolderText = state.createVideoPlaylistPlaceholderTitle,
                setInputValidity = viewModel::setNewPlaylistTitleValidity,
                onRenameDialogPositiveButtonClicked = viewModel::updateVideoPlaylistTitle,
                onDeleteDialogPositiveButtonClicked = viewModel::removeVideoPlaylists,
                onAddElementsClicked = onAddElementsClicked,
                errorMessage = state.createDialogErrorMessage,
                onClick = { item, index ->
                    if (navHostController.currentDestination?.route == videoPlaylistDetailRoute) {
                        viewModel.onVideoItemOfPlaylistClicked(item, index)
                    }
                },
                onMenuClick = { onMenuClick(it, VIDEO_PLAYLIST_DETAIL) },
                onLongClick = viewModel::onVideoItemOfPlaylistLongClicked,
                onDeleteVideosDialogPositiveButtonClicked = onDeleteVideosDialogPositiveButtonClicked,
                onPlayAllClicked = viewModel::playAllButtonClicked,
                onBackPressed = {
                    if (state.selectedVideoElementIDs.isNotEmpty()) {
                        viewModel.clearAllSelectedVideosOfPlaylist()
                    } else {
                        onBackPressedDispatcher?.onBackPressed()
                    }
                },
                onMenuActionClick = { action ->
                    when (action) {
                        is VideoSectionMenuAction.VideoSectionSelectAllAction ->
                            viewModel.selectAllVideosOfPlaylist()

                        is VideoSectionMenuAction.VideoSectionClearSelectionAction ->
                            viewModel.clearAllSelectedVideosOfPlaylist()

                        else -> {
                            onMenuAction(action)
                        }
                    }
                },
                enableFavouritesPlaylistMenu = enableFavouritesPlaylistMenu,
                onRemoveFavouriteOptionClicked = {
                    viewModel.removeFavourites()
                    viewModel.clearAllSelectedVideosOfPlaylist()
                }
            )
        }

        composable(route = videoRecentlyWatchedRoute) {
            VideoRecentlyWatchedView(
                group = state.groupedVideoRecentlyWatchedItems,
                shouldApplySensitiveMode = state.hiddenNodeEnabled
                        && state.accountType?.isPaid == true
                        && !state.isBusinessAccountExpired,
                clearRecentlyWatchedVideosSuccess = state.clearRecentlyWatchedVideosSuccess,
                removeRecentlyWatchedItemSuccess = state.removeRecentlyWatchedItemSuccess,
                modifier = Modifier,
                onBackPressed = { onBackPressedDispatcher?.onBackPressed() },
                onClick = viewModel::onItemClicked,
                onActionPressed = {
                    if (it is VideoSectionMenuAction.VideoRecentlyWatchedClearAction) {
                        viewModel.clearRecentlyWatchedVideos()
                    }
                },
                onMenuClick = { onMenuClick(it, VIDEO_RECENTLY_WATCHED_MODE) },
                clearRecentlyWatchedVideosMessageShown = viewModel::resetClearRecentlyWatchedVideosSuccess,
                removedRecentlyWatchedItemMessageShown = viewModel::resetRemoveRecentlyWatchedItemSuccess
            )
        }
    }
}