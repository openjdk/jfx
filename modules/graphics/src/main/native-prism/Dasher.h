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

#ifndef DASHER_H
#define DASHER_H

#ifdef __cplusplus
extern "C" {
#endif

enum Side {LEFT, RIGHT};

typedef struct {
    // Objects of this class are used to iterate through curves. They return
    // t values where the left side of the curve has a specified length.
    // It does this by subdividing the input curve until a certain error
    // condition has been met. A recursive subdivision procedure would
    // return as many as 1<<limit curves, but this is an iterator and we
    // don't need all the curves all at once, so what we carry out a
    // lazy inorder traversal of the recursion tree (meaning we only move
    // through the tree when we need the next subdivided curve). This saves
    // us a lot of memory because at any one time we only need to store
    // limit+1 curves - one for each level of the tree + 1.
    // NOTE: the way we do things here is not enough to traverse a general
    // tree; however, the trees we are interested in have the property that
    // every non leaf node has exactly 2 children
    //    private static class LengthIterator {
    #define REC_LIMIT 4
    #define ERR .01f
    #define MIN_T_INCREMENT (1.0f / (1 << REC_LIMIT))

    // Holds the curves at various levels of the recursion. The root
    // (i.e. the original curve) is at recCurveStack[0] (but then it
    // gets subdivided, the left half is put at 1, so most of the time
    // only the right half of the original curve is at 0)
    jfloat recCurveStack[REC_LIMIT+1][8];
    // sides[i] indicates whether the node at level i+1 in the path from
    // the root to the current leaf is a left or right child of its parent.
    enum Side sides[REC_LIMIT];
    jint curveType;
//    const jint limit = REC_LIMIT;
//    const jfloat minTincrement = 1.0f / (1 << REC_LIMIT);
    // lastT and nextT delimit the current leaf.
    jfloat nextT;
    jfloat lenAtNextT;
    jfloat lastT;
    jfloat lenAtLastT;
    jfloat lenAtLastSplit;
    jfloat lastSegLen;
    // the current level in the recursion tree. 0 is the root. limit
    // is the deepest possible leaf.
    jint recLevel;
    jboolean done;

    // the lengths of the lines of the control polygon. Only its first
    // curveType/2 - 1 elements are valid. This is an optimization. See
    // next(float) for more detail.
    jfloat curLeafCtrlPolyLengths[3];

    // 0 == false, 1 == true, -1 == invalid cached value.
    jint cachedHaveLowAcceleration;

    // we want to avoid allocations/gc so we keep this array so we
    // can put roots in it,
    jfloat nextRoots[4];

    // caches the coefficients of the current leaf in its flattened
    // form (see inside next() for what that means). The cache is
    // invalid when it's third element is negative, since in any
    // valid flattened curve, this would be >= 0.
    jfloat flatLeafCoefCache[4];
} LengthIterator;

typedef struct {
    PathConsumer consumer;
    PathConsumer *out;

    jfloat *dash;
    jint numdashes;
    jfloat startPhase;
    jboolean startDashOn;
    jint startIdx;

    jboolean starting;
    jboolean needsMoveTo;

    jint idx;
    jboolean dashOn;
    jfloat phase;

    jfloat sx, sy;
    jfloat x0, y0;

    // temporary storage for the current curve
    jfloat curCurvepts[8 * 2];

    // We don't emit the first dash right away. If we did, caps would be
    // drawn on it, but we need joins to be drawn if there's a closePath()
    // So, we store the path elements that make up the first dash in the
    // buffer below.
    jint firstSegmentsBufferSIZE /* = 7*/;
    jfloat *firstSegmentsBuffer /* = malloc(firstSegmentsBufferSIZE * sizeof(jfloat))*/;
    jint firstSegidx /* = 0*/;

    LengthIterator li;
} Dasher;

void Dasher_init(Dasher *pDasher,
                 PathConsumer *out,
                 jfloat dash[], jint numdashes,
                 jfloat phase);

void Dasher_reset(Dasher *pDasher, jfloat dash[], jint ndashes, jfloat phase);

void Dasher_destroy(Dasher *pDasher);

#ifdef __cplusplus
}
#endif

#endif /* DASHER_H */

