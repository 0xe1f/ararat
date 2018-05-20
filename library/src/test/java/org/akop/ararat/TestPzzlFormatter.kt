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

package org.akop.ararat

import org.akop.ararat.io.PzzlFormatter
import org.junit.Test


class TestPzzlFormatter: BaseTest() {

    val crossword = loadResource("res/nytsyncrossword.pzzl", PzzlFormatter())

    @Test
    fun crossword_testMetadata() {
        assertMetadata(crossword, metadata)
    }

    @Test
    fun crossword_testLayout() {
        assertLayout(crossword, Array(layout.size) {
            layout[it].chunked(1).map { when (it) { "#" -> null else -> it } }.toTypedArray()
        })
    }

    companion object {
        val metadata = Metadata(
                width = 15,
                height = 15,
                squareCount = 193,
                title = "NY Times, Wed, May 18, 2016",
                flags = 0,
                description = null,
                author = "Timothy Polin / Will Shortz",
                copyright = null,
                comment = null,
                date = 0,
                hash = "b789ce239187737dca1d81dd6b3b96c1628397b0")
        val layout = arrayOf(
                "BATS#OHYES#GIST",
                "EURO#PAULA#ALPO",
                "STEPHENKINGBOOK",
                "TOERING#TKO#VIE",
                "###ATL#SEINFELD",
                "IRONSIDE#NEAP##",
                "DOYOUNEED##BANS",
                "IDS#PENTADS#ROI",
                "GETS##SOMETHING",
                "##EATS#INSEASON",
                "PARTYHAT#SRI###",
                "ARC#PUP#MEERKAT",
                "PERSONALPRONOUN",
                "ESAU#TREAT#ECRU",
                "ROBE#STOAS#THAT")
    }
}
