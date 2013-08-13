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

package javafx.scene.bounds;

import javafx.beans.property.FloatProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

/**
 * A resizable version of PerfNode. Note that in this case, layoutBounds is
 * defined to only use width and height, so if x or y changes, it should cause
 * the geom to be recomputed when necessary, but should not cause a new
 * layout bounds to be created.
 */
public class ResizablePerfNode extends PerfNode {

    @Override protected Bounds impl_computeLayoutBounds() {
        return new BoundingBox(0, 0, getWidth(), getHeight());
    }

    @Override public boolean isResizable() {
        return true;
    }

    @Override public double prefWidth(double height) {
        return getWidth();
    }

    @Override public double prefHeight(double width) {
        return getHeight();
    }

    @Override
    public void impl_notifyLayoutBoundsChanged() { }

    @Override
    protected void impl_storeWidth(FloatProperty model, float value) {
        super.impl_storeWidth(model, value);
        super.impl_notifyLayoutBoundsChanged();
    }

    @Override
    protected void impl_storeHeight(FloatProperty model, float value) {
        super.impl_storeHeight(model, value);
        super.impl_notifyLayoutBoundsChanged();
    }

    @Override public double minWidth(double height) {
        return 0;
    }

    @Override public double minHeight(double width) {
        return 0;
    }

    @Override public double maxWidth(double height) {
        return 0;
    }

    @Override public double maxHeight(double width) {
        return 0;
    }
}
