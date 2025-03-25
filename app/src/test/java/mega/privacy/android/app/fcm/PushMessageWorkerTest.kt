package mega.privacy.android.app.fcm

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.DefaultWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.SystemClock
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.notifications.ChatMessageNotificationManager
import mega.privacy.android.app.notifications.PromoPushNotificationManager
import mega.privacy.android.app.notifications.ScheduledMeetingPushMessageNotificationManager
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.data.mapper.pushmessage.PushMessageMapper
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.pushes.PushMessage
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.EmptyFolderException
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotifiableUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SetFakeIncomingCallStateUseCase
import mega.privacy.android.domain.usecase.notifications.GetChatMessageNotificationDataUseCase
import mega.privacy.android.domain.usecase.notifications.PushReceivedUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PushMessageWorkerTest {

    private lateinit var underTest: PushMessageWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val backgroundFastLoginUseCase = mock<BackgroundFastLoginUseCase>()
    private val pushReceivedUseCase = mock<PushReceivedUseCase>()
    private val retryPendingConnectionsUseCase = mock<RetryPendingConnectionsUseCase>()
    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private val pushMessageMapper = mock<PushMessageMapper>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val scheduledMeetingPushMessageNotificationManager =
        mock<ScheduledMeetingPushMessageNotificationManager>()
    private val promoPushNotificationManager = mock<PromoPushNotificationManager>()
    private val chatMessageNotificationManager = mock<ChatMessageNotificationManager>()
    private val callsPreferencesGateway = mock<CallsPreferencesGateway>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val isChatNotifiableUseCase = mock<IsChatNotifiableUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val getChatMessageNotificationDataUseCase =
        mock<GetChatMessageNotificationDataUseCase>()
    private val fileDurationMapper = mock<FileDurationMapper>()
    private val setFakeIncomingCallStateUseCase = mock<SetFakeIncomingCallStateUseCase>()
    private val isChatStatusConnectedForCallUseCase = mock<IsChatStatusConnectedForCallUseCase>()
    private val monitorChatConnectionStateUseCase = mock<MonitorChatConnectionStateUseCase>()
    private val getMyUserHandleUseCase = mock<GetMyUserHandleUseCase>()
    private val getChatCallUseCase = mock<GetChatCallUseCase>()
    private val ioDispatcher = UnconfinedTestDispatcher()


    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)

        underTest = PushMessageWorker(
            context = context,
            workerParams = WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                1,
                executor,
                Dispatchers.Unconfined,
                workExecutor,
                DefaultWorkerFactory,
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(
                    workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            pushReceivedUseCase = pushReceivedUseCase,
            retryPendingConnectionsUseCase = retryPendingConnectionsUseCase,
            pushMessageMapper = pushMessageMapper,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            scheduledMeetingPushMessageNotificationManager = scheduledMeetingPushMessageNotificationManager,
            callsPreferencesGateway = callsPreferencesGateway,
            notificationManager = notificationManager,
            isChatNotifiableUseCase = isChatNotifiableUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            fileDurationMapper = fileDurationMapper,
            promoPushNotificationManager = promoPushNotificationManager,
            getChatMessageNotificationDataUseCase = getChatMessageNotificationDataUseCase,
            chatMessageNotificationManager = chatMessageNotificationManager,
            setFakeIncomingCallStateUseCase = setFakeIncomingCallStateUseCase,
            isChatStatusConnectedForCallUseCase = isChatStatusConnectedForCallUseCase,
            monitorChatConnectionStateUseCase = monitorChatConnectionStateUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            getMyUserHandleUseCase = getMyUserHandleUseCase,
            getChatCallUseCase = getChatCallUseCase,
            ioDispatcher = ioDispatcher,
            loginMutex = mock()
        )

        whenever(notificationManager.notify(any(), any())).then(mock())
        whenever(pushMessageMapper(any())).thenReturn(PushMessage.CallPushMessage(chatId = 123L))
        whenever(notificationManager.areNotificationsEnabled()).thenReturn(false)
        whenever(callsPreferencesGateway.getCallsMeetingRemindersPreference())
            .thenReturn(flowOf(CallsMeetingReminders.Disabled))
        monitorChatCallUpdatesUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        whenever(monitorChatConnectionStateUseCase()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that doWork returns failure if fast login failed`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenThrow(SessionNotRetrievedException::class.java)
        val result = underTest.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that retryPendingConnections is invoked if fast login success`() = runTest {
        whenever(backgroundFastLoginUseCase()).thenReturn("good_session")
        whenever(pushReceivedUseCase.invoke(any())).thenReturn(Unit)
        whenever(isChatStatusConnectedForCallUseCase(any())).thenReturn(true)

        underTest.doWork()
        verify(retryPendingConnectionsUseCase).invoke(false)
    }

    @Test
    fun `test that MegaChat is initialised if rootNode exists and retryPendingConnections raises ChatNotInitializedException`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn("good_session")
            whenever(retryPendingConnectionsUseCase(any())).thenThrow(ChatNotInitializedErrorStatus())
            whenever(isChatStatusConnectedForCallUseCase(any())).thenReturn(true)

            underTest.doWork()
            verify(initialiseMegaChatUseCase).invoke("good_session")
        }

    @Test
    fun `test that initialiseMegaChat is not invoked if rootNode exists and retryPendingConnections raises exception other than ChatNotInitializedException`() =
        runTest {
            whenever(backgroundFastLoginUseCase()).thenReturn("good_session")
            whenever(retryPendingConnectionsUseCase(any())).thenThrow(
                EmptyFolderException()
            )
            whenever(isChatStatusConnectedForCallUseCase(any())).thenReturn(true)

            underTest.doWork()
            verifyNoInteractions(initialiseMegaChatUseCase)
        }

    @Test
    fun `test that doWork returns failure if rootNode exists and retryPendingConnections raises ChatNotInitializedException and initialiseMegaChat fails`() =
        runTest {
            val sessionId = "good_session"
            whenever(backgroundFastLoginUseCase()).thenReturn(sessionId)
            whenever(retryPendingConnectionsUseCase(any())).thenThrow(ChatNotInitializedErrorStatus())
            whenever(initialiseMegaChatUseCase(sessionId)).thenThrow(ChatNotInitializedErrorStatus())

            val result = underTest.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `test that ChatPushMessage is triggered as expected and getChatMessageNotificationDataUseCase is not called when chat is not notifiable`() {
        runTest {
            val push = PushMessage.ChatPushMessage(true, 1L, 2L)
            whenever(pushMessageMapper(any())).thenReturn(push)
            whenever(pushReceivedUseCase(push.shouldBeep)).thenReturn(Unit)
            whenever(isChatNotifiableUseCase(push.chatId)).thenReturn(false)
            val result = underTest.doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verifyNoInteractions(getChatMessageNotificationDataUseCase)
        }
    }

    @Test
    fun `test that ScheduledMeetingPushMessage are triggered as expected`() {
        runTest {
            val pushMessage = PushMessage.ScheduledMeetingPushMessage(
                schedId = -1L,
                userHandle = -1L,
                chatRoomHandle = -1L,
                title = null,
                description = null,
                startTimestamp = 0L,
                endTimestamp = 0L,
                timezone = null,
                isStartReminder = false,
            )
            whenever(pushMessageMapper(any())).thenReturn(pushMessage)
            whenever(notificationManager.areNotificationsEnabled()).thenReturn(true)
            whenever(getChatRoomUseCase.invoke(any())).thenReturn(mock())
            whenever(callsPreferencesGateway.getCallsMeetingRemindersPreference())
                .thenReturn(flowOf(CallsMeetingReminders.Enabled))

            val result = underTest.doWork()

            verify(getChatRoomUseCase).invoke(-1L)
            verify(scheduledMeetingPushMessageNotificationManager).show(context, pushMessage)
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }
    }

    @Test
    fun `test that PromoPushMessage are triggered as expected`() {
        runTest {
            val pushMessage = PushMessage.PromoPushMessage(
                id = 1,
                title = "Test title",
                description = "Test description",
                redirectLink = "https://mega.io",
                imagePath = null,
                subtitle = null,
                sound = null,
            )
            whenever(pushMessageMapper(any())).thenReturn(pushMessage)
            whenever(notificationManager.areNotificationsEnabled()).thenReturn(true)
            val result = underTest.doWork()

            verify(promoPushNotificationManager).show(context, pushMessage)
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }
    }
}