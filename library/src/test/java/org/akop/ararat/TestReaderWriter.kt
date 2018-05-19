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

import org.akop.ararat.core.Crossword
import org.akop.ararat.core.CrosswordReader
import org.akop.ararat.core.CrosswordWriter
import org.akop.ararat.io.PuzFormatter
import org.junit.Test
import org.junit.Assert

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class TestReaderWriter: BaseTest() {

    @Test
    fun crossword_testReadWrite() {
        checkReadWrite(loadResource("res/puzzle.puz", PuzFormatter()))
    }

    private fun checkReadWrite(crossword: Crossword) {
        println("Checking writing...")

        val content = ByteArrayOutputStream().use { stream ->
            CrosswordWriter(stream).use {
                it.write(crossword)
                stream.flush()
            }
            stream.toByteArray()
        }

        println("Checking reading...")

        val cw = ByteArrayInputStream(content).use {
            CrosswordReader(it).use { it.read() }
        }

        Assert.assertEquals("Width mismatch!", crossword.width, cw.width)
        Assert.assertEquals("Height mismatch!", crossword.height, cw.height)
        Assert.assertEquals("SquareCount mismatch!", crossword.squareCount, cw.squareCount)
        Assert.assertEquals("Title mismatch!", crossword.title, cw.title)
        Assert.assertEquals("Description mismatch!", crossword.description, cw.description)
        Assert.assertEquals("Author mismatch!", crossword.author, cw.author)
        Assert.assertEquals("Copyright mismatch!", crossword.copyright, cw.copyright)
        Assert.assertEquals("Comment mismatch!", crossword.comment, cw.comment)
        Assert.assertEquals("Date mismatch!", crossword.date, cw.date)
        Assert.assertEquals("Hash mismatch!", crossword.hash, cw.hash)
    }
}