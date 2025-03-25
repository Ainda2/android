package mega.privacy.android.data.repository

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.facade.AlbumStringResourceGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.CopyPreviewNodeListenerInterface
import mega.privacy.android.data.listener.CreateSetElementListenerInterface
import mega.privacy.android.data.listener.DisableExportSetsListenerInterface
import mega.privacy.android.data.listener.ExportSetsListenerInterface
import mega.privacy.android.data.listener.GetPreviewElementNodeListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.RemoveSetElementListenerInterface
import mega.privacy.android.data.mapper.PhotoMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumIdLink
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal typealias AlbumPhotosAddingProgressPool = MutableMap<AlbumId, MutableStateFlow<AlbumPhotosAddingProgress?>>
internal typealias AlbumPhotosRemovingProgressPool = MutableMap<AlbumId, MutableSharedFlow<AlbumPhotosRemovingProgress?>>

/**
 * Default [AlbumRepository] implementation
 */
@Singleton
internal class DefaultAlbumRepository @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val megaApiGateway: MegaApiGateway,
    private val userSetMapper: UserSetMapper,
    private val albumStringResourceGateway: AlbumStringResourceGateway,
    private val photoMapper: PhotoMapper,
    private val imageNodeMapper: ImageNodeMapper,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalRoomGateway: MegaLocalRoomGateway
) : AlbumRepository {
    private val userSets: MutableMap<Long, UserSet> = mutableMapOf()

    private val nodeSetsMap: MutableMap<NodeId, MutableSet<Long>> = mutableMapOf()

    private val userSetsFlow: MutableSharedFlow<List<UserSet>> = MutableSharedFlow(replay = 1)

    @VisibleForTesting
    internal val userSetsElementsFlow: MutableSharedFlow<List<Pair<UserSet, List<AlbumPhotoId>>>> =
        MutableSharedFlow(replay = 1)

    private val albumElements: MutableMap<AlbumId, List<AlbumPhotoId>> = mutableMapOf()

    private val albumPhotosAddingProgressPool: AlbumPhotosAddingProgressPool = mutableMapOf()

    private val albumPhotosRemovingProgressPool: AlbumPhotosRemovingProgressPool = mutableMapOf()

    @VisibleForTesting
    val publicNodesMap: MutableMap<NodeId, MegaNode> = mutableMapOf()

    @Volatile
    private var publicNodesDataMap: Map<NodeId, String> = mapOf()

    private var monitorNodeUpdatesJob: Job? = null

    @Volatile
    private var isMonitoringInitiated: Boolean = false

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = nodeRepository.monitorNodeUpdates()
            .onEach { nodeUpdate ->
                val targets = mutableMapOf<UserSet, List<AlbumPhotoId>>()

                for ((node, changes) in nodeUpdate.changes) {
                    if (node is FolderNode && changes.contains(NodeChanges.Sensitive)) {
                        targets.putAll(userSets.values.associateWith { listOf(AlbumPhotoId.default) })
                        break
                    } else {
                        val setIds = nodeSetsMap[node.id] ?: emptySet()
                        for (userSet in setIds.mapNotNull { userSets[it] }) {
                            val nodeIds = targets[userSet] ?: listOf()
                            targets[userSet] = nodeIds + AlbumPhotoId.default.copy(
                                nodeId = node.id,
                                albumId = AlbumId(userSet.id),
                            )
                        }
                    }
                }

                if (targets.isNotEmpty()) {
                    userSetsFlow.tryEmit(targets.keys.toList())
                    userSetsElementsFlow.tryEmit(targets.map { it.key to it.value })
                }
            }.launchIn(appScope)
    }

    override suspend fun createAlbum(name: String): UserSet = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.createSet(
                name,
                MegaSet.SET_TYPE_ALBUM,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            val newSet = request.megaSet
                            continuation.resumeWith(
                                Result.success(
                                    userSetMapper(
                                        newSet.id(),
                                        newSet.name(),
                                        newSet.type(),
                                        newSet.cover(),
                                        newSet.cts(),
                                        newSet.ts(),
                                        newSet.isExported,
                                    )
                                )
                            )
                        } else {
                            Timber.e("Error creating new album: ${error.errorString}")
                            continuation.failWithError(error, "createAlbum")
                        }
                    }
                )
            )
        }
    }

    override suspend fun getAllUserSets(): List<UserSet> {
        if (!isMonitoringInitiated) {
            isMonitoringInitiated = true
            monitorNodeUpdates()
        }

        return withContext(ioDispatcher) {
            val setList = megaApiGateway.getSets()
            userSets.clear()

            val userSets = (0 until setList.size())
                .filter { index ->
                    setList.get(index).type() == MegaSet.SET_TYPE_ALBUM
                }.map {
                    setList.get(it).toUserSet()
                }
                .associateBy { it.id }


            this@DefaultAlbumRepository.userSets.putAll(userSets)
            return@withContext userSets.values.toList()
        }
    }

    override suspend fun getUserSet(albumId: AlbumId): UserSet? =
        userSets[albumId.id] ?: withContext(ioDispatcher) {
            megaApiGateway.getSet(sid = albumId.id)?.toUserSet()?.also {
                userSets[it.id] = it
            }
        }

    override fun monitorUserSetsUpdate(): Flow<List<UserSet>> = merge(
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
            .mapNotNull { it.sets }
            .map { sets -> sets.map { it.toUserSet() } },
        userSetsFlow
            .filter { it.isNotEmpty() }
            .onEach { sets ->
                sets.forEach { albumElements.remove(AlbumId(it.id)) }
            },
    )

    override suspend fun getAlbumElementIDs(
        albumId: AlbumId,
        refresh: Boolean,
    ): List<AlbumPhotoId> {
        if (refresh) albumElements.remove(albumId)

        return albumElements[albumId] ?: withContext(ioDispatcher) {
            val elementList = megaApiGateway.getSetElements(sid = albumId.id)
            (0 until elementList.size()).mapNotNull { index ->
                val element = elementList[index]
                val sets = nodeSetsMap.getOrPut(NodeId(element.node())) { mutableSetOf() }
                sets.add(element.setId())

                if (nodeRepository.isNodeInRubbishBin(NodeId(element.node()))) null
                else element.toAlbumPhotoId()
            }.also { albumElements[albumId] = it }
        }
    }

    override fun monitorAlbumElementIds(albumId: AlbumId): Flow<List<AlbumPhotoId>> = merge(
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnSetElementsUpdate>()
            .mapNotNull { it.elements }
            .map { elements -> elements.filter { it.setId() == albumId.id } }
            .onEach(::checkSetsCoverRemoved)
            .map { it.map { it.toAlbumPhotoId() }.ifEmpty { listOf(AlbumPhotoId.default) } }
            .onEach { albumElements.remove(albumId) },
        userSetsElementsFlow
            .mapNotNull { sets -> sets.find { it.first.id == albumId.id } }
            .map { it.second.ifEmpty { listOf(AlbumPhotoId.default) } }
            .onEach { albumElements.remove(albumId) },
    )

    private fun checkSetsCoverRemoved(elements: List<MegaSetElement>) {
        val userSets = elements.mapNotNull { element ->
            val userSet = userSets[element.setId()]
            userSet.takeIf { element.id() == userSet?.cover }
        }

        if (userSets.isEmpty()) return
        userSetsFlow.tryEmit(userSets)
    }

    override suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>, isAsync: Boolean) {
        val progressFlow = getAlbumPhotosAddingProgressFlow(albumID)
        progressFlow.tryEmit(
            AlbumPhotosAddingProgress(
                isProgressing = true,
                totalAddedPhotos = 0,
                isAsync = isAsync,
            )
        )
        createAlbumItems(albumID, photoIDs)
    }

    private fun createAlbumItems(albumID: AlbumId, photoIDs: List<NodeId>) = appScope.launch {
        val listener = CreateSetElementListenerInterface(
            target = photoIDs.size,
            onCompletion = { success, _ ->
                val progressFlow = getAlbumPhotosAddingProgressFlow(albumID)
                progressFlow.tryEmit(
                    AlbumPhotosAddingProgress(
                        isProgressing = false,
                        totalAddedPhotos = success,
                        isAsync = progressFlow.value?.isAsync ?: false,
                    )
                )
            }
        )

        for (photoID in photoIDs) {
            megaApiGateway.createSetElement(albumID.id, photoID.longValue, listener)
        }
    }

    override suspend fun removePhotosFromAlbum(albumID: AlbumId, photoIDs: List<AlbumPhotoId>) =
        withContext(ioDispatcher) {
            val progressFlow = getAlbumPhotosRemovingProgressFlow(albumID)
            progressFlow.tryEmit(
                AlbumPhotosRemovingProgress(
                    isProgressing = true,
                    totalRemovedPhotos = 0,
                )
            )

            val listener = RemoveSetElementListenerInterface(
                target = photoIDs.size,
                onCompletion = { success, _ ->
                    progressFlow.tryEmit(
                        AlbumPhotosRemovingProgress(
                            isProgressing = false,
                            totalRemovedPhotos = success,
                        )
                    )
                }
            )

            for (photoID in photoIDs) {
                megaApiGateway.removeSetElement(albumID.id, photoID.id, listener)
            }
        }

    override suspend fun removeAlbums(albumIds: List<AlbumId>) = withContext(ioDispatcher) {
        albumIds.map { albumId ->
            async {
                suspendCoroutine { continuation ->
                    megaApiGateway.removeSet(
                        albumId.id,
                        OptionalMegaRequestListenerInterface(
                            onRequestFinish = { _, error ->
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(Unit))
                                } else {
                                    continuation.failWithError(error, "removeAlbums")
                                }
                            }
                        ),
                    )
                }
            }
        }.joinAll()
    }

    override fun observeAlbumPhotosAddingProgress(albumId: AlbumId): Flow<AlbumPhotosAddingProgress?> =
        getAlbumPhotosAddingProgressFlow(albumId)

    override suspend fun updateAlbumPhotosAddingProgressCompleted(albumId: AlbumId) {
        val progressFlow = getAlbumPhotosAddingProgressFlow(albumId)
        progressFlow.tryEmit(null)
    }

    override fun observeAlbumPhotosRemovingProgress(albumId: AlbumId): Flow<AlbumPhotosRemovingProgress?> =
        getAlbumPhotosRemovingProgressFlow(albumId).distinctUntilChanged()

    override suspend fun updateAlbumPhotosRemovingProgressCompleted(albumId: AlbumId) {
        val progressFlow = getAlbumPhotosRemovingProgressFlow(albumId)
        progressFlow.tryEmit(null)
    }

    override suspend fun updateAlbumName(
        albumId: AlbumId,
        newName: String,
    ): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("updateAlbumName") {
                return@getRequestListener it.text
            }
            megaApiGateway.updateSetName(
                sid = albumId.id,
                name = newName,
                listener = listener
            )
        }
    }

    override suspend fun getProscribedAlbumTitles(): List<String> =
        albumStringResourceGateway.getSystemAlbumNames() + albumStringResourceGateway.getProscribedStrings()

    override suspend fun updateAlbumCover(albumId: AlbumId, elementId: NodeId) =
        withContext(ioDispatcher) {
            megaApiGateway.putSetCover(
                sid = albumId.id,
                eid = elementId.longValue,
            )
        }

    override suspend fun exportAlbums(albumIds: List<AlbumId>): List<AlbumIdLink> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = ExportSetsListenerInterface(
                    totalSets = albumIds.size,
                    onCompletion = { setLinks ->
                        val albumIdsLinks = setLinks.map { (sid, link) ->
                            AlbumId(sid) to AlbumLink(link)
                        }
                        continuation.resumeWith(Result.success(albumIdsLinks))
                    },
                )

                if (albumIds.isNotEmpty()) {
                    for (albumId in albumIds) {
                        megaApiGateway.exportSet(
                            sid = albumId.id,
                            listener = listener,
                        )
                    }
                } else {
                    continuation.resumeWith(Result.success(listOf()))
                }

            }
        }

    override suspend fun disableExportAlbums(albumIds: List<AlbumId>) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = DisableExportSetsListenerInterface(
                totalSets = albumIds.size,
                onCompletion = { success, _ ->
                    continuation.resumeWith(Result.success(success))
                }
            )

            if (albumIds.isNotEmpty()) {
                for (albumId in albumIds) {
                    megaApiGateway.disableExportSet(
                        sid = albumId.id,
                        listener = listener,
                    )
                }
            } else {
                continuation.resumeWith(Result.success(0))
            }

        }
    }

    override suspend fun fetchPublicAlbum(albumLink: AlbumLink) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        launch {
                            val userSet = request.megaSet?.toUserSet()
                            val albumPhotoIds = request.megaSetElementList?.let { elementList ->
                                (0 until elementList.size()).map { index ->
                                    elementList.get(index).toAlbumPhotoId()
                                }
                            }.orEmpty()

                            if (userSet != null && userSet.id != -1L) {
                                continuation.resume(userSet to albumPhotoIds)
                            } else {
                                continuation.failWithError(error, "fetchPublicAlbum")
                            }
                        }
                    } else {
                        continuation.failWithError(error, "fetchPublicAlbum")
                    }
                },
            )

            publicNodesMap.clear()

            megaApiGateway.stopPublicSetPreview()
            megaApiGateway.fetchPublicSet(
                publicSetLink = albumLink.link,
                listener = listener,
            )

        }
    }

    override suspend fun getPublicPhotos(albumPhotoIds: List<AlbumPhotoId>): List<Photo> {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val nodeAlbumPhotoIdMap = albumPhotoIds.associateBy(
                    keySelector = { it.nodeId.longValue },
                    valueTransform = { it },
                )

                val listener = GetPreviewElementNodeListenerInterface(
                    nodeAlbumPhotoIdMap = nodeAlbumPhotoIdMap,
                    onCompletion = { nodeAlbumPhotoIdPairs ->
                        launch {
                            val photos = nodeAlbumPhotoIdPairs.mapNotNull { (node, albumPhotoId) ->
                                publicNodesMap[NodeId(node.handle)] = node
                                photoMapper(node, albumPhotoId)
                            }
                            continuation.resume(photos)
                        }
                    }
                )

                if (albumPhotoIds.isNotEmpty()) {
                    for (albumPhotoId in albumPhotoIds) {
                        megaApiGateway.getPreviewElementNode(
                            eid = albumPhotoId.id,
                            listener = listener,
                        )
                    }
                } else {
                    continuation.resume(listOf())
                }

            }
        }
    }

    override suspend fun downloadPublicThumbnail(photo: Photo, callback: (Boolean) -> Unit) {
        withContext(ioDispatcher) {
            val node = publicNodesMap[NodeId(photo.id)]
            val thumbnailFilePath = photo.thumbnailFilePath

            if (thumbnailFilePath.isNullOrBlank()) {
                callback(false)
            } else if (File(thumbnailFilePath).exists()) {
                callback(true)
            } else if (node == null) {
                callback(false)
            } else {
                megaApiGateway.getThumbnail(
                    node = node,
                    thumbnailFilePath = thumbnailFilePath,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            callback(error.errorCode == MegaError.API_OK)
                        },
                    ),
                )
            }
        }
    }

    override suspend fun downloadPublicPreview(photo: Photo, callback: (Boolean) -> Unit) {
        withContext(ioDispatcher) {
            val node = publicNodesMap[NodeId(photo.id)]
            val previewFilePath = photo.previewFilePath

            if (previewFilePath.isNullOrBlank()) {
                callback(false)
            } else if (File(previewFilePath).exists()) {
                callback(true)
            } else if (node == null) {
                callback(false)
            } else {
                megaApiGateway.getPreview(
                    node = node,
                    previewFilePath = previewFilePath,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            callback(error.errorCode == MegaError.API_OK)
                        },
                    ),
                )
            }
        }
    }

    override fun getPublicAlbumNodesData(): Map<NodeId, String> =
        publicNodesMap.mapValues { it.value.serialize() }

    override suspend fun addBulkPhotosToAlbum(
        albumId: AlbumId,
        photoIds: List<NodeId>,
    ): Int = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val success = request.megaSetElementList?.size()?.toInt() ?: 0
                        continuation.resume(success)
                    } else {
                        continuation.failWithError(error, "addBulkPhotosToAlbum")
                    }
                },
            )

            if (photoIds.isNotEmpty()) {
                val handleList = MegaHandleList.createInstance().apply {
                    for (photoId in photoIds) {
                        addMegaHandle(photoId.longValue)
                    }
                }
                megaApiGateway.createSetElements(albumId.id, handleList, null, listener)
            } else {
                continuation.resume(0)
            }

        }
    }

    override suspend fun saveAlbumToFolder(
        folderName: String,
        photoIds: List<NodeId>,
        targetParentFolderNodeId: NodeId,
    ): List<NodeId> = withContext(ioDispatcher) {
        val getFinalFolderName: suspend (MegaNode) -> String = { folder ->
            var finalFolderName = folderName
            while (megaApiGateway.getChildNode(folder, finalFolderName) != null) {
                finalFolderName = "$finalFolderName (1)"
            }
            finalFolderName
        }

        val createAlbumFolder: suspend (MegaNode, String) -> MegaNode = { folder, folderName ->
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            launch {
                                val node = megaApiGateway.getMegaNodeByHandle(request.nodeHandle)
                                requireNotNull(node) { "Album folder failed to create" }

                                continuation.resume(node)
                            }
                        } else {
                            continuation.failWithError(error, "saveAlbumToFolder")
                        }
                    },
                )

                megaApiGateway.createFolder(
                    name = folderName,
                    parent = folder,
                    listener = listener,
                )

            }
        }

        val savePhotoToFolder: suspend (MegaNode) -> List<NodeId> = { folder ->
            suspendCancellableCoroutine { continuation ->
                val nodes = photoIds.mapNotNull { nodeId ->
                    publicNodesMap[nodeId]
                }

                val listener = CopyPreviewNodeListenerInterface(
                    nodes = nodes,
                    onCompletion = { nodeIds ->
                        continuation.resume(nodeIds)
                    },
                )

                if (nodes.isNotEmpty()) {
                    for (node in nodes) {
                        megaApiGateway.copyNode(
                            nodeToCopy = node,
                            newNodeParent = folder,
                            newNodeName = null,
                            listener = listener,
                        )
                    }
                } else {
                    continuation.resume(listOf())
                }

            }
        }

        val parentNode = megaApiGateway.getMegaNodeByHandle(
            nodeHandle = targetParentFolderNodeId.longValue
        )
        requireNotNull(parentNode) { "Target parent node not found" }

        val finalFolderName = getFinalFolderName(parentNode)
        val nodeParent = createAlbumFolder(parentNode, finalFolderName)
        val nodeIds = savePhotoToFolder(nodeParent)

        nodeIds
    }

    override suspend fun getPublicPhotoImageNode(nodeId: NodeId): ImageNode {
        val offline = megaLocalRoomGateway.getOfflineInformation(nodeId.longValue)
        return withContext(ioDispatcher) {
            val node = publicNodesMap[nodeId] ?: throw IllegalArgumentException("Node not found")
            imageNodeMapper(
                megaNode = node,
                offline = offline,
                numVersion = megaApiGateway::getNumVersions
            )
        }
    }

    override suspend fun getPublicImageNodes(): List<ImageNode> = withContext(ioDispatcher) {
        publicNodesMap.values.map { megaNode ->
            val offline = megaLocalRoomGateway.getOfflineInformation(nodeHandle = megaNode.handle)
            imageNodeMapper(
                megaNode = megaNode,
                offline = offline,
                requireSerializedData = true,
                numVersion = megaApiGateway::getNumVersions
            )
        }
    }

    override suspend fun getPublicPhoto(nodeId: NodeId): Photo? {
        return publicNodesMap[nodeId]?.let {
            photoMapper(
                node = it,
                albumPhotoId = null,
            )
        }
    }

    override suspend fun getAlbumPhotoFileUrlByNodeHandle(nodeId: NodeId): String? =
        publicNodesMap[nodeId]?.let { node ->
            megaApiGateway.httpServerGetLocalLink(node)
        }

    override suspend fun isAlbumLinkValid(albumLink: AlbumLink): Boolean =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        continuation.resume(error.errorCode == MegaError.API_OK && request.megaSet?.id() != -1L)
                    },
                )

                megaApiGateway.stopPublicSetPreview()
                megaApiGateway.fetchPublicSet(
                    publicSetLink = albumLink.link,
                    listener = listener,
                )

            }
        }

    override fun clearAlbumCache(albumId: AlbumId) {
        userSets.remove(albumId.id)
    }

    override fun clearCache() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = null

        isMonitoringInitiated = false

        userSets.clear()
        nodeSetsMap.clear()
        albumElements.clear()
        publicNodesMap.clear()
        albumPhotosAddingProgressPool.clear()
        albumPhotosRemovingProgressPool.clear()

        userSetsFlow.tryEmit(listOf())
        userSetsElementsFlow.tryEmit(listOf())
    }

    private fun getAlbumPhotosAddingProgressFlow(albumId: AlbumId): MutableStateFlow<AlbumPhotosAddingProgress?> =
        albumPhotosAddingProgressPool.getOrPut(albumId) { MutableStateFlow(null) }

    private fun getAlbumPhotosRemovingProgressFlow(albumId: AlbumId): MutableSharedFlow<AlbumPhotosRemovingProgress?> =
        albumPhotosRemovingProgressPool.getOrPut(albumId) { MutableSharedFlow(replay = 1) }

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

    private fun MegaSetElement.toAlbumPhotoId(): AlbumPhotoId = AlbumPhotoId(
        id = id(),
        nodeId = NodeId(node()),
        albumId = AlbumId(setId()),
    )
}
