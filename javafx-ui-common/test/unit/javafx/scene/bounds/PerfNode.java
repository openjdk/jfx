/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.bounds;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.Node;

import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.pgstub.StubRectangle;
import com.sun.javafx.sg.PGNode;

/**
 * A special node used for performance tests to make sure that the minimum
 * amount of bounds computation work happens as possible.
 */
public class PerfNode extends Node {


    public PerfNode() {
    }

    public PerfNode(float x, float y, float width, float height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }
    private FloatProperty x;

    public final void setX(float value) {
        xProperty().set(value);
    }

    public final float getX() {
        return x == null ? 0 : x.get();
    }

    public FloatProperty xProperty() {
        if (x == null) {
            x = new SimpleFloatProperty() {

                @Override
                protected void invalidated() {
                    impl_geomChanged();
                }
            };
        }
        return x;
    }
    private FloatProperty y;

    public final void setY(float value) {
        yProperty().set(value);
    }

    public final float getY() {
        return y == null ? 0 : y.get();
    }

    public FloatProperty yProperty() {
        if (y == null) {
            y = new SimpleFloatProperty() {

                @Override
                protected void invalidated() {
                    impl_geomChanged();
                }
            };
        }
        return y;
    }
    private FloatProperty width;

    public final void setWidth(float value) {
        widthProperty().set(value);
    }

    public final float getWidth() {
        return width == null ? 100 : width.get();
    }

    public FloatProperty widthProperty() {
        if (width == null) {
            width = new SimpleFloatProperty() {

                @Override
                protected void invalidated() {
                    impl_storeWidth(width, get());
                }
            };
        }
        return width;
    }

    protected void impl_storeWidth(FloatProperty model, float value) {
        impl_geomChanged();
    }

    private FloatProperty height;

    public final void setHeight(float value) {
        heightProperty().set(value);
    }

    public final float getHeight() {
        return height == null ? 100 : height.get();
    }

    public FloatProperty heightProperty() {
        if (height == null) {
            height = new SimpleFloatProperty() {

                @Override
                protected void invalidated() {
                    impl_storeHeight(height, get());
                }
            };
        }
        return height;
    }

    protected void impl_storeHeight(FloatProperty model, float value) {
        impl_geomChanged();
    }

    int geomComputeCount = 0;

    public com.sun.javafx.geom.BaseBounds impl_computeGeomBounds(com.sun.javafx.geom.BaseBounds bounds, com.sun.javafx.geom.transform.BaseTransform tx) {
        geomComputeCount++;
        bounds = bounds.deriveWithNewBounds(0, 0, 0, 100, 100, 0);
        return bounds;
    }

    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        // Stub
        return false;
    }

    @Override
    protected PGNode impl_createPGNode() {
        return new StubRectangle();
    }

    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return null;
    }
}
