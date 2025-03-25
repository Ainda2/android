package mega.privacy.android.domain.entity.transfer

/**
 * Domain implementation of active transfer group
 */
data class ActiveTransferGroupImpl(
    override val groupId: Int? = null,
    override val transferType: TransferType,
    override val destination: String,
    override val singleFileName: String? = null,
    override val startTime: Long,
) : ActiveTransferGroup