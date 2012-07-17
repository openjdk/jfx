/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.skin;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

/**
 *
 */
public class SeparatorSkin extends SkinBase<Separator, BehaviorBase<Separator>> {

    /**
     * Separator's have no intrinsic length, so we need to hard code some sort
     * of default preferred size when a separator is not otherwise being resized.
     * This is the default length to use (height when vertical, width when horizontal)
     * for computing the preferred width/height.
     */
    private static final double DEFAULT_LENGTH = 10;

    /**
     * The region to use for rendering the line. The line is specified via
     * CSS. By default we use a single stroke to render the line, but the
     * programmer could use images or whatnot from CSS instead.
     */
    private final Region line;

    /**
     * Create a new SeparatorSkin. Just specify the separator, thanks very much.
     * @param separator not null
     */
    public SeparatorSkin(Separator separator) {
        // There is no behavior for the separator, so we just create a
        // dummy behavior base instead, since SkinBase will complain
        // about it being null.
        super(separator, new BehaviorBase<Separator>(separator));

        line = new Region();
        line.getStyleClass().setAll("line");

        getChildren().add(line);
        registerChangeListener(separator.orientationProperty(), "ORIENTATION");
        registerChangeListener(separator.halignmentProperty(), "HALIGNMENT");
        registerChangeListener(separator.valignmentProperty(), "VALIGNMENT");
    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("ORIENTATION".equals(p) || "HALIGNMENT".equals(p) || "VALIGNMENT".equals(p)) {
            requestLayout();
        }
    }

    /**
     * We only need to deal with the single "line" child region. The important
     * thing here is that we want a horizontal separator to have a line which is
     * as wide as the separator (less the left/right padding), but as thin as
     * it can be (based on its own pref height). The same idea for a vertical
     * separator. It should be as tall as the separator (less the top and
     * bottom padding) but as thin as can be (the pref width of the line).
     * <p>
     * Then position the line within the separator such that the alignment
     * properties are honored.
     */
    @Override protected void layoutChildren() {
        final Separator sep = getSkinnable();
        final Insets p = getInsets();

        // content width and height
        final double cw = getWidth() - (p.getLeft() + p.getRight());
        final double ch = getHeight() - (p.getTop() + p.getBottom());

        if (sep.getOrientation() == Orientation.HORIZONTAL) {
            // Resize to the content width, and the pref height of the line.
            line.resize(cw, line.prefHeight(-1));
        } else {
            // Resize to the pref width of the line and the content height.
            line.resize(line.prefWidth(-1), ch);
        }

        // Now that the line has been sized, simply position it
        positionInArea(line, p.getLeft(), p.getTop(), cw, ch, 0, sep.getHalignment(), sep.getValignment());
    }

    @Override protected double computePrefWidth(double h) {
        double w = getSkinnable().getOrientation() == Orientation.VERTICAL ? line.prefWidth(-1) : DEFAULT_LENGTH;
        return w + getInsets().getLeft() + getInsets().getRight();
    }

    @Override protected double computePrefHeight(double w) {
        double h = getSkinnable().getOrientation() == Orientation.VERTICAL ? DEFAULT_LENGTH : line.prefHeight(-1);
        return h + getInsets().getTop() + getInsets().getBottom();
    }

    @Override protected double computeMaxWidth(double h) {
        return getSkinnable().getOrientation() == Orientation.VERTICAL ? getSkinnable().prefWidth(h) : Double.MAX_VALUE;
    }

    @Override protected double computeMaxHeight(double w) {
        return getSkinnable().getOrientation() == Orientation.VERTICAL ? Double.MAX_VALUE : getSkinnable().prefHeight(w);
    }
}
