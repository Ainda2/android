package mega.privacy.android.app.presentation.login.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.fromId
import mega.privacy.android.app.presentation.extensions.login.error
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.twofactorauthentication.view.TWO_FACTOR_AUTHENTICATION_TEST_TAG
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val stateWithLoginRequired = LoginState(isLoginRequired = true)

    private fun setupRule(state: LoginState = LoginState()) {
        composeRule.setContent {
            LoginView(
                state = state,
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClicked = {},
                onForgotPassword = {},
                onCreateAccount = {},
                onSnackbarMessageConsumed = {},
                on2FAPinChanged = { _, _ -> },
                on2FAChanged = {},
                onLostAuthenticatorDevice = {},
                onBackPressed = {},
                onFirstTime2FAConsumed = {},
                onReportIssue = {},
            )
        }
    }

    @Test
    fun `test that login title is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.login_to_mega)).assertExists()
    }

    @Test
    fun `test that email field is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(mega.privacy.android.shared.resources.R.string.email_text)).assertExists()
    }

    @Test
    fun `test that email error is shown if login is required and the error exists`() {
        val emailError = LoginError.EmptyEmail
        setupRule(LoginState(isLoginRequired = true, emailError = emailError))
        composeRule.onNodeWithText(fromId(emailError.error)).assertExists()
    }

    @Test
    fun `test that password field is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(mega.privacy.android.shared.resources.R.string.password_text)).assertExists()
    }

    @Test
    fun `test that password error is shown if login is required and the error exists`() {
        val passwordError = LoginError.EmptyPassword
        setupRule(LoginState(isLoginRequired = true, passwordError = passwordError))
        composeRule.onNodeWithText(fromId(passwordError.error)).assertExists()
    }

    @Test
    fun `test that log in button is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(mega.privacy.android.shared.resources.R.string.login_text)).assertExists()
    }

    @Test
    fun `test forgot your password button is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.forgot_pass)).assertExists()
    }

    @Test
    fun `test that new to mega text is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.new_to_mega)).assertExists()
    }

    @Test
    fun `test that create account button is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.create_account)).assertExists()
    }

    @Test
    fun `test that MEGA logo is shown if login is in progress`() {
        setupRule(LoginState(isLoginInProgress = true))
        composeRule.onNodeWithTag(MEGA_LOGO_TEST_TAG).assertExists()
    }

    @Test
    fun `test that checking signup link text is shown if login is in progress and it is processing a link`() {
        setupRule(LoginState(isLoginInProgress = true, isCheckingSignupLink = true))
        composeRule.onNodeWithText(fromId(R.string.login_querying_signup_link)).assertExists()
    }

    @Test
    fun `test that connecting to server text is shown if login is in progress`() {
        setupRule(LoginState(isLoginInProgress = true))
        composeRule.onNodeWithText(fromId(R.string.login_connecting_to_server)).assertExists()
    }

    @Test
    fun `test that updating file list text is shown if fetch nodes update exists`() {
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate()))
        composeRule.onNodeWithText(fromId(R.string.download_updating_filelist)).assertExists()
    }

    @Test
    fun `test that preparing file list text is shown if the progress of fetch nodes is greater than 0`() {
        setupRule(
            LoginState(
                fetchNodesUpdate = FetchNodesUpdate(Progress(0.5F)),
                isFirstTime = true
            )
        )
        composeRule.onNodeWithText(fromId(R.string.login_preparing_filelist)).assertExists()
    }

    @Test
    fun `test that fetch nodes progress bar is shown if the progress of fetch nodes is greater than 0`() {
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate(Progress(0.5F))))
        composeRule.onNodeWithTag(FETCH_NODES_PROGRESS_TEST_TAG).assertExists()
    }

    @Test
    fun `test that request status progress bar is shown if the request status progress is not null`() {
        setupRule(LoginState(requestStatusProgress = Progress(0.5f), isLoginInProgress = true))
        composeRule.onNodeWithTag(REQUEST_STATUS_PROGRESS_TEST_TAG).assertExists()
    }

    @Test
    fun `test that temporary error text is shown if fetch nodes update has it`() {
        val temporaryError = TemporaryWaitingError.ConnectivityIssues
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate(temporaryError = temporaryError)))
        composeRule.onNodeWithText(fromId(temporaryError.messageId)).assertExists()
    }

    @Test
    fun `test that toolbar is shown if 2FA screen is required`() {
        setupRule(LoginState(is2FARequired = true))
        composeRule.onNodeWithText(fromId(R.string.login_verification)).assertExists()
    }

    @Test
    fun `test that informing text is shown if 2FA is required`() {
        setupRule(LoginState(is2FARequired = true))
        composeRule.onNodeWithText(fromId(R.string.explain_confirm_2fa)).assertExists()
    }

    @Test
    fun `test that 2FA field id shown if 2FA screen is required`() {
        setupRule(LoginState(is2FARequired = true))
        composeRule.onNodeWithTag(TWO_FACTOR_AUTHENTICATION_TEST_TAG).assertExists()
    }

    @Test
    fun `test that lost authenticator device button is shown if 2FA is required`() {
        setupRule(LoginState(is2FARequired = true))
        composeRule.onNodeWithText(fromId(R.string.lost_your_authenticator_device)).assertExists()
    }

    @Test
    fun `test that error is not visible if 2FA is required and 2FA state does not exists`() {
        setupRule(
            LoginState(is2FARequired = true, multiFactorAuthState = null)
        )
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertDoesNotExist()
    }

    @Test
    fun `test that progress bar is shown if 2FA status is Checking`() {
        setupRule(LoginState(multiFactorAuthState = MultiFactorAuthState.Checking))
        composeRule.onNodeWithTag(TWO_FA_PROGRESS_TEST_TAG).assertExists()
    }

    @Test
    fun `test that error is not visible if 2FA status is Checking`() {
        setupRule(LoginState(multiFactorAuthState = MultiFactorAuthState.Checking))
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertDoesNotExist()
    }

    @Test
    fun `test that progress is not shown if 2FA status is Failed`() {
        setupRule(LoginState(multiFactorAuthState = MultiFactorAuthState.Failed))
        composeRule.onNodeWithTag(TWO_FA_PROGRESS_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that error is visible if 2FA status is Failed`() {
        setupRule(LoginState(multiFactorAuthState = MultiFactorAuthState.Failed))
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertExists()
    }

    @Test
    fun `test that error is not visible if 2FA status is Fixed`() {
        setupRule(LoginState(multiFactorAuthState = MultiFactorAuthState.Fixed))
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertDoesNotExist()
    }
}