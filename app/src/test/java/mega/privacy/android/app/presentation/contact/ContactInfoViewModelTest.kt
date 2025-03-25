package mega.privacy.android.app.presentation.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.presentation.contactinfo.ContactInfoViewModel
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.call.IsChatConnectedToInitiateCallUseCase
import mega.privacy.android.domain.usecase.call.OpenOrStartCallUseCase
import mega.privacy.android.domain.usecase.chat.CreateChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.ApplyContactUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.contact.SetUserAliasUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.domain.usecase.shares.GetInSharesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.random.Random
import kotlin.test.assertFalse

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactInfoViewModelTest {
    private var underTest: ContactInfoViewModel = mock()
    private var monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private var isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private var setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private var chatManagement: ChatManagement = mock()
    private var monitorContactUpdates: MonitorContactUpdates = mock()
    private var getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase = mock()
    private var requestUserLastGreenUseCase: RequestUserLastGreenUseCase = mock()
    private var getChatRoomUseCase: GetChatRoomUseCase = mock()
    private var getContactFromEmailUseCase: GetContactFromEmailUseCase = mock()
    private var getContactFromChatUseCase: GetContactFromChatUseCase = mock()
    private var getChatRoomByUserUseCase: GetChatRoomByUserUseCase = mock()
    private var applyContactUpdatesUseCase: ApplyContactUpdatesUseCase = mock()
    private var setUserAliasUseCase: SetUserAliasUseCase = mock()
    private var removeContactByEmailUseCase: RemoveContactByEmailUseCase = mock()
    private var getInSharesUseCase: GetInSharesUseCase = mock()
    private var monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private var monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase = mock()
    private var monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase =
        mock()
    private var startConversationUseCase: StartConversationUseCase = mock()
    private var createChatRoomUseCase: CreateChatRoomUseCase = mock()
    private var monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase = mock()
    private var monitorChatOnlineStatusUseCase: MonitorChatOnlineStatusUseCase = mock()
    private var monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase =
        mock()
    private var isChatConnectedToInitiateCallUseCase: IsChatConnectedToInitiateCallUseCase = mock()
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase> {
        on { invoke() }.thenReturn(monitorNodeUpdatesFakeFlow)
    }
    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()
    private var createShareKeyUseCase: CreateShareKeyUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private var checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val copyNodesUseCase: CopyNodesUseCase = mock()
    private var openOrStartCallUseCase: OpenOrStartCallUseCase = mock()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)
    private val testScope = CoroutineScope(UnconfinedTestDispatcher())
    private val testHandle = 123456L
    private val testEmail = "test@gmail.com"
    private val contactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
    )
    private val contactItem = ContactItem(
        handle = testHandle,
        email = "test@gmail.com",
        contactData = contactData,
        defaultAvatarColor = "red",
        visibility = UserVisibility.Visible,
        timestamp = 123456789,
        areCredentialsVerified = true,
        status = UserChatStatus.Online,
        lastSeen = 0,
        chatroomId = null,
    )
    private val chatRoom = mock<ChatRoom> {
        on { chatId }.thenReturn(123456L)
        on { changes }.thenReturn(null)
        on { title }.thenReturn("Chat title")
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            setChatVideoInDeviceUseCase,
            chatManagement,
            monitorContactUpdates,
            getUserOnlineStatusByHandleUseCase,
            requestUserLastGreenUseCase,
            getChatRoomUseCase,
            getContactFromEmailUseCase,
            getContactFromChatUseCase,
            getChatRoomByUserUseCase,
            applyContactUpdatesUseCase,
            setUserAliasUseCase,
            removeContactByEmailUseCase,
            getInSharesUseCase,
            monitorChatCallUpdatesUseCase,
            monitorChatSessionUpdatesUseCase,
            monitorUpdatePushNotificationSettingsUseCase,
            startConversationUseCase,
            createChatRoomUseCase,
            monitorChatConnectionStateUseCase,
            monitorChatOnlineStatusUseCase,
            monitorChatPresenceLastGreenUpdatesUseCase,
            isChatConnectedToInitiateCallUseCase,
            createShareKeyUseCase,
            checkNodesNameCollisionUseCase,
            copyNodesUseCase,
            openOrStartCallUseCase,
        )
    }

    private suspend fun initViewModel() {
        stubCommon()
        underTest = ContactInfoViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            chatManagement = chatManagement,
            monitorContactUpdates = monitorContactUpdates,
            getUserOnlineStatusByHandleUseCase = getUserOnlineStatusByHandleUseCase,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            getContactFromChatUseCase = getContactFromChatUseCase,
            getContactFromEmailUseCase = getContactFromEmailUseCase,
            applyContactUpdatesUseCase = applyContactUpdatesUseCase,
            setUserAliasUseCase = setUserAliasUseCase,
            removeContactByEmailUseCase = removeContactByEmailUseCase,
            getInSharesUseCase = getInSharesUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            createChatRoomUseCase = createChatRoomUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            startConversationUseCase = startConversationUseCase,
            ioDispatcher = standardDispatcher,
            applicationScope = testScope,
            createShareKeyUseCase = createShareKeyUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorChatConnectionStateUseCase = monitorChatConnectionStateUseCase,
            monitorChatOnlineStatusUseCase = monitorChatOnlineStatusUseCase,
            monitorChatPresenceLastGreenUpdatesUseCase = monitorChatPresenceLastGreenUpdatesUseCase,
            isChatConnectedToInitiateCallUseCase = isChatConnectedToInitiateCallUseCase,
            openOrStartCallUseCase = openOrStartCallUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            copyNodesUseCase = copyNodesUseCase,
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase
        )
    }

    private suspend fun stubCommon() {
        monitorContactUpdates.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorChatCallUpdatesUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorChatSessionUpdatesUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorUpdatePushNotificationSettingsUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow())
        }
        monitorChatConnectionStateUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorChatOnlineStatusUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorChatPresenceLastGreenUpdatesUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow())
        }
        val node = mock<FileNode> {
            on { name }.thenReturn("Node name")
            on { id }.thenReturn(NodeId(123456L))
        }
        getInSharesUseCase.stub { onBlocking { invoke(any()) }.thenReturn(listOf(node)) }
        whenever(getContactFromEmailUseCase(email = testEmail, skipCache = true)).thenReturn(
            contactItem
        )
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(monitorChatRetentionTimeUpdateUseCase(any())) doReturn emptyFlow()
    }

    @Test
    fun `test that get user status and request last green does not trigger last green when user status is online`() =
        runTest {
            whenever(getUserOnlineStatusByHandleUseCase(testHandle)).thenReturn(UserChatStatus.Online)
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            underTest.getUserStatusAndRequestForLastGreen()
            underTest.uiState.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(UserChatStatus.Online)
                verifyNoInteractions(requestUserLastGreenUseCase)
            }
        }

    @Test
    fun `test that get user status and request last green triggers last green when user status is away`() =
        runTest {
            whenever(getUserOnlineStatusByHandleUseCase(anyLong())).thenReturn(UserChatStatus.Away)
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            underTest.getUserStatusAndRequestForLastGreen()
            underTest.uiState.test {
                assertThat(awaitItem().userChatStatus).isEqualTo(UserChatStatus.Away)
                verify(requestUserLastGreenUseCase).invoke(userHandle = anyLong())
            }
        }

    @Test
    fun `test when update last green method is called state is updated with the last green value`() =
        runTest {
            whenever(getUserOnlineStatusByHandleUseCase(testHandle)).thenReturn(UserChatStatus.Online)
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            underTest.updateLastGreen(testHandle, lastGreen = 5)
            underTest.uiState.test {
                val nextState = awaitItem()
                assertThat(nextState.userChatStatus).isEqualTo(UserChatStatus.Online)
                assertThat(nextState.lastGreen).isEqualTo(5)
            }
        }

    @Test
    fun `test when contact info screen launched from contacts emits title`() =
        runTest {
            whenever(getChatRoomUseCase(testHandle)).thenReturn(chatRoom)
            whenever(
                getContactFromChatUseCase(
                    testHandle,
                    skipCache = true
                )
            ).thenReturn(contactItem)
            initViewModel()
            underTest.updateContactInfo(testHandle)
            underTest.uiState.test {
                val nextState = awaitItem()
                assertThat(nextState.primaryDisplayName).isEqualTo("Iron Man")
                assertThat(nextState.isFromContacts).isFalse()
                assertThat(nextState.contactItem?.email).isEqualTo("test@gmail.com")
            }
        }

    @Test
    fun `test when contact info screen launched from chats emits title`() =
        runTest {
            whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            underTest.uiState.test {
                val nextState = awaitItem()
                assertThat(nextState.primaryDisplayName).isEqualTo("Iron Man")
                assertThat(nextState.isFromContacts).isTrue()
                assertThat(nextState.contactItem?.email).isEqualTo("test@gmail.com")
            }
        }

    @Test
    fun `test when new nickname is given the nick name added snack bar message is emitted`() =
        runTest {
            whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
            whenever(setUserAliasUseCase("Spider Man", testHandle)).thenReturn("Spider Man")
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            verifyInitialData()
            underTest.updateNickName("Spider Man")
            underTest.uiState.test {
                val nextState = awaitItem()
                assertThat(nextState.snackBarMessage).isNotNull()
                assertThat(nextState.isFromContacts).isTrue()
                assertThat(nextState.contactItem?.email).isEqualTo("test@gmail.com")
            }
            underTest.onConsumeSnackBarMessageEvent()
            underTest.uiState.test {
                val nextState = awaitItem()
                assertThat(nextState.snackBarMessage).isNull()
            }
        }

    @Test
    fun `test that when remove contact is success isUserRemoved is emitted as true`() = runTest {
        whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
        whenever(removeContactByEmailUseCase(testEmail)).thenReturn(true)
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.removeContact()
        underTest.uiState.test {
            val nextState = awaitItem()
            assertThat(nextState.isUserRemoved).isTrue()
        }
    }


    @Test
    fun `test that if get in share is success the value is updated in state`() = runTest {
        whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.getInShares()
        underTest.uiState.test {
            val nextState = awaitItem()
            assertThat(nextState.inShares.size).isEqualTo(1)
        }
    }

    private suspend fun verifyInitialData() {
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.primaryDisplayName).isEqualTo("Iron Man")
            assertThat(initialState.snackBarMessage).isNull()
        }
    }

    @Test
    fun `test that when chatNotificationsClicked is clicked chatNotificationChange is fired`() =
        runTest {
            whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            verifyInitialData()
            underTest.chatNotificationsClicked()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isChatNotificationChange).isTrue()
            }
        }

    @Test
    fun `test that when chatNotificationsClicked is clicked new chat is created if chatroom does not exist`() =
        runTest {
            val newChatId = Random.nextLong()
            val newChatRoom = mock<ChatRoom> {
                on { chatId }.thenReturn(newChatId)
            }
            whenever(getContactFromEmailUseCase(email = testEmail, skipCache = true)).thenReturn(
                contactItem
            )
            whenever(getChatRoomByUserUseCase(userHandle = testHandle)).thenReturn(null)
            whenever(createChatRoomUseCase(isGroup = false, userHandles = listOf(testHandle)))
                .thenReturn(newChatId)
            whenever(getChatRoomUseCase(newChatId)).thenReturn(newChatRoom)
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            verifyInitialData()
            underTest.chatNotificationsClicked()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isChatNotificationChange).isTrue()
                assertThat(state.chatRoom?.chatId).isEqualTo(newChatId)
            }
        }

    @Test
    fun `test that when send message is clicked chat activity is opened`() = runTest {
        whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
        val chatId = Random.nextLong()
        val exampleStorageStateEvent = StorageStateEvent(
            handle = 1L,
            eventString = "eventString",
            number = 0L,
            text = "text",
            type = EventType.Storage,
            storageState = StorageState.Unknown
        )
        val storageFlow: MutableStateFlow<StorageStateEvent> =
            MutableStateFlow(exampleStorageStateEvent)
        whenever(monitorStorageStateEventUseCase()).thenReturn(storageFlow)
        whenever(startConversationUseCase(isGroup = false, userHandles = listOf(testHandle)))
            .thenReturn(chatId)
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.sendMessageToChat()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.chatRoom?.chatId).isEqualTo(123456L)
            assertThat(state.shouldNavigateToChat).isTrue()
        }
    }

    @Test
    fun `test that when node update is triggered state is updated`() = runTest {
        val unTypedNodeNew = mock<FolderNode> {
            on { parentId.longValue }.thenReturn(-1L)
        }
        val node = mock<Node> {
            on { isIncomingShare }.thenReturn(true)
        }
        val nodeUpdate = mock<NodeUpdate> {
            on { changes }.thenReturn(mapOf(Pair(node, listOf(NodeChanges.Remove))))
        }
        whenever(getChatRoomByUserUseCase(testHandle)).thenReturn(chatRoom)
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.primaryDisplayName).isEqualTo("Iron Man")
            assertThat(initialState.snackBarMessage).isNull()
            whenever(getInSharesUseCase(any())).thenReturn(listOf(unTypedNodeNew))
            monitorNodeUpdatesFakeFlow.emit(nodeUpdate)
            val newState = awaitItem()
            assertThat(newState.isNodeUpdated).isTrue()
        }
    }

    @Test
    fun `test that when onConsumeNameCollisions is triggered state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeNameCollisions()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.nameCollisions).isEmpty()
        }
    }

    @Test
    fun `test that when onConsumeCopyException is called state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeCopyException()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.copyError).isNull()
        }
    }

    @Test
    fun `test that when onConsumeNodeUpdateEvent is called state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeNodeUpdateEvent()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isNodeUpdated).isFalse()
        }
    }

    @Test
    fun `test that when onConsumeStorageOverQuotaEvent is called state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeStorageOverQuotaEvent()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isStorageOverQuota).isFalse()
        }
    }

    @Test
    fun `test that when onConsumeChatNotificationChangeEvent is called state is updated`() =
        runTest {
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            verifyInitialData()
            underTest.onConsumeChatNotificationChangeEvent()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isChatNotificationChange).isFalse()
            }
        }

    @Test
    fun `test that when onConsumeNavigateToChatEvent is called state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeNavigateToChatEvent()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.shouldNavigateToChat).isFalse()
        }
    }

    @Test
    fun `test that when onConsumePushNotificationSettingsUpdateEvent is called state is updated`() =
        runTest {
            initViewModel()
            underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
            verifyInitialData()
            underTest.onConsumePushNotificationSettingsUpdateEvent()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isPushNotificationSettingsUpdated).isFalse()
            }
        }

    @Test
    fun `test that when onConsumeSnackBarMessageEvent is called state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeSnackBarMessageEvent()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.snackBarMessage).isNull()
            assertThat(state.snackBarMessageString).isNull()
        }
    }

    @Test
    fun `test that when onConsumeChatCallStatusChangeEvent is called state is updated`() = runTest {
        initViewModel()
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
        verifyInitialData()
        underTest.onConsumeChatCallStatusChangeEvent()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.callStatusChanged).isFalse()
        }
    }

    @Test
    fun `test that an exception from remove contact by email is not propagated`() = runTest {
        whenever(removeContactByEmailUseCase(any()))
            .thenAnswer { throw MegaException(1, "It's broken") }
        initViewModel()
        with(underTest) {
            removeContact()
            uiState.test {
                assertFalse(awaitItem().isUserRemoved)
            }
        }
    }

    @Test
    fun `test that retention time updates update state correctly`() = runTest {
        val chatId = 123L
        val retentionTime = 5693L

        whenever(monitorChatRetentionTimeUpdateUseCase(chatId)) doReturn flowOf(retentionTime)

        underTest.monitorChatRetentionTimeUpdate(chatId)

        underTest.uiState.map { it.retentionTime }.test {
            assertThat(awaitItem()).isEqualTo(retentionTime)
        }
    }
}
