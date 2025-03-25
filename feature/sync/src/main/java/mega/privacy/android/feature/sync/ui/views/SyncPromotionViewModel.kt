package mega.privacy.android.feature.sync.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.SetSyncPromotionShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.ShouldShowSyncPromotionUseCase
import mega.privacy.android.feature.sync.ui.model.SyncPromotionState
import timber.log.Timber
import javax.inject.Inject

/**
 * Sync promotion view model
 *
 * @property shouldShowSyncPromotionUseCase [ShouldShowSyncPromotionUseCase]
 * @property setSyncPromotionShownUseCase [SetSyncPromotionShownUseCase]
 */
@HiltViewModel
class SyncPromotionViewModel @Inject constructor(
    private val shouldShowSyncPromotionUseCase: ShouldShowSyncPromotionUseCase,
    private val setSyncPromotionShownUseCase: SetSyncPromotionShownUseCase,
) : ViewModel() {

    /**
     * Private state
     */
    private val _state = MutableStateFlow(SyncPromotionState())

    /**
     * Public state
     */
    val state: StateFlow<SyncPromotionState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                shouldShowSyncPromotionUseCase()
            }.onSuccess { value ->
                _state.update {
                    it.copy(shouldShowSyncPromotion = value)
                }
            }.onFailure { error ->
                Timber.w("Error checking Sync Promotion: $error")
            }
        }
    }

    /**
     * Consume shouldShowSyncPromotion
     */
    fun onConsumeShouldShowSyncPromotion() {
        _state.update { state -> state.copy(shouldShowSyncPromotion = false) }
    }

    /**
     * Set that Sync Promotion has been shown
     *
     * @param doNotShowAgain True to do not show it again or False otherwise
     */
    fun setSyncPromotionShown(doNotShowAgain: Boolean = false) = viewModelScope.launch {
        runCatching {
            setSyncPromotionShownUseCase(doNotShowAgain)
        }.onFailure { error -> Timber.d(error) }
    }
}