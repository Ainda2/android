package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

class GetPathByDocumentContentUriUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    suspend operator fun invoke(uri: String): String? {
        return fileSystemRepository.getAbsolutePathByContentUri(uri)
    }
}