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

import org.akop.ararat.io.CrosswordCompilerFormatter
import org.akop.ararat.io.WSJFormatter
import org.akop.ararat.io.WSJFormatter.Companion.PUBLISH_DATE_FORMAT
import org.junit.Test


class TestCcFormatter: BaseTest() {

    val crossword = loadResource("res/la180519.xml", CrosswordCompilerFormatter())

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
                squareCount = 192,
                title = "LA Times, Sat, May 19, 2018",
                flags = 0,
                description = null,
                author = "Jeff Chen / Ed. Rich Norris",
                copyright = "© 2018 Tribune Content Agency, LLC",
                comment = null,
                date = 0,
                hash = "3613172642e2ab572d1dc5a127b7c7a43f9fbf36")
        val layout = arrayOf(
                "-WRITE--JETSETS",
                "SHANIA-DATAPLAN",
                "TITTER-JIUJITSU",
                "UTTER-FOLD-TOMB",
                "PEAL-HAKEEM-RAS",
                "ELI-EMBED-ATON-",
                "FILMNOIR-JLO---",
                "YESORNO-JEALOUS",
                "---JAG-JURYLIST",
                "-JPOP-JUNKS-LBO",
                "GOA-TBOLTS-AGOG",
                "RISK-ALIA-ATARI",
                "INSOMNIA-ALLUDE",
                "PIECAKEN-STAGES",
                "SNLHOST--HOSER-")
    }
}
