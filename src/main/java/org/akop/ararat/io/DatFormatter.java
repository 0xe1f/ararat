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

package org.akop.ararat.io;

import android.text.TextUtils;

import org.akop.ararat.core.Crossword;
import org.akop.ararat.util.SparseArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Pattern;

public class DatFormatter
		implements CrosswordFormatter
{
	private static final Pattern PIPE_SPLITTER = Pattern.compile("\\|");

	private static final int LINE_DIMENSIONS  = 1;
	private static final int LINE_MAP         = 2;
	private static final int LINE_HINTS       = 3;
	private static final int LINE_DESCRIPTION = 4;

	private static final char CHAR_EMPTY = '+';

	private static class Hint
	{
		String mHint;
		int mNumber;
		int mStartRow;
		int mStartCol;

		public Hint(int number, String hint)
		{
			mNumber = number;
			mHint = hint;
		}
	}

	@Override
	public void setEncoding(String encoding)
	{
		// Stub
	}

	@Override
	public void read(Crossword.Builder builder, InputStream inputStream)
			throws IOException
	{
		InputStreamReader reader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(reader);

		int lineNo = 0;
		int width = 0;
		int height = 0;
		char[][] map = null;
		String line;
		SparseArray<Hint> hintsAcross = new SparseArray<>();
		SparseArray<Hint> hintsDown = new SparseArray<>();
		String title = null;
		String author = null;

		while ((line = bufferedReader.readLine()) != null) {
			lineNo++;
			if (lineNo == LINE_DIMENSIONS) {
				String[] dims = line.split("\\|");
				if (dims.length != 2) {
					throw new FormatException("Dimension format unrecognized");
				}
				width = Integer.parseInt(dims[0]);
				height = Integer.parseInt(dims[1]);

				if (width < 1) {
					throw new IndexOutOfBoundsException("Width " + width + " is not valid");
				} else if (height < 1) {
					throw new IndexOutOfBoundsException("Height " + height + " is not valid");
				}

			} else if (lineNo == LINE_MAP) {
				map = new char[height][width];
				for (int i = 0, n = line.length(); i < n; i++) {
					int row = i / width;
					if (row > height) {
						throw new IndexOutOfBoundsException("Row exceeds height");
					}
					int column = i % width;
					char ch = line.charAt(i);

					if (ch != CHAR_EMPTY) {
						map[row][column] = ch;
					}
				}
			} else if (lineNo == LINE_HINTS) {
				String[] parts = PIPE_SPLITTER.split(line, -1);
				if (parts.length % 3 != 1) { // superfluous pipe at the end
					throw new FormatException("Invalid length for hints: " + parts.length);
				}

				for (int i = 0; i + 2 < parts.length; i += 3) {
					int number = Integer.parseInt(parts[i]);
					String hintAcross = parts[i + 1];
					if (!TextUtils.isEmpty(hintAcross)) {
						hintsAcross.put(number, new Hint(number, hintAcross));
					}
					String hintDown = parts[i + 2];
					if (!TextUtils.isEmpty(hintDown)) {
						hintsDown.put(number, new Hint(number, hintDown));
					}
				}
			} else if (lineNo == LINE_DESCRIPTION) {
				String[] parts = line.split("\\|");
				if (parts.length != 2) {
					throw new FormatException("Unexpected description count");
				}

				title = parts[0];
				author = parts[1];
			} else {
				break;
			}
		}

		builder.setWidth(width)
				.setHeight(height)
				.setTitle(title)
				.setAuthor(author);

		buildHints(map, hintsAcross, hintsDown);
		buildWords(builder, map, hintsAcross, hintsDown);
	}

	@Override
	public void write(Crossword crossword, OutputStream outputStream)
			throws IOException
	{
		throw new UnsupportedOperationException("Writing not supported");
	}

	@Override
	public boolean canRead()
	{
		return true;
	}

	@Override
	public boolean canWrite()
	{
		return false;
	}

	private static void buildHints(char[][] map,
			SparseArray<Hint> hintsAcross, SparseArray<Hint> hintsDown)
	{
		int height = map.length;
		if (height == 0) {
			throw new IndexOutOfBoundsException("Height of map is not valid");
		}
		int width = map[0].length;
		if (width == 0) {
			throw new IndexOutOfBoundsException("Width of map is not valid");
		}

		// Build a numbering map
		int counter = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (map[i][j] != '\0') {
					boolean incremented = false;
					if ((j == 0 || map[i][j - 1] == '\0')
							&& (j + 1 < width && map[i][j + 1] != '\0')) {
						counter++;
						incremented = true;

						Hint hint = hintsAcross.get(counter);
						if (hint == null) {
							throw new IndexOutOfBoundsException("Hint " + counter + " Across not found");
						}

						hint.mStartRow = i;
						hint.mStartCol = j;
					}

					if ((i == 0 || map[i - 1][j] == '\0')
							&& (i + 1 < height && map[i + 1][j] != '\0')) {
						if (!incremented) {
							counter++;
						}

						Hint hint = hintsDown.get(counter);
						if (hint == null) {
							throw new IndexOutOfBoundsException("Hint " + counter + " Down not found");
						}

						hint.mStartRow = i;
						hint.mStartCol = j;
					}
				}
			}
		}
	}

	private static void buildWords(Crossword.Builder cb, char[][] map,
			SparseArray<Hint> hintsAcross, SparseArray<Hint> hintsDown)
	{
		int height = map.length;
		if (height == 0) {
			throw new IndexOutOfBoundsException("Height of map is not valid");
		}
		int width = map[0].length;
		if (width == 0) {
			throw new IndexOutOfBoundsException("Width of map is not valid");
		}

		// Build the words
		for (int h = 0, n = hintsAcross.size(); h < n; h++) {
			Hint hint = hintsAcross.valueAt(h);
			Crossword.Word.Builder wb = new Crossword.Word.Builder()
					.setNumber(hint.mNumber)
					.setHint(hint.mHint)
					.setDirection(Crossword.Word.DIR_ACROSS)
					.setStartRow(hint.mStartRow)
					.setStartColumn(hint.mStartCol);

			for (int j = hint.mStartCol; j < width && map[hint.mStartRow][j] != '\0'; j++) {
				wb.addCell(map[hint.mStartRow][j]);
			}

			cb.addWord(wb.build());
		}

		for (int h = 0, n = hintsDown.size(); h < n; h++) {
			Hint hint = hintsDown.valueAt(h);
			Crossword.Word.Builder wb = new Crossword.Word.Builder()
					.setNumber(hint.mNumber)
					.setHint(hint.mHint)
					.setDirection(Crossword.Word.DIR_DOWN)
					.setStartRow(hint.mStartRow)
					.setStartColumn(hint.mStartCol);

			for (int i = hint.mStartRow; i < height && map[i][hint.mStartCol] != '\0'; i++) {
				wb.addCell(map[i][hint.mStartCol]);
			}

			cb.addWord(wb.build());
		}
	}
}
