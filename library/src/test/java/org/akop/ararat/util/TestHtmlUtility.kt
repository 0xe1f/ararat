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

package org.akop.ararat.util

import org.junit.Assert
import org.junit.Test


class TestHtmlUtility {

    @Test
    fun testEntityStripper() {
        strings.forEach { p ->
            Assert.assertEquals("String mismatch",
                    p.second, HtmlUtility.stripEntities(p.first))
        }
    }

    // Pair(before, expected)
    private val strings = listOf(
            Pair("", ""),
            Pair("&#;", ""), // Invalid num. entity
            Pair("&#", "&#"),
            Pair("&;", ""), // Invalid named entity
            Pair("&", "&"),
            Pair("&amp;", "&"),
            Pair("&amp", "&amp"),
            Pair("&am;", ""), // Invalid named entity
            Pair("It's all about the &#36;&#36;", "It's all about the $$"),
            Pair("5 &#8805; 2", "5 ≥ 2"),
            Pair("&amp;hf346,1", "&hf346,1"),
            Pair("Here&apos;s Johnny!", "Here's Johnny!"),
            Pair("&quot;Quote un-quote&quot;", "\"Quote un-quote\""),
            Pair("Here's a fraction: &frac34;", "Here's a fraction: ¾"),

            Pair("Sister of Chekhov&rsquo;s Masha and Irina",
                    "Sister of Chekhov’s Masha and Irina"),
            Pair("Ovid&rsquo;s first work",
                    "Ovid’s first work"),
            Pair("&ldquo;...upon the stair, ___ man who wasn&rsquo;t there&rdquo;",
                    "“...upon the stair, ___ man who wasn’t there”"),
            Pair("Cariou of &ldquo;Blue Bloods&rdquo;",
                    "Cariou of “Blue Bloods”")
    )
}
