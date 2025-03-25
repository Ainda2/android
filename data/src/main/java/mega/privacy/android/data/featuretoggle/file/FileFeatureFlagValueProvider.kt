package mega.privacy.android.data.featuretoggle.file

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mega.privacy.android.data.gateway.AssetsGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File feature flag value provider
 *
 */
@Singleton
class FileFeatureFlagValueProvider @Inject constructor(
    private val assetsGateway: AssetsGateway,
) : FeatureFlagValueProvider {
    private val featureFlagMaps: Map<String, Boolean> by lazy {
        assetsGateway.open("featuretoggle/feature_flags.json").bufferedReader().use { reader ->
            runCatching {
                Gson().fromJson<List<FileFeatures>>(
                    reader,
                    object : TypeToken<List<FileFeatures>>() {}.type
                )
            }.onFailure { Timber.e(it) }
                .getOrDefault(emptyList())
        }.associate { it.name to it.defaultValue }
    }

    override suspend fun isEnabled(feature: Feature): Boolean? =
        featureFlagMaps[feature.name]

    override val priority = FeatureFlagValuePriority.ConfigurationFile
}