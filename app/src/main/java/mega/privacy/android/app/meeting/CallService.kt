package mega.privacy.android.app.meeting

import mega.privacy.android.icon.pack.R as iconPackR
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.call.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallScreenOpenedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import javax.inject.Inject

/**
 * Service to handle mega calls
 *
 * @property callChangesObserver            [CallChangesObserver]
 * @property hangChatCallByChatIdUseCase    [HangChatCallByChatIdUseCase]
 * @property megaApi                        [MegaApiAndroid]
 * @property megaChatApi                    [MegaChatApiAndroid]
 * @property app                            [MegaApplication]
 * @property monitorChatCallUpdatesUseCase  [MonitorChatCallUpdatesUseCase]
 * @property monitorChatListItemUpdates     [MonitorChatListItemUpdates]
 * @property monitorCallScreenOpenedUseCase [MonitorCallScreenOpenedUseCase]
 * @property getMyUserHandleUseCase         [GetMyUserHandleUseCase]
 */
@AndroidEntryPoint
class CallService : LifecycleService() {

    @Inject
    lateinit var callChangesObserver: CallChangesObserver

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var hangChatCallByChatIdUseCase: HangChatCallByChatIdUseCase

    @Inject
    lateinit var monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase

    @Inject
    lateinit var getMyUserHandleUseCase: GetMyUserHandleUseCase

    @Inject
    lateinit var monitorChatListItemUpdates: MonitorChatListItemUpdates

    @Inject
    lateinit var monitorCallScreenOpenedUseCase: MonitorCallScreenOpenedUseCase

    private var monitorChatListItemUpdatesJob: Job? = null
    private var monitorChatCallUpdatesJob: Job? = null
    private var monitorCallScreenOpenedUpdatesJob: Job? = null

    var app: MegaApplication? = null

    private var currentChatId: Long = MEGACHAT_INVALID_HANDLE
    private var mBuilderCompat: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null
    private var mBuilderCompatO: NotificationCompat.Builder? = null
    private val notificationChannelId = Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID
    private var myUserHandle: Long = MEGACHAT_INVALID_HANDLE

    /**
     * If is in meeting fragment.
     */
    private var isInMeeting = true

    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()

        app = application as MegaApplication

        mBuilderCompat = NotificationCompat.Builder(this, notificationChannelId)
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        startMonitoringChatCallUpdates()
        startMonitorChatListItemUpdatesUpdates()
        startMonitorCallScreenOpenedUpdates()
        getMyUserHandle()
    }

    /**
     * Load my user handle
     */
    private fun getMyUserHandle() {
        lifecycleScope.launch {
            runCatching {
                myUserHandle = getMyUserHandleUseCase()
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Bind service
     */
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Start service work
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        Timber.d("Starting Call service (flags: %d, startId: %d)", flags, startId)
        intent.extras?.let { extras ->
            currentChatId =
                extras.getLong(Constants.CHAT_ID, MEGACHAT_INVALID_HANDLE)
            Timber.d("Chat handle to call: $currentChatId")
        }

        if (currentChatId == MEGACHAT_INVALID_HANDLE) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (callChangesObserver.getOpenCallChatId() != currentChatId) {
            callChangesObserver.setOpenCallChatId(currentChatId)
        }

        showCallInProgressNotification()

        return START_NOT_STICKY
    }

    /**
     * Get chat call updates
     *
     */
    private fun startMonitoringChatCallUpdates() {
        monitorChatCallUpdatesJob?.cancel()
        monitorChatCallUpdatesJob = lifecycleScope.launch {
            monitorChatCallUpdatesUseCase()
                .catch { Timber.e(it) }
                .collect { call ->
                    call.changes?.apply {
                        Timber.d("Changes in call: $this")
                        when {
                            contains(ChatCallChanges.Status) -> {
                                Timber.d("Call status is ${call.status}. Chat id id $currentChatId")

                                when (call.status) {
                                    ChatCallStatus.UserNoPresent,
                                    ChatCallStatus.InProgress,
                                        -> updateNotificationContent()

                                    ChatCallStatus.TerminatingUserParticipation,
                                    ChatCallStatus.Destroyed,
                                        -> removeNotification(call.chatId)

                                    else -> Unit
                                }
                            }

                            contains(ChatCallChanges.OnHold) -> checkAnotherActiveCall()
                            contains(ChatCallChanges.CallComposition) -> {
                                val numParticipants = call.numParticipants ?: 0
                                if (currentChatId == call.chatId && call.callCompositionChange == CallCompositionChanges.Added && numParticipants > 1 &&
                                    myUserHandle == call.peerIdCallCompositionChange && call.status == ChatCallStatus.UserNoPresent
                                ) {
                                    stopSelf()
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Get chat list item updates
     *
     */
    private fun startMonitorChatListItemUpdatesUpdates() {
        monitorChatListItemUpdatesJob?.cancel()
        monitorChatListItemUpdatesJob = lifecycleScope.launch {
            monitorChatListItemUpdates()
                .catch { Timber.e(it) }
                .collectLatest { chat ->
                    if (chat.chatId == currentChatId && chat.changes == ChatListItemChanges.Title && chat.isGroup) {
                        Timber.d("Changes in title")
                        updateNotificationContent()
                    }
                }
        }
    }

    /**
     * Monitor if calls screen is opened
     */
    private fun startMonitorCallScreenOpenedUpdates() {
        monitorCallScreenOpenedUpdatesJob?.cancel()
        monitorCallScreenOpenedUpdatesJob = lifecycleScope.launch {
            monitorCallScreenOpenedUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOpened ->
                    isInMeeting = isOpened
                    updateNotificationContent()
                }
        }
    }

    /**
     * Task removed
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        lifecycleScope.launch { hangChatCallByChatIdUseCase(currentChatId) }
    }

    /**
     * Check if another call is active
     */
    private fun checkAnotherActiveCall() {
        val activeCall = CallUtil.isAnotherActiveCall(currentChatId)
        if (currentChatId == activeCall) {
            updateNotificationContent()
        } else {
            updateCall(activeCall)
        }
    }

    /**
     * Method to create Pending intent for return to a call
     *
     * @param call MegaChatCall
     * @param requestCode RequestCode
     * @return The pending intent to return to a call.
     */
    private fun getPendingIntent(call: MegaChatCall, requestCode: Int): PendingIntent? {
        var intentCall: PendingIntent? = null
        if (call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && call.isRinging) {
            intentCall =
                CallUtil.getPendingIntentMeetingRinging(
                    this,
                    currentChatId,
                    requestCode
                )
        } else if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            intentCall = if (isInMeeting) {
                PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(""),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                CallUtil.getPendingIntentMeetingInProgress(
                    this,
                    currentChatId,
                    requestCode,
                    megaApi.isEphemeralPlusPlus
                )
            }
        }

        return intentCall
    }

    private fun PackageManager.hasPermission(
        permission: String,
        pn: String = packageName,
    ): Boolean =
        checkPermission(permission, pn) == PackageManager.PERMISSION_GRANTED

    private fun PackageManager.hasPermissions(vararg permissions: String): Boolean {
        val pn = packageName
        return permissions.all { checkPermission(it, pn) == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Update the content of the notification
     */
    @SuppressLint("ForegroundServiceType")
    private fun updateNotificationContent() {
        Timber.d("Updating notification")
        megaChatApi.getChatRoom(currentChatId)?.let { chat ->
            megaChatApi.getChatCall(currentChatId)?.let { call ->
                val notificationId = CallUtil.getCallNotificationId(call.callId)
                val pendingIntent: PendingIntent? = getPendingIntent(call, notificationId + 1)

                val contentText =
                    if (call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && call.isRinging)
                        getString(R.string.title_notification_incoming_call)
                    else if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && call.isOnHold)
                        getString(R.string.call_on_hold)
                    else if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && !call.isOnHold)
                        getString(R.string.title_notification_call_in_progress)
                    else ""

                val title = ChatUtil.getTitleChat(chat)

                val largeIcon: Bitmap =
                    if (chat.isGroup)
                        createDefaultAvatar(MEGACHAT_INVALID_HANDLE, title)
                    else
                        setProfileContactAvatar(
                            chat.getPeerHandle(0),
                            title,
                            ChatController(this@CallService).getParticipantEmail(
                                chat.getPeerHandle(
                                    0
                                )
                            )
                        )

                val actionIcon = iconPackR.drawable.ic_phone_01_medium_regular_outline
                val actionPendingIntent = getPendingIntent(call, notificationId + 1)
                val actionTitle =
                    getString(R.string.button_notification_call_in_progress)

                mBuilderCompatO?.clearActions()
                mBuilderCompatO?.apply {
                    setContentTitle(title)
                    setContentIntent(pendingIntent)
                    setLargeIcon(largeIcon)
                    addAction(
                        actionIcon,
                        actionTitle,
                        actionPendingIntent
                    )

                    if (!TextUtil.isTextEmpty(contentText))
                        setContentText(contentText)
                }
                val newNotification: Notification? = mBuilderCompatO?.build()
                newNotification?.let {
                    try {
                        // Since API 34, foreground services
                        // should not be specified before user grants permission.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val pm = packageManager
                            val microphoneServiceType = if (pm.hasPermissions(
                                    Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            ) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                            } else 0

                            val cameraServiceType = if (pm.hasPermissions(
                                    Manifest.permission.FOREGROUND_SERVICE_CAMERA
                                )
                            ) {
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                            } else 0

                            val callServiceType =
                                if (pm.hasPermissions(Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL)) {
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                                } else 0

                            startForeground(
                                notificationId,
                                newNotification,
                                cameraServiceType or callServiceType
                                        or microphoneServiceType
                            )
                            // Since API 30, microphone and camera should be specified for app to use them.
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            startForeground(
                                notificationId,
                                newNotification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                                        or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                            )
                        // Since API 29, should specify foreground service type.
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            startForeground(
                                notificationId,
                                newNotification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                            )
                        else // Before API 29, just start foreground service.
                            startForeground(notificationId, newNotification)

                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }
    }

    /**
     * Show call in progress notification
     */
    private fun showCallInProgressNotification() {
        Timber.d("Showing the notification")
        val notificationId = currentCallNotificationId
        if (notificationId == Constants.INVALID_CALL)
            return

        megaChatApi.getChatRoom(currentChatId)?.let { chat ->
            megaChatApi.getChatCall(currentChatId)?.let { call ->
                val title = ChatUtil.getTitleChat(chat)
                val colorNotification = ContextCompat.getColor(
                    this@CallService,
                    R.color.red_600_red_300
                )
                val smallIcon = iconPackR.drawable.ic_stat_notify
                val largeIcon: Bitmap =
                    if (chat.isGroup)
                        createDefaultAvatar(MEGACHAT_INVALID_HANDLE, title)
                    else
                        setProfileContactAvatar(
                            chat.getPeerHandle(0),
                            title,
                            ChatController(this@CallService).getParticipantEmail(
                                chat.getPeerHandle(
                                    0
                                )
                            )
                        )
                val actionIcon = iconPackR.drawable.ic_phone_01_medium_regular_outline
                val actionPendingIntent = getPendingIntent(call, notificationId + 1)
                val actionTitle =
                    getString(R.string.button_notification_call_in_progress)

                val channel = NotificationChannel(
                    notificationChannelId,
                    Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )

                channel.apply {
                    setShowBadge(true)
                    setSound(null, null)
                }

                mNotificationManager =
                    this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager?.createNotificationChannel(channel)
                mBuilderCompatO = NotificationCompat.Builder(this, notificationChannelId)

                mBuilderCompatO?.apply {
                    setSmallIcon(smallIcon)
                    setAutoCancel(false)
                    addAction(
                        actionIcon,
                        actionTitle,
                        actionPendingIntent
                    )
                    setOngoing(false)
                    color = colorNotification
                }

                mBuilderCompatO?.apply {
                    setLargeIcon(largeIcon)
                    setContentTitle(title)
                }
                updateNotificationContent()
            }
        }
    }

    /**
     * Method to update the MegaChatCall
     *
     * @param newChatIdCall Chat id.
     */
    private fun updateCall(newChatIdCall: Long) {
        stopForeground(STOP_FOREGROUND_REMOVE)

        cancelNotification()
        currentChatId = newChatIdCall
        if (callChangesObserver.getOpenCallChatId() != currentChatId) {
            callChangesObserver.setOpenCallChatId(currentChatId)
        }

        showCallInProgressNotification()
    }

    /**
     * Method to remove the notification
     *
     * @param chatId Chat id.
     */
    private fun removeNotification(chatId: Long) {
        val listCalls = CallUtil.getCallsParticipating()
        if (listCalls == null || listCalls.isEmpty()) {
            stopNotification(chatId)
            return
        }

        for (chatCall in listCalls) {
            if (chatCall != currentChatId) {
                updateCall(chatCall)
                return
            }
        }

        stopNotification(currentChatId)
    }

    /**
     * Method for cancelling a notification that is being displayed.
     *
     * @param chatId That chat ID of a call.
     */
    private fun stopNotification(chatId: Long) {
        stopForeground(STOP_FOREGROUND_REMOVE)

        mNotificationManager?.cancel(getCallNotificationId(chatId))
        stopSelf()
    }

    /**
     * Method to get a contact's avatar
     *
     * @param userHandle User handle.
     * @param fullName User name.
     * @param email User email.
     * @return A Bitmap with the avatar.
     */
    fun setProfileContactAvatar(userHandle: Long, fullName: String, email: String): Bitmap {
        val avatar =
            buildAvatarFile(
                email + FileUtil.JPG_EXTENSION
            )
        if (FileUtil.isFileAvailable(avatar)) {
            if (avatar != null && avatar.exists() && avatar.length() > 0) {
                val avatarBitmap =
                    BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options())
                if (avatarBitmap != null) {
                    return getCircleBitmap(avatarBitmap)
                }

                avatar.delete()
            }
        }

        return createDefaultAvatar(userHandle, fullName)
    }

    /**
     * Get the bitmap as a circle
     *
     * @param bitmap User avatar bitmap
     * @return the bitmap as a circle
     */
    private fun getCircleBitmap(bitmap: Bitmap?): Bitmap {
        val output = Bitmap.createBitmap(bitmap!!.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        bitmap.recycle()
        return output
    }

    /**
     * Create default avatar
     *
     * @param userHandle User handle.
     * @param fullName User name.
     * @return A bitmap with the avatar.
     */
    private fun createDefaultAvatar(userHandle: Long, fullName: String): Bitmap {
        val color = if (userHandle != MEGACHAT_INVALID_HANDLE) {
            AvatarUtil.getColorAvatar(userHandle)
        } else {
            AvatarUtil.getSpecificAvatarColor(Constants.AVATAR_GROUP_CHAT_COLOR)
        }
        return AvatarUtil.getDefaultAvatar(color, fullName, Constants.AVATAR_SIZE, true)
    }

    /**
     * Method for getting the call notification ID from the chat ID.
     *
     * @return call notification ID.
     */
    private val currentCallNotificationId: Int
        get() {
            val call = megaChatApi.getChatCall(currentChatId)
                ?: return Constants.INVALID_CALL
            return CallUtil.getCallNotificationId(call.callId)
        }

    /**
     * Method for cancel notification
     */
    private fun cancelNotification() {
        val notificationId = currentCallNotificationId
        if (notificationId == Constants.INVALID_CALL) return

        mNotificationManager?.cancel(notificationId)
    }

    /**
     * Method to get the notification id of a particular call
     *
     * @param chatId That chat ID of the call.
     * @return The id of the notification.
     */
    private fun getCallNotificationId(chatId: Long): Int {
        return MegaApiJava.userHandleToBase64(chatId).hashCode()
    }

    /**
     * Service ends
     */
    override fun onDestroy() {
        cancelNotification()

        callChangesObserver.setOpenCallChatId(MEGACHAT_INVALID_HANDLE)
        super.onDestroy()
    }
}
