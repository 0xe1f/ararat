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
import org.akop.ararat.io.CrosswordFormatter
import org.junit.Assert

import java.io.File


open class BaseTest {

    protected fun loadResource(path: String, formatter: CrosswordFormatter): Crossword {
        println("Loading '$path': (loader resource path: ${javaClass.classLoader.getResource(".")})")
        val resource = javaClass.classLoader.getResource(path)
        return File(resource!!.path).inputStream().use {
            Crossword.Builder().apply { formatter.read(this, it) }.build()
        }
    }

    protected fun assertMetadata(crossword: Crossword, expected: Metadata) {
        Assert.assertEquals("Width mismatch!", expected.width, crossword.width)
        Assert.assertEquals("Height mismatch!", expected.height, crossword.height)
        Assert.assertEquals("SquareCount mismatch!", expected.squareCount, crossword.squareCount)
        Assert.assertEquals("Flag mismatch!", expected.flags, crossword.flags)
        Assert.assertEquals("Title mismatch!", expected.title, crossword.title)
        Assert.assertEquals("Description mismatch!", expected.description, crossword.description)
        Assert.assertEquals("Author mismatch!", expected.author, crossword.author)
        Assert.assertEquals("Copyright mismatch!", expected.copyright, crossword.copyright)
        Assert.assertEquals("Comment mismatch!", expected.comment, crossword.comment)
        Assert.assertEquals("Date mismatch!", expected.date, crossword.date)
        Assert.assertEquals("Hash mismatch!", expected.hash, crossword.hash)
    }

    protected fun assertLayout(crossword: Crossword, expected: Array<Array<String?>>) {
        crossword.cellMap.forEachIndexed { i, actualRow ->
            val expectedRow = expected[i]
            actualRow.forEachIndexed { j, actualCell ->
                val expectedCell = expectedRow[j]
                Assert.assertEquals("Square mismatch ($i,$j)", expectedCell, actualCell?.chars)
            }
        }
    }

    protected fun dumpLayout(crossword: Crossword) {
        crossword.cellMap.forEach { row ->
            println(row.toList().map { it?.chars ?: "-" }.joinToString(""))
        }
    }
}
