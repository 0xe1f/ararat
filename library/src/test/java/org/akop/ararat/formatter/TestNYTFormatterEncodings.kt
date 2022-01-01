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

package org.akop.ararat.formatter

import org.akop.ararat.io.NYTFormatter
import org.junit.Test


class TestNYTFormatterEncodings: BaseTest() {

    val crossword = NYTFormatter().load("nyt_enc.json")

    @Test
    fun crossword_testMetadata() {
        assertMetadata(crossword, metadata)
    }

    @Test
    fun crossword_testHints() {
        assertHints(crossword, hints)
    }

    companion object {
        val metadata = Metadata(
                width = 15,
                height = 15,
                squareCount = 191,
                title = null,
                flags = 0,
                description = null,
                author = "Noki Trias and Lawrence Barrett",
                copyright = "2021",
                comment = null,
                date = 1640131200000,
                hash = "9c970b0163b1637eb7ac1b4c561268f1389a494f"
        )
        val hints = arrayOf(
                "1A.\"Darn it!\"",
                "5A.In base eight",
                "10A.\"Resident Alien\" channel",
                "14A.Sound heard in a long hall",
                "15A.Tennis champ Osaka",
                "16A.City with a beef",
                "17A.Jovial seasonal mood",
                "20A.___-Seltzer",
                "21A.Take a snooze, with \"out\"",
                "22A.Cost of not doing business, maybe",
                "29A.Significant ___",
                "30A.Hubbub",
                "31A.Like many a go-getter",
                "32A.Warrior in the Greek pantheon",
                "33A.Regional wildlife",
                "35A.Lovable goofball, say",
                "36A.NASA endeavors whose vehicles can be found at the ends of 17-, 22-, 51- and 57-Across",
                "39A.Sketch show since '75",
                "40A.Fist bump",
                "41A.Walkie-talkie band, briefly",
                "44A.Place to catch up over a hot drink",
                "48A.When repeated, slangy sound of eating",
                "51A.Why everyone loves a good train wreck",
                "54A.Fish also known as a bluegill",
                "55A.Make a bow",
                "56A.Title woman in a classic 1973 breakup tune",
                "57A.Uniquely American cleverness",
                "60A.___ homo",
                "61A.Rest atop",
                "62A.Cheeseboard choice",
                "63A.Quizzical Quebec questions?",
                "64A.\"I ___ know\" (common excuse)",
                "65A.Member of the fam",
                "1D.College app element",
                "2D.\"___ Tannenbaum\" (16th-century folk song that inspired a carol)",
                "3D.Avid skateboarder, in lingo",
                "4D.Gardeners' orders, at times",
                "5D.Straight out of the barrel",
                "6D.Nanny ___",
                "7D.Ode words",
                "8D.Morning hrs.",
                "9D.Composer of the piano piece played by Bugs Bunny in \"Rhapsody Rabbit\"",
                "10D.Lose the suit, say",
                "11D.Certain designer dog",
                "12D.Clarice Starling's employer in \"The Silence of the Lambs,\" in brief",
                "13D.Nevertheless",
                "18D.Hershey toffee confection",
                "19D.Fit of sullenness",
                "22D.Fertile ground",
                "23D.\"___ vez\" (\"Again,\" in Valencia)",
                "24D.Bother",
                "25D.\"Hamilton\" actor Leslie ___ Jr.",
                "26D.Indian flatbread",
                "27D.Cousin of a gull",
                "28D.Gabs",
                "33D.Like hay on a farm",
                "34D.\"Same here\"",
                "37D.Eventually",
                "38D.___ Islands, Polynesian archipelago",
                "41D.Awkward farewell",
                "42D.Poet who coined the term \"carpe diem\"",
                "43D.Kind of kiss",
                "45D.Timing of the Mercutio/Tybalt duel in \"Romeo and Juliet\"",
                "46D.Unfixable",
                "47D.Home to the deepest lake and river gorge in the U.S.",
                "48D.Cold rice topped with wasabi and raw fish",
                "49D.Likely cause of a cranky toddler's ear-tugging",
                "50D.\"Ridiculous!\"",
                "52D.Soaks up the hot sun",
                "53D.Ignores",
                "58D.Days gone by, in bygone days",
                "59D.Fangorn Forest dweller",
        )
    }
}
