package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getCompleteStartDate
import mega.privacy.android.app.presentation.extensions.getEndDate
import mega.privacy.android.app.presentation.extensions.getEndTime
import mega.privacy.android.app.presentation.extensions.getIntervalValue
import mega.privacy.android.app.presentation.extensions.getStartDate
import mega.privacy.android.app.presentation.extensions.getStartTime
import mega.privacy.android.app.presentation.extensions.isForever
import mega.privacy.android.app.presentation.extensions.isToday
import mega.privacy.android.app.presentation.extensions.isTomorrow
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val PLACEHOLDER_A_OPEN = "[A]"
private const val PLACEHOLDER_A_CLOSE = "[/A]"
private const val PLACEHOLDER_B_OPEN = "[B]"
private const val PLACEHOLDER_B_CLOSE = "[/B]"

/**
 * Get the appropriate day and time string for a scheduled meeting.
 *
 * @param scheduledMeeting  [ChatScheduledMeeting]
 * @param is24HourFormat    True, if it's 24 hour format. False, if not.
 * @param highLightTime     Flag to highlight time differences
 * @param highLightDate     Flag to highlight date differences
 */
@Composable
fun getRecurringMeetingDateTime(
    scheduledMeeting: ChatScheduledMeeting,
    is24HourFormat: Boolean,
    highLightTime: Boolean = false,
    highLightDate: Boolean = false,
): AnnotatedString {
    var result = ""
    val rules = scheduledMeeting.rules
    val startTime = scheduledMeeting.getStartTime(is24HourFormat)
    val endTime = scheduledMeeting.getEndTime(is24HourFormat)
    val startDate = scheduledMeeting.getStartDate()
    val endDate = scheduledMeeting.getEndDate()

    when (rules?.freq) {
        null, OccurrenceFrequencyType.Invalid -> {
            result = getTextForOneOffMeeting(
                scheduledMeeting,
                startTime,
                endTime,
                highLightTime || highLightDate
            )
        }

        OccurrenceFrequencyType.Daily -> {
            val interval = scheduledMeeting.getIntervalValue()
            result = when {
                scheduledMeeting.isForever() -> pluralStringResource(
                    id = R.plurals.notification_subtitle_scheduled_meeting_recurring_daily_forever,
                    count = interval,
                    interval,
                    startDate,
                    startTime,
                    endTime
                )

                else -> pluralStringResource(
                    id = R.plurals.notification_subtitle_scheduled_meeting_recurring_daily_until,
                    count = interval,
                    interval,
                    startDate,
                    endDate,
                    startTime,
                    endTime
                )
            }
        }

        OccurrenceFrequencyType.Weekly -> {
            rules.weekDayList?.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal }
                ?.let { weekDaysList ->
                    val interval = scheduledMeeting.getIntervalValue()
                    when (weekDaysList.size) {
                        1 -> {
                            val weekDay = getShortenedWeekDay(weekDaysList.first(), true)
                            result = when {
                                scheduledMeeting.isForever() -> pluralStringResource(
                                    R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_one_day_forever,
                                    interval,
                                    weekDay,
                                    interval,
                                    startDate,
                                    startTime,
                                    endTime
                                )

                                else -> pluralStringResource(
                                    R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_one_day_until,
                                    interval,
                                    weekDay,
                                    interval,
                                    startDate,
                                    endDate,
                                    startTime,
                                    endTime
                                )
                            }
                        }

                        else -> {
                            val lastWeekDay = getShortenedWeekDay(weekDaysList.last(), false)
                            val weekDaysListString = weekDaysListString(weekDaysList)
                            result = when {
                                scheduledMeeting.isForever() -> pluralStringResource(
                                    R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_several_days_forever,
                                    interval,
                                    weekDaysListString,
                                    lastWeekDay,
                                    interval,
                                    startDate,
                                    startTime,
                                    endTime
                                )

                                else -> pluralStringResource(
                                    R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_several_days_until,
                                    interval,
                                    weekDaysListString,
                                    lastWeekDay,
                                    interval,
                                    startDate,
                                    endDate,
                                    startTime,
                                    endTime
                                )
                            }
                        }
                    }
                }
        }

        OccurrenceFrequencyType.Monthly -> {
            val interval = scheduledMeeting.getIntervalValue()
            rules.monthDayList?.takeIf { it.isNotEmpty() }?.let { monthDayList ->
                val dayOfTheMonth = monthDayList.first()
                result = when {
                    scheduledMeeting.isForever() -> pluralStringResource(
                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_single_day_forever,
                        interval,
                        dayOfTheMonth,
                        interval,
                        startDate,
                        startTime,
                        endTime
                    )

                    else -> pluralStringResource(
                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_single_day_until,
                        interval,
                        dayOfTheMonth,
                        interval,
                        startDate,
                        endDate,
                        startTime,
                        endTime
                    )
                }
            }

            rules.monthWeekDayList.takeIf { it.isNotEmpty() }?.let { monthWeekDayList ->
                val monthWeekDayItem = monthWeekDayList.first()
                val weekOfMonth = monthWeekDayItem.weekOfMonth
                when (monthWeekDayItem.weekDaysList.first()) {
                    Weekday.Monday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First -> {
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            }

                            WeekOfMonth.Second ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Third ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fourth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fifth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                        }
                    }

                    Weekday.Tuesday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Second ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Third ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fourth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fifth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                        }
                    }

                    Weekday.Wednesday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Second ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Third ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fourth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fifth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                        }
                    }

                    Weekday.Thursday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Second ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Third ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fourth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fifth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                        }
                    }

                    Weekday.Friday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First -> {
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }

                            WeekOfMonth.Second -> {
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }

                            WeekOfMonth.Third -> {
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }

                            WeekOfMonth.Fourth -> {
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }

                            WeekOfMonth.Fifth -> {
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }
                        }
                    }

                    Weekday.Saturday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Second ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Third ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fourth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fifth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                        }
                    }

                    Weekday.Sunday -> {
                        when (weekOfMonth) {
                            WeekOfMonth.First ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Second ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Third ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fourth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }

                            WeekOfMonth.Fifth ->
                                result = when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )

                                    else -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                        }
                    }
                }
            }
        }
    }

    return formatAnnotatedString(result, highLightTime, highLightDate)
}

@Composable
fun getAppropiateSubTextString(
    scheduledMeeting: ChatScheduledMeeting?,
    occurrence: ChatScheduledMeetingOccurr?,
    is24HourFormat: Boolean,
    highLightTime: Boolean = false,
    highLightDate: Boolean = false,
): AnnotatedString? {
    occurrence?.let {
        return getOccurrenceDateTime(occurrence, is24HourFormat)
    }

    scheduledMeeting?.let {
        return getRecurringMeetingDateTime(
            scheduledMeeting = scheduledMeeting,
            is24HourFormat = is24HourFormat,
            highLightTime = highLightTime,
            highLightDate = highLightDate
        )
    }

    return null
}

/**
 * Get the appropriate day and time string for a occurrence.
 *
 * @param occurrence  [ChatScheduledMeetingOccurr]
 */
@Composable
fun getOccurrenceDateTime(
    occurrence: ChatScheduledMeetingOccurr, is24HourFormat: Boolean,
): AnnotatedString {
    val startTime = occurrence.getStartTime(is24HourFormat)
    val endTime = occurrence.getEndTime(is24HourFormat)
    val result = stringResource(
        id = R.string.notification_subtitle_scheduled_meeting_one_off,
        occurrence.getCompleteStartDate(),
        startTime,
        endTime
    )

    return formatAnnotatedString(result, highLightTime = false, highLightDate = false)
}

/**
 * Get the appropriate frequency string for a scheduled meeting.
 *
 * @param rules             [ChatScheduledRules]
 * @param isWeekdays        True, if it's weekdays. False, if not.
 * @param currentDay        [Weekday]
 * @param isDayShortened    True, if the abbreviated day name should be used. False, if not.
 */
@Composable
fun getScheduledMeetingFrequencyText(
    rules: ChatScheduledRules,
    isWeekdays: Boolean,
    currentDay: Weekday? = null,
    currentDayOfMonth: Int? = null,
    isDayShortened: Boolean = false,
): String = when (rules.freq) {
    OccurrenceFrequencyType.Invalid -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_never_label)
    OccurrenceFrequencyType.Daily ->
        when {
            isWeekdays -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_every_weekday_label)
            else -> pluralStringResource(
                id = R.plurals.meetings_schedule_meeting_recurrence_every_number_of_days_label,
                count = rules.interval,
                rules.interval
            )
        }

    OccurrenceFrequencyType.Weekly -> {
        val weekdays = rules.weekDayList
        if (weekdays.isNullOrEmpty()) {
            stringResource(id = R.string.meetings_schedule_meeting_recurrence_weekly_label)
        } else {
            weekdays.let {
                when (it.size) {
                    1 -> {
                        val interval = rules.interval
                        when {
                            currentDay == it.first() && interval == 1 -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_weekly_label)
                            else -> pluralStringResource(
                                id = R.plurals.meetings_schedule_meeting_recurrence_one_day_every_number_of_weeks_label,
                                count = rules.interval,
                                getShortenedWeekDay(it.first(), true), rules.interval
                            )
                        }
                    }

                    else -> {
                        when {
                            isWeekdays -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_every_weekday_label)
                            else -> pluralStringResource(
                                id = R.plurals.meetings_schedule_meeting_recurrence_several_days_every_number_of_weeks_label,
                                count = rules.interval,
                                weekDaysListString(weekdays),
                                getShortenedWeekDay(weekdays.last(), false),
                                rules.interval
                            )
                        }
                    }
                }
            }
        }
    }

    OccurrenceFrequencyType.Monthly -> {
        val interval = rules.interval
        val monthDayList = rules.monthDayList
        val monthWeekDayList = rules.monthWeekDayList

        if (monthDayList.isNullOrEmpty() && monthWeekDayList.isEmpty()) {
            stringResource(id = R.string.meetings_schedule_meeting_recurrence_monthly_label)
        } else if (!monthDayList.isNullOrEmpty()) {
            val dayOfTheMonth = monthDayList.first()

            when {
                interval == 1 && dayOfTheMonth == currentDayOfMonth -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_monthly_label)
                else -> pluralStringResource(
                    id = R.plurals.meetings_schedule_meeting_recurrence_specific_day_every_number_of_months_label,
                    count = rules.interval,
                    rules.interval,
                    dayOfTheMonth
                )
            }
        } else if (monthWeekDayList.isNotEmpty()) {
            val monthWeekDayItem = monthWeekDayList.first()
            val weekOfMonth = monthWeekDayItem.weekOfMonth
            val weekDaysList = monthWeekDayItem.weekDaysList
            val weekDay = weekDaysList.first()

            val weekDaysListString = if (isDayShortened) getShortenedWeekDay(
                day = weekDay,
                isForSentenceStart = false
            ) else getWeekDay(day = weekDay)

            when (weekOfMonth) {
                WeekOfMonth.First -> pluralStringResource(
                    id = R.plurals.meetings_schedule_meeting_recurrence_first_day_every_number_of_months_label,
                    count = rules.interval,
                    weekDaysListString,
                    rules.interval,
                )

                WeekOfMonth.Second -> pluralStringResource(
                    id = R.plurals.meetings_schedule_meeting_recurrence_second_day_every_number_of_months_label,
                    count = rules.interval,
                    weekDaysListString,
                    rules.interval,
                )

                WeekOfMonth.Third -> pluralStringResource(
                    id = R.plurals.meetings_schedule_meeting_recurrence_third_day_every_number_of_months_label,
                    count = rules.interval,
                    weekDaysListString,
                    rules.interval,
                )

                WeekOfMonth.Fourth -> pluralStringResource(
                    id = R.plurals.meetings_schedule_meeting_recurrence_fourth_day_every_number_of_months_label,
                    count = rules.interval,
                    weekDaysListString,
                    rules.interval,
                )

                WeekOfMonth.Fifth -> pluralStringResource(
                    id = R.plurals.meetings_schedule_meeting_recurrence_fifth_day_every_number_of_months_label,
                    count = rules.interval,
                    weekDaysListString,
                    rules.interval,
                )
            }
        } else {
            stringResource(id = R.string.meetings_schedule_meeting_recurrence_monthly_label)
        }
    }
}

/**
 * Get the appropriate end recurrence string for a scheduled meeting.
 *
 * @param date         [ZonedDateTime]
 */
@Composable
fun getScheduledMeetingEndRecurrenceText(
    date: ZonedDateTime,
): String {
    val dateFormatter: DateTimeFormatter =
        DateTimeFormatter
            .ofPattern("d MMM yyyy")
            .withZone(ZoneId.systemDefault())

    return dateFormatter.format(date)
}

/**
 * Get week days list string
 *
 * @param weekdays  [Weekday] list
 * @return Weekdays list formatted
 */
@Composable
private fun weekDaysListString(weekdays: List<Weekday>) = StringBuilder().apply {
    weekdays.forEachIndexed { index, weekday ->
        if (index != weekdays.size - 1) {
            append(getShortenedWeekDay(weekday, index == 0))
            if (index != weekdays.size - 2) append(", ")
        }
    }
}.toString()


private fun formatAnnotatedString(
    dateTime: String,
    highLightTime: Boolean,
    highLightDate: Boolean,
): AnnotatedString =
    buildAnnotatedString {
        if (!highLightDate && !highLightTime) {
            append(
                dateTime
                    .replace(PLACEHOLDER_A_OPEN, "").replace(PLACEHOLDER_A_CLOSE, "")
                    .replace(PLACEHOLDER_B_OPEN, "").replace(PLACEHOLDER_B_CLOSE, "")
            )
        } else {
            val indexAOpenStart = dateTime.indexOf(PLACEHOLDER_A_OPEN)
            val indexAOpenEnd = indexAOpenStart + PLACEHOLDER_A_OPEN.length
            val indexACloseStart = dateTime.indexOf(PLACEHOLDER_A_CLOSE)
            val indexACloseEnd = indexACloseStart + PLACEHOLDER_A_CLOSE.length

            val indexBOpenStart = dateTime.indexOf(PLACEHOLDER_B_OPEN)
            val indexBOpenEnd = indexBOpenStart + PLACEHOLDER_B_OPEN.length
            val indexBCloseStart = dateTime.indexOf(PLACEHOLDER_B_CLOSE)
            val indexBCloseEnd = indexBCloseStart + PLACEHOLDER_B_CLOSE.length

            if (indexAOpenStart != -1) {
                append(dateTime.substring(0, indexAOpenStart))
            } else if (indexBOpenStart != -1) {
                append(dateTime.substring(0, indexBOpenStart))
            } else { // Nothing to highlight
                append(dateTime)
                return@buildAnnotatedString
            }

            if (indexAOpenStart != -1) {
                if (highLightDate) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(dateTime.substring(indexAOpenEnd, indexACloseStart))
                    }
                } else {
                    append(dateTime.substring(indexAOpenEnd, indexACloseStart))
                }
            }

            if (indexBOpenStart != -1) {
                append(dateTime.substring(indexACloseEnd, indexBOpenStart))
            } else {
                append(dateTime.substring(indexACloseEnd, dateTime.lastIndex))
                return@buildAnnotatedString
            }

            if (highLightTime) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(dateTime.substring(indexBOpenEnd, indexBCloseStart))
                }
            } else {
                append(dateTime.substring(indexBOpenEnd, indexBCloseStart))
            }

            if (indexBCloseEnd < dateTime.lastIndex) {
                append(dateTime.substring(indexBCloseEnd, dateTime.lastIndex))
            }
        }
    }

/**
 * Get Text of subtitle for one off meeting
 *
 * @param scheduledMeeting      [ChatScheduledMeeting]
 * @param startTime             Start time
 * @param endTime               End time
 * @param highlightDateTime     Flag to highlight date or time
 * @return                      Text of one off meeting
 */
@Composable
private fun getTextForOneOffMeeting(
    scheduledMeeting: ChatScheduledMeeting,
    startTime: String,
    endTime: String,
    highlightDateTime: Boolean,
): String =
    stringResource(
        id = when {
            highlightDateTime -> R.string.notification_subtitle_scheduled_meeting_one_off
            scheduledMeeting.isToday() -> R.string.meetings_one_off_occurrence_info_today
            scheduledMeeting.isTomorrow() -> R.string.meetings_one_off_occurrence_info_tomorrow
            else -> R.string.notification_subtitle_scheduled_meeting_one_off
        },
        scheduledMeeting.getCompleteStartDate(),
        startTime,
        endTime
    )

/**
 * Get the string corresponding to the day of the week shortened
 *
 * @param day [Weekday]
 * @param isForSentenceStart True if the weekday string is to be used at the beginning of the sentence, or False otherwise (in the middle of the sentence).
 * @return String of day of the week
 */
@Composable
private fun getShortenedWeekDay(day: Weekday, isForSentenceStart: Boolean): String = when (day) {
    Weekday.Monday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_mon else R.string.notification_scheduled_meeting_week_day_sentence_middle_mon)
    Weekday.Tuesday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_tue else R.string.notification_scheduled_meeting_week_day_sentence_middle_tue)
    Weekday.Wednesday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_wed else R.string.notification_scheduled_meeting_week_day_sentence_middle_wed)
    Weekday.Thursday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_thu else R.string.notification_scheduled_meeting_week_day_sentence_middle_thu)
    Weekday.Friday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_fri else R.string.notification_scheduled_meeting_week_day_sentence_middle_fri)
    Weekday.Saturday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_sat else R.string.notification_scheduled_meeting_week_day_sentence_middle_sat)
    Weekday.Sunday -> stringResource(id = if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_sun else R.string.notification_scheduled_meeting_week_day_sentence_middle_sun)
}

/**
 * Get the string corresponding to the day of the week
 *
 * @param day [Weekday]
 * @return String of day of the week
 */
@Composable
private fun getWeekDay(day: Weekday): String = when (day) {
    Weekday.Monday -> stringResource(id = R.string.meetings_custom_recurrence_monday_monthly_section_sentence_middle)
    Weekday.Tuesday -> stringResource(id = R.string.meetings_custom_recurrence_tuesday_monthly_section_sentence_middle)
    Weekday.Wednesday -> stringResource(id = R.string.meetings_custom_recurrence_wednesday_monthly_section_sentence_middle)
    Weekday.Thursday -> stringResource(id = R.string.meetings_custom_recurrence_thursday_monthly_section_sentence_middle)
    Weekday.Friday -> stringResource(id = R.string.meetings_custom_recurrence_friday_monthly_section_sentence_middle)
    Weekday.Saturday -> stringResource(id = R.string.meetings_custom_recurrence_saturday_monthly_section_sentence_middle)
    Weekday.Sunday -> stringResource(id = R.string.meetings_custom_recurrence_sunday_monthly_section_sentence_middle)
}