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
import org.akop.ararat.core.buildWord

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.ArrayList


class PzzlFormatter : CrosswordFormatter {

    private var encoding = DEFAULT_ENCODING

    override fun setEncoding(encoding: String) {
        this.encoding = encoding
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val reader = InputStreamReader(inputStream, encoding).buffered()

        var cellMap: Array<Array<Cell?>>? = null
        var row = 0
        val hintsAcross = ArrayList<String>()
        val hintsDown = ArrayList<String>()
        var section = 0

        outer@
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) {
                section++
                continue
            }

            when (section) {
                SECTION_TITLE -> builder.title = line
                SECTION_AUTHOR -> builder.author = line
                SECTION_WIDTH -> {} // Not actual width
                SECTION_HEIGHT -> builder.height = line.toInt() // Not actual height
                SECTION_MAP -> {
                    if (builder.height < 1) throw IndexOutOfBoundsException("Height not set")

                    var i = 0

                    if (row == 0) {
                        builder.width = 0
                        while (i <= line.lastIndex) {
                            var ch = line[i]
                            while (ch == ',' && i <= line.lastIndex) {
                                ch = line[i]
                                i += 2
                            }

                            if (ch != '.' && ch != '%') builder.width++
                            i++
                        }

                        if (builder.width == 0) continue@outer

                        cellMap = Array(builder.height) { arrayOfNulls<Cell?>(builder.width) }
                    }

                    var cell: Cell? = null
                    var p = 0
                    i = -1

                    inner@
                    while (++i <= line.lastIndex) {
                        val ch = line[i]
                        when (ch) {
                            '.' -> continue@inner
                            '#' -> {}
                            '%' -> {
                                cell = Cell(attrs = Crossword.Cell.ATTR_CIRCLED)
                                continue@inner
                            }
                            else -> {
                                cell = cell ?: Cell()

                                // Determine the count of chars for cell
                                var charCount = 1
                                var j = i + 1
                                while (j <= line.lastIndex && line[j] == ',') {
                                    charCount++
                                    j += 2
                                }

                                // Copy the chars
                                val chars = CharArray(charCount)
                                (0 until charCount).forEach {
                                    chars[it] = line[i + it * 2]
                                }
                                cell.chars = String(chars)

                                // Advance the index
                                i += (charCount - 1) * 2
                            }
                        }

                        cellMap!![row][p++] = cell
                        cell = null
                    }

                    row++
                }
                SECTION_ACROSS -> hintsAcross += line
                SECTION_DOWN -> hintsDown += line
            }
        }

        // Complete word information given the 2D map
        mapOutWords(builder, hintsAcross, hintsDown, cellMap!!)
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = false

    private fun mapOutWords(cb: Crossword.Builder,
                            hintsAcross: List<String>,
                            hintsDown: List<String>,
                            cellMap: Array<Array<Cell?>>) {
        var acrossIndex = 0
        var downIndex = 0
        var number = 0
        var actualHeight = 0

        (0..cellMap.lastIndex).forEach { i ->
            var allEmpty = true
            (0..cellMap[i].lastIndex).forEach inner@ { j ->
                if (cellMap[i][j] == null) return@inner

                allEmpty = false
                var incremented = false
                if ((j == 0 || j > 0 && cellMap[i][j - 1] == null)
                        && j < cellMap[i].lastIndex
                        && cellMap[i][j + 1] != null) {
                    // Start of a new Across word
                    number++
                    incremented = true

                    cb.words += buildWord {
                        direction = Crossword.Word.DIR_ACROSS
                        hint = hintsAcross[acrossIndex++]
                        this.number = number
                        startRow = i
                        startColumn = j

                        // Copy contents to a temp buffer
                        for (k in j..cellMap[i].lastIndex) {
                            val cell = cellMap[i][k] ?: break
                            addCell(cell.chars, cell.attrs)
                        }
                    }
                }

                if (i == 0 || i > 0 && cellMap[i - 1][j] == null
                        && i < cellMap.lastIndex
                        && cellMap[i + 1][j] != null) {
                    // Start of a new Down word
                    if (!incremented) number++

                    cb.words += buildWord {
                        direction = Crossword.Word.DIR_DOWN
                        hint = hintsDown[downIndex++]
                        this.number = number
                        startRow = i
                        startColumn = j

                        for (k in i..cellMap.lastIndex) {
                            val cell = cellMap[k][j] ?: break
                            addCell(cell.chars, cell.attrs)
                        }
                    }
                }
            }

            if (!allEmpty) actualHeight++
        }

        cb.setHeight(actualHeight)
    }

    private class Cell(var chars: String = "",
                       var attrs: Int = 0)

    companion object {
        private const val DEFAULT_ENCODING = "Windows-1252"

        // Section offsets, 0-based
        private const val SECTION_TITLE  = 2
        private const val SECTION_AUTHOR = 3
        private const val SECTION_WIDTH  = 4
        private const val SECTION_HEIGHT = 5
        private const val SECTION_MAP    = 8
        private const val SECTION_ACROSS = 9
        private const val SECTION_DOWN   = 10
    }
}
