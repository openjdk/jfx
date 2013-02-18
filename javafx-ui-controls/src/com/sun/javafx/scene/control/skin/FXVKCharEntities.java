/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import java.util.HashMap;

/**
 * Provides a mapping from HTML ISO-8859-1 character entity names to Unicode.
 */
class FXVKCharEntities  {
    private static final HashMap<String, Character> map = new HashMap<String, Character>();

    public static String get(String name) {
        Character c = map.get(name);
        if (c == null) {
            if (name.length() != 1) {
                //System.err.println("FXVKCharEntities: Unknown HTML entity name <"+name+">");
            }
            return name;
        } else {
            return ""+c;
        }
    }

    private static void put(String name, int decimalChar) {
        map.put(name, (char)decimalChar);
    }

    static {
        put("space",     32); // Non-standard
        put("quot",      34);
        put("apos",      39);
        put("amp",       38);
        put("lt",        60);
        put("gt",        62);
        put("nbsp",     160);
        put("iexcl",    161);
        put("cent",     162);
        put("pound",    163);
        put("curren",   164);
        put("yen",      165);
        put("brvbar",   166);
        put("sect",     167);
        put("uml",      168);
        put("copy",     169);
        put("ordf",     170);
        put("laquo",    171);
        put("not",      172);
        put("shy",      173);
        put("reg",      174);
        put("macr",     175);
        put("deg",      176);
        put("plusmn",   177);
        put("sup2",     178);
        put("sup3",     179);
        put("acute",    180);
        put("micro",    181);
        put("para",     182);
        put("middot",   183);
        put("cedil",    184);
        put("sup1",     185);
        put("ordm",     186);
        put("raquo",    187);
        put("frac14",   188);
        put("frac12",   189);
        put("frac34",   190);
        put("iquest",   191);
        put("times",    215);
        put("divide",   247);

        put("Agrave",   192);
        put("Aacute",   193);
        put("Acirc",    194);
        put("Atilde",   195);
        put("Auml",     196);
        put("Aring",    197);
        put("AElig",    198);
        put("Ccedil",   199);
        put("Egrave",   200);
        put("Eacute",   201);
        put("Ecirc",    202);
        put("Euml",     203);
        put("Igrave",   204);
        put("Iacute",   205);
        put("Icirc",    206);
        put("Iuml",     207);
        put("ETH",      208);
        put("Ntilde",   209);
        put("Ograve",   210);
        put("Oacute",   211);
        put("Ocirc",    212);
        put("Otilde",   213);
        put("Ouml",     214);
        put("Oslash",   216);
        put("Ugrave",   217);
        put("Uacute",   218);
        put("Ucirc",    219);
        put("Uuml",     220);
        put("Yacute",   221);
        put("THORN",    222);
        put("szlig",    223);
        put("agrave",   224);
        put("aacute",   225);
        put("acirc",    226);
        put("atilde",   227);
        put("auml",     228);
        put("aring",    229);
        put("aelig",    230);
        put("ccedil",   231);
        put("egrave",   232);
        put("eacute",   233);
        put("ecirc",    234);
        put("euml",     235);
        put("igrave",   236);
        put("iacute",   237);
        put("icirc",    238);
        put("iuml",     239);
        put("eth",      240);
        put("ntilde",   241);
        put("ograve",   242);
        put("oacute",   243);
        put("ocirc",    244);
        put("otilde",   245);
        put("ouml",     246);
        put("oslash",   248);
        put("ugrave",   249);
        put("uacute",   250);
        put("ucirc",    251);
        put("uuml",     252);
        put("yacute",   253);
        put("thorn",    254);
        put("yuml",     255);

        put("scedil", 0x015f); // Non-standard
        put("scaron", 0x0161); // Non-standard
        put("ycirc",  0x0177); // Non-standard
        put("ymacron",0x0233); // Non-standard
        put("pi",     0x03c0); // Non-standard
        put("sigma",  0x03c3); // Non-standard
        put("ygrave", 0x1ef3); // Non-standard
        put("yhook",  0x1ef7); // Non-standard
        put("permil", 0x2030); // Non-standard
        put("euro",   0x20ac); // Non-standard
        put("tm",     0x2122); // Non-standard
        put("neq",    0x2260); // Non-standard
    }
}
