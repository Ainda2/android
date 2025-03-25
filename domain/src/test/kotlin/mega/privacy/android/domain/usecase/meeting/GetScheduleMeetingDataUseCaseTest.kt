package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetScheduleMeetingDataUseCaseTest {

    private lateinit var underTest: GetScheduleMeetingDataUseCase

    private val getScheduledMeetingByChatUseCase = mock<GetScheduledMeetingByChatUseCase>()
    private val fetchScheduledMeetingOccurrencesByChatUseCase =
        mock<FetchScheduledMeetingOccurrencesByChatUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()

    @Before
    fun setUp() {
        underTest = GetScheduleMeetingDataUseCase(
            getScheduledMeetingByChatUseCase,
            GetNextSchedMeetingOccurrenceUseCase(fetchScheduledMeetingOccurrencesByChatUseCase),
            getChatRoomUseCase,
        )
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `test that getMeetingScheduleData return null`() =
        runTest {
            val chatId = 123L
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(null)

            val result = underTest.invoke(chatId) { _, _ -> "" }

            assertThat(result).isNull()
        }

    @Test
    fun `test that getChatRoom is called accordingly`() =
        runTest {
            val chatId = 123L
            val chat = mock<ChatRoom> {
                on { chatId }.thenReturn(chatId)
            }
            whenever(getChatRoomUseCase(chatId)).thenReturn(chat)
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            underTest.invoke(chatId) { _, _ -> "" }

            verify(getChatRoomUseCase).invoke(chatId)
        }

    @Test
    fun `test that getChatRoom return error accordingly`() =
        runTest {
            val chatId = 123L
            whenever(getChatRoomUseCase(chatId)).thenReturn(null)
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            assertThrows<IllegalStateException> {
                underTest.invoke(chatId) { _, _ -> "" }
            }
        }

    @Test
    fun `test that getNextSchedMeetingOccurrence is called accordingly`() =
        runTest {
            val chatId = 123L
            val now = Instant.now().minus(1L, ChronoUnit.HALF_DAYS).epochSecond
            val meetingOccurrence = ChatScheduledMeetingOccurr(
                schedId = 456L,
                startDateTime = Instant.now().plusSeconds(60).epochSecond,
                endDateTime = Instant.now().plusSeconds(120).epochSecond,
                isCancelled = false
            )

            whenever(getChatRoomUseCase(chatId)).thenReturn(mock())
            whenever(getScheduledMeetingByChatUseCase(chatId)).thenReturn(
                listOf(
                    ChatScheduledMeeting(
                        chatId = chatId,
                        parentSchedId = -1L,
                        isCanceled = false
                    )
                )
            )

            whenever(
                fetchScheduledMeetingOccurrencesByChatUseCase(chatId, now)
            ).thenReturn(listOf(meetingOccurrence))

            underTest.invoke(chatId) { _, _ -> "" }

            verify(fetchScheduledMeetingOccurrencesByChatUseCase).invoke(chatId, now)
        }
}
