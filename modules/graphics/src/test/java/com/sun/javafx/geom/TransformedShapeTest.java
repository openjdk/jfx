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

package com.sun.javafx.geom;

import static junit.framework.Assert.assertEquals;
import com.sun.javafx.geom.transform.BaseTransform;
import org.junit.Test;

/**
 *
 */
public class TransformedShapeTest {
    // Avoid integer coordinates so that float->double->float roundoff errors
    // do not end up causing "failures of questionable value"
    static Shape testShapes[] = {
        new Arc2D(9.5f, 9.5f, 26.2f, 16.1f, 0, 270, Arc2D.PIE),
        new Arc2D(9.5f, 9.5f, 26.2f, 16.1f, 0, 270, Arc2D.OPEN),
        new Arc2D(9.5f, 9.5f, 26.2f, 16.1f, 0, 270, Arc2D.CHORD),
        new CubicCurve2D(9.5f, 9.5f, 40.5f, 9.5f, 9.5f, 40.5f, 40.5f, 40.5f),
        new Ellipse2D(9.5f, 9.5f, 21f, 16f),
        new Line2D(9.5f, 9.5f, 40.5f, 40.5f),
        makePath(Path2D.WIND_EVEN_ODD),
        makePath(Path2D.WIND_NON_ZERO),
        new QuadCurve2D(9.5f, 9.5f, 20.5f, 40.5f, 40.5f, 9.5f),
        new RoundRectangle2D(9.5f, 9.5f, 21f, 16f, 5f, 5f),
    };

    static Shape makePath(int rule) {
        Path2D p2d = new Path2D(rule);
        p2d.moveTo(9.5f, 9.5f);
        p2d.lineTo(20.5f, 9.5f);
        p2d.quadTo(20.5f, 30.5f, 40.5f, 40.5f);
        p2d.lineTo(9.5f, 40.5f);
        p2d.curveTo(30.5f, 30.5f, 20.5f, 10.5f, 40.5f, 10.5f);
        p2d.lineTo(9.5f, 20.5f);
        p2d.closePath();
        return p2d;
    }

    static BaseTransform testTransforms[] = {
        BaseTransform.getTranslateInstance(5.125, 5.75),
        BaseTransform.getRotateInstance(Math.toRadians(45), 25, 25),
        BaseTransform.getScaleInstance(0.5, 0.5),
        BaseTransform.getScaleInstance(1.75, 1.6),
    };

    public @Test void testTranslatedShapes() {
        for (Shape s : testShapes) {
            test(TransformedShape.translatedShape(s, 5.125, 8.25));
            test(TransformedShape.translatedShape(s, -5.25, 8.125));
            test(TransformedShape.translatedShape(s, 5.125, -8.25));
            test(TransformedShape.translatedShape(s, -5.25, -8.125));
        }
    }

    public @Test void testTransformedShapes() {
        BaseTransform combinedtx = BaseTransform.IDENTITY_TRANSFORM;
        for (BaseTransform tx : testTransforms) {
            test(tx);
            combinedtx = combinedtx.deriveWithConcatenation(tx);
            test(combinedtx);
        }
    }

    static void test(BaseTransform tx) {
        for (Shape s : testShapes) {
            test(TransformedShape.transformedShape(s, tx));
        }
    }

    // Number of ulp to hunt around for a good answer for fuzzy testing
    static final int FUZZY = 5;

    static void test(TransformedShape s1) {
        BaseTransform tx = s1.getTransformNoClone();
        Shape raws1 = s1.getDelegateNoClone();
        Shape s2 = tx.createTransformedShape(raws1);
        for (int y = 0; y < 50; y++) {
            for (int x = 0; x < 50; x++) {
                boolean cp1 = s1.contains(x, y);
                boolean cp2 = s2.contains(x, y);
                boolean cr1 = s1.contains(x, y, 1, 1);
                boolean cr2 = s2.contains(x, y, 1, 1);
                boolean ir1 = s1.intersects(x, y, 1, 1);
                boolean ir2 = s2.intersects(x, y, 1, 1);
                boolean cpfail = (cp1 != cp2);
                boolean crfail = (cr1 != cr2);
                boolean irfail = (ir1 != ir2);
                if (cpfail || crfail || irfail) {
                    Float ulpx = Math.ulp(x);
                    Float ulpy = Math.ulp(y);
                    for (int i = -FUZZY; i <= +FUZZY; i++) {
                        float fy = y + ulpy * i;
                        for (int j = -FUZZY; j <= +FUZZY; j++) {
                            float fx = x + ulpx * j;
                            cpfail = cpfail && (s1.contains(fx, fy) !=
                                                s2.contains(fx, fy));
                            crfail = crfail && (s1.contains(fx, fy, 1, 1) !=
                                                s2.contains(fx, fy, 1, 1));
                            irfail = irfail && (s1.intersects(fx, fy, 1, 1) !=
                                                s2.intersects(fx, fy, 1, 1));
                        }
                    }
                    System.err.println("testing: "+raws1+" transformed by "+tx);
                         if (cpfail) { assertEquals(cp2, cp1); }
                    else if (crfail) { assertEquals(cr2, cr1); }
                    else if (irfail) { assertEquals(ir2, ir1); }
                    else {
                        System.err.println("fuzzy test required for ("+x+", "+y+")");
                    }
                }
            }
        }
    }
}
