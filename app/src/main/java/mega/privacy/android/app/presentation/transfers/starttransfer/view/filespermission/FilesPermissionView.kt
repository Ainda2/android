package mega.privacy.android.app.presentation.transfers.starttransfer.view.filespermission

import mega.privacy.android.shared.resources.R as sharedResR
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.FullScreenDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun FilesPermissionDialog(
    onDoNotShowAgainClick: () -> Unit,
    onStartTransferAndDismiss: () -> Unit,
) {
    val manageStoragePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onStartTransferAndDismiss()
        }

    FullScreenDialog(
        onDismissRequest = onStartTransferAndDismiss,
    ) {
        FilesPermissionView(
            onAllowClick = {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        manageStoragePermissionLauncher.launch(
                            Intent(
                                ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                            )
                        )
                    }
                }.onFailure { _ ->
                    // handle case activity not found
                    onStartTransferAndDismiss()
                }
            },
            onDoNotShowAgainClick = onDoNotShowAgainClick,
            onStartTransferAndDismiss = onStartTransferAndDismiss,
        )
    }
}

@Composable
internal fun FilesPermissionView(
    onAllowClick: () -> Unit,
    onDoNotShowAgainClick: () -> Unit,
    onStartTransferAndDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .verticalScroll(rememberScrollState())
        .testTag(FILES_PERMISSION_VIEW_TAG)
        .fillMaxSize()
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Image(
        painter = painterResource(id = R.drawable.ic_files),
        contentDescription = "Files permission image",
        modifier = Modifier
            .testTag(IMAGE_TAG)
            .padding(top = 134.dp)
            .size(128.dp),
    )
    MegaText(
        text = stringResource(sharedResR.string.files_permission_screen_title),
        textColor = TextColor.Primary,
        style = MaterialTheme.typography.h6,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag(TITLE_TAG)
            .padding(top = 24.dp),
    )
    MegaText(
        text = stringResource(sharedResR.string.files_permission_screen_message),
        textColor = TextColor.Secondary,
        style = MaterialTheme.typography.subtitle1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag(MESSAGE_TAG)
            .padding(top = 20.dp),
    )
    RaisedDefaultMegaButton(
        text = stringResource(R.string.dialog_positive_button_allow_permission),
        onClick = onAllowClick,
        modifier = Modifier
            .testTag(ALLOW_BUTTON_TAG)
            .fillMaxWidth()
            .padding(top = 68.dp),
    )
    OutlinedMegaButton(
        text = stringResource(R.string.permissions_not_now_button),
        onClick = onStartTransferAndDismiss,
        rounded = false,
        modifier = Modifier
            .testTag(NOT_NOW_BUTTON_TAG)
            .fillMaxWidth()
            .padding(top = 20.dp),
    )
    TextMegaButton(
        text = stringResource(sharedResR.string.files_permission_screen_do_not_show_again_button),
        onClick = {
            onDoNotShowAgainClick()
            onStartTransferAndDismiss()
        },
        modifier = Modifier
            .testTag(DO_NOT_SHOW_AGAIN_BUTTON_TAG)
            .padding(top = 20.dp, bottom = 68.dp),
    )
}

@CombinedThemePreviews
@Composable
private fun FilesPermissionViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FilesPermissionDialog(
            onDoNotShowAgainClick = {},
            onStartTransferAndDismiss = {},
        )
    }
}

internal const val FILES_PERMISSION_VIEW_TAG = "files_permission_view"
internal const val IMAGE_TAG = "$FILES_PERMISSION_VIEW_TAG:image"
internal const val TITLE_TAG = "$FILES_PERMISSION_VIEW_TAG:title"
internal const val MESSAGE_TAG = "$FILES_PERMISSION_VIEW_TAG:message"
internal const val ALLOW_BUTTON_TAG = "$FILES_PERMISSION_VIEW_TAG:allow_button"
internal const val NOT_NOW_BUTTON_TAG = "$FILES_PERMISSION_VIEW_TAG:not_now_button"
internal const val DO_NOT_SHOW_AGAIN_BUTTON_TAG =
    "$FILES_PERMISSION_VIEW_TAG:do_not_show_again_button"