package mega.privacy.android.app.presentation.fileinfo.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.account.model.AccountDeactivatedStatus
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Represents the view state of the File info screen
 * @param title the title for this screen, node's name
 * @param isFile true if the node is a file false if it's a folder
 * @param origin from which origin the info screen was opened
 * @param oneOffViewEvent one-off events to be consumed by the view
 * @param downloadEvent one-off event to start downloading the node
 * @param jobInProgressState indicates if there are any job in progress that needs to be notified
 * @param historyVersions the num of history versions that this file contains, 0 if it's not a file or doesn't contain history versions
 * @param isNodeInBackups determines if the node is in Backups or not
 * @param isNodeInRubbish determines if the node is in the rubbish bin or not
 * @param previewUriString the uri of the file containing the preview
 * @param thumbnailUriString the uri of the file containing the thumbnail, just as a fallback in case there's no [previewUriString]
 * @param folderTreeInfo the folder info if the node is a folder
 * @param outShares shares in case of a node shared with others as outgoing share (To be used when the compose view is used)
 * @param nodeLocationInfo the location info of the node
 * @param isAvailableOffline true if the file is available offline
 * @param isAvailableOfflineEnabled true if offline availability can be changed, false if not
 * @param isAvailableOfflineAvailable true if the available offline should be available (should be visible)
 * @param inShareOwnerContactItem the [ContactItem] of the owner
 * @param accessPermission the [AccessPermission] the user has to this node
 * @param contactToShowOptions the contact selected to show related options in the bottom sheet dialog
 * @param outShareContactsSelected a list of contacts selected to batch modify
 * @param iconResource the icon resource that represents this node
 * @param sizeInBytes Size of this node, for folder it will be the total size of its content, including versions
 * @param isExported
 * @param isTakenDown Node is taken down
 * @param publicLink the share link of the node
 * @param publicLinkCreationTime the date on which it was shared
 * @param showLink Share link should be shown
 * @param creationTime creation time of the node
 * @param modificationTime modification time of the node in case of file node
 * @param descriptionText description for node
 * @param hasPreview this node has a preview (Images, pdf, videos, etc.)
 * @param actions a list of [FileInfoMenuAction] representing available actions for this node
 * @param requiredExtraAction an initiated action that needs to be confirmed by the user or more data needs to be specified (typically by an alert dialog)
 * @param isRemindersForContactVerificationEnabled checks if reminders for contact verification is enabled
 * @param tagsEnabled checks if tags are enabled
 * @param tags list of tags for the node
 * @param mapLocationEnabled checks if GIS location is enabled
 * @param longitude the longitude of the node
 * @param latitude the latitude of the node
 * @param isPhoto true if the node is a photo (Image or Video)
 * @param accountDeactivatedStatus the status of the account if it's deactivated
 */
internal data class FileInfoViewState(
    val title: String = "",
    val isFile: Boolean = true,
    val origin: FileInfoOrigin = FileInfoOrigin.Other,
    val oneOffViewEvent: StateEventWithContent<FileInfoOneOffViewEvent> = consumed(),
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val jobInProgressState: FileInfoJobInProgressState? = FileInfoJobInProgressState.InitialLoading,
    val historyVersions: Int = 0,
    val isNodeInBackups: Boolean = false,
    val isNodeInRubbish: Boolean = false,
    val previewUriString: String? = null,
    val thumbnailUriString: String? = null,
    val folderTreeInfo: FolderTreeInfo? = null,
    val outShares: List<ContactPermission> = emptyList(),
    val nodeLocationInfo: LocationInfo? = null,
    val isAvailableOffline: Boolean = false,
    val isAvailableOfflineEnabled: Boolean = false,
    val isAvailableOfflineAvailable: Boolean = false,
    val inShareOwnerContactItem: ContactItem? = null,
    val accessPermission: AccessPermission = AccessPermission.UNKNOWN,
    val contactToShowOptions: ContactPermission? = null,
    val outShareContactsSelected: List<String> = emptyList(),
    val iconResource: Int? = null,
    val sizeInBytes: Long = 0,
    val isExported: Boolean = false,
    val isTakenDown: Boolean = false,
    val publicLink: String? = null,
    val publicLinkCreationTime: Long? = null,
    val showLink: Boolean = false,
    val creationTime: Long? = null,
    val modificationTime: Long? = null,
    val descriptionText: String = "",
    val hasPreview: Boolean = false,
    val actions: List<FileInfoMenuAction> = emptyList(),
    val requiredExtraAction: FileInfoExtraAction? = null,
    val isRemindersForContactVerificationEnabled: Boolean = false,
    val tagsEnabled: Boolean = false,
    val tags: List<String> = emptyList(),
    val mapLocationEnabled: Boolean = false,
    val longitude: Double = 0.0,
    val latitude: Double = 0.0,
    val isPhoto: Boolean = false,
    val accountDeactivatedStatus: AccountDeactivatedStatus? = null,
) {

    /**
     * Creates a copy of this view state with the info that can be extracted directly from typedNode
     */
    fun copyWithTypedNode(typedNode: TypedNode) = this.copy(
        title = typedNode.name,
        isFile = typedNode is FileNode,
        sizeInBytes = (typedNode as? FileNode)?.size ?: this.sizeInBytes,
        isAvailableOfflineAvailable = if (typedNode is FileNode) {
            true
        } else {
            this.isAvailableOfflineAvailable
        },
        isTakenDown = typedNode.isTakenDown,
        isExported = typedNode.exportedData != null,
        publicLink = typedNode.exportedData?.publicLink,
        publicLinkCreationTime = typedNode.exportedData?.publicLinkCreationTime,
        showLink = !typedNode.isTakenDown && typedNode.exportedData != null,
        creationTime = typedNode.creationTime,
        modificationTime = (typedNode as? TypedFileNode)?.modificationTime,
        descriptionText = typedNode.description.orEmpty(),
        hasPreview = (typedNode as? TypedFileNode)?.hasPreview == true,
        tags = typedNode.tags.orEmpty(),
    )


    /**
     * Check Conditions to enable description field
     */
    fun isDescriptionEnabled() = !isNodeInRubbish && !isNodeInBackups &&
            (accessPermission == AccessPermission.FULL ||
                    accessPermission == AccessPermission.OWNER)

    /**
     * Check Conditions to enable gis field
     */
    fun canEnableMapLocation() = mapLocationEnabled && accessPermission == AccessPermission.OWNER

    /**
     * Check Conditions to enable tags field
     */
    fun canEditTags() = tagsEnabled && !isNodeInRubbish && !isNodeInBackups &&
            (accessPermission == AccessPermission.OWNER || accessPermission == AccessPermission.FULL)

    fun canViewTags() = tagsEnabled && !isNodeInRubbish && !isNodeInBackups &&
            (accessPermission == AccessPermission.READ || accessPermission == AccessPermission.READWRITE)

    /**
     * Creates a copy of this view state with the info that can be extracted directly from folderTreeInfo
     */
    fun copyWithFolderTreeInfo(folderTreeInfo: FolderTreeInfo) = this.copy(
        folderTreeInfo = folderTreeInfo,
        sizeInBytes = folderTreeInfo.totalCurrentSizeInBytes + folderTreeInfo.sizeOfPreviousVersionsInBytes,
        isAvailableOfflineAvailable = folderTreeInfo.numberOfFiles > 0,
    )

    /**
     * determines if the file history versions should be shown
     */
    val showHistoryVersions = historyVersions > 0

    /**
     * determines if the folder history versions should be shown
     */
    val showFolderHistoryVersions = (folderTreeInfo?.numberOfVersions ?: 0) > 0

    /**
     * to get the uri to use for the preview, it gets [previewUriString] and [thumbnailUriString] as fallback
     */
    val actualPreviewUriString = previewUriString ?: thumbnailUriString

    /**
     * the limited amount of outshares to be shown
     */
    val outSharesCoerceMax by lazy(LazyThreadSafetyMode.NONE) {
        outShares.takeIf { it.size <= MAX_NUMBER_OF_CONTACTS_IN_LIST }
            ?: (outShares.take(MAX_NUMBER_OF_CONTACTS_IN_LIST))
    }

    /**
     * true if this node is an incoming shared root node
     */
    val isIncomingSharedNode = inShareOwnerContactItem != null

    companion object {
        internal const val MAX_NUMBER_OF_CONTACTS_IN_LIST = 5
    }
}
