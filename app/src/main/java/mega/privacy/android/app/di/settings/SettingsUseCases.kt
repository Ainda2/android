package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.di.SettingModule
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.DefaultCanDeleteAccount
import mega.privacy.android.domain.usecase.DefaultIsChatLoggedIn
import mega.privacy.android.domain.usecase.DefaultMonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.DefaultMonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.DefaultToggleAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.SetChatImageQuality
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks

/**
 * Settings use cases module
 *
 * Provides use cases used by the [mega.privacy.android.app.presentation.settings.SettingsViewModel]
 */
@Module(includes = [SettingModule::class])
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
abstract class SettingsUseCases {

    @Binds
    abstract fun bindCanDeleteAccount(useCase: DefaultCanDeleteAccount): CanDeleteAccount

    @Binds
    abstract fun bindToggleAutoAcceptQRLinks(useCase: DefaultToggleAutoAcceptQRLinks): ToggleAutoAcceptQRLinks

    @Binds
    abstract fun bindIsChatLoggedIn(useCase: DefaultIsChatLoggedIn): IsChatLoggedIn

    @Binds
    abstract fun bindMonitorAutoAcceptQRLinks(implementation: DefaultMonitorAutoAcceptQRLinks): MonitorAutoAcceptQRLinks

    /**
     * Provide MonitorMediaDiscoveryView implementation
     */
    @Binds
    abstract fun bindMonitorMediaDiscoveryView(implementation: DefaultMonitorMediaDiscoveryView): MonitorMediaDiscoveryView

    companion object {

        @Provides
        fun provideFetchAutoAcceptQRLinks(settingsRepository: SettingsRepository): FetchAutoAcceptQRLinks =
            FetchAutoAcceptQRLinks(settingsRepository::fetchContactLinksOption)

        @Provides
        fun provideIsMultiFactorAuthAvailable(accountRepository: AccountRepository): IsMultiFactorAuthAvailable =
            IsMultiFactorAuthAvailable(accountRepository::isMultiFactorAuthAvailable)

        @Provides
        fun provideRequestAccountDeletion(accountRepository: AccountRepository): RequestAccountDeletion =
            RequestAccountDeletion(accountRepository::requestDeleteAccountLink)

        @Provides
        fun provideGetChatImageQuality(settingsRepository: SettingsRepository): GetChatImageQuality =
            GetChatImageQuality(settingsRepository::getChatImageQuality)

        @Provides
        fun provideSetChatImageQuality(settingsRepository: SettingsRepository): SetChatImageQuality =
            SetChatImageQuality(settingsRepository::setChatImageQuality)

        /**
         * Provide SetMediaDiscoveryView implementation
         */
        @Provides
        fun provideSetMediaDiscoveryView(settingsRepository: SettingsRepository): SetMediaDiscoveryView =
            SetMediaDiscoveryView(settingsRepository::setMediaDiscoveryView)
    }
}
