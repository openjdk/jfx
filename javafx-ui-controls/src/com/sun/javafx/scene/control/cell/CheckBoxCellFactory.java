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

import com.sun.javafx.beans.annotations.NoBuilder;
import com.sun.javafx.scene.control.CheckBoxTreeItem;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing cell factories that use {@link CheckBox CheckBox} instances, 
 * although the precise way in which these cell factories work differs between 
 * controls. Be sure to read the API documentation for each static method, as 
 * well as the class documentation for {@link CheckBoxListCell}, 
 * {@link CheckBoxTableCell} and {@link CheckBoxTreeCell} before using it.
 * 
 * @see CheckBox
 * @see CheckBoxListCell
 * @see CheckBoxTreeCell
 * @see CheckBoxTableCell
 */
@NoBuilder
public final class CheckBoxCellFactory {
    
    private CheckBoxCellFactory() { }
    
    /**
     * Creates a cell factory for use in ListView controls. When used in a 
     * ListView, the {@link CheckBoxListCell} is rendered with a CheckBox on the 
     * left-hand side of the ListView, with the text related to the list item 
     * taking up all remaining horizontal space. 
     * 
     * @param <T> The type of the elements contained within the ListView.
     * @param getSelectedProperty A {@link Callback} that, given an object of 
     *      type T (which is a value taken out of the 
     *      {@code ListView<T>.items} list), 
     *      will return an {@code ObservableValue<Boolean>} that represents 
     *      whether the given item is selected or not. This ObservableValue will 
     *      be bound bidirectionally (meaning that the CheckBox in the cell will 
     *      set/unset this property based on user interactions, and the CheckBox 
     *      will reflect the state of the ObservableValue, if it changes 
     *      externally).
     * @return A {@link Callback} that will return a ListCell that is able to 
     *      work on the type of element contained within the ListView items list.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final Callback<T, ObservableValue<Boolean>> getSelectedProperty) {
        return forListView(getSelectedProperty, null);
    }
    
    /**
     * Creates a cell factory for use in ListView controls. When used in a 
     * ListView, the {@link CheckBoxListCell} is rendered with a CheckBox on the
     * left-hand side of the ListView, with the text related to the list item 
     * taking up all remaining horizontal space. 
     * 
     * @param <T> The type of the elements contained within the ListView.
     * @param getSelectedProperty A {@link Callback} that, given an object 
     *      of type T (which is a value taken out of the 
     *      {@code ListView<T>.items} list), 
     *      will return an {@code ObservableValue<Boolean>} that represents 
     *      whether the given item is selected or not. This ObservableValue will 
     *      be bound bidirectionally (meaning that the CheckBox in the cell will 
     *      set/unset this property based on user interactions, and the CheckBox 
     *      will reflect the state of the ObservableValue, if it changes 
     *      externally).
     * @param converter A StringConverter that, give an object of type T, will 
     *      return a String that can be used to represent the object visually. 
     * @return A {@link Callback} that will return a ListCell that is able to 
     *      work on the type of element contained within the ListView.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<T> converter) {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> list) {
                return new CheckBoxListCell<T>(getSelectedProperty, converter);
            }
        };
    }
    
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
                new Callback<TreeItem<T>, ObservableValue<Boolean>>() {
            @Override public ObservableValue<Boolean> call(TreeItem<T> item) {
                if (item instanceof CheckBoxTreeItem<?>) {
                    return ((CheckBoxTreeItem<?>)item).selectedProperty();
                }
                return null;
            }
        };
        Callback<TreeItem<T>, ObservableValue<Boolean>> getIndeterminateProperty = 
                new Callback<TreeItem<T>, ObservableValue<Boolean>>() {
            @Override public ObservableValue<Boolean> call(TreeItem<T> item) {
                if (item instanceof CheckBoxTreeItem<?>) {
                    return ((CheckBoxTreeItem<?>)item).indeterminateProperty();
                }
                return null;
            }
        };
        return forTreeView(getSelectedProperty, 
                           CellUtils.<T>defaultTreeItemStringConverter(), 
                           getIndeterminateProperty);
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
     *      type TreeItem<T>, will return an {@code ObservableValue<Boolean>} 
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
     *      type TreeItem<T>, will return an {@code ObservableValue<Boolean>} 
     *      that represents whether the given item is selected or not. This 
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally 
     *      (meaning that the CheckBox in the cell will set/unset this property 
     *      based on user interactions, and the CheckBox will reflect the state of 
     *      the {@code ObservableValue<Boolean>}, if it changes externally).
     * @param converter A StringConverter that, give an object of type TreeItem<T>, 
     *      will return a String that can be used to represent the object
     *      visually. The default implementation in {@link #forTreeView(Callback)} 
     *      is to simply call .toString() on all non-null items (and to just 
     *      return an empty string in cases where the given item is null).      
     * @return A {@link Callback} that will return a TreeCell that is able to 
     *      work on the type of element contained within the TreeView root, and 
     *      all of its children (recursively).
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<TreeItem<T>> converter) {
        return forTreeView(getSelectedProperty, converter, null);
    }
    
    /**
     * Creates a cell factory for use in a TreeView control. Unlike 
     * {@link #forTreeView()}, this method does not assume that all TreeItem 
     * instances in the TreeView are {@link CheckBoxTreeItem}.
     * 
     * <p>To call this method, it is necessary to provide three 
     * {@link Callback Callbacks}:
     * 
     * <ol>
     *   <li><b>getSelectedProperty</b>: A Callback that, given an object of type 
     *      TreeItem<T>, will return an {@code ObservableValue<Boolean>} that 
     *      represents whether the given item is selected or not. This 
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally 
     *      (meaning that the CheckBox in the cell will set/unset this property 
     *      based on user interactions, and the CheckBox will reflect the state of 
     *      the {@code ObservableValue<Boolean>}, if it changes externally).</li>
     *   <li><b>converter</b>: A StringConverter that, give an object of type TreeItem<T>, 
     *      will return a String that can be used to represent the object
     *      visually. The default implementation in {@link #forTreeView(Callback)} 
     *      is to simply call .toString() on all non-null items (and to just 
     *      return an empty string in cases where the given item is null).</li>
     *   <li><b>getIndeterminateProperty</b>: A Callback that, given an object 
     *      of type TreeItem<T>, will return an {@code ObservableValue<Boolean>} 
     *      that represents whether the given item is in an indeterminate state 
     *      or not (refer to the {@link CheckBox#indeterminateProperty()} for
     *      more information on what an indeterminate state is. This 
     *      {@code ObservableValue<Boolean>} will be bound bidirectionally 
     *      (meaning that the CheckBox in the cell will set/unset this property 
     *      based on user interactions, and the CheckBox will reflect the state of 
     *      the {@code ObservableValue<Boolean>}, if it changes externally).</li>
     * </ol>
     * 
     * <p>When used in a TreeView, the CheckBoxCell is rendered with a CheckBox 
     * to the right of the 'disclosure node' (i.e. the arrow). The item stored 
     * in {@link TreeItem#getValue()} will then have the toString Callback 
     * called on it, and this text will take all remaining horizontal space.
     * 
     * <p>Unlike {@link #forTreeView()}, this cell factory does not handle 
     * updating the state of parent or children TreeItems - it simply toggles 
     * the BooleanProperties that are provided via the 
     * <code>getSelectedProperty</code> and <code>getIndeterminateProperty</code>
     * callbacks, and no more. Of course, this functionality can then be 
     * implemented externally by adding observers to the BooleanProperties, and 
     * toggling the state of other properties as necessary.
     * 
     * @param <T> The type of the elements contained within the {@link TreeItem} 
     *      instances.
     * @param getSelectedProperty A {@link Callback} that will return an 
     *      {@code ObservableValue<Boolean>} that represents whether the given 
     *      item is selected or not.
     * @param converter A StringConverter that, give an object of type TreeItem<T>, will 
     *      return a String that can be used to represent the object visually.
     * @param getIndeterminateProperty A {@link Callback} that will return an 
     *      {@code ObservableValue<Boolean>} that represents whether the given 
     *      item is indeterminate or not.
     * @return A {@link Callback} that will return a TreeCell that is able to 
     *      work on the type of element contained within the TreeView root, and 
     *      all of its children (recursively).
     */
    // TODO this is not currently public API, as we don't even use the indeterminate property!
    private static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty, 
            final StringConverter<TreeItem<T>> converter, 
            final Callback<TreeItem<T>, ObservableValue<Boolean>> getIndeterminateProperty) {
        return new Callback<TreeView<T>, TreeCell<T>>() {
            @Override public TreeCell<T> call(TreeView<T> list) {
                return new CheckBoxTreeCell<T>(getSelectedProperty, converter/*, getIndeterminateProperty*/);
            }
        };
    }
    
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
     * if it changes externally).</li>
     * 
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
     *      consider using {@link #forTableColumn(Callback, Callback)}, which 
     *      allows for you to provide a callback that specifies the label for a 
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
        return new Callback<TableColumn<S,T>, TableCell<S,T>>() {
            @Override public TableCell<S,T> call(TableColumn<S,T> list) {
                return new CheckBoxTableCell<S,T>(getSelectedProperty, converter);
            }
        };
    }
}
