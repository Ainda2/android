package mega.privacy.android.app.presentation.favourites

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.favourites.GetFavouriteSortOrderUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.favourites.MapFavouriteSortOrderUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FavouritesViewModelTest {
    private lateinit var underTest: FavouritesViewModel

    private val stringUtilWrapper =
        mock<StringUtilWrapper> {
            on {
                getFolderInfo(
                    any(),
                    any(),
                    any(),
                )
            }.thenReturn("info")
        }
    private val favouriteMapper = mock<FavouriteMapper> {
        on { invoke(any(), any(), any(), any(), any(), any()) }
            .thenAnswer {
                val typedFileNode = it.arguments[1] as TypedFileNode
                mock<FavouriteFile> {
                    on { typedNode }.thenReturn(typedFileNode)
                }
            }
    }
    private val getFavouriteSortOrderUseCase = mock<GetFavouriteSortOrderUseCase> {
        onBlocking { invoke() }.thenReturn(FavouriteSortOrder.ModifiedDate(false))
    }

    private val megaNode = mock<MegaNode>()

    private val fetchNodeWrapper =
        mock<FetchNodeWrapper> { onBlocking { invoke(any()) }.thenReturn(megaNode) }

    private val mapFavouriteSortOrderUseCase = mock<MapFavouriteSortOrderUseCase>()

    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase> {
        onBlocking { invoke(any()) }.thenReturn(false)
    }

    private val evenString = "Even"
    private val oddString = "Odd"
    private val timeDescending = 10L downTo 1L
    private val descendingTimeNodes = timeDescending.map { time ->
        val nameString = if (time.mod(2) == 0) "$evenString $time" else "$oddString $time"
        mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(time))
            on { modificationTime }.thenReturn(time)
            on { name }.thenReturn(nameString)
        }
    }
    private val getAllFavorites =
        mock<GetAllFavoritesUseCase> { on { invoke() }.thenReturn(flowOf(descendingTimeNodes)) }

    private val connectedFlow = MutableStateFlow(false)

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase> {
        on { invoke() }.thenReturn(connectedFlow)
    }

    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }

    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }

    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(false))
    }

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase> {
        onBlocking {
            invoke(any())
        }.thenReturn(false)
    }

    private val isHidingActionAllowedUseCase = mock<IsHidingActionAllowedUseCase> {
        onBlocking {
            invoke(NodeId(any()))
        }.thenReturn(false)
    }

    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

    @BeforeEach
    fun setUp() {
        Mockito.reset(
            mapFavouriteSortOrderUseCase,
            getFileTypeInfoByNameUseCase,
            getNodeContentUriUseCase
        )
        initViewModel()
    }

    private val headerMapper = mock<HeaderMapper> {
        on { invoke(any()) }.thenReturn(
            FavouriteHeaderItem(null, null)
        )
    }

    private fun initViewModel() {
        underTest = FavouritesViewModel(
            getAllFavoritesUseCase = getAllFavorites,
            stringUtilWrapper = stringUtilWrapper,
            getFavouriteSortOrderUseCase = getFavouriteSortOrderUseCase,
            removeFavouritesUseCase = mock(),
            favouriteMapper = favouriteMapper,
            fetchNodeWrapper = fetchNodeWrapper,
            mapFavouriteSortOrderUseCase = mapFavouriteSortOrderUseCase,
            headerMapper = headerMapper,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            isHidingActionAllowedUseCase = isHidingActionAllowedUseCase,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            getFileTypeInfoByNameUseCase = getFileTypeInfoByNameUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    @Test
    fun `test default state`() = runTest {
        whenever(getAllFavorites()).thenReturn(
            flow { awaitCancellation() }
        )

        initViewModel()

        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
        }
    }

    @Test
    fun `test empty state`() = runTest {
        whenever(getAllFavorites()).thenReturn(
            flowOf(emptyList())
        )

        initViewModel()

        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Empty::class.java)
        }
    }

    @Test
    fun `test load favourites success`() = runTest {
        whenever(getAllFavorites()).thenReturn(
            flowOf(descendingTimeNodes)
        )
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
        }
    }

    @Test
    fun `test that selecting a node adds the id to selected node list`() = runTest {
        val expected = timeDescending
            .map { longValue ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(NodeId(longValue))
                }
            }.first()
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            underTest.itemSelected(mock<FavouriteFile> { on { typedNode }.thenReturn(expected) })
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactly(
                expected.id
            )
        }
    }

    @Test
    fun `test that selecting items again removes them from the list `() = runTest {
        val expected = timeDescending
            .map { longValue ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(NodeId(longValue))
                }
            }.first()

        whenever(getAllFavorites()).thenReturn(flowOf(descendingTimeNodes))
        initViewModel()

        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            val selected = mock<FavouriteFile> { on { typedNode }.thenReturn(expected) }
            underTest.itemSelected(selected)
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactly(
                expected.id
            )
            underTest.itemSelected(selected)
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).isEmpty()
        }
    }

    @Test
    fun `test that select all selects the whole list`() = runTest {
        val expected = timeDescending
            .map { NodeId(it) }
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            underTest.selectAll()
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactlyElementsIn(
                expected
            )
        }
    }

    @Test
    fun `test that clear selection returns an empty selected set`() = runTest {
        val expected = timeDescending
            .map { NodeId(it) }
        whenever(getAllFavorites()).thenReturn(flowOf(descendingTimeNodes))
        initViewModel()
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            underTest.selectAll()
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactlyElementsIn(
                expected
            )
            underTest.clearSelections()
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).isEmpty()
        }
    }


    private fun verifyDefaultSortOrder(items: FavouriteLoadState) {
        val expected = timeDescending.reversed()
        verifyInOrder(items, expected)
    }

    private fun verifyInOrder(
        items: FavouriteLoadState,
        expected: Iterable<Long>,
        message: String? = "Verify in order failed",
    ) {
        assertThat(items).isInstanceOf(FavouriteLoadState.Success::class.java)
        assertWithMessage(message).that((items as FavouriteLoadState.Success).favourites.drop(
            1
        )
            .map { (it.favourite?.typedNode as? TypedFileNode)?.modificationTime })
            .containsExactlyElementsIn(
                expected
            ).inOrder()
    }

    @Test
    fun `test that initial connected state matches current state`() = runTest {
        val currentConnectedState = connectedFlow.value
        underTest.favouritesState.filterNot { it is FavouriteLoadState.Loading }.test {
            assertThat(awaitItem().isConnected).isEqualTo(currentConnectedState)
        }
    }

    @Test
    fun `test that subsequent connected state matches latest value`() = runTest {
        connectedFlow.emit(true)
        val initial = connectedFlow.value

        initViewModel()

        underTest.favouritesState.test {
            assertThat(awaitItem().isConnected).isEqualTo(initial)
            connectedFlow.emit(!initial)
            assertThat(awaitItem().isConnected).isEqualTo(!initial)
        }
    }

    @Test
    fun `test that getFileTypeInfoByNameUseCase function is invoked and returns as expected`() =
        runTest {
            val mockName = "name"
            val expectedFileTypeInfo = VideoFileTypeInfo("", "", 10.seconds)
            whenever(getFileTypeInfoByNameUseCase(mockName)).thenReturn(expectedFileTypeInfo)
            val actual = underTest.getFileTypeInfo(mockName)
            assertThat(actual is VideoFileTypeInfo).isTrue()
            verify(getFileTypeInfoByNameUseCase).invoke(mockName)
        }

    @Test
    fun `test that getFileTypeInfoByNameUseCase returns null when an exception is thrown`() =
        runTest {
            val mockName = "name"
            whenever(getFileTypeInfoByNameUseCase(mockName)).thenThrow(NullPointerException())
            val actual = underTest.getFileTypeInfo(mockName)
            assertThat(actual).isNull()
        }

    @Test
    fun `test that GetNodeContentUriUseCase function is invoked and returns as expected`() =
        runTest {
            val mockTypedFileNode = mock<TypedFileNode>()
            val expectedContentUri = NodeContentUri.RemoteContentUri("", false)
            whenever(getNodeContentUriUseCase(any())).thenReturn(expectedContentUri)
            val actual = underTest.getNodeContentUri(mockTypedFileNode)
            assertThat(actual).isEqualTo(expectedContentUri)
            verify(getNodeContentUriUseCase).invoke(mockTypedFileNode)
        }
}