/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.geom;

import com.sun.javafx.geom.Path2D;
import org.junit.Test;

/**
 * @test
 * @bug 8169294
 * @summary Check the growth algorithm (needRoom) in JavaFX Path2D
 */
/*
Before Patch:
 - Test(Path2D[0]) ---
testAddMoves[1000000] duration= 16.319813 ms.
testAddLines[1000000] duration= 1685.904265 ms.
testAddQuads[1000000] duration= 6435.015055999999 ms.
testAddCubics[1000000] duration= 14643.259248999999 ms.
testAddMoveAndCloses[1000000] duration= 2269.6810179999998 ms.

 - Test(Path2D) ---
testAddMoves[1000000] duration= 4.645376 ms.
testAddLines[1000000] duration= 1673.896613 ms.
testAddQuads[1000000] duration= 6448.857066 ms.
testAddCubics[1000000] duration= 14679.410602999998 ms.
testAddMoveAndCloses[1000000] duration= 2278.352159 ms.

After patch:
 - Test(Path2D[0]) ---
testAddMoves[1000000] duration= 15.889125 ms.
testAddLines[1000000] duration= 37.788070999999995 ms.
testAddQuads[1000000] duration= 57.228248 ms.
testAddCubics[1000000] duration= 62.25714 ms.
testAddMoveAndCloses[1000000] duration= 41.76611 ms.

 - Test(Path2D) ---
testAddMoves[1000000] duration= 15.857171999999998 ms.
testAddLines[1000000] duration= 28.228354999999997 ms.
testAddQuads[1000000] duration= 38.190948 ms.
testAddCubics[1000000] duration= 52.453748999999995 ms.
testAddMoveAndCloses[1000000] duration= 26.837844 ms.
 */
public class Path2DGrowTest {

    public static final int N = 1000 * 1000;

    private static boolean verbose = false;
    private static boolean force = false;

    static void echo(String msg) {
        System.out.println(msg);
    }

    static void log(String msg) {
        if (verbose || force) {
            echo(msg);
        }
    }

    @Test(timeout=10000)
    public void testEmptyFloatPaths() {
        echo("\n - Test: new Path2D(0) ---");
        test(() -> new Path2D(Path2D.WIND_NON_ZERO, 0));
    }

    @Test(timeout=10000)
    public void testFloatPaths() {
        echo("\n - Test: new Path2D() ---");
        test(() -> new Path2D());
    }

    interface PathFactory {
        Path2D makePath();
    }

    static void test(PathFactory pf) {
        long start, end;

        for (int n = 1; n <= N; n *= 10) {
            force = (n == N);

            start = System.nanoTime();
            testAddMoves(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddMoves[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddLines(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddLines[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddQuads(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddQuads[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddCubics(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddCubics[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");

            start = System.nanoTime();
            testAddMoveAndCloses(pf.makePath(), n);
            end = System.nanoTime();
            log("testAddMoveAndCloses[" + n + "] duration= "
                + (1e-6 * (end - start)) + " ms.");
        }
    }

    static void addMove(Path2D p2d, int i) {
        p2d.moveTo(1.0f * i, 0.5f * i);
    }

    static void addLine(Path2D p2d, int i) {
        p2d.lineTo(1.1f * i, 2.3f * i);
    }

    static void addCubic(Path2D p2d, int i) {
        p2d.curveTo(1.1f * i, 1.2f * i, 1.3f * i, 1.4f * i, 1.5f * i, 1.6f * i);
    }

    static void addQuad(Path2D p2d, int i) {
        p2d.quadTo(1.1f * i, 1.2f * i, 1.3f * i, 1.4f * i);
    }

    static void addClose(Path2D p2d) {
        p2d.closePath();
    }

    static void testAddMoves(Path2D pathA, int n) {
        for (int i = 0; i < n; i++) {
            addMove(pathA, i);
        }
    }

    static void testAddLines(Path2D pathA, int n) {
        addMove(pathA, 0);
        for (int i = 0; i < n; i++) {
            addLine(pathA, i);
        }
    }

    static void testAddQuads(Path2D pathA, int n) {
        addMove(pathA, 0);
        for (int i = 0; i < n; i++) {
            addQuad(pathA, i);
        }
    }

    static void testAddCubics(Path2D pathA, int n) {
        addMove(pathA, 0);
        for (int i = 0; i < n; i++) {
            addCubic(pathA, i);
        }
    }

    static void testAddMoveAndCloses(Path2D pathA, int n) {
        for (int i = 0; i < n; i++) {
            addMove(pathA, i);
            addClose(pathA);
        }
    }
}
