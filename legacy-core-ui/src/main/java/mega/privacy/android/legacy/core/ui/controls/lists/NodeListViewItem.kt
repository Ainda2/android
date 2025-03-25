package mega.privacy.android.legacy.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.color_button_brand
import mega.privacy.android.shared.original.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.shared.original.core.ui.utils.isScreenOrientationLandscape
import mega.privacy.android.legacy.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import java.io.File

/**
 * A Composable UI that serves as a base Node List View UI in which all other Node UIs are
 * derived from
 *
 * @param modifier [Modifier]
 * @param isSelected true if the item is selected, and false if otherwise
 * @param folderInfo folder info, if null the item is a File
 * @param icon icon resource
 * @param showLinkIcon whether to show public share link icon
 * @param applySecondaryColorIconTint if true, applies the textColorSecondary color from
 * [MaterialTheme.colors]. No tint is applied if false
 * @param fileSize file size
 * @param modifiedDate modified date
 * @param name name
 * @param infoColor The Info Text Color
 * @param infoIcon The Info Icon
 * @param infoIconTint The Info Icon Tint
 * @param labelColor labelColor
 * @param isTakenDown is taken down
 * @param isFavourite is favourite
 * @param isSharedWithPublicLink is shared with public link
 * @param onLongClick onLongItemClick
 * @param onMenuClick three dots click
 * @param imageState Thumbnail state
 */
@Composable
fun NodeListViewItem(
    isSelected: Boolean,
    folderInfo: String?,
    @DrawableRes icon: Int,
    fileSize: String?,
    modifiedDate: String?,
    name: String,
    isTakenDown: Boolean,
    showMenuButton: Boolean,
    isFavourite: Boolean,
    imageState: State<File?>,
    onClick: () -> Unit,
    isSharedWithPublicLink: Boolean,
    modifier: Modifier = Modifier,
    showLinkIcon: Boolean = true,
    applySecondaryColorIconTint: Boolean = false,
    infoColor: Color? = null,
    @DrawableRes infoIcon: Int? = null,
    @DrawableRes sharesIcon: Int? = null,
    infoIconTint: Color? = null,
    labelColor: Color? = null,
    onLongClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    onMenuClick: () -> Unit = {},
    nodeAvailableOffline: Boolean = false,
) {
    NodeListViewItem(
        modifier = modifier,
        isSelected = isSelected,
        folderInfo = folderInfo,
        icon = icon,
        applySecondaryColorIconTint = applySecondaryColorIconTint,
        fileSize = fileSize,
        modifiedDate = modifiedDate,
        name = name,
        infoColor = infoColor,
        infoIcon = infoIcon,
        sharesIcon = sharesIcon,
        infoIconTint = infoIconTint,
        labelColor = labelColor,
        showMenuButton = showMenuButton,
        showLinkIcon = showLinkIcon,
        isTakenDown = isTakenDown,
        isFavourite = isFavourite,
        isSharedWithPublicLink = isSharedWithPublicLink,
        thumbnailData = imageState.value,
        onClick = onClick,
        onLongClick = onLongClick,
        isEnabled = isEnabled,
        onMenuClick = onMenuClick,
        nodeAvailableOffline = nodeAvailableOffline
    )
}

/**
 * A Composable UI that serves as a base Node List View UI in which all other Node UIs are
 * derived from
 *
 * @param modifier [Modifier]
 * @param isSelected true if the item is selected, and false if otherwise
 * @param folderInfo folder info, if null the item is a File
 * @param icon icon resource
 * @param applySecondaryColorIconTint if true, applies the textColorSecondary color from
 * [MaterialTheme.colors]. No tint is applied if false
 * @param fileSize file size
 * @param modifiedDate modified date
 * @param name name
 * @param infoColor The Info Text Color
 * @param infoIcon The Info Icon
 * @param infoIconTint The Info Icon Tint
 * @param labelColor labelColor
 * @param isTakenDown is taken down
 * @param isFavourite is favourite
 * @param isSharedWithPublicLink is shared with public link
 * @param onLongClick onLongItemClick
 * @param onMenuClick three dots click
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeListViewItem(
    isSelected: Boolean,
    folderInfo: String?,
    @DrawableRes icon: Int,
    fileSize: String?,
    modifiedDate: String?,
    name: String,
    showMenuButton: Boolean,
    isTakenDown: Boolean,
    isFavourite: Boolean,
    isSharedWithPublicLink: Boolean,
    thumbnailData: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLinkIcon: Boolean = true,
    applySecondaryColorIconTint: Boolean = false,
    infoColor: Color? = null,
    @DrawableRes infoIcon: Int? = null,
    @DrawableRes sharesIcon: Int? = null,
    @DrawableRes verifiedIcon: Int? = null,
    infoIconTint: Color? = null,
    labelColor: Color? = null,
    sharesSubtitle: String? = null,
    onLongClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    onMenuClick: () -> Unit = {},
    nodeAvailableOffline: Boolean = false,
    isUnverifiedShare: Boolean = false,
) {
    Column(
        modifier = if (isEnabled) {
            modifier
                .alpha(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        } else {
            modifier
                .alpha(0.5f)
                .clickable(enabled = false) { }
        }
            .fillMaxWidth()
            .height(72.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbNailModifier = Modifier
                .height(48.dp)
                .width(48.dp)
                .clip(RoundedCornerShape(8.dp))
            if (isSelected) {
                Image(
                    modifier = thumbNailModifier
                        .testTag(SELECTED_TEST_TAG),
                    painter = painterResource(R.drawable.ic_select_folder),
                    contentDescription = "Selected",
                )
            } else {
                if (folderInfo != null) {
                    Image(
                        modifier = thumbNailModifier
                            .testTag(FOLDER_TEST_TAG),
                        painter = painterResource(id = icon),
                        contentDescription = "Folder Thumbnail",
                        colorFilter = if (applySecondaryColorIconTint) {
                            ColorFilter.tint(
                                MaterialTheme.colors.textColorSecondary
                            )
                        } else null
                    )
                } else {
                    ThumbnailView(
                        modifier = thumbNailModifier
                            .testTag(FILE_TEST_TAG),
                        data = thumbnailData,
                        defaultImage = icon,
                        contentDescription = "Thumbnail"
                    )
                }
            }
            ConstraintLayout(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth()
            ) {
                val (nodeInfo, threeDots, infoRow, availableOffline, sharesStatus) = createRefs()
                Image(
                    painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                    contentDescription = "3 dots",
                    modifier = Modifier
                        .constrainAs(threeDots) {
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            visibility =
                                if (showMenuButton) Visibility.Visible else Visibility.Gone
                        }
                        .clickable { onMenuClick() }
                        .testTag(MORE_ICON_TEST_TAG)
                )

                Row(
                    modifier = Modifier
                        .constrainAs(nodeInfo) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(threeDots.start)
                            width = Dimension.fillToConstraints
                        }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val iconModifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp)

                    MiddleEllipsisText(
                        text = name,
                        modifier = Modifier.widthIn(max = if (isScreenOrientationLandscape()) 275.dp else 190.dp)
                            .testTag(NODE_TITLE_TEXT_TEST_TAG),
                        style = MaterialTheme.typography.subtitle1,
                        color = if (isTakenDown || isUnverifiedShare) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary,
                    )
                    labelColor?.let {
                        Box(
                            modifier = iconModifier
                                .size(10.dp)
                                .background(
                                    shape = CircleShape, color = it
                                ).testTag(LABEL_TEST_TAG)
                        ) {}
                    }
                    if (isFavourite) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .testTag(FAVORITE_TEST_TAG),
                            painter = painterResource(id = R.drawable.ic_favorite),
                            contentDescription = "Favorite",

                            )
                    }
                    if (isSharedWithPublicLink && showLinkIcon) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .size(16.dp)
                                .testTag(EXPORTED_TEST_TAG),
                            painter = painterResource(id = IconPackR.drawable.ic_link01_medium_regular_outline),
                            contentDescription = "Link",
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colors.textColorSecondary
                            )
                        )
                    }
                    if (isTakenDown) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .testTag(TAKEN_TEST_TAG),
                            painter = painterResource(id = IconPackR.drawable.ic_alert_triangle_medium_regular_outline),
                            contentDescription = "Taken Down",
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.color_button_brand)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(
                            top = 1.dp,
                            end = if (nodeAvailableOffline) 48.dp else 24.dp,
                        )
                        .constrainAs(infoRow) {
                            top.linkTo(nodeInfo.bottom)
                            start.linkTo(parent.start)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (infoIcon != null) {
                        Icon(
                            modifier = Modifier
                                .testTag(INFO_ICON_TEST_TAG)
                                .size(16.dp),
                            painter = painterResource(infoIcon),
                            tint = infoIconTint ?: Color.Unspecified,
                            contentDescription = "Info Icon"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = sharesSubtitle ?: folderInfo ?: "$fileSize · $modifiedDate",
                        modifier = Modifier.testTag(INFO_TEXT_TEST_TAG),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.subtitle2,
                        color = infoColor ?: MaterialTheme.colors.textColorSecondary,
                    )
                    if (verifiedIcon != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            modifier = Modifier
                                .testTag(VERIFIED_TEST_TAG)
                                .size(18.dp),
                            painter = painterResource(verifiedIcon),
                            tint = infoIconTint ?: Color.Unspecified,
                            contentDescription = "Info Icon"
                        )
                    }
                }
                if (sharesIcon != null)
                    Image(
                        modifier = Modifier
                            .constrainAs(sharesStatus) {
                                top.linkTo(infoRow.top)
                                bottom.linkTo(infoRow.top)
                                end.linkTo(availableOffline.start)
                                visibility = Visibility.Visible
                            }
                            .padding(end = 4.dp)
                            .size(21.dp)
                            .testTag(SHARES_ICON_TEST_TAG),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colors.textColorSecondary
                        ),
                        painter = painterResource(id = sharesIcon),
                        contentDescription = "Shares"
                    )
                Image(
                    modifier = Modifier
                        .constrainAs(availableOffline) {
                            top.linkTo(infoRow.top)
                            bottom.linkTo(infoRow.bottom)
                            end.linkTo(threeDots.start)
                            visibility =
                                if (nodeAvailableOffline) Visibility.Visible else Visibility.Gone
                        }.testTag(AVAILABLE_OFFLINE_ICON_TEST_TAG),
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colors.textColorSecondary
                    ),
                    painter = painterResource(id = R.drawable.ic_offline_indicator),
                    contentDescription = "Available Offline"
                )
            }
        }
    }
}

/**
 * Test tag for info text
 */
const val INFO_TEXT_TEST_TAG = "node_list_view_item:text_info"

/**
 * Test tag for shares text
 */
const val SHARES_ICON_TEST_TAG = "node_list_view_item:shares_icon"

/**
 * Test tag for available offline
 */
const val AVAILABLE_OFFLINE_ICON_TEST_TAG = "node_list_view_item:available_offline"

/**
 * Test tag for node title
 */
const val NODE_TITLE_TEXT_TEST_TAG = "node_list_view_item:node_title"

/**
 * Text tag for selected item
 */
const val SELECTED_TEST_TAG = "node_list_view_item:image_selected"

/**
 * Test tag for folder item
 */
const val FOLDER_TEST_TAG = "node_list_view_item:image_folder"

/**
 * Test tag for file item
 */
const val FILE_TEST_TAG = "node_list_view_item:thumbnail_file"

/**
 * Test tag for favorite item
 */
const val FAVORITE_TEST_TAG = "node_list_view_item:image_favorite"

/**
 * Test tag for exported item
 */
const val EXPORTED_TEST_TAG = "node_list_view_item:image_exported"

/**
 * Test tag for taken item
 */
const val TAKEN_TEST_TAG = "node_list_view_item:image_taken"

/**
 * Test tag for verified credential badge
 */
const val VERIFIED_TEST_TAG = "node_list_view_item:icon_verified"

/**
 * Test tag for the Info Icon
 */
const val INFO_ICON_TEST_TAG = "node_list_view_item:icon_info_icon"

/**
 * Test tag for more icon
 */
const val MORE_ICON_TEST_TAG = "node_list_view_item:more_icon"

/**
 * Test tag for the label
 */
const val LABEL_TEST_TAG = "node_list_view_item:label"


@CombinedThemePreviews
@Composable
private fun FilePreview() {
    val imageState = remember {
        mutableStateOf(null as File?)
    }
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            modifier = Modifier,
            isSelected = false,
            folderInfo = null,
            icon = IconPackR.drawable.ic_pdf_medium_solid,
            fileSize = "1.2 MB",
            modifiedDate = "Dec 29, 2022",
            name = "documentation.pdf",
            showMenuButton = true,
            isFavourite = false,
            isSharedWithPublicLink = false,
            isTakenDown = false,
            onClick = {},
            imageState = imageState,
            nodeAvailableOffline = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FolderPreview() {
    val imageState = remember {
        mutableStateOf(null as File?)
    }
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            modifier = Modifier,
            isSelected = false,
            folderInfo = "Empty Folder",
            icon = IconPackR.drawable.ic_folder_medium_solid,
            sharesIcon = R.drawable.ic_alert_triangle,
            fileSize = "1.2 MB",
            modifiedDate = "Dec 29, 2022",
            name = "documentation.pdf",
            showMenuButton = true,
            isFavourite = false,
            isSharedWithPublicLink = false,
            isTakenDown = false,
            onClick = {},
            imageState = imageState,
            nodeAvailableOffline = true
        )
    }
}