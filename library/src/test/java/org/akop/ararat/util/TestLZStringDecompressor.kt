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
import org.junit.Assert.assertNull
import org.junit.Test


class TestLZStringDecompressor {

    private val COMPRESSED_CONTENT =
            "\u0485\u1032\u60f2@\u6204\ua902\ucb40\u8260\u02b1\u5061\u4419\u5f20\u0000"
    private val UNCOMPRESSED_CONTENT = "HELLO FROM ALPHACROSS"
    private val RANDOM_DATA = "a\u1234bcdefg\u02b1h"
    private val decompressor = LZStringDecompressor()

    @Test
    fun givenValidCompressedContent_ensureDecompressed() {
        val stream = UnicodeDataInputStream(COMPRESSED_CONTENT)
        assertEquals(UNCOMPRESSED_CONTENT,
                decompressor.decompress(stream))
    }

    @Test
    fun givenInvalidCompressedContent_ensureNull() {
        val stream = UnicodeDataInputStream(RANDOM_DATA)
        assertNull(decompressor.decompress(stream))
    }
}
