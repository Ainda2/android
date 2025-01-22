package mega.privacy.android.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature

enum class DomainFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {
    /**
     * Use file descriptor for uploads to avoid the copy to the cache folder
     */
    UseFileDescriptorForUploads(
        "Use file descriptor for uploads to avoid the copy to the cache folder",
        true,
    ),

    /**
     * Use file descriptor for uploads to avoid the copy to the cache folder
     */
    AllowToChooseDownloadDestination(
        "Allow to choose the download destination regardless of android version",
        false,
    ),
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            DomainFeatures.entries.firstOrNull { it == feature }?.defaultValue

        override val priority = FeatureFlagValuePriority.Default
    }
}