/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.text.TextRun;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;

public class NGText extends NGShape {

    static final BaseTransform IDENT = BaseTransform.IDENTITY_TRANSFORM;

    public NGText() {
    }

    private GlyphList[] runs;
    public void setGlyphs(Object[] glyphs) {
        this.runs = (GlyphList[])glyphs;
        geometryChanged();
    }

    private float layoutX, layoutY;
    public void setLayoutLocation(float x, float y) {
        layoutX = x;
        layoutY = y;
        geometryChanged();
    }

    private PGFont font;
    public void setFont(Object font) {
        if (font != null && font.equals(this.font)) {
            return;
        }
        this.font = (PGFont)font;
        this.fontStrike = null;
        this.identityStrike = null;
        geometryChanged();
    }

    private int fontSmoothingType;
    public void setFontSmoothingType(int fontSmoothingType) {
        this.fontSmoothingType = fontSmoothingType;
        geometryChanged();
    }

    private boolean underline;
    public void setUnderline(boolean underline) {
        this.underline = underline;
        geometryChanged();
    }

    private boolean strikethrough;
    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        geometryChanged();
    }

    private Object selectionPaint;
    private int selectionStart;
    private int selectionEnd;
    public void setSelection(int start, int end, Object color) {
        selectionPaint = color;
        selectionStart = start;
        selectionEnd = end;
        geometryChanged();
    }

    /**
     * Provide some lucky padding in the case that we are rendering LCD
     * text since there might be some pixels that lie outside the normally
     * computed content bounds.
     */
    @Override protected BaseBounds computePadding(BaseBounds region) {
        float pad = fontSmoothingType == FontResource.AA_LCD ? 2f : 1f;
        return region.deriveWithNewBounds(region.getMinX() - pad,
                                          region.getMinY() - pad,
                                          region.getMinZ(),
                                          region.getMaxX() + pad,
                                          region.getMaxY() + pad,
                                          region.getMaxZ());
    }

    private static double EPSILON = 0.01;
    private FontStrike fontStrike = null;
    private FontStrike identityStrike = null;
    private double[] strikeMat = new double[4];
    private FontStrike getStrike(BaseTransform xform) {
        int smoothingType = fontSmoothingType;
        if (getMode() == Mode.STROKE_FILL) {
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

    @Override public Shape getShape() {
        if (runs == null) {
            return new Path2D();
        }
        FontStrike strike = getStrike(IDENT);
        Path2D outline = new Path2D();
        for (int i = 0; i < runs.length; i++) {
            GlyphList run = runs[i];
            Point2D pt = run.getLocation();
            float x = pt.x - layoutX;
            float y = pt.y - layoutY;
            BaseTransform t = BaseTransform.getTranslateInstance(x, y);
            outline.append(strike.getOutline(run, t), false);
            Metrics metrics = null;
            if (underline) {
                metrics = strike.getMetrics();
                RoundRectangle2D rect = new RoundRectangle2D();
                rect.x = x;
                rect.y = y + metrics.getUnderLineOffset();
                rect.width = run.getWidth();
                rect.height = metrics.getUnderLineThickness();
                outline.append(rect, false);
            }
            if (strikethrough) {
                if (metrics == null) {
                    metrics = strike.getMetrics();
                }
                RoundRectangle2D rect = new RoundRectangle2D();
                rect.x = x;
                rect.y = y + metrics.getStrikethroughOffset();
                rect.width = run.getWidth();
                rect.height = metrics.getStrikethroughThickness();
                outline.append(rect, false);
            }
        }
        return outline;
    }

    private boolean drawingEffect = false;
    @Override protected void renderEffect(Graphics g) {
        /* Text as pre-composed image glyphs must be rendered in
         * device space because otherwise pixelisation effects are
         * very apparent.
         * The Effects implementation seems to indicate that it applies
         * effects in a space with the transforms already applied :
         * ie PrEffectHelper.effect() says for at least a 2D TX :
         *  // process the effect using the current 2D transform, and then
         *  // render the resulting image in device space (i.e., with identity)
         * However its apparent that (eg) a rotation is applied twice to
         * shadow text. As if drawing the "non-shadow" text over the image
         * with the shadow text overlooks that this is in effect
         * applying that rotation again. However I don't think its quite
         * that simple. Also the shadow text is cut-off as if it was
         * clipped to the original unrotated coordinates.
         * To work around this if an effect is detected, we will render
         * as shapes since they are provided in user space.
         * This is probably a reasonable compromise.
         * However if no transform is detected we can try to use the normal
         * image drawing path. If that's causes problems this test can
         * be removed.
         */
        if (!g.getTransformNoClone().isTranslateOrIdentity()) {
            drawingEffect = true;
        }
        try {
            super.renderEffect(g);
        } finally {
            drawingEffect = false;
        }
    }

    private static int FILL        = 1 << 1;
    private static int SHAPE_FILL  = 1 << 2;
    private static int TEXT        = 1 << 3;
    private static int DECORATION  = 1 << 4;
    @Override protected void renderContent2D(Graphics g, boolean printing) {
        if (mode == Mode.EMPTY) return;
        if (runs == null || runs.length == 0) return;

        BaseTransform tx = g.getTransformNoClone();
        FontStrike strike = getStrike(tx);

        if (strike.getAAMode() == FontResource.AA_LCD ||
                (fillPaint != null && fillPaint.isProportional()) ||
                (drawPaint != null && drawPaint.isProportional()))
        {
            /*
             * This check is only a performance optimization, to prevent
             * unnecessarily computing bounds. It's a quickly cautious estimate
             * if we might need to setNodeBounds, graphics does practically no
             * extra work by setting node bounds.  But it's much faster to
             * setNodeBounds for LCD text rendering and is required for correct
             * proportional gradient.
             */
            BaseBounds bds = getContentBounds(new RectBounds(), IDENT);
            g.setNodeBounds((RectBounds)bds);
        }

        Color selectionColor = null;
        if (selectionStart != selectionEnd && selectionPaint instanceof Color) {
            selectionColor = (Color)selectionPaint;
        }

        BaseBounds clipBds = null;
        if (getClipNode() != null) {
            // Note: this clip does not including any clip in the ancestors.
            clipBds = getClippedBounds(new RectBounds(), IDENT);
        }

        // FILL or STROKE_FILL
        if (mode != Mode.STROKE) {
            g.setPaint(fillPaint);
            int op = TEXT;
            op |= strike.drawAsShapes() || drawingEffect ? SHAPE_FILL : FILL;
            renderText(g, strike, clipBds, selectionColor, op);

            // Splitting decoration from text rendering is important in order
            // to group common render states together, for fast performance.
            if (underline || strikethrough) {
                op = DECORATION | SHAPE_FILL;
                renderText(g, strike, clipBds, selectionColor, op);
            }
        }

        // STROKE or STROKE_FILL
        if (mode != Mode.FILL) {
            g.setPaint(drawPaint);
            g.setStroke(drawStroke);
            int op = TEXT;
            if (underline || strikethrough) {
                op |= DECORATION;
            }
            renderText(g, strike, clipBds, selectionColor, op);
        }
        g.setNodeBounds(null);
    }

    private void renderText(Graphics g, FontStrike strike, BaseBounds clipBds,
                            Color selectionColor, int op) {
        for (int i = 0; i < runs.length; i++) {
            TextRun run = (TextRun)runs[i];
            RectBounds lineBounds = run.getLineBounds();
            Point2D pt = run.getLocation();
            float x = pt.x - layoutX;
            float y = pt.y - layoutY;
            if (clipBds != null) {
                if (y > clipBds.getMaxY()) break;
                if (y + lineBounds.getHeight() < clipBds.getMinY()) continue;
                if (x > clipBds.getMaxX()) continue;
                if (x + run.getWidth() < clipBds.getMinX()) continue;
            }
            y -= lineBounds.getMinY();

            if ((op & TEXT) != 0 && run.getGlyphCount() > 0) {
                if ((op & FILL) != 0) {
                    int start = run.getStart();
                    g.drawString(run, strike, x, y,
                                 selectionColor,
                                 selectionStart - start,
                                 selectionEnd - start);
                } else {
                    BaseTransform t = BaseTransform.getTranslateInstance(x, y);
                    if ((op & SHAPE_FILL) != 0) {
                        g.fill(strike.getOutline(run, t));
                    } else {
                        g.draw(strike.getOutline(run, t));
                    }
                }

            }
            if ((op & DECORATION) != 0) {
                Metrics metrics = strike.getMetrics();
                if (underline) {
                    float offset = y + metrics.getUnderLineOffset();
                    float thickness = metrics.getUnderLineThickness();
                    if ((op & SHAPE_FILL) != 0) {
                        if (thickness <= 1f && g.getTransformNoClone().isTranslateOrIdentity()) {
                            float myt = (float)g.getTransformNoClone().getMyt();
                            offset = Math.round(offset + myt) - myt;
                        }
                        g.fillRect(x, offset, run.getWidth(), thickness);
                    } else {
                        g.drawRect(x, offset, run.getWidth(), thickness);
                    }
                }
                if (strikethrough) {
                    float offset = y + metrics.getStrikethroughOffset();
                    float thickness = metrics.getStrikethroughThickness();
                    if ((op & SHAPE_FILL) != 0) {
                        if (thickness <= 1f && g.getTransformNoClone().isTranslateOrIdentity()) {
                            float myt = (float)g.getTransformNoClone().getMyt();
                            offset = Math.round(offset + myt)  - myt;
                        }
                        g.fillRect(x, offset, run.getWidth(), thickness);
                    } else {
                        g.drawRect(x, offset, run.getWidth(), thickness);
                    }
                }
            }
        }
    }
}
