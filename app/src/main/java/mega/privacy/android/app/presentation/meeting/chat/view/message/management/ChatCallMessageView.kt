package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.ChatManagementMessage
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallStartedMessage
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Chat call message view
 *
 * @param message Call message
 * @param isOneToOneChat Whether the chat is one to one
 * @param modifier Modifier
 */
@Composable
fun ChatCallMessageView(
    message: CallMessage,
    isOneToOneChat: Boolean,
    modifier: Modifier = Modifier,
) {
    ChatManagementMessage(
        iconResId = message.getIconPainterResId(),
        text = message.getText(isOneToOneChat),
        modifier = modifier
    )
}

@DrawableRes
private fun CallMessage.getIconPainterResId(): Int {
    return when (this) {
        is CallStartedMessage -> R.drawable.ic_new_call_started
        is CallEndedMessage -> {
            when (termCode) {
                ChatMessageTermCode.ENDED, ChatMessageTermCode.BY_MODERATOR -> R.drawable.ic_new_call_ended
                ChatMessageTermCode.REJECTED -> R.drawable.ic_new_call_rejected
                ChatMessageTermCode.NO_ANSWER -> R.drawable.ic_new_call_not_answered
                ChatMessageTermCode.FAILED -> R.drawable.ic_new_call_not_answered
                ChatMessageTermCode.CANCELLED -> R.drawable.ic_new_call_cancelled
            }
        }

        else -> R.drawable.ic_new_call_started
    }
}

@Composable
private fun CallMessage.getText(isOneToOneChat: Boolean): String {
    return when (this) {
        is CallStartedMessage -> stringResource(id = R.string.call_started_messages)
        is CallEndedMessage -> {
            when (termCode) {
                ChatMessageTermCode.ENDED, ChatMessageTermCode.BY_MODERATOR -> getAppropriateStringForCallEnded(
                    isOneToOneChat,
                    duration
                )

                ChatMessageTermCode.REJECTED -> TextUtil.removeFormatPlaceholder(stringResource(R.string.call_rejected_messages))
                ChatMessageTermCode.NO_ANSWER -> TextUtil.removeFormatPlaceholder(
                    if (isMine) stringResource(R.string.call_not_answered_messages)
                    else stringResource(R.string.call_missed_messages)
                )

                ChatMessageTermCode.FAILED -> TextUtil.removeFormatPlaceholder(stringResource(R.string.call_failed_messages))
                ChatMessageTermCode.CANCELLED -> TextUtil.removeFormatPlaceholder(
                    if (isMine) stringResource(R.string.call_cancelled_messages)
                    else stringResource(R.string.call_missed_messages)
                )
            }
        }

        else -> ""
    }
}

@Composable
private fun getAppropriateStringForCallEnded(
    isOneToOne: Boolean,
    duration: Duration,
): String = with(duration.inWholeSeconds) {
    val hasDuration = this > 0
    if (!isOneToOne && !hasDuration) {
        return TextUtil.removeFormatPlaceholder(
            stringResource(R.string.group_call_ended_no_duration_message)
        )
    } else {
        val result = StringBuilder()
        result.append(
            if (isOneToOne) stringResource(R.string.call_ended_message)
            else stringResource(R.string.group_call_ended_message)
        )
        val hours = TimeUnit.SECONDS.toHours(this)
        val minutes = (TimeUnit.SECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours))
        val seconds =
            TimeUnit.SECONDS.toSeconds(this) - (TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(
                minutes
            ))

        if (hours > 0) {
            result.append(
                pluralStringResource(
                    R.plurals.plural_call_ended_messages_hours,
                    hours.toInt(),
                    hours
                )
            ).append(", ")
        }

        if (minutes > 0) {
            result.append(
                pluralStringResource(
                    R.plurals.plural_call_ended_messages_minutes,
                    minutes.toInt(),
                    minutes
                )
            ).append(", ")
        }

        result.append(
            pluralStringResource(
                R.plurals.plural_call_ended_messages_seconds,
                seconds.toInt(),
                seconds
            )
        )

        return TextUtil.removeFormatPlaceholder(result.toString())
    }
}

@CombinedThemePreviews
@Composable
private fun ChatCallMessageViewPreview(
    @PreviewParameter(ChatCallMessageViewParameterProvider::class) parameters: PreviewParameters,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatCallMessageView(
            message = parameters.message,
            isOneToOneChat = parameters.isOneToOneChat,
        )
    }
}

private class PreviewParameters(
    val message: CallMessage,
    val isOneToOneChat: Boolean,
)

private class ChatCallMessageViewParameterProvider : PreviewParameterProvider<PreviewParameters> {
    override val values: Sequence<PreviewParameters>
        get() = sequenceOf(
            PreviewParameters(
                message = CallStartedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = true
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.ENDED,
                    duration = 0.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = true
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.ENDED,
                    duration = 0.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = false
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.ENDED,
                    duration = 100.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = true
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.ENDED,
                    duration = 100.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = false
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.FAILED,
                    duration = 0.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = true
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.CANCELLED,
                    duration = 0.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = false
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.REJECTED,
                    duration = 0.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = true
            ),
            PreviewParameters(
                message = CallEndedMessage(
                    chatId = 1L,
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isDeletable = false,
                    isEditable = false,
                    isMine = true,
                    termCode = ChatMessageTermCode.NO_ANSWER,
                    duration = 0.seconds,
                    userHandle = 123L,
                    reactions = emptyList(),
                    status = ChatMessageStatus.UNKNOWN,
                    content = null
                ),
                isOneToOneChat = false
            ),
        )
}
