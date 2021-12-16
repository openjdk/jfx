/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.scene.control.Properties;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.WeakListChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import com.sun.javafx.scene.control.behavior.ListViewBehavior;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.javafx.scene.control.skin.resources.ControlResources;

/**
 * Default skin implementation for the {@link ListView} control.
 *
 * @see ListView
 * @since 9
 */
public class ListViewSkin<T> extends VirtualContainerBase<ListView<T>, ListCell<T>> {

    /* *************************************************************************
     *                                                                         *
     * Static Fields                                                           *
     *                                                                         *
     **************************************************************************/

    // RT-34744 : IS_PANNABLE will be false unless
    // javafx.scene.control.skin.ListViewSkin.pannable
    // is set to true. This is done in order to make ListView functional
    // on embedded systems with touch screens which do not generate scroll
    // events for touch drag gestures.
    @SuppressWarnings("removal")
    private static final boolean IS_PANNABLE =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.scene.control.skin.ListViewSkin.pannable"));



    /* *************************************************************************
     *                                                                         *
     * Internal Fields                                                         *
     *                                                                         *
     **************************************************************************/

    // JDK-8090129: This constant should not be static, because the
    // Locale may change between instances.
    private static final String EMPTY_LIST_TEXT = ControlResources.getString("ListView.noContent");

    private final VirtualFlow<ListCell<T>> flow;

    /**
     * Region placed over the top of the flow (and possibly the header row) if
     * there is no data.
     */
    // FIXME this should not be a StackPane
    private StackPane placeholderRegion;
    private Node placeholderNode;

    private ObservableList<T> listViewItems;

    private boolean needCellsRebuilt = true;
    private boolean needCellsReconfigured = false;

    private int itemCount = -1;
    private ListViewBehavior<T> behavior;



    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private MapChangeListener<Object, Object> propertiesMapListener = c -> {
        if (! c.wasAdded()) return;
        if (Properties.RECREATE.equals(c.getKey())) {
            needCellsRebuilt = true;
            getSkinnable().requestLayout();
            getSkinnable().getProperties().remove(Properties.RECREATE);
        }
    };

    private WeakMapChangeListener<Object, Object> weakPropertiesMapListener =
            new WeakMapChangeListener<>(propertiesMapListener);

    private final ListChangeListener<T> listViewItemsListener = new ListChangeListener<T>() {
        @Override public void onChanged(Change<? extends T> c) {
            while (c.next()) {
                if (c.wasReplaced()) {
                    // RT-28397: Support for when an item is replaced with itself (but
                    // updated internal values that should be shown visually).
                    // This code was updated for RT-36714 to not update all cells,
                    // just those affected by the change
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        flow.setCellDirty(i);
                    }

                    break;
                } else if (c.getRemovedSize() == itemCount) {
                    // RT-22463: If the user clears out an items list then we
                    // should reset all cells (in particular their contained
                    // items) such that a subsequent addition to the list of
                    // an item which equals the old item (but is rendered
                    // differently) still displays as expected (i.e. with the
                    // updated display, not the old display).
                    itemCount = 0;
                    break;
                }
            }

            // fix for RT-37853
            getSkinnable().edit(-1);

            markItemCountDirty();
            getSkinnable().requestLayout();
        }
    };

    private final WeakListChangeListener<T> weakListViewItemsListener =
            new WeakListChangeListener<T>(listViewItemsListener);


    private final InvalidationListener itemsChangeListener = observable -> updateListViewItems();

    private WeakInvalidationListener
                weakItemsChangeListener = new WeakInvalidationListener(itemsChangeListener);

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ListViewSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ListViewSkin(final ListView<T> control) {
        super(control);

        // install default input map for the ListView control
        behavior = new ListViewBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

        // init the behavior 'closures'
        behavior.setOnFocusPreviousRow(() -> onFocusPreviousCell());
        behavior.setOnFocusNextRow(() -> onFocusNextCell());
        behavior.setOnMoveToFirstCell(() -> onMoveToFirstCell());
        behavior.setOnMoveToLastCell(() -> onMoveToLastCell());
        behavior.setOnSelectPreviousRow(() -> onSelectPreviousCell());
        behavior.setOnSelectNextRow(() -> onSelectNextCell());
        behavior.setOnScrollPageDown(this::onScrollPageDown);
        behavior.setOnScrollPageUp(this::onScrollPageUp);

        updateListViewItems();

        // init the VirtualFlow
        flow = getVirtualFlow();
        flow.setId("virtual-flow");
        flow.setPannable(IS_PANNABLE);
        flow.setVertical(control.getOrientation() == Orientation.VERTICAL);
        flow.setCellFactory(flow -> createCell());
        flow.setFixedCellSize(control.getFixedCellSize());
        getChildren().add(flow);

        EventHandler<MouseEvent> ml = event -> {
            // This ensures that the list maintains the focus, even when the vbar
            // and hbar controls inside the flow are clicked. Without this, the
            // focus border will not be shown when the user interacts with the
            // scrollbars, and more importantly, keyboard navigation won't be
            // available to the user.
            if (control.isFocusTraversable()) {
                control.requestFocus();
            }
        };
        flow.getVbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);
        flow.getHbar().addEventFilter(MouseEvent.MOUSE_PRESSED, ml);

        updateItemCount();

        control.itemsProperty().addListener(weakItemsChangeListener);

        final ObservableMap<Object, Object> properties = control.getProperties();
        properties.remove(Properties.RECREATE);
        properties.addListener(weakPropertiesMapListener);

        // Register listeners
        registerChangeListener(control.itemsProperty(), o -> updateListViewItems());
        registerChangeListener(control.orientationProperty(), o ->
            flow.setVertical(control.getOrientation() == Orientation.VERTICAL)
        );
        registerChangeListener(control.cellFactoryProperty(), o -> flow.recreateCells());
        registerChangeListener(control.parentProperty(), o -> {
            if (control.getParent() != null && control.isVisible()) {
                control.requestLayout();
            }
        });
        registerChangeListener(control.placeholderProperty(), o -> updatePlaceholderRegionVisibility());
        registerChangeListener(control.fixedCellSizeProperty(), o ->
            flow.setFixedCellSize(control.getFixedCellSize())
        );
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        if (getSkinnable() == null) return;
        // listener cleanup fixes side-effects (NPE on refresh, setItems, modifyItems)
        getSkinnable().getProperties().removeListener(weakPropertiesMapListener);
        getSkinnable().itemsProperty().removeListener(weakItemsChangeListener);
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
            listViewItems = null;
        }
        getChildren().remove(flow);
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        super.layoutChildren(x, y, w, h);

        if (needCellsRebuilt) {
            flow.rebuildCells();
        } else if (needCellsReconfigured) {
            flow.reconfigureCells();
        }

        needCellsRebuilt = false;
        needCellsReconfigured = false;

        if (getItemCount() == 0) {
            // show message overlay instead of empty listview
            if (placeholderRegion != null) {
                placeholderRegion.setVisible(w > 0 && h > 0);
                placeholderRegion.resizeRelocate(x, y, w, h);
            }
        } else {
            flow.resizeRelocate(x, y, w, h);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        checkState();

        if (getItemCount() == 0) {
            if (placeholderRegion == null) {
                updatePlaceholderRegionVisibility();
            }
            if (placeholderRegion != null) {
                return placeholderRegion.prefWidth(height) + leftInset + rightInset;
            }
        }

        return computePrefHeight(-1, topInset, rightInset, bottomInset, leftInset) * 0.618033987;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return 400;
    }

    /** {@inheritDoc} */
    @Override protected int getItemCount() {
        return itemCount;
    }

    /** {@inheritDoc} */
    @Override protected void updateItemCount() {
        if (flow == null) return;

        int oldCount = itemCount;
        int newCount = listViewItems == null ? 0 : listViewItems.size();

        itemCount = newCount;

        flow.setCellCount(newCount);

        updatePlaceholderRegionVisibility();
        if (newCount == oldCount) {
            needCellsReconfigured = true;
        } else if (oldCount == 0) {
            requestRebuildCells();
        }
    }

    /** {@inheritDoc} */
    @Override protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case FOCUS_ITEM: {
                FocusModel<?> fm = getSkinnable().getFocusModel();
                int focusedIndex = fm.getFocusedIndex();
                if (focusedIndex == -1) {
                    if (placeholderRegion != null && placeholderRegion.isVisible()) {
                        return placeholderRegion.getChildren().get(0);
                    }
                    if (getItemCount() > 0) {
                        focusedIndex = 0;
                    } else {
                        return null;
                    }
                }
                return flow.getPrivateCell(focusedIndex);
            }
            case ITEM_COUNT: return getItemCount();
            case ITEM_AT_INDEX: {
                Integer rowIndex = (Integer)parameters[0];
                if (rowIndex == null) return null;
                if (0 <= rowIndex && rowIndex < getItemCount()) {
                    return flow.getPrivateCell(rowIndex);
                }
                return null;
            }
            case SELECTED_ITEMS: {
                MultipleSelectionModel<T> sm = getSkinnable().getSelectionModel();
                ObservableList<Integer> indices = sm.getSelectedIndices();
                List<Node> selection = new ArrayList<>(indices.size());
                for (int i : indices) {
                    ListCell<T> row = flow.getPrivateCell(i);
                    if (row != null) selection.add(row);
                }
                return FXCollections.observableArrayList(selection);
            }
            case VERTICAL_SCROLLBAR: return flow.getVbar();
            case HORIZONTAL_SCROLLBAR: return flow.getHbar();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override protected void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case SHOW_ITEM: {
                Node item = (Node)parameters[0];
                if (item instanceof ListCell) {
                    @SuppressWarnings("unchecked")
                    ListCell<T> cell = (ListCell<T>)item;
                    flow.scrollTo(cell.getIndex());
                }
                break;
            }
            case SET_SELECTED_ITEMS: {
                @SuppressWarnings("unchecked")
                ObservableList<Node> items = (ObservableList<Node>)parameters[0];
                if (items != null) {
                    MultipleSelectionModel<T> sm = getSkinnable().getSelectionModel();
                    if (sm != null) {
                        sm.clearSelection();
                        for (Node item : items) {
                            if (item instanceof ListCell) {
                                @SuppressWarnings("unchecked")
                                ListCell<T> cell = (ListCell<T>)item;
                                sm.select(cell.getIndex());
                            }
                        }
                    }
                }
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    private ListCell<T> createCell() {
        ListCell<T> cell;
        if (getSkinnable().getCellFactory() != null) {
            cell = getSkinnable().getCellFactory().call(getSkinnable());
        } else {
            cell = createDefaultCellImpl();
        }

        cell.updateListView(getSkinnable());

        return cell;
    }

    private void updateListViewItems() {
        if (listViewItems != null) {
            listViewItems.removeListener(weakListViewItemsListener);
        }

        this.listViewItems = getSkinnable().getItems();

        if (listViewItems != null) {
            listViewItems.addListener(weakListViewItemsListener);
        }

        markItemCountDirty();
        getSkinnable().requestLayout();
    }

    private final void updatePlaceholderRegionVisibility() {
        boolean visible = getItemCount() == 0;

        if (visible) {
            placeholderNode = getSkinnable().getPlaceholder();
            if (placeholderNode == null && (EMPTY_LIST_TEXT != null && ! EMPTY_LIST_TEXT.isEmpty())) {
                placeholderNode = new Label();
                ((Label)placeholderNode).setText(EMPTY_LIST_TEXT);
            }

            if (placeholderNode != null) {
                if (placeholderRegion == null) {
                    placeholderRegion = new StackPane();
                    placeholderRegion.getStyleClass().setAll("placeholder");
                    getChildren().add(placeholderRegion);
                }

                placeholderRegion.getChildren().setAll(placeholderNode);
            }
        }

        flow.setVisible(!visible);
        if (placeholderRegion != null) {
            placeholderRegion.setVisible(visible);
        }
    }

    private static <T> ListCell<T> createDefaultCellImpl() {
        return new ListCell<T>() {
            @Override public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof Node) {
                    setText(null);
                    Node currentNode = getGraphic();
                    Node newNode = (Node) item;
                    if (currentNode == null || ! currentNode.equals(newNode)) {
                        setGraphic(newNode);
                    }
                } else {
                    /**
                     * This label is used if the item associated with this cell is to be
                     * represented as a String. While we will lazily instantiate it
                     * we never clear it, being more afraid of object churn than a minor
                     * "leak" (which will not become a "major" leak).
                     */
                    setText(item == null ? "null" : item.toString());
                    setGraphic(null);
                }
            }
        };
    }

    private void onFocusPreviousCell() {
        FocusModel<T> fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.scrollTo(fm.getFocusedIndex());
    }

    private void onFocusNextCell() {
        FocusModel<T> fm = getSkinnable().getFocusModel();
        if (fm == null) return;
        flow.scrollTo(fm.getFocusedIndex());
    }

    private void onSelectPreviousCell() {
        SelectionModel<T> sm = getSkinnable().getSelectionModel();
        if (sm == null) return;

        int pos = sm.getSelectedIndex();
        flow.scrollTo(pos);

        // Fix for RT-11299
        IndexedCell<T> cell = flow.getFirstVisibleCell();
        if (cell == null || pos < cell.getIndex()) {
            flow.setPosition(pos / (double) getItemCount());
        }
    }

    private void onSelectNextCell() {
        SelectionModel<T> sm = getSkinnable().getSelectionModel();
        if (sm == null) return;

        int pos = sm.getSelectedIndex();
        flow.scrollTo(pos);

        // Fix for RT-11299
        ListCell<T> cell = flow.getLastVisibleCell();
        if (cell == null || cell.getIndex() < pos) {
            flow.setPosition(pos / (double) getItemCount());
        }
    }

    private void onMoveToFirstCell() {
        flow.scrollTo(0);
        flow.setPosition(0);
    }

    private void onMoveToLastCell() {
//        SelectionModel sm = getSkinnable().getSelectionModel();
//        if (sm == null) return;
//
        int endPos = getItemCount() - 1;
//        sm.select(endPos);
        flow.scrollTo(endPos);
        flow.setPosition(1);
    }

    /**
     * Function used to scroll the container down by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the right.
     */
    private int onScrollPageDown(boolean isFocusDriven) {
        ListCell<T> lastVisibleCell = flow.getLastVisibleCellWithinViewport();
        if (lastVisibleCell == null) return -1;

        final SelectionModel<T> sm = getSkinnable().getSelectionModel();
        final FocusModel<T> fm = getSkinnable().getFocusModel();
        if (sm == null || fm == null) return -1;

        int lastVisibleCellIndex = lastVisibleCell.getIndex();

//        boolean isSelected = sm.isSelected(lastVisibleCellIndex) || fm.isFocused(lastVisibleCellIndex) || lastVisibleCellIndex == anchor;
        // isSelected represents focus OR selection
        boolean isSelected = false;
        if (isFocusDriven) {
            isSelected = lastVisibleCell.isFocused() || fm.isFocused(lastVisibleCellIndex);
        } else {
            isSelected = lastVisibleCell.isSelected() || sm.isSelected(lastVisibleCellIndex);
        }

        if (isSelected) {
            boolean isLeadIndex = (isFocusDriven && fm.getFocusedIndex() == lastVisibleCellIndex)
                               || (! isFocusDriven && sm.getSelectedIndex() == lastVisibleCellIndex);

            if (isLeadIndex) {
                // if the last visible cell is selected, we want to shift that cell up
                // to be the top-most cell, or at least as far to the top as we can go.
                flow.scrollToTop(lastVisibleCell);

                ListCell<T> newLastVisibleCell = flow.getLastVisibleCellWithinViewport();
                lastVisibleCell = newLastVisibleCell == null ? lastVisibleCell : newLastVisibleCell;
            }
        } else {
            // if the selection is not on the 'bottom' most cell, we firstly move
            // the selection down to that, without scrolling the contents, so
            // this is a no-op
        }

        int newSelectionIndex = lastVisibleCell.getIndex();
        flow.scrollTo(lastVisibleCell);
        return newSelectionIndex;
    }

    /**
     * Function used to scroll the container up by one 'page', although
     * if this is a horizontal container, then the scrolling will be to the left.
     */
    private int onScrollPageUp(boolean isFocusDriven) {
        ListCell<T> firstVisibleCell = flow.getFirstVisibleCellWithinViewport();
        if (firstVisibleCell == null) return -1;

        final SelectionModel<T> sm = getSkinnable().getSelectionModel();
        final FocusModel<T> fm = getSkinnable().getFocusModel();
        if (sm == null || fm == null) return -1;

        int firstVisibleCellIndex = firstVisibleCell.getIndex();

        // isSelected represents focus OR selection
        boolean isSelected = false;
        if (isFocusDriven) {
            isSelected = firstVisibleCell.isFocused() || fm.isFocused(firstVisibleCellIndex);
        } else {
            isSelected = firstVisibleCell.isSelected() || sm.isSelected(firstVisibleCellIndex);
        }

        if (isSelected) {
            boolean isLeadIndex = (isFocusDriven && fm.getFocusedIndex() == firstVisibleCellIndex)
                               || (! isFocusDriven && sm.getSelectedIndex() == firstVisibleCellIndex);

            if (isLeadIndex) {
                // if the first visible cell is selected, we want to shift that cell down
                // to be the bottom-most cell, or at least as far to the bottom as we can go.
                flow.scrollToBottom(firstVisibleCell);

                ListCell<T> newFirstVisibleCell = flow.getFirstVisibleCellWithinViewport();
                firstVisibleCell = newFirstVisibleCell == null ? firstVisibleCell : newFirstVisibleCell;
            }
        } else {
            // if the selection is not on the 'top' most cell, we firstly move
            // the selection up to that, without scrolling the contents, so
            // this is a no-op
        }

        int newSelectionIndex = firstVisibleCell.getIndex();
        flow.scrollTo(firstVisibleCell);
        return newSelectionIndex;
    }
}
