/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.TreeTableViewSkin;
import javafx.scene.control.skin.TreeViewSkin;
import org.junit.jupiter.api.Test;

/**
 * Contains tests that should pass for all concrete implementations
 * of VirtualContainerBase.
 */
public class ConcreteVirtualContainerTest {

    /**
     * Test for JDK-8221334: flow's cellCount must be initialized.
     */
    @Test
    public void testTableSkinCellCountInitial() {
        TableView<Locale> control = new TableView<>(FXCollections.observableArrayList(Locale.getAvailableLocales()));
        control.setSkin(new TableViewSkin<>(control) {
            {
                assertEquals(control.getItems().size(), getVirtualFlow().getCellCount(), "flow's cellCount must be initialized");
            }
        });
    }

    @Test
    public void testTreeTableSkinCellCountInitial() {
        List<TreeItem<Locale>> treeItems = Arrays.stream(Locale.getAvailableLocales())
                .map(TreeItem::new)
                .collect(Collectors.toList());
        TreeItem<Locale> root = new TreeItem<>(new Locale("dummy"));
        root.setExpanded(true);
        root.getChildren().addAll(treeItems);
        TreeTableView<Locale> control = new TreeTableView<>(root);
        control.setSkin(new TreeTableViewSkin<>(control) {
            {
                assertEquals(treeItems.size() + 1, getVirtualFlow().getCellCount(), "flow's cellCount must be initialized");
            }
        });
    }

    @Test
    public void testTreeSkinCellCountInitial() {
        List<TreeItem<Locale>> treeItems = Arrays.stream(Locale.getAvailableLocales())
                .map(TreeItem::new)
                .collect(Collectors.toList());
        TreeItem<Locale> root = new TreeItem<>(new Locale("dummy"));
        root.setExpanded(true);
        root.getChildren().addAll(treeItems);
        TreeView<Locale> control = new TreeView<>(root);
        control.setSkin(new TreeViewSkin<>(control) {
            {
                assertEquals(treeItems.size() + 1, getVirtualFlow().getCellCount(), "flow's cellCount must be initialized");
            }
        });
    }

    @Test
    public void testListSkinCellCountInitial() {
        ListView<Locale> control = new ListView<>(FXCollections.observableArrayList(Locale.getAvailableLocales()));
        control.setSkin(new ListViewSkin<>(control) {
            {
                assertEquals(control.getItems().size(), getVirtualFlow().getCellCount(), "flow's cellCount must be initialized");
            }
        });
    }
}
