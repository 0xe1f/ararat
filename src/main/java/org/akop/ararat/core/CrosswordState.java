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


public class CrosswordState
		implements Parcelable
{
	public static final int FLAG_CHEATED = 0x01;
	public static final int FLAG_MARKED  = 0x02;

	private static final int SEL_NUMBER_MASK  = 0x0000fff;
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

	public static final Creator<CrosswordState> CREATOR = new Creator<CrosswordState>()
	{
		public CrosswordState createFromParcel(Parcel in)
		{
			return new CrosswordState(in);
		}

		public CrosswordState[] newArray(int size)
		{
			return new CrosswordState[size];
		}
	};

	int mHeight;
	int mWidth;
	long mSquareCounts;
	long mPlayTimeMillis;
	long mLastPlayed;
	int mSelection;
	String[][] mCharMatrix;
	int[][] mAttrMatrix;

	CrosswordState()
	{
	}

	public CrosswordState(CrosswordState other)
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

	CrosswordState(int width, int height)
	{
		mWidth = width;
		mHeight = height;
		mCharMatrix = new String[height][width];
		mAttrMatrix = new int[height][width];
	}

	private CrosswordState(Parcel in)
	{
		mWidth = in.readInt();
		mHeight = in.readInt();

		mCharMatrix = new String[mHeight][mWidth];
		String[] charArray = in.createStringArray();

		for (int i = 0, k = 0; i < mHeight; i++, k += mWidth) {
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
		return (int) (mSquareCounts & SCOUNT_TOTAL_MASK);
	}

	public boolean isCompleted()
	{
		return getSquaresSolved() + getSquaresCheated() >= getSquareCount();
	}

	void setSquareStats(int solved, int cheated, int wrong,
			int count)
	{
		mSquareCounts = (((long) solved << SCOUNT_SOLVED_SHIFT) & SCOUNT_SOLVED_MASK)
				| (((long) cheated << SCOUNT_CHEATED_SHIFT) & SCOUNT_CHEATED_MASK)
				| (((long) wrong << SCOUNT_WRONG_SHIFT) & SCOUNT_WRONG_MASK)
				| ((long) count & SCOUNT_TOTAL_MASK);
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
		return mSelection & SEL_NUMBER_MASK;
	}

	public int getSelectedCell()
	{
		return (mSelection & SEL_CHAR_MASK) >> SEL_CHAR_SHIFT;
	}

	public void setSelection(int direction, int number, int cell)
	{
		mSelection = ((direction << SEL_DIR_SHIFT) & SEL_DIR_MASK)
				| (number & SEL_NUMBER_MASK)
				| ((cell << SEL_CHAR_SHIFT) & SEL_CHAR_MASK);
	}

	public boolean hasSelection()
	{
		return mSelection != 0;
	}

	public String charAt(int row, int column)
	{
		return mCharMatrix[row][column];
	}

	public void setCharAt(int row, int column, String ch)
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
		String[] charArray = new String[mHeight * mWidth];
		for (int i = 0, k = 0; i < mHeight; i++, k += mWidth) {
			System.arraycopy(mCharMatrix[i], 0, charArray, k, mWidth);
		}

		int[] attrArray = new int[mHeight * mWidth];
		for (int i = 0, k = 0; i < mHeight; i++, k += mWidth) {
			System.arraycopy(mAttrMatrix[i], 0, attrArray, k, mWidth);
		}

		dest.writeInt(mWidth);
		dest.writeInt(mHeight);
		dest.writeStringArray(charArray);
		dest.writeIntArray(attrArray);
		dest.writeLong(mSquareCounts);
		dest.writeLong(mPlayTimeMillis);
		dest.writeLong(mLastPlayed);
		dest.writeInt(mSelection);
	}
}
