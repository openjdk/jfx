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
import java.text.Format;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A class containing cell factories that make it easy to convert objects into
 * String representations for display within ListView, TreeView and TableView 
 * controls.
 * 
 * @see StringConverter
 * @see StringConverterListCell
 * @see StringConverterTreeCell
 * @see StringConverterTableCell
 */
@NoBuilder
public final class StringConverterCellFactory {
    
    private StringConverterCellFactory() { }
    
    /**
     * Returns a cell factory that creates a {@link StringConverterListCell} 
     * by setting the {@link StringConverter} to the provided instance.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final StringConverter converter) {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> list) {
                return new StringConverterListCell<T>(converter);
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a {@link StringConverterTreeCell} 
     * by setting the {@link StringConverter} to the provided instance.
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final StringConverter converter) {
        return new Callback<TreeView<T>, TreeCell<T>>() {
            @Override public TreeCell<T> call(TreeView<T> list) {
                return new StringConverterTreeCell<T>(converter);
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a {@link StringConverterTableCell} 
     * by setting the {@link StringConverter} to the provided instance.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final StringConverter converter) {
        return new Callback<TableColumn<S,T>, TableCell<S,T>>() {
            @Override public TableCell<S,T> call(TableColumn<S,T> list) {
                return new StringConverterTableCell<S,T>(converter);
            }
        };
    }
}