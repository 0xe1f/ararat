// Copyright (c) 2017 Akop Karapetyan
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class WSJFormatter
		implements CrosswordFormatter
{
	private static final String LOG_TAG = WSJFormatter.class.getSimpleName();

	private static final String DEFAULT_ENCODING = "UTF-8";
	private static DateFormat PUBLISH_DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM yyyy",
			Locale.US);

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
		InputStreamReader reader = new InputStreamReader(inputStream, mEncoding);

		StringBuilder sb = new StringBuilder();
		int nread;
		char[] buffer = new char[4000];
		while ((nread = reader.read(buffer, 0, buffer.length)) > -1) {
			sb.append(buffer, 0, nread);
		}

		JSONObject obj;
		try {
			obj = new JSONObject(sb.toString());
		} catch (JSONException e) {
			throw new FormatException("Error parsing JSON object", e);
		}

		JSONObject dataObj = obj.optJSONObject("data");
		if (dataObj == null) {
			throw new FormatException("Missing 'data'");
		}

		JSONObject copyObj = dataObj.optJSONObject("copy");
		if (copyObj == null) {
			throw new FormatException("Missing 'data.copy'");
		}

		JSONObject gridObj = copyObj.optJSONObject("gridsize");
		if (gridObj == null) {
			throw new FormatException("Missing 'data.copy.gridsize'");
		}

		builder.setTitle(copyObj.optString("title"));
		builder.setDescription(copyObj.optString("description"));
		builder.setCopyright(copyObj.optString("publisher"));
		builder.setAuthor(copyObj.optString("byline"));

		String pubString = copyObj.optString("date-publish");
		try {
			builder.setDate(PUBLISH_DATE_FORMAT.parse(pubString).getTime());
		} catch (ParseException e) {
			throw new FormatException("Can't parse '" + pubString + "' as publish date");
		}

		int width = gridObj.optInt("cols");
		int height = gridObj.optInt("rows");

		builder.setWidth(width);
		builder.setHeight(height);

		readClues(builder, copyObj,
				Grid.parseJSON(dataObj.optJSONArray("grid"), width, height));
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

	private static void readClues(Crossword.Builder builder, JSONObject copyObj,
			Grid grid)
	{
		JSONArray cluesArray = copyObj.optJSONArray("clues");
		if (cluesArray == null) {
			throw new FormatException("Missing 'data.copy.clues[]'");
		} else if (cluesArray.length() != 2) {
			throw new FormatException("Unexpected clues length of '" + cluesArray.length() + "'");
		}

		JSONArray wordsArray = copyObj.optJSONArray("words");
		if (wordsArray == null) {
			throw new FormatException("Missing 'data.copy.words[]'");
		}

		// We'll need this to assign x/y locations to each clue
		SparseArray<Word> words = new SparseArray<>();
		for (int i = 0, n = wordsArray.length(); i < n; i++) {
			Word word;
			try {
				word = Word.parseJSON(wordsArray.optJSONObject(i));
			} catch (Exception e) {
				throw new FormatException("Error parsing 'data.copy.words[" + i + "]'", e);
			}

			words.put(word.mId, word);
		}

		// Go through the list of clues
		for (int i = 0, n = cluesArray.length(); i < n; i++) {
			JSONObject clueObj = cluesArray.optJSONObject(i);
			if (clueObj == null) {
				throw new FormatException("'data.copy.clues[" + i + "]' is null");
			}

			JSONArray subcluesArray = clueObj.optJSONArray("clues");
			if (subcluesArray == null) {
				throw new FormatException("Missing 'data.copy.clues[" + i + "].clues'");
			}

			int dir;
			String clueDir = clueObj.optString("title");
			if ("Across".equalsIgnoreCase(clueDir)) {
				dir = Crossword.Word.DIR_ACROSS;
			} else if ("Down".equalsIgnoreCase(clueDir)) {
				dir = Crossword.Word.DIR_DOWN;
			} else {
				throw new FormatException("Invalid direction: '" + clueDir + "'");
			}

			for (int j = 0, o = subcluesArray.length(); j < o; j++) {
				JSONObject subclue = subcluesArray.optJSONObject(j);
				Word word = words.get(subclue.optInt("word", -1));
				if (word == null) {
					throw new FormatException("No matching word for clue at 'data.copy.clues[" + i + "].clues[" + j + "].word'");
				}

				Crossword.Word.Builder wb = new Crossword.Word.Builder()
						.setDirection(dir)
						.setHint(subclue.optString("clue"))
						.setNumber(subclue.optInt("number"))
						.setStartColumn(word.mCol)
						.setStartRow(word.mRow);

				if (dir == Crossword.Word.DIR_ACROSS) {
					for (int k = word.mCol, l = 0; l < word.mLen; k++, l++) {
						Square square = grid.mSquares[word.mRow][k];
						if (square == null) {
							throw new FormatException("grid["
									+ word.mRow + "][" + k + "] is null (it shouldn't be)");
						}
						wb.addCell(square.mChar, 0);
					}
				} else {
					for (int k = word.mRow, l = 0; l < word.mLen; k++, l++) {
						Square square = grid.mSquares[k][word.mCol];
						if (square == null) {
							throw new FormatException("grid["
									+ k + "][" + word.mCol + "] is null (it shouldn't be)");
						}
						wb.addCell(square.mChar, 0);
					}
				}

				builder.addWord(wb.build());
			}
		}
	}

	private static class Grid
	{
		int mWidth;
		int mHeight;
		final Square[][] mSquares;

		Grid(int width, int height)
		{
			mWidth = width;
			mHeight = height;
			mSquares = new Square[height][width];
		}

		static Grid parseJSON(JSONArray gridArray, int width, int height)
		{
			if (gridArray == null) {
				throw new FormatException("Missing 'data.grid[]'");
			} else if (gridArray.length() != height) {
				throw new FormatException("Unexpected clues length of "
						+ gridArray.length() + " (expected " + height + ")");
			}

			Grid grid = new Grid(width, height);
			for (int i = 0; i < height; i++) {
				JSONArray rowArray = gridArray.optJSONArray(i);
				if (rowArray == null) {
					throw new FormatException("Missing 'data.grid[" + i + "]'");
				} else if (rowArray.length() != width) {
					throw new FormatException("Unexpected clues length of "
							+ rowArray.length() + " (expected " + width + ")");
				}

				for (int j = 0; j < width; j++) {
					grid.mSquares[i][j] = Square.parseJSON(rowArray.optJSONObject(j));
				}
			}

			return grid;
		}
	}

	private static class Square
	{
		String mChar;

		static Square parseJSON(JSONObject squareObj)
		{
			Square square = null;

			String letter = squareObj.optString("Letter");
			if (letter != null && !letter.isEmpty()) {
				square = new Square();
				square.mChar = letter;
			}

			return square;
		}
	}

	private static class Word
	{
		int mId;
		int mRow;
		int mCol;
		int mLen;

		static Word parseJSON(JSONObject wordObj)
		{
			Word word = new Word();
			word.mId = wordObj.optInt("id", -1);
			if (word.mId == -1) {
				throw new FormatException("Word missing identifier");
			}

			String posStr;
			int dashIdx;

			if ((posStr = wordObj.optString("x")) == null) {
				throw new FormatException("Word missing 'x'");
			}
			if ((dashIdx = posStr.indexOf('-')) != -1) {
				word.mCol = Integer.parseInt(posStr.substring(0, dashIdx)) - 1;
				word.mLen = Integer.parseInt(posStr.substring(dashIdx + 1)) - word.mCol;
			} else {
				word.mCol = Integer.parseInt(posStr) - 1;
			}

			if ((posStr = wordObj.optString("y")) == null) {
				throw new FormatException("Word missing 'y'");
			}
			if ((dashIdx = posStr.indexOf('-')) != -1) {
				word.mRow = Integer.parseInt(posStr.substring(0, dashIdx)) - 1;
				word.mLen = Integer.parseInt(posStr.substring(dashIdx + 1)) - word.mRow;
			} else {
				word.mRow = Integer.parseInt(posStr) - 1;
			}

			return word;
		}
	}
}
