package mega.privacy.android.app.contacts.usecase

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.format.DateUtils.getRelativeTimeSpanString
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase.Result
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_AVATAR
import nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaContactRequest.STATUS_REMINDED
import nz.mega.sdk.MegaContactRequest.STATUS_UNRESOLVED
import nz.mega.sdk.MegaError
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Use case to retrieve contact requests for current user.
 *
 * @property context                    Application context required to get resources
 * @property megaApi                    MegaApi required to call the SDK
 * @property megaChatApi                MegaChatApi required to call the SDK
 * @property getGlobalChangesUseCase    Use case required to get contact updates
 */
class GetContactRequestsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase
) {

    fun get(): Flowable<List<ContactRequestItem>> =
        Flowable.create({ emitter ->
            val requests = mutableListOf<MegaContactRequest>().apply {
                addAll(megaApi.incomingContactRequests)
                addAll(megaApi.outgoingContactRequests)
            }

            val requestItems = requests
                .sortedByDescending { it.creationTime }
                .map { it.toContactRequestItem() }
                .toMutableList()

            emitter.onNext(requestItems)

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = requestItems.indexOfFirst { it.email == request.email }
                        if (index != INVALID_POSITION) {
                            val currentContact = requestItems[index]

                            when (request.paramType) {
                                USER_ATTR_AVATAR ->
                                    requestItems[index] = currentContact.copy(
                                        avatarUri = File(request.file).toUri()
                                    )
                            }

                            emitter.onNext(requestItems)
                        }
                    } else {
                        Timber.w(error.toThrowable())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    Timber.e(error.toThrowable())
                }
            )

            val globalSubscription = getGlobalChangesUseCase()
                .filter { it is Result.OnContactRequestsUpdate }
                .map { (it as Result.OnContactRequestsUpdate).contactRequests ?: emptyList() }
                .subscribeBy(
                    onNext = { contactRequests ->
                        if (emitter.isCancelled) return@subscribeBy

                        if (contactRequests.isNotEmpty()) {
                            contactRequests.forEach { request ->
                                when (request.status) {
                                    STATUS_UNRESOLVED -> {
                                        if (requestItems.any { it.handle == request.handle }) return@forEach

                                        val newRequestItem = request.toContactRequestItem().apply {
                                            val userImageFile = AvatarUtil.getUserAvatarFile(email)?.absolutePath
                                            megaApi.getUserAvatar(email, userImageFile, userAttrsListener)
                                            megaApi.getUserAttribute(email, USER_ATTR_FIRSTNAME, userAttrsListener)
                                        }

                                        requestItems.add(newRequestItem)
                                    }
                                    STATUS_REMINDED -> {
                                        // do nothing
                                    }
                                    else -> {
                                        requestItems.removeIf { it.handle == request.handle }
                                    }
                                }
                            }

                            emitter.onNext(requestItems)
                        }
                    },
                    onError = Timber::e
                )

            requestItems.forEach { request ->
                if (request.avatarUri == null) {
                    val userImageFile = AvatarUtil.getUserAvatarFile(request.email)?.absolutePath
                    megaApi.getUserAvatar(request.email, userImageFile, userAttrsListener)
                }
            }

            emitter.setCancellable {
                globalSubscription.dispose()
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Build ContactRequestItem from MegaContactRequest object
     *
     * @return  ContactRequestItem
     */
    private fun MegaContactRequest.toContactRequestItem(): ContactRequestItem {
        var userImageUri: Uri? = null

        val userEmail = if (isOutgoing) targetEmail else sourceEmail
        val userImageColor = megaApi.getUserAvatarColor(handle.toString())?.toColorInt() ?: -1
        val placeholder = getImagePlaceholder(userEmail, userImageColor)
        val userImageFile = AvatarUtil.getUserAvatarFile(userEmail)
        if (userImageFile?.exists() == true) {
            userImageUri = userImageFile.toUri()
        }

        return ContactRequestItem(
            handle = handle,
            email = userEmail,
            avatarUri = userImageUri,
            placeholder = placeholder,
            isOutgoing = isOutgoing,
            createdTime = getRelativeTimeSpanString(creationTime * 1000).toString()
        )
    }

    /**
     * Build Avatar placeholder Drawable given a Title and a Color
     *
     * @param title     Title string
     * @param color     Background color
     * @return          Drawable with the placeholder
     */
    private fun getImagePlaceholder(title: String, @ColorInt color: Int): Drawable =
        TextDrawable.builder()
            .beginConfig()
            .width(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
            .height(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
            .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_contact_text_size))
            .textColor(ContextCompat.getColor(context, R.color.white))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(AvatarUtil.getFirstLetter(title), color)
}
