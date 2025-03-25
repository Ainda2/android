package mega.privacy.android.app.presentation.meeting.chat.view.message.management.permission

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatManagementMessageView
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Permission change message view
 *
 * @param message
 * @param modifier
 * @param viewModel
 */
@Composable
fun PermissionChangeMessageView(
    message: PermissionChangeMessage,
    modifier: Modifier = Modifier,
    viewModel: PermissionChangeMessageViewModel = hiltViewModel(),
) {
    var ownerActionFullName by remember { mutableStateOf("") }
    var targetActionFullName by remember { mutableStateOf("") }
    val context: Context = LocalContext.current
    LaunchedEffect(Unit) {
        val myUserHandle = viewModel.getMyUserHandle()
        ownerActionFullName = if (message.isMine) {
            viewModel.getMyFullName()
        } else {
            viewModel.getParticipantFullName(message.userHandle)
        } ?: context.getString(R.string.unknown_name_label)
        targetActionFullName = if (message.userHandle == message.handleOfAction) {
            ownerActionFullName
        } else {
            if (message.handleOfAction == myUserHandle) {
                viewModel.getMyFullName()
            } else {
                viewModel.getParticipantFullName(message.handleOfAction)
            } ?: context.getString(R.string.unknown_name_label)
        }
    }
    PermissionChangeMessageView(
        message = message,
        ownerActionFullName = ownerActionFullName,
        targetActionFullName = targetActionFullName,
        modifier = modifier
    )
}

/**
 * Permission change message view
 *
 * @param message
 * @param ownerActionFullName
 * @param targetActionFullName
 * @param modifier
 */
@Composable
fun PermissionChangeMessageView(
    message: PermissionChangeMessage,
    ownerActionFullName: String,
    targetActionFullName: String,
    modifier: Modifier = Modifier,
) {
    val permissionChangeMessage = when (message.privilege) {
        ChatRoomPermission.Moderator -> stringResource(
            id = R.string.chat_chat_room_message_permissions_changed_to_host,
            targetActionFullName,
            ownerActionFullName
        )

        ChatRoomPermission.Standard -> stringResource(
            id = R.string.chat_chat_room_message_permissions_changed_to_standard,
            targetActionFullName,
            ownerActionFullName
        )

        ChatRoomPermission.ReadOnly -> stringResource(
            id = R.string.chat_chat_room_message_permissions_changed_to_read_only,
            targetActionFullName,
            ownerActionFullName
        )

        else -> ""
    }

    ChatManagementMessageView(
        modifier = modifier,
        text = permissionChangeMessage,
        styles = mapOf(
            SpanIndicator('A') to MegaSpanStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextColor.Primary
            ),
            SpanIndicator('B') to MegaSpanStyle(
                SpanStyle(),
                color = TextColor.Secondary
            ),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun PermissionChangeMessageViewPreview(
    @PreviewParameter(ChatRoomPermissionProvider::class) permission: ChatRoomPermission,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        PermissionChangeMessageView(
            message = PermissionChangeMessage(
                chatId = 1L,
                msgId = 0,
                time = 0,
                isDeletable = false,
                isEditable = false,
                isMine = true,
                userHandle = 0,
                privilege = permission,
                handleOfAction = 0,
                reactions = emptyList(),
                status = ChatMessageStatus.UNKNOWN,
                content = null
            ),
            ownerActionFullName = "Owner",
            targetActionFullName = "Target",
        )
    }
}

private class ChatRoomPermissionProvider :
    CollectionPreviewParameterProvider<ChatRoomPermission>(
        listOf(
            ChatRoomPermission.Moderator,
            ChatRoomPermission.Standard,
            ChatRoomPermission.ReadOnly
        )
    )