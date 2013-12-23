/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

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
    public static final int RULE_NONZERO = 0;

    /**
     * The winding rule constant for specifying an even-odd rule
     * for determining the interior of a path.
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    public static final int RULE_EVENODD = 1;

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

    public abstract boolean hasCurrentPoint();

    public abstract boolean isEmpty();

    public abstract void translate(double x, double y);

    public abstract void transform(double mxx, double myx,
                                   double mxy, double myy,
                                   double mxt, double myt);

    public abstract int getWindingRule();

    public abstract void setWindingRule(int rule);

    public abstract P getPlatformPath();

    public abstract WCPathIterator getPathIterator();
}
