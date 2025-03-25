package mega.privacy.android.app.presentation.settings.filesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.filesettings.model.FilePreferencesState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.cache.ClearCacheUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheSizeUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.filenode.RemoveAllVersionsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.ClearOfflineUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderSizeUseCase
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption
import timber.log.Timber
import javax.inject.Inject

/**
 * File Preferences ViewModel
 */
@HiltViewModel
class FilePreferencesViewModel @Inject constructor(
    private val getFolderVersionInfo: GetFolderVersionInfo,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFileVersionsOption: GetFileVersionsOption,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val enableFileVersionsOption: EnableFileVersionsOption,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val getCacheSizeUseCase: GetCacheSizeUseCase,
    private val getOfflineFolderSizeUseCase: GetOfflineFolderSizeUseCase,
    private val clearOfflineUseCase: ClearOfflineUseCase,
    private val removeAllVersionsUseCase: RemoveAllVersionsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(FilePreferencesState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        viewModelScope.launch {
            val info = getFolderVersionInfo()
            _state.update {
                it.copy(
                    numberOfPreviousVersions = info?.numberOfVersions,
                    sizeOfPreviousVersionsInBytes = info?.sizeOfPreviousVersionsInBytes
                )
            }
        }
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .filter { it == UserChanges.DisableVersions }
                .collect { getFileVersionOption() }
        }
        getFileVersionOption()
    }

    private fun getFileVersionOption() {
        viewModelScope.launch {
            runCatching { getFileVersionsOption(forceRefresh = true) }
                .onSuccess { isDisableFileVersions ->
                    _state.update { it.copy(isFileVersioningEnabled = isDisableFileVersions.not()) }
                }
        }
    }

    /**
     * Reset versions info
     *
     */
    fun resetVersionsInfo() {
        _state.update {
            it.copy(
                numberOfPreviousVersions = 0,
                sizeOfPreviousVersionsInBytes = 0
            )
        }
    }

    /**
     * Enable file version option
     *
     * @param enable
     */
    fun enableFileVersionOption(enable: Boolean) = viewModelScope.launch {
        runCatching { enableFileVersionsOption(enabled = enable) }
            .onSuccess { _state.update { it.copy(isFileVersioningEnabled = enable) } }
            .onFailure { Timber.w("Exception enabling file versioning.", it) }
    }

    /**
     * Clear the cache
     */
    fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            getCacheSize()
        }
    }

    /**
     * Get cache size
     */
    fun getCacheSize() {
        viewModelScope.launch {
            val size = getCacheSizeUseCase()
            _state.update { it.copy(updateCacheSizeSetting = size) }
        }
    }

    /**
     * Clear offline
     */
    fun clearOffline() {
        viewModelScope.launch {
            clearOfflineUseCase()
            getOfflineFolderSize()
        }
    }

    /**
     * Get offline size
     */
    fun getOfflineFolderSize() {
        viewModelScope.launch {
            val size = getOfflineFolderSizeUseCase()
            _state.update { it.copy(updateOfflineSize = size) }
        }
    }

    /**
     * Reset updateCacheSizeSetting
     */
    fun resetUpdateCacheSizeSetting() {
        _state.update { it.copy(updateCacheSizeSetting = null) }
    }

    /**
     * Reset updateCacheSizeSetting
     */
    fun resetUpdateOfflineSize() {
        _state.update { it.copy(updateOfflineSize = null) }
    }

    /**
     * Clear all versions
     */
    fun clearAllVersions() {
        viewModelScope.launch {
            runCatching {
                removeAllVersionsUseCase()
            }.onSuccess {
                _state.update { it.copy(deleteAllVersionsEvent = triggered<Throwable?>(null)) }
            }.onFailure { e ->
                _state.update { it.copy(deleteAllVersionsEvent = triggered<Throwable?>(e)) }
            }
        }
    }

    /**
     * Reset delete all versions event
     */
    fun resetDeleteAllVersionsEvent() {
        _state.update { it.copy(deleteAllVersionsEvent = consumed()) }
    }
}
