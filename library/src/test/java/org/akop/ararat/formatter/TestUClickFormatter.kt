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

import org.akop.ararat.io.UClickFormatter
import org.junit.Test


class TestUClickFormatter: BaseTest() {

    val crossword = UClickFormatter().load("univ-daily-180401.xml")

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
                squareCount = 189,
                title = "TYPE CASTING",
                flags = 0,
                description = null,
                author = "By Timothy E. Parker",
                copyright = "Universal UClick",
                comment = null,
                date = 0,
                hash = "ca07bbda63cd5e59b3a60bcc4586164f9fd597a7")
        val layout = arrayOf(
                "TAUPE#DAD#KEPIS",
                "ASPEN#OBI#ARENA",
                "STORM#DEN#TROUT",
                "KINDAFOLKSY#PRY",
                "###USA##YODELER",
                "LAKESIDE#PILE##",
                "ARI#ENEMY#DAKAR",
                "SING#TEEUP#NILE",
                "HADNT#DELIS#NON",
                "##WARP#RELOADED",
                "ELITIST##ALL###",
                "VIS#FIVEOFAKIND",
                "ETHEL#SUM#CACAO",
                "NUEVE#ERA#ELOPE",
                "APSES#TON#SINES")
        val hints = arrayOf(
                "1A.Grayish brown",
                "6A.Mom's mate",
                "9A.Military hats",
                "14A.Major ski resort",
                "15A.Sci-fi's ___-Wan",
                "16A.Boxing milieu",
                "17A.Wind nearing 90 knots",
                "18A.Place for a lion ",
                "19A.Fish in a brook",
                "20A.Tinged with Southern charm",
                "23A.Mind another's business",
                "24A.Where Anytown is?",
                "25A.Alpine vocalist",
                "27A.Great camping site",
                "32A.Stack",
                "33A.Jackie's husband No. 2",
                "34A.Foe",
                "36A.Largest city of Senegal",
                "39A.Do more than hum",
                "41A.Start a 300-yard drive",
                "43A.Very long river",
                "44A.\"Didn't\" kin",
                "46A.Supermarket sections",
                "48A.\"Stop\" starter",
                "49A.Bend out of shape",
                "51A.Filled an empty magazine",
                "53A.Stereotypical one-percenter",
                "56A.With zero remaining",
                "57A.Face-to-face word surrounding \"a\"",
                "58A.Impossible poker hand",
                "64A.Waters or Merman",
                "66A.Added total",
                "67A.Bean variety",
                "68A.Highest Spanish single-digit",
                "69A.Historic time period",
                "70A.Wed super-quick",
                "71A.Places for stained-glass windows",
                "72A.Heavy weight",
                "73A.Trig terms",
                "1D.Chore",
                "2D.Piedmont city",
                "3D.Bedtime story word",
                "4D.Big name in chicken",
                "5D.As one big body",
                "6D.Extinct bird",
                "7D.Eve's boy",
                "8D.Small and insignificant",
                "9D.Grasshopper variety",
                "10D.Miss an easy one",
                "11D.We humans, to Trudeau",
                "12D.Cause to accept",
                "13D.Lecherous mythical creature",
                "21D.Hard to hear",
                "22D.Bread-in-gravy action",
                "26D.Splashy style",
                "27D.Mascara target",
                "28D.Opera showstopper",
                "29D.Sweet closing",
                "30D.Proof of ownership",
                "31D.Arab VIP (var.)",
                "35D.\"O Tannenbaum\" season",
                "37D.Vera the healer",
                "38D.Rip or rupture",
                "40D.Wee buzzer",
                "42D.Seasoned rice dish",
                "45D.Small potatoes, figuratively",
                "47D.Comforting thoughts",
                "50D.Penultimate Greek letter",
                "52D.A neutralizer",
                "53D.\"... not ___ mouse\"",
                "54D.Suddenly smiled",
                "55D.Image maker",
                "59D.Dollar, elsewhere",
                "60D.Arabian Peninsula bottom",
                "61D.Cathedral display",
                "62D.Shaved neck part",
                "63D.Performs",
                "65D.Pre-festival time")
    }
}
