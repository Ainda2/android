package mega.privacy.android.feature.devicecenter.domain.entity

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.domain.entity.node.NodeId

/**
 * A domain data class representing a Backup Folder of a Backup Device
 *
 * @property id The Device Folder ID
 * @property name The Device Folder Name
 * @property status The Device Folder Status
 * @property rootHandle The Device Folder Root NodeId(Handle)
 * @property type The Device Folder Type
 * @property userAgent The Device Folder User Agent
 * @property localFolderPath The Device Folder Local Folder Path
 */
data class DeviceFolderNode(
    override val id: String,
    override val name: String,
    override val status: DeviceCenterNodeStatus,
    val rootHandle: NodeId,
    val localFolderPath: String,
    val type: BackupInfoType,
    val userAgent: BackupInfoUserAgent,
) : DeviceCenterNode
