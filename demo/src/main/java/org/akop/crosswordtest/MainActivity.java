// Copyright (c) 2016 Akop Karapetyan
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

package org.akop.crosswordtest;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.RawRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.akop.ararat.core.Crossword;
import org.akop.ararat.core.CrosswordState;
import org.akop.ararat.io.PuzFormatter;
import org.akop.ararat.view.CrosswordView;

import java.io.IOException;
import java.io.InputStream;


// Crossword: Double-A's by Ben Tausig
// http://www.inkwellxwords.com/iwxpuzzles.html
public class MainActivity
		extends AppCompatActivity
		implements CrosswordView.OnLongPressListener, CrosswordView.OnStateChangeListener, CrosswordView.OnSelectionChangeListener
{
	private CrosswordView mCrosswordView;
	private TextView mHint;

	private boolean mSolvedShown;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		mCrosswordView = (CrosswordView) findViewById(R.id.crossword);
		mHint = (TextView) findViewById(R.id.hint);

		Crossword crossword = readPuzzle(this, R.raw.puzzle);

		setTitle(getString(R.string.title_by_author,
				crossword.getTitle(), crossword.getAuthor()));

		mCrosswordView.setCrossword(crossword);
		mCrosswordView.setOnLongPressListener(this);
		mCrosswordView.setOnStateChangeListener(this);
		mCrosswordView.setOnSelectionChangeListener(this);

		mCrosswordView.setUndoMode(CrosswordView.UNDO_NONE);
		mCrosswordView.setMarkerDisplayMode(CrosswordView.MARKER_CHEAT);

		onSelectionChanged(mCrosswordView,
				mCrosswordView.getSelectedWord(), mCrosswordView.getSelectedCell());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		mCrosswordView.restoreState((CrosswordState) savedInstanceState.getParcelable("state"));
		mSolvedShown = savedInstanceState.getBoolean("solved");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putParcelable("state", mCrosswordView.getState());
		outState.putBoolean("solved", mSolvedShown);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
		case R.id.menu_restart:
			mCrosswordView.reset();
			return true;
		case R.id.menu_solve_cell:
			mCrosswordView.solveChar(mCrosswordView.getSelectedWord(),
					mCrosswordView.getSelectedCell());
			return true;
		case R.id.menu_solve_word:
			mCrosswordView.solveWord(mCrosswordView.getSelectedWord());
			return true;
		case R.id.menu_solve_puzzle:
			mCrosswordView.solveCrossword();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCellLongPressed(CrosswordView view,
			Crossword.Word word, int cell)
	{
		Toast.makeText(this, "Show popup menu for " + word.getHint(),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCrosswordChanged(CrosswordView view)
	{
	}

	@Override
	public void onCrosswordSolved(CrosswordView view)
	{
		if (mSolvedShown) {
			return;
		}

		mSolvedShown = true;
		Toast.makeText(this, "You've solved the puzzle!",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCrosswordUnsolved(CrosswordView view)
	{
		mSolvedShown = false;
	}

	private static Crossword readPuzzle(Context context, @RawRes int resourceId)
	{
		Resources res = context.getResources();
		InputStream resStream = res.openRawResource(resourceId);

		Crossword.Builder cb = new Crossword.Builder();
		PuzFormatter formatter = new PuzFormatter();

		try {
			formatter.read(cb, resStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try { resStream.close(); }
			catch (IOException e) { /* */ }
		}

		return cb.build();
	}

	@Override
	public void onSelectionChanged(CrosswordView view, Crossword.Word word, int position)
	{
		String description = null;
		if (word != null) {
			description = word.getNumber() + "";
			if (word.getDirection() == Crossword.Word.DIR_ACROSS) {
				description += " " + getString(R.string.across);
			} else if (word.getDirection() == Crossword.Word.DIR_DOWN) {
				description += " " + getString(R.string.down);
			}
			description += " • " + word.getHint();
		}

		mHint.setText(description);
	}
}
