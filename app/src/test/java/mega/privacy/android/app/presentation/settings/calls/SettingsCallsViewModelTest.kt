package mega.privacy.android.app.presentation.settings.calls

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.usecase.call.MonitorCallSoundEnabledUseCase
import mega.privacy.android.domain.usecase.call.SetCallsSoundEnabledStateUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsMeetingInvitationsUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsMeetingRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsMeetingInvitationsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsMeetingRemindersUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsCallsViewModelTest {

    private lateinit var underTest: SettingsCallsViewModel

    private val monitorCallSoundEnabledUseCase = mock<MonitorCallSoundEnabledUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val setCallsSoundEnabledStateUseCase = mock<SetCallsSoundEnabledStateUseCase>()
    private val getCallsMeetingInvitations = mock<GetCallsMeetingInvitationsUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val setCallsMeetingInvitations = mock<SetCallsMeetingInvitationsUseCase>()
    private val getCallsMeetingReminders = mock<GetCallsMeetingRemindersUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }
    private val setCallsMeetingReminders = mock<SetCallsMeetingRemindersUseCase>()

    private val sendStatisticsMeetings = mock<SendStatisticsMeetingsUseCase>()

    private fun stubCommon() {
        reset(
            monitorCallSoundEnabledUseCase,
            setCallsSoundEnabledStateUseCase,
            getCallsMeetingInvitations,
            setCallsMeetingInvitations,
            getCallsMeetingReminders,
            setCallsMeetingReminders,
            sendStatisticsMeetings,
        )

        whenever(monitorCallSoundEnabledUseCase()).thenReturn(emptyFlow())
        whenever(getCallsMeetingInvitations()).thenReturn(emptyFlow())
        whenever(getCallsMeetingReminders()).thenReturn(emptyFlow())

    }

    /**
     * Initializes [SettingsCallsViewModel] for testing
     */
    private suspend fun setupUnderTest() {
        stubCommon()
        underTest = SettingsCallsViewModel(
            monitorCallSoundEnabledUseCase = monitorCallSoundEnabledUseCase,
            setCallsSoundEnabledStateUseCase = setCallsSoundEnabledStateUseCase,
            getCallsMeetingInvitations = getCallsMeetingInvitations,
            setCallsMeetingInvitations = setCallsMeetingInvitations,
            getCallsMeetingReminders = getCallsMeetingReminders,
            setCallsMeetingReminders = setCallsMeetingReminders,
            sendStatisticsMeetingsUseCase = sendStatisticsMeetings,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        setupUnderTest()
        underTest.uiState.test {
            val initial = awaitItem()
            Truth.assertThat(initial.soundNotifications).isNull()
            Truth.assertThat(initial.callsMeetingInvitations).isNull()
            Truth.assertThat(initial.callsMeetingReminders).isNull()
        }
    }

    @Test
    fun `test that the option returned by getCallsSoundNotifications is set the status of call notification sounds`() =
        runTest {
            setupUnderTest()
            whenever(monitorCallSoundEnabledUseCase()).thenReturn(flowOf(CallsSoundEnabledState.Enabled))

            underTest.uiState.map { it.soundNotifications }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                scheduler.advanceUntilIdle()
                Truth.assertThat(awaitItem()).isEqualTo(CallsSoundEnabledState.Enabled)
            }
        }

    @Test
    fun `test that status of call notification sounds is updated when a new value is emitted`() =
        runTest {
            setupUnderTest()
            whenever(monitorCallSoundEnabledUseCase()).thenReturn(
                flowOf(
                    CallsSoundEnabledState.Enabled, CallsSoundEnabledState.Disabled
                )
            )

            underTest.uiState.map { it.soundNotifications }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                scheduler.advanceUntilIdle()
                underTest.setNewCallsSoundNotifications(true)
                Truth.assertThat(awaitItem()).isEqualTo(CallsSoundEnabledState.Enabled)
                underTest.setNewCallsSoundNotifications(false)
                Truth.assertThat(awaitItem()).isEqualTo(CallsSoundEnabledState.Disabled)
            }
        }

    @Test
    fun `test that setNewCallsSoundNotifications calls the set use case with the correct value`() =
        runTest {
            setupUnderTest()
            underTest.setNewCallsSoundNotifications(false)
            scheduler.advanceUntilIdle()
            verify(setCallsSoundEnabledStateUseCase).invoke(CallsSoundEnabledState.Disabled)
        }

    @Test
    fun `test that the option returned by getCallsMeetingInvitations is set as the status of calls meeting invitations`() =
        runTest {
            setupUnderTest()
            whenever(getCallsMeetingInvitations()).thenReturn(flowOf(CallsMeetingInvitations.Enabled))

            underTest.uiState.map { it.callsMeetingInvitations }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                scheduler.advanceUntilIdle()
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingInvitations.Enabled)
            }
        }

    @Test
    fun `test that status of calls meeting invitations is updated when a new value is emitted`() =
        runTest {
            setupUnderTest()
            whenever(getCallsMeetingInvitations()).thenReturn(
                flowOf(
                    CallsMeetingInvitations.Enabled, CallsMeetingInvitations.Disabled
                )
            )

            underTest.uiState.map { it.callsMeetingInvitations }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                scheduler.advanceUntilIdle()
                underTest.setNewCallsMeetingInvitations(true)
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingInvitations.Enabled)
                underTest.setNewCallsMeetingInvitations(false)
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingInvitations.Disabled)
            }
        }

    @Test
    fun `test that setNewCallsMeetingInvitations calls the set use case with the correct value`() =
        runTest {
            setupUnderTest()
            underTest.setNewCallsMeetingInvitations(false)
            scheduler.advanceUntilIdle()
            verify(setCallsMeetingInvitations).invoke(CallsMeetingInvitations.Disabled)
        }

    @Test
    fun `test that the option returned by getCallsMeetingReminders is set as the status of calls meeting reminders`() =
        runTest {
            setupUnderTest()
            whenever(getCallsMeetingReminders()).thenReturn(flowOf(CallsMeetingReminders.Enabled))

            underTest.uiState.map { it.callsMeetingReminders }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                scheduler.advanceUntilIdle()
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingReminders.Enabled)
            }
        }

    @Test
    fun `test that status of calls meeting reminders is updated when a new value is emitted`() =
        runTest {
            setupUnderTest()
            whenever(getCallsMeetingReminders()).thenReturn(
                flowOf(
                    CallsMeetingReminders.Enabled, CallsMeetingReminders.Disabled
                )
            )

            underTest.uiState.map { it.callsMeetingReminders }.distinctUntilChanged().test {
                Truth.assertThat(awaitItem()).isNull()
                scheduler.advanceUntilIdle()
                underTest.setNewCallsMeetingReminders(true)
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingReminders.Enabled)
                underTest.setNewCallsMeetingReminders(false)
                Truth.assertThat(awaitItem()).isEqualTo(CallsMeetingReminders.Disabled)
            }
        }

    @Test
    fun `test that setNewCallsMeetingReminders calls the set use case with the correct value`() =
        runTest {
            setupUnderTest()
            underTest.setNewCallsMeetingReminders(false)
            scheduler.advanceUntilIdle()
            verify(setCallsMeetingReminders).invoke(CallsMeetingReminders.Disabled)
        }

    companion object {
        private val scheduler = TestCoroutineScheduler()
        private val standardDispatcher = StandardTestDispatcher(scheduler)

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(standardDispatcher)
    }
}
