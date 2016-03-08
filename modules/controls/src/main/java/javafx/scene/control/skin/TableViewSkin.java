/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import com.sun.javafx.scene.control.behavior.TableViewBehavior;

/**
 * Default skin implementation for the {@link TableView} control.
 *
 * @see TableView
 * @since 9
 */
public class TableViewSkin<T> extends TableViewSkinBase<T, T, TableView<T>, TableRow<T>, TableColumn<T, ?>> {

    /***************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final TableViewBehavior<T>  behavior;



    /***************************************************************************
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
//        control.setInputMap(behavior.getInputMap());

        flow.setFixedCellSize(control.getFixedCellSize());
        flow.setCellFactory(flow -> createCell());

        EventHandler<MouseEvent> ml = event -> {
            // RT-15127: cancel editing on scroll. This is a bit extreme
            // (we are cancelling editing on touching the scrollbars).
            // This can be improved at a later date.
            if (control.getEditingCell() != null) {
                control.edit(-1, null);
            }

            // This ensures that the table maintains the focus, even when the vbar
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

        // init the behavior 'closures'
        behavior.setOnFocusPreviousRow(() -> onFocusPreviousCell());
        behavior.setOnFocusNextRow(() -> onFocusNextCell());
        behavior.setOnMoveToFirstCell(() -> onMoveToFirstCell());
        behavior.setOnMoveToLastCell(() -> onMoveToLastCell());
        behavior.setOnScrollPageDown(isFocusDriven -> onScrollPageDown(isFocusDriven));
        behavior.setOnScrollPageUp(isFocusDriven -> onScrollPageUp(isFocusDriven));
        behavior.setOnSelectPreviousRow(() -> onSelectPreviousCell());
        behavior.setOnSelectNextRow(() -> onSelectNextCell());
        behavior.setOnSelectLeftCell(() -> onSelectLeftCell());
        behavior.setOnSelectRightCell(() -> onSelectRightCell());

        registerChangeListener(control.fixedCellSizeProperty(), e -> flow.setFixedCellSize(getSkinnable().getFixedCellSize()));

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
    @Override public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case SELECTED_ITEMS: {
                List<Node> selection = new ArrayList<>();
                TableViewSelectionModel<T> sm = getSkinnable().getSelectionModel();
                for (TablePosition<T,?> pos : sm.getSelectedCells()) {
                    TableRow<T> row = flow.getPrivateCell(pos.getRow());
                    if (row != null) selection.add(row);
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



    /***************************************************************************
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
            cell = new TableRow<T>();
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

    /** {@inheritDoc} */
    @Override ObservableList<TableColumn<T, ?>> getVisibleLeafColumns() {
        return getSkinnable().getVisibleLeafColumns();
    }

    @Override int getVisibleLeafIndex(TableColumn<T, ?> tc) {
        return getSkinnable().getVisibleLeafIndex(tc);
    }

    @Override TableColumn<T, ?> getVisibleLeafColumn(int col) {
        return getSkinnable().getVisibleLeafColumn(col);
    }

    /** {@inheritDoc} */
    @Override TableViewFocusModel<T> getFocusModel() {
        return getSkinnable().getFocusModel();
    }

    /** {@inheritDoc} */
    @Override TablePosition<T, ?> getFocusedCell() {
        return getSkinnable().getFocusModel().getFocusedCell();
    }

    /** {@inheritDoc} */
    @Override TableSelectionModel<T> getSelectionModel() {
        return getSkinnable().getSelectionModel();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<Callback<TableView<T>, TableRow<T>>> rowFactoryProperty() {
        return getSkinnable().rowFactoryProperty();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<Node> placeholderProperty() {
        return getSkinnable().placeholderProperty();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<ObservableList<T>> itemsProperty() {
        return getSkinnable().itemsProperty();
    }

    /** {@inheritDoc} */
    @Override ObservableList<TableColumn<T, ?>> getColumns() {
        return getSkinnable().getColumns();
    }

    /** {@inheritDoc} */
    @Override BooleanProperty tableMenuButtonVisibleProperty() {
        return getSkinnable().tableMenuButtonVisibleProperty();
    }

    /** {@inheritDoc} */
    @Override ObjectProperty<Callback<ResizeFeaturesBase, Boolean>> columnResizePolicyProperty() {
        return (ObjectProperty<Callback<ResizeFeaturesBase, Boolean>>) (Object) getSkinnable().columnResizePolicyProperty();
    }

    /** {@inheritDoc} */
    @Override ObservableList<TableColumn<T,?>> getSortOrder() {
        return getSkinnable().getSortOrder();
    }

    /** {@inheritDoc} */
    @Override boolean resizeColumn(TableColumn<T, ?> tc, double delta) {
        return getSkinnable().resizeColumn(tc, delta);
    }

    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    /** {@inheritDoc} */
    @Override void resizeColumnToFitContent(TableColumn<T, ?> tc, int maxRows) {
        if (!tc.isResizable()) return;

//        final TableColumn<T, ?> col = tc;
        List<?> items = itemsProperty().get();
        if (items == null || items.isEmpty()) return;

        Callback/*<TableColumn<T, ?>, TableCell<T,?>>*/ cellFactory = tc.getCellFactory();
        if (cellFactory == null) return;

        TableCell<T,?> cell = (TableCell<T, ?>) cellFactory.call(tc);
        if (cell == null) return;

        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put(Properties.DEFER_TO_PARENT_PREF_WIDTH, Boolean.TRUE);

        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
        }

        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        for (int row = 0; row < rows; row++) {
            cell.updateTableColumn(tc);
            cell.updateTableView(getSkinnable());
            cell.updateIndex(row);

            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                getChildren().add(cell);
                cell.applyCss();
                maxWidth = Math.max(maxWidth, cell.prefWidth(-1));
                getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);

        // RT-36855 - take into account the column header text / graphic widths.
        // Magic 10 is to allow for sort arrow to appear without text truncation.
        TableColumnHeader header = getTableHeaderRow().getColumnHeaderFor(tc);
        double headerTextWidth = Utils.computeTextWidth(header.label.getFont(), tc.getText(), -1);
        Node graphic = header.label.getGraphic();
        double headerGraphicWidth = graphic == null ? 0 : graphic.prefWidth(-1) + header.label.getGraphicTextGap();
        double headerWidth = headerTextWidth + headerGraphicWidth + 10 + header.snappedLeftInset() + header.snappedRightInset();
        maxWidth = Math.max(maxWidth, headerWidth);

        // RT-23486
        maxWidth += padding;
        if(getSkinnable().getColumnResizePolicy() == TableView.CONSTRAINED_RESIZE_POLICY) {
            maxWidth = Math.max(maxWidth, tc.getWidth());
        }

        tc.impl_setWidth(maxWidth);
    }
}
