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


public class CrosswordReader
		implements Closeable
{
	private ObjectInputStream mInStream;

	public CrosswordReader(InputStream in)
			throws IOException
	{
		mInStream = new ObjectInputStream(in);
	}

	public Crossword read()
			throws IOException
	{
		Crossword crossword;

		// Check magic number
		if (mInStream.readInt() != CrosswordWriter.MAGIC_NUMBER) {
			throw new IllegalArgumentException("Magic number mismatch");
		}

		// Check version number
		int version = mInStream.readByte();
		if (version != CrosswordWriter.VERSION_CURRENT) {
			throw new IllegalArgumentException("Version " + version + " not supported");
		}

		// Read the puzzle
		try {
			crossword = readCrossword(version);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		return crossword;
	}

	private Crossword readCrossword(int version)
			throws IOException, ClassNotFoundException
	{
		Crossword crossword = new Crossword();

		crossword.mWidth = mInStream.readInt();
		crossword.mHeight = mInStream.readInt();
		crossword.mSquareCount = mInStream.readInt();
		crossword.mTitle = (String) mInStream.readObject();
		crossword.mDescription = (String) mInStream.readObject();
		crossword.mAuthor = (String) mInStream.readObject();
		crossword.mCopyright = (String) mInStream.readObject();
		crossword.mAlphabet = (char[]) mInStream.readObject();
		crossword.mDate = mInStream.readLong();

		for (int i = mInStream.readInt(); i > 0; i--) {
			Crossword.Word word = readWord(version);
			word.mDirection = Crossword.Word.DIR_ACROSS;

			crossword.mWordsAcross.add(word);
		}
		for (int i = mInStream.readInt(); i > 0; i--) {
			Crossword.Word word = readWord(version);
			word.mDirection = Crossword.Word.DIR_DOWN;

			crossword.mWordsDown.add(word);
		}

		return crossword;
	}

	private Crossword.Word readWord(int version)
			throws IOException, ClassNotFoundException
	{
		Crossword.Word word = new Crossword.Word();

		word.mNumber = mInStream.readShort();
		word.mHint = (String) mInStream.readObject();
		word.mStartRow = mInStream.readShort();
		word.mStartColumn = mInStream.readShort();
		word.mHintUrl = (String) mInStream.readObject();
		word.mCitation = (String) mInStream.readObject();

		word.mCells = new Crossword.Cell[mInStream.readInt()];
		for (int i = 0; i < word.mCells.length; i++) {
			word.mCells[i] = readCell(version);
		}

		return word;
	}

	private Crossword.Cell readCell(int version)
			throws IOException, ClassNotFoundException
	{
		Crossword.Cell cell = new Crossword.Cell();

		cell.mAttrFlags = mInStream.readByte();
		cell.mChars = (char[]) mInStream.readObject();

		return cell;
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
