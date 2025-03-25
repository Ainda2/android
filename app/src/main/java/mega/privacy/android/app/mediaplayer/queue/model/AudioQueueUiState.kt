package mega.privacy.android.app.mediaplayer.queue.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Audio queue UI state
 *
 * @property items list of media queue item UI entities
 * @property isPaused whether the playing audio is paused
 * @property currentPlayingPosition the current playing position of the audio
 * @property indexOfCurrentPlayingItem the index of the current playing item
 * @property selectedItemHandles the selected item handles
 * @property isSearchMode whether the search mode is activated
 * @property isSelectMode whether the select mode is activated
 * @property removedItems the removed items
 * @property itemsRemovedEvent the removed items event
 */
data class AudioQueueUiState(
    val items: List<MediaQueueItemUiEntity>,
    val isPaused: Boolean = false,
    val currentPlayingPosition: String = "00:00",
    val indexOfCurrentPlayingItem: Int = -1,
    val selectedItemHandles: List<Long> = emptyList(),
    val isSearchMode: Boolean = false,
    val isSelectMode: Boolean = false,
    val removedItems: List<MediaQueueItemUiEntity> = emptyList(),
    val itemsRemovedEvent: StateEvent = consumed,
)