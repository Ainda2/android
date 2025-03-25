package mega.privacy.android.legacy.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.AlertDialog
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary

/**
 * Alert dialog with a text input field and a confirmation button with optional cancel button
 * Confirm and cancel button will be placed horizontally if there are enough room, vertically if not.
 * @param title the title of the dialog, if no there will be no title
 * @param confirmButtonText text for the confirm button
 * @param cancelButtonText text for the cancel button, if null there will be no cancel button
 * @param onConfirm to be triggered when confirm button is pressed
 * @param onDismiss to be triggered when dialog is hidden, wither with cancel button, confirm button, back or outside press.
 * @param dismissOnClickOutside if true, the dialog will be dismiss when the user taps outside of the dialog, default to true.
 * @param dismissOnBackPress if true, the dialog will be dismiss when the user does back action, default to true.
 * @param keyboardActions Specifies Keyboard Actions to be performed
 * @param keyboardType Specifies the type of keys available for the Keyboard (e.g. Text, Number)
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun InputDialog(
    title: String,
    confirmButtonText: String,
    cancelButtonText: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "",
    hint: String = "",
    text: String = "",
    error: String? = null,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    onInputChange: (String) -> Unit = {},
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        )
    }

    CompositionLocalProvider(LocalAbsoluteElevation provides 24.dp) {
        val focusRequester = remember { FocusRequester() }
        AlertDialog(
            modifier = modifier.semantics { testTagsAsResourceId = true },
            title = {
                Text(
                    modifier = Modifier
                        .testTag(INPUT_DIALOG_TITLE_TAG)
                        .fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                )
            },
            text = {
                Column {
                    if (message.isNotEmpty()) {
                        Text(
                            modifier = Modifier
                                .testTag(INPUT_DIALOG_TITLE_TAG)
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            text = message,
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.textColorSecondary
                        )
                    }
                    GenericTextField(
                        modifier = Modifier
                            .testTag(INPUT_DIALOG_TEXT_TAG)
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                focusRequester.requestFocus()
                            },
                        placeholder = hint,
                        onTextChange = {
                            textFieldValue = it
                            onInputChange(it.text)
                        },
                        textFieldValue = textFieldValue,
                        keyboardActions = keyboardActions,
                        keyboardType = keyboardType,
                        errorText = error,
                    )
                }
            },
            onDismissRequest = onDismiss,
            buttons = {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    cancelButtonText?.let {
                        TextMegaButton(
                            modifier = Modifier.testTag(INPUT_DIALOG_CANCEL_TAG),
                            text = cancelButtonText,
                            onClick = onDismiss,
                        )
                    }
                    TextMegaButton(
                        modifier = Modifier.testTag(INPUT_DIALOG_CONFIRM_TAG),
                        text = confirmButtonText,
                        onClick = { onConfirm(textFieldValue.text) },
                    )
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside,
            ),
        )
    }
}

internal const val INPUT_DIALOG_TITLE_TAG = "input_dialog:text_title"
internal const val INPUT_DIALOG_TEXT_TAG = "input_dialog:generic_text_field_input"
internal const val INPUT_DIALOG_CANCEL_TAG = "input_dialog:button_cancel"
internal const val INPUT_DIALOG_CONFIRM_TAG = "input_dialog:button_confirm"

@CombinedThemeRtlPreviews
@Composable
private fun InputDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Box(
            modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
            content = {
                InputDialog(
                    title = "Dialog title",
                    hint = "hint text",
                    confirmButtonText = "Accept",
                    cancelButtonText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                )
            },
        )
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun InputDialogWithMessagePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Box(
            modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
            content = {
                InputDialog(
                    title = "Dialog title",
                    hint = "hint text",
                    message = "Message",
                    confirmButtonText = "Accept",
                    cancelButtonText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                )
            },
        )
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun InputDialogPreviewWithError() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Box(
            modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
            content = {
                InputDialog(
                    title = "Dialog title",
                    text = "Input text",
                    hint = "nick name",
                    confirmButtonText = "Accept",
                    cancelButtonText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                    error = "Error"
                )
            },
        )
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun InputDialogPreviewWithLargeButtonText() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Box(
            modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
            content = {
                InputDialog(
                    title = "Dialog title",
                    text = "Input text",
                    hint = "nick name",
                    confirmButtonText = "Accept button with very large text",
                    cancelButtonText = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                    error = "Error"
                )
            },
        )
    }
}
