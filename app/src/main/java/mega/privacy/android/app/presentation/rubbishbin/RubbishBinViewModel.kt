package mega.privacy.android.app.presentation.rubbishbin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import java.util.Stack
import javax.inject.Inject

/**
 * [ViewModel] class associated to RubbishBinFragment
 *
 * @param getRubbishBinChildrenNode [GetRubbishBinChildrenNode] Fetch the rubbish bin nodes
 * @param monitorNodeUpdates Monitor node updates
 * @param getRubbishBinParentNodeHandle [GetParentNodeHandle] Fetch parent handle
 * @param getRubbishBinChildren [GetRubbishBinChildren] Fetch Rubbish Bin [Node]
 * @param getNodeByHandle [GetNodeByHandle] Get MegaNode from Handle
 */
@HiltViewModel
class RubbishBinViewModel @Inject constructor(
    private val getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val getRubbishBinParentNodeHandle: GetParentNodeHandle,
    private val getRubbishBinChildren: GetRubbishBinChildren,
    private val getNodeByHandle: GetNodeByHandle,
) : ViewModel() {

    /**
     * The RubbishBin UI State
     */
    private val _state = MutableStateFlow(RubbishBinState())

    /**
     * The RubbishBin UI State accessible outside the ViewModel
     */
    val state: StateFlow<RubbishBinState> = _state

    /**
     * Stack to maintain folder navigation clicks
     */
    private val lastPositionStack = Stack<Int>()

    /**
     * Get current nodes when RubbishBinViewModel gets created
     *
     * Uses MonitorNodeUpdates to observe any Node updates
     *
     * A received Node update will refresh the list of nodes
     */
    init {
        viewModelScope.launch {
            refreshNodes()
            monitorNodeUpdates().collect {
                checkForDeletedNodes(it.changes)
            }
        }
    }

    /**
     * Set the current rubbish bin handle to the UI state
     *
     * @param handle the id of the current rubbish bin parent handle to set
     */
    fun setRubbishBinHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(rubbishBinHandle = handle) }
        refreshNodes()
    }

    /**
     * Retrieves the list of Nodes
     * Call the Use Case [getRubbishBinChildrenNode] to retrieve and return the list of Nodes
     *
     * @return a List of Inbox Nodes
     */
    fun refreshNodes() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    nodes = getRubbishBinChildrenNode(_state.value.rubbishBinHandle) ?: emptyList(),
                    parentHandle = getRubbishBinParentNodeHandle(_state.value.rubbishBinHandle),
                    nodeList = getNodeUiItems(getRubbishBinChildren(_state.value.rubbishBinHandle))
                )
            }
        }
    }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<Node>): List<NodeUIItem> {
        return nodeList.map {
            NodeUIItem(node = it, isSelected = false, isInvisible = false)
        }
    }

    /**
     * Handles back click of rubbishBinFragment
     */
    fun onBackPressed() {
        _state.value.parentHandle?.let {
            setRubbishBinHandle(it)
        }
    }

    /**
     * Pop scroll position for previous depth
     *
     * @return last position saved
     */
    fun popLastPositionStack(): Int = lastPositionStack.takeIf { it.isNotEmpty() }?.pop() ?: 0

    /**
     * Push lastPosition to stack
     * @param lastPosition last position to be added to stack
     */
    private fun pushPositionOnStack(lastPosition: Int) {
        lastPositionStack.push(lastPosition)
    }

    /**
     * Performs action when folder is clicked from adapter
     * @param lastFirstVisiblePosition visible position based on listview type
     * @param handle node handle
     */
    fun onFolderItemClicked(lastFirstVisiblePosition: Int, handle: Long) {
        pushPositionOnStack(lastFirstVisiblePosition)
        setRubbishBinHandle(handle)
        onItemPerformedClicked()
    }

    /**
     * When item is clicked on activity
     */
    fun onItemPerformedClicked() {
        _state.update {
            it.copy(
                megaNode = null,
                itemIndex = -1
            )
        }
    }

    /**
     * This will update handle for rubbishBin if any node is deleted from browser and
     * we are in same screen else will simply refresh nodes with parentID
     * if restored and we are inside folder, it will simply refresh rubbish node
     * @param changes [Map] of [Node], list of [NodeChanges]
     */
    private fun checkForDeletedNodes(changes: Map<Node, List<NodeChanges>>) {
        changes.forEach { (key, value) ->
            if (value.contains(NodeChanges.Remove) && _state.value.rubbishBinHandle == key.id.longValue) {
                setRubbishBinHandle(key.parentId.longValue)
                return@forEach
            } else if (value.contains(NodeChanges.Parent) && _state.value.rubbishBinHandle == key.id.longValue) {
                setRubbishBinHandle(-1)
                return@forEach
            }
        }
        refreshNodes()
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem) {
        val index =
            _state.value.nodeList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        if (_state.value.isInSelection) {
            updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
        } else {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        megaNode = getNodeByHandle(nodeUIItem.id.longValue),
                        itemIndex = index
                    )
                }
            }
        }
    }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem) {
        val index =
            _state.value.nodeList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        val newNodesList = _state.value.nodeList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedNodes = 1,
                nodeList = newNodesList,
                isInSelection = true
            )
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeInSelectionState(nodeUIItem: NodeUIItem, index: Int) {
        nodeUIItem.isSelected = !nodeUIItem.isSelected
        val totalSelectedNode = if (nodeUIItem.isSelected) {
            _state.value.selectedNodes + 1
        } else {
            _state.value.selectedNodes - 1
        }
        val newNodesList = _state.value.nodeList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedNodes = totalSelectedNode,
                nodeList = newNodesList
            )
        }
    }
}
