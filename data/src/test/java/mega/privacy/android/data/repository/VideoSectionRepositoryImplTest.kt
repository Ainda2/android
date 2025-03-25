package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.FavouritesVideoPlaylistMapper
import mega.privacy.android.data.mapper.videosection.UserVideoPlaylistMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedItemMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoSectionRepositoryImplTest {
    private lateinit var underTest: VideoSectionRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val fileNodeMapper = mock<FileNodeMapper>()
    private val typedVideoNodeMapper = mock<TypedVideoNodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val userSetMapper: UserSetMapper = ::createUserSet
    private val userVideoPlaylistMapper = mock<UserVideoPlaylistMapper>()
    private val megaSearchFilterMapper = mock<MegaSearchFilterMapper>()
    private val appPreferencesGateway = mock<AppPreferencesGateway>()
    private val videoRecentlyWatchedItemMapper = mock<VideoRecentlyWatchedItemMapper>()
    private val favouritesVideoPlaylistMapper = mock<FavouritesVideoPlaylistMapper>()

    @BeforeAll
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        wheneverBlocking { megaLocalRoomGateway.getAllOfflineInfo() }.thenReturn(emptyList())
        underTest = VideoSectionRepositoryImpl(
            megaApiGateway = megaApiGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            fileNodeMapper = fileNodeMapper,
            typedVideoNodeMapper = typedVideoNodeMapper,
            cancelTokenProvider = cancelTokenProvider,
            megaLocalRoomGateway = megaLocalRoomGateway,
            userSetMapper = userSetMapper,
            userVideoPlaylistMapper = userVideoPlaylistMapper,
            megaSearchFilterMapper = megaSearchFilterMapper,
            appPreferencesGateway = appPreferencesGateway,
            videoRecentlyWatchedItemMapper = videoRecentlyWatchedItemMapper,
            favouritesVideoPlaylistMapper = favouritesVideoPlaylistMapper,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            sortOrderIntMapper,
            fileNodeMapper,
            typedVideoNodeMapper,
            megaLocalRoomGateway,
            userVideoPlaylistMapper,
            appPreferencesGateway,
            videoRecentlyWatchedItemMapper,
            favouritesVideoPlaylistMapper
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that get all videos returns successfully`() = runTest {
        val node = mock<MegaNode> {
            on { isFile }.thenReturn(true)
            on { isFolder }.thenReturn(false)
            on { duration }.thenReturn(100)
        }
        val backupNode = mock<MegaNode> {
            on { isFile }.thenReturn(true)
            on { isFolder }.thenReturn(false)
            on { duration }.thenReturn(100)
        }
        val fileNode = mock<FileNode>()
        val filter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()
        val typedVideoNode = mock<TypedVideoNode> {
            on { thumbnailPath }.thenReturn(null)
        }
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC))
            .thenReturn(ORDER_DEFAULT_DESC)
        whenever(
            megaSearchFilterMapper(
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.VIDEO
            )
        ).thenReturn(filter)
        whenever(
            megaApiGateway.searchWithFilter(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC),
                token
            )
        ).thenReturn(listOf(node, backupNode))
        whenever(megaLocalRoomGateway.getAllOfflineInfo()).thenReturn(null)
        whenever(
            fileNodeMapper(
                megaNode = node,
                requireSerializedData = false,
                offline = null
            )
        ).thenReturn(fileNode)
        whenever(megaApiGateway.isInBackups(backupNode)).thenReturn(true)
        whenever(megaApiGateway.isInBackups(node)).thenReturn(false)
        whenever(typedVideoNodeMapper(fileNode, node.duration, null)).thenReturn(typedVideoNode)
        initUnderTest()
        val actual = underTest.getAllVideos("", null, null, SortOrder.ORDER_MODIFICATION_DESC)
        assertThat(actual).isNotEmpty()
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0]).isEqualTo(typedVideoNode)
    }

    @Test
    fun `test that get video playlists returns correctly when all set types are SET_TYPE_PLAYLIST`() =
        runTest {
            val megaSet1 = createMegaSet(1L)
            val megaSet2 = createMegaSet(2L)

            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                on { get(0) }.thenReturn(megaSet1)
                on { get(1) }.thenReturn(megaSet2)
            }

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }

            initReturnValues(megaSetList, megaSetElementList)
            initEmptyFavouritesVideoPlaylist()
            initUnderTest()
            val actual = underTest.getVideoPlaylists()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(3)
            assertThat(actual[0] is FavouritesVideoPlaylist).isTrue()
        }

    private suspend fun initEmptyFavouritesVideoPlaylist() {
        val filter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(
            megaSearchFilterMapper(
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.VIDEO
            )
        ).thenReturn(filter)
        whenever(
            megaApiGateway.searchWithFilter(filter, sortOrderIntMapper(SortOrder.ORDER_NONE), token)
        ).thenReturn(emptyList())
        whenever(favouritesVideoPlaylistMapper(anyOrNull())).thenReturn(mock())
    }

    @Test
    fun `test that get video playlists returns correctly when all set types are not SET_TYPE_PLAYLIST`() =
        runTest {
            val megaSet1 = createMegaSet(1L, MegaSet.SET_TYPE_ALBUM)
            val megaSet2 = createMegaSet(2L, MegaSet.SET_TYPE_ALBUM)

            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                on { get(0) }.thenReturn(megaSet1)
                on { get(1) }.thenReturn(megaSet2)
            }

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }

            initReturnValues(megaSetList, megaSetElementList)
            initEmptyFavouritesVideoPlaylist()
            initUnderTest()
            val actual = underTest.getVideoPlaylists()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual[0] is FavouritesVideoPlaylist).isTrue()
        }

    @Test
    fun `test that get video playlists returns correctly when all set types are not all SET_TYPE_PLAYLIST`() =
        runTest {
            val megaSet1 = createMegaSet(1L)
            val megaSet2 = createMegaSet(2L, MegaSet.SET_TYPE_ALBUM)

            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                on { get(0) }.thenReturn(megaSet1)
                on { get(1) }.thenReturn(megaSet2)
            }

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }

            initReturnValues(megaSetList, megaSetElementList)
            initEmptyFavouritesVideoPlaylist()
            initUnderTest()
            val actual = underTest.getVideoPlaylists()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(2)
            assertThat(actual[0] is FavouritesVideoPlaylist).isTrue()
        }

    private suspend fun initReturnValues(
        megaSetList: MegaSetList,
        megaSetElementList: MegaSetElementList,
    ) {
        val megaNode = mock<MegaNode> {
            on { duration }.thenReturn(100)
            on { isOutShare }.thenReturn(false)
        }
        whenever(megaApiGateway.getSets()).thenReturn(megaSetList)
        whenever(megaApiGateway.getSetElements(any())).thenReturn(megaSetElementList)
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
    }

    @Test
    fun `test that items of the video playlists returns correctly`() =
        runTest {
            val userSet = getUserSetAndInitReturnValues()

            val typedVideoNode = mock<TypedVideoNode> {
                on { thumbnailPath }.thenReturn(null)
                on { duration.inWholeSeconds }.thenReturn(100L)
                on { isOutShared }.thenReturn(false)
                on { watchedTimestamp }.thenReturn(0L)
                on { isFavourite }.thenReturn(false)
            }

            val testVideos: List<TypedVideoNode> =
                listOf(typedVideoNode, typedVideoNode, typedVideoNode)

            val testVideoPlaylist = mock<UserVideoPlaylist> {
                on { id }.thenReturn(NodeId(1L))
                on { title }.thenReturn("video playlist title")
                on { videos }.thenReturn(listOf(mock(), mock(), mock()))
            }

            whenever(
                typedVideoNodeMapper(
                    anyOrNull(),
                    any(),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            ).thenReturn(
                typedVideoNode
            )
            whenever(megaApiGateway.isInRubbish(any())).thenReturn(false)
            whenever(userVideoPlaylistMapper(userSet, testVideos)).thenReturn(testVideoPlaylist)
            initEmptyFavouritesVideoPlaylist()
            initUnderTest()
            val actual = underTest.getVideoPlaylists()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(2)
            assertThat(actual[1].videos?.size).isEqualTo(3)
        }

    private suspend fun getUserSetAndInitReturnValues(): UserSet {
        val megaSet = createMegaSet(1L)

        val megaSetList = mock<MegaSetList> {
            on { size() }.thenReturn(1L)
            on { get(0) }.thenReturn(megaSet)
        }

        val megaSetElement = mock<MegaSetElement> {
            on { node() }.thenReturn(1L)
        }

        val megaSetElementList = mock<MegaSetElementList> {
            on { size() }.thenReturn(3L)
            on { get(0) }.thenReturn(megaSetElement)
            on { get(1) }.thenReturn(megaSetElement)
            on { get(2) }.thenReturn(megaSetElement)
        }

        initReturnValues(megaSetList, megaSetElementList)

        return createUserSet(
            1L,
            "MegaSet",
            MegaSet.SET_TYPE_PLAYLIST,
            null,
            2L,
            3L,
            false
        )
    }

    @Test
    fun `test that items of the video playlists returns correctly when all videos are in rubbish bin`() =
        runTest {
            val userSet = getUserSetAndInitReturnValues()

            val testVideoPlaylist = mock<UserVideoPlaylist> {
                on { id }.thenReturn(NodeId(1L))
                on { title }.thenReturn("video playlist title")
            }

            whenever(megaApiGateway.isInRubbish(any())).thenReturn(true)
            whenever(userVideoPlaylistMapper(userSet, emptyList())).thenReturn(testVideoPlaylist)
            whenever(favouritesVideoPlaylistMapper(anyOrNull())).thenReturn(mock())
            initEmptyFavouritesVideoPlaylist()
            initUnderTest()
            val actual = underTest.getVideoPlaylists()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(2)
            assertThat(actual[1]).isEqualTo(testVideoPlaylist)
        }

    @Test
    fun `test that the created video playlist has the correct title`() = runTest {
        val api = mock<MegaApiJava>()

        val testMegaSet = mock<MegaSet> {
            on { id() }.thenReturn(1L)
            on { name() }.thenReturn("video playlist title")
        }

        val userSet = createUserSet(
            testMegaSet.id(),
            testMegaSet.name(),
            MegaSet.SET_TYPE_PLAYLIST,
            null,
            testMegaSet.cts(),
            testMegaSet.ts(),
            false,
        )

        val expectedVideoPlaylist = mock<UserVideoPlaylist> {
            on { id }.thenReturn(NodeId(userSet.id))
            on { title }.thenReturn(userSet.name)
        }

        val request = mock<MegaRequest> {
            on { megaSet }.thenReturn(testMegaSet)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.createSet(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }
        whenever(userVideoPlaylistMapper(any(), any())).thenReturn(expectedVideoPlaylist)

        val actual = underTest.createVideoPlaylist(userSet.name)
        assertThat(actual is UserVideoPlaylist).isTrue()
        assertThat((actual as UserVideoPlaylist).id.longValue).isEqualTo(userSet.id)
        assertThat(actual.title).isEqualTo(userSet.name)
    }

    private fun createMegaSet(id: Long, type: Int = MegaSet.SET_TYPE_PLAYLIST) = mock<MegaSet> {
        on { id() }.thenReturn(id)
        on { name() }.thenReturn("MegaSet")
        on { type() }.thenReturn(type)
        on { cover() }.thenReturn(-1L)
        on { cts() }.thenReturn(2L)
        on { ts() }.thenReturn(3L)
        on { isExported }.thenReturn(false)
    }

    private fun createUserSet(
        id: Long,
        name: String,
        type: Int,
        cover: Long?,
        creationTime: Long,
        modificationTime: Long,
        isExported: Boolean,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val type: Int = type

        override val cover: Long? = cover

        override val creationTime: Long = creationTime

        override val modificationTime: Long = modificationTime

        override val isExported: Boolean = isExported

        override fun equals(other: Any?): Boolean {
            val otherSet = other as? UserSet ?: return false
            return id == otherSet.id
                    && name == otherSet.name
                    && cover == otherSet.cover
                    && modificationTime == otherSet.modificationTime
                    && isExported == otherSet.isExported
        }
    }

    @Test
    fun `test that addVideosToPlaylist returns correctly`() =
        runTest {
            val testPlaylistId = NodeId(1L)
            val testVideoIDs = listOf(NodeId(1L), NodeId(2L))

            whenever(megaApiGateway.createSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_OK)
                    }
                )
            }

            initUnderTest()
            val actual =
                underTest.addVideosToPlaylist(playlistID = testPlaylistId, videoIDs = testVideoIDs)
            assertThat(actual).isEqualTo(testVideoIDs.size)
        }

    @Test
    fun `test that addVideosToPlaylist returns 0 when createSetElement returns a MegaError`() =
        runTest {
            val testPlaylistId = NodeId(1L)
            val testVideoIDs = listOf(NodeId(1L), NodeId(2L))

            whenever(megaApiGateway.createSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_EBLOCKED)
                    }
                )
            }

            initUnderTest()
            val actual =
                underTest.addVideosToPlaylist(playlistID = testPlaylistId, videoIDs = testVideoIDs)
            assertThat(actual).isEqualTo(0)
        }

    @Test
    fun `test that removeVideosFromPlaylist returns correctly`() =
        runTest {
            val testPlaylistId = NodeId(1L)
            val testVideoElementIDs = listOf(1L, 2L)

            whenever(megaApiGateway.removeSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_OK)
                    }
                )
            }

            initUnderTest()
            val actual =
                underTest.removeVideosFromPlaylist(
                    playlistID = testPlaylistId,
                    videoElementIDs = testVideoElementIDs
                )
            assertThat(actual).isEqualTo(2)
        }

    @Test
    fun `test that removeVideosFromPlaylist returns 0 when removeSetElement returns a MegaError`() =
        runTest {
            val testPlaylistId = NodeId(1L)
            val testVideoIDs = listOf(1L, 2L)

            whenever(megaApiGateway.removeSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_EBLOCKED)
                    }
                )
            }

            initUnderTest()
            val actual =
                underTest.removeVideosFromPlaylist(
                    playlistID = testPlaylistId,
                    videoElementIDs = testVideoIDs
                )
            assertThat(actual).isEqualTo(0)
        }

    @Test
    fun `test that removing video playlists returns the removed ids`() = runTest {
        val videoPlaylistIDs = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )

        val megaRequestError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        val megaRequest = mock<MegaRequest> {
            on { parentHandle }.thenReturn(1L)
        }

        whenever(megaApiGateway.removeSet(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaRequestError,
            )
        }

        initUnderTest()

        val actual = underTest.removeVideoPlaylists(videoPlaylistIDs)
        assertThat(actual.size).isEqualTo(videoPlaylistIDs.size)
        actual.map {
            assertThat(it).isEqualTo(1L)
        }
    }

    @Test
    fun `test that updating the video playlist title returns the new title`() = runTest {
        val newTitle = "new title"

        val megaRequestError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }
        val megaRequest = mock<MegaRequest> {
            on { text }.thenReturn(newTitle)
        }

        whenever(megaApiGateway.updateSetName(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaRequestError,
            )
        }

        initUnderTest()

        val actual = underTest.updateVideoPlaylistTitle(NodeId(1L), newTitle)
        assertThat(actual).isEqualTo(newTitle)
    }

    @Test
    fun `test that monitorVideoPlaylistSetsUpdate emits correct result when OnSetsUpdate is emitted`() =
        runTest {
            val expectedUserSets = (1..3L).map {
                createUserSet(
                    id = it,
                    name = "Playlist $it",
                    type = MegaSet.SET_TYPE_PLAYLIST,
                    cover = -1L,
                    creationTime = it,
                    modificationTime = it,
                    isExported = false,
                )
            }

            val megaSets = expectedUserSets.map { set ->
                mock<MegaSet> {
                    on { id() }.thenReturn(set.id)
                    on { name() }.thenReturn(set.name)
                    on { ts() }.thenReturn(set.modificationTime)
                    on { type() }.thenReturn(MegaSet.SET_TYPE_PLAYLIST)
                }
            }

            whenever(megaApiGateway.globalUpdates)
                .thenReturn(flowOf(GlobalUpdate.OnSetsUpdate(ArrayList(megaSets))))

            initUnderTest()
            underTest.monitorSetsUpdates().test {
                val actual = awaitItem()
                assertThat(actual).isNotEmpty()
                assertThat(actual[0]).isEqualTo(1L)
                assertThat(actual[1]).isEqualTo(2L)
                assertThat(actual[2]).isEqualTo(3L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorVideoPlaylistSetsUpdate emits correct result when OnSetElementsUpdate is emitted`() =
        runTest {
            val testElement = mock<MegaSetElement> {
                on { setId() }.thenReturn(1L)
            }
            getUserSetAndInitReturnValues()
            underTest.getVideoPlaylistSets()
            whenever(megaApiGateway.globalUpdates)
                .thenReturn(flowOf(GlobalUpdate.OnSetElementsUpdate(arrayListOf(testElement))))
            underTest.monitorSetsUpdates().test {
                val actual = awaitItem()
                assertThat(actual).isNotEmpty()
                assertThat(actual[0]).isEqualTo(1L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that getVideoPlaylistSets function returns the correct result`() = runTest {
        val userSet = getUserSetAndInitReturnValues()

        val actual = underTest.getVideoPlaylistSets()
        assertThat(actual).isNotEmpty()
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0]).isEqualTo(userSet)
    }

    @Test
    fun `test that add video to multiple playlists returns the ids of added video playlists which added the video`() =
        runTest {
            val videoPlaylistIDs = listOf(1L, 2L, 3L)

            val megaRequestError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApiGateway.createSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaRequestError,
                )
            }

            initUnderTest()

            val actual = underTest.addVideoToMultiplePlaylists(videoPlaylistIDs, 1L)
            assertThat(actual.size).isEqualTo(videoPlaylistIDs.size)
            actual.forEachIndexed { index, handle ->
                assertThat(handle).isEqualTo(videoPlaylistIDs[index])
            }
        }

    @Test
    fun `test that gets video playlists returns correctly with Favourites video playlist has the correct items`() =
        runTest {
            val megaSets = (1..2L).map {
                createMegaSet(it)
            }
            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                megaSets.mapIndexed { index, set ->
                    on { get(index.toLong()) }.thenReturn(set)
                }
            }
            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }
            initReturnValues(megaSetList, megaSetElementList)

            val megaNodes = (1..3L).map {
                initMegaNode(it, it != 2L)
            }
            val fileNodes = megaNodes.map {
                mock<TypedFileNode>()
            }
            val typedVideoNodes = megaNodes.map {
                initTypedVideoNode(it.handle, 100, null, it.handle != 2L)
            }
            val expectedVideos = typedVideoNodes.filter { it.isFavourite }
            val favouritesVideoPlaylist = mock<FavouritesVideoPlaylist> {
                on { videos }.thenReturn(expectedVideos)
            }
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(megaApiGateway.isInBackups(any())).thenReturn(false)
            whenever(sortOrderIntMapper(SortOrder.ORDER_NONE)).thenReturn(ORDER_DEFAULT_DESC)
            whenever(
                megaSearchFilterMapper(
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.VIDEO
                )
            ).thenReturn(filter)
            whenever(
                megaApiGateway.searchWithFilter(
                    filter,
                    sortOrderIntMapper(SortOrder.ORDER_NONE),
                    token
                )
            ).thenReturn(megaNodes)
            whenever(megaLocalRoomGateway.getAllOfflineInfo()).thenReturn(emptyList())
            megaNodes.mapIndexed { index, node ->
                whenever(
                    fileNodeMapper(megaNode = node, requireSerializedData = false, offline = null)
                ).thenReturn(fileNodes[index])
                whenever(typedVideoNodeMapper(fileNodes[index], node.duration, null))
                    .thenReturn(typedVideoNodes[index])
            }
            whenever(favouritesVideoPlaylistMapper(expectedVideos)).thenReturn(
                favouritesVideoPlaylist
            )

            initUnderTest()
            val actual = underTest.getVideoPlaylists()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(3)
            actual[0].let { playlist ->
                assertThat(playlist is FavouritesVideoPlaylist).isTrue()
                assertThat(playlist.videos?.size).isEqualTo(2)
                playlist.videos?.let {
                    assertThat(it[0].id.longValue).isEqualTo(1L)
                    assertThat(it[1].id.longValue).isEqualTo(3L)
                }
            }
        }

    @Test
    fun `test that saveVideoRecentlyWatched function is invoked as expected with migrating the old data`() =
        runTest {
            val testHandle = 12345L
            val testTimestamp = 100000L
            val testVideoRecentlyWatchedData = mutableListOf<VideoRecentlyWatchedItem>().apply {
                add(VideoRecentlyWatchedItem(testHandle, testTimestamp))
            }
            val addedHandle = 54321L
            val addedTimestamp = 200000L
            val addedCollectionId = 654321L
            val addedCollectionTitle = "collection title"
            val addedRecentlyWatchedItem = VideoRecentlyWatchedItem(
                addedHandle,
                addedTimestamp,
                addedCollectionId,
                addedCollectionTitle
            )
            val jsonString = Json.encodeToString(testVideoRecentlyWatchedData)
            whenever(appPreferencesGateway.monitorString(anyOrNull(), anyOrNull())).thenReturn(
                flowOf(jsonString)
            )
            whenever(
                videoRecentlyWatchedItemMapper(
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            ).thenReturn(
                addedRecentlyWatchedItem
            )
            initUnderTest()
            underTest.saveVideoRecentlyWatched(addedHandle, addedTimestamp)
            verify(megaLocalRoomGateway).saveRecentlyWatchedVideos(testVideoRecentlyWatchedData)
            verify(appPreferencesGateway).putString(
                "PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS",
                Json.encodeToString(emptyList<VideoRecentlyWatchedItem>())
            )
            verify(megaLocalRoomGateway).saveRecentlyWatchedVideo(addedRecentlyWatchedItem)
        }

    @Test
    fun `test that monitorRecentlyWatchedVideoNodes returns the correct result with migrating the old data`() =
        runTest {
            val testHandle = 12345L
            val testTimestamp = 100000L

            val collectionIdIndex = 1

            val testVideoRecentlyWatchedData = mutableListOf<VideoRecentlyWatchedItem>().apply {
                add(VideoRecentlyWatchedItem(testHandle, testTimestamp))
            }
            val jsonString = Json.encodeToString(testVideoRecentlyWatchedData)
            whenever(appPreferencesGateway.monitorString(anyOrNull(), anyOrNull())).thenReturn(
                flowOf(jsonString)
            )
            val testHandles = listOf(12345L, 23456L, 34567L)
            val testTimestamps = listOf(100000L, 200000L, 300000L)

            val testCollectionId = 654321L

            val testCollectionTitles = listOf(null, "set name", "collection title")

            val testItems = testHandles.mapIndexed { index, handle ->
                VideoRecentlyWatchedItem(
                    handle,
                    testTimestamps[index],
                    if (index == collectionIdIndex) {
                        testCollectionId
                    } else {
                        0
                    },
                    testCollectionTitles[index]
                )
            }

            val testMegaNodes = testHandles.map { handle ->
                initMegaNode(handle)
            }
            val testFileNodes = testHandles.map {
                mock<TypedFileNode>()
            }
            val testTypedVideoNodes = testHandles.mapIndexed { index, handle ->
                initTypedVideoNode(
                    handle,
                    testTimestamps[index],
                    testCollectionTitles[index]
                )
            }
            testMegaNodes.mapIndexed { index, node ->
                whenever(megaApiGateway.getMegaNodeByHandle(node.handle)).thenReturn(node)
                whenever(fileNodeMapper(node, false, null)).thenReturn(testFileNodes[index])
                whenever(
                    typedVideoNodeMapper(
                        fileNode = testFileNodes[index],
                        duration = 100,
                        watchedTimestamp = testTimestamps[index],
                        collectionTitle = testCollectionTitles[index]
                    )
                )
                    .thenReturn(testTypedVideoNodes[index])
            }

            initUnderTest()
            whenever(appPreferencesGateway.monitorString(anyOrNull(), anyOrNull())).thenReturn(
                flowOf(jsonString)
            )
            whenever(megaLocalRoomGateway.getAllRecentlyWatchedVideos()).thenReturn(flowOf(testItems))
            whenever(megaLocalRoomGateway.getAllOfflineInfo()).thenReturn(emptyList())
            val megaSetElement = mock<MegaSetElement> {
                on { node() }.thenReturn(testHandles[collectionIdIndex])
            }
            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(1L)
                on { get(0) }.thenReturn(megaSetElement)
            }
            val megaSet = mock<MegaSet> {
                on { name() }.thenReturn(testCollectionTitles[collectionIdIndex])
            }
            whenever(megaApiGateway.getSetElements(anyOrNull())).thenReturn(megaSetElementList)
            whenever(megaApiGateway.getSet(anyOrNull())).thenReturn(megaSet)

            underTest.monitorRecentlyWatchedVideoNodes().test {
                val actual = awaitItem()
                verify(megaLocalRoomGateway).saveRecentlyWatchedVideos(testVideoRecentlyWatchedData)
                verify(appPreferencesGateway).putString(
                    "PREFERENCE_KEY_RECENTLY_WATCHED_VIDEOS",
                    Json.encodeToString(emptyList<VideoRecentlyWatchedItem>())
                )
                assertThat(actual).isNotEmpty()
                assertThat(actual.size).isEqualTo(3)
                testTimestamps.sortedByDescending { it }.mapIndexed { index, expectedTimestamp ->
                    assertThat(actual[index].watchedTimestamp).isEqualTo(expectedTimestamp)
                    assertThat(actual[index].collectionTitle).isEqualTo(
                        testCollectionTitles.reversed()[index]
                    )
                }
                cancelAndConsumeRemainingEvents()
            }
        }

    private fun initMegaNode(nodeHandle: Long, favourite: Boolean = true) = mock<MegaNode> {
        on { handle }.thenReturn(nodeHandle)
        on { duration }.thenReturn(100)
        on { isFavourite }.thenReturn(favourite)
    }

    private fun initTypedVideoNode(
        nodeHandle: Long,
        timestamp: Long,
        title: String?,
        favourite: Boolean = false,
    ) =
        mock<TypedVideoNode> {
            on { id }.thenReturn(NodeId(nodeHandle))
            on { duration }.thenReturn(100.seconds)
            on { watchedTimestamp }.thenReturn(timestamp)
            on { collectionTitle }.thenReturn(title)
            on { isFavourite }.thenReturn(favourite)
        }

    @Test
    fun `test that clearRecentlyWatchedVideos function is invoked as expected`() = runTest {
        underTest.clearRecentlyWatchedVideos()
        verify(megaLocalRoomGateway).clearRecentlyWatchedVideos()
    }

    @Test
    fun `test that removeRecentlyWatchedItem function is invoked as expected`() =
        runTest {
            val testHandle = 123456L
            underTest.removeRecentlyWatchedItem(testHandle)
            verify(megaLocalRoomGateway).removeRecentlyWatchedVideo(testHandle)
        }
}