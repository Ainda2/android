package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsPdfFileUseCaseTest {

    private lateinit var underTest: IsPdfFileUseCase

    private lateinit var fileSystemRepository: FileSystemRepository

    @BeforeAll
    fun setup() {
        fileSystemRepository = mock()
        underTest = IsPdfFileUseCase(fileSystemRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest(name = "when local path is {0} and content type is {1} the result is {2}")
    @MethodSource("provideParameters")
    fun `test that IsPdfFileUseCase returns correctly`(
        uriPathString: String,
        contentTypeFromPath: String?,
        contentTypeFromUri: String?,
        expectedResult: Boolean,
    ) = runTest {
        val uriPath = UriPath(uriPathString)
        whenever(fileSystemRepository.getGuessContentTypeFromName(uriPathString))
            .thenReturn(contentTypeFromPath)
        whenever(fileSystemRepository.getContentTypeFromContentUri(uriPath))
            .thenReturn(contentTypeFromUri)
        Truth.assertThat(underTest.invoke(uriPath)).isEqualTo(expectedResult)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of("/", null, null, false),
        Arguments.of("/any/video.mp4", "video", null, false),
        Arguments.of("/any/image.png", "image", null, false),
        Arguments.of("/any/file.pdf", "application/pdf", null, true),
        Arguments.of("content://any/image.png", null, "image", false),
        Arguments.of("content://any/file.pdf", null, "application/pdf", true),
        Arguments.of("content://any/image.png", null, "image", false),
        Arguments.of("content://any/file.pdf", null, "application/pdf", true),
    )
}