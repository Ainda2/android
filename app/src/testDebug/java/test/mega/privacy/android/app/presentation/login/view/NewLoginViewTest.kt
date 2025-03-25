package test.mega.privacy.android.app.presentation.login.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.fromId
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.LoginTestTags
import mega.privacy.android.app.presentation.login.view.NewLoginView
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewLoginViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val stateWithLoginRequired = LoginState(isLoginRequired = true)

    private fun setupRule(
        state: LoginState = LoginState(),
        onLoginClicked: () -> Unit = { },
        onForgotPassword: () -> Unit = { },
    ) {
        composeRule.setContent {
            NewLoginView(
                state = state,
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClicked = onLoginClicked,
                onForgotPassword = onForgotPassword,
                onCreateAccount = {},
                onSnackbarMessageConsumed = {},
                on2FAChanged = {},
                onLostAuthenticatorDevice = {},
                onBackPressed = {},
                onReportIssue = {},
            )
        }
    }

    @Test
    fun `test that all text fields are displayed correctly`() {
        setupRule(stateWithLoginRequired)

        composeRule.onNodeWithText(fromId(sharedR.string.login_page_title))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_text))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.password_text))
            .assertExists()
        composeRule.onNodeWithText(
            fromId(sharedR.string.login_page_sign_up_action_footer_text)
                .replace("[A]", "")
                .replace("[/A]", "")
        ).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.LOGIN_BUTTON)
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.SIGN_UP_BUTTON)
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.FORGOT_PASSWORD_BUTTON)
            .assertExists()
    }

    @Test
    fun `test that invalid email address shows when email is invalid`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                emailError = LoginError.NotValidEmail
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_email_error_message))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_password_error_message))
            .assertDoesNotExist()
    }

    @Test
    fun `test that invalid email address shows when email is empty`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                emailError = LoginError.EmptyEmail
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_email_error_message))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_password_error_message))
            .assertDoesNotExist()
    }

    @Test
    fun `test that invalid password shows when password is empty`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                passwordError = LoginError.EmptyPassword
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_password_error_message))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_email_error_message))
            .assertDoesNotExist()
    }

    @Test
    fun `test that wrong email or password shows when email or password is incorrect`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                loginException = LoginWrongEmailOrPassword()
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_wrong_credential_error_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.WRONG_CREDENTIAL_BANNER)
            .assertExists()
    }

    @Test
    fun `test that too many attempts shows when too many attempts`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                loginException = LoginTooManyAttempts()
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_too_many_attempts_error_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.TOO_MANY_ATTEMPTS_BANNER)
            .assertExists()
    }
}