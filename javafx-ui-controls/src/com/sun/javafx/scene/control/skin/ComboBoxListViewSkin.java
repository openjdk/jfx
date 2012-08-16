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
import com.sun.javafx.scene.control.FocusableTextField;
import com.sun.javafx.scene.control.WeakListChangeListener;
import javafx.scene.control.ComboBox;
import com.sun.javafx.scene.control.behavior.ComboBoxListViewBehavior;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/    
    
    private final ComboBox<T> comboBox;
    
    private ListCell<T> buttonCell;
    private Callback<ListView<T>, ListCell<T>> cellFactory;
    private TextField textField;
    
    private final ListView<T> listView;
    
    private ObservableList<T> listViewItems;
    
    private boolean listSelectionLock = false;
    private boolean listViewSelectionDirty = false;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/
    
    private boolean itemCountDirty;
    private final ListChangeListener listViewItemsListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            itemCountDirty = true;
            requestLayout();
        }
    };
    
    private final WeakListChangeListener weakListViewItemsListener =
            new WeakListChangeListener(listViewItemsListener);
    
    
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/   
    
    public ComboBoxListViewSkin(final ComboBox<T> comboBox) {
        super(comboBox, new ComboBoxListViewBehavior<T>(comboBox));
        this.comboBox = comboBox;
        this.listView = createListView();
        this.textField = getEditableInputNode();
        
        // Fix for RT-21207. Additional code related to this bug is further below.
        this.listView.setManaged(false);
        getChildren().add(listView);
        // -- end of fix
                
        updateListViewItems();
        updateCellFactory();
        
        updateButtonCell();
        
        // move focus in to the textfield if the comboBox is editable
        comboBox.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                updateFakeFocus(comboBox.isFocused());
            }
        });
        
        comboBox.addEventFilter(InputEvent.ANY, new EventHandler<InputEvent>() {
            @Override public void handle(InputEvent t) {
                if (textField == null) return;
                
                // When the user hits the enter or F4 keys, we respond before 
                // ever giving the event to the TextField.
                if (t instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent)t;
                    
                    if (ke.getCode() == KeyCode.ENTER) {
                        setTextFromTextFieldIntoComboBoxValue();
                        t.consume();
                        return;
                    } else if (ke.getCode() == KeyCode.F4 && ke.getEventType() == KeyEvent.KEY_RELEASED) {
                        if (comboBox.isShowing()) comboBox.hide();
                        else comboBox.show();
                        t.consume();
                        return;
                    } else if (ke.getCode() == KeyCode.F10 || ke.getCode() == KeyCode.ESCAPE) {
                        // RT-23275: The TextField fires F10 and ESCAPE key events
                        // up to the parent, which are then fired back at the 
                        // TextField, and this ends up in an infinite loop until
                        // the stack overflows. So, here we consume these two
                        // events and stop them from going any further.
                        t.consume();
                        return;
                    }
                }
                
                textField.fireEvent(t);
            }
        });
        
        // Fix for RT-19431 (also tested via ComboBoxListViewSkinTest)
        updateValue();
        
        registerChangeListener(comboBox.itemsProperty(), "ITEMS");
        registerChangeListener(comboBox.promptTextProperty(), "PROMPT_TEXT");
        registerChangeListener(comboBox.cellFactoryProperty(), "CELL_FACTORY");
        registerChangeListener(comboBox.visibleRowCountProperty(), "VISIBLE_ROW_COUNT");
        registerChangeListener(comboBox.converterProperty(), "CONVERTER");
        registerChangeListener(comboBox.editorProperty(), "EDITOR");
        registerChangeListener(comboBox.buttonCellProperty(), "BUTTON_CELL");
        registerChangeListener(comboBox.valueProperty(), "VALUE");
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/  
    
    /** {@inheritDoc} */
    @Override protected void handleControlPropertyChanged(String p) {
        // Fix for RT-21207
        if (p == "SHOWING") {
            if (getSkinnable().isShowing()) {
                this.listView.setManaged(true);
            } else {
                this.listView.setManaged(false);
            }
        }
        // -- end of fix
        
        super.handleControlPropertyChanged(p);
        
        if (p == "ITEMS") {
            updateListViewItems();
        } else if ("PROMPT_TEXT".equals(p)) {
            updateDisplayNode();
        } else if ("CELL_FACTORY".equals(p)) {
            updateCellFactory();
        } else if ("VISIBLE_ROW_COUNT".equals(p)) {
            if (listView == null) return;
            listView.setPrefHeight(getListViewPrefHeight());
        } else if ("CONVERTER".equals(p)) {
            updateListViewItems();
        } else if ("EDITOR".equals(p)) {
            getEditableInputNode();
        } else if ("BUTTON_CELL".equals(p)) {
            updateButtonCell();
        } else if ("VALUE".equals(p)) {
            updateValue();
        }
    }
    
    /** {@inheritDoc} */
    @Override public Node getDisplayNode() {
        Node displayNode;
        if (comboBox.isEditable()) {
            displayNode = textField;
        } else {
            displayNode = buttonCell;
        }
        
        updateDisplayNode();
        
        return displayNode;
    }
    
    /**
     * 
     */
    public void updateListViewItems() {
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
        }

        this.listViewItems = comboBox.getItems();
        listView.setItems(null);
        listView.setItems(listViewItems);

        if (listViewItems != null) {
            listViewItems.addListener(weakListViewItemsListener);
        }
        
        itemCountDirty = true;
        requestLayout();
    }
    
    @Override public Node getPopupContent() {
        return listView;
    }
    
    @Override protected double computePrefWidth(double height) {
        double pw = listView.prefWidth(height);
        
        reconfigurePopup();
        
        return pw;
    }
    
    @Override protected double computeMinWidth(double height) {
        return 50;
    }
    
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        if (listViewSelectionDirty) {
            try {
                listSelectionLock = true;
                T item = comboBox.getSelectionModel().getSelectedItem();
                listView.getSelectionModel().clearSelection();
                listView.getSelectionModel().select(item);
            } finally {
                listSelectionLock = false;
                listViewSelectionDirty = false;
            }
        }
        
        super.layoutChildren(x,y,w,h);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/    
    
    private void updateValue() {
        T newValue = comboBox.getValue();
        
        SelectionModel sm = listView.getSelectionModel();
        
        if (newValue == null) {
            sm.clearSelection();
        } else {
            // RT-22386: We need to test to see if the value is in the comboBox
            // items list. If it isn't, then we should clear the listview 
            // selection
            int indexOfNewValue = getIndexOfComboBoxValueInItemsList();
            if (indexOfNewValue == -1) {
                listSelectionLock = true;
                sm.clearSelection();
                listSelectionLock = false;
            } else {
                int index = comboBox.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < comboBox.getItems().size()) {
                    T itemsObj = comboBox.getItems().get(index);
                    if (itemsObj != null && itemsObj.equals(newValue)) {
                        sm.select(index);
                    } else {
                        sm.select(newValue);
                    }
                } else {
                    // just select the first instance of newValue in the list
                    int listViewIndex = listView.getItems().indexOf(newValue);
                    if (listViewIndex == -1) {
                        // RT-21336 Show the ComboBox value even though it doesn't
                        // exist in the ComboBox items list (part one of fix)
                        updateDisplayNode();
                    } else {
                        sm.select(listViewIndex);
                    }
                }
            }
        }
    }
    
    private TextField getEditableInputNode() {
        if (textField != null) return textField;
        
        textField = comboBox.getEditor();
        textField.setFocusTraversable(true);
        textField.promptTextProperty().bind(comboBox.promptTextProperty());
        
        // focus always goes to the comboBox, which then forwards events down 
        // to the TextField. This ensures that the ComboBox appears focused
        // externally for people listening to the focus property.
        // Also, (for RT-21454) set the currently typed text as the value when focus 
        // is lost from the ComboBox
        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean hasFocus) {
                if (hasFocus) {
                    // Using Platform.runLater here, as without it it seems we
                    // enter into some form of race condition where we are 
                    // wanting to set focus on the comboBox whilst the textField
                    // is still notifying of its focus gain.
                    // This issue was identified in RT-21088.
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            comboBox.requestFocus();
                        }
                    });
                }
                
                updateFakeFocus(hasFocus);
                
                // RT-21454 starts here
                if (! hasFocus) {
                    setTextFromTextFieldIntoComboBoxValue();
                }
            }
        });

        return textField;
    }
    
    private void updateFakeFocus(boolean b) {
        if (textField == null) return;
        if (! (textField instanceof FocusableTextField)) return;
        ((FocusableTextField)textField).setFakeFocus(b);
    }
    
    private void updateDisplayNode() {
        StringConverter<T> c = comboBox.getConverter();
        if (c == null) return;
              
        T value = comboBox.getValue();
        if (comboBox.isEditable()) {
            String stringValue = c.toString(value);
            if (value == null || stringValue == null) {
                textField.setText("");
            } else if (! stringValue.equals(textField.getText())) {
                textField.setText(stringValue);
            }
        } else {
            int index = getIndexOfComboBoxValueInItemsList();
            if (index > -1) {
                buttonCell.updateListView(listView);
                buttonCell.updateIndex(index);
            } else {
                // RT-21336 Show the ComboBox value even though it doesn't
                // exist in the ComboBox items list (part two of fix)
                updateDisplayText(buttonCell, value, false);
            }
        }
    }
    
    private void updateDisplayText(ListCell<T> cell, T item, boolean empty) {
        if (empty) {
            if (buttonCell == null) return;
            cell.setGraphic(null);
            cell.setText(comboBox.getPromptText() == null ? null : comboBox.getPromptText());
        } else if (item instanceof Node) {
            Node currentNode = buttonCell.getGraphic();
            Node newNode = (Node) item;
            if (currentNode == null || ! currentNode.equals(newNode)) {
                cell.setText(null);
                cell.setGraphic(newNode);
            }
        } else {
            // run item through StringConverter if it isn't null
            StringConverter c = comboBox.getConverter();
            String s = item == null ? comboBox.getPromptText() : (c == null ? item.toString() : c.toString(item));
            cell.setText(s);
            cell.setGraphic(null);
        }
    }
    
    private void setTextFromTextFieldIntoComboBoxValue() {
        if (! comboBox.isEditable()) return;
        
        StringConverter<T> c = comboBox.getConverter();
        if (c == null) return;
        
        T oldValue = comboBox.getValue();
        T value = c.fromString(textField.getText());
        
        if ((value == null && oldValue == null) || (value != null && value.equals(oldValue))) {
            // no point updating values needlessly (as they are the same)
            return;
        }
        
        comboBox.setValue(value);
    }
    
    private int getIndexOfComboBoxValueInItemsList() {
        T value = comboBox.getValue();
        int index = comboBox.getItems().indexOf(value);
        return index;
    }
    
    private void updateButtonCell() {
        buttonCell = comboBox.getButtonCell() != null ? 
                comboBox.getButtonCell() : getDefaultCellFactory().call(listView);
        buttonCell.setMouseTransparent(true);
    }

    private void updateCellFactory() {
        Callback<ListView<T>, ListCell<T>> cf = comboBox.getCellFactory();
        cellFactory = cf != null ? cf : getDefaultCellFactory();
        listView.setCellFactory(cellFactory);
    }
    
    private Callback<ListView<T>, ListCell<T>> getDefaultCellFactory() {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> listView) {
                return new ListCell<T>() {
                    @Override public void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        updateDisplayText(this, item, empty);
                    }
                };
            }
        };
    }
    
    private ListView<T> createListView() {
        final ListView<T> listView = new ListView<T>() {
            private boolean isFirstSizeCalculation = true;
            
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
                    
                    pw = Math.max(comboBox.getWidth(), skin.getMaxCellWidth(rowsToMeasure) + 30);
                } else {
                    pw = Math.max(100, comboBox.getWidth());
                }
                
                return Math.max(50, pw);
            }

            @Override protected double computePrefHeight(double width) {
                doCSSCheck();
                
                return getListViewPrefHeight();
            }
            
            private void doCSSCheck() {
                Parent parent = getPopup().getScene().getRoot();
                if ((isFirstSizeCalculation || getSkin() == null) && parent.getScene() != null) {
                    // if the skin is null, it means that the css related to the
                    // listview skin hasn't been loaded yet, so we force it here.
                    // This ensures the combobox button is the correct width
                    // when it is first displayed, before the listview is shown.
                    parent.impl_processCSS(true);
                    isFirstSizeCalculation = false;
                }
            }
        };

        listView.setId("list-view");
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
             @Override public void invalidated(Observable o) {
                 if (listSelectionLock) return;
                 int index = listView.getSelectionModel().getSelectedIndex();
                 comboBox.getSelectionModel().select(index);
                 updateDisplayNode();
             }
         });
         
        comboBox.getSelectionModel().selectedItemProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                listViewSelectionDirty = true;
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
    
    private double getListViewPrefHeight() {
        double ph;
        if (listView.getSkin() instanceof VirtualContainerBase) {
            int maxRows = comboBox.getVisibleRowCount();
            VirtualContainerBase skin = (VirtualContainerBase)listView.getSkin();
            ph = skin.getVirtualFlowPreferredHeight(maxRows);
        } else {
            double ch = comboBox.getItems().size() * 25;
            ph = Math.min(ch, 200);
        }
        
        return ph;
    }


    
    /**************************************************************************
     * 
     * API for testing
     * 
     *************************************************************************/
    
    public ListView<T> getListView() {
        return listView;
    }
}
