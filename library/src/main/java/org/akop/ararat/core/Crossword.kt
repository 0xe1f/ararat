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

import android.os.Parcel
import android.os.Parcelable
import org.akop.ararat.util.sha1
import org.akop.ararat.util.toHexString

import java.io.ByteArrayOutputStream
import java.util.ArrayList
import java.util.HashSet


class Crossword internal constructor(val width: Int = 0,
                                     val height: Int = 0,
                                     val squareCount: Int = 0,
                                     val flags: Int = 0,
                                     val title: String? = null,
                                     val description: String? = null,
                                     val author: String? = null,
                                     val copyright: String? = null,
                                     val comment: String? = null,
                                     val date: Long = 0,
                                     wordsAcross: List<Word>? = null,
                                     wordsDown: List<Word>? = null,
                                     alphabet: Set<Char>? = null) : Parcelable {

    val wordsAcross: List<Word> = ArrayList()
    val wordsDown: List<Word> = ArrayList()
    val alphabet: Set<Char> = HashSet()

    private constructor(source: Parcel): this(
            width = source.readInt(),
            height = source.readInt(),
            squareCount = source.readInt(),
            flags = source.readInt(),
            title = source.readString(),
            description = source.readString(),
            author = source.readString(),
            copyright = source.readString(),
            comment = source.readString(),
            date = source.readLong(),
            wordsAcross = source.createTypedArrayList(Word.CREATOR),
            wordsDown = source.createTypedArrayList(Word.CREATOR),
            alphabet = source.createCharArray()!!.toSet())

    init {
        wordsAcross?.let { (this.wordsAcross as MutableList) += it }
        wordsDown?.let { (this.wordsDown as MutableList) += it }
        alphabet?.let { (this.alphabet as MutableSet) += it }
    }

    val cellMap: Array<Array<Cell?>> by lazy { buildMap() }
    val hash: String by lazy { femputeHash() }

    class Builder() {

        @set:JvmName("width")
        var width: Int = 0
        @set:JvmName("height")
        var height: Int = 0
        @set:JvmName("title")
        var title: String? = null
        @set:JvmName("description")
        var description: String? = null
        @set:JvmName("author")
        var author: String? = null
        @set:JvmName("copyright")
        var copyright: String? = null
        @set:JvmName("comment")
        var comment: String? = null
        @set:JvmName("date")
        var date: Long = 0
        @set:JvmName("flags")
        var flags: Int = 0

        val alphabet: MutableSet<Char> = HashSet(ALPHABET_ENGLISH)
        val words: MutableList<Word> = ArrayList()

        constructor(crossword: Crossword): this() {
            width = crossword.width
            height = crossword.height
            title = crossword.title
            description = crossword.description
            author = crossword.author
            comment = crossword.comment
            copyright = crossword.copyright
            date = crossword.date
            flags = crossword.flags
            alphabet.clear()
            alphabet += crossword.alphabet
            words += crossword.wordsAcross + crossword.wordsDown
        }

        fun setWidth(value: Int): Builder {
            width = value
            return this
        }

        fun setHeight(value: Int): Builder {
            height = value
            return this
        }

        fun setTitle(text: String?): Builder {
            title = text
            return this
        }

        fun setDescription(text: String?): Builder {
            description = text
            return this
        }

        fun setAuthor(text: String?): Builder {
            author = text
            return this
        }

        fun setCopyright(text: String?): Builder {
            copyright = text
            return this
        }

        fun setComment(text: String?): Builder {
            comment = text
            return this
        }

        fun setDate(value: Long): Builder {
            date = value
            return this
        }

        fun setFlags(value: Int): Builder {
            flags = value
            return this
        }

        fun addWord(word: Word): Builder {
            words += word
            return this
        }

        fun setAlphabet(alphabet: Set<Char>): Builder {
            this.alphabet.clear()
            this.alphabet += alphabet

            return this
        }

        private fun countSquares(): Int {
            var count = 0
            val done = Array(height) { BooleanArray(width) }

            for (word in words) {
                when {
                    word.direction == Word.DIR_ACROSS -> {
                        word.directionRange
                                .filter { !done[word.startRow][it] }
                                .forEach {
                                    count++
                                    done[word.startRow][it] = true
                                }
                    }
                    word.direction == Word.DIR_DOWN -> {
                        word.directionRange
                                .filter { !done[it][word.startColumn] }
                                .forEach {
                                    count++
                                    done[it][word.startColumn] = true
                                }
                    }
                }
            }

            return count
        }

        /* FIXME
        fun autoNumber() {
            // autonumber left-to-right, top-to-bottom
            val tuples = Array(words.size) { IntArray(4) }
            var i = 0
            val n = words.size
            while (i < n) {
                val word = words[i]
                tuples[i] = intArrayOf(i, word.startRow, word.startColumn, word.direction)
                i++
            }

            Arrays.sort(tuples, Comparator { lhs, rhs ->
                if (lhs[1] != rhs[1]) { // sort by row
                    return@Comparator lhs[1] - rhs[1]
                }
                if (lhs[2] != rhs[2]) { // sort by column
                    return@Comparator lhs[2] - rhs[2]
                }
                if (lhs[3] != rhs[3]) { // sort by direction
                    if (lhs[3] == Word.DIR_ACROSS) -1 else 1
                } else 0

                // Should never get here
            })

            var pr = -1
            var pc = -1
            var number = 0

            for (tuple in tuples) {
                if (pr != tuple[1] || pc != tuple[2]) {
                    number++
                }

                words[tuple[0]].number = number
                pr = tuple[1]
                pc = tuple[2]
            }
        }
        */

        fun build(): Crossword = Crossword(
                width = width,
                height = height,
                squareCount = countSquares(),
                flags = flags,
                title = title,
                description = description,
                author = author,
                copyright = copyright,
                comment = comment,
                date = date,
                wordsAcross = words
                        .filter { it.direction == Word.DIR_ACROSS }
                        .sortedBy { it.number },
                wordsDown = words
                        .filter { it.direction == Word.DIR_DOWN }
                        .sortedBy { it.number },
                alphabet = alphabet)
    }

    fun newState(): CrosswordState = CrosswordState(width, height)

    fun previousWord(word: Word?): Word? {
        if (word != null) {
            val index = indexOf(word.direction, word.number)
            when {
                index > -1 && word.direction == Word.DIR_ACROSS -> when {
                    index > 0 -> return wordsAcross[index - 1]
                    wordsDown.isNotEmpty() -> return wordsDown.last()
                }
                index > -1 && word.direction == Word.DIR_DOWN -> when {
                    index > 0 -> return wordsDown[index - 1]
                    wordsAcross.isNotEmpty() -> return wordsAcross.last()
                }
            }
        }

        return when {
            wordsDown.isNotEmpty() -> wordsDown.last()
            wordsAcross.isNotEmpty() -> wordsAcross.last()
            else -> null
        }
    }

    fun nextWord(word: Word?): Word? {
        if (word != null) {
            val index = indexOf(word.direction, word.number)
            when {
                index > -1 && word.direction == Word.DIR_ACROSS -> when {
                    index < wordsAcross.lastIndex -> return wordsAcross[index + 1]
                    wordsDown.isNotEmpty() -> return wordsDown.first()
                }
                index > -1 && word.direction == Word.DIR_DOWN -> when {
                    index < wordsDown.lastIndex -> return wordsDown[index + 1]
                    wordsAcross.isNotEmpty() -> return wordsAcross.first()
                }
            }
        }

        return when {
            wordsAcross.isNotEmpty() -> wordsAcross.first()
            wordsDown.isNotEmpty() -> wordsDown.first()
            else -> null
        }
    }

    fun findWord(direction: Int, number: Int): Word? {
        val index = indexOf(direction, number)
        if (index < 0) return null

        return when (direction) {
            Word.DIR_ACROSS -> wordsAcross[index]
            Word.DIR_DOWN -> wordsDown[index]
            else -> throw IllegalArgumentException("Invalid word direction")
        }
    }

    fun findWord(direction: Int, row: Int, column: Int): Word? {
        return when (direction) {
            Word.DIR_ACROSS -> wordsAcross.firstOrNull {
                it.startRow == row && column >= it.startColumn
                        && column < it.startColumn + it.length
            }
            Word.DIR_DOWN -> wordsDown.firstOrNull {
                it.startColumn == column && row >= it.startRow
                        && row < it.startRow + it.length
            }
            else -> throw IllegalArgumentException("Invalid word direction")
        }
    }

    private fun indexOf(direction: Int, number: Int): Int {
        return when (direction) {
            Word.DIR_ACROSS -> wordsAcross.indexOfFirst { it.number == number }
            Word.DIR_DOWN -> wordsDown.indexOfFirst { it.number == number }
            else -> throw IllegalArgumentException("Invalid word direction")
        }
    }

    private fun femputeHash(): String = ByteArrayOutputStream().use { s ->
        CrosswordWriter(s).use { it.writeForHash(this) }
        s.toByteArray()
    }.sha1().toHexString()

    private fun buildMap(): Array<Array<Cell?>> {
        val map = Array(height) { arrayOfNulls<Cell>(width) }

        wordsAcross.forEach { word ->
            word.directionRange
                    .forEachIndexed { ix, col -> map[word.startRow][col] = word.cells[ix] }
        }
        wordsDown.forEach { word ->
            word.directionRange
                    .forEachIndexed { ix, row -> map[row][word.startColumn] = word.cells[ix] }
        }

        return map
    }

    // FIXME: this shouldn't be here
    fun updateStateStatistics(state: CrosswordState) {
        if (state.width != width) {
            throw RuntimeException("State width doesn't match puzzle width")
        } else if (state.height != height) {
            throw RuntimeException("State height doesn't match puzzle height")
        }

        var totalCount = 0
        var solvedCount = 0
        var cheatedCount = 0
        var unknownCount = 0
        var wrongCount = 0

        val done = Array(height) { BooleanArray(width) }
        val p = Pos()

        for (word in wordsAcross + wordsDown) {
            for (i in 0..word.cells.lastIndex) {
                when (word.direction) {
                    Word.DIR_ACROSS -> p.set(word.startRow, word.startColumn + i)
                    Word.DIR_DOWN -> p.set(word.startRow + i, word.startColumn)
                    else -> throw RuntimeException("Unexpected direction")
                }
                if (done[p.r][p.c]) continue

                totalCount++
                val stateChar = state.charMatrix[p.r][p.c]
                val cell = word.cells[i]

                when {
                    (cell.attrFlags.toInt() and Cell.ATTR_NO_SOLUTION != 0)
                            && stateChar != null -> unknownCount++
                    cell.contains(stateChar)
                            && state.isFlagSet(CrosswordState.FLAG_CHEATED, p.r, p.c) -> cheatedCount++
                    cell.contains(stateChar) -> solvedCount++
                    stateChar != null -> wrongCount++
                }

                done[p.r][p.c] = true
            }
        }

        state.setSquareStats(solvedCount, cheatedCount, wrongCount,
                unknownCount, totalCount)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, parcelFlags: Int) {
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeInt(squareCount)
        dest.writeInt(flags)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(author)
        dest.writeString(copyright)
        dest.writeString(comment)
        dest.writeLong(date)
        dest.writeTypedList(wordsAcross)
        dest.writeTypedList(wordsDown)
        dest.writeCharArray(alphabet.toCharArray())
    }

    override fun hashCode(): Int = hash.hashCode()

    override fun equals(other: Any?): Boolean = (other as? Crossword)?.hash == hash

    class Cell internal constructor(val chars: String = "",
                                    val attrFlags: Byte = 0): Parcelable {

        val isEmpty: Boolean
            get() = chars.isEmpty()

        val isCircled: Boolean
            get() = attrFlags.toInt() and ATTR_CIRCLED == ATTR_CIRCLED

        private constructor(source: Parcel): this(
                chars = source.readString()!!,
                attrFlags = source.readByte())

        fun chars(): String = chars

        operator fun contains(charSought: String?): Boolean = chars == charSought

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(chars)
            dest.writeByte(attrFlags)
        }

        override fun toString(): String = when (chars.length) {
            0 -> " "
            1 -> chars[0].toString()
            else -> "[$chars]"
        }

        companion object {
            const val ATTR_CIRCLED = 1
            const val ATTR_NO_SOLUTION = 2

            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<Cell> = object : Parcelable.Creator<Cell> {
                override fun createFromParcel(source: Parcel): Cell = Cell(source)
                override fun newArray(size: Int): Array<Cell?> = arrayOfNulls(size)
            }
        }
    }

    class Word internal constructor(val number: Int = 0,
                                    val hint: String? = null,
                                    val startRow: Int = 0,
                                    val startColumn: Int = 0,
                                    val direction: Int = 0,
                                    val hintUrl: String? = null,
                                    val citation: String? = null,
                                    cells: Array<Cell>?) : Parcelable {

        // FIXME: Mutable
        internal val cells: Array<Cell>

        init {
            this.cells = cells?.copyOf() ?: EMPTY_CELL
            if (direction != DIR_ACROSS && direction != DIR_DOWN) {
                throw IllegalArgumentException("Direction not valid: $direction")
            }
        }

        val length: Int
            get() = cells.size

        val directionRange: IntRange
            get() = when (direction) {
                DIR_ACROSS -> (startColumn..startColumn + cells.lastIndex)
                DIR_DOWN -> (startRow..startRow + cells.lastIndex)
                else -> throw RuntimeException("Unexpected direction: $direction")
            }

        class Builder {

            @set:JvmName("number")
            var number: Int = NUMBER_NONE
            @set:JvmName("hint")
            var hint: String? = null
            @set:JvmName("startRow")
            var startRow: Int = 0
            @set:JvmName("startColumn")
            var startColumn: Int = 0
            @set:JvmName("direction")
            var direction: Int = 0
            @set:JvmName("hintUrl")
            var hintUrl: String? = null
            @set:JvmName("citation")
            var citation: String? = null
            val cells = ArrayList<Cell>()

            fun setNumber(value: Int): Builder {
                number = value
                return this
            }

            fun setHint(text: String?): Builder {
                hint = text
                return this
            }

            fun setStartRow(value: Int): Builder {
                startRow = value
                return this
            }

            fun setStartColumn(value: Int): Builder {
                startColumn = value
                return this
            }

            fun setDirection(value: Int): Builder {
                direction = value
                return this
            }

            fun setHintUrl(text: String?): Builder {
                hintUrl = text
                return this
            }

            fun setCitation(text: String?): Builder {
                citation = text
                return this
            }

            fun addCell(ch: Char, attrFlags: Int = 0) = addCell(ch.toString(), attrFlags)

            fun addCell(chars: String, attrFlags: Int = 0) {
                cells.add(Cell(chars, attrFlags.toByte()))
            }

            fun build(): Word {
                if (number == NUMBER_NONE)
                    throw RuntimeException("Missing hint number")

                return Word(
                        number = number,
                        hint = hint,
                        startRow = startRow,
                        startColumn = startColumn,
                        direction = direction,
                        hintUrl = hintUrl,
                        citation = citation,
                        cells = cells.toTypedArray())
            }

            companion object {
                const val NUMBER_NONE = -1
            }
        }

        private constructor(source: Parcel): this(
                number = source.readInt(),
                hint = source.readString(),
                startRow = source.readInt(),
                startColumn = source.readInt(),
                direction = source.readInt(),
                hintUrl = source.readString(),
                citation = source.readString(),
                cells = source.readParcelableArray(Cell::class.java.classLoader)!!
                        .map { it as Cell }
                        .toTypedArray())

        operator fun get(pos: Int): Cell = cells[pos]

        fun cellAt(pos: Int): Cell = cells[pos]

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(number)
            dest.writeString(hint)
            dest.writeInt(startRow)
            dest.writeInt(startColumn)
            dest.writeInt(direction)
            dest.writeString(hintUrl)
            dest.writeString(citation)
            dest.writeParcelableArray(cells, 0)
        }

        override fun equals(other: Any?): Boolean = (other as? Word)?.let {
            it.direction == direction && it.number == number
        } ?: false

        override fun hashCode(): Int = "$direction-$number".hashCode()

        override fun toString(): String = buildString {
            append(number)
            append(" ")
            append(when (direction) {
                DIR_ACROSS -> "Across"
                DIR_DOWN -> "Down"
                else -> "????"
            })
            append(": ")
            append(hint)

            append(" (")
            append(cells.joinToString(""))
            append(")")
        }

        companion object {
            const val DIR_ACROSS = 0
            const val DIR_DOWN = 1

            private val EMPTY_CELL = arrayOf<Cell>()

            @JvmField
            val CREATOR: Parcelable.Creator<Word> = object : Parcelable.Creator<Word> {
                override fun createFromParcel(source: Parcel): Word = Word(source)
                override fun newArray(size: Int): Array<Word?> = arrayOfNulls(size)
            }
        }
    }

    companion object {
        @JvmField
        val ALPHABET_ENGLISH: Set<Char> = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray().toSet()

        const val FLAG_NO_SOLUTION = 1

        @JvmField
        val CREATOR: Parcelable.Creator<Crossword> = object : Parcelable.Creator<Crossword> {
            override fun createFromParcel(source: Parcel): Crossword = Crossword(source)
            override fun newArray(size: Int): Array<Crossword?> = arrayOfNulls(size)
        }
    }
}

fun buildCrossword(block: Crossword.Builder.() -> Unit): Crossword =
        Crossword.Builder().apply {
            block(this)
        }.build()

fun buildWord(block: Crossword.Word.Builder.() -> Unit): Crossword.Word =
        Crossword.Word.Builder().apply {
            block(this)
        }.build()
