// Copyright (c) Akop Karapetyan
//
// Some of these are part of KotX: https://github.com/0xe1f/KotX
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.akop.ararat.util

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import java.security.MessageDigest


internal fun ByteArray.toHexString(): String {
    val hexArray = "0123456789abcdef".toCharArray()
    val hexChars = CharArray(size * 2)
    for (i in 0 until size) {
        val b = this[i].toInt() and 0xff
        hexChars[i * 2] = hexArray[b ushr 4]
        hexChars[i * 2 + 1] = hexArray[b and 0x0f]
    }

    return String(hexChars)
}

internal fun ByteArray.sha1(): ByteArray = MessageDigest.getInstance("SHA-1").apply {
    update(this@sha1, 0, size)
}.digest()

internal fun String.htmlEncode(): String = TextUtils.htmlEncode(this)

internal fun PointF.clampTo(rect: RectF) {
    if (x < rect.left) x = rect.left else if (x > rect.right) x = rect.right
    if (y < rect.top) y = rect.top else if (y > rect.bottom) y = rect.bottom
}

internal fun String.toColor(): Int = Color.parseColor(this)

internal fun Path.with(reset: Boolean = false, block: Path.() -> Unit): Path {
    if (reset) reset()
    block(this)
    close()

    return this
}

internal val Bitmap.sizeInBytes
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) allocationByteCount else byteCount

internal inline fun <reified T: Parcelable> Parcel.readTypedParcelable(): T? =
        readParcelable(T::class.java.classLoader)

internal fun Context.withStyledAttributes(attrs: IntArray,
                                          set: AttributeSet? = null,
                                          defStyleAttr: Int = 0,
                                          defStyleRes: Int = 0,
                                          block: TypedArray.() -> Unit) {
    set?.let {
        theme.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes)
                .apply(block)
                .recycle()
    }
}

internal val Context.inputMethodManager: InputMethodManager?
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
