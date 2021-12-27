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

package org.akop.ararat.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test


class TestLZStringDecompressorStream {

    @Test
    fun givenCompressedContent_ensureDecompresses() {
        val inStream = COMPRESSED_CONTENT
                .byteInputStream()
        val lzStringDecompressor = LZStringDecompressorStream(inStream)
                .reader()

        assertEquals(UNCOMPRESSED_CONTENT, buildString {
            var byte = lzStringDecompressor.read()
            while (byte != -1) {
                append(byte.toChar())
                byte = lzStringDecompressor.read()
            }
        })
    }

    @Test
    fun givenBadCompressionData_ensureThrows() {
        val inStream = RANDOM_DATA.byteInputStream()
        assertThrows(CompressionException::class.java) {
            LZStringDecompressorStream(inStream).reader().use { it.readText() }
        }
    }

    companion object {
        private val COMPRESSED_CONTENT =
                "\u0485\u1032\u60f2@\u6204\ua902\ucb50\u8ca0\u800d\u0421" +
                "\ua817\u0fea\u0400\u0000"
        private val UNCOMPRESSED_CONTENT =
                "HELLO FROM \u0531\u0580\u0561\u0580\u0561\u057f"
        private val RANDOM_DATA = "a\u1234bcdefg\u02b1h"
    }
}
