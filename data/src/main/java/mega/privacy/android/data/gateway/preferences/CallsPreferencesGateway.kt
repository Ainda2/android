package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders

/**
 * Gateway to manage calls preferences.
 */
interface CallsPreferencesGateway {

    /**
     * Gets if notification sounds are activated when there are changes in participants in group calls or meetings.
     *
     * @return If notification sounds are enabled or disabled.
     */
    fun getCallsSoundNotificationsPreference(): Flow<CallsSoundEnabledState>

    /**
     * Get calls meeting invitations preference
     *
     * @return If meeting invitations are enabled or disabled.
     */
    fun getCallsMeetingInvitationsPreference(): Flow<CallsMeetingInvitations>

    /**
     * Get calls meeting reminders preference
     *
     * @return If meeting reminders are enabled or disabled.
     */
    fun getCallsMeetingRemindersPreference(): Flow<CallsMeetingReminders>

    /**
     * Get waiting room reminders preference
     *
     * @return If waiting room reminders are enabled or disabled.
     */
    fun getWaitingRoomRemindersPreference(): Flow<WaitingRoomReminders>

    /**
     * Get users call limit reminders preference
     *
     * @return If waiting room reminders are enabled or disabled.
     */
    fun getUsersCallLimitRemindersPreference(): Flow<UsersCallLimitReminders>

    /**
     * Enable or disable notification sounds are enabled when there are changes in participants in group calls or meetings.
     *
     * @param soundNotifications True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsSoundNotificationsPreference(soundNotifications: CallsSoundEnabledState)

    /**
     * Enable or disable calls meeting invitations preference
     *
     * @param callsMeetingInvitations True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsMeetingInvitationsPreference(callsMeetingInvitations: CallsMeetingInvitations)

    /**
     * Enable or disable calls meeting reminders preference
     *
     * @param callsMeetingReminders True, if must be enabled. False, if must be disabled.
     */
    suspend fun setCallsMeetingRemindersPreference(callsMeetingReminders: CallsMeetingReminders)

    /**
     * Enable or disable waiting room warning reminders preference
     *
     * @param waitingRoomReminders True, if must be enabled. False, if must be disabled.
     */
    suspend fun setWaitingRoomRemindersPreference(waitingRoomReminders: WaitingRoomReminders)

    /**
     * Enable or disable users call limit reminders preference
     *
     * @param usersCallLimitReminders True, if must be enabled. False, if must be disabled.
     */
    suspend fun setUsersCallLimitRemindersPreference(usersCallLimitReminders: UsersCallLimitReminders)


    /**
     * Get raise to hand suggestion preference
     */
    suspend fun getRaiseToHandSuggestionPreference(): Boolean?


    /**
     * Set raise to hand suggestion preference
     */
    suspend fun setRaiseToHandSuggestionPreference()

    /**
     * Clears calls preferences.
     */
    suspend fun clearPreferences()
}
