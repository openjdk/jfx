/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * StubPath.fx
 */

package com.sun.javafx.pgstub;

import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.sg.PGPath;

public class StubPath extends StubShape implements PGPath {

    private Path2D path2D = new Path2D();

    public void reset() { path2D = new Path2D(); }
    public void update() { }

    public void setFillRule(PGPath.FillRule fillRule) {
        path2D.setWindingRule(fillRule == FillRule.EVEN_ODD ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
    }

    public float getCurrentX() {
        return path2D.getCurrentX();
    }

    public float getCurrentY() {
        return path2D.getCurrentY();
    }

    public void addClosePath() {
        path2D.closePath();
    }

    public void addMoveTo(float x, float y) {
        path2D.moveTo(x, y);
    }
    
    public void addLineTo(float x, float y) {
        path2D.lineTo(x, y);
    }

    public void addQuadTo(float ctrlx, float ctrly, float x, float y) { 
        path2D.quadTo(ctrlx, ctrly, x, y);
    }

    public void addCubicTo(float ctrlx1, float ctrly1,
                           float ctrlx2, float ctrly2,
                           float x, float y) {
        path2D.curveTo(ctrlx1, ctrly1, ctrlx2, ctrly2, x, y);
    }

    // used by AnimationPathHelper to get access to raw path object
    public Object getGeometry() { 
        return path2D;
    }

    public void addArcTo(float arcX, float arcY, float arcW, float arcH,
                         float arcStart, float arcExtent, float xAxisRotation) {
       
        final Arc2D arc = new Arc2D((float)arcX, (float)arcY, (float)arcW, (float)arcH,
                (float)arcStart, (float)arcExtent, Arc2D.OPEN);
        final Affine2D xform = new Affine2D();
        xform.setToRotation(xAxisRotation, arc.getCenterX(), arc.getCenterY());
        path2D.append(arc.getPathIterator(xform), true);
    }

    @Override
    public boolean acceptsPath2dOnUpdate() {
        return true;
    }

    @Override
    public void updateWithPath2d(Path2D path) {
        path2D.setTo(path);
    }
}
