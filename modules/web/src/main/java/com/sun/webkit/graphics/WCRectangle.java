/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

public final class WCRectangle {
    float x;
    float y;
    float w;
    float h;

    public WCRectangle(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public WCRectangle(WCRectangle r) {
        this.x = r.x;
        this.y = r.y;
        this.w = r.w;
        this.h = r.h;
    }

    public WCRectangle() {
    }

    public float getX() {
        return x;
    }

    public int getIntX() {
        return (int)x;
    }

    public float getY() {
        return y;
    }

    public int getIntY() {
        return (int)y;
    }

    public float getWidth() {
        return w;
    }

    public int getIntWidth() {
        return (int)w;
    }

    public float getHeight() {
        return h;
    }

    public int getIntHeight() {
        return (int)h;
    }

    public boolean contains(WCRectangle r) {
        return x <= r.x && x + w >= r.x + r.w && y <= r.y && y + h >= r.y + r.h;
    }

    /**
     * Determines whether or not this <code>WCRectangle</code> and the specified
     * <code>WCRectangle</code> intersect. Two rectangles intersect if
     * their intersection is nonempty.
     *
     * @param r the specified <code>WCRectangle</code>
     * @return    <code>true</code> if the specified <code>WCRectangle</code>
     *            and this <code>WCRectangle</code> intersect;
     *            <code>false</code> otherwise.
     */
    public WCRectangle intersection(WCRectangle r) {
        float tx1 = this.x;
        float ty1 = this.y;
        float rx1 = r.x;
        float ry1 = r.y;
        float tx2 = tx1; tx2 += this.w;
        float ty2 = ty1; ty2 += this.h;
        float rx2 = rx1; rx2 += r.w;
        float ry2 = ry1; ry2 += r.h;
        if (tx1 < rx1) tx1 = rx1;
        if (ty1 < ry1) ty1 = ry1;
        if (tx2 > rx2) tx2 = rx2;
        if (ty2 > ry2) ty2 = ry2;
        tx2 -= tx1;
        ty2 -= ty1;
        // tx2,ty2 will never overflow (they will never be
        // larger than the smallest of the two source w,h)
        // they might underflow, though...
        if (tx2 < Float.MIN_VALUE) tx2 = Float.MIN_VALUE;
        if (ty2 < Float.MIN_VALUE) ty2 = Float.MIN_VALUE;
        return new WCRectangle(tx1, ty1, tx2, ty2);
    }

    /**
     * Translates this <code>WCRectangle</code> the indicated distance,
     * to the right along the X coordinate axis, and
     * downward along the Y coordinate axis.
     * @param dx the distance to move this <code>WCRectangle</code>
     *                 along the X axis
     * @param dy the distance to move this <code>WCRectangle</code>
     *                 along the Y axis
     */
    public void translate(float dx, float dy) {
        float oldv = this.x;
        float newv = oldv + dx;
        if (dx < 0) {
            // moving leftward
            if (newv > oldv) {
                // negative overflow
                // Only adjust width if it was valid (>= 0).
                if (w >= 0) {
                    // The right edge is now conceptually at
                    // newv+width, but we may move newv to prevent
                    // overflow.  But we want the right edge to
                    // remain at its new location in spite of the
                    // clipping.  Think of the following adjustment
                    // conceptually the same as:
                    // width += newv; newv = MIN_VALUE; width -= newv;
                    w += newv - Float.MIN_VALUE;
                    // width may go negative if the right edge went past
                    // MIN_VALUE, but it cannot overflow since it cannot
                    // have moved more than MIN_VALUE and any non-negative
                    // number + MIN_VALUE does not overflow.
                }
                newv = Float.MIN_VALUE;
            }
        } else {
            // moving rightward (or staying still)
            if (newv < oldv) {
                // positive overflow
                if (w >= 0) {
                    // Conceptually the same as:
                    // width += newv; newv = MAX_VALUE; width -= newv;
                    w += newv - Float.MAX_VALUE;
                    // With large widths and large displacements
                    // we may overflow so we need to check it.
                    if (w < 0) w = Float.MAX_VALUE;
                }
                newv = Float.MAX_VALUE;
            }
        }
        this.x = newv;

        oldv = this.y;
        newv = oldv + dy;
        if (dy < 0) {
            // moving upward
            if (newv > oldv) {
                // negative overflow
                if (h >= 0) {
                    h += newv - Float.MIN_VALUE;
                    // See above comment about no overflow in this case
                }
                newv = Float.MIN_VALUE;
            }
        } else {
            // moving downward (or staying still)
            if (newv < oldv) {
                // positive overflow
                if (h >= 0) {
                    h += newv - Float.MAX_VALUE;
                    if (h < 0) h = Float.MAX_VALUE;
                }
                newv = Float.MAX_VALUE;
            }
        }
        this.y = newv;
    }

    public WCRectangle createUnion(WCRectangle r) {
        WCRectangle dest = new WCRectangle();
        WCRectangle.union(this, r, dest);
        return dest;
    }

    public static void union(WCRectangle src1,
                             WCRectangle src2,
                             WCRectangle dest)
    {
        float x1 = Math.min(src1.getMinX(), src2.getMinX());
        float y1 = Math.min(src1.getMinY(), src2.getMinY());
        float x2 = Math.max(src1.getMaxX(), src2.getMaxX());
        float y2 = Math.max(src1.getMaxY(), src2.getMaxY());
        dest.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    public void setFrameFromDiagonal(float x1, float y1, float x2, float y2) {
        if (x2 < x1) {
            float t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y2 < y1) {
            float t = y1;
            y1 = y2;
            y2 = t;
        }
        setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public void setFrame(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public float getMinX() {
        return getX();
    }

    public float getMaxX() {
        return getX() + getWidth();
    }

    public float getMinY() {
        return getY();
    }

    public float getMaxY() {
        return getY() + getHeight();
    }

    public boolean isEmpty() {
        return (w  <= 0) || (h  <= 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WCRectangle) {
            WCRectangle rc = (WCRectangle)obj;
            return (x == rc.x) && (y == rc.y) && (w == rc.w) && (h == rc.h);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "WCRectangle{x:" + x + " y:" + y + " w:" + w + " h:" + h + "}";
    }
}

