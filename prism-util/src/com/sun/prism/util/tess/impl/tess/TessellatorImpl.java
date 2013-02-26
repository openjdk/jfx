/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.1 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
**
** Author: Eric Veach, July 1994
** Java Port: Pepijn Van Eeckhoudt, July 2003
** Java Port: Nathan Parker Burg, August 2003
*/
package com.sun.prism.util.tess.impl.tess;

import com.sun.prism.util.tess.*;

public class TessellatorImpl implements Tessellator {
    public static final int TESS_MAX_CACHE = 100;

    private int state;          /* what begin/end calls have we seen? */

    private GLUhalfEdge lastEdge;       /* lastEdge->Org is the most recent vertex */
    GLUmesh mesh;               /* stores the input contours, and eventually
                                   the tessellation itself */

    /*** state needed for projecting onto the sweep plane ***/

    double[] normal = new double[3];    /* user-specified normal (if provided) */
    double[] sUnit = new double[3];     /* unit vector in s-direction (debugging) */
    double[] tUnit = new double[3];     /* unit vector in t-direction (debugging) */

    /*** state needed for the line sweep ***/

    private double relTolerance;        /* tolerance for merging features */
    int windingRule;    /* rule for determining polygon interior */
    boolean fatalError; /* fatal error: needed combine callback */

    Dict dict;          /* edge dictionary for sweep line */
    PriorityQ pq;               /* priority queue of vertex events */
    GLUvertex event;            /* current sweep event being processed */

    /*** state needed for rendering callbacks (see render.c) ***/

    boolean flagBoundary;       /* mark boundary edges (use EdgeFlag) */
    boolean boundaryOnly;       /* Extract contours, not triangles */
    boolean avoidDegenerateTris; /* JOGL-specific hint to try to improve triangulation
                                    by avoiding producing degenerate (zero-area) triangles;
                                    has not been tested exhaustively and is therefore an option */

    GLUface lonelyTriList;
    /* list of triangles which could not be rendered as strips or fans */



    /*** state needed to cache single-contour polygons for renderCache() */

    private boolean flushCacheOnNextVertex;             /* empty cache on next vertex() call */
    int cacheCount;             /* number of cached vertices */
    CachedVertex[] cache = new CachedVertex[TESS_MAX_CACHE];    /* the vertex data */

    /*** rendering callbacks that also pass polygon data  ***/
    private Object polygonData;         /* client data for current polygon */

    private TessellatorCallback callBegin;
    private TessellatorCallback callEdgeFlag;
    private TessellatorCallback callVertex;
    private TessellatorCallback callEnd;
//    private TessellatorCallback callMesh;
    private TessellatorCallback callError;
    private TessellatorCallback callCombine;

    private TessellatorCallback callBeginData;
    private TessellatorCallback callEdgeFlagData;
    private TessellatorCallback callVertexData;
    private TessellatorCallback callEndData;
//    private TessellatorCallback callMeshData;
    private TessellatorCallback callErrorData;
    private TessellatorCallback callCombineData;

    private static final double GLU_TESS_DEFAULT_TOLERANCE = 0.0;
//    private static final int GLU_TESS_MESH = 100112;  /* void (*)(GLUmesh *mesh)          */
    private static TessellatorCallback NULL_CB = new TessellatorCallbackAdapter();

//    #define MAX_FAST_ALLOC    (MAX(sizeof(EdgePair), \
//                 MAX(sizeof(GLUvertex),sizeof(GLUface))))

    private TessellatorImpl() {
        state = TessState.T_DORMANT;

        normal[0] = 0;
        normal[1] = 0;
        normal[2] = 0;

        relTolerance = GLU_TESS_DEFAULT_TOLERANCE;
        windingRule = Tess.TESS_WINDING_ODD;
        flagBoundary = false;
        boundaryOnly = false;

        callBegin = NULL_CB;
        callEdgeFlag = NULL_CB;
        callVertex = NULL_CB;
        callEnd = NULL_CB;
        callError = NULL_CB;
        callCombine = NULL_CB;
//        callMesh = NULL_CB;

        callBeginData = NULL_CB;
        callEdgeFlagData = NULL_CB;
        callVertexData = NULL_CB;
        callEndData = NULL_CB;
        callErrorData = NULL_CB;
        callCombineData = NULL_CB;

        polygonData = null;

        for (int i = 0; i < cache.length; i++) {
            cache[i] = new CachedVertex();
        }
    }

    static public Tessellator gluNewTess()
    {
        return new TessellatorImpl();
    }


    private void makeDormant() {
        /* Return the tessellator to its original dormant state. */

        if (mesh != null) {
            Mesh.__gl_meshDeleteMesh(mesh);
        }
        state = TessState.T_DORMANT;
        lastEdge = null;
        mesh = null;
    }

    private void requireState(int newState) {
        if (state != newState) gotoState(newState);
    }

    private void gotoState(int newState) {
        while (state != newState) {
            /* We change the current state one level at a time, to get to
             * the desired state.
             */
            if (state < newState) {
                if (state == TessState.T_DORMANT) {
                    callErrorOrErrorData(Tess.TESS_MISSING_BEGIN_POLYGON);
                    gluTessBeginPolygon(null);
                } else if (state == TessState.T_IN_POLYGON) {
                    callErrorOrErrorData(Tess.TESS_MISSING_BEGIN_CONTOUR);
                    gluTessBeginContour();
                }
            } else {
                if (state == TessState.T_IN_CONTOUR) {
                    callErrorOrErrorData(Tess.TESS_MISSING_END_CONTOUR);
                    gluTessEndContour();
                } else if (state == TessState.T_IN_POLYGON) {
                    callErrorOrErrorData(Tess.TESS_MISSING_END_POLYGON);
                    /* gluTessEndPolygon( tess ) is too much work! */
                    makeDormant();
                }
            }
        }
    }

    public void gluDeleteTess() {
        requireState(TessState.T_DORMANT);
    }

    public void gluTessProperty(int which, double value) {
        switch (which) {
            case Tess.TESS_TOLERANCE:
                if (value < 0.0 || value > 1.0) break;
                relTolerance = value;
                return;

            case Tess.TESS_WINDING_RULE:
                int windingRule = (int) value;
                if (windingRule != value) break;        /* not an integer */

                switch (windingRule) {
                    case Tess.TESS_WINDING_ODD:
                    case Tess.TESS_WINDING_NONZERO:
                    case Tess.TESS_WINDING_POSITIVE:
                    case Tess.TESS_WINDING_NEGATIVE:
                    case Tess.TESS_WINDING_ABS_GEQ_TWO:
                        this.windingRule = windingRule;
                        return;
                    default:
                        break;
                }

            case Tess.TESS_BOUNDARY_ONLY:
                boundaryOnly = (value != 0);
                return;

            case Tess.TESS_AVOID_DEGENERATE_TRIANGLES:
                avoidDegenerateTris = (value != 0);
                return;

            default:
                callErrorOrErrorData(Tess.INVALID_ENUM);
                return;
        }
        callErrorOrErrorData(Tess.INVALID_VALUE);
    }

/* Returns tessellator property */
    public void gluGetTessProperty(int which, double[] value, int value_offset) {
        switch (which) {
            case Tess.TESS_TOLERANCE:
/* tolerance should be in range [0..1] */
                assert (0.0 <= relTolerance && relTolerance <= 1.0);
                value[value_offset] = relTolerance;
                break;
            case Tess.TESS_WINDING_RULE:
                assert (windingRule == Tess.TESS_WINDING_ODD ||
                        windingRule == Tess.TESS_WINDING_NONZERO ||
                        windingRule == Tess.TESS_WINDING_POSITIVE ||
                        windingRule == Tess.TESS_WINDING_NEGATIVE ||
                        windingRule == Tess.TESS_WINDING_ABS_GEQ_TWO);
                value[value_offset] = windingRule;
                break;
            case Tess.TESS_BOUNDARY_ONLY:
                assert (boundaryOnly == true || boundaryOnly == false);
                value[value_offset] = boundaryOnly ? 1 : 0;
                break;
            case Tess.TESS_AVOID_DEGENERATE_TRIANGLES:
                value[value_offset] = avoidDegenerateTris ? 1 : 0;
                break;
            default:
                value[value_offset] = 0.0;
                callErrorOrErrorData(Tess.INVALID_ENUM);
                break;
        }
    } /* gluGetTessProperty() */

    public void gluTessNormal(double x, double y, double z) {
        normal[0] = x;
        normal[1] = y;
        normal[2] = z;
    }

    public void gluTessCallback(int which, TessellatorCallback aCallback) {
        switch (which) {
            case Tess.TESS_BEGIN:
                callBegin = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_BEGIN_DATA:
                callBeginData = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_EDGE_FLAG:
                callEdgeFlag = aCallback == null ? NULL_CB : aCallback;
/* If the client wants boundary edges to be flagged,
 * we render everything as separate triangles (no strips or fans).
 */
                flagBoundary = aCallback != null;
                return;
            case Tess.TESS_EDGE_FLAG_DATA:
                callEdgeFlagData = callBegin = aCallback == null ? NULL_CB : aCallback;
/* If the client wants boundary edges to be flagged,
 * we render everything as separate triangles (no strips or fans).
 */
                flagBoundary = (aCallback != null);
                return;
            case Tess.TESS_VERTEX:
                callVertex = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_VERTEX_DATA:
                callVertexData = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_END:
                callEnd = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_END_DATA:
                callEndData = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_ERROR:
                callError = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_ERROR_DATA:
                callErrorData = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_COMBINE:
                callCombine = aCallback == null ? NULL_CB : aCallback;
                return;
            case Tess.TESS_COMBINE_DATA:
                callCombineData = aCallback == null ? NULL_CB : aCallback;
                return;
//            case GLU_TESS_MESH:
//                callMesh = aCallback == null ? NULL_CB : aCallback;
//                return;
            default:
                callErrorOrErrorData(Tess.INVALID_ENUM);
                return;
        }
    }

    private boolean addVertex(double[] coords, Object vertexData) {
        GLUhalfEdge e;

        e = lastEdge;
        if (e == null) {
/* Make a self-loop (one vertex, one edge). */

            e = Mesh.__gl_meshMakeEdge(mesh);
            if (e == null) return false;
            if (!Mesh.__gl_meshSplice(e, e.Sym)) return false;
        } else {
/* Create a new vertex and edge which immediately follow e
 * in the ordering around the left face.
 */
            if (Mesh.__gl_meshSplitEdge(e) == null) return false;
            e = e.Lnext;
        }

/* The new vertex is now e.Org. */
        e.Org.data = vertexData;
        e.Org.coords[0] = coords[0];
        e.Org.coords[1] = coords[1];
        e.Org.coords[2] = coords[2];

/* The winding of an edge says how the winding number changes as we
 * cross from the edge''s right face to its left face.  We add the
 * vertices in such an order that a CCW contour will add +1 to
 * the winding number of the region inside the contour.
 */
        e.winding = 1;
        e.Sym.winding = -1;

        lastEdge = e;

        return true;
    }

    private void cacheVertex(double[] coords, Object vertexData) {
        if (cache[cacheCount] == null) {
            cache[cacheCount] = new CachedVertex();
        }

        CachedVertex v = cache[cacheCount];

        v.data = vertexData;
        v.coords[0] = coords[0];
        v.coords[1] = coords[1];
        v.coords[2] = coords[2];
        ++cacheCount;
    }


    private boolean flushCache() {
        CachedVertex[] v = cache;

        mesh = Mesh.__gl_meshNewMesh();
        if (mesh == null) return false;

        for (int i = 0; i < cacheCount; i++) {
            CachedVertex vertex = v[i];
            if (!addVertex(vertex.coords, vertex.data)) return false;
        }
        cacheCount = 0;
        flushCacheOnNextVertex = false;

        return true;
    }

    public void gluTessVertex(double[] coords, int coords_offset, Object vertexData) {
        int i;
        boolean tooLarge = false;
        double x;
        double[] clamped = new double[3];

        requireState(TessState.T_IN_CONTOUR);

        if (flushCacheOnNextVertex) {
            if (!flushCache()) {
                callErrorOrErrorData(Tess.OUT_OF_MEMORY);
                return;
            }
            lastEdge = null;
        }
        for (i = 0; i < 3; ++i) {
            x = coords[i+coords_offset];
            if (x < -Tess.TESS_MAX_COORD) {
                x = -Tess.TESS_MAX_COORD;
                tooLarge = true;
            }
            if (x > Tess.TESS_MAX_COORD) {
                x = Tess.TESS_MAX_COORD;
                tooLarge = true;
            }
            clamped[i] = x;
        }
        if (tooLarge) {
            callErrorOrErrorData(Tess.TESS_COORD_TOO_LARGE);
        }

        if (mesh == null) {
            if (cacheCount < TESS_MAX_CACHE) {
                cacheVertex(clamped, vertexData);
                return;
            }
            if (!flushCache()) {
                callErrorOrErrorData(Tess.OUT_OF_MEMORY);
                return;
            }
        }

        if (!addVertex(clamped, vertexData)) {
            callErrorOrErrorData(Tess.OUT_OF_MEMORY);
        }
    }


    public void gluTessBeginPolygon(Object data) {
        requireState(TessState.T_DORMANT);

        state = TessState.T_IN_POLYGON;
        cacheCount = 0;
        flushCacheOnNextVertex = false;
        mesh = null;

        polygonData = data;
    }


    public void gluTessBeginContour() {
        requireState(TessState.T_IN_POLYGON);

        state = TessState.T_IN_CONTOUR;
        lastEdge = null;
        if (cacheCount > 0) {
/* Just set a flag so we don't get confused by empty contours
 * -- these can be generated accidentally with the obsolete
 * NextContour() interface.
 */
            flushCacheOnNextVertex = true;
        }
    }


    public void gluTessEndContour() {
        requireState(TessState.T_IN_CONTOUR);
        state = TessState.T_IN_POLYGON;
    }

    public void gluTessEndPolygon() {
        GLUmesh mesh;

        try {
            requireState(TessState.T_IN_POLYGON);
            state = TessState.T_DORMANT;

            if (this.mesh == null) {
                if (!flagBoundary /*&& callMesh == NULL_CB*/) {

/* Try some special code to make the easy cases go quickly
 * (eg. convex polygons).  This code does NOT handle multiple contours,
 * intersections, edge flags, and of course it does not generate
 * an explicit mesh either.
 */
                    if (Render.__gl_renderCache(this)) {
                        polygonData = null;
                        return;
                    }
                }
                if (!flushCache()) throw new RuntimeException(); /* could've used a label*/
            }

/* Determine the polygon normal and project vertices onto the plane
         * of the polygon.
         */
            Normal.__gl_projectPolygon(this);

/* __gl_computeInterior( tess ) computes the planar arrangement specified
 * by the given contours, and further subdivides this arrangement
 * into regions.  Each region is marked "inside" if it belongs
 * to the polygon, according to the rule given by windingRule.
 * Each interior region is guaranteed be monotone.
 */
            if (!Sweep.__gl_computeInterior(this)) {
                throw new RuntimeException();   /* could've used a label */
            }

            mesh = this.mesh;
            if (!fatalError) {
                boolean rc = true;

/* If the user wants only the boundary contours, we throw away all edges
 * except those which separate the interior from the exterior.
 * Otherwise we tessellate all the regions marked "inside".
 */
                if (boundaryOnly) {
                    rc = TessMono.__gl_meshSetWindingNumber(mesh, 1, true);
                } else {
                    rc = TessMono.__gl_meshTessellateInterior(mesh, avoidDegenerateTris);
                }
                if (!rc) throw new RuntimeException();  /* could've used a label */

                Mesh.__gl_meshCheckMesh(mesh);

                if (callBegin != NULL_CB || callEnd != NULL_CB
                        || callVertex != NULL_CB || callEdgeFlag != NULL_CB
                        || callBeginData != NULL_CB
                        || callEndData != NULL_CB
                        || callVertexData != NULL_CB
                        || callEdgeFlagData != NULL_CB) {
                    if (boundaryOnly) {
                        Render.__gl_renderBoundary(this, mesh);  /* output boundary contours */
                    } else {
                        Render.__gl_renderMesh(this, mesh);        /* output strips and fans */
                    }
                }
//                if (callMesh != NULL_CB) {
//
///* Throw away the exterior faces, so that all faces are interior.
//                 * This way the user doesn't have to check the "inside" flag,
//                 * and we don't need to even reveal its existence.  It also leaves
//                 * the freedom for an implementation to not generate the exterior
//                 * faces in the first place.
//                 */
//                    TessMono.__gl_meshDiscardExterior(mesh);
//                    callMesh.mesh(mesh);              /* user wants the mesh itself */
//                    mesh = null;
//                    polygonData = null;
//                    return;
//                }
            }
            Mesh.__gl_meshDeleteMesh(mesh);
            polygonData = null;
            mesh = null;
        } catch (Exception e) {
            e.printStackTrace();
            callErrorOrErrorData(Tess.OUT_OF_MEMORY);
        }
    }

    /*******************************************************/

/* Obsolete calls -- for backward compatibility */

    public void gluBeginPolygon() {
        gluTessBeginPolygon(null);
        gluTessBeginContour();
    }


/*ARGSUSED*/
    public void gluNextContour(int type) {
        gluTessEndContour();
        gluTessBeginContour();
    }


    public void gluEndPolygon() {
        gluTessEndContour();
        gluTessEndPolygon();
    }

    void callBeginOrBeginData(int a) {
        if (callBeginData != NULL_CB)
            callBeginData.beginData(a, polygonData);
        else
            callBegin.begin(a);
    }

    void callVertexOrVertexData(Object a) {
        if (callVertexData != NULL_CB)
            callVertexData.vertexData(a, polygonData);
        else
            callVertex.vertex(a);
    }

    void callEdgeFlagOrEdgeFlagData(boolean a) {
        if (callEdgeFlagData != NULL_CB)
            callEdgeFlagData.edgeFlagData(a, polygonData);
        else
            callEdgeFlag.edgeFlag(a);
    }

    void callEndOrEndData() {
        if (callEndData != NULL_CB)
            callEndData.endData(polygonData);
        else
            callEnd.end();
    }

    void callCombineOrCombineData(double[] coords, Object[] vertexData, float[] weights, Object[] outData) {
        if (callCombineData != NULL_CB)
            callCombineData.combineData(coords, vertexData, weights, outData, polygonData);
        else
            callCombine.combine(coords, vertexData, weights, outData);
    }

    void callErrorOrErrorData(int a) {
        if (callErrorData != NULL_CB)
            callErrorData.errorData(a, polygonData);
        else
            callError.error(a);
    }

}
