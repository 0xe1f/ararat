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

import com.google.gson.Gson
import org.akop.ararat.core.Crossword
import org.akop.ararat.core.buildWord
import org.akop.ararat.util.fromJson
import org.akop.ararat.util.stripHtmlEntities

import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class NYTFormatter : CrosswordFormatter {

    private var encoding = Charset.forName(DEFAULT_ENCODING)

    override fun setEncoding(encoding: String) {
        this.encoding = Charset.forName(encoding)
    }

    override fun read(builder: Crossword.Builder,
                      inputStream: InputStream) {
        val decodedString = inputStream
                .bufferedReader(encoding)
                .use { it.readText() }

        val doc: Doc = Gson().fromJson(decodedString)
        val cellMap = doc.gamePageData.cells.associateBy { it.index }

        with (doc.gamePageData.dimensions) {
            if (rowCount < 1 || columnCount < 1) {
                throw FormatException("Puzzle has bad dimensions (${columnCount}x${rowCount}")
            }
            builder.width = columnCount
            builder.height = rowCount
        }
        with (doc.gamePageData.meta) {
            builder.author = constructors.joinToString(", ").stripHtmlEntities()
            builder.copyright = copyright.stripHtmlEntities()
            builder.comment = notes?.firstOrNull()?.text?.stripHtmlEntities()
            builder.date = PUBLISH_DATE_FORMAT.parse(publicationDate)?.time ?: 0
        }
        doc.gamePageData.clues.forEach { clue ->
            val cells = clue
                    .cells
                    .map { cellMap[it] ?: error("Cell $it not found") }
            val firstCell = cells.first()
            builder.addWord(buildWord {
                startColumn = firstCell.index % builder.width
                startRow = firstCell.index / builder.width
                number = clue.label
                direction = when (val dir = clue.direction) {
                    DIRECTION_ACROSS -> Crossword.Word.DIR_ACROSS
                    DIRECTION_DOWN -> Crossword.Word.DIR_DOWN
                    else -> error("$dir is not a valid direction")
                }
                hint = clue.text.stripHtmlEntities()
                cells.forEach { cell ->
                    addCell(cell.answer, when (cell.type) {
                        TYPE_CIRCLED -> Crossword.Cell.ATTR_CIRCLED
                        else -> 0
                    })
                }
            })
        }
    }

    private data class Doc(
            val gamePageData: GamePageData,
    )

    private data class GamePageData(
            val meta: Meta,
            val dimensions: Dimensions,
            val cells: List<Cell>,
            val clues: List<Clue>,
    )

    private data class Note(
            val text: String,
    )

    private data class Meta(
            val constructors: List<String>,
            val copyright: String,
            val notes: List<Note>?,
            val publicationDate: String,
    )

    private data class Dimensions(
            val rowCount: Int,
            val columnCount: Int,
    )

    private data class Cell(
            val type: Int,
            val answer: String,
            val index: Int,
    )

    private data class Clue(
            val cells: List<Int>,
            val direction: String,
            val label: Int,
            val text: String,
    )

    companion object {
        private const val DEFAULT_ENCODING = "UTF-8"

        private const val DIRECTION_ACROSS = "Across"
        private const val DIRECTION_DOWN   = "Down"
        private const val TYPE_CIRCLED = 2

        private val PUBLISH_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
