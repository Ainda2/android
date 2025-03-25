package mega.privacy.android.app.presentation.login.onboarding.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList

/**
 * A model represents the onboarding ui item
 *
 * @property imageDrawableId The drawable resource ID for the image.
 * @property titleStringId The string resource ID for the title.
 * @property subtitleStringId The string resource ID for the subtitle.
 * @property gradientColors The list of colors for the gradient background.
 */
data class OnboardingUiItem(
    @DrawableRes val imageDrawableId: Int,
    @StringRes val titleStringId: Int,
    @StringRes val subtitleStringId: Int,
    val gradientColors: ImmutableList<Color>,
)
