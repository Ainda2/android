package mega.privacy.android.app.presentation.startconversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.findItemByHandle
import mega.privacy.android.app.data.extensions.replaceIfExists
import mega.privacy.android.app.data.extensions.sortList
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.presentation.startconversation.model.StartConversationState
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactDataUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.AddNewContactsUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import timber.log.Timber
import javax.inject.Inject

/**
 * StartConversationFragment view model.
 *
 * @property getVisibleContactsUseCase                      [GetVisibleContactsUseCase]
 * @property getContactDataUseCase                          [GetContactDataUseCase]
 * @property startConversationUseCase                       [StartConversationUseCase]
 * @property monitorContactUpdates                          [MonitorContactUpdates]
 * @property applyContactUpdates                            [ApplyContactUpdates]
 * @property monitorChatPresenceLastGreenUpdatesUseCase     [MonitorChatPresenceLastGreenUpdatesUseCase]
 * @property monitorChatOnlineStatusUseCase                 [MonitorChatOnlineStatusUseCase]
 * @property monitorContactRequestUpdatesUseCase                   [MonitorContactRequestUpdatesUseCase]
 * @property addNewContactsUseCase                          [AddNewContactsUseCase]
 * @property requestUserLastGreenUseCase                    [RequestUserLastGreenUseCase]
 * @property state                    Current view state as [StartConversationState]
 */
@HiltViewModel
class StartConversationViewModel @Inject constructor(
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getContactDataUseCase: GetContactDataUseCase,
    private val startConversationUseCase: StartConversationUseCase,
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val applyContactUpdates: ApplyContactUpdates,
    private val monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase,
    private val monitorChatOnlineStatusUseCase: MonitorChatOnlineStatusUseCase,
    private val monitorContactRequestUpdatesUseCase: MonitorContactRequestUpdatesUseCase,
    private val addNewContactsUseCase: AddNewContactsUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(StartConversationState())
    val state: StateFlow<StartConversationState> = _state

    private val isConnected =
        monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    internal val searchExpandedKey = "SEARCH_EXPANDED"
    internal val typedSearchKey = "TYPED_SEARCH"
    internal val fromChatKey = "FROM_CHAT"

    private val searchExpanded = savedStateHandle.getStateFlow(
        viewModelScope,
        searchExpandedKey,
        SearchWidgetState.COLLAPSED
    )

    private val typedSearch = savedStateHandle.getStateFlow(
        viewModelScope,
        typedSearchKey,
        ""
    )

    private val fromChat = savedStateHandle.getStateFlow(
        viewModelScope,
        fromChatKey,
        false
    )

    init {
        getApiFeatureFlag()
        observeStateChanges()
        getContacts()
        observeContactUpdates()
        observeLastGreenUpdates()
        observeOnlineStatusUpdates()
        observeNewContacts()
    }

    /**
     * Sets from chat state value.
     */
    fun setFromChat(fromChat: Boolean) {
        this.fromChat.update { fromChat }
    }

    private fun observeStateChanges() {
        viewModelScope.launch {
            merge(
                searchExpanded.map { widgetState ->
                    { state: StartConversationState ->
                        state.copy(
                            searchWidgetState = widgetState,
                            buttonsVisible = widgetState != SearchWidgetState.EXPANDED
                        )
                    }
                },
                typedSearch.map { typed ->
                    { state: StartConversationState ->
                        state.copy(
                            typedSearch = typed,
                            filteredContactList = getFilteredContactList(
                                contactList = state.contactItemList,
                                typedSearch = typed
                            )
                        )
                    }
                },
                fromChat.map { isFromChat ->
                    { state: StartConversationState -> state.copy(fromChat = isFromChat) }
                }
            ).collect {
                _state.update(it)
            }
        }
    }

    private fun getFilteredContactList(
        contactList: List<ContactItem>,
        typedSearch: String,
    ): List<ContactItem>? =
        if (typedSearch.isEmpty()) null
        else contactList.filter { (_, email, contactData) ->
            val filter = typedSearch.lowercase()

            email.lowercase().contains(filter)
                    || contactData.fullName?.lowercase()?.contains(filter) == true
                    || contactData.alias?.lowercase()?.contains(filter) == true
        }

    private fun getContacts() {
        viewModelScope.launch {
            val contactList = getVisibleContactsUseCase()
            _state.update {
                it.copy(
                    contactItemList = contactList,
                    emptyViewVisible = contactList.isEmpty(),
                    searchAvailable = contactList.isNotEmpty(),
                    filteredContactList = getFilteredContactList(
                        contactList = contactList,
                        typedSearch = typedSearch.value
                    )
                )
            }
            getContactsData(contactList)
        }
    }

    /**
     * Get note to yourself api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.NoteToYourselfFlag)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { flag ->
                _state.update { state ->
                    state.copy(
                        isNoteToYourselfFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    private suspend fun getContactsData(contactList: List<ContactItem>) {
        contactList.forEach { contactItem ->
            val contactData = getContactDataUseCase(contactItem)
            _state.value.contactItemList.apply {
                findItemByHandle(contactItem.handle)?.apply {
                    toMutableList().apply {
                        replaceIfExists(copy(contactData = contactData))
                        _state.update { it.copy(contactItemList = this.sortList()) }
                    }
                }
            }
        }
    }

    private fun observeContactUpdates() {
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { userUpdates ->
                val contactList = applyContactUpdates(_state.value.contactItemList, userUpdates)
                _state.update { it.copy(contactItemList = contactList) }
            }
        }
    }

    private fun observeLastGreenUpdates() {
        viewModelScope.launch {
            monitorChatPresenceLastGreenUpdatesUseCase().collectLatest { (handle, lastGreen) ->
                _state.value.contactItemList.apply {
                    findItemByHandle(handle)?.apply {
                        toMutableList().apply {
                            replaceIfExists(copy(lastSeen = lastGreen))
                            _state.update { it.copy(contactItemList = this.sortList()) }
                        }
                    }
                }
            }
        }
    }

    private fun observeOnlineStatusUpdates() {
        viewModelScope.launch {
            monitorChatOnlineStatusUseCase().collectLatest { (userHandle, status) ->
                if (status != UserChatStatus.Online) {
                    requestUserLastGreenUseCase(userHandle)
                }

                _state.value.contactItemList.apply {
                    findItemByHandle(userHandle)?.apply {
                        toMutableList().apply {
                            replaceIfExists(copy(status = status))
                            _state.update { it.copy(contactItemList = this.sortList()) }
                        }
                    }
                }
            }
        }
    }

    private fun observeNewContacts() {
        viewModelScope.launch {
            monitorContactRequestUpdatesUseCase().collectLatest { newContacts ->
                val contactList = addNewContactsUseCase(_state.value.contactItemList, newContacts)
                _state.update { it.copy(contactItemList = contactList.sortList()) }
            }
        }
    }

    /**
     * Sets the search expanded value.
     *
     * @param widgetState [SearchWidgetState].
     */
    fun updateSearchWidgetState(widgetState: SearchWidgetState) {
        searchExpanded.update { widgetState }
    }

    /**
     * Sets the typed search.
     *
     * @param newTypedText  New typed search.
     */
    fun setTypedSearch(newTypedText: String) {
        typedSearch.update { newTypedText }
    }

    /**
     * Starts a conversation if there is internet connection, shows an error if not.
     */
    fun onContactTap(contactItem: ContactItem) {
        if (isConnected.value) {
            viewModelScope.launch {
                runCatching {
                    startConversationUseCase(
                        isGroup = false,
                        userHandles = listOf(contactItem.handle)
                    )
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(result = -1L, error = R.string.general_text_error) }
                }.onSuccess { chatHandle -> _state.update { it.copy(result = chatHandle) } }
            }
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * On Start group conversation tap
     *
     * @param emails
     * @param title
     * @param addParticipants
     * @param isEkr
     * @param chatLink
     */
    fun onStartGroupConversation(
        emails: List<String>?,
        title: String?,
        addParticipants: Boolean,
        isEkr: Boolean,
        chatLink: Boolean,
    ) {
        if (!emails.isNullOrEmpty()) {
            viewModelScope.launch {
                runCatching {
                    createGroupChatRoomUseCase(
                        emails = emails,
                        title = title,
                        isEkr = isEkr,
                        addParticipants = addParticipants,
                        chatLink = chatLink,
                    )
                }.onSuccess { chatId ->
                    _state.update { it.copy(result = chatId) }
                }.onFailure(Timber.Forest::e)
            }
        } else {
            Timber.e("onStartGroupConversation() missing parameters")
        }
    }

    /**
     * Updates searchWidgetState as [SearchWidgetState.COLLAPSED]
     */
    fun onCloseSearchTap() {
        updateSearchWidgetState(SearchWidgetState.COLLAPSED)
        setTypedSearch("")
    }

    /**
     * Create or open note to self chat
     */
    fun onNoteToSelfTap() {
        Timber.e("Create or open note to self chat")
    }

    /**
     * Updates searchWidgetState as [SearchWidgetState.EXPANDED]
     */
    fun onSearchTap() {
        updateSearchWidgetState(SearchWidgetState.EXPANDED)
    }
}