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

#ifndef RENDERER_H
#define RENDERER_H

#include "PathConsumer.h"
#include "AlphaConsumer.h"
#include "Curve.h"

#ifdef __cplusplus
extern "C" {
#endif

#define INIT_CROSSINGS_SIZE   10
typedef struct {
    jint *crossings;
    jint crossingsSIZE;
    jint *edgePtrs;
    jint edgePtrsSIZE;
    jint edgeCount;

    // crossing bounds. The bounds are not necessarily tight (the scan line
    // at minY, for example, might have no crossings). The x bounds will
    // be accumulated as crossings are computed.
    jint nextY;
} ScanlineIterator;

// common to all types of input path segments.
#define YMAX         0
#define CURX         1

// NEXT and OR are meant to be indices into "int" fields, but arrays must
// be homogenous, so every field is a float. However floats can represent
// exactly up to 26 bit ints, so we're ok.
#define OR           2
#define SLOPE        3
#define NEXT         4
#define SIZEOF_EDGE  5

#define WIND_EVEN_ODD   0
#define WIND_NON_ZERO   1

#define DEC_BND   20.0f
#define INC_BND   8.0f

typedef struct {
    PathConsumer consumer;

    ScanlineIterator iterator;

    jint sampleRowMin;
    jint sampleRowMax;
    jfloat edgeMinX;
    jfloat edgeMaxX;

    jfloat *edges;
    jint edgesSIZE;
    jint *edgeBuckets;
    jint edgeBucketsSIZE;
    jint numEdges;

    // Bounds of the drawing region, at subpixel precision.
    jint boundsMinX, boundsMinY, boundsMaxX, boundsMaxY;

    // Current winding rule
    jint windingRule;

    // Current drawing position, i.e., final point of last segment
    jfloat x0, y0;

    // Position of most recent 'moveTo' command
    jfloat pix_sx0, pix_sy0;

    Curve c;
} Renderer;

extern void Renderer_setup(jint subpixelLgPositionsX, jint subpixelLgPositionsY);

extern void Renderer_init(Renderer *pRenderer);

extern void Renderer_reset(Renderer *pRenderer,
                           jint pix_boundsX, jint pix_boundsY,
                           jint pix_boundsWidth, jint pix_boundsHeight,
                           jint windingRule);

extern void Renderer_destroy(Renderer *pRenderer);

extern void Renderer_getOutputBounds(Renderer *pRenderer, jint bounds[]);

extern void Renderer_produceAlphas(Renderer *pRenderer, AlphaConsumer *pAC);

#ifdef __cplusplus
}
#endif

#endif /* RENDERER_H */

