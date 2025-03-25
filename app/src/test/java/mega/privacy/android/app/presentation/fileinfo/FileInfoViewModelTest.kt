package mega.privacy.android.app.presentation.fileinfo

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoExtraAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoJobInProgressState
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoOneOffViewEvent
import mega.privacy.android.app.presentation.fileinfo.model.mapper.NodeActionMapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.data.gateway.ClipboardGateway
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetImageNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsMasterBusinessAccountUseCase
import mega.privacy.android.domain.usecase.MonitorChildrenUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.MonitorOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.business.IsBusinessAccountActiveUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandleUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.filenode.GetNodeVersionsByHandleUseCase
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.GetAvailableNodeActionsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.SetNodeDescriptionUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.GetNodeOutSharesUseCase
import mega.privacy.android.domain.usecase.shares.SetOutgoingPermissions
import mega.privacy.android.domain.usecase.shares.StopSharingNode
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.net.URI
import java.util.stream.Stream
import kotlin.test.assertNull

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileInfoViewModelTest {
    private lateinit var underTest: FileInfoViewModel

    private val fileUtilWrapper: FileUtilWrapper = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase = mock()
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase = mock()
    private val deleteNodeByHandleUseCase: DeleteNodeByHandleUseCase = mock()
    private val deleteNodeVersionsUseCase: DeleteNodeVersionsUseCase = mock()
    private val node: MegaNode = mock()
    private val getPreviewUseCase: GetPreviewUseCase = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getContactItemFromInShareFolder: GetContactItemFromInShareFolder = mock()
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById = mock()
    private val monitorChildrenUpdates: MonitorChildrenUpdates = mock()
    private val monitorContactUpdates: MonitorContactUpdates = mock()
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getNodeVersionsByHandleUseCase: GetNodeVersionsByHandleUseCase = mock()
    private val getNodeLocationInfo: GetNodeLocationInfo = mock()
    private val getNodeOutSharesUseCase: GetNodeOutSharesUseCase = mock()
    private val setNodeDescriptionUseCase: SetNodeDescriptionUseCase = mock()
    private val isAvailableOffline: IsAvailableOfflineUseCase = mock()
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase = mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val setOutgoingPermissions: SetOutgoingPermissions = mock()
    private val stopSharingNode: StopSharingNode = mock()
    private val nodeActionMapper: NodeActionMapper = mock()
    private val getAvailableNodeActionsUseCase: GetAvailableNodeActionsUseCase = mock()
    private val monitorChatOnlineStatusUseCase = mock<MonitorChatOnlineStatusUseCase>()
    private val clipboardGateway = mock<ClipboardGateway>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val isMediaUploadsEnabledUseCase = mock<IsMediaUploadsEnabledUseCase>()
    private val monitorOfflineFileAvailabilityUseCase =
        mock<MonitorOfflineFileAvailabilityUseCase>()
    private val getContactVerificationWarningUseCase = mock<GetContactVerificationWarningUseCase>()
    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val fileTypeIconMapper = FileTypeIconMapper()
    private val monitorAccountDetailsUseCase = mock<MonitorAccountDetailUseCase>()
    private val isMasterBusinessAccountUseCase = mock<IsMasterBusinessAccountUseCase>()
    private val isBusinessAccountActiveUseCase = mock<IsBusinessAccountActiveUseCase>()

    private val typedFileNode: TypedFileNode = mock()

    private val previewFile: File = mock()
    private val getImageNodeByIdUseCase = mock<GetImageNodeByIdUseCase>()

    @BeforeEach
    fun cleanUp() = runTest {
        resetMocks()
        initDefaultMockBehaviour()
        initUnderTestViewModel()
    }

    private fun resetMocks() {
        reset(
            fileUtilWrapper,
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            isNodeInBackupsUseCase,
            isNodeInRubbishBinUseCase,
            moveNodeToRubbishBinUseCase,
            checkNodesNameCollisionWithActionUseCase,
            deleteNodeByHandleUseCase,
            deleteNodeVersionsUseCase,
            node,
            getPreviewUseCase,
            getFolderTreeInfo,
            getNodeByIdUseCase,
            getContactItemFromInShareFolder,
            monitorNodeUpdatesById,
            monitorChildrenUpdates,
            monitorContactUpdates,
            megaNodeRepository,
            getNodeVersionsByHandleUseCase,
            getNodeLocationInfo,
            getNodeOutSharesUseCase,
            setNodeDescriptionUseCase,
            isAvailableOffline,
            removeOfflineNodeUseCase,
            getNodeAccessPermission,
            setOutgoingPermissions,
            stopSharingNode,
            nodeActionMapper,
            getAvailableNodeActionsUseCase,
            monitorChatOnlineStatusUseCase,
            clipboardGateway,
            getFeatureFlagValueUseCase,
            getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase,
            isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase,
            monitorOfflineFileAvailabilityUseCase,
            getContactVerificationWarningUseCase,
            typedFileNode,
            previewFile,
            isMasterBusinessAccountUseCase,
            isBusinessAccountActiveUseCase,
            monitorAccountDetailsUseCase
        )
    }

    private fun initUnderTestViewModel() {
        underTest = FileInfoViewModel(
            tempMegaNodeRepository = megaNodeRepository,
            fileUtilWrapper = fileUtilWrapper,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            moveNodeToRubbishBinUseCase = moveNodeToRubbishBinUseCase,
            deleteNodeByHandleUseCase = deleteNodeByHandleUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            getPreviewUseCase = getPreviewUseCase,
            getFolderTreeInfo = getFolderTreeInfo,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getContactItemFromInShareFolder = getContactItemFromInShareFolder,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            monitorChildrenUpdates = monitorChildrenUpdates,
            monitorContactUpdates = monitorContactUpdates,
            getNodeVersionsByHandleUseCase = getNodeVersionsByHandleUseCase,
            getNodeLocationInfo = getNodeLocationInfo,
            getNodeOutSharesUseCase = getNodeOutSharesUseCase,
            setNodeDescriptionUseCase = setNodeDescriptionUseCase,
            isAvailableOfflineUseCase = isAvailableOffline,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            setOutgoingPermissions = setOutgoingPermissions,
            stopSharingNode = stopSharingNode,
            getAvailableNodeActionsUseCase = getAvailableNodeActionsUseCase,
            nodeActionMapper = nodeActionMapper,
            monitorChatOnlineStatusUseCase = monitorChatOnlineStatusUseCase,
            clipboardGateway = clipboardGateway,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
            monitorOfflineFileAvailabilityUseCase = monitorOfflineFileAvailabilityUseCase,
            getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            getImageNodeByNodeId = getImageNodeByIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailsUseCase,
            isMasterBusinessAccountUseCase = isMasterBusinessAccountUseCase,
            isBusinessAccountActiveUseCase = isBusinessAccountActiveUseCase,
            iODispatcher = UnconfinedTestDispatcher()
        )
    }

    private suspend fun initDefaultMockBehaviour() {
        whenever(node.handle).thenReturn(NODE_HANDLE)
        whenever(typedFileNode.id).thenReturn(nodeId)
        whenever(isConnectedToInternetUseCase.invoke()).thenReturn(true)
        whenever(typedFileNode.versionCount).thenReturn(0)
        whenever(isNodeInBackupsUseCase(NODE_HANDLE)).thenReturn(false)
        whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(false)
        whenever(previewFile.exists()).thenReturn(true)
        whenever(previewFile.toURI()).thenReturn(URI.create(PREVIEW_URI))
        whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(typedFileNode)
        whenever(megaNodeRepository.getNodeByHandle(node.handle)).thenReturn(node)
        whenever(getNodeVersionsByHandleUseCase(nodeId)).thenReturn(null)
        whenever(monitorNodeUpdatesById.invoke(nodeId)).thenReturn(emptyFlow())
        whenever(monitorChildrenUpdates.invoke(nodeId)).thenReturn(emptyFlow())
        whenever(monitorChatOnlineStatusUseCase.invoke()).thenReturn(emptyFlow())
        whenever(monitorContactUpdates.invoke()).thenReturn(emptyFlow())
        whenever(fileUtilWrapper.getFileIfExists(null, THUMB_URI))
            .thenReturn(File(null as File?, THUMB_URI))
        whenever(typedFileNode.name).thenReturn("File name")
        whenever(typedFileNode.id).thenReturn(nodeId)
        whenever(getNodeAccessPermission.invoke(nodeId)).thenReturn(AccessPermission.READ)
        whenever(getPreviewUseCase.invoke(any())).thenReturn(null)
        whenever(typedFileNode.thumbnailPath).thenReturn(null)
        whenever(typedFileNode.hasPreview).thenReturn(false)
        whenever(isAvailableOffline.invoke(any())).thenReturn(true)
        whenever(setNodeDescriptionUseCase(NodeId(any()), any())).thenReturn(Unit)
        whenever(getAvailableNodeActionsUseCase(any())).thenReturn(emptyList())
        whenever(getNodeOutSharesUseCase(nodeId)).thenReturn(emptyList())
        whenever(getContactVerificationWarningUseCase()).thenReturn(false)
        whenever(monitorOfflineFileAvailabilityUseCase()).thenReturn(emptyFlow())
        whenever(typedFileNode.type).thenReturn(
            StaticImageFileTypeInfo(
                mimeType = "image/jpg",
                extension = "jpg"
            )
        )
        whenever(monitorAccountDetailsUseCase()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that viewModel state's historyVersions property reflects the value of the getFileHistoryNumVersions use case after updating the node`() =
        runTest {
            for (n in 0..5) {
                whenever(typedFileNode.versionCount).thenReturn(n)
                underTest.setNode(node.handle, true)
                assertThat(underTest.uiState.value.historyVersions).isEqualTo(n)
            }
        }

    @Test
    fun `test that viewModel state's isNodeInBackups property reflects the value of the isNodeInBackups use case after updating the node`() =
        runTest {
            suspend fun verify(isNodeInBackups: Boolean) {
                whenever(isNodeInBackupsUseCase(NODE_HANDLE)).thenReturn(isNodeInBackups)
                underTest.setNode(node.handle, true)
                assertThat(underTest.uiState.value.isNodeInBackups).isEqualTo(isNodeInBackups)
            }
            verify(true)
            verify(false)
        }

    @Test
    fun `test that viewModel state's isNodeInRubbish property reflects the value of the isNodeInRubbish use case after updating the node`() =
        runTest {
            suspend fun verify(isNodeInRubbish: Boolean) {
                whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(isNodeInRubbish)
                underTest.setNode(node.handle, true)
                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.isNodeInRubbish).isEqualTo(isNodeInRubbish)
                }
            }
            verify(true)
            verify(false)
        }

    @Test
    fun `test showHistoryVersions is true if the node contains one version`() =
        runTest {
            whenever(typedFileNode.versionCount).thenReturn(1)
            underTest.setNode(node.handle, true)
            assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(true)
        }

    @Test
    fun `test showHistoryVersions is true if the node contains more than one version`() =
        runTest {
            whenever(typedFileNode.versionCount).thenReturn(2)
            underTest.setNode(node.handle, true)
            assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(true)
        }

    @Test
    fun `test showHistoryVersions is false if the node contains no versions`() =
        runTest {
            whenever(typedFileNode.versionCount).thenReturn(0)
            underTest.setNode(node.handle, true)
            assertThat(underTest.uiState.value.showHistoryVersions).isEqualTo(false)
        }

    @Test
    fun `test NotConnected event is launched if not connected while moving`() =
        runTest {
            whenever(isConnectedToInternetUseCase.invoke()).thenReturn(false)
            underTest.moveNodeCheckingCollisions(parentId)
            assertThat((underTest.uiState.value.oneOffViewEvent as? StateEventWithContentTriggered)?.content)
                .isEqualTo(FileInfoOneOffViewEvent.NotConnected)

        }

    @Test
    fun `test NotConnected event is launched if not connected while copying`() =
        runTest {
            whenever(isConnectedToInternetUseCase.invoke()).thenReturn(false)
            underTest.copyNodeCheckingCollisions(parentId)
            assertThat((underTest.uiState.value.oneOffViewEvent as? StateEventWithContentTriggered)?.content)
                .isEqualTo(FileInfoOneOffViewEvent.NotConnected)
        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while moving`() =
        runTest {
            mockCollisionMoving()
            underTest.setNode(node.handle, true)
            underTest.moveNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test CollisionDetected event is launched when a collision is found while copying`() =
        runTest {
            mockCollisionCopying()
            underTest.setNode(node.handle, true)
            underTest.copyNodeCheckingCollisions(parentId)
            testEventIsOfType(FileInfoOneOffViewEvent.CollisionDetected::class.java)
        }

    @Test
    fun `test FinishedMoving event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockMoveSuccess()
            underTest.setNode(node.handle, true)
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Moving::class.java)?.also {
                assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMoving event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockMoveFailure()
            underTest.setNode(node.handle, true)
            underTest.moveNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Moving::class.java)?.also {
                assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedMovingToRubbish event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockMoveToRubbishSuccess()
            underTest.setNode(node.handle, true)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.MovingToRubbish::class.java)?.also {
                assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedMovingToRubbish event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockMoveToRubbishFailure()
            underTest.setNode(node.handle, true)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.MovingToRubbish::class.java)?.also {
                assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedDeleting event is launched without exceptions when the delete finished successfully`() =
        runTest {
            mockDeleteSuccess()
            underTest.setNode(node.handle, true)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Deleting::class.java)?.also {
                assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedDeleting event is launched with the proper exceptions when the delete finished with an error`() =
        runTest {
            mockDeleteFailure()
            underTest.setNode(node.handle, true)
            underTest.removeNode()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Deleting::class.java)?.also {
                assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedCopying event is launched without exceptions when the move finished successfully`() =
        runTest {
            mockCopySuccess()
            underTest.setNode(node.handle, true)
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Copying::class.java)?.also {
                assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedCopying event is launched with the proper exceptions when the move finished with an error`() =
        runTest {
            mockCopyFailure()
            underTest.setNode(node.handle, true)
            underTest.copyNodeCheckingCollisions(parentId)
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.Copying::class.java)?.also {
                assertThat(it.exception).isNotNull()
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched without exceptions when the delete versions finished successfully`() =
        runTest {
            mockDeleteVersionsSuccess()
            underTest.setNode(node.handle, true)
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                assertThat(it.exception).isNull()
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched with the proper exceptions when the delete versions finished with an error`() =
        runTest {
            mockDeleteVersionsFailure(null)
            underTest.setNode(node.handle, true)
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                assertThat(it.exception).isNotNull()
                assertThat(it.exception)
                    .isNotInstanceOf(VersionsNotDeletedException::class.java)
            }
        }

    @Test
    fun `test FinishedDeletingVersions event is launched with the proper exceptions when the delete versions finished with some errors`() =
        runTest {
            val errors = 3
            val total = 5
            mockDeleteVersionsFailure(total, errors)
            underTest.setNode(node.handle, true)
            underTest.deleteHistoryVersions()
            testNextEventIsOfTypeFinishedAndJobIsOfType(FileInfoJobInProgressState.DeletingVersions::class.java)?.also {
                assertThat(it.exception).isNotNull()
                assertThat(it.exception).isInstanceOf(VersionsNotDeletedException::class.java)
                (it.exception as? VersionsNotDeletedException)?.let { exception ->
                    assertThat(exception.totalNotDeleted).isEqualTo(errors)
                    assertThat(exception.totalRequestedToDelete).isEqualTo(total)
                }
            }
        }

    @Test
    fun `test that NodeDescriptionAdded event is triggered when setting a new non empty description`() =
        runTest {
            whenever(setNodeDescriptionUseCase(NodeId(any()), any())).thenReturn(Unit)
            underTest.setNode(node.handle, true)
            val newDescription = "new description"
            underTest.setNodeDescription(newDescription)
            testEventIsOfType(FileInfoOneOffViewEvent.Message.NodeDescriptionAdded::class.java)
            assertThat(underTest.uiState.value.descriptionText).isEqualTo(newDescription)
        }

    @Test
    fun `test that NodeDescriptionUpdated event is triggered when updating a non empty description`() =
        runTest {
            whenever(setNodeDescriptionUseCase(NodeId(any()), any())).thenReturn(Unit)
            underTest.setNode(node.handle, true)
            val initialDescription = "initial description"
            underTest.setNodeDescription(initialDescription)
            val updatedDescription = "updated description"
            underTest.setNodeDescription(updatedDescription)
            testEventIsOfType(FileInfoOneOffViewEvent.Message.NodeDescriptionUpdated::class.java)
            assertThat(underTest.uiState.value.descriptionText).isEqualTo(updatedDescription)
        }

    @Test
    fun `test that no OneOffEvent is triggered when not changing an empty description`() =
        runTest {
            whenever(setNodeDescriptionUseCase(NodeId(any()), any())).thenReturn(Unit)
            underTest.setNode(node.handle, true)
            underTest.setNodeDescription("")
            assertThat(underTest.uiState.value.oneOffViewEvent).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
            assertThat(underTest.uiState.value.descriptionText).isEqualTo("")
        }

    @Test
    fun `test that NodeDescriptionUpdated event is triggered when setting the same description`() =
        runTest {
            whenever(setNodeDescriptionUseCase(NodeId(any()), any())).thenReturn(Unit)
            underTest.setNode(node.handle, true)
            val sameDescription = "same description"
            underTest.setNodeDescription(sameDescription)
            underTest.setNodeDescription(sameDescription)
            testEventIsOfType(FileInfoOneOffViewEvent.Message.NodeDescriptionUpdated::class.java)
            assertThat(underTest.uiState.value.descriptionText).isEqualTo(sameDescription)
        }

    @Test
    fun `test that no OneOffEvent is triggered when setting node description fails`() =
        runTest {
            whenever(setNodeDescriptionUseCase(NodeId(any()), any())).thenThrow(RuntimeException())
            underTest.setNode(node.handle, true)
            val description = "a description"
            underTest.setNodeDescription(description)
            assertThat(underTest.uiState.value.oneOffViewEvent).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying successfully, and unset at the end`() =
        runTest {
            mockCopySuccess()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying with an error, and unset at the end`() =
        runTest {
            mockCopyFailure()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while copying and Name conflict found, and unset at the end`() =
        runTest {
            mockCollisionCopying()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileCopyingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving successfully, and unset at the end`() =
        runTest {
            mockMoveSuccess()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving with an error, and unset at the end`() =
        runTest {
            mockMoveFailure()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving and Name conflict found, and unset at the end`() =
        runTest {
            mockCollisionMoving()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileMovingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving to rubbish successfully, and unset at the end`() =
        runTest {
            mockMoveToRubbishSuccess()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileMovingToRubbishBinAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while moving to rubbish with an error, and unset at the end`() =
        runTest {
            mockMoveToRubbishFailure()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileMovingToRubbishBinAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting successfully, and unset at the end`() =
        runTest {
            mockDeleteSuccess()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileDeletingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting with an error, and unset at the end`() =
        runTest {
            mockDeleteSuccess()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileDeletingAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting versions successfully, and unset at the end`() =
        runTest {
            mockDeleteVersionsSuccess()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileDeletingVersionsAndUnset()
        }

    @Test
    fun `test FileInfoJobInProgressState is set while deleting versions with an error, and unset at the end`() =
        runTest {
            mockDeleteVersionsFailure()
            underTest.setNode(node.handle, true)
            testProgressIsSetWhileDeletingVersionsAndUnset()
        }


    @Test
    fun `test on-off event is removed from state once is consumed`() {
        `test CollisionDetected event is launched when a collision is found while copying`()
        runTest {
            assertThat((underTest.uiState.value.oneOffViewEvent as? StateEventWithContentTriggered)?.content)
                .isNotNull()
            underTest.consumeOneOffEvent()
            assertThat((underTest.uiState.value.oneOffViewEvent as? StateEventWithContentTriggered)?.content)
                .isNull()
        }
    }

    @Test
    fun `test preview is assigned when node is updated`() = runTest {
        whenever(typedFileNode.hasPreview).thenReturn(true)
        whenever(getPreviewUseCase.invoke(typedFileNode)).thenReturn(previewFile)
        whenever(typedFileNode.thumbnailPath).thenReturn(null)
        underTest.setNode(node.handle, true)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(PREVIEW_URI)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test exception from getPreview is not propagated`() = runTest {
        whenever(typedFileNode.hasPreview).thenReturn(true)
        whenever(getPreviewUseCase.invoke(typedFileNode)).thenAnswer {
            throw MegaException(
                -5,
                "Failed Permanently"
            )
        }
        whenever(typedFileNode.thumbnailPath).thenReturn(null)
        underTest.setNode(node.handle, true)
        underTest.uiState.test {
            assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
        }
    }

    @Test
    fun `test thumbnail is assigned when node is updated and there are no preview`() = runTest {
        whenever(typedFileNode.hasPreview).thenReturn(false)
        whenever(typedFileNode.thumbnailPath).thenReturn(THUMB_URI)
        underTest.setNode(node.handle, true)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            assertThat(state).isEqualTo("file:$THUMB_URI")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test preview has priority over thumbnail`() = runTest {
        whenever(getPreviewUseCase.invoke(typedFileNode)).thenReturn(previewFile)
        whenever(typedFileNode.thumbnailPath).thenReturn(THUMB_URI)
        whenever(typedFileNode.hasPreview).thenReturn(true)
        underTest.setNode(node.handle, true)
        underTest.uiState.mapNotNull { it.actualPreviewUriString }.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(PREVIEW_URI)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test getContactItemFromInShareFolder is invoked when the node is a Folder`() = runTest {
        val folderNode = mockFolder()
        underTest.setNode(node.handle, true)
        //first quick fetch
        verify(getContactItemFromInShareFolder).invoke(folderNode, false)
        //second not cached slow fetch
        verify(getContactItemFromInShareFolder).invoke(folderNode, true)
    }

    @Test
    fun `test monitorNodeUpdatesById updates owner`() = runTest {
        val folderNode = mockFolder()
        whenever(monitorNodeUpdatesById.invoke(folderNode.id)).thenReturn(
            flowOf(listOf(NodeChanges.Owner))
        )
        underTest.setNode(node.handle, true)
        //check 2 invocations: first invocation when node is set, second one the update itself
        verify(getContactItemFromInShareFolder, times(2)).invoke(folderNode, true)
    }

    @ParameterizedTest
    @MethodSource("provideNodeChanges")
    fun `test monitorNodeUpdatesById updates tag`(nodeChanges: NodeChanges) = runTest {
        val folderNode = mockFolder()
        whenever(monitorNodeUpdatesById.invoke(folderNode.id)).thenReturn(
            flowOf(listOf(nodeChanges))
        )
        underTest.setNode(node.handle, true)
        //check 2 invocations: first invocation when node is set, second one the update itself
        verify(getNodeByIdUseCase, times(2)).invoke(folderNode.id)
    }

    @Test
    fun `test getNodeAccessPermission is fetched if getContactItemFromInShareFolder returns ContactItem`() =
        runTest {
            val expected = AccessPermission.FULL
            val folderNode = mockFolder()
            whenever(getContactItemFromInShareFolder.invoke(folderNode, false)).thenReturn(mock())
            whenever(getNodeAccessPermission.invoke(folderNode.id)).thenReturn(expected)
            underTest.setNode(folderNode.id.longValue)
            assertThat(underTest.uiState.value.accessPermission)
                .isEqualTo(expected)
            verify(getNodeAccessPermission, times(1)).invoke(nodeId)
        }

    @Test
    fun `test getNodeAccessPermission is fetched again if monitorNodeUpdatesById updates In share`() =
        runTest {
            val expectedFirst = AccessPermission.FULL
            val expectedChanged = AccessPermission.READ
            val folderNode = mockFolder()
            whenever(getContactItemFromInShareFolder.invoke(folderNode, false)).thenReturn(mock())
            whenever(getNodeAccessPermission.invoke(nodeId))
                .thenReturn(expectedFirst)
                .thenReturn(expectedChanged)
            whenever(monitorNodeUpdatesById.invoke(nodeId)).thenReturn(
                flowOf(listOf(NodeChanges.Inshare))
            )
            underTest.setNode(node.handle, true)
            assertThat(underTest.uiState.value.accessPermission)
                .isEqualTo(expectedChanged)
            verify(getNodeAccessPermission, times(2)).invoke(nodeId)
        }

    @Test
    fun `test getNodeLocationInfo fetches node location when its set`() = runTest {
        underTest.setNode(node.handle, true)
        verify(getNodeLocationInfo, times(1)).invoke(typedFileNode)
    }

    @Test
    fun `test getNodeLocationInfo fetches node location when its parent is updated`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId))
            .thenReturn(
                flowOf(listOf(NodeChanges.Parent))
            )
            .thenReturn(emptyFlow()) //second time we don't want to emit another update to avoid a circular call in this test
        underTest.setNode(node.handle, true)
        verify(getNodeLocationInfo, times(2)).invoke(typedFileNode)
    }

    @Test
    fun `test getOutShares result is set on uiState`() = runTest {
        val expected = mock<List<ContactPermission>>()
        whenever(getNodeOutSharesUseCase.invoke(nodeId)).thenReturn(expected)
        underTest.setNode(node.handle, true)
        assertThat(underTest.uiState.value.outShares).isEqualTo(expected)
    }

    @Test
    fun `test getOutShares is fetched when contacts update is received and there are out shares`() =
        runTest {
            val expected = mock<List<ContactPermission>>()
            whenever(getNodeOutSharesUseCase.invoke(nodeId)).thenReturn(expected)
            val updateChanges = mapOf(Pair(UserId(1L), listOf(UserChanges.Alias)))
            val update = mock<UserUpdate> {
                on { changes }.thenReturn(updateChanges)
            }
            whenever(monitorContactUpdates.invoke()).thenReturn(flowOf(update))
            underTest.setNode(node.handle, true)
            verify(getNodeOutSharesUseCase, times(2)).invoke(nodeId)
        }

    @Test
    fun `test isAvailableOffline result is set on uiState`() = runTest {
        val expected = true
        whenever(isAvailableOffline.invoke(typedFileNode)).thenReturn(expected)
        underTest.setNode(node.handle, true)
        assertThat(underTest.uiState.value.isAvailableOffline).isEqualTo(expected)
    }

    @Test
    fun `test availableOfflineChanged does not start download if storage state is pay wall`() =
        runTest {
            mockMonitorStorageStateEvent(StorageState.PayWall)
            whenever(isAvailableOffline.invoke(typedFileNode)).thenReturn(false)
            underTest.setNode(node.handle, true)
            underTest.availableOfflineChanged(true)
            verifyNoInteractions(removeOfflineNodeUseCase)
        }

    @Test
    fun `test when stopSharing is called then disableExport use case is called`() =
        runTest {
            underTest.setNode(node.handle, true)
            underTest.stopSharing()
            verify(stopSharingNode, times(1)).invoke(nodeId)
        }

    @Test
    fun `test when setSharePermissionForUsers for a set of users then SetSharePermission use case is called`() =
        runTest {
            val emails = List(5) { "email$it@example.com" }
            val folderNode = mockFolder()
            underTest.setNode(folderNode.id.longValue)
            underTest.setSharePermissionForUsers(AccessPermission.READ, emails)

            verify(setOutgoingPermissions, times(1))
                .invoke(folderNode, AccessPermission.READ, *emails.toTypedArray())
        }

    @Test
    fun `test when removeSharePermissionForUsers for a set of users then SetSharePermission use case is called`() =
        runTest {
            val emails = Array(5) { "email$it@example.com" }
            val folderNode = mockFolder()
            underTest.setNode(folderNode.id.longValue)
            underTest.removeSharePermissionForUsers(*emails)

            verify(setOutgoingPermissions, times(1))
                .invoke(folderNode, AccessPermission.UNKNOWN, *emails)
        }

    @Test
    fun `test that clipboard gateway is called with the correct link`() = runTest {
        val link = "https://megalink"
        val exportedData = ExportedData(link, 100L)
        whenever(typedFileNode.exportedData).thenReturn(exportedData)
        underTest.setNode(node.handle, true)
        underTest.copyPublicLink()
        verify(clipboardGateway, times(1)).setClip(Constants.COPIED_TEXT_LABEL, link)
    }

    @Test
    fun `test that when initiate remove node on a node in the rubbish bin then the Delete confirmation action is set`() =
        runTest {
            underTest.setNode(node.handle, true)
            underTest.initiateRemoveNode(false)
            underTest.uiState.mapNotNull { it.requiredExtraAction }.test {
                assertThat(awaitItem()).isEqualTo(FileInfoExtraAction.ConfirmRemove.Delete)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when initiate remove node on a node not in the rubbish bin then the SendToRubbish confirmation action is set`() =
        runTest {
            whenever(getPrimarySyncHandleUseCase()).thenReturn(-1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(-1)
            underTest.setNode(node.handle, true)
            underTest.initiateRemoveNode(true)
            underTest.uiState.mapNotNull { it.requiredExtraAction }.test {
                assertThat(awaitItem())
                    .isEqualTo(FileInfoExtraAction.ConfirmRemove.SendToRubbish)
                cancelAndIgnoreRemainingEvents()
            }
        }


    @Test
    fun `test that when initiate remove node on the primary camera upload folder then the SendToRubbishCameraUploads confirmation action is set`() =
        runTest {
            whenever(getPrimarySyncHandleUseCase()).thenReturn(nodeId.longValue)
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            underTest.setNode(node.handle, true)
            underTest.initiateRemoveNode(true)
            underTest.uiState.mapNotNull { it.requiredExtraAction }.test {
                assertThat(awaitItem())
                    .isEqualTo(FileInfoExtraAction.ConfirmRemove.SendToRubbishCameraUploads)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when initiate remove node on the secondary camera upload folder then the SendToRubbishSecondaryMediaUploads confirmation action is set`() =
        runTest {
            whenever(getPrimarySyncHandleUseCase()).thenReturn(-1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(nodeId.longValue)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            underTest.setNode(node.handle, true)
            underTest.initiateRemoveNode(true)
            underTest.uiState.mapNotNull { it.requiredExtraAction }.test {
                assertThat(awaitItem())
                    .isEqualTo(FileInfoExtraAction.ConfirmRemove.SendToRubbishSecondaryMediaUploads)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that an exception from get folder tree info is not propagated`() = runTest {
        val folderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(nodeId)
            on { name }.thenReturn("Folder name")
        }.also { folderNode ->
            whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(folderNode)
            whenever(getFolderTreeInfo.invoke(folderNode))
                .thenAnswer { throw MegaException(1, "It's broken") }
            whenever(getContactItemFromInShareFolder.invoke(any(), any())).thenReturn(mock())
        }

        with(underTest) {
            setNode(folderNode.id.longValue)
            uiState.test {
                assertNull(awaitItem().folderTreeInfo)
            }
        }
    }

    @Test
    fun `test that when folder tree info is received then total size, folder content and available offline is updated correctly`() =
        runTest {
            val actualSize = 1024L
            val versionsSize = 512L
            val folderTreeInfo = FolderTreeInfo(1, 2, actualSize, 1, versionsSize)
            val folderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
                on { name }.thenReturn("Folder name")
            }.also { folderNode ->
                whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(folderNode)
                whenever(getFolderTreeInfo.invoke(folderNode))
                    .thenReturn(folderTreeInfo)
                whenever(getContactItemFromInShareFolder.invoke(any(), any())).thenReturn(mock())
            }
            underTest.setNode(folderNode.id.longValue)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.folderTreeInfo).isEqualTo(folderTreeInfo)
                assertThat(actual.isAvailableOfflineAvailable).isTrue()
                assertThat(actual.sizeInBytes)
                    .isEqualTo(actualSize + versionsSize)
            }
        }

    @Test
    fun `test that start download node event is launched when start download node is triggered`() =
        runTest {
            underTest.setNode(node.handle, true)
            underTest.startDownloadNode()
            assertThat((underTest.uiState.value.downloadEvent as? StateEventWithContentTriggered)?.content)
                .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
        }

    @Test
    fun `test that start download node for offline event is launched when download worker feature flag is enabled`() =
        runTest {
            mockMonitorStorageStateEvent(StorageState.Green)
            whenever(isAvailableOffline(any())).thenReturn(false)
            underTest.setNode(node.handle, true)
            underTest.availableOfflineChanged(true)
            assertThat((underTest.uiState.value.downloadEvent as? StateEventWithContentTriggered)?.content)
                .isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
        }

    @Test
    fun `test that removeOfflineNodeUseCase is invoked when availableOfflineChanged is set to false`() =
        runTest {
            mockMonitorStorageStateEvent(StorageState.Green)
            whenever(isAvailableOffline(any())).thenReturn(true)
            underTest.setNode(node.handle, true)
            underTest.availableOfflineChanged(false)

            verify(removeOfflineNodeUseCase).invoke(nodeId)
        }

    private fun mockMonitorStorageStateEvent(state: StorageState) {
        val storageStateEvent = StorageStateEvent(
            1L, "", 1L, "", EventType.Storage,
            state,
        )
        whenever(monitorStorageStateEventUseCase.invoke()).thenReturn(
            MutableStateFlow(storageStateEvent)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : FileInfoOneOffViewEvent> testEventIsOfType(
        clazz: Class<T>,
    ): T? {
        return getEvent().also {
            assertThat(it).isInstanceOf(clazz)
        } as? T?
    }

    private suspend fun <T : FileInfoJobInProgressState> testNextEventIsOfTypeFinishedAndJobIsOfType(
        finishedJobClass: Class<T>,
    ): FileInfoOneOffViewEvent.Finished? {
        return getEvent().also {
            assertThat(it).isInstanceOf(FileInfoOneOffViewEvent.Finished::class.java)
            val jobFinished = (it as FileInfoOneOffViewEvent.Finished).jobFinished
            assertThat(jobFinished).isInstanceOf(finishedJobClass)
        } as? FileInfoOneOffViewEvent.Finished?
    }

    private suspend fun getEvent(): FileInfoOneOffViewEvent =
        (underTest.uiState.value.oneOffViewEvent as? StateEventWithContentTriggered)?.content
            ?: (underTest.uiState.mapNotNull { it.oneOffViewEvent }
                .first() as StateEventWithContentTriggered).content


    private suspend fun mockCollisionCopying() {
        whenever(
            checkNodesNameCollisionWithActionUseCase(
                mapOf(nodeId.longValue to parentId.longValue),
                NodeNameCollisionType.COPY
            )
        ).thenReturn(
            NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = mapOf(
                        nodeId.longValue to NodeNameCollision.Default(
                            collisionHandle = 123L,
                            nodeHandle = 456L,
                            name = "name",
                            size = 789L,
                            childFolderCount = 0,
                            childFileCount = 0,
                            lastModified = 123456L,
                            parentHandle = 789L,
                            isFile = true
                        )
                    ),
                    type = NodeNameCollisionType.COPY
                ),
                moveRequestResult = null
            )
        )
    }

    private suspend fun mockCollisionMoving() {
        whenever(
            checkNodesNameCollisionWithActionUseCase(
                mapOf(nodeId.longValue to parentId.longValue),
                NodeNameCollisionType.MOVE
            )
        ).thenReturn(
            NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = mapOf(
                        nodeId.longValue to NodeNameCollision.Default(
                            collisionHandle = 123L,
                            nodeHandle = 456L,
                            name = "name",
                            size = 789L,
                            childFolderCount = 0,
                            childFileCount = 0,
                            lastModified = 123456L,
                            parentHandle = 789L,
                            isFile = true
                        )
                    ),
                    type = NodeNameCollisionType.MOVE
                ),
                moveRequestResult = null
            )
        )
    }

    private suspend fun mockCopySuccess() {
        whenever(
            checkNodesNameCollisionWithActionUseCase(
                mapOf(nodeId.longValue to parentId.longValue),
                NodeNameCollisionType.COPY
            )
        ).thenReturn(
            NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.COPY
                ),
                moveRequestResult = MoveRequestResult.Copy(
                    count = 1,
                    errorCount = 0
                )
            )
        )
    }

    private suspend fun mockMoveSuccess() {
        whenever(
            checkNodesNameCollisionWithActionUseCase(
                mapOf(nodeId.longValue to parentId.longValue),
                NodeNameCollisionType.MOVE
            )
        ).thenReturn(
            NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.MOVE
                ),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
        )
    }

    private suspend fun mockCopyFailure() {
        whenever(
            checkNodesNameCollisionWithActionUseCase(
                mapOf(nodeId.longValue to parentId.longValue),
                NodeNameCollisionType.COPY
            )
        ).thenReturn(
            NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.COPY
                ),
                moveRequestResult = MoveRequestResult.Copy(
                    count = 1,
                    errorCount = 1
                )
            )
        )
    }

    private suspend fun mockMoveFailure() {
        whenever(
            checkNodesNameCollisionWithActionUseCase(
                mapOf(nodeId.longValue to parentId.longValue),
                NodeNameCollisionType.MOVE
            )
        ).thenReturn(
            NodeNameCollisionWithActionResult(
                collisionResult = NodeNameCollisionsResult(
                    noConflictNodes = emptyMap(),
                    conflictNodes = emptyMap(),
                    type = NodeNameCollisionType.MOVE
                ),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
        )
    }

    private suspend fun mockMoveToRubbishSuccess() {
        whenever(moveNodeToRubbishBinUseCase.invoke(nodeId)).thenReturn(Unit)
    }

    private suspend fun mockMoveToRubbishFailure() {
        whenever(moveNodeToRubbishBinUseCase.invoke(nodeId)).thenThrow(RuntimeException("fake exception"))
    }

    private suspend fun mockDeleteSuccess() {
        whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(true)
        underTest.setNode(node.handle, true)
        whenever(deleteNodeByHandleUseCase.invoke(nodeId)).thenReturn(Unit)
    }

    private suspend fun mockDeleteVersionsSuccess() {
        whenever(deleteNodeVersionsUseCase.invoke(nodeId)).thenReturn(Unit)
    }

    private suspend fun mockDeleteVersionsFailure(
        totalRequested: Int? = null,
        totalFailure: Int? = null,
    ) {
        whenever(deleteNodeVersionsUseCase.invoke(nodeId)).thenThrow(
            if (totalFailure == null) {
                RuntimeException("fake exception")
            } else {
                VersionsNotDeletedException(totalRequested ?: (totalFailure + 1), totalFailure)
            }
        )
    }

    private suspend fun mockDeleteFailure() {
        whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(true)
        underTest.setNode(node.handle, true)
        whenever(deleteNodeByHandleUseCase.invoke(nodeId)).thenThrow(RuntimeException("fake exception"))
    }

    private fun testProgressIsSetWhileCopyingAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.Copying) {
            underTest.copyNodeCheckingCollisions(parentId)
        }

    private fun testProgressIsSetWhileMovingAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.Moving) {
            underTest.moveNodeCheckingCollisions(parentId)
        }

    private fun testProgressIsSetWhileMovingToRubbishBinAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.MovingToRubbish) {
            underTest.removeNode()
        }

    private fun testProgressIsSetWhileDeletingAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.Deleting) {
            underTest.removeNode()
        }

    private fun testProgressIsSetWhileDeletingVersionsAndUnset() =
        testProgressSetAndUnset(FileInfoJobInProgressState.DeletingVersions) {
            underTest.deleteHistoryVersions()
        }

    private fun testProgressSetAndUnset(
        progress: FileInfoJobInProgressState,
        block: () -> Unit,
    ) = runTest {
        underTest.uiState.map { it.jobInProgressState }.distinctUntilChanged().test {
            assertThat(awaitItem()).isNull()
            block()
            assertThat(awaitItem()).isEqualTo(progress)
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun provideNodeChanges() = Stream.of(
        Arguments.of(NodeChanges.Description),
        Arguments.of(NodeChanges.Name),
        Arguments.of(NodeChanges.Tags),
        Arguments.of(NodeChanges.Timestamp),
    )

    private suspend fun mockFolder() = mock<TypedFolderNode> {
        on { id }.thenReturn(nodeId)
        on { name }.thenReturn("Folder name")
    }.also { folderNode ->
        whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(folderNode)
        whenever(getFolderTreeInfo.invoke(folderNode)).thenReturn(mock())
        whenever(getContactItemFromInShareFolder.invoke(any(), any())).thenReturn(mock())
    }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
        private const val THUMB_URI = "/thumb"
        private const val PREVIEW_URI = "/preview"
    }
}
