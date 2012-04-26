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

/**
 * A class containing cell factories that make it easy to manipulate the String 
 * representation of items contained within ListView, TreeView and TableView 
 * controls.
 * 
 * @see Format
 * @see TextAlignment
 * @see StringFormatListCell
 * @see StringFormatTreeCell
 * @see StringFormatTableCell
 */
@NoBuilder
public final class StringFormatCellFactory {
    
    private StringFormatCellFactory() { }
    
    /**
     * Returns a cell factory that creates a default {@link StringFormatListCell}.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView() {
        return forListView(TextAlignment.LEFT);
    }
    
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final TextAlignment align) {
        return forListView(align, null);
    }
    
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final Format format) {
        return forListView(TextAlignment.LEFT, format);
    }
    
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final TextAlignment align, 
            final Format format) {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> list) {
                return new StringFormatListCell<T>(align, format);
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a {@link StringFormatListCell} that 
     * uses the provided {@link Callback} to convert from the generic T type to 
     * a String, such that it is rendered appropriately.
     * 
     * @param toString A {@link Callback} that converts an instance of type T to
     *      a String, for rendering in the {@link LabelCellFactory}.
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
            final Callback<T, String> toString) {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> list) {
                return new StringFormatListCell<T>() {
                    @Override public String toString(T item) {
                        return toString == null ? 
                                super.toString(item) : toString.call(item);
                    };
                };
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a default {@link StringFormatTreeCell}.
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView() {
        return forTreeView(TextAlignment.LEFT);
    }
    
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final TextAlignment align) {
        return forTreeView(align, null);
    }
    
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final Format format) {
        return forTreeView(TextAlignment.LEFT, format);
    }
    
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final TextAlignment align, 
            final Format format) {
        return new Callback<TreeView<T>, TreeCell<T>>() {
            @Override public TreeCell<T> call(TreeView<T> list) {
                return new StringFormatTreeCell<T>(align, format);
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a {@link StringFormatTableCell} that 
     * uses the provided {@link Callback} to convert from the generic T type to 
     * a String, such that it is rendered appropriately.
     * 
     * @param toString A {@link Callback} that converts an instance of type T to 
     *      a String, for rendering in the {@link TableCell}.
     */
    public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
            final Callback<T, String> toString) {
        return new Callback<TreeView<T>, TreeCell<T>>() {
            @Override public TreeCell<T> call(TreeView<T> list) {
                return new StringFormatTreeCell<T>() {
                    @Override public String toString(T item) {
                        return toString == null ? 
                                super.toString(item) : toString.call(item);
                    };
                };
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a default {@link StringFormatTableCell}.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn() {
        return forTableColumn(TextAlignment.LEFT);
    }
    
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final TextAlignment align) {
        return forTableColumn(align, null);
    }
    
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final Format format) {
        return forTableColumn(TextAlignment.LEFT, format);
    }
    
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final TextAlignment align, 
            final Format format) {
        return new Callback<TableColumn<S,T>, TableCell<S,T>>() {
            @Override public TableCell<S,T> call(TableColumn<S,T> list) {
                return new StringFormatTableCell<S,T>(align, format);
            }
        };
    }
    
    /**
     * Returns a cell factory that creates a {@link StringFormatTreeCell} that 
     * uses the provided {@link Callback} to convert from the generic T type to 
     * a String, such that it is rendered appropriately.
     * 
     * @param toString A {@link Callback} that converts an instance of type T to 
     *      a String, for rendering in the {@link TreeCell}.
     */
    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final Callback<T, String> toString) {
        return new Callback<TableColumn<S,T>, TableCell<S,T>>() {
            @Override public TableCell<S,T> call(TableColumn<S,T> list) {
                return new StringFormatTableCell<S,T>() {
                    @Override public String toString(T item) {
                        return toString == null ? 
                                super.toString(item) : toString.call(item);
                    };
                };
            }
        };
    }
}