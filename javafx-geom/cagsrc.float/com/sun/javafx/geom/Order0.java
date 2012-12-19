/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

import java.util.Vector;

final class Order0 extends Curve {
    private float x;
    private float y;

    public Order0(float x, float y) {
        super(INCREASING);
        this.x = x;
        this.y = y;
    }

    public int getOrder() {
        return 0;
    }

    public float getXTop() {
        return x;
    }

    public float getYTop() {
        return y;
    }

    public float getXBot() {
        return x;
    }

    public float getYBot() {
        return y;
    }

    public float getXMin() {
        return x;
    }

    public float getXMax() {
        return x;
    }

    public float getX0() {
        return x;
    }

    public float getY0() {
        return y;
    }

    public float getX1() {
        return x;
    }

    public float getY1() {
        return y;
    }

    public float XforY(float y) {
        return y;
    }

    public float TforY(float y) {
        return 0;
    }

    public float XforT(float t) {
        return x;
    }

    public float YforT(float t) {
        return y;
    }

    public float dXforT(float t, int deriv) {
        return 0;
    }

    public float dYforT(float t, int deriv) {
        return 0;
    }

    public float nextVertical(float t0, float t1) {
        return t1;
    }

    public int crossingsFor(float x, float y) {
        return 0;
    }

    public boolean accumulateCrossings(Crossings c) {
        return (x > c.getXLo() &&
                x < c.getXHi() &&
                y > c.getYLo() &&
                y < c.getYHi());
    }

    public void enlarge(RectBounds r) {
        r.add(x, y);
    }

    public Curve getSubCurve(float ystart, float yend, int dir) {
        return this;
    }

    public Curve getReversedCurve() {
        return this;
    }

    public int getSegment(float coords[]) {
        coords[0] = x;
        coords[1] = y;
        return PathIterator.SEG_MOVETO;
    }
}
