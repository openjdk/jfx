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
// This implementation is borrowed from
// https://github.com/andy-goryachev/FxTextEditor/blob/master/src/goryachev/fxtexteditor/internal/rtf/RtfWriter.java
// with permission from the author.

package com.sun.jfx.incubator.scene.control.richtext;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

/**
 * StyledOutput which generates RTF.
 *
 * RTF 1.5 Spec:
 * https://www.biblioscape.com/rtf15_spec.htm
 */
public class RtfStyledOutput implements StyledOutput {
    private final LookupTable<Color> colorTable = new LookupTable<>(Color.BLACK);
    private final LookupTable<String> fontTable = new LookupTable<>("system");
    private final StyleResolver resolver;
    private final Writer writer;
    private boolean startOfLine = true;
    private StyleAttributeMap prevStyle;
    private Color color;
    private Color background;
    private boolean bold;
    private boolean italic;
    private boolean under;
    private boolean strike;
    private String fontFamily;
    private Double fontSize;

    public RtfStyledOutput(StyleResolver r, Writer wr) {
        this.resolver = new CachingStyleResolver(r);
        this.writer = wr;
    }

    public StyledOutput firstPassBuilder() {
        return new StyledOutput() {
            @Override
            public void consume(StyledSegment seg) throws IOException {
                switch (seg.getType()) {
                case PARAGRAPH_ATTRIBUTES:
                    // TODO
                    break;
                case TEXT:
                    StyleAttributeMap a = seg.getStyleAttributeMap(resolver);
                    if (a != null) {
                        // colors
                        Color c = getTextColor(a);
                        colorTable.add(c);

                        // TODO background color
                        //                    c = mixBackground(st.getBackgroundColor());
                        //                    if (c != null) {
                        //                        colorTable.add(c);
                        //                    }

                        // TODO font table
                        String family = a.getFontFamily();
                        if (family != null) {
                            fontTable.add(family);
                        }
                    }
                    break;
                }
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    //    \fnil   Unknown or default fonts (the default)
    //    \froman Roman, proportionally spaced serif fonts    Times New Roman, Palatino
    //    \fswiss Swiss, proportionally spaced sans serif fonts   Arial
    //    \fmodern    Fixed-pitch serif and sans serif fonts  Courier New, Pica
    //    \fscript    Script fonts    Cursive
    //    \fdecor Decorative fonts    Old English, ITC Zapf Chancery
    //    \ftech  Technical, symbol, and mathematical fonts   Symbol
    //    \fbidi  Arabic, Hebrew, or other bidirectional font Miriam
    private String lookupFontFamily(String name) {
        try {
            switch (name.toLowerCase()) {
            case "monospaced":
                return "\\fmodern Courier New";
            case "system":
            case "sans-serif":
                return "\\fswiss Helvetica";
            case "serif":
                return "\\froman Times New Roman";
            case "cursive":
                return "\\fscript Brush Script";
            case "fantasy":
                return "\\fdecor ITC Zapf Chancery";
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void writePrologue() throws IOException {
        // preamble
        write("{\\rtf1\\ansi\\ansicpg1252\\uc1\\sl0\\sb0\\sa0\\deff0");

        // font table
        write("{\\fonttbl");
        int ix = 0;
        for (String name : fontTable.getItems()) {
            String fam = lookupFontFamily(name);

            write("\\f");
            write(String.valueOf(ix++));
            if (fam == null) {
                write("\\fnil");
                write(" ");
                write(name);
            } else {
                write(fam);
            }
            write(";");
        }
        write("}\r\n");

        // color table
        write("{\\colortbl ;");
        for (Color c : colorTable.getItems()) {
            write("\\red");
            write(toInt255(c.getRed()));
            write("\\green");
            write(toInt255(c.getGreen()));
            write("\\blue");
            write(toInt255(c.getBlue()));
            write(";");
        }
        write("}\r\n");

        // TODO \deftab720 Default tab width in twips (the default is 720).  a twip is one-twentieth of a point
    }

    @Override
    public void consume(StyledSegment seg) throws IOException {
        switch (seg.getType()) {
        case LINE_BREAK:
            writeEndOfLine();
            writeNewLine();
            break;
        case PARAGRAPH_ATTRIBUTES:
            // TODO
            break;
        case REGION:
            Node n = seg.getParagraphNodeGenerator().get();
            writeParagraph(n);
            writeNewLine();
            break;
        case TEXT:
            writeTextSegment(seg);
            break;
        }
    }

    public void writeEpilogue() throws IOException {
        writeEndOfLine();
        write("\r\n}\r\n");
    }

    private void writeEndOfLine() throws IOException {
        if (color != null) {
            write("\\cf0 ");
            color = null;
        }

        if (background != null) {
            write("\\highlight0 ");
            background = null;
        }

        if (bold) {
            write("\\b0 ");
            bold = false;
        }

        if (italic) {
            write("\\i0 ");
            italic = false;
        }

        if (under) {
            write("\\ul0 ");
            under = false;
        }

        if (strike) {
            write("\\strike0 ");
            strike = false;
        }
    }

    private void writeNewLine() throws IOException {
        write("\\par\r\n");
        startOfLine = true;
    }

    @SuppressWarnings("null") // see L280
    private void writeTextSegment(StyledSegment seg) throws IOException {
        checkCancelled();

        if (startOfLine) {
            // first line indent 0, left aligned
            write("\\fi0\\ql ");
            prevStyle = null;

            startOfLine = false;
        }

        StyleAttributeMap a = seg.getStyleAttributeMap(resolver);

        if (RichUtils.notEquals(a, prevStyle) || RichUtils.notEquals(getTextColor(a), getTextColor(prevStyle))) {
            Color col;
            Color bg;
            boolean bld;
            boolean ita;
            boolean und;
            boolean str;
            String fam;
            Double fsize;

            if (a == null) {
                col = null;
                bg = null;
                bld = false;
                ita = false;
                und = false;
                str = false;
                fam = null;
                fsize = null;
            } else {
                col = getTextColor(a);
                bg = null; // TODO mixBackground(st.getBackgroundColor());
                bld = a.isBold();
                ita = a.isItalic();
                und = a.isUnderline();
                str = a.isStrikeThrough();
                fam = a.getFontFamily();
                fsize = a.getFontSize();
            }

            prevStyle = a;

            // emit changes

            if (RichUtils.notEquals(fontFamily, fam)) {
                int ix = fontTable.getIndexFor(fam);
                write("\\f");
                write(String.valueOf(ix));

                fontFamily = fam;
            }

            if (RichUtils.notEquals(fontSize, fsize)) {
                write("\\fs");
                // twice the points
                double fs = (fsize == null) ? 24.0 : (fsize * 2.0);
                write(String.valueOf((int)Math.round(fs)));
                fontSize = fsize;
            }

            if (RichUtils.notEquals(col, color)) {
                if (col == null) {
                    write("\\cf0 ");
                } else {
                    int ix = colorTable.getIndexFor(col);
                    if (ix > 0) {
                        ix++;
                    }

                    write("\\cf");
                    write(String.valueOf(ix));
                    write(" ");
                }

                color = col;
            }

            if (RichUtils.notEquals(bg, background)) {
                if (bg == null) {
                    write("\\highlight0 ");
                } else {
                    int ix = colorTable.getIndexFor(bg);
                    if (ix > 0) {
                        ix++;
                    }
                    write("\\highlight");
                    write(String.valueOf(ix));
                    write(" ");
                }

                background = bg;
            }

            if (bld != bold) {
                write(bld ? "\\b " : "\\b0 ");
                bold = bld;
            }

            if (ita != italic) {
                write(ita ? "\\i " : "\\i0 ");
                italic = ita;
            }

            if (und != under) {
                write(und ? "\\ul " : "\\ul0 ");
                under = und;
            }

            if (str != strike) {
                write(str ? "\\strike " : "\\strike0 ");
                strike = str;
            }
        }

        String text = seg.getText();
        String encoded = encode(text);
        write(encoded);
    }

    // TODO does not seem to work on Mac
    private void writeParagraph(Node n) throws IOException {
        WritableImage im = resolver.snapshot(n);
        byte[] bytes = RichUtils.writePNG(im);
        int w = (int)im.getWidth();
        int h = (int)im.getHeight();

        write("{\\*\\shppict {\\pict \\pngblip");
        write("\\picscalex100\\picscaley100\\piccropl10\\piccropr0\\piccropt0\\piccropb0");
        write("\\picw");
        write(String.valueOf(w));
        write("\\pich");
        write(String.valueOf(h));
        write("\\picwgoal");
        // let's try to default to 6".  72 * 6 * 2 = 864
        int wgoal = 864;
        write(String.valueOf(wgoal));
        int hgoal = h * wgoal / w;
        write("\\pichgoal");
        write(String.valueOf(hgoal));
        write("\r\n");
        // There is no set maximum line length for an RTF file.
        StringBuilder sb = new StringBuilder(2);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            hex2(sb, b);
            write(sb.toString());
            if ((i % 80) == 79) {
                write("\r\n");
            }
        }
        write("\r\n}}\r\n");
    }

    private static void hex2(StringBuilder sb, byte b) {
        sb.setLength(0);
        String hex = "0123456789abcdef";
        sb.append(hex.charAt((b >> 4) & 0x0f));
        sb.append(hex.charAt(b & 0x0f));
    }

    // TODO unit test!
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
        sb.append(text, 0, ix);

        for (int i = ix; i < len; i++) {
            char c = text.charAt(i);
            if (c < 0x20) {
                switch (c) {
                case '\n':
                case '\r':
                    break;
                case '\t':
                    sb.append(c);
                    break;
                }
            } else if (c < 0x80) {
                switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '{':
                    sb.append("\\{");
                    break;
                case '}':
                    sb.append("\\}");
                    break;
                default:
                    sb.append(c);
                    break;
                }
            } else {
                sb.append("\\u");
                sb.append(String.valueOf((short)c));
                sb.append("?");
            }
        }

        return sb.toString();
    }

    private static int indexOfSpecialChar(String text) {
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c < 0x20) {
                switch (c) {
                case '\t':
                    continue;
                default:
                    return i;
                }
            } else if (c < 0x80) {
                switch (c) {
                case '\\':
                case '{':
                case '}':
                    return i;
                default:
                    continue;
                }
            } else {
                return i;
            }
        }
        return -1;
    }

    private static String toInt255(double x) {
        int v = (int)Math.round(255 * x);
        if (v < 0) {
            v = 0;
        } else if (v > 255) {
            v = 255;
        }
        return String.valueOf(v);
    }

    private static void checkCancelled() throws IOException {
        // check if interrupted
        if (Thread.currentThread().isInterrupted()) {
            // don't want to have it as a checked exception... may be throws Exception?
            throw new IOException(new InterruptedException());
        }

        // TODO check if nearly out of memory
    }

    private void write(String s) throws IOException {
        writer.write(s);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private static Color getTextColor(StyleAttributeMap a) {
        Color c = a.getTextColor();
        return c == null ? Color.BLACK : c;
    }

    /** RTF is unable to specify colors inline it seems, needs a color lookup table */
    protected static class LookupTable<T> {
        private final ArrayList<T> items = new ArrayList<>();
        private final HashMap<T, Integer> indexes = new HashMap<>();

        public LookupTable(T initValue) {
            if (initValue != null) {
                add(initValue);
            }
        }

        public void add(T item) {
            if (!indexes.containsKey(item)) {
                Integer ix = Integer.valueOf(items.size());
                items.add(item);
                indexes.put(item, ix);
            }
        }

        /** returns index or 0 if not found */
        public int getIndexFor(T c) {
            Integer ix = indexes.get(c);
            if (ix == null) {
                return 0;
            }
            return ix.intValue();
        }

        public List<T> getItems() {
            return items;
        }
    }
}
