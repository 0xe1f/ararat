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

package org.akop.ararat.graphics

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

import org.akop.ararat.core.Crossword
import org.akop.ararat.core.CrosswordState

import java.io.FileOutputStream
import java.io.IOException


class CrosswordRenderer(context: Context) {

    private val cellStrokePaint = Paint()
    private val cellFillPaint = Paint()
    private val puzzleBackgroundPaint = Paint()
    private val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scaledDensity: Float = 0f
    private var cellStrokeWidthPx: Float = 0f
        set(value) {
            field = value
            cellStrokePaint.strokeWidth = value
            puzzleBackgroundPaint.strokeWidth = value
        }

    init {
        init(context.resources)
    }

    private fun init(res: Resources) {
        val dm = res.displayMetrics

        scaledDensity = dm.scaledDensity

        cellStrokePaint.color = CELL_STROKE_COLOR
        cellStrokePaint.style = Paint.Style.STROKE

        cellFillPaint.color = CELL_FILL_COLOR
        cellFillPaint.style = Paint.Style.FILL

        puzzleBackgroundPaint.color = PUZZLE_BG_COLOR
        puzzleBackgroundPaint.style = Paint.Style.FILL

        circleStrokePaint.color = CIRCLE_STROKE_COLOR
        circleStrokePaint.style = Paint.Style.STROKE

        cellStrokeWidthPx = DEFAULT_CELL_STROKE_WIDTH_DP * dm.density
    }

    private fun renderCell(rp: RenderParams, row: Int, col: Int, cellRect: RectF) {
        val cell = rp.crossword.cellMap[row][col] ?: return

        rp.canvas.drawRect(cellRect, cellFillPaint)
        rp.canvas.drawRect(cellRect, cellStrokePaint)

        if (cell.isCircled && rp.flags and FLAG_RENDER_MARKERS == FLAG_RENDER_MARKERS) {
            rp.canvas.drawCircle(cellRect.centerX(), cellRect.centerY(),
                    rp.radius, circleStrokePaint)
        }

        when {
            rp.flags and FLAG_RENDER_ANSWER == FLAG_RENDER_ANSWER -> if (!cell.isEmpty) {
                rp.canvas.drawText(cell.chars, cellRect.left + rp.cellDim / 2,
                        cellRect.bottom - rp.answerMetrics.descent, rp.answerTextPaint)
            }
            rp.flags and FLAG_RENDER_ATTEMPT == FLAG_RENDER_ATTEMPT -> {
                rp.state?.charAt(row, col)?.let {
                    val text = if (it.length > 8) it.substring(0, 8) + "…" else it
                    var textSize = rp.answerTextSize
                    var textWidth: Float

                    do {
                        rp.answerTextPaint.textSize = textSize
                        textWidth = rp.answerTextPaint.measureText(text)
                        textSize -= scaledDensity
                    } while (textWidth >= rp.answerTextSize)

                    rp.answerTextPaint.getTextBounds("A", 0, 1, rp.tempRect)
                    val xOffset = textWidth / 2f
                    val yOffset = (rp.tempRect.height() / 2).toFloat()

                    rp.canvas.drawText(text, cellRect.centerX() - xOffset,
                            cellRect.centerY() + yOffset, rp.answerTextPaint)
                }
            }
        }
    }

    fun renderToCanvas(canvas: Canvas, crossword: Crossword,
                       state: CrosswordState?, flags: Int) {
        val rp = RenderParams(canvas, crossword, state, flags)
        val puzzleRect = RectF(0f, 0f, rp.bitmapWidth.toFloat(),
                rp.bitmapHeight.toFloat())

        canvas.drawRect(puzzleRect, puzzleBackgroundPaint)

        val renderedWidth = rp.cellDim * crossword.width.toFloat()
        val renderedHeight = rp.cellDim * crossword.height.toFloat()

        val cellRect = RectF()
        val leftmost = (rp.bitmapWidth.toFloat() - renderedWidth) / 2f
        var rectTop = (rp.bitmapHeight.toFloat() - renderedHeight) / 2f

        for (i in 0 until crossword.height) {
            var rectLeft = leftmost
            for (j in 0 until crossword.width) {
                cellRect.set(rectLeft, rectTop, rectLeft + rp.cellDim, rectTop + rp.cellDim)
                renderCell(rp, i, j, cellRect)
                rectLeft += rp.cellDim
            }
            rectTop += rp.cellDim
        }
    }

    @Throws(IOException::class)
    fun renderToFile(path: String,
                     crossword: Crossword, state: CrosswordState?,
                     width: Int, height: Int, flags: Int) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        renderToCanvas(Canvas(bmp), crossword, state, flags)

        FileOutputStream(path).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 80, it)
        }
    }

    private inner class RenderParams(val canvas: Canvas,
                                     val crossword: Crossword,
                                     val state: CrosswordState?,
                                     val flags: Int) {

        val cellDim: Float
        val radius: Float
        val answerTextSize: Float
        val answerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bitmapWidth: Int = canvas.width
        val bitmapHeight: Int = canvas.height
        val answerMetrics: Paint.FontMetrics
        val tempRect = Rect()

        init {
            val longestCwDim = Math.max(crossword.width, crossword.height)
            val shortestWantedDim = Math.min(bitmapWidth, bitmapHeight).toFloat()
            cellDim = shortestWantedDim / longestCwDim - cellStrokeWidthPx / longestCwDim
            radius = cellDim / 2 - circleStrokePaint.strokeWidth
            answerTextSize = cellDim * 0.75f
            answerTextPaint.color = TEXT_COLOR
            answerMetrics = answerTextPaint.fontMetrics
        }
    }

    companion object {
        const val FLAG_RENDER_ANSWER  = 1
        const val FLAG_RENDER_MARKERS = 2
        const val FLAG_RENDER_ATTEMPT = 4

        private val CELL_STROKE_COLOR = Color.parseColor("#000000")
        private val CELL_FILL_COLOR = Color.parseColor("#ffffff")
        private val PUZZLE_BG_COLOR = Color.parseColor("#000000")
        private val CIRCLE_STROKE_COLOR = Color.parseColor("#555555")
        private val TEXT_COLOR = Color.parseColor("#000000")

        private const val DEFAULT_CELL_STROKE_WIDTH_DP = 1f
    }
}
