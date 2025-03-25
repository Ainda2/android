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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Title change message view
 *
 * @param message The message
 * @param modifier Modifier
 * @param viewModel The view model
 */
@Composable
fun TitleChangeMessageView(
    message: TitleChangeMessage,
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

    TitleChangeMessageView(
        newTitle = message.content,
        ownerActionFullName = ownerActionFullName,
        modifier = modifier
    )
}

/**
 * Title change message view
 *
 * @param newTitle
 * @param ownerActionFullName
 * @param modifier
 */
@Composable
internal fun TitleChangeMessageView(
    newTitle: String,
    ownerActionFullName: String,
    modifier: Modifier = Modifier,
) = ChatManagementMessageView(
    text = stringResource(id = R.string.change_title_messages, ownerActionFullName, newTitle),
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
        SpanIndicator('C') to MegaSpanStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
            ),
            color = TextColor.Primary
        ),
    )
)

@CombinedThemePreviews
@Composable
private fun TitleChangeMessageViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        TitleChangeMessageView(
            newTitle = "New title",
            ownerActionFullName = "My name"
        )
    }
}