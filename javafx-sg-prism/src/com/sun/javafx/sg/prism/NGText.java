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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.PGText;
import com.sun.javafx.sg.PGTextHelper;
import com.sun.javafx.text.TextRun;
import com.sun.javafx.text.TextLine;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import static com.sun.javafx.sg.prism.NGTextHelper.IDENT;

public class NGText extends NGShape implements PGText {

    /*
     * There are two TextHelper instances for a Text node.
     * One for use on the FX thread (etc).
     * The other to be used on the Prism thread by this node when rendering.
     * Essentially all state resides on the Helper node the NGText node.
     * The SG Text node updates its copy and that is synced to the NGText
     * node's copy during the pulse.
     * Any work done by the sgTextHelper such as calculation line breaks
     * is copied over to the local helper to avoid re-doing the work.
     */
    NGTextHelper helper = null;       // used by NGText
    NGTextHelper sgTextHelper = null; // used by SG Text node.

    public NGText() {
        helper = new NGTextHelper();
        sgTextHelper = new NGTextHelper();
    }

    public PGTextHelper getTextHelper() {
        return sgTextHelper;
    }

    /**
     * Webview is directly using NGText.
     * So to set properties it still needs to set them on the NGNode
     * This means we need keep the setters it needs and forward
     * to the helper.
     * Please : nobody else call these and don't add any more!
     */
    public void setText(String text) {
        helper.setText(text);
    }

    public void setFont(Object font) {
        helper.setFont(font);
    }

    public void setLocation(float x, float y) {
        helper.setLocation(x, y);
    }
    /* END unapproved webview support */

    public void geometryChanged() {
        super.geometryChanged();
        helper.geometryChangedTextValid();
    }

    public void locationChanged() {
        super.locationChanged();
        helper.geometryChangedTextValid();
    }

    public void updateText() {
        sgTextHelper.sync(helper);
        if (sgTextHelper.isGeometryChanged()) {
            super.geometryChanged();
            helper.resetGeometryChanged();
            sgTextHelper.resetGeometryChanged();
        }
        if (sgTextHelper.isLocationChanged()) {
            super.locationChanged();
            helper.resetLocationChanged();
            sgTextHelper.resetLocationChanged();
        }
    }

    private FontStrike getStrike(BaseTransform xform) {
        return helper.getStrike(xform);
    }

    /**
     * Get text shape including decoration. This function does not transform or
     * render shape.
     *
     * @param translateShape if true will translate shape to x, y, and correct
     * VPos.
     * @return the shape
     */
    private Shape getShape(boolean translateShape) {
        return helper.getShape(translateShape);
    }

    @Override
    public Shape getShape() {
        return getShape(true);
    }

    private void drawDecoration(Graphics g,
                                float x, float y, 
                                float width, float height) {

        RoundRectangle2D rect = new RoundRectangle2D();
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;

        Paint oldPaint = g.getPaint();

        if (mode != Mode.FILL) {
            g.setPaint(drawPaint);
            g.setStroke(drawStroke);
            g.draw(rect);
        }
        if (mode != Mode.STROKE) {
            g.setPaint(fillPaint);
            g.fill(rect);
        }

        // Restore old paint
        if (g.getPaint() != oldPaint) {
            g.setPaint(oldPaint);
        }
    }
    
    private void drawLayout(Graphics g, FontStrike strike, float x, float y) {
        TextLine[] lines = helper.lines;
        NGTextHelper.Selection selection = helper.getSelection();
        Color selectionColor = null;
        int selectionStart = -1;
        int selectionEnd = -1;
        if (selection != null && !selection.isEmpty() &&
            selection.fillPaint != fillPaint &&
            selection.fillPaint instanceof Color) {
            selectionColor = (Color)selection.fillPaint;
            selectionStart = selection.start;
            selectionEnd = selection.end;
        }

        BaseBounds clipBds = null;
        if (getClipNode() != null) {
            clipBds = new RectBounds();
            clipBds = getClippedBounds(clipBds, IDENT);
        }

        float lineY = y;
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            RectBounds bounds = line.getBounds();
            float lineHeight = bounds.getHeight();
            
            // Probably sufficient to use dsc+ldg rather than
            // lineheight so this is an abundance of caution.
            if (clipBds != null) {
                if (lineY - lineHeight > clipBds.getMaxY()) {
                    break;
                }
                if (lineY + lineHeight < clipBds.getMinY()) {
                    lineY += lineHeight;
                    continue;
                }
            }
            
            TextRun[] runs = line.getRuns();
            float lineX = x + bounds.getMinX();
            float lineWidth = bounds.getWidth();
            if (helper.underline) {
                float offset = strike.getUnderLineOffset();
                float thickness = strike.getUnderLineThickness();
                drawDecoration(g, lineX, lineY + offset, lineWidth, thickness);
            }
            if (helper.strikethrough) {
                float offset = strike.getStrikethroughOffset();
                float thickness = strike.getStrikethroughThickness();
                drawDecoration(g, lineX, lineY + offset, lineWidth, thickness);
            }
            for (int j = 0; j < runs.length; j++) {
                TextRun run = runs[j];
                if (clipBds != null) {
                    if (lineX > clipBds.getMaxX()) {
                        break;
                    }
                    if (lineX < clipBds.getMinX()) {
                        lineX += run.getWidth();
                        continue;
                    }
                }
                if (run.getGlyphCount() > 0) {
                    int start = run.getStart();
                    g.drawString(run, strike, lineX, lineY,
                                 selectionColor,
                                 selectionStart - start,
                                 selectionEnd - start);
                }
                lineX += run.getWidth();
            }
            lineY += lineHeight;
        }
    }

    private boolean drawingEffect = false;
    @Override
    protected void renderEffect(Graphics g) {
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

    @Override
    protected void renderContent(Graphics g) {
        if (mode == Mode.EMPTY) return;
        String text = helper.getText();
        if (text.isEmpty()) return;

        helper.validateText();

        // Render this text node as geometry if it is 3D transformed
        if (!g.getTransformNoClone().is2D()) {
            super.renderContent(g);
            return;
        }
        
        FontStrike localStrike = getStrike(g.getTransformNoClone());
        if (localStrike.getAAMode() == FontResource.AA_LCD ||
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
            BaseBounds bds = helper.computeBoundsLogical(null, IDENT, true);
            g.setNodeBounds((RectBounds)bds);
        }
        if (mode != Mode.STROKE &&
            !localStrike.drawAsShapes() && !drawingEffect) {
            
            g.setPaint(fillPaint);
            float ty = helper.getY() + helper.getYAdjustment();
            drawLayout(g, localStrike, helper.getX(), ty);
            
        }
        /* If its FILL or FILL_STROKE, and we have one or other of these
         * other conditions, then instead use this path for painting.
         */
        else if (mode != Mode.STROKE &&
                 (localStrike.drawAsShapes() || drawingEffect)) {
            g.setPaint(fillPaint);
            g.fill(getShape(true));
        }
        /* Finally, if there's a STROKE, ie if the mode is either STROKE
         * or STROKE_FILL, we need to stroke.
         */
        if (mode != Mode.FILL) {
            g.setPaint(drawPaint);
            g.setStroke(drawStroke);
            // REMIND: take clipBds into account in getting the shape to render
            g.draw(getShape(true));
        }
        g.setNodeBounds(null);
    }
}
