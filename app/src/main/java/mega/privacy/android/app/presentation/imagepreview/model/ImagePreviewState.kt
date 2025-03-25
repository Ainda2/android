package mega.privacy.android.app.presentation.imagepreview.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NameCollision

internal data class ImagePreviewState(
    val isInitialized: Boolean = false,
    val imageNodes: List<ImageNode> = emptyList(),
    val currentImageNode: ImageNode? = null,
    val currentImageNodeIndex: Int = 0,
    val isCurrentImageNodeAvailableOffline: Boolean = false,
    val showAppBar: Boolean = true,
    val inFullScreenMode: Boolean = false,
    val resultMessage: String = "",
    val copyMoveException: Throwable? = null,
    val nameCollision: NameCollision? = null,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val showDeletedMessage: Boolean = false,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isMagnifierMode: Boolean = false,
    val isBusinessAccountExpired: Boolean = false,
    val isOnline: Boolean = false,
)