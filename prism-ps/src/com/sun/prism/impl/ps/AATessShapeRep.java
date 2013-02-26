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
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.prism.Graphics;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.shape.ShapeRep;
import java.util.ArrayList;
import java.util.List;
import static com.sun.prism.shape.ShapeRep.InvalidationType.*;

public class AATessShapeRep implements ShapeRep {

    // static fields used by adjust/restoreLocation()
    static float savedX;
    static float savedY;

    private AATessShapeRepState fillState;
    private AATessShapeRepState drawState;

    public AATessShapeRep() {
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
            fillState = new AATessShapeRepState();
        }
        fillState.render(g, shape, null, savedX, savedY);
        restoreLocation(shape);
    }

    public void draw(Graphics g, Shape shape, BaseBounds bounds) {
        adjustLocation(shape);
        if (drawState == null) {
            drawState = new AATessShapeRepState();
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

class AATessShapeRepState {

    private static class GeomData {
        CacheEntry cacheEntry;
        VertexBuffer vbSolid;
        VertexBuffer vbCurve;
        int numSolidVerts;
        int numCurveVerts;

        GeomData copy() {
            GeomData data = new GeomData();
            data.cacheEntry     = this.cacheEntry;
            data.vbSolid        = this.vbSolid;
            data.vbCurve        = this.vbCurve;
            data.numSolidVerts  = this.numSolidVerts;
            data.numCurveVerts  = this.numCurveVerts;
            return data;
        }

        void copyInto(GeomData other) {
            if (other == null) {
                throw new InternalError("MaskTexData must be non-null");
            }
            other.cacheEntry     = this.cacheEntry;
            other.vbSolid        = this.vbSolid;
            other.vbCurve        = this.vbCurve;
            other.numSolidVerts  = this.numSolidVerts;
            other.numCurveVerts  = this.numCurveVerts;
        }
    }

    private static class CacheEntry {
        Shape shape;
        RectBounds shapeBounds;
        GeomData geomData;
        int refCount;

        Paint lastPaint;
        float lastExtraAlpha = -1f;
    }

    private static class GeomCache {
        private final List<CacheEntry> entries = new ArrayList<CacheEntry>();

        void get(BaseShaderContext context,
                 GeomData geomData,
                 Shape shape, RectBounds shapeBounds)
        {
            if (geomData == null) {
                throw new InternalError("GeomData must be non-null");
            }
            if (geomData.cacheEntry != null) {
                throw new InternalError("CacheEntry should already be null");
            }

            // NOTE: keep the list sorted and use binary search...
            for (int i = 0; i < entries.size(); i++) {
                CacheEntry entry = entries.get(i);

                // first do a quick check of the bounds
                if (dimensionsAreSimilar(shapeBounds, entry.shapeBounds)) {
                    // the bounds match an existing entry, so now try to
                    // match the actual shape
                    if (entry.shape.equals(shape)) {
                        // increment ref count for the chosen entry and
                        // link the given geomData to it
                        entry.refCount++;
                        entry.geomData.copyInto(geomData);
                        geomData.cacheEntry = entry;
                        return;
                    }
                }
            }

            // did not find an existing entry; create a new one here
            VertexBuffer vbSolid = context.getResourceFactory().createVertexBuffer(16);
            VertexBuffer vbCurve = context.getResourceFactory().createVertexBuffer(16);
            int[] numVerts = tess.generate(shape, vbSolid, vbCurve);
            if (numVerts != null) {
                // tesselation was successful
                geomData.vbSolid = vbSolid;
                geomData.vbCurve = vbCurve;
                geomData.numSolidVerts = numVerts[0];
                geomData.numCurveVerts = numVerts[1];
            } else {
                // tesselation failed; null out the vb references to indicate
                // that the tesselated geometry should not be used (but we
                // will still create a cache entry so that other identical
                // shapes don't need to bother with the tesselation step)
                geomData.vbSolid = null;
                geomData.vbCurve = null;
                geomData.numSolidVerts = 0;
                geomData.numCurveVerts = 0;
            }

            // add the new geom data to the cache; note that we copy the
            // shape so that dependents are not affected if the original
            // geometry is mutated (since NGPath will reuse and mutate a
            // single Path2D instance, for example)
            CacheEntry entry = new CacheEntry();
            // NOTE: if the shape was created by createStrokedShape() below,
            // then we don't need to use the copy constructor here...
            entry.shape = shape.copy();
            entry.shapeBounds = new RectBounds(shapeBounds);
            entry.geomData = geomData.copy();
            entry.refCount = 1;
            geomData.cacheEntry = entry;
            entries.add(entry);
        }

        void unref(GeomData geomData) {
            if (geomData == null) {
                throw new InternalError("GeomData must be non-null");
            }
            CacheEntry entry = geomData.cacheEntry;
            if (entry == null) {
                return;
            }
            geomData.cacheEntry = null;
            geomData.vbSolid = null;
            geomData.vbCurve = null;
            entry.refCount--;
            if (entry.refCount <= 0) {
                entry.shape = null;
                entry.shapeBounds = null;
                entry.geomData.vbSolid = null;
                entry.geomData.vbCurve = null;
                entry.geomData = null;
                entries.remove(entry);
            }
        }

        /**
         * Returns true if the dimensions of the two bounding boxes are
         * reasonably similar; otherwise returns false.  This is intended
         * to be a quick and dirty check.
         */
        private static boolean dimensionsAreSimilar(RectBounds a, RectBounds b) {
            return
                Math.abs(a.getWidth() - b.getWidth()) < 0.001 &&
                Math.abs(a.getHeight() - b.getHeight()) < 0.001;
        }
    }

    private static final AATesselatorImpl tess = new AATesselatorImpl();
    private static final GeomCache geomCache = new GeomCache();

    private final GeomData geomData;
    private Boolean useGeom = null;

    private final Object disposerReferent = new Object();
    private final Disposer.Record disposerRecord;

    AATessShapeRepState() {
        this.geomData = new GeomData();
        this.disposerRecord = new AATessDisposerRecord(geomData);
        Disposer.addRecord(disposerReferent, disposerRecord);
    }

    void invalidate() {
        // Note: this method will be called from the FX thread
        geomCache.unref(geomData);
        useGeom = null;
    }

    void render(Graphics g, Shape shape, BasicStroke stroke, float x, float y) {
        BaseShaderGraphics bsg = (BaseShaderGraphics)g;
        Paint paint = bsg.getPaint();
        float ea = bsg.getExtraAlpha();

        boolean updatePaint = false;
        if (useGeom == null) {
            if (stroke != null) {
                shape = stroke.createStrokedShape(shape);
            }
            BaseShaderContext context = bsg.getContext();
            RectBounds shapeBounds = shape.getBounds();
            geomCache.get(context, geomData, shape, shapeBounds);
            if (geomData.vbSolid != null) {
                useGeom = Boolean.TRUE;
                updatePaint = true;
            } else {
                useGeom = Boolean.FALSE;
            }
        }
        if (useGeom == Boolean.FALSE) {
            // the shape could not be tesselated, so just use the normal
            // software rasterization codepath

            // RT-27376
            // TODO: should not be using translate() here, as that will
            // affect the way non-proportional gradients are rendered...
            g.translate(x, y);
            g.fill(shape);
            g.translate(-x, -y);
            return;
        }

        VertexBuffer vbSolid = geomData.vbSolid;
        VertexBuffer vbCurve = geomData.vbCurve;
        int numSolidVerts = geomData.numSolidVerts;
        int numCurveVerts = geomData.numCurveVerts;

        if (updatePaint ||
            paint != geomData.cacheEntry.lastPaint ||
            ea != geomData.cacheEntry.lastExtraAlpha)
        {
            if (paint.getType() == Paint.Type.COLOR) {
                vbSolid.setPerVertexColor((Color)paint, ea);
                vbSolid.updateVertexColors(numSolidVerts);
                vbCurve.setPerVertexColor((Color)paint, ea);
                vbCurve.updateVertexColors(numCurveVerts);
            } else {
                vbSolid.setPerVertexColor(ea);
                vbSolid.updateVertexColors(numSolidVerts);
                vbCurve.setPerVertexColor(ea);
                vbCurve.updateVertexColors(numCurveVerts);
            }
            geomData.cacheEntry.lastPaint = paint;
            geomData.cacheEntry.lastExtraAlpha = ea;
        }

        float bx = 0f, by = 0f, bw = 0f, bh = 0f;
        if (paint.getType().isGradient() && ((Gradient)paint).isProportional()) {
            RectBounds b = shape.getBounds();
            bx = b.getMinX();
            by = b.getMinY();
            bw = b.getWidth();
            bh = b.getHeight();
        }

        // RT-27376
        // TODO: should not be using translate() here, as that will
        // affect the way non-proportional gradients are rendered...
        bsg.translate(x, y);
        if (numSolidVerts > 0) {
            bsg.fillTriangles(vbSolid, numSolidVerts, bx, by, bw, bh);
        }
        if (numCurveVerts > 0) {
            bsg.fillCubicCurves(vbCurve, numCurveVerts, bx, by, bw, bh);
        }
        bsg.translate(-x, -y);
    }

    private static class AATessDisposerRecord implements Disposer.Record {
        private GeomData geomData;

        private AATessDisposerRecord(GeomData geomData) {
            this.geomData = geomData;
        }

        public void dispose() {
            // Note: this method will only be called from the rendering thread
            if (geomData != null) {
                geomCache.unref(geomData);
                geomData = null;
            }
        }
    }
}
