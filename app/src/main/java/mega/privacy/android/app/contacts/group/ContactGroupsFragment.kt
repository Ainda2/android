package mega.privacy.android.app.contacts.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.group.adapter.ContactGroupsAdapter
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.FragmentContactGroupsBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment that represents the UI showing the list of contact groups for the current user.
 */
@AndroidEntryPoint
class ContactGroupsFragment : Fragment() {
    @Inject
    lateinit var navigator: MegaNavigator

    private lateinit var binding: FragmentContactGroupsBinding

    private val viewModel by viewModels<ContactGroupsViewModel>()
    private val groupsAdapter by lazy { ContactGroupsAdapter(::openGroupChatScreen) }
    private lateinit var createGroupChatLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createGroupChatLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val intent = result.data
                val resultCode = result.resultCode
                if (resultCode != Activity.RESULT_OK || intent == null) {
                    Timber.w("Error creating chat")
                    return@registerForActivityResult
                }

                val contactsData =
                    intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                val isGroup =
                    intent.getBooleanExtra(AddContactActivity.EXTRA_GROUP_CHAT, false)

                if (contactsData == null || !isGroup) {
                    Timber.w("Is one to one chat or no contacts selected")
                    return@registerForActivityResult
                }

                val chatTitle =
                    intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE)

                val allowAddParticipants =
                    intent.getBooleanExtra(AddContactActivity.ALLOW_ADD_PARTICIPANTS, false)

                viewModel.getGroupChatRoom(contactsData, chatTitle, allowAddParticipants)
                    .observe(viewLifecycleOwner) { chatId ->
                        if (chatId == MEGACHAT_INVALID_HANDLE) {
                            (requireActivity() as SnackbarShower).showSnackbar(
                                getString(R.string.create_chat_error)
                            )
                        } else {
                            openGroupChatScreen(chatId)
                        }
                    }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentContactGroupsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_contact_search, menu)
        menu.findItem(R.id.action_search)?.setupSearchView { query ->
            viewModel.setQuery(query)
        }
    }

    override fun onDestroyView() {
        binding.list.clearOnScrollListeners()
        super.onDestroyView()
    }

    private fun setupView() {
        binding.list.adapter = groupsAdapter
        binding.list.setHasFixedSize(true)
        binding.list.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.contact_list_divider,
                        null
                    )!!
                )
            }
        )
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                (activity as ContactsActivity?)?.showElevation(showElevation)
            }
        })
        binding.listScroller.setRecyclerView(binding.list)

        binding.btnCreateGroup.setOnClickListener {
            val intent = Intent(
                requireContext(),
                AddContactActivity::class.java
            ).apply {
                putExtra(AddContactActivity.EXTRA_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
                putExtra(AddContactActivity.EXTRA_ONLY_CREATE_GROUP, true)
            }
            intentToCreateGroupChat(intent)
        }

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
            .formatColorTag(requireContext(), 'B', R.color.grey_900_grey_100)
            .toSpannedHtmlText()
    }

    /**
     * Launches an Intent to open create group chat room screen.
     */
    private fun intentToCreateGroupChat(intent: Intent) {
        createGroupChatLauncher.launch(intent)
    }

    private fun setupObservers() {
        viewModel.getGroups().observe(viewLifecycleOwner, ::showGroups)
    }

    /**
     * Show a list of Contact Group View Items on the current RecyclerView.
     *
     * @param items Contact Group View Items to be shown
     */
    private fun showGroups(items: List<ContactGroupItem>) {
        binding.listScroller.isVisible = items.size >= MIN_ITEMS_SCROLLBAR
        binding.viewEmpty.isVisible = items.isNullOrEmpty()
        groupsAdapter.submitList(items)
    }

    /**
     * Open group chat screen
     *
     * @param groupId   Group Id to be opened
     */
    private fun openGroupChatScreen(groupId: Long) {
        navigator.openChat(
            context = requireActivity(),
            chatId = groupId,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
        )
    }
}
