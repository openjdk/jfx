/*
 * Copyright (c) 1998, 2022, Oracle and/or its affiliates. All rights reserved.
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

final class Order0 extends Curve {
    private double x;
    private double y;

    public Order0(double x, double y) {
        super(INCREASING);
        this.x = x;
        this.y = y;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public double getXTop() {
        return x;
    }

    @Override
    public double getYTop() {
        return y;
    }

    @Override
    public double getXBot() {
        return x;
    }

    @Override
    public double getYBot() {
        return y;
    }

    @Override
    public double getXMin() {
        return x;
    }

    @Override
    public double getXMax() {
        return x;
    }

    @Override
    public double getX0() {
        return x;
    }

    @Override
    public double getY0() {
        return y;
    }

    @Override
    public double getX1() {
        return x;
    }

    @Override
    public double getY1() {
        return y;
    }

    @Override
    public double XforY(double y) {
        return y;
    }

    @Override
    public double TforY(double y) {
        return 0;
    }

    @Override
    public double XforT(double t) {
        return x;
    }

    @Override
    public double YforT(double t) {
        return y;
    }

    @Override
    public double dXforT(double t, int deriv) {
        return 0;
    }

    @Override
    public double dYforT(double t, int deriv) {
        return 0;
    }

    @Override
    public double nextVertical(double t0, double t1) {
        return t1;
    }

    @Override
    public int crossingsFor(double x, double y) {
        return 0;
    }

    @Override
    public boolean accumulateCrossings(Crossings c) {
        return (x > c.getXLo() &&
                x < c.getXHi() &&
                y > c.getYLo() &&
                y < c.getYHi());
    }

    @Override
    public void enlarge(RectBounds r) {
        r.add((float) x, (float) y);
    }

    @Override
    public Curve getSubCurve(double ystart, double yend, int dir) {
        return this;
    }

    @Override
    public Curve getReversedCurve() {
        return this;
    }

    @Override
    public int getSegment(float coords[]) {
        coords[0] = (float) x;
        coords[1] = (float) y;
        return PathIterator.SEG_MOVETO;
    }
}
