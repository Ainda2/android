package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.ViewerNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroup
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransferState
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest
import java.io.File
import kotlin.time.Duration

/**
 * Transfer repository of Domain Module
 */
interface TransferRepository {
    /**
     * Monitor transfer events
     *
     * @return flow of transfer event
     */
    fun monitorTransferEvents(): Flow<TransferEvent>

    /**
     * Gets the number of pending download transfers that are not background transfers.
     *
     * @return Number of pending downloads.
     */
    suspend fun getNumPendingDownloadsNonBackground(): Int

    /**
     * Gets the number of pending upload transfers.
     *
     * @return Number of pending uploads.
     */
    suspend fun getNumPendingUploads(): Int

    /**
     * @return the number of pending Camera Uploads
     */
    suspend fun getNumPendingCameraUploads(): Int

    /**
     * Gets number of pending transfers.
     *
     * @return Number of pending transfers.
     */
    suspend fun getNumPendingTransfers(): Int

    /**
     * Checks if the completed transfers list is empty.
     *
     * @return True if the completed transfers is empty, false otherwise.
     */
    suspend fun isCompletedTransfersEmpty(): Boolean

    /**
     * Gets the number of pending and paused uploads.
     *
     * @return Number of pending and paused uploads.
     */
    suspend fun getNumPendingPausedUploads(): Int

    /**
     * @return the number of pending and paused Camera Uploads
     */
    suspend fun getNumPendingPausedCameraUploads(): Int

    /**
     * Gets the number of pending, non-background and paused downloads.
     *
     * @return Number of pending, non-background and paused downloads.
     */
    suspend fun getNumPendingNonBackgroundPausedDownloads(): Int

    /**
     * Cancel Transfer by Tag
     * @param transferTag   Tag that identifies the transfer
     */
    suspend fun cancelTransferByTag(transferTag: Int)

    /**
     * Monitor the offline availability of the file
     */
    fun monitorOfflineFileAvailability(): Flow<Long>

    /**
     * Broadcast the offline availability of the file
     * @param nodeHandle the node handle
     */
    suspend fun broadcastOfflineFileAvailability(nodeHandle: Long)

    /**
     * Monitor transfer over quota
     */
    fun monitorTransferOverQuota(): Flow<Boolean>

    /**
     * Broadcast transfer over quota
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     *
     */
    suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Monitor storage over quota
     */
    fun monitorStorageOverQuota(): Flow<Boolean>

    /**
     * Broadcast storage over quota
     *
     */
    suspend fun broadcastStorageOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Cancels all transfers, uploads and downloads.
     */
    suspend fun cancelTransfers()

    /**
     * Checks if exist ongoing transfers.
     */
    suspend fun ongoingTransfersExist(): Boolean

    /**
     * Move transfer to first by tag
     *
     * @param transferTag
     */
    suspend fun moveTransferToFirstByTag(transferTag: Int)

    /**
     * Move transfer to last by tag
     *
     * @param transferTag
     */
    suspend fun moveTransferToLastByTag(transferTag: Int)

    /**
     * Move transfer before by tag
     *
     * @param transferTag
     * @param prevTransferTag
     */
    suspend fun moveTransferBeforeByTag(transferTag: Int, prevTransferTag: Int)

    /**
     * Get transfer by tag
     *
     * @param transferTag
     */
    suspend fun getTransferByTag(transferTag: Int): Transfer?

    /**
     * Monitors paused transfers.
     */
    fun monitorPausedTransfers(): StateFlow<Boolean>

    /**
     * Get in progress transfers
     *
     */
    suspend fun getInProgressTransfers(): List<Transfer>

    /**
     * Monitor completed transfers
     *
     * @return a flow of [CompletedTransferState]
     */
    fun monitorCompletedTransfer(): Flow<CompletedTransferState>

    /**
     * Monitors list of completed transfers
     *
     * @param size the limit size of the list. If null, the limit does not apply
     */
    fun monitorCompletedTransfers(size: Int? = null): Flow<List<CompletedTransfer>>

    /**
     * Add a list of completed transfer to local storage
     *
     * @param finishEventsAndPaths
     */
    suspend fun addCompletedTransfers(
        finishEventsAndPaths: Map<TransferEvent.TransferFinishEvent, String?>,
    )

    /**
     * Add failed completed transfers from a failed pending transfer
     *
     * @param pendingTransfer
     * @param sizeInBytes Size of the node related to the pending transfer
     * @param error The error that caused this pending transfer to fail
     */
    suspend fun addCompletedTransferFromFailedPendingTransfer(
        pendingTransfer: PendingTransfer,
        sizeInBytes: Long,
        error: Throwable,
    )

    /**
     * Add failed completed transfers from a failed pending transfer list
     *
     * @param pendingTransfer
     * @param error The error that caused this pending transfer to fail
     */
    suspend fun addCompletedTransferFromFailedPendingTransfers(
        pendingTransfer: List<PendingTransfer>,
        error: Throwable,
    )

    /**
     * Add completed transfers if not exist
     *
     * @param transfers
     */
    suspend fun addCompletedTransfersIfNotExist(transfers: List<Transfer>)

    /**
     * Delete oldest completed transfers
     */
    suspend fun deleteOldestCompletedTransfers()

    /**
     * Delete completed transfers which path contains the given path
     * @param path to search for
     */
    suspend fun deleteCompletedTransfersByPath(path: String)

    /**
     * Starts the download worker to monitor the download transfers as a foreground service
     */
    suspend fun startDownloadWorker()

    /**
     * Starts the chat uploads worker to monitor the chat uploads transfers as a foreground service
     */
    suspend fun startChatUploadsWorker()

    /**
     * Start downloading a node to desired destination and returns a flow to expose download progress
     *
     * @param node              The node we want to download, it can be a folder
     * @param localPath         Full path to the destination folder of [node]. If this path does not exist it will try to create it.
     * @param appData           Custom app data to save in the MegaTransfer object.
     * @param shouldStartFirst  Puts the transfer on top of the download queue.
     */
    fun startDownload(
        node: TypedNode,
        localPath: String,
        appData: List<TransferAppData>?,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent>

    /**
     * Upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNodeId The parent node id for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param modificationTime The custom modification time for the file or folder, denoted in
     * seconds since the epoch. Null if no custom modification time is needed.
     * @param appData The custom app data to save, which can be nullable
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * @param shouldStartFirst Whether the file or folder should be placed on top of the upload
     * queue or not
     *
     * @return a Flow of [TransferEvent]
     */
    fun startUpload(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long?,
        appData: List<TransferAppData>?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent>

    /**
     * Upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNodeId The parent node id for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param appData The custom app data to save chat upload related information, at least one should be a
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * queue or not
     *
     * @return a Flow of [TransferEvent]
     */
    fun startUploadForChat(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        appData: List<TransferAppData>,
        isSourceTemporary: Boolean,
    ): Flow<TransferEvent>

    /**
     * Get active transfer by tag
     */
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransfer?

    /**
     * Get active transfers by type
     * @return a flow of all active transfers list
     */
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransfer>>

    /**
     * Get current active transfers by type
     * @return A list of all active transfers of this type
     */
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransfer>

    /**
     * Get current active transfers
     * @return all active transfers list
     */
    suspend fun getCurrentActiveTransfers(): List<ActiveTransfer>

    /**
     * Insert a new active transfer or replace it if there's already an active transfer with the same tag
     */
    suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer)

    /**
     * Insert (or replace  if there's already an active transfer with the same tag) a list of active transfers
     */
    suspend fun insertOrUpdateActiveTransfers(activeTransfers: List<ActiveTransfer>)

    /**
     * Set or update the transferred bytes counter of this transfer
     */
    suspend fun updateTransferredBytes(transfers: List<Transfer>)

    /**
     * Delete all active transfer of this type
     */
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    /**
     * Delete all active transfer
     */
    suspend fun deleteAllActiveTransfers()

    /**
     * Set an active transfer as finished cancelled by its tag
     */
    suspend fun setActiveTransferAsCancelledByTag(tags: List<Int>)

    /**
     * Get active transfer totals by type
     * @return a flow of active transfer totals
     */
    fun getActiveTransferTotalsByType(transferType: TransferType): Flow<ActiveTransferTotals>

    /**
     * Get the current active transfer totals by type
     * @return the current active transfer totals
     */
    suspend fun getCurrentActiveTransferTotalsByType(transferType: TransferType): ActiveTransferTotals

    /**
     * Get current upload speed.
     *
     * @return Current upload speed.
     */
    suspend fun getCurrentUploadSpeed(): Int

    /**
     * Pause transfers
     *
     * @param isPause
     * @return boolean is pause or resume
     */
    suspend fun pauseTransfers(isPause: Boolean): Boolean

    /**
     * Sets the pause transfers queue as false, which should be the default value after login.
     */
    suspend fun resetPauseTransfers()

    /**
     * Delete all completed transfers
     */
    suspend fun deleteAllCompletedTransfers()

    /**
     * Get failed or cancel transfers
     *
     * @return the failed or cancelled transfer list
     */
    suspend fun getFailedOrCanceledTransfers(): List<CompletedTransfer>

    /**
     * Delete failed or canceled transfers
     *
     * @return the failed or cancelled transfer list was deleted
     */
    suspend fun deleteFailedOrCanceledTransfers(): List<CompletedTransfer>

    /**
     * Delete completed transfer
     *
     * @param transfer
     */
    suspend fun deleteCompletedTransfer(transfer: CompletedTransfer, isRemoveCache: Boolean)

    /**
     * Pause transfer by tag
     *
     * @param transferTag
     * @param isPause
     */
    suspend fun pauseTransferByTag(transferTag: Int, isPause: Boolean): Boolean

    /**
     * Get completed transfer by id
     *
     * @param id id of completed transfer
     */
    suspend fun getCompletedTransferById(id: Int): CompletedTransfer?

    /**
     * Get current download speed.
     *
     * @return Current download speed.
     */
    suspend fun getCurrentDownloadSpeed(): Int

    /**
     * Get or create a folder for transfers in the cache of SD Card if any
     *
     * @return the File corresponding to the folder in cache in the SD
     *         Return null if the folder cannot be created or there's no SD card
     */
    suspend fun getOrCreateSDCardTransfersCacheFolder(): File?

    /**
     * @return a flow that emits true if DownloadsWorker is enqueued. false otherwise
     */
    fun isDownloadsWorkerEnqueuedFlow(): Flow<Boolean>

    /**
     * @return a flow that emits true if ChatUploadsWorker is enqueued. false otherwise
     */
    fun isChatUploadsWorkerEnqueuedFlow(): Flow<Boolean>

    /**
     * @return true if the user can choose download's destination. False means downloads will be saved to default destination. See [settingsRepository.setDefaultStorageDownloadLocation()]
     */
    suspend fun allowUserToSetDownloadDestination(): Boolean

    /**
     * Monitors ask resume transfers.
     */
    fun monitorAskedResumeTransfers(): StateFlow<Boolean>

    /**
     * Set ask resume transfers.
     */
    suspend fun setAskedResumeTransfers()

    /**
     * Starts the uploads worker to monitor the uploads transfers as a foreground service
     */
    suspend fun startUploadsWorker()

    /**
     * @return a flow that emits true if UploadsWorker is enqueued. false otherwise
     */
    fun isUploadsWorkerEnqueuedFlow(): Flow<Boolean>


    /**
     * Updates or adds a new transfer to the in progress transfers list.
     */
    suspend fun updateInProgressTransfer(transfer: Transfer)

    /**
     * Updates or adds a list of transfers to the in progress transfers list.
     */
    suspend fun updateInProgressTransfers(transfers: List<Transfer>)

    /**
     * Monitor in progress transfers flow.
     */
    fun monitorInProgressTransfers(): Flow<Map<Int, InProgressTransfer>>

    /**
     * Remove in progress transfer by tag.
     */
    suspend fun removeInProgressTransfer(tag: Int)

    /**
     * Remove a list of in progress transfers by tag.
     */
    suspend fun removeInProgressTransfers(tags: Set<Int>)

    /**
     * Gets a flow for pending transfers by type.
     */
    fun monitorPendingTransfersByType(transferType: TransferType): Flow<List<PendingTransfer>>

    /**
     * Gets pending transfers by type.
     */
    suspend fun getPendingTransfersByType(transferType: TransferType): List<PendingTransfer>

    /**
     * Gets pending transfers by state.
     */
    suspend fun getPendingTransfersByState(pendingTransferState: PendingTransferState): List<PendingTransfer>

    /**
     * Gets a flow for pending transfers by type and state.
     */
    fun monitorPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): Flow<List<PendingTransfer>>

    /**
     * Gets pending transfers by type and state.
     */
    suspend fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): List<PendingTransfer>

    /**
     * Inserts a list of pending transfers.
     */
    suspend fun insertPendingTransfers(pendingTransfer: List<InsertPendingTransferRequest>)

    /**
     * Updates a list of pending transfers.
     */
    suspend fun updatePendingTransfers(
        updatePendingTransferRequests: List<UpdatePendingTransferRequest>,
    )

    /**
     * Updates a pending transfer.
     */
    suspend fun updatePendingTransfer(
        updatePendingTransferRequest: UpdatePendingTransferRequest,
    )

    /**
     * Gets a pending transfer by tag.
     */
    suspend fun getPendingTransfersByTag(tag: Int): PendingTransfer?

    /**
     * Deletes a pending transfer by tag.
     */
    suspend fun deletePendingTransferByTag(tag: Int)

    /**
     * Deletes all pending transfers.
     */
    suspend fun deleteAllPendingTransfers()

    /**
     * Sets whether the user has denied the file access permission request
     */
    suspend fun setRequestFilesPermissionDenied()

    /**
     * Monitors whether the user has denied the file access permission request
     */
    fun monitorRequestFilesPermissionDenied(): Flow<Boolean>

    /**
     * Clear all preferences
     */
    suspend fun clearPreferences()

    /**
     * Get the time during which transfers will be stopped due to a bandwidth over quota
     *
     * @return Time during which transfers will be stopped, otherwise 0
     */
    suspend fun getBandwidthOverQuotaDelay(): Duration

    /**
     * Insert a new [ActiveTransferGroup].
     * If there's an existing [ActiveTransferGroup] with the same id, it will be ignored
     */
    suspend fun insertActiveTransferGroup(activeTransferGroup: ActiveTransferGroup): Long

    /**
     * Get the [ActiveTransferGroup] by id
     *
     * @return [ActiveTransferGroup] with this [id] or null if it's not found
     */
    suspend fun getActiveTransferGroupById(id: Int): ActiveTransferGroup?

    /**
     * Downloads a file node in background.
     *
     * @param viewerNode File node to download.
     * @return The local path of the downloaded file.
     */
    @Deprecated(
        message = "ViewerNode should be replaced by [TypedNode], there's a similar use-case to download any type of [TypedNode] and receive a flow of the progress: StartDownloadUseCase. Please add [TransferAppData.BackgroundTransfer] to avoid this transfers to be added in the counters of the DownloadService notification",
        replaceWith = ReplaceWith("StartDownloadUseCase"),
    )
    suspend fun downloadBackgroundFile(viewerNode: ViewerNode): String
}
