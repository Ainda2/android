package mega.privacy.android.app.presentation.settings.calls

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.calls.model.SettingsCallsState
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.Typography
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun SettingsCallsView(
    settingsCallsState: SettingsCallsState,
    onSoundNotificationsChanged: (Boolean) -> Unit = {},
    onMeetingInvitationsChanged: (Boolean) -> Unit = {},
    onMeetingRemindersChanged: (Boolean) -> Unit = {},
) {
    Column {
        CallSettingItem(
            sharedR.string.settings_calls_sound_notifications_title,
            sharedR.string.settings_calls_sound_notifications_body,
            settingsCallsState.soundNotifications == CallsSoundEnabledState.Enabled,
            onCheckedChange = onSoundNotificationsChanged
        )
    }
}

@Composable
fun CallSettingItem(
    titleId: Int,
    textId: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { enabled -> onCheckedChange(enabled) })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(start = 0.dp, top = 0.dp, bottom = 0.dp, end = 19.dp)
        ) {
            Text(
                text = stringResource(id = titleId),
                style = Typography.subtitle1,
                color = colorResource(id = R.color.grey_087_white_087)
            )
            Text(
                text = stringResource(id = textId), style = Typography.subtitle2,
                color = colorResource(id = R.color.grey_054_white_054)
            )
        }

        MegaSwitch(
            checked = checked,
            onCheckedChange = null,
        )
    }
    Divider(color = colorResource(id = R.color.grey_012_white_012), thickness = 1.dp)
}


@CombinedThemePreviews
@Composable
private fun PreviewSettingsCallsView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SettingsCallsView(
            settingsCallsState = SettingsCallsState(
                soundNotifications = CallsSoundEnabledState.Enabled,
                callsMeetingInvitations = CallsMeetingInvitations.Enabled,
                callsMeetingReminders = CallsMeetingReminders.Enabled,
            )
        )
    }
}