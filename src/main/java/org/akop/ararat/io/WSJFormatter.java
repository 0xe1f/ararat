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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class WSJFormatter
		implements CrosswordFormatter
{
	private static final String LOG_TAG = WSJFormatter.class.getSimpleName();

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
}
