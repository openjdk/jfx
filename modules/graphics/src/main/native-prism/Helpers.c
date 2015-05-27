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

#include <jni.h>
#ifdef ANDROID_NDK
#include <stddef.h>
#endif

#include "math.h"
#include "Helpers.h"
#include "PathConsumer.h"

#ifdef __APPLE__

#include <TargetConditionals.h>

#if TARGET_OS_IPHONE /* iOS */

JNIEXPORT jint JNICALL
JNI_OnLoad_prism_common(JavaVM* vm, void* reserved) {
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}

#endif
#endif

void PathConsumer_init(PathConsumer *pConsumer,
                       MoveToFunc       *moveTo,
                       LineToFunc       *lineTo,
                       QuadToFunc       *quadTo,
                       CurveToFunc      *curveTo,
                       ClosePathFunc    *closePath,
                       PathDoneFunc     *pathDone)
{
    pConsumer->moveTo = moveTo;
    pConsumer->lineTo = lineTo;
    pConsumer->quadTo = quadTo;
    pConsumer->curveTo = curveTo;
    pConsumer->closePath = closePath;
    pConsumer->pathDone = pathDone;
}

// Usable AlmostEqual function
// See http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm
jboolean Helpers_withinULP(const jfloat A, const jfloat B, const int maxUlps) {
    // Make sure maxUlps is non-negative and small enough that the
    // default NAN won't compare as equal to anything.
//    assert(maxUlps > 0 && maxUlps < 4 * 1024 * 1024);
    jint aInt, bInt;

    // Make aInt lexicographically ordered as a twos-complement int
    // This cast can induce "false positive" warnings from various compilers
    // or bug checking tools, but is correct as sizeof(jint) == sizeof(jfloat)
    aInt = *((jint *) &A);
    if (aInt < 0) {
        aInt = 0x80000000 - aInt;
    }

    // Make bInt lexicographically ordered as a twos-complement int
    // This cast can induce "false positive" warnings from various compilers
    // or bug checking tools, but is correct as sizeof(jint) == sizeof(jfloat)
    bInt = *((jint *) &B);
    if (bInt < 0) {
        bInt = 0x80000000 - bInt;
    }

    // aInt,bInt are in the range [-0x7fffffff, +0x7fffffff]
    // assuming maxUlps is much smaller than 0x7fffffff
    // (<negative number> + maxUlps) will never overflow
    // (<positive number> - maxUlps) will never overflow
    if (aInt < bInt) {
        return (aInt < 0) ? aInt + maxUlps >= bInt : bInt - maxUlps <= aInt;
    } else {
        return (bInt < 0) ? bInt + maxUlps >= aInt : aInt - maxUlps <= bInt;
    }
}

jboolean Helpers_within(const jfloat x, const jfloat y, const jfloat err) {
    const jfloat d = y - x;
    return (d <= err && d >= -err);
}

jboolean Helpers_withind(const double x, const double y, const double err) {
    const double d = y - x;
    return (d <= err && d >= -err);
}

jint Helpers_quadraticRoots(const jfloat a, const jfloat b, const jfloat c,
                            jfloat zeroes[], const jint off)
{
    jint ret = off;
    jfloat t;
    if (a != 0.0f) {
        const jfloat dis = b*b - 4*a*c;
        if (dis > 0) {
            const jfloat sqrtDis = (jfloat) sqrt(dis);
            // depending on the sign of b we use a slightly different
            // algorithm than the traditional one to find one of the roots
            // so we can avoid adding numbers of different signs (which
            // might result in loss of precision).
            if (b >= 0) {
                zeroes[ret++] = (2 * c) / (-b - sqrtDis);
                zeroes[ret++] = (-b - sqrtDis) / (2 * a);
            } else {
                zeroes[ret++] = (-b + sqrtDis) / (2 * a);
                zeroes[ret++] = (2 * c) / (-b + sqrtDis);
            }
        } else if (dis == 0.0f) {
            t = (-b) / (2 * a);
            zeroes[ret++] = t;
        }
    } else {
        if (b != 0.0f) {
            t = (-c) / b;
            zeroes[ret++] = t;
        }
    }
    return ret - off;
}

static double Math_cbrt(double v) {
    if (v < 0) {
        return -pow(-v, 1.0/3.0);
    } else {
        return pow(v, 1.0/3.0);
    }
}

// find the roots of g(t) = d*t^3 + a*t^2 + b*t + c in [A,B)
jint Helpers_cubicRootsInAB(jfloat d, jfloat a, jfloat b, jfloat c,
                            jfloat pts[], const jint off,
                            const jfloat A, const jfloat B)
{
    double sq_A, p, q;
    double cb_p, D;
    jint num;
    jfloat sub;
    jint i;

    if (d == 0) {
        jint num = Helpers_quadraticRoots(a, b, c, pts, off);
        return Helpers_filterOutNotInAB(pts, off, num, A, B) - off;
    }
    // From Graphics Gems:
    // http://tog.acm.org/resources/GraphicsGems/gems/Roots3And4.c
    // (also from awt.geom.CubicCurve2D. But here we don't need as
    // much accuracy and we don't want to create arrays so we use
    // our own customized version).

    /* normal form: x^3 + ax^2 + bx + c = 0 */
    a /= d;
    b /= d;
    c /= d;

    //  substitute x = y - A/3 to eliminate quadratic term:
    //     x^3 +Px + Q = 0
    //
    // Since we actually need P/3 and Q/2 for all of the
    // calculations that follow, we will calculate
    // p = P/3
    // q = Q/2
    // instead and use those values for simplicity of the code.
    sq_A = a * a;
    p = 1.0/3 * (-1.0/3 * sq_A + b);
    q = 1.0/2 * (2.0/27 * a * sq_A - 1.0/3 * a * b + c);

    /* use Cardano's formula */

    cb_p = p * p * p;
    D = q * q + cb_p;

    if (D < 0) {
        // see: http://en.wikipedia.org/wiki/Cubic_function#Trigonometric_.28and_hyperbolic.29_method
        const double phi = 1.0/3 * acos(-q / sqrt(-cb_p));
        const double t = 2 * sqrt(-p);

        pts[ off+0 ] =  (jfloat)( t * cos(phi));
        pts[ off+1 ] =  (jfloat)(-t * cos(phi + PI / 3));
        pts[ off+2 ] =  (jfloat)(-t * cos(phi - PI / 3));
        num = 3;
    } else {
        const double sqrt_D = sqrt(D);
        const double u = Math_cbrt(sqrt_D - q);
        const double v = - Math_cbrt(sqrt_D + q);

        pts[ off ] = (jfloat)(u + v);
        num = 1;

        if (Helpers_withind(D, 0, 1e-8)) {
            pts[off+1] = -(pts[off] / 2);
            num = 2;
        }
    }

    sub = 1.0f/3 * a;

    for (i = 0; i < num; ++i) {
        pts[ off+i ] -= sub;
    }

    return Helpers_filterOutNotInAB(pts, off, num, A, B) - off;
}

// These use a hardcoded factor of 2 for increasing sizes. Perhaps this
// should be provided as an argument.
//static jfloat *widenArray(jfloat *in, const int cursize, const int numToAdd) {
//    if (in.length >= cursize + numToAdd) {
//        return in;
//    }
//    return Arrays.copyOf(in, 2 * (cursize + numToAdd));
//}

//    static int[] widenArray(int[] in, const int cursize, const int numToAdd) {
//        if (in.length >= cursize + numToAdd) {
//            return in;
//        }
//        return Arrays.copyOf(in, 2 * (cursize + numToAdd));
//    }

jfloat Helpers_evalCubic(const jfloat a, const jfloat b,
                         const jfloat c, const jfloat d,
                         const jfloat t)
{
    return t * (t * (t * a + b) + c) + d;
}

jfloat Helpers_evalQuad(const jfloat a, const jfloat b,
                        const jfloat c, const jfloat t)
{
    return t * (t * a + b) + c;
}

// returns the index 1 past the last valid element remaining after filtering
jint Helpers_filterOutNotInAB(jfloat nums[], const jint off, const jint len,
                              const jfloat a, const jfloat b)
{
    jint ret = off;
    jint i;
    for (i = off; i < off + len; i++) {
        if (nums[i] >= a && nums[i] < b) {
            nums[ret++] = nums[i];
        }
    }
    return ret;
}

jfloat Helpers_polyLineLength(jfloat poly[], const jint off, const jint nCoords) {
//    assert nCoords % 2 == 0 && poly.length >= off + nCoords : "";
    jfloat acc = 0;
    jint i;
    for (i = off + 2; i < off + nCoords; i += 2) {
        acc += Helpers_linelen(poly[i], poly[i+1], poly[i-2], poly[i-1]);
    }
    return acc;
}

jfloat Helpers_linelen(jfloat x1, jfloat y1, jfloat x2, jfloat y2) {
    const jfloat dx = x2 - x1;
    const jfloat dy = y2 - y1;
    return (jfloat) sqrt(dx*dx + dy*dy);
}

void Helpers_subdivide(jfloat src[], jint srcoff,
                       jfloat left[], jint leftoff,
                       jfloat right[], jint rightoff, jint type)
{
    switch(type) {
    case 6:
        Helpers_subdivideQuad(src, srcoff, left, leftoff, right, rightoff);
        break;
    case 8:
        Helpers_subdivideCubic(src, srcoff, left, leftoff, right, rightoff);
        break;
//    default:
//        fprintf(stderr, "Unsupported curve type");
        //throw new InternalError("Unsupported curve type");
    }
}

void Helpers_isort(jfloat a[], jint off, jint len) {
    jint i;
    for (i = off + 1; i < off + len; i++) {
        jfloat ai = a[i];
        jint j = i - 1;
        for (; j >= off && a[j] > ai; j--) {
            a[j+1] = a[j];
        }
        a[j+1] = ai;
    }
}

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
void Helpers_subdivideCubic(jfloat src[], jint srcoff,
                            jfloat left[], jint leftoff,
                            jfloat right[], jint rightoff)
{
    jfloat x1 = src[srcoff + 0];
    jfloat y1 = src[srcoff + 1];
    jfloat ctrlx1 = src[srcoff + 2];
    jfloat ctrly1 = src[srcoff + 3];
    jfloat ctrlx2 = src[srcoff + 4];
    jfloat ctrly2 = src[srcoff + 5];
    jfloat x2 = src[srcoff + 6];
    jfloat y2 = src[srcoff + 7];
    jfloat centerx, centery;
    if (left != NULL) {
        left[leftoff + 0] = x1;
        left[leftoff + 1] = y1;
    }
    if (right != NULL) {
        right[rightoff + 6] = x2;
        right[rightoff + 7] = y2;
    }
    x1 = (x1 + ctrlx1) / 2.0f;
    y1 = (y1 + ctrly1) / 2.0f;
    x2 = (x2 + ctrlx2) / 2.0f;
    y2 = (y2 + ctrly2) / 2.0f;
    centerx = (ctrlx1 + ctrlx2) / 2.0f;
    centery = (ctrly1 + ctrly2) / 2.0f;
    ctrlx1 = (x1 + centerx) / 2.0f;
    ctrly1 = (y1 + centery) / 2.0f;
    ctrlx2 = (x2 + centerx) / 2.0f;
    ctrly2 = (y2 + centery) / 2.0f;
    centerx = (ctrlx1 + ctrlx2) / 2.0f;
    centery = (ctrly1 + ctrly2) / 2.0f;
    if (left != NULL) {
        left[leftoff + 2] = x1;
        left[leftoff + 3] = y1;
        left[leftoff + 4] = ctrlx1;
        left[leftoff + 5] = ctrly1;
        left[leftoff + 6] = centerx;
        left[leftoff + 7] = centery;
    }
    if (right != NULL) {
        right[rightoff + 0] = centerx;
        right[rightoff + 1] = centery;
        right[rightoff + 2] = ctrlx2;
        right[rightoff + 3] = ctrly2;
        right[rightoff + 4] = x2;
        right[rightoff + 5] = y2;
    }
}

void Helpers_subdivideCubicAt(jfloat t,
                              jfloat src[], jint srcoff,
                              jfloat left[], jint leftoff,
                              jfloat right[], jint rightoff)
{
    jfloat x1 = src[srcoff + 0];
    jfloat y1 = src[srcoff + 1];
    jfloat ctrlx1 = src[srcoff + 2];
    jfloat ctrly1 = src[srcoff + 3];
    jfloat ctrlx2 = src[srcoff + 4];
    jfloat ctrly2 = src[srcoff + 5];
    jfloat x2 = src[srcoff + 6];
    jfloat y2 = src[srcoff + 7];
    jfloat centerx, centery;
    if (left != NULL) {
        left[leftoff + 0] = x1;
        left[leftoff + 1] = y1;
    }
    if (right != NULL) {
        right[rightoff + 6] = x2;
        right[rightoff + 7] = y2;
    }
    x1 = x1 + t * (ctrlx1 - x1);
    y1 = y1 + t * (ctrly1 - y1);
    x2 = ctrlx2 + t * (x2 - ctrlx2);
    y2 = ctrly2 + t * (y2 - ctrly2);
    centerx = ctrlx1 + t * (ctrlx2 - ctrlx1);
    centery = ctrly1 + t * (ctrly2 - ctrly1);
    ctrlx1 = x1 + t * (centerx - x1);
    ctrly1 = y1 + t * (centery - y1);
    ctrlx2 = centerx + t * (x2 - centerx);
    ctrly2 = centery + t * (y2 - centery);
    centerx = ctrlx1 + t * (ctrlx2 - ctrlx1);
    centery = ctrly1 + t * (ctrly2 - ctrly1);
    if (left != NULL) {
        left[leftoff + 2] = x1;
        left[leftoff + 3] = y1;
        left[leftoff + 4] = ctrlx1;
        left[leftoff + 5] = ctrly1;
        left[leftoff + 6] = centerx;
        left[leftoff + 7] = centery;
    }
    if (right != NULL) {
        right[rightoff + 0] = centerx;
        right[rightoff + 1] = centery;
        right[rightoff + 2] = ctrlx2;
        right[rightoff + 3] = ctrly2;
        right[rightoff + 4] = x2;
        right[rightoff + 5] = y2;
    }
}

void Helpers_subdivideQuad(jfloat src[], jint srcoff,
                           jfloat left[], jint leftoff,
                           jfloat right[], jint rightoff)
{
    jfloat x1 = src[srcoff + 0];
    jfloat y1 = src[srcoff + 1];
    jfloat ctrlx = src[srcoff + 2];
    jfloat ctrly = src[srcoff + 3];
    jfloat x2 = src[srcoff + 4];
    jfloat y2 = src[srcoff + 5];
    if (left != NULL) {
        left[leftoff + 0] = x1;
        left[leftoff + 1] = y1;
    }
    if (right != NULL) {
        right[rightoff + 4] = x2;
        right[rightoff + 5] = y2;
    }
    x1 = (x1 + ctrlx) / 2.0f;
    y1 = (y1 + ctrly) / 2.0f;
    x2 = (x2 + ctrlx) / 2.0f;
    y2 = (y2 + ctrly) / 2.0f;
    ctrlx = (x1 + x2) / 2.0f;
    ctrly = (y1 + y2) / 2.0f;
    if (left != NULL) {
        left[leftoff + 2] = x1;
        left[leftoff + 3] = y1;
        left[leftoff + 4] = ctrlx;
        left[leftoff + 5] = ctrly;
    }
    if (right != NULL) {
        right[rightoff + 0] = ctrlx;
        right[rightoff + 1] = ctrly;
        right[rightoff + 2] = x2;
        right[rightoff + 3] = y2;
    }
}

void Helpers_subdivideQuadAt(jfloat t,
                             jfloat src[], jint srcoff,
                             jfloat left[], jint leftoff,
                             jfloat right[], jint rightoff)
{
    jfloat x1 = src[srcoff + 0];
    jfloat y1 = src[srcoff + 1];
    jfloat ctrlx = src[srcoff + 2];
    jfloat ctrly = src[srcoff + 3];
    jfloat x2 = src[srcoff + 4];
    jfloat y2 = src[srcoff + 5];
    if (left != NULL) {
        left[leftoff + 0] = x1;
        left[leftoff + 1] = y1;
    }
    if (right != NULL) {
        right[rightoff + 4] = x2;
        right[rightoff + 5] = y2;
    }
    x1 = x1 + t * (ctrlx - x1);
    y1 = y1 + t * (ctrly - y1);
    x2 = ctrlx + t * (x2 - ctrlx);
    y2 = ctrly + t * (y2 - ctrly);
    ctrlx = x1 + t * (x2 - x1);
    ctrly = y1 + t * (y2 - y1);
    if (left != NULL) {
        left[leftoff + 2] = x1;
        left[leftoff + 3] = y1;
        left[leftoff + 4] = ctrlx;
        left[leftoff + 5] = ctrly;
    }
    if (right != NULL) {
        right[rightoff + 0] = ctrlx;
        right[rightoff + 1] = ctrly;
        right[rightoff + 2] = x2;
        right[rightoff + 3] = y2;
    }
}

void Helpers_subdivideAt(jfloat t,
                         jfloat src[], jint srcoff,
                         jfloat left[], jint leftoff,
                         jfloat right[], jint rightoff, jint size)
{
    switch(size) {
    case 8:
        Helpers_subdivideCubicAt(t, src, srcoff, left, leftoff, right, rightoff);
        break;
    case 6:
        Helpers_subdivideQuadAt(t, src, srcoff, left, leftoff, right, rightoff);
        break;
    }
}
