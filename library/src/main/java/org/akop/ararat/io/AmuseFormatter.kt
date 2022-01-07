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
import org.akop.ararat.annotations.DontObfuscate
import org.akop.ararat.core.Crossword
import org.akop.ararat.core.buildWord
import org.akop.ararat.util.fromJson
import java.io.InputStream
import java.nio.charset.Charset


class AmuseFormatter : CrosswordFormatter {

    private var encoding = Charset.forName(DEFAULT_ENCODING)

    override fun setEncoding(encoding: String) {
        this.encoding = Charset.forName(encoding)
    }

    override fun read(builder: Crossword.Builder,
                      inputStream: InputStream) {
        val json = inputStream.bufferedReader(encoding).use { it.readText() }
        val doc: Doc = Gson().fromJson(json)

        builder.title = doc.title
        builder.description = doc.description
        builder.copyright = doc.copyright
        builder.author = doc.author
        builder.width = doc.w
        builder.height = doc.h
        builder.date = doc.publishTime

        val attrMap = Array(builder.height) { IntArray(builder.width) { 0 } }
        doc.cellInfos
                ?.filter { it.isCircled == true }
                ?.forEach { attrMap[it.y][it.x] = Crossword.Cell.ATTR_CIRCLED }
        doc.placedWords
                .mapTo(builder.words) { parseWord(it, attrMap) }
    }

    private fun parseWord(placedWord: PlacedWord,
                          attrMap: Array<IntArray>) = buildWord {
        direction = if (placedWord.acrossNotDown) {
            Crossword.Word.DIR_ACROSS
        } else {
            Crossword.Word.DIR_DOWN
        }
        hint = placedWord.clue.clue
        number = placedWord.clueNum
        startColumn = placedWord.x
        startRow = placedWord.y
        placedWord.word
                .toCharArray()
                .forEachIndexed { i, ch ->
                    addCell(ch, when (direction) {
                        Crossword.Word.DIR_ACROSS -> attrMap[startRow][startColumn + i]
                        Crossword.Word.DIR_DOWN -> attrMap[startRow + i][startColumn]
                        else -> 0
                    })
                }
    }

    @DontObfuscate
    private data class Doc(
            val title: String?,
            val description: String?,
            val copyright: String?,
            val author: String?,
            val w: Int,
            val h: Int,
            val publishTime: Long,
            val publishTimeZone: String?,
            val cellInfos: List<CellInfo>?,
            val placedWords: List<PlacedWord>,
    )
    @DontObfuscate
    private data class CellInfo(
            val x: Int,
            val y: Int,
            val isCircled: Boolean?,
    )
    @DontObfuscate
    private data class PlacedWord(
            val acrossNotDown: Boolean,
            val clueNum: Int,
            val x: Int,
            val y: Int,
            val clue: Clue,
            val word: String,
    )
    @DontObfuscate
    private data class Clue(
            val clue: String,
    )

    companion object {
        private const val DEFAULT_ENCODING = "UTF-8"
    }
}
