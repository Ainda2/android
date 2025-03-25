package mega.privacy.android.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.notifications.ChatMessageNotificationData
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.toFormattedText
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Manager class to show a chat message Notification
 *
 * @property notificationManagerCompat    [NotificationManagerCompat]
 *
 */
class ChatMessageNotificationManager @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
) {
    companion object {
        private const val GROUP_KEY = "Karere"
    }

    /**
     * Show Notification given a [ChatMessageNotificationData]
     * @param context                   [Context]
     * @param chatMessageNotificationData   [ChatMessageNotificationData]
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(
        context: Context,
        chatMessageNotificationData: ChatMessageNotificationData,
        fileDurationMapper: FileDurationMapper,
    ) = with(chatMessageNotificationData) {
        val messageNotificationId = MegaApiJava.userHandleToBase64(msg.messageId).hashCode()
        val summaryNotificationId = MegaApiJava.userHandleToBase64(chat.chatId).hashCode()
        val unreadCount = chat.unreadCount

        when (this) {
            is ChatMessageNotificationData.DeletedMessage -> {
                Timber.d("Message deleted. Removing notification")
                notificationManagerCompat.cancel(messageNotificationId)
                checkIfShouldCancelSummaryNotification(unreadCount, summaryNotificationId)
                return@with
            }

            is ChatMessageNotificationData.SeenMessage -> {
                Timber.d("Message seen. Removing notification")
                notificationManagerCompat.cancel(messageNotificationId)
                checkIfShouldCancelSummaryNotification(unreadCount, summaryNotificationId)
                return@with
            }

            is ChatMessageNotificationData.Message -> {
                if (chat.chatId == MegaApplication.openChatId) {
                    Timber.d("Chat opened. Removing notification")
                    notificationManagerCompat.cancel(messageNotificationId)
                    checkIfShouldCancelSummaryNotification(unreadCount, summaryNotificationId)
                    return@with
                }
            }
        }

        val intent = Intent(context, ManagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
            putExtra(Constants.CHAT_ID, chat.chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            msg.messageId.toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationColor = ContextCompat.getColor(context, R.color.red_600_red_300)
        val title = EmojiUtilsShortcodes.emojify(chat.title)

        val msgContent = EmojiUtilsShortcodes.emojify(
            getMsgContent(
                context,
                msg,
                fileDurationMapper
            )
        )

        val largeIcon =
            getAvatar(context, senderAvatar, chat, senderAvatarColor)?.let {
                Util.getCircleBitmap(it)
            }

        val isNormalTypeMsg = msg.type == ChatMessageType.NORMAL

        val messagingStyleContent = NotificationCompat.MessagingStyle(
            Person.Builder().apply { setName(title) }.build()
        ).also {
            it.addMessage(
                if (isNormalTypeMsg) msgContent.toFormattedText() else msgContent,
                msg.timestamp,
                Person.Builder().apply { setName(senderName) }.build()
            )
            it.conversationTitle = title
        }

        val groupKey = GROUP_KEY + chat.chatId

        val summaryNotification =
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_CHAT_ID)
                .setSmallIcon(iconPackR.drawable.ic_stat_notify)
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                .setContentIntent(pendingIntent)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .build()

        val messageNotification =
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_CHAT_ID)
                .setSmallIcon(iconPackR.drawable.ic_stat_notify)
                .setAutoCancel(true)
                .setColor(notificationColor)
                .setStyle(messagingStyleContent)
                .setContentIntent(pendingIntent)
                .setWhen(msg.timestamp * 1000)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    notificationBehaviour.sound?.let { setSound(it.toUri()) }
                        ?: setSilent(true)
                    largeIcon?.let { setLargeIcon(it) }
                }
                .setGroup(groupKey)
                .build()

        Timber.d("Showing notification for chat groupKey: $groupKey, notificationId: $messageNotificationId")
        notificationManagerCompat.notify(summaryNotificationId, summaryNotification)
        notificationManagerCompat.notify(messageNotificationId, messageNotification)
    }

    private fun checkIfShouldCancelSummaryNotification(
        unreadCount: Int,
        summaryNotificationId: Int,
    ) {
        if (unreadCount == 0) {
            Timber.d("No unread messages in chat. Removing summary notification $summaryNotificationId")
            notificationManagerCompat.cancel(summaryNotificationId)
        }
    }

    private fun getMsgContent(
        context: Context,
        msg: ChatMessage,
        fileDurationMapper: FileDurationMapper,
    ) = when (msg.type) {
        ChatMessageType.NODE_ATTACHMENT, ChatMessageType.VOICE_CLIP -> {
            with(msg.nodeList) {
                val node = if (isEmpty()) null else this[0]
                val duration = (node as? FileNode)?.type?.let { fileDurationMapper(it) }
                    ?: 0.seconds

                if (node == null) msg.content
                else if (!typeForName(node.name).isAudioVoiceClip) node.name
                else "\uD83C\uDF99 " + CallUtil.milliSecondsToTimer(
                    if (duration.inWholeSeconds == 0L) 0 else duration.inWholeMilliseconds
                )
            }
        }

        ChatMessageType.CONTACT_ATTACHMENT -> {
            Timber.d("TYPE_CONTACT_ATTACHMENT")
            val userCount = msg.usersCount
            if (userCount == 1L) {
                msg.userNames[0]
            } else {
                val name = StringBuilder("")
                name.append(msg.userNames[0])
                for (j in 1 until userCount.toInt()) {
                    name.append(", " + msg.userNames[j])
                }
                name.toString()
            }
        }

        ChatMessageType.CONTAINS_META -> {
            Timber.d("TYPE_CONTAINS_META")
            if (msg.containsMeta?.type == ContainsMetaType.GEOLOCATION) {
                "\uD83D\uDCCD " + context.getString(R.string.title_geolocation_message)
            } else {
                msg.content
            }
        }

        else -> {
            Timber.d("OTHER")
            msg.content
        }
    }

    private fun getAvatar(
        context: Context,
        senderAvatar: File?,
        chat: ChatRoom,
        senderAvatarColor: Int,
    ) = if (senderAvatar?.exists() == true && senderAvatar.length() > 0) {
        BitmapFactory.decodeFile(senderAvatar.absolutePath, BitmapFactory.Options())
    } else {
        val color = if (chat.isGroup) {
            ContextCompat.getColor(context, R.color.grey_012_white_012)
        } else {
            senderAvatarColor
        }

        AvatarUtil.getDefaultAvatar(
            color,
            chat.title,
            Constants.AVATAR_SIZE,
            true,
            true
        )
    }
}