package mega.privacy.android.app.presentation.recentactions.view


import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Composable for when Recent Actions is hidden in settings
 *
 * @param modifier [Modifier]
 * @param onShowActivityActionClick Callback when the show activity action button is clicked
 */
@Composable
fun RecentActionsHiddenView(
    modifier: Modifier = Modifier,
    onShowActivityActionClick: () -> Unit = {},
) {

    ConstraintLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val (image, text, button) = createRefs()

        Image(
            painter = painterResource(iconPackR.drawable.ic_clock_glass),
            contentDescription = "Recent Actions Icon",
            modifier = Modifier
                .constrainAs(image) {
                    bottom.linkTo(text.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(RECENTS_HIDDEN_IMAGE_TEST_TAG),
        )
        MegaSpannedText(
            value = stringResource(id = R.string.recents_activity_hidden),
            baseStyle = MaterialTheme.typography.body2,
            styles = mapOf(
                SpanIndicator('B') to MegaSpanStyle(
                    spanStyle = SpanStyle(),
                ),
            ),
            color = TextColor.Primary,
            modifier = Modifier
                .constrainAs(text) {
                    bottom.linkTo(button.top, 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(RECENTS_HIDDEN_TEXT_TEST_TAG),
        )
        OutlinedMegaButton(
            textId = R.string.show_activity_action,
            rounded = false,
            onClick = { onShowActivityActionClick() },
            modifier = Modifier
                .testTag(RECENTS_HIDDEN_BUTTON_TEST_TAG)
                .constrainAs(button) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        )
    }
}

internal const val RECENTS_HIDDEN_IMAGE_TEST_TAG = "recent_actions_hidden_view:image"
internal const val RECENTS_HIDDEN_TEXT_TEST_TAG = "recent_actions_hidden_view:text"
internal const val RECENTS_HIDDEN_BUTTON_TEST_TAG = "recent_actions_hidden_view:button"

@CombinedThemePreviews
@Composable
private fun RecentActionsHiddenViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RecentActionsHiddenView()
    }
}