package mega.privacy.android.app.presentation.requeststatus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestStatusProgressContainerTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that progress dialog is displayed correctly`() {
        composeRule.setContent {
            RequestStatusProgressBarContent(showProgressBar = true, progress = 500)
        }

        composeRule.onNodeWithTag(PROGRESS_BAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that dialog is not displayed when showProgressBar is false`() {
        composeRule.setContent {
            RequestStatusProgressBarContent(showProgressBar = false, progress = -1)
        }

        composeRule.onNodeWithTag(PROGRESS_BAR_TEST_TAG).assertDoesNotExist()
    }
}