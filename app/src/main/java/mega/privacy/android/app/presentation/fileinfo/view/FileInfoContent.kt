package mega.privacy.android.app.presentation.fileinfo.view

import mega.privacy.android.shared.resources.R as sharedR
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.account.business.AccountSuspendedDialog
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.view.sharedinfo.SharedInfoView
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

/**
 * Content for FileInfo screen, all except toolbar, bottom sheets, dialogs
 */
@Composable
internal fun FileInfoContent(
    viewState: FileInfoViewState,
    onTakeDownLinkClick: (link: String) -> Unit,
    onLocationClick: () -> Unit,
    availableOfflineChanged: (checked: Boolean) -> Unit,
    onVersionsClick: () -> Unit,
    onContactClick: (ContactPermission) -> Unit,
    onContactSelected: (ContactPermission) -> Unit,
    onContactUnselected: (ContactPermission) -> Unit,
    onContactsClosed: () -> Unit,
    onContactMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreContactsClick: () -> Unit,
    onPublicLinkCopyClick: () -> Unit,
    onVerifyContactClick: (String) -> Unit,
    onSetDescriptionClick: (String) -> Unit,
    onAddTagClick: () -> Unit,
    getAddress: suspend (Context, Double, Double) -> Address?,
    modifier: Modifier = Modifier,
) {
    var isShareContactExpanded by remember { mutableStateOf(false) }
    var showExpiredBusinessDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
    ) {
        val paddingEnd = Modifier.padding(end = 16.dp)
        val paddingHorizontal = Modifier.padding(start = paddingStartDefault.dp, end = 16.dp)
        with(viewState) {
            //take down alert
            var showTakeDownWarning by remember(isTakenDown) { mutableStateOf(isTakenDown) }
            if (showTakeDownWarning) {
                TakeDownWarningView(
                    isFile = isFile,
                    onLinkClick = onTakeDownLinkClick,
                    onCloseClick = { showTakeDownWarning = false }
                )
            }
            if (showExpiredBusinessDialog) {
                accountDeactivatedStatus?.let {
                    AccountSuspendedDialog(
                        accountDeactivatedStatus = it,
                        onAlertAcknowledged = { showExpiredBusinessDialog = false },
                        onAlertDismissed = { showExpiredBusinessDialog = false },
                    )
                }
            }

            //owner (incoming share)
            inShareOwnerContactItem?.let { contactItem ->
                OwnerInfoView(
                    contactItem,
                    modifier = paddingEnd,
                )
                if (viewState.isRemindersForContactVerificationEnabled && !contactItem.areCredentialsVerified) {
                    Text(
                        text = stringResource(id = R.string.contact_approve_credentials_toolbar_title),
                        style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.secondary),
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 72.dp, end = 16.dp)
                            .clickable(onClick = { onVerifyContactClick(contactItem.email) })
                            .testTag(TEST_TAG_LOCATION)
                    )
                }
                FileInfoContentDivider()
            }

            //available offline
            if (isAvailableOfflineAvailable) {
                AvailableOfflineView(
                    enabled = isAvailableOfflineEnabled,
                    available = isAvailableOffline,
                    onCheckChanged = availableOfflineChanged,
                    modifier = paddingHorizontal,
                )
                FileInfoContentDivider()
            }

            //file versions
            if (showHistoryVersions) {
                FileVersionsView(
                    versions = historyVersions,
                    onClick = onVersionsClick,
                    modifier = paddingEnd,
                )
                FileInfoContentDivider()
            }

            //shared info (outgoing share)
            if (outShares.isNotEmpty()) {
                SharedInfoView(
                    contacts = outShares,
                    selectedContacts = outShareContactsSelected,
                    expanded = isShareContactExpanded,
                    onHeaderClick = {
                        if (isShareContactExpanded) {
                            onContactsClosed()
                        }
                        isShareContactExpanded = !isShareContactExpanded
                    },
                    onContactClick = {
                        if (viewState.outShareContactsSelected.contains(it.contactItem.email)) {
                            onContactUnselected(it)
                        } else if (viewState.outShareContactsSelected.isEmpty()) {
                            onContactClick(it)
                        } else {
                            onContactSelected(it)
                        }
                    },
                    onContactLongClick = {
                        if (viewState.outShareContactsSelected.contains(it.contactItem.email)) {
                            onContactUnselected(it)
                        } else {
                            onContactSelected(it)
                        }
                    },
                    onContactMoreOptionsClick = onContactMoreOptionsClick,
                    onShowMoreContactsClick = onShowMoreContactsClick,
                )
                FileInfoContentDivider()
            }

            //file size layout
            Spacer(modifier = Modifier.height(8.dp))
            NodeSizeView(
                forFolder = !isFile,
                sizeString = Util.getSizeString(sizeInBytes, LocalContext.current),
                modifier = paddingHorizontal
            )

            //folder content
            folderTreeInfo?.let {
                FolderContentView(
                    numberOfFolders = it.numberOfFolders - 1, //we don't want to count itself
                    numberOfFiles = it.numberOfFiles,
                    modifier = paddingHorizontal,
                )
            }

            //folder versions
            if (showFolderHistoryVersions) {
                FolderVersionsView(
                    numberOfVersions = folderTreeInfo?.numberOfVersions ?: 0,
                    currentVersionsSizeInBytes = folderTreeInfo?.totalCurrentSizeInBytes ?: 0,
                    previousVersionsSizeInBytes = folderTreeInfo?.sizeOfPreviousVersionsInBytes
                        ?: 0,
                    modifier = paddingHorizontal,
                )
            }

            //location
            nodeLocationInfo?.location?.let {
                LocationInfoView(
                    location = it,
                    modifier = paddingHorizontal,
                    onClick = onLocationClick,
                )
            }

            //creation and modification times
            creationTime?.let {
                CreationModificationTimesView(
                    creationTimeInSeconds = it,
                    modificationTimeInSeconds = modificationTime,
                    modifier = paddingHorizontal,
                )
            }

            // Map location
            if (isPhoto && canEnableMapLocation()) {
                FileInfoContentDivider()
                FileInfoMapLocationView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    latitude = latitude,
                    longitude = longitude,
                    getAddress = getAddress,
                )
                FileInfoContentDivider()
            }

            //description
            FileInfoDescriptionField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        if (viewState.accountDeactivatedStatus != null) {
                            showExpiredBusinessDialog = true
                        } else {
                            onSetDescriptionClick(descriptionText)
                        }
                    },
                descriptionText = descriptionText,
                labelId = sharedR.string.file_info_information_description_label,
                placeholder = if (isDescriptionEnabled()) stringResource(id = sharedR.string.file_info_information_description_placeholder) else stringResource(
                    id = sharedR.string.file_info_information_no_description_placeholder
                ),
                isEditable = isDescriptionEnabled() && viewState.accountDeactivatedStatus == null,
                onConfirmDescription = onSetDescriptionClick,
            )

            //tags
            if (canEditTags() || canViewTags()) {
                FileInfoTagsView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    tags = tags,
                    canEditTags = canEditTags(),
                    onAddTagClick = {
                        if (viewState.accountDeactivatedStatus != null) {
                            showExpiredBusinessDialog = true
                        } else {
                            onAddTagClick()
                        }
                    },
                )
            }

            //link
            if (showLink && publicLink != null) {
                ShareLinkView(
                    link = publicLink,
                    date = publicLinkCreationTime ?: 0,
                    onCopyLinkClick = onPublicLinkCopyClick,
                    modifier = paddingHorizontal,
                )
            }
        }
    }
}

/**
 * Add a border divider
 */
@Composable
fun FileInfoContentDivider(
    paddingStart: Dp = paddingStartDefault.dp,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
) {
    Divider(
        modifier = Modifier.padding(start = paddingStart, top = paddingTop, bottom = paddingBottom),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
        thickness = 1.dp
    )
}

/**
 * Preview for [FileInfoContent] for a file
 */
@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun FileInfoContentPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    val scrollState = rememberScrollState()
    var state by mutableStateOf(viewState) //not remembered to allow multiple states in device, don't do that in real code, just in previews
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FileInfoContent(
            viewState = state,
            onTakeDownLinkClick = {},
            availableOfflineChanged = {
                state = state.copy(isAvailableOffline = !state.isAvailableOffline)
            },
            onVersionsClick = {},
            onContactClick = {},
            onContactSelected = {},
            onContactUnselected = {},
            onContactsClosed = {},
            onContactMoreOptionsClick = {},
            onShowMoreContactsClick = {},
            onPublicLinkCopyClick = {},
            onLocationClick = {},
            onVerifyContactClick = {},
            onSetDescriptionClick = {},
            onAddTagClick = {},
            modifier = Modifier.verticalScroll(scrollState),
            getAddress = { _, _, _ -> null }
        )
    }
}

