package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Avatar message
 */
abstract class AvatarMessage : UiChatMessage {

    /**
     * Content composable
     */
    @Composable
    abstract fun RowScope.ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean
    )

    abstract override val message: TypedMessage
    
    /**
     * Avatar composable
     */
    @Composable
    open fun RowScope.MessageAvatar(lastUpdatedCache: Long, modifier: Modifier) {
        if (showAvatar) {
            ChatAvatar(
                modifier = modifier.align(Alignment.Bottom),
                handle = userHandle,
                lastUpdatedCache = lastUpdatedCache
            )
        } else {
            Spacer(modifier = modifier)
        }
    }

    /**
     * Show avatar
     */
    abstract val showAvatar: Boolean

    @Composable
    override fun MessageListItem(
        state: UIMessageState,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = shouldDisplayForwardIcon,
            reactions = reactions,
            onMoreReactionsClick = { onMoreReactionsClicked(id) },
            onReactionClick = { onReactionClicked(id, it, reactions) },
            onReactionLongClick = { onReactionLongClick(it, reactions) },
            onForwardClicked = { onForwardClicked(message) },
            time = this.getTimeOrNull(timeFormatter),
            avatarOrIcon = { avatarModifier ->
                MessageAvatar(
                    lastUpdatedCache = state.lastUpdatedCache,
                    avatarModifier,
                )
            },
            content = { interactionEnabled ->
                ContentComposable(onLongClick, interactionEnabled)
            },
            isSelectMode = state.isInSelectMode,
            isSelected = state.isChecked,
            onSelectionChanged = {},
        )
    }

    override val isSelectable = true

    override fun key() = super.key() + "_${showAvatar}"
}