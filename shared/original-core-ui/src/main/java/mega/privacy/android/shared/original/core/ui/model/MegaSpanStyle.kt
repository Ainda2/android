package mega.privacy.android.shared.original.core.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import mega.privacy.android.shared.original.core.ui.controls.text.megaSpanStyle
import mega.android.core.ui.theme.values.TextColor

/**
 * MegaSpanStyle : SpanStyle with TextColor
 *
 * @param spanStyle [SpanStyle]
 * @param color [TextColor]
 */
data class MegaSpanStyle(val spanStyle: SpanStyle, val color: TextColor?) {
    constructor(spanStyle: SpanStyle) : this(spanStyle, null)
    constructor(color: TextColor) : this(SpanStyle(), color)

    /**
     * Converts MegaSpanStyle to SpanStyle
     */
    @Composable
    internal fun toSpanStyle(): SpanStyle {
        return color?.let {
            megaSpanStyle(base = spanStyle, color = it)
        } ?: spanStyle
    }
}
