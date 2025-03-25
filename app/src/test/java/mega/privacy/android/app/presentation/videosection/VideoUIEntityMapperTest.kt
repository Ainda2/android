package mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videosection.mapper.VideoUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoUIEntityMapperTest {
    private lateinit var underTest: VideoUIEntityMapper

    private val expectedId = NodeId(123456L)
    private val expectedParentId = NodeId(654321L)
    private val expectedName = "video file name"
    private val expectedSize: Long = 100
    private val expectedAvailableOffline = true
    private val expectedDurationTime = 10.minutes
    private val expectedIsFavourite = false
    private val expectedElementID = 2L
    private val expectedLabel = 1
    private val expectedExportedData = mock<ExportedData>()
    private val expectedType = mock<VideoFileTypeInfo>()
    private val expectedWatchedTimestamp = 100L
    private val expectedCollectionTitle = "collection title"

    @BeforeAll
    fun setUp() {
        underTest = VideoUIEntityMapper()
    }

    @Test
    fun `test that VideoUIEntity can be mapped correctly when exportedData is not null and isOutShare is false`() =
        runTest {
            val testNode = initTypedVideoNode(expectedExportedData, false)
            val videoUIEntity = underTest(testNode)
            assertMappedVideoUIEntity(
                videoUIEntity = videoUIEntity,
                expectedIsShared = true
            )
        }

    @Test
    fun `test that VideoUIEntity can be mapped correctly when exportedData is not null and isOutShare is true`() =
        runTest {
            val testNode = initTypedVideoNode(expectedExportedData, true)
            val videoUIEntity = underTest(testNode)
            assertMappedVideoUIEntity(
                videoUIEntity = videoUIEntity,
                expectedIsShared = true
            )
        }

    @Test
    fun `test that VideoUIEntity can be mapped correctly when exportedData is null and isOutShared is true`() =
        runTest {
            val testNode = initTypedVideoNode(null, true)
            val videoUIEntity = underTest(testNode)
            assertMappedVideoUIEntity(
                videoUIEntity = videoUIEntity,
                expectedIsShared = true
            )
        }

    @Test
    fun `test that VideoUIEntity can be mapped correctly when exportedData is null and isOutShared is false`() =
        runTest {
            val testNode = initTypedVideoNode(null, false)
            val videoUIEntity = underTest(testNode)
            assertMappedVideoUIEntity(
                videoUIEntity = videoUIEntity,
                expectedIsShared = false
            )
        }

    @Test
    fun `test that VideoUIEntity can be mapped correctly when when elementId is null`() =
        runTest {
            val testNode = initTypedVideoNode(
                exportData = null,
                expectedIsOutShared = false,
                elementIDParam = null
            )
            val videoUIEntity = underTest(testNode)
            assertMappedVideoUIEntity(
                videoUIEntity = videoUIEntity,
                expectedIsShared = false,
                elementID = testNode.id.longValue
            )
        }

    private fun initTypedVideoNode(
        exportData: ExportedData?,
        expectedIsOutShared: Boolean,
        elementIDParam: Long? = expectedElementID,
    ) = mock<TypedVideoNode> {
        on { id }.thenReturn(expectedId)
        on { parentId }.thenReturn(expectedParentId)
        on { name }.thenReturn(expectedName)
        on { size }.thenReturn(expectedSize)
        on { isFavourite }.thenReturn(expectedIsFavourite)
        on { isAvailableOffline }.thenReturn(expectedAvailableOffline)
        on { duration }.thenReturn(expectedDurationTime)
        on { exportedData }.thenReturn(exportData)
        on { elementID }.thenReturn(elementIDParam)
        on { label }.thenReturn(expectedLabel)
        on { type }.thenReturn(expectedType)
        on { isOutShared }.thenReturn(expectedIsOutShared)
        on { watchedTimestamp }.thenReturn(expectedWatchedTimestamp)
        on { collectionTitle }.thenReturn(expectedCollectionTitle)
    }

    private fun assertMappedVideoUIEntity(
        videoUIEntity: VideoUIEntity,
        expectedIsShared: Boolean,
        elementID: Long = expectedElementID
    ) {
        videoUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${videoUIEntity::class.simpleName}",
                { assertThat(it.id).isEqualTo(expectedId) },
                { assertThat(it.parentId).isEqualTo(expectedParentId) },
                { assertThat(it.name).isEqualTo(expectedName) },
                { assertThat(it.size).isEqualTo(expectedSize) },
                { assertThat(it.duration).isEqualTo(expectedDurationTime) },
                { assertThat(it.nodeAvailableOffline).isEqualTo(expectedAvailableOffline) },
                { assertThat(it.isFavourite).isEqualTo(expectedIsFavourite) },
                { assertThat(it.isSharedItems).isEqualTo(expectedIsShared) },
                { assertThat(it.elementID).isEqualTo(elementID) },
                { assertThat(it.label).isEqualTo(expectedLabel) },
                { assertThat(it.fileTypeInfo).isEqualTo(expectedType) },
                { assertThat(it.watchedDate).isEqualTo(expectedWatchedTimestamp) },
                { assertThat(it.collectionTitle).isEqualTo(expectedCollectionTitle) }
            )
        }
    }
}