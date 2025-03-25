package mega.privacy.android.app.mediaplayer.gateway

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.service.Metadata

/**
 * The media player service gateway
 */
interface MediaPlayerServiceGateway {
    /**
     *  Metadata update
     *
     *  @return Flow<Metadata>
     */
    fun metadataUpdate(): Flow<Metadata>

    /**
     * Monitor the mediaNotAllowPlayState
     *
     * @return mediaNotAllowPlayState
     */
    fun monitorMediaNotAllowPlayState(): Flow<Boolean>

    /**
     * Stop player
     */
    fun stopPlayer()

    /**
     * Judge the player whether is playing
     *
     * @return true is playing, otherwise is false.
     */
    fun playing(): Boolean

    /**
     * Seek to the index
     *
     * @param index the index that is sought to
     * @param handle the handle that is sought to
     */
    fun seekTo(index: Int, handle: Long)

    /**
     * Set playWhenReady
     *
     * @param playWhenReady playWhenReady
     */
    fun setPlayWhenReady(playWhenReady: Boolean)

    /**
     * Remove the listener from player
     *
     * @param listener removed listener
     */
    fun removeListener(listener: Player.Listener)

    /**
     * Add the listener for player
     *
     * @param listener Player.Listener
     */
    fun addPlayerListener(listener: Player.Listener)

    /**
     * Get the playback state
     *
     * @return playback state
     */
    fun getPlaybackState(): Int?

    /**
     * Get the current media item
     *
     * @return MediaItem
     */
    fun getCurrentMediaItem(): MediaItem?

    /**
     * Get current playing position
     *
     * @return current playing position
     */
    fun getCurrentPlayingPosition(): Long

    /**
     * Setup player view
     *
     * @param playerView PlayerView
     * @param useController useController
     * @param controllerShowTimeoutMs controllerShowTimeoutMs
     * @param isAudioPlayer true is audio player, otherwise is false
     * @param controllerHideOnTouch controllerHideOnTouch
     * @param showShuffleButton showShuffleButton
     */
    fun setupPlayerView(
        playerView: PlayerView,
        useController: Boolean = true,
        controllerShowTimeoutMs: Int = 0,
        controllerHideOnTouch: Boolean = false,
        isAudioPlayer: Boolean = true,
        showShuffleButton: Boolean? = null,
    )

    /**
     * Check the user if logged in when the audio player is closed, if not, stop the audio service
     */
    fun stopAudioServiceWhenAudioPlayerClosedWithUserNotLogin()

    /**
     * Get the current adapter type
     *
     * @return the current adapter type
     */
    fun getCurrentAdapterType(): Int
}