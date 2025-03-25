package mega.privacy.android.app.presentation.node.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.Test
import org.mockito.kotlin.mock

class GetDefaultFolderIconTest {
    @Test
    fun `test that folders in the rubbish bin returns folder icon`() {
        val folderNode = mock<TypedFolderNode> { on { isInRubbishBin }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_medium_solid)
    }


    @Test
    fun `test that incoming share returns the incoming folder icon`() {
        val folderNode = mock<TypedFolderNode> { on { isIncomingShare }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_incoming_medium_solid)
    }

    @Test
    fun `test that media sync folders return camera backup icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.MediaSyncFolder)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_camera_uploads_medium_solid)
    }

    @Test
    fun `test that chat files folder returns chat folder icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.ChatFilesFolder)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_chat_medium_solid)
    }

    @Test
    fun `test that shared folder returns shared icon`() {
        val folderNode = mock<TypedFolderNode> { on { isShared }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_outgoing_medium_solid)
    }

    @Test
    fun `test that pending share folder returns shared icon`() {
        val folderNode = mock<TypedFolderNode> { on { isPendingShare }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_outgoing_medium_solid)
    }

    @Test
    fun `test that root backup folder returns cloud backup icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.RootBackup)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_backup_medium_solid)
    }

    @Test
    fun `test that windows device backup returns windows icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Windows))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_windows_medium_solid)
    }

    @Test
    fun `test that linux device backup returns linux icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Linux))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_linux_medium_solid)
    }

    @Test
    fun `test that mac device backup returns mac icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Mac))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_mac_medium_solid)
    }

    @Test
    fun `test that external device backup returns external icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.ExternalDrive))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_external_drive_medium_solid)
    }

    @Test
    fun `test that unknown device backup returns generic pc icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Unknown))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_medium_solid)
    }

    @Test
    fun `test that child backup folder returns backup icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.ChildBackup)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_backup_medium_solid)
    }

    @Test
    fun `test that synced folder returns sync icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.Sync)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_sync_medium_solid)
    }

    @Test
    fun `test that normal folder returns folder icon`() {
        val folderNode = mock<TypedFolderNode>()

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_medium_solid)
    }

}