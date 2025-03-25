package mega.privacy.android.app.presentation.fingerprintauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.fingerprintauth.model.SecurityUpgradeState
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInAppUseCase
import mega.privacy.android.domain.usecase.account.UpgradeSecurityUseCase
import javax.inject.Inject

/**
 * ViewModel associated with [SecurityUpgradeDialogFragment] responsible to call account security related functions
 */
@HiltViewModel
class SecurityUpgradeViewModel @Inject constructor(
    private val upgradeSecurityUseCase: UpgradeSecurityUseCase,
    private val setSecurityUpgradeInAppUseCase: SetSecurityUpgradeInAppUseCase,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(SecurityUpgradeState())

    /** public UI state */
    val state: StateFlow<SecurityUpgradeState> = _state

    /**
     * Function to upgrade account security
     */
    fun upgradeAccountSecurity() {
        viewModelScope.launch {
            val result = runCatching {
                upgradeSecurityUseCase()
            }.onSuccess {
                setSecurityUpgradeInAppUseCase(false)
            }.onFailure {
                setSecurityUpgradeInAppUseCase(true)
            }
            setShouldFinishScreen(result.isSuccess)
        }
    }

    /**
     * Set UI state should finishScreen
     *
     * @param shouldFinish true if the screen should finish
     */
    private fun setShouldFinishScreen(shouldFinish: Boolean) {
        _state.update {
            it.copy(shouldFinishScreen = shouldFinish)
        }
    }
}
