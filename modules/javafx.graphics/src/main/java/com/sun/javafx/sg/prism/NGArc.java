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

package com.sun.javafx.sg.prism;

import javafx.scene.shape.ArcType;
import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.Shape;
import com.sun.prism.Graphics;
import com.sun.prism.shape.ShapeRep;

/**
 *
 */
public class NGArc extends NGShape {
    private Arc2D arc = new Arc2D();

    public void updateArc(float cx, float cy, float rx, float ry,
                                    float start, float extent, ArcType type) {
        arc.x = cx - rx;
        arc.width = rx * 2f;
        arc.y = cy - ry;
        arc.height = ry * 2f;
        arc.start = start;
        arc.extent = extent;

        if (type == ArcType.CHORD) {
            arc.setArcType(Arc2D.CHORD);
        } else if (type == ArcType.OPEN) {
            arc.setArcType(Arc2D.OPEN);
        } else if (type == ArcType.ROUND) {
            arc.setArcType(Arc2D.PIE);
        } else {
            throw new AssertionError("Unknown arc type specified");
        }
        geometryChanged();
    }

    @Override public Shape getShape() { return arc; }
    @Override protected ShapeRep createShapeRep(Graphics g) {
        return g.getResourceFactory().createArcRep();
    }
}
