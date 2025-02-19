package mega.privacy.android.app.presentation.videoplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.videoplayer.mapper.VideoPlayerItemMapper
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerUiState
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_SIZE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStopUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for video player.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @VideoPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val videoPlayerItemMapper: VideoPlayerItemMapper,
    private val getVideoNodeByHandleUseCase: GetVideoNodeByHandleUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiFolderHttpServerStopUseCase: MegaApiFolderHttpServerStopUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerStop: MegaApiHttpServerStopUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    private val getOfflineNodeInformationByIdUseCase: GetOfflineNodeInformationByIdUseCase,
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    internal val uiState = _uiState.asStateFlow()

    private var needStopStreamingServer = false
    private var playerRetry = 0

    init {
        setupTransferListener()
    }

    /**
     * Setup transfer listener
     */
    private fun setupTransferListener() =
        viewModelScope.launch {
            monitorTransferEventsUseCase()
                .catch {
                    Timber.e(it)
                }.collect { event ->
                    if (event is TransferEvent.TransferTemporaryErrorEvent) {
                        val error = event.error
                        val transfer = event.transfer
                        if (transfer.nodeHandle == _uiState.value.currentPlayingHandle
                            && ((error is QuotaExceededMegaException
                                    && !transfer.isForeignOverQuota
                                    && error.value != 0L)
                                    || error is BlockedMegaException)
                        ) {
                            _uiState.update { it.copy(error = error) }
                        }
                    }
                }
        }

    internal fun initVideoPlaybackSources(intent: Intent?) {
        viewModelScope.launch {
            buildPlaybackSources(intent)
        }
    }

    private suspend fun buildPlaybackSources(intent: Intent?) {
        if (intent == null || !validateIntent(intent)) return

        val launchSource = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val uri = intent.data
        val currentPlayingHandle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
        val currentPlayingFileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME).orEmpty()
        needStopStreamingServer =
            intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        playerRetry = 0

        val currentPlayingUri = getCurrentPlayingUri(uri, launchSource, currentPlayingHandle)
        if (currentPlayingUri == null) {
            logInvalidParam("folder link uri is null")
            return
        }

        val currentPlayingMediaItem = MediaItem.Builder()
            .setUri(currentPlayingUri)
            .setMediaId(currentPlayingHandle.toString())
            .build()

        updateStateWithMediaItem(currentPlayingMediaItem, currentPlayingFileName)

        if (!intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)) {
            setPlayingItem(currentPlayingHandle, currentPlayingFileName, launchSource)
            return
        }

        if (launchSource != OFFLINE_ADAPTER && launchSource != ZIP_ADAPTER) {
            needStopStreamingServer = needStopStreamingServer || setupStreamingServer(launchSource)
        }

        withContext(ioDispatcher) {
            handlePlaybackSourceByLaunchSource(intent, launchSource, currentPlayingHandle)
        }
    }

    private fun validateIntent(intent: Intent): Boolean {
        val isValid = when {
            !intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true) -> {
                logInvalidParam("Rebuild playlist param is false")
                false
            }

            intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE) == INVALID_VALUE -> {
                logInvalidParam("Launch source is invalid")
                false
            }

            intent.data == null -> {
                logInvalidParam("URI is null")
                false
            }

            intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE) == INVALID_HANDLE -> {
                logInvalidParam("The first playing video handle is invalid")
                false
            }

            intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) == null -> {
                logInvalidParam("The first playing video file name is null")
                false
            }

            else -> true
        }
        return isValid
    }

    private fun logInvalidParam(message: String) {
        Timber.d("Build playback sources failed: $message")
        _uiState.update { it.copy(isRetry = false) }
    }

    private suspend fun setupStreamingServer(launchSource: Int): Boolean {
        val isServerRunning = if (launchSource == FOLDER_LINK_ADAPTER && !hasCredentialsUseCase()) {
            megaApiFolderHttpServerIsRunningUseCase()
        } else {
            megaApiHttpServerIsRunningUseCase()
        }

        if (isServerRunning != 0) return false

        if (launchSource == FOLDER_LINK_ADAPTER && !hasCredentialsUseCase()) {
            megaApiFolderHttpServerStartUseCase()
        } else {
            megaApiHttpServerStartUseCase()
        }

        return true
    }

    private suspend fun getCurrentPlayingUri(uri: Uri?, launchSource: Int, handle: Long) =
        when (launchSource) {
            FOLDER_LINK_ADAPTER -> {
                val url = if (hasCredentialsUseCase()) {
                    getLocalFolderLinkFromMegaApiUseCase(handle)
                } else {
                    getLocalFolderLinkFromMegaApiFolderUseCase(handle)
                }
                url?.let { Uri.parse(it) }
            }

            else -> uri
        }

    private fun updateStateWithMediaItem(mediaItem: MediaItem, fileName: String) {
        MediaPlaySources(
            mediaItems = listOf(mediaItem),
            newIndexForCurrentItem = INVALID_VALUE,
            nameToDisplay = fileName
        ).also { sources ->
            _uiState.update { it.copy(mediaPlaySources = sources) }
            buildPlaybackSourcesForPlayer(sources)
        }
    }

    private fun buildPlaybackSourcesForPlayer(mediaPlaySources: MediaPlaySources) {
        Timber.d("Playback sources: ${mediaPlaySources.mediaItems.size} items")
        with(mediaPlayerGateway) {
            buildPlaySources(mediaPlaySources)
            setPlayWhenReady(_uiState.value.isPaused && mediaPlaySources.isRestartPlaying)
            playerPrepare()
        }
        mediaPlaySources.nameToDisplay?.let { name ->
            _uiState.update { it.copy(metadata = Metadata(null, null, null, nodeName = name)) }
        }
    }

    private suspend fun setPlayingItem(handle: Long, fileName: String?, source: Int) {
        val node = getVideoNodeByHandleUseCase(handle)
        val thumbnail = getThumbnailForNode(node, handle, source)
        val playingItem = videoPlayerItemMapper(
            nodeHandle = handle,
            nodeName = fileName.orEmpty(),
            thumbnail = thumbnail,
            type = MediaQueueItemType.Playing,
            size = node?.size ?: INVALID_SIZE,
            duration = node?.duration ?: 0.seconds,
        )

        _uiState.update { it.copy(items = listOf(playingItem)) }
    }

    private suspend fun getThumbnailForNode(
        node: TypedVideoNode?,
        handle: Long,
        source: Int,
    ) = when {
        node == null -> null
        source == OFFLINE_ADAPTER -> getThumbnailUseCase(handle)
        else -> runCatching {
            File(
                ThumbnailUtils.getThumbFolder(context),
                node.base64Id.plus(FileUtil.JPG_EXTENSION)
            )
        }.getOrNull()
    }

    private suspend fun handlePlaybackSourceByLaunchSource(
        intent: Intent,
        launchSource: Int,
        playingHandle: Long,
    ) {
        when (launchSource) {
            OFFLINE_ADAPTER -> handleOfflineSource(intent, playingHandle)
            ZIP_ADAPTER -> handleZipSource(intent, playingHandle)
            else -> handleGeneralSource()
        }
    }

    private suspend fun handleOfflineSource(intent: Intent, playingHandle: Long) {
        val parentId = intent.getIntExtra(INTENT_EXTRA_KEY_PARENT_ID, -1)
        val title = if (parentId == -1) {
            context.getString(R.string.section_saved_for_offline_new)
        } else {
            runCatching {
                getOfflineNodeInformationByIdUseCase(parentId)
            }.getOrNull()?.name.orEmpty()
        }
        buildPlaybackSourcesByOfflineNodes(title, parentId, playingHandle)
    }

    private suspend fun buildPlaybackSourcesByOfflineNodes(
        title: String,
        parentId: Int,
        firstPlayHandle: Long,
    ) {
        runCatching {
            getOfflineNodesByParentIdUseCase(parentId)
        }.onSuccess { list ->
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayingIndex = -1
            val videoPlayerItems = list.filter {
                it.fileTypeInfo is VideoFileTypeInfo && it.fileTypeInfo?.isSupported == true
            }.mapIndexed { index, item ->
                if (item.handle.toLong() == firstPlayHandle) currentPlayingIndex = index

                runCatching { Uri.parse(item.absolutePath) }.getOrNull()?.let {
                    mediaItems.add(
                        MediaItem.Builder()
                            .setUri(it)
                            .setMediaId(item.handle)
                            .build()
                    )
                }

                val thumbnailFile = runCatching {
                    item.thumbnail?.let { File(it) }
                }.getOrNull()

                videoPlayerItemMapper(
                    nodeHandle = item.handle.toLong(),
                    nodeName = item.name,
                    thumbnail = thumbnailFile,
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = item.totalSize,
                    duration = (item.fileTypeInfo as? VideoFileTypeInfo)?.duration ?: 0.seconds,
                )
            }

            updatePlaybackSources(
                videoPlayerItems = videoPlayerItems,
                mediaItems = mediaItems,
                title = title,
                currentPlayingIndex = currentPlayingIndex,
                firstPlayHandle = firstPlayHandle
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun handleZipSource(intent: Intent, playingHandle: Long) {
        intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY)?.let { zipPath ->
            buildPlaybackSourcesByFiles(zipPath, playingHandle)
        }
    }

    private fun getMediaQueueItemType(currentIndex: Int, playingIndex: Int) =
        when {
            currentIndex == playingIndex -> MediaQueueItemType.Playing
            playingIndex == -1 || currentIndex < playingIndex -> MediaQueueItemType.Previous
            else -> MediaQueueItemType.Next
        }

    private fun updatePlaybackSources(
        videoPlayerItems: List<VideoPlayerItem>,
        mediaItems: List<MediaItem>,
        title: String,
        currentPlayingIndex: Int,
        firstPlayHandle: Long,
    ) {
        val mediaPlaySources = MediaPlaySources(
            mediaItems = mediaItems,
            newIndexForCurrentItem = currentPlayingIndex,
            nameToDisplay = null
        )

        _uiState.update {
            it.copy(
                items = videoPlayerItems,
                mediaPlaySources = mediaPlaySources,
                playQueueTitle = title,
                currentPlayingIndex = currentPlayingIndex,
                currentPlayingHandle = firstPlayHandle
            )
        }
        buildPlaybackSourcesForPlayer(mediaPlaySources)
    }

    private suspend fun buildPlaybackSourcesByFiles(zipPath: String, firstPlayHandle: Long) {
        runCatching {
            val (title, files) = getFileByPathUseCase(zipPath)?.parentFile.let { parentFile ->
                parentFile?.name.orEmpty() to parentFile?.listFiles().orEmpty()
            }
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayingIndex = -1
            val videoPlayerItems = files.filter {
                it.isFile && getFileTypeInfoByNameUseCase(it.name) is VideoFileTypeInfo
            }.mapIndexed { index, file ->
                if (file.name.hashCode().toLong() == firstPlayHandle) currentPlayingIndex = index

                mediaItems.add(
                    MediaItem.Builder()
                        .setUri(FileUtil.getUriForFile(context, file))
                        .setMediaId(file.name.hashCode().toString())
                        .build()
                )

                videoPlayerItemMapper(
                    nodeHandle = file.name.hashCode().toLong(),
                    nodeName = file.name,
                    thumbnail = null,
                    type = getMediaQueueItemType(index, currentPlayingIndex),
                    size = file.length(),
                    duration = 0.seconds,
                )
            }

            updatePlaybackSources(
                videoPlayerItems = videoPlayerItems,
                mediaItems = mediaItems,
                title = title,
                currentPlayingIndex = currentPlayingIndex,
                firstPlayHandle = firstPlayHandle
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    private fun handleGeneralSource() {
        //The function will be implemented in ticket CC-8417
    }

    /**
     * onCleared
     */
    override fun onCleared() {
        super.onCleared()
        clear()
    }

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    private fun clear() {
        applicationScope.launch {
            if (needStopStreamingServer) {
                megaApiHttpServerStop()
                megaApiFolderHttpServerStopUseCase()
            }
        }
    }
}