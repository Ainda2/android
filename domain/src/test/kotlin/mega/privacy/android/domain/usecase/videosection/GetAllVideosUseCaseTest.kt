package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllVideosUseCaseTest {
    private lateinit var underTest: GetAllVideosUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    val order = SortOrder.ORDER_MODIFICATION_DESC

    @BeforeAll
    fun setUp() {
        underTest = GetAllVideosUseCase(
            videoSectionRepository = videoSectionRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            videoSectionRepository,
            getCloudSortOrder
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that videos is not empty`() = runTest {
        val list = listOf(mock<TypedVideoNode>())
        whenever(
            videoSectionRepository.getAllVideos(
                searchQuery = any(),
                tag = anyOrNull(),
                description = anyOrNull(),
                order = eq(order)
            )
        ).thenReturn(list)
        whenever(getCloudSortOrder()).thenReturn(order)
        assertThat(underTest("", null, null)).isNotEmpty()
    }

    @Test
    fun `test that video is empty`() {
        runTest {
            whenever(
                videoSectionRepository.getAllVideos(
                    searchQuery = any(),
                    tag = anyOrNull(),
                    description = anyOrNull(),
                    order = eq(order)
                )
            ).thenReturn(emptyList())
            whenever(getCloudSortOrder()).thenReturn(order)
            assertThat(underTest("", null, null)).isEmpty()
        }
    }
}