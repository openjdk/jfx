/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.control;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sun.javafx.tk.Toolkit;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;

/**
 * Miscellaneous convenience methods to support javafx.controls tests.
 */
public class ControlUtils {
    private ControlUtils() { }

    /**
     * Creates a TableView with three columns and three rows. Each cell contains a
     * "..." string.
     */
    public static TableView<String> createTableView() {
        TableView<String> t = new TableView<>();
        t.requestFocus();
        t.getColumns().addAll(
            createTableColumn("C0"),
            createTableColumn("C1"),
            createTableColumn("C2")
        );
        t.getItems().addAll(
            "",
            "",
            ""
        );
        return t;
    }

    /**
     * Creates a TreeTableView with three columns and three rows (root is hidden).
     * Each cell contains a "..." string.
     */
    public static TreeTableView<String> createTreeTableView() {
        TreeItem<String> root = new TreeItem<>("");
        root.setExpanded(true);
        root.getChildren().setAll(
            new TreeItem<>(""),
            new TreeItem<>(""),
            new TreeItem<>("")
        );

        TreeTableView<String> t = new TreeTableView<>();
        t.setRoot(root);
        t.setShowRoot(false);
        t.requestFocus();
        t.getColumns().addAll(
            createTreeTableColumn("C0"),
            createTreeTableColumn("C1"),
            createTreeTableColumn("C2")
        );
        return t;
    }

    /**
     * Performs a node lookup, returning a TreeTableCell at the given (row, column),
     * or throws an Error if not found, or more than one instance is found.
     */
    public static TreeTableCell getTreeTableCell(TreeTableView t, int row, int column) {
        TreeTableColumn col = (TreeTableColumn)t.getColumns().get(column);
        return findTheOnly(t, ".tree-table-cell", TreeTableCell.class, (n) -> {
            if (n instanceof TreeTableCell c) {
                if (row == c.getTableRow().getIndex()) {
                    if (col == c.getTableColumn()) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * Performs a node lookup, returning a TreeTableRow at the given row, or throws
     * an Error if not found, or more than one instance is found.
     */
    public static TreeTableRow getTreeTableRow(TreeTableView t, int row) {
        return findTheOnly(t, ".tree-table-row-cell", TreeTableRow.class, (n) -> {
            if (n instanceof TreeTableRow c) {
                if (row == c.getIndex()) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Performs a node lookup, returning a TableCell at the given (row, column) or
     * throws an Error if not found, or more than one instance is found.
     */
    public static TableCell getTableCell(TableView t, int row, int column) {
        TableColumn col = (TableColumn)t.getColumns().get(column);
        return findTheOnly(t, ".table-cell", TableCell.class, (x) -> {
            if (x instanceof TableCell c) {
                if (row == c.getTableRow().getIndex()) {
                    if (col == c.getTableColumn()) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * Performs a node lookup, returning a TableRow at the given row, or throws an
     * Error if not found, or more than one instance is found.
     */
    public static TableRow getTableRow(TableView t, int row) {
        return findTheOnly(t, ".table-row-cell", TableRow.class, (x) -> {
            if (x instanceof TableRow c) {
                if (row == c.getIndex()) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Creates a TreeTableColumn with the given name, setting up the cell value
     * factory to place a "..." string at each cell.
     */
    public static TreeTableColumn createTreeTableColumn(String name) {
        TreeTableColumn c = new TreeTableColumn(name);
        c.setCellValueFactory((f) -> new SimpleStringProperty("..."));
        return c;
    }

    /**
     * Creates a TableColumn with the given name, setting up the cell value factory
     * to place a "..." string at each cell.
     */
    public static TableColumn createTableColumn(String name) {
        TableColumn c = new TableColumn(name);
        c.setCellValueFactory((f) -> new SimpleStringProperty("..."));
        return c;
    }

    /**
     * Simulates a mouse click with given KeyModifier(s) over the specified target,
     * then fires a pulse.
     */
    public static void mouseClick(EventTarget target, KeyModifier... modifiers) {
        MouseEventFirer m = new MouseEventFirer(target);
        m.fireMousePressAndRelease(modifiers);
        m.dispose();

        Toolkit.getToolkit().firePulse();
    }

    /**
     * Finds a Node given the selector and predicate filter, then insures there is
     * only one such node
     */
    protected static <T> T findTheOnly(Node container, String selector, Class<T> type, Predicate<Node> filter) {
        Set<Node> nodes = container.lookupAll(selector).
            stream().
            filter(filter).
            collect(Collectors.toSet());

        assertEquals(1, nodes.size());
        return (T)nodes.toArray()[0];
    }
}
