package mega.privacy.android.app.presentation.qrcode

import mega.privacy.android.icon.pack.R as iconPackR
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white

/**
 * App bar for QR code screen
 */
@Composable
internal fun QRCodeTopBar(
    context: Context,
    isQRCodeAvailable: Boolean,
    showMoreMenu: Boolean,
    onShowMoreClicked: () -> Unit,
    onMenuDismissed: () -> Unit,
    onSave: () -> Unit,
    onResetQRCode: () -> Unit,
    onDeleteQRCode: () -> Unit,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
    navigateToQrSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.section_qr_code),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = stringResource(id = R.string.general_back_button),
                    tint = MaterialTheme.colors.black_white
                )
            }
        },
        actions = {
            if (isQRCodeAvailable) {
                IconButton(
                    modifier = Modifier.testTag(SHARE_TAG),
                    onClick = onShare
                ) {
                    Icon(
                        painter = painterResource(id = iconPackR.drawable.ic_share_network_medium_regular_outline),
                        contentDescription = stringResource(id = R.string.general_share),
                        tint = MaterialTheme.colors.black_white
                    )
                }

                IconButton(
                    modifier = Modifier.testTag(MORE_TAG),
                    onClick = onShowMoreClicked
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = stringResource(id = R.string.label_more),
                        tint = MaterialTheme.colors.black_white,
                    )
                }

                DropdownMenu(
                    modifier = Modifier.testTag(DROPDOWN_TAG),
                    expanded = showMoreMenu,
                    onDismissRequest = onMenuDismissed,
                ) {
                    DropdownMenuItem(onClick = {
                        onMenuDismissed()
                        onSave()
                    }) {
                        Text(text = stringResource(id = R.string.save_action))
                    }
                    DropdownMenuItem(onClick = {
                        onMenuDismissed()
                        onGotoSettings(context, navigateToQrSettings)
                    }) {
                        Text(text = stringResource(id = R.string.action_settings))
                    }
                    DropdownMenuItem(onClick = {
                        onMenuDismissed()
                        onResetQRCode()
                    }) {
                        Text(text = stringResource(id = R.string.action_reset_qr))
                    }
                    DropdownMenuItem(onClick = {
                        onMenuDismissed()
                        onDeleteQRCode()
                    }) {
                        Text(text = stringResource(id = R.string.action_delete_qr))
                    }
                }
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp,
    )
}

private fun onGotoSettings(context: Context, navigateToQrSettings: () -> Unit) {
    navigateToQrSettings()
    (context as? Activity)?.finish()
}

@CombinedThemePreviews
@Composable
private fun PreviewQRCodeTopBar(
    @PreviewParameter(BooleanProvider::class) qrCodeAvailable: Boolean
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        QRCodeTopBar(
            context = LocalContext.current,
            isQRCodeAvailable = qrCodeAvailable,
            showMoreMenu = false,
            onShowMoreClicked = { },
            onMenuDismissed = { },
            onSave = { },
            onResetQRCode = { },
            onDeleteQRCode = { },
            onBackPressed = { },
            onShare = { },
            navigateToQrSettings = {},
        )
    }
}

internal const val SHARE_TAG = "qr_code_top_bar:icon_share"
internal const val MORE_TAG = "qr_code_top_bar:icon_more"
internal const val DROPDOWN_TAG = "qr_code_top_bar:menu_dropdown"