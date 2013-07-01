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

package com.sun.prism.impl.shape;

import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.util.tess.Tess;
import com.sun.prism.util.tess.Tessellator;
import com.sun.prism.util.tess.TessellatorCallbackAdapter;
import static com.sun.prism.util.tess.Tess.*;

public final class TesselatorImpl {

    private final TessCallbacks listener;
    private final Tessellator tess;
    private final float[] coords = new float[6];

    public TesselatorImpl() {
        listener = new TessCallbacks();
        tess = Tess.newTess();
        Tess.tessCallback(tess, TESS_BEGIN, listener);
        Tess.tessCallback(tess, TESS_VERTEX, listener);
        Tess.tessCallback(tess, TESS_END, listener);
        Tess.tessCallback(tess, TESS_COMBINE, listener);
        Tess.tessCallback(tess, TESS_ERROR, listener);
        // we don't care about the edge flag callback, but registering
        // it here ensures that we only get triangles (no fans or strips)
        Tess.tessCallback(tess, TESS_EDGE_FLAG, listener);
    }

    /**
     * @return the number of vertices added to the given vertex buffer
     */
    public int generate(Shape shape, VertexBuffer vb) {
        listener.setVertexBuffer(vb);

        BaseTransform xform = BaseTransform.IDENTITY_TRANSFORM;
        PathIterator pi = shape.getPathIterator(xform, 1f/*TODO*/);
        Tess.tessProperty(tess, TESS_WINDING_RULE,
                            (pi.getWindingRule() == PathIterator.WIND_NON_ZERO) ?
                            TESS_WINDING_NONZERO : TESS_WINDING_ODD);

        double[] vert;
        boolean started = false;
        Tess.tessBeginPolygon(tess, null);
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                if (started) {
                    Tess.tessEndContour(tess);
                }
                Tess.tessBeginContour(tess);
                started = true;
                // TODO: reduce garbage
                vert = new double[] { coords[0], coords[1], 0 };
                Tess.tessVertex(tess, vert, 0, vert);
                break;
            case PathIterator.SEG_LINETO:
                if (!hasInfOrNaN(coords, 2)) {
                    started = true;
                    vert = new double[] { coords[0], coords[1], 0 };
                    Tess.tessVertex(tess, vert, 0, vert);
                }
                break;
            case PathIterator.SEG_CLOSE:
                if (started) {
                    Tess.tessEndContour(tess);
                    started = false;
                }
                break;
            default:
                throw new InternalError("Path must be flattened");
            }
            pi.next();
        }
        if (started) {
            Tess.tessEndContour(tess);
        }
        Tess.tessEndPolygon(tess);

        return listener.getNumVerts();
    }

    /**
     * Returns true if any of {@code num} elements in the given array
     * are Inf or NaN.  Ideally PathIterators would never produce segments
     * with these values, but they are sometimes seen in practice and can
     * confuse the Tessellator, so it is a good idea to protect against
     * them here.
     */
    private static boolean hasInfOrNaN(float[] coords, int num) {
        for (int i = 0; i < num; i++) {
            if (Float.isInfinite(coords[i]) || Float.isNaN(coords[i])) {
                return true;
            }
        }
        return false;
    }
}

class TessCallbacks extends TessellatorCallbackAdapter {

    private int nPrims = 0;
    private int nVerts = 0;
    private VertexBuffer vb;

    public void setVertexBuffer(VertexBuffer vb) {
        this.vb = vb;
        nPrims = 0;
        nVerts = 0;
    }

    public int getNumVerts() {
        return nVerts;
    }

    @Override
    public void begin(int primType) {
        if (primType != GL_TRIANGLES) {
            throw new InternalError("Only GL_TRIANGLES is supported");
        }
        nVerts = 0;
    }

    @Override
    public void end() {
        nPrims++;
        //System.out.println("End primitive: " + nPrims + " " + nVerts);
    }

    @Override
    public void vertex(Object vertexData) {
        double[] coords = (double[]) vertexData;
        vb.addVert((float)coords[0], (float)coords[1]);
        nVerts++;
    }

    @Override
    public void edgeFlag(boolean edge) {
    }

    @Override
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData)
    {
        double[] vertex = new double[3];
        for (int i = 0; i < 3; i++) {
            vertex[i] = coords[i];
        }
        outData[0] = vertex;
    }

    @Override
    public void error(int errorNumber) {
        System.err.println("Tesselation error " + errorNumber);
    }
}
