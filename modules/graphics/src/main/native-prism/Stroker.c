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

#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <jni.h>

#include "Helpers.h"
#include "PathConsumer.h"

#include "Stroker.h"

// NOTE: some of the arithmetic here is too verbose and prone to hard to
// debug typos. We should consider making a small Point/Vector class that
// has methods like plus(Point), minus(Point), dot(Point), cross(Point)and such
//public final class Stroker implements PathConsumer2D {

#define MOVE_TO  0
#define DRAWING_OP_TO  1 // ie. curve, line, or quad
#define CLOSE  2

static MoveToFunc       Stroker_moveTo;
static LineToFunc       Stroker_lineTo;
static QuadToFunc       Stroker_quadTo;
static CurveToFunc      Stroker_curveTo;
static ClosePathFunc    Stroker_closePath;
static PathDoneFunc     Stroker_pathDone;

#define this (*((Stroker *) pStroker))

static void drawJoin(PathConsumer *pStroker,
                     jfloat pdx, jfloat pdy,
                     jfloat x0, jfloat y0,
                     jfloat dx, jfloat dy,
                     jfloat omx, jfloat omy,
                     jfloat mx, jfloat my);

static void drawRoundJoin2(PathConsumer *pStroker,
                           jfloat cx, jfloat cy,
                           jfloat omx, jfloat omy,
                           jfloat mx, jfloat my,
                           jboolean rev);

static void drawBezApproxForArc(PathConsumer *pStroker,
                                const jfloat cx, const jfloat cy,
                                const jfloat omx, const jfloat omy,
                                const jfloat mx, const jfloat my,
                                jboolean rev);

static void emitMoveTo(PathConsumer *pStroker, const jfloat x0, const jfloat y0);

static void emitLineTo(PathConsumer *pStroker, const jfloat x1, const jfloat y1,
                       const jboolean rev);

static void emitCurveTo(PathConsumer *pStroker,
                        const jfloat x0, const jfloat y0,
                        const jfloat x1, const jfloat y1,
                        const jfloat x2, const jfloat y2,
                        const jfloat x3, const jfloat y3, const jboolean rev);

static void emitClose(PathConsumer *pStroker);

static void emitReverse(PathConsumer *pStroker);

static void finish(PathConsumer *pStroker);

extern void PolyStack_init(PolyStack *pStack);

extern void PolyStack_destroy(PolyStack *pStack);

extern jboolean PolyStack_isEmpty(PolyStack *pStack);

extern void PolyStack_pushLine(PolyStack *pStack,
                               jfloat x, jfloat y);

extern void PolyStack_pushCubic(PolyStack *pStack,
                                jfloat x0, jfloat y0,
                                jfloat x1, jfloat y1,
                                jfloat x2, jfloat y2);

extern void PolyStack_pushQuad(PolyStack *pStack,
                               jfloat x0, jfloat y0,
                               jfloat x1, jfloat y1);

extern void PolyStack_pop(PolyStack *pStack, PathConsumer *io);

    /**
     * Constructs a <code>Stroker</code>.
     *
     * @param pc2d an output <code>PathConsumer2D</code>.
     * @param lineWidth the desired line width in pixels
     * @param capStyle the desired end cap style, one of
     * <code>CAP_BUTT</code>, <code>CAP_ROUND</code> or
     * <code>CAP_SQUARE</code>.
     * @param joinStyle the desired line join style, one of
     * <code>JOIN_MITER</code>, <code>JOIN_ROUND</code> or
     * <code>JOIN_BEVEL</code>.
     * @param miterLimit the desired miter limit
    public Stroker(PathConsumer2D pc2d,
                   float lineWidth,
                   jint capStyle,
                   jint joinStyle,
                   float miterLimit)
    {
        this(pc2d);

        reset(lineWidth, capStyle, joinStyle, miterLimit);
    }

    public Stroker(PathConsumer2D pc2d) {
        setConsumer(pc2d);
    }

    public void setConsumer(PathConsumer2D pc2d) {
        this.out = pc2d;
    }
     */

extern void Stroker_reset(Stroker *pStroker, jfloat lineWidth,
                          jint capStyle, jint joinStyle, jfloat miterLimit);

void Stroker_init(Stroker *pStroker,
                  PathConsumer *out,
                  jfloat lineWidth,
                  jint capStyle,
                  jint joinStyle,
                  jfloat miterLimit)
{
    memset(pStroker, 0, sizeof(Stroker));
    PathConsumer_init(&this.consumer,
                      Stroker_moveTo,
                      Stroker_lineTo,
                      Stroker_quadTo,
                      Stroker_curveTo,
                      Stroker_closePath,
                      Stroker_pathDone);

    this.out = out;
    Stroker_reset(pStroker, lineWidth, capStyle, joinStyle, miterLimit);
    PolyStack_init(&pStroker->reverse);
}

void Stroker_reset(Stroker *pStroker, jfloat lineWidth,
                   jint capStyle, jint joinStyle, jfloat miterLimit)
{
    jfloat limit;

    this.lineWidth2 = lineWidth / 2;
    this.capStyle = capStyle;
    this.joinStyle = joinStyle;

    limit = miterLimit * this.lineWidth2;
    this.miterLimitSq = limit*limit;

    this.prev = CLOSE;
}

void Stroker_destroy(Stroker *pStroker) {
    PolyStack_destroy(&pStroker->reverse);
}

void computeOffset(const jfloat lx, const jfloat ly,
                   const jfloat w, jfloat m[])
{
    const jfloat len = (jfloat) sqrt(lx*lx + ly*ly);
    if (len == 0) {
        m[0] = m[1] = 0;
    } else {
        m[0] = (ly * w)/len;
        m[1] = -(lx * w)/len;
    }
}

// Returns true if the vectors (dx1, dy1) and (dx2, dy2) are
// clockwise (if dx1,dy1 needs to be rotated clockwise to close
// the smallest angle between it and dx2,dy2).
// This is equivalent to detecting whether a point q is on the right side
// of a line passing through points p1, p2 where p2 = p1+(dx1,dy1) and
// q = p2+(dx2,dy2), which is the same as saying p1, p2, q are in a
// clockwise order.
// NOTE: "clockwise" here assumes coordinates with 0,0 at the bottom left.
static jboolean isCW(const jfloat dx1, const jfloat dy1,
                     const jfloat dx2, const jfloat dy2)
{
    return dx1 * dy2 <= dy1 * dx2;
}

// pisces used to use fixed point arithmetic with 16 decimal digits. I
// didn't want to change the values of the constant below when I converted
// it to floating point, so that's why the divisions by 2^16 are there.
#define ROUND_JOIN_THRESHOLD   (1000/65536.0f)

static void drawRoundJoin(PathConsumer *pStroker,
                          jfloat x, jfloat y,
                          jfloat omx, jfloat omy, jfloat mx, jfloat my,
                          jboolean rev,
                          jfloat threshold)
{
    jfloat domx, domy, len;

    if ((omx == 0 && omy == 0) || (mx == 0 && my == 0)) {
        return;
    }

    domx = omx - mx;
    domy = omy - my;
    len = domx*domx + domy*domy;
    if (len < threshold) {
        return;
    }

    if (rev) {
        omx = -omx;
        omy = -omy;
        mx = -mx;
        my = -my;
    }
    drawRoundJoin2(pStroker, x, y, omx, omy, mx, my, rev);
}

static void drawRoundJoin2(PathConsumer *pStroker,
                           jfloat cx, jfloat cy,
                           jfloat omx, jfloat omy,
                           jfloat mx, jfloat my,
                           jboolean rev)
{
    // The sign of the dot product of mx,my and omx,omy is equal to the
    // the sign of the cosine of ext
    // (ext is the angle between omx,omy and mx,my).
    jdouble cosext = omx * mx + omy * my;
    // If it is >=0, we know that abs(ext) is <= 90 degrees, so we only
    // need 1 curve to approximate the circle section that joins omx,omy
    // and mx,my.
    const jint numCurves = cosext >= 0 ? 1 : 2;

    switch (numCurves) {
    case 1:
        drawBezApproxForArc(pStroker, cx, cy, omx, omy, mx, my, rev);
        break;
    case 2:
        {
            // we need to split the arc into 2 arcs spanning the same angle.
            // The point we want will be one of the 2 intersections of the
            // perpendicular bisector of the chord (omx,omy)->(mx,my) and the
            // circle. We could find this by scaling the vector
            // (omx+mx, omy+my)/2 so that it has length=lineWidth2 (and thus lies
            // on the circle), but that can have numerical problems when the angle
            // between omx,omy and mx,my is close to 180 degrees. So we compute a
            // normal of (omx,omy)-(mx,my). This will be the direction of the
            // perpendicular bisector. To get one of the intersections, we just scale
            // this vector that its length is lineWidth2 (this works because the
            // perpendicular bisector goes through the origin). This scaling doesn't
            // have numerical problems because we know that lineWidth2 divided by
            // this normal's length is at least 0.5 and at most sqrt(2)/2 (because
            // we know the angle of the arc is > 90 degrees).
            jfloat nx = my - omy, ny = omx - mx;
            jfloat nlen = (jfloat) sqrt(nx*nx + ny*ny);
            jfloat scale = this.lineWidth2/nlen;
            jfloat mmx = nx * scale, mmy = ny * scale;

            // if (isCW(omx, omy, mx, my) != isCW(mmx, mmy, mx, my)) then we've
            // computed the wrong intersection so we get the other one.
            // The test above is equivalent to if (rev).
            if (rev) {
                mmx = -mmx;
                mmy = -mmy;
            }
            drawBezApproxForArc(pStroker, cx, cy, omx, omy, mmx, mmy, rev);
            drawBezApproxForArc(pStroker, cx, cy, mmx, mmy, mx, my, rev);
            break;
        }
    }
}

// the input arc defined by omx,omy and mx,my must span <= 90 degrees.
static void drawBezApproxForArc(PathConsumer *pStroker,
                                const jfloat cx, const jfloat cy,
                                const jfloat omx, const jfloat omy,
                                const jfloat mx, const jfloat my,
                                jboolean rev)
{
    jfloat cosext2 = (omx * mx + omy * my) / (2 * this.lineWidth2 * this.lineWidth2);
    // cv is the length of P1-P0 and P2-P3 divided by the radius of the arc
    // (so, cv assumes the arc has radius 1). P0, P1, P2, P3 are the points that
    // define the bezier curve we're computing.
    // It is computed using the constraints that P1-P0 and P3-P2 are parallel
    // to the arc tangents at the endpoints, and that |P1-P0|=|P3-P2|.
    jfloat cv = (jfloat) ((4.0 / 3.0) * sqrt(0.5-cosext2) /
                          (1.0 + sqrt(cosext2+0.5)));
    jfloat x1, y1, x2, y2, x3, y3, x4, y4;

    // if clockwise, we need to negate cv.
    if (rev) { // rev is equivalent to isCW(omx, omy, mx, my)
        cv = -cv;
    }
    x1 = cx + omx;
    y1 = cy + omy;
    x2 = x1 - cv * omy;
    y2 = y1 + cv * omx;

    x4 = cx + mx;
    y4 = cy + my;
    x3 = x4 + cv * my;
    y3 = y4 - cv * mx;

    emitCurveTo(pStroker, x1, y1, x2, y2, x3, y3, x4, y4, rev);
}

static void drawRoundCap(PathConsumer *pStroker, jfloat cx, jfloat cy, jfloat mx, jfloat my) {
    const jfloat C = 0.5522847498307933f;
    // the first and second arguments of the following two calls
    // are really will be ignored by emitCurveTo (because of the false),
    // but we put them in anyway, as opposed to just giving it 4 zeroes,
    // because it's just 4 additions and it's not good to rely on this
    // sort of assumption (right now it's true, but that may change).
    emitCurveTo(pStroker,
                cx+mx,      cy+my,
                cx+mx-C*my, cy+my+C*mx,
                cx-my+C*mx, cy+mx+C*my,
                cx-my,      cy+mx,
                JNI_FALSE);
    emitCurveTo(pStroker,
                cx-my,      cy+mx,
                cx-my-C*mx, cy+mx-C*my,
                cx-mx-C*my, cy-my+C*mx,
                cx-mx,      cy-my,
                JNI_FALSE);
}

// Return the intersection point of the lines (x0, y0) -> (x1, y1)
// and (x0p, y0p) -> (x1p, y1p) in m[0] and m[1]
static void computeMiter(const jfloat x0, const jfloat y0,
                         const jfloat x1, const jfloat y1,
                         const jfloat x0p, const jfloat y0p,
                         const jfloat x1p, const jfloat y1p,
                         jfloat m[], jint off)
{
    jfloat x10 = x1 - x0;
    jfloat y10 = y1 - y0;
    jfloat x10p = x1p - x0p;
    jfloat y10p = y1p - y0p;

    // if this is 0, the lines are parallel. If they go in the
    // same direction, there is no intersection so m[off] and
    // m[off+1] will contain infinity, so no miter will be drawn.
    // If they go in the same direction that means that the start of the
    // current segment and the end of the previous segment have the same
    // tangent, in which case this method won't even be involved in
    // miter drawing because it won't be called by drawMiter (because
    // (mx == omx && my == omy) will be true, and drawMiter will return
    // immediately).
    jfloat den = x10*y10p - x10p*y10;
    jfloat t = x10p*(y0-y0p) - y10p*(x0-x0p);
    t /= den;
    m[off++] = x0 + t*x10;
    m[off] = y0 + t*y10;
}

// Return the intersection point of the lines (x0, y0) -> (x1, y1)
// and (x0p, y0p) -> (x1p, y1p) in m[0] and m[1]
static void safecomputeMiter(const jfloat x0, const jfloat y0,
                             const jfloat x1, const jfloat y1,
                             const jfloat x0p, const jfloat y0p,
                             const jfloat x1p, const jfloat y1p,
                             jfloat m[], jint off)
{
    jfloat x10 = x1 - x0;
    jfloat y10 = y1 - y0;
    jfloat x10p = x1p - x0p;
    jfloat y10p = y1p - y0p;

    // if this is 0, the lines are parallel. If they go in the
    // same direction, there is no intersection so m[off] and
    // m[off+1] will contain infinity, so no miter will be drawn.
    // If they go in the same direction that means that the start of the
    // current segment and the end of the previous segment have the same
    // tangent, in which case this method won't even be involved in
    // miter drawing because it won't be called by drawMiter (because
    // (mx == omx && my == omy) will be true, and drawMiter will return
    // immediately).
    jfloat den = x10*y10p - x10p*y10;
    jfloat t;

    if (den == 0) {
        m[off++] = (x0 + x0p) / 2.0f;
        m[off] = (y0 + y0p) / 2.0f;
        return;
    }
    t = x10p*(y0-y0p) - y10p*(x0-x0p);
    t /= den;
    m[off++] = x0 + t*x10;
    m[off] = y0 + t*y10;
}

static void drawMiter(PathConsumer *pStroker,
                      const jfloat pdx, const jfloat pdy,
                      const jfloat x0, const jfloat y0,
                      const jfloat dx, const jfloat dy,
                      jfloat omx, jfloat omy, jfloat mx, jfloat my,
                      jboolean rev)
{
    jfloat lenSq;

    if ((mx == omx && my == omy) ||
        (pdx == 0 && pdy == 0) ||
        (dx == 0 && dy == 0)) {
        return;
    }

    if (rev) {
        omx = -omx;
        omy = -omy;
        mx = -mx;
        my = -my;
    }

    computeMiter((x0 - pdx) + omx, (y0 - pdy) + omy, x0 + omx, y0 + omy,
                 (dx + x0) + mx, (dy + y0) + my, x0 + mx, y0 + my,
                 this.miter, 0);

    lenSq = (this.miter[0]-x0)*(this.miter[0]-x0) + (this.miter[1]-y0)*(this.miter[1]-y0);

    if (lenSq < this.miterLimitSq) {
        emitLineTo(pStroker, this.miter[0], this.miter[1], rev);
    }
}

static void Stroker_moveTo(PathConsumer *pStroker, jfloat x0, jfloat y0) {
    if (this.prev == DRAWING_OP_TO) {
        finish(pStroker);
    }
    this.sx0 = this.cx0 = x0;
    this.sy0 = this.cy0 = y0;
    this.cdx = this.sdx = 1;
    this.cdy = this.sdy = 0;
    this.prev = MOVE_TO;
}

static void Stroker_lineTo(PathConsumer *pStroker, jfloat x1, jfloat y1) {
    jfloat dx = x1 - this.cx0;
    jfloat dy = y1 - this.cy0;
    jfloat mx, my;

    if (dx == 0.0f && dy == 0.0f) {
        dx = 1;
    }
    computeOffset(dx, dy, this.lineWidth2, this.offset[0]);
    mx = this.offset[0][0];
    my = this.offset[0][1];

    drawJoin(pStroker,
             this.cdx, this.cdy, this.cx0, this.cy0,
             dx, dy, this.cmx, this.cmy, mx, my);

    emitLineTo(pStroker, this.cx0 + mx, this.cy0 + my, JNI_FALSE);
    emitLineTo(pStroker, x1 + mx, y1 + my, JNI_FALSE);

    emitLineTo(pStroker, this.cx0 - mx, this.cy0 - my, JNI_TRUE);
    emitLineTo(pStroker, x1 - mx, y1 - my, JNI_TRUE);

    this.cmx = mx;
    this.cmy = my;
    this.cdx = dx;
    this.cdy = dy;
    this.cx0 = x1;
    this.cy0 = y1;
    this.prev = DRAWING_OP_TO;
}

static void Stroker_closePath(PathConsumer *pStroker) {
    if (this.prev != DRAWING_OP_TO) {
        if (this.prev == CLOSE) {
            return;
        }
        emitMoveTo(pStroker, this.cx0, this.cy0 - this.lineWidth2);
        this.cmx = this.smx = 0;
        this.cmy = this.smy = -this.lineWidth2;
        this.cdx = this.sdx = 1;
        this.cdy = this.sdy = 0;
        finish(pStroker);
        return;
    }

    if (this.cx0 != this.sx0 || this.cy0 != this.sy0) {
        Stroker_lineTo(pStroker, this.sx0, this.sy0);
    }

    drawJoin(pStroker,
             this.cdx, this.cdy, this.cx0, this.cy0,
             this.sdx, this.sdy, this.cmx, this.cmy,
             this.smx, this.smy);

    emitLineTo(pStroker, this.sx0 + this.smx, this.sy0 + this.smy, JNI_FALSE);

    emitMoveTo(pStroker, this.sx0 - this.smx, this.sy0 - this.smy);
    emitReverse(pStroker);

    this.prev = CLOSE;
    emitClose(pStroker);
}

static void emitReverse(PathConsumer *pStroker) {
    while (!PolyStack_isEmpty(&this.reverse)) {
        PolyStack_pop(&this.reverse, this.out);
    }
}

static void Stroker_pathDone(PathConsumer *pStroker) {
    if (this.prev == DRAWING_OP_TO) {
        finish(pStroker);
    }

    this.out->pathDone(this.out);
    // this shouldn't matter since this object won't be used
    // after the call to this method.
    this.prev = CLOSE;
}

static void finish(PathConsumer *pStroker) {
    if (this.capStyle == CAP_ROUND) {
        drawRoundCap(pStroker, this.cx0, this.cy0, this.cmx, this.cmy);
    } else if (this.capStyle == CAP_SQUARE) {
        emitLineTo(pStroker, this.cx0 - this.cmy + this.cmx, this.cy0 + this.cmx + this.cmy, JNI_FALSE);
        emitLineTo(pStroker, this.cx0 - this.cmy - this.cmx, this.cy0 + this.cmx - this.cmy, JNI_FALSE);
    }

    emitReverse(pStroker);

    if (this.capStyle == CAP_ROUND) {
        drawRoundCap(pStroker, this.sx0, this.sy0, -this.smx, -this.smy);
    } else if (this.capStyle == CAP_SQUARE) {
        emitLineTo(pStroker, this.sx0 + this.smy - this.smx, this.sy0 - this.smx - this.smy, JNI_FALSE);
        emitLineTo(pStroker, this.sx0 + this.smy + this.smx, this.sy0 - this.smx + this.smy, JNI_FALSE);
    }

    emitClose(pStroker);
}

static void emitMoveTo(PathConsumer *pStroker, const jfloat x0, const jfloat y0) {
    this.out->moveTo(this.out, x0, y0);
}

static void emitLineTo(PathConsumer *pStroker, const jfloat x1, const jfloat y1,
                       const jboolean rev)
{
    if (rev) {
        PolyStack_pushLine(&this.reverse, x1, y1);
    } else {
        this.out->lineTo(this.out, x1, y1);
    }
}

static void emitQuadTo(PathConsumer *pStroker,
                       const jfloat x0, const jfloat y0,
                       const jfloat x1, const jfloat y1,
                       const jfloat x2, const jfloat y2, const jboolean rev)
{
    if (rev) {
        PolyStack_pushQuad(&this.reverse, x0, y0, x1, y1);
    } else {
        this.out->quadTo(this.out, x1, y1, x2, y2);
    }
}

static void emitCurveTo(PathConsumer *pStroker,
                        const jfloat x0, const jfloat y0,
                        const jfloat x1, const jfloat y1,
                        const jfloat x2, const jfloat y2,
                        const jfloat x3, const jfloat y3, const jboolean rev)
{
    if (rev) {
        PolyStack_pushCubic(&this.reverse, x0, y0, x1, y1, x2, y2);
    } else {
        this.out->curveTo(this.out, x1, y1, x2, y2, x3, y3);
    }
}

static void emitClose(PathConsumer *pStroker) {
    this.out->closePath(this.out);
}

static void drawJoin(PathConsumer *pStroker,
                     jfloat pdx, jfloat pdy,
                     jfloat x0, jfloat y0,
                     jfloat dx, jfloat dy,
                     jfloat omx, jfloat omy,
                     jfloat mx, jfloat my)
{
    if (this.prev != DRAWING_OP_TO) {
        emitMoveTo(pStroker, x0 + mx, y0 + my);
        this.sdx = dx;
        this.sdy = dy;
        this.smx = mx;
        this.smy = my;
    } else {
        jboolean cw = isCW(pdx, pdy, dx, dy);
        if (this.joinStyle == JOIN_MITER) {
            drawMiter(pStroker, pdx, pdy, x0, y0, dx, dy, omx, omy, mx, my, cw);
        } else if (this.joinStyle == JOIN_ROUND) {
            drawRoundJoin(pStroker,
                          x0, y0,
                          omx, omy,
                          mx, my, cw,
                          ROUND_JOIN_THRESHOLD);
        }
        emitLineTo(pStroker, x0, y0, !cw);
    }
    this.prev = DRAWING_OP_TO;
}

static jboolean withinULP(const jfloat x1, const jfloat y1,
                          const jfloat x2, const jfloat y2,
                          const int maxUlps)
{
//    assert maxUlps is much smaller than 0x7fffffff;
    // compare taxicab distance. ERR will always be small, so using
    // true distance won't give much benefit
    return (Helpers_withinULP(x1, x2, maxUlps) &&
            Helpers_withinULP(y1, y2, maxUlps));
}

static void getLineOffsets(PathConsumer *pStroker,
                           jfloat x1, jfloat y1,
                           jfloat x2, jfloat y2,
                           jfloat left[], jfloat right[]) {
    computeOffset(x2 - x1, y2 - y1, this.lineWidth2, this.offset[0]);
    left[0] = x1 + this.offset[0][0];
    left[1] = y1 + this.offset[0][1];
    left[2] = x2 + this.offset[0][0];
    left[3] = y2 + this.offset[0][1];
    right[0] = x1 - this.offset[0][0];
    right[1] = y1 - this.offset[0][1];
    right[2] = x2 - this.offset[0][0];
    right[3] = y2 - this.offset[0][1];
}

static jint computeOffsetCubic(PathConsumer *pStroker,
                               jfloat pts[], const jint off,
                               jfloat leftOff[], jfloat rightOff[])
{
    jfloat dotsq, l1sq, l4sq;
    jfloat x, y, dxm, dym;

    // if p1=p2 or p3=p4 it means that the derivative at the endpoint
    // vanishes, which creates problems with computeOffset. Usually
    // this happens when this stroker object is trying to winden
    // a curve with a cusp. What happens is that curveTo splits
    // the input curve at the cusp, and passes it to this function.
    // because of inaccuracies in the splitting, we consider points
    // equal if they're very close to each other.
    const jfloat x1 = pts[off + 0], y1 = pts[off + 1];
    const jfloat x2 = pts[off + 2], y2 = pts[off + 3];
    const jfloat x3 = pts[off + 4], y3 = pts[off + 5];
    const jfloat x4 = pts[off + 6], y4 = pts[off + 7];

    jfloat dx4 = x4 - x3;
    jfloat dy4 = y4 - y3;
    jfloat dx1 = x2 - x1;
    jfloat dy1 = y2 - y1;

    // if p1 == p2 && p3 == p4: draw line from p1->p4, unless p1 == p4,
    // in which case ignore if p1 == p2
    const jboolean p1eqp2 = withinULP(x1,y1,x2,y2, 6);
    const jboolean p3eqp4 = withinULP(x3,y3,x4,y4, 6);
    if (p1eqp2 && p3eqp4) {
        getLineOffsets(pStroker, x1, y1, x4, y4, leftOff, rightOff);
        return 4;
    } else if (p1eqp2) {
        dx1 = x3 - x1;
        dy1 = y3 - y1;
    } else if (p3eqp4) {
        dx4 = x4 - x2;
        dy4 = y4 - y2;
    }

    // if p2-p1 and p4-p3 are parallel, that must mean this curve is a line
    dotsq = (dx1 * dx4 + dy1 * dy4);
    dotsq = dotsq * dotsq;
    l1sq = dx1 * dx1 + dy1 * dy1;
    l4sq = dx4 * dx4 + dy4 * dy4;
    if (Helpers_withinULP(dotsq, l1sq * l4sq, 4)) {
        getLineOffsets(pStroker, x1, y1, x4, y4, leftOff, rightOff);
        return 4;
    }

//      What we're trying to do in this function is to approximate an ideal
//      offset curve (call it I) of the input curve B using a bezier curve Bp.
//      The constraints I use to get the equations are:
//
//      1. The computed curve Bp should go through I(0) and I(1). These are
//      x1p, y1p, x4p, y4p, which are p1p and p4p. We still need to find
//      4 variables: the x and y components of p2p and p3p (i.e. x2p, y2p, x3p, y3p).
//
//      2. Bp should have slope equal in absolute value to I at the endpoints. So,
//      (by the way, the operator || in the comments below means "aligned with".
//      It is defined on vectors, so when we say I'(0) || Bp'(0) we mean that
//      vectors I'(0) and Bp'(0) are aligned, which is the same as saying
//      that the tangent lines of I and Bp at 0 are parallel. Mathematically
//      this means (I'(t) || Bp'(t)) <==> (I'(t) = c * Bp'(t)) where c is some
//      nonzero constant.)
//      I'(0) || Bp'(0) and I'(1) || Bp'(1). Obviously, I'(0) || B'(0) and
//      I'(1) || B'(1); therefore, Bp'(0) || B'(0) and Bp'(1) || B'(1).
//      We know that Bp'(0) || (p2p-p1p) and Bp'(1) || (p4p-p3p) and the same
//      is true for any bezier curve; therefore, we get the equations
//          (1) p2p = c1 * (p2-p1) + p1p
//          (2) p3p = c2 * (p4-p3) + p4p
//      We know p1p, p4p, p2, p1, p3, and p4; therefore, this reduces the number
//      of unknowns from 4 to 2 (i.e. just c1 and c2).
//      To eliminate these 2 unknowns we use the following constraint:
//
//      3. Bp(0.5) == I(0.5). Bp(0.5)=(x,y) and I(0.5)=(xi,yi), and I should note
//      that I(0.5) is *the only* reason for computing dxm,dym. This gives us
//          (3) Bp(0.5) = (p1p + 3 * (p2p + p3p) + p4p)/8, which is equivalent to
//          (4) p2p + p3p = (Bp(0.5)*8 - p1p - p4p) / 3
//      We can substitute (1) and (2) from above into (4) and we get:
//          (5) c1*(p2-p1) + c2*(p4-p3) = (Bp(0.5)*8 - p1p - p4p)/3 - p1p - p4p
//      which is equivalent to
//          (6) c1*(p2-p1) + c2*(p4-p3) = (4/3) * (Bp(0.5) * 2 - p1p - p4p)
//
//      The right side of this is a 2D vector, and we know I(0.5), which gives us
//      Bp(0.5), which gives us the value of the right side.
//      The left side is just a matrix vector multiplication in disguise. It is
//
//      [x2-x1, x4-x3][c1]
//      [y2-y1, y4-y3][c2]
//      which, is equal to
//      [dx1, dx4][c1]
//      [dy1, dy4][c2]
//      At this point we are left with a simple linear system and we solve it by
//      getting the inverse of the matrix above. Then we use [c1,c2] to compute
//      p2p and p3p.

    x = 0.125f * (x1 + 3 * (x2 + x3) + x4);
    y = 0.125f * (y1 + 3 * (y2 + y3) + y4);
    // (dxm,dym) is some tangent of B at t=0.5. This means it's equal to
    // c*B'(0.5) for some constant c.
    dxm = x3 + x4 - x1 - x2;
    dym = y3 + y4 - y1 - y2;

    // this computes the offsets at t=0, 0.5, 1, using the property that
    // for any bezier curve the vectors p2-p1 and p4-p3 are parallel to
    // the (dx/dt, dy/dt) vectors at the endpoints.
    computeOffset(dx1, dy1, this.lineWidth2, this.offset[0]);
    computeOffset(dxm, dym, this.lineWidth2, this.offset[1]);
    computeOffset(dx4, dy4, this.lineWidth2, this.offset[2]);
    {
        jfloat x1p = x1 + this.offset[0][0]; // start
        jfloat y1p = y1 + this.offset[0][1]; // point
        jfloat xi  = x  + this.offset[1][0]; // interpolation
        jfloat yi  = y  + this.offset[1][1]; // point
        jfloat x4p = x4 + this.offset[2][0]; // end
        jfloat y4p = y4 + this.offset[2][1]; // point

        jfloat invdet43 = 4.0f / (3.0f * (dx1 * dy4 - dy1 * dx4));

        jfloat two_pi_m_p1_m_p4x = 2*xi - x1p - x4p;
        jfloat two_pi_m_p1_m_p4y = 2*yi - y1p - y4p;
        jfloat c1 = invdet43 * (dy4 * two_pi_m_p1_m_p4x - dx4 * two_pi_m_p1_m_p4y);
        jfloat c2 = invdet43 * (dx1 * two_pi_m_p1_m_p4y - dy1 * two_pi_m_p1_m_p4x);

        jfloat x2p, y2p, x3p, y3p;
        x2p = x1p + c1*dx1;
        y2p = y1p + c1*dy1;
        x3p = x4p + c2*dx4;
        y3p = y4p + c2*dy4;

        leftOff[0] = x1p; leftOff[1] = y1p;
        leftOff[2] = x2p; leftOff[3] = y2p;
        leftOff[4] = x3p; leftOff[5] = y3p;
        leftOff[6] = x4p; leftOff[7] = y4p;

        x1p = x1 -     this.offset[0][0]; y1p = y1 -     this.offset[0][1];
        xi  = xi - 2 * this.offset[1][0]; yi  = yi - 2 * this.offset[1][1];
        x4p = x4 -     this.offset[2][0]; y4p = y4 -     this.offset[2][1];

        two_pi_m_p1_m_p4x = 2*xi - x1p - x4p;
        two_pi_m_p1_m_p4y = 2*yi - y1p - y4p;
        c1 = invdet43 * (dy4 * two_pi_m_p1_m_p4x - dx4 * two_pi_m_p1_m_p4y);
        c2 = invdet43 * (dx1 * two_pi_m_p1_m_p4y - dy1 * two_pi_m_p1_m_p4x);

        x2p = x1p + c1*dx1;
        y2p = y1p + c1*dy1;
        x3p = x4p + c2*dx4;
        y3p = y4p + c2*dy4;

        rightOff[0] = x1p; rightOff[1] = y1p;
        rightOff[2] = x2p; rightOff[3] = y2p;
        rightOff[4] = x3p; rightOff[5] = y3p;
        rightOff[6] = x4p; rightOff[7] = y4p;
    }
    return 8;
}

// compute offset curves using bezier spline through t=0.5 (i.e.
// ComputedCurve(0.5) == IdealParallelCurve(0.5))
// return the kind of curve in the right and left arrays.
static jint computeOffsetQuad(PathConsumer *pStroker,
                              jfloat pts[], const jint off,
                              jfloat leftOff[], jfloat rightOff[])
{
    const jfloat x1 = pts[off + 0], y1 = pts[off + 1];
    const jfloat x2 = pts[off + 2], y2 = pts[off + 3];
    const jfloat x3 = pts[off + 4], y3 = pts[off + 5];
    jfloat dotsq, l1sq, l3sq;

    jfloat dx3 = x3 - x2;
    jfloat dy3 = y3 - y2;
    jfloat dx1 = x2 - x1;
    jfloat dy1 = y2 - y1;

    // if p1=p2 or p3=p4 it means that the derivative at the endpoint
    // vanishes, which creates problems with computeOffset. Usually
    // this happens when this stroker object is trying to winden
    // a curve with a cusp. What happens is that curveTo splits
    // the input curve at the cusp, and passes it to this function.
    // because of inaccuracies in the splitting, we consider points
    // equal if they're very close to each other.

    // if p1 == p2 && p3 == p4: draw line from p1->p4, unless p1 == p4,
    // in which case ignore.
    const jboolean p1eqp2 = withinULP(x1,y1,x2,y2, 6);
    const jboolean p2eqp3 = withinULP(x2,y2,x3,y3, 6);
    if (p1eqp2 || p2eqp3) {
        getLineOffsets(pStroker, x1, y1, x3, y3, leftOff, rightOff);
        return 4;
    }

    // if p2-p1 and p4-p3 are parallel, that must mean this curve is a line
    dotsq = (dx1 * dx3 + dy1 * dy3);
    dotsq = dotsq * dotsq;
    l1sq = dx1 * dx1 + dy1 * dy1;
    l3sq = dx3 * dx3 + dy3 * dy3;
    if (Helpers_withinULP(dotsq, l1sq * l3sq, 4)) {
        getLineOffsets(pStroker, x1, y1, x3, y3, leftOff, rightOff);
        return 4;
    }

    // this computes the offsets at t=0, 0.5, 1, using the property that
    // for any bezier curve the vectors p2-p1 and p4-p3 are parallel to
    // the (dx/dt, dy/dt) vectors at the endpoints.
    computeOffset(dx1, dy1, this.lineWidth2, this.offset[0]);
    computeOffset(dx3, dy3, this.lineWidth2, this.offset[1]);
    {
        jfloat x1p = x1 + this.offset[0][0]; // start
        jfloat y1p = y1 + this.offset[0][1]; // point
        jfloat x3p = x3 + this.offset[1][0]; // end
        jfloat y3p = y3 + this.offset[1][1]; // point

        safecomputeMiter(x1p, y1p, x1p+dx1, y1p+dy1, x3p, y3p, x3p-dx3, y3p-dy3, leftOff, 2);
        leftOff[0] = x1p; leftOff[1] = y1p;
        leftOff[4] = x3p; leftOff[5] = y3p;
        x1p = x1 - this.offset[0][0]; y1p = y1 - this.offset[0][1];
        x3p = x3 - this.offset[1][0]; y3p = y3 - this.offset[1][1];
        safecomputeMiter(x1p, y1p, x1p+dx1, y1p+dy1, x3p, y3p, x3p-dx3, y3p-dy3, rightOff, 2);
        rightOff[0] = x1p; rightOff[1] = y1p;
        rightOff[4] = x3p; rightOff[5] = y3p;
    }
    return 6;
}

// This is where the curve to be processed is put. We give it
// enough room to store 2 curves: one for the current subdivision, the
// other for the rest of the curve.
#define MAX_N_CURVES   11

static jfloat middle[MAX_N_CURVES*8];
static jfloat lp[8];
static jfloat rp[8];
static jfloat subdivTs[MAX_N_CURVES - 1];

// The following variation of somethingTo() caused problems when this was
// Java code as indicated by the following comment.  Now that this code has
// been converted into C, we should look at investigating the potential
// performance benefits of using this version instead of the "safer" version
// that survived in the Java sources and is currently being used below.
    // If this class is compiled with ecj, then Hotspot crashes when OSR
    // compiling this function. See bugs 7004570 and 6675699
    // NOTE: until those are fixed, we should work around that by
    // manually inlining this into curveTo and quadTo.
/******************************* WORKAROUND **********************************
    private void somethingTo(final int type) {
        // need these so we can update the state at the end of this method
        final float xf = middle[type-2], yf = middle[type-1];
        float dxs = middle[2] - middle[0];
        float dys = middle[3] - middle[1];
        float dxf = middle[type - 2] - middle[type - 4];
        float dyf = middle[type - 1] - middle[type - 3];
        switch(type) {
        case 6:
            if ((dxs == 0f && dys == 0f) ||
                (dxf == 0f && dyf == 0f)) {
               dxs = dxf = middle[4] - middle[0];
               dys = dyf = middle[5] - middle[1];
            }
            break;
        case 8:
            boolean p1eqp2 = (dxs == 0f && dys == 0f);
            boolean p3eqp4 = (dxf == 0f && dyf == 0f);
            if (p1eqp2) {
                dxs = middle[4] - middle[0];
                dys = middle[5] - middle[1];
                if (dxs == 0f && dys == 0f) {
                    dxs = middle[6] - middle[0];
                    dys = middle[7] - middle[1];
                }
            }
            if (p3eqp4) {
                dxf = middle[6] - middle[2];
                dyf = middle[7] - middle[3];
                if (dxf == 0f && dyf == 0f) {
                    dxf = middle[6] - middle[0];
                    dyf = middle[7] - middle[1];
                }
            }
        }
        if (dxs == 0f && dys == 0f) {
            // this happens iff the "curve" is just a point
            lineTo(middle[0], middle[1]);
            return;
        }
        // if these vectors are too small, normalize them, to avoid future
        // precision problems.
        if (Math.abs(dxs) < 0.1f && Math.abs(dys) < 0.1f) {
            float len = (float)Math.sqrt(dxs*dxs + dys*dys);
            dxs /= len;
            dys /= len;
        }
        if (Math.abs(dxf) < 0.1f && Math.abs(dyf) < 0.1f) {
            float len = (float)Math.sqrt(dxf*dxf + dyf*dyf);
            dxf /= len;
            dyf /= len;
        }

        computeOffset(dxs, dys, lineWidth2, offset[0]);
        final float mx = offset[0][0];
        final float my = offset[0][1];
        drawJoin(pStroker, cdx, cdy, cx0, cy0, dxs, dys, cmx, cmy, mx, my);

        int nSplits = findSubdivPoints(pStroker, middle, subdivTs, type, lineWidth2);

        int kind = 0;
        Iterator<Integer> it = Curve.breakPtsAtTs(middle, type, subdivTs, nSplits);
        while(it.hasNext()) {
            int curCurveOff = it.next();

            kind = 0;
            switch (type) {
            case 8:
                kind = computeOffsetCubic(middle, curCurveOff, lp, rp);
                break;
            case 6:
                kind = computeOffsetQuad(middle, curCurveOff, lp, rp);
                break;
            }
            if (kind != 0) {
                emitLineTo(pStroker, lp[0], lp[1], JNI_FALSE);
                switch(kind) {
                case 8:
                    emitCurveTo(pStroker, lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6], lp[7], false);
                    emitCurveTo(pStroker, rp[0], rp[1], rp[2], rp[3], rp[4], rp[5], rp[6], rp[7], true);
                    break;
                case 6:
                    emitQuadTo(pStroker, lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], false);
                    emitQuadTo(pStroker, rp[0], rp[1], rp[2], rp[3], rp[4], rp[5], true);
                    break;
                case 4:
                    emitLineTo(pStroker, lp[2], lp[3], JNI_FALSE);
                    emitLineTo(pStroker, rp[0], rp[1], JNI_TRUE);
                    break;
                }
                emitLineTo(pStroker, rp[kind - 2], rp[kind - 1], true);
            }
        }

        this.cmx = (lp[kind - 2] - rp[kind - 2]) / 2;
        this.cmy = (lp[kind - 1] - rp[kind - 1]) / 2;
        this.cdx = dxf;
        this.cdy = dyf;
        this.cx0 = xf;
        this.cy0 = yf;
        this.prev = DRAWING_OP_TO;
    }
****************************** END WORKAROUND *******************************/

// finds values of t where the curve in pts should be subdivided in order
// to get good offset curves a distance of w away from the middle curve.
// Stores the points in ts, and returns how many of them there were.
static jint findSubdivPoints(PathConsumer *pStroker,
                             jfloat pts[], jfloat ts[],
                             const jint type, const jfloat w)
{
    jint ret = 0;

    const jfloat x12 = pts[2] - pts[0];
    const jfloat y12 = pts[3] - pts[1];
    // if the curve is already parallel to either axis we gain nothing
    // from rotating it.
    if (y12 != 0.0f && x12 != 0.0f) {
        // we rotate it so that the first vector in the control polygon is
        // parallel to the x-axis. This will ensure that rotated quarter
        // circles won't be subdivided.
        const jfloat hypot = (jfloat) sqrt(x12 * x12 + y12 * y12);
        const jfloat cos = x12 / hypot;
        const jfloat sin = y12 / hypot;
        const jfloat x1 = cos * pts[0] + sin * pts[1];
        const jfloat y1 = cos * pts[1] - sin * pts[0];
        const jfloat x2 = cos * pts[2] + sin * pts[3];
        const jfloat y2 = cos * pts[3] - sin * pts[2];
        const jfloat x3 = cos * pts[4] + sin * pts[5];
        const jfloat y3 = cos * pts[5] - sin * pts[4];
        switch(type) {
        case 8:
        {
            const jfloat x4 = cos * pts[6] + sin * pts[7];
            const jfloat y4 = cos * pts[7] - sin * pts[6];
            Curve_setcubic(&this.c, x1, y1, x2, y2, x3, y3, x4, y4);
            break;
        }
        case 6:
            Curve_setquad(&this.c, x1, y1, x2, y2, x3, y3);
            break;
        }
    } else {
        Curve_set(&this.c, pts, type);
    }

    // we subdivide at values of t such that the remaining rotated
    // curves are monotonic in x and y.
    ret += Curve_dxRoots(&this.c, ts, ret);
    ret += Curve_dyRoots(&this.c, ts, ret);
    // subdivide at inflection points.
    if (type == 8) {
        // quadratic curves can't have inflection points
        ret += Curve_infPoints(&this.c, ts, ret);
    }

    // now we must subdivide at points where one of the offset curves will have
    // a cusp. This happens at ts where the radius of curvature is equal to w.
    ret += Curve_rootsOfROCMinusW(&this.c, ts, ret, w, 0.0001f);

    ret = Helpers_filterOutNotInAB(ts, 0, ret, 0.0001f, 0.9999f);
    Helpers_isort(ts, 0, ret);
    return ret;
}

static void Stroker_curveTo(PathConsumer *pStroker,
                           jfloat x1, jfloat y1,
                           jfloat x2, jfloat y2,
                           jfloat x3, jfloat y3)
{
    jfloat xf, yf, dxs, dys, dxf, dyf;
    jfloat mx, my;
    jint nSplits;
    jfloat prevT;
    jint i, kind;
    jboolean p1eqp2, p3eqp4;

    middle[0] = this.cx0; middle[1] = this.cy0;
    middle[2] = x1;  middle[3] = y1;
    middle[4] = x2;  middle[5] = y2;
    middle[6] = x3;  middle[7] = y3;

    // inlined version of somethingTo(8);
    // See the NOTE on somethingTo

    // need these so we can update the state at the end of this method
    xf = middle[6], yf = middle[7];
    dxs = middle[2] - middle[0];
    dys = middle[3] - middle[1];
    dxf = middle[6] - middle[4];
    dyf = middle[7] - middle[5];

    p1eqp2 = (dxs == 0.0f && dys == 0.0f);
    p3eqp4 = (dxf == 0.0f && dyf == 0.0f);
    if (p1eqp2) {
        dxs = middle[4] - middle[0];
        dys = middle[5] - middle[1];
        if (dxs == 0.0f && dys == 0.0f) {
            dxs = middle[6] - middle[0];
            dys = middle[7] - middle[1];
        }
    }
    if (p3eqp4) {
        dxf = middle[6] - middle[2];
        dyf = middle[7] - middle[3];
        if (dxf == 0.0f && dyf == 0.0f) {
            dxf = middle[6] - middle[0];
            dyf = middle[7] - middle[1];
        }
    }
    if (dxs == 0.0f && dys == 0.0f) {
        // this happens iff the "curve" is just a point
        Stroker_lineTo(pStroker, middle[0], middle[1]);
        return;
    }

    // if these vectors are too small, normalize them, to avoid future
    // precision problems.
    if (fabs(dxs) < 0.1f && fabs(dys) < 0.1f) {
        jfloat len = (jfloat) sqrt(dxs*dxs + dys*dys);
        dxs /= len;
        dys /= len;
    }
    if (fabs(dxf) < 0.1f && fabs(dyf) < 0.1f) {
        jfloat len = (jfloat) sqrt(dxf*dxf + dyf*dyf);
        dxf /= len;
        dyf /= len;
    }

    computeOffset(dxs, dys, this.lineWidth2, this.offset[0]);
    mx = this.offset[0][0];
    my = this.offset[0][1];
    drawJoin(pStroker,
             this.cdx, this.cdy, this.cx0, this.cy0,
             dxs, dys, this.cmx, this.cmy,
             mx, my);

    nSplits = findSubdivPoints(pStroker, middle, subdivTs, 8, this.lineWidth2);
    prevT = 0.0f;
    for (i = 0; i < nSplits; i++) {
        jfloat t = subdivTs[i];
        Helpers_subdivideCubicAt((t - prevT) / (1 - prevT),
                                 middle, i*6,
                                 middle, i*6,
                                 middle, i*6+6);
        prevT = t;
    }

    kind = 0;
    for (i = 0; i <= nSplits; i++) {
        kind = computeOffsetCubic(pStroker, middle, i*6, lp, rp);
        if (kind != 0) {
            emitLineTo(pStroker, lp[0], lp[1], JNI_FALSE);
            switch(kind) {
            case 8:
                emitCurveTo(pStroker, lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6], lp[7], JNI_FALSE);
                emitCurveTo(pStroker, rp[0], rp[1], rp[2], rp[3], rp[4], rp[5], rp[6], rp[7], JNI_TRUE);
                break;
            case 4:
                emitLineTo(pStroker, lp[2], lp[3], JNI_FALSE);
                emitLineTo(pStroker, rp[0], rp[1], JNI_TRUE);
                break;
            }
            emitLineTo(pStroker, rp[kind - 2], rp[kind - 1], JNI_TRUE);
        }
    }

    this.cmx = (lp[kind - 2] - rp[kind - 2]) / 2;
    this.cmy = (lp[kind - 1] - rp[kind - 1]) / 2;
    this.cdx = dxf;
    this.cdy = dyf;
    this.cx0 = xf;
    this.cy0 = yf;
    this.prev = DRAWING_OP_TO;
}

static void Stroker_quadTo(PathConsumer *pStroker,
                          jfloat x1, jfloat y1,
                          jfloat x2, jfloat y2)
{
    jfloat xf, yf, dxs, dys, dxf, dyf;
    jfloat mx, my;
    jint nSplits, i, kind;
    jfloat prevt;

    middle[0] = this.cx0; middle[1] = this.cy0;
    middle[2] = x1;  middle[3] = y1;
    middle[4] = x2;  middle[5] = y2;

    // inlined version of somethingTo(8);
    // See the NOTE on somethingTo

    // need these so we can update the state at the end of this method
    xf = middle[4], yf = middle[5];
    dxs = middle[2] - middle[0];
    dys = middle[3] - middle[1];
    dxf = middle[4] - middle[2];
    dyf = middle[5] - middle[3];
    if ((dxs == 0.0f && dys == 0.0f) || (dxf == 0.0f && dyf == 0.0f)) {
        dxs = dxf = middle[4] - middle[0];
        dys = dyf = middle[5] - middle[1];
    }
    if (dxs == 0.0f && dys == 0.0f) {
        // this happens iff the "curve" is just a point
        Stroker_lineTo(pStroker, middle[0], middle[1]);
        return;
    }
    // if these vectors are too small, normalize them, to avoid future
    // precision problems.
    if (fabs(dxs) < 0.1f && fabs(dys) < 0.1f) {
        jfloat len = (jfloat) sqrt(dxs*dxs + dys*dys);
        dxs /= len;
        dys /= len;
    }
    if (fabs(dxf) < 0.1f && fabs(dyf) < 0.1f) {
        jfloat len = (jfloat) sqrt(dxf*dxf + dyf*dyf);
        dxf /= len;
        dyf /= len;
    }

    computeOffset(dxs, dys, this.lineWidth2, this.offset[0]);
    mx = this.offset[0][0];
    my = this.offset[0][1];
    drawJoin(pStroker,
             this.cdx, this.cdy, this.cx0, this.cy0,
             dxs, dys, this.cmx, this.cmy,
             mx, my);

    nSplits = findSubdivPoints(pStroker, middle, subdivTs, 6, this.lineWidth2);
    prevt = 0.0f;
    for (i = 0; i < nSplits; i++) {
        jfloat t = subdivTs[i];
        Helpers_subdivideQuadAt((t - prevt) / (1 - prevt),
                                middle, i*4,
                                middle, i*4,
                                middle, i*4+4);
        prevt = t;
    }

    kind = 0;
    for (i = 0; i <= nSplits; i++) {
        kind = computeOffsetQuad(pStroker, middle, i*4, lp, rp);
        if (kind != 0) {
            emitLineTo(pStroker, lp[0], lp[1], JNI_FALSE);
            switch(kind) {
            case 6:
                emitQuadTo(pStroker, lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], JNI_FALSE);
                emitQuadTo(pStroker, rp[0], rp[1], rp[2], rp[3], rp[4], rp[5], JNI_TRUE);
                break;
            case 4:
                emitLineTo(pStroker, lp[2], lp[3], JNI_FALSE);
                emitLineTo(pStroker, rp[0], rp[1], JNI_TRUE);
                break;
            }
            emitLineTo(pStroker, rp[kind - 2], rp[kind - 1], JNI_TRUE);
        }
    }

    this.cmx = (lp[kind - 2] - rp[kind - 2]) / 2;
    this.cmy = (lp[kind - 1] - rp[kind - 1]) / 2;
    this.cdx = dxf;
    this.cdy = dyf;
    this.cx0 = xf;
    this.cy0 = yf;
    this.prev = DRAWING_OP_TO;
}

// a stack of polynomial curves where each curve shares endpoints with
// adjacent ones.
/*
    private static const class PolyStack {
        jfloat[] curves;
        int end;
        int[] curveTypes;
        int numCurves;
 */

#define INIT_SIZE   50

#undef this
#define this (*((PolyStack *)pStack))

void PolyStack_init(PolyStack *pStack) {
    this.curves = new_float(8 * INIT_SIZE);
    this.curvesSIZE = 8 * INIT_SIZE;
    this.curveTypes = new_int(INIT_SIZE);
    this.curveTypesSIZE = INIT_SIZE;
    this.end = 0;
    this.numCurves = 0;
}

void PolyStack_destroy(PolyStack *pStack) {
    free(this.curves);
    this.curves = NULL;
    this.curvesSIZE = 0;
    free(this.curveTypes);
    this.curveTypes = NULL;
    this.curveTypesSIZE = 0;
}

jboolean PolyStack_isEmpty(PolyStack *pStack) {
    return this.numCurves == 0;
}

static void ensureSpace(PolyStack *pStack, jint n) {
    if (this.end + n >= this.curvesSIZE) {
        jint newSize = (this.end + n) * 2;
        jfloat *newCurves = new_float(newSize);
        System_arraycopy(this.curves, 0, newCurves, 0, this.end);
        free(this.curves);
        this.curves = newCurves;
        this.curvesSIZE = newSize;
    }
    if (this.numCurves >= this.curveTypesSIZE) {
        jint newSize = this.numCurves * 2;
        jint *newTypes = new_int(newSize);
        System_arraycopy(this.curveTypes, 0, newTypes, 0, this.numCurves);
        free(this.curveTypes);
        this.curveTypes = newTypes;
        this.curveTypesSIZE = newSize;
    }
}

void PolyStack_pushCubic(PolyStack *pStack,
                         jfloat x0, jfloat y0,
                         jfloat x1, jfloat y1,
                         jfloat x2, jfloat y2)
{
    ensureSpace(pStack, 6);
    this.curveTypes[this.numCurves++] = 8;
    // assert(x0 == lastX && y0 == lastY)

    // we reverse the coordinate order to make popping easier
    this.curves[this.end++] = x2;    this.curves[this.end++] = y2;
    this.curves[this.end++] = x1;    this.curves[this.end++] = y1;
    this.curves[this.end++] = x0;    this.curves[this.end++] = y0;
}

void PolyStack_pushQuad(PolyStack *pStack,
                        jfloat x0, jfloat y0,
                        jfloat x1, jfloat y1)
{
    ensureSpace(pStack, 4);
    this.curveTypes[this.numCurves++] = 6;
    // assert(x0 == lastX && y0 == lastY)
    this.curves[this.end++] = x1;    this.curves[this.end++] = y1;
    this.curves[this.end++] = x0;    this.curves[this.end++] = y0;
}

void PolyStack_pushLine(PolyStack *pStack,
                        jfloat x, jfloat y)
{
    ensureSpace(pStack, 2);
    this.curveTypes[this.numCurves++] = 4;
    // assert(x0 == lastX && y0 == lastY)
    this.curves[this.end++] = x;    this.curves[this.end++] = y;
}

//@SuppressWarnings("unused")
/*
jint PolyStack_pop(PolyStack *pStack, jfloat pts[]) {
    jint ret = this.curveTypes[this.numCurves - 1];
    this.numCurves--;
    this.end -= (ret - 2);
    System_arraycopy(curves, end, pts, 0, ret - 2);
    return ret;
}
*/

void PolyStack_pop(PolyStack *pStack, PathConsumer *io) {
    jint type;

    this.numCurves--;
    type = this.curveTypes[this.numCurves];
    this.end -= (type - 2);
    switch(type) {
    case 8:
        io->curveTo(io,
                    this.curves[this.end+0], this.curves[this.end+1],
                    this.curves[this.end+2], this.curves[this.end+3],
                    this.curves[this.end+4], this.curves[this.end+5]);
        break;
    case 6:
        io->quadTo(io,
                   this.curves[this.end+0], this.curves[this.end+1],
                   this.curves[this.end+2], this.curves[this.end+3]);
            break;
    case 4:
        io->lineTo(io, this.curves[this.end], this.curves[this.end+1]);
    }
}

//@Override
/*
public String toString() {
    String ret = "";
    jint nc = numCurves;
    jint last = this.end;
    while (nc > 0) {
        nc--;
        jint type = curveTypes[numCurves];
        last -= (type - 2);
        switch(type) {
        case 8:
            ret += "cubic: ";
            break;
        case 6:
            ret += "quad: ";
            break;
        case 4:
            ret += "line: ";
            break;
        }
        ret += Arrays.toString(Arrays.copyOfRange(curves, last, last+type-2)) + "\n";
    }
    return ret;
}
 */
