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

import org.akop.ararat.io.PzzlFormatter
import org.junit.Test


class TestPzzlFormatter: BaseTest() {

    val crossword = PzzlFormatter().load("nytsyn-180703.pzzl")

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
                squareCount = 189,
                title = "NY Times, Tue, Jul 3, 2018",
                flags = 0,
                description = null,
                author = "Jeff Stillman / Will Shortz",
                copyright = null,
                comment = null,
                date = 0,
                hash = "24411f18615b2ef638df56e2ef8ce097cbd23b4a")
        val charMap = arrayOf(
                "CODA#IDES#BABEL",
                "LAIR#NOME#INURE",
                "URSAMAJOR#GOTAT",
                "ESOBESO#GODDESS",
                "##BYTE#TERI#NEG",
                "GEE##CHE#SPEEDO",
                "ARYAN#ONDOPE###",
                "LASSO#LAO#ENOLA",
                "###ERRAND#RYDER",
                "TREATY#TOP##OOF",
                "WON#HESS#ELON##",
                "ENMASSE#SCORNED",
                "EDICT#GREATBEAR",
                "TETRA#EARN#ILSA",
                "SLYER#LYES#SLEW")
        val attrMap = arrayOf(
                "....#....#.....",
                "....#....#.....",
                ".........#.....",
                ".......#.......",
                "##O...#....#..O",
                "...##O..#......",
                "O....#..O...###",
                ".....#...#.....",
                "###......#.....",
                "......#...##..O",
                "...#....#O...##",
                ".......#.......",
                ".....#.........",
                ".....#....#....",
                ".....#....#....")
        val hints = arrayOf(
                "1A.Sonata finale",
                "5A.Fateful day",
                "9A.Noted tower setting",
                "14A.Villain's hangout",
                "15A.City on the Seward Peninsula",
                "16A.Habituate",
                "17A.Constellation next to Draco",
                "19A.Subtly suggested",
                "20A.1962 Paul Anka hit",
                "21A.Widely adored woman",
                "23A.Part of a gig",
                "24A.Garr of \"Tootsie\"",
                "25A.Original of an old photo, informally",
                "26A.\"You don't say!\"",
                "28A.2008 Benicio Del Toro title role",
                "30A.Diminutive swimsuit",
                "32A.Indo-___ languages",
                "35A.Hopped up",
                "37A.Calf catcher",
                "38A.Language that becomes the name of where it's spoken if you add an \"s\"",
                "39A.___ Gay, 1945 bomber",
                "43A.Gofer's assignment",
                "45A.U-Haul alternative",
                "46A.Part of NATO",
                "49A.Outdo",
                "51A.Gut-punch reaction",
                "52A.Took the cake",
                "53A.Dame Myra of piano fame",
                "55A.SpaceX founder Musk",
                "58A.How lemmings migrate",
                "60A.Held in contempt",
                "63A.Authoritative command",
                "64A.Another term for 17-Across",
                "66A.Prefix with -hydrozoline",
                "67A.Make, as an income",
                "68A.\"Casablanca\" role",
                "69A.More foxy",
                "70A.Drain decloggers",
                "71A.Dispatched, as a dragon",
                "1D.What this is for 1-Down",
                "2D.Galley equipment",
                "3D.Doesn't mind",
                "4D.Sheik's land, in poetry",
                "5D.\"Just hang on!\"",
                "6D.Martial arts school",
                "7D.Music genre with confessional lyrics",
                "8D.Twilled fabric",
                "9D.Part of 17-Across ... and what the circles from A to G depict",
                "10D.Give ___ of approval",
                "11D.Compound in synthetic rubber",
                "12D.Wiped clean",
                "13D.\"Come on already!\"",
                "18D.Assembled",
                "22D.Approximately",
                "24D.Lease signatories",
                "26D.Hoedown partner",
                "27D.Victorian ___",
                "29D.Hi, on Hispaniola",
                "31D.Start of a decision-making process",
                "33D.What landlubbers don't like to be",
                "34D.Thing located in the night sky by extending a line from circle F past circle G",
                "36D.Numbskull",
                "40D.Former co-host of \"The View\"",
                "41D.Zodiac constellation",
                "42D.Lab warning?",
                "44D.Bakery loaves",
                "46D.Responsibility for a social media manager",
                "47D.14-line verse with only two rhyme sounds",
                "48D.Antagonism",
                "50D.Flavorers of some pies and ice cream",
                "54D.Jason of \"I Love You, Man\"",
                "56D.Auction grouping",
                "57D.Caesar's world?",
                "59D.Real estate unit",
                "60D.Dried up",
                "61D.Heart's-___ (pansy)",
                "62D.Tournament director's responsibility",
                "65D.Laser output")
    }
}
