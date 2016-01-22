/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Properties;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.shape.Rectangle;

/**
 * Base skin for table cell controls, for example:
 * {@link javafx.scene.control.TableCell} and {@link javafx.scene.control.TreeTableCell}.
 *
 * @see javafx.scene.control.TableCell
 * @see javafx.scene.control.TreeTableCell
 * @since 9
 */
public abstract class TableCellSkinBase<C extends IndexedCell> extends CellSkinBase<C> {

    /***************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    boolean isDeferToParentForPrefWidth = false;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TableCellSkinBase instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TableCellSkinBase(final C control) {
        super(control);

        // RT-22038
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(control.widthProperty());
        clip.heightProperty().bind(control.heightProperty());
        getSkinnable().setClip(clip);
        // --- end of RT-22038

        ReadOnlyDoubleProperty columnWidthProperty = columnWidthProperty();
        if (columnWidthProperty != null) {
            columnWidthProperty.addListener(weakColumnWidthListener);
        }

        registerChangeListener(control.visibleProperty(), e -> getSkinnable().setVisible(columnVisibleProperty().get()));

        if (control.getProperties().containsKey(Properties.DEFER_TO_PARENT_PREF_WIDTH)) {
            isDeferToParentForPrefWidth = true;
        }
    }



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private InvalidationListener columnWidthListener = valueModel -> getSkinnable().requestLayout();

    private WeakInvalidationListener weakColumnWidthListener =
            new WeakInvalidationListener(columnWidthListener);



    /***************************************************************************
     *                                                                         *
     * Abstract Methods                                                        *
     *                                                                         *
     **************************************************************************/

    // Equivalent to tableColumn.widthProperty()
    abstract ReadOnlyDoubleProperty columnWidthProperty();

    // Equivalent to tableColumn.visibleProperty()
    abstract BooleanProperty columnVisibleProperty();



    /***************************************************************************
     *                                                                         *
     * Public Methods                                                          *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        ReadOnlyDoubleProperty columnWidthProperty = columnWidthProperty();
        if (columnWidthProperty != null) {
            columnWidthProperty.removeListener(weakColumnWidthListener);
        }

        super.dispose();
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        // fit the cell within this space
        // FIXME the subtraction of bottom padding isn't right here - but it
        // results in better visuals, so I'm leaving it in place for now.
        layoutLabelInArea(x, y, w, h - getSkinnable().getPadding().getBottom());
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (isDeferToParentForPrefWidth) {
            return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        }
        return columnWidthProperty().get();
    }
}
