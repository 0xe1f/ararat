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
import java.nio.charset.Charset


/**
 * A stream that decompresses lz-string compressed content as it reads.
 *
 * Adapted from
 *     https://github.com/rufushuang/lz-string4java
 * ... which was adapted from
 *     https://github.com/pieroxy/lz-string
 */
class LZStringDecompressorStream(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
): InputStream() {

    private val streamReader = inputStream.reader(charset)
    private val q = Queue<Int>()

    private val dictionary = mutableMapOf<Int, String>()
    private var enlargeIn = 0
    private var dictSize = 0
    private var numBits = 0
    private var bits = 0
    private var maxpower = 0
    private var resb = 0
    private var w = ""
    private var c = '\u0000'
    private var power = 0
    private var v = 0
    private var pos = 0
    private var state = 0

    init {
        resetState()
    }

    override fun read(): Int = when {
        state == STATE_INITIAL -> handleInitState()
        !q.isEmpty() -> q.deq()
        state == STATE_READING -> handleReadState()
        else -> -1
    }

    private fun handleInitState(): Int {
        v = streamReader.read()
        if (v == -1) {
            return -1
        }

        powerLoop(2)
        c = when (bits) {
            0 -> {
                powerLoop(8)
                bits.toChar()
            }
            1 -> {
                powerLoop(16)
                bits.toChar()
            }
            2 -> throw CompressionException("Decompression error (bits == $bits)")
            else -> c
        }

        dictionary[3] = c.toString()
        w = c.toString()
        state = STATE_READING

        return enqueueContent()
    }

    private fun handleReadState(): Int {
        powerLoop(numBits)
        val cc = when (bits) {
            0 -> {
                powerLoop(8)
                dictionary[dictSize++] = bits.toChar().toString()
                enlargeIn--
                dictSize - 1
            }
            1 -> {
                powerLoop(16)
                dictionary[dictSize++] = bits.toChar().toString()
                enlargeIn--
                dictSize - 1
            }
            2 -> {
                state = STATE_COMPLETE
                return -1
            }
            else -> bits
        }
        if (enlargeIn == 0) {
            enlargeIn = 1 shl numBits
            numBits++
        }
        val entry = dictionary[cc]
                ?: if (cc == dictSize) {
                    w + w[0]
                } else {
                    null
                }
                ?: throw CompressionException("Decompression failed")

        dictionary[dictSize++] = w + entry[0]
        enlargeIn--
        w = entry

        if (enlargeIn == 0) {
            enlargeIn = 1 shl numBits
            numBits++
        }

        return enqueueContent()
    }

    private fun readOrThrow(): Int {
        val v = streamReader.read()
        if (v == -1) {
            throw CompressionException("Unexpected end of stream")
        }
        return v
    }

    private fun enqueueContent(): Int {
        q.clear()
        w.toByteArray()
                .map { it.toInt() }
                .forEach { q.enq(it) }
        return q.deq()
    }

    private fun powerLoop(places: Int) {
        bits = 0
        maxpower = 1 shl places
        power = 1

        while (power != maxpower) {
            resb = v and pos
            pos = pos shr 1
            if (pos == 0) {
                pos = RESET_VALUE
                v = readOrThrow()
            }
            bits = bits or (if (resb > 0) 1 else 0) * power
            power = power shl 1
        }
    }

    private fun resetState() {
        dictionary.clear()
        (0..2).associateWithTo(dictionary) { "${it.toChar()}" }
        enlargeIn = 4
        dictSize = 4
        numBits = 3
        bits = 0
        maxpower = 1 shl 2
        resb = 0
        w = ""
        c = '\u0000'
        power = 1
        v = 0
        pos = RESET_VALUE
        state = STATE_INITIAL
    }

    companion object {
        private const val RESET_VALUE = 32768

        private const val STATE_INITIAL  = 0
        private const val STATE_READING  = 1
        private const val STATE_COMPLETE = 2
    }
}
