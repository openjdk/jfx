/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.Separator;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;

import java.util.Collections;

/**
 * Default skin implementation for the {@link Separator} control.
 *
 * @see Separator
 * @since 9
 */
public class SeparatorSkin extends SkinBase<Separator> {

    /* *************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * Separator's have no intrinsic length, so we need to hard code some sort
     * of default preferred size when a separator is not otherwise being resized.
     * This is the default length to use (height when vertical, width when horizontal)
     * for computing the preferred width/height.
     */
    private static final double DEFAULT_LENGTH = 10;



    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * The region to use for rendering the line. The line is specified via
     * CSS. By default we use a single stroke to render the line, but the
     * programmer could use images or whatnot from CSS instead.
     */
    private final Region line;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SeparatorSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list.
     *
     * @param control The control that this skin should be installed onto.
     */
    public SeparatorSkin(Separator control) {
        // There is no behavior for the separator, so we just create a
        // dummy behavior base instead, since SkinBase will complain
        // about it being null.
        super(control);

        line = new Region();
        line.getStyleClass().setAll("line");

        getChildren().add(line);
        registerChangeListener(control.orientationProperty(), e -> getSkinnable().requestLayout());
        registerChangeListener(control.halignmentProperty(), e -> getSkinnable().requestLayout());
        registerChangeListener(control.valignmentProperty(), e -> getSkinnable().requestLayout());
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

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
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        final Separator sep = getSkinnable();

        if (sep.getOrientation() == Orientation.HORIZONTAL) {
            // Resize to the content width, and the pref height of the line.
            line.resize(w, line.prefHeight(-1));
        } else {
            // Resize to the pref width of the line and the content height.
            line.resize(line.prefWidth(-1), h);
        }

        // Now that the line has been sized, simply position it
        positionInArea(line, x, y, w, h, 0, sep.getHalignment(), sep.getValignment());
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double h, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Separator sep = getSkinnable();
        double w = sep.getOrientation() == Orientation.VERTICAL ? line.prefWidth(-1) : DEFAULT_LENGTH;
        return w + leftInset + rightInset;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double w, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Separator sep = getSkinnable();
        double h = sep.getOrientation() == Orientation.VERTICAL ? DEFAULT_LENGTH : line.prefHeight(-1);
        return h + topInset + bottomInset;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double h, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Separator sep = getSkinnable();
        return sep.getOrientation() == Orientation.VERTICAL ? sep.prefWidth(h) : Double.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double w, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Separator sep = getSkinnable();
        return sep.getOrientation() == Orientation.VERTICAL ? Double.MAX_VALUE : sep.prefHeight(w);
    }
}
