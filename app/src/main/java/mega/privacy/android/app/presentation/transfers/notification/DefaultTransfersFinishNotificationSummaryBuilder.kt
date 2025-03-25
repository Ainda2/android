package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.TransfersFinishNotificationSummaryBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.FINAL_SUMMARY_GROUP
import mega.privacy.android.domain.entity.transfer.TransferType
import javax.inject.Inject

/**
 * Default implementation of [TransfersFinishNotificationSummaryBuilder]
 */
class DefaultTransfersFinishNotificationSummaryBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
) : TransfersFinishNotificationSummaryBuilder {

    override suspend fun invoke(type: TransferType) =
        NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setGroup(FINAL_SUMMARY_GROUP + type.name)
            .setGroupSummary(true)
            .build()
}