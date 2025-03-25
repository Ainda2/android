package mega.privacy.android.data.gateway

import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Gateway class that provides SD Card-related implementations
 */
interface SDCardGateway {

    /**
     * Retrieves the Directory File from the given SD Card Path
     *
     * @param uriString A URI String of a given SD Card Path
     *
     * @return A potentially nullable Directory File, represented as a [DocumentFile]
     */
    suspend fun getDirectoryFile(uriString: String): DocumentFile?

    /**
     * Retrieves the Folder Directory name in the SD Card
     *
     * @see mega.privacy.android.app.utils.SDCardUtils.getSDCardDirName
     * @param localPath The Folder local path
     *
     * @return The Folder Directory name
     */
    suspend fun getDirectoryName(localPath: String): String

    /**
     * Checks whether a given Folder is inside the SD Card
     *
     * @see mega.privacy.android.app.utils.SDCardUtils.isLocalFolderOnSDCard
     * @param localPath The Folder local path
     *
     * @return true if the Folder exists, and false if otherwise
     */
    suspend fun doesFolderExists(localPath: String): Boolean

    /**
     * @return true if the [localPath] points to a SD card cache
     */
    suspend fun isSDCardCachePath(localPath: String): Boolean

    /**
     * Retrieves the Root SD Card path
     *
     * @see mega.privacy.android.app.utils.SDCardUtils.getSDCardRoot
     * @param path the File path
     *
     * @return the Root SD Card path
     */
    suspend fun getRootSDCardPath(path: String): String

    /**
     * Get a folder in cache in external sd card folder and returns the file
     * Create the folder if it does not exist
     *
     * @param folderName
     * @return the File corresponding to the folder in cache
     *         Return null if the folder cannot be created
     */
    suspend fun getOrCreateCacheFolder(folderName: String): File?

    /**
     * Check if the given [uriString] is a SD card URI
     */
    suspend fun isSDCardUri(uriString: String): Boolean
}