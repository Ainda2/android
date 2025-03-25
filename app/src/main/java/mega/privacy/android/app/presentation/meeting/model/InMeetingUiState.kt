package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.call.AnotherCallType
import mega.privacy.android.domain.entity.call.CallOnHoldType
import mega.privacy.android.domain.entity.call.CallUIStatusType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.meeting.SubtitleCallType
import timber.log.Timber

/**
 * In meeting UI state
 *
 * @property error                                  String resource id for showing an error.
 * @property isOpenInvite                           True if it's enabled, false if not.
 * @property callUIStatus                           [CallUIStatusType]
 * @property call                                   [ChatCall]
 * @property chat                                   [ChatRoom]
 * @property currentChatId                          Chat Id
 * @property previousState                          [ChatCallStatus]
 * @property isSpeakerSelectionAutomatic            True, if is speaker selection automatic. False, if it's manual.
 * @property haveConnection                         True, have connection. False, if not.
 * @property showCallDuration                       True, should show call duration. False, if not.
 * @property isPublicChat                           True if it's public chat. False, if not
 * @property chatTitle                              Chat title
 * @property updateCallSubtitle                     If should update the call subtitle
 * @property updateAnotherCallBannerType            Update the banner of another call.
 * @property anotherChatTitle                       Chat title of another call.
 * @property updateModeratorsName                   Update moderator's name
 * @property updateNumParticipants                  Update the num of participants
 * @property showMeetingInfoFragment                True to show meeting info fragment or False otherwise
 * @property snackbarInSpeakerViewMessage           Message to show in Snackbar in speaker view.
 * @property addScreensSharedParticipantsList       List of [Participant] to add the screen shared in the carousel
 * @property removeScreensSharedParticipantsList    List of [Participant] to remove the screen shared in the carousel
 * @property isMeeting                              True if it's meetings. False, if not.
 * @property updateListUi                           True, List was sorted and need to be updated. False, if not.
 * @property showEndMeetingAsHostBottomPanel        True, show bottom sheet when a host leaves the call. False otherwise
 * @property showEndMeetingAsOnlyHostBottomPanel    True, show bottom sheet when the only host leaves the call. False otherwise
 * @property joinedAsGuest                          True, joined as guest. False, otherwise.
 * @property shouldFinish                           True, if the activity should finish. False, if not.
 * @property minutesToEndMeeting                    Minutes to end the meeting
 * @property showMeetingEndWarningDialog            True, show the dialog to warn the user that the meeting is going to end. False otherwise
 * @property isRaiseToSpeakFeatureFlagEnabled       True, if Raise to speak feature flag enabled. False, otherwise.
 * @property anotherCall                            Another call in progress or on hold.
 * @property showCallOptionsBottomSheet             True, if should be shown the call options bottom panel. False, otherwise
 * @property isEphemeralAccount                     True, if it's ephemeral account. False, if not.
 * @property showOnlyMeEndCallTime                  Show only me end call remaining time
 * @property participantsChanges                    Message to show when a participant changes
 * @property userIdsWithChangesInRaisedHand         User identifiers with changes in the raised hand
 * @property isRaiseToHandSuggestionShown           True, if the Raise to Hand suggestion has been shown. False, otherwise.
 * @property shouldUpdateLocalAVFlags               True, if should update local av flag. False, if not
 * @property sessionOnHoldChanges                   [ChatSession] with changes in session on hold
 * @property isPictureInPictureFeatureFlagEnabled       True, if Picture in Picture feature flag enabled. False, otherwise.
 * @property isInPipMode                                True, if is in Picture in Picture mode. False, otherwise.
 * @property myUserHandle                               My user handle
 * @property changesInAVFlagsInSession              [ChatSession] with changes in remote audio video flags
 * @property changesInAudioLevelInSession           [ChatSession] with changes in audio level
 * @property changesInHiResInSession                [ChatSession] with changes in high resolution video
 * @property changesInLowResInSession               [ChatSession] with changes in low resolution video
 * @property changesInStatusInSession               [ChatSession] with changes in status
 * @property shouldCheckChildFragments              True, if should update fragments. False, if not.
 * @property isVideoEnabledDueToProximitySensor     True, video on, false, video off.
 */
data class InMeetingUiState(
    val error: Int? = null,
    val isOpenInvite: Boolean? = null,
    var callUIStatus: CallUIStatusType = CallUIStatusType.None,
    val call: ChatCall? = null,
    val chat: ChatRoom? = null,
    val currentChatId: Long = -1L,
    val previousState: ChatCallStatus = ChatCallStatus.Initial,
    val isSpeakerSelectionAutomatic: Boolean = true,
    val haveConnection: Boolean = false,
    val showCallDuration: Boolean = false,
    val chatTitle: String = " ",
    val isPublicChat: Boolean = false,
    val updateCallSubtitle: SubtitleCallType = SubtitleCallType.Connecting,
    val updateAnotherCallBannerType: AnotherCallType = AnotherCallType.NotCall,
    val anotherChatTitle: String = " ",
    val updateModeratorsName: String = " ",
    val updateNumParticipants: Int = 1,
    val showMeetingInfoFragment: Boolean = false,
    val snackbarInSpeakerViewMessage: StateEventWithContent<String> = consumed(),
    val addScreensSharedParticipantsList: List<Participant>? = null,
    val removeScreensSharedParticipantsList: List<Participant>? = null,
    val isMeeting: Boolean = false,
    val updateListUi: Boolean = false,
    val showEndMeetingAsHostBottomPanel: Boolean = false,
    val showEndMeetingAsOnlyHostBottomPanel: Boolean = false,
    val joinedAsGuest: Boolean = false,
    val shouldFinish: Boolean = false,
    val minutesToEndMeeting: Int? = null,
    val showMeetingEndWarningDialog: Boolean = false,
    val isRaiseToSpeakFeatureFlagEnabled: Boolean = false,
    val anotherCall: ChatCall? = null,
    val showCallOptionsBottomSheet: Boolean = false,
    val isEphemeralAccount: Boolean? = null,
    val showOnlyMeEndCallTime: Long? = null,
    val participantsChanges: ParticipantsChange? = null,
    val userIdsWithChangesInRaisedHand: List<Long> = emptyList(),
    val isRaiseToHandSuggestionShown: Boolean = true,
    val shouldUpdateLocalAVFlags: Boolean = true,
    val sessionOnHoldChanges: ChatSession? = null,
    val isPictureInPictureFeatureFlagEnabled: Boolean = false,
    val isInPipMode: Boolean = false,
    val myUserHandle: Long? = null,
    val changesInAVFlagsInSession: ChatSession? = null,
    val changesInAudioLevelInSession: ChatSession? = null,
    val changesInHiResInSession: ChatSession? = null,
    val changesInLowResInSession: ChatSession? = null,
    val changesInStatusInSession: ChatSession? = null,
    val shouldCheckChildFragments: Boolean = false,
    val snackbarMsg: String? = null,
    val isVideoEnabledDueToProximitySensor: Boolean? = null,
    val userAvatarUpdateId: Long? = null,
) {
    /**
     * Check if it's one to one call
     * @return True, if it's one to one call. False, if it's a group call or a meeting.
     */
    val isOneToOneCall
        get():Boolean? = chat?.let {
            it.isMeeting.not() && it.isGroup.not()
        } ?: run { null }

    /**
     * Is call on hold
     */
    val isCallOnHold
        get():Boolean? {
            call?.apply {
                return isOnHold
            }

            return null
        }

    /**
     * Is call established
     */
    val isCallEstablished
        get():Boolean {
            call?.status?.let {
                return it != ChatCallStatus.Initial && it != ChatCallStatus.Connecting && it != ChatCallStatus.TerminatingUserParticipation && it != ChatCallStatus.Destroyed
            }

            return false
        }

    /**
     * Check if the buttons are enabled
     */
    val areButtonsEnabled
        get() = isCallEstablished && isCallOnHold == false

    /**
     * Has local video
     */
    val hasLocalVideo
        get():Boolean = call?.hasLocalVideo == true

    /**
     * Has local audio
     */
    val hasLocalAudio
        get():Boolean = call?.hasLocalAudio ?: run { false }

    /**
     * Check session is on hold in one to one call
     */
    val isSessionOnHold
        get(): Boolean? = getSession?.isOnHold

    /**
     * Check session is on hold in call
     */
    fun isSessionOnHoldByClientId(clientId: Long): Boolean? =
        getSessionByClientId(clientId)?.isOnHold

    /**
     * Get session in one to one call
     */
    val getSession
        get(): ChatSession? {
            call?.apply {
                sessionsClientId?.takeIf { it.isNotEmpty() }?.let {
                    it.first().let { clientId ->
                        return sessionByClientId[clientId]
                    }
                }
            }

            return null
        }

    /**
     * Get session by client Id
     *
     * @param clientId
     * @return [ChatSession]
     */
    fun getSessionByClientId(clientId: Long): ChatSession? {
        call?.apply {
            Timber.d("Call $call and Sessions $sessionsClientId")
            return sessionByClientId[clientId]
        }

        return null
    }

    /**
     * Check if it's me as participant
     *
     * @param peerId User handle of a participant
     * @return True, if it's me. False, otherwise
     */
    fun isMeAsParticipant(peerId: Long): Boolean = peerId == myUserHandle

    /**
     * Get the button to be displayed depending on the type of call on hold you have
     */
    val getButtonTypeToShow
        get():CallOnHoldType = when {
            anotherCall != null -> CallOnHoldType.SwapCalls
            isCallOnHold == null || isCallOnHold == false -> CallOnHoldType.PutCallOnHold
            else -> CallOnHoldType.ResumeCall
        }

    /**
     * Display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    val shouldShowSwapCamera
        get(): Boolean =
            call?.let { it.status != ChatCallStatus.Connecting && hasLocalVideo && isCallOnHold == false }
                ?: run { false }

}

/**
 * Data class to represent a change in the participants list
 */
data class ParticipantsChange(
    /**
     * Text to show
     */
    val text: String,
    /**
     * Type of change
     */
    val type: ParticipantsChangeType,
)

/**
 * Enum class to represent the type of change in the participants list
 */
enum class ParticipantsChangeType {
    /**
     * A participant joined the call
     */
    Join,

    /**
     * A participant left the call
     */
    Left
}
