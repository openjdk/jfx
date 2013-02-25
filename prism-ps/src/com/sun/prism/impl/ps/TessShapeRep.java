/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.ps;

import com.sun.javafx.geom.BaseBounds;
import com.sun.prism.impl.shape.*;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.prism.Graphics;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.prism.shape.ShapeRep;
import static com.sun.prism.shape.ShapeRep.InvalidationType.*;

public class TessShapeRep implements ShapeRep {

    // static fields used by adjust/restoreLocation()
    static float savedX;
    static float savedY;

    private TessShapeRepState fillState;
    private TessShapeRepState drawState;

    public TessShapeRep() {
    }

    public boolean is3DCapable() {
        return true;
    }

    public void invalidate(InvalidationType type) {
        if (type == LOCATION_AND_GEOMETRY) {
            if (fillState != null) {
                fillState.invalidate();
            }
            if (drawState != null) {
                drawState.invalidate();
            }
        }
    }

    public void fill(Graphics g, Shape shape, BaseBounds bounds) {
        adjustLocation(shape);
        if (fillState == null) {
            fillState = new TessShapeRepState();
        }
        fillState.render(g, shape, null, savedX, savedY);
        restoreLocation(shape);
    }

    public void draw(Graphics g, Shape shape, BaseBounds bounds) {
        adjustLocation(shape);
        if (drawState == null) {
            drawState = new TessShapeRepState();
        }
        drawState.render(g, shape, g.getStroke(), savedX, savedY);
        restoreLocation(shape);
    }

    public void dispose() {
    }

    /**
     * Saves the current (x,y) location of the given shape and adjusts
     * the shape location to be relative to a known point such as (0,0)
     * so that the geometry can be cached based on that position.
     */
    protected void adjustLocation(Shape shape) {
        savedX = savedY = 0f;
    }

    /**
     * Restores the original (x,y) location of the given shape.
     */
    protected void restoreLocation(Shape shape) {
    }
}

class TessShapeRepState {
    private static final TesselatorImpl tess = new TesselatorImpl();

    private boolean valid;
    private VertexBuffer vb;
    private int numVerts;
    private Paint lastPaint;
    private float lastExtraAlpha = -1f;

    void invalidate() {
        valid = false;
        lastPaint = null;
        lastExtraAlpha = -1f;
    }

    void render(Graphics g, Shape shape, BasicStroke stroke, float x, float y) {
        BaseGraphics bg = (BaseGraphics)g;
        Paint paint = bg.getPaint();
        float ea = bg.getExtraAlpha();
        if (vb == null) {
            vb = g.getResourceFactory().createVertexBuffer(16);
        }
        boolean updatePaint = false;
        if (!valid) {
            numVerts = generate(shape, stroke, vb);
            valid = true;
            updatePaint = true;
        }
        if (updatePaint || paint != lastPaint || ea != lastExtraAlpha) {
            if (paint.getType() == Paint.Type.COLOR) {
                vb.setPerVertexColor((Color)paint, ea);
                vb.updateVertexColors(numVerts);
            } else {
                vb.setPerVertexColor(ea);
                vb.updateVertexColors(numVerts);
            }

            lastPaint = paint;
            lastExtraAlpha = ea;
        }

        float bx = 0f, by = 0f, bw = 0f, bh = 0f;
        if (paint.isProportional()) {
            RectBounds b = shape.getBounds();
            bx = b.getMinX();
            by = b.getMinY();
            bw = b.getWidth();
            bh = b.getHeight();
        }

        // RT-27376
        // TODO: should not be using translate() here, as that will
        // affect the way non-proportional gradients are rendered...
        bg.translate(x, y);
        bg.fillTriangles(vb, numVerts, bx, by, bw, bh);
        bg.translate(-x, -y);
    }

    private int generate(Shape shape, BasicStroke stroke, VertexBuffer vb) {
        if (stroke != null) {
            shape = stroke.createStrokedShape(shape);
        }
        vb.rewind();
        return tess.generate(shape, vb);
    }
}
