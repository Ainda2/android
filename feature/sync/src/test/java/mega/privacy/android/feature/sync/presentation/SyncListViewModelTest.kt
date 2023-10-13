package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncListViewModelTest {

    private lateinit var underTest: SyncListViewModel
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase = mock()
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()

    private val stalledIssues = listOf(
        StalledIssue(
            nodeId = NodeId(3L),
            localPath = "/storage/emulated/0/DCIM",
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeName = "Camera",
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
        reset(setOnboardingShownUseCase)
    }

    @Test
    fun `test that view model initialization sets onboarding shown to true`() = runTest {
        initViewModel()

        verify(setOnboardingShownUseCase).invoke(true)
    }

    @Test
    fun `test that stalled issues count is updated when stalled issues are fetched`() =
        runTest {
            whenever(monitorSyncStalledIssuesUseCase()).thenReturn(
                flow {
                    emit(stalledIssues)
                    awaitCancellation()
                }
            )
            initViewModel()

            underTest
                .state
                .test {
                    assertThat(awaitItem().stalledIssuesCount).isEqualTo(1)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    private fun initViewModel() {
        underTest = SyncListViewModel(
            setOnboardingShownUseCase,
            monitorSyncStalledIssuesUseCase
        )
    }
}