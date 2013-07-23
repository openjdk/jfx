/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.RectangularShape;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.PrinterGraphics;
import com.sun.prism.paint.Paint;
import com.sun.prism.shape.ShapeRep;
import static com.sun.prism.shape.ShapeRep.InvalidationType.LOCATION;
import static com.sun.prism.shape.ShapeRep.InvalidationType.LOCATION_AND_GEOMETRY;

/**
 */
public abstract class NGShape extends NGNode {
    public enum Mode { EMPTY, FILL, STROKE, STROKE_FILL }

    protected Paint fillPaint;
    protected Paint drawPaint;
    protected BasicStroke drawStroke;
    protected Mode mode = Mode.FILL;
    protected ShapeRep shapeRep;
    //protected boolean antialiased;

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
        // TODO: for now everything will be antialiased (RT-26939)
        //if (aa != this.antialiased) {
        //    this.antialiased = aa;
        //    invalidateStrokeShape();
        //}
    }

    public void setFillPaint(Object fillPaint) {
        if (fillPaint != this.fillPaint || 
                (this.fillPaint != null && this.fillPaint.isMutable())) 
        {           
            this.fillPaint = (Paint) fillPaint;
            visualsChanged();
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

    public static BasicStroke
        createDrawStroke(float strokeWidth,
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

        BasicStroke stroke =
            new BasicStroke(type, strokeWidth, cap, join, strokeMiterLimit);

        if (strokeDashArray.length > 0) {
            stroke.set(strokeDashArray, strokeDashOffset);
        } else {
            stroke.set((float[])null, 0f);
        }
        return stroke;
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

    protected ShapeRep createShapeRep(Graphics g, boolean needs3D) {
        return g.getResourceFactory().createPathRep(needs3D);
    }

    @Override
    protected void renderContent(Graphics g) {
        if (mode == Mode.EMPTY) {
            return;
        }
        boolean needs3D = !g.getTransformNoClone().is2D();
        ShapeRep localShapeRep =
            (g instanceof PrinterGraphics ? null : this.shapeRep);
        if (localShapeRep == null) {
            localShapeRep = createShapeRep(g, needs3D);
        } else if (needs3D && !localShapeRep.is3DCapable()) {
            // TODO: for now we only upgrade from 2D to 3D, i.e., we don't
            // currently go back from a 3D rep to a 2D one... (RT-26983)
            localShapeRep.dispose();
            localShapeRep = createShapeRep(g, true);
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
        if (!(g instanceof PrinterGraphics)) {
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
    }

    void locationChanged() {
        super.geometryChanged();
        if (shapeRep != null) {
            shapeRep.invalidate(LOCATION);
        }
    }
    
    static BaseBounds getRectShapeBounds(BaseBounds bounds,
                                       BaseTransform at, int atclass,
                                       float upad, float dpad,
                                       RectBounds r)
    {
        if (bounds == null) {
            bounds = new RectBounds();
        }
        float x1 = r.getWidth();
        float y1 = r.getHeight();
        if (x1 < 0 || y1 < 0) {
            return bounds.makeEmpty();
        }
        float x0 = r.getMinX();
        float y0 = r.getMinY();
        if (atclass <= AT_TRANS) {
            x1 += x0;
            y1 += y0;
            if (atclass == AT_TRANS) {
                float tx = (float)at.getMxt();
                float ty = (float)at.getMyt();
                x0 += tx;
                y0 += ty;
                x1 += tx;
                y1 += ty;
            }
            // TODO - only pad by upad or dpad, depending on transform (RT-26938)
            dpad += upad;
        } else {
            // TODO - only pad by upad or dpad, depending on transform (RT-26938)
            x0 -= upad;
            y0 -= upad;
            x1 += upad*2;
            y1 += upad*2;
            // Each corner is transformed by an equation similar to:
            //     x' = x * mxx + y * mxy + mxt
            //     y' = x * myx + y * myy + myt
            // Since all of the corners are translated by mxt,myt we
            // can ignore them when doing the min/max calculations
            // and add them in once when we are done.  We then have
            // to do min/max operations on 4 points defined as:
            //     x' = x * mxx + y * mxy
            //     y' = x * myx + y * myy
            // Furthermore, the four corners that we will be transforming
            // are not four independent coordinates, they are in a
            // rectangular formation.  To that end, if we translated
            // the transform to x,y and scaled it by width,height then
            // we could compute the min/max of the unit rectangle 0,0,1x1.
            // The transform would then be adjusted as follows:
            // First, the translation to x,y only affects the mxt,myt
            // components of the transform which we can hold off on adding
            // until we are done with the min/max.  The adjusted translation
            // components would be:
            //     mxt' = x * mxx + y * mxy + mxt
            //     myt' = x * myx + y * myy + myt
            // Second, the scale affects the components as follows:
            //     mxx' = mxx * width
            //     mxy' = mxy * height
            //     myx' = myx * width
            //     myy' = myy * height
            // The min/max of that rectangle then degenerates to:
            //     x00' = 0 * mxx' + 0 * mxy' = 0
            //     y00' = 0 * myx' + 0 * myy' = 0
            //     x01' = 0 * mxx' + 1 * mxy' = mxy'
            //     y01' = 0 * myx' + 1 * myy' = myy'
            //     x10' = 1 * mxx' + 0 * mxy' = mxx'
            //     y10' = 1 * myx' + 0 * myy' = myx'
            //     x11' = 1 * mxx' + 1 * mxy' = mxx' + mxy'
            //     y11' = 1 * myx' + 1 * myy' = myx' + myy'
            float mxx = (float) at.getMxx();
            float mxy = (float) at.getMxy();
            float myx = (float) at.getMyx();
            float myy = (float) at.getMyy();
            // Computed translated translation components
            float mxt = x0 * mxx + y0 * mxy + (float) at.getMxt();
            float myt = x0 * myx + y0 * myy + (float) at.getMyt();
            // Scale non-translation components by w/h
            mxx *= x1;
            mxy *= y1;
            myx *= x1;
            myy *= y1;
            x0 = (float) ((Math.min(Math.min(0,mxx),Math.min(mxy,mxx+mxy)))+mxt);
            y0 = (float) ((Math.min(Math.min(0,myx),Math.min(myy,myx+myy)))+myt);
            x1 = (float) ((Math.max(Math.max(0,mxx),Math.max(mxy,mxx+mxy)))+mxt);
            y1 = (float) ((Math.max(Math.max(0,myx),Math.max(myy,myx+myy)))+myt);
        }
        x0 -= dpad;
        y0 -= dpad;
        x1 += dpad;
        y1 += dpad;
        return bounds.deriveWithNewBounds(x0, y0, 0, x1, y1, 0);
    }

    static BaseBounds getRectShapeBounds(BaseBounds bounds,
                                       BaseTransform at, int atclass,
                                       float upad, float dpad,
                                       RectangularShape r)
    {
        if (bounds == null) {
            bounds = new RectBounds();
        }
        float x1 = r.getWidth();
        float y1 = r.getHeight();
        if (x1 < 0 || y1 < 0) {
            return bounds.makeEmpty();
        }
        float x0 = r.getX();
        float y0 = r.getY();
        if (atclass <= AT_TRANS) {
            x1 += x0;
            y1 += y0;
            if (atclass == AT_TRANS) {
                float tx = (float)at.getMxt();
                float ty = (float)at.getMyt();
                x0 += tx;
                y0 += ty;
                x1 += tx;
                y1 += ty;
            }
            // TODO - only pad by upad or dpad, depending on transform (RT-26938)
            dpad += upad;
        } else {
            // TODO - only pad by upad or dpad, depending on transform (RT-26938)
            x0 -= upad;
            y0 -= upad;
            x1 += upad*2;
            y1 += upad*2;
            // Each corner is transformed by an equation similar to:
            //     x' = x * mxx + y * mxy + mxt
            //     y' = x * myx + y * myy + myt
            // Since all of the corners are translated by mxt,myt we
            // can ignore them when doing the min/max calculations
            // and add them in once when we are done.  We then have
            // to do min/max operations on 4 points defined as:
            //     x' = x * mxx + y * mxy
            //     y' = x * myx + y * myy
            // Furthermore, the four corners that we will be transforming
            // are not four independent coordinates, they are in a
            // rectangular formation.  To that end, if we translated
            // the transform to x,y and scaled it by width,height then
            // we could compute the min/max of the unit rectangle 0,0,1x1.
            // The transform would then be adjusted as follows:
            // First, the translation to x,y only affects the mxt,myt
            // components of the transform which we can hold off on adding
            // until we are done with the min/max.  The adjusted translation
            // components would be:
            //     mxt' = x * mxx + y * mxy + mxt
            //     myt' = x * myx + y * myy + myt
            // Second, the scale affects the components as follows:
            //     mxx' = mxx * width
            //     mxy' = mxy * height
            //     myx' = myx * width
            //     myy' = myy * height
            // The min/max of that rectangle then degenerates to:
            //     x00' = 0 * mxx' + 0 * mxy' = 0
            //     y00' = 0 * myx' + 0 * myy' = 0
            //     x01' = 0 * mxx' + 1 * mxy' = mxy'
            //     y01' = 0 * myx' + 1 * myy' = myy'
            //     x10' = 1 * mxx' + 0 * mxy' = mxx'
            //     y10' = 1 * myx' + 0 * myy' = myx'
            //     x11' = 1 * mxx' + 1 * mxy' = mxx' + mxy'
            //     y11' = 1 * myx' + 1 * myy' = myx' + myy'
            float mxx = (float) at.getMxx();
            float mxy = (float) at.getMxy();
            float myx = (float) at.getMyx();
            float myy = (float) at.getMyy();
            // Computed translated translation components
            float mxt = x0 * mxx + y0 * mxy + (float) at.getMxt();
            float myt = x0 * myx + y0 * myy + (float) at.getMyt();
            // Scale non-translation components by w/h
            mxx *= x1;
            mxy *= y1;
            myx *= x1;
            myy *= y1;
            x0 = (float) ((Math.min(Math.min(0,mxx),Math.min(mxy,mxx+mxy)))+mxt);
            y0 = (float) ((Math.min(Math.min(0,myx),Math.min(myy,myx+myy)))+myt);
            x1 = (float) ((Math.max(Math.max(0,mxx),Math.max(mxy,mxx+mxy)))+mxt);
            y1 = (float) ((Math.max(Math.max(0,myx),Math.max(myy,myx+myy)))+myt);
        }
        x0 -= dpad;
        y0 -= dpad;
        x1 += dpad;
        y1 += dpad;
        return bounds.deriveWithNewBounds(x0, y0, 0, x1, y1, 0);
    }

    private static final int coordsPerSeg[] = { 2, 2, 4, 6, 0 };
    private static void accumulate(float bbox[], Shape s, BaseTransform at) {
        if (at == null || at.isIdentity()) {
            // The shape itself will often have a more optimal algorithm
            // to calculate the untransformed bounds...
            RectBounds r2d = s.getBounds();
            if (bbox[0] > r2d.getMinX()) bbox[0] = r2d.getMinX();
            if (bbox[1] > r2d.getMinY()) bbox[1] = r2d.getMinY();
            if (bbox[2] < r2d.getMaxX()) bbox[2] = r2d.getMaxX();
            if (bbox[3] < r2d.getMaxY()) bbox[3] = r2d.getMaxY();
            return;
        }
        PathIterator pi = s.getPathIterator(at);
        float coords[] = new float[6];
        while (!pi.isDone()) {
            int numcoords = coordsPerSeg[pi.currentSegment(coords)];
            for (int i = 0; i < numcoords; i++) {
                float v = coords[i];
                int off = (i & 1); // 0 for X, 1 for Y coords
                if (bbox[off+0] > v) bbox[off+0] = v;
                if (bbox[off+2] < v) bbox[off+2] = v;
            }
            pi.next();
        }
    }

    static final int AT_IDENT = 0;
    static final int AT_TRANS = 1;
    static final int AT_GENERAL = 2;
    static int classify(BaseTransform at) {
        if (at == null) return AT_IDENT;
        switch (at.getType()) {
        case BaseTransform.TYPE_IDENTITY:
            return AT_IDENT;
        case BaseTransform.TYPE_TRANSLATION:
            return AT_TRANS;
        default:
            return AT_GENERAL;
        }
    }

    static boolean shapeContains(float x, float y,
                                 NGShape node,
                                 Shape s)
    {
        Mode mode = node.mode;
        if (mode == Mode.EMPTY) {
            return false;
        }
        if (mode != Mode.STROKE) {
            if (s.contains(x, y)) {
                return true;
            }
        }
        if (mode != Mode.FILL) {
            return node.getStrokeShape().contains(x, y);
        }
        return false;
    }
}
