package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumIdLink
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.UserSetPhotoIds
import mega.privacy.android.domain.entity.set.UserSet

/**
 * Album repository
 */
interface AlbumRepository {
    /**
     * Get all user sets
     *
     * @return a list of UserSet
     */
    suspend fun getAllUserSets(): List<UserSet>

    /**
     * Get a user set
     *
     * @param albumId is the album's id to get the user set
     * @return the user set if exist
     */
    suspend fun getUserSet(albumId: AlbumId): UserSet?

    /**
     * Get album element ids
     *
     * @param albumId the id of the album which elements we want to get
     * @param refresh if the elements should be reused from cache
     *
     * @return a list of node id's
     */
    suspend fun getAlbumElementIDs(albumId: AlbumId, refresh: Boolean = false): List<AlbumPhotoId>

    /**
     * Create an album
     *
     * @param name the name of the album
     */
    suspend fun createAlbum(name: String): UserSet

    /**
     * Add photos to an album
     *
     * @param albumID the id of the album which we want to put the photos in
     * @param photoIDs the photos' node handles
     */
    suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>, isAsync: Boolean)

    /**
     * Add bulk photos to an album
     *
     * @param albumId album id which we want to add the photos into
     * @param photoIds the photos' node handles
     *
     * @return number of photos successfully added
     */
    suspend fun addBulkPhotosToAlbum(albumId: AlbumId, photoIds: List<NodeId>): Int

    /**
     * Remove photos from an album
     *
     * @param albumID the id of the album which we want to remove the elements from
     * @param photoIDs the photos' set of IDs to be removed from the album
     */
    suspend fun removePhotosFromAlbum(albumID: AlbumId, photoIDs: List<AlbumPhotoId>)

    /**
     * Monitor user sets update
     *
     * @return a flow of all new user sets update
     */
    fun monitorUserSetsUpdate(): Flow<List<UserSet>>

    /**
     * Monitor user set's element ids update
     *
     * @param albumId the id of the album which we want to map the ids associated with
     * @return a flow of all new element ids update
     */
    fun monitorAlbumElementIds(albumId: AlbumId): Flow<List<AlbumPhotoId>>

    /**
     * Remove user albums
     * @param albumIds the album ids to be removed
     * @return throw exception if the operation fails
     */
    suspend fun removeAlbums(albumIds: List<AlbumId>)

    /**
     * Observe album photos adding progress
     *
     * @param albumId the album id to be observed its photos adding progress
     * @return a flow of progress
     */
    fun observeAlbumPhotosAddingProgress(albumId: AlbumId): Flow<AlbumPhotosAddingProgress?>

    /**
     * Update to acknowledge album photos adding progress is completed
     *
     * @param albumId the album id to be observed its photos adding progress
     */
    suspend fun updateAlbumPhotosAddingProgressCompleted(albumId: AlbumId)

    /**
     * Observe album photos removing progress
     *
     * @param albumId the album id to be observed its photos removing progress
     * @return a flow of progress
     */
    fun observeAlbumPhotosRemovingProgress(albumId: AlbumId): Flow<AlbumPhotosRemovingProgress?>

    /**
     * Update to acknowledge album photos removing progress is completed
     *
     * @param albumId the album id to be observed its photos removing progress
     */
    suspend fun updateAlbumPhotosRemovingProgressCompleted(albumId: AlbumId)

    /**
     * Update album name
     *
     * @param albumId the album id
     * @param newName new album name
     */
    suspend fun updateAlbumName(
        albumId: AlbumId,
        newName: String,
    ): String

    /**
     * Get all the names that User Albums are not allowed to have
     */
    suspend fun getProscribedAlbumTitles(): List<String>

    /**
     * Update album cover
     *
     * @param albumId the album id
     * @param elementId the element id to be set as cover
     */
    suspend fun updateAlbumCover(albumId: AlbumId, elementId: NodeId)

    /**
     * Export albums
     *
     * @param albumIds list of album ids to be exported
     * @return list of generated links
     */
    suspend fun exportAlbums(albumIds: List<AlbumId>): List<AlbumIdLink>

    /**
     * Disable export albums
     *
     * @param albumIds list of exported album ids to be disabled
     * @return number of successful operations
     */
    suspend fun disableExportAlbums(albumIds: List<AlbumId>): Int

    /**
     * Fetch public album
     *
     * @param albumLink public link as identifier
     * @return a pair of set and photo id list
     */
    suspend fun fetchPublicAlbum(albumLink: AlbumLink): UserSetPhotoIds

    /**
     * Fetch public photos
     *
     * @param albumPhotoIds album photo id list
     * @return photo list
     */
    suspend fun getPublicPhotos(albumPhotoIds: List<AlbumPhotoId>): List<Photo>

    /**
     * Download public thumbnail
     *
     * @param photo which the thumbnail to be downloaded
     * @param callback to notify if the operation is successful
     */
    suspend fun downloadPublicThumbnail(photo: Photo, callback: (Boolean) -> Unit)

    /**
     * Download public preview
     *
     * @param photo which the preview to be downloaded
     * @param callback to notify if the operation is successful
     */
    suspend fun downloadPublicPreview(photo: Photo, callback: (Boolean) -> Unit)

    /**
     * Get serialized MegaNode list
     *
     * @return map of MegaNode data
     */
    fun getPublicAlbumNodesData(): Map<NodeId, String>

    /**
     * Save album photos to folder
     *
     * @param folderName the name of folder to be created
     * @param photoIds list of photos to be saved
     * @param targetParentFolderNodeId parent of target folder
     *
     * @return list of saved photo ids
     */
    suspend fun saveAlbumToFolder(
        folderName: String,
        photoIds: List<NodeId>,
        targetParentFolderNodeId: NodeId,
    ): List<NodeId>

    /**
     * Get ImageNode given public photo handle
     *
     * @param nodeId Node id of public photo
     * @return Image node
     */
    suspend fun getPublicPhotoImageNode(nodeId: NodeId): ImageNode

    /**
     * Get public ImageNodes
     *
     * @return Image nodes
     */
    suspend fun getPublicImageNodes(): List<ImageNode>

    /**
     * Get Photo given public photo handle
     *
     * @param nodeId Node id of public photo
     * @return photo
     */
    suspend fun getPublicPhoto(nodeId: NodeId): Photo?

    /**
     * Get album photo file url by node handle
     *
     * @param nodeId - Node id of public photo
     * @return local link
     */
    suspend fun getAlbumPhotoFileUrlByNodeHandle(nodeId: NodeId): String?

    /**
     * Check if album link is valid
     *
     * @param albumLink Album link to be checked
     * @return Link's validity
     */
    suspend fun isAlbumLinkValid(albumLink: AlbumLink): Boolean

    /**
     * Clear album cache
     *
     * @param albumId Album id to be cleared
     */
    fun clearAlbumCache(albumId: AlbumId)

    /**
     * Clear all albums cache
     */
    fun clearCache()
}
