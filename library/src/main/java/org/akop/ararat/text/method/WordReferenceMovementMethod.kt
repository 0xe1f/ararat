// Copyright (c) Akop Karapetyan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.akop.ararat.text.method

import android.text.NoCopySpan
import android.text.Selection
import android.text.Spannable
import android.text.method.ScrollingMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

import org.akop.ararat.core.Crossword
import org.akop.ararat.util.ReferenceScanner
import org.akop.ararat.util.htmlEncode
import java.util.regex.Pattern

/**
 * This class is a modified version of LinkMovementMethod that handles
 * URL's of form wordRef://direction/number by invoking listener
 * OnWordReferenceSelectedListener (if specified)
 */
@Suppress("unused")
class WordReferenceMovementMethod : ScrollingMovementMethod() {

    var wordReferenceSelectedListener: OnWordReferenceSelectedListener? = null

    interface OnWordReferenceSelectedListener {
        fun onWordReferenceSelected(direction: Int, number: Int)
        fun onCitationRequested(direction: Int, number: Int)
    }

    override fun canSelectArbitrarily(): Boolean = true

    override fun handleMovementKey(widget: TextView, buffer: Spannable, keyCode: Int,
                                   movementMetaState: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER ->
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)
                        && event.action == KeyEvent.ACTION_DOWN
                        && event.repeatCount == 0
                        && action(CLICK, widget, buffer)) {
                    return true
                }
        }

        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event)
    }

    override fun up(widget: TextView, buffer: Spannable): Boolean =
            if (action(UP, widget, buffer)) true else super.up(widget, buffer)

    override fun down(widget: TextView, buffer: Spannable): Boolean =
            if (action(DOWN, widget, buffer)) true else super.down(widget, buffer)

    override fun left(widget: TextView, buffer: Spannable): Boolean =
            if (action(UP, widget, buffer)) true else super.left(widget, buffer)

    override fun right(widget: TextView, buffer: Spannable): Boolean =
            if (action(DOWN, widget, buffer)) true else super.right(widget, buffer)

    private fun action(what: Int, widget: TextView, buffer: Spannable): Boolean {
        val layout = widget.layout

        val padding = widget.totalPaddingTop + widget.totalPaddingBottom
        val areatop = widget.scrollY
        val areabot = areatop + widget.height - padding

        val linetop = layout.getLineForVertical(areatop)
        val linebot = layout.getLineForVertical(areabot)

        val first = layout.getLineStart(linetop)
        val last = layout.getLineEnd(linebot)

        val candidates = buffer.getSpans(first, last, ClickableSpan::class.java)

        val a = Selection.getSelectionStart(buffer)
        val b = Selection.getSelectionEnd(buffer)

        var selStart = Math.min(a, b)
        var selEnd = Math.max(a, b)

        if (selStart < 0) {
            if (buffer.getSpanStart(FROM_BELOW) >= 0) {
                selEnd = buffer.length
                selStart = selEnd
            }
        }

        if (selStart > last) {
            selEnd = Integer.MAX_VALUE
            selStart = selEnd
        }
        if (selEnd < first) {
            selEnd = -1
            selStart = selEnd
        }

        when (what) {
            CLICK -> {
                if (selStart == selEnd) return false

                val link = buffer.getSpans(selStart, selEnd, ClickableSpan::class.java)
                if (link.size != 1)
                    return false

                handleClick(widget, link[0])
            }
            UP -> {
                var beststart = -1
                var bestend = -1

                for (i in candidates.indices) {
                    val end = buffer.getSpanEnd(candidates[i])
                    if (end < selEnd || selStart == selEnd) {
                        if (end > bestend) {
                            beststart = buffer.getSpanStart(candidates[i])
                            bestend = end
                        }
                    }
                }

                if (beststart >= 0) {
                    Selection.setSelection(buffer, bestend, beststart)
                    return true
                }
            }
            DOWN -> {
                var beststart = Integer.MAX_VALUE
                var bestend = Integer.MAX_VALUE

                for (i in candidates.indices) {
                    val start = buffer.getSpanStart(candidates[i])
                    if (start > selStart || selStart == selEnd) {
                        if (start < beststart) {
                            beststart = start
                            bestend = buffer.getSpanEnd(candidates[i])
                        }
                    }
                }

                if (bestend < Integer.MAX_VALUE) {
                    Selection.setSelection(buffer, beststart, bestend)
                    return true
                }
            }
        }

        return false
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable,
                              event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_DOWN -> {
                val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                val layout = widget.layout
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val link = buffer.getSpans(off, off, ClickableSpan::class.java)

                if (link.isNotEmpty()) {
                    when (event.action) {
                        MotionEvent.ACTION_UP -> handleClick(widget, link[0])
                        MotionEvent.ACTION_DOWN -> Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]))
                    }

                    return true
                } else {
                    Selection.removeSelection(buffer)
                }
            }
        }

        return super.onTouchEvent(widget, buffer, event)
    }

    override fun initialize(widget: TextView, text: Spannable) {
        Selection.removeSelection(text)
        text.removeSpan(FROM_BELOW)
    }

    override fun onTakeFocus(view: TextView, text: Spannable, dir: Int) {
        Selection.removeSelection(text)

        if (dir and View.FOCUS_BACKWARD != 0) {
            text.setSpan(FROM_BELOW, 0, 0, Spannable.SPAN_POINT_POINT)
        } else {
            text.removeSpan(FROM_BELOW)
        }
    }

    private fun handleClick(widget: TextView, span: ClickableSpan) {
        wordReferenceSelectedListener?.let { listener ->
            if (span is URLSpan) {
                val m = EXTRACT_REF.matcher(span.url)
                if (m.find()) {
                    val dir = m.group(2).toInt()
                    val number = m.group(3).toInt()

                    when (m.group(1)) {
                        PROTOCOL_REF -> {
                            listener.onWordReferenceSelected(dir, number)
                            return
                        }
                        PROTOCOL_CITATION -> {
                            listener.onCitationRequested(dir, number)
                            return
                        }
                    }
                }
            }
        }

        span.onClick(widget)
    }

    companion object {
        private val EXTRACT_REF = Pattern.compile("^(\\w+)://(\\d+)/(\\d+)$")

        private const val PROTOCOL_REF = "ref"
        private const val PROTOCOL_CITATION = "cite"

        private const val CLICK = 1
        private const val UP    = 2
        private const val DOWN  = 3

        fun linkify(word: Crossword.Word, hint: String?, crossword: Crossword): String =
                buildString {
                    val hintContent = hint ?: word.hint ?: ""
                    when {
                        word.hintUrl != null -> {
                            append("<a href=\"${word.hintUrl}\">")
                            append(hintContent.htmlEncode())
                            append("</a>")
                        }
                        word.citation != null -> {
                            append(hintContent.htmlEncode())
                            append(" <a href=\"$PROTOCOL_CITATION://${word.direction}/${word.number}\">&#8224;</a>")
                        }
                        else -> {
                            val refs = ReferenceScanner.findReferences(hintContent, crossword)
                            var start = 0
                            for ((number, direction, refStart, refEnd) in refs) {
                                append(hintContent.substring(start, refStart).htmlEncode())
                                append("<a href=\"$PROTOCOL_REF://$direction/$number\">")
                                append(hintContent.substring(refStart, refEnd).htmlEncode())
                                append("</a>")
                                start = refEnd
                            }

                            append(hintContent.substring(start).htmlEncode())
                        }
                    }
                }

        private val FROM_BELOW = NoCopySpan.Concrete()
    }
}
