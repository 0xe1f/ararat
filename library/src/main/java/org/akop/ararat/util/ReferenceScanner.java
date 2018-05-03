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

package org.akop.ararat.util;

import org.akop.ararat.core.Crossword;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReferenceScanner
{
	private static final String WORD_ACROSS_EN = "Across";
	private static final String WORD_DOWN_EN = "Down";

	private static final Pattern NUMBER_FINDER = Pattern.compile("\\b(\\d{1,3})\\b");
	private static final Pattern DIRECTION_FINDER_EN = Pattern.compile("((?:"
			+ WORD_ACROSS_EN + ")|(?:" + WORD_DOWN_EN + "))",
			Pattern.CASE_INSENSITIVE);

	public static class WordReference
	{
		private int mNumber;
		private int mDir;
		private int mStart;
		private int mEnd;

		public int getNumber()
		{
			return mNumber;
		}

		public int getDirection()
		{
			return mDir;
		}

		public int getStart()
		{
			return mStart;
		}

		public int getEnd()
		{
			return mEnd;
		}

		@Override
		public String toString()
		{
			String s = null;
			if (mDir == Crossword.Word.DIR_ACROSS) {
				s = mNumber + " Across";
			} else if (mDir == Crossword.Word.DIR_DOWN) {
				s = mNumber + " Down";
			}

			if (s != null) {
				s += " [" + mStart + ".." + mEnd + "]";
			} else {
				s = "??";
			}

			return s;
		}
	}

	public static List<WordReference> findReferences(String hint,
			Crossword crossword)
	{
		List<WordReference> refs = new ArrayList<>();
		if (hint == null) {
			return refs;
		}

		Matcher m = NUMBER_FINDER.matcher(hint);
		while (m.find()) {
			// Find any numbers
			int number = Integer.parseInt(m.group(1));
			int start = m.start(1);
			int end = m.end(1);

			// Find closest directional marker
			Matcher m2 = DIRECTION_FINDER_EN.matcher(hint);
			if (m2.find(end)) {
				int dir = WORD_ACROSS_EN.equalsIgnoreCase(m2.group(1))
						? Crossword.Word.DIR_ACROSS
						: Crossword.Word.DIR_DOWN;

				// Confirm that the word exists in the crossword
				Crossword.Word refWord = crossword.findWord(dir, number);
				if (refWord != null) {
					// It exists, add the reference
					WordReference ref = new WordReference();
					ref.mNumber = number;
					ref.mDir = dir;
					ref.mStart = start;
					ref.mEnd = end;
					refs.add(ref);
				}
			}
		}

		return refs;
	}
}
