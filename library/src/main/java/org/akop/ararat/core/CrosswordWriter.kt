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

package org.akop.ararat.core

import java.io.Closeable
import java.io.IOException
import java.io.ObjectOutputStream
import java.io.OutputStream


class CrosswordWriter @Throws(IOException::class) constructor(stream: OutputStream) : Closeable {

    private val outStream: ObjectOutputStream = ObjectOutputStream(stream)

    @Throws(IOException::class)
    fun write(crossword: Crossword) {
        outStream.writeInt(MAGIC_NUMBER)
        outStream.writeByte(VERSION_CURRENT)

        writeCrossword(crossword)
    }

    @Throws(IOException::class)
    private fun writeCrossword(crossword: Crossword) {
        outStream.writeInt(crossword.width)
        outStream.writeInt(crossword.height)
        outStream.writeInt(crossword.squareCount)
        outStream.writeObject(crossword.title)
        outStream.writeObject(crossword.description)
        outStream.writeObject(crossword.author)
        outStream.writeObject(crossword.copyright)
        outStream.writeObject(crossword.comment)
        outStream.writeObject(crossword.alphabet.toCharArray())
        outStream.writeLong(crossword.date)
        outStream.writeInt(crossword.flags)

        outStream.writeInt(crossword.wordsAcross.size)
        crossword.wordsAcross.forEach { writeWord(it) }
        outStream.writeInt(crossword.wordsDown.size)
        crossword.wordsDown.forEach { writeWord(it) }
    }

    @Throws(IOException::class)
    private fun writeWord(word: Crossword.Word) {
        outStream.writeShort(word.number)
        outStream.writeObject(word.hint)
        outStream.writeShort(word.startRow)
        outStream.writeShort(word.startColumn)
        outStream.writeObject(word.hintUrl)
        outStream.writeObject(word.citation)
        outStream.writeInt(word.cells.size)
        word.cells.forEach { writeCell(it) }
    }

    @Throws(IOException::class)
    private fun writeCell(cell: Crossword.Cell) {
        outStream.writeByte(cell.attrFlags.toInt())
        outStream.writeObject(cell.chars)
    }

    @Throws(IOException::class)
    internal fun writeForHash(crossword: Crossword) {
        outStream.writeInt(crossword.width)
        outStream.writeInt(crossword.height)
        crossword.wordsAcross.forEach { writeWordForHash(it) }
        crossword.wordsDown.forEach { writeWordForHash(it) }
    }

    @Throws(IOException::class)
    private fun writeWordForHash(word: Crossword.Word) {
        outStream.writeShort(word.number)
        outStream.writeObject(word.hint)
        outStream.writeShort(word.startRow)
        outStream.writeShort(word.startColumn)
        word.cells.forEach {
            outStream.writeByte(it.attrFlags.toInt())
            outStream.writeObject(it.chars)
        }
    }

    @Throws(IOException::class)
    override fun close() { outStream.close() }

    companion object {
        internal const val VERSION_CURRENT = 4
        internal const val MAGIC_NUMBER = -0x21524159
    }
}
