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

import org.akop.ararat.io.UClickFormatter
import org.junit.Test


class TestUClickFormatter: BaseTest() {

    val crossword = loadResource("res/univ-daily-180401.xml", UClickFormatter())

    @Test
    fun crossword_testMetadata() {
        assertMetadata(crossword, metadata)
    }

    @Test
    fun crossword_testLayout() {
        assertLayout(crossword, Array(layout.size) {
            layout[it].chunked(1).map { when (it) { "-" -> null else -> it } }.toTypedArray()
        })
    }

    companion object {
        val metadata = Metadata(
                width = 15,
                height = 15,
                squareCount = 189,
                title = "TYPE CASTING",
                flags = 0,
                description = null,
                author = "By Timothy E. Parker",
                copyright = "Universal UClick",
                comment = null,
                date = 0,
                hash = "ca07bbda63cd5e59b3a60bcc4586164f9fd597a7")
        val layout = arrayOf(
                "TAUPE-DAD-KEPIS",
                "ASPEN-OBI-ARENA",
                "STORM-DEN-TROUT",
                "KINDAFOLKSY-PRY",
                "---USA--YODELER",
                "LAKESIDE-PILE--",
                "ARI-ENEMY-DAKAR",
                "SING-TEEUP-NILE",
                "HADNT-DELIS-NON",
                "--WARP-RELOADED",
                "ELITIST--ALL---",
                "VIS-FIVEOFAKIND",
                "ETHEL-SUM-CACAO",
                "NUEVE-ERA-ELOPE",
                "APSES-TON-SINES")
    }
}
