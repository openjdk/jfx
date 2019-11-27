/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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


import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.Translate2D;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextSpan;
import java.text.Bidi;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class PrismTextLayout implements TextLayout {
    private static final BaseTransform IDENTITY = BaseTransform.IDENTITY_TRANSFORM;
    private static final int X_MIN_INDEX = 0;
    private static final int Y_MIN_INDEX = 1;
    private static final int X_MAX_INDEX = 2;
    private static final int Y_MAX_INDEX = 3;

    private static final Hashtable<Integer, LayoutCache> stringCache = new Hashtable<>();
    private static final Object  CACHE_SIZE_LOCK = new Object();
    private static int cacheSize = 0;
    private static final int MAX_STRING_SIZE = 256;
    private static final int MAX_CACHE_SIZE = PrismFontFactory.cacheLayoutSize;

    private char[] text;
    private TextSpan[] spans;   /* Rich text  (null for single font text) */
    private PGFont font;        /* Single font text (null for rich text) */
    private FontStrike strike;  /* cached strike of font (identity) */
    private Integer cacheKey;
    private TextLine[] lines;
    private TextRun[] runs;
    private int runCount;
    private BaseBounds logicalBounds;
    private RectBounds visualBounds;
    private float layoutWidth, layoutHeight;
    private float wrapWidth, spacing;
    private LayoutCache layoutCache;
    private Shape shape;
    private int flags;
    private int tabSize = DEFAULT_TAB_SIZE;

    public PrismTextLayout() {
        logicalBounds = new RectBounds();
        flags = ALIGN_LEFT;
    }

    private void reset() {
        layoutCache = null;
        runs = null;
        flags &= ~ANALYSIS_MASK;
        relayout();
    }

    private void relayout() {
        logicalBounds.makeEmpty();
        visualBounds = null;
        layoutWidth = layoutHeight = 0;
        flags &= ~(FLAGS_WRAPPED | FLAGS_CACHED_UNDERLINE | FLAGS_CACHED_STRIKETHROUGH);
        lines = null;
        shape = null;
    }

    /***************************************************************************
     *                                                                         *
     *                            TextLayout API                               *
     *                                                                         *
     **************************************************************************/

    public boolean setContent(TextSpan[] spans) {
        if (spans == null && this.spans == null) return false;
        if (spans != null && this.spans != null) {
            if (spans.length == this.spans.length) {
                int i = 0;
                while (i < spans.length) {
                    if (spans[i] != this.spans[i]) break;
                    i++;
                }
                if (i == spans.length) return false;
            }
        }

        reset();
        this.spans = spans;
        this.font = null;
        this.strike = null;
        this.text = null;   /* Initialized in getText() */
        this.cacheKey = null;
        return true;
    }

    public boolean setContent(String text, Object font) {
        reset();
        this.spans = null;
        this.font = (PGFont)font;
        this.strike = ((PGFont)font).getStrike(IDENTITY);
        this.text = text.toCharArray();
        if (MAX_CACHE_SIZE > 0) {
            int length = text.length();
            if (0 < length && length <= MAX_STRING_SIZE) {
                cacheKey = text.hashCode() * strike.hashCode();
            }
        }
        return true;
    }

    public boolean setDirection(int direction) {
        if ((flags & DIRECTION_MASK) == direction) return false;
        flags &= ~DIRECTION_MASK;
        flags |= (direction & DIRECTION_MASK);
        reset();
        return true;
    }

    public boolean setBoundsType(int type) {
        if ((flags & BOUNDS_MASK) == type) return false;
        flags &= ~BOUNDS_MASK;
        flags |= (type & BOUNDS_MASK);
        reset();
        return true;
    }

    public boolean setAlignment(int alignment) {
        int align = ALIGN_LEFT;
        switch (alignment) {
        case 0: align = ALIGN_LEFT; break;
        case 1: align = ALIGN_CENTER; break;
        case 2: align = ALIGN_RIGHT; break;
        case 3: align = ALIGN_JUSTIFY; break;
        }
        if ((flags & ALIGN_MASK) == align) return false;
        if (align == ALIGN_JUSTIFY || (flags & ALIGN_JUSTIFY) != 0) {
            reset();
        }
        flags &= ~ALIGN_MASK;
        flags |= align;
        relayout();
        return true;
    }

    public boolean setWrapWidth(float newWidth) {
        if (Float.isInfinite(newWidth)) newWidth = 0;
        if (Float.isNaN(newWidth)) newWidth = 0;
        float oldWidth = this.wrapWidth;
        this.wrapWidth = Math.max(0, newWidth);

        boolean needsLayout = true;
        if (lines != null && oldWidth != 0 && newWidth != 0) {
            if ((flags & ALIGN_LEFT) != 0) {
                if (newWidth > oldWidth) {
                    /* If wrapping width is increasing and there is no
                     * wrapped lines then the text remains valid.
                     */
                    if ((flags & FLAGS_WRAPPED) == 0) {
                        needsLayout = false;
                    }
                } else {
                    /* If wrapping width is decreasing but it is still
                     * greater than the max line width then the text
                     * remains valid.
                     */
                    if (newWidth >= layoutWidth) {
                        needsLayout = false;
                    }
                }
            }
        }
        if (needsLayout) relayout();
        return needsLayout;
    }

    public boolean setLineSpacing(float spacing) {
        if (this.spacing == spacing) return false;
        this.spacing = spacing;
        relayout();
        return true;
    }

    private void ensureLayout() {
        if (lines == null) {
            layout();
        }
    }

    public com.sun.javafx.scene.text.TextLine[] getLines() {
        ensureLayout();
        return lines;
    }

    public GlyphList[] getRuns() {
        ensureLayout();
        GlyphList[] result = new GlyphList[runCount];
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            GlyphList[] lineRuns = lines[i].getRuns();
            int length = lineRuns.length;
            System.arraycopy(lineRuns, 0, result, count, length);
            count += length;
        }
        return result;
    }

    public BaseBounds getBounds() {
        ensureLayout();
        return logicalBounds;
    }

    public BaseBounds getBounds(TextSpan filter, BaseBounds bounds) {
        ensureLayout();
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        if (filter != null) {
            for (int i = 0; i < lines.length; i++) {
                TextLine line = lines[i];
                TextRun[] lineRuns = line.getRuns();
                for (int j = 0; j < lineRuns.length; j++) {
                    TextRun run = lineRuns[j];
                    TextSpan span = run.getTextSpan();
                    if (span != filter) continue;
                    Point2D location = run.getLocation();
                    float runLeft = location.x;
                    if (run.isLeftBearing()) {
                        runLeft += line.getLeftSideBearing();
                    }
                    float runRight = location.x + run.getWidth();
                    if (run.isRightBearing()) {
                        runRight += line.getRightSideBearing();
                    }
                    float runTop = location.y;
                    float runBottom = location.y + line.getBounds().getHeight() + spacing;
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
            if (isMirrored()) {
                float width = getMirroringWidth();
                float bearing = left;
                left = width - right;
                right = width - bearing;
            }
        }
        return bounds.deriveWithNewBounds(left, top, 0, right, bottom, 0);
    }

    public PathElement[] getCaretShape(int offset, boolean isLeading,
                                       float x, float y) {
        ensureLayout();
        int lineIndex = 0;
        int lineCount = getLineCount();
        while (lineIndex < lineCount - 1) {
            TextLine line = lines[lineIndex];
            int lineEnd = line.getStart() + line.getLength();
            if (lineEnd > offset) break;
            lineIndex++;
        }
        int sliptCaretOffset = -1;
        int level = 0;
        float lineX = 0, lineY = 0, lineHeight = 0;
        TextLine line = lines[lineIndex];
        TextRun[] runs = line.getRuns();
        int runCount = runs.length;
        int runIndex = -1;
        for (int i = 0; i < runCount; i++) {
            TextRun run = runs[i];
            int runStart = run.getStart();
            int runEnd = run.getEnd();
            if (runStart <= offset && offset < runEnd) {
                if (!run.isLinebreak()) {
                    runIndex = i;
                }
                break;
            }
        }
        if (runIndex != -1) {
            TextRun run = runs[runIndex];
            int runStart = run.getStart();
            Point2D location = run.getLocation();
            lineX = location.x + run.getXAtOffset(offset - runStart, isLeading);
            lineY = location.y;
            lineHeight = line.getBounds().getHeight();

            if (isLeading) {
                if (runIndex > 0 && offset == runStart) {
                    level = run.getLevel();
                    sliptCaretOffset = offset - 1;
                }
            } else {
                int runEnd = run.getEnd();
                if (runIndex + 1 < runs.length && offset + 1 == runEnd) {
                    level = run.getLevel();
                    sliptCaretOffset = offset + 1;
                }
            }
        } else {
            /* end of line (line break or offset>=charCount) */
            int maxOffset = 0;

            /* set run index to zero to handle empty line case (only break line) */
            runIndex = 0;
            for (int i = 0; i < runCount; i++) {
                TextRun run = runs[i];
                /*use the trailing edge of the last logical run*/
                if (run.getStart() >= maxOffset && !run.isLinebreak()) {
                    maxOffset = run.getStart();
                    runIndex = i;
                }
            }
            TextRun run = runs[runIndex];
            Point2D location = run.getLocation();
            lineX = location.x + (run.isLeftToRight() ? run.getWidth() : 0);
            lineY = location.y;
            lineHeight = line.getBounds().getHeight();
        }
        if (isMirrored()) {
            lineX = getMirroringWidth() - lineX;
        }
        lineX += x;
        lineY += y;
        if (sliptCaretOffset != -1) {
            for (int i = 0; i < runs.length; i++) {
                TextRun run = runs[i];
                int runStart = run.getStart();
                int runEnd = run.getEnd();
                if (runStart <= sliptCaretOffset && sliptCaretOffset < runEnd) {
                    if ((run.getLevel() & 1) != (level & 1)) {
                        Point2D location = run.getLocation();
                        float lineX2 = location.x;
                        if (isLeading) {
                            if ((level & 1) != 0) lineX2 += run.getWidth();
                        } else {
                            if ((level & 1) == 0) lineX2 += run.getWidth();
                        }
                        if (isMirrored()) {
                            lineX2 = getMirroringWidth() - lineX2;
                        }
                        lineX2 += x;
                        PathElement[] result = new PathElement[4];
                        result[0] = new MoveTo(lineX, lineY);
                        result[1] = new LineTo(lineX, lineY + lineHeight / 2);
                        result[2] = new MoveTo(lineX2, lineY + lineHeight / 2);
                        result[3] = new LineTo(lineX2, lineY + lineHeight);
                        return result;
                    }
                }
            }
        }
        PathElement[] result = new PathElement[2];
        result[0] = new MoveTo(lineX, lineY);
        result[1] = new LineTo(lineX, lineY + lineHeight);
        return result;
    }

    public Hit getHitInfo(float x, float y) {
        int charIndex = -1;
        boolean leading = false;

        ensureLayout();
        int lineIndex = getLineIndex(y);
        if (lineIndex >= getLineCount()) {
            charIndex = getCharCount();
        } else {
            if (isMirrored()) {
                x = getMirroringWidth() - x;
            }
            TextLine line = lines[lineIndex];
            TextRun[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            TextRun run = null;
            x -= bounds.getMinX();
            //TODO binary search
            for (int i = 0; i < runs.length; i++) {
                run = runs[i];
                if (x < run.getWidth()) break;
                if (i + 1 < runs.length) {
                    if (runs[i + 1].isLinebreak()) break;
                    x -= run.getWidth();
                }
            }
            if (run != null) {
                int[] trailing = new int[1];
                charIndex = run.getStart() + run.getOffsetAtX(x, trailing);
                leading = (trailing[0] == 0);
            } else {
                //empty line, set to line break leading
                charIndex = line.getStart();
                leading = true;
            }
        }
        return new Hit(charIndex, -1, leading);
    }

    public PathElement[] getRange(int start, int end, int type,
                                  float x, float y) {
        ensureLayout();
        int lineCount = getLineCount();
        ArrayList<PathElement> result = new ArrayList<PathElement>();
        float lineY = 0;

        for  (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            TextLine line = lines[lineIndex];
            RectBounds lineBounds = line.getBounds();
            int lineStart = line.getStart();
            if (lineStart >= end) break;
            int lineEnd = lineStart + line.getLength();
            if (start > lineEnd) {
                lineY += lineBounds.getHeight() + spacing;
                continue;
            }

            /* The list of runs in the line is visually ordered.
             * Thus, finding the run that includes the selection end offset
             * does not mean that all selected runs have being visited.
             * Instead, this implementation first computes the number of selected
             * characters in the current line, then iterates over the runs consuming
             * selected characters till all of them are found.
             */
            TextRun[] runs = line.getRuns();
            int count = Math.min(lineEnd, end) - Math.max(lineStart, start);
            int runIndex = 0;
            float left = -1;
            float right = -1;
            float lineX = lineBounds.getMinX();
            while (count > 0 && runIndex < runs.length) {
                TextRun run = runs[runIndex];
                int runStart = run.getStart();
                int runEnd = run.getEnd();
                float runWidth = run.getWidth();
                int clmapStart = Math.max(runStart, Math.min(start, runEnd));
                int clampEnd = Math.max(runStart, Math.min(end, runEnd));
                int runCount = clampEnd - clmapStart;
                if (runCount != 0) {
                    boolean ltr = run.isLeftToRight();
                    float runLeft;
                    if (runStart > start) {
                        runLeft = ltr ? lineX : lineX + runWidth;
                    } else {
                        runLeft = lineX + run.getXAtOffset(start - runStart, true);
                    }
                    float runRight;
                    if (runEnd < end) {
                        runRight = ltr ? lineX + runWidth : lineX;
                    } else {
                        runRight = lineX + run.getXAtOffset(end - runStart, true);
                    }
                    if (runLeft > runRight) {
                        float tmp = runLeft;
                        runLeft = runRight;
                        runRight = tmp;
                    }
                    count -= runCount;
                    float top = 0, bottom = 0;
                    switch (type) {
                    case TYPE_TEXT:
                        top = lineY;
                        bottom = lineY + lineBounds.getHeight();
                        break;
                    case TYPE_UNDERLINE:
                    case TYPE_STRIKETHROUGH:
                        FontStrike fontStrike = null;
                        if (spans != null) {
                            TextSpan span = run.getTextSpan();
                            PGFont font = (PGFont)span.getFont();
                            if (font == null) break;
                            fontStrike = font.getStrike(IDENTITY);
                        } else {
                            fontStrike = strike;
                        }
                        top = lineY - run.getAscent();
                        Metrics metrics = fontStrike.getMetrics();
                        if (type == TYPE_UNDERLINE) {
                            top += metrics.getUnderLineOffset();
                            bottom = top + metrics.getUnderLineThickness();
                        } else {
                            top += metrics.getStrikethroughOffset();
                            bottom = top + metrics.getStrikethroughThickness();
                        }
                        break;
                    }

                    /* Merge continuous rectangles */
                    if (runLeft != right) {
                        if (left != -1 && right != -1) {
                            float l = left, r = right;
                            if (isMirrored()) {
                                float width = getMirroringWidth();
                                l = width - l;
                                r = width - r;
                            }
                            result.add(new MoveTo(x + l,  y + top));
                            result.add(new LineTo(x + r, y + top));
                            result.add(new LineTo(x + r, y + bottom));
                            result.add(new LineTo(x + l,  y + bottom));
                            result.add(new LineTo(x + l,  y + top));
                        }
                        left = runLeft;
                        right = runRight;
                    }
                    right = runRight;
                    if (count == 0) {
                        float l = left, r = right;
                        if (isMirrored()) {
                            float width = getMirroringWidth();
                            l = width - l;
                            r = width - r;
                        }
                        result.add(new MoveTo(x + l,  y + top));
                        result.add(new LineTo(x + r, y + top));
                        result.add(new LineTo(x + r, y + bottom));
                        result.add(new LineTo(x + l,  y + bottom));
                        result.add(new LineTo(x + l,  y + top));
                    }
                }
                lineX += runWidth;
                runIndex++;
            }
            lineY += lineBounds.getHeight() + spacing;
        }
        return result.toArray(new PathElement[result.size()]);
    }

    public Shape getShape(int type, TextSpan filter) {
        ensureLayout();
        boolean text = (type & TYPE_TEXT) != 0;
        boolean underline = (type & TYPE_UNDERLINE) != 0;
        boolean strikethrough = (type & TYPE_STRIKETHROUGH) != 0;
        boolean baselineType = (type & TYPE_BASELINE) != 0;
        if (shape != null && text && !underline && !strikethrough && baselineType) {
            return shape;
        }

        Path2D outline = new Path2D();
        BaseTransform tx = new Translate2D(0, 0);
        /* Return a shape relative to the baseline of the first line so
         * it can be used for layout */
        float firstBaseline = 0;
        if (baselineType) {
            firstBaseline = -lines[0].getBounds().getMinY();
        }
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            TextRun[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            float baseline = -bounds.getMinY();
            for (int j = 0; j < runs.length; j++) {
                TextRun run = runs[j];
                FontStrike fontStrike = null;
                if (spans != null) {
                    TextSpan span = run.getTextSpan();
                    if (filter != null && span != filter) continue;
                    PGFont font = (PGFont)span.getFont();

                    /* skip embedded runs */
                    if (font == null) continue;
                    fontStrike = font.getStrike(IDENTITY);
                } else {
                    fontStrike = strike;
                }
                Point2D location = run.getLocation();
                float runX = location.x;
                float runY = location.y + baseline - firstBaseline;
                Metrics metrics = null;
                if (underline || strikethrough) {
                    metrics = fontStrike.getMetrics();
                }
                if (underline) {
                    RoundRectangle2D rect = new RoundRectangle2D();
                    rect.x = runX;
                    rect.y = runY + metrics.getUnderLineOffset();
                    rect.width = run.getWidth();
                    rect.height = metrics.getUnderLineThickness();
                    outline.append(rect, false);
                }
                if (strikethrough) {
                    RoundRectangle2D rect = new RoundRectangle2D();
                    rect.x = runX;
                    rect.y = runY + metrics.getStrikethroughOffset();
                    rect.width = run.getWidth();
                    rect.height = metrics.getStrikethroughThickness();
                    outline.append(rect, false);
                }
                if (text && run.getGlyphCount() > 0) {
                    tx.restoreTransform(1, 0, 0, 1, runX, runY);
                    Path2D path = (Path2D)fontStrike.getOutline(run, tx);
                    outline.append(path, false);
                }
            }
        }

        if (text && !underline && !strikethrough) {
            shape = outline;
        }
        return outline;
    }

    @Override
    public boolean setTabSize(int spaces) {
        if (spaces < 1) {
            spaces = 1;
        }
        if (tabSize != spaces) {
            tabSize = spaces;
            relayout();
            return true;
        }
        return false;
    }

    /***************************************************************************
     *                                                                         *
     *                     Text Layout Implementation                          *
     *                                                                         *
     **************************************************************************/

    private int getLineIndex(float y) {
        int index = 0;
        float bottom = 0;
        int lineCount = getLineCount();
        while (index < lineCount) {
            bottom += lines[index].getBounds().getHeight() + spacing;
            if (index + 1 == lineCount) bottom -= lines[index].getLeading();
            if (bottom > y) break;
            index++;
        }
        return index;
    }

    private boolean copyCache() {
        int align = flags & ALIGN_MASK;
        int boundsType = flags & BOUNDS_MASK;
        /* Caching for boundsType == Center, bias towards  Modena */
        return wrapWidth != 0 || align != ALIGN_LEFT || boundsType == 0 || isMirrored();
    }

    private void initCache() {
        if (cacheKey != null) {
            if (layoutCache == null) {
                LayoutCache cache = stringCache.get(cacheKey);
                if (cache != null && cache.font.equals(font) && Arrays.equals(cache.text, text)) {
                    layoutCache = cache;
                    runs = cache.runs;
                    runCount = cache.runCount;
                    flags |= cache.analysis;
                }
            }
            if (layoutCache != null) {
                if (copyCache()) {
                    /* This instance has some property that requires it to
                     * build its own lines (i.e. wrapping width). Thus, only use
                     * the runs from the cache (and it needs to make a copy
                     * before using it as they will be modified).
                     * Note: the copy of the elements in the array happens in
                     * reuseRuns().
                     */
                    if (layoutCache.runs == runs) {
                        runs = new TextRun[runCount];
                        System.arraycopy(layoutCache.runs, 0, runs, 0, runCount);
                    }
                } else {
                    if (layoutCache.lines != null) {
                        runs = layoutCache.runs;
                        runCount = layoutCache.runCount;
                        flags |= layoutCache.analysis;
                        lines = layoutCache.lines;
                        layoutWidth = layoutCache.layoutWidth;
                        layoutHeight = layoutCache.layoutHeight;
                        float ascent = lines[0].getBounds().getMinY();
                        logicalBounds = logicalBounds.deriveWithNewBounds(0, ascent, 0,
                                layoutWidth, layoutHeight + ascent, 0);
                    }
                }
            }
        }
    }

    private int getLineCount() {
        return lines.length;
    }

    private int getCharCount() {
        if (text != null) return text.length;
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            count += lines[i].getLength();
        }
        return count;
    }

    public TextSpan[] getTextSpans() {
        return spans;
    }

    public PGFont getFont() {
        return font;
    }

    public int getDirection() {
        if ((flags & DIRECTION_LTR) != 0) {
            return Bidi.DIRECTION_LEFT_TO_RIGHT;
        }
        if ((flags & DIRECTION_RTL) != 0) {
            return Bidi.DIRECTION_RIGHT_TO_LEFT;
        }
        if ((flags & DIRECTION_DEFAULT_LTR) != 0) {
            return Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT;
        }
        if ((flags & DIRECTION_DEFAULT_RTL) != 0) {
            return Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT;
        }
        return Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT;
    }

    public void addTextRun(TextRun run) {
        if (runCount + 1 > runs.length) {
            TextRun[] newRuns = new TextRun[runs.length + 64];
            System.arraycopy(runs, 0, newRuns, 0, runs.length);
            runs = newRuns;
        }
        runs[runCount++] = run;
    }

    private void buildRuns(char[] chars) {
        runCount = 0;
        if (runs == null) {
            int count = Math.max(4, Math.min(chars.length / 16, 16));
            runs = new TextRun[count];
        }
        GlyphLayout layout = GlyphLayout.getInstance();
        flags = layout.breakRuns(this, chars, flags);
        layout.dispose();
        for (int j = runCount; j < runs.length; j++) {
            runs[j] = null;
        }
    }

    private void shape(TextRun run, char[] chars, GlyphLayout layout) {
        FontStrike strike;
        PGFont font;
        if (spans != null) {
            if (spans.length == 0) return;
            TextSpan span = run.getTextSpan();
            font = (PGFont)span.getFont();
            if (font == null) {
                RectBounds bounds = span.getBounds();
                run.setEmbedded(bounds, span.getText().length());
                return;
            }
            strike = font.getStrike(IDENTITY);
        } else {
            font = this.font;
            strike = this.strike;
        }

        /* init metrics for line breaks for empty lines */
        if (run.getAscent() == 0) {
            Metrics m = strike.getMetrics();

            /* The implementation of the center layoutBounds mode is to assure the
             * layout has the same number of pixels above and bellow the cap
             * height.
             */
            if ((flags & BOUNDS_MASK) == BOUNDS_CENTER) {
                float ascent = m.getAscent();
                /* Segoe UI has a very large internal leading area, applying the
                 * center layoutBounds heuristics on it would result in several pixels
                 * being added to the descent. The final results would be
                 * overly large and visually unappealing. The fix is to reduce
                 * the ascent before applying the algorithm. */
                if (font.getFamilyName().equals("Segoe UI")) {
                    ascent *= 0.80;
                }
                ascent = (int)(ascent-0.75);
                float descent = (int)(m.getDescent()+0.75);
                float leading = (int)(m.getLineGap()+0.75);
                float capHeight = (int)(m.getCapHeight()+0.75);
                float topPadding = -ascent - capHeight;
                if (topPadding > descent) {
                    descent = topPadding;
                } else {
                    ascent += (topPadding - descent);
                }
                run.setMetrics(ascent, descent, leading);
            } else {
                run.setMetrics(m.getAscent(), m.getDescent(), m.getLineGap());
            }
        }

        if (run.isTab()) return;
        if (run.isLinebreak()) return;
        if (run.getGlyphCount() > 0) return;
        if (run.isComplex()) {
            /* Use GlyphLayout to shape complex text */
            layout.layout(run, font, strike, chars);
        } else {
            FontResource fr = strike.getFontResource();
            int start = run.getStart();
            int length = run.getLength();

            /* No glyph layout required */
            if (layoutCache == null) {
                float fontSize = strike.getSize();
                CharToGlyphMapper mapper  = fr.getGlyphMapper();

                /* The text contains complex and non-complex runs */
                int[] glyphs = new int[length];
                mapper.charsToGlyphs(start, length, chars, glyphs);
                float[] positions = new float[(length + 1) << 1];
                float xadvance = 0;
                for (int i = 0; i < length; i++) {
                    float width = fr.getAdvance(glyphs[i], fontSize);
                    positions[i<<1] = xadvance;
                    //yadvance always zero
                    xadvance += width;
                }
                positions[length<<1] = xadvance;
                run.shape(length, glyphs, positions, null);
            } else {

                /* The text only contains non-complex runs, all the glyphs and
                 * advances are stored in the shapeCache */
                if (!layoutCache.valid) {
                    float fontSize = strike.getSize();
                    CharToGlyphMapper mapper  = fr.getGlyphMapper();
                    mapper.charsToGlyphs(start, length, chars, layoutCache.glyphs, start);
                    int end = start + length;
                    float width = 0;
                    for (int i = start; i < end; i++) {
                        float adv = fr.getAdvance(layoutCache.glyphs[i], fontSize);
                        layoutCache.advances[i] = adv;
                        width += adv;
                    }
                    run.setWidth(width);
                }
                run.shape(length, layoutCache.glyphs, layoutCache.advances);
            }
        }
    }

    private TextLine createLine(int start, int end, int startOffset) {
        int count = end - start + 1;
        TextRun[] lineRuns = new TextRun[count];
        if (start < runCount) {
            System.arraycopy(runs, start, lineRuns, 0, count);
        }

        /* Recompute line width, height, and length (wrapping) */
        float width = 0, ascent = 0, descent = 0, leading = 0;
        int length = 0;
        for (int i = 0; i < lineRuns.length; i++) {
            TextRun run = lineRuns[i];
            width += run.getWidth();
            ascent = Math.min(ascent, run.getAscent());
            descent = Math.max(descent, run.getDescent());
            leading = Math.max(leading, run.getLeading());
            length += run.getLength();
        }
        if (width > layoutWidth) layoutWidth = width;
        return new TextLine(startOffset, length, lineRuns,
                            width, ascent, descent, leading);
    }

    private void reorderLine(TextLine line) {
        TextRun[] runs = line.getRuns();
        int length = runs.length;
        if (length > 0 && runs[length - 1].isLinebreak()) {
            length--;
        }
        if (length < 2) return;
        byte[] levels = new byte[length];
        for (int i = 0; i < length; i++) {
            levels[i] = runs[i].getLevel();
        }
        Bidi.reorderVisually(levels, 0, runs, 0, length);
    }

    private char[] getText() {
        if (text == null) {
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
        }
        return text;
    }

    private boolean isSimpleLayout() {
        int textAlignment = flags & ALIGN_MASK;
        boolean justify = wrapWidth > 0 && textAlignment == ALIGN_JUSTIFY;
        int mask = FLAGS_HAS_BIDI | FLAGS_HAS_COMPLEX;
        return (flags & mask) == 0 && !justify;
    }

    private boolean isMirrored() {
        boolean mirrored = false;
        switch (flags & DIRECTION_MASK) {
        case DIRECTION_RTL: mirrored = true; break;
        case DIRECTION_LTR: mirrored = false; break;
        case DIRECTION_DEFAULT_LTR:
        case DIRECTION_DEFAULT_RTL:
            mirrored = (flags & FLAGS_RTL_BASE) != 0;
        }
        return mirrored;
    }

    private float getMirroringWidth() {
        /* The text node in the scene layer is mirrored based on
         * result of computeLayoutBounds. The coordinate translation
         * in text layout has to be based on the same width.
         */
        return wrapWidth != 0 ? wrapWidth : layoutWidth;
    }

    private void reuseRuns() {
        /* The runs list is always accessed by the same thread (as TextLayout
         * is not thread safe) thus it can be modified at any time, but the
         * elements inside of the list are shared among threads and cannot be
         * modified. Each reused element has to be cloned.*/
        runCount = 0;
        int index = 0;;
        while (index < runs.length) {
            TextRun run = runs[index];
            if (run == null) break;
            runs[index] = null;
            index++;
            runs[runCount++] = run = run.unwrap();

            if (run.isSplit()) {
                run.merge(null); /* unmark split */
                while (index < runs.length) {
                    TextRun nextRun = runs[index];
                    if (nextRun == null) break;
                    run.merge(nextRun);
                    runs[index] = null;
                    index++;
                    if (nextRun.isSplitLast()) break;
                }
            }
        }
    }

    private float getTabAdvance() {
        float spaceAdvance = 0;
        if (spans != null) {
            /* Rich text case - use the first font (for now) */
            for (int i = 0; i < spans.length; i++) {
                TextSpan span = spans[i];
                PGFont font = (PGFont)span.getFont();
                if (font != null) {
                    FontStrike strike = font.getStrike(IDENTITY);
                    spaceAdvance = strike.getCharAdvance(' ');
                    break;
                }
            }
        } else {
            spaceAdvance = strike.getCharAdvance(' ');
        }
        return tabSize * spaceAdvance;
    }

    private void layout() {
        /* Try the cache */
        initCache();

        /* Whole layout retrieved from the cache */
        if (lines != null) return;
        char[] chars = getText();

        /* runs and runCount are set in reuseRuns or buildRuns */
        if ((flags & FLAGS_ANALYSIS_VALID) != 0 && isSimpleLayout()) {
            reuseRuns();
        } else {
            buildRuns(chars);
        }

        GlyphLayout layout = null;
        if ((flags & (FLAGS_HAS_COMPLEX)) != 0) {
            layout = GlyphLayout.getInstance();
        }

        float tabAdvance = 0;
        if ((flags & FLAGS_HAS_TABS) != 0) {
            tabAdvance = getTabAdvance();
        }

        BreakIterator boundary = null;
        if (wrapWidth > 0) {
            if ((flags & (FLAGS_HAS_COMPLEX | FLAGS_HAS_CJK)) != 0) {
                boundary = BreakIterator.getLineInstance();
                boundary.setText(new CharArrayIterator(chars));
            }
        }
        int textAlignment = flags & ALIGN_MASK;

        /* Optimize simple case: reuse the glyphs and advances as long as the
         * text and font are the same.
         * The simple case is no bidi, no complex, no justify, no features.
         */

        if (isSimpleLayout()) {
            if (layoutCache == null) {
                layoutCache = new LayoutCache();
                layoutCache.glyphs = new int[chars.length];
                layoutCache.advances = new float[chars.length];
            }
        } else {
            layoutCache = null;
        }

        float lineWidth = 0;
        int startIndex = 0;
        int startOffset = 0;
        ArrayList<TextLine> linesList = new ArrayList<TextLine>();
        for (int i = 0; i < runCount; i++) {
            TextRun run = runs[i];
            shape(run, chars, layout);
            if (run.isTab()) {
                float tabStop = ((int)(lineWidth / tabAdvance) +1) * tabAdvance;
                run.setWidth(tabStop - lineWidth);
            }

            float runWidth = run.getWidth();
            if (wrapWidth > 0 && lineWidth + runWidth > wrapWidth && !run.isLinebreak()) {

                /* Find offset of the first character that does not fit on the line */
                int hitOffset = run.getStart() + run.getWrapIndex(wrapWidth - lineWidth);

                /* Only keep whitespaces (not tabs) in the current run to avoid
                 * dealing with unshaped runs.
                 */
                int offset = hitOffset;
                int runEnd = run.getEnd();
                while (offset + 1 < runEnd && chars[offset] == ' ') {
                    offset++;
                    /* Preserve behaviour: only keep one white space in the line
                     * before wrapping. Needed API to allow change.
                     */
                    break;
                }

                /* Find the break opportunity */
                int breakOffset = offset;
                if (boundary != null) {
                    /* Use Java BreakIterator when complex script are present */
                    breakOffset = boundary.isBoundary(offset) || chars[offset] == '\t' ? offset : boundary.preceding(offset);
                } else {
                    /* Simple break strategy for latin text (Performance) */
                    boolean currentChar = Character.isWhitespace(chars[breakOffset]);
                    while (breakOffset > startOffset) {
                        boolean previousChar = Character.isWhitespace(chars[breakOffset - 1]);
                        if (!currentChar && previousChar) break;
                        currentChar = previousChar;
                        breakOffset--;
                    }
                }

                /* Never break before the line start offset */
                if (breakOffset < startOffset) breakOffset = startOffset;

                /* Find the run that contains the break offset */
                int breakRunIndex = startIndex;
                TextRun breakRun = null;
                while (breakRunIndex < runCount) {
                    breakRun = runs[breakRunIndex];
                    if (breakRun.getEnd() > breakOffset) break;
                    breakRunIndex++;
                }

                /* No line breaks  between hit offset and line start offset.
                 * Try character wrapping mode at the hit offset.
                 */
                if (breakOffset == startOffset) {
                    breakRun = run;
                    breakRunIndex = i;
                    breakOffset = hitOffset;
                }

                int breakOffsetInRun = breakOffset - breakRun.getStart();
                /* Wrap the entire run to the next (only if it is not the first
                 * run of the line).
                 */
                if (breakOffsetInRun == 0 && breakRunIndex != startIndex) {
                    i = breakRunIndex - 1;
                } else {
                    i = breakRunIndex;

                    /* The break offset is at the first offset of the first run of the line.
                     * This happens when the wrap width is smaller than the width require
                     * to show the first character for the line.
                     */
                    if (breakOffsetInRun == 0) {
                        breakOffsetInRun++;
                    }
                    if (breakOffsetInRun < breakRun.getLength()) {
                        if (runCount >= runs.length) {
                            TextRun[] newRuns = new TextRun[runs.length + 64];
                            System.arraycopy(runs, 0, newRuns, 0, i + 1);
                            System.arraycopy(runs, i + 1, newRuns, i + 2, runs.length - i - 1);
                            runs = newRuns;
                        } else {
                            System.arraycopy(runs, i + 1, runs, i + 2, runCount - i - 1);
                        }
                        runs[i + 1] = breakRun.split(breakOffsetInRun);
                        if (breakRun.isComplex()) {
                            shape(breakRun, chars, layout);
                        }
                        runCount++;
                    }
                }

                /* No point marking the last run of a line a softbreak */
                if (i + 1 < runCount && !runs[i + 1].isLinebreak()) {
                    run = runs[i];
                    run.setSoftbreak();
                    flags |= FLAGS_WRAPPED;

                    // Tabs should preserve width

                    /*
                     * Due to contextual forms (arabic) it is possible this line
                     * is still too big since the splitting of the arabic run
                     * changes the shape of boundary glyphs. For now the
                     * implementation has opted to have the appropriate
                     * initial/final shapes and allow those glyphs to
                     * potentially overlap the wrapping width, rather than use
                     * the medial form within the wrappingWidth. A better place
                     * to solve this would be TextRun#getWrapIndex - but its TBD
                     * there too.
                     */
                }
            }

            lineWidth += runWidth;
            if (run.isBreak()) {
                TextLine line = createLine(startIndex, i, startOffset);
                linesList.add(line);
                startIndex = i + 1;
                startOffset += line.getLength();
                lineWidth = 0;
            }
        }
        if (layout != null) layout.dispose();

        linesList.add(createLine(startIndex, runCount - 1, startOffset));
        lines = new TextLine[linesList.size()];
        linesList.toArray(lines);

        float fullWidth = Math.max(wrapWidth, layoutWidth);
        float lineY = 0;
        float align;
        if (isMirrored()) {
            align = 1; /* Left and Justify */
            if (textAlignment == ALIGN_RIGHT) align = 0;
        } else {
            align = 0; /* Left and Justify */
            if (textAlignment == ALIGN_RIGHT) align = 1;
        }
        if (textAlignment == ALIGN_CENTER) align = 0.5f;
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            int lineStart = line.getStart();
            RectBounds bounds = line.getBounds();

            /* Center and right alignment */
            float lineX = (fullWidth - bounds.getWidth()) * align;
            line.setAlignment(lineX);

            /* Justify */
            boolean justify = wrapWidth > 0 && textAlignment == ALIGN_JUSTIFY;
            if (justify) {
                TextRun[] lineRuns = line.getRuns();
                int lineRunCount = lineRuns.length;
                if (lineRunCount > 0 && lineRuns[lineRunCount - 1].isSoftbreak()) {
                    /* count white spaces but skipping trailings whitespaces */
                    int lineEnd = lineStart + line.getLength();
                    int wsCount = 0;
                    boolean hitChar = false;
                    for (int j = lineEnd - 1; j >= lineStart; j--) {
                        if (!hitChar && chars[j] != ' ') hitChar = true;
                        if (hitChar && chars[j] == ' ') wsCount++;
                    }
                    if (wsCount != 0) {
                        float inc = (fullWidth - bounds.getWidth()) / wsCount;
                        done:
                        for (int j = 0; j < lineRunCount; j++) {
                            TextRun textRun = lineRuns[j];
                            int runStart = textRun.getStart();
                            int runEnd = textRun.getEnd();
                            for (int k = runStart; k < runEnd; k++) {
                                // TODO kashidas
                                if (chars[k] == ' ') {
                                    textRun.justify(k - runStart, inc);
                                    if (--wsCount == 0) break done;
                                }
                            }
                        }
                        lineX = 0;
                        line.setAlignment(lineX);
                        line.setWidth(fullWidth);
                    }
                }
            }

            if ((flags & FLAGS_HAS_BIDI) != 0) {
                reorderLine(line);
            }

            computeSideBearings(line);

            /* Set run location */
            float runX = lineX;
            TextRun[] lineRuns = line.getRuns();
            for (int j = 0; j < lineRuns.length; j++) {
                TextRun run = lineRuns[j];
                run.setLocation(runX, lineY);
                run.setLine(line);
                runX += run.getWidth();
            }
            if (i + 1 < lines.length) {
                lineY = Math.max(lineY, lineY + bounds.getHeight() + spacing);
            } else {
                lineY += (bounds.getHeight() - line.getLeading());
            }
        }
        float ascent = lines[0].getBounds().getMinY();
        layoutHeight = lineY;
        logicalBounds = logicalBounds.deriveWithNewBounds(0, ascent, 0, layoutWidth,
                                            layoutHeight + ascent, 0);


        if (layoutCache != null) {
            if (cacheKey != null && !layoutCache.valid && !copyCache()) {
                /* After layoutCache is added to the stringCache it can be
                 * accessed by multiple threads. All the data in it must
                 * be immutable. See copyCache() for the cases where the entire
                 * layout is immutable.
                 */
                layoutCache.font = font;
                layoutCache.text = text;
                layoutCache.runs = runs;
                layoutCache.runCount = runCount;
                layoutCache.lines = lines;
                layoutCache.layoutWidth = layoutWidth;
                layoutCache.layoutHeight = layoutHeight;
                layoutCache.analysis = flags & ANALYSIS_MASK;
                synchronized (CACHE_SIZE_LOCK) {
                    int charCount = chars.length;
                    if (cacheSize + charCount > MAX_CACHE_SIZE) {
                        stringCache.clear();
                        cacheSize = 0;
                    }
                    stringCache.put(cacheKey, layoutCache);
                    cacheSize += charCount;
                }
            }
            layoutCache.valid = true;
        }
    }

    @Override
    public BaseBounds getVisualBounds(int type) {
        ensureLayout();

        /* Not defined for rich text */
        if (strike == null) {
            return null;
        }

        boolean underline = (type & TYPE_UNDERLINE) != 0;
        boolean hasUnderline = (flags & FLAGS_CACHED_UNDERLINE) != 0;
        boolean strikethrough = (type & TYPE_STRIKETHROUGH) != 0;
        boolean hasStrikethrough = (flags & FLAGS_CACHED_STRIKETHROUGH) != 0;
        if (visualBounds != null && underline == hasUnderline
                && strikethrough == hasStrikethrough) {
            /* Return last cached value */
            return visualBounds;
        }

        flags &= ~(FLAGS_CACHED_STRIKETHROUGH | FLAGS_CACHED_UNDERLINE);
        if (underline) flags |= FLAGS_CACHED_UNDERLINE;
        if (strikethrough) flags |= FLAGS_CACHED_STRIKETHROUGH;
        visualBounds = new RectBounds();

        float xMin = Float.POSITIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY;
        float xMax = Float.NEGATIVE_INFINITY;
        float yMax = Float.NEGATIVE_INFINITY;
        float bounds[] = new float[4];
        FontResource fr = strike.getFontResource();
        Metrics metrics = strike.getMetrics();
        float size = strike.getSize();
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            TextRun[] runs = line.getRuns();
            for (int j = 0; j < runs.length; j++) {
                TextRun run = runs[j];
                Point2D pt = run.getLocation();
                if (run.isLinebreak()) continue;
                int glyphCount = run.getGlyphCount();
                for (int gi = 0; gi < glyphCount; gi++) {
                    int gc = run.getGlyphCode(gi);
                    if (gc != CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
                        fr.getGlyphBoundingBox(run.getGlyphCode(gi), size, bounds);
                        if (bounds[X_MIN_INDEX] != bounds[X_MAX_INDEX]) {
                            float glyphX = pt.x + run.getPosX(gi);
                            float glyphY = pt.y + run.getPosY(gi);
                            float glyphMinX = glyphX + bounds[X_MIN_INDEX];
                            float glyphMinY = glyphY - bounds[Y_MAX_INDEX];
                            float glyphMaxX = glyphX + bounds[X_MAX_INDEX];
                            float glyphMaxY = glyphY - bounds[Y_MIN_INDEX];
                            if (glyphMinX < xMin) xMin = glyphMinX;
                            if (glyphMinY < yMin) yMin = glyphMinY;
                            if (glyphMaxX > xMax) xMax = glyphMaxX;
                            if (glyphMaxY > yMax) yMax = glyphMaxY;
                        }
                    }
                }
                if (underline) {
                    float underlineMinX = pt.x;
                    float underlineMinY = pt.y + metrics.getUnderLineOffset();
                    float underlineMaxX = underlineMinX + run.getWidth();
                    float underlineMaxY = underlineMinY + metrics.getUnderLineThickness();
                    if (underlineMinX < xMin) xMin = underlineMinX;
                    if (underlineMinY < yMin) yMin = underlineMinY;
                    if (underlineMaxX > xMax) xMax = underlineMaxX;
                    if (underlineMaxY > yMax) yMax = underlineMaxY;
                }
                if (strikethrough) {
                    float strikethroughMinX = pt.x;
                    float strikethroughMinY = pt.y + metrics.getStrikethroughOffset();
                    float strikethroughMaxX = strikethroughMinX + run.getWidth();
                    float strikethroughMaxY = strikethroughMinY + metrics.getStrikethroughThickness();
                    if (strikethroughMinX < xMin) xMin = strikethroughMinX;
                    if (strikethroughMinY < yMin) yMin = strikethroughMinY;
                    if (strikethroughMaxX > xMax) xMax = strikethroughMaxX;
                    if (strikethroughMaxY > yMax) yMax = strikethroughMaxY;
                }
            }
        }

        if (xMin < xMax && yMin < yMax) {
            visualBounds.setBounds(xMin, yMin, xMax, yMax);
        }
        return visualBounds;
    }

    private void computeSideBearings(TextLine line) {
        TextRun[] runs = line.getRuns();
        if (runs.length == 0) return;
        float bounds[] = new float[4];
        FontResource defaultFontResource = null;
        float size = 0;
        if (strike != null) {
            defaultFontResource = strike.getFontResource();
            size = strike.getSize();
        }

        /* The line lsb is the lsb of the first visual character in the line */
        float lsb = 0;
        float width = 0;
        lsbdone:
        for (int i = 0; i < runs.length; i++) {
            TextRun run = runs[i];
            int glyphCount = run.getGlyphCount();
            for (int gi = 0; gi < glyphCount; gi++) {
                float advance = run.getAdvance(gi);
                /* Skip any leading zero-width glyphs in the line */
                if (advance != 0) {
                    int gc = run.getGlyphCode(gi);
                    /* Skip any leading invisible glyphs in the line */
                    if (gc != CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
                        FontResource fr = defaultFontResource;
                        if (fr == null) {
                            TextSpan span = run.getTextSpan();
                            PGFont font = (PGFont)span.getFont();
                            /* No need to check font != null (run.glyphCount > 0)  */
                            size = font.getSize();
                            fr = font.getFontResource();
                        }
                        fr.getGlyphBoundingBox(gc, size, bounds);
                        float glyphLsb = bounds[X_MIN_INDEX];
                        lsb = Math.min(0, glyphLsb + width);
                        run.setLeftBearing();
                        break lsbdone;
                    }
                }
                width += advance;
            }
            // tabs
            if (glyphCount == 0) {
                width += run.getWidth();
            }
        }

        /* The line rsb is the rsb of the last visual character in the line */
        float rsb = 0;
        width = 0;
        rsbdone:
        for (int i = runs.length - 1; i >= 0 ; i--) {
            TextRun run = runs[i];
            int glyphCount = run.getGlyphCount();
            for (int gi = glyphCount - 1; gi >= 0; gi--) {
                float advance = run.getAdvance(gi);
                /* Skip any trailing zero-width glyphs in the line */
                if (advance != 0) {
                    int gc = run.getGlyphCode(gi);
                    /* Skip any trailing invisible glyphs in the line */
                    if (gc != CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
                        FontResource fr = defaultFontResource;
                        if (fr == null) {
                            TextSpan span = run.getTextSpan();
                            PGFont font = (PGFont)span.getFont();
                            /* No need to check font != null (run.glyphCount > 0)  */
                            size = font.getSize();
                            fr = font.getFontResource();
                        }
                        fr.getGlyphBoundingBox(gc, size, bounds);
                        float glyphRsb = bounds[X_MAX_INDEX] - advance;
                        rsb = Math.max(0, glyphRsb - width);
                        run.setRightBearing();
                        break rsbdone;
                    }
                }
                width += advance;
            }
            // tabs
            if (glyphCount == 0) {
                width += run.getWidth();
            }
        }
        line.setSideBearings(lsb, rsb);
    }
}
