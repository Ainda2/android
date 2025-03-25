package mega.privacy.android.legacy.core.ui.controls.chips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.legacy.core.ui.customTextSelectionColors
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.TextFieldProvider
import mega.privacy.android.shared.original.core.ui.preview.TextFieldState
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary

/**
 * Text button chip
 *
 * @param text          The text in TextButton
 * @param onClick       Lambda to receive clicks on this button
 * @param modifier
 * @param isChecked     True, if it's checked. False, if not
 */
@Composable
fun TextButtonChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isChecked: Boolean = true,
) = Box {

    val customTextSelectionColors = customTextSelectionColors()
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        TextButton(
            modifier = modifier
                .testTag(TEST_TAG_TEXT_BUTTON_CHIP)
                .width(32.dp)
                .height(32.dp),
            onClick = onClick,
            shape = RoundedCornerShape(size = 8.dp),
            border = BorderStroke(
                1.dp,
                if (isChecked) MaterialTheme.colors.secondary else MaterialTheme.colors.textColorSecondary
            ),
            enabled = true,
            colors = if (isChecked) colorsChecked() else colorsUnChecked(),
            elevation = ButtonDefaults.elevation(0.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.button.copy(
                    textAlign = TextAlign.Center, color =
                    if (isChecked)
                        MaterialTheme.colors.onSecondary
                    else
                        MaterialTheme.colors.onPrimary
                )
            )
        }
    }
}

@Composable
private fun colorsChecked() = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.secondary,
    contentColor = MaterialTheme.colors.secondary,
    disabledContentColor = MaterialTheme.colors.secondary,
    disabledBackgroundColor = MaterialTheme.colors.secondary,
)


@Composable
private fun colorsUnChecked() = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.onSecondary,
    contentColor = MaterialTheme.colors.onSecondary,
    disabledContentColor = MaterialTheme.colors.onSecondary,
    disabledBackgroundColor = MaterialTheme.colors.onSecondary,
)


@CombinedThemePreviews
@Composable
private fun PreviewTextButtonChip(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        TextButtonChip(
            onClick = { },
            text = "M",
            modifier = Modifier,
            isChecked = true,
        )
    }
}

internal const val TEST_TAG_TEXT_BUTTON_CHIP = "testTagTextButtonChip"
