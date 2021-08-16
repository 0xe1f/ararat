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

import org.akop.ararat.io.AmuseFormatter
import org.junit.Test


class TestAmuseFormatter: BaseTest() {

    @Test
    fun crossword_test() {
        proofs.forEach { p ->
            println("${p.filePath}: ")
            AmuseFormatter().load(p.filePath).also { cw -> assertProof(cw, p) }
        }
    }

    companion object {
        val proofs = arrayOf(
                Proof(
                        filePath = "latimes-210808.json",
                        metadata = Metadata(
                                width = 21,
                                height = 21,
                                squareCount = 357,
                                title = "L. A. Times, Sun, Aug 8, 2021 - \"Split Decisions\"",
                                flags = 0,
                                description = "",
                                author = "Bryant White / Ed. Rich Norris",
                                copyright = "© 2021 Tribune Content Agency, LLC",
                                comment = null,
                                date = 1628395200000,
                                hash = "f5b6e7fca1842653905ea1b9d8dd7b1f939b5bc1"
                        ),
                        hints = arrayOf(
                                "1A.Race distance",
                                "5A.Shrink",
                                "12A.Traditional koa wood product",
                                "19A.\"Gentlemen Prefer Blondes\" author Loos",
                                "21A.Unworthy of",
                                "22A.Started up again",
                                "23A.__ officer",
                                "24A.Bishopric cousin",
                                "25A.Like aftershave after a shave",
                                "26A.1994 A.L. batting champ Paul",
                                "28A.*Wine ingredient?",
                                "29A.*Sarah of \"Suits\"",
                                "30A.Pigment used in rustproof primer paints",
                                "32A.Blood lines",
                                "34A.Bananas or nuts",
                                "37A.Monetary \"p\"",
                                "38A.Diamonds, in slang",
                                "43A.Rib-tickler",
                                "46A.Oval-shaped wind",
                                "48A.Benefit",
                                "49A.*Slip through the cracks",
                                "50A.*Traffic stoppers",
                                "51A.Junk bond rating",
                                "54A.Yitzhak's predecessor",
                                "55A.Allegro non __: fast, but not too fast",
                                "57A.Umbrella component",
                                "58A.Problematic to the max",
                                "60A.Gas pump fractions",
                                "61A.Fermented honey drink",
                                "64A.Mountain nymph",
                                "65A.World Cup \"Way to go!\"",
                                "66A.Product with lots of shapes ... or what each of four black squares effectively is?",
                                "70A.Poli-__",
                                "73A.Absinthe flavoring",
                                "74A.A-line line",
                                "75A.Bench warmer?",
                                "77A.Stone set alone",
                                "81A.Basic card game",
                                "83A.1994 Olympic gold medalist skater Baiul",
                                "84A.__ donna",
                                "85A.Triple __: liqueur",
                                "86A.*Take by force",
                                "88A.*\"A Clockwork Orange\" antihero",
                                "89A.Keys",
                                "90A.South American river with a crocodile namesake",
                                "92A.Clumsy boats",
                                "93A.Tribal emblem",
                                "94A.Minty cocktail",
                                "96A.Gun",
                                "98A.Daredevil's stock-in-trade",
                                "99A.Put out",
                                "104A.*Closely match",
                                "109A.*Aconcagua's range",
                                "114A.Conditionally let out",
                                "115A.Winning game after game",
                                "116A.They're seen among the reeds",
                                "118A.Samurai lacking a master",
                                "119A.Go back over",
                                "120A.Superheroes always have them",
                                "121A.They come with strings attached",
                                "122A.Fine-tuned",
                                "123A.Krypton, but not Tatooine",
                                "124A.Canapé spread",
                                "1D.Bruce Wayne lives in one",
                                "2D.Pointless",
                                "3D.Stayed",
                                "4D.Business for many Amazon explorers?",
                                "5D.Agatha Christie's \"The __ Murders\"",
                                "6D.\"The Day the Earth Stood Still\" actress Patricia",
                                "7D.Part of A.D.",
                                "8D.Musician Redbone",
                                "9D.Half the taijitu symbol",
                                "10D.Tried hard",
                                "11D.*Biblical possessive",
                                "12D.Polished",
                                "13D.Grooves made by a saw",
                                "14D.Sky-high gp.",
                                "15D.Time co-founder",
                                "16D.Mideast leader",
                                "17D.Fast time",
                                "18D.On pins and needles",
                                "20D.*Gene variant",
                                "27D.Vientiane native",
                                "29D.Ocasek of the Cars",
                                "31D.Means of access",
                                "33D.*Put on the books",
                                "35D.Elon University st.",
                                "36D.Washington city where Olympic skiers Phil and Steve Mahre were born",
                                "37D.Circle ratios",
                                "38D.*Norse mythology battle used as the subtitle of a 2017 \"Thor\" film",
                                "39D.Egg-shaped",
                                "40D.Racer Yarborough",
                                "41D.Roasts, in a way",
                                "42D.Blind segment",
                                "43D.Brando role in 1978's \"Superman\"",
                                "44D.Critical layer",
                                "45D.Fulfilled",
                                "47D.Defies authority",
                                "49D.Ringling Brothers brother",
                                "50D.Half a Balkan country",
                                "52D.Prefix with -aholic",
                                "53D.Magnum stopper",
                                "56D.*Ghost",
                                "59D.Boston-based sportswear giant",
                                "62D.Hotshot",
                                "63D.Attract",
                                "67D.Goddess with a throne headdress",
                                "68D.Insignificant",
                                "69D.Key of Schubert's \"Trout Quintet\"",
                                "70D.Cornfield sight",
                                "71D.Sundae alternatives",
                                "72D.Big name in movies?",
                                "73D.Mann of 'Til Tuesday",
                                "76D.Winter Palace resident",
                                "77D.Roasting rod",
                                "78D.Rounding phrase",
                                "79D.Merry-go-round tune",
                                "80D.*Dangerous strain",
                                "82D.Derby, perhaps",
                                "86D.Cool",
                                "87D.Anchorage for a galleon",
                                "91D.Mark down, maybe",
                                "92D.*Place abuzz with activity",
                                "94D.Sent raspberries to?",
                                "95D.Text letters often in blue",
                                "97D.Diamond pro",
                                "98D.Location",
                                "100D.Body with arms?",
                                "101D.1994 rival of Nancy",
                                "102D.\"The Cocktail Party\" playwright",
                                "103D.Fog modifier",
                                "104D.Red dessert wine",
                                "105D.From square one",
                                "106D.Appraise",
                                "107D.Cutting-edge brand?",
                                "108D.Security problem",
                                "110D.Iditarod terminus",
                                "111D.Carpe __",
                                "112D.\"__ quam videri\": 35-Down motto",
                                "113D.WWII weapon",
                                "116D.*It's next to nothing",
                                "117D.JFK arrival, once"
                        ),
                        layout = arrayOf(
                                "MILE##ANALYST#UKULELE",
                                "ANITA#BENEATH#RESUMED",
                                "NAVAL#CANONRY#BRACING",
                                "ONEILL#LONGI#RAFFERTY",
                                "REDLEAD####VEINS#####",
                                "####LOONY#PENCE#ROCKS",
                                "#JOKE#OCARINA###AVAIL",
                                "OOZE#BRAKES#CCC#GOLDA",
                                "TROPPO#RIB##THORNIEST",
                                "TENTHS##MEAD#OREAD###",
                                "OLE#ANIMALCRACKER#SCI",
                                "###ANISE#SEAM##BOTTOM",
                                "SOLITAIRE##WAR#OKSANA",
                                "PRIMA#SEC#HIJACK#ALEX",
                                "ISLES###ORINOCO#ARKS#",
                                "TOTEM#JULEP#REVUP####",
                                "#####PERIL####EMITTED",
                                "PARALLEL#ANDES#PAROLE",
                                "ONATEAR#OBOISTS#RONIN",
                                "RETRACE#NEMESES#YOYOS",
                                "TWEAKED#ELEMENT##PATE"
                        )
                )
        )
    }
}
