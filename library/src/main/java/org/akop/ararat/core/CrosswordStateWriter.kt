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


class CrosswordStateWriter @Throws(IOException::class) constructor(stream: OutputStream) : Closeable {

    private val outStream = ObjectOutputStream(stream)

    @Throws(IOException::class)
    fun write(state: CrosswordState) {
        outStream.writeInt(MAGIC_NUMBER)
        outStream.writeByte(VERSION_CURRENT)

        writeState(state)
    }

    @Throws(IOException::class)
    private fun writeState(state: CrosswordState) {
        outStream.writeInt(state.width)
        outStream.writeInt(state.height)
        outStream.writeShort(state.squaresSolved.toInt())
        outStream.writeShort(state.squaresCheated.toInt())
        outStream.writeShort(state.squaresWrong.toInt())
        outStream.writeShort(state.squaresUnknown.toInt())
        outStream.writeShort(state.squareCount.toInt())
        outStream.writeLong(state.playTimeMillis)
        outStream.writeLong(state.lastPlayed)
        outStream.writeInt(state.selection)
        outStream.writeObject(state.charMatrix.flatten().toTypedArray())
        outStream.writeObject(IntArray(state.height * state.width) {
            state.attrMatrix[it / state.width][it % state.width] })
    }

    @Throws(IOException::class)
    override fun close() { outStream.close() }

    companion object {
        internal const val VERSION_CURRENT = 3
        internal const val MAGIC_NUMBER = -0x45522113
    }
}
