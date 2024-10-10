package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import javax.inject.Inject

/**
 * The mapper class to convert the VideoPlaylist to VideoPlaylistUIEntity
 */
class VideoPlaylistUIEntityMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val videoUIEntityMapper: VideoUIEntityMapper,
) {

    /**
     * Convert to VideoPlaylist to VideoPlaylistUIEntity
     */
    operator fun invoke(videoPlaylist: VideoPlaylist) =
        VideoPlaylistUIEntity(
            id = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.id else NodeId(-1),
            title = when (videoPlaylist) {
                is UserVideoPlaylist -> videoPlaylist.title
                else -> ""
            },
            cover = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.cover else null,
            creationTime = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.creationTime else 0,
            modificationTime = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.modificationTime else 0,
            thumbnailList = videoPlaylist.thumbnailList,
            numberOfVideos = videoPlaylist.numberOfVideos,
            totalDuration = durationInSecondsTextMapper(videoPlaylist.totalDuration),
            videos = videoPlaylist.videos?.map {
                videoUIEntityMapper(it)
            }
        )
}
