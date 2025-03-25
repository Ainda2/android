package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Compose view for location message.
 * The message contains a title, latitude, longitude and a map.
 *
 * @param isMe whether the message is sent by me
 * @param title title of the message
 * @param geolocation the string with latitude and longitude
 * @param map the map image
 */
@Composable
fun LocationMessageView(
    isMe: Boolean,
    title: AnnotatedString,
    geolocation: String,
    map: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(width = 256.dp, height = 200.dp)
            .background(
                color = MegaOriginalTheme.colors.background.pageBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isMe) MegaOriginalTheme.colors.border.strongSelected else MegaOriginalTheme.colors.background.surface2,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        map?.let {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(
                        color = if (isMe) MegaOriginalTheme.colors.border.strong
                        else MegaOriginalTheme.colors.background.surface2
                    ),
                bitmap = it,
                contentScale = ContentScale.Crop,
                contentDescription = "map",
            )
        }
        Text(
            modifier = Modifier.padding(bottom = 6.dp, top = 16.dp, start = 12.dp),
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Normal,
            color = MegaOriginalTheme.colors.text.primary
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = geolocation,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Normal,
            color = MegaOriginalTheme.colors.text.primary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LocationMessagePreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        LocationMessageView(
            isMe = isMe,
            title = buildAnnotatedString { append("Pinned location") },
            geolocation = "41.1472° N, 8.6179° W",
            map = ImageBitmap.imageResource(IconPackR.drawable.ic_folder_incoming_medium_solid),
        )
    }
}
