package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.ChatCompressionFinished
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.entity.transfer.ChatCompressionState
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pendingMessageIds
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CheckFinishedChatUploadsUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.CompressPendingMessagesUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.GetActiveTransferTotalsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.ClearPendingMessagesCompressionProgressUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.PrepareAllPendingMessagesUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartUploadingAllPendingMessagesUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber

/**
 * Worker that will monitor current active chat upload transfers while there are some.
 * This should be used once the uploads are actually started, it won't start any upload.
 */
@HiltWorker
class ChatUploadsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    handleTransferEventUseCase: HandleTransferEventUseCase,
    areTransfersPausedUseCase: AreTransfersPausedUseCase,
    getActiveTransferTotalsUseCase: GetActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder: OverQuotaNotificationBuilder,
    notificationManager: NotificationManagerCompat,
    areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase,
    correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val chatUploadNotificationMapper: ChatUploadNotificationMapper,
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
    private val checkFinishedChatUploadsUseCase: CheckFinishedChatUploadsUseCase,
    private val compressPendingMessagesUseCase: CompressPendingMessagesUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val clearPendingMessagesCompressionProgressUseCase: ClearPendingMessagesCompressionProgressUseCase,
    private val startUploadingAllPendingMessagesUseCase: StartUploadingAllPendingMessagesUseCase,
    private val monitorPendingMessagesByStateUseCase: MonitorPendingMessagesByStateUseCase,
    private val prepareAllPendingMessagesUseCase: PrepareAllPendingMessagesUseCase,
    crashReporter: CrashReporter,
    foregroundSetter: ForegroundSetter? = null,
    notificationSamplePeriod: Long? = null,
) : AbstractTransfersWorker(
    context = context,
    workerParams = workerParams,
    type = TransferType.CHAT_UPLOAD,
    ioDispatcher = ioDispatcher,
    monitorTransferEventsUseCase = monitorTransferEventsUseCase,
    handleTransferEventUseCase = handleTransferEventUseCase,
    areTransfersPausedUseCase = areTransfersPausedUseCase,
    getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
    overQuotaNotificationBuilder = overQuotaNotificationBuilder,
    notificationManager = notificationManager,
    areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
    correctActiveTransfersUseCase = correctActiveTransfersUseCase,
    clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
    crashReporter = crashReporter,
    foregroundSetter = foregroundSetter,
    notificationSamplePeriod = notificationSamplePeriod,
) {
    override val updateNotificationId = NOTIFICATION_CHAT_UPLOAD

    private var chatCompressionProgress: MutableStateFlow<ChatCompressionState> = MutableStateFlow(
        ChatCompressionProgress(0, 0, Progress(0f))
    )

    override suspend fun createUpdateNotification(
        activeTransferTotals: ActiveTransferTotals,
        paused: Boolean,
    ) = chatUploadNotificationMapper(
        activeTransferTotals,
        (chatCompressionProgress.value as? ChatCompressionProgress),
        paused
    )

    override fun monitorProgress(): Flow<MonitorProgressResult> =
        combine(
            monitorOngoingActiveTransfersUseCase(type),
            monitorPendingMessagesByStateUseCase(
                PendingMessageState.PREPARING,
                PendingMessageState.COMPRESSING,
                PendingMessageState.READY_TO_UPLOAD,
                PendingMessageState.UPLOADING,
                PendingMessageState.ATTACHING,
            ),
            chatCompressionProgress,
        ) { monitorOngoingActiveTransfersResult, pendingMessages, _ ->
            val pendingWork = pendingMessages.isNotEmpty()
                    || monitorOngoingActiveTransfersResult.hasPendingWork(type)
            MonitorProgressResult(monitorOngoingActiveTransfersResult, pendingWork)
        }

    override suspend fun onComplete() {
        super.onComplete()
        clearPendingMessagesCompressionProgressUseCase()
    }

    override suspend fun doWorkInternal(scope: CoroutineScope) {
        scope.launch {
            super.doWorkInternal(this)
        }
        scope.launch {
            prepareAllPendingMessagesUseCase()
                .catch { Timber.e(it) }
                .collect { Timber.d("Chat Upload Preparing $it attachments") }
        }
        scope.launch {
            compressPendingMessagesUseCase()
                .catch { Timber.e(it) }
                .collect {
                    chatCompressionProgress.value = it
                }
        }
        scope.launch {
            startUploadingAllPendingMessagesUseCase()
                .catch { Timber.e(it) }
                .collect { Timber.d("Chat Upload Uploading $it attachments") }
        }
    }

    override fun hasCompleted(activeTransferTotals: ActiveTransferTotals): Boolean {
        return activeTransferTotals.hasCompleted() && chatCompressionProgress.value is ChatCompressionFinished
    }

    override suspend fun onStart() {
        checkFinishedChatUploadsUseCase()
    }

    override suspend fun onTransferEventReceived(event: TransferEvent) {
        event.transfer.pendingMessageIds()?.let { pendingMessageIds ->
            (event as? TransferEvent.TransferFinishEvent)?.let { finishEvent ->
                pendingMessageIds.forEach { pendingMessageId ->
                    if (finishEvent.error == null) {
                        runCatching {
                            Timber.d("Node will be attached")
                            //once uploaded, it can be attached to the chat
                            attachNodeWithPendingMessageUseCase(
                                pendingMessageId,
                                NodeId(event.transfer.nodeHandle),
                                finishEvent.transfer.appData,
                            )
                        }.onFailure {
                            updateState(pendingMessageId, PendingMessageState.ERROR_ATTACHING)
                            Timber.e(it, "Node could not be attached")
                        }
                    } else {
                        updateState(pendingMessageId, PendingMessageState.ERROR_UPLOADING)
                    }
                }
            }
        }
    }

    private suspend fun updateState(
        pendingMessageId: Long,
        state: PendingMessageState,
    ) {
        updatePendingMessageUseCase(UpdatePendingMessageStateRequest(pendingMessageId, state))
    }

    companion object {
        /**
         * Tag for enqueue the worker to work manager
         */
        const val SINGLE_CHAT_UPLOAD_TAG = "MEGA_CHAT_UPLOAD_TAG"
        private const val NOTIFICATION_CHAT_UPLOAD = 15
    }
}