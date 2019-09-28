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
import org.akop.ararat.util.SparseArray
import org.akop.ararat.util.stripHtmlEntities
import org.json.JSONArray
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


class WSJFormatter : CrosswordFormatter {

    private var encoding = Charset.forName(DEFAULT_ENCODING)

    override fun setEncoding(encoding: String) {
        this.encoding = Charset.forName(encoding)
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val json = inputStream.bufferedReader(encoding).use { it.readText() }
        val obj = JSONObject(json)

        val dataObj = obj.optJSONObject("data")
                ?: throw FormatException("Missing 'data'")
        val copyObj = dataObj.optJSONObject("copy")
                ?: throw FormatException("Missing 'data.copy'")
        val pubDate = copyObj.optString("date-publish")
        val gridObj = copyObj.optJSONObject("gridsize")
                ?: throw FormatException("Missing 'data.copy.gridsize'")

        builder.width = gridObj.optInt("cols")
        builder.height = gridObj.optInt("rows")
        builder.title = copyObj.optString("title")
        builder.description = copyObj.optString("description")
        builder.copyright = copyObj.optString("publisher")
        builder.author = copyObj.optString("byline")
        builder.date = PUBLISH_DATE_FORMAT.parse(pubDate)!!.time

        val grid = Grid(builder.width, builder.height,
                dataObj.getJSONArray("grid"))

        readClues(builder, copyObj, grid)
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = false

    private fun readClues(builder: Crossword.Builder, copyObj: JSONObject, grid: Grid) {
        val cluesArray = copyObj.optJSONArray("clues")
        when {
            cluesArray == null ->
                throw FormatException("Missing 'data.copy.clues[]'")
            cluesArray.length() != 2 ->
                throw FormatException("Unexpected clues length of '${cluesArray.length()}'")
        }

        val wordsArray = copyObj.optJSONArray("words")
                ?: throw FormatException("Missing 'data.copy.words[]'")

        // We'll need this to assign x/y locations to each clue
        val words = SparseArray<Word>()
        (0 until wordsArray.length())
                .map { Word(wordsArray.optJSONObject(it)!!) }
                .forEach { words[it.id] = it }

        (0 until cluesArray.length())
                .map { cluesArray.optJSONObject(it)!! }
                .forEach {
                    val clueDir = it.optString("title")
                    val dir = when (clueDir) {
                        "Across" -> Crossword.Word.DIR_ACROSS
                        "Down" -> Crossword.Word.DIR_DOWN
                        else -> throw FormatException("Invalid direction: '$clueDir'")
                    }

                    val subcluesArray = it.optJSONArray("clues")!!
                    (0 until subcluesArray.length())
                            .map { subcluesArray.optJSONObject(it)!! }
                            .mapTo(builder.words) {
                                buildWord {
                                    val word = words[it.optInt("word", -1)]!!

                                    direction = dir
                                    hint = it.optString("clue")?.stripHtmlEntities()
                                    number = it.optInt("number")
                                    startColumn = word.column
                                    startRow = word.row

                                    if (dir == Crossword.Word.DIR_ACROSS) {
                                        (startColumn until startColumn + word.length)
                                                .map { grid.squares[startRow][it]!! }
                                                .forEach { addCell(it.char) }
                                    } else {
                                        (startRow until startRow + word.length)
                                                .map { grid.squares[it][startColumn]!! }
                                                .forEach { addCell(it.char) }
                                    }
                                }
                            }
                }
    }

    private class Grid(width: Int, height: Int, gridArray: JSONArray) {

        val squares = Array<Array<Square?>>(height) { arrayOfNulls(width) }

        init {
            if (gridArray.length() != height)
                throw FormatException("Grid length mismatch (got: ${gridArray.length()}; exp.: $height)")

            for (i in 0 until height) {
                val rowArray = gridArray.optJSONArray(i)
                when {
                    rowArray == null -> throw FormatException("Missing 'data.grid[$i]'")
                    rowArray.length() != width ->
                        throw FormatException("Grid row $i mismatch (got: ${rowArray.length()}); exp.: $width)")
                    else -> (0 until width).forEach { j ->
                        squares[i][j] = parseSquare(rowArray.optJSONObject(j))
                    }
                }
            }
        }

        private fun parseSquare(squareObj: JSONObject): Square? {
            val letter = squareObj.optString("Letter")
            return when {
                letter?.isEmpty() == false -> Square(letter)
                else -> null
            }
        }
    }

    private class Square(val char: String)

    private class Word(wordObj: JSONObject) {

        val id: Int = wordObj.optInt("id", -1)
        val row: Int
        val column: Int
        val length: Int

        init {
            if (id == -1) throw FormatException("Word missing identifier")

            val xStr: String = wordObj.optString("x")
            val xDashIdx: Int = xStr.indexOf('-')
            val yStr = wordObj.optString("y")
            val yDashIdx = yStr.indexOf('-')

            column = when {
                xDashIdx != -1 -> xStr.substring(0, xDashIdx).toInt() - 1
                else -> xStr.toInt() - 1
            }
            row = when {
                yDashIdx != -1 -> yStr.substring(0, yDashIdx).toInt() - 1
                else -> yStr.toInt() - 1
            }
            length = when {
                xDashIdx != -1 -> xStr.substring(xDashIdx + 1).toInt() - column
                yDashIdx != -1 -> yStr.substring(yDashIdx + 1).toInt() - row
                else -> 1
            }
        }
    }

    companion object {
        private const val DEFAULT_ENCODING = "UTF-8"
        internal val PUBLISH_DATE_FORMAT = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US)
    }
}
