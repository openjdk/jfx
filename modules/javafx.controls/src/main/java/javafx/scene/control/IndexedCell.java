/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.css.PseudoClass;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;

/**
 * An implementation of {@link Cell} which contains an index property which maps
 * into the data model underlying the visualization. Despite this,
 * {@code IndexedCell} should not be instantiated directly in a cell factory
 * (refer to {@link Cell} for more details on what a cell factory is).
 * Instead of creating {@code IndexedCell} directly, you should
 * instead make use of the control-specific cell implementations (for example,
 * {@link ListCell}, {@link TreeCell}) {@link TableRow} and {@link TableCell}).
 * For more information about using and customizing cells, refer to the
 * {@link Cell} API documentation.
 *
 * <p>Because each sequential index represents a single sequential element in the
 * control, this allows for easy alternative row highlighting. By default the
 * controls which use {@link Cell Cells} provide their own alternative row
 * highlighting colors, but this can be overridden using two pseudo class states
 * provided by {@code IndexedCell}: "even" and "odd".
 *
 * @param <T> The type of the item contained within the Cell.
 * @since JavaFX 2.0
 */
public class IndexedCell<T> extends Cell<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default IndexedCell with the default style class of 'indexed-cell'.
     */
    public IndexedCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Index
    private ReadOnlyIntegerWrapper index = new ReadOnlyIntegerWrapper(this, "index", -1) {
        @Override protected void invalidated() {
            boolean active = ((get() % 2) == 0);
            pseudoClassStateChanged(PSEUDO_CLASS_EVEN,  active);
            pseudoClassStateChanged(PSEUDO_CLASS_ODD,  !active);
        }
    };

    /**
     * Returns the index that this cell represents in the underlying control
     * data model.
     * @return the index that this cell represents in the underlying control
     * data model
     */
    public final int getIndex() { return index.get(); }

    /**
     * The location of this cell in the virtualized control (e.g:
     * {@link ListView}, {@link TreeView}, {@link TableView}, etc). This is the model
     * index which corresponds exactly with the Cell {@link #itemProperty() item}
     * property. For example,
     * in the case of a {@link ListView}, this means the following:
     * <code>cell.item == listView.getItems().get(cell.getIndex())</code>
     * @return the location of this cell in the virtualized control
     */
    public final ReadOnlyIntegerProperty indexProperty() { return index.getReadOnlyProperty(); }

    /***************************************************************************
     *                                                                         *
     * Expert API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Updates the index associated with this IndexedCell.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins. It is not common
     *       for developers or designers to access this function directly.
     * @param i the index associated with this indexed cell
     */
    public void updateIndex(int i) {
        final int oldIndex = index.get();
        index.set(i);
        indexChanged(oldIndex, i);
    }

    /**
     * This method is called whenever the index is changed, regardless of whether
     * the new index is the same as the old index.
     * @param oldIndex
     * @param newIndex
     */
    void indexChanged(int oldIndex, int newIndex) {
        // no-op
    }

    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "indexed-cell";

    private static final PseudoClass PSEUDO_CLASS_ODD = PseudoClass.getPseudoClass("odd");
    private static final PseudoClass PSEUDO_CLASS_EVEN = PseudoClass.getPseudoClass("even");

}
