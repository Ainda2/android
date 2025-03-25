package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.TextFieldProvider
import mega.privacy.android.shared.original.core.ui.preview.TextFieldState
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.autofill

/**
 * Text field for password.
 *
 * @param onTextChange    Action required for notifying about text changes.
 * @param imeAction       [ImeAction]
 * @param keyboardActions [KeyboardActions]
 * @param modifier        [Modifier]
 * @param text            Typed text.
 * @param errorText       Error to show if any.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PasswordTextField(
    onTextChange: (String) -> Unit,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    text: String = "",
    errorText: String? = null,
    hint: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MegaOriginalTheme.colors.text.primary,
        backgroundColor = MegaOriginalTheme.colors.background.pageBackground,
        cursorColor = MegaOriginalTheme.colors.border.strongSelected,
        errorCursorColor = MegaOriginalTheme.colors.text.error,
        errorIndicatorColor = MegaOriginalTheme.colors.support.error,
        focusedIndicatorColor = MegaOriginalTheme.colors.border.strongSelected,
        unfocusedIndicatorColor = MegaOriginalTheme.colors.border.disabled,
        focusedLabelColor = MegaOriginalTheme.colors.text.accent,
        unfocusedLabelColor = MegaOriginalTheme.colors.text.placeholder,
        errorLabelColor = MegaOriginalTheme.colors.text.error,
    )
    var isFocused by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val customTextSelectionColors = customTextSelectionColors()

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .background(Color.Transparent)
                .indicatorLine(true, isError, interactionSource, colors)
                .fillMaxWidth()
                .onFocusChanged {
                    isFocused = it.isFocused
                    onFocusChanged(it.isFocused)
                }
                .autofill(
                    autofillTypes = listOf(AutofillType.Password), onAutoFilled = onTextChange
                ),
            textStyle = MaterialTheme.typography.subtitle1.copy(color = MegaOriginalTheme.colors.text.primary),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
                autoCorrect = false,
            ),
            keyboardActions = keyboardActions,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            interactionSource = interactionSource,
            singleLine = true,
        ) {
            val hintString = hint ?: stringResource(id = mega.privacy.android.shared.resources.R.string.password_text)
            TextFieldDefaults.TextFieldDecorationBox(
                value = text,
                innerTextField = it,
                enabled = true,
                singleLine = true,
                interactionSource = interactionSource,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                isError = isError,
                label = {
                    Text(
                        text = hintString,
                        modifier = Modifier.padding(bottom = if (isFocused) 6.dp else 0.dp),
                        style = when {
                            isError && isFocused -> MaterialTheme.typography.caption.copy(color = MegaOriginalTheme.colors.text.error)
                            isError && text.isEmpty() -> MaterialTheme.typography.body1.copy(color = MegaOriginalTheme.colors.text.error)
                            isFocused -> MaterialTheme.typography.caption.copy(color = MegaOriginalTheme.colors.text.accent)
                            text.isNotEmpty() -> MaterialTheme.typography.caption.copy(color = MegaOriginalTheme.colors.text.placeholder)
                            else -> MaterialTheme.typography.body1.copy(color = MegaOriginalTheme.colors.text.placeholder)
                        }
                    )
                },
                trailingIcon = {
                    if (isFocused) {
                        Icon(
                            modifier = Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { showPassword = !showPassword },
                            ),
                            painter = painterResource(id = R.drawable.ic_visibility_outline),
                            tint = if (showPassword) MegaOriginalTheme.colors.icon.accent else MegaOriginalTheme.colors.icon.disabled,
                            contentDescription = "see"
                        )
                    }
                },
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp, bottom = 7.dp
                )
            )
        }
    }

    errorText?.apply { ErrorTextTextField(errorText = this) }
}

@CombinedThemePreviews
@Composable
private fun PreviewErrorPasswordTextField(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        var text by remember { mutableStateOf(state.text) }

        PasswordTextField(
            onTextChange = { text = it },
            imeAction = ImeAction.Default,
            keyboardActions = KeyboardActions(),
            text = state.text,
            errorText = state.error,
        )
    }

}