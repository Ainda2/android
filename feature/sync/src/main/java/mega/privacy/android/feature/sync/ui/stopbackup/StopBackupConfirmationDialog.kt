package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaRadioButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body2medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.feature.sync.ui.model.StopBackupOption

@Composable
internal fun StopBackupConfirmationDialog(
    onConfirm: (option: StopBackupOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by rememberSaveable { mutableStateOf(StopBackupOption.MOVE) }

    ConfirmationDialog(
        title = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_title),
        text = {
            Column(
                modifier = modifier
                    .selectableGroup()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag(STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG)
            ) {
                MegaText(
                    text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_text),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1,
                )
                Row(
                    modifier = modifier
                        .selectable(
                            selected = selectedOption == StopBackupOption.MOVE,
                            onClick = { selectedOption = StopBackupOption.MOVE },
                            role = Role.RadioButton,
                        )
                        .testTag(STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_ROW_TEST_TAG)
                ) {
                    MegaRadioButton(
                        selected = selectedOption == StopBackupOption.MOVE,
                        onClick = { selectedOption = StopBackupOption.MOVE },
                        modifier = modifier.padding(top = 8.dp, end = 12.dp),
                    )
                    Column(modifier = modifier.padding(top = 24.dp)) {
                        MegaText(
                            text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_move_option),
                            textColor = TextColor.Primary,
                            style = MaterialTheme.typography.subtitle1,
                        )
                        MegaText(
                            text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_move_option_text),
                            textColor = TextColor.Primary,
                            modifier = modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.body2,
                        )
                        Row(modifier = modifier.padding(top = 8.dp)) {
                            MegaText(
                                text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_move_option_destination_label),
                                textColor = TextColor.Primary,
                                modifier = modifier.padding(end = 4.dp),
                                style = MaterialTheme.typography.body2,
                            )
                            MegaText(
                                text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_move_option_select_destination_button),
                                textColor = TextColor.Primary,
                                modifier = modifier.testTag(
                                    STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_SELECT_DESTINATION_TEST_TAG
                                ),
                                style = MaterialTheme.typography.body2medium,
                            )
                        }
                    }
                }
                Row(
                    modifier = modifier
                        .selectable(
                            selected = selectedOption == StopBackupOption.DELETE,
                            onClick = { selectedOption = StopBackupOption.DELETE },
                            role = Role.RadioButton,
                        )
                        .testTag(STOP_BACKUP_CONFIRMATION_DIALOG_DELETE_OPTION_ROW_TEST_TAG)
                ) {
                    MegaRadioButton(
                        selected = selectedOption == StopBackupOption.DELETE,
                        onClick = { selectedOption = StopBackupOption.DELETE },
                        modifier = modifier.padding(top = 2.dp, end = 12.dp),
                    )
                    Column(modifier = modifier.padding(top = 16.dp)) {
                        MegaText(
                            text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_delete_option),
                            textColor = TextColor.Primary,
                            style = MaterialTheme.typography.subtitle1
                        )
                        MegaText(
                            text = stringResource(id = sharedR.string.sync_stop_backup_confirm_dialog_delete_option_text),
                            textColor = TextColor.Primary,
                            modifier = modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        },
        confirmButtonText = stringResource(id = sharedR.string.general_confirm_button),
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onConfirm = { onConfirm(selectedOption) },
        onDismiss = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun StopBackupConfirmationDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaScaffold {
            StopBackupConfirmationDialog(
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

internal const val STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG =
    "stop_backup_confirmation_dialog:body"
internal const val STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_ROW_TEST_TAG =
    "stop_backup_confirmation_dialog:move_option_row"
internal const val STOP_BACKUP_CONFIRMATION_DIALOG_DELETE_OPTION_ROW_TEST_TAG =
    "stop_backup_confirmation_dialog:delete_option_row"
internal const val STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_SELECT_DESTINATION_TEST_TAG =
    "stop_backup_confirmation_dialog:move_option_select_destination"