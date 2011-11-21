/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import com.javafx.preview.control.ComboBox;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ComboBoxListViewSkin<T> extends ComboBoxPopupControl<T> {
    
    private final ComboBox<T> comboBox;
    
    private ListCell<T> listCellLabel;
    private TextField textField;
    
    private final ListView<T> listView;
    
    public ComboBoxListViewSkin(final ComboBox<T> comboBox) {
        super(comboBox);
        this.comboBox = comboBox;
        this.listView = createListView();
    }
    
    @Override public Node getDisplayNode() {
        Node displayNode;
        if (comboBox.isEditable()) {
            if (textField == null) {
                textField = getEditableInputNode();
            }
            displayNode = textField;
        } else {
            if (listCellLabel == null) {
                listCellLabel = getListCellLabel();
            }
            displayNode = listCellLabel;
        }
        
        updateDisplayNode();
        
        return displayNode;
    }
    
    private TextField getEditableInputNode() {
        if (textField != null) return textField;
        
        textField = new TextField();

        // When the user hits the enter key, set the value in the 
        // ComboBox value property
        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    StringConverter<T> c = comboBox.getConverter();
                    if (c == null) return;
                    T value = c.fromString(textField.getText());
                    comboBox.setValue(value);
                }
            }
        });
        
        return textField;
    }
    
    private void updateDisplayNode() {
        StringConverter<T> c = comboBox.getConverter();
        if (c == null) return;
                        
        T item = comboBox.getSelectionModel().getSelectedItem();
        int index = comboBox.getSelectionModel().getSelectedIndex();        
        
        if (comboBox.isEditable()) {
            textField.setText(c.toString(item));
        } else {
            listCellLabel.updateListView(listView);
            listCellLabel.updateIndex(index);
        }
    }
    
    private void updateDisplayText(T item, boolean empty) {
        if (empty) {
            listCellLabel.setText(null);
            listCellLabel.setGraphic(null);
        } else if (item instanceof Node) {
            Node currentNode = listCellLabel.getGraphic();
            Node newNode = (Node) item;
            if (currentNode == null || ! currentNode.equals(newNode)) {
                listCellLabel.setText(null);
                listCellLabel.setGraphic(newNode);
            }
        } else {
            listCellLabel.setText(item == null ? "" : item.toString());
        }
    }
    
    
    
    private ListCell<T> getListCellLabel() {
        if (listCellLabel != null) return listCellLabel;
        
        Callback<ListView<T>, ListCell<T>> cellFactory = listView.getCellFactory();
        listCellLabel = cellFactory != null ? cellFactory.call(listView) : new ListCell<T>() {
            @Override public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                updateDisplayText(item, empty);
            }
        };
        
        return listCellLabel;
    }

    @Override public Node getPopupContent() {
        return listView;
    }
    
    private ListView<T> createListView() {
        final ListView<T> listView = new ListView<T>() {
            @Override protected double computePrefWidth(double height) {
                if (getSkin() == null) {
                    // if the skin is null, it means that the css related to the
                    // listview skin hasn't been loaded yet, so we force it here.
                    // This ensures the combobox button is the correct width
                    // when it is first displayed, before the listview is shown.
                    getPopup().getScene().getRoot().impl_processCSS(true);
                }
                
                if (getSkin() instanceof VirtualContainerBase) {
                    VirtualContainerBase skin = (VirtualContainerBase)getSkin();
                    return skin.getVirtualFlowPreferredWidth(height) + 10;
                } else {
                    return Math.max(100, comboBox.getWidth());
                }
            }

            @Override protected double computePrefHeight(double width) {
                double ch = comboBox.getItems().size() * 25;
                return Math.min(ch, 200);
            }
        };

        listView.itemsProperty().bind(comboBox.itemsProperty());
        listView.cellFactoryProperty().bind(comboBox.cellFactoryProperty());
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                int index = listView.getSelectionModel().getSelectedIndex();
                comboBox.getSelectionModel().select(index);
                comboBox.setValue(listView.getSelectionModel().getSelectedItem());
            }
        });

        listView.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent t) {
                comboBox.hide();
            }
        });

        listView.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent t) {
                // TODO move to behavior, when (or if) this class becomes a SkinBase
                if (t.getCode() == KeyCode.ENTER || 
                        t.getCode() == KeyCode.SPACE || 
                        t.getCode() == KeyCode.ESCAPE) {
                    comboBox.hide();
                }
            }
        });
        
        return listView;
    }

    @Override protected double computePrefWidth(double height) {
        return listView.prefWidth(height);
    }
}
