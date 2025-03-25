package mega.privacy.android.app.presentation.contact.authenticitycredendials

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Authenticity Credentials Activity.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class AuthenticityCredentialsActivity : ComponentActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<AuthenticityCredentialsViewModel>()

    companion object {

        /**
         * Intent to start [AuthenticityCredentialsActivity].
         * @param context           current context.
         * @param email             email of the contact.
         * @param isIncomingShares  true if the contact is an incoming share.
         */
        fun getIntent(context: Context, email: String, isIncomingShares: Boolean) =
            Intent(context, AuthenticityCredentialsActivity::class.java).apply {
                putExtra(Constants.EMAIL, email)
                putExtra(Constants.IS_NODE_INCOMING, isIncomingShares)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        intent.extras?.getString(Constants.EMAIL)?.let {
            viewModel.requestData(it)
        } ?: finish()

        viewModel.setShowContactVerificationBanner(
            intent.extras?.getBoolean(Constants.IS_NODE_INCOMING) ?: false
        )

        setContent { AuthenticityCredentialsView() }
    }

    @Composable
    private fun AuthenticityCredentialsView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val uiState by viewModel.state.collectAsState()

        OriginalTheme(isDark = themeMode.isDarkMode()) {
            mega.privacy.android.app.presentation.contact.authenticitycredendials.view.AuthenticityCredentialsView(
                state = uiState,
                onButtonClicked = viewModel::actionClicked,
                onBackPressed = { finish() },
                onErrorShown = viewModel::errorShown
            )
        }
    }
}