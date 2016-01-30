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

package org.akop.ararat.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


public class CrosswordStateWriter
		implements Closeable
{
	private static final int VERSION_CURRENT = 1;

	private ObjectOutputStream mOutStream;

	public CrosswordStateWriter(OutputStream out)
			throws IOException
	{
		mOutStream = new ObjectOutputStream(out);
	}

	public void write(Crossword.State state)
			throws IOException
	{
		mOutStream.writeByte(VERSION_CURRENT);
		writeState(state);
	}

	private void writeState(Crossword.State state)
			throws IOException
	{
		mOutStream.writeInt(state.mWidth);
		mOutStream.writeInt(state.mHeight);
		mOutStream.writeLong(state.mSquareCounts);
		mOutStream.writeLong(state.mPlayTimeMillis);
		mOutStream.writeLong(state.mLastPlayed);
		mOutStream.writeInt(state.mSelection);

		// Flatten the matrices
		char[] flatChars = new char[state.mWidth * state.mHeight];
		int[] flatAttrs = new int[state.mWidth * state.mHeight];

		for (int i = 0, j = 0; i < state.mHeight; i++, j += state.mWidth) {
			System.arraycopy(state.mCharMatrix[i], 0, flatChars, j, state.mWidth);
			System.arraycopy(state.mAttrMatrix[i], 0, flatAttrs, j, state.mWidth);
		}

		mOutStream.writeObject(flatChars);
		mOutStream.writeObject(flatAttrs);
	}

	@Override
	public void close()
			throws IOException
	{
		if (mOutStream != null) {
			mOutStream.close();
		}
	}
}
