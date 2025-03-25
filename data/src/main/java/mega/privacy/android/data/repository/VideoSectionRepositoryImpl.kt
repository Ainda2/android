package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.listener.AddElementToSetsListenerInterface
import mega.privacy.android.data.listener.CreateSetElementListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.RemoveSetElementListenerInterface
import mega.privacy.android.data.listener.RemoveSetsListenerInterface
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.FavouritesVideoPlaylistMapper
import mega.privacy.android.data.mapper.videosection.UserVideoPlaylistMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedItemMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of VideoSectionRepository
 */
@Singleton
internal class VideoSectionRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val fileNodeMapper: FileNodeMapper,
    private val typedVideoNodeMapper: TypedVideoNodeMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val userSetMapper: UserSetMapper,
    private val userVideoPlaylistMapper: UserVideoPlaylistMapper,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val appPreferencesGateway: AppPreferencesGateway,
    private val videoRecentlyWatchedItemMapper: VideoRecentlyWatchedItemMapper,
    private val favouritesVideoPlaylistMapper: FavouritesVideoPlaylistMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoSectionRepository {
    private val videoPlaylistsMap: MutableMap<Long, UserSet> = mutableMapOf()
    private val videoSetsMap: MutableMap<NodeId, MutableSet<Long>> = mutableMapOf()

    override suspend fun getAllVideos(
        searchQuery: String,
        tag: String?,
        description: String?,
        order: SortOrder,
    ): List<TypedVideoNode> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            getAllVideoMegaNodes(
                searchQuery = searchQuery,
                tag = tag,
                description = description,
                order = order
            ).map { megaNode ->
                val isOutShared =
                    megaApiGateway.getMegaNodeByHandle(megaNode.parentHandle)?.isOutShare == true
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(offlineItems[megaNode.handle.toString()]),
                    duration = megaNode.duration,
                    isOutShared = isOutShared
                )
            }
        }

    private suspend fun getAllVideoMegaNodes(
        searchQuery: String = "",
        tag: String? = null,
        description: String? = null,
        order: SortOrder,
    ): List<MegaNode> {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            searchTarget = SearchTarget.ROOT_NODES,
            searchCategory = SearchCategory.VIDEO,
            searchQuery = searchQuery,
            tag = tag,
            description = description,
            useAndForTextQuery = description == null && tag == null,
        )
        return megaApiGateway.searchWithFilter(
            filter,
            sortOrderIntMapper(order),
            megaCancelToken,
        ).filter { !megaApiGateway.isInBackups(it) }
    }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo().associateBy { it.handle }

    private suspend fun MegaNode.convertToFileNode(offline: Offline?) = fileNodeMapper(
        megaNode = this, requireSerializedData = false, offline = offline
    )

    override suspend fun getVideoPlaylists(sortOrder: SortOrder): List<VideoPlaylist> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            val systemVideoPlaylist = listOf(getFavouritesVideoPlaylist(sortOrder, offlineItems))
            val userVideoPlaylists = getAllUserSets().map { userSet ->
                userSet.toVideoPlaylist(offlineItems)
            }
            systemVideoPlaylist + userVideoPlaylists
        }

    private suspend fun getFavouritesVideoPlaylist(
        sortOrder: SortOrder,
        offlineItems: Map<String, Offline>,
    ): FavouritesVideoPlaylist {
        val favouriteVideos =
            getAllVideoMegaNodes(
                order = sortOrder
            ).filter { it.isFavourite }.map { megaNode ->
                val isOutShared =
                    megaApiGateway.getMegaNodeByHandle(megaNode.parentHandle)?.isOutShare == true
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(offlineItems[megaNode.handle.toString()]),
                    duration = megaNode.duration,
                    isOutShared = isOutShared
                )
            }
        return favouritesVideoPlaylistMapper(favouriteVideos)
    }

    private suspend fun getAllUserSets(): List<UserSet> {
        videoPlaylistsMap.clear()
        videoSetsMap.clear()
        val setList = megaApiGateway.getSets()
        val userSets = (0 until setList.size())
            .filter { index ->
                setList.get(index).type() == MegaSet.SET_TYPE_PLAYLIST
            }.map {
                setList.get(it).toUserSet()
            }
            .associateBy { it.id }
        videoPlaylistsMap.putAll(userSets)
        return userSets.values.toList()
    }

    private fun MegaSet.toUserSet(): UserSet {
        val cover = cover().takeIf { it != -1L }
        return userSetMapper(
            id(),
            name(),
            type(),
            cover,
            cts(),
            ts(),
            isExported
        )
    }

    private suspend fun UserSet.toVideoPlaylist(offlineMap: Map<String, Offline>?): VideoPlaylist {
        val elementList = megaApiGateway.getSetElements(sid = id)
        val videoNodeList = (0 until elementList.size()).mapNotNull { index ->
            val element = elementList[index]
            val elementNode = element.node()

            megaApiGateway.getMegaNodeByHandle(elementNode)?.let { megaNode ->
                val isInRubbish = megaApiGateway.isInRubbish(megaNode)
                if (isInRubbish) return@mapNotNull null

                videoSetsMap.getOrPut(NodeId(elementNode)) { mutableSetOf() }
                    .add(element.setId())
                typedVideoNodeMapper(
                    fileNode = megaNode.convertToFileNode(
                        offlineMap?.get(megaNode.handle.toString())
                    ),
                    duration = megaNode.duration,
                    elementID = element.id()
                )
            }
        }.sortedBy { it.name }
        return userVideoPlaylistMapper(
            userSet = this,
            videoNodeList = videoNodeList
        )
    }

    override suspend fun createVideoPlaylist(title: String): VideoPlaylist =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { megaRequest, megaError ->
                        if (megaError.errorCode == MegaError.API_OK) {
                            val newSet = megaRequest.megaSet
                            continuation.resumeWith(
                                Result.success(
                                    userVideoPlaylistMapper(
                                        userSet = newSet.toUserSet(),
                                        videoNodeList = emptyList()
                                    )
                                )
                            )
                        } else {
                            Timber.e("Error creating new playlist: ${megaError.errorString}")
                            continuation.failWithError(megaError, "createPlaylist")
                        }
                    }
                )
                megaApiGateway.createSet(
                    name = title,
                    type = MegaSet.SET_TYPE_PLAYLIST,
                    listener = listener
                )
            }
        }

    override suspend fun removeVideoPlaylists(playlistIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = RemoveSetsListenerInterface(
                    target = playlistIDs.size,
                    onCompletion = { success, _ ->
                        continuation.resumeWith(Result.success(success))
                    }
                )
                for (playlistID in playlistIDs) {
                    megaApiGateway.removeSet(
                        sid = playlistID.longValue,
                        listener = listener,
                    )
                }
            }
        }

    override suspend fun removeVideosFromPlaylist(
        playlistID: NodeId,
        videoElementIDs: List<Long>,
    ) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                launch {
                    val listener = RemoveSetElementListenerInterface(
                        target = videoElementIDs.size,
                        onCompletion = { success, _ ->
                            continuation.resumeWith(Result.success(success))
                        }
                    )
                    for (elementID in videoElementIDs) {
                        megaApiGateway.removeSetElement(
                            sid = playlistID.longValue,
                            eid = elementID,
                            listener = listener,
                        )
                    }
                }
            }
        }

    override suspend fun addVideosToPlaylist(playlistID: NodeId, videoIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = CreateSetElementListenerInterface(
                    target = videoIDs.size,
                    onCompletion = { success, _ ->
                        continuation.resumeWith(Result.success(success))
                    }
                )
                for (videoID in videoIDs) {
                    megaApiGateway.createSetElement(
                        sid = playlistID.longValue,
                        node = videoID.longValue,
                        listener = listener
                    )
                }
            }
        }

    override suspend fun addVideoToMultiplePlaylists(playlistIDs: List<Long>, videoID: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = AddElementToSetsListenerInterface(
                    target = playlistIDs,
                    onCompletion = { success, _ ->
                        continuation.resumeWith(Result.success(success))
                    }
                )
                for (playlistID in playlistIDs) {
                    megaApiGateway.createSetElement(
                        sid = playlistID,
                        node = videoID,
                        listener = listener
                    )
                }
            }
        }

    override suspend fun updateVideoPlaylistTitle(
        playlistID: NodeId,
        newTitle: String,
    ): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("updateVideoPlaylistTitle") {
                    it.text
                }
                megaApiGateway.updateSetName(
                    sid = playlistID.longValue,
                    name = newTitle,
                    listener = listener
                )
            }
        }

    override fun monitorSetsUpdates(): Flow<List<Long>> = merge(megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
        .mapNotNull { it.sets }
        .map { sets ->
            sets.filter {
                it.type() == MegaSet.SET_TYPE_PLAYLIST
            }.map {
                it.id()
            }
        },
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnSetElementsUpdate>()
            .mapNotNull { it.elements }
            .map { elements ->
                val updatedIds = elements.map { it.setId() }
                if (updatedIds.any { it in videoPlaylistsMap.keys }) {
                    updatedIds
                } else {
                    emptyList()
                }
            }
    )

    override fun getVideoSetsMap() = videoSetsMap

    override fun getVideoPlaylistsMap() = videoPlaylistsMap

    override suspend fun getVideoPlaylistSets(): List<UserSet> = getAllUserSets()

    override suspend fun saveVideoRecentlyWatched(
        handle: Long,
        timestamp: Long,
        collectionId: Long,
        collectionTitle: String?,
    ) =
        withContext(ioDispatcher) {
            migrateOldDataToDatabase()
            megaLocalRoomGateway.saveRecentlyWatchedVideo(
                videoRecentlyWatchedItemMapper(
                    handle,
                    timestamp,
                    collectionId,
                    collectionTitle
                )
            )
        }

    private suspend fun migrateOldDataToDatabase() = withContext(ioDispatcher) {
        val items: List<VideoRecentlyWatchedItem>? =
            appPreferencesGateway.monitorString(
                PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS,
                null
            ).firstOrNull()?.let { jsonString ->
                Json.decodeFromString(jsonString)
            }
        if (items.isNullOrEmpty()) return@withContext

        megaLocalRoomGateway.saveRecentlyWatchedVideos(items)

        appPreferencesGateway.putString(
            PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS,
            Json.encodeToString(emptyList<VideoRecentlyWatchedItem>())
        )
    }

    override suspend fun monitorRecentlyWatchedVideoNodes(): Flow<List<TypedVideoNode>> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            getRecentlyWatchedData().map { list ->
                list.mapNotNull { item ->
                    megaApiGateway.getMegaNodeByHandle(item.videoHandle)?.let { megaNode ->
                        val title =
                            megaNode.getCollectionTitle(item.collectionId, item.collectionTitle)
                        typedVideoNodeMapper(
                            fileNode = megaNode.convertToFileNode(offlineItems[megaNode.handle.toString()]),
                            duration = megaNode.duration,
                            watchedTimestamp = item.watchedTimestamp,
                            collectionTitle = title
                        )
                    }
                }.sortedByDescending { it.watchedTimestamp }
            }
        }

    private suspend fun getRecentlyWatchedData(): Flow<List<VideoRecentlyWatchedItem>> {
        migrateOldDataToDatabase()
        return megaLocalRoomGateway.getAllRecentlyWatchedVideos()
    }

    private suspend fun MegaNode.getCollectionTitle(
        collectionId: Long,
        collectionTitle: String?,
    ): String? =
        when {
            collectionId != 0L -> {
                val elementList = megaApiGateway.getSetElements(collectionId)
                val isInCollection = (0 until elementList.size()).mapNotNull { index ->
                    elementList[index].node()
                }.any { it == handle }
                if (isInCollection) {
                    megaApiGateway.getSet(collectionId)?.name()
                } else null
            }

            collectionTitle != null && isFavourite -> collectionTitle

            else -> null
        }

    override suspend fun clearRecentlyWatchedVideos() = withContext(ioDispatcher) {
        megaLocalRoomGateway.clearRecentlyWatchedVideos()
    }

    override suspend fun removeRecentlyWatchedItem(handle: Long) =
        megaLocalRoomGateway.removeRecentlyWatchedVideo(handle)

    companion object {
        private const val PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS =
            "PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS"
    }
}


