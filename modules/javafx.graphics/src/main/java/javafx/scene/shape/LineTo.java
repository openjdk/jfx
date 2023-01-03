/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.scene.shape.LineToHelper;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;


/**
 * Creates a line path element by drawing a straight line
 * from the current coordinate to the new coordinates.
 *
 * <p>For more information on path elements see the {@link Path} and
 * {@link PathElement} classes.
 *
 * <p>Example:
 *
<PRE>
import javafx.scene.shape.*;

Path path = new Path();
path.getElements().add(new MoveTo(0.0f, 50.0f));
path.getElements().add(new LineTo(100.0f, 100.0f));
</PRE>
 * @since JavaFX 2.0
 */
public class LineTo extends PathElement {
    static {
        LineToHelper.setLineToAccessor(new LineToHelper.LineToAccessor() {
            @Override
            public void doAddTo(PathElement pathElement, Path2D path) {
                ((LineTo) pathElement).doAddTo(path);
            }
        });
    }

    /**
     * Creates an empty instance of LineTo.
     */
    public LineTo() {
        LineToHelper.initHelper(this);
    }

    /**
     * Creates a new isntance of LineTo.
     * @param x the horizontal coordinate of the line end point
     * @param y the vertical coordinate of the line end point
     */
    public LineTo(double x, double y) {
        setX(x);
        setY(y);
        LineToHelper.initHelper(this);
    }

    /**
     * Defines the X coordinate.
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
                    return LineTo.this;
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
     * Defines the Y coordinate.
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
                    return LineTo.this;
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
        if (isAbsolute()) {
            pgPath.addLineTo((float)getX(), (float)getY());
        } else {
            pgPath.addLineTo((float)(pgPath.getCurrentX() + getX()),
                             (float)(pgPath.getCurrentY() + getY()));
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doAddTo(Path2D path) {
        if (isAbsolute()) {
            path.lineTo((float)getX(), (float)getY());
        } else {
            path.lineTo((float)(path.getCurrentX() + getX()),
                        (float)(path.getCurrentY() + getY()));
        }
    }

    /**
     * Returns a string representation of this {@code LineTo} object.
     * @return a string representation of this {@code LineTo} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LineTo[");
        sb.append("x=").append(getX());
        sb.append(", y=").append(getY());
        return sb.append("]").toString();
    }
}

