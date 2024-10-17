/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor
// Copyright © 2017-2023 Andy Goryachev <andy@goryachev.com>

package com.sun.jfx.incubator.scene.control.richtext;

import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;

/**
 * Selection Helper encapsulates the logic required to generate selection shapes.
 *
 * The goal is to find out which shapes correspond to the top-most and bottom-most
 * text rows (in the presence of wrapping).  These shapes (#) should be added to selection as is.
 * Any space in between (x) would generate a single rectangular block that fills the
 * width of the container.  Additional shapes (#) will be added when necessary to make
 * the selection appear contiguious.  These shapes are positioned to the left or to the right
 * of the selected text depending on the direction of text.
 *
 * TODO RTL text
 * <pre>
 * ----***--***####
 * xxxxxxxxxxxxxxxx
 * xxxxxxxxxxxxxxxx
 * ####**----------
 * </pre>
 * TODO this class can be static because everything happens in the FX app thread.
 */
public class SelectionHelper {
    private final FxPathBuilder pathBuilder;
    private final double left;
    private final double right;
    private double topUp = Double.POSITIVE_INFINITY;
    private double topDn = Double.POSITIVE_INFINITY;
    private double topLeft = Double.POSITIVE_INFINITY;
    private double topRight = Double.NEGATIVE_INFINITY;
    private double bottomUp = Double.NEGATIVE_INFINITY;
    private double bottomDn = Double.NEGATIVE_INFINITY;
    private double bottomLeft = Double.POSITIVE_INFINITY;
    private double bottomRight = Double.NEGATIVE_INFINITY;
    private static final double EPSILON = 0.001; // float point arithmetic is inexact

    public SelectionHelper(FxPathBuilder b, double left, double right) {
        this.pathBuilder = b;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "topUp=" + topUp + " topDn=" + topDn + " botUp=" + bottomUp + " botDn=" + bottomDn;
    }

    @FunctionalInterface
    protected interface PathHandler {
        public void processPoint(double x, double y);
    }

    private void process(PathElement[] elements, PathHandler h) {
        if (elements != null) {
            for (PathElement em : elements) {
                if (em instanceof LineTo p) {
                    h.processPoint(p.getX(), p.getY());
                } else if (em instanceof MoveTo p) {
                    h.processPoint(p.getX(), p.getY());
                } else {
                    throw new Error("?" + em);
                }
            }
        }
    }

    private void generateMiddle(boolean topLTR, boolean bottomLTR, double lineSpacing) {
        if (Double.isNaN(topUp)) {
            return;
        }

        double td = topDn + lineSpacing;
        double bd = bottomDn + lineSpacing;

        // only if the middle exists
        if (bottomUp > topDn) {
            if (topLTR) {
                pathBuilder.moveto(topRight, topUp);
                pathBuilder.lineto(right, topUp);
                pathBuilder.lineto(right, td);
                pathBuilder.lineto(topRight, td);
                pathBuilder.lineto(topRight, topUp);
            } else {
                // TODO
            }

            pathBuilder.moveto(left, td);
            pathBuilder.lineto(right, td);
            pathBuilder.lineto(right, bottomUp);
            pathBuilder.lineto(left, bottomUp);
            pathBuilder.lineto(left, td);

            // trailer

            if (bottomLTR) {
                pathBuilder.moveto(left, bottomUp);
                pathBuilder.lineto(bottomLeft, bottomUp);
                pathBuilder.lineto(bottomLeft, bd);
                pathBuilder.lineto(left, bd);
                pathBuilder.lineto(left, bottomUp);
            } else {
                // TODO
            }
        }
    }

    private boolean isNear(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    private void determineTopYLimits(double x, double y) {
        if (y < topUp) {
            topUp = y;
        }
    }

    private void determineTopXLimits(double x, double y) {
        if (isNear(y, topUp)) {
            if (x < topLeft) {
                topLeft = x;
            }

            if (x > topRight) {
                topRight = x;
            }
        } else {
            if (y < topDn) {
                topDn = y;
            }
        }
    }

    private void determineBottomYLimits(double x, double y) {
        if (y > bottomDn) {
            bottomDn = y;
        }
    }

    private void determineBottomXLimits(double x, double y) {
        if (isNear(y, bottomDn)) {
            if (x < bottomLeft) {
                bottomLeft = x;
            }

            if (x > bottomRight) {
                bottomRight = x;
            }
        } else {
            if (y > bottomUp) {
                bottomUp = y;
            }
        }
    }

    /** adjusts for line spacing and left padding */
    private void adjust(PathElement[] elements, double px, double py, double dx, double dy) {
        if (elements != null) {
            for (PathElement em : elements) {
                if (em instanceof LineTo p) {
                    double x = p.getX();
                    if (isNear(x, px)) {
                        //p.setX(x - dx);
                    }

                    double y = p.getY();
                    if (isNear(y, py)) {
                        p.setY(y + dy);
                    }
                } else if (em instanceof MoveTo p) {
                    double x = p.getX();
                    if (isNear(x, px)) {
                        //p.setX(x - dx);
                    }

                    double y = p.getY();
                    if (isNear(y, py)) {
                        p.setY(y + dy);
                    }
                }
            }
        }
    }

    public void generate(
        PathElement[] top,
        PathElement[] bottom,
        boolean topLTR,
        boolean bottomLTR,
        double leftPadding,
        double lineSpacing
    ) {
        process(top, this::determineTopYLimits);
        process(top, this::determineTopXLimits);

        if (bottom == null) {
            // TODO special handling when outside of visible area

            adjust(top, topLeft, topDn, leftPadding, lineSpacing);
            pathBuilder.addAll(top);
        } else {
            process(bottom, this::determineBottomYLimits);
            process(bottom, this::determineBottomXLimits);

            adjust(top, topLeft, topDn, leftPadding, lineSpacing);
            adjust(bottom, bottomLeft, bottomDn, leftPadding, lineSpacing);

//            D.p("top", dump(top), "bottom", dump(btm)); // FIX
//            D.p(" top: y=" + r(topUp) + ".." + r(topDn) + " x=" + r(topLeft) + ".." + r(topRight));
//            D.p(" bot: y=" + r(bottomUp) + ".." + r(bottomDn) + " x=" + r(bottomLeft) + ".." + r(bottomRight));

            pathBuilder.addAll(top);
            generateMiddle(topLTR, bottomLTR, lineSpacing);
            pathBuilder.addAll(bottom);
        }
    }
}