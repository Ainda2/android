package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import kotlin.time.Duration

/**
 * The entity for the video is displayed in videos section
 *
 * @property id NodeId
 * @property parentId the video's parent id
 * @property name the video's name
 * @property description the video's description
 * @property tags the video's tags
 * @property size the video's size
 * @property fileTypeInfo the video's file type info
 * @property duration the video's duration
 * @property isFavourite the video if is Favourite
 * @property nodeAvailableOffline the video if is available for offline
 * @property isSharedItems the video if is share
 * @property label the video's label
 * @property elementID the element id if the video is belong to a playlist
 * @property isSelected the video if is selected
 * @property isMarkedSensitive the video if is marked as sensitive
 * @property isSensitiveInherited the video if is sensitive inherited
 * @property watchedDate the video's watched date
 * @property collectionTitle the collection title of the video
 * @property hasThumbnail the video if has thumbnail
 */
data class VideoUIEntity(
    val id: NodeId,
    val parentId: NodeId,
    val name: String,
    val description: String? = null,
    val tags: List<String>? = null,
    val size: Long,
    val fileTypeInfo: FileTypeInfo,
    val duration: Duration,
    val isFavourite: Boolean = false,
    val nodeAvailableOffline: Boolean = false,
    val isSharedItems: Boolean = false,
    val label: Int = 0,
    val elementID: Long? = null,
    val isSelected: Boolean = false,
    val isMarkedSensitive: Boolean = false,
    val isSensitiveInherited: Boolean = false,
    val watchedDate: Long = 0,
    val collectionTitle: String? = null,
    val hasThumbnail: Boolean = true,
)
