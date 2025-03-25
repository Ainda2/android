package mega.privacy.android.data.di

import android.webkit.MimeTypeMap
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.mapper.AccountSessionDetailMapper
import mega.privacy.android.data.mapper.AccountStorageDetailMapper
import mega.privacy.android.data.mapper.AccountTransferDetailMapper
import mega.privacy.android.data.mapper.AchievementsOverviewMapper
import mega.privacy.android.data.mapper.BooleanPreferenceMapper
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.ContactRequestMapper
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.data.mapper.CountryMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.LocalPricingMapper
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.MegaPurchaseMapper
import mega.privacy.android.data.mapper.MegaSkuMapper
import mega.privacy.android.data.mapper.MimeTypeMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.PaymentMethodTypeMapper
import mega.privacy.android.data.mapper.PricingMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderIntMapperImpl
import mega.privacy.android.data.mapper.SortOrderMapper
import mega.privacy.android.data.mapper.SortOrderMapperImpl
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapperImpl
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapperImpl
import mega.privacy.android.data.mapper.changepassword.PasswordStrengthMapper
import mega.privacy.android.data.mapper.changepassword.PasswordStrengthMapperImpl
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.getMimeType
import mega.privacy.android.data.mapper.mapBooleanPreference
import mega.privacy.android.data.mapper.mapMegaNodeListToNodeUpdate
import mega.privacy.android.data.mapper.storageStateToInt
import mega.privacy.android.data.mapper.toAccountSessionDetail
import mega.privacy.android.data.mapper.toAccountStorageDetail
import mega.privacy.android.data.mapper.toAccountTransferDetail
import mega.privacy.android.data.mapper.toAchievementsOverview
import mega.privacy.android.data.mapper.toChatFilesFolderUserAttribute
import mega.privacy.android.data.mapper.toContactRequest
import mega.privacy.android.data.mapper.toCountry
import mega.privacy.android.data.mapper.toCountryCallingCodes
import mega.privacy.android.data.mapper.toDuration
import mega.privacy.android.data.mapper.toImage
import mega.privacy.android.data.mapper.toLocalPricing
import mega.privacy.android.data.mapper.toMediaStoreFileTypeUri
import mega.privacy.android.data.mapper.toMegaAchievement
import mega.privacy.android.data.mapper.toMegaPurchase
import mega.privacy.android.data.mapper.toMegaSku
import mega.privacy.android.data.mapper.toPaymentMethodType
import mega.privacy.android.data.mapper.toPricing
import mega.privacy.android.data.mapper.toUserSet
import mega.privacy.android.data.mapper.toVideo
import mega.privacy.android.data.mapper.verification.SmsPermissionMapper
import mega.privacy.android.data.mapper.verification.SmsPermissionMapperImpl
import mega.privacy.android.data.mapper.viewtype.ViewTypeMapper
import mega.privacy.android.data.mapper.viewtype.ViewTypeMapperImpl
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.preference.StartScreen
import nz.mega.sdk.MegaChatMessage

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MapperModule {

    @Binds
    abstract fun bindSmsPermissionMapper(implementation: SmsPermissionMapperImpl): SmsPermissionMapper

    @Binds
    abstract fun bindUploadOptionIntMapper(implementation: UploadOptionIntMapperImpl): UploadOptionIntMapper

    @Binds
    abstract fun bindViewTypeMapper(implementation: ViewTypeMapperImpl): ViewTypeMapper

    @Binds
    abstract fun bindCameraUploadsHandlesMapper(implementation: CameraUploadsHandlesMapperImpl): CameraUploadsHandlesMapper


    @Binds
    abstract fun bindSortOrderMapper(implementation: SortOrderMapperImpl): SortOrderMapper

    @Binds
    abstract fun bindSortOrderIntMapper(implementation: SortOrderIntMapperImpl): SortOrderIntMapper

    /**
     * Provides PasswordStrength Mapper
     */
    @Binds
    abstract fun bindsPasswordStrengthMapper(implementation: PasswordStrengthMapperImpl): PasswordStrengthMapper


    companion object {
        /**
         * Provide start screen mapper
         */
        @Provides
        fun provideStartScreenMapper(): StartScreenMapper = { StartScreen(it) }

        /**
         * Provide images mapper
         */
        @Provides
        fun provideImagesMapper(): ImageMapper = ::toImage

        /**
         * Provide videos mapper
         */
        @Provides
        fun provideVideosMapper(): VideoMapper = ::toVideo

        /**
         * Provide media store file type uri mapper
         */
        @Provides
        fun provideMediaStoreFileTypeUriMapper(): MediaStoreFileTypeUriMapper =
            ::toMediaStoreFileTypeUri

        /**
         * Provide [StorageState] to [Int] mapper
         */
        @Provides
        fun provideStorageStateIntMapper(): StorageStateIntMapper = ::storageStateToInt

        /**
         * Provide node update mapper
         */
        @Provides
        fun provideNodeUpdateMapper(): NodeUpdateMapper = ::mapMegaNodeListToNodeUpdate

        /**
         * Provide boolean preference mapper
         */
        @Provides
        fun provideBooleanPreferenceMapper(): BooleanPreferenceMapper = ::mapBooleanPreference

        /**
         * Provide contact request mapper
         */
        @Provides
        fun provideContactRequestMapper(): ContactRequestMapper = ::toContactRequest

        /**
         * Provide mime type mapper
         */
        @Provides
        fun provideMimeTypeMapper(): MimeTypeMapper = { extension ->
            getMimeType(extension, MimeTypeMap.getSingleton()::getMimeTypeFromExtension)
        }

        /**
         * Provide local pricing mapper
         */
        @Provides
        fun provideLocalPricingMapper(): LocalPricingMapper = ::toLocalPricing

        /**
         * Provide currency mapper
         */
        @Provides
        fun provideCurrencyMapper(): CurrencyMapper = ::Currency

        /**
         * Provide paymentMethod type mapper
         */
        @Provides
        fun providePaymentMethodTypeMapper(): PaymentMethodTypeMapper = ::toPaymentMethodType

        /**
         * Provide mega achievement mapper
         */
        @Provides
        fun provideMegaAchievementMapper(): MegaAchievementMapper = ::toMegaAchievement

        /**
         * Provide mega achievements detail mapper
         */
        @Provides
        fun provideAchievementsOverviewMapper(): AchievementsOverviewMapper =
            ::toAchievementsOverview

        /**
         * Provide [UserSetMapper] mapper
         */
        @Provides
        fun provideUserSetMapper(): UserSetMapper = ::toUserSet

        /**
         * Provide [CountryMapper] mapper
         */
        @Provides
        fun provideCountryMapper(): CountryMapper = ::toCountry

        /**
         * Provide country calling codes mapper
         */
        @Provides
        fun provideCountryCallingCodeMapper(): CountryCallingCodeMapper =
            CountryCallingCodeMapper(::toCountryCallingCodes)

        /**
         * Provide pricing mapper
         *
         */
        @Provides
        fun providePricingMapper(): PricingMapper = ::toPricing

        @Provides
        fun provideMegaSkuMapper(): MegaSkuMapper = ::toMegaSku

        @Provides
        fun provideMegaPurchaseMapper(): MegaPurchaseMapper = ::toMegaPurchase

        @Provides
        fun provideChatFilesFolderUserAttributeMapper(): ChatFilesFolderUserAttributeMapper =
            ::toChatFilesFolderUserAttribute

        /**
         * Provide account transfer detail mapper
         */
        @Provides
        fun provideAccountTransferDetailMapper(): AccountTransferDetailMapper =
            ::toAccountTransferDetail

        /**
         * Provide account session detail mapper
         */
        @Provides
        fun provideAccountSessionDetailMapper(): AccountSessionDetailMapper =
            ::toAccountSessionDetail

        /**
         * Provide account storage detail mapper
         */
        @Provides
        fun provideAccountStorageDetailMapper(): AccountStorageDetailMapper =
            ::toAccountStorageDetail

        /**
         * Provide file duration mapper
         */
        @Provides
        fun provideFileDurationMapper(): FileDurationMapper = ::toDuration

        /**
         * Provide gson builder
         */
        @Provides
        fun provideGsonBuilder(): GsonBuilder = GsonBuilder()

        @Provides
        fun provideChatMessageMapper(mapper: ChatMessageMapper): @JvmSuppressWildcards suspend (@JvmSuppressWildcards MegaChatMessage) -> @JvmSuppressWildcards ChatMessage =
            mapper::invoke
    }
}
