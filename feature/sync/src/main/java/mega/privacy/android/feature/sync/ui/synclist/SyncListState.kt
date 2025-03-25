package mega.privacy.android.feature.sync.ui.synclist

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncOption

internal data class SyncListState(
    val stalledIssuesCount: Int = 0,
    @StringRes val snackbarMessage: Int? = null,
    val shouldShowCleanSolvedIssueMenuItem: Boolean = false,
    val selectedSyncOption: SyncOption = SyncOption.WI_FI_OR_MOBILE_DATA,
    val deviceName: String = "",
)