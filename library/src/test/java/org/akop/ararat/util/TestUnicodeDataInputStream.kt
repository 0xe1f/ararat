// Copyright (c) Akop Karapetyan
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

package org.akop.ararat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test


class TestUnicodeDataInputStream {

    private val TEST_STRING = "abcdefghijkl"
    private val stream = UnicodeDataInputStream(TEST_STRING)

    @Test
    fun givenTestStream_ensureRead() {
        var pos = 0
        while (pos < TEST_STRING.length) {
            assertEquals(TEST_STRING[pos++].toInt(), stream.read())
        }
        assertEquals(pos, TEST_STRING.length)
    }

    @Test
    fun givenTestStream_ensureCanRead() {
        var pos = 0
        while (pos++ < TEST_STRING.length) {
            assertTrue(stream.canRead())
            stream.read()
        }
        assertFalse(stream.canRead())
    }

    @Test
    fun givenTestStream_ensureEventuallyThrows() {
        assertThrows(Exception::class.java) {
            repeat(TEST_STRING.length + 1) { stream.read() }
        }
    }
}
