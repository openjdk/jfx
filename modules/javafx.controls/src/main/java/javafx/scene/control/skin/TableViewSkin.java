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

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.behavior.TableViewBehavior;

/**
 * Default skin implementation for the {@link TableView} control.
 *
 * @see TableView
 * @since 9
 */
public class TableViewSkin<T> extends TableViewSkinBase<T, T, TableView<T>, TableRow<T>, TableColumn<T, ?>> {

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final TableViewBehavior<T>  behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TableViewSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TableViewSkin(final TableView<T> control) {
        super(control);

        // install default input map for the TableView control
        behavior = new TableViewBehavior<>(control);

        flow.setFixedCellSize(control.getFixedCellSize());
        flow.setCellFactory(flow -> createCell());

        ListenerHelper lh = ListenerHelper.get(this);

        EventHandler<MouseEvent> ml = event -> {
            // This ensures that the table maintains the focus, even when the vbar
            // and hbar controls inside the flow are clicked. Without this, the
            // focus border will not be shown when the user interacts with the
            // scrollbars, and more importantly, keyboard navigation won't be
            // available to the user.
            if (control.isFocusTraversable()) {
                control.requestFocus();
            }
        };
        lh.addEventFilter(flow.getVbar(), MouseEvent.MOUSE_PRESSED, ml);
        lh.addEventFilter(flow.getHbar(), MouseEvent.MOUSE_PRESSED, ml);

        // init the behavior 'closures'
        behavior.setOnFocusPreviousRow(() -> onFocusAboveCell());
        behavior.setOnFocusNextRow(() -> onFocusBelowCell());
        behavior.setOnMoveToFirstCell(() -> onMoveToFirstCell());
        behavior.setOnMoveToLastCell(() -> onMoveToLastCell());
        behavior.setOnScrollPageDown(isFocusDriven -> onScrollPageDown(isFocusDriven));
        behavior.setOnScrollPageUp(isFocusDriven -> onScrollPageUp(isFocusDriven));
        behavior.setOnSelectPreviousRow(() -> onSelectAboveCell());
        behavior.setOnSelectNextRow(() -> onSelectBelowCell());
        behavior.setOnSelectLeftCell(() -> onSelectLeftCell());
        behavior.setOnSelectRightCell(() -> onSelectRightCell());
        behavior.setOnFocusLeftCell(() -> onFocusLeftCell());
        behavior.setOnFocusRightCell(() -> onFocusRightCell());

        lh.addChangeListener(control.fixedCellSizeProperty(), (ev) -> {
            flow.setFixedCellSize(getSkinnable().getFixedCellSize());
        });

        updateItemCount();
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        if (behavior != null) {
            behavior.dispose();
        }

        super.dispose();
    }

    /** {@inheritDoc} */
    @Override public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case SELECTED_ITEMS: {
                List<Node> selection = new ArrayList<>();
                TableViewSelectionModel<T> sm = getSkinnable().getSelectionModel();
                if (sm != null) {
                    for (TablePosition<T,?> pos : sm.getSelectedCells()) {
                        TableRow<T> row = flow.getPrivateCell(pos.getRow());
                        if (row != null) selection.add(row);
                    }
                }
                return FXCollections.observableArrayList(selection);
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override protected void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case SHOW_ITEM: {
                Node item = (Node)parameters[0];
                if (item instanceof TableCell) {
                    @SuppressWarnings("unchecked")
                    TableCell<T, ?> cell = (TableCell<T, ?>)item;
                    flow.scrollTo(cell.getIndex());
                }
                break;
            }
            case SET_SELECTED_ITEMS: {
                @SuppressWarnings("unchecked")
                ObservableList<Node> items = (ObservableList<Node>)parameters[0];
                if (items != null) {
                    TableSelectionModel<T> sm = getSkinnable().getSelectionModel();
                    if (sm != null) {
                        sm.clearSelection();
                        for (Node item : items) {
                            if (item instanceof TableCell) {
                                @SuppressWarnings("unchecked")
                                TableCell<T, ?> cell = (TableCell<T, ?>)item;
                                sm.select(cell.getIndex(), cell.getTableColumn());
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
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    private TableRow<T> createCell() {
        TableRow<T> cell;

        TableView<T> tableView = getSkinnable();
        if (tableView.getRowFactory() != null) {
            cell = tableView.getRowFactory().call(tableView);
        } else {
            cell = new TableRow<>();
        }

        cell.updateTableView(tableView);
        return cell;
    }

    /** {@inheritDoc} */
    @Override protected int getItemCount() {
        TableView<T> tableView = getSkinnable();
        return tableView.getItems() == null ? 0 : tableView.getItems().size();
    }

    /** {@inheritDoc} */
    @Override void horizontalScroll() {
        super.horizontalScroll();
        if (getSkinnable().getFixedCellSize() > 0) {
            flow.requestCellLayout();
        }
    }
}
