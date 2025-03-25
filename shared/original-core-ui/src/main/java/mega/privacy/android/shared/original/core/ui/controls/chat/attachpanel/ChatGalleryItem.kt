package mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Chat gallery item
 *
 * @param modifier Modifier
 * @param content Content
 */
@Composable
fun ChatGalleryItem(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = MegaOriginalTheme.colors.background.surface2,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
    ) {
        // we hide the border when content loaded, so it place here instead of container modifier
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MegaOriginalTheme.colors.border.strong,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        content()
    }
}

@CombinedThemePreviews
@Composable
private fun ChatGalleryItemPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatGalleryItem(modifier = Modifier.size(88.dp)) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.ic_emoji_smile_medium_regular),
                contentDescription = ""
            )
        }
    }
}