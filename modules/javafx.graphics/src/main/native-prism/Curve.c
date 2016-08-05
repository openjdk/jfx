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
#include <jni.h>

#include "Curve.h"
#include "Helpers.h"

//final class Curve {

#define this (*((Curve *) pCurve))

void Curve_set(Curve *pCurve, jfloat points[], jint type) {
    switch(type) {
    case 8:
        Curve_setcubic(pCurve,
                       points[0], points[1],
                       points[2], points[3],
                       points[4], points[5],
                       points[6], points[7]);
        break;
    case 6:
        Curve_setquad(pCurve,
                      points[0], points[1],
                      points[2], points[3],
                      points[4], points[5]);
        break;
//    default:
//        throw new InternalError("Curves can only be cubic or quadratic");
    }
}

void Curve_setcubic(Curve *pCurve,
                    jfloat x1, jfloat y1,
                    jfloat x2, jfloat y2,
                    jfloat x3, jfloat y3,
                    jfloat x4, jfloat y4)
{
    this.ax = 3 * (x2 - x3) + x4 - x1;
    this.ay = 3 * (y2 - y3) + y4 - y1;
    this.bx = 3 * (x1 - 2 * x2 + x3);
    this.by = 3 * (y1 - 2 * y2 + y3);
    this.cx = 3 * (x2 - x1);
    this.cy = 3 * (y2 - y1);
    this.dx = x1;
    this.dy = y1;
    this.dax = 3 * this.ax; this.day = 3 * this.ay;
    this.dbx = 2 * this.bx; this.dby = 2 * this.by;
}

void Curve_setquad(Curve *pCurve,
                   jfloat x1, jfloat y1,
                   jfloat x2, jfloat y2,
                   jfloat x3, jfloat y3)
{
    this.ax = this.ay = 0.0f;

    this.bx = x1 - 2 * x2 + x3;
    this.by = y1 - 2 * y2 + y3;
    this.cx = 2 * (x2 - x1);
    this.cy = 2 * (y2 - y1);
    this.dx = x1;
    this.dy = y1;
    this.dax = 0; this.day = 0;
    this.dbx = 2 * this.bx; this.dby = 2 * this.by;
}

static jfloat xat(Curve *pCurve, jfloat t) {
    return t * (t * (t * this.ax + this.bx) + this.cx) + this.dx;
}
static jfloat yat(Curve *pCurve, jfloat t) {
    return t * (t * (t * this.ay + this.by) + this.cy) + this.dy;
}

static jfloat dxat(Curve *pCurve, jfloat t) {
    return t * (t * this.dax + this.dbx) + this.cx;
}
static jfloat dyat(Curve *pCurve, jfloat t) {
    return t * (t * this.day + this.dby) + this.cy;
}

jint Curve_dxRoots(Curve *pCurve, jfloat roots[], jint off) {
    return Helpers_quadraticRoots(this.dax, this.dbx, this.cx, roots, off);
}
jint Curve_dyRoots(Curve *pCurve, jfloat roots[], jint off) {
    return Helpers_quadraticRoots(this.day, this.dby, this.cy, roots, off);
}

jint Curve_infPoints(Curve *pCurve, jfloat pts[], jint off) {
    // inflection point at t if -f'(t)x*f''(t)y + f'(t)y*f''(t)x == 0
    // Fortunately, this turns out to be quadratic, so there are at
    // most 2 inflection points.
    const jfloat a = this.dax * this.dby - this.dbx * this.day;
    const jfloat b = 2 * (this.cy * this.dax - this.day * this.cx);
    const jfloat c = this.cy * this.dbx - this.cx * this.dby;

    return Helpers_quadraticRoots(a, b, c, pts, off);
}

// finds points where the first and second derivative are
// perpendicular. This happens when g(t) = f'(t)*f''(t) == 0 (where
// * is a dot product). Unfortunately, we have to solve a cubic.
static jint perpendiculardfddf(Curve *pCurve, jfloat pts[], jint off) {
//    assert pts.length >= off + 4;

    // these are the coefficients of some multiple of g(t) (not g(t),
    // because the roots of a polynomial are not changed after multiplication
    // by a constant, and this way we save a few multiplications).
    const jfloat a = 2*(this.dax*this.dax + this.day*this.day);
    const jfloat b = 3*(this.dax*this.dbx + this.day*this.dby);
    const jfloat c = 2*(this.dax*this.cx + this.day*this.cy)
                      + this.dbx*this.dbx + this.dby*this.dby;
    const jfloat d = this.dbx*this.cx + this.dby*this.cy;
    return Helpers_cubicRootsInAB(a, b, c, d, pts, off, 0.0f, 1.0f);
}

static jfloat ROCsq(Curve *pCurve, const jfloat t);
static jfloat falsePositionROCsqMinusX(Curve *pCurve,
                                       jfloat x0, jfloat x1,
                                       const jfloat x, const jfloat err);
static jboolean sameSign(double x, double y);

// Tries to find the roots of the function ROC(t)-w in [0, 1). It uses
// a variant of the false position algorithm to find the roots. False
// position requires that 2 initial values x0,x1 be given, and that the
// function must have opposite signs at those values. To find such
// values, we need the local extrema of the ROC function, for which we
// need the roots of its derivative; however, it's harder to find the
// roots of the derivative in this case than it is to find the roots
// of the original function. So, we find all points where this curve's
// first and second derivative are perpendicular, and we pretend these
// are our local extrema. There are at most 3 of these, so we will check
// at most 4 sub-intervals of (0,1). ROC has asymptotes at inflection
// points, so roc-w can have at least 6 roots. This shouldn't be a
// problem for what we're trying to do (draw a nice looking curve).
jint Curve_rootsOfROCMinusW(Curve *pCurve,
                            jfloat roots[], jint off,
                            const jfloat w, const jfloat err)
{
    // no OOB exception, because by now off<=6, and roots.length >= 10
//    assert off <= 6 && roots.length >= 10;
    jint i;
    jint ret = off;
    jint numPerpdfddf = perpendiculardfddf(pCurve, roots, off);
    jfloat t0 = 0, ft0 = ROCsq(pCurve, t0) - w*w;
    roots[off + numPerpdfddf] = 1.0f; // always check interval end points
    numPerpdfddf++;
    for (i = off; i < off + numPerpdfddf; i++) {
        jfloat t1 = roots[i], ft1 = ROCsq(pCurve, t1) - w*w;
        if (ft0 == 0.0f) {
            roots[ret++] = t0;
        } else if (ft1 * ft0 < 0.0f) { // have opposite signs
            // (ROC(t)^2 == w^2) == (ROC(t) == w) is true because
            // ROC(t) >= 0 for all t.
            roots[ret++] = falsePositionROCsqMinusX(pCurve, t0, t1, w*w, err);
        }
        t0 = t1;
        ft0 = ft1;
    }

    return ret - off;
}

static jfloat eliminateInf(jfloat x) {
    return (x > BIGGEST_FLOAT ? BIGGEST_FLOAT :
            (x < -BIGGEST_FLOAT ? -BIGGEST_FLOAT : x));
//    return (x == Float.POSITIVE_INFINITY ? Float.MAX_VALUE :
//        (x == Float.NEGATIVE_INFINITY ? Float.MIN_VALUE : x));
}

// A slight modification of the false position algorithm on wikipedia.
// This only works for the ROCsq-x functions. It might be nice to have
// the function as an argument, but that would be awkward in java6.
// NOTE: It is something to consider for java8 (or whenever lambda
// expressions make it into the language), depending on how closures
// and turn out. Same goes for the newton's method
// algorithm in Helpers.java
static jfloat falsePositionROCsqMinusX(Curve *pCurve,
                                       jfloat x0, jfloat x1,
                                       const jfloat x, const jfloat err)
{
    const jint iterLimit = 100;
    jint side = 0;
    jfloat t = x1, ft = eliminateInf(ROCsq(pCurve, t) - x);
    jfloat s = x0, fs = eliminateInf(ROCsq(pCurve, s) - x);
    jfloat r = s, fr;
    jint i;
    for (i = 0; i < iterLimit && fabs(t - s) > err * fabs(t + s); i++) {
        r = (fs * t - ft * s) / (fs - ft);
        fr = ROCsq(pCurve, r) - x;
        if (sameSign(fr, ft)) {
            ft = fr; t = r;
            if (side < 0) {
                fs /= (1 << (-side));
                side--;
            } else {
                side = -1;
            }
        } else if (fr * fs > 0) {
            fs = fr; s = r;
            if (side > 0) {
                ft /= (1 << side);
                side++;
            } else {
                side = 1;
            }
        } else {
            break;
        }
    }
    return r;
}

static jboolean sameSign(double x, double y) {
    // another way is to test if x*y > 0. This is bad for small x, y.
    return (x < 0 && y < 0) || (x > 0 && y > 0);
}

// returns the radius of curvature squared at t of this curve
// see http://en.wikipedia.org/wiki/Radius_of_curvature_(applications)
static jfloat ROCsq(Curve *pCurve, const jfloat t) {
    // dx=xat(t) and dy=yat(t). These calls have been inlined for efficiency
    const jfloat dx = t * (t * this.dax + this.dbx) + this.cx;
    const jfloat dy = t * (t * this.day + this.dby) + this.cy;
    const jfloat ddx = 2 * this.dax * t + this.dbx;
    const jfloat ddy = 2 * this.day * t + this.dby;
    const jfloat dx2dy2 = dx*dx + dy*dy;
    const jfloat ddx2ddy2 = ddx*ddx + ddy*ddy;
    const jfloat ddxdxddydy = ddx*dx + ddy*dy;
    return dx2dy2*((dx2dy2*dx2dy2) / (dx2dy2 * ddx2ddy2 - ddxdxddydy*ddxdxddydy));
}

/*
    // curve to be broken should be in pts
    // this will change the contents of pts but not Ts
    // NOTE: There's no reason for Ts to be an array. All we need is a sequence
    // of t values at which to subdivide. An array statisfies this condition,
    // but is unnecessarily restrictive. Ts should be an Iterator<Float> instead.
    // Doing this will also make dashing easier, since we could easily make
    // LengthIterator an Iterator<Float> and feed it to this function to simplify
    // the loop in Dasher.somethingTo.
    static Iterator<Integer> breakPtsAtTs(const jfloat[] pts, const int type,
                                          const jfloat[] Ts, const int numTs)
    {
        assert pts.length >= 2*type && numTs <= Ts.length;
        return new Iterator<Integer>() {
            // these prevent object creation and destruction during autoboxing.
            // Because of this, the compiler should be able to completely
            // eliminate the boxing costs.
            const Integer i0 = 0;
            const Integer itype = type;
            int nextCurveIdx = 0;
            Integer curCurveOff = i0;
            jfloat prevT = 0;

            @Override public boolean hasNext() {
                return nextCurveIdx < numTs + 1;
            }

            @Override public Integer next() {
                Integer ret;
                if (nextCurveIdx < numTs) {
                    jfloat curT = Ts[nextCurveIdx];
                    jfloat splitT = (curT - prevT) / (1 - prevT);
                    Helpers.subdivideAt(splitT,
                                        pts, curCurveOff,
                                        pts, 0,
                                        pts, type, type);
                    prevT = curT;
                    ret = i0;
                    curCurveOff = itype;
                } else {
                    ret = curCurveOff;
                }
                nextCurveIdx++;
                return ret;
            }

            @Override public void remove() {}
        };
    }
 */
