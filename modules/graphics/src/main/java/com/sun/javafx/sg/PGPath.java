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

package com.sun.javafx.sg;

import com.sun.javafx.geom.Path2D;

/**
 *
 */
public interface PGPath extends PGShape {
    public enum FillRule { EVEN_ODD, NON_ZERO };

    public void reset();
    public void update();
    public void setFillRule(FillRule fillRule);
    public float getCurrentX();
    public float getCurrentY();
    public void addClosePath();
    public void addMoveTo(float x, float y);
    public void addLineTo(float x, float y);
    public void addQuadTo(float ctrlx, float ctrly, float x, float y);
    public void addCubicTo(float ctrlx1, float ctrly1,
                           float ctrlx2, float ctrly2,
                           float x, float y);
    public void addArcTo(float arcX, float arcY, float arcW, float arcH,
                         float arcStart, float arcExtent, float xAxisRotation);
    public boolean acceptsPath2dOnUpdate();
    public void updateWithPath2d(Path2D path);
}
