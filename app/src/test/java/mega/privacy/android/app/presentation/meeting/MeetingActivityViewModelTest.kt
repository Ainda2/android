package mega.privacy.android.app.presentation.meeting

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivityRepository
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.mapper.ChatParticipantMapper
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.QueryChatLinkUseCase
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInviteWithChatIdUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarUseCase
import mega.privacy.android.domain.usecase.call.AllowUsersJoinCallUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.call.CreateMeetingUseCase
import mega.privacy.android.domain.usecase.call.GetCallIdsOfOthersCallsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.call.RingIndividualInACallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.CreateChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.chat.UpdateChatPermissionsUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallScreenOpenedUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableAudioUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorAudioOutputUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MuteAllPeersUseCase
import mega.privacy.android.domain.usecase.meeting.MutePeersUseCase
import mega.privacy.android.domain.usecase.meeting.StartVideoDeviceUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@ExtendWith(value = [CoroutineMainDispatcherExtension::class, InstantTaskExecutorExtension::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MeetingActivityViewModelTest {

    private lateinit var underTest: MeetingActivityViewModel

    private val meetingActivityRepository: MeetingActivityRepository = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val getCallIdsOfOthersCallsUseCase: GetCallIdsOfOthersCallsUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val chatManagement: ChatManagement = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val checkChatLink: CheckChatLinkUseCase = mock()
    private val getChatParticipants: GetChatParticipants = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorFinishActivityUseCase: MonitorFinishActivityUseCase = mock()
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase = mock()
    private val getChatRoomUseCase: GetChatRoomUseCase = mock()
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase = mock()
    private val queryChatLinkUseCase: QueryChatLinkUseCase = mock()
    private val setOpenInviteWithChatIdUseCase: SetOpenInviteWithChatIdUseCase = mock()
    private val chatParticipantMapper: ChatParticipantMapper = mock()
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase = mock()
    private val createChatLinkUseCase: CreateChatLinkUseCase = mock()
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase = mock()
    private val updateChatPermissionsUseCase: UpdateChatPermissionsUseCase = mock()
    private val removeFromChaUseCase: RemoveFromChat = mock()
    private val startConversationUseCase: StartConversationUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val hangChatCallUseCase: HangChatCallUseCase = mock()
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase = mock()
    private val broadcastCallScreenOpenedUseCase: BroadcastCallScreenOpenedUseCase = mock()
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase = mock()
    private val getMyFullNameUseCase: GetMyFullNameUseCase = mock()
    private val monitorUserUpdates: MonitorUserUpdates = mock()
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase =
        mock()

    private val deviceGateway: DeviceGateway = mock()
    private val ringIndividualInACallUseCase: RingIndividualInACallUseCase = mock()
    private val allowUsersJoinCallUseCase: AllowUsersJoinCallUseCase = mock()
    private val mutePeersUseCase: MutePeersUseCase = mock()
    private val muteAllPeersUseCase: MuteAllPeersUseCase = mock()
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val getPluralStringFromStringResMapper: GetPluralStringFromStringResMapper = mock()
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val startVideoDeviceUseCase: StartVideoDeviceUseCase = mock()
    private val monitorCallEndedUseCase: MonitorCallEndedUseCase = mock()
    private val monitorAudioOutputUseCase: MonitorAudioOutputUseCase = mock()
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase = mock()

    private val enableOrDisableVideoUseCase: EnableOrDisableVideoUseCase = mock()
    private val enableOrDisableAudioUseCase: EnableOrDisableAudioUseCase = mock()
    private val createMeetingUseCase: CreateMeetingUseCase = mock()
    private val startCallUseCase: StartCallUseCase = mock()
    private val getUserAvatarUseCase: GetUserAvatarUseCase = mock()

    private val context: Context = mock()

    private val chatId = 123L

    @BeforeEach
    fun setUp() {
        reset(
            meetingActivityRepository,
            answerChatCallUseCase,
            getCallIdsOfOthersCallsUseCase,
            getChatCallUseCase,
            rtcAudioManagerGateway,
            chatManagement,
            setChatVideoInDeviceUseCase,
            checkChatLink,
            context,
            getChatParticipants,
            monitorConnectivityUseCase,
            monitorFinishActivityUseCase,
            monitorChatCallUpdatesUseCase,
            monitorChatSessionUpdatesUseCase,
            getChatRoomUseCase,
            monitorChatRoomUpdatesUseCase,
            queryChatLinkUseCase,
            setOpenInviteWithChatIdUseCase,
            chatParticipantMapper,
            isEphemeralPlusPlusUseCase,
            createChatLinkUseCase,
            inviteContactWithHandleUseCase,
            updateChatPermissionsUseCase,
            removeFromChaUseCase,
            startConversationUseCase,
            isConnectedToInternetUseCase,
            monitorStorageStateEventUseCase,
            hangChatCallUseCase,
            broadcastCallEndedUseCase,
            broadcastCallScreenOpenedUseCase,
            getScheduledMeetingByChatUseCase,
            getMyFullNameUseCase,
            monitorUserUpdates,
            monitorScheduledMeetingUpdatesUseCase,
            deviceGateway,
            ringIndividualInACallUseCase,
            allowUsersJoinCallUseCase,
            mutePeersUseCase,
            muteAllPeersUseCase,
            getStringFromStringResMapper,
            getPluralStringFromStringResMapper,
            getCurrentSubscriptionPlanUseCase,
            getFeatureFlagValueUseCase,
            getMyUserHandleUseCase,
            startVideoDeviceUseCase,
            createMeetingUseCase,
            startCallUseCase,
            monitorCallEndedUseCase,
            enableOrDisableAudioUseCase,
            enableOrDisableVideoUseCase,
            monitorAudioOutputUseCase,
            savedStateHandle,
        )
        wheneverBlocking { monitorChatConnectionStateUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorConnectivityUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorFinishActivityUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorChatCallUpdatesUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorChatSessionUpdatesUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorChatRoomUpdatesUseCase(chatId) } doReturn emptyFlow()
        wheneverBlocking { monitorStorageStateEventUseCase() } doReturn MutableStateFlow(
            StorageStateEvent(1L, "", 0L, "", EventType.Unknown, StorageState.Unknown)
        )
        wheneverBlocking { monitorUserUpdates() } doReturn emptyFlow()
        wheneverBlocking { monitorScheduledMeetingUpdatesUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorCallEndedUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorAudioOutputUseCase() } doReturn emptyFlow()
    }

    private fun initUnderTest() {
        stubCommon()
        underTest = MeetingActivityViewModel(
            meetingActivityRepository = meetingActivityRepository,
            answerChatCallUseCase = answerChatCallUseCase,
            getCallIdsOfOthersCallsUseCase = getCallIdsOfOthersCallsUseCase,
            getChatCallUseCase = getChatCallUseCase,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            chatManagement = chatManagement,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            checkChatLink = checkChatLink,
            context = context,
            getChatParticipants = getChatParticipants,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            monitorChatRoomUpdatesUseCase = monitorChatRoomUpdatesUseCase,
            queryChatLinkUseCase = queryChatLinkUseCase,
            setOpenInviteWithChatIdUseCase = setOpenInviteWithChatIdUseCase,
            chatParticipantMapper = chatParticipantMapper,
            isEphemeralPlusPlusUseCase = isEphemeralPlusPlusUseCase,
            createChatLinkUseCase = createChatLinkUseCase,
            inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
            updateChatPermissionsUseCase = updateChatPermissionsUseCase,
            removeFromChaUseCase = removeFromChaUseCase,
            startConversationUseCase = startConversationUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            hangChatCallUseCase = hangChatCallUseCase,
            broadcastCallEndedUseCase = broadcastCallEndedUseCase,
            broadcastCallScreenOpenedUseCase = broadcastCallScreenOpenedUseCase,
            getScheduledMeetingByChatUseCase = getScheduledMeetingByChatUseCase,
            getMyFullNameUseCase = getMyFullNameUseCase,
            monitorUserUpdates = monitorUserUpdates,
            monitorScheduledMeetingUpdatesUseCase = monitorScheduledMeetingUpdatesUseCase,
            deviceGateway = deviceGateway,
            ringIndividualInACallUseCase = ringIndividualInACallUseCase,
            allowUsersJoinCallUseCase = allowUsersJoinCallUseCase,
            mutePeersUseCase = mutePeersUseCase,
            muteAllPeersUseCase = muteAllPeersUseCase,
            getStringFromStringResMapper = getStringFromStringResMapper,
            getPluralStringFromStringResMapper = getPluralStringFromStringResMapper,
            getCurrentSubscriptionPlanUseCase = getCurrentSubscriptionPlanUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            savedStateHandle = savedStateHandle,
            enableOrDisableAudioUseCase = enableOrDisableAudioUseCase,
            enableOrDisableVideoUseCase = enableOrDisableVideoUseCase,
            startVideoDeviceUseCase = startVideoDeviceUseCase,
            monitorCallEndedUseCase = monitorCallEndedUseCase,
            createMeetingUseCase = createMeetingUseCase,
            startCallUseCase = startCallUseCase,
            monitorAudioOutputUseCase = monitorAudioOutputUseCase,
            monitorChatConnectionStateUseCase = monitorChatConnectionStateUseCase,
            getUserAvatarUseCase = getUserAvatarUseCase,
            megaChatRequestHandler = mock(),
        )
    }

    private fun stubCommon() {
        whenever(savedStateHandle.get<Long>(MeetingActivity.MEETING_CHAT_ID)).thenReturn(chatId)
    }

    @ParameterizedTest
    @EnumSource(ChatConnectionStatus::class)
    fun `test that MonitorChatConnectionStateUseCase updates state if invoked with correct chatId`(
        chatConnectionStatus: ChatConnectionStatus,
    ) = runTest {
        val updateFlow = MutableStateFlow(
            ChatConnectionState(
                chatId = chatId,
                chatConnectionStatus = chatConnectionStatus
            )
        )

        whenever(monitorChatConnectionStateUseCase()).thenReturn(updateFlow)

        initUnderTest()
        underTest.monitorChatConnectionStatus(chatId)
        underTest.state.map { it.chatConnectionStatus }.test {
            assertThat(awaitItem()).isEqualTo(chatConnectionStatus)
        }
    }

    @ParameterizedTest
    @EnumSource(ChatConnectionStatus::class)
    fun `test that MonitorChatConnectionStateUseCase updates state if invoked with incorrect chatId`(
        chatConnectionStatus: ChatConnectionStatus,
    ) = runTest {
        val updateFlow = MutableStateFlow(
            ChatConnectionState(
                chatId = 321L,
                chatConnectionStatus = chatConnectionStatus
            )
        )

        whenever(monitorChatConnectionStateUseCase()).thenReturn(updateFlow)

        initUnderTest()
        underTest.monitorChatConnectionStatus(chatId)
        underTest.state.map { it.chatConnectionStatus }.test {
            assertThat(awaitItem()).isNull()
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
