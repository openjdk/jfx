/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.cell;

import static com.sun.javafx.scene.control.cell.ComboBoxCell.createComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link TreeCell} implementation that draws a 
 * {@link ComboBox} node inside the cell.
 * 
 * <p>By default, the ComboBoxTreeCell is rendered as a {@link Label} when not 
 * being edited, and as a ComboBox when in editing mode. The ComboBox will, by 
 * default, stretch to fill the entire tree cell.
 * 
 * <p>To create a ComboBoxTreeCell, it is necessary to provide zero or more 
 * items that will be shown to the user when the {@link ComboBox} menu is 
 * showing. These items must be of the same type as the TreeView TreeItems, such 
 * that upon selection, they replace the existing value in the 
 * {@link TreeItem#valueProperty()}.
 * 
 * @param <T> The type of the TreeItems contained within the TreeView.
 */
public class ComboBoxTreeCell<T> extends TreeCell<T> {
    private final ObservableList<T> items;

    private ComboBox<T> comboBox;
    
    private final StringConverter<T> converter;

    /**
     * Creates a default {@link ComboBoxTreeCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown.
     * 
     * @param items The items to show in the ComboBox popup menu when selected 
     *      by the user.
     */
    public ComboBoxTreeCell(T... items) {
        this(FXCollections.observableArrayList(items));
    }
    
    /**
     * Creates a {@link ComboBoxTreeCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown, and the 
     * {@link StringConverter} being used to convert the item in to a 
     * user-readable form.
     * 
     * @param converter A {@link StringConverter} that can convert an item of 
     *      type T into a user-readable string so that it may then be shown in 
     *      the ComboBox popup menu.
     * @param items The items to show in the ComboBox popup menu when selected 
     *      by the user.
     */
    public ComboBoxTreeCell(StringConverter<T> converter, T... items) {
        this(converter, FXCollections.observableArrayList(items));
    }
    
    /**
     * Creates a default {@link ComboBoxTreeCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown.
     * 
     * @param items The items to show in the ComboBox popup menu when selected 
     *      by the user.
     */
    public ComboBoxTreeCell(ObservableList<T> items) {
        this(null, items);
    }

    /**
     * Creates a {@link ComboBoxTreeCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown, and the 
     * {@link StringConverter} being used to convert the item in to a 
     * user-readable form.
     * 
     * @param converter A {@link StringConverter} that can convert an item of 
     *      type T into a user-readable string so that it may then be shown in 
     *      the ComboBox popup menu.
     * @param items The items to show in the ComboBox popup menu when selected 
     *      by the user.
     */
    public ComboBoxTreeCell(StringConverter<T> converter, ObservableList<T> items) {
        this.items = items;
        this.converter = converter != null ? converter : CellUtils.<T>defaultStringConverter();
    }
    
    /** {@inheritDoc} */
    @Override public void startEdit() {
        if (! isEditable() || ! getTreeView().isEditable()) {
            return;
        }
        if (comboBox == null) {
            comboBox = createComboBox(this, items);
        }
        
        comboBox.getSelectionModel().select(getTreeItem().getValue());
        
        super.startEdit();
        setText(null);
        setGraphic(comboBox);
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        super.cancelEdit();
        
        setText(converter.toString(getItem()));
        setGraphic(null);
    }
    
    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        ComboBoxCell.updateItem(this, comboBox, converter);
    };
}