package mega.privacy.android.app.mediaplayer.trackinfo

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_ADDED_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_ALBUM_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_ARTIST_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_DURATION_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_LAST_MODIFIED_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_LOCATION_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_SIZE_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_TITLE_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.OFFLINE_OPTION_TEST_TAG
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.TimeUtils.formatLongDateTime
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import java.io.File

/**
 * Audio track info view
 *
 * @param uiState TrackInfoState
 * @param metadata Metadata
 * @param onLocationClicked the function for location clicked
 * @param onCheckedChange the function for switch checked changed
 */
@Composable
fun AudioTrackInfoView(
    uiState: TrackInfoState,
    metadata: Metadata,
    onLocationClicked: (location: LocationInfo?) -> Unit,
    onCheckedChange: (isChecked: Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.grey_020_grey_800))
            .verticalScroll(rememberScrollState())
    ) {
        AudioInfoView(
            thumbnail = uiState.thumbnail,
            titleString = metadata.title ?: metadata.nodeName,
            artistString = metadata.artist,
            albumString = metadata.album,
            duration = uiState.durationString
        )

        AudioNodeInfoView(
            onCheckedChange = onCheckedChange,
            isEnabled = uiState.availableOffline,
            sizeValue = uiState.size,
            locationValue = uiState.location,
            onLocationClicked = onLocationClicked,
            addedValue = formatLongDateTime(uiState.added),
            lastModifiedValue = formatLongDateTime(uiState.lastModified)
        )
    }
}

/**
 * The view shows the info regarding audio file
 *
 * @param thumbnail thumbnail file of audio
 * @param titleString audio title string
 * @param artistString audio artist string
 * @param albumString audio album string
 * @param duration audio duration
 */
@Composable
fun AudioInfoView(
    thumbnail: File?,
    titleString: String?,
    artistString: String?,
    albumString: String?,
    duration: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(id = R.color.white_dark_grey)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                if (thumbnail != null && thumbnail.exists()) {
                    thumbnail
                } else {
                    iconPackR.drawable.ic_default_audio_cover
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 84.dp)
                .size(128.dp)
                .clip(
                    RoundedCornerShape(8.dp)
                )
        )

        Text(
            text = titleString ?: "",
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.grey_087_white),
            modifier = Modifier
                .padding(top = 24.dp, start = 20.dp, end = 20.dp)
                .testTag(AUDIO_TITLE_TEST_TAG)
        )

        artistString?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = colorResource(id = R.color.grey_054_white_054),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(AUDIO_ARTIST_TEST_TAG)
            )
        }

        albumString?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = colorResource(id = R.color.grey_054_white_054),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(AUDIO_ALBUM_TEST_TAG)
            )
        }

        Text(
            text = duration ?: "00:00",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = colorResource(id = R.color.accent_900),
            modifier = Modifier
                .padding(top = 8.dp, bottom = 24.dp)
                .testTag(AUDIO_DURATION_TEST_TAG)
        )
    }
}

/**
 * The view shows the info regarding mega node
 *
 * @param isEnabled true is offline available, otherwise is false
 * @param onCheckedChange the function for switch checked changed
 * @param sizeValue audio size value
 * @param locationValue audio location string
 * @param onLocationClicked the function for location clicked
 * @param addedValue audio added value
 * @param lastModifiedValue audio last modified value
 */
@Composable
fun AudioNodeInfoView(
    isEnabled: Boolean?,
    onCheckedChange: (isChecked: Boolean) -> Unit,
    sizeValue: String?,
    locationValue: LocationInfo?,
    onLocationClicked: (location: LocationInfo?) -> Unit,
    addedValue: String?,
    lastModifiedValue: String?,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .padding(start = 72.dp)
                .fillMaxWidth()
                .height(55.dp)
                .toggleable(
                    value = isEnabled ?: false,
                    role = Role.Switch,
                    onValueChange = { onCheckedChange(it) }
                ).testTag(OFFLINE_OPTION_TEST_TAG),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.file_properties_available_offline),
                color = colorResource(R.color.grey_087_white)
            )

            MegaSwitch(
                checked = isEnabled ?: false,
                onCheckedChange = null,
                modifier = Modifier
                    .padding(end = 16.dp)
            )
        }

        Divider(
            color = colorResource(id = R.color.grey_012_white_012),
            thickness = 1.dp,
            modifier = Modifier.padding(start = 72.dp)
        )

        Text(
            text = stringResource(id = R.string.file_properties_info_size_file),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.grey_087_white),
            modifier = Modifier.padding(top = 9.dp, start = 72.dp)
        )

        Text(
            text = sizeValue ?: "",
            fontSize = 14.sp,
            color = colorResource(R.color.grey_070_white_070),
            modifier = Modifier
                .padding(top = 4.dp, start = 72.dp)
                .testTag(AUDIO_SIZE_TEST_TAG)
        )

        Text(
            text = stringResource(id = R.string.file_properties_info_location),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.grey_087_white),
            modifier = Modifier.padding(top = 14.dp, start = 72.dp)
        )

        Text(
            text = (locationValue?.location ?: ""),
            fontSize = 14.sp,
            color = colorResource(R.color.accent_900),
            modifier = Modifier
                .padding(top = 4.dp, start = 72.dp)
                .clickable {
                    onLocationClicked(locationValue)
                }
                .testTag(AUDIO_LOCATION_TEST_TAG)
        )

        Text(
            text = stringResource(id = R.string.file_properties_info_added),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.grey_087_white),
            modifier = Modifier.padding(top = 14.dp, start = 72.dp)
        )

        Text(
            text = addedValue ?: "",
            fontSize = 14.sp,
            color = colorResource(R.color.grey_070_white_070),
            modifier = Modifier
                .padding(top = 4.dp, start = 72.dp)
                .testTag(AUDIO_ADDED_TEST_TAG)
        )

        Text(
            text = stringResource(id = R.string.file_properties_info_last_modified),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.grey_087_white),
            modifier = Modifier.padding(top = 14.dp, start = 72.dp)
        )

        Text(
            text = lastModifiedValue ?: "",
            fontSize = 14.sp,
            color = colorResource(R.color.grey_070_white_070),
            modifier = Modifier
                .padding(top = 4.dp, start = 72.dp)
                .testTag(AUDIO_LAST_MODIFIED_TEST_TAG)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AudioTrackInfoViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AudioTrackInfoView(
            uiState = TrackInfoState(
                durationString = "00:35"
            ),
            metadata = Metadata(
                title = "Great song",
                album = "Great album",
                artist = "The best artist",
                nodeName = "Node name"
            ),
            {},
            {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AudioInfoViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AudioInfoView(
            thumbnail = null,
            titleString = "Vengeance Rhythm",
            artistString = "Two Fingers",
            albumString = "Vengeance Rhythm Single",
            duration = "03:24"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AudioNodeInfoViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AudioNodeInfoView(
            onCheckedChange = {},
            isEnabled = false,
            sizeValue = "8.2MB",
            locationValue = LocationInfo("MUSIC"),
            onLocationClicked = {},
            addedValue = "19 May 2017 12:48",
            lastModifiedValue = "20 May 2017 12:48"
        )
    }
}


internal object Constants {

    /**
     * Test tag for audio title
     */
    const val AUDIO_TITLE_TEST_TAG = "audio_title_test_tag"

    /**
     * Test tag for audio artist
     */
    const val AUDIO_ARTIST_TEST_TAG = "audio_artist_test_tag"

    /**
     * Test tag for audio album
     */
    const val AUDIO_ALBUM_TEST_TAG = "audio_album_test_tag"

    /**
     * Test tag for audio duration
     */
    const val AUDIO_DURATION_TEST_TAG = "audio_duration_test_tag"

    /**
     * Test tag for available offline option
     */
    const val OFFLINE_OPTION_TEST_TAG = "offline_option_test_tag"

    /**
     * Test tag for audio size
     */
    const val AUDIO_SIZE_TEST_TAG = "audio_size_test_tag"

    /**
     * Test tag for audio location
     */
    const val AUDIO_LOCATION_TEST_TAG = "audio_location_test_tag"

    /**
     * Test tag for audio added
     */
    const val AUDIO_ADDED_TEST_TAG = "audio_added_test_tag"

    /**
     * Test tag for audio last modified
     */
    const val AUDIO_LAST_MODIFIED_TEST_TAG = "audio_last_modified_test_tag"
}
