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
 * Creates a vertical line path element from the current point to y.
 *
 * <p>For more information on path elements see the {@link Path} and
 * {@link PathElement} classes.
 *
 * <p>Example:
<PRE>
import javafx.scene.shape.*;

Path path = new Path();
path.getElements().add(new MoveTo(50.0f, 0.0f));
path.getElements().add(new VLineTo(50.0f));
</PRE>
 */
public  class VLineTo extends PathElement {

    /**
     * Creates an empty instance of VLineTo.
     */
    public VLineTo() {
    }

    /**
     * Creates an instance of VLineTo.
     * @param y the vertical coordinate to line to
     */
     public VLineTo(double y) {
        setY(y);
    }

    /**
     * Defines the Y coordinate.
     *
     * @defaultValue 0.0
     */
    private DoubleProperty y;


    public final void setY(double value) {
        yProperty().set(value);
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
                    return VLineTo.this;
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
    void addTo(PGPath pgPath) {
        if (isAbsolute()) {
            pgPath.addLineTo(pgPath.getCurrentX(), (float)getY());
        } else {
            pgPath.addLineTo(pgPath.getCurrentX(), (float)(pgPath.getCurrentY() + getY()));
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
            path.lineTo(path.getCurrentX(), (float)getY());
        } else {
            path.lineTo(path.getCurrentX(), (float)(path.getCurrentY() + getY()));
        }
    }
}
