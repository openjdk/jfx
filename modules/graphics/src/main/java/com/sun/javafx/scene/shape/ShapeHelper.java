/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.shape;

import com.sun.javafx.sg.prism.NGShape;
import com.sun.javafx.util.Utils;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;

/**
 * Used to access internal methods of Shape.
 */
public abstract class ShapeHelper {
    private static ShapeAccessor shapeAccessor;

    static {
        Utils.forceInit(Shape.class);
    }

    protected ShapeHelper() {
    }

    // TODO: This method will eventually be moved to Node once all the
    // impl_XXX encapsulation work is done
    private static ShapeHelper getHelper(Shape shape) {
        return shapeAccessor.getHelper(shape);
    }

    // TODO: This method will eventually be moved to Node once all the
    // impl_XXX encapsulation work is done
    protected static void setHelper(Shape shape, ShapeHelper shapeHelper) {
        shapeAccessor.setHelper(shape, shapeHelper);
    }

    /*
     * Static helper methods for cases where the implementation is done in an
     * instance method that is overridden by subclasses.
     * These methods exist in the base class only.
     */

    public static Paint cssGetFillInitialValue(Shape shape) {
        return getHelper(shape).cssGetFillInitialValueImpl(shape);
    }

    public static Paint cssGetStrokeInitialValue(Shape shape) {
        return getHelper(shape).cssGetStrokeInitialValueImpl(shape);
    }

    public static com.sun.javafx.geom.Shape configShape(Shape shape) {
        return getHelper(shape).configShapeImpl(shape);
    }

    /*
     * Methods that will be overridden by subclasses
     */

    protected Paint cssGetFillInitialValueImpl(Shape shape) {
        return shapeAccessor.doCssGetFillInitialValue(shape);
    }

    protected Paint cssGetStrokeInitialValueImpl(Shape shape) {
        return shapeAccessor.doCssGetStrokeInitialValue(shape);
    }

    protected abstract com.sun.javafx.geom.Shape configShapeImpl(Shape shape);

    /*
     * Methods used by Shape (base) class only
     */

    public static NGShape.Mode getMode(Shape shape) {
        return shapeAccessor.getMode(shape);
    }

    public static void setMode(Shape shape, NGShape.Mode mode) {
        shapeAccessor.setMode(shape, mode);
    }

    public static void setShapeChangeListener(Shape shape, Runnable listener) {
        shapeAccessor.setShapeChangeListener(shape, listener);
    }

    public static void setShapeAccessor(final ShapeAccessor newAccessor) {
        if (shapeAccessor != null) {
            throw new IllegalStateException();
        }

        shapeAccessor = newAccessor;
    }

    public interface ShapeAccessor {
        ShapeHelper getHelper(Shape shape);
        void setHelper(Shape shape, ShapeHelper shapeHelper);
        Paint doCssGetFillInitialValue(Shape shape);
        Paint doCssGetStrokeInitialValue(Shape shape);
        NGShape.Mode getMode(Shape shape);
        void setMode(Shape shape, NGShape.Mode mode);
        void setShapeChangeListener(Shape shape, Runnable listener);
    }

}
