package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.dark_grey
import mega.privacy.android.shared.original.core.ui.theme.grey_200
import mega.privacy.android.shared.original.core.ui.theme.grey_500
import mega.privacy.android.shared.original.core.ui.theme.accent_050
import mega.privacy.android.shared.original.core.ui.theme.accent_900
import mega.privacy.android.shared.original.core.ui.theme.white

/**
 * Enable Camera Uploads View
 */
@Composable
fun EnableCU(
    timelineViewState: TimelineViewState = TimelineViewState(),
    onUploadVideosChanged: (Boolean) -> Unit = {},
    onUseCellularConnectionChanged: (Boolean) -> Unit = {},
    enableCUClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                state = scrollState,
            ),
    ) {
        val configuration = LocalConfiguration.current
        Image(
            painter = painterResource(id = R.drawable.ic_enable_cu),
            contentDescription = "Enable camera uploads",
            contentScale = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                ContentScale.FillWidth
            } else {
                ContentScale.Fit
            },
            modifier = Modifier
                .padding(top = 24.dp, start = 65.dp, end = 65.dp)
                .fillMaxWidth(),
        )

        Text(
            text = stringResource(id = R.string.settings_camera_upload_on),
            fontSize = 20.sp,
            color = colorResource(id = R.color.grey_087_white_087),
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        Text(
            text = stringResource(id = R.string.enable_cu_subtitle),
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            color = colorResource(id = R.color.grey_054_white_060),
            modifier = Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
        )

        Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Card(
                modifier = Modifier
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8),
                border = BorderStroke(1.dp, colorResource(id = R.color.grey_012_white_020)),
                elevation = 0.dp
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.camera_uploads_upload_videos),
                            color = colorResource(id = R.color.grey_087_white_087),
                            fontSize = 18.sp,
                        )
                        MegaSwitch(
                            checked = timelineViewState.cuUploadsVideos,
                            onCheckedChange = onUploadVideosChanged,
                        )
                    }

                    Divider(
                        color = colorResource(id = R.color.grey_012_white_020),
                        thickness = 1.dp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.camera_uploads_cellular_connection),
                            color = colorResource(id = R.color.grey_087_white_087),
                            fontSize = 18.sp,
                        )
                        MegaSwitch(
                            checked = timelineViewState.cuUseCellularConnection,
                            onCheckedChange = onUseCellularConnectionChanged,
                        )
                    }
                }
            }
        }

        Button(
            onClick = enableCUClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colorResource(id = R.color.accent_900)
            )
        ) {
            Text(
                text = stringResource(id = R.string.general_enable),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (MaterialTheme.colors.isLight) {
                    Color.White
                } else {
                    colorResource(id = R.color.grey_087_white_087)
                }
            )
        }
    }
}

@Composable
fun EnableCameraUploadsScreen(
    onEnable: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val orientation = LocalConfiguration.current.orientation

    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                EnableCameraUploadsButton(onEnable = onEnable)
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    Image(
                        painter = painterResource(id = R.drawable.enable_camera_uploads_image),
                        contentDescription = "Enable camera uploads",
                        modifier = Modifier.size(180.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.settings_camera_upload_on),
                        color = black.takeIf { isLight } ?: white,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W500,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.enable_cu_subtitle),
                        color = grey_500.takeIf { isLight } ?: grey_200,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body1,
                    )

                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Spacer(modifier = Modifier.height(8.dp))

                        EnableCameraUploadsButton(onEnable = onEnable)
                    }
                },
            )
        }
    )
}

@Composable
fun EnableCameraUploadsButton(
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Button(
        onClick = onEnable,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = accent_900.takeIf { isLight } ?: accent_050,
        ),
        content = {
            Text(
                text = stringResource(id = R.string.general_enable),
                color = white.takeIf { isLight } ?: dark_grey,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.button,
            )
        },
    )
}

/**
 * Enable Camera Uploads View Preview
 */
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewTimelineView"
)
@Preview
@Composable
fun PreviewEnableCU() {
    OriginalTheme(isSystemInDarkTheme()) {
        Scaffold { paddingValues ->
            Column(modifier = Modifier
                .padding(paddingValues)) { }
            EnableCU()
        }
    }
}
