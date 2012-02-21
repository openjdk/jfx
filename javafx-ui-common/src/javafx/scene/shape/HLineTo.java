/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */

package javafx.scene.shape;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.sg.PGPath;


/**
 * Creates a horizontal line path element from the current point to x.
 *
 * <p>For more information on path elements see the {@link Path} and
 * {@link PathElement} classes.
 *
 * <p>Example:
 *
<PRE>
import javafx.scene.shape.*;

Path path = new Path();
path.getElements().add(new MoveTo(0.0f, 0.0f));
path.getElements().add(new HLineTo(80.0f));
</PRE>
 */
public class HLineTo extends PathElement {

    /**
     * Creates an empty instance of HLineTo.
     */
    public HLineTo() {
    }

    /**
     * Creates an instance of HLineTo.
     * @param x the horizontal coordinate to line to
     */
    public HLineTo(double x) {
        setX(x);
    }

    /**
     * Defines the X coordinate.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty x;


    public final void setX(double value) {
        xProperty().set(value);
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
                    return HLineTo.this;
                }

                @Override
                public String getName() {
                    return "x";
                }
            };
        }
        return x;
    }

    @Override
    void addTo(PGPath pgPath) {
        if (isAbsolute()) {
            pgPath.addLineTo((float)getX(), pgPath.getCurrentY());
        } else {
            pgPath.addLineTo((float)(pgPath.getCurrentX() + getX()), pgPath.getCurrentY());
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_addTo(Path2D path) {
        if (isAbsolute()) {
            path.lineTo((float)getX(), path.getCurrentY());
        } else {
            path.lineTo((float)(path.getCurrentX() + getX()), path.getCurrentY());
        }
    }
}

