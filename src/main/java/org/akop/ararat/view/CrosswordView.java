// Copyright (c) 2014-2016 Akop Karapetyan
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

package org.akop.ararat.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;

import org.akop.ararat.R;
import org.akop.ararat.core.Crossword;
import org.akop.ararat.view.inputmethod.CrosswordInputConnection;
import org.akop.ararat.widget.Zoomer;

import java.util.Locale;
import java.util.Stack;


@SuppressWarnings("unused")
public class CrosswordView
		extends View
		implements View.OnKeyListener
{
	private static final String LOG_TAG = CrosswordView.class.getSimpleName();

	public static final int MARKER_CUSTOM = 1;
	public static final int MARKER_CHEAT  = 1 << 1;
	public static final int MARKER_ERROR  = 1 << 2;

	public static final int UNDO_NONE  = 0;
	public static final int UNDO_SMART = 1;

	public interface OnStateChangeListener
	{
		void onCrosswordChanged(CrosswordView view);
		void onCrosswordSolved(CrosswordView view);
		void onCrosswordUnsolved(CrosswordView view);
	}

	public interface OnSelectionChangeListener
	{
		void onSelectionChanged(CrosswordView view,
				Crossword.Word word, int position);
	}

	public interface OnLongPressListener
	{
		void onCellLongPressed(CrosswordView view,
				Crossword.Word word, int cell);
	}

	private static final Cell[][] EMPTY_CELLS = new Cell[0][0];
	private static final char[] EMPTY_CHARS = new char[0];

	private static final int NAVIGATION_SCROLL_DURATION_MS = 500;
	private static final int DEFAULT_MAX_BITMAP_DIMENSION = 2048; // largest allowed bitmap width or height

	private static final float MARKER_TRIANGLE_LENGTH_FRACTION = 0.3f;

	private static final float FLING_VELOCITY_DOWNSCALE = 2.0f;
	private static final float CELL_SIZE = 10;
	private static final float NUMBER_TEXT_PADDING = 1;
	private static final float NUMBER_TEXT_SIZE = 3;
	private static final float ANSWER_TEXT_PADDING = 2;
	private static final float ANSWER_TEXT_SIZE = 7;
	private static final float NUMBER_TEXT_STROKE_WIDTH = 1;

	private static final int NORMAL_CELL_FILL_COLOR = Color.parseColor("#ffffff");
	private static final int CHEATED_CELL_FILL_COLOR = Color.parseColor("#ff8b85");
	private static final int MISTAKE_CELL_FILL_COLOR = Color.parseColor("#ff0000");
	private static final int SELECTED_WORD_FILL_COLOR = Color.parseColor("#faeace");
	private static final int SELECTED_CELL_FILL_COLOR = Color.parseColor("#ecae44");
	private static final int MARKED_CELL_FILL_COLOR = Color.parseColor("#cedefa");
	private static final int TEXT_COLOR = Color.parseColor("#000000");
	private static final int CELL_STROKE_COLOR = Color.parseColor("#000000");
	private static final int CIRCLE_STROKE_COLOR = Color.parseColor("#555555");

	private RectF mContentRect; // Content rectangle - bounds of the view
	private RectF mPuzzleRect; // Puzzle rectangle - basically the output size of the bitmap
	private float mScaledCellSize;
	private Crossword mCrossword;

	private Paint mCellStrokePaint;
	private Paint mCircleStrokePaint;
	private Paint mCellFillPaint;
	private Paint mCheatedCellFillPaint;
	private Paint mMistakeCellFillPaint;
	private Paint mMarkedCellFillPaint;
	private Paint mSelectedWordFillPaint;
	private Paint mSelectedCellFillPaint;
	private Paint mNumberTextPaint;
	private Paint mNumberStrokePaint;
	private Paint mAnswerTextPaint;
	private Paint mRebusTextPaint;
	private float mCellSize;
	private float mMarkerSideLength;
	private float mCircleRadius;
	private float mNumberTextPadding;
	private float mNumberTextHeight;
	private float mScaledDensity;
	private Stack<UndoItem> mUndoBuffer;

	private int mPuzzleWidth; // Total number of cells across
	private int mPuzzleHeight; // Total number of cells down
	private Cell[][] mPuzzleCells; // Map of the cells
	private Selectable mSelection;
	private char[] mAllowedChars;

	private float mMinScaleFactor; // Scale at which the puzzle takes up the entire screen
	private float mFitWidthScaleFactor; // Scale at which the puzzle fits in horizontally
	private float mMaxScaleFactor; // Scale beyond which the puzzle shouldn't be enlarged
	private PointF mCenteredOffset; // Offset at which the puzzle appears exactly in the middle
	private float mRenderScale; // This is the scale at which the bitmap is rendered
	private float mBitmapScale; // This is the scaling applied to the bitmap in touch resize mode
	private float mScaleStart; // The value of mRenderScale when touch resizing begins
	private PointF mBitmapOffset; // Offset of the rendered bitmap
	private RectF mTranslationBounds; // Bitmap translation limits

	private ScaleGestureDetector mScaleDetector;
	private GestureDetector mGestureDetector;

	private BitmapRenderer mAsyncRenderer;
	private Canvas mPuzzleCanvas;
	private Bitmap mPuzzleBitmap;
	private Paint mBitmapPaint;

	private boolean mIsZooming;
	private boolean mIgnoreZoom;
	private Zoomer mZoomer;
	private Scroller mScroller;
	private boolean mIsSolved;

	private boolean mSoftInputEnabled;
	private boolean mIsEditable;
	private boolean mSkipOccupiedOnType;
	private boolean mSelectFirstUnoccupiedOnNav;
	private int mUndoMode;
	private boolean mRevealSetsCheatFlag;
	private int mMarkerDisplayMode;
	private int mMaxBitmapSize;

	private Rect mTempRect = new Rect();

	private final Object mRendererLock = new Object();

	private OnSelectionChangeListener mSelectionChangeListener;
	private OnStateChangeListener mStateChangeListener;
	private OnLongPressListener mLongpressListener;

	private CrosswordInputConnection.OnInputEventListener mInputEventListener
			= new CrosswordInputConnection.OnInputEventListener()
	{
		@Override
		public void onWordEntered(CharSequence text)
		{
			if (text != null && mSelection != null) {
				// Words like "ain't" contain punctuation marks that ain't
				// valid, but may appear in a crossword in punctuation-less
				// form. For this reason, we strip out invalid characters
				// before considering whether we want to fill them into the
				// selection.
				char[] chars = text.toString().toCharArray();

				// Copy all acceptable chars to a separate array
				String[] filtered = new String[chars.length];
				int k = 0;
				for (char ch: chars) {
					if (isAcceptableChar(ch)) {
						filtered[k++] = String.valueOf(ch);
					}
				}

				if (k == 0) {
					return; // No valid chars
				}

				String[][] matrix;
				if (mSelection.getDirection() == Crossword.Word.DIR_ACROSS) {
					matrix = new String[1][k];
					System.arraycopy(filtered, 0, matrix[0], 0, k);
				} else {
					matrix = new String[k][1];
					for (int i = 0; i < k; i++) {
						matrix[i][0] = filtered[i];
					}
				}

				Selectable s = null;
				if (mSelection.isCellWithinBounds(mSelection.mCell + k - 1)) {
					// If there's enough room at the current position, add the new word
					setChars(mSelection.getRow(), mSelection.getColumn(), matrix, false);
					s = new Selectable(mSelection.mWord,
							Math.min(mSelection.mCell + k, mSelection.mWord.getLength() - 1));
				} else if (k == mSelection.mWord.getLength()) {
					// Not enough room from the current, but perfect fit for the entire row/col
					setChars(mSelection.getRow(0), mSelection.getColumn(0), matrix, false);
					s = new Selectable(mSelection.mWord, k - 1);
				}

				if (s != null) {
					resetSelection(s);
				}
			}
		}

		@Override
		public void onWordCancelled()
		{
			handleBackspace();
		}

		@Override
		public void onEditorAction(int actionCode)
		{
			if (actionCode == EditorInfo.IME_ACTION_NEXT) {
				selectNextWord();
			}
		}
	};

	public CrosswordView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		if (!isInEditMode()) {
			setLayerType(View.LAYER_TYPE_HARDWARE, null);
		}

		// Set drawing defaults
		Resources r = context.getResources();
		DisplayMetrics dm = r.getDisplayMetrics();

		int cellFillColor = NORMAL_CELL_FILL_COLOR;
		int cheatedCellFillColor = CHEATED_CELL_FILL_COLOR;
		int mistakeCellFillColor = MISTAKE_CELL_FILL_COLOR;
		int selectedWordFillColor = SELECTED_WORD_FILL_COLOR;
		int selectedCellFillColor = SELECTED_CELL_FILL_COLOR;
		int markedCellFillColor = MARKED_CELL_FILL_COLOR;
		int textColor = TEXT_COLOR;
		int cellStrokeColor = CELL_STROKE_COLOR;
		int circleStrokeColor = CIRCLE_STROKE_COLOR;

		mScaledDensity = dm.scaledDensity;
		float numberTextSize = NUMBER_TEXT_SIZE * mScaledDensity;
		float answerTextSize = ANSWER_TEXT_SIZE * mScaledDensity;

		mCellSize = CELL_SIZE * dm.density;
		mNumberTextPadding = NUMBER_TEXT_PADDING * dm.density;
		mIsEditable = true;
		mSoftInputEnabled = true;
		mSkipOccupiedOnType = false;
		mSelectFirstUnoccupiedOnNav = true;
		mMaxBitmapSize = DEFAULT_MAX_BITMAP_DIMENSION;

		// Read supplied attributes
		if (attrs != null) {
			Resources.Theme theme = context.getTheme();
			TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.CrosswordView, 0, 0);

			mCellSize = a.getDimension(R.styleable.CrosswordView_cellSize, mCellSize);
			mNumberTextPadding = a.getDimension(R.styleable.CrosswordView_numberTextPadding, mNumberTextPadding);
			numberTextSize = a.getDimension(R.styleable.CrosswordView_numberTextSize, numberTextSize);
			answerTextSize = a.getDimension(R.styleable.CrosswordView_answerTextSize, answerTextSize);
			cellFillColor = a.getColor(R.styleable.CrosswordView_defaultCellFillColor, cellFillColor);
			cheatedCellFillColor = a.getColor(R.styleable.CrosswordView_cheatedCellFillColor, cheatedCellFillColor);
			mistakeCellFillColor = a.getColor(R.styleable.CrosswordView_mistakeCellFillColor, mistakeCellFillColor);
			selectedWordFillColor = a.getColor(R.styleable.CrosswordView_selectedWordFillColor, selectedWordFillColor);
			selectedCellFillColor = a.getColor(R.styleable.CrosswordView_selectedCellFillColor, selectedCellFillColor);
			markedCellFillColor = a.getColor(R.styleable.CrosswordView_markedCellFillColor, markedCellFillColor);
			cellStrokeColor = a.getColor(R.styleable.CrosswordView_cellStrokeColor, cellStrokeColor);
			circleStrokeColor = a.getColor(R.styleable.CrosswordView_circleStrokeColor, circleStrokeColor);
			textColor = a.getColor(R.styleable.CrosswordView_textColor, textColor);
			mIsEditable = a.getBoolean(R.styleable.CrosswordView_editable, mIsEditable);
			mSkipOccupiedOnType = a.getBoolean(R.styleable.CrosswordView_skipOccupiedOnType, mSkipOccupiedOnType);
			mSelectFirstUnoccupiedOnNav = a.getBoolean(R.styleable.CrosswordView_selectFirstUnoccupiedOnNav,
					mSelectFirstUnoccupiedOnNav);

			a.recycle();
		}

		mRevealSetsCheatFlag = true;
		mMarkerSideLength = mCellSize * MARKER_TRIANGLE_LENGTH_FRACTION;

		// Init paints
		mCellFillPaint = new Paint();
		mCellFillPaint.setColor(cellFillColor);
		mCellFillPaint.setStyle(Paint.Style.FILL);

		mCheatedCellFillPaint = new Paint();
		mCheatedCellFillPaint.setColor(cheatedCellFillColor);
		mCheatedCellFillPaint.setStyle(Paint.Style.FILL);

		mMistakeCellFillPaint = new Paint();
		mMistakeCellFillPaint.setColor(mistakeCellFillColor);
		mMistakeCellFillPaint.setStyle(Paint.Style.FILL);

		mSelectedWordFillPaint = new Paint();
		mSelectedWordFillPaint.setColor(selectedWordFillColor);
		mSelectedWordFillPaint.setStyle(Paint.Style.FILL);

		mSelectedCellFillPaint = new Paint();
		mSelectedCellFillPaint.setColor(selectedCellFillColor);
		mSelectedCellFillPaint.setStyle(Paint.Style.FILL);

		mMarkedCellFillPaint = new Paint();
		mMarkedCellFillPaint.setColor(markedCellFillColor);
		mMarkedCellFillPaint.setStyle(Paint.Style.FILL);

		mCellStrokePaint = new Paint();
		mCellStrokePaint.setColor(cellStrokeColor);
		mCellStrokePaint.setStyle(Paint.Style.STROKE);
//		mCellStrokePaint.setStrokeWidth(1);

		mCircleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCircleStrokePaint.setColor(circleStrokeColor);
		mCircleStrokePaint.setStyle(Paint.Style.STROKE);
		mCircleStrokePaint.setStrokeWidth(1);

		mNumberTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mNumberTextPaint.setColor(textColor);
		mNumberTextPaint.setTextAlign(Paint.Align.CENTER);
		mNumberTextPaint.setTextSize(numberTextSize);

		// Compute number height
		mNumberTextPaint.getTextBounds("0", 0, "0".length(), mTempRect);
		mNumberTextHeight = mTempRect.height();

		mNumberStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mNumberStrokePaint.setColor(cellFillColor);
		mNumberStrokePaint.setTextAlign(Paint.Align.CENTER);
		mNumberStrokePaint.setTextSize(numberTextSize);
		mNumberStrokePaint.setStyle(Paint.Style.STROKE);
		mNumberStrokePaint.setStrokeWidth(NUMBER_TEXT_STROKE_WIDTH * mScaledDensity);

 		mAnswerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mAnswerTextPaint.setColor(textColor);
		mAnswerTextPaint.setTextSize(answerTextSize);

		mAnswerTextPaint.getTextBounds("A", 0, "A".length(), mTempRect);

		mRebusTextPaint = new Paint(mAnswerTextPaint);

		// Init rest of the values
		mCircleRadius = (mCellSize / 2) - mCircleStrokePaint.getStrokeWidth();
		mPuzzleCells = EMPTY_CELLS;
		mPuzzleWidth = 0;
		mPuzzleHeight = 0;
		mAllowedChars = EMPTY_CHARS;

		mRenderScale = 0;
		mBitmapOffset = new PointF();
		mCenteredOffset = new PointF();
		mTranslationBounds = new RectF();

		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new GestureListener());

		mContentRect = new RectF();
		mPuzzleRect = new RectF();

		mBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

		mScroller = new Scroller(context, null, true);
		mZoomer = new Zoomer(context);
		mUndoBuffer = new Stack<>();

		setFocusableInTouchMode(mIsEditable && mSoftInputEnabled);
		setOnKeyListener(this);
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);

		savedState.mBitmapOffset = mBitmapOffset;
		savedState.mRenderScale = mRenderScale;

		return savedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState savedState = (SavedState)state;
		super.onRestoreInstanceState(savedState.getSuperState());

		mBitmapOffset = savedState.mBitmapOffset;
		mRenderScale = savedState.mRenderScale;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent ev)
	{
		boolean retVal = mScaleDetector.onTouchEvent(ev);
		retVal = mGestureDetector.onTouchEvent(ev) || retVal;

		return retVal || super.onTouchEvent(ev);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (mPuzzleBitmap != null) {
			constrainTranslation();

			canvas.save();
			canvas.clipRect(mContentRect);
			canvas.translate(mBitmapOffset.x, mBitmapOffset.y);
			canvas.scale(mBitmapScale, mBitmapScale);
			canvas.drawBitmap(mPuzzleBitmap, 0, 0, mBitmapPaint);

			canvas.restore();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);

		// Get the content rect
		mContentRect.set(getPaddingLeft(), getPaddingTop(),
				w - getPaddingRight(), h - getPaddingBottom());

		resetConstraintsAndRedraw(false);
		if (mSelection != null) {
			bringIntoView(mSelection);
		}
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs)
	{
		Log.v(LOG_TAG, "onCreateInputConnection()");

		CrosswordInputConnection inputConnection = null;
		if (mSoftInputEnabled) {
			outAttrs.actionLabel = null;
			outAttrs.inputType = InputType.TYPE_NULL;
			outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_FULLSCREEN;
			outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
			outAttrs.imeOptions &= ~EditorInfo.IME_MASK_ACTION;
			outAttrs.imeOptions |= EditorInfo.IME_ACTION_NEXT;
			outAttrs.packageName = getContext().getPackageName();

			inputConnection = new CrosswordInputConnection(this);
			inputConnection.setOnInputEventListener(mInputEventListener);
		}

		return inputConnection;
	}

	@Override
	public boolean onCheckIsTextEditor()
	{
		return mIsEditable && mSoftInputEnabled;
	}

	@Override
	public void computeScroll()
	{
		super.computeScroll();

		boolean invalidate = false;

		if (mZoomer.computeZoom()) {
			mRenderScale = mScaleStart + mZoomer.getCurrZoom();

			recomputePuzzleRect();
			if (constrainScaling()) {
				recomputePuzzleRect();
			}

			mBitmapScale = mRenderScale / mScaleStart;
			invalidate = true;
		} else {
			if (mIsZooming) {
				regenerateBitmaps();
				mIsZooming = false;
			}
		}

		if (mScroller.computeScrollOffset()) {
			mBitmapOffset.set(mScroller.getCurrX(), mScroller.getCurrY());
			invalidate = true;
		}

		if (invalidate) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event)
	{
		boolean handled = false;
		if (event.getAction() == KeyEvent.ACTION_UP) {
			if (keyCode == KeyEvent.KEYCODE_SPACE) {
				switchWordDirection();
				handled = true;
			} else if (keyCode == KeyEvent.KEYCODE_DEL) {
				handleBackspace();
				handled = true;
			} else {
				int uniChar = event.getUnicodeChar();
				if (uniChar != 0) {
					handleInput((char) event.getUnicodeChar());
					handled = true;
				}
			}
		}

		return handled;
	}

	public void solveWord(Crossword.Word word)
	{
		String matrix[][];
		int wordLen = word.getLength();
		if (word.getDirection() == Crossword.Word.DIR_ACROSS) {
			matrix = new String[1][wordLen];
			for (int i = 0; i < wordLen; i++) {
				matrix[0][i] = word.cellAt(i).chars();
			}
		} else if (word.getDirection() == Crossword.Word.DIR_DOWN) {
			matrix = new String[wordLen][1];
			for (int i = 0; i < wordLen; i++) {
				matrix[i][0] = word.cellAt(i).chars();
			}
		} else {
			throw new IllegalArgumentException("Word direction not valid");
		}

		setChars(word.getStartRow(), word.getStartColumn(), matrix, true);
	}

	public boolean isSquareMarked(Crossword.Word word, int square)
	{
		Selectable s = new Selectable(word, square);
		Cell cell = mPuzzleCells[s.getRow()][s.getColumn()];

		return cell.isFlagSet(Cell.FLAG_MARKED);
	}

	public void toggleSquareMark(Crossword.Word word, int square, boolean mark)
	{
		Selectable s = new Selectable(word, square);
		Cell cell = mPuzzleCells[s.getRow()][s.getColumn()];

		if (cell != null && cell.setFlag(Cell.FLAG_MARKED, mark)) {
			if (mStateChangeListener != null) {
				mStateChangeListener.onCrosswordChanged(this);
			}
			redrawInPlace();
		}
	}

	public void solveChar(Crossword.Word word, int charIndex)
	{
		int row = word.getStartRow();
		int column = word.getStartColumn();

		if (word.getDirection() == Crossword.Word.DIR_ACROSS) {
			column += charIndex;
		} else if (word.getDirection() == Crossword.Word.DIR_DOWN) {
			row += charIndex;
		}

		String ch = word.cellAt(charIndex).chars();
		setChars(row, column, new String[][]{{ch}}, true);
	}

	public void solveCrossword()
	{
		String[][] matrix = new String[mPuzzleHeight][mPuzzleWidth];
		for (Crossword.Word word: mCrossword.getWordsAcross()) {
			int row = word.getStartRow();
			int startCol = word.getStartColumn();
			for (int i = 0, n = word.getLength(); i < n; i++) {
				matrix[row][startCol + i] = word.cellAt(i).chars();
			}
		}
		for (Crossword.Word word: mCrossword.getWordsDown()) {
			int startRow = word.getStartRow();
			int col = word.getStartColumn();
			for (int i = 0, n = word.getLength(); i < n; i++) {
				matrix[startRow + i][col] = word.cellAt(i).chars();
			}
		}

		setChars(0, 0, matrix, true);
	}

	public void reset()
	{
		for (int i = 0; i < mPuzzleHeight; i++) {
			for (int j = 0; j < mPuzzleWidth; j++) {
				Cell cell = mPuzzleCells[i][j];
				if (cell != null) {
					cell.reset();
				}
			}
		}

		onBoardChanged();
		redrawInPlace();
	}

	public Crossword.Word getSelectedWord()
	{
		return mSelection != null ? mSelection.mWord : null;
	}

	private void setChars(int startRow, int startColumn, String charMatrix[][],
			boolean setCheatFlag)
	{
		setChars(startRow, startColumn, charMatrix, setCheatFlag, false);
	}

	private void setChars(int startRow, int startColumn, String charMatrix[][],
			boolean setCheatFlag, boolean bypassUndoBuffer)
	{
		// Check startRow/startColumn
		if (startRow < 0 || startColumn < 0) {
			throw new IllegalArgumentException("Invalid startRow/startColumn");
		}

		// Check dimensions
		if (charMatrix.length < 1 || charMatrix[0].length < 1) {
			throw new IllegalArgumentException("Invalid matrix size");
		}

		int charHeight = charMatrix.length;
		int charWidth = charMatrix[0].length;
		int endRow = startRow + charHeight - 1;
		int endColumn = startColumn + charWidth - 1;

		// Check bounds
		if (endRow >= mPuzzleHeight || endColumn >= mPuzzleWidth) {
			throw new IllegalArgumentException("Chars out of bounds");
		}

		// Set up undo buffer
		String[][] undoBuf = null;
		if (!bypassUndoBuffer && mUndoMode != UNDO_NONE) {
			undoBuf = new String[charHeight][charWidth];
		}

		// Fill in the char array
		boolean cwChanged = false;
		Crossword.Cell[][] map = mCrossword.getCellMap();
		for (int i = startRow, k = 0; i <= endRow; i++, k++) {
			for (int j = startColumn, l = 0; j <= endColumn; j++, l++) {
				Cell vwCell = mPuzzleCells[i][j];
				if (vwCell != null) {
					if (undoBuf != null) {
						undoBuf[k][l] = vwCell.mChar;
					}
					String ch = Cell.canonicalize(charMatrix[k][l]);
					if (!TextUtils.equals(ch, vwCell.mChar) && isAcceptableChar(ch)) {
						boolean cellChanged = !TextUtils.equals(vwCell.mChar, ch);
						if (cellChanged) {
							vwCell.setChar(ch);
							cwChanged = true;
						}
						if (setCheatFlag) {
							vwCell.setFlag(Cell.FLAG_CHEATED, true);
						}
						if ((mMarkerDisplayMode & MARKER_ERROR) != 0) {
							vwCell.markError(map[i][j], mRevealSetsCheatFlag);
						}
					}
				}
			}
		}

		// Redraw selection
		redrawInPlace();
		if (cwChanged) {
			if (undoBuf != null) {
				clearUndoBufferIfNeeded(mSelection);
				UndoItem item = new UndoItem(undoBuf, startRow, startColumn);
				if (mSelection != null) {
					item.setSelectable(mSelection);
				}
				mUndoBuffer.push(item);
			}
			onBoardChanged();
		}
	}

	public Crossword getCrossword()
	{
		return mCrossword;
	}

	public void setCrossword(Crossword crossword)
	{
		mPuzzleHeight = 0;
		mPuzzleWidth = 0;
		mPuzzleCells = EMPTY_CELLS;
		mAllowedChars = EMPTY_CHARS;
		mCrossword = crossword;

		if (crossword != null) {
			initializeCrossword(crossword);
			selectNextWord();
		}

		mRenderScale = 0; // Will recompute when reset
		resetConstraintsAndRedraw(true);
	}

	public Crossword.State getState()
	{
		if (mCrossword == null) {
			return null;
		}

		Crossword.State state = mCrossword.newState();

		if (mSelection != null) {
			state.setSelection(mSelection.getDirection(),
					mSelection.mWord.getNumber(), mSelection.mCell);
		}

		for (int i = 0; i < mPuzzleHeight; i++) {
			for (int j = 0; j < mPuzzleWidth; j++) {
				Cell cell = mPuzzleCells[i][j];
				if (cell != null) {
					if (!cell.isEmpty()) {
						state.setCharAt(i, j, cell.mChar);
					}
					state.setCheatedAt(i, j, cell.isFlagSet(Cell.FLAG_CHEATED));
					state.setMarkedAt(i, j, cell.isFlagSet(Cell.FLAG_MARKED));
				}
			}
		}
		mCrossword.updateStateStatistics(state);

		return state;
	}

	public void restoreState(Crossword.State state)
	{
		if (state.getHeight() != mPuzzleHeight || state.getWidth() != mPuzzleWidth) {
			throw new RuntimeException("Dimensions for puzzle and state don't match");
		}

		Crossword.Cell[][] map = mCrossword.getCellMap();
		for (int i = 0; i < mPuzzleHeight; i++) {
			for (int j = 0; j < mPuzzleWidth; j++) {
				Cell cell = mPuzzleCells[i][j];
				if (cell != null) {
					cell.setFlag(Cell.FLAG_CHEATED, state.cheatedAt(i, j));
					cell.setFlag(Cell.FLAG_MARKED, state.markedAt(i, j));
					cell.setChar(state.charAt(i, j));
					if ((mMarkerDisplayMode & MARKER_ERROR) != 0) {
						cell.markError(map[i][j], mRevealSetsCheatFlag);
					}
				}
			}
		}

		if (state.hasSelection()) {
			Crossword.Word word = mCrossword.findWord(state.getSelectedDirection(),
					state.getSelectedNumber());
			int cell = state.getSelectedCell();
			if (word != null && cell < word.getLength()) {
				resetSelection(new Selectable(word, cell), false);
			}
		}

		resetConstraintsAndRedraw(true);
		onBoardChanged();
	}

	public void setOnSelectionChangeListener(OnSelectionChangeListener listener)
	{
		mSelectionChangeListener = listener;
	}

	public void setOnStateChangeListener(OnStateChangeListener listener)
	{
		mStateChangeListener = listener;
	}

	public void setOnLongPressListener(OnLongPressListener listener)
	{
		mLongpressListener = listener;
	}

	public void selectPreviousWord()
	{
		if (mCrossword != null) {
			selectWord(mCrossword.previousWord(mSelection != null ? mSelection.mWord : null));
		}
	}

	public void selectNextWord()
	{
		if (mCrossword != null) {
			selectWord(mCrossword.nextWord(mSelection != null ? mSelection.mWord : null));
		}
	}

	public void selectWord(int direction, int number)
	{
		Crossword.Word word = mCrossword.findWord(direction, number);
		if (word != null) {
			selectWord(word);
		}
	}

	public void selectWord(Crossword.Word word)
	{
		if (mCrossword != null) {
			int cell = 0;
			if (mSelectFirstUnoccupiedOnNav && word != null) {
				cell = Math.max(firstFreeCell(word), 0);
			}
			resetSelection(word != null ? new Selectable(word, cell) : null);
		}
	}

	public Rect getCellRect(Crossword.Word word, int cell)
	{
		return getCellRect(new Selectable(word, cell));
	}

	public String getCellContents(Crossword.Word word, int charIndex)
	{
		int row = word.getStartRow();
		int column = word.getStartColumn();

		if (word.getDirection() == Crossword.Word.DIR_ACROSS) {
			column += charIndex;
		} else if (word.getDirection() == Crossword.Word.DIR_DOWN) {
			row += charIndex;
		}

		Cell cell = mPuzzleCells[row][column];
		if (cell == null) {
			return null;
		}

		return cell.mChar;
	}

	public void setCellContents(Crossword.Word word, int charIndex, String sol)
	{
		int row = word.getStartRow();
		int column = word.getStartColumn();

		if (word.getDirection() == Crossword.Word.DIR_ACROSS) {
			column += charIndex;
		} else if (word.getDirection() == Crossword.Word.DIR_DOWN) {
			row += charIndex;
		}

		setChars(row, column, new String[][]{{sol}}, true);
	}

	public Typeface getAnswerTypeface()
	{
		return mAnswerTextPaint.getTypeface();
	}

	public void setAnswerTypeface(Typeface typeface)
	{
		mAnswerTextPaint.setTypeface(typeface);
		mRebusTextPaint = new Paint(mAnswerTextPaint);

		redrawInPlace();
	}

	public int getMarkerDisplayMode()
	{
		return mMarkerDisplayMode;
	}

	public void setMarkerDisplayMode(int newMode)
	{
		if (mMarkerDisplayMode != newMode) {
			int oldMode = mMarkerDisplayMode;
			mMarkerDisplayMode = newMode;
			if ((oldMode & MARKER_ERROR) != (newMode & MARKER_ERROR)) {
				resetErrorMarkers();
			}

			redrawInPlace();
		}
	}

	public boolean isEditable()
	{
		return mIsEditable;
	}

	public void setEditable(boolean editable)
	{
		mIsEditable = editable;
		resetInputMode();
	}

	public boolean isSoftInputEnabled()
	{
		return mSoftInputEnabled;
	}

	public void setSoftInputEnabled(boolean enabled)
	{
		mSoftInputEnabled = enabled;
		resetInputMode();
	}

	public boolean skipOccupiedOnType()
	{
		return mSkipOccupiedOnType;
	}

	public void setSkipOccupiedOnType(boolean skip)
	{
		mSkipOccupiedOnType = skip;
	}

	public boolean selectFirstUnoccupiedOnNav()
	{
		return mSelectFirstUnoccupiedOnNav;
	}

	public void setSelectFirstUnoccupiedOnNav(boolean selectFirst)
	{
		mSelectFirstUnoccupiedOnNav = selectFirst;
	}

	public int getMaxBitmapSize()
	{
		return mMaxBitmapSize;
	}

	public void setMaxBitmapSize(int maxBitmapSize)
	{
		if (mMaxBitmapSize != maxBitmapSize) {
			mMaxBitmapSize = maxBitmapSize;
			resetConstraintsAndRedraw(true);
		}
	}

	public int getUndoMode()
	{
		return mUndoMode;
	}

	public void setUndoMode(int mode)
	{
		mUndoMode = mode;
	}

	private Rect getCellRect(Selectable sel)
	{
		Rect cellRect = null;
		if (sel != null) {
			float left = sel.getColumn() * mScaledCellSize + mBitmapOffset.x;
			float top = sel.getRow() * mScaledCellSize + mBitmapOffset.y;

			cellRect = new Rect();
			cellRect.left = (int) (left);
			cellRect.top = (int) (top);
			cellRect.right = (int) (left + mScaledCellSize);
			cellRect.bottom = (int) (top + mScaledCellSize);
		}

		return cellRect;
	}

	private void clearUndoBufferIfNeeded(Selectable selectable)
	{
		if (mUndoMode != UNDO_SMART || mUndoBuffer.size() < 1) {
			return;
		}

		// Check the top item in the undo buffer. If it belongs to a different word, clear
		// the buffer
		UndoItem top = mUndoBuffer.peek();
		if (top.mSelectable != null
			&& !Crossword.Word.equals(selectable.mWord, top.mSelectable.mWord)) {
			mUndoBuffer.clear();
		}
	}

	private void resetInputMode()
	{
		setFocusableInTouchMode(mIsEditable && mSoftInputEnabled);

		InputMethodManager imm = (InputMethodManager)
				getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (imm.isActive(this)) {
				imm.hideSoftInputFromWindow(getWindowToken(), 0);
			}
			imm.restartInput(this);
		}
	}

	protected void handleInput(char ch)
	{
		if (!mIsEditable) {
			return;
		}

		String sch = String.valueOf(ch);
		if (mSelection != null && isAcceptableChar(ch)) {
			clearUndoBufferIfNeeded(mSelection);

			int row = mSelection.getRow();
			int col = mSelection.getColumn();

			Cell cell = mPuzzleCells[row][col];
			boolean changed = !TextUtils.equals(cell.mChar, sch);
			if (changed) {
				mUndoBuffer.push(new UndoItem(cell.mChar, row, col)
						.setSelectable(mSelection));
				cell.setChar(sch);
			}

			if ((mMarkerDisplayMode & MARKER_ERROR) != 0) {
				Crossword.Cell[][] map = mCrossword.getCellMap();
				cell.markError(map[row][col], mRevealSetsCheatFlag);
			}

			resetSelection(nextSelectable(mSelection));
			if (changed) {
				onBoardChanged();
			}
		}
	}

	private Selectable nextSelectable(Selectable selected)
	{
		int cell = selected.mCell;
		Crossword.Word word = selected.mWord;
		int nextCell = -1;

		if (mSkipOccupiedOnType) {
			nextCell = firstFreeCell(word, cell + 1);
			if (nextCell == -1 && cell + 1 < word.getLength()) {
				// No more free cells from this point, but we're still not
				// at the end
				nextCell = cell + 1;
			}
		} else {
			if (cell + 1 < word.getLength()) {
				nextCell = cell + 1;
			}
		}

		if (nextCell == -1) {
			word = mCrossword.nextWord(word);
			if (mSelectFirstUnoccupiedOnNav) {
				nextCell = Math.max(firstFreeCell(word), 0);
			} else {
				nextCell = 0;
			}
		}

		return new Selectable(word, nextCell);
	}

	protected void handleBackspace()
	{
		if (mSelection == null || !mIsEditable) {
			return;
		}

		// If the undo buffer contains items, perform an undo action
		if (!mUndoBuffer.isEmpty()) {
			UndoItem item = mUndoBuffer.pop();
			setChars(item.mStartRow, item.mStartCol, item.mChars, false, true);
			Selectable sel = item.mSelectable;
			if (sel != null) {
				Crossword.Word word = mCrossword.findWord(sel.getDirection(),
						sel.mWord.getNumber());
				resetSelection(new Selectable(word, sel.mCell));
			}

			return;
		}

		// Otherwise, act as a simple backspace
		Crossword.Word selectedWord = mSelection.mWord;
		int selectedCell = mSelection.mCell;

		Selectable s = new Selectable(mSelection);

		if (mPuzzleCells[s.getRow()][s.getColumn()].isEmpty()) {
			if (selectedCell > 0) {
				// Go back one cell and remove the char
				s.mCell = --selectedCell;
			} else {
				// At the first letter of a word. Select the previous word and do
				// what we did if (mSelectedCell > 0)
				selectedWord = mCrossword.previousWord(selectedWord);
				selectedCell = selectedWord.getLength() - 1;

				s.mWord = selectedWord;
				s.mCell = selectedCell;
			}
		}

		int row = s.getRow();
		int col = s.getColumn();

		boolean changed = mPuzzleCells[row][col].clearChar();
		if ((mMarkerDisplayMode & MARKER_ERROR) != 0) {
			mPuzzleCells[row][col].setFlag(Cell.FLAG_ERROR, false);
		}

		resetSelection(new Selectable(selectedWord, selectedCell));
		if (changed) {
			onBoardChanged();
		}
	}

	public void switchWordDirection()
	{
		Selectable ortho = null;
		if (mSelection == null && mCrossword != null) {
			ortho = new Selectable(mCrossword.nextWord(null), 0);
		} else if (mSelection != null) {
			Cell cell = mPuzzleCells[mSelection.getRow()][mSelection.getColumn()];
			if (mSelection.getDirection() == Crossword.Word.DIR_ACROSS) {
				if (cell.mWordDownNumber != Cell.WORD_NUMBER_NONE) {
					Crossword.Word word = mCrossword.findWord(Crossword.Word.DIR_DOWN,
							cell.mWordDownNumber);
					if (word != null) {
						ortho = new Selectable(word,
								mSelection.getStartRow() - word.getStartRow());
					}
				}
			} else if (mSelection.getDirection() == Crossword.Word.DIR_DOWN) {
				if (cell.mWordAcrossNumber != Cell.WORD_NUMBER_NONE) {
					Crossword.Word word = mCrossword.findWord(Crossword.Word.DIR_ACROSS,
							cell.mWordAcrossNumber);
					if (word != null) {
						ortho = new Selectable(word,
								mSelection.getStartColumn() - word.getStartColumn());
					}
				}
			}
		}

		if (ortho != null) {
			resetSelection(ortho);
		}
	}

	protected boolean zoomTo(float finalRenderScale)
	{
		if (Math.abs(finalRenderScale - mRenderScale) < .01f) {
			return false;
		}

		mZoomer.forceFinished(true);
		mIsZooming = true;
		mScaleStart = mRenderScale;

		mZoomer.startZoom(finalRenderScale - mScaleStart);
		ViewCompat.postInvalidateOnAnimation(CrosswordView.this);

		return true;
	}

	protected void showKeyboard()
	{
		if (mSoftInputEnabled) {
			InputMethodManager imm = (InputMethodManager)
					getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(CrosswordView.this, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	private void resetErrorMarkers()
	{
		Crossword.Cell[][] map = mCrossword.getCellMap();
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				Cell vwCell = mPuzzleCells[i][j];
				if (vwCell != null) {
					if ((mMarkerDisplayMode & MARKER_ERROR) != 0) {
						vwCell.markError(map[i][j], mRevealSetsCheatFlag);
					} else {
						vwCell.setFlag(Cell.FLAG_ERROR, false);
					}
				}
			}
		}
	}

	private void initializeCrossword(Crossword crossword)
	{
		mPuzzleWidth = crossword.getWidth();
		mPuzzleHeight = crossword.getHeight();
		mPuzzleCells = new Cell[mPuzzleHeight][mPuzzleWidth];

		// Copy allowed characters
		char allowedChars[] = crossword.getAlphabet();
		mAllowedChars = new char[allowedChars.length];
		for (int i = 0, n = allowedChars.length; i < n; i++) {
			mAllowedChars[i] = Character.toUpperCase(allowedChars[i]);
		}

		// Copy across
		for (Crossword.Word word: crossword.getWordsAcross()) {
			int row = word.getStartRow();
			int startColumn = word.getStartColumn();

			// Chars
			for (int column = startColumn, p = 0, n = word.getLength(); p < n; column++, p++) {
				Cell cell = new Cell();
				cell.mWordAcrossNumber = word.getNumber();
				cell.setFlag(Cell.FLAG_CIRCLED, word.cellAt(p).isCircled());
				mPuzzleCells[row][column] = cell;
			}

			// Number
			mPuzzleCells[row][startColumn].mNumber = word.getNumber() + "";
		}

		// Copy down
		for (Crossword.Word word: crossword.getWordsDown()) {
			int startRow = word.getStartRow();
			int column = word.getStartColumn();

			// Chars
			for (int row = startRow, p = 0, n = word.getLength(); p < n; row++, p++) {
				// It's possible that we already have a cell in that position from 'Across'
				// Check before creating a new cell

				Cell cell = mPuzzleCells[row][column];
				if (cell == null) {
					cell = new Cell();
					if (word.cellAt(p).isCircled()) {
						cell.setFlag(Cell.FLAG_CIRCLED, true);
					}
					mPuzzleCells[row][column] = cell;
				}

				cell.mWordDownNumber = word.getNumber();
			}

			// Number
			if (mPuzzleCells[startRow][column].mNumber == null) {
				mPuzzleCells[startRow][column].mNumber = word.getNumber() + "";
			}
		}
	}

	private void resetConstraintsAndRedraw(boolean forceBitmapRegen)
	{
		boolean regenBitmaps = mPuzzleBitmap == null;

		// Determine the scale at which the puzzle takes up the entire width
		float unscaledWidth = mPuzzleWidth * mCellSize + 1; // +1px for stroke brush
		mFitWidthScaleFactor = mContentRect.width() / unscaledWidth;

		// Set the default scale to be "fit to width"
		if (mRenderScale < .01) {
			mRenderScale = mFitWidthScaleFactor;
		}

		// Determine the smallest scale factor
		if (mContentRect.width() < mContentRect.height()) {
			mMinScaleFactor = mFitWidthScaleFactor;
		} else {
			float unscaledHeight = mPuzzleHeight * mCellSize + 1;
			mMinScaleFactor = mContentRect.height() / unscaledHeight; // +1px for stroke brush
		}

		int largestDimension = Math.max(mPuzzleWidth, mPuzzleHeight);
		float maxAvailableDimension = (float)(mMaxBitmapSize - 1); // stroke brush again

		mMaxScaleFactor = maxAvailableDimension / (largestDimension * mCellSize);
		mMaxScaleFactor = Math.max(mMaxScaleFactor, mMinScaleFactor);

		mBitmapScale = 1.0f;
		mIsZooming = false;

		// Recompute scaled puzzle size
		recomputePuzzleRect();

		if (regenBitmaps || forceBitmapRegen) {
			regenerateBitmaps();
		}
	}

	private void regenerateBitmaps()
	{
		synchronized (mRendererLock) {
			if (mAsyncRenderer != null) {
				mAsyncRenderer.cancel(false);
			}

			// A 1px size line is always present, so it's not enough to just
			// check for zero
			if (mPuzzleRect.width() > 1 && mPuzzleRect.height() > 1) {
				mAsyncRenderer = new BitmapRenderer(mRenderScale);
				mAsyncRenderer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}

	private void recomputePuzzleRect()
	{
		// Compute scaled puzzle rect
		mScaledCellSize = mRenderScale * mCellSize;
		mPuzzleRect.set(0, 0,
				mPuzzleWidth * mScaledCellSize + 1,   // w/h get an extra pixel due to the
				mPuzzleHeight * mScaledCellSize + 1); // hairline-wide stroke of the cell

		// Determine center locations
		mCenteredOffset.set(mContentRect.left + (mContentRect.width() - mPuzzleRect.width()) / 2.0f,
				mContentRect.top + (mContentRect.height() - mPuzzleRect.height()) / 2.0f);

		// Compute translation bounds
		mTranslationBounds.set(mContentRect.right - mPuzzleRect.right,
				mContentRect.bottom - mPuzzleRect.bottom,
				mContentRect.left - mPuzzleRect.left,
				mContentRect.top - mPuzzleRect.top);

		constrainTranslation();
	}

	private void constrainTranslation()
	{
		// Clamp the offset to fit within the view
		clampPointF(mBitmapOffset, mTranslationBounds);

		if (mPuzzleRect.width() < mContentRect.width()) {
			// Puzzle is narrower than the available width - center it horizontally
			mBitmapOffset.x = mCenteredOffset.x;
		}

		// Vertical
		if (mPuzzleRect.height() < mContentRect.height()) {
			// Puzzle is shorter than the available height - center it vertically
			mBitmapOffset.y = mCenteredOffset.y;
		}
	}

	private boolean constrainScaling()
	{
		if (mPuzzleRect.width() < mContentRect.width() &&
				mPuzzleRect.height() < mContentRect.height()) {
			if (mRenderScale < mMinScaleFactor) {
				mRenderScale = mMinScaleFactor;
				return true;
			}
		}

		if (mRenderScale > mMaxScaleFactor) {
			mRenderScale = mMaxScaleFactor;
			return true;
		}

		return false;
	}

	private void bringIntoView(Selectable sel)
	{
		if (sel == null) {
			return;
		}

		RectF wordRect = new RectF();

		wordRect.left = sel.getStartColumn()
				* mScaledCellSize - mContentRect.left;
		wordRect.top = sel.getStartRow()
				* mScaledCellSize - mContentRect.top;

		if (sel.getDirection() == Crossword.Word.DIR_ACROSS) {
			wordRect.right = wordRect.left + sel.mWord.getLength() * mScaledCellSize;
			wordRect.bottom = wordRect.top + mScaledCellSize;
		} else if (sel.getDirection() == Crossword.Word.DIR_DOWN) {
			wordRect.right = wordRect.left + mScaledCellSize;
			wordRect.bottom = wordRect.top + sel.mWord.getLength() * mScaledCellSize;
		}

		RectF objectRect = new RectF(wordRect);
		RectF visibleArea = new RectF(-mBitmapOffset.x, -mBitmapOffset.y,
				-mBitmapOffset.x + mContentRect.width(),
				-mBitmapOffset.y + mContentRect.height());

		if (visibleArea.contains(objectRect)) {
			return; // Already visible
		}

		if (objectRect.width() > visibleArea.width()
				|| objectRect.height() > visibleArea.height()) {
			// Available area isn't large enough to fit the entire word
			// Is the selected cell visible? If not, bring it into view

			RectF cellRect = new RectF();
			cellRect.left = sel.getColumn() * mScaledCellSize;
			cellRect.top = sel.getRow() * mScaledCellSize;
			cellRect.right = cellRect.left + mScaledCellSize;
			cellRect.bottom = cellRect.top + mScaledCellSize;

			if (visibleArea.contains(cellRect)) {
				return; // Already visible
			}

			objectRect.set(cellRect);
		}

		// Compute view that includes the object in the center
		PointF end = new PointF((visibleArea.width() - objectRect.width()) / 2.0f - objectRect.left,
				(visibleArea.height() - objectRect.height()) / 2.0f - objectRect.top);

		// Clamp the values
		clampPointF(end, mTranslationBounds);

		// Compute the distance to travel from current location
		float distanceX = end.x - mBitmapOffset.x;
		float distanceY = end.y - mBitmapOffset.y;

		// Scroll the point into view
		mScroller.startScroll((int) mBitmapOffset.x, (int) mBitmapOffset.y,
				(int) distanceX, (int) distanceY, NAVIGATION_SCROLL_DURATION_MS);
	}

	private static void clampPointF(PointF point, RectF rect)
	{
		// Clamp the values
		if (point.x < rect.left) {
			point.x = rect.left;
		} else if (point.x > rect.right) {
			point.x = rect.right;
		}

		if (point.y < rect.top) {
			point.y = rect.top;
		} else if (point.y > rect.bottom) {
			point.y = rect.bottom;
		}
	}

	private void redrawInPlace()
	{
		if (mPuzzleCanvas != null) {
			mAsyncRenderer = new BitmapRenderer(mRenderScale);
			mAsyncRenderer.renderPuzzle(mPuzzleCanvas);

			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	private void renderCell(Canvas canvas, Cell cell, RectF cellRect, Paint fillPaint)
	{
		canvas.drawRect(cellRect, fillPaint);
		canvas.drawRect(cellRect, mCellStrokePaint);

		if (cell.isFlagSet(Cell.FLAG_CIRCLED)) {
			canvas.drawCircle(cellRect.centerX(), cellRect.centerY(),
					mCircleRadius, mCircleStrokePaint);
		}

		float numberY = cellRect.top + mNumberTextPadding + mNumberTextHeight;

		if (cell.mNumber != null) {
			float textWidth = mNumberTextPaint.measureText(cell.mNumber);
			float numberX = cellRect.left + mNumberTextPadding + (textWidth / 2);

			if (cell.isFlagSet(Cell.FLAG_CIRCLED)) {
				canvas.drawText(cell.mNumber, numberX, numberY, mNumberStrokePaint);
			}

			canvas.drawText(cell.mNumber, numberX, numberY, mNumberTextPaint);
		}

		if (cell.isFlagSet(Cell.FLAG_MARKED) && (mMarkerDisplayMode & MARKER_CUSTOM) != 0) {
			Path path = new Path();
			path.moveTo(cellRect.right - mMarkerSideLength, cellRect.top);
			path.lineTo(cellRect.right, cellRect.top);
			path.lineTo(cellRect.right, cellRect.top + mMarkerSideLength);
			path.close();

			canvas.drawPath(path, mMarkedCellFillPaint);
			canvas.drawPath(path, mCellStrokePaint);
		}

		if (cell.isFlagSet(Cell.FLAG_CHEATED) && (mMarkerDisplayMode & MARKER_CHEAT) != 0) {
			Path path = new Path();
			path.moveTo(cellRect.right, cellRect.bottom);
			path.lineTo(cellRect.right - mMarkerSideLength, cellRect.bottom);
			path.lineTo(cellRect.right, cellRect.bottom - mMarkerSideLength);
			path.close();

			canvas.drawPath(path, mCheatedCellFillPaint);
			canvas.drawPath(path, mCellStrokePaint);
		}

		if (cell.isFlagSet(Cell.FLAG_ERROR) && (mMarkerDisplayMode & MARKER_ERROR) != 0) {
			Path path = new Path();
			path.moveTo(cellRect.left, cellRect.bottom);
			path.lineTo(cellRect.left + mMarkerSideLength, cellRect.bottom);
			path.lineTo(cellRect.left, cellRect.bottom - mMarkerSideLength);
			path.close();

			canvas.drawPath(path, mMistakeCellFillPaint);
			canvas.drawPath(path, mCellStrokePaint);
		}

		if (!cell.isEmpty()) {
			String text = cell.mChar;
			RectF textRect = new RectF(cellRect);
			textRect.top = numberY;

			// FIXME: cache text widths and heights

			if (text.length() > 8) {
				// FIXME: allow customization of max length and replacement pattern
				text = text.substring(0, 8) + "…";
			}

			float textSize = mAnswerTextPaint.getTextSize();
			float textWidth;
			float xOffset;

			do {
				mRebusTextPaint.setTextSize(textSize);
				textWidth = mRebusTextPaint.measureText(text);

				xOffset = textWidth / 2f;
				textSize -= mScaledDensity;
			} while (textWidth >= mCellSize);

			mRebusTextPaint.getTextBounds("A", 0, 1, mTempRect);
			float yOffset = mTempRect.height() / 2;

			canvas.drawText(text, textRect.centerX() - xOffset,
					textRect.centerY() + yOffset, mRebusTextPaint);
		}
	}

	private void resetSelection(Selectable selection)
	{
		resetSelection(selection, true);
	}

	private void resetSelection(Selectable selection, boolean bringIntoView)
	{
		boolean selectionChanged = !Selectable.equals(selection, mSelection);

		if (mPuzzleCanvas != null) {
			// Create a canvas on top of the existing bitmap
			if (mSelection != null && selectionChanged) {
				// Clear the selection from the deselected word
				renderSelection(mPuzzleCanvas, true);
			}
		}

		// Set new selection
		mSelection = selection;

		if (mPuzzleCanvas != null) {
			// Bring new selection into view, if requested
			if (bringIntoView) {
				bringIntoView(mSelection);
			}

			// Render the new selection
			renderSelection(mPuzzleCanvas, false);
		}

		// Notify the listener of the change in selection
		if (selectionChanged && mSelectionChangeListener != null) {
			mSelectionChangeListener.onSelectionChanged(this,
					mSelection != null ? mSelection.mWord : null,
					mSelection != null ? mSelection.mCell : -1);
		}

		// Invalidate the view
		invalidate();
	}

	private void renderSelection(Canvas canvas, boolean clearSelection)
	{
		if (mSelection == null) {
			return;
		}

		int startRow = mSelection.getStartRow();
		int endRow = mSelection.getEndRow();
		int startColumn = mSelection.getStartColumn();
		int endColumn = mSelection.getEndColumn();
		RectF cellRect = new RectF();

		canvas.save();
		canvas.scale(mRenderScale, mRenderScale);

		float top = mSelection.getStartRow() * mCellSize;
		for (int row = startRow, index = 0; row <= endRow; row++, top += mCellSize) {
			float left = mSelection.getStartColumn() * mCellSize;
			for (int column = startColumn; column <= endColumn; column++, left += mCellSize) {
				Cell cell = mPuzzleCells[row][column];
				if (cell != null) {
					// Draw the unselected cell
					Paint paint;
					if (clearSelection) {
						paint = mCellFillPaint;
					} else {
						if (index == mSelection.mCell) {
							paint = mSelectedCellFillPaint;
						} else {
							paint = mSelectedWordFillPaint;
						}
					}

					cellRect.set(left, top, left + mCellSize, top + mCellSize);
					renderCell(canvas, cell, cellRect, paint);
				}

				index++;
			}
		}

		canvas.restore();
	}

	private boolean isAcceptableChar(String ch)
	{
		if (ch == null) {
			return false;
		}

		String upper = ch.toUpperCase();
		for (int i = 0, n = ch.length(); i < n; i++) {
			if (!isAcceptableChar(upper.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	private boolean isAcceptableChar(char ch)
	{
		char upper = Character.toUpperCase(ch);
		for (char allowedChar: mAllowedChars) {
			if (upper == allowedChar) {
				return true;
			}
		}

		return false;
	}

	private boolean getCellOffset(float viewX, float viewY, CellOffset offset)
	{
		viewX -= mBitmapOffset.x;
		viewY -= mBitmapOffset.y;

		int column = (int) (viewX / mScaledCellSize);
		int row = (int) (viewY / mScaledCellSize);

		if (row >= 0 && row < mPuzzleHeight && column >= 0 && column < mPuzzleWidth) {
			offset.mRow = row;
			offset.mColumn = column;

			return true;
		}

		return false;
	}

	private void handleCellTap(CellOffset offset)
	{
		Cell cell = mPuzzleCells[offset.mRow][offset.mColumn];
		if (cell == null) {
			return;
		}

		int preferredDir = Crossword.Word.DIR_ACROSS;
		if (mSelection != null) {
			if (offset.mRow == mSelection.getRow()
					&& offset.mColumn == mSelection.getColumn()) {
				// Same cell tapped - flip direction
				switchWordDirection();
				if (mIsEditable) {
					showKeyboard();
				}
				return;
			}

			// Select a word in the same direction
			preferredDir = mSelection.getDirection();
		}

		Selectable sel = getSelectable(offset, preferredDir);
		if (sel != null) {
			resetSelection(sel);

			// Undo buffer is always reset whenever a user switches to a different word.
			// We also want to reset the buffer if the user taps a different cell in the same word
			mUndoBuffer.clear();
		}

		if (mIsEditable) {
			showKeyboard();
		}
	}

	private void handleCellLongPress(CellOffset offset)
	{
		Cell cell = mPuzzleCells[offset.mRow][offset.mColumn];
		if (cell == null) {
			return;
		}

		int preferredDir = Crossword.Word.DIR_ACROSS;
		if (mSelection != null) {
			preferredDir = mSelection.getDirection();
		}

		Selectable sel = getSelectable(offset, preferredDir);
		if (sel != null) {
			resetSelection(sel, false);
			if (mLongpressListener != null) {
				// Notify the listener
				mLongpressListener.onCellLongPressed(this,
						mSelection.mWord, sel.mCell);
			}
		}
	}

	private int firstFreeCell(Crossword.Word word)
	{
		return firstFreeCell(word, 0);
	}

	private int firstFreeCell(Crossword.Word word, int start)
	{
		int firstFree = -1;
		if (word != null) {
			for (int i = start, n = word.getLength(); i < n; i++) {
				int row = word.getStartRow();
				int col = word.getStartColumn();
				if (word.getDirection() == Crossword.Word.DIR_ACROSS) {
					col += i;
				} else if (word.getDirection() == Crossword.Word.DIR_DOWN) {
					row += i;
				}

				Cell cell = mPuzzleCells[row][col];
				if (cell.isEmpty()) {
					firstFree = i;
					break;
				}
			}
		}

		return firstFree;
	}

	public boolean isSolved()
	{
		boolean solved = true;
		Crossword.Cell[][] map = mCrossword.getCellMap();
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				Cell vwCell = mPuzzleCells[i][j];
				if (vwCell != null) {
					if (!vwCell.isSolved(map[i][j])) {
						solved = false;
						break;
					}
				}
			}
		}

		return solved;
	}

	private void onBoardChanged()
	{
		boolean solved = isSolved();
		if (mStateChangeListener != null) {
			mStateChangeListener.onCrosswordChanged(this);

			if (solved != mIsSolved) {
				if (solved) {
					mStateChangeListener.onCrosswordSolved(this);
				} else {
					mStateChangeListener.onCrosswordUnsolved(this);
				}
			}
		}
		mIsSolved = solved;
	}

	private Selectable getSelectable(CellOffset co, int preferredDir)
	{
		Cell cell = mPuzzleCells[co.mRow][co.mColumn];
		Selectable sel = null;

		if (cell != null) {
			int across = cell.mWordAcrossNumber;
			int down = cell.mWordDownNumber;

			// Select an Across word if we're currently selecting Across words,
			// or if we're selecting Down words, but no Down words are
			// available at that cell
			if (across != Cell.WORD_NUMBER_NONE) {
				if (preferredDir == Crossword.Word.DIR_ACROSS
						|| (preferredDir == Crossword.Word.DIR_DOWN
						&& down == Cell.WORD_NUMBER_NONE)) {
					Crossword.Word word = mCrossword.findWord(Crossword.Word.DIR_ACROSS, across);
					sel = new Selectable(word, co.mColumn - word.getStartColumn());
				}
			}

			// Select a Down word if we're currently selecting Down words,
			// or if we're selecting Across words, but no Across words are
			// available at that cell
			if (down != Cell.WORD_NUMBER_NONE) {
				if (preferredDir == Crossword.Word.DIR_DOWN
						|| (preferredDir == Crossword.Word.DIR_ACROSS
						&& across == Cell.WORD_NUMBER_NONE)) {
					Crossword.Word word = mCrossword.findWord(Crossword.Word.DIR_DOWN, down);
					sel = new Selectable(word, co.mRow - word.getStartRow());
				}
			}
		}

		return sel;
	}

	static class SavedState
			extends BaseSavedState
	{
		public static final Creator<SavedState> CREATOR = new Creator<SavedState>()
		{
			@Override
			public SavedState createFromParcel(Parcel source)
			{
				return new SavedState(source);
			}

			@Override
			public SavedState[] newArray(int size)
			{
				return new SavedState[size];
			}
		};

		private float mRenderScale;
		private PointF mBitmapOffset;

		SavedState(Parcelable superState)
		{
			super(superState);
		}

		private SavedState(Parcel in)
		{
			super(in);

			mRenderScale = in.readFloat();
			mBitmapOffset = in.readParcelable(PointF.class.getClassLoader());
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags)
		{
			super.writeToParcel(dest, flags);

			dest.writeFloat(mRenderScale);
			dest.writeParcelable(mBitmapOffset, 0);
		}
	}

	private static class UndoItem
	{
		short mStartRow;
		short mStartCol;
		String[][] mChars;
		Selectable mSelectable;

		UndoItem(String ch, int row, int col)
		{
			this(new String[][] { { ch } }, row, col);
		}

		UndoItem(String[][] chars, int row, int col)
		{
			mChars = chars;
			mStartRow = (short) row;
			mStartCol = (short) col;
			mSelectable = null;
		}

		UndoItem setSelectable(Selectable selectable)
		{
			mSelectable = selectable;
			return this;
		}
	}

	private static class Selectable
			implements Parcelable
	{
		Crossword.Word mWord;
		int mCell;

		Selectable(Selectable s)
		{
			mWord = s.mWord;
			mCell = s.mCell;
		}

		Selectable(Crossword.Word word, int cell)
		{
			mWord = word;
			mCell = cell;
		}

		int getDirection()
		{
			return mWord.getDirection();
		}

		int getStartRow()
		{
			return mWord.getStartRow();
		}

		int getStartColumn()
		{
			return mWord.getStartColumn();
		}

		int getRow()
		{
			return getRow(mCell);
		}

		int getColumn()
		{
			return getColumn(mCell);
		}

		int getEndRow()
		{
			return getRow(mWord.getLength() - 1);
		}

		int getEndColumn()
		{
			return getColumn(mWord.getLength() - 1);
		}

		int getRow(int cell)
		{
			int v = mWord.getStartRow();
			if (mWord.getDirection() == Crossword.Word.DIR_DOWN) {
				v += Math.min(cell, mWord.getLength() - 1);
			}

			return v;
		}

		int getColumn(int cell)
		{
			int v = mWord.getStartColumn();
			if (mWord.getDirection() == Crossword.Word.DIR_ACROSS) {
				v += Math.min(cell, mWord.getLength() - 1);
			}

			return v;
		}

		boolean isCellWithinBounds(int cell)
		{
			return cell >= 0 && cell < mWord.getLength();
		}

		public static boolean equals(Selectable s1, Selectable s2)
		{
			if (s1 == null || s2 == null) {
				return s1 == s2;
			}

			return Crossword.Word.equals(s1.mWord, s2.mWord)
					&& s1.mCell == s2.mCell;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof Selectable) {
				return equals(this, (Selectable) o);
			}

			return super.equals(o);
		}

		public static final Creator<Selectable> CREATOR = new Creator<Selectable>()
		{
			public Selectable createFromParcel(Parcel in)
			{
				return new Selectable(in);
			}

			public Selectable[] newArray(int size)
			{
				return new Selectable[size];
			}
		};

		private Selectable(Parcel in)
		{
			mWord = in.readParcelable(Crossword.Word.class.getClassLoader());
			mCell = in.readInt();
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeParcelable(mWord, 0);
			dest.writeInt(mCell);
		}
	}

	private static class CellOffset
	{
		private int mRow;
		private int mColumn;

		public CellOffset()
		{
		}

		public CellOffset(CellOffset offset)
		{
			mRow = offset.mRow;
			mColumn = offset.mColumn;
		}
	}

	private static class Cell
			implements Parcelable
	{
		public static final Creator<Cell> CREATOR = new Creator<Cell>()
		{
			public Cell createFromParcel(Parcel in)
			{
				return new Cell(in);
			}

			public Cell[] newArray(int size)
			{
				return new Cell[size];
			}
		};

		static final int WORD_NUMBER_NONE = -1;

		static final int FLAG_CHEATED = 1;
		static final int FLAG_CIRCLED = 1 << 1;
		static final int FLAG_ERROR   = 1 << 2;
		static final int FLAG_MARKED  = 1 << 3;

		String mNumber;
		String mChar;
		int mWordAcrossNumber;
		int mWordDownNumber;
		int mFlag;

		public Cell()
		{
			mFlag = 0;
			mWordAcrossNumber = mWordDownNumber = WORD_NUMBER_NONE;
		}

		public static String canonicalize(String ch)
		{
			return ch != null ? ch.toUpperCase() : null;
		}

		public boolean isEmpty()
		{
			return mChar == null;
		}

		public boolean clearChar()
		{
			return setChar(null);
		}

		public void reset()
		{
			clearChar();
			setFlag(FLAG_CHEATED | FLAG_ERROR, false);
		}

		public boolean setChar(String ch)
		{
			boolean changed = false;
			ch = canonicalize(ch);

			if (!TextUtils.equals(ch, mChar)) {
				mChar = ch;
				changed = true;
			}

			return changed;
		}

		public boolean isSolved(Crossword.Cell cwCell)
		{
			return mChar != null && cwCell.contains(mChar);
		}

		public void markError(Crossword.Cell cwCell, boolean setCheatFlag)
		{
			boolean error = !isEmpty() && !cwCell.contains(mChar);
			if (error) {
				mFlag |= FLAG_ERROR;
				if (setCheatFlag) {
					mFlag |= FLAG_CHEATED;
				}
			} else {
				mFlag &= ~FLAG_ERROR;
			}
		}

		public boolean isFlagSet(int flag)
		{
			return (mFlag & flag) == flag;
		}

		public boolean setFlag(int flag, boolean set)
		{
			int old = mFlag;
			if (set) {
				mFlag |= flag;
			} else {
				mFlag &= ~flag;
			}

			return old != mFlag;
		}

		private Cell(Parcel in)
		{
			mNumber = in.readString();
			setChar(in.readString());
			mWordAcrossNumber = in.readInt();
			mWordDownNumber = in.readInt();
			mFlag = in.readInt();
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeString(mNumber);
			dest.writeString(mChar);
			dest.writeInt(mWordAcrossNumber);
			dest.writeInt(mWordDownNumber);
			dest.writeInt(mFlag);
		}
	}

	private class BitmapRenderer
			extends AsyncTask<Void, Void, Void>
	{
		private float mScale;
		private Canvas mRenderingCanvas;
		private Bitmap mRenderedPuzzle;

		public BitmapRenderer(float scaleFactor)
		{
			mScale = scaleFactor;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			Canvas canvas = null;
			Bitmap puzzleBitmap = null;

			if (mPuzzleWidth > 0 && mPuzzleHeight > 0) {
				int width = (int) mPuzzleRect.width();
				int height = (int) mPuzzleRect.height();

				puzzleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
				canvas = new Canvas(puzzleBitmap);

				int sizeBytes;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					sizeBytes = puzzleBitmap.getAllocationByteCount();
				} else {
					sizeBytes = puzzleBitmap.getByteCount();
				}
				Log.d(LOG_TAG, String.format("Created a new %dx%d puzzle bitmap (%,dkB)...",
						width, height, sizeBytes / 1024));

				renderPuzzle(canvas);
			} else {
				Log.d(LOG_TAG, "Not creating an empty puzzle bitmap");
			}

			mRenderedPuzzle = puzzleBitmap;
			mRenderingCanvas = canvas;

			return null;
		}

		@Override
		protected void onPostExecute(Void param)
		{
			super.onPostExecute(param);

			mPuzzleCanvas = mRenderingCanvas;
			mPuzzleBitmap = mRenderedPuzzle;
			mBitmapScale = 1.0f;

			Log.d(LOG_TAG, "Invalidating...");
			ViewCompat.postInvalidateOnAnimation(CrosswordView.this);

			synchronized (mRendererLock) {
				mAsyncRenderer = null;
			}
		}

		@Override
		protected void onCancelled(Void aVoid)
		{
			super.onCancelled(aVoid);

			Log.d(LOG_TAG, "Task cancelled");

			synchronized (mRendererLock) {
				mAsyncRenderer = null;
			}
		}

		public void renderPuzzle(Canvas canvas)
		{
			long startedMillis = SystemClock.uptimeMillis();
			RectF cellRect = new RectF();

			canvas.save();
			canvas.scale(mScale, mScale);

			float top = 0;
			for (int i = 0; i < mPuzzleHeight; i++, top += mCellSize) {
				float left = 0;
				for (int j = 0; j < mPuzzleWidth; j++, left += mCellSize) {
					if (isCancelled()) {
						return;
					}

					Cell cell = mPuzzleCells[i][j];
					if (cell != null) {
						cellRect.set(left, top, left + mCellSize, top + mCellSize);
						renderCell(canvas, cell, cellRect, mCellFillPaint);
					}
				}
			}

			canvas.restore();
			renderSelection(canvas, false);

			Log.d(LOG_TAG, String.format("Rendered puzzle (%.02fs)",
					(SystemClock.uptimeMillis() - startedMillis) / 1000f));
		}
	}

	private class ScaleListener
			extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector)
		{
			if (mIsZooming) {
				// Double-tap scaling interferes with autozoom, so ignore
				// zoom requests
				mIgnoreZoom = true;
				return true;
			}

			mZoomer.forceFinished(true);
			mIsZooming = false;

			mScaleStart = mRenderScale;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector)
		{
			if (mIgnoreZoom) {
				return true;
			}

			mRenderScale *= detector.getScaleFactor();

			recomputePuzzleRect();
			if (constrainScaling()) {
				recomputePuzzleRect();
			}

			mBitmapScale = mRenderScale / mScaleStart;

			invalidate();
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector)
		{
			if (!mIgnoreZoom) {
				regenerateBitmaps();
			}

			mIgnoreZoom = false;
		}
	}

	private class GestureListener
			extends GestureDetector.SimpleOnGestureListener
	{
		private CellOffset mTapLocation;

		public GestureListener()
		{
			mTapLocation = new CellOffset();
		}

		@Override
		public boolean onDown(MotionEvent e)
		{
			if (!mScroller.isFinished()) {
				mScroller.forceFinished(true);
			}

			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e)
		{
			if (getCellOffset(e.getX(), e.getY(), mTapLocation)) {
				handleCellTap(new CellOffset(mTapLocation));
				return true;
			}

			return false;
		}

		@Override
		public void onLongPress(MotionEvent e)
		{
			if (!mScaleDetector.isInProgress()
					&& getCellOffset(e.getX(), e.getY(), mTapLocation)) {
				handleCellLongPress(mTapLocation);

				// Cancel the scale gesture by sending the detector
				// an ACTION_CANCEL event, to prevent double-tap scaling
				// from interfering. There should be a nicer way to do
				// this, but there isn't..
				MotionEvent cancelEvent = MotionEvent.obtain(e.getDownTime(),
						e.getEventTime(), MotionEvent.ACTION_CANCEL,
						e.getX(), e.getY(), e.getMetaState());
				mScaleDetector.onTouchEvent(cancelEvent);
			}
		}

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			zoomTo(mFitWidthScaleFactor);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distX, float distY)
		{
			mBitmapOffset.offset(-distX, -distY);

			constrainTranslation();
			invalidate();

			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{
			// Horizontal
			int startX = (int) mBitmapOffset.x;
			int minX = startX;
			int maxX = startX;
			if (mPuzzleRect.width() >= mContentRect.width()) {
				// Puzzle exceeds content, set horizontal flinging bounds
				minX = (int) mTranslationBounds.left;
				maxX = (int) mTranslationBounds.right;
			}

			// Vertical
			int startY = (int) mBitmapOffset.y;
			int minY = startY;
			int maxY = startY;
			if (mPuzzleRect.height() >= mContentRect.height()) {
				// Puzzle exceeds content, set vertical flinging bounds
				minY = (int) mTranslationBounds.top;
				maxY = (int) mTranslationBounds.bottom;
			}

			mScroller.fling(startX, startY,
					(int) (velocityX / FLING_VELOCITY_DOWNSCALE),
					(int) (velocityY / FLING_VELOCITY_DOWNSCALE),
					minX, maxX, minY, maxY);

			return true;
		}
	}
}
