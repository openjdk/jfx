/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.shape.ArcToHelper;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;

// PENDING_DOC_REVIEW
/**
 * A path element that forms an arc from the previous coordinates
 * to the specified x and y coordinates using the specified radius.
 *
 * <p>For more information on path elements see the {@link Path} and
 * {@link PathElement} classes.
 *
 * <p>Example:
 *
<PRE>
import javafx.scene.shape.*;

Path path = new Path();

MoveTo moveTo = new MoveTo();
moveTo.setX(0.0);
moveTo.setY(0.0);

ArcTo arcTo = new ArcTo();
arcTo.setX(50.0);
arcTo.setY(50.0);
arcTo.setRadiusX(50.0);
arcTo.setRadiusY(50.0);

path.getElements().add(moveTo);
path.getElements().add(arcTo);
</PRE>
 *
 * <p>
 * Following image demonstrates {@code radiusX}, {@code radiusY} and
 * {@code xAxisRotation} parameters:
 * {@code radiusX} is the horizontal radius of the full ellipse of which this arc is
 * a partial section, {@code radiusY} is its vertical radius.
 * {@code xAxisRotation} defines the rotation of the ellipse in degrees.
 * </p>
 * <p>
 * <img src="doc-files/arcto.png" alt="A visual rendering of ArcTo shape">
 * </p>
 * <p>
 * In most cases, there are four options of how to draw an arc from
 * starting point to given end coordinates. They can be distinguished by
 * {@code largeArcFlag} and {@code sweepFlag} parameters.
 * {@code largeArcFlag == true} means that the arc greater than 180 degrees will
 * be drawn. {@code sweepFlag == true} means that the arc will be drawn
 * in the positive angle direction - i.e. the angle in the
 * ellipse formula will increase from {@code [fromX, fromY]} to {@code [x,y]}.
 * Following images demonstrate this behavior:
 * </p>
 * <p>
 * <img src="doc-files/arcto-flags.png" alt="A visual rendering of ArcTo shape
 * with setting to its different properties">
 * </p>
 * @since JavaFX 2.0
 */
public class ArcTo extends PathElement {
    static {
        ArcToHelper.setArcToAccessor(new ArcToHelper.ArcToAccessor() {
            @Override
            public void doAddTo(PathElement pathElement, Path2D path) {
                ((ArcTo) pathElement).doAddTo(path);
            }
        });
    }

    /**
     * Creates an empty instance of ArcTo.
     */
    public ArcTo() {
        ArcToHelper.initHelper(this);
    }

    /**
     * Creates a new instance of ArcTo.
     * @param radiusX horizontal radius of the arc
     * @param radiusY vertical radius of the arc
     * @param xAxisRotation the x-axis rotation in degrees
     * @param x horizontal position of the arc end point
     * @param y vertical position of the arc end point
     * @param largeArcFlag large arg flag: determines which arc to use (large/small)
     * @param sweepFlag sweep flag: determines which arc to use (direction)
     */
    public ArcTo(double radiusX, double radiusY, double xAxisRotation,
        double x, double y, boolean largeArcFlag, boolean sweepFlag)
    {
        setRadiusX(radiusX);
        setRadiusY(radiusY);
        setXAxisRotation(xAxisRotation);
        setX(x);
        setY(y);
        setLargeArcFlag(largeArcFlag);
        setSweepFlag(sweepFlag);
        ArcToHelper.initHelper(this);
    }

    /**
     * The horizontal radius to use for the arc.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty radiusX = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            u();
        }

        @Override
        public Object getBean() {
            return ArcTo.this;
        }

        @Override
        public String getName() {
            return "radiusX";
        }
    };

    public final void setRadiusX(double value) {
        radiusX.set(value);
    }

    public final double getRadiusX() {
        return radiusX.get();
    }

    public final DoubleProperty radiusXProperty() {
        return radiusX;
    }

    /**
     * The vertical radius to use for the arc.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty radiusY = new DoublePropertyBase() {

        @Override
        public void invalidated() {
            u();
        }

        @Override
        public Object getBean() {
            return ArcTo.this;
        }

        @Override
        public String getName() {
            return "radiusY";
        }
    };

    public final void setRadiusY(double value) {
        radiusY.set(value);
    }

    public final double getRadiusY() {
        return radiusY.get();
    }

    public final DoubleProperty radiusYProperty() {
        return radiusY;
    }

    /**
     * The x-axis rotation in degrees.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty xAxisRotation;

    /**
     * Sets the x-axis rotation in degrees.
     * @param value the x-axis rotation in degrees.
     */
    public final void setXAxisRotation(double value) {
        if (xAxisRotation != null || value != 0.0) {
            XAxisRotationProperty().set(value);
        }
    }

    /**
     * Gets the x-axis rotation in degrees.
     * @return the x-axis rotation in degrees.
     */
    public final double getXAxisRotation() {
        return xAxisRotation == null ? 0.0 : xAxisRotation.get();
    }

    /**
     * The x-axis rotation in degrees.
     * @return The XAxisRotation property
     */
    public final DoubleProperty XAxisRotationProperty() {
        if (xAxisRotation == null) {
            xAxisRotation = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return ArcTo.this;
                }

                @Override
                public String getName() {
                    return "XAxisRotation";
                }
            };
        }
        return xAxisRotation;
    }

    /**
     * The large arc flag.
     *
     * @defaultValue false
     */
    private BooleanProperty largeArcFlag;

    public final void setLargeArcFlag(boolean value) {
        if (largeArcFlag != null || value != false) {
            largeArcFlagProperty().set(value);
        }
    }

    public final boolean isLargeArcFlag() {
        return largeArcFlag == null ? false : largeArcFlag.get();
    }

    public final BooleanProperty largeArcFlagProperty() {
        if (largeArcFlag == null) {
            largeArcFlag = new BooleanPropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return ArcTo.this;
                }

                @Override
                public String getName() {
                    return "largeArcFlag";
                }
            };
        }
        return largeArcFlag;
    }

    /**
     * The sweep flag
     *
     * @defaultValue false
     */
    private BooleanProperty sweepFlag;

    public final void setSweepFlag(boolean value) {
        if (sweepFlag != null || value != false) {
            sweepFlagProperty().set(value);
        }
    }

    public final boolean isSweepFlag() {
        return sweepFlag == null ? false : sweepFlag.get();
    }

    public final BooleanProperty sweepFlagProperty() {
        if (sweepFlag == null) {
            sweepFlag = new BooleanPropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return ArcTo.this;
                }

                @Override
                public String getName() {
                    return "sweepFlag";
                }
            };
        }
        return sweepFlag;
    }

    /**
     * The x coordinate to arc to.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty x;



    public final void setX(double value) {
        if (x != null || value != 0.0) {
            xProperty().set(value);
        }
    }

    public final double getX() {
        return x == null ? 0.0 : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return ArcTo.this;
                }

                @Override
                public String getName() {
                    return "x";
                }
            };
        }
        return x;
    }

    /**
     * The y coordinate to arc to.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty y;



    public final void setY(double value) {
        if (y != null || value != 0.0) {
            yProperty().set(value);
        }
    }

    public final double getY() {
        return y == null ? 0.0 : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return ArcTo.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    @Override
    void addTo(NGPath pgPath) {
        addArcTo(pgPath, null, pgPath.getCurrentX(), pgPath.getCurrentY());
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doAddTo(Path2D path) {
        addArcTo(null, path, path.getCurrentX(), path.getCurrentY());
    }

    // This function is nearly identical to the one written for the
    // original port of the F3 graphics/UI library:
    // javafx.ui.canvas.ArcTo#addTo
    private void addArcTo(NGPath pgPath, Path2D path,
                          final double x0, final double y0)
    {
        double localX = getX();
        double localY = getY();
        boolean localSweepFlag = isSweepFlag();
        boolean localLargeArcFlag = isLargeArcFlag();

        // Determine target "to" position
        final double xto = (isAbsolute()) ? localX : localX + x0;
        final double yto = (isAbsolute()) ? localY : localY + y0;
        // Compute the half distance between the current and the final point
        final double dx2 = (x0 - xto) / 2.0;
        final double dy2 = (y0 - yto) / 2.0;
        // Convert angle from degrees to radians
        final double xAxisRotationR = Math.toRadians(getXAxisRotation());
        final double cosAngle = Math.cos(xAxisRotationR);
        final double sinAngle = Math.sin(xAxisRotationR);

        //
        // Step 1 : Compute (x1, y1)
        //
        final double x1 = ( cosAngle * dx2 + sinAngle * dy2);
        final double y1 = (-sinAngle * dx2 + cosAngle * dy2);
        // Ensure radii are large enough
        double rx = Math.abs(getRadiusX());
        double ry = Math.abs(getRadiusY());
        double Prx = rx * rx;
        double Pry = ry * ry;
        final double Px1 = x1 * x1;
        final double Py1 = y1 * y1;
        // check that radii are large enough
        final double radiiCheck = Px1/Prx + Py1/Pry;
        if (radiiCheck > 1.0) {
            rx = Math.sqrt(radiiCheck) * rx;
            ry = Math.sqrt(radiiCheck) * ry;
            if (rx == rx && ry == ry) {/* not NANs */} else {
                if (pgPath == null) {
                    path.lineTo((float) xto, (float) yto);
                } else {
                    pgPath.addLineTo((float) xto, (float) yto);
                }
                return;
            }
            Prx = rx * rx;
            Pry = ry * ry;
        }

        //
        // Step 2 : Compute (cx1, cy1)
        //
        double sign = ((localLargeArcFlag == localSweepFlag) ? -1.0 : 1.0);
        double sq = ((Prx*Pry)-(Prx*Py1)-(Pry*Px1)) / ((Prx*Py1)+(Pry*Px1));
        sq = (sq < 0.0) ? 0.0 : sq;
        final double coef = (sign * Math.sqrt(sq));
        final double cx1 = coef * ((rx * y1) / ry);
        final double cy1 = coef * -((ry * x1) / rx);

        //
        // Step 3 : Compute (cx, cy) from (cx1, cy1)
        //
        final double sx2 = (x0 + xto) / 2.0;
        final double sy2 = (y0 + yto) / 2.0;
        final double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
        final double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

        //
        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
        //
        final double ux = (x1 - cx1) / rx;
        final double uy = (y1 - cy1) / ry;
        final double vx = (-x1 - cx1) / rx;
        final double vy = (-y1 - cy1) / ry;
        // Compute the angle start
        double n = Math.sqrt((ux * ux) + (uy * uy));
        double p = ux; // (1 * ux) + (0 * uy)
        sign = ((uy < 0.0) ? -1.0 : 1.0);
        double angleStart = Math.toDegrees(sign * Math.acos(p / n));

        // Compute the angle extent
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        sign = ((ux * vy - uy * vx < 0.0) ? -1.0 : 1.0);
        double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
        if (!localSweepFlag && (angleExtent > 0)) {
            angleExtent -= 360.0;
        } else if (localSweepFlag && (angleExtent < 0)) {
            angleExtent += 360.0;
        }
        angleExtent = angleExtent % 360;
        angleStart = angleStart % 360;

        //
        // We can now build the resulting Arc2D
        //
        final float arcX = (float) (cx - rx);
        final float arcY = (float) (cy - ry);
        final float arcW = (float) (rx * 2.0);
        final float arcH = (float) (ry * 2.0);
        final float arcStart = (float) -angleStart;
        final float arcExtent = (float) -angleExtent;

        if (pgPath == null) {
            final Arc2D arc =
                new Arc2D(arcX, arcY, arcW, arcH,
                          arcStart, arcExtent, Arc2D.OPEN);
            BaseTransform xform = (xAxisRotationR == 0) ? null :
                BaseTransform.getRotateInstance(xAxisRotationR, cx, cy);
            PathIterator pi = arc.getPathIterator(xform);
            // RT-8926, append(true) converts the initial moveTo into a
            // lineTo which can generate huge miter joins if the segment
            // is small enough.  So, we manually skip it here instead.
            pi.next();
            path.append(pi, true);
        } else {
            pgPath.addArcTo(arcX, arcY, arcW, arcH,
                            arcStart, arcExtent, (float) xAxisRotationR);
        }
    }

    /**
     * Returns a string representation of this {@code ArcTo} object.
     * @return a string representation of this {@code ArcTo} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ArcTo[");

        sb.append("x=").append(getX());
        sb.append(", y=").append(getY());
        sb.append(", radiusX=").append(getRadiusX());
        sb.append(", radiusY=").append(getRadiusY());
        sb.append(", xAxisRotation=").append(getXAxisRotation());

        if (isLargeArcFlag()) {
            sb.append(", lartArcFlag");
        }

        if (isSweepFlag()) {
            sb.append(", sweepFlag");
        }

        return sb.append("]").toString();
    }
}
