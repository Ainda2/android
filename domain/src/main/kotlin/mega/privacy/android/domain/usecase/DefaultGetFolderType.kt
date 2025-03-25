package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default [GetFolderType] implementation
 */
class DefaultGetFolderType @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val chatRepository: ChatRepository,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val hasAncestor: HasAncestor,
    private val getDeviceType: GetDeviceType,
    private val nodeRepository: NodeRepository,
) : GetFolderType {
    override suspend fun invoke(folder: FolderNode) = with(folder) {
        when {
            isMediaSyncFolder(id) -> FolderType.MediaSyncFolder
            isChatFolder(id) -> FolderType.ChatFilesFolder
            isRootBackup(id) -> FolderType.RootBackup
            isChildBackup(id) -> FolderType.ChildBackup
            isDeviceFolder(this) -> FolderType.DeviceBackup(getDeviceType(this))
            isSyncedFolder(id) -> FolderType.Sync
            else -> FolderType.Default
        }
    }

    private suspend fun isMediaSyncFolder(folder: NodeId) = folder.longValue in listOf(
        cameraUploadsRepository.getPrimarySyncHandle(),
        cameraUploadsRepository.getSecondarySyncHandle()
    )

    private suspend fun isChatFolder(folder: NodeId) =
        chatRepository.getChatFilesFolderId() == folder

    private suspend fun isRootBackup(folder: NodeId) =
        monitorBackupFolder().firstOrNull()?.getOrNull() == folder

    private suspend fun isChildBackup(
        nodeId: NodeId,
    ) = monitorBackupFolder()
        .firstOrNull()
        ?.getOrNull()
        ?.let { hasAncestor(nodeId, it) } ?: false

    private fun isDeviceFolder(folder: FolderNode) =
        !folder.device.isNullOrEmpty()

    private suspend fun isSyncedFolder(nodeId: NodeId) =
        nodeRepository.isNodeSynced(nodeId = nodeId)
}
