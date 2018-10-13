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

package org.akop.ararat

import org.akop.ararat.io.WSJFormatter
import org.akop.ararat.io.WSJFormatter.Companion.PUBLISH_DATE_FORMAT
import org.junit.Test


class TestWSJFormatter: BaseTest() {

    val crossword = WSJFormatter().load("wsj.json")

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
                squareCount = 185,
                title = "That Smarts!",
                flags = 0,
                description = "",
                author = "By Dan Fisher/Edited by Mike Shenk",
                copyright = "The Wall Street Journal",
                comment = null,
                date = PUBLISH_DATE_FORMAT.parse("Monday, 07 May 2018").time,
                hash = "14f52e52fb2ad08278c92aae1962f8ca270b11ec")
        val layout = arrayOf(
                "DAMUP#ALAS##TNT",
                "OBESE#CANI#SHOE",
                "POWERMOWER#IRIS",
                "EVE#CAR#TOPMOST",
                "REDCHINA#CROWE#",
                "###PEN#SECOND##",
                "GOBAD##SNOWYOWL",
                "MOO##SOILS##WOO",
                "COWTOWNS##MONKS",
                "##WIDEST#CAP###",
                "#WILDE#SPARTANS",
                "RENTSTO#ART#LOO",
                "ALDA#BLOWBYBLOW",
                "PLOT#AGEE#RAISE",
                "SSW##YARD#STEED")
        val hints = arrayOf(
                "1A.Block, as a river",
                "6A.\"What a pity!\"",
                "10A.Explosive stuff",
                "13A.More than plump",
                "14A.\"What ___ do for you?\"",
                "15A.Moccasin or mule",
                "16A.Lawn-tending machine",
                "18A.Pupil setting",
                "19A.Eden exile",
                "20A.Lot buy",
                "21A.Crowning",
                "23A.Mao's domain",
                "26A.\"Gladiator\" star",
                "27A.Autographing need",
                "28A.Minute part",
                "30A.Spoil",
                "33A.Harry Potter's Hedwig, for one",
                "36A.Cattle call",
                "37A.Dirties",
                "39A.Pursue romantically",
                "40A.Backwater burgs",
                "43A.Brothers in cowls",
                "45A.Least narrow",
                "46A.Put a lid on",
                "47A.\"The Importance of Being Earnest\" playwright",
                "48A.Michigan State team",
                "53A.Has as a tenant",
                "55A.Painting, sculpture and the like",
                "56A.Brit's bathroom",
                "57A.Alan of \"M*A*S*H\"",
                "58A.Minutely detailed, as an account",
                "61A.Storyline",
                "62A.\"A Death in the Family\" author James",
                "63A.Employee's reward",
                "64A.Dir. opposite NNE",
                "65A.Place to employ a 16-Across",
                "66A.Spirited horse",
                "1D.Steroids addict",
                "2D.Superior to",
                "3D.Made kittenish cries",
                "4D.Take advantage of",
                "5D.Roosted",
                "6D.Item in a squirrel's stash",
                "7D.Bar focus",
                "8D.Working without ___ (taking risks)",
                "9D.Hot Mediterranean winds",
                "10D.Hardcore hip-hop performance",
                "11D.Sleep preventer",
                "12D.Final, for one",
                "15D.Buying or selling of church offices",
                "17D.Primary",
                "22D.Front of a freighter",
                "24D.Expert in IRS rules",
                "25D.Lends a hand",
                "29D.In the mil. by choice",
                "30D.Maker of Yukons and Acadias",
                "31D.Tic-tac-toe win",
                "32D.Projecting architectural feature",
                "34D.Stir-frying vessel",
                "35D.___ Angeles",
                "37D.Shrub with aromatic leaves",
                "38D.Clip-___ (some ties)",
                "41D.Attack with a lance",
                "42D.Racetrack numbers",
                "43D.They suffer for causes",
                "44D.Make a choice",
                "46D.Atkins Diet no-no",
                "47D.Water sources",
                "49D.Handled indelicately",
                "50D.TV oldie \"Kate & ___\"",
                "51D.Gallows sight",
                "52D.Planted",
                "53D.Emulates Cardi B",
                "54D.Gymnast Korbut",
                "59D.\"___ the ramparts we...\"",
                "60D.Club on a diamond")
    }
}
