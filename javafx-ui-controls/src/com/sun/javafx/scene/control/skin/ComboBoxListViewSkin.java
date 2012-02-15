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

import com.sun.javafx.event.EventDispatchChainImpl;
import com.sun.javafx.scene.control.WeakListChangeListener;
import javafx.scene.control.ComboBox;
import com.sun.javafx.scene.control.behavior.ComboBoxListViewBehavior;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ComboBoxListViewSkin<T> extends ComboBoxPopupControl<T> {
    
    // By default we measure the width of all cells in the ListView. If this
    // is too burdensome, the developer may set a property in the ComboBox
    // properties map with this key to specify the number of rows to measure.
    // This may one day become a property on the ComboBox itself.
    private static final String COMBO_BOX_ROWS_TO_MEASURE_WIDTH_KEY = "comboBoxRowsToMeasureWidth";
    
    private final ComboBox<T> comboBox;
    
    private ListCell<T> listCellLabel;
    private FocusableTextField textField;
    
    private final ListView<T> listView;
    
    private ObservableList<T> listViewItems;
    
    public ComboBoxListViewSkin(final ComboBox<T> comboBox) {
        super(comboBox, new ComboBoxListViewBehavior<T>(comboBox));
        this.comboBox = comboBox;
        this.listView = createListView();
        
        // move focus in to the textfield if the comboBox is editable
        comboBox.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (textField == null) return;
                textField.setFakeFocus(comboBox.isFocused());
            }
        });
        
        comboBox.addEventFilter(InputEvent.ANY, new EventHandler<InputEvent>() {
            @Override public void handle(InputEvent t) {
                if (textField == null) return;
                textField.fireEvent(t);
            }
        });
        
        // Fix for RT-19431 (also tested via ComboBoxListViewSkinTest)
        comboBox.valueProperty().addListener(new ChangeListener<T>() {
            @Override public void changed(ObservableValue<? extends T> ov, T oldValue, T newValue) {
                if (newValue == null) {
                    listView.getSelectionModel().clearSelection();
                } else {
                    int index = comboBox.getSelectionModel().getSelectedIndex();
                    if (index < comboBox.getItems().size()) {
                        T itemsObj = comboBox.getItems().get(index);
                        if (itemsObj != null && itemsObj.equals(newValue)) {
                            listView.getSelectionModel().select(index);
                        } else {
                            listView.getSelectionModel().select(newValue);
                        }
                    } else {
                        // just select the first instance of newValue in the list
                        listView.getSelectionModel().select(newValue);
                    }
                }
            }
        });
        
        updateListViewItems();
        
        registerChangeListener(comboBox.itemsProperty(), "ITEMS");
        registerChangeListener(comboBox.promptTextProperty(), "PROMPT_TEXT");
    }
    
    public void updateListViewItems() {
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
        }

        this.listViewItems = comboBox.getItems();
        listView.setItems(listViewItems);

        if (listViewItems != null) {
            listViewItems.addListener(weakListViewItemsListener);
        }
        
        itemCountDirty = true;
        requestLayout();
    }
    
    private boolean itemCountDirty;
    private final ListChangeListener listViewItemsListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            itemCountDirty = true;
            requestLayout();
        }
    };
    
    private final WeakListChangeListener weakListViewItemsListener =
            new WeakListChangeListener(listViewItemsListener);
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if (p == "ITEMS") {
            updateListViewItems();
        } else if ("PROMPT_TEXT".equals(p)) {
            updateDisplayNode();
        }
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
    
    private FocusableTextField getEditableInputNode() {
        if (textField != null) return textField;
        
        textField = new FocusableTextField();
        textField.setFocusTraversable(true);
        textField.promptTextProperty().bind(comboBox.promptTextProperty());
        
        // focus always goes to the comboBox, which then forwards events down 
        // to the TextField. This ensures that the ComboBox appears focused
        // externally for people listening to the focus property.
        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                comboBox.requestFocus();
            }
        });

        // When the user hits the enter key, set the value in the 
        // ComboBox value property
        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    StringConverter<T> c = comboBox.getConverter();
                    if (c == null) return;
                    T value = c.fromString(textField.getText());
                    comboBox.setValue(value);
                } else if (t.getCode() == KeyCode.F4) {
                    if (comboBox.isShowing()) comboBox.hide();
                    else comboBox.show();
                }
            }
        });
        
        return textField;
    }
    
    private void updateDisplayNode() {
        StringConverter<T> c = comboBox.getConverter();
        if (c == null) return;
                        
        if (comboBox.isEditable()) {
            T value = comboBox.getValue();
            String stringValue = c.toString(value);
            if (value == null || stringValue == null) {
                textField.setText("");
            } else if (! stringValue.equals(textField.getText())) {
                textField.setText(stringValue);
            }
        } else {
            int index = getSelectedIndex();
            listCellLabel.updateListView(listView);
            listCellLabel.updateIndex(index);
        }
    }
    
    private void updateDisplayText(T item, boolean empty) {
        if (empty) {
            listCellLabel.setGraphic(null);
            listCellLabel.setText(comboBox.getPromptText() == null ? null : comboBox.getPromptText());
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
    
    private int getSelectedIndex() {
        T value = comboBox.getValue();
        int index = comboBox.getItems().indexOf(value);
        return index;
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
        listCellLabel.setMouseTransparent(true);
        
        return listCellLabel;
    }

    @Override public Node getPopupContent() {
        return listView;
    }
    
    private ListView<T> createListView() {
        final ListView<T> listView = new ListView<T>() {
            
            @Override protected double computeMinHeight(double width) {
                return 30;
            }
            
            @Override protected double computePrefWidth(double height) {
                doCSSCheck();
                
                double pw;
                if (getSkin() instanceof ListViewSkin) {
                    ListViewSkin skin = (ListViewSkin)getSkin();
                    if (itemCountDirty) {
                        skin.updateCellCount();
                        itemCountDirty = false;
                    }
                    
                    int rowsToMeasure = -1;
                    if (comboBox.getProperties().containsKey(COMBO_BOX_ROWS_TO_MEASURE_WIDTH_KEY)) {
                        rowsToMeasure = (Integer) comboBox.getProperties().get(COMBO_BOX_ROWS_TO_MEASURE_WIDTH_KEY);
                    }
                    
                    pw = skin.getMaxCellWidth(rowsToMeasure) + 30;
                } else {
                    pw = Math.max(100, comboBox.getWidth());
                }
                
                return Math.max(50, pw);
            }

            @Override protected double computePrefHeight(double width) {
                doCSSCheck();
                
                double ph;
                if (getSkin() instanceof VirtualContainerBase) {
                    int maxRows = comboBox.getVisibleRowCount();
                    VirtualContainerBase skin = (VirtualContainerBase)getSkin();
                    ph = skin.getVirtualFlowPreferredHeight(maxRows);
                } else {
                    double ch = comboBox.getItems().size() * 25;
                    ph = Math.min(ch, 200);
                }
                
                return ph;
            }
            
            private void doCSSCheck() {
                if (getSkin() == null) {
                    // if the skin is null, it means that the css related to the
                    // listview skin hasn't been loaded yet, so we force it here.
                    // This ensures the combobox button is the correct width
                    // when it is first displayed, before the listview is shown.
                    getPopup().getScene().getRoot().impl_processCSS(true);
                }
            }
        };

        listView.setId("list-view");
        listView.cellFactoryProperty().bind(comboBox.cellFactoryProperty());
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                int index = listView.getSelectionModel().getSelectedIndex();
                comboBox.getSelectionModel().select(index);
                updateDisplayNode();
            }
        });
        
        listView.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent t) {
                // RT-18672: Without checking if the user is clicking in the 
                // scrollbar area of the ListView, the comboBox will hide. Therefore,
                // we add the check below to prevent this from happening.
                EventTarget target = t.getTarget();
                if (target instanceof Parent) {
                    List<String> s = ((Parent) target).getStyleClass();
                    if (s.contains("thumb") 
                            || s.contains("track") 
                            || s.contains("decrement-arrow") 
                            || s.contains("increment-arrow")) {
                        return;
                    }
                }
                
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
    
    @Override protected double computeMinWidth(double height) {
        return 50;
    }

    
    /**************************************************************************
     * 
     * API for testing
     * 
     *************************************************************************/
    
    ListView<T> getListView() {
        return listView;
    }
    
    
    private final class FocusableTextField extends TextField {
    
        public void setFakeFocus(boolean focus) {
            setFocused(focus);
        }

        @Override
        public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
            EventDispatchChain chain = new EventDispatchChainImpl();
            chain.append(textField.getEventDispatcher());
            return chain;
        }
    }
}
