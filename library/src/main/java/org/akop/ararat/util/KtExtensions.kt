// Copyright (c) Akop Karapetyan
//
// Part of KotX: https://github.com/0xe1f/KotX
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

import android.text.TextUtils
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
