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

import javafx.util.StringConverter;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import com.sun.javafx.scene.control.behavior.ChoiceBoxBehavior;
import javafx.collections.WeakListChangeListener;


/**
 * ChoiceBoxSkin - default implementation
 */
    public class ChoiceBoxSkin<T> extends BehaviorSkinBase<ChoiceBox<T>, ChoiceBoxBehavior<T>> {

    public ChoiceBoxSkin(ChoiceBox control) {
        super(control, new ChoiceBoxBehavior(control));
        initialize();
        requestLayout();
        registerChangeListener(control.selectionModelProperty(), "SELECTION_MODEL");
        registerChangeListener(control.showingProperty(), "SHOWING");
        registerChangeListener(control.itemsProperty(), "ITEMS");
        registerChangeListener(control.getSelectionModel().selectedItemProperty(), "SELECTION_CHANGED");
        registerChangeListener(control.converterProperty(), "CONVERTER");
    }

    private ObservableList<?> choiceBoxItems;

    private ContextMenu popup;

    // The region that shows the "arrow" box portion
    private StackPane openButton;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    /*
     * Watch for if the user changes the selected index, and if so, we toggle
     * the selection in the toggle group (so the check shows in the right place)
     */
    private SelectionModel selectionModel;

    private Label label;

    private final ListChangeListener choiceBoxItemsListener = new ListChangeListener() {
        @Override public void onChanged(Change c) {
            while (c.next()) {
                if (c.getRemovedSize() > 0) {
                    popup.getItems().clear();
                    int i = 0;
                    for (Object obj : c.getList()) {
                        addPopupItem(obj, i);
                        i++;
                    }
                    requestLayout(); // RT-18052
                    return;
                }
                for (int i = c.getFrom(); i < c.getTo(); i++) {
                    final Object obj = c.getList().get(i);
                    addPopupItem(obj, i);
                }
            }
            updateSelection();
            // RT-21891 weird initial appearance: need a better fix for this instead
            // of having to rely on impl_processCSS. 
            popup.getScene().getRoot().impl_processCSS(true); 
            requestLayout(); // RT-18052 resize of choicebox should happen immediately.
        }
    };
    
    private final WeakListChangeListener weakChoiceBoxItemsListener =
            new WeakListChangeListener(choiceBoxItemsListener);

    private void initialize() {
        updateChoiceBoxItems();

        label = new Label();
        label.setMnemonicParsing(false);  // ChoiceBox doesn't do Mnemonics

        openButton = new StackPane();
        openButton.getStyleClass().setAll("open-button");

        StackPane region = new StackPane();
        region.getStyleClass().setAll("arrow");
        openButton.getChildren().clear();
        openButton.getChildren().addAll(region);

        popup = new ContextMenu();
        // When popup is hidden by autohide - the ChoiceBox Showing property needs
        // to be updated. So we listen to when autohide happens. Calling hide()
        // there after causes Showing to be set to false
        popup.setOnAutoHide(new EventHandler<Event>() {
            @Override public void handle(Event event) {
                ((ChoiceBox)getSkinnable()).hide();
            }
        });
        // fix RT-14469 : When tab shifts ChoiceBox focus to another control,
        // its popup should hide.
        getSkinnable().focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    ((ChoiceBox)getSkinnable()).hide();
                }
            }
        });
        // This is used as a way of accessing the context menu within the ChoiceBox.
        popup.setId("choice-box-popup-menu");
//        popup.getItems().clear();
//        popup.getItems().addAll(popupItems);
//        popup.setManaged(false);
//        popup.visibleProperty().addListener(new InvalidationListener() {
//            @Override public void invalidated(ObservableValue valueModel) {
//                if (popup.isVisible() {
////                    RadioMenuItem selected = (RadioMenuItem) toggleGroup.getSelectedToggle();
////                    if (selected != null) selected.requestFocus();
//                } else {
//                    getBehavior().close();
//                }
//            }
//        });
        getChildren().setAll(label, openButton);

        updatePopupItems();

        updateSelectionModel();
        updateSelection();
        if(selectionModel != null && selectionModel.getSelectedIndex() == -1) {
            label.setText(""); // clear label text when selectedIndex is -1
        }
    }

    private void updateChoiceBoxItems() {
        if (choiceBoxItems != null) {
            choiceBoxItems.removeListener(weakChoiceBoxItemsListener);
        }
        choiceBoxItems = getSkinnable().getItems();
        if (choiceBoxItems != null) {
            choiceBoxItems.addListener(weakChoiceBoxItemsListener);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("ITEMS".equals(p)) {
            updateChoiceBoxItems();
            updatePopupItems();
        } else if (("SELECTION_MODEL").equals(p)) {
            updateSelectionModel();
        } else if ("SELECTION_CHANGED".equals(p)) {
            if (getSkinnable().getSelectionModel() != null) {
                int index = getSkinnable().getSelectionModel().getSelectedIndex();
                if (index != -1) {
                    MenuItem item = popup.getItems().get(index);
                    if (item instanceof RadioMenuItem) ((RadioMenuItem)item).setSelected(true);
                }
            }
        } else if ("SHOWING".equals(p)) {
            if (getSkinnable().isShowing()) {
                MenuItem item = null;

                SelectionModel sm = getSkinnable().getSelectionModel();
                if (sm == null) return;

                long currentSelectedIndex = sm.getSelectedIndex();
                int itemInControlCount = choiceBoxItems.size();
                boolean hasSelection = currentSelectedIndex >= 0 && currentSelectedIndex < itemInControlCount;
                if (hasSelection) {
                    item = popup.getItems().get((int) currentSelectedIndex);
                    if (item != null && item instanceof RadioMenuItem) ((RadioMenuItem)item).setSelected(true);
                } else {
                    if (itemInControlCount > 0) item = popup.getItems().get(0);
                }

                // This is a fix for RT-9071. Ideally this won't be necessary in
                // the long-run, but for now at least this resolves the
                // positioning
                // problem of ChoiceBox inside a Cell.
                getSkinnable().autosize();
                // -- End of RT-9071 fix

                double y = 0f;
                // TODO without this, the choicebox won't shift vertically..
//                if (item != null) y = -item.prefHeight(-1) - item.getLayoutY();

                if (popup.getSkin() != null) {
                    ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
                    if (cmContent != null && currentSelectedIndex != -1) {
                        y = -(cmContent.getMenuYOffset((int)currentSelectedIndex));
                    }
                }
                // TODO will need to do this, but for now, if I do this, then the
                // choice box changes size when the popup is shown
                // popup.setWidth(getWidth());
                
                popup.show(getSkinnable(), Side.BOTTOM, 2, y);
            } else {
                popup.hide();
            }
        } else if ("CONVERTER".equals(p)) {
            updateChoiceBoxItems();
            updatePopupItems();
        }
    }

    private void addPopupItem(Object o, int i) {
        MenuItem popupItem = null;
        if (o instanceof Separator) {
            // We translate the Separator into a SeparatorMenuItem...
            popupItem = new SeparatorMenuItem();
        } else if (o instanceof SeparatorMenuItem) {
            popupItem = (SeparatorMenuItem) o;
        } else {
            StringConverter c = getSkinnable().getConverter();
            String displayString = (c == null) ? ((o == null) ? "" : o.toString()) :  c.toString(o);
            final RadioMenuItem item = new RadioMenuItem(displayString);
            item.setId("choice-box-menu-item");
            item.setToggleGroup(toggleGroup);
            final int index = i;
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    if (selectionModel == null) return;
                    selectionModel.select(index);
                    item.setSelected(true);
                }
            });
            popupItem = item;
        }
        popupItem.setMnemonicParsing(false);   // ChoiceBox doesn't do Mnemonics
        popup.getItems().add(i, popupItem);
    }

    private void updatePopupItems() {
        popup.getItems().clear();
        toggleGroup.selectToggle(null);

        for (int i = 0; i < choiceBoxItems.size(); i++) {
            Object o = choiceBoxItems.get(i);
            addPopupItem(o, i);
        }
    }

    private void updateSelectionModel() {
        if (selectionModel != null) {
            selectionModel.selectedIndexProperty().removeListener(selectionChangeListener);
        }
        this.selectionModel = getSkinnable().getSelectionModel();
        if (selectionModel != null) {
            selectionModel.selectedIndexProperty().addListener(selectionChangeListener);
        }
    }

    private InvalidationListener selectionChangeListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            updateSelection();
        }
    };

    private void updateSelection() {
        if (selectionModel == null || selectionModel.isEmpty()) {
            toggleGroup.selectToggle(null);
            label.setText("");
        } else {
            int selectedIndex = selectionModel.getSelectedIndex();
            if (selectedIndex == -1 || selectedIndex > popup.getItems().size()) {
                label.setText(""); // clear label text
                return;
            }
            if (selectedIndex < popup.getItems().size()) {
                MenuItem selectedItem = popup.getItems().get(selectedIndex);
                if (selectedItem instanceof RadioMenuItem) {
                    ((RadioMenuItem) selectedItem).setSelected(true);
                    toggleGroup.selectToggle(null);
                }
                // update the label
                label.setText(popup.getItems().get(selectedIndex).getText());
            }
        }
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        // open button width/height
        double obw = openButton.prefWidth(-1);

        label.resizeRelocate(getInsets().getLeft(), getInsets().getTop(), w, h);
        openButton.resize(obw, openButton.prefHeight(-1));
        positionInArea(openButton, getWidth() - getInsets().getRight() - obw,
                getInsets().getTop(), obw, h, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    @Override protected double computeMinWidth(double height) {
        final double boxWidth = label.minWidth(-1) + openButton.minWidth(-1);
        final double popupWidth = popup.minWidth(-1);
        return getInsets().getLeft() + Math.max(boxWidth, popupWidth)
                + getInsets().getRight();
    }

    @Override protected double computeMinHeight(double width) {
        final double displayHeight = label.minHeight(-1);
        final double openButtonHeight = openButton.minHeight(-1);
        return getInsets().getTop()
                + Math.max(displayHeight, openButtonHeight)
                + getInsets().getBottom();
    }

    @Override protected double computePrefWidth(double height) {
        final double boxWidth = label.prefWidth(-1)
                + openButton.prefWidth(-1);
        double popupWidth = popup.prefWidth(-1);
        if (popupWidth <= 0) { // first time: when the popup has not shown yet
            if (popup.getItems().size() > 0){
                popupWidth = (new Text(((MenuItem)popup.getItems().get(0)).getText())).prefWidth(-1);
            }
        }
        return (popup.getItems().size() == 0) ? 50 : getInsets().getLeft() + Math.max(boxWidth, popupWidth)
                + getInsets().getRight();
    }

    @Override protected double computePrefHeight(double width) {
        final double displayHeight = label.prefHeight(-1);
        final double openButtonHeight = openButton.prefHeight(-1);
        return getInsets().getTop()
                + Math.max(displayHeight, openButtonHeight)
                + getInsets().getBottom();
    }
    
    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }
    
    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }
}
