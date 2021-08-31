/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.control.cell.CellUtils.createChoiceBox;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link ListCell} implementation that draws a
 * {@link ChoiceBox} node inside the cell.
 *
 * <p>By default, the ChoiceBoxTreeCell is rendered as a {@link Label} when not
 * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
 * default, stretch to fill the entire tree cell.
 *
 * <p>To create a ChoiceBoxTreeCell, it is necessary to provide zero or more
 * items that will be shown to the user when the {@link ChoiceBox} menu is
 * showing. These items must be of the same type as the TreeView TreeItems, such
 * that upon selection, they replace the existing value in the
 * {@link TreeItem#valueProperty()}.
 *
 * @param <T> The type of the TreeItems contained within the TreeView.
 * @since JavaFX 2.2
 */
public class ChoiceBoxTreeCell<T> extends DefaultTreeCell<T> {

    /* *************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a ChoiceBox cell factory for use in {@link TreeView} controls. By
     * default, the ChoiceBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
     * default, stretch to fill the entire tree cell.
     *
     * @param <T> The type of the elements contained within the TreeView.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
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
     * Creates a ChoiceBox cell factory for use in {@link TreeView} controls. By
     * default, the ChoiceBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
     * default, stretch to fill the entire tree cell, excluding the space
     * allocated to the tree cell indentation and disclosure node (i.e. the
     * arrow).
     *
     * @param <T> The type of the {@link TreeItem} elements contained within the
     *      TreeView.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ChoiceBox} menu is showing.
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
     * Creates a ChoiceBox cell factory for use in {@link TreeView} controls. By
     * default, the ChoiceBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
     * default, stretch to fill the entire tree cell.
     *
     * @param <T> The type of the elements contained within the TreeView.
     * @param converter A {@link StringConverter} to convert the given item (of type T)
     *      to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
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
     * Creates a ChoiceBox cell factory for use in {@link TreeView} controls. By
     * default, the ChoiceBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
     * default, stretch to fill the entire tree cell.
     *
     * @param <T> The type of the elements contained within the TreeView.
     * @param converter A {@link StringConverter} to convert the given item (of type T)
     *      to a String for displaying to the user.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ChoiceBox} menu is showing.
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
        return list -> new ChoiceBoxTreeCell<T>(converter, items);
    }



    /* *************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private final ObservableList<T> items;

    private ChoiceBox<T> choiceBox;

    private HBox hbox;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ChoiceBoxTreeCell with an empty items list.
     */
    public ChoiceBoxTreeCell() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default {@link ChoiceBoxTreeCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown.
     *
     * @param items The items to show in the ChoiceBox popup menu when selected
     * by the user.
     */
    @SafeVarargs
    public ChoiceBoxTreeCell(T... items) {
        this(FXCollections.observableArrayList(items));
    }

    /**
     * Creates a {@link ChoiceBoxTreeCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown, and the
     * {@link StringConverter} being used to convert the item in to a
     * user-readable form.
     *
     * @param converter A {@link Callback} that can convert an item of type T
     *      into a user-readable string so that it may then be shown in the
     *      ChoiceBox popup menu.
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    @SafeVarargs
    public ChoiceBoxTreeCell(StringConverter<T> converter, T... items) {
        this(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a default {@link ChoiceBoxTreeCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown.
     *
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    public ChoiceBoxTreeCell(ObservableList<T> items) {
        this(null, items);
    }

    /**
     * Creates a {@link ChoiceBoxTreeCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown, and the
     * {@link StringConverter} being used to convert the item in to a
     * user-readable form.
     *
     * @param converter A {@link Callback} that can convert an item of type T
     *      into a user-readable string so that it may then be shown in the
     *      ChoiceBox popup menu.
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    public ChoiceBoxTreeCell(StringConverter<T> converter, ObservableList<T> items) {
        this.getStyleClass().add("choice-box-tree-cell");
        this.items = items;
        setConverter(converter != null ? converter : CellUtils.<T>defaultStringConverter());
    }



    /* *************************************************************************
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



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the items to be displayed in the ChoiceBox when it is showing.
     * @return the items to be displayed in the ChoiceBox when it is showing
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

        if (choiceBox == null) {
            choiceBox = createChoiceBox(this, items, converterProperty());
        }
        if (hbox == null) {
            hbox = new HBox(CellUtils.TREE_VIEW_HBOX_GRAPHIC_PADDING);
        }

        choiceBox.getSelectionModel().select(treeItem.getValue());

        super.startEdit();

        if (isEditing()) {
            setText(null);

            Node graphic = getTreeItemGraphic();
            if (graphic != null) {
                hbox.getChildren().setAll(graphic, choiceBox);
                setGraphic(hbox);
            } else {
                setGraphic(choiceBox);
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

        CellUtils.updateItem(this, getConverter(), hbox, getTreeItemGraphic(), choiceBox);
    };



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private Node getTreeItemGraphic() {
        TreeItem<T> treeItem = getTreeItem();
        return treeItem == null ? null : treeItem.getGraphic();
    }
}
