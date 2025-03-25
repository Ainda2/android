package mega.privacy.android.domain.usecase.notifications

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.notifications.ChatMessageNotificationData
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import javax.inject.Inject

/**
 * Use case for getting all the required data for a chat message notification.
 */
class GetChatMessageNotificationDataUseCase @Inject constructor(
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val getMessageSenderNameUseCase: GetMessageSenderNameUseCase,
    private val getUserAvatarUseCase: GetUserAvatarUseCase,
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase,
    private val getChatMessageNotificationBehaviourUseCase: GetChatMessageNotificationBehaviourUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke.
     *
     * @param shouldBeep True if the notification should beep, false otherwise.
     * @param chatId Chat id.
     * @param msgId Message id.
     * @param defaultSound Default notification sound.
     */
    suspend operator fun invoke(
        shouldBeep: Boolean,
        chatId: Long,
        msgId: Long,
        defaultSound: String?,
    ): ChatMessageNotificationData? = withContext(ioDispatcher) {
        val chatRoom = getChatRoomUseCase(chatId) ?: return@withContext null
        getChatMessageUseCase(chatId, msgId)?.let { message ->
            when {
                message.status == ChatMessageStatus.SEEN -> {
                    ChatMessageNotificationData.SeenMessage(chat = chatRoom, msg = message)
                }

                message.isDeleted -> {
                    ChatMessageNotificationData.DeletedMessage(chat = chatRoom, msg = message)
                }

                else -> {
                    val senderName = async {
                        runCatching {
                            getMessageSenderNameUseCase(
                                message.userHandle,
                                chatId
                            )
                        }.getOrNull()
                    }
                    val senderAvatar = async {
                        runCatching { getUserAvatarUseCase(message.userHandle) }.getOrNull()
                    }
                    val senderAvatarColor = getUserAvatarColorUseCase(message.userHandle)
                    val notificationBehaviour =
                        getChatMessageNotificationBehaviourUseCase(shouldBeep, defaultSound)

                    ChatMessageNotificationData.Message(
                        chat = chatRoom,
                        msg = message,
                        senderName = senderName.await().orEmpty(),
                        senderAvatar = senderAvatar.await(),
                        senderAvatarColor = senderAvatarColor,
                        notificationBehaviour = notificationBehaviour
                    )
                }
            }
        }
    }
}