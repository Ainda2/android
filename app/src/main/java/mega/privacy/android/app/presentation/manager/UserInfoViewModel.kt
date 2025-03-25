package mega.privacy.android.app.presentation.manager

import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.manager.model.UserInfoUiState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.MonitorContactCacheUpdates
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.UpdateMyAvatarWithNewEmail
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class UserInfoViewModel @Inject constructor(
    private val getCurrentUserFullName: GetCurrentUserFullName,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val updateMyAvatarWithNewEmail: UpdateMyAvatarWithNewEmail,
    private val monitorContactCacheUpdates: MonitorContactCacheUpdates,
    private val reloadContactDatabase: ReloadContactDatabase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val avatarContentMapper: AvatarContentMapper,
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase,
    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(UserInfoUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email }
                .collect {
                    when (it) {
                        UserChanges.Email -> handleEmailChange()
                        UserChanges.Firstname,
                        UserChanges.Lastname,
                        -> {
                            getUserFullName(true)
                            getUserAvatarOrDefault(isForceRefresh = false)
                        }

                        else -> Unit
                    }
                }
        }
        viewModelScope.launch {
            monitorContactCacheUpdates()
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Contact cache update: $it")
                }
        }
        viewModelScope.launch {
            monitorMyAvatarFile()
                .catch { Timber.e(it) }
                .collect {
                    getUserAvatarOrDefault(isForceRefresh = false)
                }
        }
        // Load from the cache first, in case offline mode
        viewModelScope.launch {
            getMyEmail(false)
            getUserFullName(false)
            getUserAvatarOrDefault(false)
        }
    }

    private suspend fun getUserAvatarOrDefault(isForceRefresh: Boolean) {
        val avatarFile = runCatching { getMyAvatarFileUseCase(isForceRefresh) }
            .onFailure { Timber.e(it) }.getOrNull()
        val avatarContent = avatarContentMapper(
            fullName = _state.value.fullName,
            localFile = avatarFile,
            backgroundColor = getMyAvatarColorUseCase(),
            showBorder = false,
            textSize = 36.sp
        )
        _state.update {
            it.copy(
                avatarContent = avatarContent,
            )
        }
    }

    private suspend fun handleEmailChange() {
        val oldEmail = _state.value.email
        getMyEmail(true)
        val newEmail = _state.value.email
        runCatching { updateMyAvatarWithNewEmail(oldEmail, newEmail) }
            .onSuccess { success -> if (success) Timber.d("The avatar file was correctly renamed") }
            .onFailure {
                Timber.e(it, "EXCEPTION renaming the avatar on changing email")
            }
    }

    /**
     * Get user info from sdk
     *
     */
    fun getUserInfo() {
        viewModelScope.launch {
            getMyEmail(true)
            getUserFullName(true)
            getUserAvatarOrDefault(true)
        }
    }

    private suspend fun getMyEmail(forceRefresh: Boolean) {
        runCatching { getCurrentUserEmail(forceRefresh) }
            .onSuccess { mail ->
                _state.update { it.copy(email = mail.orEmpty()) }
            }.onFailure {
                Timber.e(it)
            }
    }

    private suspend fun getUserFullName(isForceRefresh: Boolean) {
        runCatching {
            getCurrentUserFullName(
                forceRefresh = isForceRefresh,
                defaultFirstName = context.getString(R.string.first_name_text),
                defaultLastName = context.getString(R.string.lastname_text),
            )
        }.onSuccess { fullName ->
            _state.update { it.copy(fullName = fullName) }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Reload contact database
     * Make it run in application scope so it still running when activity destroyed
     */
    fun refreshContactDatabase(isForce: Boolean) {
        applicationScope.launch {
            runCatching {
                reloadContactDatabase(isForce)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Check password reminder status
     *
     */
    fun checkPasswordReminderStatus() {
        viewModelScope.launch {
            runCatching { checkPasswordReminderUseCase(false) }
                .onSuccess { show ->
                    _state.update { it.copy(isTestPasswordRequired = show) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Get user achievements
     */
    fun getUserAchievements() {
        viewModelScope.launch {
            runCatching { getAccountAchievementsOverviewUseCase() }
                .onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Show test password handled
     *
     */
    fun onTestPasswordHandled() {
        _state.update { it.copy(isTestPasswordRequired = false) }
    }
}