package mega.privacy.android.app.presentation.contactinfo.view

import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun UpdateNickNameDialog(
    hasAlias: Boolean,
    updateNickName: (String?) -> Unit,
    updateNickNameDialogVisibility: (Boolean) -> Unit,
    nickName: String? = null,
) {
    InputDialog(
        modifier = Modifier.testTag(UPDATE_NICKNAME_DIALOG_TAG),
        title = stringResource(id = if (hasAlias) R.string.edit_nickname else R.string.add_nickname),
        text = nickName.orEmpty(),
        hint = nickName ?: stringResource(R.string.nickname_title),
        confirmButtonText = stringResource(id = R.string.button_set),
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onConfirm = {
            updateNickName(it)
            updateNickNameDialogVisibility(false)
        },
        onDismiss = {
            updateNickNameDialogVisibility(false)
        }
    )
}

internal const val UPDATE_NICKNAME_DIALOG_TAG = "update_nickname:input_dialog"

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "VerticalButtonDialog")
@Composable
private fun PreviewVerticalButtonDialogView() {

    OriginalTheme(isDark = isSystemInDarkTheme()) {
        UpdateNickNameDialog(
            hasAlias = true,
            nickName = "Jack",
            updateNickName = {},
            updateNickNameDialogVisibility = {},
        )
    }
}