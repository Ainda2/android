package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.favourites.GetFavouriteFolderInfoUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GetFavouriteFolderInfoUseCaseTest {
    lateinit var underTest: GetFavouriteFolderInfoUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val addNodeType = mock<AddNodeType>()

    @Before
    fun setUp() {
        underTest = GetFavouriteFolderInfoUseCase(
            nodeRepository = nodeRepository,
            addNodeType = addNodeType,
            getCloudSortOrder = getCloudSortOrder
        )
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(emptyFlow())
        runBlocking {
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        }
    }

    @Test
    fun `test that a non folder node throws an exception`() = runTest {
        val fileNode = mock<FileNode>()
        val nodeId = NodeId(1)
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(fileNode)
        underTest(nodeId.longValue).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(ParentNotAFolderException::class.java)
        }
    }

    @Test
    fun `test that subsequent items are returned when nodes update`() = runTest {
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
        val first = "first"
        val second = "second"
        val folderNode = mock<FolderNode> {
            on { name }.thenReturn(first, second)
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodeId = NodeId(1)
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(folderNode)
        whenever(nodeRepository.getNodeChildren(folderNode.id, SortOrder.ORDER_NONE)).thenReturn(emptyList())

        underTest(nodeId.longValue).test {
            assertThat(awaitItem().name).isEqualTo(first)
            assertThat(awaitItem().name).isEqualTo(second)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that children are returned if present`() = runTest {
        val folderNode = mock<FolderNode> {
            on { name }.thenReturn("Name")
            on { parentId }.thenReturn(NodeId(2))
        }
        val nodeId = NodeId(1)
        val children = listOf(mock<FileNode>())
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(folderNode)
        whenever(nodeRepository.getNodeChildren(folderNode.id, SortOrder.ORDER_NONE)).thenReturn(children)

        underTest(nodeId.longValue).test {
            assertThat(awaitItem().children).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

}