/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.scene.control.ContextMenuContent;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
 * Default skin implementation for the {@link ChoiceBox} control.
 *
 * @see ChoiceBox
 * @since 9
 */
public class ChoiceBoxSkin<T> extends SkinBase<ChoiceBox<T>> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private ObservableList<T> choiceBoxItems;

    private ContextMenu popup;

    // The region that shows the "arrow" box portion
    private StackPane openButton;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    /*
     * Watch for if the user changes the selected index, and if so, we toggle
     * the selection in the toggle group (so the check shows in the right place)
     */
    private SelectionModel<T> selectionModel;

    private Label label;

    private final BehaviorBase<ChoiceBox<T>> behavior;



    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private final ListChangeListener<T> choiceBoxItemsListener = new ListChangeListener<>() {
        @Override public void onChanged(Change<? extends T> c) {
            while (c.next()) {
                if (c.getRemovedSize() > 0 || c.wasPermutated()) {
                    toggleGroup.getToggles().clear();
                    popup.getItems().clear();
                    int i = 0;
                    for (T obj : c.getList()) {
                        addPopupItem(obj, i);
                        i++;
                    }
                } else {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        final T obj = c.getList().get(i);
                        addPopupItem(obj, i);
                    }
                }
            }
            updateSelection();
            getSkinnable().requestLayout(); // RT-18052 resize of choicebox should happen immediately.
        }
    };

    private final WeakListChangeListener<T> weakChoiceBoxItemsListener =
            new WeakListChangeListener<>(choiceBoxItemsListener);

    private final InvalidationListener itemsObserver;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ChoiceBoxSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ChoiceBoxSkin(ChoiceBox<T> control) {
        super(control);

        // install default input map for the ChoiceBox control
        behavior = new ChoiceBoxBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

        initialize();

        itemsObserver = observable -> updateChoiceBoxItems();
        control.itemsProperty().addListener(itemsObserver);

        control.requestLayout();
        registerChangeListener(control.selectionModelProperty(), e -> updateSelectionModel());
        registerChangeListener(control.showingProperty(), e -> {
            if (getSkinnable().isShowing()) {

                SelectionModel<T> sm = getSkinnable().getSelectionModel();
                if (sm == null) return;

                long currentSelectedIndex = sm.getSelectedIndex();

                // This is a fix for RT-9071. Ideally this won't be necessary in
                // the long-run, but for now at least this resolves the
                // positioning
                // problem of ChoiceBox inside a Cell.
                getSkinnable().autosize();
                // -- End of RT-9071 fix

                double y = 0;

                if (popup.getSkin() != null) {
                    ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
                    if (cmContent != null && currentSelectedIndex != -1) {
                        y = -(cmContent.getMenuYOffset((int)currentSelectedIndex));
                    }
                }

                popup.show(getSkinnable(), Side.BOTTOM, 2, y);
            } else {
                popup.hide();
            }
        });
        registerChangeListener(control.itemsProperty(), e -> {
            updateChoiceBoxItems();
            updatePopupItems();
            updateSelectionModel();
            updateSelection();
        });
        registerChangeListener(control.converterProperty(), e -> {
            updateChoiceBoxItems();
            updatePopupItems();
            updateLabelText();
        });
        registerChangeListener(control.valueProperty(), e -> {
            updateLabelText();
        });
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        if (getSkinnable() == null) return;
        // removing itemsObserver fixes NP on setting items
        getSkinnable().itemsProperty().removeListener(itemsObserver);
         // removing the content listener fixes NPE from listener
        if (choiceBoxItems != null) {
            choiceBoxItems.removeListener(weakChoiceBoxItemsListener);
            choiceBoxItems = null;
        }
        // removing the path listener fixes the memory leak on replacing skin
        if (selectionModel != null) {
            selectionModel.selectedIndexProperty().removeListener(selectionChangeListener);
            selectionModel = null;
        }

        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        // open button width/height
        double obw = openButton.prefWidth(-1);

        label.resizeRelocate(x, y, w, h);
        openButton.resize(obw, openButton.prefHeight(-1));
        positionInArea(openButton, (x+w) - obw,
                y, obw, h, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double boxWidth = label.minWidth(-1) + openButton.minWidth(-1);
        final double popupWidth = popup.minWidth(-1);
        return leftInset + Math.max(boxWidth, popupWidth) + rightInset;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double displayHeight = label.minHeight(-1);
        final double openButtonHeight = openButton.minHeight(-1);
        return topInset + Math.max(displayHeight, openButtonHeight) + bottomInset;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double boxWidth = label.prefWidth(-1)
                + openButton.prefWidth(-1);
        double popupWidth = popup.prefWidth(-1);
        if (popupWidth <= 0) { // first time: when the popup has not shown yet
            if (popup.getItems().size() > 0){
                popupWidth = (new Text(popup.getItems().get(0).getText())).prefWidth(-1);
            }
        }
        return (popup.getItems().size() == 0) ? 50 : leftInset + Math.max(boxWidth, popupWidth)
                + rightInset;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double displayHeight = label.prefHeight(-1);
        final double openButtonHeight = openButton.prefHeight(-1);
        return topInset
                + Math.max(displayHeight, openButtonHeight)
                + bottomInset;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

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
        popup.showingProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                getSkinnable().hide();
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
        updateLabelText();
    }

    private void updateLabelText() {
        T value = getSkinnable().getValue();
        label.setText(getDisplayText(value));
    }

    private String getDisplayText(T value) {
        if (getSkinnable().getConverter() != null) {
            return getSkinnable().getConverter().toString(value);
        }
        return value == null ? "" : value.toString();
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

    // Test only purpose
    String getChoiceBoxSelectedText() {
        return label.getText();
    }

    // Test only purpose
    ContextMenu getChoiceBoxPopup() {
        return popup;
    }

    private void addPopupItem(final T o, int i) {
        MenuItem popupItem = null;
        if (o instanceof Separator) {
            // We translate the Separator into a SeparatorMenuItem...
            popupItem = new SeparatorMenuItem();
        } else if (o instanceof SeparatorMenuItem) {
            popupItem = (SeparatorMenuItem) o;
        } else {
            final RadioMenuItem item = new RadioMenuItem(getDisplayText(o));
            item.setId("choice-box-menu-item");
            item.setToggleGroup(toggleGroup);
            item.setOnAction(e -> {
                if (selectionModel == null) return;
                int index = getSkinnable().getItems().indexOf(o);
                selectionModel.select(index);
                item.setSelected(true);
            });
            popupItem = item;
        }
        popupItem.setMnemonicParsing(false);   // ChoiceBox doesn't do Mnemonics
        popup.getItems().add(i, popupItem);
    }

    private void updatePopupItems() {
        toggleGroup.getToggles().clear();
        popup.getItems().clear();
        toggleGroup.selectToggle(null);

        for (int i = 0; i < choiceBoxItems.size(); i++) {
            T o = choiceBoxItems.get(i);
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

    private InvalidationListener selectionChangeListener = observable -> {
        updateSelection();
    };

    private void updateSelection() {
        if (selectionModel == null || selectionModel.isEmpty()) {
            toggleGroup.selectToggle(null);
         } else {
            int selectedIndex = selectionModel.getSelectedIndex();
            if (selectedIndex == -1 || selectedIndex > popup.getItems().size()) {
                // FIXME: when do we get here?
                // and if, shouldn't we unselect the toggles?
                return;
            }
            if (selectedIndex < popup.getItems().size()) {
                MenuItem selectedItem = popup.getItems().get(selectedIndex);
                if (selectedItem instanceof RadioMenuItem) {
                    ((RadioMenuItem) selectedItem).setSelected(true);
                } else {
                    // need to unselect toggles if selectionModel allows a Separator/MenuItem
                    // to be selected
                    toggleGroup.selectToggle(null);
                }
            }
        }
    }
}
