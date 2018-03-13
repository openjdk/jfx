/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.TableColumnBaseHelper;
import com.sun.javafx.scene.control.TreeTableViewBackingList;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ResizeFeaturesBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableFocusModel;
import javafx.scene.control.TablePositionBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;

// NOT PUBLIC API
class TableSkinUtils {

    private TableSkinUtils() { }

    public static boolean resizeColumn(TableViewSkinBase<?,?,?,?,?> tableSkin, TableColumnBase<?,?> tc, double delta) {
        if (!tc.isResizable()) return false;

        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).resizeColumn((TableColumn)tc, delta);
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).resizeColumn((TreeTableColumn)tc, delta);
        }
        return false;
    }

    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    /** {@inheritDoc} */
    public static void resizeColumnToFitContent(TableViewSkinBase<?,?,?,?,?> tableSkin, TableColumnBase<?,?> tc, int maxRows) {
        if (!tc.isResizable()) return;

        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            resizeColumnToFitContent((TableView)control, (TableColumn)tc, tableSkin, maxRows);
        } else if (control instanceof TreeTableView) {
            resizeColumnToFitContent((TreeTableView)control, (TreeTableColumn)tc, tableSkin, maxRows);
        }
    }

    private static <T,S> void resizeColumnToFitContent(TableView<T> tv, TableColumn<T, S> tc, TableViewSkinBase tableSkin, int maxRows) {
        List<?> items = tv.getItems();
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
            cell.updateTableView(tv);
            cell.updateIndex(row);

            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                tableSkin.getChildren().add(cell);
                cell.applyCss();
                maxWidth = Math.max(maxWidth, cell.prefWidth(-1));
                tableSkin.getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);

        // RT-36855 - take into account the column header text / graphic widths.
        // Magic 10 is to allow for sort arrow to appear without text truncation.
        TableColumnHeader header = tableSkin.getTableHeaderRow().getColumnHeaderFor(tc);
        double headerTextWidth = Utils.computeTextWidth(header.label.getFont(), tc.getText(), -1);
        Node graphic = header.label.getGraphic();
        double headerGraphicWidth = graphic == null ? 0 : graphic.prefWidth(-1) + header.label.getGraphicTextGap();
        double headerWidth = headerTextWidth + headerGraphicWidth + 10 + header.snappedLeftInset() + header.snappedRightInset();
        maxWidth = Math.max(maxWidth, headerWidth);

        // RT-23486
        maxWidth += padding;
        if (tv.getColumnResizePolicy() == TableView.CONSTRAINED_RESIZE_POLICY && tv.getWidth() > 0) {

            if (maxWidth > tc.getMaxWidth()) {
                maxWidth = tc.getMaxWidth();
            }

            int size = tc.getColumns().size();
            if (size > 0) {
                resizeColumnToFitContent(tableSkin, tc.getColumns().get(size - 1), maxRows);
                return;
            }

            resizeColumn(tableSkin, tc, Math.round(maxWidth - tc.getWidth()));
        } else {
            TableColumnBaseHelper.setWidth(tc, maxWidth);
        }
    }


    /*
     * FIXME: Naive implementation ahead
     * Attempts to resize column based on the pref width of all items contained
     * in this column. This can be potentially very expensive if the number of
     * rows is large.
     */
    private static <T,S> void resizeColumnToFitContent(TreeTableView<T> ttv, TreeTableColumn<T,S> tc, TableViewSkinBase tableSkin, int maxRows) {
        List<?> items = new TreeTableViewBackingList(ttv);
        if (items == null || items.isEmpty()) return;

        Callback cellFactory = tc.getCellFactory();
        if (cellFactory == null) return;

        TreeTableCell<T,S> cell = (TreeTableCell) cellFactory.call(tc);
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

        TreeTableRow<T> treeTableRow = new TreeTableRow<>();
        treeTableRow.updateTreeTableView(ttv);

        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        for (int row = 0; row < rows; row++) {
            treeTableRow.updateIndex(row);
            treeTableRow.updateTreeItem(ttv.getTreeItem(row));

            cell.updateTreeTableColumn(tc);
            cell.updateTreeTableView(ttv);
            cell.updateTreeTableRow(treeTableRow);
            cell.updateIndex(row);

            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                tableSkin.getChildren().add(cell);
                cell.applyCss();

                double w = cell.prefWidth(-1);

                maxWidth = Math.max(maxWidth, w);
                tableSkin.getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);

        // RT-36855 - take into account the column header text / graphic widths.
        // Magic 10 is to allow for sort arrow to appear without text truncation.
        TableColumnHeader header = tableSkin.getTableHeaderRow().getColumnHeaderFor(tc);
        double headerTextWidth = Utils.computeTextWidth(header.label.getFont(), tc.getText(), -1);
        Node graphic = header.label.getGraphic();
        double headerGraphicWidth = graphic == null ? 0 : graphic.prefWidth(-1) + header.label.getGraphicTextGap();
        double headerWidth = headerTextWidth + headerGraphicWidth + 10 + header.snappedLeftInset() + header.snappedRightInset();
        maxWidth = Math.max(maxWidth, headerWidth);

        // RT-23486
        maxWidth += padding;
        if (ttv.getColumnResizePolicy() == TreeTableView.CONSTRAINED_RESIZE_POLICY && ttv.getWidth() > 0) {

            if (maxWidth > tc.getMaxWidth()) {
                maxWidth = tc.getMaxWidth();
            }

            int size = tc.getColumns().size();
            if (size > 0) {
                resizeColumnToFitContent(tableSkin, tc.getColumns().get(size - 1), maxRows);
                return;
            }

            resizeColumn(tableSkin, tc, Math.round(maxWidth - tc.getWidth()));
        } else {
            TableColumnBaseHelper.setWidth(tc, maxWidth);
        }
    }

    public static ObjectProperty<Callback<ResizeFeaturesBase,Boolean>> columnResizePolicyProperty(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).columnResizePolicyProperty();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).columnResizePolicyProperty();
        }
        return null;
    }

    public static BooleanProperty tableMenuButtonVisibleProperty(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).tableMenuButtonVisibleProperty();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).tableMenuButtonVisibleProperty();
        }
        return null;
    }

    public static ObjectProperty<Node> placeholderProperty(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).placeholderProperty();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).placeholderProperty();
        }
        return null;
    }

    public static <C extends Control,I extends IndexedCell<?>> ObjectProperty<Callback<C,I>> rowFactoryProperty(TableViewSkinBase<?,?,C,I,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).rowFactoryProperty();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).rowFactoryProperty();
        }
        return null;
    }

    public static ObservableList<TableColumnBase<?,?>> getSortOrder(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).getSortOrder();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getSortOrder();
        }
        return FXCollections.emptyObservableList();
    }

    public static ObservableList<TableColumnBase<?,?>> getColumns(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).getColumns();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getColumns();
        }
        return FXCollections.emptyObservableList();
    }

    public static <T> TableSelectionModel<T> getSelectionModel(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).getSelectionModel();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getSelectionModel();
        }
        return null;
    }

    public static <T> TableFocusModel<T,?> getFocusModel(TableViewSkinBase<T,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView<T>)control).getFocusModel();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getFocusModel();
        }
        return null;
    }

    public static <T, TC extends TableColumnBase<T,?>> TablePositionBase<? extends TC> getFocusedCell(TableViewSkinBase<?,T,?,?,TC> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView<T>)control).getFocusModel().getFocusedCell();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getFocusModel().getFocusedCell();
        }
        return null;
    }

    public static <TC extends TableColumnBase<?,?>> ObservableList<TC> getVisibleLeafColumns(TableViewSkinBase<?,?,?,?,TC> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).getVisibleLeafColumns();
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getVisibleLeafColumns();
        }
        return FXCollections.emptyObservableList();
    }

    // returns the index of a column in the visible leaf columns
    public static int getVisibleLeafIndex(TableViewSkinBase<?,?,?,?,?> tableSkin, TableColumnBase tc) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).getVisibleLeafIndex((TableColumn)tc);
        } else if (control instanceof TreeTableView) {
            return ((TreeTableView)control).getVisibleLeafIndex((TreeTableColumn)tc);
        }
        return -1;
    }

    // returns the leaf column at the given index
    public static <T, TC extends TableColumnBase<T,?>> TC getVisibleLeafColumn(TableViewSkinBase<?,T,?,?,TC> tableSkin, int col) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return (TC) ((TableView)control).getVisibleLeafColumn(col);
        } else if (control instanceof TreeTableView) {
            return (TC) ((TreeTableView)control).getVisibleLeafColumn(col);
        }
        return null;
    }

    // returns a property representing the list of items in the control
    public static <T> ObjectProperty<ObservableList<T>> itemsProperty(TableViewSkinBase<?,?,?,?,?> tableSkin) {
        Object control = tableSkin.getSkinnable();
        if (control instanceof TableView) {
            return ((TableView)control).itemsProperty();
        } else if (control instanceof TreeTableView && tableSkin instanceof TreeTableViewSkin) {
            TreeTableViewSkin treeTableViewSkin = (TreeTableViewSkin)tableSkin;
            if (treeTableViewSkin.tableBackingListProperty == null) {
                treeTableViewSkin.tableBackingList = new TreeTableViewBackingList<>((TreeTableView)control);
                treeTableViewSkin.tableBackingListProperty = new SimpleObjectProperty<>(treeTableViewSkin.tableBackingList);
            }
            return treeTableViewSkin.tableBackingListProperty;
        }
        return null;
    }
}
