package mega.privacy.android.app.main.managerSections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.main.adapters.MegaCompletedTransfersAdapter
import mega.privacy.android.app.modalbottomsheet.ManageTransferBottomSheetDialogFragment
import mega.privacy.android.app.presentation.transfers.page.TransferPageFragment
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.transfer.CompletedTransfer

/**
 * The Fragment is used for displaying the finished items of transfer.
 */
@AndroidEntryPoint
class CompletedTransfersFragment : TransfersBaseFragment() {
    private val adapter: MegaCompletedTransfersAdapter by lazy(LazyThreadSafetyMode.NONE) {
        MegaCompletedTransfersAdapter(
            context = requireActivity(),
            onShowTransferOptionPanel = ::showTransferOptionPanel
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return initView(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transfersEmptyText.text = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.completed_transfers_empty_new)
        )

        setupFlow()
        binding.transfersListView.adapter = adapter
    }

    private fun setupFlow() {
        viewLifecycleOwner.collectFlow(viewModel.failedTransfer) {
            requireActivity().invalidateOptionsMenu()
        }
        viewLifecycleOwner.collectFlow(viewModel.completedTransfers) { completedTransfers ->
            adapter.submitList(completedTransfers) {
                // List Adapter doesn't auto move to top when new item inserted, we need to use this callback to scroll to top
                binding.transfersListView.scrollToPosition(0)
            }
            setEmptyView(completedTransfers.size)
        }
    }

    private fun showTransferOptionPanel(completedTransfer: CompletedTransfer) {
        val id = completedTransfer.id
        if (childFragmentManager.findFragmentByTag(ManageTransferBottomSheetDialogFragment.TAG) != null || id == null) {
            return
        }
        ManageTransferBottomSheetDialogFragment.newInstance(id)
            .show(childFragmentManager, ManageTransferBottomSheetDialogFragment.TAG)
    }

    /**
     * Retry single transfer
     *
     * @param transfer
     */
    fun retrySingleTransfer(transfer: CompletedTransfer) {
        (parentFragment as? TransferPageFragment)?.retryTransfer(transfer)
    }

    companion object {

        /**
         * Generate a new instance for [CompletedTransfersFragment]
         *
         * @return new [CompletedTransfersFragment] instance
         */
        @JvmStatic
        fun newInstance(): CompletedTransfersFragment = CompletedTransfersFragment()
    }
}
