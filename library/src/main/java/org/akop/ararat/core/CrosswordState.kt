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


// FIXME inner arrays of char/attrMatrix are mutable
class CrosswordState internal constructor(val width: Int = 0,
                                          val height: Int = 0,
                                          var playTimeMillis: Long = 0,
                                          var lastPlayed: Long = 0,
                                          internal var selection: Int = 0,
                                          var squaresSolved: Short = 0,
                                          var squaresCheated: Short = 0,
                                          var squaresWrong: Short = 0,
                                          var squaresUnknown: Short = 0,
                                          var squareCount: Short = 0) : Parcelable {

    internal val charMatrix: Array<Array<String?>> = Array(height) { arrayOfNulls<String?>(width) }
    internal val attrMatrix: Array<IntArray> = Array(height) { IntArray(width) }

    val isCompleted: Boolean
        get() = squaresSolved + squaresCheated >= squareCount

    val selectedDirection: Int
        get() = selection and SEL_DIR_MASK shr SEL_DIR_SHIFT

    val selectedNumber: Int
        get() = selection and SEL_NUMBER_MASK

    val selectedCell: Int
        get() = selection and SEL_CHAR_MASK shr SEL_CHAR_SHIFT

    constructor(other: CrosswordState): this(
            width = other.width,
            height = other.height,
            playTimeMillis = other.playTimeMillis,
            lastPlayed = other.lastPlayed,
            selection = other.selection,
            squaresSolved = other.squaresSolved,
            squaresCheated = other.squaresCheated,
            squaresWrong = other.squaresWrong,
            squaresUnknown = other.squaresUnknown,
            squareCount = other.squareCount) {
        (0 until height).forEach { r ->
            System.arraycopy(other.charMatrix[r], 0, charMatrix[r], 0, width)
            System.arraycopy(other.attrMatrix[r], 0, attrMatrix[r], 0, width)
        }
    }

    private constructor(source: Parcel): this(
            width = source.readInt(),
            height = source.readInt(),
            playTimeMillis = source.readLong(),
            lastPlayed = source.readLong(),
            selection = source.readInt(),
            squaresSolved = source.readInt().toShort(),
            squaresCheated = source.readInt().toShort(),
            squaresWrong = source.readInt().toShort(),
            squaresUnknown = source.readInt().toShort(),
            squareCount = source.readInt().toShort()) {
        source.createStringArray()!!
                .forEachIndexed { i, c -> charMatrix[i / width][i % width] = c }
        source.createIntArray()!!
                .forEachIndexed { i, a -> attrMatrix[i / width][i % width] = a }
    }

    // FIXME: unneeded
    internal fun setSquareStats(solved: Int, cheated: Int, wrong: Int, unknown: Int, count: Int) {
        squaresSolved = solved.toShort()
        squaresCheated = cheated.toShort()
        squaresWrong = wrong.toShort()
        squaresUnknown = unknown.toShort()
        squareCount = count.toShort()
    }

    fun setSelection(direction: Int, number: Int, cell: Int) {
        selection = (direction shl SEL_DIR_SHIFT and SEL_DIR_MASK
                or (number and SEL_NUMBER_MASK)
                or (cell shl SEL_CHAR_SHIFT and SEL_CHAR_MASK))
    }

    fun hasSelection(): Boolean {
        return selection != 0
    }

    fun charAt(row: Int, column: Int): String? = charMatrix[row][column]

    fun setCharAt(row: Int, column: Int, ch: String?) {
        charMatrix[row][column] = ch
    }

    fun isFlagSet(flag: Int, row: Int, column: Int): Boolean =
            attrMatrix[row][column] and flag == flag

    fun setFlagAt(flag: Int, row: Int, column: Int, set: Boolean) {
        if (set) {
            attrMatrix[row][column] = attrMatrix[row][column] or flag
        } else {
            attrMatrix[row][column] = attrMatrix[row][column] and flag.inv()
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeLong(playTimeMillis)
        dest.writeLong(lastPlayed)
        dest.writeInt(selection)
        dest.writeInt(squaresSolved.toInt())
        dest.writeInt(squaresCheated.toInt())
        dest.writeInt(squaresWrong.toInt())
        dest.writeInt(squaresUnknown.toInt())
        dest.writeInt(squareCount.toInt())
        dest.writeStringArray(charMatrix.flatten().toTypedArray())
        dest.writeIntArray(IntArray(height * width) { attrMatrix[it / width][it % width] })
    }

    companion object {
        const val FLAG_CHEATED = 0x01
        const val FLAG_MARKED  = 0x02

        private const val SEL_NUMBER_MASK = 0x0000fff
        private const val SEL_CHAR_MASK   = 0x0fff000
        private const val SEL_CHAR_SHIFT  = 12
        private const val SEL_DIR_MASK    = 0xf000000
        private const val SEL_DIR_SHIFT   = 24

        @JvmField
        val CREATOR: Parcelable.Creator<CrosswordState> = object : Parcelable.Creator<CrosswordState> {
            override fun createFromParcel(source: Parcel) = CrosswordState(source)
            override fun newArray(size: Int): Array<CrosswordState?> = arrayOfNulls(size)
        }
    }
}
