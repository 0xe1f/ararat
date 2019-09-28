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

import org.akop.ararat.io.CrosswordCompilerFormatter
import org.junit.Test


class TestCcFormatter: BaseTest() {

    val crossword = CrosswordCompilerFormatter().load("la180519.xml")

    @Test
    fun crossword_testMetadata() {
        assertMetadata(crossword, metadata)
    }

    @Test
    fun crossword_testLayout() {
        assertLayout(crossword, Array(layout.size) { row ->
            layout[row].chunked(1).map { when (it) { "#" -> null else -> it } }.toTypedArray()
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
                squareCount = 192,
                title = "LA Times, Sat, May 19, 2018",
                flags = 0,
                description = null,
                author = "Jeff Chen / Ed. Rich Norris",
                copyright = "© 2018 Tribune Content Agency, LLC",
                comment = null,
                date = 0,
                hash = "3613172642e2ab572d1dc5a127b7c7a43f9fbf36")
        val layout = arrayOf(
                "#WRITE##JETSETS",
                "SHANIA#DATAPLAN",
                "TITTER#JIUJITSU",
                "UTTER#FOLD#TOMB",
                "PEAL#HAKEEM#RAS",
                "ELI#EMBED#ATON#",
                "FILMNOIR#JLO###",
                "YESORNO#JEALOUS",
                "###JAG#JURYLIST",
                "#JPOP#JUNKS#LBO",
                "GOA#TBOLTS#AGOG",
                "RISK#ALIA#ATARI",
                "INSOMNIA#ALLUDE",
                "PIECAKEN#STAGES",
                "SNLHOST##HOSER#")
        val hints = arrayOf(
                "1A.Begin another chapter, e.g.",
                "6A.Gets high at a moment's notice?",
                "13A.Country name on \"The Woman in Me\"",
                "14A.Cell package",
                "15A.Suppressed guffaw",
                "16A.Art using locks",
                "17A.Verbalize",
                "18A.Double over?",
                "19A.Grave",
                "20A.Bit of tintinnabulation",
                "21A.NBA great __ Olajuwon",
                "23A.MIT mentors",
                "24A.Lilly of pharmaceuticals",
                "25A.Insert into an email, as a video",
                "26A.Buckets",
                "28A.Hard-boiled genre",
                "30A.Self-titled 2001 album",
                "31A.Voter's choices",
                "32A.Worried about losing one's place?",
                "36A.Spree",
                "37A.Source of trial figures",
                "38A.Asian genre influenced by The Beatles",
                "40A.Throws out",
                "41A.Debt-laden Wall St. deal",
                "42A.Small Indian state",
                "43A.Hardware with elongated heads",
                "45A.Awestruck",
                "46A.Game with a 2210 A.D. edition",
                "48A.Et __",
                "49A.Arcade giant",
                "50A.Retirement obstacle",
                "52A.Hint at, with \"to\"",
                "53A.Dessert analog to turducken",
                "54A.Puts on",
                "55A.George Carlin was the first, briefly",
                "56A.Firefighter, at times",
                "1D.Slight fiction",
                "2D.Faddish '80s-'90s hairstyles",
                "3D.Agents' gathering",
                "4D.Level with fans",
                "5D.__ trumpet",
                "6D.Given a sentence to complete",
                "7D.Manuscript with dense notes?",
                "8D.Part of a historic 19-Across name",
                "9D.Hot rod?",
                "10D.Corrida combatant",
                "11D.Eponymous explorer of Australia",
                "12D.Intentionally overlooks",
                "13D.Make one's eyes pop out",
                "14D.Nickname for tennis star/prankster Novak Djokovic",
                "18D.Icon often pictured with wind-blown hair",
                "21D.Chinese ethnic group",
                "22D.Ringgit spenders",
                "25D.Unable to look away",
                "27D.Passing charge",
                "29D.Magical power",
                "30D.Yanks",
                "32D.Post-fall cabal",
                "33D.Pressure indicator",
                "34D.Part of it was a 2016 campaign issue",
                "35D.Cheap smokes",
                "37D.Assange of WikiLeaks",
                "38D.Hum along, say",
                "39D.Scores, with \"a\"",
                "40D.City where Jake Blues was in prison",
                "42D.Some movie set techs",
                "44D.Hold-up targets",
                "45D.Hold-up man?",
                "47D.\"Mayor\" author",
                "49D.One not often hitting the high note",
                "51D.Leader with a jacket named for him",
                "52D.Elastic wood")
    }
}
