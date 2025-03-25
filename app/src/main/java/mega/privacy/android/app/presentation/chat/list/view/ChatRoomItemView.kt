package mega.privacy.android.app.presentation.chat.list.view

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.core.graphics.toColorInt
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.GroupChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.IndividualChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.MessageText
import mega.privacy.android.shared.original.core.ui.controls.chip.IconBadge
import mega.privacy.android.shared.original.core.ui.controls.chip.CounterBadge
import mega.privacy.android.shared.original.core.ui.controls.chip.BadgeSize
import mega.privacy.android.shared.original.core.ui.controls.meetings.CallChronometer
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect
import mega.privacy.android.icon.pack.R as iconR

/**
 * Chat room item view
 *
 * @param item                  [ChatRoomItem]
 * @param isSelected
 * @param isSelectionEnabled
 * @param onItemClick
 * @param onItemMoreClick
 * @param onItemSelected
 */
@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatRoomItemView(
    item: ChatRoomItem,
    isSelected: Boolean,
    isSelectionEnabled: Boolean,
    onItemClick: (Long) -> Unit,
    onItemMoreClick: (ChatRoomItem) -> Unit,
    onItemSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val hasOngoingCall = item.hasOngoingCall()
    val isLoading = item.lastTimestampFormatted.isNullOrBlank()
    val isPending = item is MeetingChatRoomItem && item.isPendingMeeting()
    val callDurationFromInitialTimestamp = item.getDurationFromInitialTimestamp()
    val shouldShownCallDuration =
        item.hasCallInProgress() && callDurationFromInitialTimestamp != null

    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(MaterialTheme.colors.surface)
            .combinedClickable(
                onClick = {
                    if (isSelectionEnabled) {
                        hapticFeedback.performHapticFeedback(LongPress)
                        onItemSelected(item.chatId)
                    } else {
                        onItemClick(item.chatId)
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(LongPress)
                    onItemSelected(item.chatId)
                },
            )
            .indication(
                interactionSource = MutableInteractionSource(),
                indication = ripple(bounded = true),
            ),
    ) {
        val (
            avatarImage,
            titleText,
            statusIcon,
            privateIcon,
            muteIcon,
            recurringIcon,
            callIcon,
            lastMessageIcon,
            middleText,
            durationChrono,
            bottomText,
            moreButton,
            unreadCountIcon,
        ) = createRefs()

        if (isSelected) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_select_contact),
                contentDescription = "Selected item",
                modifier = Modifier
                    .testTag("chat_room_item:selected_image")
                    .size(40.dp)
                    .constrainAs(avatarImage) {
                        start.linkTo(parent.start, 16.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
            )
        } else {
            ChatAvatarView(
                avatars = item.getChatAvatars(),
                modifier = Modifier
                    .testTag("chat_room_item:avatar_image")
                    .size(40.dp)
                    .constrainAs(avatarImage) {
                        start.linkTo(parent.start, 16.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.textColorPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .testTag("chat_room_item:title_text")
                .constrainAs(titleText) {
                    linkTo(avatarImage.end, parent.end, 16.dp, 74.dp, 72.dp, 0.dp, 0f)
                    top.linkTo(parent.top)
                    bottom.linkTo(middleText.top)
                    width = Dimension.preferredWrapContent
                }
                .shimmerEffect(isLoading, CircleShape),
        )

        if (!isLoading) {
            val userChatStatus = if (item is IndividualChatRoomItem) item.userChatStatus else null
            ChatUserStatusView(
                userChatStatus = userChatStatus,
                modifier = Modifier
                    .testTag("chat_room_item:status_icon")
                    .constrainAs(statusIcon) {
                        start.linkTo(titleText.end, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                        visibility =
                            if (userChatStatus != null) Visibility.Visible else Visibility.Gone
                    },
            )

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_key_02),
                contentDescription = "Private chat icon",
                tint = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                modifier = Modifier
                    .testTag("chat_room_item:private_icon")
                    .size(16.dp)
                    .constrainAs(privateIcon) {
                        start.linkTo(statusIcon.end, 4.dp, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                        visibility =
                            if (item.isPublicChat()) Visibility.Gone else Visibility.Visible
                    },
            )

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_bell_off),
                contentDescription = "Mute chat icon",
                tint = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                modifier = Modifier
                    .testTag("chat_room_item:mute_icon")
                    .size(16.dp)
                    .constrainAs(muteIcon) {
                        start.linkTo(privateIcon.end, 2.dp, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                        visibility = if (item.isMuted) Visibility.Visible else Visibility.Gone
                    },
            )

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_rotate_cw),
                contentDescription = "Recurring meeting icon",
                tint = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                modifier = Modifier
                    .testTag("chat_room_item:recurring_icon")
                    .size(16.dp)
                    .constrainAs(recurringIcon) {
                        start.linkTo(muteIcon.end, 2.dp, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                        visibility = if (item.isRecurringMeeting())
                            Visibility.Visible
                        else
                            Visibility.Gone
                    },
            )

            IconBadge(
                imageVector = ImageVector.vectorResource(id = iconR.drawable.ic_phone_01_medium_thin_solid),
                contentDescription = "Ongoing call icon",
                size = BadgeSize.Small,
                modifier = Modifier
                    .testTag("chat_room_item:call_icon")
                    .constrainAs(callIcon) {
                        start.linkTo(recurringIcon.end, 2.dp, 4.dp)
                        top.linkTo(titleText.top)
                        bottom.linkTo(titleText.bottom)
                        visibility = if (hasOngoingCall)
                            Visibility.Visible
                        else
                            Visibility.Gone
                    },
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_chat_audio),
            contentDescription = "Last message audio icon",
            tint = MaterialTheme.colors.textColorSecondary,
            modifier = Modifier
                .testTag("chat_room_item:last_message_icon")
                .size(16.dp)
                .constrainAs(lastMessageIcon) {
                    start.linkTo(parent.start, 72.dp)
                    top.linkTo(titleText.bottom)
                    bottom.linkTo(bottomText.top)
                    visibility =
                        if (!isLoading && item.isLastMessageVoiceClip)
                            Visibility.Visible
                        else
                            Visibility.Gone
                },
        )

        MiddleTextView(
            modifier = Modifier
                .testTag(TEST_TAG_MIDDLE_TEXT)
                .constrainAs(middleText) {
                    linkTo(
                        start = lastMessageIcon.end,
                        end = unreadCountIcon.start,
                        startMargin = 2.dp,
                        endMargin = 8.dp,
                        startGoneMargin = 72.dp,
                        endGoneMargin = 6.dp,
                        bias = 0f
                    )
                    top.linkTo(titleText.bottom)
                    bottom.linkTo(bottomText.top, 5.dp)
                    width = Dimension.preferredWrapContent
                }
                .padding(vertical = if (isLoading) 2.dp else 0.dp)
                .shimmerEffect(isLoading, CircleShape),
            lastMessage = item.lastMessage,
            isPending = isPending,
            scheduledTimestamp = if (item is MeetingChatRoomItem) item.scheduledTimestampFormatted else null,
            highlight = item.highlight,
            shouldShownCallDuration = shouldShownCallDuration,
            isRecurringDaily = item is MeetingChatRoomItem && item.isRecurringDaily,
            isRecurringWeekly = item is MeetingChatRoomItem && item.isRecurringWeekly,
            isRecurringMonthly = item is MeetingChatRoomItem && item.isRecurringMonthly,
            isNormalMessage = item.isLastMessageNormal
        )

        if (shouldShownCallDuration) {
            CallChronometer(
                modifier = Modifier
                    .testTag(if (isPending) TEST_TAG_BOTTOM_TEXT_CALL_CHRONOMETER else TEST_TAG_MIDDLE_TEXT_CALL_CHRONOMETER)
                    .constrainAs(durationChrono) {
                        start.linkTo(if (isPending) bottomText.end else middleText.end)
                        top.linkTo(if (isPending) bottomText.top else middleText.top)
                        bottom.linkTo(if (isPending) bottomText.bottom else middleText.bottom)
                        width = Dimension.preferredWrapContent
                    }
                    .shimmerEffect(isLoading, CircleShape),
                duration = callDurationFromInitialTimestamp,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary)
            )
        }

        BottomTextView(
            modifier = Modifier
                .testTag(TEST_TAG_BOTTOM_TEXT)
                .constrainAs(bottomText) {
                    linkTo(
                        start = parent.start,
                        end = unreadCountIcon.start,
                        startMargin = 72.dp,
                        endMargin = 8.dp,
                        startGoneMargin = 72.dp,
                        endGoneMargin = 6.dp,
                        bias = 0f
                    )
                    top.linkTo(middleText.bottom, 4.dp)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.preferredWrapContent
                }
                .shimmerEffect(isLoading, CircleShape),
            isRecurring = item is MeetingChatRoomItem && item.isRecurring(),
            isPending = isPending,
            highlight = item.highlight,
            lastTimestamp = item.lastTimestampFormatted,
            shouldShownCallDuration = shouldShownCallDuration,
            lastMessage = item.lastMessage,
        )

        IconButton(
            onClick = {
                if (isSelectionEnabled) {
                    hapticFeedback.performHapticFeedback(LongPress)
                    onItemSelected(item.chatId)
                } else {
                    onItemMoreClick(item)
                }
            },
            modifier = Modifier
                .testTag("chat_room_item:more_button")
                .size(24.dp)
                .constrainAs(moreButton) {
                    end.linkTo(parent.end, 16.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    visibility = if (!isLoading)
                        Visibility.Visible
                    else
                        Visibility.Gone
                },
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_more),
                contentDescription = "See more Icon",
                tint = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
            )
        }

        CounterBadge(
            count = item.unreadCount,
            size = BadgeSize.Normal,
            modifier = Modifier
                .testTag("chat_room_item:unread_count_icon")
                .constrainAs(unreadCountIcon) {
                    end.linkTo(moreButton.start, 8.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    visibility = if (!isLoading && item.unreadCount > 0)
                        Visibility.Visible
                    else
                        Visibility.Gone
                },
        )

        createVerticalChain(
            titleText,
            middleText,
            bottomText,
            chainStyle = ChainStyle.Packed,
        )
    }
}

@Composable
private fun MiddleTextView(
    modifier: Modifier,
    lastMessage: String?,
    isPending: Boolean,
    scheduledTimestamp: String?,
    highlight: Boolean,
    shouldShownCallDuration: Boolean,
    isRecurringDaily: Boolean,
    isRecurringWeekly: Boolean,
    isRecurringMonthly: Boolean,
    isNormalMessage: Boolean,
) {
    val textMessage = when {
        isPending && !scheduledTimestamp.isNullOrBlank() ->
            when {
                isRecurringDaily -> stringResource(
                    R.string.meetings_list_scheduled_meeting_daily_label,
                    scheduledTimestamp
                )

                isRecurringWeekly -> stringResource(
                    R.string.meetings_list_scheduled_meeting_weekly_label,
                    scheduledTimestamp
                )

                isRecurringMonthly -> stringResource(
                    R.string.meetings_list_scheduled_meeting_monthly_label,
                    scheduledTimestamp
                )

                else -> scheduledTimestamp
            }

        !isPending && shouldShownCallDuration ->
            "$lastMessage · "

        else ->
            lastMessage
    }

    val textColor = when {
        isPending -> TextColor.Secondary
        highlight -> TextColor.Accent
        else -> TextColor.Primary
    }

    if (textMessage.isNullOrBlank() || !isNormalMessage) {
        MegaText(
            text = textMessage ?: stringResource(R.string.error_message_unrecognizable),
            textColor = textColor,
            style = MaterialTheme.typography.subtitle2,
            overflow = LongTextBehaviour.Ellipsis(maxLines = 1),
            modifier = modifier
        )
    } else {
        MessageText(
            message = textMessage,
            style = MaterialTheme.typography.subtitle2,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

@Composable
private fun BottomTextView(
    modifier: Modifier,
    isRecurring: Boolean,
    isPending: Boolean,
    highlight: Boolean,
    lastTimestamp: String?,
    shouldShownCallDuration: Boolean,
    lastMessage: String?,
) {
    val textColor: Color
    var textMessage: String?
    when {
        isPending && highlight -> {
            textColor = MaterialTheme.colors.secondary
            textMessage = lastMessage
        }

        isPending && !highlight -> {
            textColor = MaterialTheme.colors.textColorSecondary
            textMessage = if (isRecurring) {
                stringResource(R.string.meetings_list_recurring_meeting_label)
            } else {
                stringResource(R.string.meetings_list_upcoming_meeting_label)
            }
        }

        else -> {
            textColor = MaterialTheme.colors.textColorSecondary
            textMessage = lastTimestamp
        }
    }

    if (isPending && !textMessage.isNullOrBlank() && shouldShownCallDuration)
        textMessage = "$textMessage · "

    Text(
        text = textMessage ?: stringResource(R.string.error_message_unrecognizable),
        color = textColor,
        style = MaterialTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@CombinedThemeComponentPreviews
@Composable
private fun PreviewIndividualChatRoomItem(
    @PreviewParameter(ChatRoomItemProvider::class) itemToSelected: Pair<ChatRoomItem, Boolean>,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatRoomItemView(
            item = itemToSelected.first,
            isSelected = itemToSelected.second,
            isSelectionEnabled = itemToSelected.second,
            onItemClick = {},
            onItemMoreClick = {},
            onItemSelected = {},
        )
    }
}


class ChatRoomItemProvider : PreviewParameterProvider<Pair<ChatRoomItem, Boolean>> {
    override val values = sequenceOf(
        IndividualChatRoomItem(
            chatId = 1L,
            title = "Mieko Kawakami",
            peerEmail = "mieko@miekokawakami.jp",
            lastMessage = "Call ended",
            lastTimestampFormatted = "Monday 14:25",
            unreadCount = 5,
            highlight = false,
            avatar = ChatAvatarItem("M", color = "#FEBC00".toColorInt()),
            isMuted = false,
        ) to false,
        GroupChatRoomItem(
            chatId = 2L,
            title = "Recipe test #14",
            lastMessage = "Anna: Seeya all soon!",
            avatars = listOf(
                ChatAvatarItem("A", color = "#FEBC00".toColorInt()),
                ChatAvatarItem("J", color = "#B965C1".toColorInt())
            ),
            lastTimestampFormatted = "1 May 2022 17:53",
            unreadCount = 150,
            currentCallStatus = ChatRoomItemStatus.NotJoined,
            highlight = true,
            isMuted = true,
            isPublic = false,
        ) to false,
        MeetingChatRoomItem(
            chatId = 3L,
            schedId = 1L,
            title = "Photos Meeting #1325",
            lastMessage = "Anna: Seeya all soon!",
            avatars = listOf(
                ChatAvatarItem("C", color = "#009372".toColorInt()),
                ChatAvatarItem("L", color = "#FF8F00".toColorInt())
            ),
            lastTimestampFormatted = "1 May 2022 17:53",
            scheduledStartTimestamp = 100L,
            scheduledEndTimestamp = 200L,
            scheduledTimestampFormatted = "10:00pm - 11:00pm",
            unreadCount = 0,
            isPending = true,
            isRecurringWeekly = true,
            isMuted = true,
            isPublic = true,
            header = "Monday, 23 May"
        ) to true,
        GroupChatRoomItem(
            chatId = 1L,
            title = "Photos Meeting #1325",
            isPublic = true,
        ) to false
    )
}

/**
 * Test tag for call chronometer in middle text
 */
const val TEST_TAG_MIDDLE_TEXT_CALL_CHRONOMETER = "chat_room_item:middle_text_call_chronometer"

/**
 * Test tag for call chronometer in bottom text
 */
const val TEST_TAG_BOTTOM_TEXT_CALL_CHRONOMETER = "chat_room_item:bottom_text_call_chronometer"

/**
 * Test tag for middle text
 */
const val TEST_TAG_MIDDLE_TEXT = "chat_room_item:middle_text"

/**
 * Test tag for bottom text
 */
const val TEST_TAG_BOTTOM_TEXT = "chat_room_item:bottom_text"
