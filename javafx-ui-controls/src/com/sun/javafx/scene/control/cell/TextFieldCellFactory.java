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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * A class containing cell factories that use {@link TextField TextFields}. 
 * Be sure to read the API documentation for each static method before using it!
 * 
 * @see TextField
 * @see TextFieldListCell
 * @see TextFieldTreeCell
 * @see TextFieldTableCell
 */
@NoBuilder
public final class TextFieldCellFactory {
    
    private TextFieldCellFactory() { }
    
    /**
     * Provides a {@link TextField} that allows editing of the cell content when
     * the cell is double-clicked, or when {@link ListView#edit(int)} is called. 
     * This method will only work on {@link ListView} instances which are of 
     * type String.
     * 
     * @return A {@link Callback} that can be inserted into the 
     *      {@link ListView#cellFactoryProperty() cell factory property} of a 
     *      ListView, that enables textual editing of the content.
     */
    public static Callback<ListView<String>, ListCell<String>> forListView() {
        return forListView(new DefaultStringConverter());
    }
    
    /**
     * Provides a {@link TextField} that allows editing of the cell content when 
     * the cell is double-clicked, or when {@link ListView#edit(int)} is called. 
     * This method will work on any ListView instance, regardless of its generic 
     * type. However, to enable this, a {@link StringConverter} must be provided 
     * that will convert the given String (from what the user typed in) into an 
     * instance of type T. This item will then be passed along to the 
     * {@link ListView#onEditCommitProperty()} callback.
     * 
     * @param converter A {@link StringConverter} that can convert the given String 
     *      (from what the user typed in) into an instance of type T.
     * @return A {@link Callback} that can be inserted into the 
     *      {@link ListView#cellFactoryProperty() cell factory property} of a 
     *      ListView, that enables textual editing of the content.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(final StringConverter<T> converter) {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> list) {
                return new TextFieldListCell<T>(converter);
            }
        };
    }
    
    /**
     * Provides a {@link TextField} that allows editing of the cell content when 
     * the cell is double-clicked, or when {@link ListView#edit(int)} is called. 
     * This method will only work on {@link TreeView} instances which are of 
     * type String.
     * 
     * @return A {@link Callback} that can be inserted into the 
     *      {@link TreeView#cellFactoryProperty() cell factory property} of a 
     *      TreeView, that enables textual editing of the content.
     */
    public static Callback<TreeView<String>, TreeCell<String>> forTreeView() {
        return forTreeView(new DefaultStringConverter());
    }
    
    /**
     * Provides a {@link TextField} that allows editing of the cell content when 
     * the cell is double-clicked, or when 
     * {@link TreeView#edit(javafx.scene.control.TreeItem)} is called. This 
     * method will work on any {@link TreeView} instance, 
     * regardless of its generic type. However, to enable this, a 
     * {@link StringConverter} must be provided that will convert the given String 
     * (from what the user typed in) into an instance of type T. This item will 
     * then be passed along to the {@link TreeView#onEditCommitProperty()} 
     * callback.
     * 
     * @param converter A {@link StringConverter} that can convert the given String 
     *      (from what the user typed in) into an instance of type T.
     * @return A {@link Callback} that can be inserted into the 
     *      {@link TreeView#cellFactoryProperty() cell factory property} of a 
     *      TreeView, that enables textual editing of the content.
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final StringConverter<T> converter) {
        return new Callback<TreeView<T>, TreeCell<T>>() {
            @Override public TreeCell<T> call(TreeView<T> list) {
                return new TextFieldTreeCell<T>(converter);
            }
        };
    }
    
    /**
     * Provides a {@link TextField} that allows editing of the cell content when
     * the cell is double-clicked, or when 
     * {@link TableView#edit(int, TableColumn)} is called. This method will only 
     * work on {@link TableColumn} instances which are of type String.
     * 
     * @return A {@link Callback} that can be inserted into the 
     *      {@link TableColumn#cellFactoryProperty() cell factory property} of a 
     *      TableColumn, that enables textual editing of the content.
     */
    public static <S> Callback<TableColumn<S,String>, TableCell<S,String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }
    
    /**
     * Provides a {@link TextField} that allows editing of the cell content when
     * the cell is double-clicked, or when 
     * {@link TableView#edit(int, TableColumn)} is called. This method will work 
     * on any {@link TableColumn} instance, regardless of its generic type. 
     * However, to enable this, a {@link StringConverter} must be provided that will 
     * convert the given String (from what the user typed in) into an instance 
     * of type T. This item will then be passed along to the 
     * {@link TableColumn#onEditCommitProperty()} callback.
     * 
     * @param converter A {@link StringConverter} that can convert the given String 
     *      (from what the user typed in) into an instance of type T.
     * @return A {@link Callback} that can be inserted into the 
     *      {@link TableColumn#cellFactoryProperty() cell factory property} of a 
     *      TableColumn, that enables textual editing of the content.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final StringConverter<T> converter) {
        return new Callback<TableColumn<S,T>, TableCell<S,T>>() {
            @Override public TableCell<S,T> call(TableColumn<S,T> list) {
                return new TextFieldTableCell<S,T>(converter);
            }
        };
    }
}
