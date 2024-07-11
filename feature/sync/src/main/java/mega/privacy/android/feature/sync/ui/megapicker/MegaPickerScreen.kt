package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.createnewfolder.CreateNewFolderDialog
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderMenuAction
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaApiJava

@Composable
internal fun MegaPickerScreen(
    currentFolder: Node?,
    nodes: List<TypedNodeUiModel>,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    errorMessageId: Int?,
    errorMessageShown: () -> Unit,
    isSelectEnabled: Boolean,
    onCreateNewFolderDialogSuccess: (String) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val showCurrentFolderName =
        currentFolder != null &&
                currentFolder.parentId != NodeId(MegaApiJava.INVALID_HANDLE)

    var showCreateNewFolderDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = currentFolder?.name?.takeIf { showCurrentFolderName }
                    ?: stringResource(R.string.sync_toolbar_title),
                subtitle = if (showCurrentFolderName) {
                    null
                } else {
                    "Select folder"
                },
                elevation = 0.dp,
                onNavigationPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                },
                actions = mutableListOf<MenuAction>(CreateNewFolderMenuAction()),
                onActionPressed = {
                    when (it) {
                        is CreateNewFolderMenuAction -> {
                            showCreateNewFolderDialog = true
                        }
                    }
                },
            )
        }, content = { paddingValues ->
            MegaPickerScreenContent(
                nodes = nodes,
                folderClicked = folderClicked,
                currentFolderSelected = currentFolderSelected,
                fileTypeIconMapper = fileTypeIconMapper,
                modifier = Modifier.padding(paddingValues),
                isSelectEnabled = isSelectEnabled,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                MegaSnackbar(snackbarData = data, modifier = Modifier.padding(bottom = 56.dp))
            }
        }
    )

    if (showCreateNewFolderDialog) {
        currentFolder?.let {
            CreateNewFolderDialog(
                currentFolder = currentFolder,
                onSuccess = { newFolderName ->
                    showCreateNewFolderDialog = false
                    onCreateNewFolderDialogSuccess(newFolderName)
                },
                onCancel = { showCreateNewFolderDialog = false },
            )
        }
    }

    LaunchedEffect(errorMessageId) {
        if (errorMessageId != null) {
            snackbarHostState.showSnackbar(
                message = context.resources.getString(errorMessageId),
            )
            errorMessageShown()
        }
    }
}

@Composable
private fun MegaPickerScreenContent(
    nodes: List<TypedNodeUiModel>,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    isSelectEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        MegaFolderPickerView(
            modifier = Modifier
                .padding(start = 12.dp, top = 8.dp, end = 12.dp)
                .weight(1f),
            onSortOrderClick = {},
            onChangeViewTypeClick = {},
            nodesList = nodes,
            sortOrder = "",
            showSortOrder = false,
            showChangeViewType = false,
            listState = LazyListState(),
            onFolderClick = {
                folderClicked(it)
            },
            fileTypeIconMapper = fileTypeIconMapper
        )

        Box(
            Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
                textId = R.string.sync_general_select_to_download,
                onClick = {
                    currentFolderSelected()
                },
                enabled = isSelectEnabled,
            )
        }
    }

}

@CombinedThemePreviews
@Composable
private fun SyncNewFolderScreenPreview(
    @PreviewParameter(BooleanProvider::class) isSelectEnabled: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaPickerScreen(
            null,
            SampleNodeDataProvider.values,
            {},
            {},
            FileTypeIconMapper(),
            errorMessageId = null,
            errorMessageShown = {},
            isSelectEnabled = isSelectEnabled,
        )
    }
}