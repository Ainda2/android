package mega.privacy.android.app.presentation.settings.reportissue.view

import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.legacy.core.ui.controls.dialogs.MegaDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
fun DiscardReportDialog(
    onDiscardCancelled: () -> Unit,
    onDiscard: () -> Unit,
) {
    MegaDialog(
        onDismissRequest = onDiscardCancelled,
        titleString = stringResource(id = R.string.settings_help_report_issue_discard_dialog_title),
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                modifier = Modifier
            ) {
                Text(
                    text = stringResource(id = R.string.settings_help_report_issue_discard_button),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.accent_050) else colorResource(
                        id = R.color.accent_900
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscardCancelled,
                modifier = Modifier
            ) {
                Text(
                    text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.accent_050) else colorResource(
                        id = R.color.accent_900
                    )
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    )
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewDiscardReportDialog() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DiscardReportDialog(onDiscardCancelled = {},
            onDiscard = {})
    }
}