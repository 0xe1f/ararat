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

package org.akop.ararat.io

import org.akop.ararat.core.Crossword
import org.akop.ararat.util.SparseArray
import org.xmlpull.v1.XmlPullParser

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class CrosswordCompilerFormatter : SimpleXmlParser(), CrosswordFormatter {

    private var builder: Crossword.Builder? = null
    private val wordBuilders = SparseArray<Crossword.Word.Builder>()
    private var cells: Array<Array<CCCell?>>? = null
    private var currentWordBuilder: Crossword.Word.Builder? = null

    override fun setEncoding(encoding: String) { /* Stub */ }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        // Initialize state
        wordBuilders.clear()
        cells = null
        currentWordBuilder = null
        this.builder = builder

        parseXml(inputStream)

        wordBuilders.forEach { _, v ->
            if (v.number == Crossword.Word.Builder.NUMBER_NONE) {
                // For any words that don't have numbers, check the cells
                val cell = cells!![v.startRow][v.startColumn]!!
                if (cell.number != Crossword.Word.Builder.NUMBER_NONE) {
                    v.number = cell.number
                }
            }

            builder.words += v.build()
        }
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = false

    override fun onStartElement(path: SimpleXmlParser.SimpleXmlPath, parser: XmlPullParser) {
        super.onStartElement(path, parser)

        if (!path.startsWith("?", "rectangular-puzzle")) return

        if (path.startsWith("crossword")) {
            if (path.startsWith("grid")) {
                if (path.isEqualTo("cell")) {
                    // ?/rectangular-puzzle/crossword/grid/cell
                    parser.stringValue("solution")?.let { sol ->
                        val row = parser.intValue("y", 0) - 1
                        val column = parser.intValue("x", 0) - 1
                        val number = parser.intValue("number", Crossword.Word.Builder.NUMBER_NONE)

                        val cell = CCCell(sol, number)
                        cells!![row][column] = cell

                        parser.stringValue("background-shape")?.let {
                            cell.attr = cell.attr or Crossword.Cell.ATTR_CIRCLED
                        }
                    }
                } else if (path.isDeadEnd) {
                    // ?/rectangular-puzzle/crossword/grid
                    val width = parser.intValue("width", -1)
                    val height = parser.intValue("height", -1)

                    cells = Array(height) { arrayOfNulls<CCCell?>(width) }
                    builder!!.width = width
                    builder!!.height = height
                }
            } else if (path.isEqualTo("word")) {
                // ?/rectangular-puzzle/crossword/word
                val id = parser.intValue("id", -1)
                val xSpan = parser.stringValue("x")!!
                val ySpan = parser.stringValue("y")!!
                val xDashIndex = xSpan.indexOf("-")

                val wb = Crossword.Word.Builder()
                if (xDashIndex != -1) {
                    val startColumn = xSpan.substring(0, xDashIndex).toInt() - 1
                    val endColumn = xSpan.substring(xDashIndex + 1).toInt() - 1
                    val row = ySpan.toInt() - 1

                    // Across
                    wb.direction = Crossword.Word.DIR_ACROSS
                    wb.startRow = row
                    wb.startColumn = startColumn

                    // Build the individual characters from the char map
                    for (column in startColumn..endColumn) {
                        val cell = cells!![row][column]!!
                        wb.addCell(cell.chars, cell.attr)
                    }
                } else {
                    val yDashIndex = ySpan.indexOf("-")
                    val startRow = ySpan.substring(0, yDashIndex).toInt() - 1
                    val endRow = ySpan.substring(yDashIndex + 1).toInt() - 1
                    val column = xSpan.toInt() - 1

                    // Down
                    wb.direction = Crossword.Word.DIR_DOWN
                    wb.startRow = startRow
                    wb.startColumn = column

                    // Build the individual characters from the char map
                    for (row in startRow..endRow) {
                        val cell = cells!![row][column]!!
                        wb.addCell(cell.chars, cell.attr)
                    }
                }

                wordBuilders.put(id, wb)
            } else if (path.isEqualTo("clues", "clue")) {
                // ?/rectangular-puzzle/crossword/clues/clue
                val wordId = parser.intValue("word", -1)
                val number = parser.intValue("number", -1)

                currentWordBuilder = wordBuilders[wordId]
                currentWordBuilder!!.number = number
                currentWordBuilder!!.hintUrl = parser.stringValue("hint-url")
                currentWordBuilder!!.citation = parser.stringValue("citation")
            }
        } else if (path.isDeadEnd) {
            // ?/rectangular-puzzle
            parser.stringValue("alphabet")?.let {
                builder!!.setAlphabet(it.toCharArray().toSet())
            }
        }
    }

    override fun onTextContent(path: SimpleXmlParser.SimpleXmlPath, text: String) {
        super.onTextContent(path, text)

        if (!path.startsWith("?", "rectangular-puzzle")) return

        if (path.startsWith("metadata")) {
            when {
                path.isEqualTo("title") -> // ?/rectangular-puzzle/metadata/title
                    builder!!.title = text
                path.isEqualTo("creator") -> // ?/rectangular-puzzle/metadata/creator
                    builder!!.author = text
                path.isEqualTo("copyright") -> // ?/rectangular-puzzle/metadata/copyright
                    builder!!.copyright = text
                path.isEqualTo("description") -> // ?/rectangular-puzzle/metadata/description
                    builder!!.description = text
            }
        } else if (path.isEqualTo("crossword", "clues", "clue")) {
            // ?/rectangular-puzzle/crossword/clues/clue
            currentWordBuilder!!.hint = text
        }
    }

    private class CCCell(val chars: String,
                         val number: Int,
                         var attr: Int = 0)
}
