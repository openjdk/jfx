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

import static javafx.scene.control.cell.CellUtils.createComboBox;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link ListCell} implementation that draws a
 * {@link ComboBox} node inside the cell.
 *
 * <p>By default, the ComboBoxListCell is rendered as a {@link Label} when not
 * being edited, and as a ComboBox when in editing mode. The ComboBox will, by
 * default, stretch to fill the entire list cell.
 *
 * <p>To create a ComboBoxListCell, it is necessary to provide zero or more
 * items that will be shown to the user when the {@link ComboBox} menu is
 * showing. These items must be of the same type as the ListView items sequence,
 * such that upon selection, they replace the existing value in the
 * {@link ListView#itemsProperty() items} list.
 *
 * @param <T> The type of the elements contained within the ListView.
 * @since JavaFX 2.2
 */
public class ComboBoxListCell<T> extends ListCell<T> {

    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a ComboBox cell factory for use in {@link ListView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same
     *      type as the ListView items list, such that upon selection, they
     *      replace the existing value in the
     *      {@link ListView#itemsProperty() items} list.
     * @return A {@link Callback} that will return a ListCell that is able to
     *      work on the type of element contained within the ListView.
     */
    @SafeVarargs
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(final T... items) {
        return forListView(FXCollections.observableArrayList(items));
    }

    /**
     * Creates a ComboBox cell factory for use in {@link ListView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param converter A {@link StringConverter} to convert the given item (of
     *      type T) to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same
     *      type as the ListView items list, such that
     *      upon selection, they replace the existing value in the
     *      {@link ListView#itemsProperty() items} list.
     * @return A {@link Callback} that will return a ListCell that is able to
     *      work on the type of element contained within the ListView.
     */
    @SafeVarargs
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
                final StringConverter<T> converter,
                final T... items) {
        return forListView(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a ComboBox cell factory for use in {@link ListView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ComboBox} menu is showing.
     *      These items must be of the same type as the ListView items sequence,
     *      such that upon selection, they replace the existing value in the
     *      {@link ListView#itemsProperty() items} list.
     * @return A {@link Callback} that will return a ListCell that is able to
     *      work on the type of element contained within the ListView.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final ObservableList<T> items) {
        return forListView(null, items);
    }

    /**
     * Creates a ComboBox cell factory for use in {@link ListView} controls. By
     * default, the ComboBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by
     * default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param converter A {@link StringConverter} to convert the given item (of
     *      type T) to a String for displaying to the user.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ComboBox} menu is showing.
     *      These items must be of the same type as the ListView items sequence,
     *      such that upon selection, they replace the existing value in the
     *      {@link ListView#itemsProperty() items} list.
     * @return A {@link Callback} that will return a ListCell that is able to
     *      work on the type of element contained within the ListView.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final StringConverter<T> converter,
            final ObservableList<T> items) {
        return list -> new ComboBoxListCell<T>(converter, items);
    }



    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private final ObservableList<T> items;

    private ComboBox<T> comboBox;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ComboBoxListCell with an empty items list.
     */
    public ComboBoxListCell() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default {@link ComboBoxListCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown.
     *
     * @param items The items to show in the ComboBox popup menu when selected
     *      by the user.
     */
    @SafeVarargs
    public ComboBoxListCell(T... items) {
        this(FXCollections.observableArrayList(items));
    }

    /**
     * Creates a {@link ComboBoxListCell} instance with the given items
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
    public ComboBoxListCell(StringConverter<T> converter, T... items) {
        this(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a default {@link ComboBoxListCell} instance with the given items
     * being used to populate the {@link ComboBox} when it is shown.
     *
     * @param items The items to show in the ComboBox popup menu when selected
     *      by the user.
     */
    public ComboBoxListCell(ObservableList<T> items) {
        this(null, items);
    }

    /**
     * Creates a {@link ComboBoxListCell} instance with the given items
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
    public ComboBoxListCell(StringConverter<T> converter, ObservableList<T> items) {
        this.getStyleClass().add("combo-box-list-cell");
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
     * @return the property representing whether the ComboBox, when shown to
     * the user, is editable or not
     */
    public final BooleanProperty comboBoxEditableProperty() {
        return comboBoxEditable;
    }

    /**
     * Configures the ComboBox to be editable (to allow user input outside of the
     * options provide in the dropdown list).
     * @param value the editable value for this ComboBox
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
     * Returns the items to be displayed in the ChoiceBox when it is showing.
     * @return the items to be displayed in the ChoiceBox when it is showing
     */
    public ObservableList<T> getItems() {
        return items;
    }

    /** {@inheritDoc} */
    @Override public void startEdit() {
        if (! isEditable() || ! getListView().isEditable()) {
            return;
        }

        if (comboBox == null) {
            comboBox = createComboBox(this, items, converterProperty());
            comboBox.editableProperty().bind(comboBoxEditableProperty());
        }

        comboBox.getSelectionModel().select(getItem());

        super.startEdit();

        if (isEditing()) {
            setText(null);
            setGraphic(comboBox);
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
        CellUtils.updateItem(this, getConverter(), null, null, comboBox);
    }
}
