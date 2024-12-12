package mega.privacy.android.domain.entity.transfer

/**
 * Data to identify different types of transfers within the app
 */
sealed interface TransferAppData {
    /**
     * Identify a camera upload transfer
     */
    data object CameraUpload : TransferAppData

    /**
     * Common interface for chat transfers app data
     */
    sealed interface ChatUploadAppData : TransferAppData

    /**
     * Identify a voice clip transfer
     */
    data object VoiceClip : ChatUploadAppData

    /**
     * Identify a chat transfer and its message
     * @param pendingMessageId the chat message Id related to this transfer
     */
    data class ChatUpload(val pendingMessageId: Long) : ChatUploadAppData


    /**
     * Indicates the transfer should be transparent for the user and should not show any notification
     */
    data object BackgroundTransfer : TransferAppData

    /**
     * Identify a download transfers that needs to be stored in the SD card.
     * @param targetPathForSDK the path where the transfers will be stored by the SDK
     * @param finalTargetUri the target uri in the sd card where the file will be moved once downloaded
     */
    data class SdCardDownload(val targetPathForSDK: String, val finalTargetUri: String) :
        TransferAppData

    /**
     * Identify the original content URI of an upload transfer that needs to be copied to the cache folder for retry purposes.
     * @param originalUri
     */
    data class OriginalContentUri(val originalUri: String) : TransferAppData

    /**
     * Identify a chat download transfer.
     *
     * @param chatId the chat Id related to this transfer
     * @param msgId the message Id related to this transfer
     * @param msgIndex the index of the node in the chat message
     */
    data class ChatDownload(val chatId: Long, val msgId: Long, val msgIndex: Int) :
        TransferAppData
}