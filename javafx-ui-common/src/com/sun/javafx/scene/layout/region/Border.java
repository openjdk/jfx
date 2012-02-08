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
package com.sun.javafx.scene.layout.region;

import javafx.geometry.Insets;

/**
 * Baseclass for CSS style Borders
 *
 */
public abstract class Border {

    protected static class Builder {
        protected double leftWidth = 1;
        protected double topWidth = 1;
        protected double rightWidth = 1;
        protected double bottomWidth = 1;
        protected boolean proportionalWidth = false;
        protected Insets offsets = Insets.EMPTY;

        public Builder setLeftWidth(double f) { leftWidth = f; return this; }
        public Builder setTopWidth(double f) { topWidth = f; return this; }
        public Builder setRightWidth(double f) { rightWidth = f; return this; }
        public Builder setBottomWidth(double f) { bottomWidth = f; return this; }
        public Builder setProportionalWidth(boolean b) { proportionalWidth = b; return this; }
        public Builder setOffsets(Insets i) { offsets = i; return this; }
    }

    /**
     * Width of the border on the left of the Region. If the Region is non
     * rectangular then this is used for the border around the whole shape.
     * Percentages are a percentage of region width.
     *
     * @css border-left-width
     * @defaultValue 1
     */
    public double getLeftWidth() { return leftWidth; }
    final private double leftWidth;

    /**
     * Width of the border on the top of the Region. If {@code null} then the
     * leftWidth is used. Percentages are a percentage of region height.
     *
     * @css border-top-width
     * @defaultValue null = use leftWidth
     */
    public double getTopWidth() { return topWidth; }
    final private double topWidth;

    /**
     * Width of the border on the right of the Region. If {@code null} then the
     * leftWidth is used. Percentages are a percentage of region width.
     *
     * @css border-right-width
     * @defaultValue null = use leftWidth
     */
    public double getRightWidth() { return rightWidth; }
    final private double rightWidth;

    /**
     * Width of the border on the top of the Region. If {@code null} then the
     * leftWidth is used. Percentages are a percentage of region height.
     *
     * @css border-bottom-width
     * @defaultValue null = use leftWidth
     */
    public double getBottomWidth() { return bottomWidth; }
    final private double bottomWidth;

    /**
     * Indicates whether the width units are proportional or absolute.
     * If this flag is true, width units are defined in a [0..1] space and
     * represent a percentage of the image width. If this flag is false,
     * then width units are image pixels.
     * @default false
     */
    public boolean isProportionalWidth() { return proportionalWidth; }
    final private boolean proportionalWidth;

    /**
     * Offsets to use from the region bounds. Units are scene graph units.
     *
     * @defaultValue null
     */
    public Insets getOffsets() { return offsets; }
    final private Insets offsets;

    /** */
    protected Border(double topWidth, double rightWidth, double bottomWidth,
            double leftWidth, boolean proportional, Insets offsets) {
        this.topWidth = topWidth;
        this.rightWidth = rightWidth;
        this.bottomWidth = bottomWidth;
        this.leftWidth = leftWidth;
        this.proportionalWidth = proportional;
        this.offsets = offsets;
    }
}
