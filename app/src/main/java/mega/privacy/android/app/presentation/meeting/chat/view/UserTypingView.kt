package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * User typing view
 *
 * @param usersTyping the list of users typing
 */
@Composable
fun UserTypingView(
    usersTyping: List<String?>,
    modifier: Modifier = Modifier,
) {
    if (usersTyping.isNotEmpty()) {
        MegaSpannedText(
            modifier = modifier,
            value = usersTyping.getTypingText(),
            baseStyle = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            styles = mapOf(
                SpanIndicator('A') to MegaSpanStyle(
                    spanStyle = SpanStyle(
                        fontWeight = FontWeight.Normal
                    ),
                    color = TextColor.Secondary
                )
            ),
            color = TextColor.Primary,
        )
    }
}

@Composable
private fun List<String?>.getTypingText(): String = when {
    size <= 2 -> pluralStringResource(
        id = R.plurals.user_typing,
        count = size,
        joinToString(", ")
    )

    else -> stringResource(id = R.string.more_users_typing, take(2).joinToString(", "))
}

@CombinedThemePreviews
@Composable
private fun UserTypingViewPreview(
    @PreviewParameter(UserTypingParameter::class) usersTyping: List<String>,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        UserTypingView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            usersTyping = usersTyping,
        )
    }
}

private class UserTypingParameter : CollectionPreviewParameterProvider<List<String>>(
    listOf(
        listOf("User 1", "User 2", "User 3"),
        listOf("User 1", "User 2"),
        listOf("User 1"),
    )
)