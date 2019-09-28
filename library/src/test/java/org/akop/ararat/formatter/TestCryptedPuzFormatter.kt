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

package org.akop.ararat.formatter

import org.akop.ararat.core.buildCrossword
import org.akop.ararat.io.FormatException
import org.akop.ararat.io.PuzFormatter
import org.junit.Test
import java.io.File


class TestCryptedPuzFormatter: BaseTest() {

    val crossword = PuzFormatter().load("crypted.puz")

    @Test
    fun crossword_testKey() {
        // FIXME: read file stream to RAM and use a RAM-based inputstream
        for (key in 0..9999) {
            val success = try {
                File(root, "crypted.puz").inputStream().use { s ->
                    buildCrossword { PuzFormatter().readWithKey(this, s, key) } }
                true
            } catch (e: FormatException) {
                false
            }

            assert((key == actualKey) == success)
        }
    }

    @Test
    fun crossword_testLayout() {
        assertLayout(crossword, Array(charMap.size) { row ->
            charMap[row].chunked(1).map { when (it) { "#" -> null else -> it } }.toTypedArray()
        })
    }

    companion object {
        val actualKey = 2949
        val charMap = arrayOf(
                "WHEN#CANWE#MAYI",
                "WACO#BLAHS#INOT",
                "WHOWASTHAT#DYKE",
                "#ALOT###TAPROOM",
                "###READY#SHIN##",
                "FRAS#LOOS#OBEYS",
                "LIRE#TRUEST#HOW",
                "AGE#WHERETO#OUI",
                "WHY#ROMANI#IMIN",
                "STORY#INTL#NENE",
                "##UIES#GOTIT###",
                "YESORNO###DRAC#",
                "ABUT#ARTTHOUNOT",
                "RARE#CLEAR#SEMI",
                "DYED#KYLES#TWOS")
    }
}
