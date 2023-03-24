package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [MegaTransfer] to [Transfer] mapper
 */
class TransferMapper @Inject constructor() {
    operator fun invoke(transfer: MegaTransfer) = Transfer(
        tag = transfer.tag,
        transferType = mapTransferType(transfer.type),
        totalBytes = transfer.totalBytes,
        transferredBytes = transfer.transferredBytes,
        isFinished = transfer.isFinished,
        transferState = mapTransferState(transfer.state),
        fileName = transfer.fileName,
        handle = transfer.nodeHandle,
        isFolderTransfer = transfer.isFolderTransfer,
        priority = transfer.priority,
        isStreamingTransfer = transfer.isStreamingTransfer,
        notificationNumber = transfer.notificationNumber,
        speed = transfer.speed,
        appData = transfer.appData.orEmpty()
    )

    private fun mapTransferType(transferType: Int): TransferType = when (transferType) {
        MegaTransfer.TYPE_DOWNLOAD -> TransferType.TYPE_DOWNLOAD
        MegaTransfer.TYPE_UPLOAD -> TransferType.TYPE_UPLOAD
        else -> TransferType.NONE
    }

    private fun mapTransferState(transferState: Int): TransferState = when (transferState) {
        MegaTransfer.STATE_NONE -> TransferState.STATE_NONE
        MegaTransfer.STATE_QUEUED -> TransferState.STATE_QUEUED
        MegaTransfer.STATE_ACTIVE -> TransferState.STATE_ACTIVE
        MegaTransfer.STATE_PAUSED -> TransferState.STATE_PAUSED
        MegaTransfer.STATE_RETRYING -> TransferState.STATE_RETRYING
        MegaTransfer.STATE_COMPLETING -> TransferState.STATE_COMPLETING
        MegaTransfer.STATE_COMPLETED -> TransferState.STATE_COMPLETED
        MegaTransfer.STATE_CANCELLED -> TransferState.STATE_CANCELLED
        MegaTransfer.STATE_FAILED -> TransferState.STATE_FAILED
        else -> TransferState.STATE_NONE
    }
}