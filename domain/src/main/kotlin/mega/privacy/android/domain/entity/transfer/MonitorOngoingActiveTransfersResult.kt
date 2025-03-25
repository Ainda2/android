package mega.privacy.android.domain.entity.transfer

/**
 * Data class to return required data in Monitor OngoingActive Transfers UseCases
 * @param activeTransferTotals [ActiveTransferTotals]
 * @param paused true if transfers are paused globally or all individual transfers are paused
 * @param transfersOverQuota true if the transfers are currently on over quota
 * @param storageOverQuota true if the storage is currently on over quota
 */
data class MonitorOngoingActiveTransfersResult(
    val activeTransferTotals: ActiveTransferTotals,
    val paused: Boolean,
    val transfersOverQuota: Boolean,
    val storageOverQuota: Boolean,
) {
    /**
     * @return true if there are ongoing transfers and there's no over quota for this transfer type
     */
    fun hasPendingWork(transferType: TransferType) =
        activeTransferTotals.hasOngoingTransfers() &&
                when {
                    transferType.isDownloadType() -> !transfersOverQuota
                    transferType.isUploadType() -> !storageOverQuota
                    else -> true
                }
}