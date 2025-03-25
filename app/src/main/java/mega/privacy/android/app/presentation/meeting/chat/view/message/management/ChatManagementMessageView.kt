package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Chat management message view
 *
 * @param text the text to show
 * @param styles the list of the tag and the custom style
 * @param modifier Modifier
 */
@Composable
fun ChatManagementMessageView(
    text: String,
    styles: Map<SpanIndicator, MegaSpanStyle>,
    modifier: Modifier = Modifier,
) {
    MegaSpannedText(
        modifier = modifier,
        value = text,
        baseStyle = MaterialTheme.typography.subtitle2,
        styles = styles,
        color = TextColor.Secondary
    )
}

@CombinedThemePreviews
@Composable
private fun ChatManagementMessageViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatManagementMessageView(
            text = "[A]Hello[/A] World",
            styles = mapOf(
                SpanIndicator('A') to MegaSpanStyle(
                    spanStyle = SpanStyle(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextColor.Primary
                )
            )
        )
    }
}