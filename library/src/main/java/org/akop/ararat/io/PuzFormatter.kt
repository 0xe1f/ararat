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

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.HashSet


// Based on https://code.google.com/p/puz/wiki/FileFormat
class PuzFormatter : CrosswordFormatter {

    private var encoding = DEFAULT_ENCODING

    override fun setEncoding(encoding: String) {
        this.encoding = encoding
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val reader = inputStream.reader(Charset.forName(encoding))

        // Overall checksum
        reader.ensureSkip(2)

        // Magic string
        val temp = CharArray(128)
        if (reader.read(temp, 0, MAGIC_STRING.length) != MAGIC_STRING.length)
            throw FormatException("Magic string incomplete")

        val magic = String(temp, 0, MAGIC_STRING.length)
        if (MAGIC_STRING != magic)
            throw FormatException("Magic string mismatch (got '$magic')")

        // Checksums
        reader.ensureSkip(2) // CIB checksum
        reader.ensureSkip(4) // Masked low checksum
        reader.ensureSkip(4) // Masked high checksum

        // Version
        if (reader.read(temp, 0, 4) != 4)
            throw FormatException("Version information incomplete")

        // Garbage
        reader.ensureSkip(2)

        // Scrambled checksum
        val unscrambledChecksum = reader.readShort()

        // More garbage
        reader.ensureSkip(12)

        // Width, height
        val width = reader.readByte().toInt()
        val height = reader.readByte().toInt()

        // Clue count
        val clueCount = reader.readShort().toInt()

        // Unknown
        reader.ensureSkip(2)

        // Scrambled/unscrambled
        val puzzleType = reader.readShort()

        // The layout
        val charMap = Array(height) { CharArray(width) }
        for (i in 0 until height) {
            var totalRead = 0
            val read = reader.read(charMap[i], totalRead, width - totalRead)
            while (read < width) {
                if (read < 0)
                    throw FormatException("Line $i incomplete (read $totalRead; expected $width)")
                totalRead += read
            }
        }

        // State (skip for now)
        reader.ensureSkip((width * height).toLong())

        // Title
        val title = reader.readNullTerminatedString()
        // Author
        val author = reader.readNullTerminatedString()
        // Copyright
        val copyright = reader.readNullTerminatedString()

        // Clues
        val clues = (0 until clueCount)
                .map { reader.readNullTerminatedString() }

        // Notes
        val notes = reader.readNullTerminatedString()

        // Sections
        var rebusMap: Array<IntArray>? = null
        var rebusSols: SparseArray<String>? = null
        val attrMap = Array(height) { ByteArray(width) }
        val section = CharArray(4)
        while (reader.read(section, 0, section.size) == section.size) {
            val dataLength = reader.readShort().toInt()
            reader.ensureSkip(2) // checksum FIXME: verify

            // Read the data
            val data = CharArray(dataLength)
            var dataRead = 0
            while (dataRead < dataLength) {
                val read = reader.read(data, dataRead, dataLength - dataRead)
                if (read < 0) {
                    throw FormatException("Unexpected end while reading extra data")
                }

                dataRead += read
            }

            // Handle sections
            when (String(section)) {
                "GEXT" -> {
                    var d = 0
                    for (i in 0 until height) {
                        for (j in 0 until width) {
                            attrMap[i][j] = (attrMap[i][j].toInt() or data[d++].toInt()).toByte()
                        }
                    }
                }
                "GRBS" -> {
                    rebusMap = Array(height) { IntArray(width) }
                    var r = 0
                    for (i in 0 until height) {
                        for (j in 0 until width) {
                            rebusMap[i][j] = data[r++].toInt()
                        }
                    }
                }
                "RTBL" -> {
                    rebusSols = SparseArray()
                    String(data).split(';').forEach {
                        val sep = it.indexOf(':')
                        when {
                            sep == -1 -> throw FormatException("Missing rebus delimiter")
                            sep < 1 -> throw FormatException("Invalid rebus index ($sep)")
                            sep + 1 >= it.length -> throw FormatException("Missing rebus solution")
                            else -> {
                                var ixStart = 0
                                while (it[ixStart] == ' ') ixStart++

                                val index = it.substring(ixStart, sep).toInt()
                                rebusSols[index + 1] = it.substring(sep + 1)
                            }
                        }
                    }
                }
            }

            reader.ensureSkip(1) // trailing null
        }

        if (rebusMap != null && rebusSols != null) {
            // Verify rebus solutions
            for (i in 0 until height) {
                for (j in 0 until width) {
                    val key = rebusMap[i][j]
                    if (key != 0 && rebusSols[key] == null)
                        throw FormatException("Missing rebus solution for key $key")
                }
            }
        }

        val isScrambled = puzzleType == PUZZLE_TYPE_SCRAMBLED
        val hasSolution = puzzleType != PUZZLE_TYPE_NO_SOLUTION

        // If scrambled, unscramble by brute-forcing a key (0000-9999)
        if (isScrambled && !bruteForceKey(charMap, unscrambledChecksum))
            throw FormatException("Unable to locate a key (brute-force failed)")

        builder.flags = if (hasSolution) 0 else Crossword.FLAG_NO_SOLUTION
        builder.width = width
        builder.height = height
        builder.title = title
        builder.author = author
        builder.copyright = copyright
        builder.comment = notes

        buildWords(builder, clues, charMap, attrMap, rebusMap, rebusSols, hasSolution)
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = false

    private fun buildWords(cb: Crossword.Builder,
                           clues: List<String>,
                           charMap: Array<CharArray>,
                           attrMap: Array<ByteArray>,
                           rebusMap: Array<IntArray>?,
                           rebusSolutions: SparseArray<String>?,
                           hasSolution: Boolean) {
        val alphabet = HashSet(Crossword.ALPHABET_ENGLISH)

        var clue = 0
        var number = 0
        val iend = charMap.lastIndex
        for (i in 0..iend) {
            val jend = charMap[i].lastIndex
            for (j in 0..jend) {
                if (charMap[i][j] == EMPTY) continue
                alphabet.add(charMap[i][j])

                var incremented = false
                if ((j == 0 || j > 0 && charMap[i][j - 1] == EMPTY)
                        && j + 1 < jend && charMap[i][j + 1] != EMPTY) {
                    // Start of a new Across word
                    number++
                    incremented = true

                    cb.words += buildWord {
                        direction = Crossword.Word.DIR_ACROSS
                        hint = clues[clue++]
                        this.number = number
                        startRow = i
                        startColumn = j

                        var k = j
                        while (k <= jend && charMap[i][k] != EMPTY) {
                            var attrs = 0
                            if (attrMap[i][k].toInt() and GEXT_CIRCLED.toInt() != 0)
                                attrs = attrs or Crossword.Cell.ATTR_CIRCLED
                            if (!hasSolution)
                                attrs = attrs or Crossword.Cell.ATTR_NO_SOLUTION

                            val rebus: String? = rebusMap?.let {
                                rebusSolutions?.get(it[i][k])
                            }

                            addCell(rebus ?: charMap[i][k].toString(), attrs)
                            k++
                        }
                    }
                }

                if ((i == 0 || i > 0 && charMap[i - 1][j] == EMPTY)
                        && i + 1 < iend && charMap[i + 1][j] != EMPTY) {
                    // Start of a new Down word
                    if (!incremented) number++

                    cb.words += buildWord {
                        direction = Crossword.Word.DIR_DOWN
                        hint = clues[clue++]
                        this.number = number
                        startRow = i
                        startColumn = j

                        var k = i
                        while (k <= iend && charMap[k][j] != EMPTY) {
                            var attrs = 0
                            if (attrMap[k][j].toInt() and GEXT_CIRCLED.toInt() != 0)
                                attrs = attrs or Crossword.Cell.ATTR_CIRCLED
                            if (!hasSolution)
                                attrs = attrs or Crossword.Cell.ATTR_NO_SOLUTION

                            val rebus: String? = rebusMap?.let {
                                rebusSolutions?.get(it[k][j])
                            }

                            addCell(rebus ?: charMap[k][j].toString(), attrs)
                            k++
                        }
                    }
                }
            }
        }

        cb.setAlphabet(alphabet)
    }

    private fun checksumRegion(content: CharSequence, checksum: Short): Short {
        var checksumInt = checksum.toInt() and 0xffff
        for (i in 0..content.lastIndex) {
            checksumInt = if (checksumInt and 1 != 0) {
                checksumInt.ushr(1) + 0x8000 and 0xffff
            } else {
                checksumInt ushr 1
            }
            checksumInt = checksumInt + content[i].toInt() and 0xffff
        }
        return checksumInt.toShort()
    }

    private fun toColumnMajorOrder(map: Array<CharArray>): StringBuilder {
        if (map.isEmpty()) throw IndexOutOfBoundsException("Height cannot be zero")
        return StringBuilder().apply {
            for (j in 0..map[0].lastIndex) {
                for (i in 0..map.lastIndex) {
                    if (map[i][j] != EMPTY) append(map[i][j])
                }
            }
        }
    }

    private fun mapChecksum(map: Array<CharArray>): Short =
            checksumRegion(toColumnMajorOrder(map), 0.toShort())

    private fun unshift(content: StringBuilder, pivot: Int) {
        val len = content.length
        val first = content.substring(len - pivot)
        val second = content.substring(0, len - pivot)

        val fl = first.length
        var sp = 0
        for (i in 0..content.lastIndex) {
            if (i < fl) content.setCharAt(i, first[i])
            else content.setCharAt(i, second[sp++])
        }
    }

    private fun unscrambleString(sb: StringBuilder) {
        val len = sb.length
        val mid = len / 2

        val back = StringBuilder(mid + 1)
        val front = StringBuilder(mid + 1)

        for (i in 0 until len) {
            if (i % 2 == 0) back.append(sb[i])
            else front.append(sb[i])
        }

        var bp = 0
        val fl = front.length
        for (i in 0 until len) {
            if (i < fl) sb.setCharAt(i, front[i])
            else sb.setCharAt(i, back[bp++])
        }
    }

    private fun unscramble(map: Array<CharArray>, key: Int) {
        val height = map.size
        if (height == 0) return
        val width = map[0].size

        val keyDigits = intArrayOf(
                key % 10,
                key / 10 % 10,
                key / 100 % 10,
                key / 1000 % 10)
        val input = toColumnMajorOrder(map)
        var unscrambled = StringBuilder(input)

        val len = unscrambled.length
        keyDigits.forEach { kd ->
            unscrambleString(unscrambled)
            unshift(unscrambled, kd)

            val sb = StringBuilder()
            for (i in 0 until len) {
                var code = unscrambled[i].toInt() - keyDigits[i % keyDigits.size]
                if (code < 65) code += 26
                sb.append(code.toChar())
            }

            unscrambled = sb
        }

        var k = 0
        for (j in 0 until width) {
            for (i in 0 until height) {
                if (map[i][j] != EMPTY) map[i][j] = unscrambled[k++]
            }
        }
    }

    private fun bruteForceKey(map: Array<CharArray>, unscrambledChecksum: Short): Boolean {
        val height = map.size
        if (height == 0) return false
        val width = map[0].size

        val copy = Array(height) { CharArray(width) }
        for (code in 0..9999) {
            for (i in 0 until height)
                System.arraycopy(map[i], 0, copy[i], 0, width)

            unscramble(copy, code)
            if (mapChecksum(copy) == unscrambledChecksum) {
                for (i in 0 until height)
                    System.arraycopy(copy[i], 0, map[i], 0, width)

                return true
            }
        }

        return false
    }

    private fun InputStreamReader.readByte(): Byte = read().toByte()

    private fun InputStreamReader.readShort(): Short {
        val buf = CharArray(2)
        if (read(buf, 0, 2) != 2) throw FormatException("16-bit value incomplete")

        // little-endian
        return (buf[0].toInt() or (buf[1].toInt() shl 8)).toShort()
    }

    @Throws(IOException::class)
    private fun InputStreamReader.readNullTerminatedString(): String {
        return buildString {
            val ch = CharArray(1)
            loop@ while (true) {
                val read = read(ch, 0, 1)
                when {
                    read != 1 -> throw FormatException("Unexpected end of null-terminated string")
                    ch[0] == '\u0000' -> break@loop
                }
                append(ch[0])
            }
        }
    }

    private fun InputStreamReader.ensureSkip(len: Long) {
        val skipped = skip(len)
        if (skipped != len)
            throw FormatException("Skip failed ($skipped instead of $len)")
    }

    companion object {
        private const val PUZZLE_TYPE_SCRAMBLED = 4.toShort()
        private const val PUZZLE_TYPE_NO_SOLUTION = 2.toShort()

        private const val GEXT_CIRCLED: Char = 0x80.toChar()
        private const val MAGIC_STRING = "ACROSS&DOWN\u0000"

        private const val EMPTY = '.'
        private const val DEFAULT_ENCODING = "ISO-8859-1"
    }
}
