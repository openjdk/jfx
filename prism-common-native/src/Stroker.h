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

#ifndef STROKER_H
#define STROKER_H

#include "Curve.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Constant value for join style.
 */
#define JOIN_MITER  0

/**
 * Constant value for join style.
 */
#define JOIN_ROUND  1

/**
 * Constant value for join style.
 */
#define JOIN_BEVEL  2

/**
 * Constant value for end cap style.
 */
#define CAP_BUTT    0

/**
 * Constant value for end cap style.
 */
#define CAP_ROUND   1

/**
 * Constant value for end cap style.
 */
#define CAP_SQUARE  2

typedef struct {
    jfloat *curves;
    jint curvesSIZE;
    jint end;
    jint *curveTypes;
    jint curveTypesSIZE;
    jint numCurves;
} PolyStack;

typedef struct {
    PathConsumer consumer;
    PathConsumer *out;

    jint capStyle;
    jint joinStyle;

    jfloat lineWidth2;

    jfloat offset[3][2];
    jfloat miter[2];
    jfloat miterLimitSq;

    jint prev;

    // The starting point of the path, and the slope there.
    jfloat sx0, sy0, sdx, sdy;
    // the current point and the slope there.
    jfloat cx0, cy0, cdx, cdy; // c stands for current
    // vectors that when added to (sx0,sy0) and (cx0,cy0) respectively yield the
    // first and last points on the left parallel path. Since this path is
    // parallel, it's slope at any point is parallel to the slope of the
    // original path (thought they may have different directions), so these
    // could be computed from sdx,sdy and cdx,cdy (and vice versa), but that
    // would be error prone and hard to read, so we keep these anyway.
    jfloat smx, smy, cmx, cmy;

    PolyStack reverse;
    Curve c;
} Stroker;

extern void Stroker_reset(Stroker *pStroker, jfloat lineWidth,
                          jint capStyle, jint joinStyle, jfloat miterLimit);

extern void Stroker_init(Stroker *pStroker,
                         PathConsumer *out,
                         jfloat lineWidth,
                         jint capStyle,
                         jint joinStyle,
                         jfloat miterLimit);

extern void Stroker_destroy(Stroker *pStroker);

#ifdef __cplusplus
}
#endif

#endif /* STROKER_H */

