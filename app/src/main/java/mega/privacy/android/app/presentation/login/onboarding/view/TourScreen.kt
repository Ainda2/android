package mega.privacy.android.app.presentation.login.onboarding.view

import mega.privacy.android.shared.resources.R as SharedR
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.imageLoader
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.isTablet
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.presentation.login.onboarding.model.OnboardingUiItem
import mega.privacy.android.app.presentation.login.onboarding.model.TourUiState
import mega.privacy.android.app.presentation.meeting.view.dialog.PasteMeetingLinkGuestDialog
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.pager.InfiniteHorizontalPagerWithIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import timber.log.Timber
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TourRoute(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onOpenLink: (meetingLink: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TourViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TourScreen(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        uiState = uiState,
        onLoginClick = onLoginClick,
        onCreateAccountClick = onCreateAccountClick,
        onMeetingLinkChange = viewModel::onMeetingLinkChange,
        onConfirmMeetingLinkClick = viewModel::onConfirmMeetingLinkClick,
        onOpenLink = {
            onOpenLink(it)
            viewModel.resetOpenLink()
        },
        onClearLogoutProgressFlag = viewModel::clearLogoutProgressFlag
    )
}

@Composable
internal fun TourScreen(
    uiState: TourUiState,
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onMeetingLinkChange: (String) -> Unit,
    onConfirmMeetingLinkClick: () -> Unit,
    onOpenLink: (meetingLink: String) -> Unit,
    onClearLogoutProgressFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    var shouldShowPasteMeetingLinkGuestDialog by rememberSaveable { mutableStateOf(false) }

    // For small screen like nexus one or bigger screen, this is to force the scroll view to bottom to show buttons
    // Meanwhile, tour image glide could also be shown
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    LaunchedEffect(uiState.shouldOpenLink) {
        if (uiState.shouldOpenLink) {
            onOpenLink(uiState.meetingLink)
            onMeetingLinkChange("")
            shouldShowPasteMeetingLinkGuestDialog = false
        }
    }

    val bluetoothLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("onActivityResult: PERMISSION GRANTED")
                onClearLogoutProgressFlag()
                shouldShowPasteMeetingLinkGuestDialog = true
            } else {
                Timber.d("onActivityResult: PERMISSION DENIED")
                coroutineScope.launch {
                    val result = snackBarHostState.showAutoDurationSnackbar(
                        message = context.getString(R.string.meeting_bluetooth_connect_required_permissions_warning),
                        actionLabel = context.getString(R.string.action_settings)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.navigateToAppSettings()
                    }
                }
            }
        }

    Box(modifier = modifier.navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalTourPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .conditional(context.isTablet() || isLandscape) {
                            weight(1f)
                        }
                        .padding(bottom = if (isLandscape) 20.dp else 40.dp),
                    item = getOnboardingUiItem()
                )

                Row(
                    modifier = Modifier.padding(top = if (isLandscape) 24.dp else 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextMegaButton(
                        modifier = Modifier.testTag(BUTTON_LOGIN_TAG),
                        textId = sharedR.string.login_text,
                        onClick = onLoginClick
                    )

                    RaisedDefaultMegaButton(
                        modifier = Modifier.testTag(BUTTON_CREATE_ACCOUNT_TAG),
                        textId = R.string.create_account,
                        onClick = onCreateAccountClick
                    )
                }
            }

            OutlinedMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .conditional(isLandscape) {
                        wrapContentWidth()
                    }
                    .align(Alignment.CenterHorizontally)
                    .navigationBarsPadding()
                    .padding(
                        bottom = if (isLandscape) 8.dp else 40.dp,
                        start = 24.dp,
                        end = 24.dp,
                        top = if (isLandscape) 8.dp else 16.dp
                    )
                    .testTag(JOIN_A_MEETING_AS_GUEST_TAG),
                textId = R.string.join_meeting_as_guest,
                onClick = {
                    Timber.d("onJoinMeetingAsGuestClick")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                onClearLogoutProgressFlag()
                                shouldShowPasteMeetingLinkGuestDialog = true
                            }

                            else -> {
                                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        }
                    } else {
                        onClearLogoutProgressFlag()
                        shouldShowPasteMeetingLinkGuestDialog = true
                    }
                },
                rounded = false
            )
        }

        SnackbarHost(
            modifier = Modifier
                .safeDrawingPadding()
                .align(Alignment.BottomCenter),
            hostState = snackBarHostState
        )
    }

    if (shouldShowPasteMeetingLinkGuestDialog) {
        PasteMeetingLinkGuestDialog(
            modifier = Modifier.testTag(PASTE_MEETING_LINK_DIALOG_TAG),
            meetingLink = uiState.meetingLink,
            errorText = uiState.errorTextId?.let { stringResource(id = uiState.errorTextId) },
            onTextChange = onMeetingLinkChange,
            onConfirm = onConfirmMeetingLinkClick,
            onCancel = {
                shouldShowPasteMeetingLinkGuestDialog = false
                onMeetingLinkChange("")
            }
        )
    }
}

private fun getOnboardingUiItem() = listOf(
    OnboardingUiItem(
        imageDrawableId = R.drawable.tour1,
        titleStringId = SharedR.string.cloud_drive_tour_first_title,
        subtitleStringId = SharedR.string.cloud_drive_tour_first_subtitle,
        gradientColors = listOf(
            Color(0xFFFF8989),
            Color(0xFFFF5252)
        ).toImmutableList()
    ),
    OnboardingUiItem(
        imageDrawableId = R.drawable.tour2,
        titleStringId = SharedR.string.cloud_drive_tour_second_title,
        subtitleStringId = SharedR.string.cloud_drive_tour_second_subtitle,
        gradientColors = listOf(
            Color(0xFF55D2F0),
            Color(0xFF2BA6DE)
        ).toImmutableList()
    ),
    OnboardingUiItem(
        imageDrawableId = R.drawable.tour3,
        titleStringId = SharedR.string.cloud_drive_tour_third_title,
        subtitleStringId = SharedR.string.cloud_drive_tour_third_subtitle,
        gradientColors = listOf(
            Color(0xFFFFA700),
            Color(0xFFFF6F00)
        ).toImmutableList()
    ),
    OnboardingUiItem(
        imageDrawableId = R.drawable.tour4,
        titleStringId = SharedR.string.cloud_drive_tour_fourth_title,
        subtitleStringId = SharedR.string.cloud_drive_tour_fourth_subtitle,
        gradientColors = listOf(
            Color(0xFF00BDB2),
            Color(0xFF00897B)
        ).toImmutableList()
    ),
    OnboardingUiItem(
        imageDrawableId = R.drawable.tour5,
        titleStringId = SharedR.string.cloud_drive_tour_fifth_title,
        subtitleStringId = SharedR.string.cloud_drive_tour_fifth_subtitle,
        gradientColors = listOf(
            Color(0xFFC86DD7),
            Color(0xFFA037F3)
        ).toImmutableList()
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalTourPager(
    item: List<OnboardingUiItem>,
    modifier: Modifier = Modifier,
) {
    InfiniteHorizontalPagerWithIndicator(
        modifier = modifier,
        pageCount = item.size,
        isOverScrollModeEnable = false,
        // We need to know the tallest height among the onboarding items.
        // Since the initial page is the middle item,
        // we only need to render the half left and right sides.
        beyondViewportPageCount = item.size / 2,
        verticalAlignment = Alignment.Top
    ) { page ->
        val context = LocalContext.current
        val isTablet = context.isTablet()
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val painter = rememberAsyncImagePainter(
                model = item[page].imageDrawableId,
                imageLoader = context.imageLoader.newBuilder()
                    .components { add(SvgDecoder.Factory()) }
                    .build()
            )
            val intrinsicSize = painter.intrinsicSize
            val imageRatio = if (intrinsicSize != Size.Unspecified) {
                intrinsicSize.width / intrinsicSize.height
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isTablet || isLandscape) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.aspectRatio(imageRatio)
                        }
                    ),
            ) {
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = item[page].gradientColors,
                            )
                        )
                        .testTag("${IMAGE_BACKGROUND_TOUR_TAG}_$page")
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .testTag("${IMAGE_RESOURCE_TOUR_TAG}_$page"),
                    painter = painter,
                    contentScale = ContentScale.Fit,
                    contentDescription = stringResource(id = item[page].titleStringId)
                )
            }
            MegaText(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .testTag("${TEXT_TOUR_TITLE_TAG}_$page"),
                text = stringResource(id = item[page].titleStringId),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.h6
            )

            MegaText(
                modifier = Modifier
                    .padding(top = 8.dp, start = 24.dp, end = 24.dp)
                    .testTag("${TEXT_TOUR_DESCRIPTION_TAG}_$page"),
                text = stringResource(id = item[page].subtitleStringId),
                textAlign = TextAlign.Center,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2
            )
        }
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun TourScreenPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        TourScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = TourUiState(),
            onLoginClick = {},
            onCreateAccountClick = {},
            onMeetingLinkChange = {},
            onConfirmMeetingLinkClick = {},
            onOpenLink = {},
            onClearLogoutProgressFlag = {}
        )
    }
}

internal const val JOIN_A_MEETING_AS_GUEST_TAG = "tour_screen:button_join_a_meeting_as_guest"
internal const val PASTE_MEETING_LINK_DIALOG_TAG = "tour_screen:dialog_join_a_meeting_as_guest"
internal const val BUTTON_LOGIN_TAG = "tour_screen:button_login"
internal const val BUTTON_CREATE_ACCOUNT_TAG = "tour_screen:button_create_account"
internal const val TEXT_TOUR_TITLE_TAG = "tour_screen:text_tour_title"
internal const val TEXT_TOUR_DESCRIPTION_TAG = "tour_screen:text_tour_description"
internal const val IMAGE_RESOURCE_TOUR_TAG = "tour_screen:image_resource"
internal const val IMAGE_BACKGROUND_TOUR_TAG = "tour_screen:image_background"
