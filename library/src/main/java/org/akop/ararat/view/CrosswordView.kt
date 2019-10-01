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

package org.akop.ararat.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Scroller

import org.akop.ararat.R
import org.akop.ararat.core.Crossword
import org.akop.ararat.core.CrosswordState
import org.akop.ararat.util.*
import org.akop.ararat.view.inputmethod.CrosswordInputConnection
import org.akop.ararat.widget.Zoomer
import java.lang.ref.WeakReference

import java.util.HashSet
import java.util.Stack
import kotlin.math.absoluteValue


typealias InputValidator = (String) -> Boolean

@Suppress("unused", "MemberVisibilityCanBePrivate")
class CrosswordView(context: Context, attrs: AttributeSet?) : View(context, attrs), View.OnKeyListener {

    interface OnStateChangeListener {
        fun onCrosswordChanged(view: CrosswordView)
        fun onCrosswordSolved(view: CrosswordView)
        fun onCrosswordUnsolved(view: CrosswordView)
    }

    interface OnSelectionChangeListener {
        fun onSelectionChanged(view: CrosswordView,
                               word: Crossword.Word?, position: Int)
    }

    interface OnLongPressListener {
        fun onCellLongPressed(view: CrosswordView,
                              word: Crossword.Word, cell: Int)
    }

    private val defaultInputValidator: InputValidator = { ch ->
        val upper = ch.toUpperCase()
        !(0..ch.length).any { !isAcceptableChar(upper[it]) }
    }

    private val contentRect = RectF() // Content rectangle - bounds of the view
    private val puzzleRect = RectF() // Puzzle rectangle - basically the output size of the bitmap
    private var scaledCellSize = 0f

    private val cellStrokePaint = Paint()
    private val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellFillPaint = Paint()
    private val cheatedCellFillPaint = Paint()
    private val mistakeCellFillPaint = Paint()
    private val markedCellFillPaint = Paint()
    private val selectedWordFillPaint = Paint()
    private val selectedCellFillPaint = Paint()
    private val numberTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val numberStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val answerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cellSize: Float = 0f
    private val markerSideLength: Float
    private val circleRadius: Float
    private var numberTextPadding: Float = 0f
    private val numberTextHeight: Float
    private val scaledDensity: Float
    private var answerTextSize: Float = 0f
    private val undoBuffer = Stack<UndoItem>()

    private var puzzleWidth: Int = 0 // Total number of cells across
    private var puzzleHeight: Int = 0 // Total number of cells down
    private var puzzleCells: Array<Array<Cell?>> = EMPTY_CELLS // Map of the cells
    private var selection: Selectable? = null
    private val allowedChars = HashSet<Char>()

    private var minScaleFactor: Float = 0f // Scale at which the puzzle takes up the entire screen
    private var fitWidthScaleFactor: Float = 0f // Scale at which the puzzle fits in horizontally
    private var maxScaleFactor: Float = 0f // Scale beyond which the puzzle shouldn't be enlarged
    private var renderScale: Float = 0f // This is the scale at which the bitmap is rendered
    private var bitmapScale: Float = 0f // This is the scaling applied to the bitmap in touch resize mode
    private var scaleStart: Float = 0f // The value of mRenderScale when touch resizing begins
    private var bitmapOffset = PointF() // Offset of the rendered bitmap
    private val centeredOffset = PointF() // Offset at which the puzzle appears exactly in the middle
    private val translationBounds = RectF() // Bitmap translation limits

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    private var renderTask: RenderTask? = null
    private var puzzleCanvas: Canvas? = null
    private var puzzleBitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private var isZooming: Boolean = false
    private var ignoreZoom: Boolean = false
    private val zoomer: Zoomer
    private val scroller: Scroller
    private val revealSetsCheatFlag: Boolean = true
    private var isSolved: Boolean = false

    private var _inputMode: Int = INPUT_MODE_KEYBOARD
    private var _isEditable: Boolean = true
    private var _maxBitmapSize: Int = DEFAULT_MAX_BITMAP_DIMENSION

    private val tempRect = Rect()

    private val rendererLock = Any()
    private val inPlaceRenderer = Renderer(this)

    private val inputEventListener = object : CrosswordInputConnection.OnInputEventListener {

        override fun onWordEntered(text: CharSequence) {
            val sel = selection ?: return

            // Words like "ain't" contain punctuation marks that ain't
            // valid, but may appear in a crossword in punctuation-less
            // form. For this reason, we strip out invalid characters
            // before considering whether we want to fill them into the
            // selection.
            val chars = text.toString().toCharArray()

            // Copy all acceptable chars to a separate array
            val filtered = arrayOfNulls<String>(chars.size)
            var k = 0
            for (ch in chars) {
                if (isAcceptableChar(ch)) filtered[k++] = ch.toString()
            }

            // No valid chars
            if (k == 0) return

            val matrix: Array<Array<String?>>
            if (sel.direction == Crossword.Word.DIR_ACROSS) {
                matrix = Array(1) { arrayOfNulls<String?>(k) }
                System.arraycopy(filtered, 0, matrix[0], 0, k)
            } else {
                matrix = Array(k) { arrayOfNulls<String?>(1) }
                for (i in 0 until k) matrix[i][0] = filtered[i]
            }

            val s: Selectable? = when {
                sel.isCellWithinBounds(sel.cell + k - 1) -> {
                    // If there's enough room at the current position, add the new word
                    setChars(sel.row, sel.column, matrix, false)
                    Selectable(sel.word,
                            minOf(sel.cell + k, sel.word.length - 1))
                }
                k == sel.word.length -> {
                    // Not enough room from the current, but perfect fit for the entire row/col
                    setChars(sel.getRow(0), sel.getColumn(0), matrix, false)
                    Selectable(sel.word, k - 1)
                }
                else -> null
            }

            if (s != null) resetSelection(s)
        }

        override fun onWordCancelled() {
            handleBackspace()
        }

        override fun onEditorAction(actionCode: Int) {
            if (actionCode == EditorInfo.IME_ACTION_NEXT) {
                selectNextWord()
            }
        }
    }

    var skipOccupiedOnType: Boolean = false
    var skipCompletedWords: Boolean = false
    var selectFirstUnoccupiedOnNav: Boolean = true
    var undoMode: Int = 0
    var markerDisplayMode: Int = 0
        set(newMode) {
            if (markerDisplayMode != newMode) {
                val oldMode = markerDisplayMode
                field = newMode
                if (oldMode and MARKER_ERROR != newMode and MARKER_ERROR) {
                    resetErrorMarkers()
                }

                redrawInPlace()
            }
        }

    var onSelectionChangeListener: OnSelectionChangeListener? = null
    var onStateChangeListener: OnStateChangeListener? = null
    var onLongPressListener: OnLongPressListener? = null
    var inputValidator: InputValidator? = null

    var selectedWord: Crossword.Word?
        get() = selection?.word
        set(value) { selectWord(value) }

    val selectedCell: Int
        get() = selection?.cell ?: -1

    val state: CrosswordState?
        get() {
            val cw = crossword ?: return null
            val state = cw.newState()

            selection?.let { state.setSelection(it.direction, it.word.number, it.cell) }

            for (i in 0 until puzzleHeight) {
                for (j in 0 until puzzleWidth) {
                    puzzleCells[i][j]?.let { cell ->
                        if (!cell.isEmpty) state.setCharAt(i, j, cell.char)
                        state.setFlagAt(CrosswordState.FLAG_CHEATED,
                                i, j, cell.isFlagSet(Cell.FLAG_CHEATED))
                        state.setFlagAt(CrosswordState.FLAG_MARKED,
                                i, j, cell.isFlagSet(Cell.FLAG_MARKED))
                    }
                }
            }
            cw.updateStateStatistics(state)

            return state
        }

    var answerTypeface: Typeface?
        get() = answerTextPaint.typeface
        set(typeface) {
            if (typeface !== answerTypeface) {
                answerTextPaint.typeface = typeface
                redrawInPlace()
            }
        }

    var answerColor: Int
        get() = answerTextPaint.color
        set(value) {
            if (value != answerColor) {
                answerTextPaint.color = value
                redrawInPlace()
            }
        }

    var isEditable: Boolean
        get() = _isEditable
        set(value) {
            if (value != isEditable) {
                _isEditable = value
                resetInputMode()
            }
        }

    var inputMode: Int
        get() = _inputMode
        set(value) {
            if (value != inputMode) {
                _inputMode = value
                resetInputMode()
            }
        }

    /**
     * Sets maximum bitmap dimension, in pixels. Fiddling with this value
     * is currently not recommended. Default value is 2048.
     */
    var maxBitmapSize: Int
        get() = _maxBitmapSize
        set(value) {
            if (value != maxBitmapSize) {
                _maxBitmapSize = value
                resetConstraintsAndRedraw(true)
            }
        }

    // Will recompute when reset
    var crossword: Crossword? = null
        set(value) {
            field = value

            initializeCrossword()
            selectNextWord()

            renderScale = 0f
            resetConstraintsAndRedraw(true)
        }

    init {
        if (!isInEditMode)
            setLayerType(LAYER_TYPE_HARDWARE, null)

        // Set drawing defaults
        val r = context.resources
        val dm = r.displayMetrics

        var cellFillColor = NORMAL_CELL_FILL_COLOR
        var cheatedCellFillColor = CHEATED_CELL_FILL_COLOR
        var mistakeCellFillColor = MISTAKE_CELL_FILL_COLOR
        var selectedWordFillColor = SELECTED_WORD_FILL_COLOR
        var selectedCellFillColor = SELECTED_CELL_FILL_COLOR
        var markedCellFillColor = MARKED_CELL_FILL_COLOR
        var numberTextColor = NUMBER_TEXT_COLOR
        var cellStrokeColor = CELL_STROKE_COLOR
        var circleStrokeColor = CIRCLE_STROKE_COLOR
        var answerTextColor = ANSWER_TEXT_COLOR

        scaledDensity = dm.scaledDensity
        var numberTextSize = NUMBER_TEXT_SIZE * scaledDensity
        answerTextSize = ANSWER_TEXT_SIZE * scaledDensity

        cellSize = CELL_SIZE * dm.density
        numberTextPadding = NUMBER_TEXT_PADDING * dm.density

        // Read supplied attributes
        context.withStyledAttributes(R.styleable.CrosswordView, attrs) {
            cellSize = getDimension(R.styleable.CrosswordView_cellSize, cellSize)
            numberTextPadding = getDimension(R.styleable.CrosswordView_numberTextPadding, numberTextPadding)
            numberTextSize = getDimension(R.styleable.CrosswordView_numberTextSize, numberTextSize)
            answerTextSize = getDimension(R.styleable.CrosswordView_answerTextSize, answerTextSize)
            answerTextColor = getColor(R.styleable.CrosswordView_answerTextColor, answerTextColor)
            cellFillColor = getColor(R.styleable.CrosswordView_defaultCellFillColor, cellFillColor)
            cheatedCellFillColor = getColor(R.styleable.CrosswordView_cheatedCellFillColor, cheatedCellFillColor)
            mistakeCellFillColor = getColor(R.styleable.CrosswordView_mistakeCellFillColor, mistakeCellFillColor)
            selectedWordFillColor = getColor(R.styleable.CrosswordView_selectedWordFillColor, selectedWordFillColor)
            selectedCellFillColor = getColor(R.styleable.CrosswordView_selectedCellFillColor, selectedCellFillColor)
            markedCellFillColor = getColor(R.styleable.CrosswordView_markedCellFillColor, markedCellFillColor)
            cellStrokeColor = getColor(R.styleable.CrosswordView_cellStrokeColor, cellStrokeColor)
            circleStrokeColor = getColor(R.styleable.CrosswordView_circleStrokeColor, circleStrokeColor)
            numberTextColor = getColor(R.styleable.CrosswordView_numberTextColor, numberTextColor)
            _isEditable = getBoolean(R.styleable.CrosswordView_editable, _isEditable)
            skipOccupiedOnType = getBoolean(R.styleable.CrosswordView_skipOccupiedOnType, skipOccupiedOnType)
            skipCompletedWords = getBoolean(R.styleable.CrosswordView_skipCompletedWords, skipCompletedWords)
            selectFirstUnoccupiedOnNav = getBoolean(R.styleable.CrosswordView_selectFirstUnoccupiedOnNav,
                    selectFirstUnoccupiedOnNav)
        }

        markerSideLength = cellSize * MARKER_TRIANGLE_LENGTH_FRACTION

        // Init paints
        cellFillPaint.color = cellFillColor
        cellFillPaint.style = Paint.Style.FILL

        cheatedCellFillPaint.color = cheatedCellFillColor
        cheatedCellFillPaint.style = Paint.Style.FILL

        mistakeCellFillPaint.color = mistakeCellFillColor
        mistakeCellFillPaint.style = Paint.Style.FILL

        selectedWordFillPaint.color = selectedWordFillColor
        selectedWordFillPaint.style = Paint.Style.FILL

        selectedCellFillPaint.color = selectedCellFillColor
        selectedCellFillPaint.style = Paint.Style.FILL

        markedCellFillPaint.color = markedCellFillColor
        markedCellFillPaint.style = Paint.Style.FILL

        cellStrokePaint.color = cellStrokeColor
        cellStrokePaint.style = Paint.Style.STROKE
        // cellStrokePaint.setStrokeWidth(1);

        circleStrokePaint.color = circleStrokeColor
        circleStrokePaint.style = Paint.Style.STROKE
        circleStrokePaint.strokeWidth = 1f

        numberTextPaint.color = numberTextColor
        numberTextPaint.textAlign = Paint.Align.CENTER
        numberTextPaint.textSize = numberTextSize

        // Compute number height
        numberTextPaint.getTextBounds("0", 0, "0".length, tempRect)
        numberTextHeight = tempRect.height().toFloat()

        numberStrokePaint.color = cellFillColor
        numberStrokePaint.textAlign = Paint.Align.CENTER
        numberStrokePaint.textSize = numberTextSize
        numberStrokePaint.style = Paint.Style.STROKE
        numberStrokePaint.strokeWidth = NUMBER_TEXT_STROKE_WIDTH * scaledDensity

        answerTextPaint.color = answerTextColor
        answerTextPaint.textSize = answerTextSize

        // Init rest of the values
        circleRadius = cellSize / 2 - circleStrokePaint.strokeWidth

        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        scroller = Scroller(context, null, true)
        zoomer = Zoomer(context)

        isFocusableInTouchMode = _isEditable && _inputMode != INPUT_MODE_NONE
        setOnKeyListener(this)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)

        savedState.bitmapOffset = bitmapOffset
        savedState.renderScale = renderScale

        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        val savedState = state
        super.onRestoreInstanceState(savedState.superState)

        bitmapOffset = savedState.bitmapOffset
        renderScale = savedState.renderScale
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        var retVal = scaleDetector.onTouchEvent(ev)
        retVal = gestureDetector.onTouchEvent(ev) || retVal

        return retVal || super.onTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        constrainTranslation()

        canvas.save()
        canvas.clipRect(contentRect)
        canvas.translate(bitmapOffset.x, bitmapOffset.y)
        canvas.scale(bitmapScale, bitmapScale)

        if (puzzleBitmap != null) {
            canvas.drawBitmap(puzzleBitmap!!, 0f, 0f, bitmapPaint)
        } else {
            // Perform a fast, barebones render so that the screen doesn't
            // look completely empty

            inPlaceRenderer.renderPuzzle(canvas, renderScale, true)
        }

        canvas.restore()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Get the content rect
        contentRect.set(paddingLeft.toFloat(), paddingTop.toFloat(),
                (w - paddingRight).toFloat(), (h - paddingBottom).toFloat())

        resetConstraintsAndRedraw(false)
        if (selection != null) bringIntoView(selection)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        Log.v(LOG_TAG, "onCreateInputConnection()")

        var inputConnection: CrosswordInputConnection? = null
        if (_inputMode != INPUT_MODE_NONE) {
            outAttrs.actionLabel = null
            outAttrs.inputType = InputType.TYPE_NULL
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION.inv()
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_ACTION_NEXT
            outAttrs.packageName = context.packageName

            inputConnection = CrosswordInputConnection(this)
            inputConnection.onInputEventListener = inputEventListener
        }

        return inputConnection
    }

    override fun onCheckIsTextEditor(): Boolean =
            _isEditable && _inputMode != INPUT_MODE_NONE

    override fun computeScroll() {
        super.computeScroll()

        var invalidate = false

        if (zoomer.computeZoom()) {
            renderScale = scaleStart + zoomer.currZoom

            recomputePuzzleRect()
            if (constrainScaling()) recomputePuzzleRect()

            bitmapScale = renderScale / scaleStart
            invalidate = true
        } else {
            if (isZooming) {
                regenerateBitmaps()
                isZooming = false
            }
        }

        if (scroller.computeScrollOffset()) {
            bitmapOffset.set(scroller.currX.toFloat(), scroller.currY.toFloat())
            invalidate = true
        }

        if (invalidate) postInvalidateOnAnimation()
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        var handled = false
        if (event.action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_SPACE -> {
                    switchWordDirection()
                    handled = true
                }
                KeyEvent.KEYCODE_DEL -> {
                    handleBackspace()
                    handled = true
                }
                else -> {
                    val uniChar = event.unicodeChar
                    if (uniChar != 0) {
                        handleInput(uniChar.toChar())
                        handled = true
                    }
                }
            }
        } else if (event.action == KeyEvent.ACTION_MULTIPLE) {
            val uniChars = event.characters
            if (!uniChars.isNullOrEmpty()) {
                handleInput(uniChars[0])
                handled = true
            }
        }

        return handled
    }

    fun solveWord(word: Crossword.Word) {
        val matrix: Array<Array<String?>>
        val wordLen = word.length
        when (word.direction) {
            Crossword.Word.DIR_ACROSS -> {
                matrix = Array(1) { arrayOfNulls<String?>(wordLen) }
                for (i in 0 until wordLen) matrix[0][i] = word.cellAt(i).chars()
            }
            Crossword.Word.DIR_DOWN -> {
                matrix = Array(wordLen) { arrayOfNulls<String?>(1) }
                for (i in 0 until wordLen) matrix[i][0] = word.cellAt(i).chars()
            }
            else -> throw IllegalArgumentException("Word direction not valid")
        }

        setChars(word.startRow, word.startColumn, matrix, true)
    }

    fun isSquareMarked(word: Crossword.Word, square: Int): Boolean {
        val s = Selectable(word, square)
        return puzzleCells[s.row][s.column]!!.isFlagSet(Cell.FLAG_MARKED)
    }

    fun toggleSquareMark(word: Crossword.Word, square: Int, mark: Boolean) {
        val s = Selectable(word, square)
        val cell = puzzleCells[s.row][s.column]

        if (cell?.setFlag(Cell.FLAG_MARKED, mark) == true) {
            onStateChangeListener?.onCrosswordChanged(this)
            redrawInPlace()
        }
    }

    fun solveChar(word: Crossword.Word, charIndex: Int) {
        var row = word.startRow
        var column = word.startColumn

        if (word.direction == Crossword.Word.DIR_ACROSS) {
            column += charIndex
        } else if (word.direction == Crossword.Word.DIR_DOWN) {
            row += charIndex
        }

        val ch = word.cellAt(charIndex).chars()
        setChars(row, column, arrayOf(arrayOf<String?>(ch)), true)
    }

    fun solveCrossword() {
        val matrix = Array<Array<String?>>(puzzleHeight) { arrayOfNulls(puzzleWidth) }
        for (word in crossword!!.wordsAcross) {
            val row = word.startRow
            val startCol = word.startColumn
            var i = 0
            val n = word.length
            while (i < n) {
                matrix[row][startCol + i] = word.cellAt(i).chars()
                i++
            }
        }
        for (word in crossword!!.wordsDown) {
            val startRow = word.startRow
            val col = word.startColumn
            var i = 0
            val n = word.length
            while (i < n) {
                matrix[startRow + i][col] = word.cellAt(i).chars()
                i++
            }
        }

        setChars(0, 0, matrix, true)
    }

    fun reset() {
        for (i in 0 until puzzleHeight) {
            for (j in 0 until puzzleWidth) puzzleCells[i][j]?.reset()
        }

        onBoardChanged()
        redrawInPlace()
    }

    fun restoreState(state: CrosswordState) {
        if (state.height != puzzleHeight || state.width != puzzleWidth) {
            throw RuntimeException("Dimensions for puzzle and state don't match")
        }

        val map = crossword!!.cellMap
        for (i in 0 until puzzleHeight) {
            for (j in 0 until puzzleWidth) {
                puzzleCells[i][j]?.let { cell ->
                    cell.setFlag(Cell.FLAG_CHEATED,
                            state.isFlagSet(CrosswordState.FLAG_CHEATED, i, j))
                    cell.setFlag(Cell.FLAG_MARKED,
                            state.isFlagSet(CrosswordState.FLAG_MARKED, i, j))
                    cell.setChar(state.charAt(i, j))
                    if (markerDisplayMode and MARKER_ERROR != 0) {
                        cell.markError(map[i][j]!!, revealSetsCheatFlag)
                    }
                }
            }
        }

        if (state.hasSelection()) {
            val word = crossword!!.findWord(state.selectedDirection,
                    state.selectedNumber)
            val cell = state.selectedCell
            if (word != null && cell < word.length) {
                resetSelection(Selectable(word, cell), false)
            }
        }

        resetConstraintsAndRedraw(true)
        onBoardChanged()
    }

    fun selectPreviousWord() {
        selectWord(if (skipCompletedWords)
            previousIncomplete(selection?.word)
        else
            crossword?.previousWord(selection?.word))
    }

    fun selectNextWord() {
        selectWord(if (skipCompletedWords)
            nextIncomplete(selection?.word)
        else
            crossword?.nextWord(selection?.word))
    }

    fun selectWord(direction: Int, number: Int) {
        crossword?.findWord(direction, number)?.let { selectWord(it) }
    }

    fun getCellRect(word: Crossword.Word, cell: Int): Rect? =
            getCellRect(Selectable(word, cell))

    fun getCellContents(word: Crossword.Word, charIndex: Int): String? {
        var row = word.startRow
        var column = word.startColumn

        when (word.direction) {
            Crossword.Word.DIR_ACROSS -> column += charIndex
            Crossword.Word.DIR_DOWN -> row += charIndex
        }

        return puzzleCells[row][column]?.char
    }

    fun setCellContents(word: Crossword.Word, charIndex: Int,
                        sol: String, markAsCheated: Boolean) {
        var row = word.startRow
        var column = word.startColumn

        if (word.direction == Crossword.Word.DIR_ACROSS) {
            column += charIndex
        } else if (word.direction == Crossword.Word.DIR_DOWN) {
            row += charIndex
        }

        setChars(row, column, arrayOf(arrayOf<String?>(sol)), markAsCheated)
    }

    fun isSolved(): Boolean {
        val cw = crossword ?: return false
        if (cw.flags and Crossword.FLAG_NO_SOLUTION != 0) return false

        val map = cw.cellMap
        for (i in map.indices) {
            for (j in map[i].indices) {
                puzzleCells[i][j]?.let { cell ->
                    if (!cell.isSolved(map[i][j]!!)) return false
                }
            }
        }

        return true
    }

    // Returns previous incomplete word.
    // If all words have been completed, returns previous word - irrelevant of
    // completion status.
    private fun previousIncomplete(word: Crossword.Word?): Crossword.Word? {
        val firstNext = crossword?.previousWord(word)
        var w = firstNext

        wordLoop@while (w != null) {
            when (w.direction) {
                Crossword.Word.DIR_ACROSS -> for (c in w.startColumn until w.startColumn + w.length)
                    if (puzzleCells[w.startRow][c]?.isEmpty == true) break@wordLoop
                Crossword.Word.DIR_DOWN -> for (r in w.startRow until w.startRow + w.length)
                    if (puzzleCells[r][w.startColumn]?.isEmpty == true) break@wordLoop
            }

            w = crossword?.previousWord(w)
            if (w == firstNext) break
        }

        return w
    }

    // Returns next incomplete word.
    // If all words have been completed, returns next word - irrelevant of
    // completion status.
    private fun nextIncomplete(word: Crossword.Word?): Crossword.Word? {
        val firstNext = crossword?.nextWord(word)
        var w = firstNext

        wordLoop@while (w != null) {
            when (w.direction) {
                Crossword.Word.DIR_ACROSS -> for (c in w.startColumn until w.startColumn + w.length)
                    if (puzzleCells[w.startRow][c]?.isEmpty == true) break@wordLoop
                Crossword.Word.DIR_DOWN -> for (r in w.startRow until w.startRow + w.length)
                    if (puzzleCells[r][w.startColumn]?.isEmpty == true) break@wordLoop
            }

            w = crossword?.nextWord(w)
            if (w == firstNext) break
        }

        return w
    }

    private fun selectWord(word: Crossword.Word?) {
        if (word == null) {
            resetSelection(null)
        } else {
            resetSelection(Selectable(word,
                    if (selectFirstUnoccupiedOnNav) maxOf(firstFreeCell(word, 0), 0) else 0))
        }
    }

    private fun getCellRect(sel: Selectable?): Rect? {
        if (sel == null) return null

        val left = sel.column * scaledCellSize + bitmapOffset.x
        val top = sel.row * scaledCellSize + bitmapOffset.y

        return Rect(left.toInt(), top.toInt(),
                (left + scaledCellSize).toInt(), (top + scaledCellSize).toInt())
    }

    private fun clearUndoBufferIfNeeded(selectable: Selectable?) {
        if (undoMode != UNDO_SMART || undoBuffer.size < 1) return

        // Check the top item in the undo buffer. If it belongs to a different word, clear
        // the buffer
        val top = undoBuffer.peek()
        if (top.selectable != null && top.selectable.word != selectable!!.word) {
            undoBuffer.clear()
        }
    }

    private fun resetInputMode() {
        isFocusableInTouchMode = _isEditable && _inputMode != INPUT_MODE_NONE
        context.inputMethodManager?.let { imm ->
            if (imm.isActive(this)) imm.hideSoftInputFromWindow(windowToken, 0)
            imm.restartInput(this)
        }
    }

    private fun handleInput(ch: Char) {
        if (!_isEditable) return

        val sch = ch.toString()
        val validator = inputValidator ?: defaultInputValidator
        if (selection != null && validator.invoke(sch)) {
            clearUndoBufferIfNeeded(selection)

            val row = selection!!.row
            val col = selection!!.column

            val cell = puzzleCells[row][col]
            val changed = cell!!.char != sch
            if (changed) {
                undoBuffer.push(UndoItem(cell.char, row, col, selection))
                cell.setChar(sch)
            }

            if (markerDisplayMode and MARKER_ERROR != 0) {
                cell.markError(crossword!!.cellMap[row][col]!!, revealSetsCheatFlag)
            }

            resetSelection(nextSelectable(selection!!))
            if (changed) onBoardChanged()
        }
    }

    private fun nextSelectable(selected: Selectable): Selectable {
        val cell = selected.cell
        var word: Crossword.Word? = selected.word
        var nextCell = -1

        if (skipOccupiedOnType) {
            nextCell = firstFreeCell(word, cell + 1)
            if (nextCell == -1 && cell + 1 < word!!.length) {
                // No more free cells from this point, but we're still not
                // at the end
                nextCell = cell + 1
            }
        } else {
            if (cell + 1 < word!!.length) nextCell = cell + 1
        }

        if (nextCell == -1) {
            word = if (skipCompletedWords) nextIncomplete(word) else crossword!!.nextWord(word)
            nextCell = if (selectFirstUnoccupiedOnNav) maxOf(firstFreeCell(word, 0), 0) else 0
        }

        return Selectable(word!!, nextCell)
    }

    private fun handleBackspace() {
        val crossword = crossword ?: return
        val sel = selection ?: return

        if (!_isEditable) return

        // If the undo buffer contains items, perform an undo action
        if (!undoBuffer.isEmpty()) {
            val item = undoBuffer.pop()
            setChars(item.startRow, item.startCol, item.chars,
                    setCheatFlag = false, bypassUndoBuffer = true)
            item.selectable?.let {
                val word = crossword.findWord(it.direction, it.word.number)!!
                resetSelection(Selectable(word, it.cell))
            }

            return
        }

        // Otherwise, act as a simple backspace
        var selectedWord: Crossword.Word? = sel.word
        var selectedCell = sel.cell

        val s = Selectable(sel)

        if (puzzleCells[s.row][s.column]?.isEmpty == true) {
            if (selectedCell > 0) {
                // Go back one cell and remove the char
                s.cell = --selectedCell
            } else {
                // At the first letter of a word. Select the previous word and do
                // what we did if (mSelectedCell > 0)
                selectedWord = crossword.previousWord(selectedWord)
                selectedCell = selectedWord!!.length - 1

                s.word = selectedWord
                s.cell = selectedCell
            }
        }

        val row = s.row
        val col = s.column

        val changed = puzzleCells[row][col]!!.clearChar()
        if (markerDisplayMode and MARKER_ERROR != 0) {
            puzzleCells[row][col]!!.setFlag(Cell.FLAG_ERROR, false)
        }

        selectedWord?.let { resetSelection(Selectable(it, selectedCell)) }
        if (changed) onBoardChanged()
    }

    fun switchWordDirection() {
        val crossword = crossword ?: return
        val sel = selection

        var ortho: Selectable? = null
        if (sel == null) {
            ortho = Selectable(crossword.nextWord(null)!!, 0)
        } else {
            val cell = puzzleCells[sel.row][sel.column]!!
            when (sel.direction) {
                Crossword.Word.DIR_ACROSS -> if (cell.downNumber != Cell.WORD_NUMBER_NONE) {
                    crossword.findWord(Crossword.Word.DIR_DOWN,
                            cell.downNumber)?.let { word ->
                        ortho = Selectable(word, sel.startRow - word.startRow)
                    }
                }
                Crossword.Word.DIR_DOWN -> if (cell.acrossNumber != Cell.WORD_NUMBER_NONE) {
                    crossword.findWord(Crossword.Word.DIR_ACROSS,
                            cell.acrossNumber)?.let { word ->
                        ortho = Selectable(word, sel.startColumn - word.startColumn)
                    }
                }
            }
        }

        if (ortho != null) {
            resetSelection(ortho)
            clearUndoBufferIfNeeded(ortho)
        }
    }

    private fun setChars(startRow: Int, startColumn: Int, charMatrix: Array<Array<String?>>,
                         setCheatFlag: Boolean, bypassUndoBuffer: Boolean = false) {
        // Check startRow/startColumn
        require(startRow >= 0 && startColumn >= 0) { "Invalid startRow/startColumn" }

        // Check dimensions
        require(charMatrix.isNotEmpty() && charMatrix[0].isNotEmpty()) { "Invalid matrix size" }

        val charHeight = charMatrix.size
        val charWidth = charMatrix[0].size
        val endRow = startRow + charHeight - 1
        val endColumn = startColumn + charWidth - 1

        // Check bounds
        require(endRow < puzzleHeight && endColumn < puzzleWidth) { "Chars out of bounds" }

        // Set up undo buffer
        var undoBuf: Array<Array<String?>>? = null
        if (!bypassUndoBuffer && undoMode != UNDO_NONE) {
            undoBuf = Array(charHeight) { arrayOfNulls<String?>(charWidth) }
        }

        // Fill in the char array
        var cwChanged = false
        val map = crossword!!.cellMap
        var i = startRow
        var k = 0
        while (i <= endRow) {
            var j = startColumn
            var l = 0
            while (j <= endColumn) {
                val vwCell = puzzleCells[i][j]
                if (vwCell != null) {
                    if (undoBuf != null) {
                        undoBuf[k][l] = vwCell.char
                    }
                    val ch = charMatrix[k][l].canonicalize()
                    val validator = inputValidator ?: defaultInputValidator
                    if (ch != vwCell.char && (ch == null || validator.invoke(ch))) {
                        val cellChanged = vwCell.char != ch
                        if (cellChanged) {
                            vwCell.setChar(ch)
                            cwChanged = true
                        }
                        if (setCheatFlag) {
                            vwCell.setFlag(Cell.FLAG_CHEATED, true)
                        }
                        if (markerDisplayMode and MARKER_ERROR != 0) {
                            vwCell.markError(map[i][j]!!, revealSetsCheatFlag)
                        }
                    }
                }
                j++
                l++
            }
            i++
            k++
        }

        // Redraw selection
        redrawInPlace()
        if (cwChanged) {
            if (undoBuf != null) {
                clearUndoBufferIfNeeded(selection)
                undoBuffer.push(UndoItem(undoBuf, startRow, startColumn, selection))
            }
            onBoardChanged()
        }
    }

    private fun zoomTo(finalRenderScale: Float): Boolean {
        if ((finalRenderScale - renderScale).absoluteValue < .01f) return false

        zoomer.forceFinished(true)
        isZooming = true
        scaleStart = renderScale

        zoomer.startZoom(finalRenderScale - scaleStart)
        postInvalidateOnAnimation()

        return true
    }

    private fun showKeyboard() {
        if (_inputMode != INPUT_MODE_NONE) {
            context.inputMethodManager?.let { imm ->
                if (!imm.isActive(this)) requestFocus()
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun resetErrorMarkers() {
        val cw = crossword ?: return

        val map = cw.cellMap
        for (i in map.indices) {
            for (j in map[i].indices) {
                puzzleCells[i][j]?.let { cell ->
                    if (markerDisplayMode and MARKER_ERROR != 0) {
                        cell.markError(map[i][j]!!, revealSetsCheatFlag)
                    } else {
                        cell.setFlag(Cell.FLAG_ERROR, false)
                    }
                }
            }
        }
    }

    private fun initializeCrossword() {
        puzzleHeight = 0
        puzzleWidth = 0
        puzzleCells = EMPTY_CELLS
        allowedChars.clear()

        val crossword = crossword ?: return

        puzzleWidth = crossword.width
        puzzleHeight = crossword.height
        puzzleCells = Array(puzzleHeight) { arrayOfNulls<Cell?>(puzzleWidth) }

        // Copy allowed characters
        for (ch in crossword.alphabet) {
            allowedChars.add(Character.toUpperCase(ch))
        }

        // Copy across
        for (word in crossword.wordsAcross) {
            val row = word.startRow
            val startColumn = word.startColumn

            // Chars
            var column = startColumn
            var p = 0
            val n = word.length
            while (p < n) {
                val cell = Cell()
                cell.acrossNumber = word.number
                cell.setFlag(Cell.FLAG_CIRCLED, word.cellAt(p).isCircled)
                puzzleCells[row][column] = cell
                column++
                p++
            }

            // Number
            puzzleCells[row][startColumn]!!.number = "${word.number}"
        }

        // Copy down
        for (word in crossword.wordsDown) {
            val startRow = word.startRow
            val column = word.startColumn

            // Chars
            var row = startRow
            var p = 0
            val n = word.length
            while (p < n) {
                // It's possible that we already have a cell in that position from 'Across'
                // Check before creating a new cell

                var cell: Cell? = puzzleCells[row][column]
                if (cell == null) {
                    cell = Cell()
                    if (word.cellAt(p).isCircled) {
                        cell.setFlag(Cell.FLAG_CIRCLED, true)
                    }
                    puzzleCells[row][column] = cell
                }

                cell.downNumber = word.number
                row++
                p++
            }

            // Number
            if (puzzleCells[startRow][column]!!.number == null) {
                puzzleCells[startRow][column]!!.number = "${word.number}"
            }
        }
    }

    private fun resetConstraintsAndRedraw(forceBitmapRegen: Boolean) {
        val regenBitmaps = puzzleBitmap == null && (renderTask == null || renderTask!!.isCancelled)

        // Determine the scale at which the puzzle takes up the entire width
        val unscaledWidth = puzzleWidth * cellSize + 1 // +1px for stroke brush
        fitWidthScaleFactor = contentRect.width() / unscaledWidth

        // Set the default scale to be "fit to width"
        if (renderScale < .01) {
            renderScale = fitWidthScaleFactor
        }

        // Determine the smallest scale factor
        minScaleFactor = if (contentRect.width() < contentRect.height()) {
            fitWidthScaleFactor
        } else {
            contentRect.height() / (puzzleHeight * cellSize + 1) // +1px for stroke brush
        }

        val largestDimension = maxOf(puzzleWidth, puzzleHeight)
        val maxAvailableDimension = (_maxBitmapSize - 1).toFloat() // stroke brush again

        maxScaleFactor = maxAvailableDimension / (largestDimension * cellSize)
        maxScaleFactor = maxOf(maxScaleFactor, minScaleFactor)

        bitmapScale = 1.0f
        isZooming = false

        // Recompute scaled puzzle size
        recomputePuzzleRect()

        if (regenBitmaps || forceBitmapRegen) {
            regenerateBitmaps()
        }
    }

    private fun regenerateBitmaps() {
        synchronized(rendererLock) {
            renderTask?.cancel(false)

            // A 1px size line is always present, so it's not enough to just
            // check for zero
            if (puzzleRect.width() > 1 && puzzleRect.height() > 1) {
                renderTask = RenderTask(this, renderScale)
                renderTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
    }

    private fun recomputePuzzleRect() {
        // Compute scaled puzzle rect
        scaledCellSize = renderScale * cellSize
        puzzleRect.set(0f, 0f,
                puzzleWidth * scaledCellSize + 1, // w/h get an extra pixel due to the
                puzzleHeight * scaledCellSize + 1) // hairline-wide stroke of the cell

        // Determine center locations
        centeredOffset.set(contentRect.left + (contentRect.width() - puzzleRect.width()) / 2.0f,
                contentRect.top + (contentRect.height() - puzzleRect.height()) / 2.0f)

        // Compute translation bounds
        translationBounds.set(contentRect.right - puzzleRect.right,
                contentRect.bottom - puzzleRect.bottom,
                contentRect.left - puzzleRect.left,
                contentRect.top - puzzleRect.top)

        constrainTranslation()
    }

    private fun constrainTranslation() {
        // Clamp the offset to fit within the view
        bitmapOffset.clampTo(translationBounds)

        if (puzzleRect.width() < contentRect.width()) {
            // Puzzle is narrower than the available width - center it horizontally
            bitmapOffset.x = centeredOffset.x
        }

        // Vertical
        if (puzzleRect.height() < contentRect.height()) {
            // Puzzle is shorter than the available height - center it vertically
            bitmapOffset.y = centeredOffset.y
        }
    }

    private fun constrainScaling(): Boolean {
        if (puzzleRect.width() < contentRect.width() && puzzleRect.height() < contentRect.height()) {
            if (renderScale < minScaleFactor) {
                renderScale = minScaleFactor
                return true
            }
        }

        if (renderScale > maxScaleFactor) {
            renderScale = maxScaleFactor
            return true
        }

        return false
    }

    private fun bringIntoView(sel: Selectable?) {
        if (sel == null) return

        val wordRect = RectF(sel.startColumn * scaledCellSize - contentRect.left,
                sel.startRow * scaledCellSize - contentRect.top, 0f, 0f)

        if (sel.direction == Crossword.Word.DIR_ACROSS) {
            wordRect.right = wordRect.left + sel.word.length * scaledCellSize
            wordRect.bottom = wordRect.top + scaledCellSize
        } else if (sel.direction == Crossword.Word.DIR_DOWN) {
            wordRect.right = wordRect.left + scaledCellSize
            wordRect.bottom = wordRect.top + sel.word.length * scaledCellSize
        }

        val objectRect = RectF(wordRect)
        val visibleArea = RectF(-bitmapOffset.x, -bitmapOffset.y,
                -bitmapOffset.x + contentRect.width(),
                -bitmapOffset.y + contentRect.height())

        if (visibleArea.contains(objectRect)) return // Already visible

        if (objectRect.width() > visibleArea.width() || objectRect.height() > visibleArea.height()) {
            // Available area isn't large enough to fit the entire word
            // Is the selected cell visible? If not, bring it into view

            val cellRect = RectF()
            cellRect.left = sel.column * scaledCellSize
            cellRect.top = sel.row * scaledCellSize
            cellRect.right = cellRect.left + scaledCellSize
            cellRect.bottom = cellRect.top + scaledCellSize

            if (visibleArea.contains(cellRect)) return // Already visible

            objectRect.set(cellRect)
        }

        // Compute view that includes the object in the center
        val end = PointF((visibleArea.width() - objectRect.width()) / 2.0f - objectRect.left,
                (visibleArea.height() - objectRect.height()) / 2.0f - objectRect.top)

        // Clamp the values
        end.clampTo(translationBounds)

        // Compute the distance to travel from current location
        val distanceX = end.x - bitmapOffset.x
        val distanceY = end.y - bitmapOffset.y

        // Scroll the point into view
        scroller.startScroll(bitmapOffset.x.toInt(), bitmapOffset.y.toInt(),
                distanceX.toInt(), distanceY.toInt(), NAVIGATION_SCROLL_DURATION_MS)
    }

    private fun redrawInPlace() {
        val canvas = puzzleCanvas ?: return

        synchronized(rendererLock) {
            renderTask?.cancel(false)
        }

        inPlaceRenderer.renderPuzzle(canvas, renderScale, false)
        postInvalidateOnAnimation()
    }

    private fun resetSelection(newSelection: Selectable?,
                               bringIntoView: Boolean = true) {
        val selectionChanged = newSelection != selection

        val canvas = puzzleCanvas
        if (canvas != null) {
            // Create a canvas on top of the existing bitmap
            if (selection != null && selectionChanged) {
                // Clear the selection from the deselected word
                inPlaceRenderer.renderSelection(canvas, true)
            }
        }

        // Set new selection
        selection = newSelection

        if (canvas != null) {
            // Bring new selection into view, if requested
            if (bringIntoView) bringIntoView(newSelection)

            // Render the new selection
            inPlaceRenderer.renderSelection(canvas, false)
        }

        // Notify the listener of the change in selection
        if (selectionChanged) onSelectionChangeListener?.onSelectionChanged(this,
                newSelection?.word, newSelection?.cell ?: -1)

        // Invalidate the view
        invalidate()
    }

    private fun isAcceptableChar(ch: Char): Boolean =
            allowedChars.contains(Character.toUpperCase(ch))

    private fun getCellOffset(viewX: Float, viewY: Float, offset: CellOffset): Boolean {
        val column = ((viewX - bitmapOffset.x) / scaledCellSize).toInt()
        val row = ((viewY - bitmapOffset.y) / scaledCellSize).toInt()

        if (row in 0 until puzzleHeight && column in 0 until puzzleWidth) {
            offset.row = row
            offset.column = column

            return true
        }

        return false
    }

    private fun handleCellTap(offset: CellOffset) {
        var preferredDir = Crossword.Word.DIR_ACROSS
        if (selection != null) {
            if (offset.row == selection!!.row && offset.column == selection!!.column) {
                // Same cell tapped - flip direction
                switchWordDirection()
                if (_isEditable) {
                    showKeyboard()
                }
                return
            }

            // Select a word in the same direction
            preferredDir = selection!!.direction
        }

        val sel = getSelectable(offset, preferredDir)
        if (sel != null) {
            resetSelection(sel)

            // Undo buffer is always reset whenever a user switches to a different word.
            // We also want to reset the buffer if the user taps a different cell in the same word
            undoBuffer.clear()
        }

        if (_isEditable) showKeyboard()
    }

    private fun handleCellLongPress(offset: CellOffset) {
        if (puzzleCells[offset.row][offset.column] == null) return

        val dir = selection?.direction ?: Crossword.Word.DIR_ACROSS
        getSelectable(offset, dir)?.let { sel ->
            resetSelection(sel, false)
            onLongPressListener?.onCellLongPressed(this, sel.word, sel.cell)
        }
    }

    private fun firstFreeCell(word: Crossword.Word?, start: Int): Int {
        var firstFree = -1
        if (word != null) {
            var i = start
            val n = word.length
            while (i < n) {
                var row = word.startRow
                var col = word.startColumn
                when (word.direction) {
                    Crossword.Word.DIR_ACROSS -> col += i
                    Crossword.Word.DIR_DOWN -> row += i
                }

                if (puzzleCells[row][col]?.isEmpty == true) {
                    firstFree = i
                    break
                }
                i++
            }
        }

        return firstFree
    }

    private fun onBoardChanged() {
        val solved = isSolved()
        onStateChangeListener?.onCrosswordChanged(this)
        if (solved != isSolved) {
            if (solved) {
                onStateChangeListener?.onCrosswordSolved(this)
            } else {
                onStateChangeListener?.onCrosswordUnsolved(this)
            }
        }
        isSolved = solved
    }

    private fun getSelectable(co: CellOffset, preferredDir: Int): Selectable? {
        val cell = puzzleCells[co.row][co.column]
        var sel: Selectable? = null

        if (cell != null) {
            val across = cell.acrossNumber
            val down = cell.downNumber

            // Select an Across word if we're currently selecting Across words,
            // or if we're selecting Down words, but no Down words are
            // available at that cell
            if (across != Cell.WORD_NUMBER_NONE) {
                if (preferredDir == Crossword.Word.DIR_ACROSS || preferredDir == Crossword.Word.DIR_DOWN && down == Cell.WORD_NUMBER_NONE) {
                    val word = crossword!!.findWord(Crossword.Word.DIR_ACROSS, across)
                    sel = Selectable(word!!, co.column - word.startColumn)
                }
            }

            // Select a Down word if we're currently selecting Down words,
            // or if we're selecting Across words, but no Across words are
            // available at that cell
            if (down != Cell.WORD_NUMBER_NONE) {
                if (preferredDir == Crossword.Word.DIR_DOWN || preferredDir == Crossword.Word.DIR_ACROSS && across == Cell.WORD_NUMBER_NONE) {
                    val word = crossword!!.findWord(Crossword.Word.DIR_DOWN, down)
                    sel = Selectable(word!!, co.row - word.startRow)
                }
            }
        }

        return sel
    }

    internal class SavedState : BaseSavedState {

        var renderScale: Float
        var bitmapOffset: PointF

        constructor(superState: Parcelable?) : super(superState) {
            renderScale = 0f
            bitmapOffset = PointF()
        }

        private constructor(parcel: Parcel) : super(parcel) {
            renderScale = parcel.readFloat()
            bitmapOffset = parcel.readTypedParcelable()!!
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)

            dest.writeFloat(renderScale)
            dest.writeParcelable(bitmapOffset, 0)
        }

        companion object CREATOR: Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    private class UndoItem(val chars: Array<Array<String?>> = emptyArray(),
                           val startRow: Int = 0,
                           val startCol: Int = 0,
                           val selectable: Selectable? = null) {

        constructor(ch: String?, row: Int, col: Int, selectable: Selectable? = null) : this(
                chars = arrayOf<Array<String?>>(arrayOf(ch)),
                startRow = row,
                startCol = col,
                selectable = selectable)
    }

    private class Selectable(var word: Crossword.Word,
                             var cell: Int = 0) : Parcelable {

        val direction: Int
            get() = word.direction

        val startRow: Int
            get() = word.startRow

        val startColumn: Int
            get() = word.startColumn

        val row: Int
            get() = getRow(cell)

        val column: Int
            get() = getColumn(cell)

        val endRow: Int
            get() = getRow(word.length - 1)

        val endColumn: Int
            get() = getColumn(word.length - 1)

        constructor(s: Selectable): this(s.word, s.cell)

        private constructor(parcel: Parcel): this(
                word = parcel.readTypedParcelable<Crossword.Word>()!!,
                cell = parcel.readInt())

        fun getRow(cell: Int): Int {
            var v = word.startRow
            if (word.direction == Crossword.Word.DIR_DOWN)
                v += minOf(cell, word.length - 1)

            return v
        }

        fun getColumn(cell: Int): Int {
            var v = word.startColumn
            if (word.direction == Crossword.Word.DIR_ACROSS)
                v += minOf(cell, word.length - 1)

            return v
        }

        fun isCellWithinBounds(cell: Int) = cell >= 0 && cell < word.length

        override fun equals(other: Any?): Boolean {
            val s2 = other as? Selectable ?: return false
            return s2.word == word && s2.cell == cell
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(word, 0)
            dest.writeInt(cell)
        }

        // Autogenerated
        override fun hashCode(): Int = 31 * word.hashCode() + cell

        companion object CREATOR: Parcelable.Creator<Selectable> {
            override fun createFromParcel(parcel: Parcel): Selectable = Selectable(parcel)
            override fun newArray(size: Int): Array<Selectable?> = arrayOfNulls(size)
        }
    }

    private class CellOffset(var row: Int = 0,
                             var column: Int = 0) {

        constructor(offset: CellOffset): this(offset.row, offset.column)
    }

    private class Cell : Parcelable {

        var number: String? = null
        var char: String? = null
        var acrossNumber: Int = WORD_NUMBER_NONE
        var downNumber: Int = WORD_NUMBER_NONE
        var flags: Int = 0

        val isEmpty: Boolean
            get() = char == null

        constructor()

        private constructor(parcel: Parcel) {
            number = parcel.readString()
            char = parcel.readString()
            acrossNumber = parcel.readInt()
            downNumber = parcel.readInt()
            flags = parcel.readInt()
        }

        fun clearChar(): Boolean = setChar(null)

        fun reset() {
            clearChar()
            setFlag(FLAG_CHEATED or FLAG_ERROR, false)
        }

        fun setChar(ch: String?): Boolean {
            val canon = ch.canonicalize()
            if (canon != char) {
                char = canon
                return true
            }

            return false
        }

        fun isSolved(cwCell: Crossword.Cell): Boolean =
                char != null && cwCell.contains(char)

        fun markError(cwCell: Crossword.Cell, setCheatFlag: Boolean) {
            val error = !isEmpty && !cwCell.contains(char)
            if (error) {
                flags = flags or FLAG_ERROR
                if (setCheatFlag) flags = flags or FLAG_CHEATED
            } else {
                flags = flags and FLAG_ERROR.inv()
            }
        }

        fun isFlagSet(flag: Int): Boolean = flags and flag == flag

        fun setFlag(flag: Int, set: Boolean): Boolean {
            val old = flags
            flags = if (set) {
                flags or flag
            } else {
                flags and flag.inv()
            }

            return old != flags
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, parcelFlags: Int) {
            dest.writeString(number)
            dest.writeString(char)
            dest.writeInt(acrossNumber)
            dest.writeInt(downNumber)
            dest.writeInt(flags)
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<Cell> = object : Parcelable.Creator<Cell> {
                override fun createFromParcel(parcel: Parcel): Cell = Cell(parcel)
                override fun newArray(size: Int): Array<Cell?> = arrayOfNulls(size)
            }

            const val WORD_NUMBER_NONE = -1

            const val FLAG_CHEATED = 1
            const val FLAG_CIRCLED = 2
            const val FLAG_ERROR   = 4
            const val FLAG_MARKED  = 8
        }
    }

    private class Renderer(private val v: CrosswordView) {

        var cancelRender: Boolean = false

        private val cellPath = Path()
        private val cellRect = RectF()
        private val tempRect = Rect()
        private val answerTextRect = RectF()

        fun renderCell(v: CrosswordView, canvas: Canvas,
                       cell: Cell, fillPaint: Paint, fastRender: Boolean) {
            canvas.drawRect(cellRect, fillPaint)

            if (!fastRender) {
                // Render the markers first, so that the cell stroke paints over
                if (cell.isFlagSet(Cell.FLAG_MARKED) && v.markerDisplayMode and MARKER_CUSTOM != 0) {
                    canvas.drawPath(cellPath.with(true) {
                        moveTo(cellRect.right - v.markerSideLength, cellRect.top)
                        lineTo(cellRect.right, cellRect.top)
                        lineTo(cellRect.right, cellRect.top + v.markerSideLength)
                    }, v.markedCellFillPaint)
                }

                if (cell.isFlagSet(Cell.FLAG_CHEATED) && v.markerDisplayMode and MARKER_CHEAT != 0) {
                    canvas.drawPath(cellPath.with(true) {
                        moveTo(cellRect.right, cellRect.bottom)
                        lineTo(cellRect.right - v.markerSideLength, cellRect.bottom)
                        lineTo(cellRect.right, cellRect.bottom - v.markerSideLength)
                    }, v.cheatedCellFillPaint)
                }

                if (cell.isFlagSet(Cell.FLAG_ERROR) && v.markerDisplayMode and MARKER_ERROR != 0) {
                    canvas.drawPath(cellPath.with(true) {
                        moveTo(cellRect.left, cellRect.bottom)
                        lineTo(cellRect.left + v.markerSideLength, cellRect.bottom)
                        lineTo(cellRect.left, cellRect.bottom - v.markerSideLength)
                    }, v.mistakeCellFillPaint)
                }
            }

            canvas.drawRect(cellRect, v.cellStrokePaint)

            if (fastRender) return

            if (cell.isFlagSet(Cell.FLAG_CIRCLED)) {
                canvas.drawCircle(cellRect.centerX(), cellRect.centerY(),
                        v.circleRadius, v.circleStrokePaint)
            }

            val numberY = cellRect.top + v.numberTextPadding + v.numberTextHeight

            cell.number?.let { num ->
                val textWidth = v.numberTextPaint.measureText(num)
                val numberX = cellRect.left + v.numberTextPadding + textWidth / 2

                if (cell.isFlagSet(Cell.FLAG_CIRCLED)) {
                    canvas.drawText(num, numberX, numberY, v.numberStrokePaint)
                }

                canvas.drawText(num, numberX, numberY, v.numberTextPaint)
            }

            if (!cell.isEmpty) {
                var text = cell.char
                if (text!!.length > 8) {
                    // FIXME: customize max length and replacement pattern
                    text = text.substring(0, 8) + "…"
                }

                answerTextRect.set(cellRect.left, numberY,
                        cellRect.right, cellRect.bottom)

                var textSize = v.answerTextSize
                var textWidth: Float

                do {
                    v.answerTextPaint.textSize = textSize
                    textWidth = v.answerTextPaint.measureText(text)
                    textSize -= v.scaledDensity
                } while (textWidth >= v.cellSize)

                v.answerTextPaint.getTextBounds("A", 0, 1, tempRect)
                val xOffset = textWidth / 2f
                val yOffset = (tempRect.height() / 2).toFloat()

                canvas.drawText(text, answerTextRect.centerX() - xOffset,
                        answerTextRect.centerY() + yOffset, v.answerTextPaint)
            }
        }

        fun renderSelection(canvas: Canvas, clearSelection: Boolean) {
            val sel = v.selection ?: return

            cancelRender = false

            canvas.save()
            canvas.scale(v.renderScale, v.renderScale)

            var top = sel.startRow * v.cellSize
            var index = 0
            for (row in sel.startRow..sel.endRow) {
                var left = sel.startColumn * v.cellSize
                for (column in sel.startColumn..sel.endColumn) {
                    if (cancelRender) {
                        canvas.restore()
                        return
                    }

                    v.puzzleCells[row][column]?.let { cell ->
                        // Draw the unselected cell
                        val paint = when {
                            clearSelection -> v.cellFillPaint
                            index == sel.cell -> v.selectedCellFillPaint
                            else -> v.selectedWordFillPaint
                        }

                        cellRect.set(left, top, left + v.cellSize, top + v.cellSize)
                        renderCell(v, canvas, cell, paint, false)
                    }

                    index++
                    left += v.cellSize
                }
                top += v.cellSize
            }

            canvas.restore()
        }

        fun renderPuzzle(canvas: Canvas, scale: Float, fastRender: Boolean) {
            cancelRender = false
            val startedMillis = SystemClock.uptimeMillis()

            canvas.save()
            canvas.scale(scale, scale)

            var top = 0f
            for (i in 0 until v.puzzleHeight) {
                var left = 0f
                for (j in 0 until v.puzzleWidth) {
                    if (cancelRender) {
                        canvas.restore()
                        return
                    }

                    v.puzzleCells[i][j]?.let { cell ->
                        cellRect.set(left, top, left + v.cellSize, top + v.cellSize)
                        renderCell(v, canvas, cell, v.cellFillPaint, fastRender)
                    }
                    left += v.cellSize
                }
                top += v.cellSize
            }

            canvas.restore()

            if (!fastRender) renderSelection(canvas, false)

            Log.d(LOG_TAG, String.format("Rendered puzzle (%.02fs)",
                    (SystemClock.uptimeMillis() - startedMillis) / 1000f))
        }
    }

    private class RenderTask(view: CrosswordView,
                             var scale: Float) : AsyncTask<Void?, Void?, Bitmap?>() {

        private val viewRef = WeakReference(view)
        private val renderer = Renderer(view)

        override fun doInBackground(vararg params: Void?): Bitmap? {
            val v = viewRef.get() ?: return null
            if (v.puzzleWidth == 0 || v.puzzleHeight == 0) return null

            val width = v.puzzleRect.width().toInt()
            val height = v.puzzleRect.height().toInt()

            val puzzleBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val puzzleCanvas = Canvas(puzzleBmp)

            Log.d(LOG_TAG, String.format("Created a new %dx%d puzzle bitmap (%,dkB)...",
                    width, height, puzzleBmp.sizeInBytes / 1024))

            renderer.renderPuzzle(puzzleCanvas, scale, false)

            return puzzleBmp
        }

        override fun onPostExecute(param: Bitmap?) {
            val v = viewRef.get() ?: return
            if (isCancelled || param == null) return

            v.puzzleCanvas = Canvas(param)
            v.puzzleBitmap = param
            v.bitmapScale = 1.0f

            Log.d(LOG_TAG, "Invalidating...")
            v.postInvalidateOnAnimation()

            synchronized(v.rendererLock) {
                v.renderTask = null
            }
        }

        override fun onCancelled(aVoid: Bitmap?) {
            val v = viewRef.get() ?: return

            renderer.cancelRender = true
            Log.d(LOG_TAG, "Task cancelled")

            synchronized(v.rendererLock) {
                v.renderTask = null
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            if (isZooming) {
                // Double-tap scaling interferes with autozoom, so ignore
                // zoom requests
                ignoreZoom = true
                return true
            }

            zoomer.forceFinished(true)
            isZooming = false

            scaleStart = renderScale
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (ignoreZoom) return true

            renderScale *= detector.scaleFactor

            recomputePuzzleRect()
            if (constrainScaling()) recomputePuzzleRect()

            bitmapScale = renderScale / scaleStart

            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (!ignoreZoom) regenerateBitmaps()
            ignoreZoom = false
        }
    }

    private inner class GestureListener: GestureDetector.SimpleOnGestureListener() {

        private val tapLocation = CellOffset()

        override fun onDown(e: MotionEvent): Boolean {
            if (!scroller.isFinished) scroller.forceFinished(true)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (getCellOffset(e.x, e.y, tapLocation)) {
                handleCellTap(CellOffset(tapLocation))
                return true
            }

            return false
        }

        override fun onLongPress(e: MotionEvent) {
            if (!scaleDetector.isInProgress && getCellOffset(e.x, e.y, tapLocation)) {
                handleCellLongPress(tapLocation)

                // Cancel the scale gesture by sending the detector
                // an ACTION_CANCEL event, to prevent double-tap scaling
                // from interfering. There should be a nicer way to do
                // this, but there isn't..
                val cancelEvent = MotionEvent.obtain(e.downTime,
                        e.eventTime, MotionEvent.ACTION_CANCEL,
                        e.x, e.y, e.metaState)
                scaleDetector.onTouchEvent(cancelEvent)
            }
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomTo(fitWidthScaleFactor)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent,
                              distX: Float, distY: Float): Boolean {
            bitmapOffset.offset(-distX, -distY)

            constrainTranslation()
            invalidate()

            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent,
                             velocityX: Float, velocityY: Float): Boolean {
            // Horizontal
            val startX = bitmapOffset.x.toInt()
            var minX = startX
            var maxX = startX
            if (puzzleRect.width() >= contentRect.width()) {
                // Puzzle exceeds content, set horizontal flinging bounds
                minX = translationBounds.left.toInt()
                maxX = translationBounds.right.toInt()
            }

            // Vertical
            val startY = bitmapOffset.y.toInt()
            var minY = startY
            var maxY = startY
            if (puzzleRect.height() >= contentRect.height()) {
                // Puzzle exceeds content, set vertical flinging bounds
                minY = translationBounds.top.toInt()
                maxY = translationBounds.bottom.toInt()
            }

            scroller.fling(startX, startY,
                    (velocityX / FLING_VELOCITY_DOWNSCALE).toInt(),
                    (velocityY / FLING_VELOCITY_DOWNSCALE).toInt(),
                    minX, maxX, minY, maxY)

            return true
        }
    }

    companion object {
        private val LOG_TAG = CrosswordView::class.java.simpleName

        const val MARKER_CUSTOM = 1
        const val MARKER_CHEAT  = 2
        const val MARKER_ERROR  = 4

        const val UNDO_NONE  = 0
        const val UNDO_SMART = 1

        const val INPUT_MODE_NONE     = 0
        const val INPUT_MODE_KEYBOARD = 1

        private val EMPTY_CELLS = Array<Array<Cell?>>(0) { arrayOfNulls(0) }

        private const val NAVIGATION_SCROLL_DURATION_MS = 500
        private const val DEFAULT_MAX_BITMAP_DIMENSION = 2048 // largest allowed bitmap width or height

        private const val MARKER_TRIANGLE_LENGTH_FRACTION = 0.3f

        private const val FLING_VELOCITY_DOWNSCALE = 2.0f
        private const val CELL_SIZE = 10f
        private const val NUMBER_TEXT_PADDING = 1f
        private const val NUMBER_TEXT_SIZE = 3f
        private const val ANSWER_TEXT_SIZE = 7f
        private const val NUMBER_TEXT_STROKE_WIDTH = 1f

        private val NORMAL_CELL_FILL_COLOR = "#ffffff".toColor()
        private val CHEATED_CELL_FILL_COLOR = "#ff8b85".toColor()
        private val MISTAKE_CELL_FILL_COLOR = "#ff0000".toColor()
        private val SELECTED_WORD_FILL_COLOR = "#faeace".toColor()
        private val SELECTED_CELL_FILL_COLOR = "#ecae44".toColor()
        private val MARKED_CELL_FILL_COLOR = "#cedefa".toColor()
        private val NUMBER_TEXT_COLOR = "#000000".toColor()
        private val ANSWER_TEXT_COLOR = "#0041b7".toColor()
        private val CELL_STROKE_COLOR = "#000000".toColor()
        private val CIRCLE_STROKE_COLOR = "#555555".toColor()
    }
}

private fun String?.canonicalize(): String? = this?.toUpperCase()
