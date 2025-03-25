package mega.privacy.android.app.presentation.photos.albums.actionMode

import mega.privacy.android.shared.resources.R as sharedR
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.mobile.analytics.event.AlbumDeselectAllEvent
import mega.privacy.mobile.analytics.event.AlbumListShareLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.AlbumSelectAllEvent
import mega.privacy.mobile.analytics.event.AlbumsListDeleteAlbumsEvent

/**
 * Action Mode Callback class for Albums
 */
class AlbumsActionModeCallback(
    private val fragment: PhotosFragment,
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.photos_albums_action, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.findItem(R.id.action_menu_get_link)?.let {
            it.isVisible = true
            it.isEnabled = true
            it.title = fragment.context?.resources?.getQuantityString(
                sharedR.plurals.label_share_links,
                fragment.albumsViewModel.state.value.selectedAlbumIds.size
            )
        }

        menu?.findItem(R.id.action_menu_remove_link)?.let {
            it.isVisible = fragment.isAllSelectedAlbumExported()
            it.isEnabled = fragment.isAllSelectedAlbumExported()
            it.title = fragment.context?.resources?.getQuantityString(
                R.plurals.album_share_remove_links_dialog_button,
                fragment.albumsViewModel.state.value.selectedAlbumIds.size
            )
        }
        updateSelectAllMenu(menu, fragment.albumsViewModel.state.value)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_menu_get_link -> {
                Analytics.tracker.trackEvent(AlbumListShareLinkMenuItemEvent)
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    val selectedAlbums = fragment.albumsViewModel.state.value.selectedAlbumIds
                    if (selectedAlbums.size == 1) {
                        fragment.openAlbumGetLinkScreen()
                    } else {
                        fragment.openAlbumGetMultipleLinksScreen()
                    }
                    fragment.albumsViewModel.clearAlbumSelection()
                }
            }

            R.id.action_delete -> {
                Analytics.tracker.trackEvent(AlbumsListDeleteAlbumsEvent)
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    fragment.albumsViewModel.showDeleteAlbumsConfirmation()
                }
            }

            R.id.action_menu_remove_link -> {
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    fragment.albumsViewModel.showRemoveLinkDialog()
                }
            }

            R.id.action_context_select_all -> {
                with(fragment.albumsViewModel) {
                    Analytics.tracker.trackEvent(
                        AlbumSelectAllEvent(albumsCount = state.value.albums.filter { it.id is Album.UserAlbum }.size)
                    )
                    selectAllAlbums()
                }
            }

            R.id.action_context_clear_selection -> {
                Analytics.tracker.trackEvent(AlbumDeselectAllEvent)
                fragment.albumsViewModel.clearAlbumSelection()
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        fragment.albumsViewModel.clearAlbumSelection()
    }

    private fun updateSelectAllMenu(menu: Menu?, state: AlbumsViewState) {
        menu?.findItem(R.id.action_context_select_all)?.isVisible =
            state.selectedAlbumIds.size < state.albums.filter { it.id is Album.UserAlbum }.size
    }
}
