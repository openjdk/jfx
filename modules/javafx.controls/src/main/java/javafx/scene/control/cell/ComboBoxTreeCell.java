/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import static javafx.scene.control.cell.CellUtils.createComboBox;

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
 * @since JavaFX 2.2
 */
public class ComboBoxTreeCell<T> extends DefaultTreeCell<T> {

    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a ComboBox cell factory for use in {@link TreeView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire tree cell.
     *
     * @param <T> The type of the elements contained within the TreeView.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same
     *      type as the {@literal TreeView<T>}, such that upon selection, they replace the
     *      existing value in the TreeItem {@link TreeItem#valueProperty() value}
     *      property.
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView.
     */
    @SafeVarargs
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(T... items) {
        return forTreeView(FXCollections.observableArrayList(items));
    }

    /**
     * Creates a ComboBox cell factory for use in {@link TreeView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire tree cell, excluding the space
     * allocated to the tree cell indentation and disclosure node(i.e. the arrow).
     *
     * @param <T> The type of the {@link TreeItem} elements contained within the
     *      TreeView.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ComboBox} menu is showing.
     *      These items must be of the same type as the TreeView generic type,
     *      such that upon selection, they replace the existing value in the
     *      {@link TreeItem} that is being edited (as noted in the
     *      {@link TreeView#editingItemProperty()}.
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView.
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final ObservableList<T> items) {
        return forTreeView(null, items);
    }

    /**
     * Creates a ComboBox cell factory for use in {@link TreeView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire tree cell.
     *
     * @param <T> The type of the elements contained within the TreeView.
     * @param converter A {@link StringConverter} to convert the given item (of
     *      type T) to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same
     *      type as the {@literal TreeView<T>}, such that upon selection, they replace the
     *      existing value in the TreeItem {@link TreeItem#valueProperty() value}
     *      property.
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView.
     */
    @SafeVarargs
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final StringConverter<T> converter,
            final T... items) {
        return forTreeView(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a ComboBox cell factory for use in {@link TreeView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire tree cell.
     *
     * @param <T> The type of the elements contained within the TreeView.
     * @param converter A {@link StringConverter} to convert the given item (of
     *      type T) to a String for displaying to the user.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ComboBox} menu is showing.
     *      These items must be of the same type as the TreeView generic type,
     *      such that upon selection, they replace the existing value in the
     *      {@link TreeItem} that is being edited (as noted in the
     *      {@link TreeView#editingItemProperty()}.
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView.
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final StringConverter<T> converter,
            final ObservableList<T> items) {
        return list -> new ComboBoxTreeCell<T>(converter, items);
    }



    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/
    private final ObservableList<T> items;

    private ComboBox<T> comboBox;
    private HBox hbox;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ComboBoxTreeCell with an empty items list.
     */
    public ComboBoxTreeCell() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default {@link ComboBoxTreeCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown.
     *
     * @param items The items to show in the ComboBox popup menu when selected
     *      by the user.
     */
    @SafeVarargs
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
    @SafeVarargs
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
        this.getStyleClass().add("combo-box-tree-cell");
        this.items = items;
        setConverter(converter != null ? converter : CellUtils.<T>defaultStringConverter());
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- converter
    private ObjectProperty<StringConverter<T>> converter =
            new SimpleObjectProperty<StringConverter<T>>(this, "converter");

    /**
     * The {@link StringConverter} property.
     * @return the {@link StringConverter} property
     */
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link StringConverter} to be used in this cell.
     * @param value the {@link StringConverter} to be used in this cell
     */
    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    /**
     * Returns the {@link StringConverter} used in this cell.
     * @return the {@link StringConverter} used in this cell
     */
    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }


    // --- comboBox editable
    private BooleanProperty comboBoxEditable =
            new SimpleBooleanProperty(this, "comboBoxEditable");

    /**
     * A property representing whether the ComboBox, when shown to the user,
     * is editable or not.
     * @return the property representing whether the ComboBox, when shown to the
     * user, is editable or not
     */
    public final BooleanProperty comboBoxEditableProperty() {
        return comboBoxEditable;
    }

    /**
     * Configures the ComboBox to be editable (to allow user input outside of the
     * options provide in the dropdown list).
     * @param value the editable value to be set for this ComboBox
     */
    public final void setComboBoxEditable(boolean value) {
        comboBoxEditableProperty().set(value);
    }

    /**
     * Returns true if the ComboBox is editable.
     * @return true if the ComboBox is editable
     */
    public final boolean isComboBoxEditable() {
        return comboBoxEditableProperty().get();
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the items to be displayed in the ComboBox when it is showing.
     * @return the items to be displayed in this ComboBox when it is showing
     */
    public ObservableList<T> getItems() {
        return items;
    }

    /** {@inheritDoc} */
    @Override public void startEdit() {
        if (! isEditable() || ! getTreeView().isEditable()) {
            return;
        }

        TreeItem<T> treeItem = getTreeItem();
        if (treeItem == null) {
            return;
        }

        if (comboBox == null) {
            comboBox = createComboBox(this, items, converterProperty());
            comboBox.editableProperty().bind(comboBoxEditableProperty());
        }
        if (hbox == null) {
            hbox = new HBox(CellUtils.TREE_VIEW_HBOX_GRAPHIC_PADDING);
        }

        comboBox.getSelectionModel().select(treeItem.getValue());

        super.startEdit();

        if (isEditing()) {
            setText(null);

            Node graphic = CellUtils.getGraphic(treeItem);
            if (graphic != null) {
                hbox.getChildren().setAll(graphic, comboBox);
                setGraphic(hbox);
            } else {
                setGraphic(comboBox);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        super.cancelEdit();

        setText(getConverter().toString(getItem()));
        setGraphic(null);
    }

    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        Node graphic = CellUtils.getGraphic(getTreeItem());
        CellUtils.updateItem(this, getConverter(), hbox, graphic, comboBox);
    };
}
