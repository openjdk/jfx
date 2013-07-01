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

#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "PathConsumer.h"

#include "Helpers.h"
#include "Dasher.h"

static void LIinitializeIterationOnCurve(LengthIterator *pLI, jfloat pts[], jint type);
static jfloat LInext(LengthIterator *pLI, const jfloat len);
static jfloat LIlastSegLen(LengthIterator *pLI);
static void LIgoLeft(LengthIterator *pLI);
static void LIgoToNextLeaf(LengthIterator *pLI);
static jfloat LIonLeaf(LengthIterator *pLI);

/**
 * The <code>Dasher</code> class takes a series of linear commands
 * (<code>moveTo</code>, <code>lineTo</code>, <code>close</code> and
 * <code>end</code>) and breaks them into smaller segments according to a
 * dash pattern array and a starting dash phase.
 *
 * <p> Issues: in J2Se, a zero length dash segment as drawn as a very
 * short dash, whereas Pisces does not draw anything.  The PostScript
 * semantics are unclear.
 *
 */

static MoveToFunc       Dasher_MoveTo;
static LineToFunc       Dasher_LineTo;
static QuadToFunc       Dasher_QuadTo;
static CurveToFunc      Dasher_CurveTo;
static ClosePathFunc    Dasher_ClosePath;
static PathDoneFunc     Dasher_PathDone;

#define this (*(Dasher *)pDasher)

    /**
     * Constructs a <code>Dasher</code>.
     *
     * @param out an output <code>PathConsumer2D</code>.
     * @param dash an array of <code>float</code>s containing the dash pattern
     * @param phase a <code>float</code> containing the dash phase
    public Dasher(PathConsumer2D out, float[] dash, float phase) {
        this(out);
        reset(dash, phase);
    }

    public Dasher(PathConsumer2D out) {
        this.out = out;

        // we need curCurvepts to be able to contain 2 curves because when
        // dashing curves, we need to subdivide it
        curCurvepts = new float[8 * 2];
    }
     */

void Dasher_init(Dasher *pDasher,
                 PathConsumer *out,
                 jfloat dash[], jint numdashes,
                 jfloat phase)
{
    memset(pDasher, 0, sizeof(Dasher));
    PathConsumer_init(&this.consumer,
                      Dasher_MoveTo,
                      Dasher_LineTo,
                      Dasher_QuadTo,
                      Dasher_CurveTo,
                      Dasher_ClosePath,
                      Dasher_PathDone);

    this.firstSegmentsBufferSIZE = 7;
    this.firstSegmentsBuffer = new_float(this.firstSegmentsBufferSIZE);
    this.firstSegidx = 0;

    this.out = out;
    Dasher_reset(pDasher, dash, numdashes, phase);
}

void Dasher_reset(Dasher *pDasher, jfloat dash[], jint ndashes, jfloat phase) {
    jint sidx;
    jfloat d;

    if (phase < 0) {
        phase = 0;
//        throw new IllegalArgumentException("phase < 0 !");
    }

    // Normalize so 0 <= phase < dash[0]
    sidx = 0;
    this.dashOn = JNI_TRUE;
    while (phase >= (d = dash[sidx])) {
        phase -= d;
        sidx = (sidx + 1) % ndashes;
        this.dashOn = !this.dashOn;
    }

    this.dash = dash;
    this.numdashes = ndashes;
    this.startPhase = this.phase = phase;
    this.startDashOn = this.dashOn;
    this.startIdx = sidx;
    this.starting = JNI_TRUE;
}

void Dasher_destroy(Dasher *pDasher) {
    free(pDasher->firstSegmentsBuffer);
    pDasher->firstSegmentsBuffer = NULL;
    pDasher->firstSegmentsBufferSIZE = 0;
}

static void emitSeg(PathConsumer *pDasher, jfloat buf[], jint off, jint type) {
    switch (type) {
    case 8:
        this.out->curveTo(this.out,
                          buf[off+0], buf[off+1],
                          buf[off+2], buf[off+3],
                          buf[off+4], buf[off+5]);
        break;
    case 6:
        this.out->quadTo(this.out,
                         buf[off+0], buf[off+1],
                         buf[off+2], buf[off+3]);
        break;
    case 4:
        this.out->lineTo(this.out, buf[off], buf[off+1]);
    }
}

static void emitFirstSegments(PathConsumer *pDasher) {
    jint i;
    for (i = 0; i < this.firstSegidx; ) {
        emitSeg(pDasher, this.firstSegmentsBuffer, i+1, (jint) this.firstSegmentsBuffer[i]);
        i += (((jint) this.firstSegmentsBuffer[i]) - 1);
    }
    this.firstSegidx = 0;
}

// precondition: pts must be in relative coordinates (relative to x0,y0)
// fullCurve is true iff the curve in pts has not been split.
static void goTo(PathConsumer *pDasher, jfloat pts[], jint off, jint type) {
    jfloat x = pts[off + type - 4];
    jfloat y = pts[off + type - 3];
    if (this.dashOn) {
        if (this.starting) {
            if (this.firstSegmentsBufferSIZE < this.firstSegidx + (type-1)) {
                jint newSize = (this.firstSegidx + (type-1)) * 2;
                jfloat *newSegs = new_float(newSize);
                System_arraycopy(this.firstSegmentsBuffer, 0, newSegs, 0, this.firstSegidx);
                free(this.firstSegmentsBuffer);
                this.firstSegmentsBuffer = newSegs;
                this.firstSegmentsBufferSIZE = newSize;
            }
            this.firstSegmentsBuffer[this.firstSegidx++] = (jfloat) type;
            System_arraycopy(pts, off, this.firstSegmentsBuffer, this.firstSegidx, type - 2);
            this.firstSegidx += type - 2;
        } else {
            if (this.needsMoveTo) {
                this.out->moveTo(this.out, this.x0, this.y0);
                this.needsMoveTo = JNI_FALSE;
            }
            emitSeg(pDasher, pts, off, type);
        }
    } else {
        this.starting = JNI_FALSE;
        this.needsMoveTo = JNI_TRUE;
    }
    this.x0 = x;
    this.y0 = y;
}

static void Dasher_MoveTo(PathConsumer *pDasher, jfloat newx0, jfloat newy0) {
    if (this.firstSegidx > 0) {
        this.out->moveTo(this.out, this.sx, this.sy);
        emitFirstSegments(pDasher);
    }
    this.needsMoveTo = JNI_TRUE;
    this.idx = this.startIdx;
    this.dashOn = this.startDashOn;
    this.phase = this.startPhase;
    this.sx = this.x0 = newx0;
    this.sy = this.y0 = newy0;
    this.starting = JNI_TRUE;
}

static void Dasher_LineTo(PathConsumer *pDasher, jfloat x1, jfloat y1) {
    jfloat cx, cy;
    jfloat dx = x1 - this.x0;
    jfloat dy = y1 - this.y0;

    jfloat len = (jfloat) sqrt(dx*dx + dy*dy);

    if (len == 0) {
        return;
    }

    // The scaling factors needed to get the dx and dy of the
    // transformed dash segments.
    cx = dx / len;
    cy = dy / len;

    while (1) {
        jfloat dashdx, dashdy;
        jfloat leftInThisDashSegment = this.dash[this.idx] - this.phase;
        if (len <= leftInThisDashSegment) {
            this.curCurvepts[0] = x1;
            this.curCurvepts[1] = y1;
            goTo(pDasher, this.curCurvepts, 0, 4);
            // Advance phase within current dash segment
            this.phase += len;
            if (len == leftInThisDashSegment) {
                this.phase = 0.0f;
                this.idx = (this.idx + 1) % this.numdashes;
                this.dashOn = !this.dashOn;
            }
            return;
        }

        dashdx = this.dash[this.idx] * cx;
        dashdy = this.dash[this.idx] * cy;
        if (this.phase == 0) {
            this.curCurvepts[0] = this.x0 + dashdx;
            this.curCurvepts[1] = this.y0 + dashdy;
        } else {
            jfloat p = leftInThisDashSegment / this.dash[this.idx];
            this.curCurvepts[0] = this.x0 + p * dashdx;
            this.curCurvepts[1] = this.y0 + p * dashdy;
        }

        goTo(pDasher, this.curCurvepts, 0, 4);

        len -= leftInThisDashSegment;
        // Advance to next dash segment
        this.idx = (this.idx + 1) % this.numdashes;
        this.dashOn = !this.dashOn;
        this.phase = 0;
    }
}

static jboolean pointCurve(jfloat curve[], jint type) {
    jint i;
    for (i = 2; i < type; i++) {
        if (curve[i] != curve[i-2]) {
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

//    private LengthIterator li = null;

// preconditions: curCurvepts must be an array of length at least 2 * type,
// that contains the curve we want to dash in the first type elements
static void somethingTo(PathConsumer *pDasher, jint type) {
    jint curCurveoff;
    jfloat lastSplitT;
    jfloat t;
    jfloat leftInThisDashSegment;

    if (pointCurve(this.curCurvepts, type)) {
        return;
    }
    LIinitializeIterationOnCurve(&this.li, this.curCurvepts, type);

    curCurveoff = 0; // initially the current curve is at curCurvepts[0...type]
    lastSplitT = 0;
    t = 0;
    leftInThisDashSegment = this.dash[this.idx] - this.phase;
    while ((t = LInext(&this.li, leftInThisDashSegment)) < 1) {
        if (t != 0) {
            Helpers_subdivideAt((t - lastSplitT) / (1 - lastSplitT),
                                this.curCurvepts, curCurveoff,
                                this.curCurvepts, 0,
                                this.curCurvepts, type, type);
            lastSplitT = t;
            goTo(pDasher, this.curCurvepts, 2, type);
            curCurveoff = type;
        }
        // Advance to next dash segment
        this.idx = (this.idx + 1) % this.numdashes;
        this.dashOn = !this.dashOn;
        this.phase = 0;
        leftInThisDashSegment = this.dash[this.idx];
    }
    goTo(pDasher, this.curCurvepts, curCurveoff+2, type);
    this.phase += LIlastSegLen(&this.li);
    if (this.phase >= this.dash[this.idx]) {
        this.phase = 0.0f;
        this.idx = (this.idx + 1) % this.numdashes;
        this.dashOn = !this.dashOn;
    }
}

static void Dasher_CurveTo(PathConsumer *pDasher,
                           jfloat x1, jfloat y1,
                           jfloat x2, jfloat y2,
                           jfloat x3, jfloat y3)
{
    this.curCurvepts[0] = this.x0;   this.curCurvepts[1] = this.y0;
    this.curCurvepts[2] = x1;        this.curCurvepts[3] = y1;
    this.curCurvepts[4] = x2;        this.curCurvepts[5] = y2;
    this.curCurvepts[6] = x3;        this.curCurvepts[7] = y3;
    somethingTo(pDasher, 8);
}

static void Dasher_QuadTo(PathConsumer *pDasher,
                          jfloat x1, jfloat y1,
                          jfloat x2, jfloat y2)
{
    this.curCurvepts[0] = this.x0;   this.curCurvepts[1] = this.y0;
    this.curCurvepts[2] = x1;        this.curCurvepts[3] = y1;
    this.curCurvepts[4] = x2;        this.curCurvepts[5] = y2;
    somethingTo(pDasher, 6);
}

static void Dasher_ClosePath(PathConsumer *pDasher) {
    Dasher_LineTo(pDasher, this.sx, this.sy);
    if (this.firstSegidx > 0) {
        if (!this.dashOn || this.needsMoveTo) {
            this.out->moveTo(this.out, this.sx, this.sy);
        }
        emitFirstSegments(pDasher);
    }
    Dasher_MoveTo(pDasher, this.sx, this.sy);
}

static void Dasher_PathDone(PathConsumer *pDasher) {
    if (this.firstSegidx > 0) {
        this.out->moveTo(this.out, this.sx, this.sy);
        emitFirstSegments(pDasher);
    }
    this.out->pathDone(this.out);
}


/*
        public LengthIterator(jint reclimit, float err) {
            this.limit = reclimit;
            this.minTincrement = 1f / (1 << limit);
            this.ERR = err;
            this.recCurveStack = new float[reclimit+1][8];
            this.sides = new Side[reclimit];
            // if any methods are called without first initializing this object on
            // a curve, we want it to fail ASAP.
            this.nextT = Float.MAX_VALUE;
            this.lenAtNextT = Float.MAX_VALUE;
            this.lenAtLastSplit = Float.MIN_VALUE;
            this.recLevel = Integer.MIN_VALUE;
            this.lastSegLen = Float.MAX_VALUE;
            this.done = true;
        }
 */

#undef this
#define this (*((LengthIterator *) pLI))

static void LIinitializeIterationOnCurve(LengthIterator *pLI, jfloat pts[], jint type) {
    System_arraycopy(pts, 0, this.recCurveStack[0], 0, type);
    this.curveType = type;
    this.recLevel = 0;
    this.lastT = 0;
    this.lenAtLastT = 0;
    this.nextT = 0;
    this.lenAtNextT = 0;
    LIgoLeft(pLI); // initializes nextT and lenAtNextT properly
    this.lenAtLastSplit = 0;
    if (this.recLevel > 0) {
        this.sides[0] = LEFT;
        this.done = JNI_FALSE;
    } else {
        // the root of the tree is a leaf so we're done.
        this.sides[0] = RIGHT;
        this.done = JNI_TRUE;
    }
    this.lastSegLen = 0;
    this.cachedHaveLowAcceleration = -1;
    /* = {0, 0, -1, 0}*/;
    this.flatLeafCoefCache[0] = 0;
    this.flatLeafCoefCache[1] = 0;
    this.flatLeafCoefCache[2] = -1;
    this.flatLeafCoefCache[3] = 0;
}

static jboolean LIhaveLowAcceleration(LengthIterator *pLI, jfloat err) {
    if (this.cachedHaveLowAcceleration == -1) {
        const jfloat len1 = this.curLeafCtrlPolyLengths[0];
        const jfloat len2 = this.curLeafCtrlPolyLengths[1];
        // the test below is equivalent to !within(len1/len2, 1, err).
        // It is using a multiplication instead of a division, so it
        // should be a bit faster.
        if (!Helpers_within(len1, len2, err*len2)) {
            this.cachedHaveLowAcceleration = 0;
            return JNI_FALSE;
        }
        if (this.curveType == 8) {
            const jfloat len3 = this.curLeafCtrlPolyLengths[2];
            // if len1 is close to 2 and 2 is close to 3, that probably
            // means 1 is close to 3 so the second part of this test might
            // not be needed, but it doesn't hurt to include it.
            if (!(Helpers_within(len2, len3, err*len3) &&
                  Helpers_within(len1, len3, err*len3)))
            {
                this.cachedHaveLowAcceleration = 0;
                return JNI_FALSE;
            }
        }
        this.cachedHaveLowAcceleration = 1;
        return JNI_TRUE;
    }

    return (this.cachedHaveLowAcceleration == 1);
}

// returns the t value where the remaining curve should be split in
// order for the left subdivided curve to have length len. If len
// is >= than the length of the uniterated curve, it returns 1.
static jfloat LInext(LengthIterator *pLI, const jfloat len) {
    const jfloat targetLength = this.lenAtLastSplit + len;
    jfloat leaflen;
    jfloat t;
    while(this.lenAtNextT < targetLength) {
        if (this.done) {
            this.lastSegLen = this.lenAtNextT - this.lenAtLastSplit;
            return 1;
        }
        LIgoToNextLeaf(pLI);
    }
    this.lenAtLastSplit = targetLength;
    leaflen = this.lenAtNextT - this.lenAtLastT;
    t = (targetLength - this.lenAtLastT) / leaflen;

    // cubicRootsInAB is a fairly expensive call, so we just don't do it
    // if the acceleration in this section of the curve is small enough.
    if (!LIhaveLowAcceleration(pLI, 0.05f)) {
        jfloat a, b, c, d;
        jint n;
        // We flatten the current leaf along the x axis, so that we're
        // left with a, b, c which define a 1D Bezier curve. We then
        // solve this to get the parameter of the original leaf that
        // gives us the desired length.

        if (this.flatLeafCoefCache[2] < 0) {
            jfloat x = 0+this.curLeafCtrlPolyLengths[0],
                    y = x+this.curLeafCtrlPolyLengths[1];
            if (this.curveType == 8) {
                jfloat z = y + this.curLeafCtrlPolyLengths[2];
                this.flatLeafCoefCache[0] = 3*(x - y) + z;
                this.flatLeafCoefCache[1] = 3*(y - 2*x);
                this.flatLeafCoefCache[2] = 3*x;
                this.flatLeafCoefCache[3] = -z;
            } else if (this.curveType == 6) {
                this.flatLeafCoefCache[0] = 0.0f;
                this.flatLeafCoefCache[1] = y - 2*x;
                this.flatLeafCoefCache[2] = 2*x;
                this.flatLeafCoefCache[3] = -y;
            }
        }
        a = this.flatLeafCoefCache[0];
        b = this.flatLeafCoefCache[1];
        c = this.flatLeafCoefCache[2];
        d = t*this.flatLeafCoefCache[3];

        // we use cubicRootsInAB here, because we want only roots in 0, 1,
        // and our quadratic root finder doesn't filter, so it's just a
        // matter of convenience.
        n = Helpers_cubicRootsInAB(a, b, c, d, this.nextRoots, 0, 0, 1);
        if (n == 1 && !Math_isnan(this.nextRoots[0])) {
            t = this.nextRoots[0];
        }
    }
    // t is relative to the current leaf, so we must make it a valid parameter
    // of the original curve.
    t = t * (this.nextT - this.lastT) + this.lastT;
    if (t >= 1) {
        t = 1;
        this.done = JNI_TRUE;
    }
    // even if done = true, if we're here, that means targetLength
    // is equal to, or very, very close to the total length of the
    // curve, so lastSegLen won't be too high. In cases where len
    // overshoots the curve, this method will exit in the while
    // loop, and lastSegLen will still be set to the right value.
    this.lastSegLen = len;
    return t;
}

static jfloat LIlastSegLen(LengthIterator *pLI) {
    return this.lastSegLen;
}

// go to the next leaf (in an inorder traversal) in the recursion tree
// preconditions: must be on a leaf, and that leaf must not be the root.
static void LIgoToNextLeaf(LengthIterator *pLI) {
    // We must go to the first ancestor node that has an unvisited
    // right child.
    this.recLevel--;
    while(this.sides[this.recLevel] == RIGHT) {
        if (this.recLevel == 0) {
            this.done = JNI_TRUE;
            return;
        }
        this.recLevel--;
    }

    this.sides[this.recLevel] = RIGHT;
    System_arraycopy(this.recCurveStack[this.recLevel], 0,
                     this.recCurveStack[this.recLevel+1], 0, this.curveType);
    this.recLevel++;
    LIgoLeft(pLI);
}

// go to the leftmost node from the current node. Return its length.
static void LIgoLeft(LengthIterator *pLI) {
    jfloat len = LIonLeaf(pLI);
    if (len >= 0) {
        this.lastT = this.nextT;
        this.lenAtLastT = this.lenAtNextT;
        this.nextT += (1 << (REC_LIMIT - this.recLevel)) * MIN_T_INCREMENT;
        this.lenAtNextT += len;
        // invalidate caches
        this.flatLeafCoefCache[2] = -1;
        this.cachedHaveLowAcceleration = -1;
    } else {
        Helpers_subdivide(this.recCurveStack[this.recLevel], 0,
                          this.recCurveStack[this.recLevel+1], 0,
                          this.recCurveStack[this.recLevel], 0, this.curveType);
        this.sides[this.recLevel] = LEFT;
        this.recLevel++;
        LIgoLeft(pLI);
    }
}

// this is a bit of a hack. It returns -1 if we're not on a leaf, and
// the length of the leaf if we are on a leaf.
static jfloat LIonLeaf(LengthIterator *pLI) {
    jfloat *curve = this.recCurveStack[this.recLevel];
    jfloat polyLen = 0;
    jfloat lineLen;

    jfloat x0 = curve[0], y0 = curve[1];
    jint i;
    for (i = 2; i < this.curveType; i += 2) {
        const jfloat x1 = curve[i], y1 = curve[i+1];
        const jfloat len = Helpers_linelen(x0, y0, x1, y1);
        polyLen += len;
        this.curLeafCtrlPolyLengths[i/2 - 1] = len;
        x0 = x1;
        y0 = y1;
    }

    lineLen = Helpers_linelen(curve[0], curve[1], curve[this.curveType-2], curve[this.curveType-1]);
    if (polyLen - lineLen < ERR || this.recLevel == REC_LIMIT) {
        return (polyLen + lineLen)/2;
    }
    return -1;
}
