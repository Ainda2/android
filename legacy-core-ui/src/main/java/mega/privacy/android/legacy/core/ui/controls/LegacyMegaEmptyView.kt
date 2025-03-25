package mega.privacy.android.legacy.core.ui.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_300_grey_600
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_300_white_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_900_grey_100
import mega.privacy.android.shared.original.core.ui.theme.extensions.light_grey_light_black
import mega.android.core.ui.theme.values.TextColor

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageVector using ImageVector.vectorResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imageVector
 * @param text
 */
@Composable
fun LegacyMegaEmptyView(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
) {
    LegacyMegaEmptyView(modifier, text) {
        Image(
            imageVector = imageVector,
            contentDescription = "Empty Icon",
        )
    }
}

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageBitmap using ImageBitmap.imageResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imageBitmap
 * @param text
 */
@Composable
fun LegacyMegaEmptyView(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap,
    text: String,
) {
    LegacyMegaEmptyView(modifier, text) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Empty Icon",
        )
    }
}

/**
 * Reusable EmptyView with Image & Text
 * Pass imagePainter using painterResource(id = R.drawable.ic_xyz)
 * @param modifier
 * @param imagePainter
 * @param text
 */
@Composable
fun LegacyMegaEmptyViewWithImage(
    modifier: Modifier = Modifier,
    imagePainter: Painter,
    text: String,
) {
    LegacyMegaEmptyView(modifier, text) {
        Image(
            painter = imagePainter,
            contentDescription = "Empty Image"
        )
    }
}

/**
 * Reusable EmptyView with Icon & Text
 * Pass imageVector using ImageVector.vectorResource(id = R.drawable.ic_xyz)
 * @param modifier [Modifier]
 * @param text with string
 * @param imagePainter
 */
@Composable
fun LegacyMegaEmptyView(
    text: String,
    imagePainter: Painter,
    modifier: Modifier = Modifier,
) {
    LegacyMegaEmptyView(modifier, text) {
        Image(
            painter = imagePainter,
            contentDescription = "Empty Icon",
        )
    }
}

@Composable
private fun LegacyMegaEmptyView(modifier: Modifier, text: String, Icon: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon()
        val style = MaterialTheme.typography.subtitle2
        val emptySpanStyle = MegaSpanStyle(SpanStyle())
        mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText(
            value = text, baseStyle = style,
            color = TextColor.Primary,
            styles = mapOf(
                SpanIndicator('A') to emptySpanStyle,
                SpanIndicator('B') to emptySpanStyle
            )
        )
    }
}

/**
 * Empty view for search
 * @param text empty text to be displayed
 * @param imagePainter [Painter] resource id in form of painter
 * @param modifier [Modifier]
 */
@Composable
fun LegacyMegaEmptyViewForSearch(
    text: String,
    imagePainter: Painter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = imagePainter,
            contentDescription = "Empty Icon",
            modifier = Modifier.padding(bottom = 30.dp),
            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.light_grey_light_black)
        )
        MegaSpannedText(
            value = text, baseStyle = MaterialTheme.typography.subtitle1.copy(
                fontSize = 16.sp,
            ),
            styles = mapOf(
                SpanIndicator('A') to SpanStyle(color = MaterialTheme.colors.grey_900_grey_100),
                SpanIndicator('B') to SpanStyle(color = MaterialTheme.colors.grey_300_grey_600)
            ),
            color = MaterialTheme.colors.grey_300_white_alpha_087
        )
    }
}

