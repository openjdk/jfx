/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.paint;

import com.sun.javafx.geom.transform.BaseTransform;
import java.util.List;

public final class RadialGradient extends Gradient {

    private final float centerX;
    private final float centerY;
    private final float focusAngle;
    private final float focusDistance;
    private final float radius;

    public RadialGradient(float centerX, float centerY,
                          float focusAngle, float focusDistance,
                          float radius,
                          BaseTransform gradientTransform,
                          boolean proportional,
                          int spreadMethod,
                          List<Stop> stops)
    {
        super(Type.RADIAL_GRADIENT, gradientTransform, proportional, spreadMethod, stops);
        this.centerX = centerX;
        this.centerY = centerY;
        this.focusAngle = focusAngle;
        this.focusDistance = focusDistance;
        this.radius = radius;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getFocusAngle() {
        return focusAngle;
    }

    public float getFocusDistance() {
        return focusDistance;
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public String toString()
    {
        return "RadialGradient: FocusAngle: "+focusAngle+" FocusDistance: "+focusDistance+
               " CenterX: "+centerX+" CenterY "+centerY+
               " Radius: "+radius+"stops:"+getStops();
    }
}
