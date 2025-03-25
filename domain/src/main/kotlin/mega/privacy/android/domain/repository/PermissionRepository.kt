package mega.privacy.android.domain.repository

/**
 * Permission repository
 *
 */
interface PermissionRepository {
    /**
     * Has media permission
     */
    fun hasMediaPermission(): Boolean

    /**
     * Has audio permission
     */
    fun hasAudioPermission(): Boolean

    /**
     * Has manage external storage permission
     */
    fun hasManageExternalStoragePermission(): Boolean

    /**
     * Has location permission
     */
    fun isLocationPermissionGranted(): Boolean
}