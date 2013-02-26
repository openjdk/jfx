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

#ifndef HELPERS_H
#define HELPERS_H

#ifdef __cplusplus
extern "C" {
#endif

#define BIGGEST_FLOAT 3.4028234663852886E38f
#define PI 3.141592653589793

#define new_float(s)   ((jfloat *) calloc((s), sizeof(jfloat)))
#define new_int(s)     ((jint   *) calloc((s), sizeof(jint)))

#define Math_max(a, b)  (((a) > (b)) ? (a) : (b))
#define Math_min(a, b)  (((a) < (b)) ? (a) : (b))
#define Math_isnan(v)    (!((v) == (v)))

#define System_arraycopy(arr1, off1, arr2, off2, n)       \
    do {                                                  \
        jint __i;                                         \
        for (__i = 0; __i < (n); ++__i) {                 \
            (arr2)[(off2) + __i] = (arr1)[(off1) + __i];  \
        }                                                 \
    } while (0)

#define Arrays_fill(arr, from, to, val)                   \
    do {                                                  \
        jint __i;                                         \
        jint __to = (to);                                 \
        for (__i = (from); __i < __to; ++__i) {           \
            (arr)[__i] = (val);                           \
        }                                                 \
    } while (0)

extern jboolean Helpers_withinULP(const jfloat x, const jfloat y, const int maxUlps);

extern jboolean Helpers_within(const jfloat x, const jfloat y, const jfloat err);

extern jint Helpers_quadraticRoots(const jfloat a, const jfloat b, const jfloat c,
                                   jfloat zeroes[], const jint off);

// find the roots of g(t) = d*t^3 + a*t^2 + b*t + c in [A,B)
extern jint Helpers_cubicRootsInAB(jfloat d, jfloat a, jfloat b, jfloat c,
                                   jfloat pts[], const jint off,
                                   const jfloat A, const jfloat B);

extern jfloat Helpers_evalCubic(const jfloat a, const jfloat b,
                                const jfloat c, const jfloat d,
                                const jfloat t);

extern jfloat Helpers_evalQuad(const jfloat a, const jfloat b,
                               const jfloat c, const jfloat t);

// returns the index 1 past the last valid element remaining after filtering
extern jint Helpers_filterOutNotInAB(jfloat nums[], const jint off, const jint len,
                                     const jfloat a, const jfloat b);

extern jfloat Helpers_polyLineLength(jfloat poly[], const jint off, const jint nCoords);

extern jfloat Helpers_linelen(jfloat x1, jfloat y1, jfloat x2, jfloat y2);

extern void Helpers_subdivide(jfloat src[], jint srcoff, jfloat left[], jint leftoff,
                              jfloat right[], jint rightoff, jint type);

extern void Helpers_isort(jfloat a[], jint off, jint len);

// Most of these are copied from classes in java.awt.geom because we need
// float versions of these functions, and Line2D, CubicCurve2D,
// QuadCurve2D don't provide them.
/**
 * Subdivides the cubic curve specified by the coordinates
 * stored in the <code>src</code> array at indices <code>srcoff</code>
 * through (<code>srcoff</code>&nbsp;+&nbsp;7) and stores the
 * resulting two subdivided curves into the two result arrays at the
 * corresponding indices.
 * Either or both of the <code>left</code> and <code>right</code>
 * arrays may be <code>null</code> or a reference to the same array
 * as the <code>src</code> array.
 * Note that the last point in the first subdivided curve is the
 * same as the first point in the second subdivided curve. Thus,
 * it is possible to pass the same array for <code>left</code>
 * and <code>right</code> and to use offsets, such as <code>rightoff</code>
 * equals (<code>leftoff</code> + 6), in order
 * to avoid allocating extra storage for this common point.
 * @param src the array holding the coordinates for the source curve
 * @param srcoff the offset into the array of the beginning of the
 * the 6 source coordinates
 * @param left the array for storing the coordinates for the first
 * half of the subdivided curve
 * @param leftoff the offset into the array of the beginning of the
 * the 6 left coordinates
 * @param right the array for storing the coordinates for the second
 * half of the subdivided curve
 * @param rightoff the offset into the array of the beginning of the
 * the 6 right coordinates
 * @since 1.7
 */
extern void Helpers_subdivideCubic(jfloat src[], jint srcoff,
                                   jfloat left[], jint leftoff,
                                   jfloat right[], jint rightoff);

extern void Helpers_subdivideCubicAt(jfloat t, jfloat src[], jint srcoff,
                                     jfloat left[], jint leftoff,
                                     jfloat right[], jint rightoff);

extern void Helpers_subdivideQuad(jfloat src[], jint srcoff,
                                  jfloat left[], jint leftoff,
                                  jfloat right[], jint rightoff);

extern void Helpers_subdivideQuadAt(jfloat t, jfloat src[], jint srcoff,
                                    jfloat left[], jint leftoff,
                                    jfloat right[], jint rightoff);

extern void Helpers_subdivideAt(jfloat t, jfloat src[], jint srcoff,
                                jfloat left[], jint leftoff,
                                jfloat right[], jint rightoff, jint size);

#ifdef __cplusplus
}
#endif

#endif /* HELPERS_H */

