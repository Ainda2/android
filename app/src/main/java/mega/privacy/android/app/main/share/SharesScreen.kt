package mega.privacy.android.app.main.share

import mega.privacy.android.icon.pack.R as IconR
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeFragment
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.shares.links.LinksComposeFragment
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesComposeFragment
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.shared.original.core.ui.controls.appbar.TEST_TAG_APP_BAR
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.tab.Tabs
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SharesScreen(
    statusBarPadding: Int = 0,
    uiState: SharesUiState = SharesUiState(),
    incomingUiState: IncomingSharesState = IncomingSharesState(),
    outgoingUiState: OutgoingSharesState = OutgoingSharesState(),
    linksUiState: LinksUiState = LinksUiState(),
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onPageSelected: (SharesTab) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val view = LocalView.current
    val pagerState = rememberPagerState(initialPage = uiState.currentTab.position) { 3 }
    val unverifiedIncoming =
        incomingUiState.nodesList.count { it.node.shareData?.isUnverifiedDistinctNode == true }
    val unVerifiedOutgoing =
        outgoingUiState.nodesList.count { it.node.shareData?.isUnverifiedDistinctNode == true }
    val elevationState by remember { mutableStateOf(BooleanArray(3)) }
    var isScrolled by remember { mutableStateOf(false) }
    val isTabShown = when (uiState.currentTab) {
        SharesTab.INCOMING_TAB -> incomingUiState.isInRootLevel && !incomingUiState.isInSelection
        SharesTab.OUTGOING_TAB -> outgoingUiState.isInRootLevel && !outgoingUiState.isInSelection
        SharesTab.LINKS_TAB -> linksUiState.isInRootLevel && !linksUiState.isInSelection
        else -> true
    }
    val titleName = when (uiState.currentTab) {
        SharesTab.INCOMING_TAB -> incomingUiState.currentNodeName
        SharesTab.OUTGOING_TAB -> outgoingUiState.currentNodeName
        SharesTab.LINKS_TAB -> linksUiState.parentNode?.name
        else -> stringResource(R.string.title_shared_items)
    } ?: stringResource(R.string.title_shared_items)
    val isShowMore = when (uiState.currentTab) {
        SharesTab.INCOMING_TAB -> !incomingUiState.isInRootLevel
        SharesTab.OUTGOING_TAB -> !outgoingUiState.isInRootLevel
        SharesTab.LINKS_TAB -> !linksUiState.isInRootLevel
        else -> false
    }

    LaunchedEffect(uiState.currentTab) {
        if (uiState.currentTab != SharesTab.NONE) {
            pagerState.animateScrollToPage(uiState.currentTab.position)
            isScrolled = elevationState[uiState.currentTab.position]
        }
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.currentPage != uiState.currentTab.position && !pagerState.isScrollInProgress) {
            onPageSelected(SharesTab.fromPosition(pagerState.currentPage))
            isScrolled = elevationState[pagerState.currentPage]
        }
    }

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            Surface(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface),
                elevation = if (isScrolled) AppBarDefaults.TopAppBarElevation else 0.dp,
            ) {
                Column {
                    TopAppBar(
                        modifier = Modifier.testTag(TEST_TAG_APP_BAR),
                        windowInsets = WindowInsets(top = statusBarPadding),
                        title = {
                            Text(
                                text = titleName,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (isTabShown) {
                                        onOpenDrawer()
                                    } else {
                                        onBackPressedDispatcher?.onBackPressed()
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(if (isTabShown) mega.privacy.android.core.R.drawable.ic_menu else mega.privacy.android.core.R.drawable.ic_back),
                                    contentDescription = "Back button",
                                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                                )
                            }
                        },
                        elevation = 0.dp,
                        backgroundColor = MaterialTheme.colors.surface,
                        actions = {
                            IconButton(
                                modifier = Modifier.testTag(MENU_SEARCH_TEST_TAG),
                                onClick = onSearchClick,
                            ) {
                                Icon(
                                    painter = painterResource(mega.privacy.android.legacy.core.ui.R.drawable.ic_search),
                                    contentDescription = "Search button",
                                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                                )
                            }
                            if (isShowMore) {
                                IconButton(
                                    modifier = Modifier.testTag(MENU_MORE_TEST_TAG),
                                    onClick = onMoreClick,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more),
                                        contentDescription = "More button",
                                        tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    )
                    Tabs(
                        modifier = Modifier.testTag(TAB_ROW_TEST_TAG),
                        shouldTabsShown = isTabShown,
                        pagerEnabled = false,
                        pagerState = pagerState,
                        onTabSelected = {
                            Timber.d("Selected tab: $it")
                        },
                    ) {
                        SharesTab.entries.filter { it != SharesTab.NONE }
                            .forEachIndexed { page, item ->
                                addIconTab(
                                    text = stringResource(item.stringRes),
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (item == SharesTab.INCOMING_TAB && unverifiedIncoming > 0) {
                                                    Badge {
                                                        Text("$unverifiedIncoming")
                                                    }
                                                } else if (item == SharesTab.OUTGOING_TAB && unVerifiedOutgoing > 0) {
                                                    Badge {
                                                        Text("$unVerifiedOutgoing")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    id = item.getIcon(
                                                        pagerState.currentPage == page
                                                    )
                                                ),
                                                contentDescription = stringResource(item.stringRes),
                                            )
                                        }
                                    },
                                    tag = item.name,
                                )
                            }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(innerPadding)
        ) {
            // AndroidFragment can't be previewed
            if (!view.isInEditMode) {
                HorizontalPager(
                    modifier = Modifier.fillMaxWidth(),
                    state = pagerState,
                    beyondViewportPageCount = 3,
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = isTabShown,
                ) { page: Int ->
                    Column(
                        Modifier.fillMaxSize(),
                    ) {
                        when (page) {
                            SharesTab.INCOMING_TAB.position -> AndroidFragment(
                                IncomingSharesComposeFragment::class.java
                            ) {
                                it.toggleAppBarElevation = { value ->
                                    elevationState[page] = value
                                    isScrolled = value
                                }
                            }

                            SharesTab.OUTGOING_TAB.position -> AndroidFragment(
                                OutgoingSharesComposeFragment::class.java
                            ) {
                                it.toggleAppBarElevation = { value ->
                                    elevationState[page] = value
                                    isScrolled = value
                                }
                            }

                            SharesTab.LINKS_TAB.position -> AndroidFragment(
                                LinksComposeFragment::class.java
                            ) {
                                it.toggleAppBarElevation = { value ->
                                    elevationState[page] = value
                                    isScrolled = value
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun SharesTab.getIcon(isSelected: Boolean): Int {
    return if (isSelected) {
        when (this) {
            SharesTab.INCOMING_TAB -> IconR.drawable.ic_folder_incoming_medium_regular_solid
            SharesTab.OUTGOING_TAB -> IconR.drawable.ic_folder_outgoing_medium_regular_solid
            SharesTab.LINKS_TAB -> IconR.drawable.ic_link01_medium_regular_solid
            else -> throw IllegalArgumentException("Invalid SharesTab")
        }
    } else {
        when (this) {
            SharesTab.INCOMING_TAB -> IconR.drawable.ic_folder_incoming_medium_regular_outline
            SharesTab.OUTGOING_TAB -> IconR.drawable.ic_folder_outgoing_medium_regular_outline
            SharesTab.LINKS_TAB -> IconR.drawable.ic_link01_medium_regular_outline
            else -> throw IllegalArgumentException("Invalid SharesTab")
        }
    }
}

private val SharesTab.stringRes: Int
    get() = when (this) {
        SharesTab.INCOMING_TAB -> R.string.tab_incoming_shares
        SharesTab.OUTGOING_TAB -> R.string.tab_outgoing_shares
        SharesTab.LINKS_TAB -> R.string.tab_links_shares
        else -> throw IllegalArgumentException("Invalid SharesTab")
    }

@Preview(showBackground = true)
@Composable
private fun SharesScreenPreview() {
    OriginalTempTheme(isSystemInDarkTheme()) {
        SharesScreen()
    }
}

/**
 * Tab row test tag
 */
const val TAB_ROW_TEST_TAG = "shares_screen:tab_row"
const val MENU_SEARCH_TEST_TAG = "shares_view:action_search"
const val MENU_MORE_TEST_TAG = "shares_view:action_more"
