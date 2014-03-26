/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.PrinterGraphics;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.paint.Paint;
import com.sun.prism.shape.ShapeRep;
import static com.sun.prism.shape.ShapeRep.InvalidationType.LOCATION_AND_GEOMETRY;

/**
 */
public abstract class NGShape extends NGNode {
    public enum Mode { EMPTY, FILL, STROKE, STROKE_FILL }

    /**
     * We cache a representation of this shape into an image if we are
     * rendering the shape with a 3D transform. We attempt to keep this
     * cached image from render to render, and invalidate it if
     * this NGShape changes either in geometry or visuals.
     */
    private RTTexture cached3D;
    protected Paint fillPaint;
    protected Paint drawPaint;
    protected BasicStroke drawStroke;
    protected Mode mode = Mode.FILL;
    protected ShapeRep shapeRep;

    public void setMode(Mode mode) {
        if (mode != this.mode) {
            this.mode = mode;
            geometryChanged();
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setAntialiased(boolean aa) {
        // We don't support aliased shapes at this time
    }

    public void setFillPaint(Object fillPaint) {
        if (fillPaint != this.fillPaint || 
                (this.fillPaint != null && this.fillPaint.isMutable())) 
        {           
            this.fillPaint = (Paint) fillPaint;
            visualsChanged();
            invalidateOpaqueRegion();
        }
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setDrawPaint(Object drawPaint) {
        if (drawPaint != this.drawPaint || 
                (this.drawPaint != null && this.drawPaint.isMutable())) 
        {
            this.drawPaint = (Paint) drawPaint;
            visualsChanged();
        }
    }

    public void setDrawStroke(BasicStroke drawStroke) {
        if (this.drawStroke != drawStroke) {
            this.drawStroke = drawStroke;
            geometryChanged();
        }
    }

    public void setDrawStroke(float strokeWidth,
                              StrokeType strokeType,
                              StrokeLineCap lineCap, StrokeLineJoin lineJoin,
                              float strokeMiterLimit,
                              float[] strokeDashArray, float strokeDashOffset)
    {
        int type;
        if (strokeType == StrokeType.CENTERED) {
            type = BasicStroke.TYPE_CENTERED;
        } else if (strokeType == StrokeType.INSIDE) {
            type = BasicStroke.TYPE_INNER;
        } else {
            type = BasicStroke.TYPE_OUTER;
        }

        int cap;
        if (lineCap == StrokeLineCap.BUTT) {
            cap = BasicStroke.CAP_BUTT;
        } else if (lineCap == StrokeLineCap.SQUARE) {
            cap = BasicStroke.CAP_SQUARE;
        } else {
            cap = BasicStroke.CAP_ROUND;
        }

        int join;
        if (lineJoin == StrokeLineJoin.BEVEL) {
            join = BasicStroke.JOIN_BEVEL;
        } else if (lineJoin == StrokeLineJoin.MITER) {
            join = BasicStroke.JOIN_MITER;
        } else {
            join = BasicStroke.JOIN_ROUND;
        }

        if (drawStroke == null) {
            drawStroke = new BasicStroke(type, strokeWidth, cap, join, strokeMiterLimit);
        } else {
            drawStroke.set(type, strokeWidth, cap, join, strokeMiterLimit);
        }
        if (strokeDashArray.length > 0) {
            drawStroke.set(strokeDashArray, strokeDashOffset);
        } else {
            drawStroke.set((float[])null, 0f);
        }

        geometryChanged();
    }

    public abstract Shape getShape();

    protected ShapeRep createShapeRep(Graphics g) {
        return g.getResourceFactory().createPathRep();
    }

    @Override
    protected void visualsChanged() {
        super.visualsChanged();
        // If there is a cached image, we have to forget about it
        // and regenerate it when we paint if needs3D
        if (cached3D != null) {
            cached3D.dispose();
            cached3D = null;
        }
    }

    @Override
    protected void renderContent(Graphics g) {
        if (mode == Mode.EMPTY) {
            return;
        }

        // Need to know whether we are being asked to print or not
        final boolean printing = g instanceof PrinterGraphics;

        // If a 3D transform is being used, then we're going to render to
        // an intermediate texture before we then do the final render operation.
        final boolean needs3D = !g.getTransformNoClone().is2D();

        // If there is already a cached image, then we need to check that
        // the surface is not lost, and that we haven't switched from a 3D
        // rendering situation to a 2D one. In either case we need to throw
        // away this cached image and build up a new one.
        if (cached3D != null) {
            cached3D.lock();
            if (cached3D.isSurfaceLost() || !needs3D) {
                cached3D.unlock();
                cached3D.dispose();
                cached3D = null;
            }
        }

        if (needs3D) {
            // For rendering the shape in 3D, we need to first render to the cached
            // image, and then render that image in 3D
            if (cached3D == null) {
                final BaseTransform tx = g.getTransformNoClone();
                final double scaleX = Math.hypot(tx.getMxx(), tx.getMyx());
                final double scaleY = Math.hypot(tx.getMxy(), tx.getMyy());
                final int w = (int) Math.ceil(contentBounds.getWidth() * scaleX);
                final int h = (int) Math.ceil(contentBounds.getHeight() * scaleY);
                // Nothing to do if the scaled bounds is 0 in either dimension;
                // attempting to allocate a texture would fail so we just return
                if (w <= 0 || h <= 0) {
                    return;
                }
                cached3D = g.getResourceFactory().createRTTexture(w, h,
                        Texture.WrapMode.CLAMP_TO_ZERO,
                        false);
                cached3D.contentsUseful();
                final Graphics textureGraphics = cached3D.createGraphics();
                // Have to move the origin such that when rendering to x=0, we actually end up rendering
                // at x=bounds.getMinX(). Otherwise anything rendered to the left of the origin would be lost
                textureGraphics.scale((float) scaleX, (float) scaleY);
                textureGraphics.translate(-contentBounds.getMinX(), -contentBounds.getMinY());
                renderContent2D(textureGraphics, printing);
            }
            // Now render the cached image in 3D
            g.drawTexture(cached3D,
                    contentBounds.getMinX(), contentBounds.getMinY(),
                    contentBounds.getMaxX(), contentBounds.getMaxY(),
                    cached3D.getContentX(), cached3D.getContentY(),
                    cached3D.getContentX() + cached3D.getContentWidth(),
                    cached3D.getContentY() + cached3D.getContentHeight());
            cached3D.unlock();
        } else {
            // Just render in 2D like normal
            renderContent2D(g, printing);
        }
    }

    /**
     * Renders the content as though it is 2D in all cases. In the case that a 3D
     * transform is in use at the time an NGShape is rendered, it will render as 2D
     * into a texture and then transform in 3D that texture.
     *
     * @param g The graphics object to render with
     */
    protected void renderContent2D(Graphics g, boolean printing) {
        ShapeRep localShapeRep = printing ? null : this.shapeRep;
        if (localShapeRep == null) {
            localShapeRep = createShapeRep(g);
        }
        Shape shape = getShape();
        if (mode != Mode.STROKE) {
            g.setPaint(fillPaint);
            localShapeRep.fill(g, shape, contentBounds);
        }
        if (mode != Mode.FILL && drawStroke.getLineWidth() > 0) {
            g.setPaint(drawPaint);
            g.setStroke(drawStroke);
            localShapeRep.draw(g, shape, contentBounds);
        }

        if (!printing) {
            this.shapeRep = localShapeRep;
        }
    }

    @Override
    protected boolean hasOverlappingContents() {
        return mode == Mode.STROKE_FILL;
    }

    protected Shape getStrokeShape() {
        return drawStroke.createStrokedShape(getShape());
    }

    @Override
    protected void geometryChanged() {
        // TODO: consider caching the stroke shape (RT-26940)
        super.geometryChanged();
        if (shapeRep != null) {
            shapeRep.invalidate(LOCATION_AND_GEOMETRY);
        }
        // If there is a cached image, we have to forget about it
        // and regenerate it when we paint if needs3D
        if (cached3D != null) {
            cached3D.dispose();
            cached3D = null;
        }
    }

    @Override
    protected boolean hasOpaqueRegion() {
        final Mode mode = getMode();
        final Paint fillPaint = getFillPaint();
        return super.hasOpaqueRegion() &&
                    (mode == Mode.FILL || mode == Mode.STROKE_FILL) &&
                    (fillPaint != null && fillPaint.isOpaque());
    }
}
