package mega.privacy.android.shared.original.core.ui.controls.chat.messages.file

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * File message content for file without preview
 *
 * @param isMe whether message is sent from me
 * @param fileTypeResId resource id of file type icon
 * @param modifier
 * @param fileName name of file
 * @param fileSize size string of file. It can be "Uploading" when message is loading.
 */

@Composable
internal fun FileNoPreviewMessageView(
    isMe: Boolean,
    fileTypeResId: Int?,
    modifier: Modifier = Modifier,
    fileName: String = "",
    fileSize: String = "",
) {
    Row(
        modifier = modifier
            .background(
                color = if (isMe) MegaOriginalTheme.colors.button.primary else MegaOriginalTheme.colors.background.surface2,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(fileTypeResId, isMe)
        Column {
            FileNameText(fileName, isMe)
            FileSizeText(fileSize, isMe)
        }
    }
}


@Composable
private fun FileSizeText(fileSize: String, isMe: Boolean) {
    MegaText(
        text = fileSize,
        style = MaterialTheme.typography.caption,
        textColor = if (isMe) TextColor.Inverse else TextColor.Primary,
        modifier = Modifier.testTag(FILE_MESSAGE_VIEW_FILE_SIZE_TEST_TAG),
    )
}

@Composable
private fun FileNameText(fileName: String, isMe: Boolean) {
    MegaText(
        text = fileName,
        style = MaterialTheme.typography.subtitle1,
        textColor = if (isMe) TextColor.Inverse else TextColor.Primary,
        overflow = LongTextBehaviour.MiddleEllipsis,
        modifier = Modifier
            .widthIn(max = 184.dp)
            .testTag(FILE_MESSAGE_VIEW_FILE_NAME_TEST_TAG),
    )
}

@Composable
private fun FileIcon(
    fileTypeResId: Int?,
    isMe: Boolean,
    modifier: Modifier = Modifier,
) = Box(
    modifier = Modifier.size(48.dp),
    contentAlignment = Alignment.Center,
) {
    fileTypeResId?.let {
        Icon(
            tint = if (isMe) MegaOriginalTheme.colors.icon.inverse else MegaOriginalTheme.colors.icon.primary,
            imageVector = ImageVector.vectorResource(fileTypeResId),
            contentDescription = null,
            modifier = modifier.testTag(FILE_MESSAGE_VIEW_FILE_TYPE_ICON_TEST_TAG),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileNoPreviewMessageViewPreview(
    @PreviewParameter(NoPreviewPreviewProvider::class) params: NoPreviewPreviewParams,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FileContainerMessageView(
            modifier = Modifier.padding(12.dp),
            content = {
                FileNoPreviewMessageView(
                    isMe = params.isMe,
                    fileTypeResId = params.fileTypeResId,
                    fileName = params.fileName,
                    fileSize = params.fileSize,
                )
            },
            loadProgress = params.loadProgress,
        )
    }
}

private data class NoPreviewPreviewParams(
    val isMe: Boolean,
    val loadProgress: Float?,
    val fileName: String,
    val fileSize: String,
    val fileTypeResId: Int,
)

private class NoPreviewPreviewProvider : PreviewParameterProvider<NoPreviewPreviewParams> {
    override val values = listOf(true, false).flatMap { isMe ->
        listOf(
            NoPreviewPreviewParams(
                isMe = isMe,
                loadProgress = null,
                fileName = "This is a very very very long file name.pdf",
                fileSize = "30 MB",
                fileTypeResId = R.drawable.ic_check_circle,
            ),
            NoPreviewPreviewParams(
                isMe = isMe,
                loadProgress = null,
                fileName = "hello.pdf",
                fileSize = "30 MB",
                fileTypeResId = R.drawable.ic_check_circle,
            ),
            NoPreviewPreviewParams(
                isMe = isMe,
                fileTypeResId = iconPackR.drawable.ic_alert_circle_regular_medium_outline,
                loadProgress = 0.6f,
                fileName = "long long long file name.pdf",
                fileSize = "30 MB",
            )
        )
    }.asSequence()
}

internal const val FILE_MESSAGE_VIEW_FILE_NAME_TEST_TAG =
    "chat_file_message_view:file_name_text"
internal const val FILE_MESSAGE_VIEW_FILE_SIZE_TEST_TAG =
    "chat_file_message_view:file_size_text"
internal const val FILE_MESSAGE_VIEW_FILE_TYPE_ICON_TEST_TAG =
    "chat_file_message_view:file_type_icon"
