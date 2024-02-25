package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.SortOrder

/**
 * The state is for the videos section
 *
 * @property allVideos the all video items
 * @property sortOrder the sort order of video items
 * @property isPendingRefresh
 * @property progressBarShowing the progress bar showing state
 * @property searchMode the search mode state
 * @property scrollToTop the scroll to top state
 * @property selectedVideoHandles the selected video handles
 * @property isInSelection if list is in selection mode or not
 * @property videoPlaylists the video playlists
 * @property currentVideoPlaylist the current video playlist
 * @property isVideoPlaylistCreatedSuccessfully the video playlist created successfully state
 * @property numberOfAddedVideos the number of added videos
 * @property isPlaylistProgressBarShown true if the playlist progress bar is being shown
 * @property isInputTitleValid true if the input title is valid
 * @property shouldCreateVideoPlaylist true if there is a need to create a video playlist
 * @property shouldRenameVideoPlaylist true if there is a need to rename a video playlist
 * @property shouldDeleteVideoPlaylist true if there is a need to delete video playlists
 * @property shouldDeleteSingleVideoPlaylist true if there is a need to delete a single video playlist from detail
 * @property shouldShowMoreVideoPlaylistOptions true if there is a need to show more options of a video playlist
 * @property createVideoPlaylistPlaceholderTitle the create video playlist placeholder title
 * @property createDialogErrorMessage the create dialog error message
 * @property deletedVideoPlaylistTitles the deleted video playlist titles
 * @property areVideoPlaylistsRemovedSuccessfully true if the video playlists are removed successfully
 * @property currentDestinationRoute the current destination route
 */
data class VideoSectionState(
    val allVideos: List<VideoUIEntity> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isPendingRefresh: Boolean = false,
    val progressBarShowing: Boolean = true,
    val searchMode: Boolean = false,
    val scrollToTop: Boolean = false,
    val selectedVideoHandles: List<Long> = emptyList(),
    val isInSelection: Boolean = false,
    val videoPlaylists: List<VideoPlaylistUIEntity> = emptyList(),
    val currentVideoPlaylist: VideoPlaylistUIEntity? = null,
    val isVideoPlaylistCreatedSuccessfully: Boolean = false,
    val numberOfAddedVideos: Int = 0,
    val isPlaylistProgressBarShown: Boolean = true,
    val isInputTitleValid: Boolean = true,
    val shouldCreateVideoPlaylist: Boolean = false,
    val shouldShowMoreVideoPlaylistOptions: Boolean = false,
    val shouldDeleteSingleVideoPlaylist: Boolean = false,
    val createVideoPlaylistPlaceholderTitle: String = "",
    val createDialogErrorMessage: Int? = null,
    val shouldRenameVideoPlaylist: Boolean = false,
    val shouldDeleteVideoPlaylist: Boolean = false,
    val deletedVideoPlaylistTitles: List<String> = emptyList(),
    val areVideoPlaylistsRemovedSuccessfully: Boolean = false,
    val currentDestinationRoute: String? = null
)
