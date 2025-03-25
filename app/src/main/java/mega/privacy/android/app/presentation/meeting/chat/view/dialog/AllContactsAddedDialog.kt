package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * All contacts added dialog
 *
 */
@Composable
fun AllContactsAddedDialog(
    onNavigateToInviteContact: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.chat_add_participants_no_contacts_left_to_add_title),
        text = stringResource(id = R.string.chat_add_participants_no_contacts_left_to_add_message),
        confirmButtonText = stringResource(id = R.string.contact_invite),
        onDismiss = onDismiss,
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onConfirm = {
            onNavigateToInviteContact()
            onDismiss()
        },
    )
}

@CombinedThemePreviews
@Composable
private fun AllContactsAddedDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AllContactsAddedDialog(onNavigateToInviteContact = {})
    }
}