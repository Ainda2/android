package mega.privacy.android.app.main.megachat

import mega.privacy.android.shared.resources.R as sharedR
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.userWaitingForCall
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityGroupChatPropertiesBinding
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.listeners.GetPeerAttributesListener
import mega.privacy.android.app.listeners.InviteToChatRoomListener
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.main.megachat.chatAdapters.MegaParticipantsChatAdapter
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ManageChatLinkBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ParticipantBottomSheetDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsLeftToAddDialogFragment
import mega.privacy.android.app.presentation.chat.groupInfo.GroupChatInfoViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.createForceAppUpdateDialog
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity which shows group chat room information.
 *
 * @property binding [ActivityGroupChatPropertiesBinding]
 * @property chatLink The chat link
 * @property chat [MegaChatRoom]
 * @property chatC [ChatController]
 * @property chatHandle The chat id
 * @property selectedHandleParticipant The handle of participant selected
 * @property participantsCount Number of participants
 * @property endCallForAllShouldBeVisible True when end call for all should be visible. False, otherwise.
 * */
@AndroidEntryPoint
class GroupChatInfoActivity : PasscodeActivity(), MegaChatRequestListenerInterface,
    MegaRequestListenerInterface, SnackbarShower {

    @Inject
    lateinit var monitorChatListItemUpdates: MonitorChatListItemUpdates

    @Inject
    lateinit var monitorChatOnlineStatusUseCase: MonitorChatOnlineStatusUseCase

    @Inject
    lateinit var monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase

    @Inject
    lateinit var monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase

    @Inject
    lateinit var chatManagement: ChatManagement

    @Inject
    lateinit var navigator: MegaNavigator

    lateinit var binding: ActivityGroupChatPropertiesBinding
    private val viewModel by viewModels<GroupChatInfoViewModel>()

    var chatLink: String? = null
    var chatC: ChatController? = null
    var chatHandle: Long = 0
    var selectedHandleParticipant: Long = 0
    var participantsCount: Long = 0
    var endCallForAllShouldBeVisible = false

    var chat: MegaChatRoom? = null
        set(value) {
            value?.let { viewModel.setChatId(it.chatId) }
            field = value
        }

    private var groupChatInfoActivity: GroupChatInfoActivity? = null
    private var permissionsDialog: AlertDialog? = null
    private var changeTitleDialog: AlertDialog? = null
    private var chatLinkDialog: AlertDialog? = null
    private var endCallForAllDialog: AlertDialog? = null
    private var forceAppUpdateDialog: AlertDialog? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: MegaParticipantsChatAdapter? = null
    private val participants = ArrayList<MegaChatParticipant?>()
    private val pendingParticipantRequests = HashMap<Int, MegaChatParticipant>()
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null
    private var countDownTimer: CountDownTimer? = null

    /**
     * Broadcast to update a contact in adapter due to a change.
     * Currently the changes contemplated are: nickname and credentials.
     */
    private val contactUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.let {
                it.action?.let { action ->
                    if (action == BroadcastConstants.ACTION_UPDATE_NICKNAME || action == BroadcastConstants.ACTION_UPDATE_FIRST_NAME ||
                        action == BroadcastConstants.ACTION_UPDATE_LAST_NAME || action == BroadcastConstants.ACTION_UPDATE_CREDENTIALS
                    ) {
                        val userHandle = intent.getLongExtra(
                            BroadcastConstants.EXTRA_USER_HANDLE,
                            MegaApiJava.INVALID_HANDLE
                        )
                        if (userHandle != MegaApiJava.INVALID_HANDLE) {
                            updateAdapter(userHandle)
                            if (action != BroadcastConstants.ACTION_UPDATE_CREDENTIALS
                                && bottomSheetDialogFragment is ParticipantBottomSheetDialogFragment
                                && bottomSheetDialogFragment.isBottomSheetDialogShown()
                                && selectedHandleParticipant == userHandle
                            ) {
                                (bottomSheetDialogFragment as ParticipantBottomSheetDialogFragment).updateContactData()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Timber.d("onCreate")

        groupChatInfoActivity = this
        chatC = ChatController(this)

        if (shouldRefreshSessionDueToKarere()) return

        checkChatChanges()
        collectFlows()

        intent.extras?.let { extras ->
            chatHandle = extras.getLong(Constants.HANDLE, MEGACHAT_INVALID_HANDLE)
            if (chatHandle == MEGACHAT_INVALID_HANDLE) {
                finish()
                return
            }

            chat = megaChatApi.getChatRoom(chatHandle)
            if (chat == null) {
                Timber.e("Chatroom NULL cannot be recovered")
                finish()
                return
            }

            dbH = getInstance().dbH

            binding = ActivityGroupChatPropertiesBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbarGroupChatProperties)
            consumeInsetsWithToolbar(customToolbar = binding.toolbarGroupChatProperties)
            supportActionBar?.apply {
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                title = if (chat?.isMeeting == true) {
                    getString(R.string.meetings_info_title)
                } else {
                    getString(R.string.group_chat_info_label)
                }
            }

            linearLayoutManager = LinearLayoutManager(this)

            binding.chatGroupContactPropertiesList.apply {
                addItemDecoration(PositionDividerItemDecoration(groupChatInfoActivity, outMetrics))
                setHasFixedSize(true)
                layoutManager = linearLayoutManager
                isFocusable = false
                itemAnimator = DefaultItemAnimator()
                isNestedScrollingEnabled = false

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val withElevation =
                            recyclerView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
                        binding.toolbarGroupChatProperties.elevation =
                            if (withElevation) resources.getDimension(R.dimen.toolbar_elevation) else 0f

                        if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                            checkIfShouldAskForUsersAttributes(RecyclerView.SCROLL_STATE_IDLE)
                        }
                    }

                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        checkIfShouldAskForUsersAttributes(newState)
                    }
                })
            }

            megaChatApi.signalPresenceActivity()

            val contactUpdateFilter =
                IntentFilter(BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
            contactUpdateFilter.addAction(BroadcastConstants.ACTION_UPDATE_NICKNAME)
            contactUpdateFilter.addAction(BroadcastConstants.ACTION_UPDATE_FIRST_NAME)
            contactUpdateFilter.addAction(BroadcastConstants.ACTION_UPDATE_LAST_NAME)
            contactUpdateFilter.addAction(BroadcastConstants.ACTION_UPDATE_CREDENTIALS)
            registerReceiver(contactUpdateReceiver, contactUpdateFilter)
            setParticipants()
            updateAdapterHeader()

            adapter?.checkNotifications(chatHandle)

            savedInstanceState?.let {
                val isEndCallForAllDialogShown = it.getBoolean(
                    END_CALL_FOR_ALL_DIALOG, false
                )
                if (isEndCallForAllDialogShown) {
                    showEndCallForAllDialog()
                }
            }
        }
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { state ->
            if (state.isPushNotificationSettingsUpdatedEvent) {
                adapter?.checkNotifications(chatHandle)
                viewModel.onConsumePushNotificationSettingsUpdateEvent()
            }
            if (state.showForceUpdateDialog) {
                showForceUpdateAppDialog()
            }

            if (state.error != null) {
                showSnackbar(getString(state.error))
                adapter?.updateAllowAddParticipants(getChatRoom().isOpenInvite)
            } else if (state.resultSetOpenInvite != null) {
                adapter?.updateAllowAddParticipants(state.resultSetOpenInvite)
                updateAdapterHeader()
                updateParticipants()
                invalidateOptionsMenu()
            }
            updateParticipantsWarning()
            val call = state.call
            val chatRoom = state.chatRoom
            val endCallVisible = call != null && call.status != ChatCallStatus.Initial
                    && call.status != ChatCallStatus.TerminatingUserParticipation
                    && call.status != ChatCallStatus.Destroyed
                    && chatRoom != null && chatRoom.ownPrivilege == ChatRoomPermission.Moderator
            if (endCallVisible != endCallForAllShouldBeVisible) {
                endCallForAllShouldBeVisible = endCallVisible
                adapter?.updateEndCallOption(endCallForAllShouldBeVisible)
                if (!endCallVisible) {
                    endCallForAllDialog?.dismiss()
                }
            }
        }

        collectFlow(viewModel.state.map { it.retentionTime }.distinctUntilChanged()) {
            it?.let { adapter?.updateRetentionTimeUI(it) }
        }
    }

    private fun updateParticipantsWarning() {
        lifecycleScope.launch {
            delay(100)
            adapter?.updateParticipantWarning(viewModel.state.value.shouldShowUserLimitsWarning)
        }
    }

    /**
     * Show Force App Update Dialog
     */
    private fun showForceUpdateAppDialog() {
        if (forceAppUpdateDialog?.isShowing == true) return
        forceAppUpdateDialog = createForceAppUpdateDialog(this) {
            viewModel.onForceUpdateDialogDismissed()
        }
        forceAppUpdateDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(contactUpdateReceiver)
    }

    private fun setParticipants() {
        //Set the first element = me
        chat?.let { chatRoom ->
            participantsCount = chatRoom.peerCount
            Timber.d("Participants count: %s", participantsCount)

            if (!chatRoom.isPreview && chatRoom.isActive) {
                var myFullName = megaChatApi.myFullname
                if (myFullName.isNullOrEmpty()) {
                    myFullName = megaChatApi.myEmail
                }
                val me = MegaChatParticipant(
                    megaChatApi.myUserHandle,
                    null,
                    null,
                    getString(R.string.chat_me_text_bracket, myFullName),
                    megaChatApi.myEmail,
                    chatRoom.ownPrivilege
                )
                participants.add(me)
            }

            for (i in 0 until participantsCount) {
                val peerPrivilege = chatRoom.getPeerPrivilege(i)
                if (peerPrivilege == MegaChatRoom.PRIV_RM) {
                    continue
                }

                val peerHandle = chatRoom.getPeerHandle(i)
                val participant = MegaChatParticipant(peerHandle, peerPrivilege)
                participants.add(participant)
                val userStatus = ChatUtil.getUserStatus(peerHandle)
                if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                    megaChatApi.requestLastGreen(participant.handle)
                }
            }

            Timber.d("Number of participants with me: %s", participants.size)

            if (adapter == null) {
                adapter = MegaParticipantsChatAdapter(this, binding.chatGroupContactPropertiesList)
                adapter?.setHasStableIds(true)
                binding.chatGroupContactPropertiesList.adapter = adapter
            }

            adapter?.setParticipants(participants)
        }
    }

    private fun updateAdapter(contactHandle: Long) {
        chat = megaChatApi.getChatRoom(chatHandle)

        if (contactHandle == megaChatApi.myUserHandle) {
            val pos = participants.size - 1
            participants[pos]?.fullName =
                getString(
                    R.string.chat_me_text_bracket,
                    megaChatApi.myFullname
                )

            adapter?.updateParticipant(pos, participants)
            return
        }

        for (participant in participants) {
            if (participant?.handle == contactHandle) {
                val pos = participants.indexOf(participant)
                participants[pos]?.fullName = chatC?.getParticipantFullName(contactHandle)
                adapter?.updateParticipant(pos, participants)
                break
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_group_chat_info, menu)
        val addParticipantsMenuItem = menu.findItem(R.id.action_add_participants)
        val changeTitleItem = menu.findItem(R.id.action_rename)
        chat?.let { chatRoom ->
            chatRoom.ownPrivilege.let { permission ->
                changeTitleItem.isVisible = permission == MegaChatRoom.PRIV_MODERATOR
                addParticipantsMenuItem.isVisible =
                    chatRoom.isActive && (permission == MegaChatRoom.PRIV_MODERATOR || chatRoom.isOpenInvite)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        megaChatApi.signalPresenceActivity()
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_add_participants -> chooseAddParticipantDialog()
            R.id.action_rename -> showRenameGroupDialog(false)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Control when set open invite option is selected
     */
    fun setOpenInvite() {
        chat?.let {
            viewModel.onAllowAddParticipantsTap(it.chatId)
        }
    }

    /**
     * Open add participants screen if has contacts
     */
    fun chooseAddParticipantDialog() {
        Timber.d("chooseAddContactDialog")
        megaChatApi.signalPresenceActivity()
        if (megaApi.rootNode != null) {
            val contacts = megaApi.contacts
            if (contacts.isNullOrEmpty() || !contacts.any { it.visibility == VISIBILITY_VISIBLE }) {
                val dialog = AddParticipantsNoContactsDialogFragment.newInstance()
                dialog.show(supportFragmentManager, dialog.tag)
            } else if (ChatUtil.areAllMyContactsChatParticipants(chat)) {
                val dialog = AddParticipantsNoContactsLeftToAddDialogFragment.newInstance()
                dialog.show(supportFragmentManager, dialog.tag)
            } else {
                val intent = Intent(this, AddContactActivity::class.java)
                intent.putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
                intent.putExtra(INTENT_EXTRA_KEY_CHAT, true)
                intent.putExtra(INTENT_EXTRA_KEY_CHAT_ID, chatHandle)
                intent.putExtra(
                    INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                    getString(R.string.add_participants_menu_item)
                )

                @Suppress("deprecation")
                startActivityForResult(intent, Constants.REQUEST_ADD_PARTICIPANTS)
            }
        } else {
            Timber.w("Online but not megaApi")
            Util.showErrorAlertDialog(
                getString(R.string.error_server_connection_problem),
                false,
                this
            )
        }
    }

    /**
     * Shows an alert dialog to confirm the deletion of a participant.
     *
     * @param handle participant's handle
     */
    fun showRemoveParticipantConfirmation(handle: Long) {
        megaChatApi.signalPresenceActivity()

        groupChatInfoActivity?.let { activity ->
            val builder = MaterialAlertDialogBuilder(
                activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )
            val name = chatC?.getParticipantFullName(handle)
            builder.setMessage(resources.getString(R.string.confirmation_remove_chat_contact, name))
                .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int -> removeParticipant() }
                .setNegativeButton(sharedR.string.general_dialog_cancel_button, null)
                .show()
        }
    }

    private fun removeParticipant() =
        chatC?.removeParticipant(chatHandle, selectedHandleParticipant)

    /**
     * Shows change permissions dialog
     *
     * @param handle The user handle.
     * @param chatToChange [MegaChatRoom] current chat
     */
    fun showChangePermissionsDialog(handle: Long, chatToChange: MegaChatRoom) {
        megaChatApi.signalPresenceActivity()

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.change_permissions_dialog, null)

        val administratorLayout =
            dialogLayout.findViewById<LinearLayout>(R.id.change_permissions_dialog_administrator_layout)
        val administratorCheck =
            dialogLayout.findViewById<CheckedTextView>(R.id.change_permissions_dialog_administrator)

        val memberLayout =
            dialogLayout.findViewById<LinearLayout>(R.id.change_permissions_dialog_member_layout)
        val memberCheck =
            dialogLayout.findViewById<CheckedTextView>(R.id.change_permissions_dialog_member)

        val observerLayout =
            dialogLayout.findViewById<LinearLayout>(R.id.change_permissions_dialog_observer_layout)
        val observerCheck =
            dialogLayout.findViewById<CheckedTextView>(R.id.change_permissions_dialog_observer)

        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        builder.setView(dialogLayout)
        builder.setTitle(getString(R.string.file_properties_shared_folder_permissions))

        permissionsDialog = builder.create()
        permissionsDialog?.show()

        when (chatToChange.getPeerPrivilegeByHandle(handle)) {
            MegaChatRoom.PRIV_STANDARD -> {
                administratorCheck.isChecked = false
                memberCheck.isChecked = true
                observerCheck.isChecked = false
            }

            MegaChatRoom.PRIV_MODERATOR -> {
                administratorCheck.isChecked = true
                memberCheck.isChecked = false
                observerCheck.isChecked = false
            }

            else -> {
                administratorCheck.isChecked = false
                memberCheck.isChecked = false
                observerCheck.isChecked = true
            }
        }

        val dialog = permissionsDialog
        val clickListener = View.OnClickListener { v: View ->
            when (v.id) {
                R.id.change_permissions_dialog_administrator_layout -> changePermissions(
                    MegaChatRoom.PRIV_MODERATOR
                )

                R.id.change_permissions_dialog_member_layout -> changePermissions(MegaChatRoom.PRIV_STANDARD)
                R.id.change_permissions_dialog_observer_layout -> changePermissions(MegaChatRoom.PRIV_RO)
            }
            dialog?.dismiss()
        }

        administratorLayout.setOnClickListener(clickListener)
        memberLayout.setOnClickListener(clickListener)
        observerLayout.setOnClickListener(clickListener)
    }

    private fun changePermissions(newPermissions: Int) {
        Timber.d("New permissions: %s", newPermissions)
        chatC?.alterParticipantsPermissions(chatHandle, selectedHandleParticipant, newPermissions)
    }

    /**
     * Show bottom sheet dialog of a participant
     *
     * @param participant The concrete participant
     */
    fun showParticipantsPanel(participant: MegaChatParticipant) {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) return

        Timber.d("Participant Handle: %s", participant.handle)
        selectedHandleParticipant = participant.handle

        bottomSheetDialogFragment = ParticipantBottomSheetDialogFragment()
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * Shows panel to get the chat link
     */
    fun showGetChatLinkPanel() {
        if (chatLink.isNullOrEmpty() || bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }

        bottomSheetDialogFragment = ManageChatLinkBottomSheetDialogFragment()
        (bottomSheetDialogFragment as ManageChatLinkBottomSheetDialogFragment).setValues(
            chatLink
                ?: return,
            chat?.ownPrivilege == MegaChatRoom.PRIV_MODERATOR,
            chat?.title, chat?.isMeeting ?: false
        )

        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * Remove chat link
     */
    fun removeChatLink() {
        Timber.d("removeChatLink")
        megaChatApi.removeChatLink(chatHandle, this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("Result Code: %s", resultCode)
        if (intent == null) {
            Timber.w("Intent is null")
            return
        }

        if (requestCode == Constants.REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            Timber.d("Participants successfully added")
            val contactsData: List<String>? =
                intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
            if (contactsData != null) {
                InviteToChatRoomListener(this).inviteToChat(chatHandle, contactsData)
            }
        } else {
            Timber.e("Error adding participants")
        }

        @Suppress("deprecation")
        super.onActivityResult(requestCode, resultCode, intent)
    }

    /**
     * Shows rename group dialog
     *
     * @param fromGetLink True, if it's from link. False, otherwise.
     */
    fun showRenameGroupDialog(fromGetLink: Boolean) {
        Timber.d("fromGetLink: %s", fromGetLink)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        if (fromGetLink) {
            val alertRename = TextView(this)
            alertRename.text = getString(R.string.message_error_set_title_get_link)
            val paramsText = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            paramsText.setMargins(
                Util.scaleWidthPx(24, outMetrics),
                Util.scaleHeightPx(8, outMetrics),
                Util.scaleWidthPx(12, outMetrics),
                0
            )
            alertRename.layoutParams = paramsText

            layout.addView(alertRename)
            params.setMargins(
                Util.scaleWidthPx(20, outMetrics),
                Util.scaleHeightPx(8, outMetrics),
                Util.scaleWidthPx(17, outMetrics),
                0
            )
        } else {
            params.setMargins(
                Util.scaleWidthPx(20, outMetrics),
                Util.scaleHeightPx(16, outMetrics),
                Util.scaleWidthPx(17, outMetrics),
                0
            )
        }

        val input = EmojiEditText(this)
        layout.addView(input, params)
        input.apply {
            setOnLongClickListener { false }
            setSingleLine()
            setSelectAllOnFocus(true)
            groupChatInfoActivity?.let { activity ->
                setTextColor(getThemeColor(activity, android.R.attr.textColorSecondary))
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setEmojiSize(Util.dp2px(Constants.EMOJI_SIZE.toFloat(), outMetrics))
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
            filters =
                arrayOf<InputFilter>(
                    InputFilter.LengthFilter(
                        ChatUtil.getMaxAllowed(
                            ChatUtil.getTitleChat(
                                chat
                            )
                        )
                    )
                )
            setText(ChatUtil.getTitleChat(chat))
        }

        val builder = MaterialAlertDialogBuilder(this)
        input.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                changeTitle(input)
            } else {
                Timber.d("Other IME%s", actionId)
            }
            false
        }
        input.setImeActionLabel(getString(R.string.context_rename), EditorInfo.IME_ACTION_DONE)

        builder.setTitle(R.string.context_rename)
            .setPositiveButton(getString(R.string.context_rename), null)
            .setNegativeButton(android.R.string.cancel, null)
            .setView(layout)
            .setOnDismissListener {
                Util.hideKeyboard(
                    groupChatInfoActivity,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }

        changeTitleDialog = builder.create()
        changeTitleDialog?.apply {
            show()
            getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener { changeTitle(input) }
        }
    }

    private fun changeTitle(input: EmojiEditText) {
        val title = input.text.toString()
        when {
            title.isEmpty() -> {
                Timber.w("Input is empty")
                input.error = getString(R.string.invalid_string)
                input.requestFocus()
            }

            !ChatUtil.isAllowedTitle(title) -> {
                Timber.w("Title is too long")
                input.error = getString(R.string.title_long)
                input.requestFocus()
            }

            else -> {
                Timber.d("Positive button pressed - change title")
                chatC?.changeTitle(chatHandle, title)
                changeTitleDialog?.dismiss()
            }
        }
    }

    /**
     * Shows dialogue to confirm leaving chat
     */
    fun showConfirmationLeaveChat() {
        MaterialAlertDialogBuilder(this).setTitle(
            if (chat?.isMeeting == true) {
                resources.getString(R.string.meetings_leave_meeting_confirmation_dialog_title)
            } else {
                resources.getString(R.string.title_confirmation_leave_group_chat)
            }
        ).setMessage(getString(R.string.confirmation_leave_group_chat))
            .setPositiveButton(R.string.general_leave) { _: DialogInterface?, _: Int -> notifyShouldLeaveChat() }
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, null).show()
    }

    /**
     * Method to show the End call for all dialog
     */
    fun showEndCallForAllDialog() {
        if (isAlertDialogShown(endCallForAllDialog)) return

        endCallForAllDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.meetings_chat_screen_dialog_title_end_call_for_all))
            .setMessage(getString(R.string.meetings_chat_screen_dialog_description_end_call_for_all))
            .setNegativeButton(R.string.meetings_chat_screen_dialog_negative_button_end_call_for_all) { _: DialogInterface?, _: Int ->
                dismissAlertDialogIfExists(endCallForAllDialog)
            }
            .setPositiveButton(R.string.meetings_chat_screen_dialog_positive_button_end_call_for_all) { _: DialogInterface?, _: Int ->
                dismissAlertDialogIfExists(endCallForAllDialog)
                viewModel.endCallForAll()
            }
            .show()
    }

    /**
     * Sends a broadcast to leave the chat from Chat activity and finishes.
     */
    private fun notifyShouldLeaveChat() =
        chat?.let { chatRoom ->
            viewModel.launchBroadcastLeaveChat(chatRoom.chatId)
            finish()
        }

    /**
     * Invite new contact
     *
     * @param email The user email
     */
    fun inviteContact(email: String?) =
        ContactController(this).inviteContact(email)

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {

    }

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {

    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        Timber.d("onRequestFinish CHAT: %d %d", request.type, e.errorCode)

        when (request.type) {
            MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS -> {
                Timber.d("Permissions changed")
                var index = -1
                var participantToUpdate: MegaChatParticipant? = null
                Timber.d("Participants count: %s", participantsCount)
                for (i in 0 until participantsCount) {
                    if (request.userHandle == participants[i.toInt()]?.handle) {
                        participantToUpdate = participants[i.toInt()]
                        participantToUpdate?.privilege = request.privilege
                        index = i.toInt()
                        break
                    }
                }

                if (index != -1 && participantToUpdate != null) {
                    participants[index] = participantToUpdate
                    adapter?.updateParticipant(index, participants)
                }
            }

            MegaChatRequest.TYPE_ARCHIVE_CHATROOM -> {
                val chatHandle = request.chatHandle
                val chat = megaChatApi.getChatRoom(chatHandle)
                var chatTitle = ChatUtil.getTitleChat(chat)
                if (chatTitle == null) {
                    chatTitle = ""
                } else if (chatTitle.isNotEmpty() && chatTitle.length > MAX_LENGTH_CHAT_TITLE) {
                    chatTitle = chatTitle.substring(0, 59) + "..."
                }

                if (chatTitle.isNotEmpty() && chat.isGroup && !chat.hasCustomTitle()) {
                    chatTitle = "\"" + chatTitle + "\""
                }

                if (e.errorCode == MegaChatError.ERROR_OK) {
                    if (request.flag) {
                        Timber.d("Chat archived")
                        viewModel.launchBroadcastChatArchived(chatTitle)
                        finish()
                    } else {
                        Timber.d("Chat unarchived")
                        showSnackbar(getString(R.string.success_unarchive_chat, chatTitle))
                    }
                } else if (request.flag) {
                    Timber.e("ERROR WHEN ARCHIVING CHAT %s", e.errorString)
                    showSnackbar(getString(R.string.error_archive_chat, chatTitle))
                } else {
                    Timber.e("ERROR WHEN UNARCHIVING CHAT %s", e.errorString)
                    showSnackbar(getString(R.string.error_unarchive_chat, chatTitle))
                }

                updateAdapterHeader()
            }

            MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM -> {
                Timber.d("Remove participant: %s", request.userHandle)
                Timber.d("My user handle: %s", megaChatApi.myUserHandle)

                if (e.errorCode == MegaChatError.ERROR_OK) {
                    if (request.userHandle == MegaApiJava.INVALID_HANDLE) {
                        Timber.d("I left the chatroom")
                        finish()
                    } else {
                        Timber.d("Removed from chat")
                        megaChatApi.getChatRoom(chatHandle)?.let { chatRoom ->
                            chat = chatRoom
                            Timber.d("Peers after onChatListItemUpdate: %s", chatRoom.peerCount)
                        }
                        updateParticipants()
                        showSnackbar(getString(R.string.remove_participant_success))
                    }
                } else if (request.userHandle == -1L) {
                    Timber.e("ERROR WHEN LEAVING CHAT%s", e.errorString)
                    showSnackbar("Error.Chat not left")
                } else {
                    Timber.e("ERROR WHEN TYPE_REMOVE_FROM_CHATROOM %s", e.errorString)
                    showSnackbar(getString(R.string.remove_participant_error))
                }
            }

            MegaChatRequest.TYPE_EDIT_CHATROOM_NAME -> {
                Timber.d("Change title")
                if (e.errorCode == MegaChatError.ERROR_OK) {
                    if (request.text != null) {
                        updateAdapterHeader()
                    }
                } else {
                    Timber.e("ERROR WHEN TYPE_EDIT_CHATROOM_NAME %s", e.errorString)
                }
            }

            MegaChatRequest.TYPE_CREATE_CHATROOM -> {
                Timber.d("Create chat request finish!!!")
                if (e.errorCode == MegaChatError.ERROR_OK) {
                    Timber.d("Open new chat")
                    navigator.openChat(
                        context = this,
                        chatId = request.chatHandle,
                        action = Constants.ACTION_CHAT_SHOW_MESSAGES
                    )
                } else {
                    Timber.e("ERROR WHEN CREATING CHAT %s", e.errorString)
                    showSnackbar(getString(R.string.create_chat_error))
                }
            }

            MegaChatRequest.TYPE_CHAT_LINK_HANDLE -> {
                Timber.d("MegaChatRequest.TYPE_CHAT_LINK_HANDLE finished!!!")
                if (!request.flag) {
                    if (request.numRetry == 0) {
                        when (e.errorCode) {
                            MegaChatError.ERROR_OK -> {
                                chatLink = request.text
                                showGetChatLinkPanel()
                                return
                            }

                            MegaChatError.ERROR_ARGS -> {
                                Timber.e("The chatroom isn't group or public")
                            }

                            MegaChatError.ERROR_NOENT -> {
                                Timber.e("The chatroom doesn't exist or the chatId is invalid")
                            }

                            MegaChatError.ERROR_ACCESS -> {
                                Timber.e("The chatroom doesn't have a topic or the caller isn't an operator")
                            }

                            else -> {
                                Timber.e("Error TYPE_CHAT_LINK_HANDLE %s", e.errorCode)
                            }
                        }

                        chat?.let { chatRoom ->
                            if (chatRoom.ownPrivilege == MegaChatRoom.PRIV_MODERATOR) {
                                if (chatRoom.hasCustomTitle()) {
                                    megaChatApi.createChatLink(chatHandle, groupChatInfoActivity)
                                } else {
                                    showRenameGroupDialog(true)
                                }
                            } else {
                                showSnackbar(getString(R.string.no_chat_link_available))
                            }
                        }
                    } else if (request.numRetry == 1) {
                        if (e.errorCode == MegaChatError.ERROR_OK) {
                            chatLink = request.text
                            showGetChatLinkPanel()
                        } else {
                            Timber.e("Error TYPE_CHAT_LINK_HANDLE %s", e.errorCode)
                            showSnackbar(getString(R.string.general_error) + ": " + e.errorString)
                        }
                    }
                } else {
                    if (request.numRetry == 0) {
                        Timber.d("Removing chat link")
                        if (e.errorCode == MegaChatError.ERROR_OK) {
                            chatLink = null
                            showSnackbar(getString(R.string.chat_link_deleted))
                        } else {
                            when (e.errorCode) {
                                MegaChatError.ERROR_ARGS -> {
                                    Timber.e("The chatroom isn't group or public")
                                }

                                MegaChatError.ERROR_NOENT -> {
                                    Timber.e("The chatroom doesn't exist or the chatId is invalid")
                                }

                                MegaChatError.ERROR_ACCESS -> {
                                    Timber.e("The chatroom doesn't have a topic or the caller isn't an operator")
                                }

                                else -> {
                                    Timber.e("Error TYPE_CHAT_LINK_HANDLE %s", e.errorCode)
                                }
                            }

                            showSnackbar(getString(R.string.general_error) + ": " + e.errorString)
                        }
                    }
                }

                updateAdapterHeader()
            }

            MegaChatRequest.TYPE_SET_PRIVATE_MODE -> {
                Timber.d("MegaChatRequest.TYPE_SET_PRIVATE_MODE finished!!!")
                when (e.errorCode) {
                    MegaChatError.ERROR_OK -> {
                        chatLink = null
                        Timber.d("Chat is PRIVATE now")
                        updateAdapterHeader()
                        return
                    }

                    MegaChatError.ERROR_ARGS -> {
                        Timber.e("NOT public chatroom")
                    }

                    MegaChatError.ERROR_NOENT -> {
                        Timber.e("Chatroom not FOUND")
                    }

                    MegaChatError.ERROR_ACCESS -> {
                        Timber.e("NOT privileges or private chatroom")
                    }
                }
                showSnackbar(getString(R.string.general_error) + ": " + e.errorString)
            }
        }
    }

    /**
     * Shows snackBar
     */
    fun showSnackbar(s: String?) = s?.let { showSnackbar(binding.fragmentContainerGroupChat, it) }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish %s", request.requestString)
        when (request.type) {
            MegaRequest.TYPE_INVITE_CONTACT -> {
                Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.number)
                if (request.number == MegaContactRequest.INVITE_ACTION_REMIND.toLong()) {
                    showSnackbar(getString(R.string.context_contact_invitation_resent))
                } else {
                    when (e.errorCode) {
                        MegaError.API_OK -> {
                            Timber.d("OK INVITE CONTACT: %s", request.email)
                            if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong()) {
                                showSnackbar(
                                    getString(
                                        R.string.context_contact_request_sent,
                                        request.email
                                    )
                                )
                            }
                            return
                        }

                        MegaError.API_EEXIST -> showSnackbar(
                            getString(
                                R.string.context_contact_already_invited,
                                request.email
                            )
                        )

                        else -> {
                            if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong() && e.errorCode == MegaError.API_EARGS) {
                                showSnackbar(getString(R.string.error_own_email_as_contact))
                            } else {
                                showSnackbar(getString(R.string.general_error) + ": " + e.errorString)
                            }
                        }
                    }
                    Timber.e("ERROR: %d___%s", e.errorCode, e.errorString)
                }
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {

    }

    private fun onChatListItemUpdate(item: ChatListItem) {
        if (item.chatId != chatHandle) {
            return
        }

        Timber.d("Chat ID: ${item.chatId}")
        chat = megaChatApi.getChatRoom(chatHandle)

        when (item.changes) {
            ChatListItemChanges.Participants -> {
                Timber.d("Change participants")
                updateParticipants()
            }

            ChatListItemChanges.OwnPrivilege -> {
                updateAdapterHeader()
                updateParticipants()
                invalidateOptionsMenu()
            }

            ChatListItemChanges.Title, ChatListItemChanges.UpdatePreviewers -> {
                updateAdapterHeader()
            }

            else -> {
                Timber.d("Changes other: ${item.changes}")
            }
        }
    }

    private fun onChatOnlineStatusUpdate(
        userHandle: Long,
        newStatus: UserChatStatus,
        inProgress: Boolean,
    ) {
        var status = newStatus
        Timber.d("User Handle: $userHandle, Status: ${status.name}, inProgress: $inProgress")
        if (inProgress) {
            status = UserChatStatus.Invalid
        }

        if (userHandle == megaChatApi.myUserHandle) {
            Timber.d("My own status update")
            val position = participants.size - 1
            adapter?.updateContactStatus(position)
        } else {
            Timber.d("Status update for the user: $userHandle")
            var indexToReplace = -1
            val itrReplace: ListIterator<MegaChatParticipant?> = participants.listIterator()
            while (itrReplace.hasNext()) {
                val participant = itrReplace.next() ?: break

                if (participant.handle == userHandle) {
                    if (status != UserChatStatus.Online && status != UserChatStatus.Busy && status != UserChatStatus.Invalid) {
                        Timber.d("Request last green for user")
                        megaChatApi.requestLastGreen(userHandle, this)
                    } else {
                        participant.lastGreen = ""
                    }
                    indexToReplace = itrReplace.nextIndex() - 1
                    break
                }
            }

            if (indexToReplace != -1) {
                Timber.d("Index to replace: $indexToReplace")
                adapter?.updateContactStatus(indexToReplace)
            }
        }
    }

    private fun onChatConnectionStateUpdate(chatId: Long, newState: ChatConnectionStatus) {
        Timber.d("Chat ID: $chatId, New state: ${newState.name}")
        megaChatApi.getChatRoom(chatId)?.let { chatRoom ->
            if (CallUtil.isChatConnectedInOrderToInitiateACall(
                    if (newState == ChatConnectionStatus.Online) {
                        MegaChatApi.CHAT_CONNECTION_ONLINE
                    } else {
                        -1
                    },
                    chatRoom
                ) && CallUtil.canCallBeStartedFromContactOption(
                    this,
                )
            ) {
                startCall()
            }
        }
    }

    /**
     * Shows dialog to confirm making the chat private
     */
    fun showConfirmationPrivateChatDialog() {
        Timber.d("showConfirmationPrivateChatDialog")
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        val inflater = this.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_chat_link_options, null)
        dialogBuilder.setView(dialogView)

        val actionButton = dialogView.findViewById<Button>(R.id.chat_link_button_action)
        actionButton.text = getString(R.string.general_enable)

        val title = dialogView.findViewById<TextView>(R.id.chat_link_title)
        title.text = getString(R.string.make_chat_private_option)

        val text = dialogView.findViewById<TextView>(R.id.text_chat_link)
        text.text = getString(R.string.context_make_private_chat_warning_text)

        val secondText = dialogView.findViewById<TextView>(R.id.second_text_chat_link)
        secondText.visibility = View.GONE

        chatLinkDialog = dialogBuilder.create()

        actionButton.setOnClickListener {
            chatLinkDialog?.dismiss()
            chat?.let {
                if (it.peerCount + 1 > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
                    showSnackbar(getString(R.string.warning_make_chat_private))
                } else {
                    megaChatApi.setPublicChatToPrivate(chatHandle, groupChatInfoActivity)
                }
            }
        }

        chatLinkDialog?.show()
    }

    /**
     * Start conversation with participant
     *
     * @param handle The user handle.
     */
    fun startConversation(handle: Long) {
        Timber.d("Handle: %s", handle)
        val chat = megaChatApi.getChatRoomByUser(handle)
        val peers = MegaChatPeerList.createInstance()

        if (chat == null) {
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD)
            megaChatApi.createChat(false, peers, this)
        } else {
            navigator.openChat(
                context = this,
                chatId = chat.chatId,
                action = Constants.ACTION_CHAT_SHOW_MESSAGES
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return

        when (requestCode) {
            Constants.REQUEST_RECORD_AUDIO -> if (CallUtil.checkCameraPermission(this)) {
                startCall()
            }

            Constants.REQUEST_CAMERA -> startCall()
        }
    }

    /**
     * Start a call
     */
    fun startCall() {
        val audio = PermissionUtils.hasPermissions(this, Manifest.permission.RECORD_AUDIO)
        viewModel.onCallTap(
            userHandle = userWaitingForCall,
            video = false,
            audio = audio,
        )
    }

    private fun onChatPresenceLastGreen(userHandle: Long, lastGreen: Int) {
        Timber.d("User Handle: %d, Last green: %d", userHandle, lastGreen)
        val state = megaChatApi.getUserOnlineStatus(userHandle)

        if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
            val formattedDate = TimeUtils.lastGreenDate(this, lastGreen)
            if (userHandle != megaChatApi.myUserHandle) {
                Timber.d("Status last green for the user: %s", userHandle)
                val itrReplace: ListIterator<MegaChatParticipant?> = participants.listIterator()

                while (itrReplace.hasNext()) {
                    val participant = itrReplace.next() ?: break
                    if (participant.handle == userHandle) {
                        participant.lastGreen = formattedDate
                        adapter?.updateContactStatus(itrReplace.nextIndex() - 1)
                        break
                    }
                }
            }

            Timber.d("Date last green: %s", formattedDate)
        }
    }

    /**
     * Stores a MegaChatParticipant with their position in the adapter.
     *
     * @param position    position of the participant in the adapter.
     * @param participant MegaChatParticipant to store.
     */
    fun addParticipantRequest(position: Int, participant: MegaChatParticipant) {
        pendingParticipantRequests[position] = participant
    }

    /**
     * If there are visible participants in the UI without attributes,
     * it launches a request to ask for them.
     *
     * @param scrollState current scroll state of the RecyclerView
     */
    private fun checkIfShouldAskForUsersAttributes(scrollState: Int) {
        if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
            countDownTimer?.cancel()
            countDownTimer = null
            return
        }

        if (pendingParticipantRequests.isEmpty()) return

        countDownTimer = object : CountDownTimer(TIMEOUT.toLong(), TIMEOUT.toLong()) {

            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                val copyOfPendingParticipantRequests = HashMap(pendingParticipantRequests)
                val handleList = MegaHandleList.createInstance()
                val participantRequests = HashMap<Int, MegaChatParticipant>()
                val participantAvatars = HashMap<Int, String>()
                linearLayoutManager?.let { layoutManager ->
                    val firstPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastPosition = layoutManager.findLastVisibleItemPosition()

                    for (i in firstPosition..lastPosition) {
                        val participant = copyOfPendingParticipantRequests[i]
                        if (participant != null) {
                            participantRequests[i] = participant
                            val handle = participant.handle

                            if (participant.isEmpty) {
                                handleList.addMegaHandle(handle)
                            }

                            if (!participant.hasAvatar()) {
                                participantAvatars[i] = MegaApiAndroid.userHandleToBase64(handle)
                            }
                        }
                        pendingParticipantRequests.remove(i)
                    }

                    copyOfPendingParticipantRequests.clear()
                    requestUserAttributes(handleList, participantRequests, participantAvatars)
                }
            }
        }.start()
    }

    /**
     * Requests the attributes of some MegaChatParticipants
     *
     * @param handleList          MegaHandleList in which the participant's handles are stored to ask for their attributes
     * @param participantRequests HashMap in which the participants and their positions in the adapter are stored
     * @param participantAvatars  HastMap in which the participants' handles and their positions in the adapter are stored
     */
    private fun requestUserAttributes(
        handleList: MegaHandleList,
        participantRequests: HashMap<Int, MegaChatParticipant>,
        participantAvatars: HashMap<Int, String>,
    ) {
        if (handleList.size() > 0) {
            megaChatApi.loadUserAttributes(
                chatHandle,
                handleList,
                GetPeerAttributesListener(this, participantRequests)
            )
        }

        for (positionInAdapter in participantAvatars.keys) {
            val handle = participantAvatars[positionInAdapter]
            if (!TextUtil.isTextEmpty(handle)) {
                megaApi.getUserAvatar(
                    handle,
                    buildAvatarFile(handle + FileUtil.JPG_EXTENSION)?.absolutePath,
                    GetAttrUserListener(this, positionInAdapter)
                )
            }
        }
    }

    /**
     * Updates in the adapter the requested participants in loadUserAttributes().
     *
     * @param chatHandle         identifier of the current MegaChatRoom
     * @param participantUpdates list of requested participants
     * @param handleList         list of the participants' identifiers
     */
    fun updateParticipants(
        chatHandle: Long,
        participantUpdates: HashMap<Int?, MegaChatParticipant>,
        handleList: MegaHandleList,
    ) {
        if (chatHandle != chat?.chatId || megaChatApi.getChatRoom(chatHandle) == null) return

        chat = megaChatApi.getChatRoom(chatHandle)

        for (i in 0 until handleList.size()) {
            val handle = handleList[i]
            chatC?.setNonContactAttributesInDB(handle)

            for (positionInAdapter in participantUpdates.keys) {
                positionInAdapter?.let { pos ->
                    if (participantUpdates[pos]?.handle == handle) {
                        adapter?.getParticipantPositionInArray(pos)?.let { positionInArray ->
                            if (positionInArray >= 0 && positionInArray < participants.size && participants[positionInArray]?.handle == handle
                            ) {
                                participantUpdates[pos]?.let { participant ->
                                    participant.email = chatC?.getParticipantEmail(handle)
                                    participant.fullName = chatC?.getParticipantFullName(handle)
                                    chat?.let { chatRoom ->
                                        participant.privilege =
                                            chatRoom.getPeerPrivilegeByHandle(handle)
                                    }

                                    if (hasParticipantAttributes(participant)) {
                                        participants[positionInArray] = participant
                                        adapter?.updateParticipant(
                                            positionInArray,
                                            participants
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the participant's avatar in the adapter.
     *
     * @param positionInAdapter participant's position in the adapter
     * @param emailOrHandle     participant's email or handle in Base64
     */
    fun updateParticipantAvatar(positionInAdapter: Int, emailOrHandle: String) {
        val isEmail = Constants.EMAIL_ADDRESS.matcher(emailOrHandle).matches()

        adapter?.getParticipantPositionInArray(positionInAdapter)?.let { positionInArray ->
            if (positionInArray >= 0 && positionInArray < participants.size && (isEmail && participants[positionInArray]?.email == emailOrHandle
                        || participants[positionInArray]?.handle == MegaApiJava.base64ToUserHandle(
                    emailOrHandle
                ))
            ) {
                AvatarUtil.getAvatarBitmap(emailOrHandle)?.let {
                    adapter?.notifyItemChanged(positionInAdapter)
                }
            }
        }
    }

    /**
     * Updates a participant in the participants' list.
     *
     * @param position    position of the participant in the list
     * @param participant MegaChatParticipant to update
     */
    fun updateParticipant(position: Int, participant: MegaChatParticipant?) {
        participants[position] = participant
    }

    /**
     * Checks if a participant has attributes.
     * If so, the mail and full name do not have to be empty.
     *
     * @param participant MegaChatParticipant to check.
     * @return True if the participant was correctly updated, false otherwise.
     */
    fun hasParticipantAttributes(participant: MegaChatParticipant?): Boolean =
        !TextUtil.isTextEmpty(participant?.email) || !TextUtil.isTextEmpty(participant?.fullName)

    private fun updateAdapterHeader() = adapter?.notifyItemChanged(0)

    /**
     * Update participants list
     */
    fun updateParticipants() = megaChatApi.getChatRoom(chatHandle)?.let { chatRoom ->
        chat = chatRoom
        participants.clear()
        setParticipants()
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) =
        showSnackbar(type, binding.fragmentContainerGroupChat, content, chatId)

    /**
     * Receive changes to OnChatListItemUpdate, OnChatOnlineStatusUpdate, OnChatConnectionStateUpdate and and OnChatPresenceLastGreen make the necessary changes
     */
    private fun checkChatChanges() {

        collectFlow(monitorChatListItemUpdates()) {
            onChatListItemUpdate(it)
        }

        collectFlow(monitorChatOnlineStatusUseCase()) {
            onChatOnlineStatusUpdate(it.userHandle, it.status, it.inProgress)
        }

        collectFlow(monitorChatOnlineStatusUseCase()) {
            onChatOnlineStatusUpdate(it.userHandle, it.status, it.inProgress)
        }

        collectFlow(monitorChatConnectionStateUseCase()) {
            onChatConnectionStateUpdate(it.chatId, it.chatConnectionStatus)
        }

        collectFlow(monitorChatPresenceLastGreenUpdatesUseCase()) {
            onChatPresenceLastGreen(it.handle, it.lastGreen)
        }
    }

    fun getChatRoom() = megaChatApi.getChatRoom(chatHandle)

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        chat?.let {
            outState.putLong(Constants.CHAT_ID, it.chatId)
        }

        outState.putBoolean(END_CALL_FOR_ALL_DIALOG, isAlertDialogShown(endCallForAllDialog))
    }

    /**
     * Open manage chat history
     */
    fun openManageChatHistory(chatId: Long) {
        navigator.openManageChatHistoryActivity(
            context = this,
            chatId = chatId
        )
    }

    companion object {
        private const val TIMEOUT = 300
        private const val MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100
        private const val MAX_LENGTH_CHAT_TITLE = 60
        private const val END_CALL_FOR_ALL_DIALOG = "isEndCallForAllDialogShown"
    }

}
