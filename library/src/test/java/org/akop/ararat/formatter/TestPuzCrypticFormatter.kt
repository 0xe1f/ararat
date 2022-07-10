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

import org.akop.ararat.io.PuzFormatter
import org.junit.Test


class TestPuzCrypticFormatter: BaseTest() {

    val crossword = PuzFormatter().load("animal_farm_cosmic_gerbil.puz")

    @Test
    fun crossword_testMetadata() {
        assertMetadata(crossword, metadata)
    }

    @Test
    fun crossword_testCharLayout() {
        assertLayout(crossword, Array(charMap.size) { row ->
            charMap[row].chunked(1).map { when (it) { "#" -> null else -> it } }.toTypedArray()
        })
    }

    @Test
    fun crossword_testHints() {
        assertHints(crossword, hints)
    }

    companion object {
        val metadata = Metadata(
            width = 5,
            height = 5,
            squareCount = 17,
            title = "Animal Farm",
            flags = 0,
            description = null,
            author = "Cosmic Gerbil",
            copyright = "Copyright Cosmic Gerbil, all rights reserved",
            comment = "Created on crosshare.org",
            date = 0,
            hash = "5b641df0985d9718042519543bbec4ae736c9f44",
        )
        val charMap = arrayOf(
            "OX#RA",
            "F#R#T",
            "#BAT#",
            "D#M#L",
            "OG#OW",
        )
        val hints = arrayOf(
            "1A.Beast of burden",
            "2A.Egyptian god",
            "5A.Flutter your eyelids",
            "8A.Old-school",
            "9A.Cry of pain",
            "1D.Preposition expressing a relationship between two objects",
            "3D.__-__, Star Wars vehicle taken down by Luke Skywalker",
            "4D.Volatile storage",
            "6D.Perform an action",
            "7D.Football position furthest away from RB",
        )
    }
}
