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
import com.sun.javafx.scene.control.behavior.ComboBoxBehavior;
import com.sun.javafx.scene.control.skin.SkinBase;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ComboBoxSkin<T> extends SkinBase<ComboBox<T>, ComboBoxBehavior<T>> {
    
    private ListCell<T> label;
    private TextField textField;
    private Node displayNode; // this is always either label or textbox
    
    private StackPane arrowButton;
    private StackPane arrow;
    
    private ComboBoxListViewPopup<T> popup;
    
    public ComboBoxSkin(final ComboBox<T> comboBox) {
        // Call the super method with the button we were just given in the 
        // constructor, as well as an instance of the behavior class.
        super(comboBox, new ComboBoxBehavior(comboBox));
        
        // open button / arrow
        arrow = new StackPane();
        arrow.getStyleClass().setAll("arrow");
        arrow.setMaxWidth(USE_PREF_SIZE);
        arrow.setMaxHeight(USE_PREF_SIZE);
        arrowButton = new StackPane();
        arrowButton.getStyleClass().setAll("arrow-button");
        arrowButton.getChildren().add(arrow);
        getChildren().add(arrowButton);
        
        arrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                getBehavior().arrowPressed(e);
                e.consume();
            }
        });
        arrowButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                getBehavior().arrowReleased(e);
                e.consume();
            }
        });
        
        // popup
        popup = new ComboBoxListViewPopup<T>(getSkinnable());
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setOnAutoHide(new EventHandler<Event>() {
            @Override public void handle(Event t) {
                getSkinnable().hide();
            }
        });
        
        // main area
        updateDisplayArea();
        
        // Register listeners
        registerChangeListener(comboBox.editableProperty(), "EDITABLE");
        registerChangeListener(comboBox.showingProperty(), "SHOWING");
        registerChangeListener(comboBox.focusedProperty(), "FOCUSED");
        registerChangeListener(comboBox.cellFactoryProperty(), "CELL_FACTORY");
//        registerChangeListener(comboBox.popupHeightProperty(), "POPUP_HEIGHT");
        registerChangeListener(comboBox.itemsProperty(), "ITEMS");
        registerChangeListener(comboBox.getSelectionModel().selectedItemProperty(), "SELECTED_ITEM");
//        registerChangeListener(popup.visibleProperty(), "POPUP_VISIBLE");
    }

     /**
     * Handles changes to properties of the MenuButton.
     */
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if (p == "SHOWING") {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                hide();
            }
        } else if (p == "ITEMS") {
            popup.getListView().setItems(getSkinnable().getItems());
        } else if (p == "POPUP_HEIGHT") {
//            if (popup.getListView() != null) {
//                double h = getSkinnable().getPopupHeight();
//                popup.getListView().setPrefHeight(h);
//                popup.getListView().setMinHeight(h);
//                popup.getListView().setMaxHeight(h);
//            }
        } else if (p == "EDITABLE") {
            updateDisplayArea();
        } else if (p == "CELL_FACTORY") {
            if (popup.getListView() != null) {
                popup.getListView().setCellFactory(getSkinnable().getCellFactory());
            }
        } else if (p == "FOCUSED") {
            if (textField != null && getSkinnable().isFocused()) {
                textField.requestFocus();
            }
        } else if (p == "SELECTED_ITEM") {
            updateDisplayText();
        }
    }
    
    private void updateDisplayArea() {
        if (getSkinnable().isEditable()) {
            if (label != null) {
                getChildren().remove(label);
                label = null;
            }
            
            if (textField == null) {
                textField = new TextField();
                
                // When the user hits the enter key, set the value in the 
                // ComboBox value property
                textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
                    @Override public void handle(KeyEvent t) {
                        if (t.getCode() == KeyCode.ENTER) {
                            StringConverter<T> c = getSkinnable().getConverter();
                            if (c == null) return;
                            T value = c.fromString(textField.getText());
                            getSkinnable().setValue(value);
                        }
                    }
                });
                
//                // as the user types their input, filter the listview and provide
//                // auto-complete functionality in this textbox.
//                textField.textProperty().addListener(new ChangeListener<String>() {
//                    @Override
//                    public void changed(ObservableValue<? extends String> ov, String t, String newText) {
////                        filterListView(newText);
//                        
////                        autoComplete(newText);
//                    }
//                });
                
                updateDisplayText();
            }
            displayNode = textField;
            
            getChildren().add(0, textField);
        } else {
            if (textField != null) {
                getChildren().remove(textField);
                textField = null;
            }
            
            if (label == null) {
                Callback<ListView<T>, ListCell<T>> cellFactory = getSkinnable().getCellFactory();
                label = cellFactory != null ? cellFactory.call(popup.getListView()) : new ListCell<T>() {
                    @Override public void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        updateDisplayText(item, empty);
                    }
                };
            }
            
            updateDisplayText();
            displayNode = label;
            
            getChildren().add(0, label);
        }
    }
    
    private void updateDisplayText() {
        StringConverter<T> c = getSkinnable().getConverter();
        if (c == null) return;
                        
        T item = getSkinnable().getSelectionModel().getSelectedItem();
        int index = getSkinnable().getSelectionModel().getSelectedIndex();        
        
        if (getSkinnable().isEditable()) {
            textField.setText(c.toString(item));
        } else {
//            updateDisplayText(item, false);
            label.updateListView(popup.getListView());
            label.updateIndex(index);
        }
    }
    
    private void updateDisplayText(T item, boolean empty) {
        if (empty) {
            label.setText(null);
            label.setGraphic(null);
        } else if (item instanceof Node) {
            Node currentNode = getNode();
            Node newNode = (Node) item;
            if (currentNode == null || ! currentNode.equals(newNode)) {
                label.setText(null);
                label.setGraphic(newNode);
            }
        } else {
            System.out.println(item + " is " + (item == null ? null : item.getClass()));
            label.setText(item == null ? "" : item.toString());
        }
    }
    
//    private void autoComplete(final String text) {
//        if (text == null || text.isEmpty()) return;
//        
//        final StringConverter<T> c = getSkinnable().getConverter();
//        if (c == null) return;
//        
//        List<T> items = getSkinnable().getItems();
//        if (items == null) return;
//        
//        // we might want to one day expose this as API on ComboBox
//        final boolean MATCH_CASE = false;
//        
//        // find first match for the given text
//        for (int i = 0; i < items.size(); i++) {
//            String suggestion = c.toString(items.get(i));
//            if (suggestion == null) continue;
//            
//            if ((MATCH_CASE && suggestion.startsWith(text)) || 
//                    (! MATCH_CASE && suggestion.toLowerCase().startsWith(text.toLowerCase()))) {
//                
//                // we've found a match - lets suggest this
//                textField.setText(suggestion);
//                textField.selectRange(text.length(), suggestion.length());
////                System.out.println("should have following text selected: " + suggestion.substring(text.length(), suggestion.length()));
//                return;
//            }
//        }
//    }
    
//    private void filterListView(final String text) {
//        if (! (popup.getListView().getItems() instanceof FilteredList)) return;
//
//        final StringConverter<T> c = getSkinnable().getConverter();
//        if (c == null) return;
//        
//        FilteredList<T> list = (FilteredList<T>) popup.getListView().getItems();
//        list.setMatcher(new Matcher<T>() {
//            @Override public boolean matches(T e) {
//                boolean match = c.toString(e).startsWith(text);
//                return match;
//            }
//        });
//    }
    
    private void show() {
        if (!popup.isShowing()) {
            popup.show(this);
        }
    }

    private void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }
    
    @Override
    protected void layoutChildren() {
        final Insets padding = getPadding();
        final Insets arrowButtonPadding = arrowButton.getPadding();

        // x, y, w, h are the content area that will hold the label and arrow */
        final double x = padding.getLeft();
        final double y = padding.getTop();
        final double w = getWidth() - (padding.getLeft() + padding.getRight());
        final double h = getHeight() - (padding.getTop() + padding.getBottom());

        final double arrowWidth = arrow.prefWidth(-1);
        final double arrowButtonWidth = arrowButtonPadding.getLeft() + arrowWidth + arrowButtonPadding.getRight();
        
        displayNode.resizeRelocate(x, y, w, h);
        
        arrowButton.resize(arrowButtonWidth, getHeight());
        positionInArea(arrowButton, getWidth() - padding.getRight() - arrowButtonWidth, 0, 
                arrowButtonWidth, getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }
    
    @Override protected double computePrefWidth(double height) {
//        final Insets padding = getPadding();
//        final Insets arrowButtonPadding = arrowButton.getPadding();
//        
//        double w;
//        if (getSkinnable().isEditable()) {
//            w = padding.getLeft()
//                + displayNode.prefWidth(height)
////                + arrowButtonPadding.getLeft()
////                + arrow.prefWidth(height)
////                + arrowButtonPadding.getRight()
//                + padding.getRight();
//        } else {
//            w = padding.getLeft()
//                + displayNode.prefWidth(height)
//                + arrowButtonPadding.getLeft()
//                + arrow.prefWidth(height)
//                + arrowButtonPadding.getRight()
//                + padding.getRight();
//        }
//        
        double listPrefWidth = popup.getPrefWidth(height);
//        return Math.max(w, listPrefWidth);
        return listPrefWidth;
    }

    @Override protected double computePrefHeight(double width) {
        final Insets padding = getPadding();
        final Insets arrowButtonPadding = arrowButton.getPadding();
        double arrowHeight = arrowButtonPadding.getTop() + arrow.prefHeight(-1) + arrowButtonPadding.getBottom();
        
        return padding.getTop()
                + Math.max(displayNode.prefHeight(width), arrowHeight)
                + padding.getBottom();
    }

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }
}
