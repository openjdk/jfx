/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tk.Toolkit;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TableViewSkinTest {
    @Test
    public void test_JDK_8188164() {
        TableView<String> tableView = new TableView<>();
        for (int i = 0; i < 5; i++) {
            TableColumn<String, String> column = new TableColumn<>("Col " + i);
            tableView.getColumns().add(column);
        }

        Scene scene = new Scene(tableView);
        scene.getStylesheets().add(TableViewSkinTest.class.getResource("TableViewSkinTest.css").toExternalForm());

        Toolkit tk = Toolkit.getToolkit();

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(500);
        stage.setHeight(400);
        stage.centerOnScreen();
        stage.show();

        tk.firePulse();

        TableHeaderRow header = (TableHeaderRow)tableView.lookup("TableHeaderRow");
        assertEquals("Table Header height specified in CSS",
                      100.0, header.getHeight(), 0.001);
    }
}
