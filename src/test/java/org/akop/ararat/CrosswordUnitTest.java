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

package org.akop.ararat;

import android.text.format.DateUtils;

import org.akop.ararat.core.Crossword;
import org.akop.ararat.core.CrosswordReader;
import org.akop.ararat.core.CrosswordWriter;
import org.akop.ararat.io.CrosswordFormatter;
import org.akop.ararat.io.PuzFormatter;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;


public class CrosswordUnitTest
{
	@Test
	public void crossword_testReadWrite()
			throws Exception
	{
		DateFormat dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
		String urlFormat = "http://herbach.dnsalias.com/Jonesin/jz%s.puz";

		long millis = System.currentTimeMillis();
		Crossword crossword = null;

		for (int i = 0; i < 30 && crossword == null; i++, millis -= DateUtils.DAY_IN_MILLIS) {
			crossword = tryDownload(String.format(urlFormat, dateFormat.format(millis)));
		}

		assertNotNull("No crosswords to test", crossword);

		Crossword clone = clone(crossword);

		assertEquals(crossword.getWidth(), clone.getWidth());
		assertEquals(crossword.getHeight(), clone.getHeight());
		assertEquals(crossword.getSquareCount(), clone.getSquareCount());
		assertEquals(crossword.getTitle(), clone.getTitle());
		assertEquals(crossword.getDescription(), clone.getDescription());
		assertEquals(crossword.getAuthor(), clone.getAuthor());
		assertEquals(crossword.getCopyright(), clone.getCopyright());
		assertEquals(crossword.getComment(), clone.getComment());
		assertEquals(crossword.getDate(), clone.getDate());
		assertEquals(crossword.getHash(), clone.getHash());
	}

	@Test
	public void crossword_testBuilder()
			throws Exception
	{
		int width = 13;
		int height = 13;
		String title = "Test title";
		String description = "Test description";
		String author = "Test author";
		String copyright = "Test copyright";
		String alphabet = "ABCDEFGHIJ";
		String comment = "Test comment";
		long date = System.currentTimeMillis();

		W[] across = new W[] {
				new W(1, "Be a couch potato", 0, 0, null, null, "VEG"),
				new W(4, "Brit's raincoat", 0, 5, null, null, "MAC"),
				new W(7, "Blue hue", 0, 9, null, null, "AQUA"),
				new W(11, "Astringent", 1, 0, null, null, "ALUM"),
				new W(13, "All The Things You -", 1, 5, null, null, "ARE"),
				new W(14, "Shoe style", 1, 9, null, null, "PUMP"),
				new W(15, "O'Hara estate", 2, 0, null, null, "TARA"),
				new W(16, "Tin Man's need", 2, 5, null, null, "OIL"),
				new W(17, "Harvest", 2, 9, null, null, "REAP"),
				new W(18, "Soup eater's noise", 3, 0, null, null, "SLURP"),
				new W(20, "Jai —", 3, 6, null, null, "ALAI"),
				new W(22, "Urban carrier", 4, 3, null, null, "CAB"),
				new W(24, "Lecher's activity", 4, 7, null, null, "OGLING"),
				new W(28, "Bright purplish-red", 5, 0, null, null, "FUCHSIA"),
				new W(32, "Tinseltown trophy", 5, 8, null, null, "OSCAR"),
				new W(33, "Physics bit", 6, 0, null, null, "ATOM"),
				new W(34, "On in years", 6, 5, null, null, "OLD"),
				new W(36, "It springs eternal", 6, 9, null, null, "HOPE"),
				new W(37, "Re ocean motion", 7, 0, null, null, "TIDAL"),
				new W(39, "Hogwash!", 7, 6, null, null, "BALONEY"),
				new W(41, "Foxy fellow", 8, 0, null, null, "SLYDOG"),
				new W(43, "Trim the grass", 8, 7, null, null, "MOW"),
				new W(44, "Part of speech", 9, 3, null, null, "NOUN"),
				new W(46, "TV-tube gas", 9, 8, null, null, "XENON"),
				new W(50, "Green gem", 10, 0, null, null, "JADE"),
				new W(53, "Witty comment", 10, 5, null, null, "MOT"),
				new W(55, "Kelly of morning TV", 10, 9, null, null, "RIPA"),
				new W(56, "Rhyming tributes", 11, 0, null, null, "ODES"),
				new W(57, "It's c-c-cold!", 11, 5, null, null, "BRR"),
				new W(58, "Comfy-cozy", 11, 9, null, null, "SNUG"),
				new W(59, "Evergreens", 12, 0, null, null, "YEWS"),
				new W(60, "Yippee!", 12, 5, null, null, "YAY"),
				new W(61, "Pompous sort", 12, 10, null, null, "ASS"),
		};
		W[] down = new W[] {
				new W(1, "Cisterns", 0, 0, null, null, "VATS"),
				new W(2, "Israeli airline", 0, 1, null, null, "ELAL"),
				new W(3, "Expert", 0, 2, null, null, "GURU"),
				new W(4, "Chinese chairman", 0, 5, null, null, "MAO"),
				new W(5, "Met melody", 0, 6, null, null, "ARIA"),
				new W(6, "Yo-Yo Ma's instrument", 0, 7, null, null, "CELLO"),
				new W(7, "Spring rains", 0, 9, null, null, "APRILSHOWERS"),
				new W(8, "— Sera, Sera", 0, 10, null, null, "QUE"),
				new W(9, "Actress Thurman", 0, 11, null, null, "UMA"),
				new W(10, "PC program", 0, 12, null, null, "APP"),
				new W(12, "Annual NCAA basketball tournament", 1, 3, null, null, "MARCHMADNESS"),
				new W(19, "Faux —", 3, 4, null, null, "PAS"),
				new W(21, "Past", 3, 8, null, null, "AGO"),
				new W(23, "Story of a lifetime?", 4, 5, null, null, "BIO"),
				new W(25, "PC picture", 4, 10, null, null, "ICON"),
				new W(26, "Scruff", 4, 11, null, null, "NAPE"),
				new W(27, "Joel of \"Cabaret\"", 4, 12, null, null, "GREY"),
				new W(28, "Waller or Domino", 5, 0, null, null, "FATS"),
				new W(29, "Gas co., for one", 5, 1, null, null, "UTIL"),
				new W(30, "Buffalo Bill —", 5, 2, null, null, "CODY"),
				new W(31, "Priest's garment", 5, 6, null, null, "ALB"),
				new W(35, "Weir", 6, 7, null, null, "DAM"),
				new W(38, "Privy", 7, 4, null, null, "LOO"),
				new W(40, "Bagel topper", 7, 8, null, null, "LOX"),
				new W(42, "Flexible green toy", 8, 5, null, null, "GUMBY"),
				new W(45, "\"A Doll's House\" heroine", 9, 6, null, null, "NORA"),
				new W(47, "1492 vessel", 9, 10, null, null, "NINA"),
				new W(48, "Piece of work", 9, 11, null, null, "OPUS"),
				new W(49, "Henpecks", 9, 12, null, null, "NAGS"),
				new W(50, "Bliss", 10, 0, null, null, "JOY"),
				new W(51, "Citric quaff", 10, 1, null, null, "ADE"),
				new W(52, "Drops on the lawn", 10, 2, null, null, "DEW"),
				new W(54, "Stab", 10, 7, null, null, "TRY"),
		};

		Crossword.Builder crosswordBuilder = new Crossword.Builder()
				.setWidth(width)
				.setHeight(height)
				.setTitle(title)
				.setDescription(description)
				.setAuthor(author)
				.setCopyright(copyright)
				.setComment(comment)
				.setAlphabet(alphabet.toCharArray())
				.setDate(date);

		addWords(crosswordBuilder, Crossword.Word.DIR_ACROSS, across);
		addWords(crosswordBuilder, Crossword.Word.DIR_DOWN, down);

		Crossword crossword = crosswordBuilder.build();

		// Check basic crossword data
		assertEquals(crossword.getWidth(), width);
		assertEquals(crossword.getHeight(), height);
		assertEquals(crossword.getTitle(), title);
		assertEquals(crossword.getDescription(), description);
		assertEquals(crossword.getAuthor(), author);
		assertEquals(crossword.getCopyright(), copyright);
		assertEquals(new String(crossword.getAlphabet()), alphabet);
		assertEquals(crossword.getDate(), date);

		List<Crossword.Word> wordsAcross = crossword.getWordsAcross();
		List<Crossword.Word> wordsDown = crossword.getWordsDown();

		// Verify the size of the word/hint arrays
		assertEquals(wordsAcross.size(), across.length);
		assertEquals(wordsDown.size(), down.length);

		// Compare words reported by the crossword against what we wanted
		assertWords(crossword, Crossword.Word.DIR_ACROSS, across);
		assertWords(crossword, Crossword.Word.DIR_DOWN, down);

		// Verify that word contents match the cell map
		Crossword.Cell[][] map = crossword.getCellMap();
		for (Crossword.Word word: wordsAcross) {
			int r = word.getStartRow();
			for (int j = 0, c = word.getStartColumn(), n = word.getLength(); j < n; j++, c++) {
				if (word.cellAt(j) == null) {
					assertNull(map[r][c]);
				} else if (map[r][c] == null) {
					assertNull(word.cellAt(j));
				} else {
					assertEquals(word.cellAt(j).chars(), map[r][c].chars());
				}
			}
		}
		for (Crossword.Word word: wordsDown) {
			int c = word.getStartColumn();
			for (int i = 0, r = word.getStartRow(), n = word.getLength(); i < n; i++, r++) {
				if (word.cellAt(i) == null) {
					assertNull(map[r][c]);
				} else if (map[r][c] == null) {
					assertNull(word.cellAt(i));
				} else {
					assertEquals(word.cellAt(i).chars(), map[r][c].chars());
				}
			}
		}
	}

	private static void assertWords(Crossword crossword,
			int direction, W[] ws)
	{
		for (W w: ws) {
			Crossword.Word word = crossword.findWord(direction, w.mNumber);

			assertNotNull(word);

			// Check basic information
			assertEquals(word.getHint(), w.mHint);
			assertEquals(word.getHintUrl(), w.mHintUrl);
			assertEquals(word.getCitation(), w.mCite);
			assertEquals(word.getLength(), w.mChars.length());
			assertEquals(word.getDirection(), direction);
			assertEquals(word.getStartRow(), w.mStartRow);
			assertEquals(word.getStartColumn(), w.mStartCol);

			// Check the answer
			String answer = "";
			for (int i = 0, n = word.getLength(); i < n; i++) {
				answer += word.cellAt(i).chars();
			}
			assertEquals(answer, w.mChars);
		}
	}

	private static void addWords(Crossword.Builder crosswordBuilder,
			int direction, W[] ws)
	{
		for (W w: ws) {
			Crossword.Word.Builder wb = new Crossword.Word.Builder()
					.setStartColumn(w.mStartCol)
					.setStartRow(w.mStartRow)
					.setDirection(direction)
					.setHint(w.mHint)
					.setCitation(w.mCite)
					.setHintUrl(w.mHintUrl)
					.setNumber(w.mNumber);

			for (int i = 0, n = w.mChars.length(); i < n; i++) {
				wb.addCell(w.mChars.charAt(i), 0);
			}

			crosswordBuilder.addWord(wb.build());
		}
	}

	private static Crossword clone(Crossword crossword)
			throws IOException
	{
		Crossword clone;

		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		CrosswordWriter writer = new CrosswordWriter(ostream);
		writer.write(crossword);
		writer.close();

		ByteArrayInputStream istream = new ByteArrayInputStream(ostream.toByteArray());
		CrosswordReader reader = new CrosswordReader(istream);
		clone = reader.read();
		reader.close();

		return clone;
	}

	private static InputStream tryGetInputStreamConnect(String urlString)
			throws IOException
	{
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();

		return connection.getInputStream();
	}

	private static Crossword tryDownload(String urlString)
			throws IOException
	{
		InputStream inputStream;
		try {
			inputStream = tryGetInputStreamConnect(urlString);
		} catch (FileNotFoundException e) {
			return null;
		}

		Crossword.Builder cb = new Crossword.Builder();
		CrosswordFormatter formatter = new PuzFormatter();

		try {
			formatter.read(cb, inputStream);
		} finally {
			try { inputStream.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}

		return cb.build();
	}

	private static class W
	{
		int mNumber;
		String mHint;
		int mStartRow;
		int mStartCol;
		String mHintUrl;
		String mCite;
		String mChars;

		W(int number, String hint, int startRow, int startCol,
				String hintUrl, String cite, String chars)
		{
			mNumber = number;
			mHint = hint;
			mStartRow = startRow;
			mStartCol = startCol;
			mHintUrl = hintUrl;
			mCite = cite;
			mChars = chars;
		}
	}
}