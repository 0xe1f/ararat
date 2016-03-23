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
import org.akop.ararat.util.SparseArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class CrosswordCompilerFormatter
		extends SimpleXmlParser
		implements CrosswordFormatter
{
	private Crossword.Builder mBuilder;
	private SparseArray<Crossword.Word.Builder> mWordBuilders;
	private char[][] mChars;
	private int[][] mAttrs;
	private Crossword.Word.Builder mCurrentWordBuilder;

	public CrosswordCompilerFormatter()
	{
		mWordBuilders = new SparseArray<>();
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
		// Initialize state
		mWordBuilders.clear();
		mChars = null;
		mAttrs = null;
		mCurrentWordBuilder = null;
		mBuilder = builder;

		try {
			parseXml(inputStream);
		} catch (XmlPullParserException e) {
			throw new RuntimeException("Malformed XML", e);
		}

		for (int i = 0, n = mWordBuilders.size(); i < n; i++) {
			mBuilder.addWord(mWordBuilders.valueAt(i).build());
		}
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

	@Override
	protected void onStartElement(SimpleXmlPath path, XmlPullParser parser)
	{
		super.onStartElement(path, parser);

		if (path.startsWith("?", "rectangular-puzzle")) {
			if (path.startsWith("crossword")) {
				if (path.startsWith("grid")) {
					if (path.isEqualTo("cell")) {
						// ?/rectangular-puzzle/crossword/grid/cell
						String sol = parser.getAttributeValue(null, "solution");
						if (sol != null) {
							int row = getIntValue(parser, "y", 0) - 1;
							int column = getIntValue(parser, "x", 0) - 1;

							mChars[row][column] = sol.charAt(0);
							String shape = parser.getAttributeValue(null, "background-shape");
							if (shape != null) {
								mAttrs[row][column] |= Crossword.Cell.ATTR_CIRCLED;
							}
						}
					} else if (path.isDeadEnd()) {
						// ?/rectangular-puzzle/crossword/grid
						int width = getIntValue(parser, "width", -1);
						int height = getIntValue(parser, "height", -1);

						mChars = new char[height][width];
						mAttrs = new int[height][width];
						mBuilder.setWidth(width)
								.setHeight(height);
					}
				} else if (path.isEqualTo("word")) {
					// ?/rectangular-puzzle/crossword/word
					int id = getIntValue(parser, "id", -1);
					String xSpan = parser.getAttributeValue(null, "x");
					String ySpan = parser.getAttributeValue(null, "y");
					int xDashIndex = xSpan.indexOf("-");

					Crossword.Word.Builder wb = new Crossword.Word.Builder();
					if (xDashIndex != -1) {
						int startColumn = Integer.parseInt(xSpan.substring(0, xDashIndex)) - 1;
						int endColumn = Integer.parseInt(xSpan.substring(xDashIndex + 1)) - 1;
						int row = Integer.parseInt(ySpan) - 1;

						// Across
						wb.setDirection(Crossword.Word.DIR_ACROSS)
								.setStartRow(row)
								.setStartColumn(startColumn);

						// Build the individual characters from the char map
						for (int column = startColumn; column <= endColumn; column++) {
							wb.addCell(new char[] { mChars[row][column] },
									mAttrs[row][column]);
						}
					} else {
						int yDashIndex = ySpan.indexOf("-");
						int startRow = Integer.parseInt(ySpan.substring(0, yDashIndex)) - 1;
						int endRow = Integer.parseInt(ySpan.substring(yDashIndex + 1)) - 1;
						int column = Integer.parseInt(xSpan) - 1;

						// Down
						wb.setDirection(Crossword.Word.DIR_DOWN)
								.setStartRow(startRow)
								.setStartColumn(column);

						// Build the individual characters from the char map
						for (int row = startRow; row <= endRow; row++) {
							wb.addCell(new char[] { mChars[row][column] },
									mAttrs[row][column]);
						}
					}

					mWordBuilders.put(id, wb);
				} else if (path.isEqualTo("clues", "clue")) {
					// ?/rectangular-puzzle/crossword/clues/clue
					int wordId = getIntValue(parser, "word", -1);
					int number = getIntValue(parser, "number", -1);

					mCurrentWordBuilder = mWordBuilders.get(wordId);
					mCurrentWordBuilder.setNumber(number);
					mCurrentWordBuilder.setHintUrl(parser.getAttributeValue(null,
							"hint-url"));
					mCurrentWordBuilder.setCitation(parser.getAttributeValue(null,
							"citation"));
				}
			} else if (path.isDeadEnd()) {
				// ?/rectangular-puzzle
				char[] allowedChars = null;
				String alphabet = parser.getAttributeValue(null, "alphabet");
				if (alphabet != null) {
					allowedChars = alphabet.toCharArray();
				}

				mBuilder.setAlphabet(allowedChars);
			}
		}
	}

	@Override
	protected void onTextContent(SimpleXmlPath path, String text)
	{
		super.onTextContent(path, text);

		if (path.startsWith("?", "rectangular-puzzle")) {
			if (path.startsWith("metadata")) {
				if (path.isEqualTo("title")) {
					// ?/rectangular-puzzle/metadata/title
					mBuilder.setTitle(text);
				} else if (path.isEqualTo("creator")) {
					// ?/rectangular-puzzle/metadata/creator
					mBuilder.setAuthor(text);
				} else if (path.isEqualTo("copyright")) {
					// ?/rectangular-puzzle/metadata/copyright
					mBuilder.setCopyright(text);
				} else if (path.isEqualTo("description")) {
					// ?/rectangular-puzzle/metadata/description
					mBuilder.setDescription(text);
				}
			} else if (path.isEqualTo("crossword", "clues", "clue")) {
				// ?/rectangular-puzzle/crossword/clues/clue
				mCurrentWordBuilder.setHint(text);
			}
		}
	}
}
