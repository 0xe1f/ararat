// Copyright (c) 2014-2015 Akop Karapetyan
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UClickFormatter
		extends SimpleXmlParser
		implements CrosswordFormatter
{
	// Matches %'s that aren't URL encoded
	private final static Pattern PERCENT_MATCHER = Pattern.compile("%(?![0-9A-Fa-f]{2})");

	private Crossword.Builder mBuilder;
	private int mWidth;
	private int mHeight;

	@Override
	public void setEncoding(String encoding)
	{
		// Stub
	}

	@Override
	public void read(Crossword.Builder builder, InputStream inputStream)
			throws IOException
	{
		mWidth = -1;
		mHeight = -1;
		mBuilder = builder;

		try {
			parseXml(inputStream);
		} catch (XmlPullParserException e) {
			throw new RuntimeException("Malformed XML", e);
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
		if (path.startsWith("crossword")) {
			if (path.startsWith("Title")) {
				mBuilder.setTitle(urlDecodedAttributeValue(parser, "v"));
			} else if (path.startsWith("Author")) {
				mBuilder.setAuthor(urlDecodedAttributeValue(parser, "v"));
			} else if (path.startsWith("Copyright")) {
				mBuilder.setCopyright(urlDecodedAttributeValue(parser, "v"));
			} else if (path.startsWith("Width")) {
				mWidth = getIntValue(parser, "v", -1);
				mBuilder.setWidth(mWidth);
			} else if (path.startsWith("Height")) {
				mHeight = getIntValue(parser, "v", -1);
				mBuilder.setHeight(mHeight);
			} else if (path.startsWith("across", "?")
					|| path.startsWith("down", "?")) {
				Crossword.Word word = parseWord(parser);
				mBuilder.addWord(word);
			}
		}
	}

	private static String safeUrlDecode(String value)
	{
		if (value != null) {
			StringBuilder sb = new StringBuilder();
			int start = 0;
			Matcher m = PERCENT_MATCHER.matcher(value);
			while (m.find()) {
				sb.append(value, start, m.start());
				sb.append("%25");
				start = m.end();
			}

			sb.append(value, start, value.length());
			value = sb.toString();

			try {
				value = URLDecoder.decode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		return value;
	}

	private String urlDecodedAttributeValue(XmlPullParser parser, String name)
	{
		return safeUrlDecode(parser.getAttributeValue(null, name));
	}

	private Crossword.Word parseWord(XmlPullParser parser)
	{
		int direction;
		String name = parser.getName();
		if (name.startsWith("a")) {
			direction = Crossword.Word.DIR_ACROSS;
		} else if (name.startsWith("d")) {
			direction = Crossword.Word.DIR_DOWN;
		} else {
			throw new IllegalArgumentException("Unexpected word indicator: " + name);
		}

		int number = getIntValue(parser, "cn", 0);
		if (number < 1) {
			throw new IllegalArgumentException("Number "
					+ parser.getAttributeValue(null, "cn") + " is not valid");
		}

		String answer = parser.getAttributeValue(null, "a");
		String hint = urlDecodedAttributeValue(parser, "c");
		int cellIndex = getIntValue(parser, "n", 0) - 1;

		Crossword.Word.Builder wb = new Crossword.Word.Builder()
				.setDirection(direction)
				.setNumber(number)
				.setStartRow(cellIndex / mWidth)
				.setStartColumn(cellIndex % mWidth)
				.setHint(hint);

		for (char ch: answer.toCharArray()) {
			wb.addCell(ch);
		}

		return wb.build();
	}
}
