package mega.privacy.android.app.contacts.list

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.SpaceBetweenAdaptersDecoration
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.list.adapter.ContactActionsListAdapter
import mega.privacy.android.app.contacts.list.adapter.ContactListAdapter
import mega.privacy.android.app.contacts.list.dialog.ContactBottomSheetDialogFragment
import mega.privacy.android.app.databinding.FragmentContactListBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.attach.createNodeAttachmentView
import mega.privacy.android.app.utils.AlertDialogUtil.createForceAppUpdateDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Fragment that represents the UI showing the list of contacts for the current user.
 */
@AndroidEntryPoint
class ContactListFragment : Fragment() {
    @Inject
    lateinit var navigator: MegaNavigator

    private lateinit var binding: FragmentContactListBinding

    private val viewModel by viewModels<ContactListViewModel>()
    private val actionsAdapter by lazy {
        ContactActionsListAdapter(::onRequestsClick, ::onGroupsClick)
    }
    private val recentlyAddedAdapter by lazy {
        ContactListAdapter(viewModel::getChatRoomId, ::onContactInfoClick, ::onContactMoreClick)
    }
    private val contactsAdapter by lazy {
        ContactListAdapter(viewModel::getChatRoomId, ::onContactInfoClick, ::onContactMoreClick)
    }

    private var contactSheet: ContactBottomSheetDialogFragment? = null

    private var forceAppUpdateDialog: AlertDialog? = null
    private val nodeAttachmentViewModel by viewModels<NodeAttachmentViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentContactListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        binding.root.addView(
            createNodeAttachmentView(
                activity = requireActivity(),
                viewModel = nodeAttachmentViewModel,
            ) { message, id ->
                (requireActivity() as? SnackbarShower)?.showSnackbarWithChat(message, id)
            }
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
        collectFlows()
    }

    /**
     * on Start
     */
    override fun onStart() {
        super.onStart()
        viewModel.monitorSFUServerUpgrade()
    }

    /**
     * on Stop
     */
    override fun onStop() {
        viewModel.cancelMonitorSFUServerUpgrade()
        super.onStop()
    }


    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(viewModel.state) { state ->
            if (state.showForceUpdateDialog) {
                showForceUpdateAppDialog()
            }

            state.shouldOpenChatWithId?.let { chatId ->
                navigator.openChat(
                    context = requireActivity(),
                    chatId = chatId,
                    action = Constants.ACTION_CHAT_SHOW_MESSAGES
                )

                contactSheet?.dismiss()

                viewModel.onChatOpened()

            }
            actionsAdapter.submitList(state.contactActionItems)
        }
    }

    /**
     * Show Force App Update Dialog
     */
    private fun showForceUpdateAppDialog() {
        if (forceAppUpdateDialog?.isShowing == true) return
        forceAppUpdateDialog = context?.let {
            createForceAppUpdateDialog(it) {
                viewModel.onForceUpdateDialogDismissed()
            }
        }
        forceAppUpdateDialog?.show()
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

    /**
     * Start call
     */
    fun startCall() {
        val audio =
            PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)
        viewModel.onCallTap(video = false, audio = audio)
    }

    private fun setupView() {
        val adapterConfig = ConcatAdapter.Config.Builder()
            .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build()
        binding.list.adapter =
            ConcatAdapter(adapterConfig, actionsAdapter, recentlyAddedAdapter, contactsAdapter)
        binding.list.addItemDecoration(
            SpaceBetweenAdaptersDecoration(
                addAtTheEndOfAdapter = ContactActionsListAdapter::class.java,
                spaceDp = 8
            )
        )
        binding.list.setHasFixedSize(true)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                (activity as ContactsActivity?)?.showElevation(showElevation)
            }
        })
        binding.listScroller.setRecyclerView(binding.list)

        binding.btnAddContact.setOnClickListener {
            navigator.openInviteContactActivity(
                requireContext(),
                false
            )
        }

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
            .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
            .toSpannedHtmlText()
    }

    private fun setupObservers() {


        viewModel.getRecentlyAddedContacts().observe(viewLifecycleOwner) { items ->
            recentlyAddedAdapter.submitList(items)
        }

        viewModel.getContactsWithHeaders().observe(viewLifecycleOwner) { items ->
            binding.listScroller.isVisible = items.size >= MIN_ITEMS_SCROLLBAR
            binding.viewEmpty.isVisible = items.isNullOrEmpty()
            contactsAdapter.submitList(items)
        }
    }

    private fun onContactInfoClick(userEmail: String) {
        ContactUtil.openContactInfoActivity(context, userEmail)
    }

    private fun onContactMoreClick(userHandle: Long) {
        contactSheet = ContactBottomSheetDialogFragment.newInstance(userHandle).apply {
            optionSendMessageClick = { handle ->
                viewModel.getChatRoomId(handle)
            }
        }

        contactSheet?.show(childFragmentManager)
    }

    private fun onRequestsClick() {
        findNavController().navigate(ContactListFragmentDirections.actionListToRequests())
    }

    private fun onGroupsClick() {
        findNavController().navigate(ContactListFragmentDirections.actionListToGroups())
    }
}
