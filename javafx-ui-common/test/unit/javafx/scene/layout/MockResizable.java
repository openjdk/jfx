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


package javafx.scene.layout;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Parent;

import com.sun.javafx.scene.DirtyBits;


class MockResizable extends Parent {
    private double minWidth = 0;
    private double minHeight = 0;
    private double prefWidth;
    private double prefHeight;
    private double maxWidth = 5000;
    private double maxHeight = 5000;
    private double width;
    private double height;

    public MockResizable(double prefWidth, double prefHeight) {
        this.prefWidth = prefWidth;
        this.prefHeight = prefHeight;
    }
    public MockResizable(double minWidth, double minHeight, double prefWidth, double prefHeight, double maxWidth, double maxHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.prefWidth = prefWidth;
        this.prefHeight = prefHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
    @Override public boolean isResizable() {
        return true;
    }
    public double getWidth() {
        return width;
    }
    public double getHeight() {
        return height;
    }
    @Override public void resize(double width, double height) {
        this.width = width;
        this.height = height;
        impl_layoutBoundsChanged();
        impl_geomChanged();
        impl_markDirty(DirtyBits.NODE_GEOMETRY);
        requestLayout();
    }
    @Override public double getBaselineOffset() {
        return Math.max(0, prefHeight - 10);
    }
    /**
     * The layout bounds of this region: {@code 0, 0  width x height}
     */
    @Override protected Bounds impl_computeLayoutBounds() {
        return new BoundingBox(0, 0, 0, width, height, 0);
    }
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_notifyLayoutBoundsChanged() {
        // change in geometric bounds does not necessarily change layoutBounds
    }
    @Override public double minWidth(double height) {
        return minWidth;
    }
    @Override public double minHeight(double width) {
        return minHeight;
    }
    @Override public double prefWidth(double height) {
        return prefWidth;
    }
    @Override public double prefHeight(double width) {
        return prefHeight;
    }
    @Override public double maxWidth(double height) {
        return maxWidth;
    }
    @Override public double maxHeight(double width) {
        return maxHeight;
    }
}
