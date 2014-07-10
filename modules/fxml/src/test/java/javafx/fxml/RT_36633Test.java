/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RT_36633Test {
    @Test public void test_rt36633_tableColumn() throws Exception {
        VBox pane = FXMLLoader.load(getClass().getResource("rt_36633.fxml"));
        TableView tableView = (TableView) pane.getChildren().get(0);
        TableColumn column = (TableColumn) tableView.getColumns().get(0);

        assertEquals("rt36633_tableColumn", column.getId());
    }

    @Test public void test_rt36633_tab() throws Exception {
        VBox pane = FXMLLoader.load(getClass().getResource("rt_36633.fxml"));
        TabPane tabPane = (TabPane) pane.getChildren().get(1);
        Tab tab = tabPane.getTabs().get(0);

        assertEquals("rt36633_tab", tab.getId());
    }

    @Test public void test_rt36633_menuItem() throws Exception {
        VBox pane = FXMLLoader.load(getClass().getResource("rt_36633.fxml"));
        MenuBar menuBar = (MenuBar) pane.getChildren().get(2);
        Menu menu = menuBar.getMenus().get(0);
        MenuItem menuItem = menu.getItems().get(0);

        assertEquals("rt36633_menu", menu.getId());
        assertEquals("rt36633_menuItem", menuItem.getId());
    }
}
