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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.DataFormatHandler;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledInput;
import javafx.incubator.scene.control.rich.model.StyledOutput;
import javafx.incubator.scene.control.rich.model.StyledSegment;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * DataFormatHandler for use with attribute-based rich text models.
 * <p>
 * The handler uses a simple text-based format:
 * <pre>
 * [optional:style][text]...[\n]...
 * </pre>
 * where style information (optional) is encoded as a sequence of backtick- (`) prefixed tokens,
 * followed by a double backtick (``) sequence.
 * Certain symbols such as backtick and percent character (%) are escaped using %XX sequences where 
 * XX is a two-character hexadecimal character value.
 * <p>
 * Character attribute tokens:
 * <ul>
 *   <li>`B - bold typeface
 *   <li>`CRRGGBB - text color with hex RGB values
 *   <li>`Fstring - font family
 *   <li>`I - italic typeface
 *   <li>`T - strike-through
 *   <li>`U - underline
 *   <li>`Zdouble - font size
 * </ul>
 * Paragraph attribute tokens:
 * <ul>
 *   <li>`bRRGGBBAA - paragraph background color
 *   <li>`estring - bullet
 *   <li>`Ldouble - line spacing
 *   <li>`R - right-to-left
 *   <li>`adouble - space above
 *   <li>`wdouble - space below
 *   <li>`fdouble - space left
 *   <li>`rdouble - space right
 *   <li>`Acode - text alignment (code: C - center, J - justify, L - left, R - right)
 * </ul>
 * In addition, any subsequent occurence of a style is simplified by providing its number using {@code 'num} sequence:
 * <pre>
 *   `0 ... `2147483647
 * </pre>
 */
public class RichTextFormatHandler extends DataFormatHandler {
    private static final char TOKEN_BACKGROUND = 'b';
    private static final char TOKEN_BOLD = 'B';
    private static final char TOKEN_BULLET = 'e';
    private static final char TOKEN_FONT_FAMILY = 'F';
    private static final char TOKEN_FONT_SIZE = 'Z';
    private static final char TOKEN_ITALIC = 'I';
    private static final char TOKEN_LINE_SPACING = 'L';
    private static final char TOKEN_RTL = 'R';
    private static final char TOKEN_SPACE_ABOVE = 'a';
    private static final char TOKEN_SPACE_BELOW = 'w';
    private static final char TOKEN_SPACE_LEFT = 'f';
    private static final char TOKEN_SPACE_RIGHT = 'r';
    private static final char TOKEN_STRIKE_THROUGH = 'T';
    private static final char TOKEN_TEXT_ALIGNMENT = 'A';
    private static final char TOKEN_TEXT_COLOR = 'C';
    private static final char TOKEN_UNDERLINE = 'U';

    public RichTextFormatHandler() {
        super(EditableRichTextModel.DATA_FORMAT);
    }

    @Override
    public StyledInput createStyledInput(Object src) {
        String input = (String)src;
        return new RichStyledInput(input);
    }

    @Override
    public Object copy(StyledTextModel m, StyleResolver r, TextPos start, TextPos end) throws IOException {
        StringWriter wr = new StringWriter();
        StyledOutput so = createStyledOutput(r, wr);
        m.exportText(start, end, so);
        return wr.toString();
    }

    @Override
    public void save(StyledTextModel m, StyleResolver r, TextPos start, TextPos end, OutputStream out) throws IOException {
        Charset cs = Charset.forName("utf-8");
        Writer wr = new OutputStreamWriter(out, cs);
        StyledOutput so = createStyledOutput(r, wr);
        m.exportText(start, end, so);
    }

    public StyledOutput createStyledOutput(StyleResolver r, Writer wr) {
        Charset cs = Charset.forName("utf-8");
        boolean buffered = isBuffered(wr);
        if (buffered) {
            return new RichStyledOutput(r, wr);
        } else {
            wr = new BufferedWriter(wr);
            return new RichStyledOutput(r, wr);
        }
    }

    private static boolean isBuffered(Writer x) {
        return
            (x instanceof BufferedWriter) ||
            (x instanceof StringWriter);
    }

    /** importer */
    private static class RichStyledInput implements StyledInput {
        private final String text;
        private int index;
        private StringBuilder sb;
        private final ArrayList<StyleAttrs> attrs = new ArrayList<>();

        public RichStyledInput(String text) {
            this.text = text;
        }

        @Override
        public StyledSegment nextSegment() {
            try {
                int c = charAt(0);
                switch(c)
                {
                case -1:
                    return null;
                case '\n':
                    index++;
                    return StyledSegment.LINE_BREAK;
                case '`':
                    index++;
                    // TODO this may return an object, for paragraph Node `P or Inline Node `N segments, or StyleAttrs.
                    StyleAttrs a = decodeStyleAttrs();
                    String text = decodeText();
                    if (text.length() == 0) {
                        StyleAttrs pa = a.getParagraphAttrs();
                        if (pa != null) {
                            return StyledSegment.ofParagraphAttributes(pa);
                        }
                    }
                    return StyledSegment.of(text, a);
                }
                String text = decodeText();
                return StyledSegment.of(text);
            } catch (IOException e) {
                e.printStackTrace(); // FIX remove
                return null;
            }
        }

        // TODO perhaps delta is not needed
        private int charAt(int delta) {
            int ix = index + delta;
            if(ix >= text.length()) {
                return -1;
            }
            return text.charAt(ix);
        }
        
        private String decodeText() throws IOException {
            int start = index;
            for(;;) {
                int c = charAt(0);
                switch(c) {
                case '\n':
                case '`':
                case -1:
                    return text.substring(start, index);
                case '%':
                    return decodeText(start, index);
                }
                index++;
            }
        }

        private TextAlignment decodeAlignment() throws IOException {
            int c = charAt(0);
            TextAlignment a = RichUtils.decodeAlignment(c);
            index++;
            return a;
        }

        private String decodeText(int start, int ix) throws IOException {
            if(sb == null) {
                sb = new StringBuilder();
            }
            if(ix > start) {
                sb.append(text, start, ix);
            }
            for(;;) {
                int c = charAt(0);
                switch(c) {
                case '\n':
                case '`':
                case -1:
                    String s = sb.toString();
                    sb.setLength(0);
                    return s;
                case '%':
                    index++;
                    int ch = decodeHexByte();
                    sb.append((char)ch);
                    break;
                }
                index++;
            }
        }
        
        private int decodeHexByte() throws IOException {
            int ch = decodeHex(charAt(0)) << 4;
            index++;
            ch += decodeHex(charAt(0));
            return ch;
        }
        
        private static int decodeHex(int ch) throws IOException {
            int c = ch - '0'; // 0...9
            if((c >= 0) && (c <= 9)) {
                return c;
            }
            c = ch - 55; // handle A...F
            if((c >= 10) && (c <= 15)) {
                return c;
            }
            c = ch - 97; // handle a...f
            if((c >= 10) && (c <= 15)) {
                return c;
            }
            throw new IOException("not a hex char:" + ch);
        }

        private StyleAttrs decodeStyleAttrs() throws IOException {
            StyleAttrs.Builder b = StyleAttrs.builder();
            for(;;) {
                int c = charAt(0);
                index++;
                switch(c) {
                case TOKEN_BACKGROUND:
                    {
                        Color col = decodeColor(true);
                        b.setBackground(col);
                    }
                    break;
                case TOKEN_BOLD:
                    b.setBold(true);
                    break;
                case TOKEN_BULLET:
                    String bullet = decodeText();
                    b.setBullet(bullet);
                    break;
                case TOKEN_FONT_FAMILY:
                    String fam = decodeText();
                    b.setFontFamily(fam);
                    break;
                case TOKEN_ITALIC:
                    b.setItalic(true);
                    break;
                case TOKEN_LINE_SPACING:
                    double lineSpacing = decodeDouble();
                    b.setLineSpacing(lineSpacing);
                    break;
                case TOKEN_RTL:
                    b.setRTL(true);
                    break;
                case TOKEN_SPACE_ABOVE:
                    double spaceAbove = decodeDouble();
                    b.setSpaceAbove(spaceAbove);
                    break;
                case TOKEN_SPACE_BELOW:
                    double spaceBelow = decodeDouble();
                    b.setSpaceBelow(spaceBelow);
                    break;
                case TOKEN_SPACE_LEFT:
                    double spaceLeft = decodeDouble();
                    b.setSpaceLeft(spaceLeft);
                    break;
                case TOKEN_SPACE_RIGHT:
                    double spaceRight = decodeDouble();
                    b.setSpaceRight(spaceRight);
                    break;
                case TOKEN_STRIKE_THROUGH:
                    b.setStrikeThrough(true);
                    break;
                case TOKEN_TEXT_ALIGNMENT:
                    {
                        TextAlignment a = decodeAlignment();
                        b.setTextAlignment(a);
                    }
                    break;
                case TOKEN_TEXT_COLOR:
                    {
                        Color col = decodeColor(false);
                        b.setTextColor(col);
                    }
                    break;
                case TOKEN_UNDERLINE:
                    b.setUnderline(true);
                    break;
                case TOKEN_FONT_SIZE:
                    double size = decodeDouble();
                    b.setFontSize(size);
                    break;
                case '`':
                    // reached the end token
                    StyleAttrs a = b.build();
                    attrs.add(a);
                    return a;
                default:
                    char ch = (char)c;
                    if(Character.isDigit(ch)) {
                        --index;
                        int num = decodeInt();
                        if((num >= 0) && (num < attrs.size())) {
                            skipEndStyleToken();
                            return attrs.get(num);
                        }
                        throw new IOException("invalid style number " + num + " index=" + index);
                    } else {
                        throw new IOException("unknown style token:" + ch + " index=" + index);
                    }
                }

                c = charAt(0);
                switch (c) {
                case '`':
                    index++;
                    continue;
                default:
                    throw new IOException("missing style terminator, index=" + index);
                }
            }
        }

        private void skipEndStyleToken() throws IOException {
            int c = charAt(0);
            if (c == '`') {
                index++;
                c = charAt(0);
                if (c == '`') {
                    index++;
                    return;
                }
            }
            throw new IOException("expecting style terminator, index=" + index);
        }

        private Color decodeColor(boolean withAlpha) throws IOException {
            int r = decodeHexByte();
            index++;
            int g = decodeHexByte();
            index++;
            int b = decodeHexByte();
            index++;
            double a;
            if (withAlpha) {
                a = decodeHexByte() / 255.0;
                index++;
            } else {
                a = 1.0;
            }
            return Color.rgb(r, g, b, a);
        }

        private int decodeInt() throws IOException {
            int v = 0;
            int ct = 0;
            for(;;) {
                int c = charAt(0);
                int d = Character.digit(c, 10);
                if(d < 0) {
                    if(ct == 0) {
                        throw new IOException("missing number index=" + index);
                    }
                    return v;
                } else {
                    v = v * 10 + d;
                    ct++;
                }
                index++;
            }
        }

        private double decodeDouble() throws IOException {
            String payload = decodePayload();
            try {
                return Double.parseDouble(payload);
            } catch(NumberFormatException e) {
                throw new IOException("expecting double: " + payload, e);
            }
        }

        private String decodePayload() throws IOException {
            int start = index;
            int i = 0;
            for(;;) {
                int c = charAt(i);
                switch(c) {
                case -1:
                    throw new IOException("unexpected end of token");
                case '`':
                    index = start + i;
                    return text.substring(start, index);
                }
                i++;
            }
        }

        @Override
        public void close() throws IOException {
        }
    }

    /** exporter */
    private static class RichStyledOutput implements StyledOutput {
        private final StyleResolver resolver;
        private final Writer wr;
        private HashMap<StyleAttrs, Integer> styles = new HashMap<>();

        public RichStyledOutput(StyleResolver resolver, Writer wr) {
            this.resolver = resolver;
            this.wr = wr;
        }

        @Override
        public void append(StyledSegment seg) throws IOException {
            switch (seg.getType()) {
            case INLINE_NODE:
                // TODO
                break;
            case LINE_BREAK:
                wr.write("\n");
                break;
            case PARAGRAPH_ATTRIBUTES:
                {
                    StyleAttrs a = seg.getStyleAttrs(resolver);
                    if ((a != null) && (!a.isEmpty())) {
                        Integer num = styles.get(a);
                        if (num == null) {
                            int sz = styles.size();
                            styles.put(a, Integer.valueOf(sz));

                            Color c = a.getBackground();
                            if (c != null) {
                                wr.write('`');
                                wr.write(TOKEN_BACKGROUND);
                                wr.write(toHex8(c.getRed()));
                                wr.write(toHex8(c.getGreen()));
                                wr.write(toHex8(c.getBlue()));
                                wr.write(toHex8(c.getOpacity()));
                            }

                            String bullet = a.getBullet();
                            if (bullet != null) {
                                wr.write('`');
                                wr.write(TOKEN_BULLET);
                                wr.write(encode(bullet));
                            }

                            Double lineSpacing = a.getLineSpacing();
                            if (lineSpacing != null) {
                                wr.write('`');
                                wr.write(TOKEN_LINE_SPACING);
                                wr.write(RichUtils.formatDouble(lineSpacing));
                            }

                            TextAlignment al = a.getTextAlignment();
                            if (al != null) {
                                wr.write('`');
                                wr.write(TOKEN_TEXT_ALIGNMENT);
                                wr.write(RichUtils.encodeAlignment(al));
                            }

                            if (a.isRTL()) {
                                wr.write('`');
                                wr.write(TOKEN_RTL);
                            }

                            Double spaceAbove = a.getSpaceAbove();
                            if (spaceAbove != null) {
                                wr.write('`');
                                wr.write(TOKEN_SPACE_ABOVE);
                                wr.write(RichUtils.formatDouble(spaceAbove));
                            }

                            Double spaceBelow = a.getSpaceBelow();
                            if (spaceBelow != null) {
                                wr.write('`');
                                wr.write(TOKEN_SPACE_BELOW);
                                wr.write(RichUtils.formatDouble(spaceBelow));
                            }

                            Double spaceLeft = a.getSpaceLeft();
                            if (spaceLeft != null) {
                                wr.write('`');
                                wr.write(TOKEN_SPACE_LEFT);
                                wr.write(RichUtils.formatDouble(spaceLeft));
                            }

                            Double spaceRight = a.getSpaceRight();
                            if (spaceRight != null) {
                                wr.write('`');
                                wr.write(TOKEN_SPACE_RIGHT);
                                wr.write(RichUtils.formatDouble(spaceRight));
                            }
                        } else {
                            // write cached style id number
                            wr.write("`");
                            wr.write(String.valueOf(num));
                        }
                        wr.write("``");
                    }
                }
                break;
            case REGION:
                // TODO
                break;
            case TEXT:
                {
                    // TODO use caching resolver with #
                    // the model manages actual attributes
                    StyleAttrs a = seg.getStyleAttrs(resolver);
                    if ((a != null) && (!a.isEmpty())) {
                        Integer num = styles.get(a);
                        if (num == null) {
                            int sz = styles.size();
                            styles.put(a, Integer.valueOf(sz));
    
                            // write style info
                            if (a.isBold()) {
                                wr.write('`');
                                wr.write(TOKEN_BOLD);
                            }
    
                            if (a.isItalic()) {
                                wr.write('`');
                                wr.write(TOKEN_ITALIC);
                            }
    
                            if (a.isStrikeThrough()) {
                                wr.write('`');
                                wr.write(TOKEN_STRIKE_THROUGH);
                            }
    
                            if (a.isUnderline()) {
                                wr.write('`');
                                wr.write(TOKEN_UNDERLINE);
                            }
    
                            String s = a.getFontFamily();
                            if (s != null) {
                                wr.write('`');
                                wr.write(TOKEN_FONT_FAMILY);
                                wr.write(encode(s));
                            }
    
                            Double n = a.getFontSize();
                            if (n != null) {
                                wr.write('`');
                                wr.write(TOKEN_FONT_SIZE);
                                wr.write(RichUtils.formatDouble(n));
                            }
    
                            Color c = a.getTextColor();
                            if (c != null) {
                                wr.write('`');
                                wr.write(TOKEN_TEXT_COLOR);
                                wr.write(toHex8(c.getRed()));
                                wr.write(toHex8(c.getGreen()));
                                wr.write(toHex8(c.getBlue()));
                            }
                        } else {
                            // write cached style id number
                            wr.write('`');
                            wr.write(String.valueOf(num));
                        }
                        wr.write("``");
                    }
    
                    String text = seg.getText();
                    text = encode(text);
                    wr.write(text);
                }
                break;
            }
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

        private static String encode(String text) {
            if (text == null) {
                return "";
            }

            int ix = indexOfSpecialChar(text);
            if (ix < 0) {
                return text;
            }

            int len = text.length();
            StringBuilder sb = new StringBuilder(len + 32);
            if (ix > 0) {
                sb.append(text.substring(0, ix));
            }

            for (int i = ix; i < len; i++) {
                char c = text.charAt(i);
                if (isSpecialChar(c)) {
                    sb.append(String.format("%%%02X", (int)c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static int indexOfSpecialChar(String text) {
            int len = text.length();
            for (int i = 0; i < len; i++) {
                char c = text.charAt(i);
                if (isSpecialChar(c)) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isSpecialChar(char c) {
            switch (c) {
            case '`':
            case '%':
                return true;
            }
            return false;
        }

        @Override
        public void flush() throws IOException {
            wr.flush();
        }

        @Override
        public void close() throws IOException {
            wr.close();
        }
    }
}
