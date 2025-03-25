package mega.privacy.android.legacy.core.ui.controls.appbar

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
@Deprecated(
    message = "This component doesn't follow our design system correctly",
    replaceWith = ReplaceWith("MegaAppBar")
)
fun SimpleNoTitleTopAppBar(
    modifier: Modifier = Modifier,
    elevation: Boolean,
    isEnabled: Boolean = true,
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = "")
        },
        navigationIcon = {
            IconButton(
                onClick = onBackPressed,
                enabled = isEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSimpleAppBarPreview")
@Composable
fun PreviewSimpleNoTitleTopAppBar() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SimpleNoTitleTopAppBar(elevation = false, onBackPressed = {})
    }
}