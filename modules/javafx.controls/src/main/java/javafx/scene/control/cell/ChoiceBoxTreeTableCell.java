/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventHandler;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link TreeTableCell} implementation that draws a
 * {@link ChoiceBox} node inside the cell.
 *
 * <p>By default, the ChoiceBoxTreeTableCell is rendered as a {@link Label} when not
 * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
 * default, stretch to fill the entire table cell.
 *
 * <p>To create a ChoiceBoxTreeTableCell, it is necessary to provide zero or more
 * items that will be shown to the user when the {@link ChoiceBox} menu is
 * showing. These items must be of the same type as the TreeTableColumn.
 *
 * @param <S> The type of the TreeTableView generic type
 * @param <T> The type of the elements contained within the TreeTableColumn.
 * @since JavaFX 8.0
 */
public class ChoiceBoxTreeTableCell<S,T> extends TreeTableCell<S,T> {

    /* *************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a ChoiceBox cell factory for use in {@link TreeTableColumn} controls.
     * By default, the ChoiceBoxCell is rendered as a {@link Label} when not
     * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will,
     * by default, stretch to fill the entire list cell.
     *
     * @param <S> The type of the TreeTableView generic type
     * @param <T> The type of the elements contained within the TreeTableColumn.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
     *      type as the TreeTableColumn. Note that it is up to the developer to set
     *      {@link EventHandler event handlers} to listen to edit events in the
     *      TreeTableColumn, and react accordingly. Methods of interest include
     *      {@link TreeTableColumn#setOnEditStart(javafx.event.EventHandler) setOnEditStart},
     *      {@link TreeTableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit},
     *      and {@link TreeTableColumn#setOnEditCancel(javafx.event.EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TreeTableCell that is able to
     *      work on the type of element contained within the TreeTableColumn.
     */
    @SafeVarargs
    public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(final T... items) {
        return forTreeTableColumn(null, items);
    }

    /**
     * Creates a ChoiceBox cell factory for use in {@link TreeTableColumn} controls.
     * By default, the ChoiceBoxCell is rendered as a {@link Label} when not
     * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox
     * will, by default, stretch to fill the entire list cell.
     *
     * @param <S> The type of the TreeTableView generic type
     * @param <T> The type of the elements contained within the TreeTableColumn.
     * @param converter A {@link StringConverter} to convert the given item (of type T)
     *      to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
     *      type as the TreeTableColumn. Note that it is up to the developer to set
     *      {@link EventHandler event handlers} to listen to edit events in the
     *      TreeTableColumn, and react accordingly. Methods of interest include
     *      {@link TreeTableColumn#setOnEditStart(javafx.event.EventHandler) setOnEditStart},
     *      {@link TreeTableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit},
     *      and {@link TreeTableColumn#setOnEditCancel(javafx.event.EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TreeTableCell that is able to
     *      work on the type of element contained within the TreeTableColumn.
     */
    @SafeVarargs
    public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(
            final StringConverter<T> converter,
            final T... items) {
        return forTreeTableColumn(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a ChoiceBox cell factory for use in {@link TreeTableColumn} controls.
     * By default, the ChoiceBoxCell is rendered as a {@link Label} when not
     * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will,
     * by default, stretch to fill the entire list cell.
     *
     * @param <S> The type of the TreeTableView generic type
     * @param <T> The type of the elements contained within the TreeTableColumn.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
     *      type as the TreeTableColumn. Note that it is up to the developer to set
     *      {@link EventHandler event handlers} to listen to edit events in the
     *      TreeTableColumn, and react accordingly. Methods of interest include
     *      {@link TreeTableColumn#setOnEditStart(javafx.event.EventHandler) setOnEditStart},
     *      {@link TreeTableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit},
     *      and {@link TreeTableColumn#setOnEditCancel(javafx.event.EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TreeTableCell that is able to
     *      work on the type of element contained within the TreeTableColumn.
     */
    public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(
            final ObservableList<T> items) {
        return forTreeTableColumn(null, items);
    }

    /**
     * Creates a ChoiceBox cell factory for use in {@link TreeTableColumn} controls.
     * By default, the ChoiceBoxCell is rendered as a {@link Label} when not
     * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will,
     * by default, stretch to fill the entire list cell.
     *
     * @param <S> The type of the TreeTableView generic type
     * @param <T> The type of the elements contained within the TreeTableColumn.
     * @param converter A {@link StringConverter} to convert the given item (of type T)
     * to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
     *      type as the TreeTableColumn. Note that it is up to the developer to set
     *      {@link EventHandler event handlers} to listen to edit events in the
     *      TreeTableColumn, and react accordingly. Methods of interest include
     *      {@link TreeTableColumn#setOnEditStart(javafx.event.EventHandler) setOnEditStart},
     *      {@link TreeTableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit},
     *      and {@link TreeTableColumn#setOnEditCancel(javafx.event.EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TreeTableCell that is able to
     *      work on the type of element contained within the TreeTableColumn.
     */
    public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(
            final StringConverter<T> converter,
            final ObservableList<T> items) {
        return list -> new ChoiceBoxTreeTableCell<>(converter, items);
    }



    /* *************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private final ObservableList<T> items;

    private ChoiceBox<T> choiceBox;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ChoiceBoxTreeTableCell with an empty items list.
     */
    public ChoiceBoxTreeTableCell() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default {@link ChoiceBoxTreeTableCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown.
     *
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    @SafeVarargs
    public ChoiceBoxTreeTableCell(T... items) {
        this(FXCollections.observableArrayList(items));
    }

    /**
     * Creates a {@link ChoiceBoxTreeTableCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown, and the
     * {@link StringConverter} being used to convert the item in to a
     * user-readable form.
     *
     * @param converter A {@link StringConverter} that can convert an item of type T
     *      into a user-readable string so that it may then be shown in the
     *      ChoiceBox popup menu.
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    @SafeVarargs
    public ChoiceBoxTreeTableCell(StringConverter<T> converter, T... items) {
        this(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a default {@link ChoiceBoxTreeTableCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown.
     *
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    public ChoiceBoxTreeTableCell(ObservableList<T> items) {
        this(null, items);
    }

    /**
     * Creates a {@link ChoiceBoxTreeTableCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown, and the
     * {@link StringConverter} being used to convert the item in to a
     * user-readable form.
     *
     * @param converter A {@link StringConverter} that can convert an item of type T
     *      into a user-readable string so that it may then be shown in the
     *      ChoiceBox popup menu.
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    public ChoiceBoxTreeTableCell(StringConverter<T> converter, ObservableList<T> items) {
        this.getStyleClass().add("choice-box-tree-table-cell");
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
            new SimpleObjectProperty<>(this, "converter");

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
        super.startEdit();
        if (!isEditing()) {
            return;
        }

        if (choiceBox == null) {
            choiceBox = createChoiceBox(this, items, converterProperty());
        }

        choiceBox.getSelectionModel().select(getItem());

        setText(null);
        setGraphic(choiceBox);
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
        CellUtils.updateItem(this, getConverter(), null, null, choiceBox);
    }
}
