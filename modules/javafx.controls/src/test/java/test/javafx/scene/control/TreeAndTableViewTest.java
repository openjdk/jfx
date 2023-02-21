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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.MouseEvent;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 * Tests for:
 * - NPE with null selection model JDK-8187145
 */
public class TreeAndTableViewTest {
    /** Sorting TableView with a null selection model should not generate an NPE */
    @Test
    public void test_TableView_jdk_8187145() {
        TableView<String> table = new TableView<>();
        table.requestFocus();
        table.getColumns().addAll(
            createTableColumn("C0"),
            createTableColumn("C1"),
            createTableColumn("C2")
            );
        table.getItems().addAll(
            "",
            "",
            ""
            );

        // important: actually creates cells
        VirtualFlowTestUtils.getCell(table, 0);

        // row selection mode
        TableViewSelectionModel<String> oldSelectionModel = table.getSelectionModel();
        table.getSelectionModel().selectAll();
        table.setSelectionModel(null);

        // verify none have 'selected' pseudostyle
        {
            List cells = table.lookupAll(".table-cell").
                stream().
                filter((n) -> ((n instanceof TableCell) && containsPseudoclass(n, "selected"))).
                collect(Collectors.toList());
            assertEquals(0, cells.size());
        }

        // cell selection mode
        table.setSelectionModel(oldSelectionModel);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().selectAll();
        table.setSelectionModel(null);

        // verify none have 'selected' pseudostyle
        {
            List cells = table.lookupAll(".table-cell").
                stream().
                filter((n) -> ((n instanceof TableCell) && containsPseudoclass(n, "selected"))).
                collect(Collectors.toList());
            assertEquals(0, cells.size());
        }

        // verify no NPE when sorting by clicking on every table column cell,
        // toggling sorting ascending -> descending -> none
        {
            for (Object x: table.lookupAll(".table-column")) {
                if (x instanceof TableColumnHeader n) {
                    mouseClick(n);
                    assertEquals(1, table.getSortOrder().size());
                    table.sort();

                    mouseClick(n);
                    assertEquals(1, table.getSortOrder().size());
                    table.sort();

                    mouseClick(n);
                    assertEquals(0, table.getSortOrder().size());
                    table.sort();
                }
            }
        }
    }

    /** Sorting TreeTableView with a null selection model should not generate an NPE */
    @Test
    public void test_TreeTableView_jdk_8187145() {
        TreeItem<String> root = new TreeItem<String>("");
        root.setExpanded(true);
        root.getChildren().setAll(
            new TreeItem<>(""),
            new TreeItem<>(""),
            new TreeItem<>("")
            );

        TreeTableView<String> tree = new TreeTableView<>();
        tree.setRoot(root);
        tree.setShowRoot(false);
        tree.requestFocus();
        tree.getColumns().addAll(
            createTreeTableColumn("C0"),
            createTreeTableColumn("C1"),
            createTreeTableColumn("C2")
            );

        // important: actually creates cells
        VirtualFlowTestUtils.getCell(tree, 0);

        // row selection mode
        TreeTableViewSelectionModel<String> oldSelectionModel = tree.getSelectionModel();
        tree.getSelectionModel().selectAll();
        tree.setSelectionModel(null);

        // verify none have 'selected' pseudostyle
        {
            List cells = tree.lookupAll(".table-cell").
                stream().
                filter((n) -> ((n instanceof TableCell) && containsPseudoclass(n, "selected"))).
                collect(Collectors.toList());
            assertEquals(0, cells.size());
        }

        // cell selection mode
        tree.setSelectionModel(oldSelectionModel);
        tree.getSelectionModel().setCellSelectionEnabled(true);
        tree.getSelectionModel().selectAll();
        tree.setSelectionModel(null);

        // verify none have 'selected' pseudostyle
        {
            List cells = tree.lookupAll(".table-cell").
                stream().
                filter((n) -> ((n instanceof TableCell) && containsPseudoclass(n, "selected"))).
                collect(Collectors.toList());
            assertEquals(0, cells.size());
        }

        // verify no NPE when sorting by clicking on every table column cell,
        // toggling sorting ascending -> descending -> none
        {
            for (Object x: tree.lookupAll(".table-column")) {
                if (x instanceof TableColumnHeader n) {
                    mouseClick(n);
                    assertEquals(1, tree.getSortOrder().size());
                    tree.sort();

                    mouseClick(n);
                    assertEquals(1, tree.getSortOrder().size());
                    tree.sort();

                    mouseClick(n);
                    assertEquals(0, tree.getSortOrder().size());
                    tree.sort();
                }
            }
        }
    }

    protected static TreeTableColumn createTreeTableColumn(String name) {
        TreeTableColumn c = new TreeTableColumn(name);
        c.setCellValueFactory((f) -> new SimpleStringProperty("..."));
        return c;
    }

    protected static void mouseClick(EventTarget t, KeyModifier... modifiers) {
        MouseEventFirer m = new MouseEventFirer(t);
        m.fireMousePressAndRelease(modifiers);
        m.fireMouseEvent(MouseEvent.MOUSE_RELEASED, modifiers);
        m.dispose();

        Toolkit.getToolkit().firePulse();
    }

    protected static TableColumn createTableColumn(String name) {
        TableColumn c = new TableColumn(name);
        c.setCellValueFactory((f) -> new SimpleStringProperty("..."));
        return c;
    }

    protected static <T extends Node> List<T> collectNodes(Node root, String selector, Class<T> type) {
        return (List<T>)root.
            lookupAll(selector).
            stream().
            filter((n) -> (n.getClass().isAssignableFrom(type))).
            collect(Collectors.toList());
    }

    protected static boolean containsPseudoclass(Node n, String pseudoclass) {
        for (PseudoClass pc : n.getPseudoClassStates()) {
            if (pseudoclass.equals(pc.getPseudoClassName())) {
                return true;
            }
        }
        return false;
    }
}
