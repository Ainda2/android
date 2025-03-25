package mega.privacy.android.data.wrapper

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Wrapper class for static functions concerning [androidx.documentfile.provider.DocumentFile]
 */
interface DocumentFileWrapper {

    /**
     * If the given DocumentFile represents a Document tree.
     */
    val DocumentFile.isTreeDocumentFile: Boolean

    /**
     * If the given DocumentFile represents a file in local storage.
     */
    val DocumentFile.isExternalStorageDocument: Boolean

    /**
     * If the given DocumentFile represents a file in local storage, Downloads folder.
     */
    val DocumentFile.isDownloadsDocument: Boolean

    /**
     * If the given DocumentFile represents a media file.
     */
    val DocumentFile.isMediaDocument: Boolean

    /**
     * If the given DocumentFile represents a file in local storage.
     */
    val DocumentFile.isInPrimaryStorage: Boolean

    /**
     * If the given DocumentFile represents a file in SD card storage.
     */
    val DocumentFile.isInSdCardStorage: Boolean

    /**
     * If the given DocumentFile represents a file created with [File]
     */
    val DocumentFile.isRawFile: Boolean

    /**
     * DocumentFile id
     */
    val DocumentFile.id: String

    /**
     * DocumentFile storage id
     */
    val DocumentFile.storageId: String

    /**
     * Creates a [DocumentFile] representing the document tree rooted at the given [uri]
     *
     * @see androidx.documentfile.provider.DocumentFile.fromTreeUri
     * @param uri the tree URI
     *
     * @return A potentially nullable [DocumentFile]
     */
    fun fromTreeUri(uri: Uri): DocumentFile?

    /**
     * Creates a [DocumentFile] representing the document at the given [uri]
     */
    fun fromSingleUri(uri: Uri): DocumentFile?

    /**
     * Gets the [DocumentFile] id.
     *
     * @see android.provider.DocumentsContract.getDocumentId
     * @param documentFile the [DocumentFile] to get the id from
     * @return the id of the [DocumentFile]
     */
    fun getDocumentId(documentFile: DocumentFile): String

    /**
     * Creates a [DocumentFile] representing the document at the given [uri]
     *
     * @see androidx.documentfile.provider.DocumentFile.fromUri
     * @param uri the URI
     *
     * @return A potentially nullable [DocumentFile]
     */
    fun fromUri(uri: Uri): DocumentFile?

    /**
     * Gets the absolute path of the given [uri]
     */
    fun getAbsolutePathFromContentUri(uri: Uri): String?

    /**
     * Creates a [DocumentFile] representing the given [file]
     */
    fun fromFile(file: File): DocumentFile

    /**
     * Creates a [DocumentFile] representing the document tree rooted at the given [uri]
     */
    fun getSdDocumentFile(
        folderUri: Uri,
        subFolders: List<String>,
        fileName: String,
        mimeType: String,
    ): DocumentFile?
}