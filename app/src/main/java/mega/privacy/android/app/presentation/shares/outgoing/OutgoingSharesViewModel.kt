package mega.privacy.android.app.presentation.shares.outgoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to OutgoingSharesFragment
 */
@HiltViewModel
class OutgoingSharesViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getOutgoingSharesChildrenNode: GetOutgoingSharesChildrenNode,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(OutgoingSharesState())

    /** public UI state */
    val state: StateFlow<OutgoingSharesState> = _state

    init {
        viewModelScope.launch {
            refreshNodes()?.let { setNodes(it) }
        }
    }

    /**
     * Refresh outgoing shares node
     */
    fun refreshOutgoingSharesNode() = viewModelScope.launch {
        refreshNodes()?.let { setNodes(it) }
    }

    /**
     * Decrease by 1 the outgoing tree depth
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun decreaseOutgoingTreeDepth(handle: Long) = viewModelScope.launch {
        setOutgoingTreeDepth(_state.value.outgoingTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the outgoing tree depth
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun increaseOutgoingTreeDepth(handle: Long) = viewModelScope.launch {
        setOutgoingTreeDepth(_state.value.outgoingTreeDepth + 1, handle)
    }

    /**
     * Reset outgoing tree depth to initial value
     */
    fun resetOutgoingTreeDepth() = viewModelScope.launch {
        setOutgoingTreeDepth(0, -1L)
    }

    /**
     * Set outgoing tree depth with given value
     * If refresh nodes return null, fallback to root node, else display empty list
     *
     *
     * @param depth the tree depth value to set
     * @param handle the id of the current outgoing parent handle to set
     */
    private fun setOutgoingTreeDepth(depth: Int, handle: Long) = viewModelScope.launch {
        _state.update {
            refreshNodes(handle)?.let { nodes ->
                it.copy(
                    nodes = nodes,
                    outgoingTreeDepth = depth,
                    outgoingParentHandle = handle,
                    isInvalidParentHandle = isInvalidParentHandle(handle)
                )
            } ?: run {
                it.copy(
                    nodes = emptyList(),
                    outgoingTreeDepth = 0,
                    outgoingParentHandle = -1L,
                    isInvalidParentHandle = true
                )
            }
        }
    }

    /**
     * Set the current nodes displayed
     *
     * @param nodes the list of nodes to set
     */
    private fun setNodes(nodes: List<MegaNode>) {
        _state.update { it.copy(nodes = nodes) }
    }

    /**
     * Refresh the list of nodes from api
     *
     * @param handle
     */
    private suspend fun refreshNodes(handle: Long = _state.value.outgoingParentHandle): List<MegaNode>? {
        Timber.d("refreshOutgoingSharesNodes")
        return getOutgoingSharesChildrenNode(handle)
    }

    /**
     * Check if the parent handle is valid
     *
     * @param handle
     * @return true if the parent handle is valid
     */
    private suspend fun isInvalidParentHandle(handle: Long = _state.value.outgoingParentHandle): Boolean {
        return handle
            .takeUnless { it == -1L || it == MegaApiJava.INVALID_HANDLE }
            ?.let { getNodeByHandle(it) == null }
            ?: true
    }


}