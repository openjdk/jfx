/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.BaseTransform;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class Path2DTest {
    void checkLine(PathIterator pi, float x1, float y1, float x2, float y2) {
        float coords[] = new float[2];
        assertFalse(pi.isDone());
        assertEquals(PathIterator.SEG_MOVETO, pi.currentSegment(coords));
        assertEquals(x1, coords[0], .001);
        assertEquals(y1, coords[1], .001);
        assertFalse(pi.isDone());
        pi.next();
        assertFalse(pi.isDone());
        assertEquals(PathIterator.SEG_LINETO, pi.currentSegment(coords));
        assertEquals(x2, coords[0], .001);
        assertEquals(y2, coords[1], .001);
        assertFalse(pi.isDone());
        pi.next();
        assertTrue(pi.isDone());
    }

    void checkAndResetPaths(Path2D pref, Path2D ptest, float curx, float cury) {
        checkAndResetPaths(pref, ptest, curx, cury, false);
    }

    void checkAndResetPaths(Path2D pref, Path2D ptest,
                            float curx, float cury,
                            boolean verbose)
    {
        assertEquals(curx, pref.getCurrentX(), .001);
        assertEquals(cury, pref.getCurrentY(), .001);
        checkShapes(pref, ptest, verbose);
        pref.reset();
        ptest.reset();
    }

    void checkShapes(Shape sref, Shape stest) {
        checkShapes(sref, stest, false);
    }

    void checkShapes(Shape sref, Shape stest, boolean verbose) {
        checkPaths(sref.getPathIterator(BaseTransform.IDENTITY_TRANSFORM),
                   stest.getPathIterator(BaseTransform.IDENTITY_TRANSFORM),
                   verbose);
    }

    void checkPaths(PathIterator piref, PathIterator pitest) {
        checkPaths(piref, pitest, false);
    }

    static int numcoords[];
    static {
        numcoords = new int[5];
        numcoords[PathIterator.SEG_MOVETO] = 2;
        numcoords[PathIterator.SEG_LINETO] = 2;
        numcoords[PathIterator.SEG_QUADTO] = 4;
        numcoords[PathIterator.SEG_CUBICTO] = 6;
        numcoords[PathIterator.SEG_CLOSE] = 0;
    }

    void checkPaths(PathIterator piref, PathIterator pitest, boolean verbose) {
        float coordsref[] = new float[6];
        float coordstest[] = new float[6];
        while (!piref.isDone()) {
            assertFalse(pitest.isDone());
            int typref = piref.currentSegment(coordsref);
            int typtest = pitest.currentSegment(coordstest);
            assertEquals(typref, typtest);
            if (verbose) System.out.println("type = "+typref);
            for (int i = 0; i < numcoords[typref]; i++) {
                assertEquals(coordsref[i], coordstest[i], .001);
                if (verbose) System.out.println("coord["+i+"] = "+coordsref[i]);
            }
            assertFalse(pitest.isDone());
            piref.next();
            pitest.next();
        }
        assertTrue(pitest.isDone());
    }

    double angle(double ux, double uy, double vx, double vy) {
        double sgn = (ux * vy - uy * vx) > 0 ? 1f : -1f;
        double dot = ux * vx + uy * vy;
        double ulen = Math.hypot(ux, uy);
        double vlen = Math.hypot(vx, vy);
        double cos = dot / (ulen * vlen);
        if (cos < -1f) cos = -1f;
        else if (cos > 1f) cos = 1f;
        return sgn * Math.acos(cos);
    }

    void checkArcTo(float x1, float y1,
                    float rw, float rh, float arcrad,
                    boolean largeArcs, boolean sweepFlag,
                    float x2, float y2)
    {
//        System.out.println("rw="+rw+", rh="+rh+", phi="+arcrad+", fA="+largeArcs+", fS="+sweepFlag+", x="+x2+", y="+y2);
        // Comparing to math specified at:
        // http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes
        Path2D path = new Path2D();
        path.moveTo(x1, y1);
        path.arcTo(rw/2f, rh/2f, arcrad, largeArcs, sweepFlag, x2, y2);
        double rx = rw/2.0;
        double ry = rh/2.0;
        if (rx == 0 || ry == 0) {
            checkLine(path.getPathIterator(BaseTransform.IDENTITY_TRANSFORM),
                      x1, y1, x2, y2);
            return;
        }
        double cosphi = Math.cos(arcrad);
        double sinphi = Math.sin(arcrad);
        double x1p =  cosphi * ((x1 - x2) / 2f) + sinphi * ((y1 - y2) / 2f);
        double y1p = -sinphi * ((x1 - x2) / 2f) + cosphi * ((y1 - y2) / 2f);
        double x1psq = x1p * x1p;
        double y1psq = y1p * y1p;
        double rxsq = rx * rx;
        double rysq = ry * ry;
        double delta = (x1psq / rxsq) + (y1psq / rysq);
        double num = rxsq * rysq - rxsq * y1psq - rysq * x1psq;
        if (delta > 1f) {
            rx *= Math.sqrt(delta);
            ry *= Math.sqrt(delta);
            rxsq = rx * rx;
            rysq = ry * ry;
            // Note that repeating the num calculation can sometimes yield
            // a negative answer...
            num = 0;
        }
        double den = rxsq * y1psq + rysq * x1psq;
        double sgn = (largeArcs == sweepFlag) ? -1f : 1f;
        double cxp = sgn * Math.sqrt(num / den) * (rx * y1p) / ry;
        double cyp = sgn * Math.sqrt(num / den) * -(ry * x1p) / rx;
        double cx = cosphi * cxp - sinphi * cyp + (x1 + x2) / 2f;
        double cy = sinphi * cxp + cosphi * cyp + (y1 + y2) / 2f;
        double theta = angle(1, 0,
                             (x1p - cxp) / rx, (y1p - cyp) / ry);
        double dtheta = angle((x1p - cxp) / rx, (y1p - cyp) / ry,
                              (-x1p - cxp) / rx, (-y1p - cyp) / ry);
        theta = Math.toDegrees(theta);
        dtheta = Math.toDegrees(dtheta);
        if (sweepFlag && dtheta < 0) dtheta += 360;
        if (!sweepFlag && dtheta > 0) dtheta -= 360;
        Arc2D arc = new Arc2D((float) (cx-rx), (float) (cy-ry),
                              (float) (rx*2.0), (float) (ry*2.0),
                              (float) -theta, (float) -dtheta, Arc2D.OPEN);
        BaseTransform arctx =
            BaseTransform.getRotateInstance(arcrad, cx, cy);
        checkPaths(arc.getPathIterator(arctx),
                   path.getPathIterator(BaseTransform.IDENTITY_TRANSFORM));
    }

    public @Test
    void testArcTo() {
        Path2D path = new Path2D();
        for (int pathdeg = 0; pathdeg <= 360; pathdeg += 15) {
            double pathrad = Math.toRadians(pathdeg);
            float px = (float) Math.cos(pathrad) * 50;
            float py = (float) Math.sin(pathrad) * 50;
            for (int arcdeg = 0; arcdeg <= 360; arcdeg += 15) {
                float arcrad = (float) Math.toRadians(arcdeg);
                for (int rw = 0; rw < 100; rw += 10) {
                    for (int rh = 0; rh < 100; rh += 10) {
                        checkArcTo(-px, -py, rw, rh, arcrad, false, false, px, py);
                        checkArcTo(-px, -py, rw, rh, arcrad, false,  true, px, py);
                        checkArcTo(-px, -py, rw, rh, arcrad,  true, false, px, py);
                        checkArcTo(-px, -py, rw, rh, arcrad,  true,  true, px, py);
                    }
                }
            }
        }
        RectBounds rectBounds = new RectBounds(10, 20, 20, 30);
        assertFalse(rectBounds.isEmpty());
        rectBounds.makeEmpty();
        assertTrue(rectBounds.isEmpty());
        assertEquals(new RectBounds(), rectBounds);

        BoxBounds boxBounds = new BoxBounds(10, 20, 10, 40, 50, 20);
        assertFalse(boxBounds.isEmpty());
        boxBounds.makeEmpty();
        assertTrue(boxBounds.isEmpty());
        assertEquals(new BoxBounds(), boxBounds);
    }

    public @Test
    void testEmptyPathException() {
        int bad = 0;
        Path2D p = new Path2D();
        try { p.lineTo(0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.quadTo(0, 0, 0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.curveTo(0, 0, 0, 0, 0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.arcTo(1, 1, 0, true, true, 1, 1); bad++; } catch (IllegalPathStateException e) {}
        try { p.moveToRel(0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.lineToRel(0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.quadToRel(0, 0, 0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.curveToRel(0, 0, 0, 0, 0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.arcToRel(1, 1, 0, true, true, 1, 1); bad++; } catch (IllegalPathStateException e) {}
        try { p.quadToSmooth(0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.curveToSmooth(0, 0, 0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.quadToSmoothRel(0, 0); bad++; } catch (IllegalPathStateException e) {}
        try { p.curveToSmoothRel(0, 0, 0, 0); bad++; } catch (IllegalPathStateException e) {}
        assertEquals(0, bad);
    }

    public @Test
    void testRelative() {
        Path2D pabs = new Path2D();
        Path2D prel = new Path2D();
        for (int x0 = -100; x0 < 100; x0 += 50) {
            for (int y0 = -100; y0 < 100; y0 += 50) {
                for (int x1 = -100; x1 < 100; x1 += 50) {
                    for (int y1 = -100; y1 < 100; y1 += 50) {
                        testRelative(pabs, prel, x0, y0, x1, y1);
                    }
                }
            }
        }
    }

    private void testRelative(Path2D pabs, Path2D prel,
                              int x0, int y0, int x1, int y1)
    {
        // Test relative moveTo following moveTo
        pabs.moveTo(x0, y0);
        pabs.moveTo(x1, y1);
        prel.moveTo(x0, y0);
        prel.moveToRel(x1-x0, y1-y0);
        checkAndResetPaths(pabs, prel, x1, y1);

        // Test relative lineTo
        pabs.moveTo(x0, y0);
        pabs.lineTo(x1, y1);
        prel.moveTo(x0, y0);
        prel.lineToRel(x1-x0, y1-y0);
        checkAndResetPaths(pabs, prel, x1, y1);

        // test relative arcTo
        pabs.moveTo(x0, y0);
        pabs.arcTo(1, 1, 0, true, true, x1, y1);
        prel.moveTo(x0, y0);
        prel.arcToRel(1, 1, 0, true, true, x1-x0, y1-y0);
        checkAndResetPaths(pabs, prel, x1, y1);

        // test relative paths with longer coordinate lists
        for (int x2 = -100; x2 < 100; x2 += 50) {
            for (int y2 = -100; y2 < 100; y2 += 50) {
                testRelative(pabs, prel, x0, y0, x1, y1, x2, y2);
            }
        }
    }

    private void testRelative(Path2D pabs, Path2D prel,
                              int x0, int y0, int x1, int y1, int x2, int y2)
    {
        // test relative quadTo
        pabs.moveTo(x0, y0);
        pabs.quadTo(x1, y1, x2, y2);
        prel.moveTo(x0, y0);
        prel.quadToRel(x1-x0, y1-y0, x2-x0, y2-y0);
        checkAndResetPaths(pabs, prel, x2, y2);

        for (int x3 = -100; x3 < 100; x3 += 50) {
            for (int y3 = -100; y3 < 100; y3 += 50) {
                // test relative cubic curveTo
                pabs.moveTo(x0, y0);
                pabs.curveTo(x1, y1, x2, y2, x3, y3);
                prel.moveTo(x0, y0);
                prel.curveToRel(x1-x0, y1-y0, x2-x0, y2-y0, x3-x0, y3-y0);
                checkAndResetPaths(pabs, prel, x3, y3);
            }
        }
    }

    public @Test
    void testSmoothCurves() {
        Path2D pabs = new Path2D();
        Path2D psmooth = new Path2D();
        for (int x0 = -100; x0 < 100; x0 += 50) {
            for (int y0 = -100; y0 < 100; y0 += 50) {
                for (int x1 = -100; x1 < 100; x1 += 50) {
                    for (int y1 = -100; y1 < 100; y1 += 50) {
                        testSmoothCurves(pabs, psmooth, x0, y0, x1, y1);
                    }
                }
            }
        }
    }
    
    private void testSmoothCurves(Path2D pabs, Path2D psmooth,
                                  int x0, int y0, int x1, int y1)
    {
        for (int xc0 = -100; xc0 < 100; xc0 += 100) {
            for (int yc0 = -100; yc0 < 100; yc0 += 100) {
                // test smooth quadto after lineTo
                pabs.moveTo(x0, y0);
                pabs.lineTo(xc0, yc0);
                pabs.quadTo(xc0, yc0, x1, y1);
                psmooth.moveTo(x0, y0);
                psmooth.lineTo(xc0, yc0);
                psmooth.quadToSmooth(x1, y1);
                checkAndResetPaths(pabs, psmooth, x1, y1);

                // test smooth relative quadTo after lineTo
                pabs.moveTo(x0, y0);
                pabs.lineTo(xc0, yc0);
                pabs.quadTo(xc0, yc0, x1, y1);
                psmooth.moveTo(x0, y0);
                psmooth.lineTo(xc0, yc0);
                psmooth.quadToSmoothRel(x1-xc0, y1-yc0);
                checkAndResetPaths(pabs, psmooth, x1, y1);

                for (int xc1 = -100; xc1 < 100; xc1 += 100) {
                    for (int yc1 = -100; yc1 < 100; yc1 += 100) {
                        float xc01 = (xc0 + xc1) / 2f;
                        float yc01 = (yc0 + yc1) / 2f;

                        // test smooth quadTo after quadTo
                        pabs.moveTo(x0, y0);
                        pabs.quadTo(xc0, yc0, xc01, yc01);
                        pabs.quadTo(xc1, yc1, x1, y1);
                        psmooth.moveTo(x0, y0);
                        psmooth.quadTo(xc0, yc0, xc01, yc01);
                        psmooth.quadToSmooth(x1, y1);
                        checkAndResetPaths(pabs, psmooth, x1, y1);

                        // test smooth relative quadTo after quadTo
                        pabs.moveTo(x0, y0);
                        pabs.quadTo(xc0, yc0, xc01, yc01);
                        pabs.quadTo(xc1, yc1, x1, y1);
                        psmooth.moveTo(x0, y0);
                        psmooth.quadTo(xc0, yc0, xc01, yc01);
                        psmooth.quadToSmoothRel(x1-xc01, y1-yc01);
                        checkAndResetPaths(pabs, psmooth, x1, y1);

                        // test smooth curveTo after lineTo
                        pabs.moveTo(x0, y0);
                        pabs.lineTo(xc0, yc0);
                        pabs.curveTo(xc0, yc0, xc1, yc1, x1, y1);
                        psmooth.moveTo(x0, y0);
                        psmooth.lineTo(xc0, yc0);
                        psmooth.curveToSmooth(xc1, yc1, x1, y1);
                        checkAndResetPaths(pabs, psmooth, x1, y1);

                        // test smooth relative curveTo after lineTo
                        pabs.moveTo(x0, y0);
                        pabs.lineTo(xc0, yc0);
                        pabs.curveTo(xc0, yc0, xc1, yc1, x1, y1);
                        psmooth.moveTo(x0, y0);
                        psmooth.lineTo(xc0, yc0);
                        psmooth.curveToSmoothRel(xc1-xc0, yc1-yc0, x1-xc0, y1-yc0);
                        checkAndResetPaths(pabs, psmooth, x1, y1);

                        testSmoothCurves(pabs, psmooth,
                                         x0, y0, x1, y1,
                                         xc0, yc0, xc01, yc01, xc1, yc1);
                    }
                }
            }
        }
    }

    private void testSmoothCurves(Path2D pabs, Path2D psmooth,
                                  int x0, int y0, int x1, int y1,
                                  int xc0, int yc0,
                                  float xc01, float yc01,
                                  int xc1, int yc1)
    {
        for (int xc2 = -100; xc2 < 100; xc2 += 100) {
            for (int yc2 = -100; yc2 < 100; yc2 += 100) {
                // test smooth curveTo after quadTo
                pabs.moveTo(x0, y0);
                pabs.quadTo(xc0, yc0, xc01, yc01);
                pabs.curveTo(xc1, yc1, xc2, yc2, x1, y1);
                psmooth.moveTo(x0, y0);
                psmooth.quadTo(xc0, yc0, xc01, yc01);
                psmooth.curveToSmooth(xc2, yc2, x1, y1);
                checkAndResetPaths(pabs, psmooth, x1, y1);

                // test smooth relative curveTo after quadTo
                pabs.moveTo(x0, y0);
                pabs.quadTo(xc0, yc0, xc01, yc01);
                pabs.curveTo(xc1, yc1, xc2, yc2, x1, y1);
                psmooth.moveTo(x0, y0);
                psmooth.quadTo(xc0, yc0, xc01, yc01);
                psmooth.curveToSmoothRel(xc2-xc01, yc2-yc01, x1-xc01, y1-yc01);
                checkAndResetPaths(pabs, psmooth, x1, y1);

                float xc12 = (xc1 + xc2) / 2f;
                float yc12 = (yc1 + yc2) / 2f;

                // test smooth quadTo after curveTo
                pabs.moveTo(x0, y0);
                pabs.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                pabs.quadTo(xc2, yc2, x1, y1);
                psmooth.moveTo(x0, y0);
                psmooth.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                psmooth.quadToSmooth(x1, y1);
                checkAndResetPaths(pabs, psmooth, x1, y1);

                // test smooth relative quadTo after curveTo
                pabs.moveTo(x0, y0);
                pabs.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                pabs.quadTo(xc2, yc2, x1, y1);
                psmooth.moveTo(x0, y0);
                psmooth.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                psmooth.quadToSmoothRel(x1-xc12, y1-yc12);
                checkAndResetPaths(pabs, psmooth, x1, y1);

                for (int xc3 = -100; xc3 < 100; xc3 += 100) {
                    for (int yc3 = -100; yc3 < 100; yc3 += 100) {
                        // test smooth curveTo after curveTo
                        pabs.moveTo(x0, y0);
                        pabs.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                        pabs.curveTo(xc2, yc2, xc3, yc3, x1, y1);
                        psmooth.moveTo(x0, y0);
                        psmooth.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                        psmooth.curveToSmooth(xc3, yc3, x1, y1);
                        checkAndResetPaths(pabs, psmooth, x1, y1);

                        // test smooth relative curveTo after curveTo
                        pabs.moveTo(x0, y0);
                        pabs.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                        pabs.curveTo(xc2, yc2, xc3, yc3, x1, y1);
                        psmooth.moveTo(x0, y0);
                        psmooth.curveTo(xc0, yc0, xc1, yc1, xc12, yc12);
                        psmooth.curveToSmoothRel(xc3-xc12, yc3-yc12, x1-xc12, y1-yc12);
                        checkAndResetPaths(pabs, psmooth, x1, y1);
                    }
                }
            }
        }
    }

    public @Test
    void testSVGPath() {
        String svgpath =
            "M 10 20 "+
            "L 20 30 "+
            "H 10 "+
            "V 20 "+
            "Q 20 30 10 20 "+
            "C 20 30 20 20 10 40 "+
            "T 10 50 "+
            "S 20 25 10 35 "+
            "A 40 60 10 0 0 15 20 "+
            "A 40 60 10 0 1 25 30 "+
            "A 40 60 10 1 0 15 10 "+
            "A 40 60 10 1 1 25 20 "+
            "Z "+
            "m 10 20 "+
            "l 20 20 "+
            "h 10 "+
            "v 20 "+
            "q 10 30 10 20 "+
            "c 10 30 10 20 10 40 "+
            "t 10 50 "+
            "s 10 25 10 35 "+
            "a 40 60 10 0 0 10 20 "+
            "a 40 60 10 0 1 10 20 "+
            "a 40 60 10 1 0 10 20 "+
            "a 40 60 10 1 1 10 20 "+
            "z";
        Path2D p2dtest = new Path2D();
        p2dtest.appendSVGPath(svgpath);
        Path2D p2dref = new Path2D();
        p2dref.moveTo(10, 20);
        p2dref.lineTo(20, 30);
        p2dref.lineTo(10, p2dref.getCurrentY());
        p2dref.lineTo(p2dref.getCurrentX(), 20);
        p2dref.quadTo(20, 30, 10, 20);
        p2dref.curveTo(20, 30, 20, 20, 10, 40);
        p2dref.quadToSmooth(10, 50);
        p2dref.curveToSmooth(20, 25, 10, 35);
        p2dref.arcTo(40, 60, (float) Math.toRadians(10), false, false, 15, 20);
        p2dref.arcTo(40, 60, (float) Math.toRadians(10), false, true,  25, 30);
        p2dref.arcTo(40, 60, (float) Math.toRadians(10), true,  false, 15, 10);
        p2dref.arcTo(40, 60, (float) Math.toRadians(10), true,  true,  25, 20);
        p2dref.closePath();
        p2dref.moveToRel(10, 20);
        p2dref.lineToRel(20, 20);
        p2dref.lineToRel(10, 0);
        p2dref.lineToRel(0, 20);
        p2dref.quadToRel(10, 30, 10, 20);
        p2dref.curveToRel(10, 30, 10, 20, 10, 40);
        p2dref.quadToSmoothRel(10, 50);
        p2dref.curveToSmoothRel(10, 25, 10, 35);
        p2dref.arcToRel(40, 60, (float) Math.toRadians(10), false, false, 10, 20);
        p2dref.arcToRel(40, 60, (float) Math.toRadians(10), false, true,  10, 20);
        p2dref.arcToRel(40, 60, (float) Math.toRadians(10), true,  false, 10, 20);
        p2dref.arcToRel(40, 60, (float) Math.toRadians(10), true,  true,  10, 20);
        p2dref.closePath();
        checkShapes(p2dref, p2dtest);
    }

    public @Test
    void testSVGPathWS() {
        String svgpathlotsofWS =
            "M 10, 20 "+
            "L 20, 30 "+
            "H 10 "+
            "V 20 "+
            "Q 20, 30 10, 20 "+
            "C 20, 30 20, 20 10, 40 "+
            "T 10, 50 "+
            "S 20, 25 10, 35 "+
            "A 40, 60 10 0 0 15, 20 "+
            "A 40, 60 10 0 1 25, 30 "+
            "A 40, 60 10 1 0 15, 10 "+
            "A 40, 60 10 1 1 25, 20 "+
            "Z "+
            "m 10, 20 "+
            "l 20, 20 "+
            "h 10 "+
            "v 20 "+
            "q 10, 30 10, 20 "+
            "c 10, 30 10, 20 10, 40 "+
            "t 10, 50 "+
            "s 10, 25 10 35 "+
            "a 40, 60 10 0 0 10, 20 "+
            "a 40, 60 10 0 1 10, 20 "+
            "a 40, 60 10 1 0 10, 20 "+
            "a 40, 60 10 1 1 10, 20 "+
            "z";
        String svgpathminWS =
            "M10,20"+
            "L20,30"+
            "H10"+
            "V20"+
            "Q20,30,10,20"+
            "C20,30,20,20,10,40"+
            "T10,50"+
            "S20,25,10,35"+
            "A40,60,10,0,0,15,20"+
            "A40,60,10,0,1,25,30"+
            "A40,60,10,1,0,15,10"+
            "A40,60,10,1,1,25,20"+
            "Z"+
            "m10,20"+
            "l20,20"+
            "h10"+
            "v20"+
            "q10,30,10,20"+
            "c10,30,10,20,10,40"+
            "t10,50"+
            "s10,25,10,35"+
            "a40,60,10,0,0,10,20"+
            "a40,60,10,0,1,10,20"+
            "a40,60,10,1,0,10,20"+
            "a40,60,10,1,1,10,20"+
            "z";
        Path2D p2dref = new Path2D();
        p2dref.appendSVGPath(svgpathlotsofWS);
        Path2D p2dtest = new Path2D();
        p2dtest.appendSVGPath(svgpathminWS);
        checkShapes(p2dref, p2dtest);
    }
}
