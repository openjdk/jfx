/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import jfx.incubator.scene.control.richtext.model.ParagraphDirection;

/**
 * Converters used to serialize/deserialize text attributes.
 */
public class Converters {
    public static StringConverter<Boolean> booleanConverter() {
        return new StringConverter<Boolean>() {
            @Override
            public String toString(Boolean v) {
                // do not output value of a boolean attribute
                return null;
            }

            @Override
            public Boolean fromString(String s) {
                // attribute present means it's value is TRUE
                return Boolean.TRUE;
            }
        };
    }

    public static StringConverter<Color> colorConverter() {
        return new StringConverter<Color>() {
            @Override
            public String toString(Color c) {
                return toHexColor(c);
            }

            @Override
            public Color fromString(String s) {
                return parseHexColor(s);
            }
        };
    }

    public static StringConverter<ParagraphDirection> paragraphDirectionConverter() {
        return new StringConverter<ParagraphDirection>() {
            @Override
            public String toString(ParagraphDirection d) {
                return fromParagraphDirection(d);
            }

            @Override
            public ParagraphDirection fromString(String s) {
                return toParagraphDirection(s);
            }
        };
    }

    public static StringConverter<TextAlignment> textAlignmentConverter() {
        return new StringConverter<TextAlignment>() {
            @Override
            public String toString(TextAlignment v) {
                return fromTextAlignment(v);
            }

            @Override
            public TextAlignment fromString(String s) {
                return toTextAlignment(s);
            }
        };
    }

    public static StringConverter<String> stringConverter() {
        return new StringConverter<String>() {
            @Override
            public String toString(String x) {
                return x;
            }

            @Override
            public String fromString(String s) {
                return s;
            }
        };
    }

    private static Color parseHexColor(String s) {
        double alpha;
        switch(s.length()) {
        case 8:
            // rrggbbaa
            alpha = parseByte(s, 6) / 255.0;
            break;
        case 6:
            // rrggbb
            alpha = 1.0;
            break;
        default:
            throw new IllegalArgumentException("unable to parse color: " + s);
        }

        int r = parseByte(s, 0);
        int g = parseByte(s, 2);
        int b = parseByte(s, 4);
        return Color.rgb(r, g, b, alpha);
    }

    protected static String toHexColor(Color c) {
        return
            toHex8(c.getRed()) +
            toHex8(c.getGreen()) +
            toHex8(c.getBlue()) +
            ((c.getOpacity() == 1.0) ? "" : toHex8(c.getOpacity()));
    }

    private static String toHex8(double x) {
        int v = (int)Math.round(255.0 * x);
        if (v < 0) {
            v = 0;
        } else if (v > 255) {
            v = 255;
        }
        return String.format("%02X", v);
    }

    protected static int parseByte(String text, int start) {
        int v = parseHexChar(text.charAt(start)) << 4;
        v += parseHexChar(text.charAt(start + 1));
        return v;
    }

    private static int parseHexChar(int ch) {
        int c = ch - '0'; // 0...9
        if ((c >= 0) && (c <= 9)) {
            return c;
        }
        c = ch - 55; // handle A...F
        if ((c >= 10) && (c <= 15)) {
            return c;
        }
        c = ch - 97; // handle a...f
        if ((c >= 10) && (c <= 15)) {
            return c;
        }
        throw new IllegalArgumentException("not a hex char:" + ch);
    }

    private static String fromTextAlignment(TextAlignment a) {
        switch (a) {
        case CENTER:
            return "C";
        case JUSTIFY:
            return "J";
        case RIGHT:
            return "R";
        case LEFT:
        default:
            return "L";
        }
    }

    private static TextAlignment toTextAlignment(String s) {
        switch (s) {
        case "C":
            return TextAlignment.CENTER;
        case "J":
            return TextAlignment.JUSTIFY;
        case "L":
            return TextAlignment.LEFT;
        case "R":
            return TextAlignment.RIGHT;
        default:
            throw new IllegalArgumentException("bad text alignment: " + s);
        }
    }

    private static String fromParagraphDirection(ParagraphDirection d) {
        switch(d) {
        case RIGHT_TO_LEFT:
            return "R";
        case LEFT_TO_RIGHT:
        default:
            return "L";
        }
    }

    private static ParagraphDirection toParagraphDirection(String s) {
        switch(s) {
        case "R":
            return ParagraphDirection.RIGHT_TO_LEFT;
        case "L":
        default:
            return ParagraphDirection.LEFT_TO_RIGHT;
        }
    }
}
