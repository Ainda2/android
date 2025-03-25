package mega.privacy.android.app.presentation.videosection.view.playlist

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.appbar.SelectModeAppBar

@Composable
internal fun VideoPlaylistDetailTopBar(
    title: String,
    isActionMode: Boolean,
    selectedSize: Int,
    isSystemVideoPlaylist: Boolean,
    isHideMenuActionVisible: Boolean,
    isUnhideMenuActionVisible: Boolean,
    onMenuActionClick: (VideoSectionMenuAction?) -> Unit,
    onBackPressed: () -> Unit,
    enableFavouritesPlaylistMenu: Boolean,
) {
    if (isActionMode) {
        SelectModeAppBar(
            title = selectedSize.toString(),
            actions = when {
                selectedSize == 0 -> emptyList()
                isSystemVideoPlaylist -> listOf(
                    VideoSectionMenuAction.VideoSectionDownloadAction,
                    VideoSectionMenuAction.VideoSectionSendToChatAction,
                    VideoSectionMenuAction.VideoSectionShareAction,
                    VideoSectionMenuAction.VideoSectionMoreAction
                )

                else ->
                    mutableListOf<VideoSectionMenuAction>().apply {
                        add(VideoSectionMenuAction.VideoSectionRemoveAction)
                        add(VideoSectionMenuAction.VideoSectionSelectAllAction)
                        add(VideoSectionMenuAction.VideoSectionClearSelectionAction)
                        if (isHideMenuActionVisible) {
                            add(VideoSectionMenuAction.VideoSectionHideAction)
                        }
                        if (isUnhideMenuActionVisible) {
                            add(VideoSectionMenuAction.VideoSectionUnhideAction)
                        }
                    }
            },
            onActionPressed = {
                onMenuActionClick(it as? VideoSectionMenuAction)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG),
            onNavigationPressed = onBackPressed
        )
    } else {
        MegaAppBar(
            modifier = Modifier.testTag(VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG),
            appBarType = AppBarType.BACK_NAVIGATION,
            title = title,
            onNavigationPressed = onBackPressed,
            actions = when {
                isSystemVideoPlaylist && !enableFavouritesPlaylistMenu -> emptyList()
                isSystemVideoPlaylist -> listOf(VideoSectionMenuAction.VideoSectionSortByAction)
                else -> listOf(VideoSectionMenuAction.VideoSectionMoreAction)
            },
            onActionPressed = { onMenuActionClick(it as? VideoSectionMenuAction) },
            windowInsets = WindowInsets(0.dp)
        )
    }
}

/**
 * Test tag for top bar
 */
const val VIDEO_PLAYLIST_DETAIL_TOP_BAR_TEST_TAG = "video_playlist_detail_view:top_bar"

/**
 * Test tag for selected mode top bar
 */
const val VIDEO_PLAYLIST_DETAIL_SELECTED_MODE_TOP_BAR_TEST_TAG =
    "video_playlist_detail_view:top_bar_selected_mode"