/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package rt_6334;

import com.sun.javafx.geom.CubicCurve2D;
import com.sun.javafx.geom.Line2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.QuadCurve2D;
import com.sun.javafx.geom.Shape;
import com.sun.prism.BasicStroke;
import org.junit.Test;

public class rt_6334Test {
    static int numcoords[] = { 2, 2, 4, 6, 0 };
    static boolean verbose;

    public static void testPath(Shape orig, float thickness) {
        BasicStroke stroke =
            new BasicStroke(thickness, BasicStroke.CAP_SQUARE,
                            BasicStroke.JOIN_MITER, 10f);
        Shape result = stroke.createStrokedShape(orig);
        PathIterator pi = result.getPathIterator(null);
        float coords[] = new float[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            int ncoords = numcoords[type];
            if (verbose) {
                System.out.print("MLQCX".charAt(type)+"[");
            }
            for (int i = 0; i < ncoords; i++) {
                if (verbose) {
                    if (i > 0) System.out.print(", ");
                    System.out.print(coords[i]);
                }
                if (coords[i] < Float.POSITIVE_INFINITY &&
                    coords[i] > Float.NEGATIVE_INFINITY)
                {
                    continue;
                } else {
                    throw new InternalError("non-finite coordinate generated: "+coords[i]);
                }
            }
            if (verbose) {
                System.out.println("]");
            }
            pi.next();
        }
    }

    @Test(timeout=1000)
    public void test_6334() {
        Path2D p = new Path2D();
        p.moveTo(304.51f, 179.78f);
        p.quadTo(301.00f, 180.78f, 305.20f, 180.76f);
        p.quadTo(305.35f, 180.76f, 304.51f, 179.78f);
        p.closePath();
        testPath(p, 1f);
    }

    static float rndFlt() {
        return (float) Math.random();
    }
    static float rndCoord() {
        return rndFlt() * 2f + 300f;
    }

    @Test(timeout=5000)
    public void testLines() {
        Line2D l = new Line2D();
        for (int i = 0; i < 50000; i++) {
            l.setLine(rndCoord(), rndCoord(),
                      rndCoord(), rndCoord());
            testPath(l, 1f);
            testPath(l, rndFlt() * 10f);
            testPath(l, 20f);
        }
    }

    @Test(timeout=5000)
    public void testQuads() {
        QuadCurve2D qc = new QuadCurve2D();
        for (int i = 0; i < 50000; i++) {
            qc.setCurve(rndCoord(), rndCoord(),
                        rndCoord(), rndCoord(),
                        rndCoord(), rndCoord());
            testPath(qc, 1f);
            testPath(qc, rndFlt() * 10f);
            testPath(qc, 20f);
        }
    }

    @Test(timeout=5000)
    public void testCubics() {
        CubicCurve2D cc = new CubicCurve2D();
        for (int i = 0; i < 50000; i++) {
            cc.setCurve(rndCoord(), rndCoord(),
                        rndCoord(), rndCoord(),
                        rndCoord(), rndCoord(),
                        rndCoord(), rndCoord());
            testPath(cc, 1f);
            testPath(cc, rndFlt() * 10f);
            testPath(cc, 20f);
        }
    }
}
