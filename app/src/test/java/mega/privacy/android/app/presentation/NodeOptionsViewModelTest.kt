package mega.privacy.android.app.presentation

import mega.privacy.android.core.R as CoreR
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeDeviceCenterInformation
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactUserNameFromDatabaseUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeSyncedUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

/**
 * Test class for [NodeOptionsViewModel]
 */
@ExperimentalCoroutinesApi
class NodeOptionsViewModelTest {

    private lateinit var underTest: NodeOptionsViewModel
    private val getNodeByHandle =
        mock<GetNodeByHandle> { onBlocking { invoke(any()) }.thenReturn(null) }
    private val createShareKeyUseCase = mock<CreateShareKeyUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val isNodeDeletedFromBackupsUseCase = mock<IsNodeDeletedFromBackupsUseCase> {
        onBlocking { invoke(NodeId(any())) }.thenReturn(false)
    }
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(true)
        )
    }

    private val removeOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val getContactUserNameFromDatabaseUseCase =
        mock<GetContactUserNameFromDatabaseUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }
    private val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
        onBlocking {
            invoke(NodeId(any()))
        }.thenReturn(false)
    }
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase> {
        onBlocking {
            invoke(any())
        }.thenReturn(true)
    }

    private val nodeIdFlow = MutableStateFlow(-1L)

    private val nodeShareInformationFlow = MutableStateFlow<NodeShareInformation?>(null)

    private val nodeDeviceCenterInformationFlow =
        MutableStateFlow<NodeDeviceCenterInformation?>(null)

    private val savedStateHandle = mock<SavedStateHandle> {
        on {
            getStateFlow(
                NodeOptionsViewModel.NODE_ID_KEY,
                -1L
            )
        }.thenReturn(nodeIdFlow)

        on {
            getStateFlow<NodeShareInformation?>(NodeOptionsViewModel.SHARE_DATA_KEY, null)
        }.thenReturn(nodeShareInformationFlow)

        on {
            getStateFlow<NodeDeviceCenterInformation?>(
                NodeOptionsViewModel.NODE_DEVICE_CENTER_INFORMATION_KEY,
                null,
            )
        }.thenReturn(nodeDeviceCenterInformationFlow)
    }
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getCameraUploadsFolderHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getMediaUploadsFolderHandleUseCase = mock<GetSecondaryFolderNodeUseCase>()
    private val getMyChatsFilesFolderIdUseCase = mock<GetMyChatsFilesFolderIdUseCase>()
    private val isNodeSyncedUseCase = mock<IsNodeSyncedUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = NodeOptionsViewModel(
            createShareKeyUseCase = createShareKeyUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodeByHandle = getNodeByHandle,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase,
            getContactUserNameFromDatabaseUseCase = getContactUserNameFromDatabaseUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            isHidingActionAllowedUseCase = isHidingActionAllowedUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getCameraUploadsFolderHandleUseCase = getCameraUploadsFolderHandleUseCase,
            getMediaUploadsFolderHandleUseCase = getMediaUploadsFolderHandleUseCase,
            getMyChatsFilesFolderIdUseCase = getMyChatsFilesFolderIdUseCase,
            isNodeSyncedUseCase = isNodeSyncedUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.canMoveNode).isFalse()
            assertThat(initial.canRestoreNode).isFalse()
            assertThat(initial.isOnline).isTrue()
            assertThat(initial.node).isNull()
            assertThat(initial.shareData).isNull()
            assertThat(initial.nodeDeviceCenterInformation).isNull()
            assertThat(initial.shareKeyCreated).isNull()
            assertThat(initial.isAvailableOffline).isFalse()
            assertThat(initial.isUserAttributeFolder).isFalse()
            assertThat(initial.isSyncedFolder).isFalse()
            assertThat(initial.isSyncActionAllowed).isFalse()
        }
    }

    @Test
    fun `test that shareKeyCreated is true if created successfully`() = runTest {
        val node = mock<MegaNode>()
        val nodeId = 123L
        getNodeByHandle.stub {
            onBlocking { invoke(nodeId) }.thenReturn(node)
        }
        val typedNode = mock<TypedFolderNode>()
        whenever(getNodeByIdUseCase.invoke(NodeId(any()))).thenReturn(typedNode)
        nodeIdFlow.emit(nodeId)

        underTest.state.filter { it.node != null }
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                assertThat(awaitItem().shareKeyCreated).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isTrue()
            }
    }

    @Test
    fun `test that shareKeyCreated is false if created throws an exception`() = runTest {
        val node = mock<MegaNode>()
        val nodeId = 123L
        getNodeByHandle.stub {
            onBlocking { invoke(nodeId) }.thenReturn(node)
        }
        nodeIdFlow.emit(nodeId)

        createShareKeyUseCase.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }
        underTest.state.filter { it.node != null }
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                assertThat(awaitItem().shareKeyCreated).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isFalse()
            }
    }

    @Test
    fun `test that shareKeyCreated is false if called with null node`() = runTest {
        createShareKeyUseCase.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }

        underTest.state
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                val initialState = awaitItem()
                assertThat(initialState.shareKeyCreated).isNull()
                assertThat(initialState.node).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isFalse()
            }
    }

    @Test
    fun `test that share info is returned if found`() = runTest {
        val expectedShareInformation = NodeShareInformation(
            user = null,
            isPending = false,
            isVerified = false
        )

        underTest.state.test {
            assertThat(awaitItem().shareData).isNull()
            nodeShareInformationFlow.emit(expectedShareInformation)
            assertThat(awaitItem().shareData).isEqualTo(expectedShareInformation)
        }
    }

    @Test
    fun `test that the node device center information is returned if found`() = runTest {
        val expectedNodeDeviceCenterInformation = NodeDeviceCenterInformation(
            name = "Device Center Node",
            status = "Up to date",
            icon = CoreR.drawable.ic_check_circle,
            isBackupsFolder = true,
        )

        underTest.state.test {
            assertThat(awaitItem().nodeDeviceCenterInformation).isNull()
            nodeDeviceCenterInformationFlow.emit(expectedNodeDeviceCenterInformation)
            assertThat(awaitItem().nodeDeviceCenterInformation).isEqualTo(
                expectedNodeDeviceCenterInformation
            )
        }
    }

    @Test
    fun `test that the restore node functionality works as expected for deleted non-backup nodes`() =
        runTest {
            val node = mock<MegaNode>()
            val nodeId = 123L
            getNodeByHandle.stub {
                onBlocking { invoke(nodeId) }.thenReturn(node)
            }
            isNodeDeletedFromBackupsUseCase.stub {
                onBlocking { invoke(NodeId(any())) }.thenReturn(false)
            }

            underTest.state.test {
                assertThat(awaitItem().node).isNull()
                nodeIdFlow.emit(nodeId)
                assertThat(awaitItem().node).isEqualTo(node)
                underTest.setRestoreNodeClicked(true)
                assertThat(awaitItem().canRestoreNode).isTrue()
            }
        }

    @Test
    fun `test that the restore node functionality becomes the move node functionality for deleted backup nodes`() =
        runTest {
            val node = mock<MegaNode>()
            val nodeId = 123L
            getNodeByHandle.stub {
                onBlocking { invoke(nodeId) }.thenReturn(node)
            }
            isNodeDeletedFromBackupsUseCase.stub {
                onBlocking { invoke(NodeId(any())) }.thenReturn(true)
            }

            underTest.state.test {
                assertThat(awaitItem().node).isNull()
                nodeIdFlow.emit(nodeId)
                assertThat(awaitItem().node).isEqualTo(node)
                underTest.setRestoreNodeClicked(true)
                assertThat(awaitItem().canMoveNode).isTrue()
            }
        }

    @Test
    fun `test that the unverified outgoing node user name is null`() = runTest {
        underTest.state.test {
            assertThat(awaitItem().shareData).isNull()
            assertThat(underTest.getUnverifiedOutgoingNodeUserName()).isNull()
        }
    }

    @Test
    fun `test that the unverified outgoing node user name is retrieved`() = runTest {
        val nodeShareInformation = NodeShareInformation(
            user = "Test User",
            isPending = false,
            isVerified = true,
        )
        val expectedUserNameFromDatabase = "Test User Name from Database"
        underTest.state.test {
            assertThat(awaitItem().shareData).isNull()
            nodeShareInformationFlow.emit(nodeShareInformation)
            assertThat(awaitItem().shareData).isEqualTo(nodeShareInformation)

            whenever(getContactUserNameFromDatabaseUseCase(any())).thenReturn(
                expectedUserNameFromDatabase
            )
            assertThat(underTest.getUnverifiedOutgoingNodeUserName()).isEqualTo(
                expectedUserNameFromDatabase
            )
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}