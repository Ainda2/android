package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CountProvider
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * The dialog to show when it is trying to delete messages.
 */
@Composable
fun DeleteMessagesConfirmationDialog(
    messagesCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) = ConfirmationDialog(
    title = stringResource(
        id = if (messagesCount == 1) R.string.confirmation_delete_one_message
        else R.string.confirmation_delete_several_messages
    ),
    cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
    confirmButtonText = stringResource(id = R.string.context_remove),
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    modifier = Modifier.testTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG)
)

@CombinedTextAndThemePreviews
@Composable
private fun DeleteMessagesConfirmationDialogPreview(
    @PreviewParameter(CountProvider::class) messagesCount: Int,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeleteMessagesConfirmationDialog(
            messagesCount = messagesCount,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

internal const val TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG =
    "chat_view:dialog_chat_remove_messages_confirmation"