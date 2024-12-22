package mega.privacy.android.app.presentation.settings.home.mapper

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.navigation.settings.SettingSectionHeader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SectionHeaderMapperTest {
    private val underTest = SectionHeaderMapper()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that SettingSectionHeader Appearance is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Appearance)())
        }
        composeTestRule.onNodeWithText("Appearance").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader About is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.About)())
        }
        composeTestRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader Features is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Features)())
        }
        composeTestRule.onNodeWithText("Features").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader Help is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Help)())
        }
        composeTestRule.onNodeWithText("Help").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader Media is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Media)())
        }
        composeTestRule.onNodeWithText("Media").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader Security is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Security)())
        }
        composeTestRule.onNodeWithText("Security").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader Storage is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Storage)())
        }
        composeTestRule.onNodeWithText("Storage").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader UserInterface is mapped correctly`() {
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.UserInterface)())
        }
        composeTestRule.onNodeWithText("User interface").assertExists()
    }

    @Test
    fun `test that SettingSectionHeader Custom is mapped correctly`() {
        val customTitle = "ExpectedCustom"
        composeTestRule.setContent {
            Text(underTest(SettingSectionHeader.Custom(customTitle))())
        }
        composeTestRule.onNodeWithText(customTitle).assertExists()
    }
}