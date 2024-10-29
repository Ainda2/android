package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.ERROR
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.BATTERY_LOW
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.STALLED_ISSUE
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.NOT_CONNECTED_TO_WIFI
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import javax.inject.Inject

/**
 * Use case to get the sync notification message based on occurred stalled issues / errors.
 *
 */
class GetSyncNotificationUseCase @Inject constructor(
    private val syncNotificationRepository: SyncNotificationRepository,
) {

    /**
     * Follow the business flow to decide if a sync notification should be displayed.
     * If the sync notification is not required, null is returned
     */
    suspend operator fun invoke(
        isBatteryLow: Boolean,
        isUserOnWifi: Boolean,
        isSyncOnlyByWifi: Boolean,
        syncs: List<FolderPair>,
        stalledIssues: List<StalledIssue>,
    ): SyncNotificationMessage? {
        val isNetworkConstraintRespected = isSyncOnlyByWifi && isUserOnWifi || !isSyncOnlyByWifi

        return when {
            isBatteryLow -> {
                getBatteryLowNotification()
            }

            !isNetworkConstraintRespected -> {
                resetBatteryLowNotification()
                getNetworkConstraintNotification()
            }

            syncs.any { it.syncError != SyncError.NO_SYNC_ERROR } -> {
                resetBatteryLowNotification()
                resetNetworkConstraintNotification()
                getSyncErrorsNotification(syncs)
            }

            stalledIssues.isNotEmpty() -> {
                resetBatteryLowNotification()
                resetNetworkConstraintNotification()
                resetSyncErrorsNotification()
                getStalledIssuesNotification(stalledIssues)
            }

            else -> {
                resetBatteryLowNotification()
                resetNetworkConstraintNotification()
                resetSyncErrorsNotification()
                resetSyncStalledIssuesNotification()
                null
            }
        }
    }

    private suspend fun resetBatteryLowNotification() {
        syncNotificationRepository.deleteDisplayedNotificationByType(BATTERY_LOW)
    }

    private suspend fun resetNetworkConstraintNotification() {
        syncNotificationRepository.deleteDisplayedNotificationByType(NOT_CONNECTED_TO_WIFI)
    }

    private suspend fun resetSyncErrorsNotification() {
        syncNotificationRepository.deleteDisplayedNotificationByType(ERROR)
    }

    private suspend fun resetSyncStalledIssuesNotification() {
        syncNotificationRepository.deleteDisplayedNotificationByType(STALLED_ISSUE)

    }

    private suspend fun getBatteryLowNotification(): SyncNotificationMessage? =
        if (syncNotificationRepository.getDisplayedNotificationsByType(BATTERY_LOW).isEmpty()) {
            syncNotificationRepository.getBatteryLowNotification()
        } else {
            null
        }

    private suspend fun getNetworkConstraintNotification(): SyncNotificationMessage? =
        if (syncNotificationRepository.getDisplayedNotificationsByType(NOT_CONNECTED_TO_WIFI)
                .isEmpty()
        ) {
            syncNotificationRepository.getUserNotOnWifiNotification()
        } else {
            null
        }

    private suspend fun getSyncErrorsNotification(syncs: List<FolderPair>): SyncNotificationMessage? {
        val shownSyncErrors = syncNotificationRepository.getDisplayedNotificationsByType(ERROR)
        val newSyncErrors = syncs.filter { sync ->
            shownSyncErrors.none {
                it.notificationDetails.path.equals(
                    sync.localFolderPath, ignoreCase = true
                )
            }
        }
        return if (newSyncErrors.isNotEmpty()) {
            syncNotificationRepository.getSyncErrorsNotification(newSyncErrors)
        } else {
            null
        }
    }

    private suspend fun getStalledIssuesNotification(stalledIssues: List<StalledIssue>): SyncNotificationMessage? {
        val shownStalledIssues =
            syncNotificationRepository.getDisplayedNotificationsByType(STALLED_ISSUE)
        val newStalledIssues = stalledIssues.filter { stalledIssue ->
            shownStalledIssues.none { shownStalledIssue ->
                shownStalledIssue.notificationDetails.path.equals(
                    stalledIssue.localPaths.firstOrNull(),
                    ignoreCase = true
                ) || shownStalledIssue.notificationDetails.path.equals(
                    stalledIssue.nodeNames.firstOrNull(),
                    ignoreCase = true
                )
            }
        }
        return if (newStalledIssues.isNotEmpty()) {
            syncNotificationRepository.getSyncStalledIssuesNotification(newStalledIssues)
        } else {
            null
        }
    }
}