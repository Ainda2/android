package mega.privacy.android.domain.usecase.favourites

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject


/**

 * Default get all favorites
 *
 * @property favouritesRepository
 * @property nodeRepository
 * @property addNodeType
 */
class GetAllFavoritesUseCase @Inject constructor(
    private val favouritesRepository: FavouritesRepository,
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
    private val sortFavouritesUseCase: SortFavouritesUseCase,
) {
    /**
     * get favourites
     * @return Flow<List<FavouriteInfo>>
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<TypedNode>> =
        flow {
            emit(favouritesRepository.getAllFavorites())
            emitAll(nodeRepository.monitorNodeUpdates()
                .mapLatest { favouritesRepository.getAllFavorites() })
        }.mapLatest { list ->
            sortFavouritesUseCase(list)
                .map { addNodeType(it) }
        }
}
