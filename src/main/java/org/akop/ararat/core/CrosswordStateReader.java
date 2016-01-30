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
import java.io.InputStream;
import java.io.ObjectInputStream;


public class CrosswordStateReader
		implements Closeable
{
	private static final int VERSION_CURRENT = 1;

	private ObjectInputStream mInStream;

	public CrosswordStateReader(InputStream in)
			throws IOException
	{
		mInStream = new ObjectInputStream(in);
	}

	public Crossword.State read()
			throws IOException
	{
		Crossword.State state;

		int version = mInStream.readByte();
		validateVersion(version);

		try {
			state = readState(version);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		return state;
	}

	private Crossword.State readState(int version)
			throws IOException, ClassNotFoundException
	{
		Crossword.State state = new Crossword.State();

		state.mWidth = mInStream.readInt();
		state.mHeight = mInStream.readInt();
		state.mSquareCounts = mInStream.readLong();
		state.mPlayTimeMillis = mInStream.readLong();
		state.mLastPlayed = mInStream.readLong();
		state.mSelection = mInStream.readInt();

		// Un-flatten the matrices
		char[] flatChars = (char[]) mInStream.readObject();
		int[] flatAttrs = (int[]) mInStream.readObject();

		state.mCharMatrix = new char[state.mHeight][state.mWidth];
		state.mAttrMatrix = new int[state.mHeight][state.mWidth];

		for (int i = 0, j = 0; i < state.mHeight; i++, j += state.mWidth) {
			System.arraycopy(flatChars, j, state.mCharMatrix[i], 0, state.mWidth);
			System.arraycopy(flatAttrs, j, state.mAttrMatrix[i], 0, state.mWidth);
		}

		return state;
	}

	private void validateVersion(int version)
	{
		if (version != VERSION_CURRENT) {
			throw new IllegalArgumentException("State version " + version + " not supported");
		}
	}

	@Override
	public void close()
			throws IOException
	{
		if (mInStream != null) {
			mInStream.close();
		}
	}
}
