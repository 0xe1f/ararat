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

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Stack;

abstract class SimpleXmlParser
{
	public static final class SimpleXmlPath
	{
		private List<String> mList;
		private int mPos;

		private SimpleXmlPath(List<String> stack)
		{
			mPos = 0;
			mList = stack;
		}

		public boolean startsWith(String... names)
		{
			int listSize = mList.size();
			if (names.length > (listSize - mPos)) {
				return false;
			}

			for (int i = 0; i < names.length; i++) {
				String stackName = mList.get(i + mPos);
				String name = names[i];

				if (name.endsWith("*")) {
					// Trailing wildcard match
					String prefix = name.substring(0, name.length() - 1);
					if (!stackName.startsWith(prefix)) {
						return false;
					}
				} else {
					// Exact match
					if (!stackName.equals(name) && !"?".equals(name)) {
						return false;
					}
				}
			}

			mPos += names.length;

			return true;
		}

		public boolean isEqualTo(String... names)
		{
			return names.length == (mList.size() - mPos) && startsWith(names);
		}

		public boolean isDeadEnd()
		{
			return isEqualTo();
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (int i = mPos, n = mList.size(); i < n; i++) {
				sb.append("/");
				sb.append(mList.get(i));
			}

			return sb.toString();
		}
	}

	private Stack<String> mStack;

	public SimpleXmlParser()
	{
		mStack = new Stack<>();
	}

	protected static int getIntValue(XmlPullParser parser, String name, int defaultValue)
	{
		String value = parser.getAttributeValue(null, name);
		if (value == null) {
			return defaultValue;
		}

		return Integer.parseInt(value);
	}

	protected void parseXml(InputStream inputStream)
			throws XmlPullParserException, IOException
	{
		mStack.clear();

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(false);

		XmlPullParser parser = factory.newPullParser();
		parser.setInput(inputStream, "UTF-8");
		parser.setFeature(Xml.FEATURE_RELAXED, true);

		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				mStack.push(parser.getName());
				onStartElement(new SimpleXmlPath(mStack), parser);
			} else if (eventType == XmlPullParser.END_TAG) {
				mStack.pop();
			} else if (eventType == XmlPullParser.TEXT) {
				onTextContent(new SimpleXmlPath(mStack), parser.getText());
			}

			eventType = parser.next();
		}
	}

	protected void onStartElement(SimpleXmlPath path, XmlPullParser parser)
	{
	}

	protected void onTextContent(SimpleXmlPath path, String text)
	{
	}
}
