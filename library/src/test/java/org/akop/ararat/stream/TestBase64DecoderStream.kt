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

package org.akop.ararat.stream

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.Base64 as JBase64


@RunWith(PowerMockRunner::class)
@PrepareForTest(Base64::class)
class TestBase64DecoderStream {

    @Before
    fun setup() {
        // Can't use Android's Base64 decoder - mock using Java's Base64 decoder
        PowerMockito.mockStatic(Base64::class.java)
        Mockito.`when`(Base64.decode(any(ByteArray::class.java), anyInt(), anyInt(), anyInt()))
                .thenAnswer { invoc ->
                    val argBuf = invoc.arguments[0] as ByteArray
                    val start = invoc.arguments[1] as Int
                    val len = invoc.arguments[2] as Int
                    JBase64.getDecoder().decode(
                            argBuf.copyOfRange(start, start + len))
                }
    }

    @Test
    fun givenEncodedStream_ensureDecodesInVariableChunks() {
        val stream = Base64DecoderStream(ENCODED_LONG.byteInputStream())
        val buffer = ByteArray(10)
        var len = 0
        val string = buildString {
            var index = 0
            while (len > -1) {
                append(String(buffer, 0, len))
                val chunkSize = ++index % 10
                len = if (chunkSize == 1) {
                    val r = stream.read()
                    if (r != -1) {
                        buffer[0] = r.toByte()
                        1
                    } else {
                        -1
                    }
                } else {
                    stream.read(buffer, 0, chunkSize)
                }
            }
        }
        assertEquals(DECODED_LONG, string)
    }

    @Test
    fun givenEncodedStreams_ensureDecodesAtOnce() {
        SAMPLE_CONTENT.forEach { pair ->
            val stream = Base64DecoderStream(pair.first.byteInputStream())
            assertEquals(pair.second,
                    stream.bufferedReader().use { it.readText() })
        }
    }

    @Test
    fun givenEncodedStreams_ensureDecodesByteAtATime() {
        SAMPLE_CONTENT.forEach { pair ->
            assertEquals(pair.second, buildString {
                val stream = Base64DecoderStream(pair.first.byteInputStream())
                var b = stream.read()
                while (b != -1) {
                    append(b.toChar())
                    b = stream.read()
                }
            })
        }
    }

    companion object {
        private val ENCODED_LONG =
                "TG9yZW0gSXBzdW0gaXMgc2ltcGx5IGR1bW15IHRleHQgb2YgdGhlIHByaW50a" +
                "W5nIGFuZCB0eXBlc2V0dGluZyBpbmR1c3RyeS4gTG9yZW0gSXBzdW0gaGFzIG" +
                "JlZW4gdGhlIGluZHVzdHJ5J3Mgc3RhbmRhcmQgZHVtbXkgdGV4dCBldmVyIHN" +
                "pbmNlIHRoZSAxNTAwcywgd2hlbiBhbiB1bmtub3duIHByaW50ZXIgdG9vayBh" +
                "IGdhbGxleSBvZiB0eXBlIGFuZCBzY3JhbWJsZWQgaXQgdG8gbWFrZSBhIHR5c" +
                "GUgc3BlY2ltZW4gYm9vay4gSXQgaGFzIHN1cnZpdmVkIG5vdCBvbmx5IGZpdm" +
                "UgY2VudHVyaWVzLCBidXQgYWxzbyB0aGUgbGVhcCBpbnRvIGVsZWN0cm9uaWM" +
                "gdHlwZXNldHRpbmcsIHJlbWFpbmluZyBlc3NlbnRpYWxseSB1bmNoYW5nZWQu" +
                "IEl0IHdhcyBwb3B1bGFyaXNlZCBpbiB0aGUgMTk2MHMgd2l0aCB0aGUgcmVsZ" +
                "WFzZSBvZiBMZXRyYXNldCBzaGVldHMgY29udGFpbmluZyBMb3JlbSBJcHN1bS" +
                "BwYXNzYWdlcywgYW5kIG1vcmUgcmVjZW50bHkgd2l0aCBkZXNrdG9wIHB1Ymx" +
                "pc2hpbmcgc29mdHdhcmUgbGlrZSBBbGR1cyBQYWdlTWFrZXIgaW5jbHVkaW5n" +
                "IHZlcnNpb25zIG9mIExvcmVtIElwc3VtLg=="
        private val DECODED_LONG =
                "Lorem Ipsum is simply dummy text of the printing and typesetting " +
                "industry. Lorem Ipsum has been the industry's standard dummy text " +
                "ever since the 1500s, when an unknown printer took a galley of type " +
                "and scrambled it to make a type specimen book. It has survived not " +
                "only five centuries, but also the leap into electronic typesetting, " +
                "remaining essentially unchanged. It was popularised in the 1960s with " +
                "the release of Letraset sheets containing Lorem Ipsum passages, and " +
                "more recently with desktop publishing software like Aldus PageMaker " +
                "including versions of Lorem Ipsum."
        private val SAMPLE_CONTENT = listOf(
                "" to "",
                "YQ==" to "a",
                "YWI=" to "ab",
                "YWJj" to "abc",
                "YWJjZA==" to "abcd",
                "YWJjZGU=" to "abcde",
                "YWJjZGVm" to "abcdef",
                "YWJjZGVmZw==" to "abcdefg",
                "YWJjZGVmZ2g=" to "abcdefgh",
                "YWJjZGVmZ2hp" to "abcdefghi",
        )
    }
}
