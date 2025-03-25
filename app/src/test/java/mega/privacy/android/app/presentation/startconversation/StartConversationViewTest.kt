package mega.privacy.android.app.presentation.startconversation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.startconversation.model.StartConversationState
import mega.privacy.android.app.presentation.startconversation.view.StartConversationView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.startconversation.view.TEST_TAG_RAISED_DEFAULT_MEGA_BUTTON

@RunWith(AndroidJUnit4::class)

class StartConversationViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun test_that_invite_contacts_button_is_shown() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(TEST_TAG_RAISED_DEFAULT_MEGA_BUTTON).assertExists()
    }

    @Test
    fun test_that_new_group_chat_button_is_shown() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.new_group_chat_label).assertExists()
    }

    @Test
    fun test_that_new_meeting_button_is_shown() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.new_meeting).assertExists()
    }

    @Test
    fun test_that_buttons_are_not_visible_if_buttons_visibility_is_false() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.invite_contacts)
    }

    @Test
    fun test_that_contacts_text_is_shown() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.section_contacts).assertExists()
    }

    private fun initComposeRuleContent(
        state: StartConversationState = StartConversationState(),
    ) {
        composeRule.setContent {
            StartConversationView(
                state = state,
                onSearchTextChange = {},
                onCloseSearchClicked = {},
                onSearchClicked = {},
                onBackPressed = {},
                onContactClicked = {},
                onInviteContactsClicked = {},
                onNoteToSelfClicked = {}
            )
        }
    }
}
