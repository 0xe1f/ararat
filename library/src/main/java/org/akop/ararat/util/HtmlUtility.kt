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


object HtmlUtility {

    private val entityList = listOf(
        Pair("amp", 38),
        Pair("lt", 60),
        Pair("gt", 62),
        Pair("Agrave", 192),
        Pair("Aacute", 193),
        Pair("Acirc", 194),
        Pair("Atilde", 195),
        Pair("Auml", 196),
        Pair("Aring", 197),
        Pair("AElig", 198),
        Pair("Ccedil", 199),
        Pair("Egrave", 200),
        Pair("Eacute", 201),
        Pair("Ecirc", 202),
        Pair("Euml", 203),
        Pair("Igrave", 204),
        Pair("Iacute", 205),
        Pair("Icirc", 206),
        Pair("Iuml", 207),
        Pair("ETH", 208),
        Pair("Ntilde", 209),
        Pair("Ograve", 210),
        Pair("Oacute", 211),
        Pair("Ocirc", 212),
        Pair("Otilde", 213),
        Pair("Ouml", 214),
        Pair("Oslash", 216),
        Pair("Ugrave", 217),
        Pair("Uacute", 218),
        Pair("Ucirc", 219),
        Pair("Uuml", 220),
        Pair("Yacute", 221),
        Pair("THORN", 222),
        Pair("szlig", 223),
        Pair("agrave", 224),
        Pair("aacute", 225),
        Pair("acirc", 226),
        Pair("atilde", 227),
        Pair("auml", 228),
        Pair("aring", 229),
        Pair("aelig", 230),
        Pair("ccedil", 231),
        Pair("egrave", 232),
        Pair("eacute", 233),
        Pair("ecirc", 234),
        Pair("euml", 235),
        Pair("igrave", 236),
        Pair("iacute", 237),
        Pair("icirc", 238),
        Pair("iuml", 239),
        Pair("eth", 240),
        Pair("ntilde", 241),
        Pair("ograve", 242),
        Pair("oacute", 243),
        Pair("ocirc", 244),
        Pair("otilde", 245),
        Pair("ouml", 246),
        Pair("oslash", 248),
        Pair("ugrave", 249),
        Pair("uacute", 250),
        Pair("ucirc", 251),
        Pair("uuml", 252),
        Pair("yacute", 253),
        Pair("thorn", 254),
        Pair("yuml", 255),
        Pair("nbsp", 160),
        Pair("iexcl", 161),
        Pair("cent", 162),
        Pair("pound", 163),
        Pair("curren", 164),
        Pair("yen", 165),
        Pair("brvbar", 166),
        Pair("sect", 167),
        Pair("uml", 168),
        Pair("copy", 169),
        Pair("ordf", 170),
        Pair("laquo", 171),
        Pair("not", 172),
        Pair("shy", 173),
        Pair("reg", 174),
        Pair("macr", 175),
        Pair("deg", 176),
        Pair("plusmn", 177),
        Pair("sup2", 178),
        Pair("sup3", 179),
        Pair("acute", 180),
        Pair("micro", 181),
        Pair("para", 182),
        Pair("cedil", 184),
        Pair("sup1", 185),
        Pair("ordm", 186),
        Pair("raquo", 187),
        Pair("frac14", 188),
        Pair("frac12", 189),
        Pair("frac34", 190),
        Pair("iquest", 191),
        Pair("times", 215),
        Pair("divide", 247),
        Pair("forall", 8704),
        Pair("part", 8706),
        Pair("exist", 8707),
        Pair("empty", 8709),
        Pair("nabla", 8711),
        Pair("isin", 8712),
        Pair("notin", 8713),
        Pair("ni", 8715),
        Pair("prod", 8719),
        Pair("sum", 8721),
        Pair("minus", 8722),
        Pair("lowast", 8727),
        Pair("radic", 8730),
        Pair("prop", 8733),
        Pair("infin", 8734),
        Pair("ang", 8736),
        Pair("and", 8743),
        Pair("or", 8744),
        Pair("cap", 8745),
        Pair("cup", 8746),
        Pair("int", 8747),
        Pair("there4", 8756),
        Pair("sim", 8764),
        Pair("cong", 8773),
        Pair("asymp", 8776),
        Pair("ne", 8800),
        Pair("equiv", 8801),
        Pair("le", 8804),
        Pair("ge", 8805),
        Pair("sub", 8834),
        Pair("sup", 8835),
        Pair("nsub", 8836),
        Pair("sube", 8838),
        Pair("supe", 8839),
        Pair("oplus", 8853),
        Pair("otimes", 8855),
        Pair("perp", 8869),
        Pair("sdot", 8901),
        Pair("Alpha", 913),
        Pair("Beta", 914),
        Pair("Gamma", 915),
        Pair("Delta", 916),
        Pair("Epsilon", 917),
        Pair("Zeta", 918),
        Pair("Eta", 919),
        Pair("Theta", 920),
        Pair("Iota", 921),
        Pair("Kappa", 922),
        Pair("Lambda", 923),
        Pair("Mu", 924),
        Pair("Nu", 925),
        Pair("Xi", 926),
        Pair("Omicron", 927),
        Pair("Pi", 928),
        Pair("Rho", 929),
        Pair("Sigma", 931),
        Pair("Tau", 932),
        Pair("Upsilon", 933),
        Pair("Phi", 934),
        Pair("Chi", 935),
        Pair("Psi", 936),
        Pair("Omega", 937),
        Pair("alpha", 945),
        Pair("beta", 946),
        Pair("gamma", 947),
        Pair("delta", 948),
        Pair("epsilon", 949),
        Pair("zeta", 950),
        Pair("eta", 951),
        Pair("theta", 952),
        Pair("iota", 953),
        Pair("kappa", 954),
        Pair("lambda", 955),
        Pair("mu", 956),
        Pair("nu", 957),
        Pair("xi", 958),
        Pair("omicron", 959),
        Pair("pi", 960),
        Pair("rho", 961),
        Pair("sigmaf", 962),
        Pair("sigma", 963),
        Pair("tau", 964),
        Pair("upsilon", 965),
        Pair("phi", 966),
        Pair("chi", 967),
        Pair("psi", 968),
        Pair("omega", 969),
        Pair("thetasym", 977),
        Pair("upsih", 978),
        Pair("piv", 982),
        Pair("OElig", 338),
        Pair("oelig", 339),
        Pair("Scaron", 352),
        Pair("scaron", 353),
        Pair("Yuml", 376),
        Pair("fnof", 402),
        Pair("circ", 710),
        Pair("tilde", 732),
        Pair("ensp", 8194),
        Pair("emsp", 8195),
        Pair("thinsp", 8201),
        Pair("zwnj", 8204),
        Pair("zwj", 8205),
        Pair("lrm", 8206),
        Pair("rlm", 8207),
        Pair("ndash", 8211),
        Pair("mdash", 8212),
        Pair("lsquo", 8216),
        Pair("rsquo", 8217),
        Pair("sbquo", 8218),
        Pair("ldquo", 8220),
        Pair("rdquo", 8221),
        Pair("bdquo", 8222),
        Pair("dagger", 8224),
        Pair("Dagger", 8225),
        Pair("bull", 8226),
        Pair("hellip", 8230),
        Pair("permil", 8240),
        Pair("prime", 8242),
        Pair("Prime", 8243),
        Pair("lsaquo", 8249),
        Pair("rsaquo", 8250),
        Pair("oline", 8254),
        Pair("euro", 8364),
        Pair("trade", 8482),
        Pair("larr", 8592),
        Pair("uarr", 8593),
        Pair("rarr", 8594),
        Pair("darr", 8595),
        Pair("harr", 8596),
        Pair("crarr", 8629),
        Pair("lceil", 8968),
        Pair("rceil", 8969),
        Pair("lfloor", 8970),
        Pair("rfloor", 8971),
        Pair("loz", 9674),
        Pair("spades", 9824),
        Pair("clubs", 9827),
        Pair("hearts", 9829),
        Pair("diams", 9830)
    ).sortedBy { it.first }

    fun stripEntities(s: String): String {
        val out = CharArray(s.length)
        var r = -1
        var w = 0

        outer@while (++r < s.length) {
            val ch = s[r]
            if (ch == '&') {
                if (r + 1 < s.length && s[r + 1] == '#') {
                    // Potential numeric entity
                    var j = r + 1
                    inner@while (++j < s.length) {
                        when (s[j]) {
                            ';' -> {
                                val v = s.substring(r + 2 until j)
                                if (v.isEmpty()) break@inner // No digits

                                out[w++] = v.toInt().toChar()
                                r = j
                                continue@outer
                            }
                            !in '0'..'9' -> break@inner
                        }
                    }
                } else {
                    // Potential named entity
                    var j = r
                    inner@while (++j < s.length) {
                        val ch2 = s[j]
                        when {
                            ch2 == ';' -> {
                                // potential valid entity name
                                val name = s.substring(r + 1 until j)
                                val p = entityList.binarySearchBy(name) { it.first }
                                if (p < 0) break@inner // No match

                                out[w++] = entityList[p].second.toChar()
                                r = j
                                continue@outer
                            }
                            ch2 !in 'a'..'z' && ch2 !in 'A'..'Z' -> break@inner
                        }
                    }
                }
            }
            out[w++] = ch
        }

        return String(out, 0, w)
    }
}
