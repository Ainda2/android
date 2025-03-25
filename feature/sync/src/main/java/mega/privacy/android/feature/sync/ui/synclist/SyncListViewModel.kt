package mega.privacy.android.feature.sync.ui.synclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceNameUseCase
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncOption
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncListViewModel @Inject constructor(
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase,
    private val stalledIssueItemMapper: StalledIssueItemMapper,
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase,
    private val clearSyncSolvedIssuesUseCase: ClearSyncSolvedIssuesUseCase,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getDeviceNameUseCase: GetDeviceNameUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncListState())
    val state: StateFlow<SyncListState> = _state.asStateFlow()

    init {
        observeOnboardingFlow()
        monitorStalledIssue()
        monitorSolvedIssue()
        monitorSyncByWifiSetting()
        getDeviceName()
    }

    private fun observeOnboardingFlow() {
        viewModelScope.launch { setOnboardingShownUseCase(true) }
    }

    private fun monitorStalledIssue() {
        viewModelScope.launch {
            monitorSyncStalledIssuesUseCase().catch { Timber.e(it) }
                .collect { stalledIssues ->
                    _state.update {
                        it.copy(
                            stalledIssuesCount = stalledIssues.size
                        )
                    }
                }
        }
    }

    private fun monitorSolvedIssue() {
        viewModelScope.launch {
            monitorSyncSolvedIssuesUseCase().catch { Timber.e(it) }.collect {
                _state.update { state ->
                    state.copy(
                        shouldShowCleanSolvedIssueMenuItem = it.isNotEmpty()
                    )
                }
            }
        }
    }

    private fun monitorSyncByWifiSetting() {
        viewModelScope.launch {
            monitorSyncByWiFiUseCase()
                .collectLatest { syncByWiFi ->
                    _state.update { state ->
                        state.copy(
                            selectedSyncOption = if (syncByWiFi) {
                                SyncOption.WI_FI_ONLY
                            } else {
                                SyncOption.WI_FI_OR_MOBILE_DATA
                            }
                        )
                    }
                }
        }
    }

    private fun getDeviceName() {
        viewModelScope.launch {
            runCatching {
                getDeviceIdUseCase()?.let { deviceId ->
                    val deviceName = getDeviceNameUseCase(deviceId).orEmpty()
                    _state.update { it.copy(deviceName = deviceName) }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun handleAction(action: SyncListAction) {
        when (action) {
            is SyncListAction.ResolveStalledIssue -> {
                viewModelScope.launch {
                    resolveStalledIssueUseCase(
                        action.selectedResolution, stalledIssueItemMapper(action.uiItem)
                    )
                }
                _state.update {
                    it.copy(
                        snackbarMessage = R.string.sync_stalled_issue_conflict_resolved
                    )
                }
            }

            SyncListAction.SnackBarShown -> {
                _state.update { state ->
                    state.copy(snackbarMessage = null)
                }
            }
        }
    }

    fun onClearSyncOptionsPressed() {
        viewModelScope.launch {
            runCatching {
                clearSyncSolvedIssuesUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}