package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateMediaUploadsBackupUseCase
import javax.inject.Inject

/**
 * UseCase that  Setup Media Upload Sync Handle
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property setupOrUpdateMediaUploadsBackupUseCase [SetupOrUpdateMediaUploadsBackupUseCase]
 */
class SetupMediaUploadsSyncHandleUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase,
) {

    /**
     * Invocation function
     *
     * @param handle [Long]
     */
    suspend operator fun invoke(handle: Long) {
        cameraUploadsRepository.setSecondarySyncHandle(handle)
        setupOrUpdateMediaUploadsBackupUseCase(targetNode = handle, localFolder = null)
    }
}
