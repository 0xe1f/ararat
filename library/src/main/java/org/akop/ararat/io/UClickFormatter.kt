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
import org.xmlpull.v1.XmlPullParser

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.util.regex.Pattern


class UClickFormatter : SimpleXmlParser(), CrosswordFormatter {

    private var builder: Crossword.Builder? = null

    override fun setEncoding(encoding: String) { /* Stub */ }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        this.builder = builder
        parseXml(inputStream)
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean = true

    override fun canWrite(): Boolean = false

    override fun onStartElement(path: SimpleXmlParser.SimpleXmlPath, parser: XmlPullParser) {
        super.onStartElement(path, parser)

        if (path.startsWith("crossword")) {
            when {
                path.startsWith("Title") ->
                    builder!!.title = parser.urlDecodedValue("v")
                path.startsWith("Author") ->
                    builder!!.author = parser.urlDecodedValue("v")
                path.startsWith("Copyright") ->
                    builder!!.copyright = parser.urlDecodedValue("v")
                path.startsWith("Width") ->
                    builder!!.setWidth(parser.intValue("v", -1))
                path.startsWith("Height") ->
                    builder!!.setHeight(parser.intValue("v", -1))
                path.startsWith("across", "?") || path.startsWith("down", "?") ->
                    builder!!.words += parseWord(parser)
            }
        }
    }

    private fun XmlPullParser.urlDecodedValue(name: String): String? =
            stringValue(name)?.safeUrlDecode()

    private fun String.safeUrlDecode(): String? {
        val encoded = buildString {
            var start = 0
            val m = PERCENT_MATCHER.matcher(this@safeUrlDecode)
            while (m.find()) {
                append(this@safeUrlDecode, start, m.start())
                append("%25")
                start = m.end()
            }

            append(this@safeUrlDecode, start, this@safeUrlDecode.length)
        }

        return URLDecoder.decode(encoded, "UTF-8")
    }

    private fun parseWord(parser: XmlPullParser): Crossword.Word {
        val direction = when {
            parser.name.startsWith("a") -> Crossword.Word.DIR_ACROSS
            parser.name.startsWith("d") -> Crossword.Word.DIR_DOWN
            else -> throw FormatException("Unexpected word indicator: ${parser.name}")
        }

        val number = parser.intValue("cn", 0)
        if (number < 1) throw FormatException("Number '${parser.stringValue("cn")}' not valid")

        val answer = parser.stringValue("a")!!
        val cellIndex = parser.intValue("n", 0) - 1

        return buildWord {
            this.direction = direction
            this.number = number
            startRow = cellIndex / builder!!.width
            startColumn = cellIndex % builder!!.width
            hint = parser.urlDecodedValue("c")

            answer.toCharArray().forEach { addCell(it) }
        }
    }

    companion object {
        // Matches %'s that aren't URL encoded
        private val PERCENT_MATCHER = Pattern.compile("%(?![0-9A-Fa-f]{2})")
    }
}
