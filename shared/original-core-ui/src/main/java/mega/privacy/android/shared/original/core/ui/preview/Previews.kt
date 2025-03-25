package mega.privacy.android.shared.original.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview


/**
 * Annotation to generate previews with multiple font scales
 */
@Preview(
    showBackground = true,
    name = "4-Small font",
    group = "font scales",
    fontScale = 0.7f
)
@Preview(
    showBackground = true,
    name = "5-Large font",
    group = "font scales",
    fontScale = 1.5f
)
private annotation class FontScalePreviews

/**
 * Annotation to generate a preview with french locale
 */
@Preview(
    locale = "fr",
    name = "3-French locale",
    group = "locales",
    showBackground = true,
)
private annotation class FrenchLocale

/**
 * Annotation to generate a preview with Arabic locale
 */
@Preview(
    locale = "ar",
    name = "4-Arabic locale",
    group = "locales",
    showBackground = true,
)
private annotation class ArabicLocale


/**
 * Annotation to generate previews with night and day themes with full screen device UI
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF18191A,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
    device = "spec:width=1080px,height=2340px,dpi=440,navigation=buttons",
)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
    device = "spec:width=1080px,height=2340px,dpi=440,navigation=buttons",
)
annotation class CombinedThemePreviews

/**
 * Annotation to generate previews with night and day themes without device UI
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF18191A,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
annotation class CombinedThemeComponentPreviews

/**
 * Annotation to generate previews with night and day themes for tablet landscape
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF18191A,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1280dp,height=800dp"

)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=1280dp,height=800dp"

)
annotation class CombinedThemeTabletLandscapePreviews

/**
 * Annotation to generate previews with night and day themes for tablet portrait
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF18191A,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=800dp,height=1280dp"

)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=800dp,height=1280dp"

)
annotation class CombinedThemeTabletPortraitPreviews

/**
 * Annotation to generate previews with night and day themes and RTL layout
 */
@ArabicLocale
@CombinedThemePreviews
annotation class CombinedThemeRtlPreviews

/**
 * Annotation to generate previews for views with texts (font scales and locales) and night and day themes
 */
@FrenchLocale
@FontScalePreviews
@CombinedThemeComponentPreviews
annotation class CombinedTextAndThemePreviews

/**
 * Annotation to generate previews with night and day themes for phone landscape
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF18191A,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=800dp,height=360dp"
)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=800dp,height=360dp"

)
annotation class CombinedThemePhoneLandscapePreviews
