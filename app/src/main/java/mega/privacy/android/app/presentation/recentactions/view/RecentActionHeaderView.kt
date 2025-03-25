package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Composable for the recent actions list sticky header
 * @param text Text to display
 * @param backgroundColor Background color
 */
@Composable
fun RecentActionHeaderView(
    text: String,
    backgroundColor: Color = MaterialTheme.colors.surface,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .height(36.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        MegaText(
            text = text,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.testTag(HEADER_TEST_TAG)
        )
    }
}

internal const val HEADER_TEST_TAG = "recent_action_header_view:text"


@CombinedThemePreviews
@Composable
private fun RecentActionHeaderPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RecentActionHeaderView("Monday, April 3, 2024")
    }
}