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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
 * A class containing cell factories that use {@link ComboBox}, although the 
 * precise way in which these cell factories work differs between controls. Be 
 * sure to read the API documentation for each static method, as well as the 
 * class documentation for {@link ComboBoxListCell}, {@link ComboBoxTableCell} 
 * and {@link ComboBoxTreeCell} before using it!
 * 
 * @see ComboBox
 * @see ComboBoxListCell
 * @see ComboBoxTreeCell
 * @see ComboBoxTableCell
 */
@NoBuilder
public final class ComboBoxCellFactory {
    
    private ComboBoxCellFactory() { }
    
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
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> list) {
                return new ComboBoxListCell<T>(converter, items);
            }
        };
    }
    
    /**
     * Creates a ComboBox cell factory for use in {@link TreeView} controls. By 
     * default, the ComboBoxCell is rendered as a {@link Label} when not being 
     * edited, and as a ComboBox when in editing mode. The ComboBox will, by 
     * default, stretch to fill the entire tree cell.
     * 
     * @param <T> The type of the elements contained within the TreeView.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same 
     *      type as the TreeView<T>, such that upon selection, they replace the 
     *      existing value in the TreeItem {@link TreeItem#valueProperty() value} 
     *      property.
     * @return A {@link Callback} that will return a TreeCell that is able to 
     *      work on the type of element contained within the TreeView.
     */    
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
     *      type as the TreeView<T>, such that upon selection, they replace the 
     *      existing value in the TreeItem {@link TreeItem#valueProperty() value} 
     *      property.
     * @return A {@link Callback} that will return a TreeCell that is able to 
     *      work on the type of element contained within the TreeView.
     */  
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
        return new Callback<TreeView<T>, TreeCell<T>>() {
          @Override public TreeCell<T> call(TreeView<T> list) {
              return new ComboBoxTreeCell<T>(converter, items);
          }
      };
    }
    
    /**
     * Creates a ComboBox cell factory for use in {@link TableColumn} controls. 
     * By default, the ComboBoxCell is rendered as a {@link Label} when not 
     * being edited, and as a ComboBox when in editing mode. The ComboBox will, 
     * by default, stretch to fill the entire list cell.
     * 
     * @param <T> The type of the elements contained within the TableColumn.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same 
     *      type as the TableColumn. Note that it is up to the developer to set 
     *      {@link EventHandler event handlers} to listen to edit events in the 
     *      TableColumn, and react accordingly. Methods of interest include 
     *      {@link TableColumn#setOnEditStart(EventHandler) setOnEditStart},
     *      {@link TableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit}, 
     *      and {@link TableColumn#setOnEditCancel(EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TableCell that is able to 
     *      work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final T... items) {
        return forTableColumn(null, items);
    }
    
    /**
     * Creates a ComboBox cell factory for use in {@link TableColumn} controls. 
     * By default, the ComboBoxCell is rendered as a {@link Label} when not 
     * being edited, and as a ComboBox when in editing mode. The ComboBox will, 
     * by default, stretch to fill the entire list cell.
     * 
     * @param <T> The type of the elements contained within the TableColumn.
     * @param converter A {@link StringConverter} to convert the given item (of 
     *      type T) to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same 
     *      type as the TableColumn. Note that it is up to the developer to set 
     *      {@link EventHandler event handlers} to listen to edit events in the 
     *      TableColumn, and react accordingly. Methods of interest include 
     *      {@link TableColumn#setOnEditStart(EventHandler) setOnEditStart},
     *      {@link TableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit}, 
     *      and {@link TableColumn#setOnEditCancel(EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TableCell that is able to 
     *      work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final StringConverter<T> converter, 
            final T... items) {
        return forTableColumn(converter, FXCollections.observableArrayList(items));
    }
    
    /**
     * Creates a ComboBox cell factory for use in {@link TableColumn} controls. 
     * By default, the ComboBoxCell is rendered as a {@link Label} when not 
     * being edited, and as a ComboBox when in editing mode. The ComboBox will, 
     * by default, stretch to fill the entire list cell.
     * 
     * @param <T> The type of the elements contained within the TableColumn.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same 
     *      type as the TableColumn. Note that it is up to the developer to set 
     *      {@link EventHandler event handlers} to listen to edit events in the 
     *      TableColumn, and react accordingly. Methods of interest include 
     *      {@link TableColumn#setOnEditStart(EventHandler) setOnEditStart},
     *      {@link TableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit}, 
     *      and {@link TableColumn#setOnEditCancel(EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TableCell that is able to 
     *      work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final ObservableList<T> items) {
        return forTableColumn(null, items);
    }
    
    /**
     * Creates a ComboBox cell factory for use in {@link TableColumn} controls. 
     * By default, the ComboBoxCell is rendered as a {@link Label} when not 
     * being edited, and as a ComboBox when in editing mode. The ComboBox will, 
     * by default, stretch to fill the entire list cell.
     * 
     * @param <T> The type of the elements contained within the TableColumn.
     * @param converter A {@link StringConverter} to convert the given item (of 
     *      type T) to a String for displaying to the user.
     * @param items Zero or more items that will be shown to the user when the
     *      {@link ComboBox} menu is showing. These items must be of the same 
     *      type as the TableColumn. Note that it is up to the developer to set 
     *      {@link EventHandler event handlers} to listen to edit events in the 
     *      TableColumn, and react accordingly. Methods of interest include 
     *      {@link TableColumn#setOnEditStart(EventHandler) setOnEditStart},
     *      {@link TableColumn#setOnEditCommit(javafx.event.EventHandler) setOnEditCommit}, 
     *      and {@link TableColumn#setOnEditCancel(EventHandler) setOnEditCancel}.
     * @return A {@link Callback} that will return a TableCell that is able to 
     *      work on the type of element contained within the TableColumn.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final StringConverter<T> converter, 
            final ObservableList<T> items) {
        return new Callback<TableColumn<S,T>, TableCell<S,T>>() {
            @Override public TableCell<S,T> call(TableColumn<S,T> list) {
                return new ComboBoxTableCell<S,T>(converter, items);
            }
        };
    }
}