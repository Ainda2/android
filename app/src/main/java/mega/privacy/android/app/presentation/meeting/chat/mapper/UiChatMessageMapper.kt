package mega.privacy.android.app.presentation.meeting.chat.mapper

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ui.AlterParticipantsUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.CallUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.ChatGiphyUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.ChatLinkCreatedUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.ChatLinkRemovedUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.ChatRichLinkUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.InvalidUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.PermissionChangeUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.TextUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.TitleChangeUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.TruncateHistoryUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkRemovedMessage
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import mega.privacy.android.domain.entity.chat.messages.management.TruncateHistoryMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import javax.inject.Inject

/**
 * Mapper to convert a [TypedMessage] to a [UiChatMessage]
 *
 */
class UiChatMessageMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param message
     * @param isOneToOne
     */
    operator fun invoke(
        message: TypedMessage,
        isOneToOne: Boolean,
        showAvatar: Boolean,
        showTime: Boolean,
        showDate: Boolean,
    ): UiChatMessage {
        return when (message) {
            is TextMessage -> TextUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            is CallMessage -> CallUiMessage(
                message = message,
                isOneToOneChat = isOneToOne,
                showDate = showDate
            )

            is RichPreviewMessage -> ChatRichLinkUiMessage(
                message = message,
                showDate = showDate,
                showAvatar = showAvatar,
                showTime = showTime
            )

            is GiphyMessage -> ChatGiphyUiMessage(
                message = message,
                showDate = showDate,
                showAvatar = showAvatar,
                showTime = showTime
            )

            is AlterParticipantsMessage -> AlterParticipantsUiMessage(
                message = message,
                showDate = showDate,
            )

            is PermissionChangeMessage -> PermissionChangeUiMessage(
                message = message,
                showDate = showDate
            )

            is TitleChangeMessage -> TitleChangeUiMessage(
                message = message,
                showDate = showDate
            )

            is TruncateHistoryMessage -> TruncateHistoryUiMessage(
                message = message,
                showDate = showDate
            )

            is ChatLinkCreatedMessage -> ChatLinkCreatedUiMessage(
                message = message,
                showDate = showDate
            )

            is ChatLinkRemovedMessage -> ChatLinkRemovedUiMessage(
                message = message,
                showDate = showDate
            )

            is InvalidMessage -> mapInvalidMessage(message, showAvatar, showTime, showDate)

            else -> object : UiChatMessage {
                override val contentComposable: @Composable (RowScope.() -> Unit) = {

                }
                override val avatarComposable: @Composable (RowScope.() -> Unit)? = null

                override val message: TypedMessage = message

                override val showAvatar: Boolean = false

                override val showTime: Boolean = true

                override val showDate: Boolean = showDate
            }
        }
    }

    private fun mapInvalidMessage(
        message: InvalidMessage,
        showAvatar: Boolean,
        showTime: Boolean,
        showDate: Boolean,
    ): InvalidUiMessage {
        return when (message) {
            is SignatureInvalidMessage -> InvalidUiMessage.SignatureInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            is FormatInvalidMessage -> InvalidUiMessage.FormatInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )

            else -> InvalidUiMessage.UnrecognizableInvalidUiMessage(
                message = message,
                showAvatar = showAvatar,
                showTime = showTime,
                showDate = showDate
            )
        }
    }
}