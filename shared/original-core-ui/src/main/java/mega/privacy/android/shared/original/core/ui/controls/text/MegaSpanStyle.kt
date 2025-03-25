package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.PlatformSpanStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.TextUnit
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Mega span style
 *
 * @param color
 * @param fontSize
 * @param fontWeight
 * @param fontStyle
 * @param fontSynthesis
 * @param fontFamily
 * @param fontFeatureSettings
 * @param letterSpacing
 * @param baselineShift
 * @param textGeometricTransform
 * @param localeList
 * @param textDecoration
 * @param shadow
 * @param platformStyle
 * @param drawStyle
 */
@Composable
fun megaSpanStyle(
    color: TextColor? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontSynthesis: FontSynthesis? = null,
    fontFamily: FontFamily? = null,
    fontFeatureSettings: String? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    baselineShift: BaselineShift? = null,
    textGeometricTransform: TextGeometricTransform? = null,
    localeList: LocaleList? = null,
    textDecoration: TextDecoration? = null,
    shadow: Shadow? = null,
    platformStyle: PlatformSpanStyle? = null,
    drawStyle: DrawStyle? = null,
) = SpanStyle(
    color = color?.let { MegaOriginalTheme.textColor(textColor = color) } ?: Color.Unspecified,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontSynthesis = fontSynthesis,
    fontFamily = fontFamily,
    fontFeatureSettings = fontFeatureSettings,
    letterSpacing = letterSpacing,
    baselineShift = baselineShift,
    textGeometricTransform = textGeometricTransform,
    localeList = localeList,
    textDecoration = textDecoration,
    shadow = shadow,
    platformStyle = platformStyle,
    drawStyle = drawStyle,
    background = Color.Unspecified,
)

/**
 * Mega span style
 *
 * @param base
 * @param color
 */
@Composable
fun megaSpanStyle(
    base: SpanStyle,
    color: TextColor,
) = base.copy(color = MegaOriginalTheme.textColor(textColor = color))