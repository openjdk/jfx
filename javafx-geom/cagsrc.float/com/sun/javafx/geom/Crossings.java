/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Enumeration;

public abstract class Crossings {
    public static final boolean debug = false;

    int limit = 0;
    float yranges[] = new float[10];

    float xlo, ylo, xhi, yhi;

    public Crossings(float xlo, float ylo, float xhi, float yhi) {
        this.xlo = xlo;
        this.ylo = ylo;
        this.xhi = xhi;
        this.yhi = yhi;
    }

    public final float getXLo() {
        return xlo;
    }

    public final float getYLo() {
        return ylo;
    }

    public final float getXHi() {
        return xhi;
    }

    public final float getYHi() {
        return yhi;
    }

    public abstract void record(float ystart, float yend, int direction);

    public void print() {
        System.out.println("Crossings [");
        System.out.println("  bounds = ["+ylo+", "+yhi+"]");
        for (int i = 0; i < limit; i += 2) {
            System.out.println("  ["+yranges[i]+", "+yranges[i+1]+"]");
        }
        System.out.println("]");
    }

    public final boolean isEmpty() {
        return (limit == 0);
    }

    public abstract boolean covers(float ystart, float yend);

    public static Crossings findCrossings(Vector curves,
                                          float xlo, float ylo,
                                          float xhi, float yhi)
    {
        Crossings cross = new EvenOdd(xlo, ylo, xhi, yhi);
        Enumeration enum_ = curves.elements();
        while (enum_.hasMoreElements()) {
            Curve c = (Curve) enum_.nextElement();
            if (c.accumulateCrossings(cross)) {
                return null;
            }
        }
        if (debug) {
            cross.print();
        }
        return cross;
    }

    public final static class EvenOdd extends Crossings {
        public EvenOdd(float xlo, float ylo, float xhi, float yhi) {
            super(xlo, ylo, xhi, yhi);
        }

        public final boolean covers(float ystart, float yend) {
            return (limit == 2 && yranges[0] <= ystart && yranges[1] >= yend);
        }

        public void record(float ystart, float yend, int direction) {
            if (ystart >= yend) {
                return;
            }
            int from = 0;
            // Quickly jump over all pairs that are completely "above"
            while (from < limit && ystart > yranges[from+1]) {
                from += 2;
            }
            int to = from;
            while (from < limit) {
                float yrlo = yranges[from++];
                float yrhi = yranges[from++];
                if (yend < yrlo) {
                    // Quickly handle insertion of the new range
                    yranges[to++] = ystart;
                    yranges[to++] = yend;
                    ystart = yrlo;
                    yend = yrhi;
                    continue;
                }
                // The ranges overlap - sort, collapse, insert, iterate
                float yll, ylh, yhl, yhh;
                if (ystart < yrlo) {
                    yll = ystart;
                    ylh = yrlo;
                } else {
                    yll = yrlo;
                    ylh = ystart;
                }
                if (yend < yrhi) {
                    yhl = yend;
                    yhh = yrhi;
                } else {
                    yhl = yrhi;
                    yhh = yend;
                }
                if (ylh == yhl) {
                    ystart = yll;
                    yend = yhh;
                } else {
                    if (ylh > yhl) {
                        ystart = yhl;
                        yhl = ylh;
                        ylh = ystart;
                    }
                    if (yll != ylh) {
                        yranges[to++] = yll;
                        yranges[to++] = ylh;
                    }
                    ystart = yhl;
                    yend = yhh;
                }
                if (ystart >= yend) {
                    break;
                }
            }
            if (to < from && from < limit) {
                System.arraycopy(yranges, from, yranges, to, limit-from);
            }
            to += (limit-from);
            if (ystart < yend) {
                if (to >= yranges.length) {
                    float newranges[] = new float[to+10];
                    System.arraycopy(yranges, 0, newranges, 0, to);
                    yranges = newranges;
                }
                yranges[to++] = ystart;
                yranges[to++] = yend;
            }
            limit = to;
        }
    }
}
