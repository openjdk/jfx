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

package com.sun.jfx.incubator.scene.control.richtext.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import com.sun.javafx.scene.text.TextFlowHelper;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * RichTextArea specific utility methods.
 */
public final class RichUtils {

    private static final DecimalFormat format = new DecimalFormat("#0.##");

    private RichUtils() {
    }

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
        if (c.getOpacity() == 1.0) {
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
     * Returns true if the font family corresponds to a logical font as defined in
     * <a href="https://wiki.openjdk.org/display/OpenJFX/Font+Setup">OpenJFX Font Setup</a> wiki.
     * @param family the font family
     * @return true if logical, false otherwise
     */
    public static boolean isLogicalFont(String family) {
        switch (family) {
        case "System":
        case "Serif":
        case "SansSerif":
        case "Monospaced":
            return true;
        }
        return false;
    }

    /**
     * Guesses the font style from the font name, until JDK-8092191 is implemented.
     * @param lowerCaseName font name, must be lowercase'd
     * @return font style: [ normal | italic | oblique ]
     */
    public static String guessFontStyle(String lowerCaseName) {
        // are we going to encounter a localized font name?
        if (lowerCaseName.contains("italic")) {
            return "italic";
        } else if (lowerCaseName.contains("oblique")) {
            return "oblique";
        }
        return "normal";
    }

    /**
     * Guesses the font weight from the font name, until JDK-8092191 is implemented.
     * @param lowerCaseName font name, must be lowercase'd
     * @return font weight: [ normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 ]
     */
    public static String guessFontWeight(String lowerCaseName) {
        // are we going to encounter a localized font name?
        if (lowerCaseName.contains("thin")) {
            return "100";
        } else if (lowerCaseName.contains("extralight")) {
            return "200";
        } else if (lowerCaseName.contains("light")) {
            return "300";
        } else if (lowerCaseName.contains("medium")) {
            return "500";
        } else if (lowerCaseName.contains("semibold")) {
            return "600";
        } else if (lowerCaseName.contains("demibold")) {
            return "600";
        } else if (lowerCaseName.contains("bold")) {
            return "700";
        } else if (lowerCaseName.contains("extrabold")) {
            return "800";
        } else if (lowerCaseName.contains("heavy")) {
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

    @Deprecated // FIX remove
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

    @Deprecated // FIX remove
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

    /**
     * Combines style attributes, returning combined object (or null).
     *
     * @param lowPri the low priority attributes
     * @param hiPri the high priority attributes
     * @return the combined attributes, or null
     */
    public static StyleAttributeMap combine(StyleAttributeMap lowPri, StyleAttributeMap hiPri) {
        if ((lowPri != null) && (!lowPri.isEmpty())) {
            if (hiPri == null) {
                return lowPri;
            } else {
                return StyleAttributeMap.builder().merge(lowPri).merge(hiPri).build();
            }
        }
        return hiPri;
    }

    /**
     * Utility method which combines {@code CssMetaData} items in one immutable list.
     * <p>
     * The intended usage is to combine the parent and the child {@code CssMetaData} for
     * the purposes of {@code getClassCssMetaData()} method, see for example {@link Node#getClassCssMetaData()}.
     * <p>
     * Example:
     * <pre>{@code
     * private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = CssMetaData.combine(
     *      <Parent>.getClassCssMetaData(),
     *      STYLEABLE1,
     *      STYLEABLE2
     *  );
     * }</pre>
     * This method returns an instance of a {@code List} that implements
     * {@link java.util.RandomAccess} interface.
     *
     * @param inheritedFromParent the {@code CssMetaData} items inherited from parent, must not be null
     * @param items the additional items
     * @return the immutable list containing all of the items
     */
    // NOTE: this should be a public utility, see https://bugs.openjdk.org/browse/JDK-8320796
    public static List<CssMetaData<? extends Styleable, ?>> combine(
        List<CssMetaData<? extends Styleable, ?>> inheritedFromParent,
        CssMetaData<? extends Styleable, ?>... items)
    {
        CssMetaData[] combined = new CssMetaData[inheritedFromParent.size() + items.length];
        inheritedFromParent.toArray(combined);
        System.arraycopy(items, 0, combined, inheritedFromParent.size(), items.length);
        // makes a copy, unfortunately
        return List.of(combined);
    }

    /**
     * Reads a UTF8 string from the input stream.
     * This method does not close the input stream.
     * @param in the input stream
     * @return the string
     * @throws IOException if an I/O error occurs
     */
    public static String readString(InputStream in) throws IOException {
        BufferedInputStream b = new BufferedInputStream(in);
        InputStreamReader rd = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(65536);
        int c;
        while ((c = rd.read()) >= 0) {
            sb.append((char)c);
        }
        return sb.toString();
    }

    /**
     * Writes an Image to a byte array in PNG format.
     *
     * @param im source image
     * @return byte array containing PNG image
     * @throws IOException if an I/O error occurs
     */
    public static byte[] writePNG(Image im) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(65536);
        // this might conflict with user-set value
        ImageIO.setUseCache(false);
        ImageIO.write(ImgUtil.fromFXImage(im, null), "PNG", out);
        return out.toByteArray();
    }

    /**
     * Returns true if code point at the specified offset is a letter or a digit,
     * returns false otherwise or if the offset is outside of the valid range.
     * @param text the text
     * @param offset the character offset
     * @param len the text length
     * @return true if the code point at the specified offset is a letter or a digit
     */
    public static boolean isLetterOrDigit(String text, int offset) {
        if (offset < 0) {
            return false;
        } else if (offset >= text.length()) {
            return false;
        }
        // ignore the case when 'c' is a high surrogate without the low surrogate
        int c = Character.codePointAt(text, offset);
        return Character.isLetterOrDigit(c);
    }

    /**
     * Returns the offset of the next code point, or the end of the text string.
     * @param text the text
     * @param offset the offset to start from
     * @return the offset of the next code point, or the end of the text string
     */
    public static int nextCodePoint(String text, int offset) {
        int len = text.length();
        if (offset < len) {
            char ch1 = text.charAt(offset++);
            if (Character.isHighSurrogate(ch1) && offset < len) {
                char ch2 = text.charAt(offset);
                if (Character.isLowSurrogate(ch2)) {
                    ++offset;
                }
            }
            return offset;
        }
        return len;
    }

    /**
     * Converts PathElement[] in the owner's coordinates to a Bounds[] in screen coordinates.
     * It assumes the input array is a sequence of <pre>
     * MoveTo (top-left)
     * LineTo (to top-right)
     * LineTo (bottom-right)
     * LineTo (bottom-left)
     * LineTo (back to top-left)
     * </pre>
     * This method will break if the input sequence is different.
     */
    public static Bounds[] pathToBoundsArray(Node owner, PathElement[] elements) {
        Bounds[] bounds = new Bounds[elements.length / 5];
        int index = 0;
        for (int i = 0; i < bounds.length; i++) {
            MoveTo topLeft = (MoveTo)elements[index];
            LineTo topRight = (LineTo)elements[index + 1];
            LineTo bottomRight = (LineTo)elements[index + 2];
            BoundingBox b = new BoundingBox(
                topLeft.getX(),
                topLeft.getY(),
                topRight.getX() - topLeft.getX(),
                bottomRight.getY() - topRight.getY()
            );
            bounds[i] = owner.localToScreen(b);
            index += 5;
        }
        return bounds;
    }

    private static int parseInt(Object x) {
        if (x instanceof Integer n) {
            return n.intValue();
        }
        return 0;
    }

    /**
     * Returns the line index of the given character offset.
     *
     * @param offset the character offset
     * @return the line index
     */
    public static int lineForOffset(TextFlow f, int offset) {
        TextLayout la = TextFlowHelper.getTextLayout(f);
        TextLine[] lines = la.getLines();
        int line = 0;
        for (int i = 1; i < lines.length; i++) {
            TextLine t = lines[i];
            if (t.getStart() > offset) {
                return line;
            }
            line++;
        }
        return line;
    }

    /**
     * Returns the line start offset of the given line index.
     *
     * @param line the line index
     * @return the line start offset
     */
    public static Integer lineStart(TextFlow f, int line) {
        TextLayout la = TextFlowHelper.getTextLayout(f);
        TextLine[] lines = la.getLines();
        if (0 <= line && line < lines.length) {
            TextLine t = lines[line];
            return t.getStart();
        }
        return null;
    }

    /**
     * Returns the line end offset of the given line index.
     *
     * @param line the line index
     * @return the line offset
     */
    public static Integer lineEnd(TextFlow f, int line) {
        TextLayout la = TextFlowHelper.getTextLayout(f);
        TextLine[] lines = la.getLines();
        if (0 <= line && line < lines.length) {
            TextLine t = lines[line];
            return t.getStart() + t.getLength();
        }
        return null;
    }

    /**
     * Convenience method for laying out the node within its parent, filling the available area.
     * This method is equivalent to calling
     * {@code Region.layoutInArea(n, x, y, w, h, 0.0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, snap);}
     *
     * @param n the node to lay out
     * @param x the horizontal offset of the layout area
     * @param y the vertical offset of the layout area
     * @param w the width of the layout area
     * @param h the height of the layout area
     */
    public static void layoutInArea(Node n, double x, double y, double w, double h) {
        Parent p = n.getParent();
        boolean snap = (p instanceof Region r) ? r.isSnapToPixel() : false;
        Region.layoutInArea(n, x, y, w, h, 0.0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, snap);
    }

    /**
     * Computes y midpoint of MoveTo and LineTo path elements.
     *
     * @param path the PathElements
     * @return the midpoint
     */
    public static double computeMidPointY(PathElement[] path) {
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        int sz = path.length;
        for (int i = 0; i < sz; i++) {
            PathElement em = path[i];
            if (em instanceof LineTo m) {
                double y = m.getY();
                if (ymin > y) {
                    ymin = y;
                }
                if (ymax < y) {
                    ymax = y;
                }
            } else if (em instanceof MoveTo m) {
                double y = m.getY();
                if (ymin > y) {
                    ymin = y;
                }
                if (ymax < y) {
                    ymax = y;
                }
            }
        }
        return (ymin == Double.POSITIVE_INFINITY) ? 0.0 : (ymax + ymin) / 2.0;
    }


    /**
     * Creates an instance of StyleAttributeMap which contains character attributes found in the specified {@link Text} node.
     * The following attributes will be set:
     * <ul>
     * <li>{@link #BOLD}
     * <li>{@link #FONT_FAMILY}
     * <li>{@link #FONT_SIZE}
     * <li>{@link #ITALIC}
     * <li>{@link #STRIKE_THROUGH}
     * <li>{@link #TEXT_COLOR}
     * <li>{@link #UNDERLINE}
     * </ul>
     *
     * @param textNode the text node
     * @return the StyleAttributeMap instance
     */
    public static StyleAttributeMap fromTextNode(Text textNode) {
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        Font f = textNode.getFont();
        String st = f.getStyle().toLowerCase(Locale.US);
        boolean bold = RichUtils.isBold(st);
        boolean italic = RichUtils.isItalic(st);

        if (bold) {
            b.setBold(true);
        }

        if (italic) {
            b.setItalic(true);
        }

        if (textNode.isStrikethrough()) {
            b.setStrikeThrough(true);
        }

        if (textNode.isUnderline()) {
            b.setUnderline(true);
        }

        String family = f.getFamily();
        b.setFontFamily(family);

        double sz = f.getSize();
        if (sz != 12.0) {
            b.setFontSize(sz);
        }

        Paint x = textNode.getFill();
        if (x instanceof Color c) {
            // we do not support gradients (although we could get the first color, for example)
            b.setTextColor(c);
        }

        return b.build();
    }
}
