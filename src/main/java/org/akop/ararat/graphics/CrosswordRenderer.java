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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import org.akop.ararat.core.Crossword;


public class CrosswordRenderer
{
	private static final int CELL_STROKE_COLOR = Color.parseColor("#000000");
	private static final int CELL_FILL_COLOR = Color.parseColor("#ffffff");
	private static final int PUZZLE_BG_COLOR = Color.parseColor("#000000");

	private static final float CELL_STROKE_WIDTH = 1;

	private Paint mCellStrokePaint;
	private Paint mCellFillPaint;
	private Paint mPuzzleBackgroundPaint;
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
	}

	public void renderCrossword(Canvas canvas, Crossword crossword,
			int wantedBmpWidth, int wantedBmpHeight)
	{
		int cwWidth = crossword.getWidth();
		int cwHeight = crossword.getHeight();
		int longestCwDim = Math.max(cwWidth, cwHeight);

		float shortestWantedDim = Math.min(wantedBmpWidth, wantedBmpHeight);
		float cellDim = (shortestWantedDim / longestCwDim)
				- (mScaledCellStrokeWidth / longestCwDim);

		float renderedWidth = cellDim * (float) cwWidth;
		float renderedHeight = cellDim * (float) cwHeight;

		Crossword.Cell[][] cellMap = crossword.getCellMap();

		RectF puzzleRect = new RectF(0, 0, wantedBmpWidth, wantedBmpHeight);
		canvas.drawRect(puzzleRect, mPuzzleBackgroundPaint);

		RectF cellRect = new RectF();
		float leftmost = ((float) wantedBmpWidth - renderedWidth) / 2f;
		float rectTop = ((float) wantedBmpHeight - renderedHeight) / 2f;
		for (int i = 0; i < cwHeight; i++, rectTop += cellDim) {
			float rectLeft = leftmost;
			for (int j = 0; j < cwWidth; j++, rectLeft += cellDim) {
				if (cellMap[i][j] != null) {
					cellRect.set(rectLeft, rectTop,
							rectLeft + cellDim, rectTop + cellDim);
					canvas.drawRect(cellRect, mCellFillPaint);
					canvas.drawRect(cellRect, mCellStrokePaint);
				}
			}
		}
	}
}
