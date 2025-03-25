package mega.privacy.android.domain.entity.backup

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError

/**
 * Data class representing the User's Backup information from the "sf" API Call. This class is
 * mapped from nz.mega.sdk.MegaBackupInfo
 *
 * @property id The Backup ID
 * @property type Any of the [BackupInfoType] values
 * @property rootHandle The Backup Root NodeId(Handle)
 * @property localFolderPath The name of the backed up Local Folder
 * @property deviceId The Device ID where the Backup originated
 * @property userAgent Any of the [BackupInfoUserAgent] values, which associates the Device where the
 * Backup originated
 * @property state Any of the [BackupInfoState] values
 * @property subState Any of the [SyncError] values
 * @property extraInfo The extra information used as a source for extracting other details
 * @property name The Backup Name
 * @property timestamp The Backup Timestamp reported by Heartbeats, measured in seconds
 * @property status Any of the [BackupInfoHeartbeatStatus] values
 * @property progress The Backup Progress reported by Heartbeats
 * @property uploadCount The total Upload count
 * @property downloadCount The total Download count
 * @property lastActivityTimestamp The Last Activity Timestamp reported by Heartbeats, measured in seconds
 * @property lastSyncedNodeHandle The Last Synced Node Handle
 */
data class BackupInfo(
    val id: Long,
    val type: BackupInfoType,
    val rootHandle: NodeId,
    val localFolderPath: String?,
    val deviceId: String?,
    val userAgent: BackupInfoUserAgent,
    val state: BackupInfoState,
    val subState: SyncError,
    val extraInfo: String?,
    val name: String?,
    val timestamp: Long,
    val status: BackupInfoHeartbeatStatus,
    val progress: Int,
    val uploadCount: Int,
    val downloadCount: Int,
    val lastActivityTimestamp: Long,
    val lastSyncedNodeHandle: Long,
)
