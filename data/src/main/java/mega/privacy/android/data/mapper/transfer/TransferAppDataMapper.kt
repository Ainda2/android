package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.BackgroundTransfer
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.CameraUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.ChatUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.OriginalContentUri
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.SDCardDownload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.VoiceClip
import mega.privacy.android.domain.entity.transfer.TransferAppData
import timber.log.Timber
import javax.inject.Inject

/**
 * Raw app data [String] to a list of [TransferAppData] mapper
 */
class TransferAppDataMapper @Inject constructor() {
    /**
     * Get a list of [TransferAppData] corresponding to raw appData [String]
     * @param appDataRaw the app data [String] as it is in MegaTransfer
     * @return a [List] of [TransferAppData]
     */
    operator fun invoke(
        appDataRaw: String,
    ): List<TransferAppData> = if (appDataRaw.isEmpty()) emptyList() else {
        appDataRaw
            .split(APP_DATA_REPEATED_TRANSFER_SEPARATOR)
            .flatMap { it.splitTransfersParameters() }
            .map { appDataRawSingle ->
                val typeAndValues = appDataRawSingle.split(APP_DATA_INDICATOR)
                AppDataTypeConstants.getTypeFromSdkValue(typeAndValues.first()) to
                        typeAndValues.drop(1)
            }
            .mapNotNull { (type, values) ->
                val result = when (type) {
                    VoiceClip -> TransferAppData.VoiceClip
                    CameraUpload -> TransferAppData.CameraUpload
                    ChatUpload -> values.firstIfNotBlank()?.toLongOrNull()
                        ?.let { TransferAppData.ChatUpload(it) }

                    SDCardDownload -> {
                        values.firstIfNotBlank()?.let {
                            TransferAppData.SdCardDownload(it, values.getOrElse(1) { "" })
                        }
                    }

                    BackgroundTransfer -> TransferAppData.BackgroundTransfer

                    OriginalContentUri -> values.firstIfNotBlank()?.let {
                        TransferAppData.OriginalContentUri(it)
                    }

                    AppDataTypeConstants.ChatDownload -> {
                        val chatId = values.getOrNull(0)?.toLongOrNull()
                        val msgId = values.getOrNull(1)?.toLongOrNull()
                        val msgIndex = values.getOrNull(2)?.toIntOrNull()
                        if (chatId != null && msgId != null && msgIndex != null) {
                            TransferAppData.ChatDownload(chatId, msgId, msgIndex)
                        } else null
                    }

                    else -> null
                }
                if (result == null) {
                    Timber.d("appData not recognized: $type $values")
                }
                return@mapNotNull result
            }
    }

    private fun List<String>.firstIfNotBlank() = this.firstOrNull()?.takeIf { it.isNotBlank() }

    /**
     * Similar to [split(APP_DATA_SEPARATOR)] but checking if there is an [AppDataTypeConstants] after the separator
     */
    private fun String.splitTransfersParameters(): List<String> {
        val separators = AppDataTypeConstants.values().map { APP_DATA_SEPARATOR + it.sdkTypeValue }
        val result = mutableListOf<String>()
        var toCheck = this
        var index = toCheck.indexOfAny(separators)
        while (index > 0) {
            val parameter = toCheck.substring(0, index)
            result.add(parameter)
            toCheck = toCheck.removePrefix(parameter)
            index = toCheck.indexOfAny(separators, 1)
        }
        result.add(toCheck)
        return result.mapIndexed { i, s ->
            if (i == 0) {
                s
            } else {
                s.removePrefix(APP_DATA_SEPARATOR)
            }
        }
    }

    companion object {

        /**
         * App data for indicating the data after it, is the value of a transfer parameter
         */
        const val APP_DATA_INDICATOR = ">"

        /**
         * App data for indicating the data after it, is a new transfer parameter.
         */
        const val APP_DATA_SEPARATOR = "-"

        /**
         * App data for indicating the data after it, is a new AppData due to a repeated transfer.
         */
        const val APP_DATA_REPEATED_TRANSFER_SEPARATOR = "!"
    }
}