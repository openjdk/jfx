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
#include <jni.h>
#include <string.h>

#include "Helpers.h"
#include "Renderer.h"
#include "AlphaConsumer.h"

//public final class Renderer implements PathConsumer2D {

//    private final class ScanlineIterator {

#define this (*((ScanlineIterator *) pIterator))

static void ScanlineIterator_reset(ScanlineIterator *pIterator,
                                   Renderer *pRenderer);

static void ScanlineIterator_init(ScanlineIterator *pIterator,
                                  Renderer *pRenderer)
{
    this.crossings = new_int(INIT_CROSSINGS_SIZE);
    this.crossingsSIZE = INIT_CROSSINGS_SIZE;
    this.edgePtrs = new_int(INIT_CROSSINGS_SIZE);
    this.edgePtrsSIZE = INIT_CROSSINGS_SIZE;
    ScanlineIterator_reset(pIterator, pRenderer);
}

static void ScanlineIterator_destroy(ScanlineIterator *pIterator) {
    free(this.crossings);
    this.crossings = NULL;
    this.crossingsSIZE = 0;
    free(this.edgePtrs);
    this.edgePtrs = NULL;
    this.edgePtrsSIZE = 0;
}

static void ScanlineIterator_reset(ScanlineIterator *pIterator,
                                   Renderer *pRenderer)
{
    // We don't care if we clip some of the line off with ceil, since
    // no scan line crossings will be eliminated (in fact, the ceil is
    // the y of the first scan line crossing).
    this.nextY = pRenderer->sampleRowMin;
    this.edgeCount = 0;
}

static jint ScanlineIterator_next(ScanlineIterator *pIterator, Renderer *pRenderer) {
    jint i, ecur;
    jint *xings;
    // NOTE: make function that convert from y value to bucket idx?
    jint cury = this.nextY++;
    jint bucket = cury - pRenderer->boundsMinY;
    jint count = this.edgeCount;
    jint *ptrs = this.edgePtrs;
    jfloat *edges = pRenderer->edges;
    jint bucketcount = pRenderer->edgeBuckets[bucket*2 + 1];

    if ((bucketcount & 0x1) != 0) {
        jint newCount = 0;
        jint i;
        for (i = 0; i < count; i++) {
            jint ecur = ptrs[i];
            if (edges[ecur+YMAX] > cury) {
                ptrs[newCount++] = ecur;
            }
        }
        count = newCount;
    }
    if (this.edgePtrsSIZE < count + (bucketcount >> 1)) {
        jint newSize = (count + (bucketcount >> 1)) * 2;
        jint *newPtrs = new_int(newSize);
        System_arraycopy(this.edgePtrs, 0, newPtrs, 0, count);
        free(this.edgePtrs);
        this.edgePtrs = newPtrs;
        this.edgePtrsSIZE = newSize;
    }
    ptrs = this.edgePtrs;
    for (ecur = pRenderer->edgeBuckets[bucket*2];
         ecur != 0;
         ecur = (jint) edges[ecur+NEXT])
    {
        ptrs[count++] = --ecur;
        // REMIND: Adjust start Y if necessary
    }
    this.edgePtrs = ptrs;
    this.edgeCount = count;
//    if ((count & 0x1) != 0) {
//        System.out.println("ODD NUMBER OF EDGES!!!!");
//    }
    xings = this.crossings;
    if (this.crossingsSIZE < count) {
        free(this.crossings);
        this.crossings = xings = new_int(this.edgePtrsSIZE);
        this.crossingsSIZE = this.edgePtrsSIZE;
    }
    for (i = 0; i < count; i++) {
        jint ecur = ptrs[i];
        jfloat curx = edges[ecur+CURX];
        jint cross = ((jint) curx) << 1;
        jint j;
        edges[ecur+CURX] = curx + edges[ecur+SLOPE];
        if (edges[ecur+OR] > 0) {
            cross |= 1;
        }
        j = i;
        while (--j >= 0) {
            jint jcross = xings[j];
            if (jcross <= cross) {
                break;
            }
            xings[j+1] = jcross;
            ptrs[j+1] = ptrs[j];
        }
        xings[j+1] = cross;
        ptrs[j+1] = ecur;
    }
    return count;
}

static jboolean ScanlineIterator_hasNext(ScanlineIterator *pIterator, Renderer *pRenderer) {
    return this.nextY < pRenderer->sampleRowMax;
}

static jint ScanlineIterator_curY(ScanlineIterator *pIterator) {
    return this.nextY - 1;
}

#undef this
#define this (*((Renderer *) pRenderer))

//////////////////////////////////////////////////////////////////////////////
//  EDGE LIST
//////////////////////////////////////////////////////////////////////////////
// NOTE(maybe): very tempting to use fixed point here. A lot of opportunities
// for shifts and just removing certain operations altogether.


// each bucket is a linked list. this method adds eptr to the
// start "bucket"th linked list.
static void addEdgeToBucket(PathConsumer *pRenderer, const jint eptr, const jint bucket) {
    // we could implement this in terms of insertEdge, but this is a special
    // case, so we optimize a bit.
    this.edges[eptr+NEXT] = (jfloat) this.edgeBuckets[bucket*2];
    this.edgeBuckets[bucket*2] = eptr + 1;
    this.edgeBuckets[bucket*2 + 1] += 2;
}

static void addLine(PathConsumer *pRenderer,
                    jfloat x1, jfloat y1,
                    jfloat x2, jfloat y2);

// Flattens using adaptive forward differencing. This only carries out
// one iteration of the AFD loop. All it does is update AFD variables (i.e.
// X0, Y0, D*[X|Y], COUNT; not variables used for computing scanline crossings).
static void quadBreakIntoLinesAndAdd(PathConsumer *pRenderer,
                                     jfloat x0, jfloat y0,
                                     const Curve c,
                                     const jfloat x2, const jfloat y2)
{
    jfloat ddx, ddy, dx, dy;
    const jfloat QUAD_DEC_BND = 32;
    const jint countlg = 4;
    jint count = 1 << countlg;
    jint countsq = count * count;
    jfloat maxDD = Math_max(c.dbx / countsq, c.dby / countsq);
    while (maxDD > QUAD_DEC_BND) {
        maxDD /= 4;
        count <<= 1;
    }

    countsq = count * count;
    ddx = c.dbx / countsq;
    ddy = c.dby / countsq;
    dx = c.bx / countsq + c.cx / count;
    dy = c.by / countsq + c.cy / count;

    while (count-- > 1) {
        jfloat x1 = x0 + dx;
        jfloat y1 = y0 + dy;
        dx += ddx;
        dy += ddy;
        addLine(pRenderer, x0, y0, x1, y1);
        x0 = x1;
        y0 = y1;
    }
    addLine(pRenderer, x0, y0, x2, y2);
}

// x0, y0 and x3,y3 are the endpoints of the curve. We could compute these
// using c.xat(0),c.yat(0) and c.xat(1),c.yat(1), but this might introduce
// numerical errors, and our callers already have the exact values.
// Another alternative would be to pass all the control points, and call c.set
// here, but then too many numbers are passed around.
static void curveBreakIntoLinesAndAdd(PathConsumer *pRenderer,
                                      jfloat x0, jfloat y0,
                                      const Curve c,
                                      const jfloat x3, const jfloat y3)
{
    const jint countlg = 3;
    jint count = 1 << countlg;
    jfloat x1, y1;

    // the dx and dy refer to forward differencing variables, not the last
    // coefficients of the "points" polynomial
    jfloat dddx, dddy, ddx, ddy, dx, dy;
    dddx = 2.0f * c.dax / (1 << (3 * countlg));
    dddy = 2.0f * c.day / (1 << (3 * countlg));

    ddx = dddx + c.dbx / (1 << (2 * countlg));
    ddy = dddy + c.dby / (1 << (2 * countlg));
    dx = c.ax / (1 << (3 * countlg)) + c.bx / (1 << (2 * countlg)) + c.cx / (1 << countlg);
    dy = c.ay / (1 << (3 * countlg)) + c.by / (1 << (2 * countlg)) + c.cy / (1 << countlg);

    // we use x0, y0 to walk the line
    x1 = x0;
    y1 = y0;
    while (count > 0) {
        while (fabs(ddx) > DEC_BND || fabs(ddy) > DEC_BND) {
            dddx /= 8;
            dddy /= 8;
            ddx = ddx/4 - dddx;
            ddy = ddy/4 - dddy;
            dx = (dx - ddx) / 2;
            dy = (dy - ddy) / 2;
            count <<= 1;
        }
        // can only do this on even "count" values, because we must divide count by 2
        while (count % 2 == 0 && fabs(dx) <= INC_BND && fabs(dy) <= INC_BND) {
            dx = 2 * dx + ddx;
            dy = 2 * dy + ddy;
            ddx = 4 * (ddx + dddx);
            ddy = 4 * (ddy + dddy);
            dddx = 8 * dddx;
            dddy = 8 * dddy;
            count >>= 1;
        }
        count--;
        if (count > 0) {
            x1 += dx;
            dx += ddx;
            ddx += dddx;
            y1 += dy;
            dy += ddy;
            ddy += dddy;
        } else {
            x1 = x3;
            y1 = y3;
        }
        addLine(pRenderer, x0, y0, x1, y1);
        x0 = x1;
        y0 = y1;
    }
}

static void addLine(PathConsumer *pRenderer,
                    jfloat x1, jfloat y1,
                    jfloat x2, jfloat y2)
{
    jfloat or = 1; // orientation of the line. 1 if y increases, 0 otherwise.
    jint firstCrossing, lastCrossing;
    jfloat slope;
    jint ptr, bucketIdx;

    if (y2 < y1) {
        or = y2; // no need to declare a temp variable. We have or.
        y2 = y1;
        y1 = or;
        or = x2;
        x2 = x1;
        x1 = or;
        or = 0;
    }
    firstCrossing = Math_max((jint) ceil(y1), this.boundsMinY);
    lastCrossing = Math_min((jint) ceil(y2), this.boundsMaxY);
    if (firstCrossing >= lastCrossing) {
        return;
    }
    if (firstCrossing < this.sampleRowMin) { this.sampleRowMin = firstCrossing; }
    if (lastCrossing > this.sampleRowMax) { this.sampleRowMax = lastCrossing; }

    slope = (x2 - x1) / (y2 - y1);

    if (slope > 0) { // <==> x1 < x2
        if (x1 < this.edgeMinX) { this.edgeMinX = x1; }
        if (x2 > this.edgeMaxX) { this.edgeMaxX = x2; }
    } else {
        if (x2 < this.edgeMinX) { this.edgeMinX = x2; }
        if (x1 > this.edgeMaxX) { this.edgeMaxX = x1; }
    }

    ptr = this.numEdges * SIZEOF_EDGE;
    if (this.edgesSIZE < ptr + SIZEOF_EDGE) {
        jint newSize = (ptr + SIZEOF_EDGE) * 2;
        jfloat *newEdges = new_float(newSize);
        System_arraycopy(this.edges, 0, newEdges, 0, ptr);
        free(this.edges);
        this.edges = newEdges;
        this.edgesSIZE = newSize;
    }
    this.numEdges++;
    this.edges[ptr+OR] = or;
    this.edges[ptr+CURX] = x1 + (firstCrossing - y1) * slope;
    this.edges[ptr+SLOPE] = slope;
    this.edges[ptr+YMAX] = (jfloat) lastCrossing;
    bucketIdx = firstCrossing - this.boundsMinY;
    addEdgeToBucket(pRenderer, ptr, bucketIdx);
    this.edgeBuckets[(lastCrossing - this.boundsMinY)*2 + 1] |= 1;
}

// END EDGE LIST
//////////////////////////////////////////////////////////////////////////////
static MoveToFunc       Renderer_moveTo;
static LineToFunc       Renderer_lineTo;
static QuadToFunc       Renderer_quadTo;
static CurveToFunc      Renderer_curveTo;
static ClosePathFunc    Renderer_closePath;
static PathDoneFunc     Renderer_pathDone;

// Antialiasing
static jint SUBPIXEL_LG_POSITIONS_X;
static jint SUBPIXEL_LG_POSITIONS_Y;
static jint SUBPIXEL_POSITIONS_X;
static jint SUBPIXEL_POSITIONS_Y;
static jint SUBPIXEL_MASK_X;
static jint SUBPIXEL_MASK_Y;
//static jint MAX_AA_ALPHA;
static jbyte *alphaMap;

static void setMaxAlpha(jint maxalpha);

void Renderer_setup(jint subpixelLgPositionsX, jint subpixelLgPositionsY) {
    SUBPIXEL_LG_POSITIONS_X = subpixelLgPositionsX;
    SUBPIXEL_LG_POSITIONS_Y = subpixelLgPositionsY;
    SUBPIXEL_POSITIONS_X = 1 << (SUBPIXEL_LG_POSITIONS_X);
    SUBPIXEL_POSITIONS_Y = 1 << (SUBPIXEL_LG_POSITIONS_Y);
    SUBPIXEL_MASK_X = SUBPIXEL_POSITIONS_X - 1;
    SUBPIXEL_MASK_Y = SUBPIXEL_POSITIONS_Y - 1;
//    MAX_AA_ALPHA = (SUBPIXEL_POSITIONS_X * SUBPIXEL_POSITIONS_Y);
    setMaxAlpha((SUBPIXEL_POSITIONS_X * SUBPIXEL_POSITIONS_Y));
}

void Renderer_init(Renderer *pRenderer) {
    memset(pRenderer, 0, sizeof(Renderer));
    PathConsumer_init(&pRenderer->consumer,
                      Renderer_moveTo,
                      Renderer_lineTo,
                      Renderer_quadTo,
                      Renderer_curveTo,
                      Renderer_closePath,
                      Renderer_pathDone);
}

void Renderer_reset(Renderer *pRenderer,
                    jint pix_boundsX, jint pix_boundsY,
                    jint pix_boundsWidth, jint pix_boundsHeight,
                    jint windingRule)
{
    jint numBuckets;

    this.windingRule = windingRule;

    this.boundsMinX = pix_boundsX * SUBPIXEL_POSITIONS_X;
    this.boundsMinY = pix_boundsY * SUBPIXEL_POSITIONS_Y;
    this.boundsMaxX = (pix_boundsX + pix_boundsWidth) * SUBPIXEL_POSITIONS_X;
    this.boundsMaxY = (pix_boundsY + pix_boundsHeight) * SUBPIXEL_POSITIONS_Y;

    this.edgeMinX = BIGGEST_FLOAT;
    this.edgeMaxX = -BIGGEST_FLOAT;
    this.sampleRowMax = this.boundsMinY;
    this.sampleRowMin = this.boundsMaxY;

    numBuckets = this.boundsMaxY - this.boundsMinY;
    if (this.edgeBuckets == NULL || this.edgeBucketsSIZE < numBuckets*2+2) {
        // The last 2 entries are ignored and only used to store unused
        // values for segments ending on the last line of the bounds
        // so we can avoid having to check the bounds on this array.
        this.edgeBuckets = new_int(numBuckets*2 + 2);
        this.edgeBucketsSIZE = numBuckets*2 + 2;
    } else {
        // Only need to fill the first numBuckets*2 entries since the
        // last 2 entries are write-only for overflow avoidance only.
        Arrays_fill(this.edgeBuckets, 0, numBuckets*2, 0);
    }
    if (this.edges == NULL) {
        this.edges = new_float(SIZEOF_EDGE * 32);
        this.edgesSIZE = SIZEOF_EDGE * 32;
    }
    this.numEdges = 0;
    this.pix_sx0 = this.pix_sy0 = this.x0 = this.y0 = 0.0f;
}

void Renderer_destroy(Renderer *pRenderer) {
    free(pRenderer->edgeBuckets);
    pRenderer->edgeBuckets = NULL;
    pRenderer->edgeBucketsSIZE = 0;
    free(pRenderer->edges);
    pRenderer->edges = NULL;
    pRenderer->edgesSIZE = 0;
}

static jfloat tosubpixx(jfloat pix_x) {
    return pix_x * SUBPIXEL_POSITIONS_X;
}
static jfloat tosubpixy(jfloat pix_y) {
    return pix_y * SUBPIXEL_POSITIONS_Y;
}

static void Renderer_moveTo(PathConsumer *pRenderer,
                            jfloat pix_x0, jfloat pix_y0)
{
    Renderer_closePath(pRenderer);
    this.pix_sx0 = pix_x0;
    this.pix_sy0 = pix_y0;
    this.y0 = tosubpixy(pix_y0);
    this.x0 = tosubpixx(pix_x0);
}

static void Renderer_lineTo(PathConsumer *pRenderer,
                            jfloat pix_x1, jfloat pix_y1)
{
    jfloat x1 = tosubpixx(pix_x1);
    jfloat y1 = tosubpixy(pix_y1);
    addLine(pRenderer, this.x0, this.y0, x1, y1);
    this.x0 = x1;
    this.y0 = y1;
}

static void Renderer_curveTo(PathConsumer *pRenderer,
                             jfloat x1, jfloat y1,
                             jfloat x2, jfloat y2,
                             jfloat x3, jfloat y3)
{
    const jfloat xe = tosubpixx(x3);
    const jfloat ye = tosubpixy(y3);
    Curve_setcubic(&this.c,
                   this.x0, this.y0,
                   tosubpixx(x1), tosubpixy(y1),
                   tosubpixx(x2), tosubpixy(y2),
                   xe, ye);
    curveBreakIntoLinesAndAdd(pRenderer, this.x0, this.y0, this.c, xe, ye);
    this.x0 = xe;
    this.y0 = ye;
}

void Renderer_quadTo(PathConsumer *pRenderer,
                     jfloat x1, jfloat y1,
                     jfloat x2, jfloat y2)
{
    const jfloat xe = tosubpixx(x2);
    const jfloat ye = tosubpixy(y2);
    Curve_setquad(&this.c,
                  this.x0, this.y0,
                  tosubpixx(x1), tosubpixy(y1),
                  xe, ye);
    quadBreakIntoLinesAndAdd(pRenderer, this.x0, this.y0, this.c, xe, ye);
    this.x0 = xe;
    this.y0 = ye;
}

static void Renderer_closePath(PathConsumer *pRenderer) {
    // lineTo expects its input in pixel coordinates.
    Renderer_lineTo(pRenderer, this.pix_sx0, this.pix_sy0);
}

static void Renderer_pathDone(PathConsumer *pRenderer) {
    Renderer_closePath(pRenderer);
}

static void setAndClearRelativeAlphas(AlphaConsumer *pAC,
                                      jint alphaRow[], jint pix_y,
                                      jint pix_from, jint pix_to);

void Renderer_produceAlphas(Renderer *pRenderer, AlphaConsumer *pAC) {
//    ac.setMaxAlpha(MAX_AA_ALPHA);

    // Mask to determine the relevant bit of the crossing sum
    // 0x1 if EVEN_ODD, all bits if NON_ZERO
    jint mask = (this.windingRule == WIND_EVEN_ODD) ? 0x1 : ~0x0;
    jint bboxx0, bboxx1;
    jint pix_minX, pix_maxX;
    jint y;
    ScanlineIterator it;

    // add 2 to better deal with the last pixel in a pixel row.
    jint width = pAC->width;
    jint savedAlpha[1024];
    jint *alpha;
    if (1024 < width+2) {
        alpha = new_int(width+2);
    } else {
        alpha = savedAlpha;
    }
    Arrays_fill(alpha, 0, width+2, 0);

    bboxx0 = pAC->originX << SUBPIXEL_LG_POSITIONS_X;
    bboxx1 = bboxx0 + (width << SUBPIXEL_LG_POSITIONS_X);

    // Now we iterate through the scanlines. We must tell emitRow the coord
    // of the first non-transparent pixel, so we must keep accumulators for
    // the first and last pixels of the section of the current pixel row
    // that we will emit.
    // We also need to accumulate pix_bbox*, but the iterator does it
    // for us. We will just get the values from it once this loop is done
    pix_maxX = bboxx1 >> SUBPIXEL_LG_POSITIONS_X;
    pix_minX = bboxx0 >> SUBPIXEL_LG_POSITIONS_Y;

    y = this.boundsMinY; // needs to be declared here so we emit the last row properly.
    ScanlineIterator_init(&it, pRenderer);
    for ( ; ScanlineIterator_hasNext(&it, pRenderer); ) {
        jint numCrossings = ScanlineIterator_next(&it, pRenderer);
        jint *crossings = it.crossings;
        jint sum, prev;
        jint i;

        y = ScanlineIterator_curY(&it);

        if (numCrossings > 0) {
            jint lowx = crossings[0] >> 1;
            jint highx = crossings[numCrossings - 1] >> 1;
            jint x0 = Math_max(lowx, bboxx0);
            jint x1 = Math_min(highx, bboxx1);

            pix_minX = Math_min(pix_minX, x0 >> SUBPIXEL_LG_POSITIONS_X);
            pix_maxX = Math_max(pix_maxX, x1 >> SUBPIXEL_LG_POSITIONS_X);
        }

        sum = 0;
        prev = bboxx0;
        for (i = 0; i < numCrossings; i++) {
            jint curxo = crossings[i];
            jint curx = curxo >> 1;
            jint crorientation = ((curxo & 0x1) << 1) - 1;
            if ((sum & mask) != 0) {
                jint x0 = Math_max(prev, bboxx0);
                jint x1 = Math_min(curx, bboxx1);
                if (x0 < x1) {
                    jint pix_x, pix_xmaxm1;

                    x0 -= bboxx0; // turn x0, x1 from coords to indices
                    x1 -= bboxx0; // in the alpha array.

                    pix_x = x0 >> SUBPIXEL_LG_POSITIONS_X;
                    pix_xmaxm1 = (x1 - 1) >> SUBPIXEL_LG_POSITIONS_X;

                    if (pix_x == pix_xmaxm1) {
                        // Start and end in same pixel
                        alpha[pix_x] += (x1 - x0);
                        alpha[pix_x+1] -= (x1 - x0);
                    } else {
                        jint pix_xmax = x1 >> SUBPIXEL_LG_POSITIONS_X;
                        alpha[pix_x] += SUBPIXEL_POSITIONS_X - (x0 & SUBPIXEL_MASK_X);
                        alpha[pix_x+1] += (x0 & SUBPIXEL_MASK_X);
                        alpha[pix_xmax] -= SUBPIXEL_POSITIONS_X - (x1 & SUBPIXEL_MASK_X);
                        alpha[pix_xmax+1] -= (x1 & SUBPIXEL_MASK_X);
                    }
                }
            }
            sum += crorientation;
            prev = curx;
        }

        // even if this last row had no crossings, alpha will be zeroed
        // from the last emitRow call. But this doesn't matter because
        // maxX < minX, so no row will be emitted to the cache.
        if ((y & SUBPIXEL_MASK_Y) == SUBPIXEL_MASK_Y) {
            setAndClearRelativeAlphas(pAC, alpha, y >> SUBPIXEL_LG_POSITIONS_Y,
                                      pix_minX, pix_maxX);
            pix_maxX = bboxx1 >> SUBPIXEL_LG_POSITIONS_X;
            pix_minX = bboxx0 >> SUBPIXEL_LG_POSITIONS_Y;
        }
    }

    // Emit final row.
    // Note, if y is on a MASK row then it was already sent above...
    if ((y & SUBPIXEL_MASK_Y) < SUBPIXEL_MASK_Y) {
        setAndClearRelativeAlphas(pAC, alpha, y >> SUBPIXEL_LG_POSITIONS_Y,
                                  pix_minX, pix_maxX);
    }
    ScanlineIterator_destroy(&it);
    if (alpha != savedAlpha) free (alpha);
}

//@Override
static void setMaxAlpha(jint maxalpha) {
    jint i;

    alphaMap = malloc(maxalpha+1);
    for (i = 0; i <= maxalpha; i++) {
        alphaMap[i] = (jbyte) ((i*255 + maxalpha/2)/maxalpha);
    }
}

static void setAndClearRelativeAlphas(AlphaConsumer *pAC,
                                      jint alphaRow[], jint pix_y,
                                      jint pix_from, jint pix_to)
{
//    System.out.println("setting row "+(pix_y - y)+
//                       " out of "+width+" x "+height);
    jint w = pAC->width;
    jint off = (pix_y - pAC->originY) * w;
    jbyte *out = pAC->alphas;
    jint a = 0;
    jint i;
    for (i = 0; i < w; i++) {
        a += alphaRow[i];
        alphaRow[i] = 0;
        out[off+i] = alphaMap[a];
    }
}

static jint getSubpixMinX(Renderer *pRenderer) {
    jint sampleColMin = (jint) ceil(this.edgeMinX);
    if (sampleColMin < this.boundsMinX) sampleColMin = this.boundsMinX;
    return sampleColMin;
}

static jint getSubpixMaxX(Renderer *pRenderer) {
    jint sampleColMax = (jint) ceil(this.edgeMaxX);
    if (sampleColMax > this.boundsMaxX) sampleColMax = this.boundsMaxX;
    return sampleColMax;
}

static jint getSubpixMinY(Renderer *pRenderer) {
    return this.sampleRowMin;
}

static jint getSubpixMaxY(Renderer *pRenderer) {
    return this.sampleRowMax;
}

static jint getOutpixMinX(Renderer *pRenderer) {
    return (getSubpixMinX(pRenderer) >> SUBPIXEL_LG_POSITIONS_X);
}

static jint getOutpixMaxX(Renderer *pRenderer) {
    return (getSubpixMaxX(pRenderer) + SUBPIXEL_MASK_X) >> SUBPIXEL_LG_POSITIONS_X;
}

static jint getOutpixMinY(Renderer *pRenderer) {
    return (this.sampleRowMin >> SUBPIXEL_LG_POSITIONS_Y);
}

static jint getOutpixMaxY(Renderer *pRenderer) {
    return (this.sampleRowMax + SUBPIXEL_MASK_Y) >> SUBPIXEL_LG_POSITIONS_Y;
}

void Renderer_getOutputBounds(Renderer *pRenderer, jint bounds[]) {
    bounds[0] = getOutpixMinX(pRenderer);
    bounds[1] = getOutpixMinY(pRenderer);
    bounds[2] = getOutpixMaxX(pRenderer);
    bounds[3] = getOutpixMaxY(pRenderer);
}
