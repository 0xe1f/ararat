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
import org.akop.ararat.io.CrosswordFormatter;
import org.akop.ararat.io.PzzlFormatter;
import org.junit.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


public class PzzlFormatterUnitTest
		extends UnitTestBase
{
	@Test
	public void crossword_testParser()
			throws Exception
	{
		DateFormat dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
		String urlFormat = "http://nytsyn.pzzl.com/nytsyn-crossword/nytsyncrossword?date=%s";

		long millis = System.currentTimeMillis();
		CrosswordFormatter formatter = new PzzlFormatter();
		for (int i = 0; i < 30; i++, millis -= DateUtils.DAY_IN_MILLIS) {
			String url = String.format(urlFormat, dateFormat.format(millis));
			System.out.print("Parsing " + url + " ...");

			Crossword crossword = null;
			try {
				if ((crossword = tryDownload(url, formatter)) == null) {
					System.out.println("MISSING");
					continue;
				}
			} catch (IOException e) {
				System.out.println("IO ERROR: " + e.getMessage());
				continue;
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}

			assertNotNull(crossword);

			Crossword.Cell[][] map = crossword.getCellMap();
			for (int y = 0; y < map.length; y++) {
				boolean allEmpty = true;
				for (int x = 0; x < map[y].length; x++) {
					if (map[y][x] != null) {
						allEmpty = false;
						break;
					}
				}
				assertFalse("Row " + y + " is completely empty (height "
						+ map.length + ")", allEmpty);
			}
			for (int x = 0; x < map[0].length; x++) {
				boolean allEmpty = true;
				for (int y = 0; y < map.length; y++) {
					if (map[y][x] != null) {
						allEmpty = false;
						break;
					}
				}
				assertFalse("Column " + x + " is completely empty (width "
						+ map[0].length + ")", allEmpty);
			}

			System.out.println("OK!");
		}
	}
}