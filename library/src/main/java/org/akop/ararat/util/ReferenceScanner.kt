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

package org.akop.ararat.util

import org.akop.ararat.core.Crossword

import java.util.ArrayList
import java.util.regex.Pattern


object ReferenceScanner {

    private const val WORD_ACROSS_EN = "Across"
    private const val WORD_DOWN_EN = "Down"

    private val NUMBER_FINDER = Pattern.compile("\\b(\\d{1,3})\\b")
    private val DIRECTION_FINDER_EN = Pattern.compile("((?:\\b$WORD_ACROSS_EN\\b)|(?:\\b$WORD_DOWN_EN\\b))",
            Pattern.CASE_INSENSITIVE)

    data class WordReference(val number: Int = 0,
                             val direction: Int = 0,
                             val start: Int = 0,
                             val end: Int = 0) {

        override fun toString(): String = when (direction) {
            Crossword.Word.DIR_ACROSS -> "$number Across [$start..$end]"
            Crossword.Word.DIR_DOWN -> "$number Down [$start..$end]"
            else -> "??"
        }
    }

    @JvmStatic
    fun findReferences(hint: String, crossword: Crossword): List<WordReference> {
        val refs = ArrayList<WordReference>()

        val m = NUMBER_FINDER.matcher(hint)
        while (m.find()) {
            // Find any numbers
            val number = m.group(1).toInt()
            val start = m.start(1)
            val end = m.end(1)

            // Find closest directional marker
            val m2 = DIRECTION_FINDER_EN.matcher(hint)
            if (m2.find(end)) {
                val dir = when {
                    WORD_ACROSS_EN.equals(m2.group(1), ignoreCase = true) -> Crossword.Word.DIR_ACROSS
                    else -> Crossword.Word.DIR_DOWN
                }

                // Confirm that the word exists in the crossword
                crossword.findWord(dir, number)?.let {
                    // It exists, add the reference
                    refs += WordReference(
                            number = number,
                            direction = dir,
                            start = start,
                            end = end)
                }
            }
        }

        return refs
    }
}
