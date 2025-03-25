package mega.privacy.android.shared.original.core.ui.controls.controlssliders

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Radio Button following MEGA design system
 */
@Composable
fun MegaRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) = RadioButton(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    colors = MegaOriginalTheme.colors.radioColors,
)

@CombinedThemePreviews
@Composable
private fun RadioPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    var selected by remember { mutableStateOf(initialValue) } //this is to make an interactive preview
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaRadioButton(
            selected = selected,
            onClick = { selected = !selected },
        )
    }
}