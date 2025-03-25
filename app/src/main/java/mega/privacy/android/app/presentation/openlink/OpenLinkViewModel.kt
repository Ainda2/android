package mega.privacy.android.app.presentation.openlink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.link.DecodeLinkUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * open link view model
 */
@HiltViewModel
class OpenLinkViewModel @Inject constructor(
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
    private val getAccountCredentials: GetAccountCredentialsUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val decodeLinkUseCase: DecodeLinkUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenLinkUiState())

    /**
     * UI State OpenLinkActivity
     * Flow of [OpenLinkUiState]
     */
    val uiState = _uiState.asStateFlow()

    /**
     * decodes the url and updates the state
     * @param url url to decode
     */
    fun decodeUrl(url: String) {
        viewModelScope.launch {
            runCatching {
                val accountCredentials = getAccountCredentials()
                val needToRefresh = getRootNodeUseCase() == null
                val decodedUrl = decodeLinkUseCase(url)
                _uiState.update {
                    it.copy(
                        decodedUrl = decodedUrl,
                        isLoggedIn = accountCredentials != null,
                        needsRefreshSession = needToRefresh,
                        urlRedirectionEvent = true
                    )
                }
            }.onFailure { error ->
                Timber.d("Error on decode url $error")
            }
        }
    }

    /**
     * logout confirmed
     * once logout is confirmed methods clears user related app data
     */
    private fun logoutConfirmed() {
        Timber.d("END logout sdk request - wait chat logout")
        MegaApplication.urlConfirmationLink?.let {
            Timber.d("Confirmation link - show confirmation screen")
            applicationScope.launch {
                runCatching {
                    clearEphemeralCredentialsUseCase()
                    localLogoutAppUseCase()
                }.onSuccess {
                    MegaApplication.urlConfirmationLink = null
                    _uiState.update { it.copy(logoutCompletedEvent = true) }
                }.onFailure {
                    Timber.d("Logout confirmation failed : ${it.message}")
                }
            }
        }
    }

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onSuccess {
            logoutConfirmed()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }

    /**
     * Get information about a new signup link
     */
    fun getAccountInvitationEmail(link: String) = viewModelScope.launch {
        runCatching {
            querySignupLinkUseCase(link)
        }.onSuccess { email ->
            Timber.d("Valid signup link")
            _uiState.update {
                it.copy(accountInvitationEmail = email)
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Reset url redirection event when consumed
     */
    fun onUrlRedirectionEventConsumed() {
        _uiState.update {
            it.copy(urlRedirectionEvent = false)
        }
    }

    /**
     * Reset logout completed event when consumed
     */
    fun onLogoutCompletedEventConsumed() {
        _uiState.update {
            it.copy(logoutCompletedEvent = false)
        }
    }
}