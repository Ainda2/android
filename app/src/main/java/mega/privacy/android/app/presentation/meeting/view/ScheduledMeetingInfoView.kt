package mega.privacy.android.app.presentation.meeting.view

import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.contact.view.ContactStatusView
import mega.privacy.android.app.presentation.contact.view.getLastSeenString
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoUiState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.view.dialog.DenyEntryToCallDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.WaitingRoomWarningDialog
import mega.privacy.android.app.presentation.meeting.view.menuaction.ScheduledMeetingInfoMenuAction
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.legacy.core.ui.controls.divider.CustomDivider
import mega.privacy.android.legacy.core.ui.controls.text.MarqueeText
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.red_300
import mega.privacy.android.shared.original.core.ui.theme.red_600
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Scheduled meeting info View
 */
@Composable
fun ScheduledMeetingInfoView(
    state: ScheduledMeetingInfoUiState,
    managementState: ScheduledMeetingManagementUiState,
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onSeeMoreOrLessClicked: () -> Unit,
    onLeaveGroupClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onDismiss: () -> Unit,
    onLeaveGroupDialog: () -> Unit,
    onInviteParticipantsDialog: () -> Unit,
    onCloseWarningClicked: () -> Unit,
    onResetStateSnackbarMessage: () -> Unit = {},
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    onParticipantClicked: (ChatParticipant) -> Unit = {},
) {
    val shouldShowParticipantsLimitWarning =
        managementState.isCallUnlimitedProPlanFeatureFlagEnabled &&
                state.shouldShowParticipantsLimitWarning && state.isModerator
    val listState = rememberLazyListState()
    val scaffoldState = rememberScaffoldState()

    val shouldShowWarningDialog =
        state.enabledAllowNonHostAddParticipantsOption && state.enabledWaitingRoomOption && state.isHost
                && managementState.waitingRoomReminder == WaitingRoomReminders.Enabled

    MegaScaffold(
        modifier = Modifier.navigationBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            ScheduledMeetingInfoAppBar(
                state = state,
                onEditClicked = onEditClicked,
                onAddParticipantsClicked = onAddParticipantsClicked,
                onBackPressed = onBackPressed,
                titleId = R.string.general_info,
            )
        },
        scrollableContentState = listState
    ) { paddingValues ->
        LeaveGroupAlertDialog(
            state = state,
            onDismiss = { onDismiss() },
            onLeave = { onLeaveGroupDialog() })

        AddParticipantsAlertDialog(
            state = state,
            onDismiss = { onDismiss() },
            onInvite = { onInviteParticipantsDialog() })

        UsersInWaitingRoomDialog()

        DenyEntryToCallDialog()

        Column {
            if (shouldShowWarningDialog) {
                WaitingRoomWarningDialog(
                    onCloseClicked = onCloseWarningClicked
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(paddingValues)
            ) {
                item(key = "Scheduled meeting title") {
                    ScheduledMeetingTitleView(state = state)
                }

                items(state.buttons) { button ->
                    ActionButton(
                        state = state,
                        action = button,
                        enabledMeetingLinkOption = managementState.enabledMeetingLinkOption,
                        isCallInProgress = managementState.isCallInProgress,
                        onButtonClicked = onButtonClicked
                    )
                }

                item(key = "Participants") { ParticipantsHeader(state = state) }

                if (shouldShowParticipantsLimitWarning) {
                    item(key = "Warning") {
                        ParticipantsLimitWarningComposeView(
                            state.isModerator,
                            modifier = Modifier.testTag(
                                SCHEDULE_MEETING_INFO_PARTICIPANTS_WARNING_TAG
                            ),
                        )
                    }
                }

                item(key = "Add participants") {
                    AddParticipantsButton(
                        state = state,
                        onAddParticipantsClicked = onAddParticipantsClicked
                    )
                }

                item(key = "Participants list") {
                    state.participantItemList.indices.forEach { i ->
                        if (i < 4 || !state.seeMoreVisible) {
                            val isLastOne =
                                state.participantItemList.size <= 4 && i == state.participantItemList.size - 1

                            ParticipantItemView(
                                participant = state.participantItemList[i],
                                !isLastOne, onParticipantClicked = onParticipantClicked
                            )
                        }
                    }

                    if (state.participantItemList.size > 4) {
                        SeeMoreOrLessParticipantsButton(
                            state,
                            onSeeMoreOrLessClicked = onSeeMoreOrLessClicked
                        )
                    }
                }

                item(key = "Scheduled meeting description") {
                    ScheduledMeetingDescriptionView(state = state)
                }

                item(key = "Leave group") {
                    LeaveGroupButton(onLeaveGroupClicked = onLeaveGroupClicked)
                }
            }
        }

        EventEffect(
            event = state.snackbarMsg, onConsumed = onResetStateSnackbarMessage
        ) {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(it)
        }
    }
}

/**
 * Scheduled meeting info Alert Dialog
 *
 * @param state                     [ScheduledMeetingInfoUiState]
 * @param onDismiss                 When dismiss the alert dialog
 * @param onLeave                   When leave the group chat room
 */
@Composable
private fun LeaveGroupAlertDialog(
    state: ScheduledMeetingInfoUiState,
    onDismiss: () -> Unit,
    onLeave: () -> Unit,
) {
    if (state.leaveGroupDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.meetings_leave_meeting_confirmation_dialog_title),
            text = stringResource(id = R.string.confirmation_leave_group_chat),
            confirmButtonText = stringResource(id = R.string.general_leave),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onConfirm = onLeave,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Scheduled meeting info Alert Dialog
 *
 * @param state                     [ScheduledMeetingInfoUiState]
 * @param onDismiss                 When dismiss the alert dialog
 * @param onInvite                  When invite participants to group chat room
 */
@Composable
private fun AddParticipantsAlertDialog(
    state: ScheduledMeetingInfoUiState,
    onDismiss: () -> Unit,
    onInvite: () -> Unit,
) {

    if (state.addParticipantsNoContactsDialog || state.addParticipantsNoContactsLeftToAddDialog) {
        ConfirmationDialog(
            title = stringResource(
                id = if (state.addParticipantsNoContactsDialog)
                    R.string.chat_add_participants_no_contacts_title
                else
                    R.string.chat_add_participants_no_contacts_left_to_add_title
            ),
            text = stringResource(
                id = if (state.addParticipantsNoContactsDialog)
                    R.string.chat_add_participants_no_contacts_message
                else
                    R.string.chat_add_participants_no_contacts_left_to_add_message
            ),
            confirmButtonText = stringResource(id = R.string.contact_invite),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onConfirm = onInvite,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Scheduled meeting info App bar view
 *
 * @param state                     [ScheduledMeetingInfoUiState]
 * @param onEditClicked             When edit option is clicked
 * @param onAddParticipantsClicked  When add participants option is clicked
 * @param onBackPressed             When on back pressed option is clicked
 * @param titleId                   Title id
 */
@Composable
private fun ScheduledMeetingInfoAppBar(
    state: ScheduledMeetingInfoUiState,
    onEditClicked: () -> Unit,
    onAddParticipantsClicked: () -> Unit,
    onBackPressed: () -> Unit,
    titleId: Int,
) {
    MegaAppBar(
        appBarType = AppBarType.BACK_NAVIGATION,
        title = stringResource(id = titleId),
        onNavigationPressed = onBackPressed,
        actions = buildList {
            if (state.isHost || state.isOpenInvite) {
                add(ScheduledMeetingInfoMenuAction.AddParticipants)
            }
            state.scheduledMeeting?.let { schedMeet ->
                if (state.isHost && !schedMeet.isPast()) {
                    add(ScheduledMeetingInfoMenuAction.EditMeeting)
                }
            }
        },
        onActionPressed = { action ->
            when (action) {
                ScheduledMeetingInfoMenuAction.AddParticipants -> onAddParticipantsClicked()
                ScheduledMeetingInfoMenuAction.EditMeeting -> onEditClicked()
            }
        },
    )
}

/**
 * Scheduled meeting info title view
 *
 * @param state [ScheduledMeetingInfoUiState]
 */
@Composable
private fun ScheduledMeetingTitleView(state: ScheduledMeetingInfoUiState) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent)
            ) {
                MeetingAvatar(state = state)
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.scheduledMeeting?.let {
                        it.title?.let { title ->
                            Text(text = title,
                                style = MaterialTheme.typography.subtitle1,
                                color = black.takeIf { isLight() } ?: white,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                ScheduledMeetingSubtitle(state = state)
            }
        }

        CustomDivider(withStartPadding = false)
    }
}

@Composable
private fun isLight(): Boolean = MaterialTheme.colors.isLight


/**
 * Scheduled meeting subtitle
 *
 * @param state [ScheduledMeetingInfoUiState]
 */
@Composable
private fun ScheduledMeetingSubtitle(state: ScheduledMeetingInfoUiState) {
    state.scheduledMeeting?.let { schedMeet ->
        if (schedMeet.isPast()) {
            Text(text = pluralStringResource(
                R.plurals.subtitle_of_group_chat,
                state.numOfParticipants,
                state.numOfParticipants
            ),
                style = MaterialTheme.typography.body1,
                color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        } else {
            val text = getRecurringMeetingDateTime(schedMeet, state.is24HourFormat)
            if (text.isNotEmpty()) {
                Text(text = text,
                    style = MaterialTheme.typography.subtitle2,
                    color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp)
            }
        }
    }
}


/**
 * Create meeting avatar view
 *
 * @param state [ScheduledMeetingInfoUiState]
 */
@Composable
private fun MeetingAvatar(state: ScheduledMeetingInfoUiState) {
    if (state.isEmptyMeeting()) {
        ChatAvatarView(
            avatarUri = null,
            avatarPlaceholder = state.chatTitle,
            avatarColor = null,
            modifier = Modifier.border(1.dp, Color.White, CircleShape)
        )
    } else if (state.isSingleMeeting()) {
        state.firstParticipant?.let { participant ->
            ChatAvatarView(
                avatarUri = participant.data.avatarUri,
                avatarPlaceholder = participant.getAvatarFirstLetter(),
                avatarColor = participant.defaultAvatarColor,
                avatarTimestamp = participant.avatarUpdateTimestamp,
                modifier = Modifier.border(1.dp, Color.White, CircleShape),
            )
        }
    } else if (state.firstParticipant != null && state.secondParticipant != null) {
        Box(
            Modifier.fillMaxSize()
        ) {
            ChatAvatarView(
                avatarUri = state.secondParticipant.data.avatarUri,
                avatarPlaceholder = state.secondParticipant.getAvatarFirstLetter(),
                avatarColor = state.secondParticipant.defaultAvatarColor,
                avatarTimestamp = state.secondParticipant.avatarUpdateTimestamp,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
                    .border(1.dp, Color.White, CircleShape)
            )
            ChatAvatarView(
                avatarUri = state.firstParticipant.data.avatarUri,
                avatarPlaceholder = state.firstParticipant.getAvatarFirstLetter(),
                avatarColor = state.firstParticipant.defaultAvatarColor,
                avatarTimestamp = state.firstParticipant.avatarUpdateTimestamp,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopStart)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Control and show the available buttons
 *
 * @param state             [ScheduledMeetingInfoUiState]
 * @param action            [ScheduledMeetingInfoAction]
 * @param onButtonClicked
 */
@Composable
private fun ActionButton(
    state: ScheduledMeetingInfoUiState,
    enabledMeetingLinkOption: Boolean,
    isCallInProgress: Boolean,
    action: ScheduledMeetingInfoAction,
    onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
) {
    Column(modifier = Modifier
        .testTag(ACTION_BUTTON_OPTION_TAG)
        .fillMaxWidth()
        .clickable {
            if (action != ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation && (action != ScheduledMeetingInfoAction.WaitingRoom || !isCallInProgress)) {
                onButtonClicked(action)
            }
        }) {
        when (action) {
            ScheduledMeetingInfoAction.ShareMeetingLink,
            ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts,
            -> {
                if (state.isPublic && enabledMeetingLinkOption) {
                    if (action == ScheduledMeetingInfoAction.ShareMeetingLink && state.isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 72.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 16.dp
                                ),
                                style = MaterialTheme.typography.button,
                                text = stringResource(id = action.title),
                                color = MaterialTheme.colors.secondary
                            )
                        }
                        CustomDivider(withStartPadding = true)
                    } else if (action == ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts && !state.isHost) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ActionOption(
                                state = state,
                                action = action,
                                isChecked = true,
                                hasSwitch = false
                            )
                        }
                        CustomDivider(withStartPadding = false)
                    }
                }
            }

            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation ->
                if (state.isHost && state.isPublic) {
                    Text(
                        modifier = Modifier.padding(
                            start = 14.dp,
                            end = 16.dp,
                            top = 18.dp
                        ),
                        style = MaterialTheme.typography.button,
                        text = stringResource(id = action.title),
                        color = MaterialTheme.colors.secondary
                    )

                    action.description?.let { description ->
                        Text(modifier = Modifier.padding(
                            start = 14.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 8.dp
                        ),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                    }

                    CustomDivider(withStartPadding = false)
                }

            ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation,
            -> if (state.isHost && !state.isPublic) {
                Text(modifier = Modifier.padding(
                    start = 14.dp,
                    end = 16.dp,
                    top = 18.dp
                ),
                    style = MaterialTheme.typography.subtitle1,
                    text = stringResource(id = action.title),
                    color = black.takeIf { isLight() } ?: white)

                action.description?.let { description ->
                    Text(modifier = Modifier.padding(
                        start = 14.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 8.dp
                    ),
                        style = MaterialTheme.typography.subtitle2,
                        text = stringResource(id = description),
                        color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                }

                CustomDivider(withStartPadding = false)
            }

            ScheduledMeetingInfoAction.MeetingLink,
            -> if (state.isHost && state.isPublic) {
                ActionOption(
                    state = state,
                    action = action,
                    isChecked = enabledMeetingLinkOption,
                    hasSwitch = true
                )
                CustomDivider(withStartPadding = true)
            }

            ScheduledMeetingInfoAction.AllowNonHostAddParticipants ->
                if (state.isHost) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = state.enabledAllowNonHostAddParticipantsOption,
                        hasSwitch = true
                    )
                    CustomDivider(withStartPadding = true)
                }

            ScheduledMeetingInfoAction.WaitingRoom -> {
                if (state.isHost) {
                    ActionOption(
                        state = state,
                        action = action,
                        isEnabled = !isCallInProgress,
                        isChecked = state.enabledWaitingRoomOption,
                        hasSwitch = true
                    )

                    action.description?.let { description ->
                        Text(modifier = Modifier.padding(
                            start = 72.dp,
                            end = 16.dp,
                            top = 2.dp,
                            bottom = 18.dp
                        ),
                            style = MaterialTheme.typography.subtitle2,
                            text = stringResource(id = description),
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                    }
                    CustomDivider(withStartPadding = true)
                }
            }

            ScheduledMeetingInfoAction.ManageChatHistory ->
                if (state.isHost) {
                    ActionOption(
                        state = state,
                        action = action,
                        isChecked = true,
                        hasSwitch = false
                    )
                    CustomDivider(withStartPadding = true)

                }

            ScheduledMeetingInfoAction.ChatNotifications -> {
                ActionOption(
                    state = state,
                    action = action,
                    isChecked = state.dndSeconds == null,
                    hasSwitch = true
                )
                CustomDivider(withStartPadding = true)
            }

            ScheduledMeetingInfoAction.ShareFiles -> {
                ActionOption(
                    state = state,
                    action = action,
                    isChecked = true,
                    hasSwitch = false
                )
                CustomDivider(withStartPadding = true)
            }
        }
    }
}

/**
 * Participants header view
 *
 * @param state [ScheduledMeetingInfoUiState]
 */
@Composable
private fun ParticipantsHeader(state: ScheduledMeetingInfoUiState) {
    Text(modifier = Modifier.padding(
        start = 16.dp,
        top = 17.dp,
        end = 16.dp,
        bottom = 12.dp
    ),
        text = stringResource(id = R.string.participants_number, state.participantItemList.size),
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Medium,
        color = black.takeIf { isLight() } ?: white)
}

/**
 * Add participants button view
 *
 * @param state [ScheduledMeetingInfoUiState]
 * @param onAddParticipantsClicked
 */
@Composable
private fun AddParticipantsButton(
    state: ScheduledMeetingInfoUiState,
    onAddParticipantsClicked: () -> Unit,
) {
    if (state.isHost || state.isOpenInvite) {
        Row(modifier = Modifier
            .clickable { onAddParticipantsClicked() }
            .fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(bottom = 18.dp, top = 18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.add_participants),
                    contentDescription = "Add participants Icon",
                    tint = MaterialTheme.colors.secondary
                )

                Text(
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.button,
                    text = stringResource(id = R.string.add_participants_menu_item),
                    color = MaterialTheme.colors.secondary
                )
            }
        }
        if (state.participantItemList.isNotEmpty()) {
            CustomDivider(withStartPadding = true)
        }
    }
}

/**
 * See more participants in the list button view
 *
 * @param state [ScheduledMeetingInfoUiState]
 * @param onSeeMoreOrLessClicked
 */
@Composable
private fun SeeMoreOrLessParticipantsButton(
    state: ScheduledMeetingInfoUiState,
    onSeeMoreOrLessClicked: () -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onSeeMoreOrLessClicked() }
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                imageVector = ImageVector.vectorResource(id = if (state.seeMoreVisible) CoreUiR.drawable.ic_chevron_down else CoreUiR.drawable.ic_chevron_up),
                contentDescription = "See more Icon",
                tint = MaterialTheme.colors.secondary
            )

            Text(
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.button,
                text = stringResource(id = if (state.seeMoreVisible) R.string.meetings_scheduled_meeting_info_see_more_participants_label else R.string.meetings_scheduled_meeting_info_see_less_participants_label),
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

/**
 * Leave group button view
 *
 * @param onLeaveGroupClicked
 */
@Composable
private fun LeaveGroupButton(
    onLeaveGroupClicked: () -> Unit,
) {
    CustomDivider(withStartPadding = false)
    Row(modifier = Modifier
        .clickable { onLeaveGroupClicked() }
        .padding(top = 36.dp, bottom = 18.dp)
        .fillMaxWidth()
        .wrapContentSize(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically) {
        Text(textAlign = TextAlign.Center,
            style = MaterialTheme.typography.button,
            text = stringResource(id = R.string.meetings_scheduled_meeting_info_leave_group_label),
            color = red_600.takeIf { isLight() } ?: red_300)
    }
}

/**
 * Scheduled meeting info description view
 *
 * @param state [ScheduledMeetingInfoUiState]
 */
@Composable
private fun ScheduledMeetingDescriptionView(state: ScheduledMeetingInfoUiState) {
    state.scheduledMeeting?.let { schedMeet ->
        schedMeet.description?.let { description ->
            CustomDivider(withStartPadding = false)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 22.dp)
                            .clip(RectangleShape)
                            .wrapContentSize(Alignment.Center)

                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_sched_meeting_description),
                            contentDescription = "Scheduled meeting description icon",
                            tint = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                    }

                    Column(
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .fillMaxSize()
                    ) {
                        Text(modifier = Modifier
                            .padding(start = 32.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.subtitle1,
                            text = stringResource(id = R.string.meetings_scheduled_meeting_info_scheduled_meeting_description_label),
                            color = black.takeIf { isLight() } ?: white)
                        Text(modifier = Modifier
                            .padding(start = 32.dp),
                            style = MaterialTheme.typography.subtitle2,
                            text = description,
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                            fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}

/**
 * Show action buttons options
 *
 * @param state         [ScheduledMeetingInfoUiState]
 * @param action        [ScheduledMeetingInfoAction]
 * @param isChecked     True, if the option is checked. False if not
 * @param hasSwitch     True, if the option has a switch. False if not
 * @param isEnabled     True, if the option must be enabled. False if not
 */
@Composable
private fun ActionOption(
    state: ScheduledMeetingInfoUiState,
    action: ScheduledMeetingInfoAction,
    isChecked: Boolean,
    hasSwitch: Boolean,
    isEnabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .wrapContentSize(Alignment.Center)

            ) {
                action.icon?.let { icon ->
                    Icon(painter = painterResource(id = icon),
                        contentDescription = "${action.name} icon",
                        tint = grey_alpha_054.takeIf { isLight() }
                            ?: white_alpha_054)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ActionText(actionText = action.title)

                state.retentionTimeSeconds?.let { time ->
                    if (action == ScheduledMeetingInfoAction.ManageChatHistory) {
                        ManageChatHistorySubtitle(seconds = time)
                    }
                }

                state.dndSeconds?.let { time ->
                    if (action == ScheduledMeetingInfoAction.ChatNotifications) {
                        ChatNotificationSubtitle(seconds = time)
                    }
                }
            }
        }

        if (hasSwitch) {
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
                    .height(40.dp)
            ) {
                MegaSwitch(
                    modifier = Modifier.align(Alignment.Center),
                    checked = isChecked,
                    enabled = isEnabled,
                    onCheckedChange = null,
                )
            }

        }
    }
}

/**
 * Subtitle text of the available options
 *
 * @param text subtitle text
 */
@Composable
private fun ActionSubtitleText(text: String) {
    Text(modifier = Modifier
        .padding(start = 32.dp, end = 23.dp),
        style = MaterialTheme.typography.subtitle2,
        text = text,
        color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
}

/**
 * Text of the available options
 *
 * @param actionText Title of the option
 */
@Composable
private fun ActionText(actionText: Int) {
    Text(modifier = Modifier
        .padding(start = 32.dp, end = 23.dp),
        style = MaterialTheme.typography.subtitle1,
        text = stringResource(id = actionText),
        color = black.takeIf { isLight() } ?: white)
}

/**
 * View of a participant in the list
 *
 * @param participant               [ChatParticipant]
 * @param showDivider               True, if the divider should be shown. False, if it should be hidden.
 * @param onParticipantClicked       Detect when a participant is clicked
 */
@Composable
private fun ParticipantItemView(
    participant: ChatParticipant,
    showDivider: Boolean,
    onParticipantClicked: (ChatParticipant) -> Unit = {},
) {
    Column {
        Row(modifier = Modifier
            .clickable {
                onParticipantClicked(participant)
            }
            .fillMaxWidth()
            .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Box {
                    ChatAvatarView(
                        avatarUri = participant.data.avatarUri,
                        avatarPlaceholder = participant.getAvatarFirstLetter(),
                        avatarColor = participant.defaultAvatarColor,
                        avatarTimestamp = participant.avatarUpdateTimestamp,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(40.dp)
                    )

                    if (participant.areCredentialsVerified) {
                        Image(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            painter = painterResource(id = IconPackR.drawable.ic_contact_verified),
                            contentDescription = "Verified user"
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val contactName =
                            participant.data.alias ?: participant.data.fullName
                            ?: participant.email ?: ""

                        MegaText(
                            text = if (participant.isMe) stringResource(
                                R.string.chat_me_text_bracket,
                                contactName
                            ) else contactName,
                            overflow = LongTextBehaviour.Ellipsis(1),
                            style = MaterialTheme.typography.subtitle1,
                            textColor = TextColor.Primary,
                        )

                        if (participant.status != UserChatStatus.Invalid) {
                            ContactStatusView(status = participant.status)
                        }
                    }

                    if (participant.lastSeen != null || participant.status != UserChatStatus.Invalid) {
                        val statusText = stringResource(id = participant.status.text)
                        val secondLineText =
                            if (participant.status == UserChatStatus.Online) {
                                statusText
                            } else {
                                getLastSeenString(participant.lastSeen) ?: statusText
                            }

                        MarqueeText(text = secondLineText,
                            color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054,
                            style = MaterialTheme.typography.subtitle2)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    ParticipantsPermissionView(participant)
                    Icon(modifier = Modifier.padding(start = 30.dp),
                        painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
                        contentDescription = "Three dots icon",
                        tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
                }
            }
        }

        if (showDivider) {
            Divider(modifier = Modifier.padding(start = 72.dp),
                color = grey_alpha_012.takeIf { isLight() } ?: white_alpha_012,
                thickness = 1.dp)
        }
    }
}

/**
 * Participants permissions view
 *
 * @param participant [ChatParticipant]
 */
@Composable
private fun ParticipantsPermissionView(participant: ChatParticipant) {
    when (participant.privilege) {
        ChatRoomPermission.Moderator -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_permissions_full_access),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
        }

        ChatRoomPermission.Standard -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_permissions_read_write),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
        }

        ChatRoomPermission.ReadOnly -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_permissions_read_only),
                contentDescription = "Permissions icon",
                tint = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038)
        }

        else -> {}
    }
}


/**
 * Manage chat history subtitle
 *
 * @param seconds  Retention time seconds
 */
@Composable
private fun ManageChatHistorySubtitle(seconds: Long) {
    val text = getRetentionTimeString(LocalContext.current, seconds)?.let {
        "${stringResource(R.string.subtitle_properties_manage_chat)} $it"
    } ?: ""

    ActionSubtitleText(text)
}

/**
 * Chat notification subtitle
 *
 * @param seconds  Dnd seconds
 */
@Composable
private fun ChatNotificationSubtitle(seconds: Long) {
    val text = if (seconds == 0L) {
        stringResource(R.string.mute_chatroom_notification_option_off)
    } else {
        getStringForDndTime(seconds)
    }

    ActionSubtitleText(text)
}

/**
 * Get the appropriate text depending on the time selected for the do not disturb option
 *
 * @param seconds       The seconds which have been set for do not disturb mode
 * @return              The right string
 */
@Composable
fun getStringForDndTime(seconds: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = seconds * 1000

    val calToday = Calendar.getInstance()
    calToday.timeInMillis = System.currentTimeMillis()

    val calTomorrow = Calendar.getInstance()
    calTomorrow.add(Calendar.DATE, +1)

    val df =
        SimpleDateFormat(
            android.text.format.DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                "HH:mm"
            ), Locale.getDefault()
        )
    val tz = cal.timeZone

    df.timeZone = tz

    return pluralStringResource(
        R.plurals.chat_notifications_muted_until_specific_time,
        cal[Calendar.HOUR_OF_DAY], df.format(cal.time)
    )
}


internal const val ACTION_BUTTON_OPTION_TAG = "scheduled_meeting_info:action_button_option"
internal const val SCHEDULE_MEETING_INFO_PARTICIPANTS_WARNING_TAG =
    "scheduled_meeting_info:participants_warning"

/**
 * Meeting link action button View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewActionButton")
@Composable
fun PreviewActionButton() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ActionButton(state = ScheduledMeetingInfoUiState(
            scheduledMeeting = ChatScheduledMeeting(
                chatId = -1,
                schedId = -1,
                parentSchedId = null,
                organizerUserId = null,
                timezone = null,
                startDateTime = -1,
                endDateTime = -1,
                title = "Scheduled title",
                description = "Scheduled description",
                attributes = null,
                overrides = null,
                flags = null,
                rules = null,
                changes = null
            )
        ),
            action = ScheduledMeetingInfoAction.MeetingLink,
            enabledMeetingLinkOption = true,
            isCallInProgress = false,
            onButtonClicked = {})
    }
}

/**
 * Add participants button View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkAddParticipantsButton")
@Composable
fun PreviewAddParticipantsButton() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AddParticipantsButton(state = ScheduledMeetingInfoUiState(
            scheduledMeeting = ChatScheduledMeeting(
                chatId = -1,
                schedId = -1,
                parentSchedId = null,
                organizerUserId = null,
                timezone = null,
                startDateTime = -1,
                endDateTime = -1,
                title = "Scheduled title",
                description = "Scheduled description",
                attributes = null,
                overrides = null,
                flags = null,
                rules = null,
                changes = null
            )
        ), onAddParticipantsClicked = {})
    }
}

/**
 * Scheduled meeting info View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewScheduledMeetingInfoView")
@Composable
fun PreviewScheduledMeetingInfoView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ScheduledMeetingInfoView(
            state = ScheduledMeetingInfoUiState(
                scheduledMeeting = ChatScheduledMeeting(
                    chatId = -1,
                    schedId = -1,
                    parentSchedId = null,
                    organizerUserId = null,
                    timezone = null,
                    startDateTime = -1,
                    endDateTime = -1,
                    title = "Scheduled title",
                    description = "Scheduled description",
                    attributes = null,
                    overrides = null,
                    flags = null,
                    rules = null,
                    changes = null
                )
            ),
            managementState = ScheduledMeetingManagementUiState(),
            onButtonClicked = {},
            onEditClicked = {},
            onAddParticipantsClicked = {},
            onSeeMoreOrLessClicked = {},
            onLeaveGroupClicked = {},
            onParticipantClicked = {},
            onBackPressed = {},
            onDismiss = {},
            onLeaveGroupDialog = {},
            onInviteParticipantsDialog = {},
            onResetStateSnackbarMessage = {},
            onCloseWarningClicked = {},
        )
    }
}
