package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.node.Node

/**
 * The repository interface regarding favourites
 */
interface FavouritesRepository {

    /**
     * Get favourites
     * @return List<FavouriteInfo>
     */
    suspend fun getAllFavorites(): List<Node>

    /**
     * Removing favourites
     * @param handles the handle of items that are removed.
     */
    suspend fun removeFavourites(handles: List<Long>)
}