/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.text.Text;

import com.sun.javafx.scene.control.behavior.DateCellBehavior;

/**
 * Default skin implementation for the {@link DateCell} control.
 *
 * @see DateCell
 * @since 9
 */
public class DateCellSkin extends CellSkinBase<DateCell> {

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final BehaviorBase<DateCell> behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new DateCellSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public DateCellSkin(DateCell control) {
        super(control);

        // install default input map for the DateCell control
        behavior = new DateCellBehavior(control);
//        control.setInputMap(behavior.getInputMap());

        control.setMaxWidth(Double.MAX_VALUE); // make the cell grow to fill a GridPane's cell
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected void updateChildren() {
        super.updateChildren();

        Text secondaryText = (Text)getSkinnable().getProperties().get("DateCell.secondaryText");
        if (secondaryText != null) {
            // LabeledSkinBase rebuilds the children list each time, so it's
            // safe to add more here.
            secondaryText.setManaged(false);
            getChildren().add(secondaryText);
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        super.layoutChildren(x, y, w, h);

        Text secondaryText = (Text)getSkinnable().getProperties().get("DateCell.secondaryText");
        if (secondaryText != null) {
            // Place the secondary Text node at BOTTOM_RIGHT.
            double textX = x + w - rightLabelPadding()  - secondaryText.getLayoutBounds().getWidth();
            double textY = y + h - bottomLabelPadding() - secondaryText.getLayoutBounds().getHeight();
            secondaryText.relocate(snapPositionX(textX), snapPositionY(textY));
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height,
                                                double topInset, double rightInset,
                                                double bottomInset, double leftInset) {
        double pref = super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        return snapSizeX(Math.max(pref, cellSize()));
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width,
                                                 double topInset, double rightInset,
                                                 double bottomInset, double leftInset) {
        double pref = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
        return snapSizeY(Math.max(pref, cellSize()));
    }



    /* *************************************************************************
     *                                                                         *
     * Pirvate implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private double cellSize() {
        double cellSize = getCellSize();
        Text secondaryText = (Text)getSkinnable().getProperties().get("DateCell.secondaryText");
        if (secondaryText != null && cellSize == DEFAULT_CELL_SIZE) {
            // Workaround for RT-31643. The cellSize property was not yet set from CSS.
            cellSize = 36;
        }
        return cellSize;
    }
}
