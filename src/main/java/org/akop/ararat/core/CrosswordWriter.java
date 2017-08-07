// Copyright (c) 2014-2017 Akop Karapetyan
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


public class CrosswordWriter
		implements Closeable
{
	static final int VERSION_CURRENT = 4;
	static final int MAGIC_NUMBER = 0xdeadbea7;

	private ObjectOutputStream mOutStream;

	public CrosswordWriter(OutputStream out)
			throws IOException
	{
		mOutStream = new ObjectOutputStream(out);
	}

	public void write(Crossword crossword)
			throws IOException
	{
		mOutStream.writeInt(MAGIC_NUMBER);
		mOutStream.writeByte(VERSION_CURRENT);

		writeCrossword(crossword);
	}

	private void writeCrossword(Crossword crossword)
			throws IOException
	{
		mOutStream.writeInt(crossword.mWidth);
		mOutStream.writeInt(crossword.mHeight);
		mOutStream.writeInt(crossword.mSquareCount);
		mOutStream.writeObject(crossword.mTitle);
		mOutStream.writeObject(crossword.mDescription);
		mOutStream.writeObject(crossword.mAuthor);
		mOutStream.writeObject(crossword.mCopyright);
		mOutStream.writeObject(crossword.mComment);
		char[] alphabet = new char[crossword.mAlphabet.size()];
		int i = 0;
		for (char ch: crossword.mAlphabet) {
			alphabet[i] = ch;
		}
		mOutStream.writeObject(alphabet);
		mOutStream.writeLong(crossword.mDate);
		mOutStream.writeInt(crossword.mFlags);

		mOutStream.writeInt(crossword.mWordsAcross.size());
		for (Crossword.Word word: crossword.mWordsAcross) {
			writeWord(word);
		}
		mOutStream.writeInt(crossword.mWordsDown.size());
		for (Crossword.Word word: crossword.mWordsDown) {
			writeWord(word);
		}
	}

	private void writeWord(Crossword.Word word)
			throws IOException
	{
		mOutStream.writeShort(word.mNumber);
		mOutStream.writeObject(word.mHint);
		mOutStream.writeShort(word.mStartRow);
		mOutStream.writeShort(word.mStartColumn);
		mOutStream.writeObject(word.mHintUrl);
		mOutStream.writeObject(word.mCitation);

		mOutStream.writeInt(word.mCells.length);
		for (Crossword.Cell cell: word.mCells) {
			writeCell(cell);
		}
	}

	private void writeCell(Crossword.Cell cell)
			throws IOException
	{
		mOutStream.writeByte(cell.mAttrFlags);
		mOutStream.writeObject(cell.mChars);
	}

	void writeForHash(Crossword crossword)
			throws IOException
	{
		mOutStream.writeInt(crossword.mWidth);
		mOutStream.writeInt(crossword.mHeight);

		for (Crossword.Word word: crossword.mWordsAcross) {
			writeWordForHash(word);
		}
		for (Crossword.Word word: crossword.mWordsDown) {
			writeWordForHash(word);
		}
	}

	private void writeWordForHash(Crossword.Word word)
			throws IOException
	{
		mOutStream.writeShort(word.mNumber);
		mOutStream.writeObject(word.mHint);
		mOutStream.writeShort(word.mStartRow);
		mOutStream.writeShort(word.mStartColumn);

		for (Crossword.Cell cell: word.mCells) {
			mOutStream.writeByte(cell.mAttrFlags);
			mOutStream.writeObject(cell.mChars);
		}
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
