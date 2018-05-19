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

import android.util.JsonReader
import android.util.JsonWriter
import org.akop.ararat.core.Crossword
import org.akop.ararat.core.Pos
import org.akop.ararat.core.buildWord

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset


class UClickJsonFormatter : CrosswordFormatter {

    private var encoding = DEFAULT_ENCODING

    override fun setEncoding(encoding: String) {
        this.encoding = encoding
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val reader = JsonReader(inputStream.bufferedReader(Charset.forName(encoding)))

        var layout: Map<Int, Pos>? = null
        var solution: Map<Pos, String>? = null
        var acrossClues: Map<Int, String>? = null
        var downClues: Map<Int, String>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when (name) {
                "Author" -> builder.author = reader.nextString()
                "Title" -> builder.title = reader.nextString()
                "Copyright" -> builder.copyright = reader.nextString()
                "Layout" -> layout = readLayout(reader)
                "Solution" -> solution = readSolution(reader)
                "AcrossClue" -> acrossClues = readClues(reader)
                "DownClue" -> downClues = readClues(reader)
                "Width" -> builder.width = reader.nextInt()
                "Height" -> builder.height = reader.nextInt()
//                "Date" -> user = readUser(reader)
//                "Editor" -> user = readUser(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (builder.width == 0 || builder.height == 0) {
            throw FormatException("Width (${builder.width}) or height(${builder.height}) not set")
        }

        if (layout == null) throw FormatException("Missing layout")
        if (solution == null) throw FormatException("Missing solution")
        if (acrossClues == null) throw FormatException("Missing clues for Across")
        if (downClues == null) throw FormatException("Missing clues for Down")

        acrossClues.forEach { (n, hint) ->
            val start = layout[n] ?: throw FormatException("No start position for $n Across")
            builder.words += buildWord {
                this.number = n
                this.direction = Crossword.Word.DIR_ACROSS
                this.hint = hint
                this.startRow = start.r
                this.startColumn = start.c

                for (i in start.c until builder.width) {
                    val sol = solution[Pos(start.r, i)]!!
                    if (sol == " ") break
                    this.cells += Crossword.Cell(sol, 0)
                }
            }
        }

        downClues.forEach { (n, hint) ->
            val start = layout[n] ?: throw FormatException("No start position for number $n Down")
            builder.words += buildWord {
                this.number = n
                this.direction = Crossword.Word.DIR_DOWN
                this.hint = hint
                this.startRow = start.r
                this.startColumn = start.c

                for (i in start.r until builder.height) {
                    val sol = solution[Pos(i, start.c)]!!
                    if (sol == " ") break
                    this.cells += Crossword.Cell(sol, 0)
                }
            }
        }
    }

    private fun readLayout(reader: JsonReader): Map<Int, Pos> {
        val map = HashMap<Int, Pos>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.matches("Line\\d+".toRegex()) -> {
                    val row = name.substring(4).toInt() - 1
                    val numbers = reader.nextString().chunked(2) { it.toString().toInt() }
                    numbers.forEachIndexed { i, n -> map[n] = Pos(row, i) }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return map
    }

    private fun readSolution(reader: JsonReader): Map<Pos, String> {
        val map = HashMap<Pos, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.matches("Line\\d+".toRegex()) -> {
                    val row = name.substring(4).toInt() - 1
                    val letters = reader.nextString().chunked(1)
                    letters.forEachIndexed { i, s -> map[Pos(row, i)] = s }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return map
    }

    private fun readClues(reader: JsonReader): Map<Int, String> {
        val map = HashMap<Int, String>()
        reader.nextString().split("\n".toRegex()).forEach {
            val pair = it.split("\\|".toRegex(), limit = 2)
            if (pair.size == 2) map[pair[0].toInt()] = pair[1]
        }

        return map
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        val writer = JsonWriter(outputStream.writer(Charset.forName(encoding)))

        writer.beginObject()
        writer.name("Width").value(crossword.width.toString())
        writer.name("Height").value(crossword.height.toString())
        writer.name("Author").value(crossword.author)
        writer.name("Title").value(crossword.title)
        writer.name("Copyright").value(crossword.copyright)

        writer.name("AcrossClue").value(crossword.wordsAcross
                .joinToString("\n") { "${"%02d".format(it.number)}|${it.hint}" })
        writer.name("DownClue").value(crossword.wordsDown
                .joinToString("\n", postfix = "\nend\n") { "${"%02d".format(it.number)}|${it.hint}" })

        val layoutMap = Array(crossword.height, { IntArray(crossword.width) })

        writer.name("Solution").beginObject()
        val allAnswers = buildString {
            crossword.cellMap.forEachIndexed { i, row ->
                row.forEachIndexed { j, col -> if (col?.chars == null) layoutMap[i][j] = -1 }
                writer.name("Line${i + 1}").value(row.joinToString("") { it?.chars ?: " " })
                append(row.joinToString("") { it?.chars ?: "-" })
            }
        }
        writer.endObject()

        writer.name("Layout").beginObject()
        crossword.wordsAcross.forEach { layoutMap[it.startRow][it.startColumn] = it.number }
        crossword.wordsDown.forEach { layoutMap[it.startRow][it.startColumn] = it.number }
        layoutMap.forEachIndexed { i, row ->
            writer.name("Line${i + 1}").value(row.joinToString("") { "%02d".format(it) })
        }
        writer.endObject()

        writer.name("AllAnswer").value(allAnswers)
        writer.endObject()
        writer.flush()
    }

    override fun canRead(): Boolean {
        return true
    }

    override fun canWrite(): Boolean {
        return true
    }

    companion object {
        private const val DEFAULT_ENCODING = "UTF-8"
    }
}
