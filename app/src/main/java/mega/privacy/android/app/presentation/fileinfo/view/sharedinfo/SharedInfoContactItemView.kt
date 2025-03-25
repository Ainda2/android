package mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import mega.privacy.android.core.R as CoreUiR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.presentation.contact.view.ContactItemView
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED_DOTS
import mega.privacy.android.app.presentation.preview.contactItemForPreviews
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Contact item for shared with lists
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SharedInfoContactItemView(
    contactItem: ContactPermission,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .testTag(TEST_TAG_CONTACT_ITEM_SHARED),
) {
    ContactItemView(
        contactItem = contactItem.contactItem,
        onClick = null,
        modifier = Modifier.weight(1f),
        statusOverride = contactItem.accessPermission.description()?.let {
            stringResource(id = it)
        } ?: "",
        selected = selected,
        dividerType = null,
    )
    IconButton(
        modifier = Modifier
            .testTag(TEST_TAG_CONTACT_ITEM_SHARED_DOTS),
        onClick = onMoreOptionsClick,
    ) {
        Image(
            painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
            contentDescription = "More options"
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun SharedInfoContactItemViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SharedInfoContactItemView(
            contactItem = ContactPermission(contactItemForPreviews, AccessPermission.READWRITE),
            selected = false,
            onClick = {},
            onLongClick = {},
            onMoreOptionsClick = {},
        )
    }
}