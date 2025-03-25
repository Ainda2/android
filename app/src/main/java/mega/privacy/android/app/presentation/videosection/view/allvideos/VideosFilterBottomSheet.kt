package mega.privacy.android.app.presentation.videosection.view.allvideos

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.shared.original.core.ui.controls.lists.SettingsItemWithRadioButton
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun VideosFilterBottomSheet(
    modifier: Modifier,
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    title: String,
    options: List<VideosFilterOptionEntity>,
    onItemSelected: (VideosFilterOptionEntity) -> Unit,
) {
    BottomSheet(
        modifier = modifier,
        modalSheetState = modalSheetState,
        sheetBody = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                MegaText(
                    modifier = Modifier.padding(16.dp),
                    text = title,
                    textColor = TextColor.Primary
                )

                options.map { option ->
                    SettingsItemWithRadioButton(
                        modifier = Modifier.padding(vertical = 5.dp),
                        title = option.title,
                        selected = option.isSelected,
                        onClick = { onItemSelected(option) }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun VideosLocationFilterBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideosFilterBottomSheet(
            modifier = Modifier,
            title = "Location",
            modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                skipHalfExpanded = false,
            ),
            coroutineScope = rememberCoroutineScope(),
            options = LocationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = false
                )
            },
            onItemSelected = {}
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun VideosDurationFilterBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideosFilterBottomSheet(
            modifier = Modifier,
            title = "Duration",
            modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                skipHalfExpanded = false,
            ),
            coroutineScope = rememberCoroutineScope(),
            options = DurationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = option == DurationFilterOption.MoreThan20
                )
            },
            onItemSelected = {}
        )
    }
}