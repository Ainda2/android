package mega.privacy.android.app.presentation.notification.view.components

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun NotificationDate(dateText: String, modifier: Modifier) {
    MegaText(
        text = dateText,
        textColor = TextColor.Secondary,
        overflow = LongTextBehaviour.Clip(),
        style = MaterialTheme.typography.caption,
        modifier = modifier
    )
}