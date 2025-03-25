package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.STORAGE_DESCRIPTION_ROW
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.android.core.ui.theme.values.TextColor

/**
 * Composable UI for feature description to reuse on Onboarding dialog for both Variants (A and B)
 */
@Composable
internal fun FeatureRow(
    drawableID: Painter,
    title: String,
    description: String,
    testTag: String,
    isLoading: Boolean = false,
    isBulletPointListUsed: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = drawableID,
            contentDescription = "",
            tint = MaterialTheme.colors.textColorPrimary,
            modifier = Modifier
                .testTag("$testTag:icon")
                .placeholder(
                    color = MaterialTheme.colors.grey_020_grey_900,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                    visible = isLoading,
                )
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            MegaText(
                text = title,
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle2medium,
                modifier = Modifier
                    .testTag("$testTag:title")
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = isLoading,
                    )
            )
            MegaText(
                text = description,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2.copy(textIndent = TextIndent(restLine = if (isBulletPointListUsed) 8.sp else 0.sp)),
                modifier = Modifier
                    .testTag("$testTag:description")
                    .padding(
                        start = if (isBulletPointListUsed) 4.dp else 0.dp,
                        top = 2.dp
                    )
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = isLoading,
                    )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@CombinedThemePreviews
@Composable
fun FeatureRowPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FeatureRow(
            drawableID = painterResource(id = R.drawable.ic_security_onboarding_dialog),
            title = "Additional security when sharing",
            description = "Set passwords and expiry dates for file and folder links.",
            testTag = STORAGE_DESCRIPTION_ROW,
        )
    }
}