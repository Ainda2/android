package mega.privacy.android.app.presentation.security.check

import androidx.activity.ComponentActivity
import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class PasscodeContainerTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val passcodeCheckViewModel = mock<PasscodeCheckViewModel>()
    private val passcodeCryptObjectFactory = mock<PasscodeCryptObjectFactory>()

    @Test
    fun `test that content is shown if passcode is not locked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.UnLocked))
        }

        val expected = "Expected"
        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = {},
                viewModel = passcodeCheckViewModel,
                content = { Text(expected) },
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
            )
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }


    @Test
    fun `test that passcode dialog is displayed when locked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.Locked))
        }

        val expected = "Expected"

        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = { Text(expected) },
                viewModel = passcodeCheckViewModel,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
            ) {}
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `test that passcode dialog is not displayed when unlocked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.UnLocked))
        }

        val notExpected = "Not Expected"

        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = { Text(notExpected) },
                viewModel = passcodeCheckViewModel,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
            ) {}
        }

        composeTestRule.onNodeWithText(notExpected).assertDoesNotExist()
    }

    @Test
    fun `test that loading is displayed while loading`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.Loading))
        }

        val expected = "Expected"
        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = {},
                viewModel = passcodeCheckViewModel,
                loading = { Text(expected) },
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
            )
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `test that content is displayed while loading if no loading defined`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.Loading))
        }

        val expected = "Expected"
        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = {},
                viewModel = passcodeCheckViewModel,
                content = { Text(expected) },
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
            )
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}