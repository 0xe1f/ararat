// Copyright (c) 2017 Akop Karapetyan
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale


class WSJFormatter : CrosswordFormatter {

    private var mEncoding = DEFAULT_ENCODING

    override fun setEncoding(encoding: String) {
        mEncoding = encoding
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val reader = InputStreamReader(inputStream, mEncoding)

        val sb = StringBuilder()
        var nread: Int
        val buffer = CharArray(4000)
        while ((nread = reader.read(buffer, 0, buffer.size)) > -1) {
            sb.append(buffer, 0, nread)
        }

        val obj: JSONObject
        try {
            obj = JSONObject(sb.toString())
        } catch (e: JSONException) {
            throw FormatException("Error parsing JSON object", e)
        }

        val dataObj = obj.optJSONObject("data")
                ?: throw FormatException("Missing 'data'")

        val copyObj = dataObj.optJSONObject("copy")
                ?: throw FormatException("Missing 'data.copy'")

        val gridObj = copyObj.optJSONObject("gridsize")
                ?: throw FormatException("Missing 'data.copy.gridsize'")

        builder.setTitle(copyObj.optString("title"))
        builder.setDescription(copyObj.optString("description"))
        builder.setCopyright(copyObj.optString("publisher"))
        builder.setAuthor(copyObj.optString("byline"))

        val pubString = copyObj.optString("date-publish")
        try {
            builder.setDate(PUBLISH_DATE_FORMAT.parse(pubString).time)
        } catch (e: ParseException) {
            throw FormatException("Can't parse '$pubString' as publish date")
        }

        val width = gridObj.optInt("cols")
        val height = gridObj.optInt("rows")

        builder.setWidth(width)
        builder.setHeight(height)

        readClues(builder, copyObj,
                Grid.parseJSON(dataObj.optJSONArray("grid"), width, height))
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean {
        return true
    }

    override fun canWrite(): Boolean {
        return false
    }

    private class Grid internal constructor(internal var mWidth: Int, internal var mHeight: Int) {
        internal val mSquares: Array<Array<Square>>

        init {
            mSquares = Array(mHeight) { arrayOfNulls(mWidth) }
        }

        companion object {

            internal fun parseJSON(gridArray: JSONArray?, width: Int, height: Int): Grid {
                if (gridArray == null) {
                    throw FormatException("Missing 'data.grid[]'")
                } else if (gridArray.length() != height) {
                    throw FormatException("Unexpected clues length of "
                            + gridArray.length() + " (expected " + height + ")")
                }

                val grid = Grid(width, height)
                for (i in 0 until height) {
                    val rowArray = gridArray.optJSONArray(i)
                    if (rowArray == null) {
                        throw FormatException("Missing 'data.grid[$i]'")
                    } else if (rowArray.length() != width) {
                        throw FormatException("Unexpected clues length of "
                                + rowArray.length() + " (expected " + width + ")")
                    }

                    for (j in 0 until width) {
                        grid.mSquares[i][j] = Square.parseJSON(rowArray.optJSONObject(j))
                    }
                }

                return grid
            }
        }
    }

    private class Square {
        internal var mChar: String

        companion object {

            internal fun parseJSON(squareObj: JSONObject): Square? {
                var square: Square? = null

                val letter = squareObj.optString("Letter")
                if (letter != null && !letter.isEmpty()) {
                    square = Square()
                    square.mChar = letter
                }

                return square
            }
        }
    }

    private class Word {
        internal var mId: Int = 0
        internal var mRow: Int = 0
        internal var mCol: Int = 0
        internal var mLen: Int = 0

        companion object {

            internal fun parseJSON(wordObj: JSONObject): Word {
                val word = Word()
                word.mId = wordObj.optInt("id", -1)
                if (word.mId == -1) {
                    throw FormatException("Word missing identifier")
                }

                var posStr: String
                var dashIdx: Int

                if ((posStr = wordObj.optString("x")) == null) {
                    throw FormatException("Word missing 'x'")
                }
                if ((dashIdx = posStr.indexOf('-')) != -1) {
                    word.mCol = Integer.parseInt(posStr.substring(0, dashIdx)) - 1
                    word.mLen = Integer.parseInt(posStr.substring(dashIdx + 1)) - word.mCol
                } else {
                    word.mCol = Integer.parseInt(posStr) - 1
                }

                if ((posStr = wordObj.optString("y")) == null) {
                    throw FormatException("Word missing 'y'")
                }
                if ((dashIdx = posStr.indexOf('-')) != -1) {
                    word.mRow = Integer.parseInt(posStr.substring(0, dashIdx)) - 1
                    word.mLen = Integer.parseInt(posStr.substring(dashIdx + 1)) - word.mRow
                } else {
                    word.mRow = Integer.parseInt(posStr) - 1
                }

                return word
            }
        }
    }

    companion object {
        private val DEFAULT_ENCODING = "UTF-8"
        private val PUBLISH_DATE_FORMAT = SimpleDateFormat("EEEE, d MMMM yyyy",
                Locale.US)

        private fun readClues(builder: Crossword.Builder, copyObj: JSONObject,
                              grid: Grid) {
            val cluesArray = copyObj.optJSONArray("clues")
            if (cluesArray == null) {
                throw FormatException("Missing 'data.copy.clues[]'")
            } else if (cluesArray.length() != 2) {
                throw FormatException("Unexpected clues length of '" + cluesArray.length() + "'")
            }

            val wordsArray = copyObj.optJSONArray("words")
                    ?: throw FormatException("Missing 'data.copy.words[]'")

            // We'll need this to assign x/y locations to each clue
            val words = SparseArray<Word>()
            run {
                var i = 0
                val n = wordsArray.length()
                while (i < n) {
                    val word: Word
                    try {
                        word = Word.parseJSON(wordsArray.optJSONObject(i))
                    } catch (e: Exception) {
                        throw FormatException("Error parsing 'data.copy.words[$i]'", e)
                    }

                    words.put(word.mId, word)
                    i++
                }
            }

            // Go through the list of clues
            var i = 0
            val n = cluesArray.length()
            while (i < n) {
                val clueObj = cluesArray.optJSONObject(i)
                        ?: throw FormatException("'data.copy.clues[$i]' is null")

                val subcluesArray = clueObj.optJSONArray("clues")
                        ?: throw FormatException("Missing 'data.copy.clues[$i].clues'")

                val dir: Int
                val clueDir = clueObj.optString("title")
                if ("Across".equals(clueDir, ignoreCase = true)) {
                    dir = Crossword.Word.DIR_ACROSS
                } else if ("Down".equals(clueDir, ignoreCase = true)) {
                    dir = Crossword.Word.DIR_DOWN
                } else {
                    throw FormatException("Invalid direction: '$clueDir'")
                }

                var j = 0
                val o = subcluesArray.length()
                while (j < o) {
                    val subclue = subcluesArray.optJSONObject(j)
                    val word = words.get(subclue.optInt("word", -1))
                            ?: throw FormatException("No matching word for clue at 'data.copy.clues[$i].clues[$j].word'")

                    val wb = Crossword.Word.Builder()
                            .setDirection(dir)
                            .setHint(subclue.optString("clue"))
                            .setNumber(subclue.optInt("number"))
                            .setStartColumn(word.mCol)
                            .setStartRow(word.mRow)

                    if (dir == Crossword.Word.DIR_ACROSS) {
                        var k = word.mCol
                        var l = 0
                        while (l < word.mLen) {
                            val square = grid.mSquares[word.mRow][k]
                                    ?: throw FormatException("grid["
                                            + word.mRow + "][" + k + "] is null (it shouldn't be)")
                            wb.addCell(square.mChar, 0)
                            k++
                            l++
                        }
                    } else {
                        var k = word.mRow
                        var l = 0
                        while (l < word.mLen) {
                            val square = grid.mSquares[k][word.mCol]
                                    ?: throw FormatException("grid["
                                            + k + "][" + word.mCol + "] is null (it shouldn't be)")
                            wb.addCell(square.mChar, 0)
                            k++
                            l++
                        }
                    }

                    builder.addWord(wb.build())
                    j++
                }
                i++
            }
        }
    }
}
