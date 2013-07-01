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

import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.Vec3f;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.impl.VertexBuffer;
import java.util.ArrayList;
import java.util.List;
import com.sun.prism.util.tess.Tess;
import com.sun.prism.util.tess.Tessellator;
import com.sun.prism.util.tess.TessellatorCallbackAdapter;
import static com.sun.prism.util.tess.Tess.*;

final class AATesselatorImpl {

    private static final float EPSILON = 0.0001f;

    private final AATessCallbacks listener;
    private final Tessellator tess;
    private final float[] coords = new float[6];

    AATesselatorImpl() {
        listener = new AATessCallbacks();
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
     * If the shape could be successfully tesselated, returns an int array
     * with two elements (arr[0]==numSolidVerts, arr[1]==numCurveVerts);
     * otherwise returns null (and in which case the given VertexBuffers
     * are not modified).
     */
    int[] generate(Shape shape, VertexBuffer vbSolid, VertexBuffer vbCurve) {
        listener.setVertexBuffer(vbSolid);

        BaseTransform xform = BaseTransform.IDENTITY_TRANSFORM;
        PathIterator pi = shape.getPathIterator(xform);

        List<Segment> segments = new ArrayList<Segment>();
        int numCurveVerts = 0;
        float area = 0f;
        float movX = 0f;
        float movY = 0f;
        float curX = 0f;
        float curY = 0f;
        float prevX = 0f;
        float prevY = 0f;
        float tmpX = 0f;
        float tmpY = 0f;
        boolean started = false;
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                if (started) {
                    // NOTE: multiple subpaths not yet supported; if we do
                    // support later we need to reset area calculations here...
                    return null;
                }
                started = true;
                movX = prevX = curX = coords[0];
                movY = prevY = curY = coords[1];
                segments.add(new MoveTo(curX, curY));
                break;
            case PathIterator.SEG_LINETO:
                if (!hasInfOrNaN(coords, 2)) {
                    started = true;
                    curX = coords[0];
                    curY = coords[1];
                    if (curX != prevX || curY != prevY) {
                        area += curX * prevY - prevX * curY;
                        segments.add(new LineTo(prevX, prevY, curX, curY));
                    }
                    prevX = curX;
                    prevY = curY;
                }
                break;
            case PathIterator.SEG_QUADTO:
                float ctrlx = coords[0];
                float ctrly = coords[1];
                coords[4] = coords[2];
                coords[5] = coords[3];
                coords[0] = (prevX     + 2 * ctrlx) / 3;
                coords[1] = (prevY     + 2 * ctrly) / 3;
                coords[2] = (coords[4] + 2 * ctrlx) / 3;
                coords[3] = (coords[5] + 2 * ctrly) / 3;
                if (!hasInfOrNaN(coords, 6)) {
                    started = true;
                    tmpX = prevX;
                    tmpY = prevY;
                    for (int i = 0; i < 6; i+=2) {
                        curX = coords[i];
                        curY = coords[i+1];
                        area += curX * prevY - prevX * curY;
                        prevX = curX;
                        prevY = curY;
                    }
                    segments.add(new CubicTo(tmpX, tmpY,
                                             coords[0], coords[1],
                                             coords[2], coords[3],
                                             curX, curY));
                }
                break;
            case PathIterator.SEG_CUBICTO:
                /*
                 * The area calculations here are borrowed from the
                 * ShapeEvaluator class.  In a nutshell:
                 * - We measure the area of the polygon formed by the
                 *   endpoints and control points of the curve.
                 * - This technique is based on the formula for calculating
                 *   the area of a polygon.  The standard formula is:
                 *     Area(Poly) = 1/2 * sum(x[i]*y[i+1] - x[i+1]y[i])
                 * - The returned area is negative if the polygon is
                 *   "mostly clockwise" and positive if the polygon is
                 *   "mostly counter-clockwise".
                 * - As we walk through the PathIterator for the overall
                 *   shape, we determine the clockwise-ness of each "local"
                 *   segment, and then we use the accumulated area of the
                 *   entire shape to determine the clockwise-ness of the
                 *   overall shape.  If the two disagree for a particular
                 *   segment, we flip the "convex" flag of that segment.
                 */
                if (!hasInfOrNaN(coords, 6)) {
                    started = true;
                    tmpX = prevX;
                    tmpY = prevY;
                    for (int i = 0; i < 6; i+=2) {
                        curX = coords[i];
                        curY = coords[i+1];
                        area += curX * prevY - prevX * curY;
                        prevX = curX;
                        prevY = curY;
                    }
                    segments.add(new CubicTo(tmpX, tmpY,
                                             coords[0], coords[1],
                                             coords[2], coords[3],
                                             curX, curY));
                }
                break;
            case PathIterator.SEG_CLOSE:
                if (started) {
                    started = false;
                    curX = movX;
                    curY = movY;
                    segments.add(new Close(prevX, prevY, movX, movY));
                }
                break;
            default:
                throw new InternalError("Unknown segment type");
            }
            pi.next();
        }
        if (started) {
            segments.add(new Close(prevX, prevY, movX, movY));
        }

        // NOTE: for now, punt if we detect any overlapping hulls
        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            for (int j = i+1; j < segments.size(); j++) {
                Segment other = segments.get(j);
                if (seg.overlaps(other)) {
                    //System.err.println("overlap!");
                    return null;
                }
            }
        }

        //System.err.println("area: " + area);

        Tess.tessProperty(tess, TESS_WINDING_RULE,
                            (pi.getWindingRule() == PathIterator.WIND_NON_ZERO) ?
                            TESS_WINDING_NONZERO : TESS_WINDING_ODD);
        Tess.tessBeginPolygon(tess, null);
        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            if (area < 0) {
                // reverse our earlier decision
                seg.convex = !seg.convex;
            }
            numCurveVerts += seg.emitVertices(tess, vbCurve);
        }
        Tess.tessEndPolygon(tess);

        int[] numVerts = new int[2];
        numVerts[0] = listener.getNumVerts();
        numVerts[1] = numCurveVerts;
        return numVerts;
    }

    private static abstract class Segment {
        enum Type { MOVETO, LINETO, QUADTO, CUBICTO, CLOSE }
        final Type type;
        boolean convex;
        protected Segment(Type type) {
            this.type = type;
        }

        abstract int getEdges(float[] edges);
        abstract int emitVertices(Tessellator tess, VertexBuffer vbCurve);

        private static final int[] triOffsets = {
            0, 2, 2, 4, 4, 0,
            6, 8, 8, 10, 10, 6,
        };
        private static final float[] tmpThis = new float[12];
        private static final float[] tmpThat = new float[12];
        boolean overlaps(Segment that) {
            int numThis = this.getEdges(tmpThis);
            int numThat = that.getEdges(tmpThat);

            if (numThis < 1 || numThat < 1) {
                return false;
            }

            if (numThis == 1 && numThat == 1) {
                // simple line-line intersection
                return linesCross(tmpThis[0], tmpThis[1], tmpThis[2], tmpThis[3],
                                  tmpThat[0], tmpThat[1], tmpThat[2], tmpThat[3]);
            }

            // foreach tri in this
            //   foreach line in tri
            //     this line intersects that line?
            //   this tri inside that tri?
            //   that tri inside this tri?
            int maxThis = numThis*2;
            int maxThat = numThat*2;

            // check to see if this hull intersects the other hull
            for (int i = 0; i < maxThis; i+=2) {
                int i0 = triOffsets[i];
                int i1 = triOffsets[i+1];
                for (int j = 0; j < maxThat; j+=2) {
                    int j0 = triOffsets[j];
                    int j1 = triOffsets[j+1];
                    if (linesCross(tmpThis[i0+0], tmpThis[i0+1], tmpThis[i1+0], tmpThis[i1+1],
                                   tmpThat[j0+0], tmpThat[j0+1], tmpThat[j1+0], tmpThat[j1+1]))
                    {
                        //System.err.println("  cross:");
                        //System.err.printf("    %f %f %f %f\n", tmpThis[i0+0], tmpThis[i0+1], tmpThis[i1+0], tmpThis[i1+1]);
                        //System.err.printf("    %f %f %f %f\n", tmpThat[j0+0], tmpThat[j0+1], tmpThat[j1+0], tmpThat[j1+1]);
                        return true;
                    }
                }
            }

            // check to see if this hull is fully contained within
            // the other hull, or vice versa (we only need to test whether
            // a single point from the first hull/line is inside one of the
            // triangles from the second hull)
            // NOTE: maybe we need to test all points, since the first point
            // might be coincident with a triangle edge?
            for (int i = 0; i < numThis*2; i+=6) {
                for (int j = 0; j < numThat*2; j+=6) {
                    if (numThat > 1) {
                        if (triangleContainsPoint(tmpThis[i+0], tmpThis[i+1],
                                                  tmpThat[j+0], tmpThat[j+1],
                                                  tmpThat[j+2], tmpThat[j+3],
                                                  tmpThat[j+4], tmpThat[j+5]))
                        {
                           return true;
                        }
                    }
                    if (numThis > 1) {
                        if (triangleContainsPoint(tmpThat[j+0], tmpThat[j+1],
                                                  tmpThis[i+0], tmpThis[i+1],
                                                  tmpThis[i+2], tmpThis[i+3],
                                                  tmpThis[i+4], tmpThis[i+5]))
                        {
                           return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    private static class MoveTo extends Segment {
        private final float x;
        private final float y;
        MoveTo(float x, float y) {
            super(Type.MOVETO);
            this.x = x;
            this.y = y;
        }
        int getEdges(float[] edges) {
            return 0;
        }
        int emitVertices(Tessellator tess, VertexBuffer vbCurve) {
            Tess.tessBeginContour(tess);
            emitVert(tess, x, y);
            return 0;
        }
    }

    private static class LineTo extends Segment {
        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;
        LineTo(float x1, float y1, float x2, float y2) {
            super(Type.LINETO);
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
        int getEdges(float[] edges) {
            edges[0] = x1;
            edges[1] = y1;
            edges[2] = x2;
            edges[3] = y2;
            return 1;
        }
        int emitVertices(Tessellator tess, VertexBuffer vbCurve) {
            // NOTE: need to antialias the edges
            emitVert(tess, x2, y2);
            return 0;
        }
    }

//    private static class QuadTo extends Segment {
//        private final float x1;
//        private final float y1;
//        private final float ctrlx;
//        private final float ctrly;
//        private final float x2;
//        private final float y2;
//        QuadTo(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
//            super(Type.QUADTO);
//            this.x1 = x1;
//            this.y1 = y1;
//            this.ctrlx = ctrlx;
//            this.ctrly = ctrly;
//            this.x2 = x2;
//            this.y2 = y2;
//            this.convex = Line2D.relativeCCW(x1, y1, ctrlx, ctrly, x2, y2) > 0;
//        }
//        int getEdges(float[] edges) {
//            edges[0] = x1;
//            edges[1] = y1;
//            edges[2] = ctrlx;
//            edges[3] = ctrly;
//            edges[4] = x2;
//            edges[5] = y2;
//            return 3;
//        }
//        int emitVertices(Tessellator tess, VertexBuffer vbCurve) {
//            float inv;
//            if (convex) {
//                // add a single line segment to the solid path
//                emitVert(tess, x2, y2);
//                inv = 1f; // use the regular equation for convex
//            } else {
//                // add two line segments to the solid path
//                emitVert(tess, ctrlx, ctrly);
//                emitVert(tess, x2, y2);
//                inv = -1f; // flip the sign for concave
//            }
//            // add the triangle representing the quad curve
//            vbCurve.addVert(x1,    y1,    0.0f, 0.0f, inv, 0.0f);
//            vbCurve.addVert(ctrlx, ctrly, 0.5f, 0.0f, inv, 0.0f);
//            vbCurve.addVert(x2,    y2,    1.0f, 1.0f, inv, 0.0f);
//            return 3;
//        }
//    }

    /**
     * The calculations for classifying cubic curve segments and deriving
     * texcoords in the emit*() methods below are based on the techniques
     * described in "Rendering Vector Art on the GPU" (GPU Gems 3, Chapter 25).
     */
    private static class CubicTo extends Segment {
        private final float x1;
        private final float y1;
        private final float ctrlx1;
        private final float ctrly1;
        private final float ctrlx2;
        private final float ctrly2;
        private final float x2;
        private final float y2;
        private final int ccw1;
        private final int ccw2;
        private int hullType = -1;

        CubicTo(float x1, float y1,
                float ctrlx1, float ctrly1,
                float ctrlx2, float ctrly2,
                float x2, float y2)
        {
            super(Type.CUBICTO);
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
            this.ccw1 = Line2D.relativeCCW(x1, y1, ctrlx1, ctrly1, x2, y2);
            this.ccw2 = Line2D.relativeCCW(x1, y1, ctrlx2, ctrly2, x2, y2);
            this.convex = ccw1 > 0 && ccw2 > 0;
        }

        private int calculateHullType() {
            if (triangleContainsPoint(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2)) {
                // endpoint 1 is inside
                return 2;
            } else if (triangleContainsPoint(x2, y2, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2)) {
                // endpoint 2 is inside
                return 3;
            } else if (triangleContainsPoint(ctrlx1, ctrly1, x1, y1, ctrlx2, ctrly2, x2, y2)) {
                // control point 1 is inside
                return 4;
            } else if (triangleContainsPoint(ctrlx2, ctrly2, x1, y1, ctrlx1, ctrly1, x2, y2)) {
                // control point 2 is inside
                return 5;
            } else {
                if (ccw1 == ccw2) {
                    // convex hull with control points on same side
                    return 0;
                } else {
                    // convex hull with control points on opposite sides
                    return 1;
                }
            }
        }

        int getEdges(float[] edges) {
            // NOTE: this is a silly, braindead approach...
            if (hullType < 0) {
                hullType = calculateHullType();
                //System.err.printf("hullType: %d %f %f %f %f %f %f %f %f\n",
                //                  hullType, x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
            }

            switch (hullType) {
            case 0:
                // convex hull with control points on same side
                edges[0] = x1;
                edges[1] = y1;
                edges[2] = ctrlx1;
                edges[3] = ctrly1;
                edges[4] = ctrlx2;
                edges[5] = ctrly2;
                edges[6] = x1;
                edges[7] = y1;
                edges[8] = ctrlx2;
                edges[9] = ctrly2;
                edges[10] = x2;
                edges[11] = y2;
                return 6;
            case 1:
                // convex hull with control points on opposite sides
                edges[0] = x1;
                edges[1] = y1;
                edges[2] = ctrlx1;
                edges[3] = ctrly1;
                edges[4] = ctrlx2;
                edges[5] = ctrly2;
                edges[6] = ctrlx1;
                edges[7] = ctrly1;
                edges[8] = ctrlx2;
                edges[9] = ctrly2;
                edges[10] = x2;
                edges[11] = y2;
                return 6;
            case 2:
                // endpoint 1 is inside
                edges[0] = ctrlx1;
                edges[1] = ctrly1;
                edges[2] = ctrlx2;
                edges[3] = ctrly2;
                edges[4] = x2;
                edges[5] = y2;
                return 3;
            case 3:
                // endpoint 2 is inside
                edges[0] = x1;
                edges[1] = y1;
                edges[2] = ctrlx1;
                edges[3] = ctrly1;
                edges[4] = ctrlx2;
                edges[5] = ctrly2;
                return 3;
            case 4:
                // control point 1 is inside
                edges[0] = x1;
                edges[1] = y1;
                edges[2] = ctrlx2;
                edges[3] = ctrly2;
                edges[4] = x2;
                edges[5] = y2;
                return 3;
            case 5:
                // control point 2 is inside
                edges[0] = x1;
                edges[1] = y1;
                edges[2] = ctrlx1;
                edges[3] = ctrly1;
                edges[4] = x2;
                edges[5] = y2;
                return 3;
            default:
                throw new InternalError("Unknown hull type");
            }
        }

        private static final Vec3f b0 = new Vec3f();
        private static final Vec3f b1 = new Vec3f();
        private static final Vec3f b2 = new Vec3f();
        private static final Vec3f b3 = new Vec3f();
        private static final Vec3f m0 = new Vec3f();
        private static final Vec3f m1 = new Vec3f();
        private static final Vec3f m2 = new Vec3f();
        private static final Vec3f m3 = new Vec3f();
        private static final Vec3f tmp = new Vec3f();
        private static final float ONE_THIRD = 1f / 3f;
        private static final float TWO_THIRDS = 2f / 3f;
        int emitVertices(Tessellator tess, VertexBuffer vbCurve) {
            b0.set(x1,     y1,     1);
            b1.set(ctrlx1, ctrly1, 1);
            b2.set(ctrlx2, ctrly2, 1);
            b3.set(x2,     y2,     1);

            tmp.cross(b3, b2);
            float a1 = b0.dot(tmp);
            tmp.cross(b0, b3);
            float a2 = b1.dot(tmp);
            tmp.cross(b1, b0);
            float a3 = b2.dot(tmp);

            float d3 = (3*a3);
            float d2 = -a2 + d3;
            float d1 = a1 - (2*a2) + d3;
            // normalize to reduce range of the values
            tmp.set(d1, d2, d3);
            tmp.normalize();
            d1 = tmp.x;
            d2 = tmp.y;
            d3 = tmp.z;

            float dd = (3*d2*d2) - (4*d1*d3);
            float discrI = d1*d1*dd;

            //System.err.println(d1 + " " + d2 + " " + d3 + " " + discrI);

            if (isCloseToZero(d1)) d1 = 0f;
            if (isCloseToZero(d2)) d2 = 0f;
            if (isCloseToZero(d3)) d3 = 0f;
            if (isCloseToZero(discrI)) discrI = 0f;

            //System.err.println("convex=" + convex + " " + ccw1 + " " + ccw2);
            if (discrI > 0f) {
                // serpent
                //System.err.println("cubic: SERPENT");
                return emitSerpent(tess, vbCurve, d1, d2, dd);
            } else if (discrI < 0f) {
                // loop
                //System.err.println("cubic: LOOP");
                return emitLoop(tess, vbCurve, d1, d2, dd);
            } else { //if (discrI == 0f) {
                if (d1 == 0f && d2 == 0f) {
                    if (d3 != 0f) {
                        // quadratic
                        //System.err.println("cubic: QUADRATIC");
                        return emitQuadratic(tess, vbCurve);
                    } else {
                        // line or point
                        throw new InternalError("Line/point segment not yet supported");
                    }
                } else {
                    // cusp
                    throw new InternalError("Cusp segment not yet supported");
                }
            }
        }

        private int emitQuadratic(Tessellator tess, VertexBuffer vbCurve) {
            float inv;
            if (convex) {
                // add a single line segment to the solid path
                emitVert(tess, x2, y2);
                inv = 1f; // use the regular equation for convex
            } else {
                // add line segments to the solid path
                emitVert(tess, ctrlx1, ctrly1);
                emitVert(tess, ctrlx2, ctrly2);
                emitVert(tess, x2, y2);
                inv = -1f; // flip the sign for concave
            }

            float ot = ONE_THIRD;
            float tt = TWO_THIRDS;
            // add the triangles representing the cubic curve
            vbCurve.addVert(x1,     y1,     0f, 0f, 0f, inv);
            vbCurve.addVert(ctrlx1, ctrly1, ot, 0f, ot, inv);
            vbCurve.addVert(x2,     y2,     1f, 1f, 1f, inv);
            vbCurve.addVert(ctrlx1, ctrly1, ot, 0f, ot, inv);
            vbCurve.addVert(ctrlx2, ctrly2, tt, ot, tt, inv);
            vbCurve.addVert(x2,     y2,     1f, 1f, 1f, inv);
            return 6;
        }

        private int emitSerpent(Tessellator tess, VertexBuffer vbCurve,
                                float d1, float d2, float dd)
        {
            float ltmp, mtmp;
            float stmp = (float)Math.sqrt(3*dd);
            float ls = (3*d2) - stmp;
            float lt = 6*d1;
            float ms = (3*d2) + stmp;
            float mt = lt;

            m0.x = ls*ms;
            m0.y = ls*ls*ls;
            m0.z = ms*ms*ms;

            m1.x = ONE_THIRD * ((3*ls*ms) - (ls*mt) - (lt*ms));
            m1.y = (ls*ls) * (ls-lt);
            m1.z = (ms*ms) * (ms-mt);

            ltmp = lt-ls;
            mtmp = mt-ms;
            m2.x = ONE_THIRD * ((lt * (mt - (2*ms))) + (ls * (3*ms - 2*mt)));
            m2.y = ls * ltmp * ltmp;
            m2.z = ms * mtmp * mtmp;

            m3.x = ltmp * mtmp;
            m3.y = -(ltmp*ltmp*ltmp);
            m3.z = -(mtmp*mtmp*mtmp);

            float inv;
            if (ccw1 != ccw2) {
                // control points straddle the line connecting the endpoints
                if (ccw1 > 0) {
                    emitVert(tess, ctrlx1, ctrly1);
                } else {
                    emitVert(tess, ctrlx2, ctrly2);
                }
                emitVert(tess, x2, y2);

                inv = -1; // TODO

                vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
                return 6;
            } else {
                // control points on same side of line connecting the endpoints
                if (hullType == 4) {
                    // control point 1 is on inside; subdivide into 3 triangles
                    if (convex) {
                        emitVert(tess, x2, y2);
                        inv = -1f;
                    } else {
                        emitVert(tess, ctrlx2, ctrly2);
                        emitVert(tess, x2, y2);
                        inv = 1f;
                    }
                    vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                    vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                    vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                    vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
                    vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
                    return 9;
                } else if (hullType == 5) {
                    // control point 2 is on inside; subdivide into 3 triangles
                    if (convex) {
                        emitVert(tess, x2, y2);
                        inv = -1f;
                    } else {
                        emitVert(tess, ctrlx1, ctrly1);
                        emitVert(tess, x2, y2);
                        inv = 1f;
                    }
                    vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                    vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
                    vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
                    return 9;
                } else {
                    // convex hull; subdivide into 2 triangles
                    if (convex) {
                        emitVert(tess, x2, y2);
                        inv = -1f;
                    } else {
                        emitVert(tess, ctrlx1, ctrly1);
                        emitVert(tess, ctrlx2, ctrly2);
                        emitVert(tess, x2, y2);
                        inv = 1f;
                    }
                    vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                    vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
                    vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
                    vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
                    return 6;
                }
            }
        }

        private int emitLoop(Tessellator tess, VertexBuffer vbCurve,
                             float d1, float d2, float dd)
        {
            float ltmp, mtmp;

            float stmp = (float)Math.sqrt(-dd);
            float ls = d2 - stmp;
            float lt = 2*d1;
            float ms = d2 + stmp;
            float mt = lt;

            m0.x = ls*ms;
            m0.y = ls*ls*ms;
            m0.z = ls*ms*ms;

            // Note: there was a typo in m1.x in GPU Gems 3
            m1.x =  ONE_THIRD * (-(ls*mt) - (lt*ms) + (3*ls*ms));
            m1.y = -ONE_THIRD * ls * ((ls*(mt-(3*ms))) + (2*lt*ms));
            m1.z = -ONE_THIRD * ms * ((ls*((2*mt)-(3*ms))) + (lt*ms));

            ltmp = lt-ls;
            mtmp = mt-ms;
            m2.x = ONE_THIRD * (lt*(mt-(2*ms)) + (ls*((3*ms)-(2*mt))));
            m2.y = ONE_THIRD * ltmp * ((ls*((2*mt)-(3*ms))) + (lt*ms));
            m2.z = ONE_THIRD * mtmp * ((ls*(mt-(3*ms))) + (2*lt*ms));

            m3.x = ltmp * mtmp;
            m3.y = -(ltmp*ltmp*mtmp);
            m3.z = -(ltmp*mtmp*mtmp);

            float inv;
            if (convex) {
                emitVert(tess, x2, y2);
                inv = -1f;
            } else {
                emitVert(tess, ctrlx1, ctrly1);
                emitVert(tess, ctrlx2, ctrly2);
                emitVert(tess, x2, y2);
                inv = 1f;
            }

            vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
            vbCurve.addVert(ctrlx1, ctrly1, m1.x, m1.y, m1.z, inv);
            vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
            vbCurve.addVert(x1,     y1,     m0.x, m0.y, m0.z, inv);
            vbCurve.addVert(ctrlx2, ctrly2, m2.x, m2.y, m2.z, inv);
            vbCurve.addVert(x2,     y2,     m3.x, m3.y, m3.z, inv);
            return 6;
        }
    }

    private static class Close extends Segment {
        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;
        Close(float x1, float y1, float x2, float y2) {
            super(Type.CLOSE);
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
        int getEdges(float[] edges) {
            edges[0] = x1;
            edges[1] = y1;
            edges[2] = x2;
            edges[3] = y2;
            return 1;
        }
        int emitVertices(Tessellator tess, VertexBuffer vbCurve) {
            // it appears that the following will close the path automatically,
            // so no need to issue another vertex here
            Tess.tessEndContour(tess);
            return 0;
        }
    }

    /**
     * Returns true if the given point is on (or very close to) the specified
     * line segment.
     * <p>
     * Based on the implementation from Line2D.ptLineDistSq(), except using
     * floats instead of doubles.
     */
    static boolean pointOnLine(float px, float py,
                               float x1, float y1, float x2, float y2)
    {
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        float dotprod = px * x2 + py * y2;
        // dotprod is the length of the px,py vector
        // projected on the x1,y1=>x2,y2 vector times the
        // length of the x1,y1=>x2,y2 vector
        float projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        float lenSq = px * px + py * py - projlenSq;
        return lenSq < EPSILON;
    }

    /**
     * Returns true if the line segments intersect and are not coincident.
     * <p>
     * Based on "Faster Line Segment Intersection" by Franklin Antonio
     * (from Graphics Gems III).
     */
    static boolean linesCross(float x1, float y1,
                              float x2, float y2,
                              float x3, float y3,
                              float x4, float y4)
    {
        if (pointOnLine(x1, y1, x3, y3, x4, y4) ||
            pointOnLine(x2, y2, x3, y3, x4, y4) ||
            pointOnLine(x3, y3, x1, y1, x2, y2) ||
            pointOnLine(x4, y4, x1, y1, x2, y2))
        {
            // the line segments share an endpoint, or the endpoint of one
            // line touches the other line segment without crossing it
            return false;
        }

        float ax = x2-x1;
        float ay = y2-y1;
        float bx = x3-x4;
        float by = y3-y4;
        float denom = (ay*bx) - (ax*by);
        if (isCloseToZero(denom)) {
            // the line segments are coincident
            return false;
        }
        float cx = x1-x3;
        float cy = y1-y3;
        float p = (by*cx) - (bx*cy);
        if (denom > 0) {
            if (p < 0 || p > denom) {
                return false;
            }
        } else {
            if (p > 0 || p < denom) {
                return false;
            }
        }
        float q = (ax*cy) - (ay*cx);
        if (denom > 0) {
            if (q < 0 || q > denom) {
                return false;
            }
        } else {
            if (q > 0 || q < denom) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the given point (px,py) is inside the triangle
     * extending from (x1,y1) to (x2,y2) to (x3,y3), or returns false if
     * the point is outside or coincident with the triangle.
     * <p>
     * Based on algorithm described at:
     *   http://mathworld.wolfram.com/TriangleInterior.html
     */
    static boolean triangleContainsPoint(float px, float py,
                                         float x1, float y1,
                                         float x2, float y2,
                                         float x3, float y3)
    {
        px -= x1;
        py -= y1;
        x2 -= x1;
        y2 -= y1;
        x3 -= x1;
        y3 -= y1;

        float denom = (x2*y3) - (x3*y2);
        if (isCloseToZero(denom)) {
            // TODO: triangle is actually a line?
            return false;
        }

        float a = ((px*y3) - (py*x3)) / denom;
        float b = ((px*y2) - (py*x2)) / -denom;

        return (a > 0) && (b > 0) && (a+b < 1);
    }

    static boolean isCloseToZero(float x) {
        return x < EPSILON && x > -EPSILON;
    }

    /**
     * Returns true if any of {@code num} elements in the given array
     * are Inf or NaN.  Ideally PathIterators would never produce segments
     * with these values, but they are sometimes seen in practice and can
     * confuse the Tessellator, so it is a good idea to protect against
     * them here.
     */
    static boolean hasInfOrNaN(float[] coords, int num) {
        for (int i = 0; i < num; i++) {
            if (Float.isInfinite(coords[i]) || Float.isNaN(coords[i])) {
                return true;
            }
        }
        return false;
    }

    static void emitVert(Tessellator tess, float x, float y) {
        double[] vert = new double[] { x, y, 0 }; // TODO: reduce garbage
        Tess.tessVertex(tess, vert, 0, vert);
    }
}

class AATessCallbacks extends TessellatorCallbackAdapter {

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
