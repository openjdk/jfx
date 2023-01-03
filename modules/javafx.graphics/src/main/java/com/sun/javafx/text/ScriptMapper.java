/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.text;

public class ScriptMapper {

    public static final int INVALID    =   -1;  /* Unknown */
    public static final int COMMON       =  0;  /* Zyyy */
    public static final int INHERITED    =  1;  /* Qaai */
    public static final int LATN         =  25;
    private static final int UNKNOWN = 103;

    private static int cache;

    // Must be synchronized because of 'cache' variable
    public static synchronized int getScript(int codePoint) {
        // optimize for runs of characters in the same script
        // ie if this code is called for each char, its somewhat likely
        // that there will be runs from a single range.
        if (codePoint >= data[cache] && codePoint < data[cache+2]) {
            return data[cache+1];
        }

        if (codePoint < CHAR_START || codePoint >= CHAR_LIMIT) {
            return UNKNOWN; // being generous for < 0.
        }

        int probe = dataPower;
        int index = 0;
        if (codePoint >= data[dataExtra]) {
            index = dataExtra;
        }

        while (probe > 2) {
            probe >>= 1;
            if (codePoint >= data[index + probe]) {
                index += probe;
            }
        }

        cache = index;
        return data[index+1];
    }

    /*
     * Referenced by platform code which wants to test for the
     * minimum char code for which layout may be required (non-optional).
     * The value here indicates the lowest char code for which failing
     * to invoke layout would prevent acceptable rendering.
     */
    private static final int MIN_LAYOUT_CHARCODE = 0x0300;

    /*
     * Referenced by platform code which wants to test for the
     * maximum char code for which layout may be required (non-optional).
     * Note this does not account for supplementary characters
     * in the case where the caller interprets 'layout' to mean where
     * one 'char' (ie the java type char) does not map to one glyph
     */
    private static final int MAX_LAYOUT_CHARCODE = 0x206F;

    /* If the character code falls into any of a number of unicode ranges
     * where we know that simple left->right layout mapping chars to glyphs
     * 1:1 and accumulating advances is going to produce incorrect results,
     * we want to know this so the caller can use a more intelligent layout
     * approach. A caller who cares about optimum performance may want to
     * check the first case and skip the method call if its in that range.
     * Although there's a lot of tests in here, knowing you can skip
     * CTL saves a great deal more. The rest of the checks are ordered
     * so that rather than checking explicitly if (>= start & <= end)
     * which would mean all ranges would need to be checked so be sure
     * CTL is not needed, the method returns as soon as it recognises
     * the code point is outside of a CTL ranges.
     * NOTE: Since this method accepts an 'int' it is asssumed to properly
     * represent a CHARACTER. ie it assumes the caller has already
     * converted surrogate pairs into supplementary characters, and so
     * can handle this case and doesn't need to be told such a case is
     * 'complex'.
     */
    public static boolean isComplexCharCode(int code) {

        if (code < MIN_LAYOUT_CHARCODE || code > MAX_LAYOUT_CHARCODE) {
            return false;
        }
        else if (code <= 0x036f) {
            // Trigger layout for combining diacriticals 0x0300->0x036f
            return true;
        }
        else if (code < 0x0590) {
            // No automatic layout for Greek, Cyrillic, Armenian.
             return false;
        }
        else if (code <= 0x06ff) {
            // Hebrew 0590 - 05ff
            // Arabic 0600 - 06ff
            return true;
        }
        else if (code < 0x0900) {
            return false; // Syriac and Thaana
        }
        else if (code <= 0x0e7f) {
            // if Indic, assume shaping for conjuncts, reordering:
            // 0900 - 097F Devanagari
            // 0980 - 09FF Bengali
            // 0A00 - 0A7F Gurmukhi
            // 0A80 - 0AFF Gujarati
            // 0B00 - 0B7F Oriya
            // 0B80 - 0BFF Tamil
            // 0C00 - 0C7F Telugu
            // 0C80 - 0CFF Kannada
            // 0D00 - 0D7F Malayalam
            // 0D80 - 0DFF Sinhala
            // 0E00 - 0E7F if Thai, assume shaping for vowel, tone marks
            return true;
        }
        else if (code <  0x0f00) {
            return false;
        }
        else if (code <= 0x0fff) { // U+0F00 - U+0FFF Tibetan
            return true;
        }
        else if (code <= 0x109f) { // U+1000 - U+109F Myanmar
            return true;
        }
        else if (code < 0x1100) {
            return false;
        }
        else if (code <= 0x11ff) { // U+1100 - U+11FF Old Hangul
            return true;
        }
        else if (code < 0x1780) {
            return false;
        }
        else if (code <= 0x17ff) { // 1780 - 17FF Khmer
            return true;
        }
        else if (code < 0x200c) {
            return false;
        }
        else if (code <= 0x200d) { //  zwj or zwnj
            return true;
        }
        else if (code >= 0x202a && code <= 0x202e) { // directional control
            return true;
        }
        else if (code >= 0x206a && code <= 0x206f) { // directional control
            return true;
        }
        return false;
    }

    /*
     * We need a mapping from unicode code point to ISO script.
     * However the script code constants for ICU do not correspond.
     * This data was generated by starting with the code point to script
     * mappings in JDK7 Character.UnicodeScript, which is up to date
     * with Unicode 6.1. Then a hand created mapping from those codes
     * ICU 4.8 known code points. The data was written out by a utility
     * javafx-font/tools/UnicodeScript.java
     * The format is then as follows.  Where n is an even value:
     * data[n] represents the code point at the start of a range
     * data[n+1] represents the script code for that range
     * data[n+2] represents the first code point in the next range.
     */
    private static int[] data = {
        0x00, 0,        //  Common
        0x041, 25,      //  LATN
        0x05b, 0,       //  Common
        0x061, 25,      //  LATN
        0x07b, 0,       //  Common
        0x0aa, 25,      //  LATN
        0x0ab, 0,       //  Common
        0x0ba, 25,      //  LATN
        0x0bb, 0,       //  Common
        0x0c0, 25,      //  LATN
        0x0d7, 0,       //  Common
        0x0d8, 25,      //  LATN
        0x0f7, 0,       //  Common
        0x0f8, 25,      //  LATN
        0x02b9, 0,      //  Common
        0x02e0, 25,     //  LATN
        0x02e5, 0,      //  Common
        0x02ea, 5,      //  BOPO
        0x02ec, 0,      //  Common
        0x0300, 1,      //  Inherited
        0x0370, 14,     //  GREK
        0x0374, 0,      //  Common
        0x0375, 14,     //  GREK
        0x037e, 0,      //  Common
        0x0384, 14,     //  GREK
        0x0385, 0,      //  Common
        0x0386, 14,     //  GREK
        0x0387, 0,      //  Common
        0x0388, 14,     //  GREK
        0x03e2, 7,      //  COPT
        0x03f0, 14,     //  GREK
        0x0400, 8,      //  CYRL
        0x0485, 1,      //  Inherited
        0x0487, 8,      //  CYRL
        0x0531, 3,      //  ARMN
        0x0589, 0,      //  Common
        0x058a, 3,      //  ARMN
        0x0591, 19,     //  HEBR
        0x0600, 2,      //  ARAB
        0x060c, 0,      //  Common
        0x060d, 2,      //  ARAB
        0x061b, 0,      //  Common
        0x061e, 2,      //  ARAB
        0x061f, 0,      //  Common
        0x0620, 2,      //  ARAB
        0x0640, 0,      //  Common
        0x0641, 2,      //  ARAB
        0x064b, 1,      //  Inherited
        0x0656, 2,      //  ARAB
        0x065f, 1,      //  Inherited
        0x0660, 0,      //  Common
        0x066a, 2,      //  ARAB
        0x0670, 1,      //  Inherited
        0x0671, 2,      //  ARAB
        0x06dd, 0,      //  Common
        0x06de, 2,      //  ARAB
        0x0700, 34,     //  SYRC
        0x0750, 2,      //  ARAB
        0x0780, 37,     //  THAA
        0x07c0, 87,     //  NKOO
        0x0800, 126,    //  SAMR
        0x0840, 84,     //  MAND
        0x08a0, 2,      //  ARAB
        0x0900, 10,     //  DEVA
        0x0951, 1,      //  Inherited
        0x0953, 10,     //  DEVA
        0x0964, 0,      //  Common
        0x0966, 10,     //  DEVA
        0x0981, 4,      //  BENG
        0x0a01, 16,     //  GURU
        0x0a81, 15,     //  GUJR
        0x0b01, 31,     //  ORYA
        0x0b82, 35,     //  TAML
        0x0c01, 36,     //  TELU
        0x0c82, 22,     //  KNDA
        0x0d02, 26,     //  MLYM
        0x0d82, 33,     //  SINH
        0x0e01, 38,     //  THAI
        0x0e3f, 0,      //  Common
        0x0e40, 38,     //  THAI
        0x0e81, 24,     //  LAOO
        0x0f00, 39,     //  TIBT
        0x0fd5, 0,      //  Common
        0x0fd9, 39,     //  TIBT
        0x01000, 28,    //  MYMR
        0x010a0, 12,    //  GEOR
        0x010fb, 0,     //  Common
        0x010fc, 12,    //  GEOR
        0x01100, 18,    //  HANG
        0x01200, 11,    //  ETHI
        0x013a0, 6,     //  CHER
        0x01400, 40,    //  CANS
        0x01680, 29,    //  OGAM
        0x016a0, 32,    //  RUNR
        0x016eb, 0,     //  Common
        0x016ee, 32,    //  RUNR
        0x01700, 42,    //  TGLG
        0x01720, 43,    //  HANO
        0x01735, 0,     //  Common
        0x01740, 44,    //  BUHD
        0x01760, 45,    //  TAGB
        0x01780, 23,    //  KHMR
        0x01800, 27,    //  MONG
        0x01802, 0,     //  Common
        0x01804, 27,    //  MONG
        0x01805, 0,     //  Common
        0x01806, 27,    //  MONG
        0x018b0, 40,    //  CANS
        0x01900, 48,    //  LIMB
        0x01950, 52,    //  TALE
        0x01980, 59,    //  TALU
        0x019e0, 23,    //  KHMR
        0x01a00, 55,    //  BUGI
        0x01a20, 103,   //  LANA
        0x01b00, 62,    //  BALI
        0x01b80, 113,   //  SUND
        0x01bc0, 63,    //  BATK
        0x01c00, 82,    //  LEPC
        0x01c50, 109,   //  OLCK
        0x01cc0, 113,   //  SUND
        0x01cd0, 1,     //  Inherited
        0x01cd3, 0,     //  Common
        0x01cd4, 1,     //  Inherited
        0x01ce1, 0,     //  Common
        0x01ce2, 1,     //  Inherited
        0x01ce9, 0,     //  Common
        0x01ced, 1,     //  Inherited
        0x01cee, 0,     //  Common
        0x01cf4, 1,     //  Inherited
        0x01cf5, 0,     //  Common
        0x01d00, 25,    //  LATN
        0x01d26, 14,    //  GREK
        0x01d2b, 8,     //  CYRL
        0x01d2c, 25,    //  LATN
        0x01d5d, 14,    //  GREK
        0x01d62, 25,    //  LATN
        0x01d66, 14,    //  GREK
        0x01d6b, 25,    //  LATN
        0x01d78, 8,     //  CYRL
        0x01d79, 25,    //  LATN
        0x01dbf, 14,    //  GREK
        0x01dc0, 1,     //  Inherited
        0x01e00, 25,    //  LATN
        0x01f00, 14,    //  GREK
        0x02000, 0,     //  Common
        0x0200c, 1,     //  Inherited
        0x0200e, 0,     //  Common
        0x02071, 25,    //  LATN
        0x02074, 0,     //  Common
        0x0207f, 25,    //  LATN
        0x02080, 0,     //  Common
        0x02090, 25,    //  LATN
        0x020a0, 0,     //  Common
        0x020d0, 1,     //  Inherited
        0x02100, 0,     //  Common
        0x02126, 14,    //  GREK
        0x02127, 0,     //  Common
        0x0212a, 25,    //  LATN
        0x0212c, 0,     //  Common
        0x02132, 25,    //  LATN
        0x02133, 0,     //  Common
        0x0214e, 25,    //  LATN
        0x0214f, 0,     //  Common
        0x02160, 25,    //  LATN
        0x02189, 0,     //  Common
        0x02800, 46,    //  BRAI
        0x02900, 0,     //  Common
        0x02c00, 56,    //  GLAG
        0x02c60, 25,    //  LATN
        0x02c80, 7,     //  COPT
        0x02d00, 12,    //  GEOR
        0x02d30, 60,    //  TFNG
        0x02d80, 11,    //  ETHI
        0x02de0, 8,     //  CYRL
        0x02e00, 0,     //  Common
        0x02e80, 17,    //  HANI
        0x02ff0, 0,     //  Common
        0x03005, 17,    //  HANI
        0x03006, 0,     //  Common
        0x03007, 17,    //  HANI
        0x03008, 0,     //  Common
        0x03021, 17,    //  HANI
        0x0302a, 1,     //  Inherited
        0x0302e, 18,    //  HANG
        0x03030, 0,     //  Common
        0x03038, 17,    //  HANI
        0x0303c, 0,     //  Common
        0x03041, 20,    //  HIRA
        0x03099, 1,     //  Inherited
        0x0309b, 0,     //  Common
        0x0309d, 20,    //  HIRA
        0x030a0, 0,     //  Common
        0x030a1, 22,    //  KANA
        0x030fb, 0,     //  Common
        0x030fd, 22,    //  KANA
        0x03105, 5,     //  BOPO
        0x03131, 18,    //  HANG
        0x03190, 0,     //  Common
        0x031a0, 5,     //  BOPO
        0x031c0, 0,     //  Common
        0x031f0, 22,    //  KANA
        0x03200, 18,    //  HANG
        0x03220, 0,     //  Common
        0x03260, 18,    //  HANG
        0x0327f, 0,     //  Common
        0x032d0, 22,    //  KANA
        0x03358, 0,     //  Common
        0x03400, 17,    //  HANI
        0x04dc0, 0,     //  Common
        0x04e00, 17,    //  HANI
        0x0a000, 41,    //  YIII
        0x0a4d0, 131,   //  LISU
        0x0a500, 99,    //  VAII
        0x0a640, 8,     //  CYRL
        0x0a6a0, 130,   //  BAMU
        0x0a700, 0,     //  Common
        0x0a722, 25,    //  LATN
        0x0a788, 0,     //  Common
        0x0a78b, 25,    //  LATN
        0x0a800, 58,    //  SYLO
        0x0a830, 0,     //  Common
        0x0a840, 90,    //  PHAG
        0x0a880, 111,   //  SAUR
        0x0a8e0, 10,    //  DEVA
        0x0a900, 79,    //  KALI
        0x0a930, 110,   //  RJNG
        0x0a960, 18,    //  HANG
        0x0a980, 78,    //  JAVA
        0x0aa00, 66,    //  CHAM
        0x0aa60, 28,    //  MYMR
        0x0aa80, 127,   //  TAVT
        0x0aae0, 103,   //  MTEI
        0x0ab01, 11,    //  ETHI
        0x0abc0, 103,   //  MTEI
        0x0ac00, 18,    //  HANG
        0x0d7fc, 103,   //  Unknown
        0x0f900, 17,    //  HANI
        0x0fb00, 25,    //  LATN
        0x0fb13, 3,     //  ARMN
        0x0fb1d, 19,    //  HEBR
        0x0fb50, 2,     //  ARAB
        0x0fd3e, 0,     //  Common
        0x0fd50, 2,     //  ARAB
        0x0fdfd, 0,     //  Common
        0x0fe00, 1,     //  Inherited
        0x0fe10, 0,     //  Common
        0x0fe20, 1,     //  Inherited
        0x0fe30, 0,     //  Common
        0x0fe70, 2,     //  ARAB
        0x0feff, 0,     //  Common
        0x0ff21, 25,    //  LATN
        0x0ff3b, 0,     //  Common
        0x0ff41, 25,    //  LATN
        0x0ff5b, 0,     //  Common
        0x0ff66, 22,    //  KANA
        0x0ff70, 0,     //  Common
        0x0ff71, 22,    //  KANA
        0x0ff9e, 0,     //  Common
        0x0ffa0, 18,    //  HANG
        0x0ffe0, 0,     //  Common
        0x010000, 49,   //  LINB
        0x010100, 0,    //  Common
        0x010140, 14,   //  GREK
        0x010190, 0,    //  Common
        0x0101fd, 1,    //  Inherited
        0x010280, 107,  //  LYCI
        0x0102a0, 104,  //  CARI
        0x010300, 30,   //  ITAL
        0x010330, 13,   //  GOTH
        0x010380, 53,   //  UGAR
        0x0103a0, 61,   //  XPEO
        0x010400, 9,    //  DSRT
        0x010450, 51,   //  SHAW
        0x010480, 50,   //  OSMA
        0x010800, 47,   //  CPRT
        0x010840, 116,  //  ARMI
        0x010900, 91,   //  PHNX
        0x010920, 76,   //  LYDI
        0x010980, 86,   //  MERO
        0x0109a0, 141,  //  MERC
        0x010a00, 57,   //  KHAR
        0x010a60, 133,  //  SARB
        0x010b00, 117,  //  AVST
        0x010b40, 125,  //  PRTI
        0x010b60, 122,  //  PHLI
        0x010c00, 103,  //  ORKH
        0x010e60, 2,    //  ARAB
        0x011000, 65,   //  BRAH
        0x011080, 120,  //  KTHI
        0x0110d0, 152,  //  SORA
        0x011100, 118,  //  CAKM
        0x011180, 151,  //  SHRD
        0x011680, 153,  //  TAKR
        0x012000, 101,  //  XSUX
        0x013000, 71,   //  EGYP
        0x016800, 130,  //  BAMU
        0x016f00, 103,  //  PLRD
        0x01b000, 22,   //  KANA
        0x01b001, 20,   //  HIRA
        0x01d000, 0,    //  Common
        0x01d167, 1,    //  Inherited
        0x01d16a, 0,    //  Common
        0x01d17b, 1,    //  Inherited
        0x01d183, 0,    //  Common
        0x01d185, 1,    //  Inherited
        0x01d18c, 0,    //  Common
        0x01d1aa, 1,    //  Inherited
        0x01d1ae, 0,    //  Common
        0x01d200, 14,   //  GREK
        0x01d300, 0,    //  Common
        0x01ee00, 2,    //  ARAB
        0x01f000, 0,    //  Common
        0x01f200, 20,   //  HIRA
        0x01f201, 0,    //  Common
        0x020000, 17,   //  HANI
        0x0e0001, 0,    //  Common
        0x0e0100, 1,    //  Inherited
        0x0e01f0, 103,  //  Unknown
    };

    private static final int dataPower = 1 << 9;
    private static final int dataExtra = data.length - dataPower;
    private static final int CHAR_START = 0;
    private static final int CHAR_LIMIT = data[data.length - 2];

}
