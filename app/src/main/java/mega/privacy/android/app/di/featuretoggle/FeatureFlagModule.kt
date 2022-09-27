package mega.privacy.android.app.di.featuretoggle

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoMap
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Feature flag module
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagModule {

    companion object {
        /**
         * Provide features
         *
         * @return App features
         */
        @Provides
        @ElementsIntoSet
        fun provideFeatures(): Set<@JvmSuppressWildcards Feature> =
            AppFeatures.values().toSet()

        /**
         * Provide feature flag value provider
         *
         */
        @Provides
        @IntoMap
        @FeatureFlagPriorityKey(
            implementingClass = AppFeatures.Companion::class,
            priority = FeatureFlagValuePriority.Default
        )
        fun provideFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            AppFeatures.Companion

    }
}