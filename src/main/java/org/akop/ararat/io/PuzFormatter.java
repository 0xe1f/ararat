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

import android.os.SystemClock;
import android.util.Log;

import org.akop.ararat.core.Crossword;
import org.akop.ararat.util.SparseArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


// Based on https://code.google.com/p/puz/wiki/FileFormat
public class PuzFormatter
		implements CrosswordFormatter
{
	private static final String LOG_TAG = CrosswordFormatter.class.getSimpleName();

	private static final char GEXT_CIRCLED = 0x80;
	private static final String MAGIC_STRING = "ACROSS&DOWN\0";

	private static final char EMPTY = '.';
	private static final String DEFAULT_ENCODING = "ISO-8859-1";

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

		// Overall checksum
		if (reader.skip(2) != 2) {
			throw new FormatException("Overall checksum incomplete");
		}

		// Magic string
		char[] temp = new char[128];
		if (reader.read(temp, 0, MAGIC_STRING.length()) != MAGIC_STRING.length()) {
			throw new FormatException("Magic string incomplete");
		}

		String magic = new String(temp, 0, MAGIC_STRING.length());
		if (!MAGIC_STRING.equals(magic)) {
			throw new FormatException("Magic string mismatch (got '" + magic + "')");
		}

		// Checksums
		if (reader.skip(2) != 2) {
			throw new FormatException("CIB checksum incomplete");
		}
		if (reader.skip(4) != 4) {
			throw new FormatException("Masked low checksum incomplete");
		}
		if (reader.skip(4) != 4) {
			throw new FormatException("Masked high checksum incomplete");
		}

		// Version
		if (reader.read(temp, 0, 4) != 4) {
			throw new FormatException("Version information incomplete");
		}

		// Garbage
		reader.skip(2);

		// Scrambled checksum
		short unscrambledChecksum = readShort(reader);

		// More garbage
		reader.skip(12);

		// Width, height
		if (reader.read(temp, 0, 2) != 2) {
			throw new FormatException("Board dimensions incomplete");
		}

		int width = temp[0];
		int height = temp[1];

		// Clue count
		int clueCount = readShort(reader);

		// Unknown
		reader.skip(2);

		// Scrambled/unscrambled
		boolean scrambled = readShort(reader) != 0;

		// The layout
		char[][] charMap = new char[height][width];
		for (int i = 0; i < height; i++) {
			int totalRead = 0;
			int read;
			while ((read = reader.read(charMap[i], totalRead, width - totalRead)) < width) {
				if (read < 0) {
					throw new FormatException("Line " + i + " incomplete (read "
							+ totalRead + " expected " + width + ")");
				}
				totalRead += read;
			}
		}

		// State (skip for now)
		if (reader.skip(width * height) != width * height) {
			throw new FormatException("State information incomplete");
		}

		// Title
		String title = readNullTerminatedString(reader);
		// Author
		String author = readNullTerminatedString(reader);
		// Copyright
		String copyright = readNullTerminatedString(reader);

		// Clues
		List<String> clues = new ArrayList<>();
		for (int i = 0; i < clueCount; i++) {
			clues.add(readNullTerminatedString(reader));
		}

		// Notes
		String notes = readNullTerminatedString(reader);

		// Sections
		int[][] rebusMap = null;
		SparseArray<String> rebusSols = null;
		byte[][] attrMap = new byte[height][width];
		char[] sect = new char[4];
		while (reader.read(sect, 0, sect.length) == sect.length) {
			int dataLength = readShort(reader);
			reader.skip(2); // checksum FIXME: verify

			// Read the data
			char[] data = new char[dataLength];
			int dataRead = 0;
			while (dataRead < dataLength) {
				int read = reader.read(data, dataRead, dataLength - dataRead);
				if (read < 0) {
					throw new FormatException("Unexpected end while reading extra data");
				}

				dataRead += read;
			}

			// Handle sections
			String sectionName = new String(sect);
			if ("GEXT".equals(sectionName)) {
				for (int i = 0, d = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {
						attrMap[i][j] |= data[d++];
					}
				}
			} else if ("GRBS".equals(sectionName)) {
				rebusMap = new int[height][width];
				for (int i = 0, r = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {
						rebusMap[i][j] = data[r++];
					}
				}
			} else if ("RTBL".equals(sectionName)) {
				rebusSols = new SparseArray<>();
				String rebusSpec = new String(data);
				for (String rebus: rebusSpec.split(";")) {
					int sep = rebus.indexOf(':');
					if (sep == -1) {
						throw new FormatException("Missing rebus delimiter");
					} else if (sep < 2) {
						throw new FormatException("Invalid rebus index");
					} else if (sep + 1 >= rebus.length()) {
						throw new FormatException("Missing rebus solution");
					}

					int ixStart = 0;
					while (rebus.charAt(ixStart) == ' ') {
						ixStart++;
					}

					int index = Integer.parseInt(rebus.substring(ixStart, sep));
					rebusSols.put(index + 1, rebus.substring(sep + 1));
				}
			}

			reader.skip(1); // trailing null
		}

		if (rebusMap != null && rebusSols != null) {
			// Verify rebus solutions
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					int key = rebusMap[i][j];
					if (key != 0) {
						if (rebusSols.get(key) == null) {
							throw new FormatException("Missing rebus solution for key " + key);
						}
					}
				}
			}
		}

		// FIXME: complete rebus implementation

		// Unscramble
		// FIXME: allow passing of key
		if (scrambled) {
			if (!bruteForceKey(charMap, unscrambledChecksum)) {
				throw new SecurityException("Unable to locate a key (tried to brute-force)");
			}
		}

		builder.setWidth(width);
		builder.setHeight(height);
		builder.setTitle(title);
		builder.setAuthor(author);
		builder.setCopyright(copyright);
		builder.setComment(notes);

		buildWords(builder, clues, charMap, attrMap, rebusMap, rebusSols);
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

	private static void buildWords(Crossword.Builder cb, List<String> clues,
			char[][] charMap, byte[][] attrMap,
			int[][] rebusMap, SparseArray<String> rebusSols)
	{
		for (int i = 0, clue = 0, number = 0; i < charMap.length; i++) {
			for (int j = 0; j < charMap[i].length; j++) {
				if (charMap[i][j] != EMPTY) {
					boolean incremented = false;
					if (j == 0 || (j > 0 && charMap[i][j - 1] == EMPTY)) {
						// Start of a new Across word
						number++;
						incremented = true;

						Crossword.Word.Builder wb = new Crossword.Word.Builder()
								.setDirection(Crossword.Word.DIR_ACROSS)
								.setHint(clues.get(clue++))
								.setNumber(number)
								.setStartRow(i)
								.setStartColumn(j);

						// Copy contents to a temp buffer
						for (int k = j; k < charMap[i].length && charMap[i][k] != EMPTY; k++) {
							int attrs = 0;
							if ((attrMap[i][k] & GEXT_CIRCLED) != 0) {
								attrs |= Crossword.Cell.ATTR_CIRCLED;
							}
							String rebus = null;
							if (rebusMap != null && rebusSols != null) {
								rebus = rebusSols.get(rebusMap[i][k]);
							}

							if (rebus != null) {
								wb.addCell(rebus, attrs);
							} else {
								wb.addCell(String.valueOf(charMap[i][k]), attrs);
							}
						}

						cb.addWord(wb.build());
					}

					if (i == 0 || (i > 0 && charMap[i - 1][j] == EMPTY)) {
						// Start of a new Down word
						if (!incremented) {
							number++;
						}

						Crossword.Word.Builder wb = new Crossword.Word.Builder()
								.setDirection(Crossword.Word.DIR_DOWN)
								.setHint(clues.get(clue++))
								.setNumber(number)
								.setStartRow(i)
								.setStartColumn(j);

						for (int k = i; k < charMap.length && charMap[k][j] != EMPTY; k++) {
							int attrs = 0;
							if ((attrMap[k][j] & GEXT_CIRCLED) != 0) {
								attrs |= Crossword.Cell.ATTR_CIRCLED;
							}
							String rebus = null;
							if (rebusMap != null && rebusSols != null) {
								rebus = rebusSols.get(rebusMap[k][j]);
							}

							if (rebus != null) {
								wb.addCell(rebus, attrs);
							} else {
								wb.addCell(String.valueOf(charMap[k][j]), attrs);
							}
						}

						cb.addWord(wb.build());
					}
				}
			}
		}
	}

	private static boolean bruteForceKey(char[][] map, short unscrambledChecksum)
	{
		int height = map.length;
		if (height == 0) {
			return false;
		}
		int width = map[0].length;

		int code;
		boolean found = false;
		long started = SystemClock.uptimeMillis();

		char[][] copy = new char[height][width];
		for (code = 0; code < 10000; code++) {
			for (int i = 0; i < height; i++) {
				System.arraycopy(map[i], 0, copy[i], 0, width);
			}

			unscramble(copy, code);

			if (mapChecksum(copy) == unscrambledChecksum) {
				for (int i = 0; i < height; i++) {
					System.arraycopy(copy[i], 0, map[i], 0, width);
				}

				found = true;
				break;
			}
		}

		if (found) {
			Log.d(LOG_TAG, String.format("Found a key (%d) in %.02fs",
					code, (SystemClock.uptimeMillis() - started) / 1000f));
		} else {
			Log.d(LOG_TAG, String.format("Key not found after %.02fs",
					(SystemClock.uptimeMillis() - started) / 1000f));
		}

		return found;
	}

	private static void unscramble(char[][] map, int key)
	{
		int[] keyDigits = new int[] {
				(key / 1000) % 10,
				(key / 100) % 10,
				(key / 10) % 10,
				(key) % 10,
		};

		int height = map.length;
		if (height == 0) {
			return;
		}
		int width = map[0].length;

		StringBuilder input = toColumnMajorOrder(map);
		StringBuilder unscrambled = new StringBuilder(input);

		int len = unscrambled.length();
		for (int k = keyDigits.length - 1; k >= 0; k--) {
			unscrambleString(unscrambled);
			unshift(unscrambled, keyDigits[k]);

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < len; i++) {
				int code = (int) unscrambled.charAt(i) - keyDigits[i % keyDigits.length];
				if (code < 65) {
					code += 26;
				}

				sb.append((char) code);
			}

			unscrambled = sb;
		}

		for (int j = 0, k = 0; j < width; j++) {
			for (int i = 0; i < height; i++) {
				if (map[i][j] != EMPTY) {
					map[i][j] = unscrambled.charAt(k++);
				}
			}
		}
	}

	private static void unshift(StringBuilder sb, int pivot)
	{
		int len = sb.length();
		String first = sb.substring(len - pivot);
		String second = sb.substring(0, len - pivot);

		for (int i = 0, sp = 0, fl = first.length(); i < len; i++) {
			if (i < fl) {
				sb.setCharAt(i, first.charAt(i));
			} else {
				sb.setCharAt(i, second.charAt(sp++));
			}
		}
	}

	private static void unscrambleString(StringBuilder sb)
	{
		int len = sb.length();
		int mid = len / 2;

		StringBuilder back = new StringBuilder(mid + 1);
		StringBuilder front = new StringBuilder(mid + 1);

		for (int i = 0; i < len; i++) {
			if (i % 2 == 0) {
				back.append(sb.charAt(i));
			} else {
				front.append(sb.charAt(i));
			}
		}

		for (int i = 0, bp = 0, fl = front.length(); i < len; i++) {
			if (i < fl) {
				sb.setCharAt(i, front.charAt(i));
			} else {
				sb.setCharAt(i, back.charAt(bp++));
			}
		}
	}

	private static short mapChecksum(char[][] map)
	{
		return checksumRegion(toColumnMajorOrder(map), (short) 0);
	}

	private static short checksumRegion(StringBuilder sb, short checksum)
	{
		int checksumInt = checksum & 0xffff;
		for (int i = 0, n = sb.length(); i < n; i++) {
			if ((checksumInt & 1) != 0) {
				checksumInt = ((checksumInt >>> 1) + 0x8000) & 0xffff;
			} else {
				checksumInt >>>= 1;
			}
			checksumInt = (checksumInt + sb.charAt(i)) & 0xffff;
		}

		return (short) checksumInt;
	}

	private static StringBuilder toColumnMajorOrder(char[][] map)
	{
		int height = map.length;
		if (height == 0) {
			throw new IndexOutOfBoundsException("Height cannot be zero");
		}
		int width = map[0].length;

		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < width; j++) {
			for (int i = 0; i < height; i++) {
				if (map[i][j] != EMPTY) {
					sb.append(map[i][j]);
				}
			}
		}
		return sb;
	}

	private static short readShort(InputStreamReader reader)
			throws IOException
	{
		char[] buf = new char[2];
		if (reader.read(buf, 0, 2) != 2) {
			throw new FormatException("16-bit value incomplete");
		}

		// little-endian
		return (short) (buf[0] | (((int) buf[1]) << 8));
	}

	private static String readNullTerminatedString(InputStreamReader reader)
			throws IOException
	{
		StringBuilder sb = new StringBuilder();
		char[] ch = new char[1];
		int read;

		while ((read = reader.read(ch, 0, 1)) == 1 && ch[0] != '\0') {
			sb.append(ch[0]);
		}

		if (read != 1) {
			throw new FormatException("Unexpected end of null-terminated string");
		}

		return sb.toString();
	}
}
