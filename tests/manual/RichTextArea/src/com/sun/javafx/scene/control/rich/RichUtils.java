/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

import java.io.IOException;
import java.text.DecimalFormat;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

/**
 * RichTextArea specific utility methods.
 */
public class RichUtils {
    private static final DecimalFormat format = new DecimalFormat("#0.##");

    /**
     * A safe substring method which is tolerant to null text, and offsets being outside of the text boundaries.
     *
     * @param text source text or null
     * @param start start offset, must be >= 0
     * @param end end offset
     * @return a non-null substring
     */
    public static String substring(String text, int start, int end) {
        if (text == null) {
            return "";
        }

        int len = text.length();
        if ((end < 0) || (end > len)) {
            end = len;
        }

        if ((start == 0) && (end == len)) {
            return text;
        }

        return text.substring(start, end);
    }

    /** Converts Color to "#rrggbb" or "rgba(r,g,b,a)" string */
    public static String toCssColor(Color c) {
        if(c.getOpacity() == 1.0) {
            return String.format(
                "#%02x%02x%02x",
                eightBit(c.getRed()),
                eightBit(c.getGreen()),
                eightBit(c.getBlue())
            );
        } else {
            return String.format(
                "rgba(%d,%d,%d,%f)",
                eightBit(c.getRed()),
                eightBit(c.getGreen()),
                eightBit(c.getBlue()),
                c.getOpacity()
            );
        }
    }

    /* Converts Color to its web CSS value #rrggbb */
    public static String toWebColor(Color c) {
        return String.format(
            "#%02x%02x%02x",
            eightBit(c.getRed()),
            eightBit(c.getGreen()),
            eightBit(c.getBlue())
        );
    }

    private static int eightBit(double val) {
        int v = (int)Math.round(val * 255);
        if (v < 0) {
            return 0;
        } else if (v > 255) {
            return 255;
        }
        return v;
    }

    /** null-tolerant !equals() */
    public static boolean notEquals(Object a, Object b) {
        return !equals(a, b);
    }

    /** null-tolerant equals() */
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null) {
            return (b == null);
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static boolean isTouchSupported() {
        return Platform.isSupported(ConditionalFeature.INPUT_TOUCH);
    }

    public static int getTextLength(TextFlow f) {
        int len = 0;
        for (Node n : f.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                len += t.getText().length();
            } else {
                // treat non-Text nodes as having 1 character
                len++;
            }
        }
        return len;
    }

    // TODO javadoc
    // translates path elements from src frame of reference to target, with additional shift by dx, dy
    // only MoveTo, LineTo are supported
    // may return null
    public static PathElement[] translatePath(Region tgt, Region src, PathElement[] elements, double deltax, double deltay) {
        //System.out.println("translatePath from=" + dump(elements) + " dx=" + deltax + " dy=" + deltay); // FIX
        Point2D ps = src.localToScreen(0.0, 0.0);
        if (ps == null) {
            return null;
        }

        Point2D pt = tgt.localToScreen(tgt.snappedLeftInset(), tgt.snappedTopInset());
        double dx = ps.getX() - pt.getX() + deltax;
        double dy = ps.getY() - pt.getY() + deltay;
        //System.out.println("dx=" + dx + " dy=" + dy); // FIX

        for (int i = 0; i < elements.length; i++) {
            PathElement em = elements[i];
            if (em instanceof LineTo m) {
                em = new LineTo(m.getX() + dx, m.getY() + dy);
            } else if (em instanceof MoveTo m) {
                em = new MoveTo(m.getX() + dx, m.getY() + dy);
            } else {
                throw new RuntimeException("unexpected path element " + em);
            }

            elements[i] = em;
        }
        //System.out.println("translatePath to=" + dump(elements)); // FIX
        return elements;
    }

    /**
     * Guesses the font style from the font name, until JDK-8092191 is implemented.
     * @param name font name, must be lowercase'd
     * @return font style: [ normal | italic | oblique ]
     */
    public static String guessFontStyle(String name) {
        // are we going to encounter a localized font name?
        if (name.contains("italic")) {
            return "italic";
        } else if (name.contains("oblique")) {
            return "oblique";
        }
        return "normal";
    }

    /**
     * Guesses the font weight from the font name, until JDK-8092191 is implemented.
     * @param name font name, must be lowercase'd
     * @return font weight: [ normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 ]
     */
    public static String guessFontWeight(String name) {
        // are we going to encounter a localized font name?
        if (name.contains("thin")) {
            return "100";
        } else if (name.contains("extralight")) {
            return "200";
        } else if (name.contains("light")) {
            return "300";
        } else if (name.contains("medium")) {
            return "500";
        } else if (name.contains("semibold")) {
            return "600";
        } else if (name.contains("demibold")) {
            return "600";
        } else if (name.contains("bold")) {
            return "700";
        } else if (name.contains("extrabold")) {
            return "800";
        } else if (name.contains("heavy")) {
            return "900";
        }
        return "normal"; // 400, see FontWeight
    }

    /**
     * Returns true if the specified lowercased font name is determined to be bold.
     * This method is not guaranteed to work in any circumstances, see JDK-8092191
     * @param lowerCaseFontName the font name converted to lower case
     * @return true if the font is bold
     */
    public static boolean isBold(String lowerCaseFontName) {
        // any others?
        // non-english names?
        return
            lowerCaseFontName.contains("bold") ||
            lowerCaseFontName.contains("extrabold") ||
            lowerCaseFontName.contains("heavy");
    }

    /**
     * Returns true if the specified lowercased font name is determined to be italic or oblique.
     * This method is not guaranteed to work in any circumstances, see JDK-8092191
     * @param lowerCaseFontName the font name converted to lower case
     * @return true if the font is italic
     */
    public static boolean isItalic(String lowerCaseFontName) {
        // any others?
        // non-english names?
        return
            lowerCaseFontName.contains("italic") ||
            lowerCaseFontName.contains("oblique");
    }

    /** dumps the path element array to a compact human-readable string */
    public static String dump(PathElement[] elements) {
        StringBuilder sb = new StringBuilder();
        if (elements == null) {
            sb.append("null");
        } else {
            for (PathElement em : elements) {
                if (em instanceof MoveTo p) {
                    sb.append('M');
                    sb.append(r(p.getX()));
                    sb.append(',');
                    sb.append(r(p.getY()));
                    sb.append(' ');
                } else if (em instanceof LineTo p) {
                    sb.append('L');
                    sb.append(r(p.getX()));
                    sb.append(',');
                    sb.append(r(p.getY()));
                    sb.append(' ');
                } else {
                    sb.append(em);
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    private static int r(double x) {
        return (int)Math.round(x);
    }

    public static String toCss(TextAlignment a) {
        switch(a) {
        case CENTER:
            return "center";
        case JUSTIFY:
            return "justify";
        case RIGHT:
            return "right";
        case LEFT:
        default:
            return "left";
        }
    }

    public static String formatDouble(Double value) {
        return format.format(value);
    }

    public static char encodeAlignment(TextAlignment a) {
        switch (a) {
        case CENTER:
            return 'C';
        case JUSTIFY:
            return 'J';
        case RIGHT:
            return 'R';
        case LEFT:
        default:
            return 'L';
        }
    }

    public static TextAlignment decodeAlignment(int c) throws IOException {
        switch (c) {
        case 'C':
            return TextAlignment.CENTER;
        case 'J':
            return TextAlignment.JUSTIFY;
        case 'L':
            return TextAlignment.LEFT;
        case 'R':
            return TextAlignment.RIGHT;
        default:
            throw new IOException("failed parsing alignment (" + (char)c + ")");
        }
    }
}
