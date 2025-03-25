package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Plain button with an outlined border
 * @param textId the text resource for this button
 * @param onClick lambda to receive clicks on this button
 * @param rounded True if the button should be rounded
 * @param modifier [Modifier]
 * @param enabled True if the button should be enabled
 * @param iconId Icon id if any
 */
@Composable
fun OutlinedMegaButton(
    textId: Int,
    onClick: () -> Unit,
    rounded: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconId: Int? = null,
) = OutlinedMegaButton(
    text = stringResource(id = textId),
    onClick = onClick,
    rounded = rounded,
    modifier = modifier,
    enabled = enabled,
    iconId = iconId,
)

/**
 * Plain button with an outlined border
 * @param text The text for this button
 * @param onClick lambda to receive clicks on this button
 * @param rounded True if the button should be rounded
 * @param modifier [Modifier]
 * @param enabled True if the button should be enabled
 * @param iconId Icon id if any
 */
@Composable
fun OutlinedMegaButton(
    text: String,
    onClick: () -> Unit,
    rounded: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconId: Int? = null,
) {
    val color = if (enabled) {
        MegaOriginalTheme.colors.button.outline
    } else {
        MegaOriginalTheme.colors.border.disabled
    }
    TextButton(
        modifier = modifier.widthIn(min = 100.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MegaOriginalTheme.colors.background.pageBackground,
            contentColor = MegaOriginalTheme.colors.text.accent,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = MegaOriginalTheme.colors.text.disabled,
        ),
        onClick = onClick,
        shape = if (rounded) RoundedCornerShape(25.dp) else MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, color)
    ) {
        iconId?.let {
            Icon(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
                imageVector = ImageVector.vectorResource(id = it),
                contentDescription = text,
                tint = color,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.button,
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewOutlinedRoundedMegaButton(
    @PreviewParameter(OutlinedMegaButtonPreviewProvider::class) state: OutlinedMegaButtonState,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        with(state) {
            var count by remember { mutableIntStateOf(0) }
            OutlinedMegaButton(
                text = stringResource(id = androidx.appcompat.R.string.search_menu_title) + if (count > 0) " $count" else "",
                onClick = { count++ },
                rounded = rounded,
                enabled = enabled,
                iconId = if (icon) R.drawable.checked else null
            )
        }
    }
}

private data class OutlinedMegaButtonState(
    val icon: Boolean,
    val enabled: Boolean,
    val rounded: Boolean,
)

private class OutlinedMegaButtonPreviewProvider :
    PreviewParameterProvider<OutlinedMegaButtonState> {
    override val values: Sequence<OutlinedMegaButtonState>
        get() = listOf(
            OutlinedMegaButtonState(icon = false, enabled = false, rounded = false),
            OutlinedMegaButtonState(icon = false, enabled = true, rounded = false),
            OutlinedMegaButtonState(icon = false, enabled = true, rounded = true),
            OutlinedMegaButtonState(icon = false, enabled = false, rounded = true),
            OutlinedMegaButtonState(icon = true, enabled = false, rounded = false),
            OutlinedMegaButtonState(icon = true, enabled = true, rounded = false),
            OutlinedMegaButtonState(icon = true, enabled = true, rounded = true),
            OutlinedMegaButtonState(icon = true, enabled = false, rounded = true),
        ).asSequence()
}