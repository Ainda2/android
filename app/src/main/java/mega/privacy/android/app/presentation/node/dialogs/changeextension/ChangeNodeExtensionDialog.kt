package mega.privacy.android.app.presentation.node.dialogs.changeextension

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun ChangeNodeExtensionDialog(
    newNodeName: String,
    nodeId: Long,
    onDismiss: () -> Unit,
    viewModel: ChangeNodeExtensionDialogViewModel = hiltViewModel(),
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChangeNodeExtensionDialogBody(
            onChangeNodeExtension = {
                onDismiss()
                viewModel.handleAction(
                    ChangeNodeExtensionAction.OnChangeExtensionConfirmed(
                        nodeId,
                        newNodeName
                    )
                )
            },
            onDismiss = {
                onDismiss()
            }
        )
    }
}

@Composable
private fun ChangeNodeExtensionDialogBody(
    onChangeNodeExtension: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.file_extension_change_title),
        text = stringResource(id = R.string.file_extension_change_warning),
        confirmButtonText = stringResource(id = R.string.action_change_anyway),
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onConfirm = {
            onChangeNodeExtension()
        },
        onDismiss = {
            onDismiss()
        }
    )
}

@CombinedTextAndThemePreviews
@Composable
private fun MoveToRubbishOrDeleteNodeDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChangeNodeExtensionDialogBody(
            onChangeNodeExtension = {},
            onDismiss = {}
        )
    }
}