package mega.privacy.android.app.mediaplayer.gateway

import android.content.Intent
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.domain.exception.MegaException
import java.io.File

/**
 * PlayerServiceViewModelGateway for visit MediaPlayerServiceViewModel from outside
 */
interface PlayerServiceViewModelGateway {
    /**
     * Remove item
     *
     * @param handle the handle that is removed
     */
    fun removeItem(handle: Long)

    /**
     * Set new text for playlist search query
     *
     * @param newText the new text string
     */
    fun searchQueryUpdate(newText: String?)

    /**
     * Get current intent
     *
     * @return current intent
     */
    fun getCurrentIntent(): Intent?

    /**
     * Get the handle of the current playing item
     *
     * @return the handle of the current playing item
     */
    fun getCurrentPlayingHandle(): Long

    /**
     *  Set the handle of the current playing item
     *
     *  @param handle MegaNode handle
     */
    fun setCurrentPlayingHandle(handle: Long)

    /**
     * Get playlist item
     *
     * @param handle MegaNode handle
     * @return PlaylistItem
     */
    fun getPlaylistItem(handle: String?): PlaylistItem?

    /**
     * Update when playlist is changed
     *
     * @return Flow<Pair<List<PlaylistItem>, Int>>
     */
    fun playlistUpdate(): Flow<Pair<List<PlaylistItem>, Int>>

    /**
     * Get updated playlist items
     *
     * @return updated playlist items
     */
    fun getUpdatedPlaylistItems(): List<PlaylistItem>

    /**
     * Update item name
     *
     * @param handle MegaNode handle
     * @param newName the new name string
     */
    fun updateItemName(handle: Long, newName: String)

    /**
     * Get playlist items
     *
     * @return List<PlaylistItem>
     */
    fun getPlaylistItems(): List<PlaylistItem>

    /**
     * Update when media playback is changed
     *
     * @return Flow<Boolean>
     */
    fun mediaPlaybackUpdate(): Flow<Boolean>

    /**
     * Update when error is happened
     *
     * @return Flow<MegaException?>
     */
    fun errorUpdate(): Flow<MegaException?>

    /**
     * Update when the items are cleared
     *
     * @return Flow<Boolean?>
     */
    fun itemsClearedUpdate(): Flow<Boolean?>

    /**
     * Update when playlist title is changed
     *
     * @return Flow<String>
     */
    fun playlistTitleUpdate(): Flow<String>

    /**
     * Remove the selected items
     *
     * @param removedHandles the list of removed item handles
     */
    fun removeSelectedItems(removedHandles: List<Long>)

    /**
     * Saved or remove the selected items
     * @param handles node handle of selected item
     */
    fun itemsSelected(handles: List<Long>)

    /**
     * Get the index from playlistItems to keep the play order is correct after reordered
     * @param handle handle of clicked item
     * @return the index of clicked item in playlistItems or null
     */
    fun getIndexFromPlaylistItems(handle: Long): Int?

    /**
     * Swap the items
     * @param current the position of from item
     * @param target the position of to item
     */
    fun swapItems(current: Int, target: Int)

    /**
     * Updated the play source of exoplayer after reordered.
     */
    fun updatePlaySource()

    /**
     * Judge the current media item whether is paused
     *
     * @return true is paused, otherwise is false
     */
    fun isPaused(): Boolean

    /**
     * Set paused
     * @param paused the paused state
     */
    fun setPaused(paused: Boolean)

    /**
     * Handle player error.
     */
    fun onPlayerError()

    /**
     * Get playing thumbnail
     *
     * @return LiveData<File>
     */
    fun getPlayingThumbnail(): LiveData<File>

    /**
     * Update playerSource
     *
     * @return Flow<MediaPlaySources>
     */
    fun playerSourceUpdate(): Flow<MediaPlaySources>

    /**
     * Update when item is removed
     *
     * @return Flow<Pair<Int, Long>> Int is the position of removed item, Long is the handle of removed item
     */
    fun mediaItemToRemoveUpdate(): Flow<Pair<Int, Long>>

    /**
     * Update node name
     *
     * @return Flow<String>
     */
    fun nodeNameUpdate(): Flow<String>

    /**
     * Update retry
     *
     * @return Flow<Boolean>
     */
    fun retryUpdate(): Flow<Boolean>

    /**
     * Build player source from start intent.
     *
     * @param intent intent received from onStartCommand
     * @return if there is no error
     */
    suspend fun buildPlayerSource(intent: Intent?): Boolean

    /**
     * Cancel search token
     */
    fun cancelSearch()

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    fun clear()

    /**
     * Reset retry state
     */
    fun resetRetryState()

    /**
     * Set the search mode
     * @param value true is in search mode, otherwise is false
     */
    fun setSearchMode(value: Boolean)

    /**
     * Monitor the media item transition state
     */
    fun monitorMediaItemTransitionState(): Flow<Long?>
}