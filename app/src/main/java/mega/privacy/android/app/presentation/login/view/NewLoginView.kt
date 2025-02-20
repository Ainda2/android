package mega.privacy.android.app.presentation.login.view

import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.apiserver.view.ChangeApiServerDialog
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.login.newError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.LoginHelpButtonPressedEvent

/**
 * Login fragment view.
 *
 * @param state                     [LoginState]
 * @param onEmailChanged            Action when the typed email changes.
 * @param onPasswordChanged         Action when the typed password changes.
 * @param onLoginClicked            Action when Login is pressed.
 * @param onForgotPassword          Action when Forgot password is pressed.
 * @param onCreateAccount           Action when Create account is pressed.
 * @param onSnackbarMessageConsumed Action when the snackbar message was consumed.
 * @param on2FAChanged              Action when a 2FA code was pasted.
 * @param onLostAuthenticatorDevice Action when Lost authenticator device is pressed.
 * @param onBackPressed             Action when back is pressed.
 * @param modifier                  [Modifier]
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewLoginView(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onSnackbarMessageConsumed: () -> Unit,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onBackPressed: () -> Unit,
    onReportIssue: () -> Unit,
    modifier: Modifier = Modifier,
    onLoginExceptionConsumed: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showChangeApiServerDialog by rememberSaveable { mutableStateOf(false) }
    val showLoginInProgress =
        state.isLoginInProgress || state.fetchNodesUpdate != null || state.isRequestStatusInProgress
    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        snackbarHost = {
            MegaSnackbar(snackbarHostState)
        },
        topBar = {
            if (state.is2FARequired || state.multiFactorAuthState != null) {
                MegaTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    navigationType = AppBarNavigationType.Back(onBackPressed),
                    title = stringResource(sharedR.string.settings_2fa),
                )
            } else if (!showLoginInProgress) {
                MegaTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = "",
                    navigationType = AppBarNavigationType.Back(onBackPressed),
                    trailingIcons = {
                        IconButton(
                            onClick = {
                                onReportIssue()
                                Analytics.tracker.trackEvent(LoginHelpButtonPressedEvent)
                            },
                        ) {
                            MegaIcon(
                                painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_help_circle_medium_regular_outline),
                                tint = IconColor.Primary,
                                contentDescription = "Report issue Icon"
                            )
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        with(state) {
            when {
                showLoginInProgress -> OriginalTheme(isDark = state.themeMode.isDarkMode()) {
                    LoginInProgress(
                        state = this,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                isLoginRequired -> RequireLogin(
                    state = this,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClicked = onLoginClicked,
                    onForgotPassword = onForgotPassword,
                    onCreateAccount = onCreateAccount,
                    onChangeApiServer = { showChangeApiServerDialog = true },
                    modifier = Modifier.padding(paddingValues),
                    onLoginExceptionConsumed = onLoginExceptionConsumed,
                )

                is2FARequired || multiFactorAuthState != null -> NewTwoFactorAuthentication(
                    state = this,
                    on2FAChanged = on2FAChanged,
                    onLostAuthenticatorDevice = onLostAuthenticatorDevice,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }

        BackHandler { onBackPressed() }

        val context = LocalContext.current

        EventEffect(
            event = state.snackbarMessage,
            onConsumed = onSnackbarMessageConsumed
        ) {
            snackbarHostState.showSnackbar(
                message = context.resources.getString(it),
                duration = SnackbarDuration.Short
            )
        }

        if (showChangeApiServerDialog) {
            OriginalTheme(isDark = state.themeMode.isDarkMode()) {
                ChangeApiServerDialog(onDismissRequest = { showChangeApiServerDialog = false })
            }
        }
    }
}

@Composable
private fun RequireLogin(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onChangeApiServer: () -> Unit,
    modifier: Modifier = Modifier,
    onLoginExceptionConsumed: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val orientation = LocalConfiguration.current.orientation
    var wrongCredentials by remember { mutableStateOf(false) }
    var tooManyAttempts by remember { mutableStateOf(false) }

    LaunchedEffect(state.loginException) {
        if (state.loginException is LoginWrongEmailOrPassword) {
            wrongCredentials = true
        } else if (state.loginException is LoginTooManyAttempts) {
            tooManyAttempts = true
        }
        onLoginExceptionConsumed()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(true)
                })
            }, contentAlignment = Alignment.TopCenter
    ) {
        val contentModifier = if (LocalDeviceType.current == DeviceType.Tablet) {
            Modifier
                .fillMaxHeight()
                .width(tabletScreenWidth(orientation))
                .padding(top = 80.dp)
        } else {
            Modifier
                .fillMaxSize()
        }

        Column(
            modifier = contentModifier,
        ) {
            Image(
                modifier = Modifier
                    .size(120.dp)
                    .padding(top = 32.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.ic_mega_round),
                contentDescription = "Login Icon"
            )

            MegaText(
                modifier = Modifier
                    .wrapContentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val downTime = System.currentTimeMillis()
                            tryAwaitRelease()
                            val upTime = System.currentTimeMillis()
                            if (upTime - downTime >= LONG_PRESS_DELAY) {
                                onChangeApiServer()
                            }
                        })
                    }
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(sharedR.string.login_page_title),
                style = AppTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                textColor = TextColor.Primary,
            )

            TextInputField(
                modifier = Modifier
                    .testTag(LoginTestTags.EMAIL_INPUT)
                    .padding(
                        top = 40.dp, start = 16.dp, end = 16.dp
                    ),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
                label = stringResource(id = sharedR.string.email_text),
                text = state.accountSession?.email.orEmpty(),
                onValueChanged = {
                    wrongCredentials = false
                    tooManyAttempts = false
                    onEmailChanged(it.trim())
                },
                errorText = when {
                    state.emailError != null -> stringResource(state.emailError.newError)
                    wrongCredentials -> ""
                    tooManyAttempts -> ""
                    else -> null
                }
            )

            PasswordTextInputField(
                modifier = Modifier
                    .testTag(LoginTestTags.PASSWORD_INPUT)
                    .padding(
                        top = 16.dp, start = 16.dp, end = 16.dp
                    ),
                label = stringResource(id = sharedR.string.password_text),
                text = state.password.orEmpty(),
                onValueChanged = {
                    wrongCredentials = false
                    tooManyAttempts = false
                    onPasswordChanged(it)
                },
                errorText = when {
                    state.passwordError != null -> stringResource(state.passwordError.newError)
                    wrongCredentials -> ""
                    tooManyAttempts -> ""
                    else -> null
                }
            )
            if (wrongCredentials) {
                InlineErrorBanner(
                    modifier = Modifier
                        .testTag(LoginTestTags.WRONG_CREDENTIAL_BANNER)
                        .fillMaxWidth()
                        .padding(
                            top = 24.dp, start = 16.dp, end = 16.dp
                        ),
                    body = stringResource(sharedR.string.login_wrong_credential_error_message),
                    showCancelButton = false
                )
            }

            if (tooManyAttempts) {
                InlineErrorBanner(
                    modifier = Modifier
                        .testTag(LoginTestTags.TOO_MANY_ATTEMPTS_BANNER)
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    body = stringResource(sharedR.string.login_too_many_attempts_error_message),
                    showCancelButton = false
                )
            }


            PrimaryFilledButton(
                modifier = Modifier
                    .testTag(LoginTestTags.LOGIN_BUTTON)
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = sharedR.string.login_text),
                isLoading = false,
                onClick = {
                    focusManager.clearFocus(true)
                    onLoginClicked()
                },
            )

            TextOnlyButton(
                modifier = Modifier
                    .testTag(LoginTestTags.FORGOT_PASSWORD_BUTTON)
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(sharedR.string.login_page_forgot_password_text),
                onClick = onForgotPassword,
            )

            LinkSpannedText(
                modifier = Modifier
                    .testTag(LoginTestTags.SIGN_UP_BUTTON)
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = 40.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                    .imePadding(),
                value = stringResource(sharedR.string.login_page_sign_up_action_footer_text),
                spanStyles = hashMapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        MegaSpanStyle.LinkColorStyle(
                            SpanStyle(),
                            LinkColor.Primary
                        ),
                        stringResource(sharedR.string.login_page_sign_up_action_footer_text)
                            .substringAfter("[A]")
                            .substringBefore("[/A]")
                    )
                ),
                baseStyle = AppTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                onAnnotationClick = {
                    onCreateAccount()
                }
            )
        }
    }
}

internal fun tabletScreenWidth(orientation: Int): Dp {
    return when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> authScreenWidthTabletLandscape.dp
        Configuration.ORIENTATION_PORTRAIT -> authScreenWidthTabletPortrait.dp
        else -> authScreenWidthTabletPortrait.dp
    }
}

internal const val authScreenWidthTabletPortrait = 348
internal const val authScreenWidthTabletLandscape = 357

internal object LoginTestTags {
    private const val LOGIN_SCREEN = "login_screen"
    const val EMAIL_INPUT = "${LOGIN_SCREEN}:email_input"
    const val PASSWORD_INPUT = "${LOGIN_SCREEN}:password_input"
    const val LOGIN_BUTTON = "${LOGIN_SCREEN}:login_button"
    const val FORGOT_PASSWORD_BUTTON = "${LOGIN_SCREEN}:forgot_password_button"
    const val SIGN_UP_BUTTON = "${LOGIN_SCREEN}:sign_up_button"
    const val ACCOUNT_BLOCKED_DIALOG = "${LOGIN_SCREEN}:account_blocked_dialog"
    const val ACCOUNT_LOCKED_DIALOG = "${LOGIN_SCREEN}:account_locked_dialog"
    const val WRONG_CREDENTIAL_BANNER = "${LOGIN_SCREEN}:wrong_credential_banner"
    const val TOO_MANY_ATTEMPTS_BANNER = "${LOGIN_SCREEN}:too_many_attempts_banner"
}

@CombinedThemePreviews
@Composable
private fun EmptyLoginViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var state by remember { mutableStateOf(LoginState(isLoginRequired = true)) }

        RequireLogin(
            state = state,
            onEmailChanged = {
                state = state.copy(accountSession = AccountSession(email = it))
            },
            onPasswordChanged = { state = state.copy(password = it) },
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onChangeApiServer = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LoginViewPreview(
    @PreviewParameter(LoginStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewLoginView(
            state = state,
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onSnackbarMessageConsumed = {},
            on2FAChanged = {},
            onLostAuthenticatorDevice = {},
            onBackPressed = {},
            onReportIssue = {},
        )
    }
}

@CombinedThemePhoneLandscapePreviews
@Composable
private fun LandscapeLoginViewPreview(
    @PreviewParameter(LoginStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewLoginView(
            state = state,
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onSnackbarMessageConsumed = {},
            on2FAChanged = {},
            onLostAuthenticatorDevice = {},
            onBackPressed = {},
            onReportIssue = {},
        )
    }
}

private const val LONG_PRESS_DELAY = 5000L
