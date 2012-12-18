/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg.prism;

import javafx.scene.text.Font;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.TransformedShape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.sg.PGShape.Mode;
import com.sun.javafx.sg.PGShape.StrokeLineCap;
import com.sun.javafx.sg.PGShape.StrokeLineJoin;
import com.sun.javafx.sg.PGShape.StrokeType;
import com.sun.javafx.sg.PGTextHelper;
import com.sun.javafx.text.LayoutCache;
import com.sun.javafx.text.TextLayout;
import com.sun.javafx.text.TextRun;
import com.sun.javafx.text.TextLine;
import com.sun.prism.BasicStroke;
import com.sun.prism.paint.Paint;
import static com.sun.javafx.text.TextLayout.ALIGN_LEFT;
import static com.sun.javafx.text.TextLayout.FLAGS_LINES_VALID;
import static com.sun.javafx.text.TextLayout.FLAGS_WRAPPED;;

public class NGTextHelper implements PGTextHelper {

    // Vertical alignment values correspond to VPos enum
    private static final int TOP      = 0;
    private static final int CENTER   = 1;
    private static final int BASELINE = 2;
    private static final int BOTTOM   = 3;

    static final BaseTransform
        IDENT = BaseTransform.IDENTITY_TRANSFORM;

    public static final boolean hinting = false; //TODO add implementation (RT-26937)

    private float x;
    private float y;
    private boolean locationIsChanged;

    private static final PGFont defaultFont =
            (PGFont) Font.getDefault().impl_getNativeFont();
    private PGFont font = defaultFont;
    private boolean fontChanged; // for syncing.

    /* Since we cache the strike on the node, it will not get
     * collected until the node is collected, but we can change this
     * as needed to say a weak ref, or do some cache management,
     * since all the calls go via getStrike() so we have
     * a single place to re-create as needed.
     */

    private short fontSmoothingType = FontResource.AA_GREYSCALE;

    private FontStrike fontStrike = null;
    private FontStrike identityStrike = null;
    private double[] strikeMat = new double[4];
    private String text = "";
    // setting origin to top is so common that this attribute
    // is stored directly on the node.
    int textOrigin = BASELINE;

    private Selection selection;

    Selection getSelection() { // NGTextHelper
        return selection;
    }

    // Rendering state attributes needed for bounds calculation.
    private BaseTransform cumulativeTransform;
    private Mode mode;
    private boolean doStroke;
    private StrokeType strokeType;
    private BasicStroke drawStroke;
    
    private Shape cachedShape;
    private int textAlignment = ALIGN_LEFT; // Default left aligned
    boolean underline;
    boolean strikethrough;
    private float wrapWidth = 0; //WrapWidth == 0 -> no wrap
    TextLine[] lines;
    private float maxLineWidth = 0f;
    private int flags = 0;

    public NGTextHelper() {
    }

    public void setCumulativeTransform(BaseTransform tx) {
        cumulativeTransform = tx;
    }

    BaseTransform getCumulativeTransform() {
         if (cumulativeTransform == null) {
           return IDENT;
        } else {
            return cumulativeTransform;
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private Mode getMode() {
        if (mode == null) {
            return Mode.EMPTY;
        } else {
            return mode;
        }
    }

    public void setStroke(boolean stroke) {
        this.doStroke = stroke;
    }

    private boolean hasStroke() {
        return doStroke;
    }

    private StrokeType getStrokeType() {
        return strokeType;
    }

    public void setStrokeParameters(StrokeType strokeType,
                                    float[] strokeDashArray,
                                    float strokeDashOffset,
                                    StrokeLineCap lineCap,
                                    StrokeLineJoin lineJoin,
                                    float strokeMiterLimit,
                                    float strokeWidth) {

        this.strokeType = strokeType;

        drawStroke =
            NGShape.createDrawStroke(strokeWidth,
                                     strokeType,
                                     lineCap,
                                     lineJoin,
                                     strokeMiterLimit,
                                     strokeDashArray,
                                     strokeDashOffset);
        
    }

    private BasicStroke getStroke() {
        return drawStroke;
    }

    /* Need to work on checking "valid" what's really dirty to
     * minimise copying here.
     * TBD: Add dirty bits for all the expensive ones - eg arrays.
     * Suppose that "getShape()" is only requested on the render thread.
     * We will over-write this on the next sync. unless we have a way to
     * know its still valid. This doesn't matter for the properties
     * that are directly set like x, y, textOrigin etc. But will matter for
     * the derived ones.
     */
    public void sync(NGTextHelper slave) {
        // If nothing changed since the last pulse, we do not need to sync.
//         if (!dirty) {
//             return;
//         }
        // These properties are set by the SG, we can always sync these.
        slave.x = x;
        slave.y = y;
        slave.fontSmoothingType = fontSmoothingType;
        slave.font = font;
        slave.text = text;
        slave.textOrigin = textOrigin;
        slave.textBoundsType = textBoundsType;
        if (selection == null) {
            slave.selection = null;
        } else {
            if (slave.selection == null) {
                slave.selection = new Selection();
            }
            slave.selection.start = selection.start;
            slave.selection.end = selection.end;
            slave.selection.drawPaint = selection.drawPaint;
            slave.selection.fillPaint = selection.fillPaint;
            // shape is derived, not set. This syncing is harmless
            // but unneeded as the shape is not currently used by NGText.
            slave.selection.shape = selection.shape;            
        }

        // rendering envt. attributes.
        slave.cumulativeTransform = cumulativeTransform;
        slave.mode = mode;
        slave.doStroke = doStroke;
        slave.strokeType = strokeType;
        slave.drawStroke = drawStroke;

        // calculated or derived attributes.
        slave.locationIsChanged = locationIsChanged;
        slave.geometryIsChanged = geometryIsChanged;
        slave.identityStrike = identityStrike;
        // The strike is also invalid if the font changed.
        // Lookup checks
        if (fontStrike != null || fontChanged) {
            slave.fontStrike = fontStrike;
            System.arraycopy(strikeMat, 0, slave.strikeMat, 0, 4);
        }
        fontChanged = false;
        slave.maxLineWidth = maxLineWidth;
        slave.visualBounds = visualBounds;
        slave.logicalBounds = logicalBounds;
        slave.yAdjCached = yAdjCached;
        slave.yAdjValid = yAdjValid;
        
        slave.cachedShape = cachedShape;
        slave.flags = flags;
        slave.textAlignment = textAlignment;
        slave.underline = underline;
        slave.strikethrough = strikethrough;
        slave.wrapWidth = wrapWidth;
        slave.lines = lines; //deep copy (Immutable objects?)
    }

    // @Override FIX
    public Shape getShape() {
        return getShape(true);
    }

    /**
     * Get text shape including decoration.
     * This function does not transform or render the shape.
     *
     * @param translateShape if true will translate shape to x, y, and correct
     * VPos.
     * @return the shape
     */
    Shape getShape(boolean translateShape) {
        if (text.isEmpty()) {
            return new Path2D();
        }
        validateText();
        if (cachedShape != null) {
            if (translateShape) {
                return translateShape(cachedShape);
            } else {
                return cachedShape;
            }
        }

        Path2D outline = new Path2D();
        BaseTransform tx = IDENT;
        FontStrike strike = getStrike(tx);
        float lineHeight = strike.getMetrics().getLineHeight();
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            TextRun[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            tx = tx.deriveWithTranslation(bounds.getMinX(), 0);
            if (underline) {
                float offset = strike.getUnderLineOffset();
                float thickness = strike.getUnderLineThickness();
                Path2D ul = createRectPath(0, offset, bounds.getWidth(), thickness);
                outline.append(ul.getPathIterator(tx), false);
            }
            if (strikethrough) {
                float offset = strike.getStrikethroughOffset();
                float thickness = strike.getStrikethroughThickness();
                Path2D st = createRectPath(0, offset, bounds.getWidth(), thickness);
                outline.append(st.getPathIterator(tx), false);
            }
            for (int j = 0; j < runs.length; j++) {
                TextRun run = runs[j];
                if (run.getGlyphCount() > 0) {
                    Path2D path = (Path2D)strike.getOutline(run, tx);
                    outline.append(path, false);
                }
                tx = tx.deriveWithTranslation(run.getWidth(), 0);
            }
            tx = tx.deriveWithTranslation(-(bounds.getMaxX()), lineHeight);
        }
        cachedShape = outline;
        if (translateShape) {
            return translateShape(cachedShape);
        } else {
            return cachedShape;
        }
    }

    private Shape translateShape(Shape textShape) {
        float yAdj = getYAdjustment();
        return TransformedShape.translatedShape(textShape, x, y + yAdj);
    }

    /* Create a Rectangle where the path is clockwise, consistent
     * with that used by glyphs.
     */
    private Path2D createRectPath(float x, float y, float w, float h) {
        Path2D p2d = new Path2D();
        p2d.moveTo(x, y);
        p2d.lineTo(x + w, y);
        p2d.lineTo(x + w, y + h);
        p2d.lineTo(x, y + h);
        p2d.lineTo(x, y);
        p2d.closePath();
        return p2d;
    }

    private static final int LOGICAL_BOUNDS = 0;
    private static final int VISUAL_BOUNDS  = 1;
    private static final int LOGICAL_LAYOUT_BOUNDS = 2;

    int textBoundsType = LOGICAL_BOUNDS;
    public void setTextBoundsType(int textBoundsType) {
        if (this.textBoundsType == textBoundsType) {
            return;
        }

        /* Toggling this doesn't affect the rendering, so
         * we don't flush any cached values, and the bounds
         * are not cached on this node, they are cached higher
         * up, so this just toggles a value that affects how
         * bounds are reported when a query is received.
         * REMIND: I'm now caching .. fix the above comment once
         * sure its staying that way.
         * However it can change layout, except when the value
         * is swapping between LOGICAL_BOUNDS and LOGICAL_LAYOUT_BOUNDS.
         * Ideally we'd optimise away that case, but its possible that
         * clients of this class will not re-query boundsInLocal unless
         * we signal that the geometry may have changed.
         */

        boolean layoutChanged =
            textBoundsType == VISUAL_BOUNDS ||
            this.textBoundsType == VISUAL_BOUNDS;
        this.textBoundsType = textBoundsType;
        if (layoutChanged) {
            geometryChanged();
        }
    }

    public final BaseBounds computeLayoutBounds(BaseBounds bounds) {
        if (textBoundsType == VISUAL_BOUNDS) {
            bounds = computeBoundsVisual(bounds, IDENT);
        } else {
            // Even if the text is empty,  for logical bounds we need to
            // return bounds which includes the height of the font.
            bounds = computeBoundsLogical(bounds, IDENT, false);
        }
        return bounds;
    }

    public BaseBounds computeContentBounds(BaseBounds bounds,
                                           BaseTransform tx) {
        if (textBoundsType == VISUAL_BOUNDS) {
            bounds = computeBoundsVisual(bounds, tx);
        } else {
            // Even if the text is empty,  for logical bounds we need to
            // return bounds which includes the height of the font.
            bounds = computeBoundsLogical(bounds, tx, true);
        }
        return bounds;
    }

    /*
     * Cached bounds are identity bounds, and have BASELINE as origin. Caching
     * bounds, helps reduce the number of strikes created during text scaling
     * and rotation, thus reducing memory consumption and increasing 
     * performance.
     *
     * Bounds caching is particularly important for visual bounds, since it
     * requires get shape of content, which is costly.
     *
     * When bounds type is VISUAL then text vertical position (yAdjustment)
     * requires the visual bounds.
     */
    private RectBounds visualBounds = null;
    private RectBounds logicalBounds = null;
    private float yAdjCached = 0f;
    private boolean yAdjValid = false;

    BaseBounds computeBoundsLogical(BaseBounds bounds,
                                    BaseTransform tx,
                                    boolean includeGeom) {
        if (bounds == null) {
            bounds = new RectBounds();
        }

        validateText();

        computeBoundsLogicalIdentity();
        float ty = getYAdjustment() + y;

        RectBounds retBounds = new RectBounds(logicalBounds.getMinX() + x,
                                              logicalBounds.getMinY() + ty,
                                              logicalBounds.getMaxX() + x,
                                              logicalBounds.getMaxY() + ty);
        Mode mode = getMode();
        // if includeGeom is true, then we need to report the pixels
        // we touch as efficiently as possible.
        // If text is empty, then padding is not required
        // If there's a stroke we need to get visual bounds.
        if (includeGeom && !text.isEmpty()) {
        // If glyph pixels exceed bounds add padding, when includeGeom is true.
            if (mode == Mode.FILL) {
                // logicalPadding only adds padding for glyphs that exceed
                // logical bounds.
                logicalHPadding(retBounds);
            } else {
                // Stroked text requires visual bounds, when include padding
                BaseBounds vBounds = computeBoundsVisual(null, IDENT);
                // Union between logical and visual bounds
                retBounds.deriveWithUnion(vBounds);
            }
        }
        return NGShape.getRectShapeBounds(bounds, tx, NGShape.classify(tx),
                                          0, 0, retBounds);
    }

    private RectBounds computeBoundsLogicalIdentity() {
        // If hinting is enabled, then bound caching may be unreliable
        if (logicalBounds != null && !hinting) {
            return logicalBounds;
        }

        FontStrike localStrike = getStrike(IDENT);
        FontStrike.Metrics fm = localStrike.getMetrics();

        float width = wrapWidth > 0 ? wrapWidth : maxLineWidth;
        float ascent = fm.getAscent();
        // adding ascent to height may look odd, but is correct since
        // "height" has the extra ascent in there which needs
        // to be subtracted (remember ascent is a negative value).
        float height = getLineCount() * fm.getLineHeight() + ascent;
        
        logicalBounds = new RectBounds(0, ascent, width, height);
        return logicalBounds;
    }

    private BaseBounds computeBoundsVisual(BaseBounds bounds,
                                           BaseTransform tx) {
        if (bounds == null) {
            bounds = new RectBounds();
        }

        Mode mode = getMode();
        if (mode == Mode.EMPTY || text.isEmpty()) {
            return bounds.makeEmpty();
        }

        // Use cached visual bounds if available
        validateText();

        if (visualBounds == null || hinting) {
            visualBounds = null;
            // Getting the whole shape is expensive, but it would be difficult
            // to do much better. Visual bounds depends on getting all the
            // glyph outlines anwyay. Hopefully this isn't used too much for
            // multi-line text either. If we re-define the visual width
            // to be the same as the wrapping width it would get a whole lot
            // cheaper but if that's what folks want, they should stick to
            // logical bounds (the default).

            // Get untranslated cached shape is required, to prevent
            //  translating twice.
            Shape s = getShape(false);

            if (mode == Mode.FILL ||
                getStrokeType() == StrokeType.INSIDE) {
                // There is no padding required if either:
                //   - When there is no stroke (Mode.FILL)
                //   - Inner Stroke
                visualBounds = s.getBounds();         
                //System.out.println("vbds="+visualBounds);


            } else {
                // accumulateShapeBounds is expensive, but provides the correct
                // shape plus stroke bounds. It might be possible to optimize
                // this in the future, or estimating padding for certain common
                // strokes. The default joint type is miter, and the miter
                // limit is set to 10. Unfortunately miter limits are expensive
                // to compute and unreliable to estimate.
                float[] bbox = new float[4];
                BasicStroke drawStroke = getStroke();
                drawStroke.accumulateShapeBounds(bbox, s, IDENT);
                visualBounds = new RectBounds(bbox[0], bbox[1],
                                              bbox[2], bbox[3]);
                //System.out.println("bds="+visualBounds);
            }
        }

        float ty = y;
        if (textBoundsType == VISUAL_BOUNDS) {
            // Note: getYAdjustment() can not be called directly from here
            // without this precaution. When textBoundsType == VISUAL_BOUNDS,
            // getYAdjustment() can call this method and cause infinite
            // recursion.
            ty += getYAdjustment(visualBounds);
        } else {
            ty += getYAdjustment();
        }
        RectBounds retBounds = new RectBounds(visualBounds.getMinX() + x,
                     visualBounds.getMinY() + ty,
                     visualBounds.getMaxX() + x,
                     visualBounds.getMaxY() + ty);
        return NGShape.getRectShapeBounds(bounds, tx, NGShape.classify(tx),
                                          0, 0, retBounds);
    }

    public void setLocation(float x, float y) {
        if (x != this.x || y != this.y) {
            this.x = x;
            this.y = y;
            locationIsChanged = true;
        }
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    boolean isLocationChanged() {
        return locationIsChanged;
    }

    void resetLocationChanged() {
        locationIsChanged = false;
    }

    /**
     * Provides the vertical adjustment relative to set text origin, for a
     * given bounds.
     * @param bounds is expected to be the "identity bounds", or text bounds
     * which has not been acquired from untranslated text at origin (BASELINE).
     * @return vertical adjustment relative to set text origin
     */
    private float getYAdjustment(final RectBounds bounds) {
        float yAdj = 0.0f;

        switch (textOrigin) {
            case TOP:
                // Y is inverted so TOP becomes ymin.
                yAdj = - bounds.getMinY();
                break;
            case CENTER:
                // vertical center = (top + botton)/2
                yAdj = - ((bounds.getMaxY() + bounds.getMinY()) / 2.0f);
                break;
            case BOTTOM:
                yAdj = - bounds.getMaxY();
                break;
            default: // BASELINE which is default and yAdj = 0.0f;
        }
        yAdjCached = yAdj;
        yAdjValid = true;
        return yAdj;
    }

    /**
     * Provides the vertical adjustment relative to set text origin, taking
     * into account setTextBoundsType.
     * @return vertical adjustment relative to set text origin
     */
    float getYAdjustment() {
        if (textOrigin == BASELINE) { // VPos.BASELINE
            // Fast path, since BASELINE is default
            yAdjCached = 0f;
            yAdjValid = true;
            return yAdjCached;
        } else if (yAdjValid) {
            return yAdjCached;
        }
        if (textBoundsType == VISUAL_BOUNDS) {
            computeBoundsVisual(null, BaseTransform.IDENTITY_TRANSFORM);
            getYAdjustment(visualBounds);
        } else {
            getYAdjustment(computeBoundsLogicalIdentity());
        }

        return yAdjCached;
    }
    
    /* Used by NGCanvas */
    float getLogicalWidth() {
        return wrapWidth > 0 ? wrapWidth : maxLineWidth;
    }

    // Presently only support bounds based picking.
    public boolean computeContains(float localX, float localY) {
        return true;
    }

    public Object getCaretShape(int charIndex, boolean isLeading) {
        validateText();

        int lineIndex = 0;
        int lineCount = getLineCount();
        while (lineIndex < lineCount) {
            TextLine line = lines[lineIndex];
            int lineEnd = line.getStart() + line.getLength();
            if (lineEnd > charIndex) break;
            lineIndex++;
        }
        int sliptCaretOffset = -1;
        int level = 0;
        float lx = x;
        if (lineIndex < lineCount) {
            TextLine line = lines[lineIndex];
            TextRun[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            lx += bounds.getMinX();
            for (int i = 0; i < runs.length; i++) {
                TextRun run = runs[i];
                int runStart = run.getStart();
                int runEnd = run.getEnd();
                if (runStart <= charIndex && charIndex < runEnd) {
                    lx += run.getXAtOffset(charIndex - runStart, isLeading);
                    if (charIndex == runStart && isLeading) {
                        sliptCaretOffset = charIndex - 1;
                    }
                    if (charIndex + 1 == runEnd && !isLeading) {
                        sliptCaretOffset = charIndex + 1;
                    }
                    level = run.getLevel();
                    break;
                }
                lx += run.getWidth();
            }
        } else {
            // charIndex beyond length
            lineIndex = lineCount - 1;
            TextLine line = lines[lineIndex];
            lx += line.getBounds().getMaxX();
        }
        FontStrike strike = getStrike();
        FontStrike.Metrics fm = strike.getMetrics();
        float ly = y + getYAdjustment() + (lineIndex * fm.getLineHeight());
        if (sliptCaretOffset != -1) {
            TextLine line = lines[lineIndex];
            TextRun[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            float lx2 = x + bounds.getMinX();
            for (int i = 0; i < runs.length; i++) {
                TextRun run = runs[i];
                int runStart = run.getStart();
                int runEnd = run.getEnd();
                if (runStart <= sliptCaretOffset && sliptCaretOffset < runEnd) {
                    if ((run.getLevel() & 1) != (level & 1)) {
                        Path2D rv = new Path2D();
                        float top = ly + fm.getAscent();
                        float height = fm.getLineHeight() / 2;
                        rv.append(new Line2D(lx, top, lx, top + height), false);
                        top += height;
                        if (!isLeading) lx2 += run.getWidth();
                        rv.append(new Line2D(lx2, top, lx2, top + height), false);
                        return rv;
                    }
                }
                lx2 += run.getWidth();
            }
        }
        return new Line2D(lx, ly + fm.getAscent(), lx, ly + fm.getDescent());
    }
    
    public Object getHitInfo(float hitx, float hity) {
        validateText();

        FontStrike localStrike = getStrike();
        FontStrike.Metrics fm = localStrike.getMetrics();

        /* The hit coords are in local coordinates but are not relative
         * to the Text node location as set by setLocation().
         * So we need to adjust for that. Since the default behaviour
         * is to interpret the y position as the baseline of the text,
         * we need to account for that too. To make local "y" mean the top
         * of the top line, we need to subtract the adjustment and the
         * ascent.
         */

        float x = (hitx < this.x) ? 0 : (hitx - this.x);
        float y = ((hity < this.y) ? 0 : (hity - this.y))
            - getYAdjustment() - fm.getAscent();
        
        float lineHeight = fm.getLineHeight();
        HitInfo info = new HitInfo();
        if (y >= lineHeight * getLineCount()) {
            info.setCharIndex(text.length());
        } else {
            int lineIndex = (int)(y/lineHeight);
            TextLine line = lines[lineIndex];
            TextRun[] runs = line.getRuns();
            RectBounds bounds = line.getBounds();
            TextRun run = null;
            x -= bounds.getMinX();
            //TODO binary search (RT-26931)
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
                info.setCharIndex(run.getStart() + run.getOffsetAtX(x, trailing));
                info.setLeading(trailing[0] == 0);
            } else {
                info.setCharIndex(line.getStart());
                //TODO leading ? last run direciton or paragraph direction (RT-26931)
            }
        }
        return info;
    }

    public Shape getSelectionShape() {
       if (selection != null) {
            if (selection.shape == null && selection.end > selection.start) {
                selection.shape = getRangeShapeImpl(selection.start,
                                                    selection.end, false);
            }
            return selection.shape;
        }
        return null;
    }

    public Shape getRangeShape(int start, int end) {
        return getRangeShapeImpl(start, end, false);
    }

    public Shape getUnderlineShape(int start, int end) {
        return getRangeShapeImpl(start, end, true);
    }

    private Shape getRangeShapeImpl(int start, int end, boolean underline) {
        Mode mode = getMode();
        if (mode == Mode.EMPTY || text.isEmpty()) {
            // TODO test, for empty text and visual bounds height = font height ? (RT-26931)
            return null;
        }
        validateText();

        FontStrike strike = getStrike();
        FontStrike.Metrics fm = strike.getMetrics();
        float lineHeight = fm.getLineHeight();
        float ty, height;
        if (underline) {
            ty = y + getYAdjustment() + strike.getUnderLineOffset();
            height = strike.getUnderLineThickness();
        } else {
            ty = y + getYAdjustment() + fm.getAscent();
            height = lineHeight;
        }
        int lineCount = getLineCount();
        Path2D rv = new Path2D();
        
        for  (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            TextLine line = lines[lineIndex];
            int lineStart = line.getStart();
            if (lineStart >= end) break;
            int lineEnd = lineStart + line.getLength();
            if (start > lineEnd) continue;
            
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
            float lineX = line.getBounds().getMinX();
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
                    float ry = ty + lineHeight * lineIndex;
                    count -= runCount;

                    /* Merge continuous rectangles */
                    if (runLeft != right) {
                        if (left != -1 && right != -1) {
                            rv.append(new RoundRectangle2D(x + left, ry, right - left, height, 0, 0), false);
                        }
                        left = runLeft;
                        right = runRight;
                    }
                    right = runRight;
                    if (count == 0) {
                        rv.append(new RoundRectangle2D(x + left, ry, right - left, height, 0, 0), false);
                    }
                }
                lineX += runWidth;
                runIndex++;
            }
        }
        return rv;
    }

   // REMIND: this needs to be called when setting attributes on the node
   // such as sub-pixel positioning etc.
    FontStrike getStrike() {
        // In the absence of a Graphics with a Transform, we need to
        // combine the transforms of this node and its parent nodes up
        // to the root of the scene.
        return getStrike(getCumulativeTransform());
    }

    private static double EPSILON = 0.01;
    FontStrike getStrike(BaseTransform xform) {
        if (xform == null) {
            return getStrike();
        }

        short smoothingType = fontSmoothingType;
        Mode mode = getMode();
        if (mode == Mode.STROKE_FILL && hasStroke()) {
            // When there's a stroke, we want the glyph to be unhinted to match
            // the stroke. This currently means it must be grayscale.
            smoothingType = FontResource.AA_GREYSCALE;
        }
        if (xform.isIdentity()) {
            if (identityStrike == null ||
                smoothingType != identityStrike.getAAMode()) {
                identityStrike = font.getStrike(IDENT, smoothingType);
            }
            return identityStrike;
        }
        // REMIND: need to enhance this, to take other rendering attributes.
        if (fontStrike == null ||
            fontStrike.getSize() != font.getSize() ||
             (xform.getMxy() == 0 && strikeMat[1] != 0) ||
             (xform.getMyx() == 0 && strikeMat[2] != 0) ||
            (Math.abs(strikeMat[0] - xform.getMxx()) > EPSILON) ||
            (Math.abs(strikeMat[1] - xform.getMxy()) > EPSILON) ||
            (Math.abs(strikeMat[2] - xform.getMyx()) > EPSILON) ||
            (Math.abs(strikeMat[3] - xform.getMyy()) > EPSILON) ||
            smoothingType != fontStrike.getAAMode())
        {
            fontStrike = font.getStrike(xform, smoothingType);
            strikeMat[0] = xform.getMxx();
            strikeMat[1] = xform.getMxy();
            strikeMat[2] = xform.getMyx();
            strikeMat[3] = xform.getMyy();
        }
        return fontStrike;
    }

    public void setFont(Object font) {
        if (this.font != null) {
            if (this.font.equals(font)) {
                return;
            }
        }
        this.font = (PGFont)font;
        this.identityStrike = null;
        this.fontStrike = null; // lazily re-evaluate.
        this.fontChanged = true;
        geometryChanged();
    }

    public void setFontSmoothingType(int fontSmoothingType) {
        if (this.fontSmoothingType != fontSmoothingType) {
            this.fontSmoothingType = (short)fontSmoothingType;
            geometryChangedTextValid();
        }
    }

    public void setLogicalSelection(int start, int end) {
       if (start < 0 || end < 0 || end <= start || start > text.length()) {
            if (selection != null) {
                selection.shape = null;
                // This filter is required in order to reset selection state,
                // and avoid creating new Selection object with invalid
                // information.
                if (selection.start != selection.end) {
                    selection.start = 0;
                    selection.end = 0;
                    // Repaint scene is required on selection state change
                    geometryChangedTextValid();
                }
            }
            return;
        }
        if (selection == null
                || selection.start != start || selection.end != end) {
            if (selection == null) {
                selection = new Selection();
            }
            selection.start = Math.max(Math.min(start, end), 0);
            selection.end = Math.min(Math.max(start, end), text.length());
            selection.shape = null;
            geometryChangedTextValid();
        }
    }

    public void setSelectionPaint(Object strokePaint, Object fillPaint) {
        if (selection == null) {
            selection = new Selection();
        }
        Paint stroke = (Paint) strokePaint;
        Paint fill = (Paint) fillPaint;
        boolean repaintNeeded = false;
        if (selection.drawPaint == null ||
            !selection.drawPaint.equals(stroke))
        {
            selection.drawPaint = stroke;
            repaintNeeded = true;
        }

        if (selection.fillPaint == null || !selection.fillPaint.equals(fill)) {
            selection.fillPaint = fill;
            repaintNeeded = true;
        }

        if (repaintNeeded) {
            geometryChangedTextValid();
        }
    }

    String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        if (!this.text.equals(text)) {
            this.text = text;
            /* Reset all information known about the text */
            flags = 0;
            geometryChanged();
        }
    }

    void validateText() {
        if ((flags & FLAGS_LINES_VALID) == 0) {
            buildTextLines();
            flags |= FLAGS_LINES_VALID;
        }
    }

    // Avoid building text again if unnecessary
    void geometryChangedTextValid() {
        cachedShape = null;
        if (selection != null) {
            selection.shape = null;
        }
        yAdjValid = false;
        visualBounds = null;
        logicalBounds = null;
        geometryIsChanged = true; // no super class to call, set a flag.
    }

    // @Override FIX
    protected void geometryChanged() {
        flags &= ~FLAGS_LINES_VALID;
        flags &= ~FLAGS_WRAPPED;
        lines = null;
        maxLineWidth = 0;
        geometryChangedTextValid();
    }

    /* The helper needs to be able to inform the NGText when geometry
     * has changed as a result of an update, rather than doing it
     * reqardless, as some attribute changes do not affect geometry,
     * even if they require re-painting.
     * So during a sync, NGText reads this value, invalidates its
     * geometry if needed, and resets the flag.
     */
    private boolean geometryIsChanged = false;
    boolean isGeometryChanged() {
        return geometryIsChanged;
    }

    void resetGeometryChanged() {
        geometryIsChanged = false;
    }

    /**
     * Determine horizontal padding required to include all pixels, which
     * extend beyond origin and max line advance.  Taking into consideration
     * alignment. If glyph exceeds origin or max line advance then bounds will
     * grow accordingly.
     * Remind: Justify alignment is not handled correctly, and requires
     * further consideration.  Justified text is layout is determined at
     * render time.
     *
     * @param bounds is out parameter which must not be null.
     */
    private void logicalHPadding(BaseBounds bounds) {
        float left = bounds.getMinX();
        float right = bounds.getMaxX();
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            RectBounds lineBounds = line.getBounds();
            float lineLeft = this.x + lineBounds.getMinX() + line.getLeftSideBearing();
            if (lineLeft < left) left = lineLeft;
            float lineRight = this.x + lineBounds.getMaxX() + line.getRightSideBearing();
            if (lineRight > right) right = lineRight;
        }
        bounds.deriveWithNewBounds(left,
                                   bounds.getMinY(),
                                   bounds.getMinZ(),
                                   right,
                                   bounds.getMaxY(),
                                   bounds.getMaxZ());
    }

    int getLineCount() {
        validateText();
        return lines.length;
    }
    
    private void buildTextLines() {
        TextLayout layout = new TextLayout();
        layout.setAlignment(textAlignment);
        layout.setWrapWidth(wrapWidth);
        layout.setContent(text, font);
        lines = (TextLine[])layout.getLines();
        maxLineWidth = layout.getBounds().getWidth();
    }

    /*
     * Alignment is only implemented if wrapping width is set to
     * a positive value
     */
    public void setTextAlignment(int textAlignment) {
        if (textAlignment != this.textAlignment) {
            this.textAlignment = textAlignment;
//            geometryChangedTextValid(); //TODO maybe we want this optimize back (RT-26931)
            geometryChanged();
        }
    }

    public void setTextOrigin(int textOrigin) {
        if (textOrigin != this.textOrigin) {
            this.textOrigin = textOrigin;
            geometryChangedTextValid();
        }
    }

    public void setUnderline(boolean underline) {
        if (underline != this.underline) {
            this.underline = underline;
            geometryChangedTextValid();
        }
    }

    public void setStrikethrough(boolean strikethrough) {
        if (strikethrough != this.strikethrough) {
            this.strikethrough = strikethrough;
            geometryChangedTextValid();
        }
    }

    public void setWrappingWidth(float newWidth) {
        if (newWidth <= 0) newWidth = 0;
        if (newWidth != wrapWidth) {
            boolean textValid = false;
            if ((flags & FLAGS_LINES_VALID) != 0 && wrapWidth != 0 && newWidth != 0) {
                if (textAlignment == ALIGN_LEFT) {
                    if (newWidth > wrapWidth) {
                        /* If wrapping width is increasing and there is no 
                         * wrapped lines then the text remains valid.
                         */
                        if ((flags & FLAGS_WRAPPED) == 0) {
                            textValid = true;
                        }
                    } else {
                        /* If wrapping width is decreasing but it is still 
                         * greater than the max line width then the text 
                         * remains valid.
                         */
                        if (newWidth > maxLineWidth) {
                            textValid = true;
                        }
                    }
                }
            }
            wrapWidth = newWidth;
            
            if (textValid) {
                geometryChangedTextValid();
            } else {
                geometryChanged();
            }
        }
    }

    static class Selection {
        boolean isEmpty() {
            return start >= end;
        }
        int start = 0;
        int end = 0;
        Paint drawPaint;
        Paint fillPaint;
        Shape shape;
    }

    private static final String LS = System.getProperty("line.separator");
    public String toString() {
        return "NGTextHelper : " +
            "x="+x+" y="+y+ LS +
            " fontSmoothingType = " + fontSmoothingType + LS +
            " font = " + font + LS +
            " textOrigin = " + textOrigin + LS +
            " fontSmoothingType = " + fontSmoothingType + LS +
            " textBoundsType = " + textBoundsType + LS +
            " locationIsChanged = " + locationIsChanged + LS +
            " geometryIsChanged = " + geometryIsChanged + LS +
            " cumulativeTransform = " + cumulativeTransform + LS +
            " mode = " + mode + LS +
            " doStroke = " + doStroke + LS +
            " fontTxStrike = " + fontStrike + LS +
            " fontIDStrike = " + identityStrike + LS +
            " strikeMat = [" + strikeMat[0] + ", "+ strikeMat[1] + ", " +
            strikeMat[2] + ", " + strikeMat[3] + "] " + LS +
            " geometryIsChanged = " + geometryIsChanged + LS +
            " maxLineAdvance = " + maxLineWidth + LS +
            " visualBounds = " + visualBounds + LS +
            " logicalBounds = " + logicalBounds + LS +
            " yAdjCached = " + yAdjCached +
            " yAdjValid = " + yAdjValid + LS +
            " selection = " +
            ((selection == null) ? " null " : selection.toString());
    }
}
