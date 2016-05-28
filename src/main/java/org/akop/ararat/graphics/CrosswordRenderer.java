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

package org.akop.ararat.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import org.akop.ararat.core.Crossword;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class CrosswordRenderer
{
	public static final int FLAG_RENDER_ANSWER  = 1;
	public static final int FLAG_RENDER_MARKERS = 2;
	public static final int FLAG_RENDER_ATTEMPT = 4;

	private static final int CELL_STROKE_COLOR = Color.parseColor("#000000");
	private static final int CELL_FILL_COLOR = Color.parseColor("#ffffff");
	private static final int PUZZLE_BG_COLOR = Color.parseColor("#000000");
	private static final int CIRCLE_STROKE_COLOR = Color.parseColor("#555555");
	private static final int TEXT_COLOR = Color.parseColor("#000000");

	private static final float CELL_STROKE_WIDTH = 1;

	private Paint mCellStrokePaint;
	private Paint mCellFillPaint;
	private Paint mPuzzleBackgroundPaint;
	private Paint mCircleStrokePaint;
	private Paint mAnswerTextPaintBase;
	private float mScaledCellStrokeWidth;

	public CrosswordRenderer(Context context)
	{
		init(context.getResources());
	}

	private void init(Resources res)
	{
		DisplayMetrics dm = res.getDisplayMetrics();

		mScaledCellStrokeWidth = CELL_STROKE_WIDTH * dm.density;

		mCellStrokePaint = new Paint();
		mCellStrokePaint.setColor(CELL_STROKE_COLOR);
		mCellStrokePaint.setStyle(Paint.Style.STROKE);
		mCellStrokePaint.setStrokeWidth(mScaledCellStrokeWidth);

		mCellFillPaint = new Paint();
		mCellFillPaint.setColor(CELL_FILL_COLOR);
		mCellFillPaint.setStyle(Paint.Style.FILL);

		mPuzzleBackgroundPaint = new Paint();
		mPuzzleBackgroundPaint.setColor(PUZZLE_BG_COLOR);
		mPuzzleBackgroundPaint.setStyle(Paint.Style.FILL);
		mPuzzleBackgroundPaint.setStrokeWidth(mScaledCellStrokeWidth);

		mCircleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCircleStrokePaint.setColor(CIRCLE_STROKE_COLOR);
		mCircleStrokePaint.setStyle(Paint.Style.STROKE);
		mCircleStrokePaint.setStrokeWidth(1);

		mAnswerTextPaintBase = new Paint(Paint.ANTI_ALIAS_FLAG);
		mAnswerTextPaintBase.setColor(TEXT_COLOR);
		mAnswerTextPaintBase.setTextAlign(Paint.Align.CENTER);
	}

	private void renderCell(RenderParams rp, int row, int col, RectF cellRect)
	{
		Crossword.Cell cell = rp.mCellMap[row][col];
		if (cell == null) {
			return;
		}

		rp.mCanvas.drawRect(cellRect, mCellFillPaint);
		rp.mCanvas.drawRect(cellRect, mCellStrokePaint);

		if (cell.isCircled() && (rp.mFlags & FLAG_RENDER_MARKERS) == FLAG_RENDER_MARKERS) {
			rp.mCanvas.drawCircle(cellRect.centerX(), cellRect.centerY(),
					rp.mRadius, mCircleStrokePaint);
		}

		if ((rp.mFlags & FLAG_RENDER_ANSWER) == FLAG_RENDER_ANSWER) {
			if (!cell.isEmpty()) {
				rp.mCanvas.drawText(cell.chars(), cellRect.left + rp.mCellDim / 2,
						cellRect.bottom - rp.mAnswerMetrics.descent, rp.mAnswerTextPaint);
			}
		} else if ((rp.mFlags & FLAG_RENDER_ATTEMPT) == FLAG_RENDER_ATTEMPT) {
			// FIXME: rebus
			String attempt = rp.mState.charAt(row, col);
			if (attempt != null) {
				rp.mCanvas.drawText(attempt, cellRect.left + rp.mCellDim / 2,
						cellRect.bottom - rp.mAnswerMetrics.descent, rp.mAnswerTextPaint);
			}
		}
	}

	public void renderToCanvas(Canvas canvas, Crossword crossword,
			Crossword.State state, int flags)
	{
		RenderParams rp = new RenderParams(canvas, crossword, state, flags);
		RectF puzzleRect = new RectF(0, 0, rp.mBmpW, rp.mBmpH);

		canvas.drawRect(puzzleRect, mPuzzleBackgroundPaint);

		float renderedWidth = rp.mCellDim * (float) rp.mCwW;
		float renderedHeight = rp.mCellDim * (float) rp.mCwH;

		RectF cellRect = new RectF();
		float leftmost = ((float) rp.mBmpW - renderedWidth) / 2f;
		float rectTop = ((float) rp.mBmpH - renderedHeight) / 2f;

		for (int i = 0; i < rp.mCwH; i++, rectTop += rp.mCellDim) {
			float rectLeft = leftmost;
			for (int j = 0; j < rp.mCwW; j++, rectLeft += rp.mCellDim) {
				cellRect.set(rectLeft, rectTop,
						rectLeft + rp.mCellDim, rectTop + rp.mCellDim);
				renderCell(rp, i, j, cellRect);
			}
		}
	}

	public void renderToFile(String path,
			Crossword crossword, Crossword.State state,
			int width, int height, int flags)
			throws IOException
	{
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		renderToCanvas(new Canvas(bmp), crossword, state, flags);

		OutputStream stream = new FileOutputStream(path);
		bmp.compress(Bitmap.CompressFormat.PNG, 80, stream);
		stream.close();
	}

	private final class RenderParams
	{
		final Canvas mCanvas;
		final float mCellDim;
		final float mRadius;
		final Paint mAnswerTextPaint;
		final int mBmpW;
		final int mBmpH;
		final int mCwW;
		final int mCwH;
		final Crossword.Cell[][] mCellMap;
		final Crossword.State mState;
		final Paint.FontMetrics mAnswerMetrics;
		final int mFlags;

		RenderParams(Canvas canvas,
				Crossword crossword, Crossword.State state, int flags)
		{
			mCanvas = canvas;
			mCwW = crossword.getWidth();
			mCwH = crossword.getHeight();
			mBmpW = canvas.getWidth();
			mBmpH = canvas.getHeight();
			mFlags = flags;

			int longestCwDim = Math.max(mCwW, mCwH);
			float shortestWantedDim = Math.min(mBmpW, mBmpH);
			mCellDim = (shortestWantedDim / longestCwDim)
					- (mScaledCellStrokeWidth / longestCwDim);
			mRadius = (mCellDim / 2) - mCircleStrokePaint.getStrokeWidth();

			mAnswerTextPaint = new Paint(mAnswerTextPaintBase);
			mAnswerTextPaint.setTextSize(mCellDim * 0.75f);

			mAnswerMetrics = mAnswerTextPaint.getFontMetrics();

			mCellMap = crossword.getCellMap();
			mState = state;
		}
	}
}
