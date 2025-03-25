package mega.privacy.android.legacy.core.ui.controls.controlssliders


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_087

/**
 * A switch with a label
 */
@Composable
fun LabelledSwitch(
    label: String,
    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckChanged
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle1,
            color = if (!MaterialTheme.colors.isLight) white_alpha_087 else grey_alpha_087
        )
        MegaSwitch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun LabelledSwitchPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    var checked by remember { mutableStateOf(initialValue) }
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        LabelledSwitch(
            label = if (checked) "On" else "Off",
            checked = checked,
            onCheckChanged = { checked = !checked })
    }
}

