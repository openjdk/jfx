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
import com.javafx.preview.control.ComboBoxContent;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ComboBoxListViewPopup<T> extends PopupControl implements ComboBoxContent<T> {
    private final ComboBox<T> comboBox;
    
    private ListCell<T> listCellLabel;
    private TextField textField;

    public ComboBoxListViewPopup(final ComboBox<T> comboBox) {
        getStyleClass().add("combo-box-list-view-popup");
        this.comboBox = comboBox;
        setAutoHide(true);
        setHideOnEscape(true);
        setOnAutoHide(new EventHandler<Event>() {
            @Override public void handle(Event t) {
                comboBox.hide();
            }
        });
    }
    
    @Override public double computePrefWidth(double height) {
        ListView listView = getListView();
        return listView == null ? 0 : listView.prefWidth(height);
    }
    
    @Override public void showPopup() {
        if (comboBox == null) {
            throw new IllegalStateException("ComboBox is null");
        }
        if (getSkin().getNode() == null) {
            throw new IllegalStateException("Node is null");
        }
        
        if (!isShowing()) {
            if (getSkin() == null) {
                getScene().getRoot().impl_processCSS(true);
            }

            Point2D p = com.sun.javafx.Utils.pointRelativeTo(comboBox, getSkin().getNode(), HPos.CENTER, VPos.BOTTOM, -7, -10, false);
            show(comboBox.getScene().getWindow(), p.getX(), p.getY());
        }
    }

    @Override public void hidePopup() {
        if (isShowing()) {
            hide();
        }
    }
    
    private ListCell<T> getListCellLabel() {
        if (listCellLabel != null) return listCellLabel;
        
        ListView<T> listView = getListView();
        if (listView == null) return null;
        
        Callback<ListView<T>, ListCell<T>> cellFactory = listView.getCellFactory();
        listCellLabel = cellFactory != null ? cellFactory.call(listView) : new ListCell<T>() {
            @Override public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                updateDisplayText(item, empty);
            }
        };
        
        return listCellLabel;
    }

    public ComboBox<T> getComboBox() {
        return comboBox;
    }

    private ListView<T> getListView() {
        if (getSkin() == null) {
            getScene().getRoot().impl_processCSS(true);
        }
        
        Skin s = getSkin();
        
        if (s instanceof ComboBoxListViewPopupSkin) {
            return ((ComboBoxListViewPopupSkin)s).getListView();
        }
        
        return null;
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
            listCellLabel.updateListView(getListView());
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
}
