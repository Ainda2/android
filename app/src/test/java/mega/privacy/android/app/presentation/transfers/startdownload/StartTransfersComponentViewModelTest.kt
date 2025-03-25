package mega.privacy.android.app.presentation.transfers.startdownload

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.CancelTransferResult
import mega.privacy.android.app.presentation.transfers.starttransfer.model.ConfirmLargeDownloadInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.SaveDestinationInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.SetAskedResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.ShouldAskForResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.SaveDoNotPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldAskDownloadDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWorkerAndWaitUntilIsStartedUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.MonitorRequestFilesPermissionDeniedUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.SetRequestFilesPermissionDeniedUseCase
import mega.privacy.android.domain.usecase.transfers.offline.SaveOfflineNodesToDevice
import mega.privacy.android.domain.usecase.transfers.offline.SaveUriToDeviceUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.domain.usecase.transfers.pending.DeleteAllPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingDownloadsForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingUploadsForFilesUseCase
import mega.privacy.android.domain.usecase.transfers.pending.MonitorPendingTransfersUntilResolvedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetCurrentUploadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadsWorkerAndWaitUntilIsStartedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartTransfersComponentViewModelTest {

    private lateinit var underTest: StartTransfersComponentViewModel

    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val totalFileSizeOfNodesUseCase = mock<TotalFileSizeOfNodesUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val isAskBeforeLargeDownloadsSettingUseCase =
        mock<IsAskBeforeLargeDownloadsSettingUseCase>()
    private val setAskBeforeLargeDownloadsSettingUseCase =
        mock<SetAskBeforeLargeDownloadsSettingUseCase>()
    private val getOrCreateStorageDownloadLocationUseCase =
        mock<GetOrCreateStorageDownloadLocationUseCase>()
    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val getCurrentDownloadSpeedUseCase = mock<GetCurrentDownloadSpeedUseCase>()
    private val getFilePreviewDownloadPathUseCase = mock<GetFilePreviewDownloadPathUseCase>()
    private val shouldAskDownloadDestinationUseCase = mock<ShouldAskDownloadDestinationUseCase>()
    private val shouldPromptToSaveDestinationUseCase = mock<ShouldPromptToSaveDestinationUseCase>()
    private val saveDoNotPromptToSaveDestinationUseCase =
        mock<SaveDoNotPromptToSaveDestinationUseCase>()
    private val setStorageDownloadAskAlwaysUseCase = mock<SetStorageDownloadAskAlwaysUseCase>()
    private val setStorageDownloadLocationUseCase = mock<SetStorageDownloadLocationUseCase>()
    private val sendChatAttachmentsUseCase = mock<SendChatAttachmentsUseCase>()
    private val shouldAskForResumeTransfersUseCase = mock<ShouldAskForResumeTransfersUseCase>()
    private val setAskedResumeTransfersUseCase = mock<SetAskedResumeTransfersUseCase>()
    private val pauseTransfersQueueUseCase = mock<PauseTransfersQueueUseCase>()
    private val saveOfflineNodesToDevice = mock<SaveOfflineNodesToDevice>()
    private val saveUriToDeviceUseCase = mock<SaveUriToDeviceUseCase>()
    private val getCurrentUploadSpeedUseCase = mock<GetCurrentUploadSpeedUseCase>()
    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()
    private val monitorRequestFilesPermissionDeniedUseCase =
        mock<MonitorRequestFilesPermissionDeniedUseCase> {
            on { invoke() } doReturn emptyFlow()
        }
    private val setRequestFilesPermissionDeniedUseCase =
        mock<SetRequestFilesPermissionDeniedUseCase>()
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase =
        mock<StartDownloadsWorkerAndWaitUntilIsStartedUseCase>()
    private val deleteAllPendingTransfersUseCase = mock<DeleteAllPendingTransfersUseCase>()
    private val monitorPendingTransfersUntilResolvedUseCase =
        mock<MonitorPendingTransfersUntilResolvedUseCase>()
    private val monitorStorageOverQuotaUseCase = mock<MonitorStorageOverQuotaUseCase> {
        on { invoke() } doReturn emptyFlow()
    }
    private val invalidateCancelTokenUseCase = mock<InvalidateCancelTokenUseCase>()
    private val insertPendingUploadsForFilesUseCase = mock<InsertPendingUploadsForFilesUseCase>()
    private val startUploadsWorkerAndWaitUntilIsStartedUseCase =
        mock<StartUploadsWorkerAndWaitUntilIsStartedUseCase>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()
    private val insertPendingDownloadsForNodesUseCase =
        mock<InsertPendingDownloadsForNodesUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val getFileNameFromStringUriUseCase = mock<GetFileNameFromStringUriUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()

    private val node: TypedFileNode = mock()
    private val nodes = listOf(node)
    private val parentNode: TypedFolderNode = mock()
    private val startDownloadEvent = TransferTriggerEvent.StartDownloadNode(nodes)
    private val startUploadFilesEvent =
        TransferTriggerEvent.StartUpload.Files(mapOf(DESTINATION to null), parentId)
    private val startUploadTextFileEvent = TransferTriggerEvent.StartUpload.TextFile(
        DESTINATION,
        parentId,
        isEditMode = false,
        fromHomePage = false
    )
    private val startUploadEvent =
        TransferTriggerEvent.StartUpload.Files(mapOf("foo" to null), NodeId(34678L))

    @BeforeAll
    fun setup() {
        initialStub()
        initTest()
    }

    private fun initTest() {
        underTest = StartTransfersComponentViewModel(
            getOfflinePathForNodeUseCase = getOfflinePathForNodeUseCase,
            getOrCreateStorageDownloadLocationUseCase = getOrCreateStorageDownloadLocationUseCase,
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            totalFileSizeOfNodesUseCase = totalFileSizeOfNodesUseCase,
            fileSizeStringMapper = fileSizeStringMapper,
            isAskBeforeLargeDownloadsSettingUseCase = isAskBeforeLargeDownloadsSettingUseCase,
            setAskBeforeLargeDownloadsSettingUseCase = setAskBeforeLargeDownloadsSettingUseCase,
            monitorOngoingActiveTransfersUseCase = monitorOngoingActiveTransfersUseCase,
            getCurrentDownloadSpeedUseCase = getCurrentDownloadSpeedUseCase,
            shouldAskDownloadDestinationUseCase = shouldAskDownloadDestinationUseCase,
            shouldPromptToSaveDestinationUseCase = shouldPromptToSaveDestinationUseCase,
            saveDoNotPromptToSaveDestinationUseCase = saveDoNotPromptToSaveDestinationUseCase,
            setStorageDownloadAskAlwaysUseCase = setStorageDownloadAskAlwaysUseCase,
            setStorageDownloadLocationUseCase = setStorageDownloadLocationUseCase,
            sendChatAttachmentsUseCase = sendChatAttachmentsUseCase,
            shouldAskForResumeTransfersUseCase = shouldAskForResumeTransfersUseCase,
            setAskedResumeTransfersUseCase = setAskedResumeTransfersUseCase,
            pauseTransfersQueueUseCase = pauseTransfersQueueUseCase,
            saveOfflineNodesToDevice = saveOfflineNodesToDevice,
            saveUriToDeviceUseCase = saveUriToDeviceUseCase,
            getCurrentUploadSpeedUseCase = getCurrentUploadSpeedUseCase,
            cancelCancelTokenUseCase = cancelCancelTokenUseCase,
            monitorRequestFilesPermissionDeniedUseCase = monitorRequestFilesPermissionDeniedUseCase,
            setRequestFilesPermissionDeniedUseCase = setRequestFilesPermissionDeniedUseCase,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase = startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            startUploadsWorkerAndWaitUntilIsStartedUseCase = startUploadsWorkerAndWaitUntilIsStartedUseCase,
            deleteAllPendingTransfersUseCase = deleteAllPendingTransfersUseCase,
            monitorPendingTransfersUntilResolvedUseCase = monitorPendingTransfersUntilResolvedUseCase,
            insertPendingDownloadsForNodesUseCase = insertPendingDownloadsForNodesUseCase,
            insertPendingUploadsForFilesUseCase = insertPendingUploadsForFilesUseCase,
            monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase,
            invalidateCancelTokenUseCase = invalidateCancelTokenUseCase,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            getFileNameFromStringUriUseCase = getFileNameFromStringUriUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getOfflinePathForNodeUseCase,
            getOrCreateStorageDownloadLocationUseCase,
            getFilePreviewDownloadPathUseCase,
            clearActiveTransfersIfFinishedUseCase,
            isConnectedToInternetUseCase,
            totalFileSizeOfNodesUseCase,
            fileSizeStringMapper,
            isAskBeforeLargeDownloadsSettingUseCase,
            setAskBeforeLargeDownloadsSettingUseCase,
            monitorOngoingActiveTransfersUseCase,
            getCurrentDownloadSpeedUseCase,
            shouldAskDownloadDestinationUseCase,
            shouldPromptToSaveDestinationUseCase,
            saveDoNotPromptToSaveDestinationUseCase,
            setStorageDownloadAskAlwaysUseCase,
            setStorageDownloadLocationUseCase,
            sendChatAttachmentsUseCase,
            shouldAskForResumeTransfersUseCase,
            setAskedResumeTransfersUseCase,
            pauseTransfersQueueUseCase,
            node,
            parentNode,
            saveOfflineNodesToDevice,
            saveUriToDeviceUseCase,
            getCurrentUploadSpeedUseCase,
            cancelCancelTokenUseCase,
            setRequestFilesPermissionDeniedUseCase,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            deleteAllPendingTransfersUseCase,
            monitorPendingTransfersUntilResolvedUseCase,
            insertPendingDownloadsForNodesUseCase,
            invalidateCancelTokenUseCase,
            insertPendingUploadsForFilesUseCase,
            startUploadsWorkerAndWaitUntilIsStartedUseCase,
            getCurrentTimeInMillisUseCase,
            areTransfersPausedUseCase,
            getFileNameFromStringUriUseCase,
            cancelTransferByTagUseCase,
        )
        initialStub()
    }

    private fun initialStub() = runTest {
        whenever(monitorOngoingActiveTransfersUseCase(any())).thenReturn(emptyFlow())
        whenever(monitorRequestFilesPermissionDeniedUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(emptyFlow())
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that clearActiveTransfersIfFinishedUseCase is invoked when startTransfer is invoked`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.startTransfer(startEvent)
        verify(clearActiveTransfersIfFinishedUseCase).invoke()
    }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that start download worker is started  when download is started`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        if (startEvent is TransferTriggerEvent.StartDownloadForOffline) {
            whenever(getOfflinePathForNodeUseCase(any())).thenReturn(DESTINATION)
        } else if (startEvent is TransferTriggerEvent.StartDownloadForPreview) {
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn(DESTINATION)
        }
        underTest.startTransfer(startEvent)
        verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    @ParameterizedTest
    @MethodSource("provideStartChatUploadEvents")
    fun `test that send chat attachments use case is invoked with correct parameters when chat upload is started`(
        startEvent: TransferTriggerEvent.StartChatUpload,
    ) = runTest {
        commonStub()
        underTest.startTransfer(startEvent)
        verify(sendChatAttachmentsUseCase).invoke(
            listOf(uploadUri.toString()).associateWith { null },
            false,
            CHAT_ID,
        )
    }

    @Test
    fun `test that no connection event is emitted when monitorConnectivityUseCase is false and start a download`() =
        runTest {
            commonStub()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            underTest.startTransfer(TransferTriggerEvent.StartDownloadNode(nodes))
            assertCurrentEventIsEqualTo(StartTransferEvent.NotConnected)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with empty list`() =
        runTest {
            commonStub()
            underTest.startTransfer(
                TransferTriggerEvent.StartDownloadNode(listOf())
            )
            assertCurrentEventIsEqualTo(StartTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with null node`() =
        runTest {
            commonStub()
            underTest.startTransfer(
                TransferTriggerEvent.StartDownloadForOffline(null)
            )
            assertCurrentEventIsEqualTo(StartTransferEvent.Message.TransferCancelled)
        }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that ConfirmLargeDownload is emitted when a large download is started`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        whenever(isAskBeforeLargeDownloadsSettingUseCase()).thenReturn(true)
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(TransfersConstants.CONFIRM_SIZE_MIN_BYTES + 1)
        val size = "x MB"
        whenever(fileSizeStringMapper(any())).thenReturn(size)
        underTest.startTransfer(startEvent)
        assertThat(underTest.uiState.value.confirmLargeDownload).isEqualTo(
            ConfirmLargeDownloadInfo(size, startEvent)
        )
    }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that setAskBeforeLargeDownloadsSettingUseCase is invoked when specified in largeDownloadAnswered`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.largeDownloadAnswered(startEvent, true)
        verify(setAskBeforeLargeDownloadsSettingUseCase).invoke(false)
    }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that setAskBeforeLargeDownloadsSettingUseCase is not invoked when not specified in largeDownloadAnswered`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.largeDownloadAnswered(startEvent, false)
        verifyNoInteractions(setAskBeforeLargeDownloadsSettingUseCase)
    }

    @Test
    fun `test that AskDestination event is triggered when a download starts and shouldAskDownloadDestinationUseCase is true`() =
        runTest {
            commonStub()
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            val event = TransferTriggerEvent.StartDownloadNode(nodes)
            underTest.startDownloadWithoutConfirmation(event)
            assertThat(underTest.uiState.value.askDestinationForDownload).isEqualTo(
                event
            )
        }

    @Test
    fun `test that promptSaveDestination state is updated when startDownloadWithDestination is invoked and shouldPromptToSaveDestinationUseCase is true`() =
        runTest {
            commonStub()
            val uriString = "content:/destination"
            val destinationName = "destinationName"
            val destinationUri = mock<Uri> {
                on { toString() } doReturn uriString
            }
            val startDownloadNode = TransferTriggerEvent.StartDownloadNode(nodes)
            val expected = SaveDestinationInfo(
                destination = uriString,
                destinationName = destinationName
            )

            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            whenever(shouldPromptToSaveDestinationUseCase()).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(uriString)).thenReturn(destinationName)

            underTest.startDownloadWithoutConfirmation(startDownloadNode)
            underTest.startDownloadWithDestination(destinationUri)

            assertThat(underTest.uiState.value.promptSaveDestination)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((underTest.uiState.value.promptSaveDestination as StateEventWithContentTriggered).content)
                .isEqualTo(expected)

        }

    @Test
    fun `test that consumePromptSaveDestination updates state`() =
        runTest {
            commonStub()
            val uriString = "content:/destination"
            val destinationName = "destinationName"
            val destinationUri = mock<Uri> {
                on { toString() } doReturn uriString
            }
            val startDownloadNode = TransferTriggerEvent.StartDownloadNode(nodes)
            val expected = SaveDestinationInfo(
                destination = uriString,
                destinationName = destinationName
            )

            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            whenever(shouldPromptToSaveDestinationUseCase()).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(uriString)).thenReturn(destinationName)

            underTest.startDownloadWithoutConfirmation(startDownloadNode)
            underTest.startDownloadWithDestination(destinationUri)
            underTest.consumePromptSaveDestination()

            assertThat(underTest.uiState.value.promptSaveDestination)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }

    @Test
    fun `test that saveDoNotPromptToSaveDestinationUseCase is invoked when doNotPromptToSaveDestinationAgain is invoked`() =
        runTest {
            underTest.doNotPromptToSaveDestinationAgain()
            verify(saveDoNotPromptToSaveDestinationUseCase).invoke()
        }

    @Test
    fun `test that setStorageDownloadLocationUseCase is invoked when saveDestination is invoked`() =
        runTest {
            val destination = "destination"
            underTest.saveDestination(destination)
            verify(setStorageDownloadLocationUseCase).invoke(destination)
        }

    @Test
    fun `test that setStorageDownloadAskAlwaysUseCase is set to false when saveDestination is invoked`() =
        runTest {
            underTest.saveDestination("destination")
            verify(setStorageDownloadAskAlwaysUseCase).invoke(false)
        }

    @Test
    fun `test that paused transfers event is emitted when shouldAskForResumeTransfersUseCase is true and start a chat upload`() =
        runTest {
            commonStub()
            whenever(shouldAskForResumeTransfersUseCase()).thenReturn(true)
            val triggerEvent =
                TransferTriggerEvent.StartChatUpload.Files(CHAT_ID, listOf(uploadUri))
            underTest.startTransfer(triggerEvent)
            assertCurrentEventIsEqualTo(StartTransferEvent.PausedTransfers(triggerEvent))
        }

    @Test
    fun `test that paused transfers event is emitted when transfers are paused and start a preview download`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(
                TransferType.DOWNLOAD,
                flow { awaitCancellation() } //no events when it's paused
            )
            whenever(areTransfersPausedUseCase()).thenReturn(true)
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn("/path")
            val triggerEvent =
                TransferTriggerEvent.StartDownloadForPreview(mock<ChatDefaultFile>(), false)
            underTest.startTransfer(triggerEvent)
            assertCurrentEventIsEqualTo(StartTransferEvent.PausedTransfers(triggerEvent))
        }

    @Test
    fun `test that no connection event is emitted when monitorConnectivityUseCase is false and start an upload`() =
        runTest {
            commonStub()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)

            underTest.startTransfer(startUploadFilesEvent)

            assertCurrentEventIsEqualTo(StartTransferEvent.NotConnected)
        }

    @Test
    fun `test that cancel event is emitted when start upload files is invoked with empty map`() =
        runTest {
            commonStub()

            underTest.startTransfer(
                TransferTriggerEvent.StartUpload.Files(mapOf(), parentId)
            )

            assertCurrentEventIsEqualTo(StartTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that job in progress is set to ProcessingFiles when start upload use case starts`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                emit(mockScanningPendingTransfers())
                awaitCancellation()
            })

            underTest.startTransfer(startUploadFilesEvent)

            assertThat(underTest.uiState.value.jobInProgressState)
                .isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
        }

    @ParameterizedTest
    @MethodSource("provideStartUploadEvents")
    fun `test that start upload use case is invoked with correct parameters when upload is started`(
        startEvent: TransferTriggerEvent.StartUpload,
    ) = runTest {
        commonStub()

        underTest.startTransfer(startEvent)

        verify(insertPendingUploadsForFilesUseCase).invoke(
            mapOf(DESTINATION to null),
            parentId,
            startEvent.isHighPriority,
        )
    }

    @Test
    fun `test that FinishUploadProcessing event is emitted if start upload use case emits an event with scanning finished true`() =
        runTest {
            commonStub()

            underTest.startTransfer(startUploadFilesEvent)

            assertThat(underTest.uiState.value.jobInProgressState).isNull()
            assertCurrentEventIsEqualTo(
                StartTransferEvent.FinishUploadProcessing(1, startUploadFilesEvent)
            )
        }

    @Test
    fun `test that FinishUploadProcessing event is emitted if start upload use case finishes correctly`() =
        runTest {
            commonStub()

            underTest.startTransfer(startUploadFilesEvent)

            assertCurrentEventIsEqualTo(
                StartTransferEvent.FinishUploadProcessing(1, startUploadFilesEvent)
            )
        }

    @Test
    fun `test that failed text file upload event is emitted when monitorActiveTransferFinishedUseCase emits a value and transferTriggerEvent is StartUploadTextFile`() =
        runTest {
            setup()
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                throw (RuntimeException())
            })

            underTest.startTransfer(startUploadTextFileEvent)
            underTest.onResume(mock())
            underTest.uiState.test {
                val expected =
                    triggered(
                        StartTransferEvent.Message.FailedTextFileUpload(
                            isEditMode = startUploadTextFileEvent.isEditMode,
                            isCloudFile = startUploadTextFileEvent.fromHomePage
                        )
                    )

                val actual = awaitItem().oneOffViewEvent

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that start download with destination trigger save offline nodes to device when event is CopyOfflineNode`() =
        runTest {
            commonStub()
            val uri = mock<Uri> {
                on { toString() } doReturn DESTINATION
            }
            val nodeId = NodeId(1)
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            whenever(saveOfflineNodesToDevice(listOf(nodeId), UriPath(DESTINATION))).thenReturn(1)
            underTest.startDownloadWithoutConfirmation(
                TransferTriggerEvent.CopyOfflineNode(listOf(nodeId))
            )
            underTest.startDownloadWithDestination(
                uri
            )
            assertCurrentEventIsEqualTo(
                StartTransferEvent.FinishCopyOffline(1)
            )
            verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
        }

    @Test
    fun `test that start download without confirmation trigger save offline nodes to device when event is CopyOfflineNode`() =
        runTest {
            commonStub()
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
            whenever(getOrCreateStorageDownloadLocationUseCase()).thenReturn(DESTINATION)
            val nodeId = NodeId(1)
            underTest.startDownloadWithoutConfirmation(
                TransferTriggerEvent.CopyOfflineNode(listOf(nodeId)),
            )
            verify(saveOfflineNodesToDevice).invoke(listOf(nodeId), UriPath(DESTINATION))
            verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
        }

    @Test
    fun `test that start download without confirmation trigger save uri to device when event is CopyUri`() =
        runTest {
            commonStub()
            val sourceUri = "Source"
            val uri = mock<Uri> {
                on { toString() } doReturn sourceUri
            }
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
            whenever(getOrCreateStorageDownloadLocationUseCase()).thenReturn(DESTINATION)
            underTest.startDownloadWithoutConfirmation(
                TransferTriggerEvent.CopyUri(
                    "name",
                    uri
                )
            )
            verify(saveUriToDeviceUseCase).invoke("name", UriPath(sourceUri), UriPath(DESTINATION))
            verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
        }

    @Test
    fun `test that cancel current transfers job invokes cancel cancelToken use case`() = runTest {
        commonStub()
        underTest.cancelCurrentTransfersJob()

        verify(cancelCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that cancel current transfers job sets state to cancelling when previous state was scanning`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                emit(mockScanningPendingTransfers())
                awaitCancellation()
            })
            underTest.uiState.test {
                awaitItem() //don't care about initial value
                underTest.startTransfer(startUploadFilesEvent)
                assertThat(awaitItem().jobInProgressState).isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)

                underTest.cancelCurrentTransfersJob()
                assertThat(awaitItem().jobInProgressState).isEqualTo(StartTransferJobInProgress.CancellingTransfers)
            }
        }

    @Test
    fun `test that cancel current transfers job does not set state to cancelling when scanning has already finished`() =
        runTest {
            commonStub()
            val pendingTransfersFlow = MutableSharedFlow<List<PendingTransfer>>()
            stubMonitorPendingTransfers(
                TransferType.GENERAL_UPLOAD,
                pendingTransfersFlow.takeWhile { it.isNotEmpty() }
            )
            underTest.uiState.test {
                awaitItem() //don't care about initial value
                underTest.startTransfer(startUploadFilesEvent)
                pendingTransfersFlow.emit(mockScanningPendingTransfers())
                assertThat(awaitItem().jobInProgressState).isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
                pendingTransfersFlow.emit(emptyList())
                assertThat(awaitItem().jobInProgressState).isEqualTo(null)
                underTest.cancelCurrentTransfersJob()
                expectNoEvents()
            }
        }

    @Test
    fun `test that cancel current transfers job does not set state to cancelling when previous state was not scanning`() =
        runTest {
            commonStub()
            underTest.uiState.test {
                underTest.cancelCurrentTransfersJob()
                assertThat(awaitItem().jobInProgressState).isNotInstanceOf(
                    StartTransferJobInProgress.CancellingTransfers::class.java
                )
            }
        }

    @Test
    fun `test that cancel current transfers job does not set state to cancelling when the cancellation fails`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                emit(mockScanningPendingTransfers())
                awaitCancellation()
            })
            whenever(cancelCancelTokenUseCase()).thenThrow(RuntimeException())
            underTest.uiState.test {

                awaitItem() //don't care about initial value
                underTest.startTransfer(startUploadFilesEvent)
                assertThat(awaitItem().jobInProgressState).isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)

                underTest.cancelCurrentTransfersJob()
                expectNoEvents()
            }
        }

    private fun mockScanningPendingTransfers(): List<PendingTransfer> = listOf(mock {
        on { scanningFoldersData } doReturn PendingTransfer.ScanningFoldersData(
            stage = TransferStage.STAGE_SCANNING
        )
    })

    @Test
    fun `test that monitorRequestFilesPermissionDeniedUseCase updates state`() =
        runTest {
            whenever(monitorRequestFilesPermissionDeniedUseCase())
                .thenReturn(flowOf(true))

            initTest()

            underTest.uiState.map { it.requestFilesPermissionDenied }.test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that setRequestFilesPermissionDenied invokes correct use case`() = runTest {
        whenever(setRequestFilesPermissionDeniedUseCase()).thenReturn(Unit)

        underTest.setRequestFilesPermissionDenied()
        verify(setRequestFilesPermissionDeniedUseCase).invoke()
    }

    @ParameterizedTest(name = " if use case returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that monitorStorageOverQuota updates state`(
        isStorageOverQuota: Boolean,
    ) = runTest {
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(flowOf(isStorageOverQuota))

        initTest()

        underTest.uiState.map { it.isStorageOverQuota }.test {
            assertThat(awaitItem()).isEqualTo(isStorageOverQuota)
        }
    }

    @Nested
    inner class StartDownload {
        @Test
        fun `test that startDownloadsWorkerAndWaitUntilIsStartedUseCase is invoked when a download starts`() =
            runTest {
                commonStub()

                underTest.startTransfer(startDownloadEvent)

                verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase)()
            }

        @Test
        fun `test that ui state is updated with pending transfers from monitorNotResolvedPendingTransfersUseCase when a download starts`() =
            runTest {
                commonStub()
                val expectedList = listOf(
                    TransferStage.STAGE_NONE,
                    TransferStage.STAGE_SCANNING,
                    TransferStage.STAGE_CREATING_TREE,
                )

                val pendingTransfers = expectedList.map { stage ->
                    listOf(mock<PendingTransfer> {
                        on { this.scanningFoldersData } doReturn
                                PendingTransfer.ScanningFoldersData(stage)
                    })
                }

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn flow {
                    pendingTransfers.forEach {
                        emit(it)
                        yield()
                    }
                    awaitCancellation()
                }

                underTest.uiState.test {
                    println(awaitItem())//ignore initial
                    underTest.startTransfer(TransferTriggerEvent.StartDownloadNode(nodes))
                    expectedList.forEach { expected ->
                        val actual =
                            (awaitItem().jobInProgressState as? StartTransferJobInProgress.ScanningTransfers)?.stage
                        println(actual)
                        assertThat(actual).isEqualTo(expected)
                    }
                }

                assertThat(underTest.uiState.value.jobInProgressState)
                    .isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
            }

        @Test
        fun `test that FinishDownloadProcessing event is emitted when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                    on { this.startedFiles } doReturn 1
                }
                val triggerEvent = TransferTriggerEvent.StartDownloadNode(nodes)
                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(triggerEvent)

                assertThat(underTest.uiState.value.jobInProgressState).isNull()
                assertCurrentEventIsEqualTo(
                    StartTransferEvent.FinishDownloadProcessing(null, triggerEvent)
                )
            }

        @Test
        fun `test that deleteAllPendingTransfersUseCase is invoked when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }
                val triggerEvent = TransferTriggerEvent.StartDownloadNode(nodes)
                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(triggerEvent)

                verify(deleteAllPendingTransfersUseCase)()
            }
    }

    @Nested
    inner class StartUpload {
        @Test
        fun `test that startUploadsWorkerAndWaitUntilIsStartedUseCase is invoked when an upload starts`() =
            runTest {
                commonStub()

                underTest.startTransfer(startUploadFilesEvent)

                verify(startUploadsWorkerAndWaitUntilIsStartedUseCase)()
            }

        @Test
        fun `test that ui state is updated with pending transfers from monitorNotResolvedPendingTransfersUseCase when an upload starts`() =
            runTest {
                commonStub()
                val expectedList = listOf(
                    TransferStage.STAGE_NONE,
                    TransferStage.STAGE_SCANNING,
                    TransferStage.STAGE_CREATING_TREE,
                )

                val pendingTransfers = expectedList.map { stage ->
                    listOf(mock<PendingTransfer> {
                        on { this.scanningFoldersData } doReturn
                                PendingTransfer.ScanningFoldersData(stage)
                    })
                }

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn flow {
                    pendingTransfers.forEach {
                        emit(it)
                        yield()
                    }
                    awaitCancellation()
                }

                underTest.uiState.test {
                    println(awaitItem())//ignore initial
                    underTest.startTransfer(startUploadEvent)
                    expectedList.forEach { expected ->
                        val actual =
                            (awaitItem().jobInProgressState as? StartTransferJobInProgress.ScanningTransfers)?.stage
                        println(actual)
                        assertThat(actual).isEqualTo(expected)
                    }
                }

                assertThat(underTest.uiState.value.jobInProgressState)
                    .isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
            }

        @Test
        fun `test that FinishDownloadProcessing event is emitted when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                    on { this.startedFiles } doReturn 1
                }
                val triggerEvent = startUploadEvent
                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(triggerEvent)

                assertThat(underTest.uiState.value.jobInProgressState).isNull()
                assertCurrentEventIsEqualTo(
                    StartTransferEvent.FinishUploadProcessing(1, triggerEvent)
                )
            }

        @Test
        fun `test that deleteAllPendingTransfersUseCase is invoked when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(startUploadEvent)

                verify(deleteAllPendingTransfersUseCase)()
            }
    }

    @Test
    fun `test that invalidateCancelTokenUseCase is invoked when monitorNotResolvedPendingTransfersUseCase finishes`() =
        runTest {
            commonStub()
            val pendingTransfer = mock<PendingTransfer> {
                val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                    TransferStage.STAGE_TRANSFERRING_FILES,
                )
                on { this.scanningFoldersData } doReturn scanningFoldersData
            }
            val triggerEvent = TransferTriggerEvent.StartDownloadNode(nodes)
            whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                    flowOf(listOf(pendingTransfer))

            underTest.startTransfer(triggerEvent)

            verify(invalidateCancelTokenUseCase)()
        }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that insertPendingDownloadsForNodesUseCase is invoked with correct parameters when a download starts`(
        startDownloadEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        whenever(getOfflinePathForNodeUseCase(any())) doReturn DESTINATION
        whenever(getFilePreviewDownloadPathUseCase()) doReturn DESTINATION

        underTest.startTransfer(startDownloadEvent)

        verify(insertPendingDownloadsForNodesUseCase)(
            startDownloadEvent.nodes,
            UriPath(DESTINATION),
            startDownloadEvent.isHighPriority,
            startDownloadEvent.appData,
        )
    }

    @Nested
    inner class TriggerEventWithoutPermission {

        @BeforeEach
        fun cleanUp() {
            underTest.consumeRequestPermission()
        }

        @Test
        fun `test that transferEventWaitingForPermissionRequest sets triggerEventWithoutPermission`() =
            runTest {
                val expected = mock<TransferTriggerEvent.StartUpload.Files>()
                underTest.uiState.test {
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                    underTest.transferEventWaitingForPermissionRequest(expected)

                    val actual = awaitItem().triggerEventWithoutPermission
                    assertThat(actual).isEqualTo(expected)
                }
            }

        @Test
        fun `test that consumeRequestPermission clears triggerEventWithoutPermission`() =
            runTest {
                val expected = mock<TransferTriggerEvent.StartUpload.Files>()
                underTest.uiState.test {
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                    underTest.transferEventWaitingForPermissionRequest(expected)
                    assertThat(awaitItem().triggerEventWithoutPermission).isNotNull()

                    underTest.consumeRequestPermission()

                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                }
            }


        @Test
        fun `test that startTransferAfterPermissionRequest starts transfer flow`() = runTest {
            commonStub()
            val event = mock<TransferTriggerEvent.StartUpload.Files>()
            underTest.transferEventWaitingForPermissionRequest(event)

            underTest.startTransferAfterPermissionRequest()

            verify(clearActiveTransfersIfFinishedUseCase).invoke()
        }

        @Test
        fun `test that startTransferAfterPermissionRequest clears triggerEventWithoutPermission`() =
            runTest {
                commonStub()
                val expected = mock<TransferTriggerEvent.StartUpload.Files>()
                underTest.uiState.test {
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                    underTest.transferEventWaitingForPermissionRequest(expected)
                    assertThat(awaitItem().triggerEventWithoutPermission).isNotNull()

                    underTest.startTransferAfterPermissionRequest()
                    awaitItem() //new ui state for the started transfer
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                }
            }
    }

    @Test
    fun `test that startTransfer with CancelPreviewDownload updates state correctly`() = runTest {
        val transferTagToCancel = 1
        underTest.uiState.test {
            assertThat(awaitItem().transferTagToCancel).isNull()
            underTest.startTransfer(TransferTriggerEvent.CancelPreviewDownload(transferTagToCancel))
            assertThat(awaitItem().transferTagToCancel).isEqualTo(transferTagToCancel)
        }
    }

    @Test
    fun `test that cancelTransferConfirmed invokes correctly and reset the transfer to cancel`() =
        runTest {
            val transferTagToCancel = 1

            whenever(cancelTransferByTagUseCase(transferTagToCancel)).thenReturn(Unit)

            with(underTest) {
                startTransfer(TransferTriggerEvent.CancelPreviewDownload(transferTagToCancel))
                cancelTransferConfirmed()

                uiState.test {
                    assertThat(awaitItem().transferTagToCancel).isNull()
                }
            }

            verify(cancelTransferByTagUseCase).invoke(transferTagToCancel)
        }

    @ParameterizedTest(name = " when use case finishes with success: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that cancelTransferConfirmed updates the state correctly`(success: Boolean) =
        runTest {
            val transferTagToCancel = 1

            whenever(cancelTransferByTagUseCase(transferTagToCancel)).also {
                if (success) {
                    it.thenReturn(Unit)
                } else {
                    it.thenThrow(RuntimeException())
                }
            }

            with(underTest) {
                startTransfer(TransferTriggerEvent.CancelPreviewDownload(transferTagToCancel))
                cancelTransferConfirmed()

                uiState.test {
                    val result = awaitItem().cancelTransferResult
                    assertThat(result).isInstanceOf(StateEventWithContentTriggered::class.java)
                    val content = (result as StateEventWithContentTriggered).content
                    assertThat(content).isInstanceOf(CancelTransferResult::class.java)
                    assertThat(content.success).isEqualTo(success)
                }
            }
        }

    @Test
    fun `test that onConsumeCancelTransferResult updates the state correctly`() =
        runTest {
            val transferTagToCancel = 1

            whenever(cancelTransferByTagUseCase(transferTagToCancel)).thenReturn(Unit)

            with(underTest) {
                startTransfer(TransferTriggerEvent.CancelPreviewDownload(transferTagToCancel))
                cancelTransferConfirmed()
                onConsumeCancelTransferResult()

                uiState.test {
                    val result = awaitItem().cancelTransferResult
                    assertThat(result).isInstanceOf(StateEventWithContentConsumed::class.java)
                }
            }
        }

    @Test
    fun `test that cancelTransferCancelled resets the transfer to cancel `() = runTest {
        val transferTagToCancel = 1

        with(underTest) {
            startTransfer(TransferTriggerEvent.CancelPreviewDownload(transferTagToCancel))
            cancelTransferCancelled()

            uiState.test {
                assertThat(awaitItem().transferTagToCancel).isNull()
            }
        }
    }

    private fun provideStartDownloadEvents() = listOf(
        TransferTriggerEvent.StartDownloadNode(nodes),
        TransferTriggerEvent.StartDownloadForOffline(node),
        TransferTriggerEvent.StartDownloadForPreview(node, false),

        )

    private fun provideStartChatUploadEvents() = listOf(
        TransferTriggerEvent.StartChatUpload.Files(CHAT_ID, listOf(uploadUri)),
    )

    private fun provideStartUploadEvents() = listOf(
        startUploadFilesEvent,
        startUploadTextFileEvent,
    )

    private fun provideStartEvents() = provideStartDownloadEvents() +
            provideStartChatUploadEvents() + provideStartUploadEvents()

    private fun assertCurrentEventIsEqualTo(event: StartTransferEvent) {
        assertThat(underTest.uiState.value.oneOffViewEvent)
            .isInstanceOf(StateEventWithContentTriggered::class.java)
        assertThat((underTest.uiState.value.oneOffViewEvent as StateEventWithContentTriggered).content)
            .isEqualTo(event)
    }

    private suspend fun commonStub() {
        whenever(isAskBeforeLargeDownloadsSettingUseCase()).thenReturn(false)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.parentId).thenReturn(parentId)
        whenever(parentNode.id).thenReturn(parentId)

        whenever(getOrCreateStorageDownloadLocationUseCase()).thenReturn(DESTINATION)

        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(1)
        whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
        whenever(monitorRequestFilesPermissionDeniedUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(emptyFlow())
    }

    private fun stubMonitorPendingTransfers(
        transferType: TransferType,
        flow: Flow<List<PendingTransfer>>,
    ) {
        whenever(
            monitorPendingTransfersUntilResolvedUseCase(transferType)
        ).thenReturn(flow)
    }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private const val CHAT_ID = 20L
        private val uploadUri = mock<Uri>()
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
        private const val DESTINATION = "/destination/"
    }
}
