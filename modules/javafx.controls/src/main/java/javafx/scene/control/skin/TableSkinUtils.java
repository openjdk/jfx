/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
