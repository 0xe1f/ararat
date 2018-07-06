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
import java.io.InputStream
import java.io.ObjectInputStream


class CrosswordStateReader @Throws(IOException::class) constructor(stream: InputStream) : Closeable {

    private val inStream = ObjectInputStream(stream)

    @Throws(IOException::class)
    fun read(): CrosswordState {
        if (inStream.readInt() != CrosswordStateWriter.MAGIC_NUMBER) {
            throw IllegalArgumentException("Magic number mismatch")
        }

        val version = inStream.readByte().toInt()
        if (version > CrosswordStateWriter.VERSION_CURRENT) {
            throw IllegalArgumentException("State version $version not supported")
        }

        return readState(version)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readState(version: Int): CrosswordState {
        val width = inStream.readInt()
        val height = inStream.readInt()

        val state = CrosswordState(width, height)

        if (version <= 2) {
            val squareCounts = inStream.readLong()
            state.squaresSolved = (squareCounts and -0x1000000000000L).ushr(48).toShort()
            state.squaresCheated = (squareCounts and 0xffff00000000L).ushr(32).toShort()
            state.squaresWrong = (squareCounts and 0xffff0000L).ushr(16).toShort()
            state.squaresUnknown = 0
            state.squareCount = (squareCounts and 0xffffL).toShort()
        } else {
            state.squaresSolved = inStream.readShort()
            state.squaresCheated = inStream.readShort()
            state.squaresWrong = inStream.readShort()
            state.squaresUnknown = inStream.readShort()
            state.squareCount = inStream.readShort()
        }

        state.playTimeMillis = inStream.readLong()
        state.lastPlayed = inStream.readLong()
        state.selection = inStream.readInt()

        if (version <= 1) {
            (inStream.readObject() as CharArray)
                    .forEachIndexed { i, c ->
                        state.charMatrix[i / width][i % width] = if (c != '\u0000') c.toString() else null }
        } else {
            @Suppress("UNCHECKED_CAST")
            (inStream.readObject() as Array<String?>)
                    .forEachIndexed { i, c ->
                        state.charMatrix[i / width][i % width] = c }
        }

        (inStream.readObject() as IntArray)
                .forEachIndexed { i, a -> state.attrMatrix[i / width][i % width] = a }

        return state
    }

    @Throws(IOException::class)
    override fun close() { inStream.close() }
}
