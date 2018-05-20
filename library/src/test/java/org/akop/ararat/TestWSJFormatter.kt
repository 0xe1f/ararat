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

import org.akop.ararat.io.WSJFormatter
import org.junit.Test


class TestWSJFormatter: BaseTest() {

    @Test
    fun crossword_testRead() {
        val crossword = loadResource("res/wsj.json", WSJFormatter())
        assertMetadata(crossword, Metadata(
                width = 15,
                height = 15,
                squareCount = 185,
                title = "That Smarts!",
                flags = 0,
                description = "",
                author = "By Dan Fisher/Edited by Mike Shenk",
                copyright = "The Wall Street Journal",
                comment = null,
                date = 1525676400000,
                hash = "14f52e52fb2ad08278c92aae1962f8ca270b11ec"))
    }
}
