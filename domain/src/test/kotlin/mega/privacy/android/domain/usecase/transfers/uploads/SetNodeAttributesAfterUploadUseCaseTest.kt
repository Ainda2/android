package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsPdfFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfThumbnailUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetNodeAttributesAfterUploadUseCaseTest {

    private lateinit var underTest: SetNodeAttributesAfterUploadUseCase

    private val createImageOrVideoThumbnailUseCase: CreateImageOrVideoThumbnailUseCase = mock()
    private val createImageOrVideoPreviewUseCase: CreateImageOrVideoPreviewUseCase = mock()
    private val setNodeCoordinatesUseCase: SetNodeCoordinatesUseCase = mock()
    private val isVideoFileUseCase: IsVideoFileUseCase = mock()
    private val isImageFileUseCase: IsImageFileUseCase = mock()
    private val isPdfFileUseCase: IsPdfFileUseCase = mock()
    private val createPdfThumbnailUseCase: CreatePdfThumbnailUseCase = mock()
    private val createPdfPreviewUseCase: CreatePdfPreviewUseCase = mock()

    private val fileName = "testName"
    private val nodeHandle = 1L
    private val localPath = "test/local/$fileName"
    private val localFile =File(localPath)
    private val uriPath = UriPath(localPath)
    private val thumbnailCache = "test/thumbnail/cache"
    private val previewCache = "test/preview/cache"
    private val thumbnailPath = "$thumbnailCache/$fileName"
    private val previewPath = "$previewCache/$fileName"

    @BeforeAll
    fun setUp() {
        underTest = SetNodeAttributesAfterUploadUseCase(
            createImageOrVideoThumbnailUseCase = createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase = createImageOrVideoPreviewUseCase,
            setNodeCoordinatesUseCase = setNodeCoordinatesUseCase,
            isVideoFileUseCase = isVideoFileUseCase,
            isImageFileUseCase = isImageFileUseCase,
            isPdfFileUseCase = isPdfFileUseCase,
            createPdfThumbnailUseCase = createPdfThumbnailUseCase,
            createPdfPreviewUseCase = createPdfPreviewUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase,
            setNodeCoordinatesUseCase,
            isVideoFileUseCase,
            isImageFileUseCase,
            isPdfFileUseCase,
            createPdfThumbnailUseCase,
            createPdfPreviewUseCase
        )
    }

    @ParameterizedTest(
        name = " is image = {0}, is video = {1} and is pdf = {2}"
    )
    @MethodSource("provideParameters")
    fun `test that SetNodeCoordinatesUseCase behaves correctly if`(
        isVideoFile: Boolean,
        isImageFile: Boolean,
        isPdfFile: Boolean,
    ) = runTest {
        whenever(isVideoFileUseCase(uriPath)).thenReturn(isVideoFile)
        whenever(isImageFileUseCase(uriPath)).thenReturn(isImageFile)
        whenever(isPdfFileUseCase(uriPath)).thenReturn(isPdfFile)
        whenever(createImageOrVideoThumbnailUseCase(nodeHandle, localFile)).thenReturn(Unit)
        whenever(createPdfThumbnailUseCase(nodeHandle, uriPath)).thenReturn(Unit)
        whenever(createImageOrVideoPreviewUseCase(nodeHandle, localFile)).thenReturn(Unit)
        whenever(createPdfPreviewUseCase(nodeHandle, uriPath)).thenReturn(Unit)
        whenever(setNodeCoordinatesUseCase(uriPath, nodeHandle)).thenReturn(Unit)

        underTest.invoke(nodeHandle, uriPath, null)

        when {
            isVideoFile || isImageFile -> {
                verify(createImageOrVideoThumbnailUseCase).invoke(nodeHandle, localFile)
                verify(createPdfThumbnailUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createImageOrVideoPreviewUseCase).invoke(nodeHandle, localFile)
                verify(createPdfPreviewUseCase, never()).invoke(nodeHandle, uriPath)
                verify(setNodeCoordinatesUseCase).invoke(uriPath, nodeHandle)
            }

            isPdfFile -> {
                verify(createImageOrVideoThumbnailUseCase, never()).invoke(nodeHandle, localFile)
                verify(createPdfThumbnailUseCase).invoke(nodeHandle, uriPath)
                verify(createImageOrVideoPreviewUseCase, never()).invoke(nodeHandle, localFile)
                verify(createPdfPreviewUseCase).invoke(nodeHandle, uriPath)
                verify(setNodeCoordinatesUseCase, never()).invoke(uriPath, nodeHandle)
            }

            else -> {
                verify(createImageOrVideoThumbnailUseCase, never()).invoke(nodeHandle, localFile)
                verify(createPdfThumbnailUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createImageOrVideoPreviewUseCase, never()).invoke(nodeHandle, localFile)
                verify(createPdfPreviewUseCase, never()).invoke(nodeHandle, uriPath)
                verify(setNodeCoordinatesUseCase, never()).invoke(uriPath, nodeHandle)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that geolocation appdata parameter is set when is an image or video file and app data contains it`(
        isVideoFile: Boolean
    ) = runTest{
        whenever(isVideoFileUseCase(uriPath)).thenReturn(isVideoFile)
        whenever(isImageFileUseCase(uriPath)).thenReturn(!isVideoFile)
        whenever(isPdfFileUseCase(uriPath)).thenReturn(false)
        val geolocation = TransferAppData.Geolocation(34.354, 45.435)
        val appData = listOf(geolocation)
        underTest.invoke(nodeHandle, uriPath, appData = appData)

        verify(setNodeCoordinatesUseCase).invoke(uriPath, nodeHandle, geolocation)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false, false),
        Arguments.of(true, false, false),
        Arguments.of(false, true, false),
        Arguments.of(false, false, true),
    )
}