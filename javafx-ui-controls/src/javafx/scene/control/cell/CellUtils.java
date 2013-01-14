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
package javafx.scene.control.cell;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

// Package protected - not intended for external use
class CellUtils {
    
    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/    
    
    private final static StringConverter defaultStringConverter = new StringConverter<Object>() {
        @Override public String toString(Object t) {
            return t == null ? null : t.toString();
        }

        @Override public Object fromString(String string) {
            return (Object) string;
        }
    };
    
    private final static StringConverter defaultTreeItemStringConverter =
        new StringConverter<TreeItem>() {
            @Override public String toString(TreeItem treeItem) {
                return (treeItem == null || treeItem.getValue() == null) ? 
                        "" : treeItem.getValue().toString();
            }

            @Override public TreeItem fromString(String string) {
                return new TreeItem(string);
            }
        };
    
    /***************************************************************************
     *                                                                         *
     * General convenience                                                     *
     *                                                                         *
     **************************************************************************/    
    
    /*
     * Simple method to provide a StringConverter implementation in various cell
     * implementations.
     */
    static <T> StringConverter<T> defaultStringConverter() {
        return (StringConverter<T>) defaultStringConverter;
    }
    
    /*
     * Simple method to provide a TreeItem-specific StringConverter 
     * implementation in various cell implementations.
     */
    static <T> StringConverter<TreeItem<T>> defaultTreeItemStringConverter() {
        return (StringConverter<TreeItem<T>>) defaultTreeItemStringConverter;
    }
    
    private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ?
            cell.getItem() == null ? "" : cell.getItem().toString() :
            converter.toString(cell.getItem());
    }
    

    
    /***************************************************************************
     *                                                                         *
     * ChoiceBox convenience                                                   *
     *                                                                         *
     **************************************************************************/   
    
    static <T> void updateItem(
            final Cell<T> cell, ChoiceBox<T> choiceBox, 
            final StringConverter<T> converter) {
        updateItem(cell, choiceBox, converter, null);
    }
    
    static <T> void updateItem(
            final Cell<T> cell, ChoiceBox<T> choiceBox, 
            final StringConverter<T> converter,
            final Node graphic) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (choiceBox != null) {
                    choiceBox.getSelectionModel().select(cell.getItem());
                }
                cell.setText(null);
                cell.setGraphic(choiceBox);
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    };
    
    static <T> ChoiceBox<T> createChoiceBox(
            final Cell<T> cell, 
            final ObservableList<T> items) {
        ChoiceBox<T> choiceBox = new ChoiceBox<T>(items);
        choiceBox.setMaxWidth(Double.MAX_VALUE);
        choiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> ov, T oldValue, T newValue) {
                if (cell.isEditing()) {
                    cell.commitEdit(newValue);
                }
            }
        });
        return choiceBox;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * TextField convenience                                                   *
     *                                                                         *
     **************************************************************************/  
    
    static <T> void updateItem(Cell<T> cell, TextField textField, StringConverter<T> converter) {
        updateItem(cell, textField, converter, null);
    }
    
    static <T> void updateItem(final Cell<T> cell, 
                               final TextField textField, 
                               final StringConverter<T> converter,
                               final Node graphic) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText(cell, converter));
                }
                cell.setText(null);
                cell.setGraphic(textField);
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    }
    
    static <T> void startEdit(
            final Cell<T> cell, 
            TextField textField, 
            final StringConverter<T> converter) {
        textField.setText(getItemText(cell, converter));
        
        cell.setText(null);
        cell.setGraphic(textField);
        
        textField.selectAll();
    }
    
    static <T> void cancelEdit(Cell<T> cell, final StringConverter<T> converter) {
        cell.setText(getItemText(cell, converter));
        cell.setGraphic(null);
    }
    
    static <T> TextField createTextField(final Cell<T> cell, final StringConverter<T> converter) {
        final TextField textField = new TextField(getItemText(cell, converter));
        textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    if (converter == null) {
                        throw new IllegalStateException(
                            "Attempting to convert text input into Object, but provided "
                                + "StringConverter is null. Be sure to set a StringConverter "
                                + "in your cell factory.");
                    }
                    cell.commitEdit(converter.fromString(textField.getText()));
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cell.cancelEdit();
                }
            }
        });
        return textField;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * ComboBox convenience                                                   *
     *                                                                         *
     **************************************************************************/ 
    
    static <T> void updateItem(Cell<T> cell, ComboBox<T> comboBox, StringConverter<T> converter) {
        updateItem(cell, comboBox, converter, null);
    }
    
    static <T> void updateItem(final Cell<T> cell, 
                               final ComboBox<T> comboBox, 
                               final StringConverter<T> converter,
                               final Node graphic) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (comboBox != null) {
                    comboBox.getSelectionModel().select(cell.getItem());
                }
                cell.setText(null);
                cell.setGraphic(comboBox);
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    };
    
    static <T> ComboBox<T> createComboBox(final Cell<T> cell, ObservableList<T> items) {
        ComboBox<T> comboBox = new ComboBox<T>(items);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<T>() {
            @Override public void changed(ObservableValue<? extends T> ov, T oldValue, T newValue) {
                if (cell.isEditing()) {
                    cell.commitEdit(newValue);
                }
            }
        });
        return comboBox;
    }
}
