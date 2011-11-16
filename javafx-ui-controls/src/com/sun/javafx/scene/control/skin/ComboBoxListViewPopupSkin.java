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
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class ComboBoxListViewPopupSkin<T> implements Skin<ComboBoxListViewPopup<T>> {
    private final ComboBoxListViewPopup control;
    private final ComboBox<T> comboBox;

    private ListView<T> listView;

    public ComboBoxListViewPopupSkin(PopupControl c) {
        this.control = (ComboBoxListViewPopup) c;
        this.comboBox = control.getComboBox();

        createListView();
        
//        listView.idProperty().bind(control.idProperty());
//        listView.styleProperty().bind(control.styleProperty());
//        listView.getStyleClass().addAll(control.getStyleClass()); // TODO needs to handle updates
    }
    
    public ListView<T> getListView() {
        return listView;
    }

    @Override public Node getNode() {
        return listView;
    }

    @Override public void dispose() {
       // do nothing subclasses should override
    }

    @Override public ComboBoxListViewPopup<T> getSkinnable() {
        return control;
    }

    public void setItems(ObservableList<T> items) {
        if (listView != null) {
            listView.setItems(items);
        }
    }
    
    private void createListView() {
        listView = new ListView<T>() {
            @Override protected double computePrefWidth(double height) {
                if (getSkin() instanceof VirtualContainerBase) {
                    VirtualContainerBase skin = (VirtualContainerBase)getSkin();
                    return skin.getVirtualFlowPreferredWidth(height) + 10;
                } else {
                    return Math.max(100, control.getComboBox().getWidth());
                }
            }

            @Override protected double computePrefHeight(double width) {
                double ch = control.getComboBox().getItems().size() * 25;
                return Math.min(ch, 200);
            }
        };
        
        listView.itemsProperty().bind(comboBox.itemsProperty());
        listView.cellFactoryProperty().bind(comboBox.cellFactoryProperty());
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
//        listView.minHeightProperty().bind(comboBox.popupHeightProperty());
//        listView.prefHeightProperty().bind(comboBox.popupHeightProperty());
//        listView.maxHeightProperty().bind(comboBox.popupHeightProperty());

        listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                int index = listView.getSelectionModel().getSelectedIndex();
                comboBox.getSelectionModel().select(index);
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
                if (t.getCode() == KeyCode.ENTER || t.getCode() == KeyCode.SPACE) {
                    comboBox.hide();
                }
            }
        });
    }
}
