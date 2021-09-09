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
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset


class AmuseFormatter : CrosswordFormatter {

    private var encoding = Charset.forName(DEFAULT_ENCODING)

    override fun setEncoding(encoding: String) {
        this.encoding = Charset.forName(encoding)
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val json = inputStream.bufferedReader(encoding).use { it.readText() }
        val root = JSONObject(json)

        builder.title = root.optString("title")
        builder.description = root.optString("description")
        builder.copyright = root.optString("copyright")
        builder.author = root.optString("author")
        builder.width = root.optInt("w")
        builder.height = root.optInt("h")
        builder.date = root.optLong("publishTime")

        val attrMap = Array(builder.height) { IntArray(builder.width) { 0 } }
        root.optJSONArray("cellInfos")?.let { array ->
            (0 until array.length())
                    .map { i -> array.optJSONObject(i) }
                    .filter { it.optBoolean("isCircled", false) }
                    .forEach {
                        attrMap[it.optInt("y")][it.optInt("x")] = Crossword.Cell.ATTR_CIRCLED
                    }
        }
        root.optJSONArray("placedWords")?.let { array ->
            (0 until array.length())
                    .map { i -> array.optJSONObject(i) }
                    .mapTo(builder.words) { parseWord(it, attrMap) }
        }
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = false

    private fun parseWord(jsonWord: JSONObject,
                          attrMap: Array<IntArray>) = buildWord {
        direction = if (jsonWord.optBoolean("acrossNotDown")) {
            Crossword.Word.DIR_ACROSS
        } else {
            Crossword.Word.DIR_DOWN
        }
        hint = jsonWord.optJSONObject("clue")?.optString("clue")
        number = jsonWord.optInt("clueNum")
        startColumn = jsonWord.optInt("x")
        startRow = jsonWord.optInt("y")
        jsonWord.optString("word")
                .toCharArray()
                .forEachIndexed { i, ch ->
                    addCell(ch, when (direction) {
                        Crossword.Word.DIR_ACROSS -> attrMap[startRow][startColumn + i]
                        Crossword.Word.DIR_DOWN -> attrMap[startRow + i][startColumn]
                        else -> 0
                    })
                }
    }

    companion object {
        private const val DEFAULT_ENCODING = "UTF-8"
    }
}
