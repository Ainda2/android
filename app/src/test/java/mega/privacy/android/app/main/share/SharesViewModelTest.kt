package mega.privacy.android.app.main.share

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SharesViewModelTest {
    private lateinit var underTest: SharesViewModel
    private val getThemeMode = mock<GetThemeMode>()

    @BeforeAll
    fun setUp() {
        underTest = SharesViewModel(getThemeMode)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getThemeMode)
    }

    @Test
    fun `onTabSelected updates currentTab`() = runTest {
        underTest.onTabSelected(SharesTab.LINKS_TAB)
        underTest.state.test {
            assertThat(awaitItem().currentTab).isEqualTo(SharesTab.LINKS_TAB)
        }
    }

    @Test
    fun `themeMode is set from GetThemeMode use case`() = runTest {
        whenever(getThemeMode()).thenReturn(flowOf(ThemeMode.Dark))
        underTest = SharesViewModel(getThemeMode)
        underTest.themeMode.test {
            assertThat(awaitItem()).isEqualTo(ThemeMode.Dark)
            awaitComplete()
        }
    }
}