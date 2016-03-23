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

package org.akop.ararat;

import org.akop.ararat.util.SparseArray;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class SparseArrayUnitTest
{
	@Test
	public void capacity_isCorrect() throws Exception
	{
		SparseArray<Integer> array = new SparseArray<>();

		// Insert enough items not to exceed default capacity
		for (int i = 0; i < SparseArray.DEFAULT_CAPACITY; i++) {
			array.put(i, i);
		}
		assertEquals(array.capacity(), SparseArray.DEFAULT_CAPACITY);

		// Add another item
		array.put(array.size() + 1, array.size() + 1);
		assertEquals(array.capacity(),
				SparseArray.DEFAULT_CAPACITY + SparseArray.CAPACITY_INCREMENT);
	}

	@Test
	public void size_isCorrect() throws Exception
	{
		SparseArray<Integer> array = new SparseArray<>();

		// Insert 10 items
		for (int i = 0; i < SparseArray.DEFAULT_CAPACITY; i++) {
			array.put(i, i);
		}
		assertEquals(array.size(), SparseArray.DEFAULT_CAPACITY);

		// Re-insert same items, plus some more
		int size = SparseArray.DEFAULT_CAPACITY + SparseArray.CAPACITY_INCREMENT;
		for (int i = 0; i < size; i++) {
			array.put(i, i);
		}
		assertEquals(array.size(), size);
	}

	@Test
	public void keys_ordered() throws Exception
	{
		SparseArray<Integer> array = new SparseArray<>();

		// Insert DEF_CAPACITY random items and check the ordering of the keys
		for (int r: generateRandom(SparseArray.DEFAULT_CAPACITY)) {
			array.put(r, r);
		}

		int prev = array.keyAt(0);
		for (int i = 1, n = array.size(); i < n; i++) {
			if (array.keyAt(i) <= prev) {
				fail("Key at index " + i + " not larger than previous");
			}
		}

		// Insert additional items and check the ordering of the keys
		for (int r: generateRandom(SparseArray.DEFAULT_CAPACITY + SparseArray.CAPACITY_INCREMENT)) {
			array.put(r, r);
		}

		prev = array.keyAt(0);
		for (int i = 1, n = array.size(); i < n; i++) {
			if (array.keyAt(i) <= prev) {
				fail("Key at index " + i + " not larger than previous");
			}
		}
	}

	@Test
	public void keysValues_match() throws Exception
	{
		SparseArray<Integer> array = new SparseArray<>();

		// Insert 100 random items
		for (int r: generateRandom(100)) {
			array.put(r, r);
		}

		// Ensure values correspond with keys
		for (int i = 0, n = array.size(); i < n; i++) {
			if (!array.valueAt(i).equals(array.keyAt(i))) {
				fail("Key at index " + i + " does not match value");
			}
		}
	}

	@Test
	public void grow_isCorrect() throws Exception
	{
		SparseArray<Integer> array = new SparseArray<>();

		// Insert enough items to exceed default capacity
		int size = SparseArray.DEFAULT_CAPACITY + 1;
		for (int i = 0; i < size; i++) {
			array.put(i, i);
		}

		// Make sure the keys/values were preserved when the
		// capacity was incremented
		for (int i = 0, n = array.size(); i < n; i++) {
			if (array.keyAt(i) != i) {
				fail("Key at index " + i + " does not match expected value");
			}
			if (!array.valueAt(i).equals(i)) {
				fail("Value at index " + i + " does not match expected value");
			}
		}
	}

	private static int[] generateRandom(int count)
	{
		int[] items = new int[count];
		for (int i = 0; i < count; i++) {
			items[i] = i;
		}

		Random rnd = new Random(System.currentTimeMillis());
		for (int i = items.length - 1; i > 0; i--)
		{
			int index = rnd.nextInt(i + 1);
			int tmp = items[index];
			items[index] = items[i];
			items[i] = tmp;
		}

		return items;
	}
}
