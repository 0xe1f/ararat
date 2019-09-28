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

import org.akop.ararat.core.Crossword
import org.akop.ararat.io.PzzlFormatter
import org.akop.ararat.util.ReferenceScanner
import org.junit.Assert
import org.junit.Test


class TestReferenceScanner: BaseTest() {

    val crossword = PzzlFormatter().load("nytsyncrossword.pzzl")

    @Test
    fun crossword_testMetadata() {
        tests.forEach {
            val found = ReferenceScanner.findReferences(it.hint, crossword)
            Assert.assertEquals("Reference mismatch", it.expectedRefs, found)
        }
    }

    class RefTest(val hint: String,
                  val expectedRefs: List<ReferenceScanner.WordReference>)

    companion object {
        val tests = arrayOf(
                RefTest("See 31-Across",
                        listOf(ReferenceScanner.WordReference(31, Crossword.Word.DIR_ACROSS, 4, 6))),
                RefTest("See 2-Across", listOf()), // there is no 2 across
                RefTest("10 Downing Street", listOf()),
                RefTest("With 43-Across, \"What is it?\"",
                        listOf(ReferenceScanner.WordReference(43, Crossword.Word.DIR_ACROSS, 5, 7))),
//                Foo("3 Doors Down", listOf()), // FIXME: broken
                RefTest("Photo badges, e.g.", listOf()),
                RefTest("\"How's ___?\"", listOf()),
                RefTest("2 hours after sundown", listOf()),
                RefTest("View across", listOf()))
    }
}
