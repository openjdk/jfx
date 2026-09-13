/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.parser;

import javafx.scene.paint.Color;
import java.util.Map;
import java.util.TreeMap;

import static javafx.scene.paint.Color.*;

public final class CssColorParser {

    private CssColorParser() {}

    /**
     * Parses a color string as specified by {@link Color#web(String)}.
     *
     * @return the color, or {@code null} if {@code colorString} is not a valid color representation
     */
    public static Color tryParseColor(String colorString) {
        return tryParseColor(colorString, 1);
    }

    /**
     * Parses a color string as specified by {@link Color#web(String, double)}.
     *
     * @return the color, or {@code null} if {@code colorString} is not a valid color representation
     */
    public static Color tryParseColor(String colorString, double opacity) {
        if (colorString == null || colorString.isEmpty()) {
            return null;
        }

        int offset = 0;

        if (colorString.charAt(0) == '#') {
            offset = 1;
        } else if (colorString.regionMatches(true, 0, "0x", 0, 2)) {
            offset = 2;
        } else if (colorString.regionMatches(true, 0, "rgb", 0, 3)) {
            if (colorString.charAt(3) == '(') {
                return parseRGBColor(colorString, 4, false, opacity);
            } else if (colorString.regionMatches(true, 3, "a(", 0, 2)) {
                return parseRGBColor(colorString, 5, true, opacity);
            }
        } else if (colorString.regionMatches(true, 0, "hsl", 0, 3)) {
            if (colorString.charAt(3) == '(') {
                return parseHSLColor(colorString, 4, false, opacity);
            } else if (colorString.regionMatches(true, 3, "a(", 0, 2)) {
                return parseHSLColor(colorString, 5, true, opacity);
            }
        } else {
            Color col = NAMED_COLORS.get(colorString);
            if (col != null) {
                if (opacity == 1.0) {
                    return col;
                } else {
                    return Color.color(col.getRed(), col.getGreen(), col.getBlue(), opacity);
                }
            }
        }

        return switch (colorString.length() - offset) {
            case 3 -> {
                long r = CssNumberParser.tryParseInt(colorString, offset, offset + 1, 16);
                long g = CssNumberParser.tryParseInt(colorString, offset + 1, offset + 2, 16);
                long b = CssNumberParser.tryParseInt(colorString, offset + 2, offset + 3, 16);
                yield validInts(r, g, b) ? Color.color(r / 15.0, g / 15.0, b / 15.0, opacity) : null;
            }

            case 4 -> {
                long r = CssNumberParser.tryParseInt(colorString, offset, offset + 1, 16);
                long g = CssNumberParser.tryParseInt(colorString, offset + 1, offset + 2, 16);
                long b = CssNumberParser.tryParseInt(colorString, offset + 2, offset + 3, 16);
                long a = CssNumberParser.tryParseInt(colorString, offset + 3, offset + 4, 16);
                yield validInts(r, g, b, a) ? Color.color(r / 15.0, g / 15.0, b / 15.0, opacity * a / 15.0) : null;
            }

            case 6 -> {
                long r = CssNumberParser.tryParseInt(colorString, offset, offset + 2, 16);
                long g = CssNumberParser.tryParseInt(colorString, offset + 2, offset + 4, 16);
                long b = CssNumberParser.tryParseInt(colorString, offset + 4, offset + 6, 16);
                yield validInts(r, g, b) ? Color.rgb((int)r, (int)g, (int)b, opacity) : null;
            }

            case 8 -> {
                long r = CssNumberParser.tryParseInt(colorString, offset, offset + 2, 16);
                long g = CssNumberParser.tryParseInt(colorString, offset + 2, offset + 4, 16);
                long b = CssNumberParser.tryParseInt(colorString, offset + 4, offset + 6, 16);
                long a = CssNumberParser.tryParseInt(colorString, offset + 6, offset + 8, 16);
                yield validInts(r, g, b, a) ? Color.rgb((int)r, (int)g, (int)b, opacity * a / 255.0) : null;
            }

            default -> null;
        };
    }

    private static Color parseRGBColor(String color, int roff, boolean hasAlpha, double a) {
        int rend = color.indexOf(',', roff);
        int gend = rend < 0 ? -1 : color.indexOf(',', rend+1);
        int bend = gend < 0 ? -1 : color.indexOf(hasAlpha ? ',' : ')', gend+1);
        int aend = hasAlpha ? (bend < 0 ? -1 : color.indexOf(')', bend+1)) : bend;
        if (aend >= 0) {
            double r = parseComponent(color, roff, rend, PARSE_COMPONENT);
            double g = parseComponent(color, rend+1, gend, PARSE_COMPONENT);
            double b = parseComponent(color, gend+1, bend, PARSE_COMPONENT);
            if (hasAlpha) {
                a *= parseComponent(color, bend+1, aend, PARSE_ALPHA);
            }

            return Double.isNaN(r) || Double.isNaN(g) || Double.isNaN(b) || Double.isNaN(a)
                ? null
                : new Color(r, g, b, a);
        }

        return null;
    }

    private static Color parseHSLColor(String color, int hoff, boolean hasAlpha, double a) {
        int hend = color.indexOf(',', hoff);
        int send = hend < 0 ? -1 : color.indexOf(',', hend+1);
        int lend = send < 0 ? -1 : color.indexOf(hasAlpha ? ',' : ')', send+1);
        int aend = hasAlpha ? (lend < 0 ? -1 : color.indexOf(')', lend+1)) : lend;
        if (aend >= 0) {
            double h = parseComponent(color, hoff, hend, PARSE_ANGLE);
            double s = parseComponent(color, hend+1, send, PARSE_PERCENT);
            double l = parseComponent(color, send+1, lend, PARSE_PERCENT);
            if (hasAlpha) {
                a *= parseComponent(color, lend+1, aend, PARSE_ALPHA);
            }

            return Double.isNaN(h) || Double.isNaN(s) || Double.isNaN(l) || Double.isNaN(a)
                ? null
                : Color.hsb(h, s, l, a);
        }

        return null;
    }

    private static final int PARSE_COMPONENT = 0; // percent, or clamped to [0,255] => [0,1]
    private static final int PARSE_PERCENT = 1; // clamped to [0,100]% => [0,1]
    private static final int PARSE_ANGLE = 2; // clamped to [0,360]
    private static final int PARSE_ALPHA = 3; // clamped to [0.0,1.0]

    private static double parseComponent(String color, int off, int end, int type) {
        int start = off;
        int limit = end;

        while (start < limit && Character.isWhitespace(color.charAt(start))) {
            start++;
        }

        while (limit > start && Character.isWhitespace(color.charAt(limit - 1))) {
            limit--;
        }

        if (start >= limit) {
            return Double.NaN;
        }

        if (color.charAt(limit - 1) == '%') {
            if (type > PARSE_PERCENT) {
                return Double.NaN;
            }

            type = PARSE_PERCENT;
            limit--;

            while (limit > start && Character.isWhitespace(color.charAt(limit - 1))) {
                limit--;
            }

            if (start >= limit) {
                return Double.NaN;
            }
        } else if (type == PARSE_PERCENT) {
            return Double.NaN;
        }

        double c;
        if (type == PARSE_COMPONENT) {
            long v = CssNumberParser.tryParseInt(color, start, limit, 10);
            c = validInt(v) ? v : Double.NaN;
        } else {
            c = CssNumberParser.tryParseDouble(color, start, limit);
        }

        if (Double.isNaN(c)) {
            return Double.NaN;
        }

        switch (type) {
            case PARSE_ALPHA:
                return (c < 0.0) ? 0.0 : ((c > 1.0) ? 1.0 : c);
            case PARSE_PERCENT:
                return (c <= 0.0) ? 0.0 : ((c >= 100.0) ? 1.0 : (c / 100.0));
            case PARSE_COMPONENT:
                return (c <= 0.0) ? 0.0 : ((c >= 255.0) ? 1.0 : (c / 255.0));
            case PARSE_ANGLE:
                return ((c < 0.0)
                        ? ((c % 360.0) + 360.0)
                        : ((c > 360.0)
                            ? (c % 360.0)
                            : c));
        }

        return Double.NaN;
    }

    private static boolean validInt(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    private static boolean validInts(long value1, long value2, long value3) {
        return validInt(value1) && validInt(value2) && validInt(value3);
    }

    private static boolean validInts(long value1, long value2, long value3, long value4) {
        return validInts(value1, value2, value3) && validInt(value4);
    }

    private static final Map<String, Color> NAMED_COLORS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static  {
        NAMED_COLORS.put("aliceblue",            ALICEBLUE);
        NAMED_COLORS.put("antiquewhite",         ANTIQUEWHITE);
        NAMED_COLORS.put("aqua",                 AQUA);
        NAMED_COLORS.put("aquamarine",           AQUAMARINE);
        NAMED_COLORS.put("azure",                AZURE);
        NAMED_COLORS.put("beige",                BEIGE);
        NAMED_COLORS.put("bisque",               BISQUE);
        NAMED_COLORS.put("black",                BLACK);
        NAMED_COLORS.put("blanchedalmond",       BLANCHEDALMOND);
        NAMED_COLORS.put("blue",                 BLUE);
        NAMED_COLORS.put("blueviolet",           BLUEVIOLET);
        NAMED_COLORS.put("brown",                BROWN);
        NAMED_COLORS.put("burlywood",            BURLYWOOD);
        NAMED_COLORS.put("cadetblue",            CADETBLUE);
        NAMED_COLORS.put("chartreuse",           CHARTREUSE);
        NAMED_COLORS.put("chocolate",            CHOCOLATE);
        NAMED_COLORS.put("coral",                CORAL);
        NAMED_COLORS.put("cornflowerblue",       CORNFLOWERBLUE);
        NAMED_COLORS.put("cornsilk",             CORNSILK);
        NAMED_COLORS.put("crimson",              CRIMSON);
        NAMED_COLORS.put("cyan",                 CYAN);
        NAMED_COLORS.put("darkblue",             DARKBLUE);
        NAMED_COLORS.put("darkcyan",             DARKCYAN);
        NAMED_COLORS.put("darkgoldenrod",        DARKGOLDENROD);
        NAMED_COLORS.put("darkgray",             DARKGRAY);
        NAMED_COLORS.put("darkgreen",            DARKGREEN);
        NAMED_COLORS.put("darkgrey",             DARKGREY);
        NAMED_COLORS.put("darkkhaki",            DARKKHAKI);
        NAMED_COLORS.put("darkmagenta",          DARKMAGENTA);
        NAMED_COLORS.put("darkolivegreen",       DARKOLIVEGREEN);
        NAMED_COLORS.put("darkorange",           DARKORANGE);
        NAMED_COLORS.put("darkorchid",           DARKORCHID);
        NAMED_COLORS.put("darkred",              DARKRED);
        NAMED_COLORS.put("darksalmon",           DARKSALMON);
        NAMED_COLORS.put("darkseagreen",         DARKSEAGREEN);
        NAMED_COLORS.put("darkslateblue",        DARKSLATEBLUE);
        NAMED_COLORS.put("darkslategray",        DARKSLATEGRAY);
        NAMED_COLORS.put("darkslategrey",        DARKSLATEGREY);
        NAMED_COLORS.put("darkturquoise",        DARKTURQUOISE);
        NAMED_COLORS.put("darkviolet",           DARKVIOLET);
        NAMED_COLORS.put("deeppink",             DEEPPINK);
        NAMED_COLORS.put("deepskyblue",          DEEPSKYBLUE);
        NAMED_COLORS.put("dimgray",              DIMGRAY);
        NAMED_COLORS.put("dimgrey",              DIMGREY);
        NAMED_COLORS.put("dodgerblue",           DODGERBLUE);
        NAMED_COLORS.put("firebrick",            FIREBRICK);
        NAMED_COLORS.put("floralwhite",          FLORALWHITE);
        NAMED_COLORS.put("forestgreen",          FORESTGREEN);
        NAMED_COLORS.put("fuchsia",              FUCHSIA);
        NAMED_COLORS.put("gainsboro",            GAINSBORO);
        NAMED_COLORS.put("ghostwhite",           GHOSTWHITE);
        NAMED_COLORS.put("gold",                 GOLD);
        NAMED_COLORS.put("goldenrod",            GOLDENROD);
        NAMED_COLORS.put("gray",                 GRAY);
        NAMED_COLORS.put("green",                GREEN);
        NAMED_COLORS.put("greenyellow",          GREENYELLOW);
        NAMED_COLORS.put("grey",                 GREY);
        NAMED_COLORS.put("honeydew",             HONEYDEW);
        NAMED_COLORS.put("hotpink",              HOTPINK);
        NAMED_COLORS.put("indianred",            INDIANRED);
        NAMED_COLORS.put("indigo",               INDIGO);
        NAMED_COLORS.put("ivory",                IVORY);
        NAMED_COLORS.put("khaki",                KHAKI);
        NAMED_COLORS.put("lavender",             LAVENDER);
        NAMED_COLORS.put("lavenderblush",        LAVENDERBLUSH);
        NAMED_COLORS.put("lawngreen",            LAWNGREEN);
        NAMED_COLORS.put("lemonchiffon",         LEMONCHIFFON);
        NAMED_COLORS.put("lightblue",            LIGHTBLUE);
        NAMED_COLORS.put("lightcoral",           LIGHTCORAL);
        NAMED_COLORS.put("lightcyan",            LIGHTCYAN);
        NAMED_COLORS.put("lightgoldenrodyellow", LIGHTGOLDENRODYELLOW);
        NAMED_COLORS.put("lightgray",            LIGHTGRAY);
        NAMED_COLORS.put("lightgreen",           LIGHTGREEN);
        NAMED_COLORS.put("lightgrey",            LIGHTGREY);
        NAMED_COLORS.put("lightpink",            LIGHTPINK);
        NAMED_COLORS.put("lightsalmon",          LIGHTSALMON);
        NAMED_COLORS.put("lightseagreen",        LIGHTSEAGREEN);
        NAMED_COLORS.put("lightskyblue",         LIGHTSKYBLUE);
        NAMED_COLORS.put("lightslategray",       LIGHTSLATEGRAY);
        NAMED_COLORS.put("lightslategrey",       LIGHTSLATEGREY);
        NAMED_COLORS.put("lightsteelblue",       LIGHTSTEELBLUE);
        NAMED_COLORS.put("lightyellow",          LIGHTYELLOW);
        NAMED_COLORS.put("lime",                 LIME);
        NAMED_COLORS.put("limegreen",            LIMEGREEN);
        NAMED_COLORS.put("linen",                LINEN);
        NAMED_COLORS.put("magenta",              MAGENTA);
        NAMED_COLORS.put("maroon",               MAROON);
        NAMED_COLORS.put("mediumaquamarine",     MEDIUMAQUAMARINE);
        NAMED_COLORS.put("mediumblue",           MEDIUMBLUE);
        NAMED_COLORS.put("mediumorchid",         MEDIUMORCHID);
        NAMED_COLORS.put("mediumpurple",         MEDIUMPURPLE);
        NAMED_COLORS.put("mediumseagreen",       MEDIUMSEAGREEN);
        NAMED_COLORS.put("mediumslateblue",      MEDIUMSLATEBLUE);
        NAMED_COLORS.put("mediumspringgreen",    MEDIUMSPRINGGREEN);
        NAMED_COLORS.put("mediumturquoise",      MEDIUMTURQUOISE);
        NAMED_COLORS.put("mediumvioletred",      MEDIUMVIOLETRED);
        NAMED_COLORS.put("midnightblue",         MIDNIGHTBLUE);
        NAMED_COLORS.put("mintcream",            MINTCREAM);
        NAMED_COLORS.put("mistyrose",            MISTYROSE);
        NAMED_COLORS.put("moccasin",             MOCCASIN);
        NAMED_COLORS.put("navajowhite",          NAVAJOWHITE);
        NAMED_COLORS.put("navy",                 NAVY);
        NAMED_COLORS.put("oldlace",              OLDLACE);
        NAMED_COLORS.put("olive",                OLIVE);
        NAMED_COLORS.put("olivedrab",            OLIVEDRAB);
        NAMED_COLORS.put("orange",               ORANGE);
        NAMED_COLORS.put("orangered",            ORANGERED);
        NAMED_COLORS.put("orchid",               ORCHID);
        NAMED_COLORS.put("palegoldenrod",        PALEGOLDENROD);
        NAMED_COLORS.put("palegreen",            PALEGREEN);
        NAMED_COLORS.put("paleturquoise",        PALETURQUOISE);
        NAMED_COLORS.put("palevioletred",        PALEVIOLETRED);
        NAMED_COLORS.put("papayawhip",           PAPAYAWHIP);
        NAMED_COLORS.put("peachpuff",            PEACHPUFF);
        NAMED_COLORS.put("peru",                 PERU);
        NAMED_COLORS.put("pink",                 PINK);
        NAMED_COLORS.put("plum",                 PLUM);
        NAMED_COLORS.put("powderblue",           POWDERBLUE);
        NAMED_COLORS.put("purple",               PURPLE);
        NAMED_COLORS.put("red",                  RED);
        NAMED_COLORS.put("rosybrown",            ROSYBROWN);
        NAMED_COLORS.put("royalblue",            ROYALBLUE);
        NAMED_COLORS.put("saddlebrown",          SADDLEBROWN);
        NAMED_COLORS.put("salmon",               SALMON);
        NAMED_COLORS.put("sandybrown",           SANDYBROWN);
        NAMED_COLORS.put("seagreen",             SEAGREEN);
        NAMED_COLORS.put("seashell",             SEASHELL);
        NAMED_COLORS.put("sienna",               SIENNA);
        NAMED_COLORS.put("silver",               SILVER);
        NAMED_COLORS.put("skyblue",              SKYBLUE);
        NAMED_COLORS.put("slateblue",            SLATEBLUE);
        NAMED_COLORS.put("slategray",            SLATEGRAY);
        NAMED_COLORS.put("slategrey",            SLATEGREY);
        NAMED_COLORS.put("snow",                 SNOW);
        NAMED_COLORS.put("springgreen",          SPRINGGREEN);
        NAMED_COLORS.put("steelblue",            STEELBLUE);
        NAMED_COLORS.put("tan",                  TAN);
        NAMED_COLORS.put("teal",                 TEAL);
        NAMED_COLORS.put("thistle",              THISTLE);
        NAMED_COLORS.put("tomato",               TOMATO);
        NAMED_COLORS.put("transparent",          TRANSPARENT);
        NAMED_COLORS.put("turquoise",            TURQUOISE);
        NAMED_COLORS.put("violet",               VIOLET);
        NAMED_COLORS.put("wheat",                WHEAT);
        NAMED_COLORS.put("white",                WHITE);
        NAMED_COLORS.put("whitesmoke",           WHITESMOKE);
        NAMED_COLORS.put("yellow",               YELLOW);
        NAMED_COLORS.put("yellowgreen",          YELLOWGREEN);
    }
}
