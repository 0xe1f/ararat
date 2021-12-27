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

import org.akop.ararat.collections.Queue
import org.akop.ararat.collections.deq
import org.akop.ararat.collections.enq
import java.io.InputStream

/**
 * A stream that unescapes '%u'-format JS content as it reads.
 */
class JSUnescapingStream(
        private val inputStream: InputStream): InputStream() {

    private val q = Queue<Int>()

    private fun getByte(): Int {
        if (!q.isEmpty()) {
            return q.deq()
        }
        return inputStream.read()
    }

    override fun read(): Int {
        val b = getByte()
        if (b == -1 || b != '%'.toInt()) {
            return b
        }
        q.enq(b)

        val m = inputStream.read()
        if (m == -1) {
            return q.deq()
        }
        q.enq(m)

        val mb = m == 'u'.toInt()
        val b1 = if (mb) {
            inputStream.read().also { q.enq(it) }
        } else {
            m
        }
        if (b1 == -1) {
            return q.deq()
        }

        var hb = when (b1.toChar()) {
            in 'A'..'F' -> b1 - 'A'.toInt() + 10
            in 'a'..'f' -> b1 - 'a'.toInt() + 10
            in '0'..'9' -> b1 - '0'.toInt()
            else -> return q.deq()
        }

        val b2 = inputStream.read()
        if (b2 == -1) {
            return q.deq()
        }
        q.enq(b2)

        hb = (hb shl 4) or when (b2.toChar()) {
            in 'A'..'F' -> b2 - 'A'.toInt() + 10
            in 'a'..'f' -> b2 - 'a'.toInt() + 10
            in '0'..'9' -> b2 - '0'.toInt()
            else -> return q.deq()
        }

        if (mb) {
            // multibyte, read 2 more
            val b3 = inputStream.read()
            if (b3 == -1) {
                return q.deq()
            }
            q.enq(b3)

            hb = (hb shl 4) or when (b3.toChar()) {
                in 'A'..'F' -> b3 - 'A'.toInt() + 10
                in 'a'..'f' -> b3 - 'a'.toInt() + 10
                in '0'..'9' -> b3 - '0'.toInt()
                else -> return q.deq()
            }

            val b4 = inputStream.read()
            if (b4 == -1) {
                return q.deq()
            }
            q.enq(b4)

            hb = (hb shl 4) or when (b4.toChar()) {
                in 'A'..'F' -> b4 - 'A'.toInt() + 10
                in 'a'..'f' -> b4 - 'a'.toInt() + 10
                in '0'..'9' -> b4 - '0'.toInt()
                else -> return q.deq()
            }
        }

        // Reset queue; we won't need to backtrack any longer
        q.clear()
        // Convert char to multibyte array, add pieces to queue, return head
        (hb.toChar() + "").toByteArray()
                .map { it.toInt() }
                .forEach { q.enq(it) }

        return q.deq()
    }
}
