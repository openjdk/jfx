/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import com.sun.javafx.scene.control.behavior.ComboBoxListViewBehavior;

import java.util.List;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Default skin implementation for the {@link ComboBox} control.
 *
 * @see ComboBox
 * @since 9
 */
public class ComboBoxListViewSkin<T> extends ComboBoxPopupControl<T> {

    /***************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/

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
    private ObservableList<T> comboBoxItems;

    private ListCell<T> buttonCell;
    private Callback<ListView<T>, ListCell<T>> cellFactory;

    private final ListView<T> listView;
    private ObservableList<T> listViewItems;

    private boolean listSelectionLock = false;
    private boolean listViewSelectionDirty = false;

    private final ComboBoxListViewBehavior behavior;



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private boolean itemCountDirty;
    private final ListChangeListener<T> listViewItemsListener = new ListChangeListener<T>() {
        @Override public void onChanged(ListChangeListener.Change<? extends T> c) {
            itemCountDirty = true;
            getSkinnable().requestLayout();
        }
    };

    private final InvalidationListener itemsObserver;

    private final WeakListChangeListener<T> weakListViewItemsListener =
            new WeakListChangeListener<T>(listViewItemsListener);


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ComboBoxListViewSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ComboBoxListViewSkin(final ComboBox<T> control) {
        super(control);

        // install default input map for the control
        this.behavior = new ComboBoxListViewBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

        this.comboBox = control;
        updateComboBoxItems();

        itemsObserver = observable -> {
            updateComboBoxItems();
            updateListViewItems();
        };
        control.itemsProperty().addListener(new WeakInvalidationListener(itemsObserver));

        // listview for popup
        this.listView = createListView();

        // Fix for RT-21207. Additional code related to this bug is further below.
        this.listView.setManaged(false);
        getChildren().add(listView);
        // -- end of fix

        updateListViewItems();
        updateCellFactory();

        updateButtonCell();

        // Fix for RT-19431 (also tested via ComboBoxListViewSkinTest)
        updateValue();

        registerChangeListener(control.itemsProperty(), e -> {
            updateComboBoxItems();
            updateListViewItems();
        });
        registerChangeListener(control.promptTextProperty(), e -> updateDisplayNode());
        registerChangeListener(control.cellFactoryProperty(), e -> updateCellFactory());
        registerChangeListener(control.visibleRowCountProperty(), e -> {
            if (listView == null) return;
            listView.requestLayout();
        });
        registerChangeListener(control.converterProperty(), e -> updateListViewItems());
        registerChangeListener(control.buttonCellProperty(), e -> {
            updateButtonCell();
            updateDisplayArea();
        });
        registerChangeListener(control.valueProperty(), e -> {
            updateValue();
            control.fireEvent(new ActionEvent());
        });
        registerChangeListener(control.editableProperty(), e -> updateEditable());

        // Refer to JDK-8095306
        if (comboBox.isShowing()) {
            show();
        }
        comboBox.sceneProperty().addListener(o -> {
            if (((ObservableValue)o).getValue() == null) {
                comboBox.hide();
            }
        });
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * By default this skin hides the popup whenever the ListView is clicked in.
     * By setting hideOnClick to false, the popup will not be hidden when the
     * ListView is clicked in. This is beneficial in some scenarios (for example,
     * when the ListView cells have checkboxes).
     */
    // --- hide on click
    private final BooleanProperty hideOnClick = new SimpleBooleanProperty(this, "hideOnClick", true);
    public final BooleanProperty hideOnClickProperty() {
        return hideOnClick;
    }
    public final boolean isHideOnClick() {
        return hideOnClick.get();
    }
    public final void setHideOnClick(boolean value) {
        hideOnClick.set(value);
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected TextField getEditor() {
        // Return null if editable is false, even if the ComboBox has an editor set.
        // Use getSkinnable() here because this method is called from the super
        // constructor before comboBox is initialized.
        return getSkinnable().isEditable() ? ((ComboBox)getSkinnable()).getEditor() : null;
    }

    /** {@inheritDoc} */
    @Override protected StringConverter<T> getConverter() {
        return ((ComboBox)getSkinnable()).getConverter();
    }

    /** {@inheritDoc} */
    @Override public Node getDisplayNode() {
        Node displayNode;
        if (comboBox.isEditable()) {
            displayNode = getEditableInputNode();
        } else {
            displayNode = buttonCell;
        }

        updateDisplayNode();

        return displayNode;
    }

    /** {@inheritDoc} */
    @Override public Node getPopupContent() {
        return listView;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        reconfigurePopup();
        return 50;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double superPrefWidth = super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        double listViewWidth = listView.prefWidth(height);
        double pw = Math.max(superPrefWidth, listViewWidth);

        reconfigurePopup();

        return pw;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        reconfigurePopup();
        return super.computeMaxWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        reconfigurePopup();
        return super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        reconfigurePopup();
        return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        reconfigurePopup();
        return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
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

        super.layoutChildren(x, y, w, h);
    }



    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void updateDisplayNode() {
        if (getEditor() != null) {
            super.updateDisplayNode();
        } else {
            T value = comboBox.getValue();
            int index = getIndexOfComboBoxValueInItemsList();
            if (index > -1) {
                buttonCell.setItem(null);
                buttonCell.updateIndex(index);
            } else {
                // RT-21336 Show the ComboBox value even though it doesn't
                // exist in the ComboBox items list (part two of fix)
                buttonCell.updateIndex(-1);
                boolean empty = updateDisplayText(buttonCell, value, false);

                // Note that empty boolean collected above. This is used to resolve
                // RT-27834, where we were getting different styling based on whether
                // the cell was updated via the updateIndex method above, or just
                // by directly updating the text. We fake the pseudoclass state
                // for empty, filled, and selected here.
                buttonCell.pseudoClassStateChanged(PSEUDO_CLASS_EMPTY,    empty);
                buttonCell.pseudoClassStateChanged(PSEUDO_CLASS_FILLED,   !empty);
                buttonCell.pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, true);
            }
        }
    }

    /** {@inheritDoc} */
    @Override ComboBoxBaseBehavior getBehavior() {
        return behavior;
    }

    private void updateComboBoxItems() {
        comboBoxItems = comboBox.getItems();
        comboBoxItems = comboBoxItems == null ? FXCollections.<T>emptyObservableList() : comboBoxItems;
    }

    private void updateListViewItems() {
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
        }

        this.listViewItems = comboBoxItems;
        listView.setItems(listViewItems);

        if (listViewItems != null) {
            listViewItems.addListener(weakListViewItemsListener);
        }

        itemCountDirty = true;
        getSkinnable().requestLayout();
    }

    private void updateValue() {
        T newValue = comboBox.getValue();

        SelectionModel<T> listViewSM = listView.getSelectionModel();

        // RT-22386: We need to test to see if the value is in the comboBox
        // items list. If it isn't, then we should clear the listview
        // selection
        final int indexOfNewValue = getIndexOfComboBoxValueInItemsList();

        if (newValue == null && indexOfNewValue == -1) {
            listViewSM.clearSelection();
        } else {
            if (indexOfNewValue == -1) {
                listSelectionLock = true;
                listViewSM.clearSelection();
                listSelectionLock = false;
            } else {
                int index = comboBox.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < comboBoxItems.size()) {
                    T itemsObj = comboBoxItems.get(index);
                    if ((itemsObj != null && itemsObj.equals(newValue)) || (itemsObj == null && newValue == null)) {
                        listViewSM.select(index);
                    } else {
                        listViewSM.select(newValue);
                    }
                } else {
                    // just select the first instance of newValue in the list
                    int listViewIndex = comboBoxItems.indexOf(newValue);
                    if (listViewIndex == -1) {
                        // RT-21336 Show the ComboBox value even though it doesn't
                        // exist in the ComboBox items list (part one of fix)
                        updateDisplayNode();
                    } else {
                        listViewSM.select(listViewIndex);
                    }
                }
            }
        }
    }

    // return a boolean to indicate that the cell is empty (and therefore not filled)
    private boolean updateDisplayText(ListCell<T> cell, T item, boolean empty) {
        if (empty) {
            if (cell == null) return true;
            cell.setGraphic(null);
            cell.setText(null);
            return true;
        } else if (item instanceof Node) {
            Node currentNode = cell.getGraphic();
            Node newNode = (Node) item;
            if (currentNode == null || ! currentNode.equals(newNode)) {
                cell.setText(null);
                cell.setGraphic(newNode);
            }
            return newNode == null;
        } else {
            // run item through StringConverter if it isn't null
            final StringConverter<T> c = comboBox.getConverter();
            final String promptText = comboBox.getPromptText();
            String s = item == null && promptText != null ? promptText :
                       c == null ? (item == null ? null : item.toString()) : c.toString(item);
            cell.setText(s);
            cell.setGraphic(null);
            return s == null || s.isEmpty();
        }
    }

    private int getIndexOfComboBoxValueInItemsList() {
        T value = comboBox.getValue();
        int index = comboBoxItems.indexOf(value);
        return index;
    }

    private void updateButtonCell() {
        buttonCell = comboBox.getButtonCell() != null ?
                comboBox.getButtonCell() : getDefaultCellFactory().call(listView);
        buttonCell.setMouseTransparent(true);
        buttonCell.updateListView(listView);

        // As long as the screen-reader is concerned this node is not a list item.
        // This matters because the screen-reader counts the number of list item
        // within combo and speaks it to the user.
        buttonCell.setAccessibleRole(AccessibleRole.NODE);
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
        final ListView<T> _listView = new ListView<T>() {

            {
                getProperties().put("selectFirstRowByDefault", false);
                // editableComboBox property is used to intercept few Key inputs from this ListView,
                // so that those inputs get forwarded to editor of ComboBox .
                getProperties().put("editableComboBox", (Supplier<Boolean>) () -> getSkinnable().isEditable());
            }

            @Override protected double computeMinHeight(double width) {
                return 30;
            }

            @Override protected double computePrefWidth(double height) {
                double pw;
                if (getSkin() instanceof ListViewSkin) {
                    ListViewSkin<?> skin = (ListViewSkin<?>)getSkin();
                    if (itemCountDirty) {
                        skin.updateItemCount();
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

                // need to check the ListView pref height in the case that the
                // placeholder node is showing
                if (getItems().isEmpty() && getPlaceholder() != null) {
                    pw = Math.max(super.computePrefWidth(height), pw);
                }

                return Math.max(50, pw);
            }

            @Override protected double computePrefHeight(double width) {
                return getListViewPrefHeight();
            }
        };

        _listView.setId("list-view");
        _listView.placeholderProperty().bind(comboBox.placeholderProperty());
        _listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        _listView.setFocusTraversable(false);

        _listView.getSelectionModel().selectedIndexProperty().addListener(o -> {
            if (listSelectionLock) return;
            int index = listView.getSelectionModel().getSelectedIndex();
            comboBox.getSelectionModel().select(index);
            updateDisplayNode();
            comboBox.notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
        });

        comboBox.getSelectionModel().selectedItemProperty().addListener(o -> {
            listViewSelectionDirty = true;
        });

        _listView.addEventFilter(MouseEvent.MOUSE_RELEASED, t -> {
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

            if (isHideOnClick()) {
                comboBox.hide();
            }
        });

        _listView.setOnKeyPressed(t -> {
            // TODO move to behavior, when (or if) this class becomes a SkinBase
            if (t.getCode() == KeyCode.ENTER ||
                    t.getCode() == KeyCode.SPACE ||
                    t.getCode() == KeyCode.ESCAPE) {
                comboBox.hide();
            }
        });

        return _listView;
    }

    private double getListViewPrefHeight() {
        double ph;
        if (listView.getSkin() instanceof VirtualContainerBase) {
            int maxRows = comboBox.getVisibleRowCount();
            VirtualContainerBase<?,?> skin = (VirtualContainerBase<?,?>)listView.getSkin();
            ph = skin.getVirtualFlowPreferredHeight(maxRows);
        } else {
            double ch = comboBoxItems.size() * 25;
            ph = Math.min(ch, 200);
        }

        return ph;
    }



    /**************************************************************************
     *
     * API for testing
     *
     *************************************************************************/

    ListView<T> getListView() {
        return listView;
    }




    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    // These three pseudo class states are duplicated from Cell
    private static final PseudoClass PSEUDO_CLASS_SELECTED =
            PseudoClass.getPseudoClass("selected");
    private static final PseudoClass PSEUDO_CLASS_EMPTY =
            PseudoClass.getPseudoClass("empty");
    private static final PseudoClass PSEUDO_CLASS_FILLED =
            PseudoClass.getPseudoClass("filled");


    /** {@inheritDoc} */
    @Override public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case FOCUS_ITEM: {
                if (comboBox.isShowing()) {
                    /* On Mac, for some reason, changing the selection on the list is not
                     * reported by VoiceOver the first time it shows.
                     * Note that this fix returns a child of the PopupWindow back to the main
                     * Stage, which doesn't seem to cause problems.
                     */
                    return listView.queryAccessibleAttribute(attribute, parameters);
                }
                return null;
            }
            case TEXT: {
                String accText = comboBox.getAccessibleText();
                if (accText != null && !accText.isEmpty()) return accText;
                String title = comboBox.isEditable() ? getEditor().getText() : buttonCell.getText();
                if (title == null || title.isEmpty()) {
                    title = comboBox.getPromptText();
                }
                return title;
            }
            case SELECTION_START:
                return (getEditor() != null) ? getEditor().getSelection().getStart() : null;
            case SELECTION_END:
                return (getEditor() != null) ? getEditor().getSelection().getEnd() : null;
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}

