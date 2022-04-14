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
            squareCount = 183,
            title = null,
            flags = 0,
            description = null,
            author = "Rebecca Goldstein",
            copyright = "2022",
            comment = null,
            date = 1649808000000,
            hash = "8ea73fa96e7ba68dd84b81c5e00f318231998b25"
        )
        val charMap = arrayOf(
            "ONS##BUNCH#OOZE",
            "PEC##ASAHI#PROS",
            "TAIL#DECOR#TERI",
            "ITSOK#DRIER#ORG",
            "MISPLACE#SOSOON",
            "IDO#EID#LOCHS##",
            "SERVER#PANKO###",
            "MASA##VEX##WRIT",
            "###LAPAZ#GENOME",
            "##RESET#BEG#SPA",
            "GEOTAG#GALOSHES",
            "ORB#PAPER#SHARE",
            "BABY#SOCKS#EMIR",
            "ASIA#URKEL##BAA",
            "GEEK#STORY##OLD",
        )
        val attrMap = arrayOf(
            "...##.....#....",
            "O..##.O...#...O",
            ".O..#..O..#..O.",
            ".....#..O..#O..",
            "...O....#O.O...",
            "...#O..#.....##",
            ".....O#.....###",
            "....##...##....",
            "###.....#......",
            "##.....#...#...",
            "......#........",
            "...#.....#.....",
            "....#.OO..#....",
            "....#.OO..##...",
            "....#.....##...",
        )
        val hints = arrayOf(
            "1A.Ending with walk or run",
            "4A.Grape group",
            "9A.Exude irrepressibly",
            "13A.Poppable muscle, informally",
            "14A.Popular Japanese brew",
            "15A.Experts",
            "16A.Scorpion's stinger",
            "18A.Ikea department",
            "19A.Polo of \"The Fosters\"",
            "20A.\"Don't worry\"",
            "22A.Less soaked",
            "24A..com alternative",
            "25A.Lose",
            "27A.\"Already?\"",
            "29A.Likely answer to \"Who wants ice cream?\"",
            "30A.Festival, in Arabic",
            "31A.Awe and Tay, for two",
            "32A.One to tip",
            "34A.Breading for tonkatsu",
            "35A.Tortilla dough",
            "36A.Bring stress or agitation to",
            "37A.___ large",
            "41A.South American capital with the world's longest urban gondola",
            "44A.Nuclear codes?",
            "46A.Zero out",
            "47A.Plead",
            "48A.Therein lies the rub!",
            "49A.Bit of metadata",
            "51A.Wet weather wear",
            "53A.Poet's sphere",
            "54A.<i>Coverer</i>",
            "56A.Portion",
            "57A.Word with sitter or steps",
            "59A.Good name for a black cat with white feet",
            "61A.Commander, in Arabic",
            "62A.Mt. Fuji setting",
            "63A.1990s TV nerd",
            "64A.Is that what ewe said?",
            "65A.Uber-enthusiast",
            "66A.Part of a child's bedtime ritual",
            "67A.Stale",
            "1D.\"That's good\" thinking",
            "2D.\"That's good thinking!\"",
            "3D.<i>Cutter</i>",
            "4D.Rotten",
            "5D.Something heard secondhand?",
            "6D.Mother-of-pearl",
            "7D.\"Parasite\" actor Woo-shik",
            "8D.Adds to the team, so to speak",
            "9D.Go (for)",
            "10D.Cookie-flavored cereal",
            "11D.Fictional character partially inspired by Mexican folklore",
            "12D.Provide digital approval",
            "17D.Apt rhyme for chop and crop",
            "21D.Artist who said \"A line is a dot that went for a walk\"",
            "23D.<i>Breaker</i>",
            "26D.The \"A\" of A.Q.I.",
            "28D.Exhibited",
            "31D.Hardly strict",
            "33D.One to tip",
            "34D.Candy once marketed as a smoking cessation aid",
            "36D.Big tub",
            "38D.One name for the game depicted in this puzzle",
            "39D.Like Rome starting in the first century B.C.",
            "40D.Promo",
            "42D.\"Rush!\"",
            "43D.Perseus' horse",
            "44D.Mousse alternative",
            "45D.Bigwigs may have big ones",
            "46D.Actress Margot of \"Bombshell\"",
            "47D.Carnival hypeman",
            "49D.Case of emergency?",
            "50D.Clear",
            "51D.Geico \"spokeslizard\"",
            "52D.\"Nevertheless, ___ persisted\"",
            "55D.Dessert wine",
            "58D.Himalayan ox",
            "60D.Foxy",
        )
    }
}
