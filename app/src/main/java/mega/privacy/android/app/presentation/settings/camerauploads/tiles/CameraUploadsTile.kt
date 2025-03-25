package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * A [Composable] that displays a [MegaSwitch] to enable or disable Camera Uploads
 *
 * @param isChecked true if the [MegaSwitch] is checked
 * @param onCheckedChange Lambda to execute when the [MegaSwitch] checked state has changed
 */
@Composable
internal fun CameraUploadsTile(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(CAMERA_UPLOADS_TILE),
            title = stringResource(R.string.section_photo_sync),
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(CAMERA_UPLOADS_TILE_SWITCH),
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                )
            }
        )
        if (isChecked) {
            MegaDivider(
                modifier = Modifier.testTag(CAMERA_UPLOADS_TILE_DIVIDER),
                dividerType = DividerType.FullSize,
            )
        }
    }
}

/**
 * A [Composable] Preview for [CameraUploadsTile]
 *
 * @param isChecked [PreviewParameter] that controls the [MegaSwitch] checked state
 */
@CombinedThemePreviews
@Composable
private fun CameraUploadsTilePreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsTile(
            isChecked = isChecked,
            onCheckedChange = {},
        )
    }
}

/**
 * Test Tags for the Camera Uploads Tile
 */
internal const val CAMERA_UPLOADS_TILE = "camera_uploads_tile:generic_two_line_list_item"
internal const val CAMERA_UPLOADS_TILE_SWITCH = "camera_uploads_tile:mega_switch"
internal const val CAMERA_UPLOADS_TILE_DIVIDER = "camera_uploads_tile:mega_divider"