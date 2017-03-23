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

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link TableCell} implementation that draws a
 * {@link CheckBox} node inside the cell, optionally with a label to indicate
 * what the checkbox represents.
 *
 * <p>By default, the CheckBoxTableCell is rendered with a CheckBox centred in
 * the TableColumn. If a label is required, it is necessary to provide a
 * non-null StringConverter instance to the
 * {@link #CheckBoxTableCell(Callback, StringConverter)} constructor.
 *
 * <p>To construct an instance of this class, it is necessary to provide a
 * {@link Callback} that, given an object of type T, will return an
 * {@code ObservableProperty<Boolean>} that represents whether the given item is
 * selected or not. This ObservableValue will be bound bidirectionally (meaning
 * that the CheckBox in the cell will set/unset this property based on user
 * interactions, and the CheckBox will reflect the state of the ObservableValue,
 * if it changes externally).
 *
 * <p>Note that the CheckBoxTableCell renders the CheckBox 'live', meaning that
 * the CheckBox is always interactive and can be directly toggled by the user.
 * This means that it is not necessary that the cell enter its
 * {@link #editingProperty() editing state} (usually by the user double-clicking
 * on the cell). A side-effect of this is that the usual editing callbacks
 * (such as {@link javafx.scene.control.TableColumn#onEditCommitProperty() on edit commit})
 * will <strong>not</strong> be called. If you want to be notified of changes,
 * it is recommended to directly observe the boolean properties that are
 * manipulated by the CheckBox.</p>
 *
 * @param <T> The type of the elements contained within the TableColumn.
 * @since JavaFX 2.2
 */
public class CheckBoxTableCell<S,T> extends TableCell<S,T> {

    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a cell factory for use in a {@link TableColumn} cell factory.
     * This method requires that the TableColumn be of type {@link Boolean}.
     *
     * <p>When used in a TableColumn, the CheckBoxCell is rendered with a
     * CheckBox centered in the column.
     *
     * <p>The {@code ObservableValue<Boolean>} contained within each cell in the
     * column will be bound bidirectionally. This means that the  CheckBox in
     * the cell will set/unset this property based on user interactions, and the
     * CheckBox will reflect the state of the {@code ObservableValue<Boolean>},
     * if it changes externally).
     *
     * @param <S> The type of the TableView generic type
     * @param column The TableColumn of type Boolean
     * @return A {@link Callback} that will return a {@link TableCell} that is
     *      able to work on the type of element contained within the TableColumn.
     */
    public static <S> Callback<TableColumn<S,Boolean>, TableCell<S,Boolean>> forTableColumn(
            final TableColumn<S, Boolean> column) {
        return forTableColumn(null, null);
    }

    /**
     * Creates a cell factory for use in a {@link TableColumn} cell factory.
     * This method requires that the TableColumn be of type
     * {@code ObservableValue<Boolean>}.
     *
     * <p>When used in a TableColumn, the CheckBoxCell is rendered with a
     * CheckBox centered in the column.
     *
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the elements contained within the {@link TableColumn}
     *      instance.
     * @param getSelectedProperty A Callback that, given an object of
     *      type {@code TableColumn<S,T>}, will return an
     *      {@code ObservableValue<Boolean>}
     *      that represents whether the given item is selected or not. This
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally
     *      (meaning that the CheckBox in the cell will set/unset this property
     *      based on user interactions, and the CheckBox will reflect the state of
     *      the {@code ObservableValue<Boolean>}, if it changes externally).
     * @return A {@link Callback} that will return a {@link TableCell} that is
     *      able to work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty) {
        return forTableColumn(getSelectedProperty, null);
    }

    /**
     * Creates a cell factory for use in a {@link TableColumn} cell factory.
     * This method requires that the TableColumn be of type
     * {@code ObservableValue<Boolean>}.
     *
     * <p>When used in a TableColumn, the CheckBoxCell is rendered with a
     * CheckBox centered in the column.
     *
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the elements contained within the {@link TableColumn}
     *      instance.
     * @param getSelectedProperty A Callback that, given an object of
     *      type {@code TableColumn<S,T>}, will return an
     *      {@code ObservableValue<Boolean>}
     *      that represents whether the given item is selected or not. This
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally
     *      (meaning that the CheckBox in the cell will set/unset this property
     *      based on user interactions, and the CheckBox will reflect the state of
     *      the {@code ObservableValue<Boolean>}, if it changes externally).
     * @param showLabel In some cases, it may be desirable to show a label in
     *      the TableCell beside the {@link CheckBox}. By default a label is not
     *      shown, but by setting this to true the item in the cell will also
     *      have toString() called on it. If this is not the desired behavior,
     *      consider using
     *      {@link #forTableColumn(javafx.util.Callback, javafx.util.StringConverter) },
     *      which allows for you to provide a callback that specifies the label for a
     *      given row item.
     * @return A {@link Callback} that will return a {@link TableCell} that is
     *      able to work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
            final boolean showLabel) {
        StringConverter<T> converter = ! showLabel ?
                null : CellUtils.<T>defaultStringConverter();
        return forTableColumn(getSelectedProperty, converter);
    }

    /**
     * Creates a cell factory for use in a {@link TableColumn} cell factory.
     * This method requires that the TableColumn be of type
     * {@code ObservableValue<Boolean>}.
     *
     * <p>When used in a TableColumn, the CheckBoxCell is rendered with a
     * CheckBox centered in the column.
     *
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the elements contained within the {@link TableColumn}
     *      instance.
     * @param getSelectedProperty A Callback that, given an object of type
     *      {@code TableColumn<S,T>}, will return an
     *      {@code ObservableValue<Boolean>} that represents whether the given
     *      item is selected or not. This {@code ObservableValue<Boolean>} will
     *      be bound bidirectionally (meaning that the CheckBox in the cell will
     *      set/unset this property based on user interactions, and the CheckBox
     *      will reflect the state of the {@code ObservableValue<Boolean>}, if
     *      it changes externally).
     * @param converter A StringConverter that, give an object of type T, will return a
     *      String that can be used to represent the object visually. The default
     *      implementation in {@link #forTableColumn(Callback, boolean)} (when
     *      showLabel is true) is to simply call .toString() on all non-null
     *      items (and to just return an empty string in cases where the given
     *      item is null).
     * @return A {@link Callback} that will return a {@link TableCell} that is
     *      able to work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
            final StringConverter<T> converter) {
        return list -> new CheckBoxTableCell<S,T>(getSelectedProperty, converter);
    }



    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/
    private final CheckBox checkBox;

    private boolean showLabel;

    private ObservableValue<Boolean> booleanProperty;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default CheckBoxTableCell.
     */
    public CheckBoxTableCell() {
        this(null, null);
    }

    /**
     * Creates a default CheckBoxTableCell with a custom {@link Callback} to
     * retrieve an ObservableValue for a given cell index.
     *
     * @param getSelectedProperty A {@link Callback} that will return an {@link
     *      ObservableValue} given an index from the TableColumn.
     */
    public CheckBoxTableCell(
            final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty) {
        this(getSelectedProperty, null);
    }

    /**
     * Creates a CheckBoxTableCell with a custom string converter.
     *
     * @param getSelectedProperty A {@link Callback} that will return a {@link
     *      ObservableValue} given an index from the TableColumn.
     * @param converter A StringConverter that, given an object of type T, will return a
     *      String that can be used to represent the object visually.
     */
    public CheckBoxTableCell(
            final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
            final StringConverter<T> converter) {
        // we let getSelectedProperty be null here, as we can always defer to the
        // TableColumn
        this.getStyleClass().add("check-box-table-cell");

        this.checkBox = new CheckBox();

        // by default the graphic is null until the cell stops being empty
        setGraphic(null);

        setSelectedStateCallback(getSelectedProperty);
        setConverter(converter);

//        // alignment is styleable through css. Calling setAlignment
//        // makes it look to css like the user set the value and css will not
//        // override. Initializing alignment by calling set on the
//        // CssMetaData ensures that css will be able to override the value.
//        final CssMetaData prop = CssMetaData.getCssMetaData(alignmentProperty());
//        prop.set(this, Pos.CENTER);


    }


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- converter
    private ObjectProperty<StringConverter<T>> converter =
            new SimpleObjectProperty<StringConverter<T>>(this, "converter") {
        protected void invalidated() {
            updateShowLabel();
        }
    };

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



    // --- selected state callback property
    private ObjectProperty<Callback<Integer, ObservableValue<Boolean>>>
            selectedStateCallback =
            new SimpleObjectProperty<Callback<Integer, ObservableValue<Boolean>>>(
            this, "selectedStateCallback");

    /**
     * Property representing the {@link Callback} that is bound to by the
     * CheckBox shown on screen.
     * @return the property representing the {@link Callback} that is bound to
     * by the CheckBox shown on screen
     */
    public final ObjectProperty<Callback<Integer, ObservableValue<Boolean>>> selectedStateCallbackProperty() {
        return selectedStateCallback;
    }

    /**
     * Sets the {@link Callback} that is bound to by the CheckBox shown on screen.
     * @param value the {@link Callback} that is bound to by the CheckBox shown
     * on screen
     */
    public final void setSelectedStateCallback(Callback<Integer, ObservableValue<Boolean>> value) {
        selectedStateCallbackProperty().set(value);
    }

    /**
     * Returns the {@link Callback} that is bound to by the CheckBox shown on screen.
     * @return the {@link Callback} that is bound to by the CheckBox shown on screen
     */
    public final Callback<Integer, ObservableValue<Boolean>> getSelectedStateCallback() {
        return selectedStateCallbackProperty().get();
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            StringConverter<T> c = getConverter();

            if (showLabel) {
                setText(c.toString(item));
            }
            setGraphic(checkBox);

            if (booleanProperty instanceof BooleanProperty) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
            }
            ObservableValue<?> obsValue = getSelectedProperty();
            if (obsValue instanceof BooleanProperty) {
                booleanProperty = (ObservableValue<Boolean>) obsValue;
                checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
            }

            checkBox.disableProperty().bind(Bindings.not(
                    getTableView().editableProperty().and(
                    getTableColumn().editableProperty()).and(
                    editableProperty())
                ));
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void updateShowLabel() {
        this.showLabel = converter != null;
        this.checkBox.setAlignment(showLabel ? Pos.CENTER_LEFT : Pos.CENTER);
    }

    private ObservableValue<?> getSelectedProperty() {
        return getSelectedStateCallback() != null ?
                getSelectedStateCallback().call(getIndex()) :
                getTableColumn().getCellObservableValue(getIndex());
    }
}
