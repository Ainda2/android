package mega.privacy.android.app.di

import mega.privacy.android.domain.di.SharedUseCaseModule as DomainSharedUseCaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.IsDatabaseEntryStale
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [DomainSharedUseCaseModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestSharedUseCases {

    @Provides
    fun provideIsDatabaseEntryStale() = mock<IsDatabaseEntryStale>()

    @Provides
    fun provideGetExtendedAccountDetail() = mock<GetExtendedAccountDetail>()

    @Provides
    fun provideGetPricing() = mock<GetPricing>()

    @Provides
    fun provideGetNumberOfSubscription() = mock<GetNumberOfSubscription>()

    @Provides
    fun provideGetFileVersionsOption() = mock<GetFileVersionsOption>()

    @Provides
    fun provideGetCurrentUserFullName() = mock<GetCurrentUserFullName>()

    @Provides
    fun provideGetUserFullName() = mock<GetUserFullNameUseCase>()
}
