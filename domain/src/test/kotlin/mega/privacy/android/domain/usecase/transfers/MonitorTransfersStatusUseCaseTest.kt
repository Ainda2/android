package mega.privacy.android.domain.usecase.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorTransfersStatusUseCaseTest {

    private lateinit var underTest: MonitorTransfersStatusUseCase

    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()

    @BeforeAll
    fun setup() {
        underTest = MonitorTransfersStatusUseCase(monitorOngoingActiveTransfersUseCase)
    }

    @BeforeEach
    fun resetMocks() = reset(monitorOngoingActiveTransfersUseCase)

    @Test
    fun `test that value with correct values is emitted when a new active transfer total is received`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows()
            val expected = TransfersStatusInfo(
                totalSizeToTransfer = 100L,
                totalSizeTransferred = 200L,
                pendingUploads = 3,
                pendingDownloads = 4,
                paused = false,
                storageOverQuota = true,
                transferOverQuota = true,
            )
            underTest().test {
                awaitItem() //ignore initial
                flowsMap[TransferType.DOWNLOAD]?.update {
                    it.copy(
                        activeTransferTotals = it.activeTransferTotals.copy(
                            transferredBytes = expected.totalSizeTransferred,
                            totalBytes = expected.totalSizeToTransfer,
                            totalFileTransfers = expected.pendingDownloads,
                            totalFinishedFileTransfers = 0,
                        ),
                        transfersOverQuota = true,
                    )
                }
                awaitItem() //ignore updated value
                flowsMap[TransferType.GENERAL_UPLOAD]?.update {
                    it.copy(
                        activeTransferTotals = it.activeTransferTotals.copy(
                            totalFileTransfers = expected.pendingUploads,
                            totalFinishedFileTransfers = 0,
                        ),
                        storageOverQuota = true,
                    )
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that a new status with pause set to false is emitted when a single transfer type is not paused`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows(paused = true)
            val expected = TransfersStatusInfo(paused = false)

            underTest().test {
                assertThat(awaitItem().paused).isTrue() // check initial is paused to validate the test is doing its job
                flowsMap.values.first().update {
                    it.copy(paused = false)
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that a new status with pause set to false is emitted when there are no ongoing transfers`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows(paused = true)
            val expected = TransfersStatusInfo(paused = false)

            underTest().test {
                flowsMap.values.forEach { flow ->
                    assertThat(awaitItem().paused).isTrue() // check initial is paused to validate the test is doing its job
                    flow.update {
                        it.copy(activeTransferTotals = it.activeTransferTotals.copy(totalTransfers = 0))
                    }
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that a new status with pause set to true is emitted when a all transfer types are paused`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows()
            val expected = TransfersStatusInfo(paused = true)

            underTest().test {
                flowsMap.values.forEach { flow ->
                    assertThat(awaitItem().paused).isFalse() // all but last should be false, including initial
                    flow.update {
                        it.copy(paused = true)
                    }
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that value with correct values is emitted when a new active transfer total is received with preview downloads`() =
        runTest {
            val group1 = mock<ActiveTransferTotals.Group> {
                on { totalFiles } doReturn 1
                on { finishedFiles } doReturn 0
                on { completedFiles } doReturn 0
                on { alreadyTransferred } doReturn 0
                on { totalBytes } doReturn 10L
                on { transferredBytes } doReturn 5L
                on { appData } doReturn listOf(TransferAppData.PreviewDownload)
            }
            val group2 = mock<ActiveTransferTotals.Group> {
                on { totalFiles } doReturn 5
                on { finishedFiles } doReturn 2
                on { completedFiles } doReturn 3
                on { alreadyTransferred } doReturn 1
                on { totalBytes } doReturn 500L
                on { transferredBytes } doReturn 200L
                on { appData } doReturn emptyList()
            }
            val activeTransferTotals = ActiveTransferTotals(
                transfersType = TransferType.DOWNLOAD,
                totalTransfers = 6,
                totalFileTransfers = 6,
                pausedFileTransfers = 0,
                totalFinishedTransfers = 2,
                totalFinishedFileTransfers = 2,
                totalCompletedFileTransfers = 3,
                totalBytes = 510L,
                transferredBytes = 205L,
                totalAlreadyTransferredFiles = 1,
                totalCancelled = 0,
                groups = listOf(group1, group2),
            )
            val flowsMap = stubActiveTransfersFlows()
            val expected = TransfersStatusInfo(
                totalSizeToTransfer = 500L,
                totalSizeTransferred = 200L,
                pendingDownloads = 3,
            )
            flowsMap[TransferType.DOWNLOAD]?.update {
                it.copy(activeTransferTotals = activeTransferTotals)
            }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }


    private fun stubActiveTransfersFlows(paused: Boolean = false) =
        TransferType.entries.filterNot { it == TransferType.NONE }.associateWith { type ->
            MutableStateFlow(
                MonitorOngoingActiveTransfersResult(
                    activeTransferTotals = emptyActiveTransferTotals(type),
                    paused = paused,
                    storageOverQuota = false,
                    transfersOverQuota = false,
                )
            ).also { flow ->
                whenever(monitorOngoingActiveTransfersUseCase(type)) doReturn flow
            }
        }

    private fun emptyActiveTransferTotals(type: TransferType) = ActiveTransferTotals(
        transfersType = type,
        totalTransfers = 1,
        totalFileTransfers = 0,
        pausedFileTransfers = 0,
        totalFinishedTransfers = 0,
        totalFinishedFileTransfers = 0,
        totalCompletedFileTransfers = 0,
        totalBytes = 0L,
        transferredBytes = 0L,
        totalAlreadyTransferredFiles = 0,
        totalCancelled = 0,
    )
}