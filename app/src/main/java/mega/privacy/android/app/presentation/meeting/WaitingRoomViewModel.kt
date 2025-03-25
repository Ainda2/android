package mega.privacy.android.app.presentation.meeting

import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLocalVideoUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.InitGuestChatSessionUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.JoinGuestChatCallUseCase
import mega.privacy.android.domain.usecase.chat.OpenChatLinkUseCase
import mega.privacy.android.domain.usecase.meeting.StartVideoDeviceUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduleMeetingDataUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.meeting.JoinMeetingAsGuestUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.waitingroom.IsValidWaitingRoomUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.mobile.analytics.event.WaitingRoomTimeoutEvent
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


/**
 * Waiting room view model
 *
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property getScheduleMeetingDataUseCase              [GetScheduleMeetingDataUseCase]
 * @property timestampMapper                            [ChatRoomTimestampMapper]
 * @property getMyAvatarFileUseCase                     [GetMyAvatarFileUseCase]
 * @property getMyAvatarColorUseCase                    [GetMyAvatarColorUseCase]
 * @property getUserFullNameUseCase                     [GetUserFullNameUseCase]
 * @property isValidWaitingRoomUseCase                  [IsValidWaitingRoomUseCase]
 * @property monitorChatCallUpdatesUseCase              [MonitorChatCallUpdatesUseCase]
 * @property monitorScheduledMeetingUpdatesUseCase      [MonitorScheduledMeetingUpdatesUseCase]
 * @property getChatCallUseCase                         [GetChatCallUseCase]
 * @property getChatLocalVideoUpdatesUseCase            [GetChatLocalVideoUpdatesUseCase]
 * @property setChatVideoInDeviceUseCase                [SetChatVideoInDeviceUseCase]
 * @property startVideoDeviceUseCase                    [StartVideoDeviceUseCase]
 * @property answerChatCallUseCase                      [AnswerChatCallUseCase]
 * @property initGuestChatSessionUseCase                [InitGuestChatSessionUseCase]
 * @property joinGuestChatCallUseCase                   [JoinGuestChatCallUseCase]
 * @property checkChatLinkUseCase                       [CheckChatLinkUseCase]
 * @property isUserLoggedInUseCase                             [IsUserLoggedInUseCase]
 * @property isEphemeralPlusPlusUseCase                 [IsEphemeralPlusPlusUseCase]
 * @property logoutUseCase                              [LogoutUseCase]
 * @property hangChatCallUseCase                        [HangChatCallUseCase]
 * @property openChatLinkUseCase                        [OpenChatLinkUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaitingRoomViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getScheduleMeetingDataUseCase: GetScheduleMeetingDataUseCase,
    private val timestampMapper: ChatRoomTimestampMapper,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val isValidWaitingRoomUseCase: IsValidWaitingRoomUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val joinMeetingAsGuestUseCase: JoinMeetingAsGuestUseCase,
    private val getChatLocalVideoUpdatesUseCase: GetChatLocalVideoUpdatesUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val startVideoDeviceUseCase: StartVideoDeviceUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
    private val joinGuestChatCallUseCase: JoinGuestChatCallUseCase,
    private val checkChatLinkUseCase: CheckChatLinkUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val chatManagement: ChatManagement,
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val openChatLinkUseCase: OpenChatLinkUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(WaitingRoomState())
    private var shouldAnswerCall = AtomicBoolean(true)

    /**
     * Waiting room state
     */
    val state: StateFlow<WaitingRoomState> = _state

    init {
        viewModelScope.launch {
            initChatGuestSessionIfNeeded()
            monitorCallUpdates()
            monitorMeetingUpdates()
            setChatVideoDevice()
            retrieveUserAvatar()
        }
    }

    /**
     * On ViewModel cleared
     */
    override fun onCleared() {
        applicationScope.launch {
            runCatching {
                startVideoDeviceUseCase(false)
            }.onSuccess {
                _state.update { it.copy(cameraEnabled = false) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
        super.onCleared()
    }

    /**
     * Start chat session for guest users if needed
     */
    private suspend fun initChatGuestSessionIfNeeded() {
        runCatching {
            if (!isUserLoggedInUseCase()) {
                initGuestChatSessionUseCase(anonymousMode = true)
            }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Load Meeting details
     *
     * @param chatId        Chat Room Id
     * @param chatLink      Chat Room Link
     */
    fun loadMeetingDetails(chatId: Long?, chatLink: String?) {
        viewModelScope.launch {
            val userLoggedIn = isUserLoggedInUseCase()
            _state.update {
                it.copy(
                    chatId = chatId ?: -1L,
                    chatLink = chatLink,
                    guestMode = !chatLink.isNullOrBlank() && !userLoggedIn,
                )
            }

            when {
                !chatLink.isNullOrBlank() -> {
                    if (isValidWaitingRoom(chatLink)) {
                        retrieveChatLinkDetails()
                        retrieveMeetingDetails()
                        if (userLoggedIn) {
                            joinCurrentCall()
                        }
                    } else {
                        _state.update { it.copy(joinCall = true) }
                    }
                }

                chatId != null -> {
                    if (isValidWaitingRoom(chatId)) {
                        retrieveMeetingDetails()
                        retrieveCallDetails()
                    } else {
                        _state.update { it.copy(joinCall = true) }
                    }
                }

                else -> error("Invalid parameters")
            }
        }
    }

    /**
     * Get SDK video stream flow
     *
     * @return  [Flow] emitting pairs of [Size] and [ByteArray] representing each video frame.
     */
    fun getVideoStream(): Flow<Pair<Size, ByteArray>> =
        getChatLocalVideoUpdatesUseCase()
            .catch { Timber.e(it) }
            .mapLatest { frame -> Size(frame.width, frame.height) to frame.byteBuffer }

    /**
     * Retrieve current Chat Room Scheduled Meeting details
     */
    private fun retrieveMeetingDetails() {
        viewModelScope.launch {
            runCatching {
                getScheduleMeetingDataUseCase(
                    chatId = _state.value.chatId,
                    meetingTimeMapper = timestampMapper::getWaitingRoomTimeFormatted
                )
            }.onSuccess { meeting ->
                _state.update {
                    it.copy(
                        schedId = meeting.schedId,
                        title = meeting.title,
                        formattedTimestamp = meeting.scheduledTimestampFormatted,
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Retrieve current Chat Room call details
     */
    private fun retrieveCallDetails() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(chatId = _state.value.chatId)
            }.onSuccess { call ->
                call?.updateUiState()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Method to know if a meeting has ended
     *
     * @param list MegaHandleList with the call ID
     * @return True, if the meeting is finished. False, if not.
     */
    private fun isMeetingEnded(list: List<Long>?): Boolean =
        list == null || list[0] == MEGACHAT_INVALID_HANDLE

    /**
     * Method to know if a meeting has ended
     *
     * @param state [ChatCallStatus]
     * @return True, if the meeting is finished. False, if not.
     */
    private fun isMeetingEnded(state: ChatCallStatus?): Boolean =
        state == null || state == ChatCallStatus.Destroyed

    /**
     * Retrieve Chat Room link details
     */
    private fun retrieveChatLinkDetails(
        shouldJoinMeetingAsGuest: Boolean = false,
    ) {
        viewModelScope.launch {
            runCatching {
                checkChatLinkUseCase(
                    chatLink = requireNotNull(_state.value.chatLink)
                ).also { requireNotNull(it.chatHandle) }
            }.onSuccess { chatRequest ->
                val isMeetingEnded = isMeetingEnded(chatRequest.handleList)
                _state.update {
                    it.copy(
                        isMeetingEnded = isMeetingEnded,
                        chatId = requireNotNull(chatRequest.chatHandle),
                        title = chatRequest.text,
                    )
                }

                if (shouldJoinMeetingAsGuest) {
                    state.value.chatLink?.let { link ->
                        state.value.guestFirstName?.let { firstName ->
                            state.value.guestLastName?.let { lastName ->
                                joinMeetingAsGuest(link, firstName, lastName)
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Monitor current chat call updates
     */
    private fun monitorCallUpdates() {
        viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .distinctUntilChanged()
                .collectLatest { call ->
                    call.changes?.apply {
                        Timber.d("Changes in call: $this")
                        when {
                            contains(ChatCallChanges.Status) -> {
                                _state.update {
                                    it.copy(
                                        isMeetingEnded = isMeetingEnded(call.status),
                                    )
                                }
                                call.updateUiState()
                            }
                        }
                    }
                }
        }
    }

    /**
     * Monitor current meeting updates
     */
    private fun monitorMeetingUpdates() {
        viewModelScope.launch {
            monitorScheduledMeetingUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { retrieveMeetingDetails() }
        }
    }

    /**
     * Set chat video In Device
     */
    private fun setChatVideoDevice() =
        viewModelScope.launch {
            runCatching {
                setChatVideoInDeviceUseCase()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Hang current chat call
     */
    private fun hangChatCall() =
        viewModelScope.launch {
            runCatching {
                val callId = _state.value.callId
                if (callId != -1L) {
                    hangChatCallUseCase(callId = callId)
                }
            }.onSuccess {
                _state.update { it.copy(callId = -1L) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Retrieve current user avatar
     */
    private fun retrieveUserAvatar() {
        viewModelScope.launch {
            runCatching {
                if (state.value.guestMode) return@launch

                val avatar = async { getMyAvatarFileUseCase(false)?.absolutePath }
                val color = async { getMyAvatarColorUseCase() }
                val fullName = async { getUserFullNameUseCase(false) }
                ChatAvatarItem(
                    uri = avatar.await(),
                    color = color.await(),
                    placeholderText = fullName.await(),
                )
            }.onSuccess { avatar ->
                _state.update { it.copy(avatar = avatar) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Check if current Chat Room Id is valid waiting room
     *
     * @param chatId    Chat Room id
     * @return          true if it's valid, false otherwise
     */
    private suspend fun isValidWaitingRoom(chatId: Long): Boolean = runCatching {
        isValidWaitingRoomUseCase(chatId)
    }.onFailure(Timber.Forest::e).getOrNull() ?: false

    /**
     * Check if current Chat Room Link is valid waiting room
     *
     * @param chatLink  Chat Room link
     * @return          true if it's valid, false otherwise
     */
    private suspend fun isValidWaitingRoom(chatLink: String): Boolean = runCatching {
        isValidWaitingRoomUseCase(chatLink)
    }.onFailure(Timber.Forest::e).getOrNull() ?: false

    /**
     * Update UI state based on the provided [ChatCall]
     */
    private fun ChatCall.updateUiState() {
        Timber.d("Call status: $status")
        when {
            hasTimeoutExpired() -> {
                Analytics.tracker.trackEvent(WaitingRoomTimeoutEvent)
                shouldAnswerCall.set(false)
                _state.update { it.copy(callId = callId, inactiveHostDialog = true) }
            }

            hasAccessBeenDenied() -> {
                shouldAnswerCall.set(false)
                _state.update { it.copy(callId = callId, denyAccessDialog = true) }
            }

            hasAccessBeenGranted() -> {
                _state.update { it.copy(callId = callId, joinCall = true) }
            }

            hasStarted() -> {
                _state.update { it.copy(callId = callId, callStarted = true) }

                if (shouldBeAnswered()) {
                    answerChatCall()
                }
            }

            else -> {
                _state.update { it.copy(callId = callId, callStarted = false) }
            }
        }
    }

    /**
     * Check if [ChatCall] access has been granted
     */
    private fun ChatCall.hasAccessBeenGranted(): Boolean =
        changes?.contains(ChatCallChanges.WaitingRoomAllow) == true
                || status == ChatCallStatus.Joining || status == ChatCallStatus.InProgress
                || waitingRoomStatus == WaitingRoomStatus.Allowed

    /**
     * Check if [ChatCall] access has been denied
     */
    private fun ChatCall.hasAccessBeenDenied(): Boolean =
        termCode == ChatCallTermCodeType.Kicked

    /**
     * Check if [ChatCall] timeout has expired
     */
    private fun ChatCall.hasTimeoutExpired(): Boolean =
        termCode == ChatCallTermCodeType.WaitingRoomTimeout

    /**
     * Check if [ChatCall] should be answered
     */
    private fun ChatCall.shouldBeAnswered(): Boolean =
        shouldAnswerCall.get() && !_state.value.guestMode && status == ChatCallStatus.UserNoPresent

    /**
     * Check if [ChatCall] has started
     */
    private fun ChatCall.hasStarted(): Boolean =
        when (status) {
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.UserNoPresent,
            ChatCallStatus.Connecting,
            ChatCallStatus.Joining,
            ChatCallStatus.InProgress,
            -> true

            else -> false
        }

    private fun answerChatCall() {
        viewModelScope.launch {
            runCatching {
                state.value.run {
                    answerChatCallUseCase(this.chatId, this.cameraEnabled, this.micEnabled)
                }

            }.onSuccess { call ->
                call?.updateUiState()
            }.onFailure { error ->
                when {
                    error is MegaException && error.errorCode == MegaChatError.ERROR_EXIST -> {
                        // Already requested, do nothing.
                    }

                    error is MegaException && error.errorCode == MegaChatError.ERROR_ACCESS -> {
                        // Retry request access
                        answerChatCall()
                    }

                    else -> Timber.e(error)
                }
            }
        }
    }

    /**
     * Enable device camera
     *
     * @param enable    true to enable camera, false otherwise
     */
    fun enableCamera(enable: Boolean) =
        viewModelScope.launch {
            runCatching {
                startVideoDeviceUseCase(enable)
            }.onSuccess {
                _state.update { it.copy(cameraEnabled = enable) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Enable device microphone
     *
     * @param enable    true to enable microphone, false otherwise
     */
    fun enableMicrophone(enable: Boolean) {
        _state.update { it.copy(micEnabled = enable) }
    }

    /**
     * Enable device speaker
     *
     * @param enable    true to enable speaker, false otherwise
     */
    fun enableSpeaker(enable: Boolean) {
        _state.update { it.copy(speakerEnabled = enable) }
    }

    /**
     * Set guest user name
     *
     * @param firstName
     * @param lastName
     */
    fun setGuestName(firstName: String, lastName: String) {
        _state.update {
            it.copy(
                guestFirstName = firstName,
                guestLastName = lastName,
            )
        }

        retrieveChatLinkDetails(
            shouldJoinMeetingAsGuest = true
        )
    }

    /**
     * Control when join a meeting as a guest
     *
     * @param meetingLink   Meeting link
     * @param firstName     Guest first name
     * @param lastName      Guest last name
     */
    private fun joinMeetingAsGuest(meetingLink: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            runCatching {
                joinMeetingAsGuestUseCase(meetingLink, firstName, lastName)
            }.onSuccess {
                chatManagement
                    .setOpeningMeetingLink(
                        state.value.chatId,
                        true
                    )
                autoJoinPublicChat()
            }.onFailure { exception ->
                Timber.e(exception)
                chatLogout()
            }
        }
    }

    /**
     * Auto join public chat
     */
    private fun autoJoinPublicChat() {
        if (!chatManagement.isAlreadyJoining(state.value.chatId)) {
            chatManagement.addJoiningChatId(state.value.chatId)
            viewModelScope.launch {
                runCatching {
                    joinPublicChatUseCase(state.value.chatId)
                }.onSuccess {
                    chatManagement.removeJoiningChatId(state.value.chatId)
                    chatManagement.broadcastJoinedSuccessfully()
                    _state.update { it.copy(guestMode = false) }
                    setChatVideoDevice()
                    retrieveMeetingDetails()
                    retrieveCallDetails()
                    retrieveUserAvatar()
                }.onFailure { exception ->
                    Timber.e(exception)
                    chatManagement.removeJoiningChatId(state.value.chatId)
                    _state.update {
                        it.copy(
                            finish = true,
                        )
                    }
                }
            }
        }
    }

    /**
    Chat logout
     */
    private fun chatLogout() {
        viewModelScope.launch {
            runCatching {
                chatLogoutUseCase()
            }.onSuccess {
                _state.update {
                    it.copy(
                        finish = true,
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update {
                    it.copy(
                        finish = true,
                    )
                }
            }
        }
    }

    /**
     * Join current chat call.
     *
     * Also disables camera during guest login process to avoid SDK issues.
     */
    private fun joinCurrentCall() {
        viewModelScope.launch {
            val currentState = _state.value
            val cameraEnabled = currentState.cameraEnabled
            runCatching {
                if (cameraEnabled) enableCamera(false).join()
                openChatLinkUseCase(
                    chatLink = requireNotNull(currentState.chatLink)
                )

            }.onSuccess {
                if (cameraEnabled) enableCamera(true)
                setChatVideoDevice()
                retrieveMeetingDetails()
                retrieveCallDetails()
                retrieveUserAvatar()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Finish current Waiting Room session
     */
    fun finishWaitingRoom() {
        viewModelScope.launch {
            runCatching {
                shouldAnswerCall.set(false)
                hangChatCall().join()

                if (isEphemeralPlusPlusUseCase()) {
                    logoutUseCase()
                }
            }.onSuccess {
                _state.update { it.copy(finish = true) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }
}
