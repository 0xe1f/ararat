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

import android.util.Base64
import java.io.InputStream

/**
 * A stream that decodes Base64 content as it reads.
 */
class Base64DecoderStream(
        private val inputStream: InputStream): InputStream() {

    private val encBuf = ByteArray(4)
    private val decBuf = ByteArray(4)
    private var dp = 0
    private var dl = 0

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var left = len
        var woff = off
        val copyLen = minOf(dl - dp, len)
        if (copyLen > 0) {
            // Copy any content available in the buffer
            System.arraycopy(decBuf, dp, b, woff, copyLen)
            woff += copyLen
            left -= copyLen
            dp += copyLen
        }
        // Read remainder to closest 4-byte aligned chunk
        val enc = ByteArray(left - left % 4)
        val nread = inputStream.read(enc)
        if (nread == -1) {
            return -1
        }
        // Decode content
        val dec = Base64.decode(enc, 0, nread, Base64.NO_WRAP)
        System.arraycopy(dec, 0, b, woff, dec.size)

        // Return length of decoded content + length copied from buffer
        return dec.size + copyLen
    }

    override fun read(): Int {
        if (dp < dl) {
            // Have some unread bytes in buffer
            return decBuf[dp++].toInt()
        }

        // Read 4 bytes at once
        val len = inputStream.read(encBuf, 0, encBuf.size)
        if (len == -1) {
            return -1
        }

        // Decode content and reset dp/dl
        val dec = Base64.decode(encBuf, 0, len, Base64.NO_WRAP)
        dp = 0
        dl = dec.size

        // Copy to internal buffer & return first byte
        System.arraycopy(dec, 0, decBuf, dp, dl)
        return decBuf[dp++].toInt()
    }
}
