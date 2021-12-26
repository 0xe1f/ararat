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


class TestNYTFormatter: BaseTest() {

    val crossword = NYTFormatter().load("nyt.json")

    @Test
    fun crossword_testMetadata() {
        assertMetadata(crossword, metadata)
    }

    @Test
    fun crossword_testCharLayout() {
        assertLayout(crossword, Array(charMap.size) { row ->
            charMap[row].chunked(1).map { when (it) { "#" -> null else -> it } }.toTypedArray()
        })
    }

    @Test
    fun crossword_testAttrLayout() {
        assertAttrLayout(crossword, Array(attrMap.size) { row ->
            attrMap[row].chunked(1).map { when (it) { "#" -> null else -> it } }.toTypedArray()
        })
    }

    @Test
    fun crossword_testHints() {
        assertHints(crossword, hints)
    }

    companion object {
        val metadata = Metadata(
                width = 15,
                height = 15,
                squareCount = 188,
                title = null,
                flags = 0,
                description = null,
                author = "Sam Buchbinder",
                copyright = "2015",
                comment = "The circled letters in this puzzle provide a hint to the starts of the answers to the four starred clues.",
                date = 1445299200000,
                hash = "817db000381c55c0efa55cf06ef2ea0fd0b3261f"
        )
        val charMap = arrayOf(
                "ABEL#DEAL#BEMAD",
                "PANASONIC#OVINE",
                "BLACKGOLD#SEXTS",
                "#ABEAM###TENTHS",
                "SNL##ASTOR#HARE",
                "ACERB#TIPI#APAR",
                "SEDER#ROUT#NEXT",
                "###DOME#LEAD###",
                "BOTS#ETTE#RENTS",
                "ANIN#ECON#IDIOT",
                "SANA#THATS##CUE",
                "SNIPES###POKER#",
                "ADEPT#ODDISNTIT",
                "LOSES#DARKHORSE",
                "ENTRY#SMEE#BYTE",
        )
        val attrMap = arrayOf(
                "....#....#.....",
                ".........#.....",
                ".........#.....",
                "#.....###......",
                "...##.....#....",
                ".....#....#....",
                ".....#OOO.#....",
                "###...O#O...###",
                "....#.OOO#.....",
                "....#....#.....",
                "....#.....##...",
                "......###.....#",
                ".....#.........",
                ".....#.........",
                ".....#....#....",
        )
        val hints = arrayOf(
                "1A.Genesis brother",
                "5A.An \"art\" for Donald Trump",
                "9A.\"If you want to throw a fit, fine\"",
                "14A.Japanese electronics giant",
                "16A.Like lambs and rams",
                "17A.*Oil, informally",
                "18A.Some titillating messages",
                "19A.Perpendicular to a ship's midline",
                "20A.Amounts after a decimal point",
                "21A.Longtime Lorne Michaels-produced show, for short",
                "22A.Lady ___, first female member of Parliament",
                "26A.Long-eared hopper",
                "27A.Bitter",
                "30A.Home on the range: Var.",
                "31A.On ___ with (comparable to)",
                "32A.Passover meal",
                "33A.Trounce",
                "34A.Call from behind the deli counter",
                "35A.Feature of Rome's Pantheon",
                "37A.Modern paint no-no",
                "39A.Droids",
                "42A.Feminine ending",
                "44A.Monopoly card listings",
                "48A.Has ___ (is connected)",
                "49A.Supply-and-demand subj.",
                "50A.Dodo",
                "51A.Yemeni capital",
                "52A.\"___ how I roll\"",
                "54A.Stick with a blue tip, maybe",
                "55A.\"Blade\" star Wesley",
                "57A.Casino staple",
                "60A.Skilled",
                "61A.*\"Weird, huh?\"",
                "65A.Misplaces",
                "66A.Surprise winner",
                "67A.Diary part",
                "68A.Captain Hook's sidekick",
                "69A.Small memory amount",
                "1D.Alert for a fleeing prisoner, in brief",
                "2D.Account amount",
                "3D.Made possible",
                "4D.Veil material",
                "5D.You better believe it",
                "6D.Ambient musician Brian",
                "7D.Suffer",
                "8D.20, for 1/4 and 1/5, e.g.: Abbr.",
                "9D.Big name in stereo speakers",
                "10D.*Fair",
                "11D.Personal music compilation",
                "12D.Hazardous mailing",
                "13D.Back-page menu item, maybe",
                "15D.Cousin of reggae",
                "20D.Overused",
                "21D.Carrier to Sweden",
                "23D.Warm up before exercising",
                "24D.Juan's uncle",
                "25D.Lavish",
                "28D.*Colorful Gulf Coast fish",
                "29D.Homie",
                "36D.Big track events",
                "38D.Bush press secretary Fleischer",
                "39D.Popular British brew",
                "40D.Endlessly",
                "41D.Like the pinky compared to the other fingers",
                "43D.Burned ___ crisp",
                "45D.\"Almost got me!\"",
                "46D.One seeing the sites",
                "47D.Canonized woman of Fr.",
                "53D.Feature of a punk hairdo",
                "56D.Online crafts site",
                "58D.___Kosh B'Gosh",
                "59D.Radio dial",
                "61D.Has too much, informally",
                "62D.Beaver's construction",
                "63D.Hip-hop's Dr. ___",
                "64D.Gym shirt",
        )
    }
}
