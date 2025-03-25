package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

@Composable
internal fun SyncItemView(
    modifier: Modifier,
    syncUiItems: List<SyncUiItem>,
    itemIndex: Int,
    cardExpanded: (SyncUiItem, Boolean) -> Unit,
    pauseRunClicked: (SyncUiItem) -> Unit,
    removeFolderClicked: (SyncUiItem) -> Unit,
    issuesInfoClicked: () -> Unit,
    onOpenDeviceFolderClicked: (String) -> Unit,
    onOpenMegaFolderClicked: (SyncUiItem) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    deviceName: String,
    @StringRes errorRes: Int? = null
) {
    val sync = syncUiItems[itemIndex]
    SyncCard(
        modifier = modifier.testTag(TEST_TAG_SYNC_ITEM_VIEW),
        sync = sync,
        expandClicked = {
            cardExpanded(sync, !sync.expanded)
        },
        pauseRunClicked = {
            pauseRunClicked(sync)
        },
        removeFolderClicked = {
            removeFolderClicked(sync)
        },
        issuesInfoClicked = issuesInfoClicked,
        onOpenDeviceFolderClicked = onOpenDeviceFolderClicked,
        onOpenMegaFolderClicked = {
            onOpenMegaFolderClicked(sync)
        },
        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
        isLowBatteryLevel = isLowBatteryLevel,
        errorRes = errorRes,
        deviceName = deviceName,
    )
}

internal const val TEST_TAG_SYNC_ITEM_VIEW = "sync_item_view:root"
