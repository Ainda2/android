package mega.privacy.android.app.main.managerSections

import android.content.DialogInterface
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.fragments.managerFragments.actionMode.TransfersActionBarCallBack
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.MegaTransfersAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.adapters.SelectModeInterface
import mega.privacy.android.app.presentation.transfers.page.TransferPageFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import mega.privacy.android.domain.entity.transfer.Transfer
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

/**
 * The Fragment is used for displaying the transfer list.
 */
@AndroidEntryPoint
internal class LegacyTransfersFragment : TransfersBaseFragment(), SelectModeInterface,
    TransfersActionBarCallBack.TransfersActionCallback {

    private val adapter: MegaTransfersAdapter by lazy(LazyThreadSafetyMode.NONE) {
        MegaTransfersAdapter(
            context = requireActivity(),
            listView = binding.transfersListView,
            selectModeInterface = this,
            transfersViewModel = viewModel,
            onPauseTransfer = ::handlePauseTransfers
        )
    }

    private var actionMode: ActionMode? = null

    private val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(
        initItemTouchHelperCallback(
            dragDirs = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return initView(inflater, container)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        getSafeAdapter()?.let {
            outState.putIntegerArrayList(
                SELECTED_ITEMS,
                ArrayList(it.selectedItems)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.transfersEmptyText.text = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.transfers_empty_new)
        )
        val activeTransfers = viewModel.getActiveTransfers()
        setEmptyView(activeTransfers.size)

        setupFlow()

        val selectedItems =
            savedInstanceState?.getIntegerArrayList(SELECTED_ITEMS).orEmpty().toSet()
        binding.transfersListView.let { recyclerView ->
            adapter.submitList(activeTransfers)
            recyclerView.adapter = adapter
            recyclerView.itemAnimator = noChangeRecyclerViewItemAnimator()
        }

        if (selectedItems.isNotEmpty()) {
            activateActionMode()
            adapter.restoreSelectedItem(selectedItems)
        } else {
            enableDragAndDrop()
        }
    }

    private fun handlePauseTransfers(transfer: Transfer) {
        viewModel.pauseOrResumeTransfer(transfer)
    }

    /**
     * Check whether is in select mode after changing tab or drawer item.
     */
    fun destroyActionModeIfNeed() {
        getSafeAdapter()?.run {
            if (isMultipleSelect()) {
                destroyActionMode()
            }
        }
    }

    override fun onCreateActionMode() = updateElevation()

    override fun onDestroyActionMode() {
        clearSelections()
        getSafeAdapter()?.hideMultipleSelect()
        updateElevation()
    }

    override fun cancelTransfers() {
        getSafeAdapter()?.run {
            showConfirmationCancelSelectedTransfers(getSelectedTransfers())
        }
    }

    override fun selectAll() {
        getSafeAdapter()?.selectAll()
    }

    override fun clearSelections() {
        getSafeAdapter()?.clearSelections()
    }

    override fun getSelectedTransfers() = getSafeAdapter()?.getSelectedItemsCount() ?: 0

    override fun areAllTransfersSelected() = getSafeAdapter()?.run {
        getSelectedItemsCount() == itemCount
    } == true

    override fun hideTabs(hide: Boolean) {
        (parentFragment as? TransferPageFragment)?.hideTab(hide)
    }

    override fun destroyActionMode() {
        actionMode?.finish()

        enableDragAndDrop()
    }

    override fun notifyItemChanged() = updateActionModeTitle()

    override fun getAdapter(): RotatableAdapter? = getSafeAdapter()

    override fun activateActionMode() {
        getSafeAdapter()?.let {
            if (!it.isMultipleSelect()) {
                it.setMultipleSelect(true)
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    TransfersActionBarCallBack(this)
                )
                updateActionModeTitle()
                disableDragAndDrop()
            }
        }
    }

    override fun multipleItemClick(position: Int) {
        getSafeAdapter()?.toggleSelection(position)
    }

    override fun updateActionModeTitle() {
        if (actionMode != null && activity != null && getSafeAdapter() != null) {
            val count = adapter.getSelectedItemsCount()
            val title: String = if (count == 0) {
                getString(R.string.title_select_transfers)
            } else {
                count.toString()
            }
            actionMode?.title = title
            actionMode?.invalidate()
        } else {
            Timber.w("RETURN: null values")
        }
    }

    override fun updateElevation() {
        if (bindingIsInitialized()) {
            (requireActivity() as ManagerActivity).changeAppBarElevation(
                binding.transfersListView.canScrollVertically(DEFAULT_SCROLL_DIRECTION) ||
                        getSafeAdapter()?.isMultipleSelect() == true
            )
        }
    }


    private fun setupFlow() {
        viewModel.activeState.flowWithLifecycle(
            viewLifecycleOwner.lifecycle,
            Lifecycle.State.CREATED
        ).onEach { transfersState ->
            when (transfersState) {
                is ActiveTransfersState.TransferMovementFinishedUpdated -> {
                    if (transfersState.success.not()) {
                        (requireActivity() as ManagerActivity).showSnackbar(
                            SNACKBAR_TYPE,
                            getString(
                                R.string.change_of_transfer_priority_failed,
                                transfersState.newTransfers[transfersState.pos].fileName
                            ),
                            MEGACHAT_INVALID_HANDLE
                        )
                    }
                }

                is ActiveTransfersState.TransferFinishedUpdated -> {
                    val transfers = transfersState.newTransfers
                    Timber.d("new transfer is ${transfers.joinToString { it.fileName }}")
                    if (transfers.isEmpty()) {
                        getSafeAdapter()?.submitList(emptyList())
                        activateActionMode()
                        destroyActionMode()
                        requireActivity().invalidateOptionsMenu()
                    }
                }

                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.collectFlow(viewModel.activeTransfer.sample(500L)) {
            getSafeAdapter()?.submitList(it)
            setEmptyView(it.size)
        }

        viewLifecycleOwner.collectFlow(viewModel.areTransfersPaused) {
            getSafeAdapter()?.areTransfersPaused = it
        }

        viewLifecycleOwner.collectFlow(viewModel.uiState) { uiState ->
            uiState.pauseOrResumeTransferResult?.let { result ->
                if (result.isSuccess) {
                    viewModel.activeTransferChangeStatus(result.getOrThrow().tag)
                } else {
                    (activity as? ManagerActivity)?.showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.error_general_nodes),
                        -1
                    )
                }
                viewModel.markHandledPauseOrResumeTransferResult()
            }
            uiState.cancelTransfersResult?.let {
                if (it.isSuccess) {
                    requireActivity().invalidateOptionsMenu()
                } else {
                    (activity as? ManagerActivity)?.showSnackbar(
                        Constants.SNACKBAR_TYPE,
                        getString(R.string.error_general_nodes),
                        -1
                    )
                }
                viewModel.markHandledCancelTransfersResult()
            }
        }
    }

    private fun initItemTouchHelperCallback(
        dragDirs: Int,
        swipeDirs: Int = 0,
    ): ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
            private var addElevation = true
            private var resetElevation = false
            private var draggedTransfer: Transfer? = null
            private var newPosition = 0

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val posDragged = viewHolder.absoluteAdapterPosition
                newPosition = target.absoluteAdapterPosition

                if (draggedTransfer == null) {
                    draggedTransfer = viewModel.getActiveTransfer(posDragged)
                }
                viewModel.activeTransfersSwap(posDragged, newPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                if (addElevation) {
                    recyclerView.post {
                        binding.transfersListView.removeItemDecoration(itemDecoration)
                    }
                    val animator = viewHolder.itemView.animate()
                    viewHolder.itemView.translationZ = dp2px(2f, resources.displayMetrics).toFloat()
                    viewHolder.itemView.alpha = 0.95f
                    animator.start()

                    addElevation = false
                }

                if (resetElevation) {
                    recyclerView.post {
                        binding.transfersListView.addItemDecoration(itemDecoration)
                    }
                    val animator = viewHolder.itemView.animate()
                    viewHolder.itemView.translationZ = 0f
                    viewHolder.itemView.alpha = 1f
                    animator.start()

                    addElevation = true
                    resetElevation = false
                }
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ) {
                super.clearView(recyclerView, viewHolder)
                // Drag finished, elevation should be removed.
                resetElevation = true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                draggedTransfer?.let {
                    if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        startMovementRequest(it, newPosition)
                        draggedTransfer = null
                    }
                }
            }
        }

    /**
     * Launches the request to change the priority of a transfer.
     *
     * @param transfer    MegaTransfer to change its priority.
     * @param newPosition The new position on the list.
     */
    private fun startMovementRequest(transfer: Transfer, newPosition: Int) {
        viewModel.moveTransfer(transfer, newPosition)
    }

    private fun enableDragAndDrop() {
        itemTouchHelper.attachToRecyclerView(binding.transfersListView)
    }

    private fun disableDragAndDrop() =
        itemTouchHelper.attachToRecyclerView(null)

    /**
     * Refresh adapter
     */
    fun refresh() {
        getSafeAdapter()?.notifyDataSetChanged()
    }

    private fun showConfirmationCancelSelectedTransfers(selectedTransfers: List<Transfer>) {
        if (selectedTransfers.isEmpty()) return
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(
                resources.getQuantityString(
                    R.plurals.cancel_selected_transfers,
                    selectedTransfers.size
                )
            )
            .setPositiveButton(
                R.string.button_continue
            ) { _: DialogInterface?, _: Int ->
                viewModel.cancelTransfersByTag(selectedTransfers.map { it.tag })
                destroyActionMode()
            }
            .setNegativeButton(R.string.general_dismiss, null)
            .setCancelable(false)
            .show()
    }

    /**
     * Get the adapter safely
     */
    private fun getSafeAdapter() = if (bindingIsInitialized()) adapter else null

    companion object {
        private const val SELECTED_ITEMS = "SELECTED_ITEMS"

        /**
         * Generate a new instance for [LegacyTransfersFragment]
         *
         * @return new [LegacyTransfersFragment] instance
         */
        @JvmStatic
        fun newInstance(): LegacyTransfersFragment = LegacyTransfersFragment()
    }
}