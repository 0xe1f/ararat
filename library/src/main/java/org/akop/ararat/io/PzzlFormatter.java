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

import org.akop.ararat.core.Crossword;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PzzlFormatter
		implements CrosswordFormatter
{
	private static final String DEFAULT_ENCODING = "Windows-1252";

	// Section offsets, 0-based
	private static final int SECTION_TITLE  = 2;
	private static final int SECTION_AUTHOR = 3;
	private static final int SECTION_WIDTH  = 4;
	private static final int SECTION_HEIGHT = 5;
	private static final int SECTION_MAP    = 8;
	private static final int SECTION_ACROSS = 9;
	private static final int SECTION_DOWN   = 10;

	private String mEncoding = DEFAULT_ENCODING;

	@Override
	public void setEncoding(String encoding)
	{
		mEncoding = encoding;
	}

	@Override
	public void read(Crossword.Builder builder, InputStream inputStream)
			throws IOException
	{
		InputStreamReader inputReader = new InputStreamReader(inputStream, mEncoding);
		BufferedReader reader = new BufferedReader(inputReader);

		Cell[][] cellMap = null;
		int width;
		int height = -1;
		int row = 0;
		List<String> hintsAcross = new ArrayList<>();
		List<String> hintsDown = new ArrayList<>();

		int section = 0;
		for (; ; ) {
			String line;
			if ((line = reader.readLine()) == null) {
				break;
			}

			if (line.length() == 0) {
				section++;
				continue;
			}

			switch (section) {
			case SECTION_TITLE:
				builder.setTitle(line);
				break;
			case SECTION_AUTHOR:
				builder.setAuthor(line);
				break;
			case SECTION_WIDTH:
				// Not actual width
				break;
			case SECTION_HEIGHT:
				// Not actual height
				height = Integer.parseInt(line);
				break;
			case SECTION_MAP:
				char[] lineChars = line.toCharArray();

				if (height < 1) {
					throw new IndexOutOfBoundsException("Height is invalid - assuming missing puzzle");
				}

				if (row == 0) {
					width = 0;
					for (int i = 0; i < lineChars.length; i++) {
						char ch = lineChars[i];
						if (ch == ',') {
							for (; i < lineChars.length && lineChars[i] == ','; i += 2) {
								ch = lineChars[i];
							}
						}

						if (ch != '.' && ch != '%') {
							width++;
						}
					}

					if (width == 0) {
						continue;
					}

					builder.setWidth(width);
					cellMap = new Cell[height][width];
				}

				Cell cell = null;
				for (int i = 0, p = 0, n = lineChars.length - 1; i <= n; i++) {
					char ch = line.charAt(i);
					if (ch == '.') {
						continue;
					} else if (ch != '#') {
						if (ch == '%') {
							cell = new Cell();
							cell.mAttrs |= Crossword.Cell.ATTR_CIRCLED;
							continue;
						} else {
							if (cell == null) {
								cell = new Cell();
							}

							// Determine the count of chars for cell
							int charCount = 1;
							for (int j = i + 1;
								 j <= n && lineChars[j] == ',';
								 j += 2) {
								charCount++;
							}

							// Copy the chars
							char[] chars = new char[charCount];
							for (int j = i, k = 0; k < charCount; j += 2, k++) {
								chars[k] = lineChars[j];
							}
							cell.mChars = new String(chars);

							// Advance the index
							i += (charCount - 1) * 2;
						}
					}

					cellMap[row][p++] = cell;
					cell = null;
				}

				row++;
				break;
			case SECTION_ACROSS:
				hintsAcross.add(line);
				break;
			case SECTION_DOWN:
				hintsDown.add(line);
				break;
			}
		}

		// Complete word information given the 2D map
		mapOutWords(builder, hintsAcross, hintsDown, cellMap);
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

	private void mapOutWords(Crossword.Builder cb,
			List<String> hintsAcross, List<String> hintsDown, Cell[][] cellMap)
	{
		int acrossIndex = 0;
		int downIndex = 0;
		int number = 0;
		int actualHeight = 0;

		for (int i = 0, m = cellMap.length - 1; i <= m; i++) {
			boolean allEmpty = true;
			for (int j = 0, n = cellMap[i].length - 1; j <= n; j++) {
				if (cellMap[i][j] != null) {
					allEmpty = false;
					boolean incremented = false;
					if ((j == 0 || (j > 0 && cellMap[i][j - 1] == null))
							&& (j < n && cellMap[i][j + 1] != null)) {
						// Start of a new Across word
						number++;
						incremented = true;

						Crossword.Word.Builder wb = new Crossword.Word.Builder()
								.setDirection(Crossword.Word.DIR_ACROSS)
								.setHint(hintsAcross.get(acrossIndex++))
								.setNumber(number)
								.setStartRow(i)
								.setStartColumn(j);

						// Copy contents to a temp buffer
						for (int k = j; k < cellMap[i].length
								&& cellMap[i][k] != null; k++) {
							Cell cell = cellMap[i][k];
							wb.addCell(cell.mChars, cell.mAttrs);
						}

						cb.addWord(wb.build());
					}

					if (i == 0 || (i > 0 && cellMap[i - 1][j] == null)
							&& (i < m && cellMap[i + 1][j] != null)) {
						// Start of a new Down word
						if (!incremented) {
							number++;
						}

						Crossword.Word.Builder wb = new Crossword.Word.Builder()
								.setDirection(Crossword.Word.DIR_DOWN)
								.setHint(hintsDown.get(downIndex++))
								.setNumber(number)
								.setStartRow(i)
								.setStartColumn(j);

						for (int k = i; k < cellMap.length
								&& cellMap[k][j] != null; k++) {
							Cell cell = cellMap[k][j];
							wb.addCell(cell.mChars, cell.mAttrs);
						}

						cb.addWord(wb.build());
					}
				}
			}

			if (!allEmpty) {
				actualHeight++;
			}
		}

		cb.setHeight(actualHeight);
	}

	@SuppressWarnings("unused")
	private static void dumpMap(Cell[][] cellMap)
	{
		for (Cell[] row: cellMap) {
			String rowDump = "";
			for (Cell cell: row) {
				if (cell != null) {
					rowDump += "[" + cell.mChars + "]";
				} else {
					rowDump += "   ";
				}
			}
			System.out.println(rowDump);
		}
	}

	private static class Cell
	{
		String mChars;
		int mAttrs;
	}
}
