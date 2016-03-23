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

package org.akop.ararat.util;

import java.util.Arrays;

public class SparseArray<E>
{
	public static final int DEFAULT_CAPACITY = 10;
	public static final int CAPACITY_INCREMENT = 16;

	private int[] mKeys;
	private Object[] mValues;
	private int mSize;

	public SparseArray(int capacity)
	{
		mSize = 0;
		mKeys = new int[capacity];
		mValues = new Object[capacity];
	}

	public SparseArray()
	{
		this(DEFAULT_CAPACITY);
	}

	@SuppressWarnings("unchecked")
	public E get(int key, E defaultIfNotFound)
	{
		int index = Arrays.binarySearch(mKeys, 0, mSize, key);
		if (index >= 0) {
			return (E) mValues[index];
		}

		return defaultIfNotFound;
	}

	public E get(int key)
	{
		return get(key, null);
	}

	public void put(int key, E value)
	{
		int index = Arrays.binarySearch(mKeys, 0, mSize, key);
		if (index >= 0) {
			mKeys[index] = key;
			mValues[index] = value;
		} else {
			index = ~index;
			if (mSize >= mKeys.length) {
				int newCapacity = mSize + CAPACITY_INCREMENT;
				int[] newKeys = new int[newCapacity];
				Object[] newValues = new Object[newCapacity];

				// Copy head
				System.arraycopy(mKeys, 0, newKeys, 0, index);
				System.arraycopy(mValues, 0, newValues, 0, index);
				// Copy tail
				System.arraycopy(mKeys, index, newKeys, index + 1, mSize - index);
				System.arraycopy(mValues, index, newValues, index + 1, mSize - index);

				mKeys = newKeys;
				mValues = newValues;
			} else {
				for (int i = mSize - 1; i >= index; i--) {
					mKeys[i + 1] = mKeys[i];
					mValues[i + 1] = mValues[i];
				}
			}

			mKeys[index] = key;
			mValues[index] = value;
			mSize++;
		}
	}

	public void clear()
	{
		int capacity = mKeys.length;

		mKeys = new int[capacity];
		mValues = new Object[capacity];
		mSize = 0;
	}

	public int size()
	{
		return mSize;
	}

	public int capacity()
	{
		return mKeys.length;
	}

	public int keyAt(int index)
	{
		if (index < 0 || index > mSize) {
			throw new ArrayIndexOutOfBoundsException(index);
		}

		return mKeys[index];
	}

	@SuppressWarnings("unchecked")
	public E valueAt(int index)
	{
		if (index < 0 || index > mSize) {
			throw new ArrayIndexOutOfBoundsException(index);
		}

		return (E) mValues[index];
	}

	@Override
	public String toString()
	{
		return Arrays.asList(mValues).toString();
	}
}
