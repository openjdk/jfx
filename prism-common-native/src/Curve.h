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

#ifndef CURVE_H
#define CURVE_H

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    jfloat ax, ay, bx, by, cx, cy, dx, dy;
    jfloat dax, day, dbx, dby;
} Curve;

extern void Curve_set(Curve *pCurve, jfloat points[], jint type);

extern void Curve_setquad(Curve *pCurve,
                          jfloat x1, jfloat y1,
                          jfloat x2, jfloat y2,
                          jfloat x3, jfloat y3);

extern void Curve_setcubic(Curve *pCurve,
                           jfloat x1, jfloat y1,
                           jfloat x2, jfloat y2,
                           jfloat x3, jfloat y3,
                           jfloat x4, jfloat y4);

extern jint Curve_dxRoots(Curve *pCurve, jfloat roots[], jint off);

extern jint Curve_dyRoots(Curve *pCurve, jfloat roots[], jint off);

extern jint Curve_infPoints(Curve *pCurve, jfloat pts[], jint off);

extern jint Curve_rootsOfROCMinusW(Curve *pCurve,
                                   jfloat roots[], jint off,
                                  const jfloat w, const jfloat err);

#ifdef __cplusplus
}
#endif

#endif /* CURVE_H */

