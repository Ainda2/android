package mega.privacy.android.domain.usecase.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoPlaylistsUseCaseTest {
    private lateinit var underTest: GetVideoPlaylistsUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    private val favouritesVideoPlaylist = mock<FavouritesVideoPlaylist>()

    @BeforeAll
    fun setUp() {
        underTest = GetVideoPlaylistsUseCase(
            getCloudSortOrder = getCloudSortOrder,
            videoSectionRepository = videoSectionRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository, getCloudSortOrder)
    }

    @Test
    fun `test that videoPlaylists is not empty`() = runTest {
        initOrder(SortOrder.ORDER_DEFAULT_ASC)
        val list = listOf(mock<UserVideoPlaylist>())
        whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
        assertThat(underTest()).isNotEmpty()
    }

    @Test
    fun `test that order of returned videoPlaylists is correctly when SortOrder is ORDER_DEFAULT_ASC`() =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "c")
            val playlist2 = initUserVideoPlaylist(title = "b")
            val playlist3 = initUserVideoPlaylist(title = "a")

            initOrder(SortOrder.ORDER_DEFAULT_ASC)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            assertThat((actual[1] as? UserVideoPlaylist)?.title).isEqualTo(playlist3.title)
            assertThat((actual[2] as? UserVideoPlaylist)?.title).isEqualTo(playlist2.title)
            assertThat((actual[3] as? UserVideoPlaylist)?.title).isEqualTo(playlist1.title)
        }

    @Test
    fun `test that order of returned videoPlaylists is based on ORDER_DEFAULT_ASC when SortOrder is ORDER_LABEL_DESC`() =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "c")
            val playlist2 = initUserVideoPlaylist(title = "b")
            val playlist3 = initUserVideoPlaylist(title = "a")

            initOrder(SortOrder.ORDER_LABEL_DESC)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            assertThat((actual[1] as? UserVideoPlaylist)?.title).isEqualTo(playlist3.title)
            assertThat((actual[2] as? UserVideoPlaylist)?.title).isEqualTo(playlist2.title)
            assertThat((actual[3] as? UserVideoPlaylist)?.title).isEqualTo(playlist1.title)
        }

    @Test
    fun `test that order of returned videoPlaylists is correctly when SortOrder is ORDER_MODIFICATION_ASC`() =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "a", creationTime = 3L)
            val playlist2 = initUserVideoPlaylist(title = "b", creationTime = 2L)
            val playlist3 = initUserVideoPlaylist(title = "c", creationTime = 1L)

            initOrder(SortOrder.ORDER_MODIFICATION_ASC)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            assertThat((actual[1] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist3.creationTime)
            assertThat((actual[2] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist2.creationTime)
            assertThat((actual[3] as? UserVideoPlaylist)?.creationTime).isEqualTo(playlist1.creationTime)
        }

    @Test
    fun `test that order of returned videoPlaylists is based on ORDER_DEFAULT_ASC when SortOrder is ORDER_MODIFICATION_ASC and creationTimes are same`() =
        runTest {
            val playlist1 = initUserVideoPlaylist(title = "c", creationTime = 0L)
            val playlist2 = initUserVideoPlaylist(title = "b", creationTime = 0L)
            val playlist3 = initUserVideoPlaylist(title = "a", creationTime = 0L)

            initOrder(SortOrder.ORDER_MODIFICATION_ASC)
            val list = listOf(
                favouritesVideoPlaylist,
                playlist1,
                playlist2,
                playlist3
            )
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(list)
            val actual = underTest()
            assertThat(actual).isNotEmpty()
            assertThat((actual[0] is FavouritesVideoPlaylist)).isTrue()
            assertThat((actual[1] as? UserVideoPlaylist)?.title).isEqualTo(playlist3.title)
            assertThat((actual[2] as? UserVideoPlaylist)?.title).isEqualTo(playlist2.title)
            assertThat((actual[3] as? UserVideoPlaylist)?.title).isEqualTo(playlist1.title)
        }

    @Test
    fun `test that videoPlaylists is empty`() =
        runTest {
            initOrder(SortOrder.ORDER_DEFAULT_ASC)
            whenever(videoSectionRepository.getVideoPlaylists(any())).thenReturn(emptyList())
            assertThat(underTest()).isEmpty()
        }

    private suspend fun initOrder(order: SortOrder) {
        whenever(getCloudSortOrder()).thenReturn(order)
    }

    private fun initUserVideoPlaylist(
        id: Long = 1L,
        title: String = "",
        numberOfVideos: Int = 0,
        creationTime: Long = 0L,
    ) = UserVideoPlaylist(
        id = NodeId(id),
        title = title,
        numberOfVideos = numberOfVideos,
        creationTime = creationTime,
        cover = null,
        modificationTime = 0,
        thumbnailList = null,
        totalDuration = Duration.ZERO,
        videos = null
    )
}