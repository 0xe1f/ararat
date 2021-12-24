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

// Adapted from
//     https://github.com/rufushuang/lz-string4java
// ... which was adapted from
//     https://github.com/pieroxy/lz-string
class LZStringDecompressor {

    fun decompress(stream: DataInputStream): String? {
        if (!stream.canRead()) {
            return null
        }

        val dictionary = mutableMapOf<Int, String?>()
        (0..2).associateWithTo(dictionary, { codeToString(it) })

        var enlargeIn = 4
        var dictSize = 4
        var numBits = 3
        val sb = StringBuilder()
        var w: String?
        var bits = 0
        var resb: Int
        var maxpower: Int = 1 shl 2
        var power = 1
        var c: String? = null
        var v: Int = stream.read()
        var pos: Int = RESET_VALUE
        while (power != maxpower) {
            resb = v and pos
            pos = pos shr 1
            if (pos == 0) {
                pos = RESET_VALUE
                v = stream.read()
            }
            bits = bits or (if (resb > 0) 1 else 0) * power
            power = power shl 1
        }
        when (bits) {
            0 -> {
                bits = 0
                maxpower = 1 shl 8
                power = 1
                while (power != maxpower) {
                    resb = v and pos
                    pos = pos shr 1
                    if (pos == 0) {
                        pos = RESET_VALUE
                        v = stream.read()
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = codeToString(bits)
            }
            1 -> {
                bits = 0
                maxpower = 1 shl 16
                power = 1
                while (power != maxpower) {
                    resb = v and pos
                    pos = pos shr 1
                    if (pos == 0) {
                        pos = RESET_VALUE
                        v = stream.read()
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = codeToString(bits)
            }
            2 -> return ""
        }
        dictionary[3] = c
        w = c
        sb.append(w)
        while (stream.canRead()) {
            bits = 0
            maxpower = 1 shl numBits
            power = 1
            while (power != maxpower) {
                resb = v and pos
                pos = pos shr 1
                if (pos == 0) {
                    pos = RESET_VALUE
                    v = stream.read()
                }
                bits = bits or (if (resb > 0) 1 else 0) * power
                power = power shl 1
            }
            var cc: Int = bits
            when (bits) {
                0 -> {
                    bits = 0
                    maxpower = 1 shl 8
                    power = 1
                    while (power != maxpower) {
                        resb = v and pos
                        pos = pos shr 1
                        if (pos == 0) {
                            pos = RESET_VALUE
                            v = stream.read()
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary[dictSize++] = codeToString(bits)
                    cc = dictSize - 1
                    enlargeIn--
                }
                1 -> {
                    bits = 0
                    maxpower = 1 shl 16
                    power = 1
                    while (power != maxpower) {
                        resb = v and pos
                        pos = pos shr 1
                        if (pos == 0) {
                            pos = RESET_VALUE
                            v = stream.read()
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary[dictSize++] = codeToString(bits)
                    cc = dictSize - 1
                    enlargeIn--
                }
                2 -> return sb.toString()
            }
            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
            val entry: String? = when {
                dictionary[cc] != null -> dictionary[cc]
                cc == dictSize -> w + w!![0]
                else -> return null
            }
            sb.append(entry)

            dictionary[dictSize++] = w + entry!![0]
            enlargeIn--
            w = entry
            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
        }
        return ""
    }

    private fun codeToString(i: Int) = i.toChar().toString()

    companion object {
        private const val RESET_VALUE = 32768
    }
}
