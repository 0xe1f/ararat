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

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class Crossword
		implements Parcelable
{
	private static char[] EMPTY_ALPHABET = new char[0];
	public static final char[] ALPHABET_ENGLISH = new char[] {
			'A','B','C','D','E','F','G','H','I','J','K','L','M',
			'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
	};

	public static class Builder
	{
		private int mWidth;
		private int mHeight;
		private String mTitle;
		private String mDescription;
		private String mAuthor;
		private String mCopyright;
		private long mDate;
		private List<Word> mWords;
		private char[] mAlphabet;

		public Builder()
		{
			mWords = new ArrayList<>();
			mAlphabet = ALPHABET_ENGLISH;
		}

		public Builder setWidth(int width)
		{
			mWidth = width;
			return this;
		}

		public Builder setHeight(int height)
		{
			mHeight = height;
			return this;
		}

		public Builder setTitle(String title)
		{
			mTitle = title;
			return this;
		}

		public Builder setDescription(String desc)
		{
			mDescription = desc;
			return this;
		}

		public Builder setAuthor(String author)
		{
			mAuthor = author;
			return this;
		}

		public Builder setCopyright(String copyright)
		{
			mCopyright = copyright;
			return this;
		}

		public Builder setDate(long date)
		{
			mDate = date;
			return this;
		}

		public Builder addWord(Word word)
		{
			mWords.add(word);
			return this;
		}

		public Builder setAlphabet(char[] alphabet)
		{
			if (alphabet != null) {
				mAlphabet = new char[alphabet.length];
				System.arraycopy(alphabet, 0, mAlphabet, 0, alphabet.length);
			} else {
				mAlphabet = EMPTY_ALPHABET;
			}

			return this;
		}

		private int countSquares()
		{
			int count = 0;
			boolean[][] done = new boolean[mHeight][mWidth];

			for (Word word: mWords) {
				if (word.mDirection == Word.DIR_ACROSS) {
					for (int i = 0, col = word.mStartColumn; i < word.mCells.length; i++, col++) {
						if (!done[word.mStartRow][col]) {
							count++;
							done[word.mStartRow][col] = true;
						}
					}
				} else if (word.mDirection == Word.DIR_DOWN) {
					for (int i = 0, row = word.mStartRow; i < word.mCells.length; i++, row++) {
						if (!done[row][word.mStartColumn]) {
							count++;
							done[row][word.mStartColumn] = true;
						}
					}
				}
			}

			return count;
		}

		public void autoNumber()
		{
			// autonumber left-to-right, top-to-bottom
			int[][] tuples = new int[mWords.size()][4];
			for (int i = 0, n = mWords.size(); i < n; i++) {
				Word word = mWords.get(i);
				tuples[i] = new int[]{ i, word.mStartRow,
						word.mStartColumn, word.mDirection,
				};
			}

			Arrays.sort(tuples, new Comparator<int[]>()
			{
				@Override
				public int compare(int[] lhs, int[] rhs)
				{
					if (lhs[1] != rhs[1]) { // sort by row
						return lhs[1] - rhs[1];
					}
					if (lhs[2] != rhs[2]) { // sort by column
						return lhs[2] - rhs[2];
					}
					if (lhs[3] != rhs[3]) { // sort by direction
						return (lhs[3] == Word.DIR_ACROSS) ? -1 : 1;
					}

					// Should never get here
					return 0;
				}
			});

			int pr = -1;
			int pc = -1;
			int number = 0;

			for (int[] tuple: tuples) {
				if (pr != tuple[1] || pc != tuple[2]) {
					number++;
				}

				mWords.get(tuple[0]).mNumber = number;
				pr = tuple[1];
				pc = tuple[2];
			}
		}

		public Crossword build()
		{
			Crossword crossword = new Crossword();
			crossword.mWidth = mWidth;
			crossword.mHeight = mHeight;
			crossword.mTitle = mTitle;
			crossword.mDescription = mDescription;
			crossword.mAuthor = mAuthor;
			crossword.mCopyright = mCopyright;
			crossword.mDate = mDate;
			crossword.mAlphabet = mAlphabet;
			crossword.mSquareCount = countSquares();

			for (Word word: mWords) {
				if (word.mDirection == Word.DIR_ACROSS) {
					crossword.mWordsAcross.add(word);
				} else if (word.mDirection == Word.DIR_DOWN) {
					crossword.mWordsDown.add(word);
				}
			}

			return crossword;
		}
	}

	public static final Creator<Crossword> CREATOR = new Creator<Crossword>()
	{
		public Crossword createFromParcel(Parcel in)
		{
			return new Crossword(in);
		}

		public Crossword[] newArray(int size)
		{
			return new Crossword[size];
		}
	};

	int mWidth;
	int mHeight;
	int mSquareCount;
	String mTitle;
	String mDescription;
	String mAuthor;
	String mCopyright;
	long mDate;
	List<Word> mWordsAcross;
	List<Word> mWordsDown;
	char[] mAlphabet;

	// Computed and cached on demand
	private String mHash;
	private Cell[][] mCellMap;

	Crossword()
	{
		mWordsAcross = new ArrayList<>();
		mWordsDown = new ArrayList<>();
		mAlphabet = EMPTY_ALPHABET;
	}

	private Crossword(Parcel in)
	{
		mWidth = in.readInt();
		mHeight = in.readInt();
		mSquareCount = in.readInt();

		mTitle = in.readString();
		mDescription = in.readString();
		mAuthor = in.readString();
		mCopyright = in.readString();

		mDate = in.readLong();

		mWordsAcross = new ArrayList<>();
		in.readTypedList(mWordsAcross, Word.CREATOR);
		mWordsDown = new ArrayList<>();
		in.readTypedList(mWordsDown, Word.CREATOR);

		mAlphabet = in.createCharArray();
	}

	public int getWidth()
	{
		return mWidth;
	}

	public int getHeight()
	{
		return mHeight;
	}

	public char[] getAlphabet()
	{
		return mAlphabet;
	}

	public String getTitle()
	{
		return mTitle;
	}

	public State newState()
	{
		return new State(mWidth, mHeight);
	}

	public String getDescription()
	{
		return mDescription;
	}

	public String getAuthor()
	{
		return mAuthor;
	}

	public String getCopyright()
	{
		return mCopyright;
	}

	public long getDate()
	{
		return mDate;
	}

	public List<Word> getWordsAcross()
	{
		return mWordsAcross;
	}

	public List<Word> getWordsDown()
	{
		return mWordsDown;
	}

	public Cell[][] getCellMap()
	{
		if (mCellMap == null) {
			mCellMap = new Cell[mHeight][mWidth];
			for (Crossword.Word word: mWordsAcross) {
				int row = word.mStartRow;
				for (int i = 0, col = word.mStartColumn;
					 i < word.mCells.length; i++, col++) {
					mCellMap[row][col] = word.cellAt(i);
				}
			}
			for (Crossword.Word word: mWordsDown) {
				int col = word.mStartColumn;
				for (int i = 0, row = word.mStartRow;
					 i < word.mCells.length; i++, row++) {
					mCellMap[row][col] = word.cellAt(i);
				}
			}
		}

		return mCellMap;
	}

	public Word previousWord(Word word)
	{
		int acrossLastIndex = mWordsAcross.size() - 1;
		int downLastIndex = mWordsDown.size() - 1;

		if (word != null) {
			int wordIndex = indexOfWord(word.mDirection, word.mNumber);
			if (word.mDirection == Word.DIR_ACROSS) {
				if (wordIndex > -1) {
					if (wordIndex > 0) {
						return mWordsAcross.get(wordIndex - 1);
					} else if (downLastIndex >= 0) {
						return mWordsDown.get(downLastIndex);
					}
				}
			} else if (word.mDirection == Word.DIR_DOWN) {
				if (wordIndex > -1) {
					if (wordIndex > 0) {
						return mWordsDown.get(wordIndex - 1);
					} else if (acrossLastIndex >= 0) {
						return mWordsAcross.get(acrossLastIndex);
					}
				}
			}
		}

		if (downLastIndex >= 0) {
			return mWordsDown.get(downLastIndex);
		}

		if (acrossLastIndex >= 0) {
			return mWordsAcross.get(acrossLastIndex);
		}

		return null;
	}

	public Word nextWord(Word word)
	{
		int acrossLastIndex = mWordsAcross.size() - 1;
		int downLastIndex = mWordsDown.size() - 1;

		if (word != null) {
			int wordIndex = indexOfWord(word.mDirection, word.mNumber);
			if (word.mDirection == Word.DIR_ACROSS) {
				if (wordIndex > -1) {
					if (wordIndex < acrossLastIndex) {
						return mWordsAcross.get(wordIndex + 1);
					} else if (downLastIndex >= 0) {
						return mWordsDown.get(0);
					}
				}
			} else if (word.mDirection == Word.DIR_DOWN) {
				if (wordIndex > -1) {
					if (wordIndex < downLastIndex) {
						return mWordsDown.get(wordIndex + 1);
					} else if (acrossLastIndex >= 0) {
						return mWordsAcross.get(0);
					}
				}
			}
		}

		if (acrossLastIndex >= 0) {
			return mWordsAcross.get(0);
		}

		if (downLastIndex >= 0) {
			return mWordsDown.get(0);
		}

		return null;
	}

	public Word findWord(int direction, int number)
	{
		int index = indexOfWord(direction, number);
		if (index >= 0) {
			if (direction == Word.DIR_ACROSS) {
				return mWordsAcross.get(index);
			} else if (direction == Word.DIR_DOWN) {
				return mWordsDown.get(index);
			}
		}

		return null;
	}

	public Word findWord(int direction, int row, int column)
	{
		List<Word> words = null;
		if (direction == Word.DIR_ACROSS) {
			words = mWordsAcross;
		} else if (direction == Word.DIR_DOWN) {
			words = mWordsDown;
		}

		if (words != null) {
			for (Word word: words) {
				if (direction == Word.DIR_ACROSS && word.mStartRow == row) {
					if (column >= word.mStartColumn
							&& column < word.mStartColumn + word.getLength()) {
						return word;
					}
				} else if (direction == Word.DIR_DOWN && word.mStartColumn == column) {
					if (row >= word.mStartRow
							&& row < word.mStartRow + word.getLength()) {
						return word;
					}
				}
			}
		}

		return null;
	}

	public int getSquareCount()
	{
		return mSquareCount;
	}

	public int getSizeRank()
	{
		final int[] table = new int[] {
				11, 14, 20, 25, 30, 35, 40,
		};

		int dimAvg = (mHeight + mWidth) / 2;
		for (int i = 0; i < table.length; i++) {
			if (dimAvg <= table[i]) {
				return i;
			}
		}

		return table.length;
	}

	private int indexOfWord(int direction, int number)
	{
		List<Word> list;
		if (direction == Word.DIR_ACROSS) {
			list = mWordsAcross;
		} else if (direction == Word.DIR_DOWN) {
			list = mWordsDown;
		} else {
			throw new IllegalArgumentException("Invalid word direction");
		}

		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).mNumber == number) {
					return i;
				}
			}
		}

		return -1;
	}

	private String computeHash()
	{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		try {
			CrosswordWriter writer = new CrosswordWriter(byteStream);
			writer.write(this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try { byteStream.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}

		// Compute SHA1 digest
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		md.update(byteStream.toByteArray());
		byte[] digest = md.digest();

		// Generate hex string
		StringBuilder sb = new StringBuilder();
		for (byte b: digest) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString();
	}

	public void updateStateStatistics(State state)
	{
		if (state.mWidth != mWidth) {
			throw new RuntimeException("State width doesn't match puzzle width");
		} else if (state.mHeight != mHeight) {
			throw new RuntimeException("State height doesn't match puzzle height");
		}

		int totalCount = 0;
		int solvedCount = 0;
		int cheatedCount = 0;
		int wrongCount = 0;

		boolean[][] done = new boolean[mHeight][mWidth];
		for (Word word: mWordsAcross) {
			int row = word.mStartRow;
			for (int i = 0, col = word.mStartColumn; i < word.mCells.length; i++, col++) {
				totalCount++;
				char stateChar = state.mCharMatrix[row][col];
				if (word.mCells[i].contains(stateChar)) {
					if (state.cheatedAt(row, col)) {
						cheatedCount++;
					} else {
						solvedCount++;
					}
				} else if (stateChar != State.EMPTY_CELL) {
					wrongCount++;
				}

				done[row][col] = true;
			}
		}
		for (Word word: mWordsDown) {
			int col = word.mStartColumn;
			for (int i = 0, row = word.mStartRow; i < word.mCells.length; i++, row++) {
				if (!done[row][col]) {
					totalCount++;
					char stateChar = state.mCharMatrix[row][col];
					if (word.mCells[i].contains(stateChar)) {
						if (state.cheatedAt(row, col)) {
							cheatedCount++;
						} else {
							solvedCount++;
						}
					} else if (stateChar != State.EMPTY_CELL) {
						wrongCount++;
					}
				}
			}
		}

		state.setSquareStats(solvedCount, cheatedCount, wrongCount, totalCount);
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(mWidth);
		dest.writeInt(mHeight);
		dest.writeInt(mSquareCount);

		dest.writeString(mTitle);
		dest.writeString(mDescription);
		dest.writeString(mAuthor);
		dest.writeString(mCopyright);
		dest.writeLong(mDate);

		dest.writeTypedList(mWordsAcross);
		dest.writeTypedList(mWordsDown);

		dest.writeCharArray(mAlphabet);
	}

	public String getHash()
	{
		if (mHash == null) {
			mHash = computeHash();
		}

		return mHash;
	}

	public static boolean equals(Crossword one, Crossword two)
	{
		if (one == null || two == null) {
			return one == two;
		}

		return one.computeHash().equals(two.computeHash());
	}

	@Override
	public int hashCode()
	{
		return getHash().hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof Crossword) && equals(this, (Crossword) o);
	}

	public static class State
			implements Parcelable
	{
		public static final char EMPTY_CELL = '\0';

		public static final int FLAG_CHEATED = 1;
		public static final int FLAG_MARKED  = 1 << 1;

		private static final int SEL_NUMBER_MASK  = 0x0000fff;
		private static final int SEL_NUMBER_SHIFT = 0;
		private static final int SEL_CHAR_MASK    = 0x0fff000;
		private static final int SEL_CHAR_SHIFT   = 12;
		private static final int SEL_DIR_MASK     = 0xf000000;
		private static final int SEL_DIR_SHIFT    = 24;

		private static final long SCOUNT_SOLVED_MASK  = 0xffff000000000000L;
		private static final int SCOUNT_SOLVED_SHIFT  = 48;
		private static final long SCOUNT_CHEATED_MASK = 0x0000ffff00000000L;
		private static final int SCOUNT_CHEATED_SHIFT = 32;
		private static final long SCOUNT_WRONG_MASK   = 0x00000000ffff0000L;
		private static final int SCOUNT_WRONG_SHIFT   = 16;
		private static final long SCOUNT_TOTAL_MASK   = 0x000000000000ffffL;
		private static final int SCOUNT_TOTAL_SHIFT   = 0;

		public static final Creator<State> CREATOR = new Creator<State>()
		{
			public State createFromParcel(Parcel in)
			{
				return new State(in);
			}

			public State[] newArray(int size)
			{
				return new State[size];
			}
		};

		int mHeight;
		int mWidth;
		long mSquareCounts;
		long mPlayTimeMillis;
		long mLastPlayed;
		int mSelection;
		char[][] mCharMatrix;
		int[][] mAttrMatrix;

		State()
		{
		}

		public State(State other)
		{
			this(other.mWidth, other.mHeight);

			for (int i = 0; i < mHeight; i++) {
				System.arraycopy(other.mCharMatrix[i], 0,
						mCharMatrix[i], 0, mWidth);
				System.arraycopy(other.mAttrMatrix[i], 0,
						mAttrMatrix[i], 0, mWidth);
			}

			mSquareCounts = other.mSquareCounts;
			mPlayTimeMillis = other.mPlayTimeMillis;
			mLastPlayed = other.mLastPlayed;
			mSelection = other.mSelection;
		}

		private State(int width, int height)
		{
			mWidth = width;
			mHeight = height;
			mCharMatrix = new char[height][width];
			mAttrMatrix = new int[height][width];
		}

		private State(Parcel in)
		{
			mWidth = in.readInt();
			mHeight = in.readInt();

			mCharMatrix = new char[mHeight][mWidth];
			char[] charArray = in.createCharArray();

			for (int i = 0, k = 0; i < mHeight; i++) {
				System.arraycopy(charArray, k, mCharMatrix[i], 0, mWidth);
			}

			mAttrMatrix = new int[mHeight][mWidth];
			int[] attrArray = in.createIntArray();

			for (int i = 0, k = 0; i < mHeight; i++, k += mWidth) {
				System.arraycopy(attrArray, k, mAttrMatrix[i], 0, mWidth);
			}

			mSquareCounts = in.readLong();
			mPlayTimeMillis = in.readLong();
			mLastPlayed = in.readLong();
			mSelection = in.readInt();
		}

		public int getWidth()
		{
			return mWidth;
		}

		public int getHeight()
		{
			return mHeight;
		}

		public int getSquaresSolved()
		{
			return (int) ((mSquareCounts & SCOUNT_SOLVED_MASK)
					>>> SCOUNT_SOLVED_SHIFT);
		}

		public int getSquaresCheated()
		{
			return (int) ((mSquareCounts & SCOUNT_CHEATED_MASK)
					>>> SCOUNT_CHEATED_SHIFT);
		}

		public int getSquaresWrong()
		{
			return (int) ((mSquareCounts & SCOUNT_WRONG_MASK)
					>>> SCOUNT_WRONG_SHIFT);
		}

		public int getSquareCount()
		{
			return (int) ((mSquareCounts & SCOUNT_TOTAL_MASK)
					>>> SCOUNT_TOTAL_SHIFT);
		}

		public boolean isCompleted()
		{
			return getSquaresSolved() + getSquaresCheated() >= getSquareCount();
		}

		private void setSquareStats(int solved, int cheated, int wrong,
									int count)
		{
			mSquareCounts = (((long) solved << SCOUNT_SOLVED_SHIFT) & SCOUNT_SOLVED_MASK)
					| (((long) cheated << SCOUNT_CHEATED_SHIFT) & SCOUNT_CHEATED_MASK)
					| (((long) wrong << SCOUNT_WRONG_SHIFT) & SCOUNT_WRONG_MASK)
					| (((long) count << SCOUNT_TOTAL_SHIFT) & SCOUNT_TOTAL_MASK);
		}

		public long getPlayTimeMillis()
		{
			return mPlayTimeMillis;
		}

		public void setPlayTimeMillis(long millis)
		{
			mPlayTimeMillis = millis;
		}

		public long getLastPlayed()
		{
			return mLastPlayed;
		}

		public void setLastPlayed(long lastPlayed)
		{
			mLastPlayed = lastPlayed;
		}

		public int getSelectedDirection()
		{
			return (mSelection & SEL_DIR_MASK) >> SEL_DIR_SHIFT;
		}

		public int getSelectedNumber()
		{
			return (mSelection & SEL_NUMBER_MASK) >> SEL_NUMBER_SHIFT;
		}

		public int getSelectedCell()
		{
			return (mSelection & SEL_CHAR_MASK) >> SEL_CHAR_SHIFT;
		}

		public void setSelection(int direction, int number, int cell)
		{
			mSelection = ((direction << SEL_DIR_SHIFT) & SEL_DIR_MASK)
					| ((number << SEL_NUMBER_SHIFT) & SEL_NUMBER_MASK)
					| ((cell << SEL_CHAR_SHIFT) & SEL_CHAR_MASK);
		}

		public boolean hasSelection()
		{
			return mSelection != 0;
		}

		public char charAt(int row, int column)
		{
			return mCharMatrix[row][column];
		}

		public void setCharAt(int row, int column, char ch)
		{
			mCharMatrix[row][column] = ch;
		}

		public boolean cheatedAt(int row, int column)
		{
			return isFlagSet(FLAG_CHEATED, row, column);
		}

		public void setCheatedAt(int row, int column, boolean cheated)
		{
			setFlag(FLAG_CHEATED, row, column, cheated);
		}

		public boolean markedAt(int row, int column)
		{
			return isFlagSet(FLAG_MARKED, row, column);
		}

		public void setMarkedAt(int row, int column, boolean cheated)
		{
			setFlag(FLAG_MARKED, row, column, cheated);
		}

		private boolean isFlagSet(int flag, int row, int column)
		{
			return (mAttrMatrix[row][column] & flag) == flag;
		}

		private void setFlag(int flag, int row, int column, boolean set)
		{
			if (set) {
				mAttrMatrix[row][column] |= flag;
			} else {
				mAttrMatrix[row][column] &= ~flag;
			}
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			char[] charArray = new char[mHeight * mWidth];
			for (int i = 0, k = 0; i < mHeight; i++, k += mWidth) {
				System.arraycopy(mCharMatrix[i], 0, charArray, k, mWidth);
			}

			int[] attrArray = new int[mHeight * mWidth];
			for (int i = 0, k = 0; i < mHeight; i++, k += mWidth) {
				System.arraycopy(mAttrMatrix[i], 0, attrArray, k, mWidth);
			}

			dest.writeInt(mWidth);
			dest.writeInt(mHeight);
			dest.writeCharArray(charArray);
			dest.writeIntArray(attrArray);
			dest.writeLong(mSquareCounts);
			dest.writeLong(mPlayTimeMillis);
			dest.writeLong(mLastPlayed);
			dest.writeInt(mSelection);
		}
	}

	public static class Cell
			implements Parcelable
	{
		private static final char[] EMPTY_CHAR = new char[] { '\0' };

		public static final int ATTR_CIRCLED = 1;

		public static final Creator<Cell> CREATOR = new Creator<Cell>()
		{
			public Cell createFromParcel(Parcel in)
			{
				return new Cell(in);
			}

			public Cell[] newArray(int size)
			{
				return new Cell[size];
			}
		};

		char[] mChars;
		byte mAttrFlags;

		Cell()
		{
			mChars = EMPTY_CHAR;
			mAttrFlags = 0;
		}

		private Cell(Parcel in)
		{
			mChars = in.createCharArray();
			mAttrFlags = in.readByte();
		}

		public char charAt(int pos)
		{
			return mChars[pos];
		}

		public boolean isEmpty()
		{
			return mChars == EMPTY_CHAR;
		}

		public boolean isCircled()
		{
			return (mAttrFlags & ATTR_CIRCLED) == ATTR_CIRCLED;
		}

		public boolean contains(char charSought)
		{
			for (char ch: mChars) {
				if (charSought == ch) {
					return true;
				}
			}

			return false;
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeCharArray(mChars);
			dest.writeByte(mAttrFlags);
		}

		@Override
		public String toString()
		{
			if (mChars.length == 1) {
				return String.valueOf(mChars[0]);
			}

			return "[" + String.valueOf(mChars) + "]";
		}
	}

	public static class Word
			implements Parcelable
	{
		public static class Builder
		{
			private int mNumber;
			private String mHint;
			private int mStartRow;
			private int mStartColumn;
			private int mDir;
			private String mHintUrl;
			private String mCitation;
			private List<Cell> mCells;

			public Builder()
			{
				mCells = new ArrayList<>();
			}

			public Builder setNumber(int number)
			{
				mNumber = number;
				return this;
			}

			public Builder setHint(String hint)
			{
				mHint = hint;
				return this;
			}

			public Builder setStartRow(int startRow)
			{
				mStartRow = startRow;
				return this;
			}

			public Builder setStartColumn(int startColumn)
			{
				mStartColumn = startColumn;
				return this;
			}

			public Builder setDirection(int dir)
			{
				mDir = dir;
				return this;
			}

			public Builder setHintUrl(String hintUrl)
			{
				mHintUrl = hintUrl;
				return this;
			}

			public Builder setCitation(String citation)
			{
				mCitation = citation;
				return this;
			}

			public Builder addCell(char ch)
			{
				return addCell(new char[] { ch }, 0);
			}

			public Builder addCell(char[] chars, int attrFlags)
			{
				Cell cell = new Cell();
				cell.mAttrFlags = (byte) attrFlags;

				cell.mChars = new char[chars.length];
				System.arraycopy(chars, 0, cell.mChars, 0, chars.length);

				mCells.add(cell);
				return this;
			}

			public Word build()
			{
				Word word = new Word();
				word.mNumber = mNumber;
				word.mHint = mHint;
				word.mStartRow = mStartRow;
				word.mStartColumn = mStartColumn;
				word.mDirection = mDir;
				word.mHintUrl = mHintUrl;
				word.mCitation = mCitation;

				word.mCells = new Cell[mCells.size()];
				mCells.toArray(word.mCells);

				return word;
			}
		}

		public static final int DIR_ACROSS = 0;
		public static final int DIR_DOWN   = 1;

		private static final Cell[] EMPTY_CELL = new Cell[] {};

		public static final Creator<Word> CREATOR = new Creator<Word>()
		{
			public Word createFromParcel(Parcel in)
			{
				return new Word(in);
			}

			public Word[] newArray(int size)
			{
				return new Word[size];
			}
		};

		int mNumber;
		String mHint;
		int mStartRow;
		int mStartColumn;
		int mDirection;
		String mHintUrl;
		String mCitation;
		Cell[] mCells;

		Word()
		{
			mCells = EMPTY_CELL;
		}

		private Word(Parcel in)
		{
			mNumber = in.readInt();
			mHint = in.readString();
			mStartRow = in.readInt();
			mStartColumn = in.readInt();
			mDirection = in.readInt();
			mHintUrl = in.readString();
			mCitation = in.readString();

			Parcelable[] p = in.readParcelableArray(Cell.class.getClassLoader());
			mCells = new Cell[p.length];
			System.arraycopy(p, 0, mCells, 0, p.length);
		}

		public int getNumber()
		{
			return mNumber;
		}

		public String getHint()
		{
			return mHint;
		}

		public int getStartRow()
		{
			return mStartRow;
		}

		public int getStartColumn()
		{
			return mStartColumn;
		}

		public int getLength()
		{
			return mCells.length;
		}

		public String getHintUrl()
		{
			return mHintUrl;
		}

		public String getCitation()
		{
			return mCitation;
		}

		public Cell cellAt(int pos)
		{
			return mCells[pos];
		}

		public int getDirection()
		{
			return mDirection;
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeInt(mNumber);
			dest.writeString(mHint);
			dest.writeInt(mStartRow);
			dest.writeInt(mStartColumn);
			dest.writeInt(mDirection);
			dest.writeString(mHintUrl);
			dest.writeString(mCitation);
			dest.writeParcelableArray(mCells, 0);
		}

		public static boolean equals(Word one, Word two)
		{
			if (one == null || two == null) {
				return false;
			}

			return one.mDirection == two.mDirection
					&& one.mNumber == two.mNumber;
		}

		@Override
		public boolean equals(Object o)
		{
			return (o instanceof Word) && equals(this, (Word) o);
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append(mNumber);
			sb.append(" ");
			sb.append(mDirection == DIR_ACROSS
					? "Across"
					: mDirection == DIR_DOWN
					? "Down"
					: "????");
			sb.append(": ");
			sb.append(mHint);

			if (mCells != null) {
				sb.append(" (");
				for (Cell cell: mCells) {
					sb.append(cell);
				}
				sb.append(")");
			}

			return sb.toString();
		}
	}
}
