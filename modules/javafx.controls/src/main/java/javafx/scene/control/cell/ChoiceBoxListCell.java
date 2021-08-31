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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link ListCell} implementation that draws a
 * {@link ChoiceBox} node inside the cell.
 *
 * <p>By default, the ChoiceBoxListCell is rendered as a {@link Label} when not
 * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
 * default, stretch to fill the entire list cell.
 *
 * <p>To create a ChoiceBoxListCell, it is necessary to provide zero or more
 * items that will be shown to the user when the {@link ChoiceBox} menu is
 * showing. These items must be of the same type as the ListView items sequence,
 * such that upon selection, they replace the existing value in the
 * {@link ListView#itemsProperty() items} list.
 *
 * @param <T> The type of the elements contained within the ListView.
 * @since JavaFX 2.2
 */
public class ChoiceBoxListCell<T> extends ListCell<T> {

    /* *************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a ChoiceBox cell factory for use in {@link ListView} controls.
     * By default, the ChoiceBoxCell is rendered as a {@link Label} when not
     * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox will,
     * by default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
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
     * Creates a ChoiceBox cell factory for use in {@link ListView} controls. By
     * default, the ChoiceBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
     * default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param converter A {@link StringConverter} to convert the given item (of type T)
     *      to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ChoiceBox} menu is showing. These items must be of the same
     *      type as the ListView items list, such that upon selection, they
     *      replace the existing value in the
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
     * Creates a ChoiceBox cell factory for use in {@link ListView} controls.
     * By default, the ChoiceBoxCell is rendered as a {@link Label} when not
     * being edited, and as a ChoiceBox when in editing mode. The ChoiceBox
     * will, by default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ChoiceBox} menu is showing.
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
     * Creates a ChoiceBox cell factory for use in {@link ListView} controls. By
     * default, the ChoiceBoxCell is rendered as a {@link Label} when not being
     * edited, and as a ChoiceBox when in editing mode. The ChoiceBox will, by
     * default, stretch to fill the entire list cell.
     *
     * @param <T> The type of the elements contained within the ListView.
     * @param converter A {@link StringConverter} to convert the given item (of type T)
     *      to a String for displaying to the user.
     * @param items An {@link ObservableList} containing zero or more items that
     *      will be shown to the user when the {@link ChoiceBox} menu is showing.
     *      These items must be of the same type as the ListView items sequence,
     *      such that upon selection, they replace the existing value in the
     *      {@link ListView#itemsProperty() items} list.
     * @return A {@link Callback} that will return a ListCell that is able to
     *      work on the type of element contained within the ListView.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final StringConverter<T> converter,
            final ObservableList<T> items) {
        return list -> new ChoiceBoxListCell<T>(converter, items);
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
     * Creates a default ChoiceBoxListCell with an empty items list.
     */
    public ChoiceBoxListCell() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default {@link ChoiceBoxListCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown.
     *
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    @SafeVarargs
    public ChoiceBoxListCell(T... items) {
        this(FXCollections.observableArrayList(items));
    }

    /**
     * Creates a {@link ChoiceBoxListCell} instance with the given items
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
    public ChoiceBoxListCell(StringConverter<T> converter, T... items) {
        this(converter, FXCollections.observableArrayList(items));
    }

    /**
     * Creates a default {@link ChoiceBoxListCell} instance with the given items
     * being used to populate the {@link ChoiceBox} when it is shown.
     *
     * @param items The items to show in the ChoiceBox popup menu when selected
     *      by the user.
     */
    public ChoiceBoxListCell(ObservableList<T> items) {
        this(null, items);
    }

    /**
     * Creates a {@link ChoiceBoxListCell} instance with the given items
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
    public ChoiceBoxListCell(StringConverter<T> converter, ObservableList<T> items) {
        this.getStyleClass().add("choice-box-list-cell");
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
        if (! isEditable() || ! getListView().isEditable()) {
            return;
        }

        if (choiceBox == null) {
            choiceBox = createChoiceBox(this, items, converterProperty());
        }

        choiceBox.getSelectionModel().select(getItem());

        super.startEdit();

        if (isEditing()) {
            setText(null);
            setGraphic(choiceBox);
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
        CellUtils.updateItem(this, getConverter(), null, null, choiceBox);
    }
}
