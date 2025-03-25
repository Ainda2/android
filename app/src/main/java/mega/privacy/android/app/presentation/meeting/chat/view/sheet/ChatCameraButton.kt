@file:OptIn(ExperimentalPermissionsApi::class)

package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.ChatGalleryItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber

internal const val TEST_TAG_CHAT_CAMERA_BUTTON = "chat_camera_button"
internal const val TEST_TAG_CHAT_CAMERA_BUTTON_ICON = "chat_camera_button:icon"

/**
 * Chat camera button
 *
 * @param modifier Modifier
 */
@Composable
fun ChatCameraButton(
    hideSheet: () -> Unit,
    modifier: Modifier = Modifier,
    onCameraPermissionDenied: () -> Unit = {},
    onTakePicture: () -> Unit = {},
    isVisible: Boolean = false,
) {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }
    var takePictureClicked by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraController.bindToLifecycle(lifecycleOwner)
            if (takePictureClicked) {
                onTakePicture()
                takePictureClicked = false
            }
        } else if (takePictureClicked) {
            hideSheet()
            onCameraPermissionDenied()
            takePictureClicked = false
        }
    }
    var isFrameReady by remember { mutableStateOf(false) }
    DisposableEffect(isVisible) {
        if (isVisible) {
            if (cameraPermissionState.status.isGranted.not()) {
                cameraPermissionsLauncher.launch(Manifest.permission.CAMERA)
            } else {
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        } else {
            cameraController.unbind()
        }
        onDispose { }
    }
    Box(
        modifier = modifier.testTag(TEST_TAG_CHAT_CAMERA_BUTTON)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = LocalContentColor.current,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp)),
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_START
                    setBackgroundColor(Color.TRANSPARENT)
                }.also { previewView ->
                    previewView.controller = cameraController
                    previewView.previewStreamState.observe(lifecycleOwner) { state ->
                        Timber.d("Preview stream state: $state")
                        isFrameReady = state == PreviewView.StreamState.STREAMING
                    }
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
        )

        if (!isFrameReady) {
            // when frame is not ready show place holder
            ChatGalleryItem(modifier = Modifier.fillMaxSize()) {}
        }

        Image(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TEST_TAG_CHAT_CAMERA_BUTTON_ICON)
                .clickable {
                    if (cameraPermissionState.status.isGranted.not()) {
                        takePictureClicked = true
                        cameraPermissionsLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        onTakePicture()
                    }
                },
            painter = painterResource(id = R.drawable.ic_take_photo),
            contentDescription = "Take photo",
            contentScale = ContentScale.Inside
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatCameraButtonPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatCameraButton(
            modifier = Modifier.size(88.dp),
            hideSheet = {},
        )
    }
}