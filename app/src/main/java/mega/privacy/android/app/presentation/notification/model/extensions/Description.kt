package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.ContactChangeAccountDeletedAlert
import mega.privacy.android.domain.entity.ContactChangeBlockedYouAlert
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.ContactChangeDeletedYouAlert
import mega.privacy.android.domain.entity.DeletedScheduledMeetingAlert
import mega.privacy.android.domain.entity.IncomingPendingContactCancelledAlert
import mega.privacy.android.domain.entity.IncomingPendingContactReminderAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.NewScheduledMeetingAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactIncomingIgnoredAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingAcceptedAlert
import mega.privacy.android.domain.entity.UpdatedPendingContactOutgoingDeniedAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingCancelAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingDateTimeAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingDescriptionAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingFieldsAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingRulesAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingTimezoneAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingTitleAlert
import mega.privacy.android.domain.entity.UserAlert

/**
 * Description
 *
 */
internal fun UserAlert.description(): (Context) -> String? =
    when (this) {
        is ContactAlert -> getDescriptionFunction()
        is ScheduledMeetingAlert -> getDescriptionFunction()
        else -> { _ -> null }
    }

/**
 * Get description id
 *
 */
@StringRes
private fun ContactAlert.getDescriptionId(): Int? = when (this) {
    is ContactChangeAccountDeletedAlert -> R.string.subtitle_account_notification_deleted
    is ContactChangeBlockedYouAlert -> R.string.subtitle_contact_notification_blocked
    is ContactChangeContactEstablishedAlert -> R.string.notification_new_contact
    is ContactChangeDeletedYouAlert -> R.string.subtitle_contact_notification_deleted
    is IncomingPendingContactCancelledAlert -> R.string.subtitle_contact_request_notification_cancelled
    is IncomingPendingContactReminderAlert -> R.string.notification_reminder_contact_request
    is IncomingPendingContactRequestAlert -> R.string.notification_new_contact_request
    is UpdatedPendingContactIncomingAcceptedAlert -> R.string.subtitle_incoming_contact_request_accepted
    is UpdatedPendingContactIncomingDeniedAlert -> R.string.subtitle_outgoing_contact_request_denied
    is UpdatedPendingContactIncomingIgnoredAlert -> R.string.subtitle_incoming_contact_request_ignored
    is UpdatedPendingContactOutgoingAcceptedAlert -> R.string.subtitle_outgoing_contact_request_accepted
    is UpdatedPendingContactOutgoingDeniedAlert -> R.string.subtitle_incoming_contact_request_denied
    else -> null
}

/**
 * Get description function
 *
 */
private fun ContactAlert.getDescriptionFunction(): (Context) -> String? = { context ->
    this.getDescriptionId()?.let {
        context.getString(it, contact.getNicknameStringOrEmail(context))
    }
}

/**
 * Get description function
 *
 */
private fun ScheduledMeetingAlert.getDescriptionFunction(): (Context) -> String? =
    { context ->
        val user = email ?: context.getString(R.string.unknown_name_label)
        when (this) {
            is NewScheduledMeetingAlert -> {
                val stringRes = if (isRecurring) {
                    R.string.notification_subtitle_scheduled_recurring_meeting_new
                } else {
                    R.string.notification_subtitle_scheduled_meeting_new
                }
                context.getString(stringRes, user)
            }

            is UpdatedScheduledMeetingCancelAlert, is DeletedScheduledMeetingAlert -> {
                val stringRes = when {
                    isOccurrence -> sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_occurrence_canceled
                    isRecurring -> R.string.notification_subtitle_scheduled_recurring_meeting_canceled
                    else -> sharedR.string.meetings_notification_subtitle_scheduled_meeting_canceled
                }
                context.getString(stringRes, user)
            }

            is UpdatedScheduledMeetingTitleAlert -> {
                context.getString(
                    R.string.notification_subtitle_scheduled_meeting_updated_title,
                    user,
                    oldTitle,
                    title
                )
            }

            is UpdatedScheduledMeetingDescriptionAlert -> {
                val stringRes = if (isRecurring) {
                    R.string.notification_subtitle_scheduled_recurring_meeting_updated_description
                } else {
                    R.string.notification_subtitle_scheduled_meeting_updated_description
                }
                context.getString(stringRes, user)
            }

            is UpdatedScheduledMeetingDateTimeAlert -> {
                val stringRes = when {
                    isOccurrence -> sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_updated_occurrence
                    hasDateChanged -> if (isRecurring) {
                        sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_scheduled_changes
                    } else {
                        sharedR.string.meetings_notification_subtitle_scheduled_meeting_updated_date
                    }

                    hasDayOfWeekChanged -> if (isRecurring) {
                        sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_date_of_the_week_changed
                    } else {
                        sharedR.string.meetings_notification_subtitle_scheduled_meeting_updated_date

                    }

                    hasTimeChanged -> if (isRecurring) {
                        sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_hour_changed
                    } else {
                        sharedR.string.meetings_notification_subtitle_scheduled_meeting_updated_time
                    }

                    else -> if (isRecurring) {
                        sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_scheduled_changes
                    } else {
                        sharedR.string.meetings_notification_subtitle_scheduled_meeting_updated_date
                    }
                }
                context.getString(stringRes, user)
            }

            is UpdatedScheduledMeetingTimezoneAlert -> context.getString(
                sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_scheduled_changes,
                user
            )

            is UpdatedScheduledMeetingRulesAlert -> {
                val stringRes = when {
                    !hasDateChanged && !hasDayOfWeekChanged -> sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_multiple_fields_changed
                    hasDateChanged && hasDayOfWeekChanged -> if (isRecurring) {
                        sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_scheduled_changes
                    } else {
                        sharedR.string.meetings_notification_subtitle_scheduled_meeting_updated_date
                    }

                    else -> if (isRecurring) {
                        sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_date_of_the_week_changed
                    } else {
                        sharedR.string.meetings_notification_subtitle_scheduled_meeting_updated_date
                    }
                }
                context.getString(stringRes, user)
            }

            is UpdatedScheduledMeetingFieldsAlert -> {
                val stringRes =
                    sharedR.string.meetings_notification_subtitle_scheduled_recurring_meeting_multiple_fields_changed
                context.getString(stringRes, user)
            }

            else -> null
        }
    }
