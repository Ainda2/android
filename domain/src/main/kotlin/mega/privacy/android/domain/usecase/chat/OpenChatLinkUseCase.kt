package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.OpenChatPreviewUseCase
import javax.inject.Inject

/**
 * Opens a chat from a link and joins if required.
 *
 * @property joinPublicChatUseCase
 * @property checkChatLinkUseCase
 */
class OpenChatLinkUseCase @Inject constructor(
    private val checkChatLinkUseCase: CheckChatLinkUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val openChatPreviewUseCase: OpenChatPreviewUseCase,
    private val setChatOpeningWithLinkUseCase: SetChatOpeningWithLinkUseCase,
) {
    /**
     * Invoke
     *
     * @param chatLink Chat link.
     * @param chatId Chat id. Mandatory in case of opening the chat conversation.
     * @param requireJoin True if needs to be joined to the chat.
     * @return chatId
     */
    suspend operator fun invoke(
        chatLink: String,
        chatId: Long? = null,
        requireJoin: Boolean = false,
    ): Long {
        var previouslyJoined = false
        var chatPublicHandle: Long? = null
        val chatRoom: ChatRoom = chatId?.let {
            val chat = getChatRoomUseCase(it)
            if (chat == null || !chat.isActive) {
                openChatPreviewUseCase(chatLink).let { chatPreview ->
                    previouslyJoined = chatPreview.exist
                    chatPublicHandle = chatPreview.request.userHandle
                }
                getChatRoomUseCase(chatId)
            } else {
                chat
            }
        } ?: run {
            openChatPreviewUseCase(chatLink).let { chatPreview ->
                previouslyJoined = chatPreview.exist
                chatPublicHandle = chatPreview.request.userHandle
                getChatRoomUseCase(chatPreview.request.chatHandle)
            }
        } ?: throw throw ChatRoomDoesNotExistException()

        if (requireJoin || previouslyJoined) {
            setChatOpeningWithLinkUseCase(chatRoom.chatId)
            joinPublicChatUseCase(
                chatId = chatRoom.chatId,
                chatPublicHandle = chatPublicHandle.takeIf { chatRoom.isPreview.not() && chatRoom.isActive.not() }
            )
        }
        return chatRoom.chatId
    }
}
