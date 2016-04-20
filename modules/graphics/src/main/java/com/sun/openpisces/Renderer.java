/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.openpisces;

import com.sun.javafx.geom.PathConsumer2D;
import java.util.Arrays;

/**
 */
public final class Renderer implements PathConsumer2D {

    private final class ScanlineIterator {

        private int[] crossings;
        private int[] edgePtrs;
        private int edgeCount;

        // crossing bounds. The bounds are not necessarily tight (the scan line
        // at minY, for example, might have no crossings). The x bounds will
        // be accumulated as crossings are computed.
        private int nextY;

        private static final int INIT_CROSSINGS_SIZE = 10;

        private ScanlineIterator() {
            crossings = new int[INIT_CROSSINGS_SIZE];
            edgePtrs = new int[INIT_CROSSINGS_SIZE];
            reset();
        }

        public void reset() {
            // We don't care if we clip some of the line off with ceil, since
            // no scan line crossings will be eliminated (in fact, the ceil is
            // the y of the first scan line crossing).
            nextY = sampleRowMin;
            edgeCount = 0;
        }

        private int next() {
            // TODO: make function that convert from y value to bucket idx?
            // (RT-26922)
            int cury = nextY++;
            int bucket = cury - boundsMinY;
            int count = this.edgeCount;
            int ptrs[] = this.edgePtrs;
            float edges[] = Renderer.this.edges;
            int bucketcount = edgeBuckets[bucket*2 + 1];
            if ((bucketcount & 0x1) != 0) {
                int newCount = 0;
                for (int i = 0; i < count; i++) {
                    int ecur = ptrs[i];
                    if (edges[ecur+YMAX] > cury) {
                        ptrs[newCount++] = ecur;
                    }
                }
                count = newCount;
            }
            ptrs = Helpers.widenArray(ptrs, count, bucketcount >> 1);
            for (int ecur = edgeBuckets[bucket*2];
                 ecur != 0;
                 ecur = (int)edges[ecur+NEXT])
            {
                ptrs[count++] = --ecur;
                // REMIND: Adjust start Y if necessary
            }
            this.edgePtrs = ptrs;
            this.edgeCount = count;
//            if ((count & 0x1) != 0) {
//                System.out.println("ODD NUMBER OF EDGES!!!!");
//            }
            int xings[] = this.crossings;
            if (xings.length < count) {
                this.crossings = xings = new int[ptrs.length];
            }
            for (int i = 0; i < count; i++) {
                int ecur = ptrs[i];
                float curx = edges[ecur+CURX];
                int cross = ((int) Math.ceil(curx - 0.5f)) << 1;
                edges[ecur+CURX] = curx + edges[ecur+SLOPE];
                if (edges[ecur+OR] > 0) {
                    cross |= 1;
                }
                int j = i;
                while (--j >= 0) {
                    int jcross = xings[j];
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

        private boolean hasNext() {
            return nextY < sampleRowMax;
        }

        private int curY() {
            return nextY - 1;
        }
    }


//////////////////////////////////////////////////////////////////////////////
//  EDGE LIST
//////////////////////////////////////////////////////////////////////////////
// TODO(maybe): very tempting to use fixed point here. A lot of opportunities
// for shifts and just removing certain operations altogether. (RT-26922)

    // common to all types of input path segments.
    private static final int YMAX = 0;
    private static final int CURX = 1;
    // NEXT and OR are meant to be indeces into "int" fields, but arrays must
    // be homogenous, so every field is a float. However floats can represent
    // exactly up to 26 bit ints, so we're ok.
    private static final int OR   = 2;
    private static final int SLOPE = 3;
    private static final int NEXT = 4;
    private static final int SIZEOF_EDGE = 5;

    private int sampleRowMin;
    private int sampleRowMax;
    private float edgeMinX;
    private float edgeMaxX;

    private float[] edges;
    private int[] edgeBuckets;
    private int numEdges;

    private static final float DEC_BND = 1.0f;
    private static final float INC_BND = 0.4f;

    // each bucket is a linked list. this method adds eptr to the
    // start "bucket"th linked list.
    private void addEdgeToBucket(final int eptr, final int bucket) {
        // we could implement this in terms of insertEdge, but this is a special
        // case, so we optimize a bit.
        edges[eptr+NEXT] = edgeBuckets[bucket*2];
        edgeBuckets[bucket*2] = eptr + 1;
        edgeBuckets[bucket*2 + 1] += 2;
    }

    // Flattens using adaptive forward differencing. This only carries out
    // one iteration of the AFD loop. All it does is update AFD variables (i.e.
    // X0, Y0, D*[X|Y], COUNT; not variables used for computing scanline crossings).
    private void quadBreakIntoLinesAndAdd(float x0, float y0,
                                          final Curve c,
                                          final float x2, final float y2)
    {
        final float QUAD_DEC_BND = 32;
        final int countlg = 4;
        int count = 1 << countlg;
        int countsq = count * count;
        float maxDD = Math.max(c.dbx / countsq, c.dby / countsq);
        while (maxDD > QUAD_DEC_BND) {
            maxDD /= 4;
            count <<= 1;
        }

        countsq = count * count;
        final float ddx = c.dbx / countsq;
        final float ddy = c.dby / countsq;
        float dx = c.bx / countsq + c.cx / count;
        float dy = c.by / countsq + c.cy / count;

        while (count-- > 1) {
            float x1 = x0 + dx;
            dx += ddx;
            float y1 = y0 + dy;
            dy += ddy;
            addLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
        addLine(x0, y0, x2, y2);
    }

    // x0, y0 and x3,y3 are the endpoints of the curve. We could compute these
    // using c.xat(0),c.yat(0) and c.xat(1),c.yat(1), but this might introduce
    // numerical errors, and our callers already have the exact values.
    // Another alternative would be to pass all the control points, and call c.set
    // here, but then too many numbers are passed around.
    private void curveBreakIntoLinesAndAdd(float x0, float y0,
                                           final Curve c,
                                           final float x3, final float y3)
    {
        final int countlg = 3;
        int count = 1 << countlg;

        // the dx and dy refer to forward differencing variables, not the last
        // coefficients of the "points" polynomial
        float dddx, dddy, ddx, ddy, dx, dy;
        dddx = 2f * c.dax / (1 << (3 * countlg));
        dddy = 2f * c.day / (1 << (3 * countlg));

        ddx = dddx + c.dbx / (1 << (2 * countlg));
        ddy = dddy + c.dby / (1 << (2 * countlg));
        dx = c.ax / (1 << (3 * countlg)) + c.bx / (1 << (2 * countlg)) + c.cx / (1 << countlg);
        dy = c.ay / (1 << (3 * countlg)) + c.by / (1 << (2 * countlg)) + c.cy / (1 << countlg);

        // we use x0, y0 to walk the line
        float x1 = x0, y1 = y0;
        while (count > 0) {
            while (Math.abs(ddx) > DEC_BND || Math.abs(ddy) > DEC_BND) {
                dddx /= 8;
                dddy /= 8;
                ddx = ddx/4 - dddx;
                ddy = ddy/4 - dddy;
                dx = (dx - ddx) / 2;
                dy = (dy - ddy) / 2;
                count <<= 1;
            }
            // can only do this on even "count" values, because we must divide count by 2
            while (count % 2 == 0 && Math.abs(dx) <= INC_BND && Math.abs(dy) <= INC_BND) {
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
            addLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
    }

    private void addLine(float x1, float y1, float x2, float y2) {
        float or = 1; // orientation of the line. 1 if y increases, 0 otherwise.
        if (y2 < y1) {
            or = y2; // no need to declare a temp variable. We have or.
            y2 = y1;
            y1 = or;
            or = x2;
            x2 = x1;
            x1 = or;
            or = 0;
        }
        final int firstCrossing = Math.max((int) Math.ceil(y1 - 0.5f), boundsMinY);
        final int lastCrossing = Math.min((int) Math.ceil(y2 - 0.5f), boundsMaxY);
        if (firstCrossing >= lastCrossing) {
            return;
        }
        if (firstCrossing < sampleRowMin) { sampleRowMin = firstCrossing; }
        if (lastCrossing > sampleRowMax) { sampleRowMax = lastCrossing; }

        final float slope = (x2 - x1) / (y2 - y1);

        if (slope > 0) { // <==> x1 < x2
            if (x1 < edgeMinX) { edgeMinX = x1; }
            if (x2 > edgeMaxX) { edgeMaxX = x2; }
        } else {
            if (x2 < edgeMinX) { edgeMinX = x2; }
            if (x1 > edgeMaxX) { edgeMaxX = x1; }
        }

        final int ptr = numEdges * SIZEOF_EDGE;
        edges = Helpers.widenArray(edges, ptr, SIZEOF_EDGE);
        numEdges++;
        edges[ptr+OR] = or;
        edges[ptr+CURX] = x1 + (firstCrossing + 0.5f - y1) * slope;
        edges[ptr+SLOPE] = slope;
        edges[ptr+YMAX] = lastCrossing;
        final int bucketIdx = firstCrossing - boundsMinY;
        addEdgeToBucket(ptr, bucketIdx);
        edgeBuckets[(lastCrossing - boundsMinY)*2 + 1] |= 1;
    }

// END EDGE LIST
//////////////////////////////////////////////////////////////////////////////


    public static final int WIND_EVEN_ODD = 0;
    public static final int WIND_NON_ZERO = 1;

    // Antialiasing
    final private int SUBPIXEL_LG_POSITIONS_X;
    final private int SUBPIXEL_LG_POSITIONS_Y;
    final private int SUBPIXEL_POSITIONS_X;
    final private int SUBPIXEL_POSITIONS_Y;
    final private int SUBPIXEL_MASK_X;
    final private int SUBPIXEL_MASK_Y;
    final int MAX_AA_ALPHA;

    // Bounds of the drawing region, at subpixel precision.
    private int boundsMinX, boundsMinY, boundsMaxX, boundsMaxY;

    // Current winding rule
    private int windingRule;

    // Current drawing position, i.e., final point of last segment
    private float x0, y0;

    // Position of most recent 'moveTo' command
    private float pix_sx0, pix_sy0;

    public Renderer(int subpixelLgPositionsX, int subpixelLgPositionsY)
    {
        this.SUBPIXEL_LG_POSITIONS_X = subpixelLgPositionsX;
        this.SUBPIXEL_LG_POSITIONS_Y = subpixelLgPositionsY;
        this.SUBPIXEL_POSITIONS_X = 1 << (SUBPIXEL_LG_POSITIONS_X);
        this.SUBPIXEL_POSITIONS_Y = 1 << (SUBPIXEL_LG_POSITIONS_Y);
        this.SUBPIXEL_MASK_X = SUBPIXEL_POSITIONS_X - 1;
        this.SUBPIXEL_MASK_Y = SUBPIXEL_POSITIONS_Y - 1;
        this.MAX_AA_ALPHA = (SUBPIXEL_POSITIONS_X * SUBPIXEL_POSITIONS_Y);
    }

    public Renderer(int subpixelLgPositionsX, int subpixelLgPositionsY,
            int pix_boundsX, int pix_boundsY,
            int pix_boundsWidth, int pix_boundsHeight,
            int windingRule)
    {
        this(subpixelLgPositionsX, subpixelLgPositionsY);
        reset(pix_boundsX, pix_boundsY, pix_boundsWidth, pix_boundsHeight,
              windingRule);
    }

    public void reset(int pix_boundsX, int pix_boundsY,
                      int pix_boundsWidth, int pix_boundsHeight,
                      int windingRule)
    {
        this.windingRule = windingRule;

        this.boundsMinX = pix_boundsX * SUBPIXEL_POSITIONS_X;
        this.boundsMinY = pix_boundsY * SUBPIXEL_POSITIONS_Y;
        this.boundsMaxX = (pix_boundsX + pix_boundsWidth) * SUBPIXEL_POSITIONS_X;
        this.boundsMaxY = (pix_boundsY + pix_boundsHeight) * SUBPIXEL_POSITIONS_Y;

        this.edgeMinX = Float.POSITIVE_INFINITY;
        this.edgeMaxX = Float.NEGATIVE_INFINITY;
        this.sampleRowMax = boundsMinY;
        this.sampleRowMin = boundsMaxY;

        int numBuckets = boundsMaxY - boundsMinY;
        if (edgeBuckets == null || edgeBuckets.length < numBuckets*2+2) {
            // The last 2 entries are ignored and only used to store unused
            // values for segments ending on the last line of the bounds
            // so we can avoid having to check the bounds on this array.
            edgeBuckets = new int[numBuckets*2 + 2];
        } else {
            // Only need to fill the first numBuckets*2 entries since the
            // last 2 entries are write-only for overflow avoidance only.
            Arrays.fill(edgeBuckets, 0, numBuckets*2, 0);
        }
        if (edges == null) {
            edges = new float[SIZEOF_EDGE * 32];
        }
        numEdges = 0;
        pix_sx0 = pix_sy0 = x0 = y0 = 0f;
    }

    private float tosubpixx(float pix_x) {
        return pix_x * SUBPIXEL_POSITIONS_X;
    }
    private float tosubpixy(float pix_y) {
        return pix_y * SUBPIXEL_POSITIONS_Y;
    }

    public void moveTo(float pix_x0, float pix_y0) {
        closePath();
        this.pix_sx0 = pix_x0;
        this.pix_sy0 = pix_y0;
        this.y0 = tosubpixy(pix_y0);
        this.x0 = tosubpixx(pix_x0);
    }

    public void lineTo(float pix_x1, float pix_y1) {
        float x1 = tosubpixx(pix_x1);
        float y1 = tosubpixy(pix_y1);
        addLine(x0, y0, x1, y1);
        x0 = x1;
        y0 = y1;
    }

    private Curve c = new Curve();
    @Override public void curveTo(float x1, float y1,
                                  float x2, float y2,
                                  float x3, float y3)
    {
        final float xe = tosubpixx(x3);
        final float ye = tosubpixy(y3);
        c.set(x0, y0, tosubpixx(x1), tosubpixy(y1), tosubpixx(x2), tosubpixy(y2), xe, ye);
        curveBreakIntoLinesAndAdd(x0, y0, c, xe, ye);
        x0 = xe;
        y0 = ye;
    }

    @Override public void quadTo(float x1, float y1, float x2, float y2) {
        final float xe = tosubpixx(x2);
        final float ye = tosubpixy(y2);
        c.set(x0, y0, tosubpixx(x1), tosubpixy(y1), xe, ye);
        quadBreakIntoLinesAndAdd(x0, y0, c, xe, ye);
        x0 = xe;
        y0 = ye;
    }

    public void closePath() {
        // lineTo expects its input in pixel coordinates.
        lineTo(pix_sx0, pix_sy0);
    }

    public void pathDone() {
        closePath();
    }

    private int savedAlpha[];
    private ScanlineIterator savedIterator;
    public void produceAlphas(AlphaConsumer ac) {
        ac.setMaxAlpha(MAX_AA_ALPHA);

        // Mask to determine the relevant bit of the crossing sum
        // 0x1 if EVEN_ODD, all bits if NON_ZERO
        int mask = (windingRule == WIND_EVEN_ODD) ? 0x1 : ~0x0;

        // add 2 to better deal with the last pixel in a pixel row.
        int width = ac.getWidth();
        int alpha[] = savedAlpha;
        if (alpha == null || alpha.length < width+2) {
            savedAlpha = alpha = new int[width+2];
        } else {
            Arrays.fill(alpha, 0, width+2, 0);
        }

        int bboxx0 = ac.getOriginX() << SUBPIXEL_LG_POSITIONS_X;
        int bboxx1 = bboxx0 + (width << SUBPIXEL_LG_POSITIONS_X);

        // Now we iterate through the scanlines. We must tell emitRow the coord
        // of the first non-transparent pixel, so we must keep accumulators for
        // the first and last pixels of the section of the current pixel row
        // that we will emit.
        // We also need to accumulate pix_bbox*, but the iterator does it
        // for us. We will just get the values from it once this loop is done
        int pix_maxX = bboxx1 >> SUBPIXEL_LG_POSITIONS_X;
        int pix_minX = bboxx0 >> SUBPIXEL_LG_POSITIONS_Y;

        int y = boundsMinY; // needs to be declared here so we emit the last row properly.
        ScanlineIterator it = savedIterator;
        if (it == null) {
            savedIterator = it = new ScanlineIterator();
        } else {
            it.reset();
        }
        for ( ; it.hasNext(); ) {
            int numCrossings = it.next();
            int[] crossings = it.crossings;
            y = it.curY();

            if (numCrossings > 0) {
                int lowx = crossings[0] >> 1;
                int highx = crossings[numCrossings - 1] >> 1;
                int x0 = Math.max(lowx, bboxx0);
                int x1 = Math.min(highx, bboxx1);

                pix_minX = Math.min(pix_minX, x0 >> SUBPIXEL_LG_POSITIONS_X);
                pix_maxX = Math.max(pix_maxX, x1 >> SUBPIXEL_LG_POSITIONS_X);
            }

            int sum = 0;
            int prev = bboxx0;
            for (int i = 0; i < numCrossings; i++) {
                int curxo = crossings[i];
                int curx = curxo >> 1;
                int crorientation = ((curxo & 0x1) << 1) - 1;
                if ((sum & mask) != 0) {
                    int x0 = Math.max(prev, bboxx0);
                    int x1 = Math.min(curx, bboxx1);
                    if (x0 < x1) {
                        x0 -= bboxx0; // turn x0, x1 from coords to indices
                        x1 -= bboxx0; // in the alpha array.

                        int pix_x = x0 >> SUBPIXEL_LG_POSITIONS_X;
                        int pix_xmaxm1 = (x1 - 1) >> SUBPIXEL_LG_POSITIONS_X;

                        if (pix_x == pix_xmaxm1) {
                            // Start and end in same pixel
                            alpha[pix_x] += (x1 - x0);
                            alpha[pix_x+1] -= (x1 - x0);
                        } else {
                            int pix_xmax = x1 >> SUBPIXEL_LG_POSITIONS_X;
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
                ac.setAndClearRelativeAlphas(alpha, y >> SUBPIXEL_LG_POSITIONS_Y,
                                             pix_minX, pix_maxX);
                pix_maxX = bboxx1 >> SUBPIXEL_LG_POSITIONS_X;
                pix_minX = bboxx0 >> SUBPIXEL_LG_POSITIONS_Y;
            }
        }

        // Emit final row.
        // Note, if y is on a MASK row then it was already sent above...
        if ((y & SUBPIXEL_MASK_Y) < SUBPIXEL_MASK_Y) {
            ac.setAndClearRelativeAlphas(alpha, y >> SUBPIXEL_LG_POSITIONS_Y,
                                         pix_minX, pix_maxX);
        }
    }

    public int getSubpixMinX() {
        int sampleColMin = (int) Math.ceil(edgeMinX - 0.5f);
        if (sampleColMin < boundsMinX) sampleColMin = boundsMinX;
        return sampleColMin;
    }

    public int getSubpixMaxX() {
        int sampleColMax = (int) Math.ceil(edgeMaxX - 0.5f);
        if (sampleColMax > boundsMaxX) sampleColMax = boundsMaxX;
        return sampleColMax;
    }

    public int getSubpixMinY() {
        return sampleRowMin;
    }

    public int getSubpixMaxY() {
        return sampleRowMax;
    }

    public int getOutpixMinX() {
        return (getSubpixMinX() >> SUBPIXEL_LG_POSITIONS_X);
    }

    public int getOutpixMaxX() {
        return (getSubpixMaxX() + SUBPIXEL_MASK_X) >> SUBPIXEL_LG_POSITIONS_X;
    }

    public int getOutpixMinY() {
        return (sampleRowMin >> SUBPIXEL_LG_POSITIONS_Y);
    }

    public int getOutpixMaxY() {
        return (sampleRowMax + SUBPIXEL_MASK_Y) >> SUBPIXEL_LG_POSITIONS_Y;
    }
}
