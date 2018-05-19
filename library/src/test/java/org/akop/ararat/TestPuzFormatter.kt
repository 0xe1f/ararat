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

import org.akop.ararat.io.PuzFormatter
import org.junit.Test
import org.junit.Assert


class TestPuzFormatter: BaseTest() {

    @Test
    fun crossword_testReadWrite() {
        val crossword = loadResource("res/puzzle.puz", PuzFormatter())
//        val crossword = loadResource("./res/puzzle.puz", PuzFormatter())
//        val crossword = loadResource("/res/puzzle.puz", PuzFormatter())

        Assert.assertEquals("Width mismatch!", crossword.width, 15)
        Assert.assertEquals("Height mismatch!", crossword.height, 15)
        Assert.assertEquals("SquareCount mismatch!", crossword.squareCount, 191)
        Assert.assertEquals("Title mismatch!", crossword.title, "Double A's")
        Assert.assertNull("Description mismatch!", crossword.description)
        Assert.assertEquals("Author mismatch!", crossword.author, "Ben Tausig")
        Assert.assertEquals("Copyright mismatch!", crossword.copyright, "")
        Assert.assertEquals("Comment mismatch!", crossword.comment, "")
        Assert.assertEquals("Date mismatch!", crossword.date, 0)
        Assert.assertEquals("Hash mismatch!", crossword.hash, "8d205cdebc52f5b65fd8b3ff5f6962fcfe211a01")
    }
}
