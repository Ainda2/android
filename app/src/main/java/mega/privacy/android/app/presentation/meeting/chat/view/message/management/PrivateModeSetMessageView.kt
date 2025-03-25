package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.chat.messages.management.PrivateModeSetMessage
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Private mode set message view
 *
 */
@Composable
fun PrivateModeSetMessageView(
    message: PrivateModeSetMessage,
    modifier: Modifier = Modifier,
    viewModel: ManagementMessageViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    var ownerActionFullName by remember {
        mutableStateOf(context.getString(R.string.unknown_name_label))
    }

    LaunchedEffect(Unit) {
        if (message.isMine) {
            viewModel.getMyFullName()
        } else {
            viewModel.getParticipantFullName(message.userHandle)
        }?.let { ownerActionFullName = it }
    }

    PrivateModeSetMessageView(
        ownerActionFullName = ownerActionFullName,
        modifier = modifier
    )
}

/**
 * Private mode set message view
 *
 */
@Composable
internal fun PrivateModeSetMessageView(
    ownerActionFullName: String,
    modifier: Modifier = Modifier,
) {
    val context: Context = LocalContext.current
    val privateModeMessage = remember(ownerActionFullName) {
        buildString {
            append(
                context.getString(
                    R.string.message_set_chat_private,
                    ownerActionFullName
                )
            )
            appendLine()
            appendLine()
            append("[A]")
            append(context.getString(R.string.subtitle_chat_message_enabled_ERK))
            append("[/A]")
        }
    }
    ChatManagementMessageView(
        text = privateModeMessage,
        modifier = modifier,
        styles = mapOf(
            SpanIndicator('A') to MegaSpanStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextColor.Primary
            ),
            SpanIndicator('B') to MegaSpanStyle(
                SpanStyle(),
                color = TextColor.Secondary
            ),
        )
    )
}

@CombinedThemePreviews
@Composable
private fun PrivateModeSetMessagePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        PrivateModeSetMessageView(
            ownerActionFullName = "Name"
        )
    }
}