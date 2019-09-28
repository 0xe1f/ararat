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

import org.akop.ararat.io.PuzFormatter
import org.junit.Test


class TestPuzFormatter: BaseTest() {

    val crossword = PuzFormatter().load("puzzle.puz")

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
    fun crossword_testHints() {
        assertHints(crossword, hints)
    }

    companion object {
        val metadata = Metadata(
                width = 15,
                height = 15,
                squareCount = 191,
                title = "Double A's",
                flags = 0,
                description = null,
                author = "Ben Tausig",
                copyright = "",
                comment = "",
                date = 0,
                hash = "8d205cdebc52f5b65fd8b3ff5f6962fcfe211a01")
        val charMap = arrayOf(
                "WOK#THEQT#SCRIP",
                "ANI#AURUM#OHARA",
                "TEN#GRAINSLAVES",
                "EGGOS###THERENT",
                "RAJA#DIS#WILDES",
                "STARINGWHALE###",
                "HIM#CANOE##SMSS",
                "EVERY#ORL#DICTA",
                "DESE##RELEE#MAT",
                "###MATEANDGRATE",
                "SIDIBE#TOO#ONUS",
                "ACETATE###ZESTA",
                "BAITTHEHATE#IOU",
                "EMCEE#LOHAN#ORC",
                "REEDS#SPANO#NYE")
        val hints = arrayOf(
                "1A.Cooking vessel for fried rice",
                "4A.What you might be asked to keep a secret on",
                "9A.Rx",
                "14A.Righteous Babe Records name",
                "15A.Gold, in Latin",
                "16A.Fictional Scarlett",
                "17A.Number of fugitives on a noted list",
                "18A.People dependent on barley?",
                "20A.Some frozen waffles",
                "22A.Monthly payment, usually",
                "23A.Title in many an Indian restaurant name",
                "24A.Put-down",
                "27A.Kim and Oscar, e.g.",
                "28A.Sea World creature who seems to be really fixated on something?",
                "31A.Pronoun not used for god in \"The Inclusive Bible\"",
                "32A.Rapids rental",
                "33A.Modern missives, briefly",
                "37A.Each partner",
                "39A.City with an NBA team, but no NFL, MLB, or NHL club: Abbr.",
                "40A.Formal rulings",
                "41A.Brooklynese pronoun",
                "42A.U.S. Grant foe",
                "44A.Yoga surface",
                "45A.Get married, then get on your partner's nerves?",
                "49A.Oscar-nominee Gabourey",
                "52A.Excessively",
                "53A.Albatross",
                "54A.Old album material",
                "56A.Source of sodium from Keebler",
                "57A.Wear an Arthur Ashe shirt to a Ku Klux Klan rally?",
                "61A.Controversial letters in California, these days",
                "62A.Act introducer",
                "63A.Archetypical \"hot mess\" Lindsay",
                "64A.Fantasy beast",
                "65A.Strips at the symphony?",
                "66A.Jessie who was A.C. Slater's girl",
                "67A.\"Science guy\" Bill",
                "1D.Landmark",
                "2D.Universal donor type",
                "3D.LeBron, to fans",
                "4D.Bombs a train car, slangily",
                "5D.\"Ben-___\"",
                "6D.Spring training stat",
                "7D.Who, abroad",
                "8D.Group led by Master Splinter, initially",
                "9D.___ Moon Frye (Punky Brewster portrayer)",
                "10D.Great Carolingian king, on regnal lists",
                "11D.Carried a glow stick, say",
                "12D.\"Fame\" singer Cara",
                "13D.Histories",
                "19D.Linguist's vowel: Var.",
                "21D.Stern-faced tool?",
                "24D.Biological blueprint",
                "25D.Decline to answer",
                "26D.Called every name in the book",
                "29D.Unpleasant, in a way",
                "30D.\"Not a goddamn chance\"",
                "34D.Garage Mahal",
                "35D.Like many legal charges",
                "36D.Skewered meat flavoring",
                "38D.Paid",
                "40D.Award for a bachelor: Abbr.",
                "43D.It became Tokyo in 1868",
                "46D.Dies away",
                "47D.Ninth Hebrew letter",
                "48D.Japanese delicacy",
                "49D.Blade in some Mathew Brady Civil War photographs",
                "50D.What \"veni\" means",
                "51D.Clear, as a cold nose?",
                "55D.Fish with slimy layers",
                "56D.Arrow paradox philosopher",
                "58D.Make like a bunny",
                "59D.\"I knew it all along!\"",
                "60D.Farmer's ___"
        )
    }
}
