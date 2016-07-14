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
	private ObjectInputStream mInStream;

	public CrosswordStateReader(InputStream in)
			throws IOException
	{
		mInStream = new ObjectInputStream(in);
	}

	public CrosswordState read()
			throws IOException
	{
		CrosswordState state;

		if (mInStream.readInt() != CrosswordStateWriter.MAGIC_NUMBER) {
			throw new IllegalArgumentException("Magic number mismatch");
		}

		int version = mInStream.readByte();
		if (version > CrosswordStateWriter.VERSION_CURRENT) {
			throw new IllegalArgumentException("State version " + version + " not supported");
		}

		try {
			state = readState(version);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		return state;
	}

	private CrosswordState readState(int version)
			throws IOException, ClassNotFoundException
	{
		CrosswordState state = new CrosswordState();

		state.mWidth = mInStream.readInt();
		state.mHeight = mInStream.readInt();
		state.mSquareCounts = mInStream.readLong();
		state.mPlayTimeMillis = mInStream.readLong();
		state.mLastPlayed = mInStream.readLong();
		state.mSelection = mInStream.readInt();

		if (version < 2) {
			char[] flatChars = (char[]) mInStream.readObject();
			state.mCharMatrix = new String[state.mHeight][state.mWidth];
			for (int i = 0, j = 0; i < state.mHeight; i++) {
				for (int c = 0; c < state.mWidth; c++) {
					char ch = flatChars[j++];
					if (ch != '\0') {
						state.mCharMatrix[i][c] = String.valueOf(ch);
					}
				}
			}
		} else {
			String[] flatChars = (String[]) mInStream.readObject();
			state.mCharMatrix = new String[state.mHeight][state.mWidth];
			for (int i = 0, j = 0; i < state.mHeight; i++, j += state.mWidth) {
				System.arraycopy(flatChars, j, state.mCharMatrix[i], 0, state.mWidth);
			}
		}

		// Un-flatten the matrices
		int[] flatAttrs = (int[]) mInStream.readObject();
		state.mAttrMatrix = new int[state.mHeight][state.mWidth];
		for (int i = 0, j = 0; i < state.mHeight; i++, j += state.mWidth) {
			System.arraycopy(flatAttrs, j, state.mAttrMatrix[i], 0, state.mWidth);
		}

		return state;
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
