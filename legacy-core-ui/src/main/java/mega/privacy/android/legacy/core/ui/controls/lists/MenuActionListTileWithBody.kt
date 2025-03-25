package mega.privacy.android.legacy.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary

/**
 * Test Tags for the Menu Action List Tile With Body
 */
internal const val TILE_WITH_BODY_MAIN_CONTAINER =
    "menu_action_list_tile_with_body:row_content_container"
internal const val TILE_WITH_BODY_ICON = "menu_action_list_tile_with_body:icon_functionality_icon"
internal const val TILE_WITH_BODY_TEXT_TITLE = "menu_action_list_tile_with_body:text_tile_title"
internal const val TILE_WITH_BODY_TEXT_BODY = "menu_action_list_tile_with_body:text_tile_body"
internal const val TILE_WITH_BODY_DIVIDER = "menu_action_list_tile_with_body:divider_tile_divider"

/**
 * A [Composable] Bottom Dialog Tile that displays three UI Elements: the [icon], [title] and [body]
 *
 * @param title The functionality Title
 * @param body The functionality Body
 * @param icon The functionality Icon
 * @param modifier The [Modifier] object
 * @param dividerType type for the divider below menu action. Hidden if NULL
 * @param iconTint The [Color] tint applied for [icon], which defaults to [textColorSecondary]
 * @param onActionClicked Lambda that executes a specific action when the Tile is clicked. No action
 * is configured by default
 */
@Composable
fun MenuActionListTileWithBody(
    title: String,
    body: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    dividerType: DividerType? = DividerType.BigStartPadding,
    iconTint: Color = MaterialTheme.colors.textColorSecondary,
    onActionClicked: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = modifier
                .testTag(TILE_WITH_BODY_MAIN_CONTAINER)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .conditional(onActionClicked != null) {
                    clickable { onActionClicked?.invoke() }
                }
                .padding(all = 16.dp),
        ) {
            Icon(
                modifier = modifier
                    .testTag(TILE_WITH_BODY_ICON)
                    .padding(end = 32.dp)
                    .size(24.dp),
                painter = painterResource(icon),
                contentDescription = "Tile Icon",
                tint = iconTint,
            )
            Column(
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = modifier
                        .testTag(TILE_WITH_BODY_TEXT_TITLE)
                        .padding(end = 16.dp),
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.textColorPrimary,
                )
                Text(
                    modifier = modifier
                        .testTag(TILE_WITH_BODY_TEXT_BODY)
                        .padding(top = 1.dp, end = 16.dp),
                    text = body,
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.textColorSecondary
                )
            }
        }

        dividerType?.let {
            MegaDivider(
                dividerType = it,
                modifier = Modifier.testTag(TILE_WITH_BODY_DIVIDER)
            )
        }
    }
}

/**
 * A Preview Composable that displays content with only the required parameters provided
 */
@CombinedThemePreviews
@Composable
private fun PreviewTile() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTileWithBody(
            title = "Tile Title",
            body = "Tile Body",
            icon = IconPackR.drawable.ic_folder_medium_solid,
        )
    }
}

/**
 * A Preview Composable that displays content without the [Divider]
 */
@CombinedThemePreviews
@Composable
private fun PreviewTileWithoutDivider() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTileWithBody(
            title = "Tile Title",
            body = "Tile Body",
            icon = IconPackR.drawable.ic_folder_medium_solid,
            dividerType = null,
        )
    }
}

/**
 * A Preview Composable that displays content with a very long Body
 *
 * The Container height automatically adjusts to display the entire Body
 */
@CombinedThemePreviews
@Composable
private fun PreviewTileWithVeryLongBody() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTileWithBody(
            title = "Tile Title",
            body = "This is a really long body text used to check if the container height dynamically expands or not",
            icon = IconPackR.drawable.ic_folder_medium_solid,
        )
    }
}