/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.pgstub;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.scene.text.TextSpan;

/**
 * Stub implementation of the {@link TextLayout} for testing purposes.
 * <p>
 * Simulates the text layout by assuming each character is a rectangle
 * with the size of the font (bold is 1 pixel wider).
 * Expects the text to contain '\n' as line separators.
 * <p>
 * This implementation ignores: alignment, bounds type, and direction.
 */
public class StubTextLayout implements TextLayout {
    private static final double DEFAULT_FONT_SIZE = 10;
    private TextSpan[] spans;
    private String text;
    private Font font;
    private int tabSize = DEFAULT_TAB_SIZE;
    private float lineSpacing;
    private float wrapWidth;
    private StubTextLine[] lines;

    public StubTextLayout() {
    }

    @Override
    public boolean setContent(TextSpan[] spans) {
        this.spans = spans;
        this.text = null;
        lines = null;
        return true;
    }

    @Override
    public boolean setContent(String text, Object font) {
        this.text = text;
        final StubFontLoader.StubFont stub = ((StubFontLoader.StubFont)font);
        this.font = stub == null ? null : stub.font;
        lines = null;
        return true;
    }

    @Override
    public boolean setAlignment(int alignment) {
        return true;
    }

    @Override
    public boolean setDirection(int direction) {
        return true;
    }

    @Override
    public boolean setLineSpacing(float spacing) {
        this.lineSpacing = spacing;
        lines = null;
        return true;
    }

    @Override
    public boolean setTabSize(int spaces) {
        if (spaces < 1) {
            spaces = 1;
        }
        if (tabSize != spaces) {
            tabSize = spaces;
            return true;
        }
        lines = null;
        return false;
    }

    @Override
    public boolean setWrapWidth(float wrapWidth) {
        this.wrapWidth = wrapWidth;
        lines = null;
        return true;
    }

    @Override
    public boolean setBoundsType(int type) {
        return true;
    }

    @Override
    public BaseBounds getBounds() {
        return getBounds(null, new RectBounds());
    }

//    @Override
//    public BaseBounds getBounds(TextSpan filter, BaseBounds bounds) {
//        ensureLayout();
//        double fontSizeH = nullFontSize;
//        double fontSizeW = nullFontSize;
//        if (font != null) {
//            fontSizeH = font.getSize();
//            fontSizeW = font.getSize();
//
//            // For better testing, we make bold text a little bit bigger.
//            boolean bold = font.getStyle().toLowerCase().contains("bold");
//            if (bold) {
//                fontSizeW++;
//            }
//        }
//
//        final String[] lines = getText().split("\n");
//        double width = 0.0;
//        double height = fontSizeH * lines.length + lineSpacing * (lines.length - 1);
//        for (String line : lines) {
//            final int length;
//            if (line.contains("\t")) {
//                // count chars but when encountering a tab round up to a tabSize boundary
//                char [] chrs = line.toCharArray();
//                int spaces = 0;
//                for (int i = 0; i < chrs.length; i++) {
//                    if (chrs[i] == '\t') {
//                        if (tabSize != 0) {
//                            while ((++spaces % tabSize) != 0) {}
//                        }
//                    } else {
//                        spaces++;
//                    }
//                }
//                length = spaces;
//            } else {
//                length = line.length();
//            }
//            width = Math.max(width, fontSizeW * length);
//        }
//        return bounds.deriveWithNewBounds(0, (float)-fontSizeH, 0, (float)width, (float)(height-fontSizeH), 0);
//    }
    @Override
    public BaseBounds getBounds(TextSpan filter, BaseBounds bounds) {
        ensureLayout();
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        if (filter != null) {
            for (int i = 0; i < lines.length; i++) {
                TextLine line = lines[i];
                GlyphList[] lineRuns = line.getRuns();
                for (int j = 0; j < lineRuns.length; j++) {
                    GlyphList run = lineRuns[j];
                    TextSpan span = run.getTextSpan();
                    if (span != filter) continue;
                    Point2D location = run.getLocation();
                    float runLeft = location.x;
                    //if (run.isLeftBearing()) {
                    //    runLeft += line.getLeftSideBearing();
                    //}
                    float runRight = location.x + run.getWidth();
                    //if (run.isRightBearing()) {
                    //    runRight += line.getRightSideBearing();
                    //}
                    float runTop = location.y;
                    float runBottom = location.y + line.getBounds().getHeight() + lineSpacing;
                    if (runLeft < left) left = runLeft;
                    if (runTop < top) top = runTop;
                    if (runRight > right) right = runRight;
                    if (runBottom > bottom) bottom = runBottom;
                }
            }
        } else {
            top = bottom = 0;
            for (int i = 0; i < lines.length; i++) {
                TextLine line = lines[i];
                RectBounds lineBounds = line.getBounds();
                float lineLeft = lineBounds.getMinX() + line.getLeftSideBearing();
                if (lineLeft < left) left = lineLeft;
                float lineRight = lineBounds.getMaxX() + line.getRightSideBearing();
                if (lineRight > right) right = lineRight;
                bottom += lineBounds.getHeight();
            }
            //if (isMirrored()) {
            //    float width = getMirroringWidth();
            //    float bearing = left;
            //    left = width - right;
            //    right = width - bearing;
            //}
        }
        return bounds.deriveWithNewBounds(left, top, 0, right, bottom, 0);
    }

    @Override
    public TextLine[] getLines() {
        ensureLayout();
        return lines;
    }

    @Override
    public GlyphList[] getRuns() {
        ensureLayout();
        ArrayList<GlyphList> rv = new ArrayList<>();
        for (StubTextLine line : lines) {
            for (GlyphList g : line.getRuns()) {
                rv.add(g);
            }
        }
        return rv.toArray(GlyphList[]::new);
    }

    @Override
    public Shape getShape(int type, TextSpan filter) {
        ensureLayout();
        // all this is undocumented
        boolean text = (type & TYPE_TEXT) != 0;
        boolean underline = (type & TYPE_UNDERLINE) != 0;
        boolean strikethrough = (type & TYPE_STRIKETHROUGH) != 0;
        boolean baselineType = (type & TYPE_BASELINE) != 0;
        // TODO
        return new Path2D();
    }

    // copied from PrismLayout
    // this implementation requires API additions to GlyphList
    @Override
    public Hit getHitInfo(float x, float y) {
        ensureLayout();
        int charIndex = -1;
        int insertionIndex = -1;
        boolean leading = false;

        ensureLayout();
        int lineIndex = getLineIndex(y);
        if (lineIndex >= lines.length) {
            charIndex = getCharCount();
            insertionIndex = charIndex + 1;
        } else {
            TextLine line = lines[lineIndex];
            GlyphList[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            GlyphList run = null;
            x -= bounds.getMinX();
            for (int i = 0; i < runs.length; i++) {
                run = runs[i];
                if (x < run.getWidth()) {
                    break;
                }
                if (i + 1 < runs.length) {
                    if (runs[i + 1].isLinebreak()) {
                        break;
                    }
                    x -= run.getWidth();
                }
            }
            if (run != null) {
                AtomicBoolean trailing = new AtomicBoolean();
                charIndex = run.getStart() + run.getOffsetAtX(x, trailing);
                leading = !trailing.get();

                insertionIndex = charIndex;
                char[] tx = getText();
                if (tx != null && insertionIndex < tx.length) {
                    if (!leading) {
                        BreakIterator charIterator = BreakIterator.getCharacterInstance();
                        charIterator.setText(new String(tx));
                        int next = charIterator.following(insertionIndex);
                        if (next == BreakIterator.DONE) {
                            insertionIndex += 1;
                        } else {
                            insertionIndex = next;
                        }
                    }
                } else if (!leading) {
                    insertionIndex += 1;
                }
            } else {
                //empty line, set to line break leading
                charIndex = line.getStart();
                leading = true;
                insertionIndex = charIndex;
            }
        }
        return new Hit(charIndex, insertionIndex, leading);
    }

    private int getCharCount() {
        if (text != null) {
            return text.length();
        }
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            count += lines[i].getLength();
        }
        return count;
    }

    private int getLineIndex(float y) {
        int index = 0;
        float bottom = 0;

        int lineCount = lines.length;
        while (index < lineCount) {
            bottom += lines[index].getBounds().getHeight() + lineSpacing;
            //if (index + 1 == lineCount) {
            //    bottom -= lines[index].getLeading();
            //}
            if (bottom > y) {
                break;
            }
            index++;
        }
        return index;
    }

    @Override
    public PathElement[] getCaretShape(int offset, boolean isLeading, float x, float y) {
        ensureLayout();
        // TODO
        return new PathElement[0];
    }

    @Override
    public PathElement[] getRange(int start, int end, int type, float x, float y) {
        ensureLayout();
        // TODO
        return new PathElement[0];
    }

    @Override
    public BaseBounds getVisualBounds(int type) {
        ensureLayout();
        // TODO
        return new RectBounds();
    }

    private char[] getText() {
        char[] text;
        int count = 0;
        for (int i = 0; i < spans.length; i++) {
            count += spans[i].getText().length();
        }
        text = new char[count];
        int offset = 0;
        for (int i = 0; i < spans.length; i++) {
            String string = spans[i].getText();
            int length = string.length();
            string.getChars(0, length, text, offset);
            offset += length;
        }
        return text;
    }

    private void ensureLayout() {
        if (lines == null) {
            lines = layout();
        }
    }

    private StubTextLine[] layout() {
        LayoutBuilder b = new LayoutBuilder(tabSize, lineSpacing, wrapWidth);
        if (text != null) {
            b.append(null, text, font);
        } else if (spans != null) {
            for(TextSpan s: spans) {
                b.append(s, s.getText(), (Font)s.getFont());
            }
        }
        return b.getLines();
    }

    /** Text Line */
    private static class StubTextLine implements TextLine {
        private final GlyphList[] runs;
        private final RectBounds bounds;
        private int start;
        private int length;

        public StubTextLine(GlyphList[] runs, RectBounds bounds, int start, int length) {
            this.runs = runs;
            this.bounds = bounds;
            this.start = start;
            this.length = length;
        }

        @Override
        public GlyphList[] getRuns() {
            return runs;
        }

        @Override
        public RectBounds getBounds() {
            return bounds;
        }

        @Override
        public float getLeftSideBearing() {
            return 0;
        }

        @Override
        public float getRightSideBearing() {
            return 0;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    /** Glyph List */
    private static class StubGlyphList implements GlyphList {
        private final TextSpan span;
        private final int start;
        private final int length;
        private final double x;
        private final double y;
        private final double charWidth;
        private final double charHeight;
        private final boolean linebreak;
        
        public StubGlyphList(
            TextSpan span,
            int start,
            int length,
            double x,
            double y,
            double charWidth,
            double charHeight,
            boolean linebreak
        ) {
            this.span = span;
            this.start = start;
            this.length = length;
            this.x = x;
            this.y = y;
            this.charWidth = charWidth;
            this.charHeight = charHeight;
            this.linebreak = linebreak;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getGlyphCount() {
            return length;
        }

        // this API is rather unclear
        @Override
        public int getGlyphCode(int glyphIndex) {
            // TODO what should it return?  for now, let's return the same thing it expects for tab and line break
            return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
        }

        @Override
        public float getPosX(int glyphIndex) {
            return (float)(x + glyphIndex * charWidth);
        }

        @Override
        public float getPosY(int glyphIndex) {
            return (float)y;
        }

        @Override
        public float getWidth() {
            return (float)(length * charWidth);
        }

        @Override
        public float getHeight() {
            return (float)charHeight;
        }

        @Override
        public RectBounds getLineBounds() {
            return new RectBounds(0, 0, getWidth(), getHeight());
        }

        @Override
        public Point2D getLocation() {
            return new Point2D((float)x, (float)y);
        }

        @Override
        public int getCharOffset(int glyphIndex) {
            return start + glyphIndex;
        }

        @Override
        public boolean isComplex() {
            return false;
        }

        @Override
        public TextSpan getTextSpan() {
            return span;
        }

        @Override
        public boolean isLinebreak() {
            return linebreak;
        }

        @Override
        public int getOffsetAtX(float x, AtomicBoolean trailing) {
            double px = Math.max(0.0, x - this.x);
            trailing.set(px % charWidth > 0.5);
            return (int)(px / charWidth);
        }
    }

    /**
     * Implements a single layout algorithm.
     */
    private static class LayoutBuilder {
        private final ArrayList<StubTextLine> lines = new ArrayList<>();
        private final ArrayList<StubGlyphList> runs = new ArrayList<>();
        private final int tabSize;
        private final double wrapWidth;
        private final double lineSpacing;
        private double charHeight;
        private double charWidth;
        private double x;
        private double y;
        private double runStartX;
        private int runStart;
        private int lineStart;
        private int column;
        private TextSpan span;
        private boolean lineBreak;

        public LayoutBuilder(int tabSize, double lineSpacing, double wrapWidth) {
            this.tabSize = tabSize;
            this.lineSpacing = lineSpacing;
            this.wrapWidth = wrapWidth;
        }

        public StubTextLine[] getLines() {
            if (!runs.isEmpty()) {
                addLine();
            }
            return lines.toArray(StubTextLine[]::new);
        }

        private void addRun(int ix) {
            if (ix > 0) {
                StubGlyphList r = new StubGlyphList(span, runStart + ix, ix, runStartX, y, charWidth, charHeight, lineBreak);
                runs.add(r);
                runStart += ix;
                runStartX = x;
            }
            lineBreak = false;
        }
        
        private void addLine() {
            int len = runStart - lineStart;
            StubGlyphList[] rs = runs.toArray(StubGlyphList[]::new);
            RectBounds bounds = new RectBounds(0, 0, (float)x, (float)(charHeight /*+ lineSpacing*/));
            lines.add(new StubTextLine(rs, bounds, lineStart, len));

            column = 0;
            x = 0.0;
            runStartX = 0.0;
            y += charHeight;
            lineStart += len;
        }

        public void append(TextSpan span, String text, Font f) {
            this.span = span;
            charHeight = (f == null) ? DEFAULT_FONT_SIZE : f.getSize();
            charWidth = charHeight;
            if (f != null) {
                boolean bold = f.getStyle().toLowerCase().contains("bold");
                if (bold) {
                    charWidth++;
                }
            }

            int len = text.length();
            for (int i = 0; i < len; i++) {
                if(wrapWidth > 0) {
                    if(x > wrapWidth) {
                        addRun(i);
                        addLine();
                    }
                }
                
                char c = text.charAt(i);
                switch (c) {
                case '\t':
                    addRun(i);
                    if(tabSize > 0) {
                        double dw = (tabSize - (column % tabSize)) * charWidth;
                        x += dw;
                    } else {
                        x += charWidth;
                        column++;
                    }
                    i++;
                    addRun(i);
                    break;
                case '\n':
                    lineBreak = true;
                    addRun(i);
                    addLine();
                    continue;
                default:
                    x += charWidth;
                    column++;
                    break;
                }
            }
        }
    }
}
