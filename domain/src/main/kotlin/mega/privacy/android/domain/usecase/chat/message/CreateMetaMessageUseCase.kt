package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

/**
 * Create meta message use case.
 */
class CreateMetaMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        when (metaType) {
            ContainsMetaType.RICH_PREVIEW -> RichPreviewMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                chatRichPreviewInfo = chatRichPreviewInfo,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                reactions = reactions,
                content = content.orEmpty(),
                isEdited = isEdited,
                status = status,
                textMessage = textMessage.orEmpty(),
            )

            ContainsMetaType.GEOLOCATION -> LocationMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                chatGeolocationInfo = chatGeolocationInfo,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                reactions = reactions,
                isEdited = isEdited,
                status = status,
                textMessage = textMessage.orEmpty(),
            )

            ContainsMetaType.GIPHY -> GiphyMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                chatGifInfo = chatGifInfo,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                reactions = reactions,
                status = status,
                textMessage = textMessage.orEmpty()
            )

            else -> InvalidMetaMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                reactions = reactions,
                status = status,
                textMessage = textMessage.orEmpty(),
            )
        }
    }
}