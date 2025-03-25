package mega.privacy.android.app.mediaplayer.queue.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.media3.ui.PlayerView
import mega.privacy.android.app.databinding.SimpleAudioPlayerBinding
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Simple audio player view for showing the audio player in the audio queue
 *
 * @param setupAudioPlayer callback for Setup player view
 * @param modifier Modifier
 */
@Composable
fun SimpleAudioPlayerView(
    setupAudioPlayer: (PlayerView) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidViewBinding(
        modifier = modifier.wrapContentHeight(),
        factory = { inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean ->
            SimpleAudioPlayerBinding.inflate(inflater, parent, attachToParent).apply {
                setupAudioPlayer(playerView)
            }
        }
    )
}


@CombinedThemePreviews
@Composable
private fun SimpleAudioPlayerViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SimpleAudioPlayerView(
            setupAudioPlayer = {}
        )
    }
}