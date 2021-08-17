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

import javafx.scene.control.CheckBoxTreeItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing a {@link TreeCell} implementation that draws a
 * {@link CheckBox} node inside the cell, along with support for common
 * interactions (discussed in more depth shortly).
 *
 * <p>To make creating TreeViews with CheckBoxes easier, a convenience class
 * called {@link CheckBoxTreeItem} is provided. It is <b>highly</b> recommended
 * that developers use this class, rather than the regular {@link TreeItem}
 * class, when constructing their TreeView tree structures. Refer to the
 * CheckBoxTreeItem API documentation for an example on how these two classes
 * can be combined.
 *
 * <p>When used in a TreeView, the CheckBoxCell is rendered with a CheckBox to
 * the right of the 'disclosure node' (i.e. the arrow). The item stored in
 * {@link CheckBoxTreeItem#getValue()} will then have the StringConverter called
 * on it, and this text will take all remaining horizontal space. Additionally,
 * by using {@link CheckBoxTreeItem}, the TreeView will automatically handle
 * situations such as:
 *
 * <ul>
 *   <li>Clicking on the {@link CheckBox} beside an item that has children will
 *      result in all children also becoming selected/unselected.
 *   <li>Clicking on the {@link CheckBox} beside an item that has a parent will
 *      possibly toggle the state of the parent. For example, if you select a
 *      single child, the parent will become indeterminate (indicating partial
 *      selection of children). If you proceed to select all children, the
 *      parent will then show that it too is selected. This is recursive, with
 *      all parent nodes updating as expected.
 * </ul>
 *
 * If it is decided that using {@link CheckBoxTreeItem} is not desirable,
 * then it is necessary to call one of the constructors where a {@link Callback}
 * is provided that can return an {@code ObservableValue<Boolean>}
 * given a {@link TreeItem} instance. This {@code ObservableValue<Boolean>}
 * should represent the boolean state of the given {@link TreeItem}.
 *
 * <p>Note that the CheckBoxTreeCell renders the CheckBox 'live', meaning that
 * the CheckBox is always interactive and can be directly toggled by the user.
 * This means that it is not necessary that the cell enter its
 * {@link #editingProperty() editing state} (usually by the user double-clicking
 * on the cell). A side-effect of this is that the usual editing callbacks
 * (such as {@link javafx.scene.control.TreeView#onEditCommitProperty() on edit commit})
 * will <strong>not</strong> be called. If you want to be notified of changes,
 * it is recommended to directly observe the boolean properties that are
 * manipulated by the CheckBox.</p>
 *
 * @param <T> The type of the elements contained within the TreeView TreeItem
 *      instances.
 * @since JavaFX 2.2
 */
public class CheckBoxTreeCell<T> extends DefaultTreeCell<T> {

    /* *************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a cell factory for use in a TreeView control, although there is a
     * major assumption when used in a TreeView: this cell factory assumes that
     * the TreeView root, and <b>all</b> children are instances of
     * {@link CheckBoxTreeItem}, rather than the default {@link TreeItem} class
     * that is used normally.
     *
     * <p>When used in a TreeView, the CheckBoxCell is rendered with a CheckBox
     * to the right of the 'disclosure node' (i.e. the arrow). The item stored
     * in {@link CheckBoxTreeItem#getValue()} will then have the StringConverter
     * called on it, and this text will take all remaining horizontal space.
     * Additionally, by using {@link CheckBoxTreeItem}, the TreeView will
     * automatically handle situations such as:
     *
     * <ul>
     *   <li>Clicking on the {@link CheckBox} beside an item that has children
     *      will result in all children also becoming selected/unselected.</li>
     *   <li>Clicking on the {@link CheckBox} beside an item that has a parent
     *      will possibly toggle the state of the parent. For example, if you
     *      select a single child, the parent will become indeterminate (indicating
     *      partial selection of children). If you proceed to select all
     *      children, the parent will then show that it too is selected. This is
     *      recursive, with all parent nodes updating as expected.</li>
     * </ul>
     *
     * <p>Unfortunately, due to limitations in Java, it is necessary to provide
     * an explicit cast when using this method. For example:
     *
     * <pre>
     * {@code
     * final TreeView<String> treeView = new TreeView<String>();
     * treeView.setCellFactory(CheckBoxCell.<String>forTreeView());}</pre>
     *
     * @param <T> The type of the elements contained within the
     *      {@link CheckBoxTreeItem} instances.
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView root, and
     *      all of its children (recursively).
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView() {
        Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty =
                item -> {
                    if (item instanceof CheckBoxTreeItem<?>) {
                        return ((CheckBoxTreeItem<?>)item).selectedProperty();
                    }
                    return null;
                };
        return forTreeView(getSelectedProperty,
                           CellUtils.<T>defaultTreeItemStringConverter());
    }

    /**
     * Creates a cell factory for use in a TreeView control. Unlike
     * {@link #forTreeView()}, this method does not assume that all TreeItem
     * instances in the TreeView are {@link CheckBoxTreeItem} instances.
     *
     * <p>When used in a TreeView, the CheckBoxCell is rendered with a CheckBox
     * to the right of the 'disclosure node' (i.e. the arrow). The item stored
     * in {@link CheckBoxTreeItem#getValue()} will then have the StringConverter
     * called on it, and this text will take all remaining horizontal space.
     *
     * <p>Unlike {@link #forTreeView()}, this cell factory does not handle
     * updating the state of parent or children TreeItems - it simply toggles
     * the {@code ObservableValue<Boolean>} that is provided, and no more. Of
     * course, this functionality can then be implemented externally by adding
     * observers to the {@code ObservableValue<Boolean>}, and toggling the state
     * of other properties as necessary.
     *
     * @param <T> The type of the elements contained within the {@link TreeItem}
     *      instances.
     * @param getSelectedProperty A {@link Callback} that, given an object of
     *      type {@literal TreeItem<T>}, will return an {@code ObservableValue<Boolean>}
     *      that represents whether the given item is selected or not. This
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally
     *      (meaning that the CheckBox in the cell will set/unset this property
     *      based on user interactions, and the CheckBox will reflect the state
     *      of the {@code ObservableValue<Boolean>}, if it changes externally).
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView root, and
     *      all of its children (recursively).
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final Callback<TreeItem<T>,
            ObservableValue<Boolean>> getSelectedProperty) {
        return forTreeView(getSelectedProperty, CellUtils.<T>defaultTreeItemStringConverter());
    }

    /**
     * Creates a cell factory for use in a TreeView control. Unlike
     * {@link #forTreeView()}, this method does not assume that all TreeItem
     * instances in the TreeView are {@link CheckBoxTreeItem}.
     *
     * <p>When used in a TreeView, the CheckBoxCell is rendered with a CheckBox
     * to the right of the 'disclosure node' (i.e. the arrow). The item stored
     * in {@link TreeItem#getValue()} will then have the the StringConverter
     * called on it, and this text will take all remaining horizontal space.
     *
     * <p>Unlike {@link #forTreeView()}, this cell factory does not handle
     * updating the state of parent or children TreeItems - it simply toggles
     * the {@code ObservableValue<Boolean>} that is provided, and no more. Of
     * course, this functionality can then be implemented externally by adding
     * observers to the {@code ObservableValue<Boolean>}, and toggling the state
     * of other properties as necessary.
     *
     * @param <T> The type of the elements contained within the {@link TreeItem}
     *      instances.
     * @param getSelectedProperty A Callback that, given an object of
     *      type {@literal TreeItem<T>}, will return an {@code ObservableValue<Boolean>}
     *      that represents whether the given item is selected or not. This
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally
     *      (meaning that the CheckBox in the cell will set/unset this property
     *      based on user interactions, and the CheckBox will reflect the state of
     *      the {@code ObservableValue<Boolean>}, if it changes externally).
     * @param converter A StringConverter that, give an object of type
     *      {@literal TreeItem<T>}, will return a String that can be used to represent the
     *      object visually. The default implementation in {@link #forTreeView(Callback)}
     *      is to simply call .toString() on all non-null items (and to just
     *      return an empty string in cases where the given item is null).
     * @return A {@link Callback} that will return a TreeCell that is able to
     *      work on the type of element contained within the TreeView root, and
     *      all of its children (recursively).
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty,
            final StringConverter<TreeItem<T>> converter) {
        return tree -> new CheckBoxTreeCell<T>(getSelectedProperty, converter);
    }




    /* *************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/
    private final CheckBox checkBox;

    private ObservableValue<Boolean> booleanProperty;

    private BooleanProperty indeterminateProperty;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default {@link CheckBoxTreeCell} that assumes the TreeView is
     * constructed with {@link CheckBoxTreeItem} instances, rather than the
     * default {@link TreeItem}.
     * By using {@link CheckBoxTreeItem}, it will internally manage the selected
     * and indeterminate state of each item in the tree.
     */
    public CheckBoxTreeCell() {
        // getSelectedProperty as anonymous inner class to deal with situation
        // where the user is using CheckBoxTreeItem instances in their tree
        this(item -> {
            if (item instanceof CheckBoxTreeItem<?>) {
                return ((CheckBoxTreeItem<?>)item).selectedProperty();
            }
            return null;
        });
    }

    /**
     * Creates a {@link CheckBoxTreeCell} for use in a TreeView control via a
     * cell factory. Unlike {@link CheckBoxTreeCell#CheckBoxTreeCell()}, this
     * method does not assume that all TreeItem instances in the TreeView are
     * {@link CheckBoxTreeItem}.
     *
     * <p>To call this method, it is necessary to provide a
     * {@link Callback} that, given an object of type {@literal TreeItem<T>}, will return
     * an {@code ObservableValue<Boolean>} that represents whether the given
     * item is selected or not. This {@code ObservableValue<Boolean>} will be
     * bound bidirectionally (meaning that the CheckBox in the cell will
     * set/unset this property based on user interactions, and the CheckBox will
     * reflect the state of the {@code ObservableValue<Boolean>}, if it changes
     * externally).
     *
     * <p>If the items are not {@link CheckBoxTreeItem} instances, it becomes
     * the developers responsibility to handle updating the state of parent and
     * children TreeItems. This means that, given a TreeItem, this class will
     * simply toggles the {@code ObservableValue<Boolean>} that is provided, and
     * no more. Of course, this functionality can then be implemented externally
     * by adding observers to the {@code ObservableValue<Boolean>}, and toggling
     * the state of other properties as necessary.
     *
     * @param getSelectedProperty A {@link Callback} that will return an
     *      {@code ObservableValue<Boolean>} that represents whether the given
     *      item is selected or not.
     */
    public CheckBoxTreeCell(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty) {
        this(getSelectedProperty, CellUtils.<T>defaultTreeItemStringConverter(), null);
    }

    /**
     * Creates a {@link CheckBoxTreeCell} for use in a TreeView control via a
     * cell factory. Unlike {@link CheckBoxTreeCell#CheckBoxTreeCell()}, this
     * method does not assume that all TreeItem instances in the TreeView are
     * {@link CheckBoxTreeItem}.
     *
     * <p>To call this method, it is necessary to provide a {@link Callback}
     * that, given an object of type {@literal TreeItem<T>}, will return an
     * {@code ObservableValue<Boolean>} that represents whether the given item
     * is selected or not. This {@code ObservableValue<Boolean>} will be bound
     * bidirectionally (meaning that the CheckBox in the cell will set/unset
     * this property based on user interactions, and the CheckBox will reflect
     * the state of the {@code ObservableValue<Boolean>}, if it changes
     * externally).
     *
     * <p>If the items are not {@link CheckBoxTreeItem} instances, it becomes
     * the developers responsibility to handle updating the state of parent and
     * children TreeItems. This means that, given a TreeItem, this class will
     * simply toggles the {@code ObservableValue<Boolean>} that is provided, and
     * no more. Of course, this functionality can then be implemented externally
     * by adding observers to the {@code ObservableValue<Boolean>}, and toggling
     * the state of other properties as necessary.
     *
     * @param getSelectedProperty A {@link Callback} that will return an
     *      {@code ObservableValue<Boolean>} that represents whether the given
     *      item is selected or not.
     * @param converter {@literal A StringConverter that, give an object of type
     * TreeItem<T>, will return a String that can be used to represent the
     * object visually.}
     */
    public CheckBoxTreeCell(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty,
            final StringConverter<TreeItem<T>> converter) {
        this(getSelectedProperty, converter, null);
    }

    private CheckBoxTreeCell(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty,
            final StringConverter<TreeItem<T>> converter,
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getIndeterminateProperty) {
        this.getStyleClass().add("check-box-tree-cell");
        setSelectedStateCallback(getSelectedProperty);
        setConverter(converter);

        this.checkBox = new CheckBox();
        this.checkBox.setAllowIndeterminate(false);

        // by default the graphic is null until the cell stops being empty
        setGraphic(null);
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- converter
    private ObjectProperty<StringConverter<TreeItem<T>>> converter =
            new SimpleObjectProperty<StringConverter<TreeItem<T>>>(this, "converter");

    /**
     * The {@link StringConverter} property.
     * @return the {@link StringConverter} property
     */
    public final ObjectProperty<StringConverter<TreeItem<T>>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link StringConverter} to be used in this cell.
     * @param value the {@link StringConverter} to be used in this cell
     */
    public final void setConverter(StringConverter<TreeItem<T>> value) {
        converterProperty().set(value);
    }

    /**
     * Returns the {@link StringConverter} used in this cell.
     * @return the {@link StringConverter} used in this cell
     */
    public final StringConverter<TreeItem<T>> getConverter() {
        return converterProperty().get();
    }



    // --- selected state callback property
    private ObjectProperty<Callback<TreeItem<T>, ObservableValue<Boolean>>>
            selectedStateCallback =
            new SimpleObjectProperty<Callback<TreeItem<T>, ObservableValue<Boolean>>>(
            this, "selectedStateCallback");

    /**
     * Property representing the {@link Callback} that is bound to by the
     * CheckBox shown on screen.
     * @return the property representing the {@link Callback} that is bound to
     * by the CheckBox shown on screen
     */
    public final ObjectProperty<Callback<TreeItem<T>, ObservableValue<Boolean>>> selectedStateCallbackProperty() {
        return selectedStateCallback;
    }

    /**
     * Sets the {@link Callback} that is bound to by the CheckBox shown on screen.
     * @param value the {@link Callback} that is bound to by the CheckBox shown on screen
     */
    public final void setSelectedStateCallback(Callback<TreeItem<T>, ObservableValue<Boolean>> value) {
        selectedStateCallbackProperty().set(value);
    }

    /**
     * Returns the {@link Callback} that is bound to by the CheckBox shown on screen.
     * @return the {@link Callback} that is bound to by the CheckBox shown on screen
     */
    public final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedStateCallback() {
        return selectedStateCallbackProperty().get();
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            StringConverter<TreeItem<T>> c = getConverter();

            TreeItem<T> treeItem = getTreeItem();

            // update the node content
            setText(c != null ? c.toString(treeItem) : (treeItem == null ? "" : treeItem.toString()));
            checkBox.setGraphic(treeItem == null ? null : treeItem.getGraphic());
            setGraphic(checkBox);

            // uninstall bindings
            if (booleanProperty != null) {
                checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
            }
            if (indeterminateProperty != null) {
                checkBox.indeterminateProperty().unbindBidirectional(indeterminateProperty);
            }

            // install new bindings.
            // We special case things when the TreeItem is a CheckBoxTreeItem
            if (treeItem instanceof CheckBoxTreeItem) {
                CheckBoxTreeItem<T> cbti = (CheckBoxTreeItem<T>) treeItem;
                booleanProperty = cbti.selectedProperty();
                checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);

                indeterminateProperty = cbti.indeterminateProperty();
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);
            } else {
                Callback<TreeItem<T>, ObservableValue<Boolean>> callback = getSelectedStateCallback();
                if (callback == null) {
                    throw new NullPointerException(
                            "The CheckBoxTreeCell selectedStateCallbackProperty can not be null");
                }

                booleanProperty = callback.call(treeItem);
                if (booleanProperty != null) {
                    checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
                }
            }
        }
    }

    @Override void updateDisplay(T item, boolean empty) {
        // no-op
        // This was done to resolve RT-33603, but will impact the ability for
        // TreeItem.graphic to change dynamically.
    }
}
