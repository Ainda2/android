package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsExternalStorageContentUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileDestinationAndAppDataForDownloadUseCaseTest {
    private lateinit var underTest: GetFileDestinationAndAppDataForDownloadUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transferRepository = mock<TransferRepository>()
    private val isExternalStorageContentUriUseCase = mock<IsExternalStorageContentUriUseCase>()
    private val getExternalPathByContentUriUseCase = mock<GetExternalPathByContentUriUseCase>()
    private val cacheRepository = mock<CacheRepository>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetFileDestinationAndAppDataForDownloadUseCase(
            fileSystemRepository,
            transferRepository,
            cacheRepository,
            isExternalStorageContentUriUseCase,
            getExternalPathByContentUriUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(
            fileSystemRepository,
            transferRepository,
            cacheRepository,
            isExternalStorageContentUriUseCase,
            getExternalPathByContentUriUseCase,
            getFeatureFlagValueUseCase,
        )
        whenever(fileSystemRepository.isSDCardPathOrUri(any())) doReturn false
        whenever(fileSystemRepository.isContentUri(any())) doReturn false
        whenever(isExternalStorageContentUriUseCase(any())) doReturn false
        whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination)) doReturn false
    }

    @Test
    fun `test that same destination and null app data is returned when the destination is not in external storage or content uri`() =
        runTest {
            val expectedUri = UriPath(PATH_STRING)

            val (actualUri, actualAppData) = underTest(expectedUri)

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that external cache folder and SdCardDownload app data is returned when the destination is a sd card path  and AllowToChooseDownloadDestination is disabled`() =
        runTest {
            val cachePath = "Cache"
            val cacheFolder = File(cachePath)
            val expectedUri = UriPath(cachePath)
            val expectedAppData = TransferAppData.SdCardDownload(
                targetPathForSDK = cachePath,
                finalTargetUri = PATH_STRING
            )

            whenever(fileSystemRepository.isSDCardPathOrUri(PATH_STRING)) doReturn true
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination)) doReturn false
            whenever(transferRepository.getOrCreateSDCardTransfersCacheFolder()) doReturn cacheFolder

            val (actualUri, actualAppData) = underTest(UriPath(PATH_STRING))

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isEqualTo(expectedAppData) },
            )
        }

    @Test
    fun `test that external cache folder and SdCardDownload app data is NOT returned when the destination is a sd card path and AllowToChooseDownloadDestination is enabled`() =
        runTest {
            val expectedUri = UriPath(PATH_STRING)

            whenever(fileSystemRepository.isSDCardPathOrUri(PATH_STRING)) doReturn true
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination)) doReturn true

            val (actualUri, actualAppData) = underTest(UriPath(PATH_STRING))

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that cache folder and SdCardDownload app data is returned when the destination is a content uri path`() =
        runTest {
            val cachePath = "Cache"
            val cacheFolder = File(cachePath)
            whenever(fileSystemRepository.isContentUri(PATH_STRING)) doReturn true
            whenever(cacheRepository.getCacheFolderNameForTransfer(false)) doReturn "temp"
            whenever(cacheRepository.getCacheFolder(any())) doReturn cacheFolder
            val expectedUri = UriPath(cachePath)
            val expectedAppData = TransferAppData.SdCardDownload(
                targetPathForSDK = cachePath,
                finalTargetUri = PATH_STRING
            )

            val (actualUri, actualAppData) = underTest(UriPath(PATH_STRING))

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isEqualTo(expectedAppData) },
            )
        }

    @Test
    fun `test that getExternalPathByContentUriUseCase folder and null app data is returned when the destination is an external content uri path`() =
        runTest {
            val externalPath = "Cache"
            whenever(isExternalStorageContentUriUseCase(PATH_STRING)) doReturn true
            whenever(getExternalPathByContentUriUseCase(PATH_STRING)) doReturn externalPath
            val expectedUri = UriPath(externalPath)

            val (actualUri, actualAppData) = underTest(UriPath(PATH_STRING))

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that original uri and null app data is returned when the destination is an external content uri path and feature flag is true`() =
        runTest {
            whenever(isExternalStorageContentUriUseCase(PATH_STRING)) doReturn true
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination)) doReturn true
            val expectedUri = UriPath(PATH_STRING)

            val (actualUri, actualAppData) = underTest(expectedUri)

            assertAll(
                { assertThat(actualUri).isEqualTo(expectedUri) },
                { assertThat(actualAppData).isNull() },
            )
        }

    @Test
    fun `test that a NullFileException is thrown when the destination is a content uri path and getOrCreateSDCardTransfersCacheFolder returns null`() =
        runTest {
            whenever(fileSystemRepository.isContentUri(PATH_STRING)) doReturn true
            whenever(transferRepository.getOrCreateSDCardTransfersCacheFolder()) doReturn null

            assertThrows<NullFileException> {
                underTest(UriPath(PATH_STRING))
            }
        }

    companion object {
        private const val PATH_STRING = "uriPath/"
    }
}