package mega.privacy.android.app.main.dialog.shares

import mega.privacy.android.shared.resources.R as sharedR
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class RemoveAllSharingContactDialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: RemoveAllSharingContactViewModel by viewModels()

    // it doesn't wait to remove the shares result and dismiss immediately show the request should execute in activityViewModel
    private val activityViewModel: ManagerViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                val text = if (uiState.numberOfShareFolder == 1) {
                    pluralStringResource(
                        R.plurals.confirmation_remove_outgoing_shares,
                        uiState.numberOfShareContact,
                        uiState.numberOfShareContact
                    )
                } else {
                    pluralStringResource(
                        R.plurals.alert_remove_several_shares,
                        uiState.numberOfShareFolder,
                        uiState.numberOfShareFolder
                    )
                }
                val confirmButtonText = if (uiState.numberOfShareFolder == 1) {
                    stringResource(id = R.string.general_remove)
                } else {
                    stringResource(id = R.string.shared_items_outgoing_unshare_confirm_dialog_button_yes)
                }
                val cancelButtonText = if (uiState.numberOfShareFolder == 1) {
                    stringResource(id = sharedR.string.general_dialog_cancel_button)
                } else {
                    stringResource(id = R.string.shared_items_outgoing_unshare_confirm_dialog_button_no)
                }
                MegaAlertDialog(
                    text = text,
                    confirmButtonText = confirmButtonText,
                    cancelButtonText = cancelButtonText,
                    onConfirm = {
                        val ids = requireArguments().getLongArray(EXTRA_NODE_IDS) ?: LongArray(0)
                        activityViewModel.removeShares(ids.toList())
                        dismissAllowingStateLoss()
                    },
                    onDismiss = { dismissAllowingStateLoss() },
                )
            }
        }
    }

    companion object {
        const val TAG = "RemoveAllSharingContactDialogFragment"
        const val EXTRA_NODE_IDS = "EXTRA_NODE_IDS"
        fun newInstance(ids: List<Long>) = RemoveAllSharingContactDialogFragment().apply {
            arguments = Bundle().apply { putLongArray(EXTRA_NODE_IDS, ids.toLongArray()) }
        }
    }
}