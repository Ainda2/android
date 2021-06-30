package mega.privacy.android.app.textEditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.constants.EventConstants.EVENT_PERFORM_SCROLL
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.addExtraOnDrawBehaviour
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.initTextPaint
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.updatePaddingsAndView

open class LineNumberTextView : AppCompatTextView {

    private val textPaint = Paint()

    private var lineNumberEnabled = false
    private var firstLineNumber = 1

    private var pendingScrollText: String? = null

    init {
        initTextPaint(textPaint)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        addExtraOnDrawBehaviour(lineNumberEnabled, firstLineNumber, canvas, textPaint)
        super.onDraw(canvas)

        if (pendingScrollText != null) {
            getScrollToText(pendingScrollText)
            pendingScrollText = null
        }
    }

    /**
     * Enables or disabled the behaviour to show line numbers.
     *
     * @param lineNumberEnabled True if should show line numbers, false otherwise.
     */
    fun setLineNumberEnabled(lineNumberEnabled: Boolean) {
        this.lineNumberEnabled = lineNumberEnabled
        updatePaddingsAndView(lineNumberEnabled)
    }

    /**
     * Sets the text to be displayed and updates the value to show as first line number.
     *
     * @param text            Text to be displayed.
     * @param firstLineNumber Number to show as first line number.
     */
    fun setText(text: CharSequence?, firstLineNumber: Int) {
        this.firstLineNumber = firstLineNumber
        this.text = text
    }

    /**
     * Gets the text where the ScrollView should scroll after rotate the screen.
     *
     * @param scroll ScrollY of the ScrollView which contains this view.
     * @return The text to scroll.
     */
    fun getScrollText(scroll: Int): String {
        val scrollLine = layout.getLineForVertical(scroll)
        var scrollText = ""

        for (i in scrollLine until lineCount) {
            scrollText += text.substring(layout.getLineStart(i), layout.getLineEnd(i))
        }

        return scrollText
    }

    /**
     * Gets the scrollY value to scroll the ScrollView after rotate the screen.
     *
     * @param scrollText The text where the ScrollView should scroll.
     */
    fun getScrollToText(scrollText: String?) {
        if (scrollText == null) {
            return
        }

        if (layout == null) {
            pendingScrollText = scrollText
            return
        }

        val offset = text.indexOf(scrollText)
        val line = layout.getLineForOffset(offset)

        LiveEventBus.get(EVENT_PERFORM_SCROLL, Int::class.java).post(layout.getLineTop(line))
    }
}