/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import java.lang.annotation.Native;

public abstract class WCPath<P> extends Ref {

    /* The WindRule should be compliant with
     * WebCore/platform/graphics/Path.h
     */

    /**
     * The winding rule constant for specifying a non-zero rule
     * for determining the interior of a path.
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    @Native public static final int RULE_NONZERO = 0;

    /**
     * The winding rule constant for specifying an even-odd rule
     * for determining the interior of a path.
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    @Native public static final int RULE_EVENODD = 1;

    public abstract void addRect(double x, double y, double w, double h);

    public abstract void addEllipse(double x, double y, double w, double h);

    public abstract void addArcTo(double x1, double y1, double x2, double y2, double r);

    public abstract void addArc(double x, double y, double r, double startAngle,
                                double endAngle, boolean aclockwise);

    public abstract boolean contains(int rule, double x, double y);

    public abstract WCRectangle getBounds();

    public abstract void clear();

    public abstract void moveTo(double x, double y);

    public abstract void addLineTo(double x, double y);

    public abstract void addQuadCurveTo(double x0, double y0, double x1, double y1);

    public abstract void addBezierCurveTo(double x0, double y0,
                                          double x1, double y1,
                                          double x2, double y2);

    public abstract void addPath(WCPath path);

    public abstract void closeSubpath();

    public abstract boolean isEmpty();

    public abstract void translate(double x, double y);

    public abstract void transform(double mxx, double myx,
                                   double mxy, double myy,
                                   double mxt, double myt);

    public abstract int getWindingRule();

    public abstract void setWindingRule(int rule);

    public abstract P getPlatformPath();

    public abstract WCPathIterator getPathIterator();

    public abstract boolean strokeContains(double x, double y,
                                           double thickness, double miterLimit,
                                           int cap, int join, double dashOffset,
                                           double[] dashArray);
}
