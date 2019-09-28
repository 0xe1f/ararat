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


/**
 * Provides common HTML-related functionality.
 */
object HtmlUtility {

    // Almost certainly not exhaustive
    private val entities = listOf(
            Entity("amp", 38),
            Entity("lt", 60),
            Entity("gt", 62),
            Entity("quot", 34),
            Entity("apos", 39),
            Entity("Agrave", 192),
            Entity("Aacute", 193),
            Entity("Acirc", 194),
            Entity("Atilde", 195),
            Entity("Auml", 196),
            Entity("Aring", 197),
            Entity("AElig", 198),
            Entity("Ccedil", 199),
            Entity("Egrave", 200),
            Entity("Eacute", 201),
            Entity("Ecirc", 202),
            Entity("Euml", 203),
            Entity("Igrave", 204),
            Entity("Iacute", 205),
            Entity("Icirc", 206),
            Entity("Iuml", 207),
            Entity("ETH", 208),
            Entity("Ntilde", 209),
            Entity("Ograve", 210),
            Entity("Oacute", 211),
            Entity("Ocirc", 212),
            Entity("Otilde", 213),
            Entity("Ouml", 214),
            Entity("Oslash", 216),
            Entity("Ugrave", 217),
            Entity("Uacute", 218),
            Entity("Ucirc", 219),
            Entity("Uuml", 220),
            Entity("Yacute", 221),
            Entity("THORN", 222),
            Entity("szlig", 223),
            Entity("agrave", 224),
            Entity("aacute", 225),
            Entity("acirc", 226),
            Entity("atilde", 227),
            Entity("auml", 228),
            Entity("aring", 229),
            Entity("aelig", 230),
            Entity("ccedil", 231),
            Entity("egrave", 232),
            Entity("eacute", 233),
            Entity("ecirc", 234),
            Entity("euml", 235),
            Entity("igrave", 236),
            Entity("iacute", 237),
            Entity("icirc", 238),
            Entity("iuml", 239),
            Entity("eth", 240),
            Entity("ntilde", 241),
            Entity("ograve", 242),
            Entity("oacute", 243),
            Entity("ocirc", 244),
            Entity("otilde", 245),
            Entity("ouml", 246),
            Entity("oslash", 248),
            Entity("ugrave", 249),
            Entity("uacute", 250),
            Entity("ucirc", 251),
            Entity("uuml", 252),
            Entity("yacute", 253),
            Entity("thorn", 254),
            Entity("yuml", 255),
            Entity("nbsp", 160),
            Entity("iexcl", 161),
            Entity("cent", 162),
            Entity("pound", 163),
            Entity("curren", 164),
            Entity("yen", 165),
            Entity("brvbar", 166),
            Entity("sect", 167),
            Entity("uml", 168),
            Entity("copy", 169),
            Entity("ordf", 170),
            Entity("laquo", 171),
            Entity("not", 172),
            Entity("shy", 173),
            Entity("reg", 174),
            Entity("macr", 175),
            Entity("deg", 176),
            Entity("plusmn", 177),
            Entity("sup2", 178),
            Entity("sup3", 179),
            Entity("acute", 180),
            Entity("micro", 181),
            Entity("para", 182),
            Entity("cedil", 184),
            Entity("sup1", 185),
            Entity("ordm", 186),
            Entity("raquo", 187),
            Entity("frac14", 188),
            Entity("frac12", 189),
            Entity("frac34", 190),
            Entity("iquest", 191),
            Entity("times", 215),
            Entity("divide", 247),
            Entity("forall", 8704),
            Entity("part", 8706),
            Entity("exist", 8707),
            Entity("empty", 8709),
            Entity("nabla", 8711),
            Entity("isin", 8712),
            Entity("notin", 8713),
            Entity("ni", 8715),
            Entity("prod", 8719),
            Entity("sum", 8721),
            Entity("minus", 8722),
            Entity("lowast", 8727),
            Entity("radic", 8730),
            Entity("prop", 8733),
            Entity("infin", 8734),
            Entity("ang", 8736),
            Entity("and", 8743),
            Entity("or", 8744),
            Entity("cap", 8745),
            Entity("cup", 8746),
            Entity("int", 8747),
            Entity("there4", 8756),
            Entity("sim", 8764),
            Entity("cong", 8773),
            Entity("asymp", 8776),
            Entity("ne", 8800),
            Entity("equiv", 8801),
            Entity("le", 8804),
            Entity("ge", 8805),
            Entity("sub", 8834),
            Entity("sup", 8835),
            Entity("nsub", 8836),
            Entity("sube", 8838),
            Entity("supe", 8839),
            Entity("oplus", 8853),
            Entity("otimes", 8855),
            Entity("perp", 8869),
            Entity("sdot", 8901),
            Entity("Alpha", 913),
            Entity("Beta", 914),
            Entity("Gamma", 915),
            Entity("Delta", 916),
            Entity("Epsilon", 917),
            Entity("Zeta", 918),
            Entity("Eta", 919),
            Entity("Theta", 920),
            Entity("Iota", 921),
            Entity("Kappa", 922),
            Entity("Lambda", 923),
            Entity("Mu", 924),
            Entity("Nu", 925),
            Entity("Xi", 926),
            Entity("Omicron", 927),
            Entity("Pi", 928),
            Entity("Rho", 929),
            Entity("Sigma", 931),
            Entity("Tau", 932),
            Entity("Upsilon", 933),
            Entity("Phi", 934),
            Entity("Chi", 935),
            Entity("Psi", 936),
            Entity("Omega", 937),
            Entity("alpha", 945),
            Entity("beta", 946),
            Entity("gamma", 947),
            Entity("delta", 948),
            Entity("epsilon", 949),
            Entity("zeta", 950),
            Entity("eta", 951),
            Entity("theta", 952),
            Entity("iota", 953),
            Entity("kappa", 954),
            Entity("lambda", 955),
            Entity("mu", 956),
            Entity("nu", 957),
            Entity("xi", 958),
            Entity("omicron", 959),
            Entity("pi", 960),
            Entity("rho", 961),
            Entity("sigmaf", 962),
            Entity("sigma", 963),
            Entity("tau", 964),
            Entity("upsilon", 965),
            Entity("phi", 966),
            Entity("chi", 967),
            Entity("psi", 968),
            Entity("omega", 969),
            Entity("thetasym", 977),
            Entity("upsih", 978),
            Entity("piv", 982),
            Entity("OElig", 338),
            Entity("oelig", 339),
            Entity("Scaron", 352),
            Entity("scaron", 353),
            Entity("Yuml", 376),
            Entity("fnof", 402),
            Entity("circ", 710),
            Entity("tilde", 732),
            Entity("ensp", 8194),
            Entity("emsp", 8195),
            Entity("thinsp", 8201),
            Entity("zwnj", 8204),
            Entity("zwj", 8205),
            Entity("lrm", 8206),
            Entity("rlm", 8207),
            Entity("ndash", 8211),
            Entity("mdash", 8212),
            Entity("lsquo", 8216),
            Entity("rsquo", 8217),
            Entity("sbquo", 8218),
            Entity("ldquo", 8220),
            Entity("rdquo", 8221),
            Entity("bdquo", 8222),
            Entity("dagger", 8224),
            Entity("Dagger", 8225),
            Entity("bull", 8226),
            Entity("hellip", 8230),
            Entity("permil", 8240),
            Entity("prime", 8242),
            Entity("Prime", 8243),
            Entity("lsaquo", 8249),
            Entity("rsaquo", 8250),
            Entity("oline", 8254),
            Entity("euro", 8364),
            Entity("trade", 8482),
            Entity("larr", 8592),
            Entity("uarr", 8593),
            Entity("rarr", 8594),
            Entity("darr", 8595),
            Entity("harr", 8596),
            Entity("crarr", 8629),
            Entity("lceil", 8968),
            Entity("rceil", 8969),
            Entity("lfloor", 8970),
            Entity("rfloor", 8971),
            Entity("loz", 9674),
            Entity("spades", 9824),
            Entity("clubs", 9827),
            Entity("hearts", 9829),
            Entity("diams", 9830)
    ).sortedBy { it.name }

    /**
     * Strips named and numeric HTML entities from a string, replacing with
     * Unicode equivalents where possible, removing where no known match.
     * Badly formatted entities (e.g. "&amp") are not modified.
     *
     * As dictated by HTML, entity names are case-sensitive.
     */
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
                    inner@while (++j < s.length) when (s[j]) {
                        ';' -> {
                            if (r + 2 < j)
                                out[w++] = s.substring(r + 2 until j).toInt().toChar()

                            r = j
                            continue@outer
                        }
                        in '0'..'9' -> {}
                        else -> break@inner
                    }
                } else {
                    // Potential named entity
                    var j = r
                    inner@while (++j < s.length) when (s[j]) {
                        ';' -> {
                            // potential valid entity name
                            val name = s.substring(r + 1 until j)
                            val p = entities.binarySearchBy(name) { it.name }
                            if (p >= 0)
                                out[w++] = entities[p].code.toChar()

                            r = j
                            continue@outer
                        }
                        in 'a'..'z', in 'A'..'Z', in '0'..'9' -> {}
                        else -> break@inner
                    }
                }
            }
            out[w++] = ch
        }

        return String(out, 0, w)
    }

    private class Entity(val name: String,
                         val code: Int)
}

/**
 * Extension method for [HtmlUtility.stripEntities].
 */
fun String.stripHtmlEntities(): String = HtmlUtility.stripEntities(this)
