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

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * The [CrosswordFormatter] interface provides a means to read and write
 * crosswords to/from streams.
 */
interface CrosswordFormatter {

    /**
     * Sets the stream [encoding] for subsequent reads/writes. Almost all
     * formatters are expected to have a default encoding, and not all will
     * support all encodings, due to format restrictions or other reasons.
     */
    fun setEncoding(encoding: String)

    /**
     * Reads contents of an [inputStream] into a [builder]. Check [canRead]
     * to ensure reading is supported.
     */
    @Throws(IOException::class)
    fun read(builder: Crossword.Builder, inputStream: InputStream)

    /**
     * Writes contents of a [crossword] to an [outputStream]. Check [canWrite]
     * to ensure reading is supported.
     */
    @Throws(IOException::class)
    fun write(crossword: Crossword, outputStream: OutputStream)

    /**
     * Returns true if the formatter supports reading from a stream.
     */
    fun canRead(): Boolean

    /**
     * Returns true if the formatter supports writing to a stream.
     */
    fun canWrite(): Boolean
}
