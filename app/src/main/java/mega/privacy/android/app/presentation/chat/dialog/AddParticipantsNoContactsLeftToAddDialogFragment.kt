package mega.privacy.android.app.presentation.chat.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.AllContactsAddedDialog
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Fragment to display a custom two buttons alert dialog when user is trying to add participants
 * to a chat/meeting but all contacts are already participants
 */
@AndroidEntryPoint
class AddParticipantsNoContactsLeftToAddDialogFragment : DialogFragment() {

    @Inject
    /** Current theme */
    lateinit var getThemeMode: GetThemeMode

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var navigator: MegaNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    AllContactsAddedDialog(
                        onNavigateToInviteContact = {
                            navigator.openInviteContactActivity(
                                requireContext(),
                                false
                            )
                        },
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Creates an instance of this class
         *
         * @return AddParticipantsNoContactsLeftToAddDialogFragment new instance
         */
        @JvmStatic
        fun newInstance() = AddParticipantsNoContactsLeftToAddDialogFragment()
    }
}