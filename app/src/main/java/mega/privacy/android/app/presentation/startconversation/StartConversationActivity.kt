package mega.privacy.android.app.presentation.startconversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.app.presentation.startconversation.view.StartConversationView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.mobile.analytics.event.GroupChatPressedEvent
import mega.privacy.mobile.analytics.event.InviteContactsPressedEvent
import javax.inject.Inject

/**
 * Activity which allows to start a new chat conversation.
 *
 * @property getThemeMode   [GetThemeMode]
 * @property resultLauncher [ActivityResultLauncher]
 */
@AndroidEntryPoint
class StartConversationActivity : ComponentActivity() {

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<StartConversationViewModel>()

    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var addContactActivityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    setResult(RESULT_OK, result.data)
                } else {
                    setResult(RESULT_CANCELED)
                }

                finish()
            }

        addContactActivityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data = result.data
                if (result.resultCode == RESULT_OK && data != null) {
                    viewModel.onStartGroupConversation(
                        title = data.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE),
                        isEkr = data.getBooleanExtra(AddContactActivity.EXTRA_EKR, false),
                        chatLink = data.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false),
                        addParticipants = data.getBooleanExtra(
                            AddContactActivity.ALLOW_ADD_PARTICIPANTS,
                            false
                        ),
                        emails = data.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                    )
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    if (state.error != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            setResult(RESULT_CANCELED)
                            finish()
                        }, Constants.LONG_SNACKBAR_DURATION)
                    } else if (state.result != null) {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_NEW_CHAT_ID, state.result)
                        )
                        finish()
                    }
                }
            }
        }

        viewModel.setFromChat(intent.getBooleanExtra(FROM_CHAT, false))
        enableEdgeToEdge()
        setContent { StartConversationView() }
    }

    @Composable
    private fun StartConversationView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val uiState by viewModel.state.collectAsState()


        MegaAppContainer(
            themeMode = themeMode,
            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
        ) {
            StartConversationView(
                state = uiState,
                onButtonClicked = ::onActionTap,
                onContactClicked = viewModel::onContactTap,
                onSearchTextChange = viewModel::setTypedSearch,
                onCloseSearchClicked = viewModel::onCloseSearchTap,
                onBackPressed = { finish() },
                onSearchClicked = viewModel::onSearchTap,
                onInviteContactsClicked = { onInviteContacts() },
                onNoteToSelfClicked = viewModel::onNoteToSelfTap
            )
        }
    }


    private fun onActionTap(action: StartConversationAction) {
        when (action) {
            StartConversationAction.NewGroup -> onNewGroup()
            StartConversationAction.NewMeeting -> onNewMeeting()
            StartConversationAction.JoinMeeting -> onJoinMeeting()
        }
    }

    private fun onNewGroup() {
        Analytics.tracker.trackEvent(GroupChatPressedEvent)
        addContactActivityLauncher.launch(
            Intent(this, AddContactActivity::class.java)
                .putExtra(Constants.INTENT_EXTRA_KEY_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
                .putExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, true)
                .putExtra(AddContactActivity.EXTRA_IS_START_CONVERSATION, true)
        )
    }

    private fun onNewMeeting() {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_NEW_MEETING, true))
        finish()
    }

    private fun onJoinMeeting() {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_JOIN_MEETING, true))
        finish()
    }

    private fun onInviteContacts() {
        Analytics.tracker.trackEvent(InviteContactsPressedEvent)
        resultLauncher.launch(
            Intent(this, InviteContactActivity::class.java).putExtra(
                Constants.INTENT_EXTRA_KEY_CONTACT_TYPE,
                Constants.CONTACT_TYPE_DEVICE
            )
        )
    }

    companion object {
        private const val FROM_CHAT = "FROM_CHAT"

        /**
         * Intent extra for creating a new meeting.
         */
        const val EXTRA_NEW_MEETING = "NEW_MEETING"

        /**
         * Intent extra for joining a meeting.
         */
        const val EXTRA_JOIN_MEETING = "JOIN_MEETING"

        /**
         * Intent extra for opening a chat conversation.
         */
        const val EXTRA_NEW_CHAT_ID = "NEW_CHAT_HANDLE"

        /**
         * Gets an [Intent] to open this screen from Chat.
         *
         * @param context Required [Context].
         * @return The [Intent].
         */
        @JvmStatic
        fun getChatIntent(context: Context): Intent =
            Intent(context, StartConversationActivity::class.java)
                .putExtra(FROM_CHAT, true)
    }
}
