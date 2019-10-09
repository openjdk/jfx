/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.Skin;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import test.com.sun.javafx.scene.control.test.Person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TableColumnHeaderTest {

    private MyTableColumnHeader tableColumnHeader;

    @Before
    public void beforeTest() {
        tableColumnHeader = null;
    }

    @Test
    public void test_resizeColumnToFitContent() {
        ObservableList<Person> model = FXCollections.observableArrayList(
                new Person("Humphrey McPhee", 76),
                new Person("Justice Caldwell", 30),
                new Person("Orrin Davies", 30),
                new Person("Emma Wilson", 8)
        );
        TableColumn<Person, String> column = new TableColumn<>("Col ");
        column.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        TableView<Person> tableView = new TableView<>(model) {
            @Override
            protected Skin<?> createDefaultSkin() {
                return new TableViewSkin(this) {
                    @Override
                    protected TableHeaderRow createTableHeaderRow() {
                        return new TableHeaderRow(this) {
                            @Override
                            protected NestedTableColumnHeader createRootHeader() {
                                return new NestedTableColumnHeader(null) {
                                    @Override
                                    protected TableColumnHeader createTableColumnHeader(TableColumnBase col) {
                                        tableColumnHeader = new MyTableColumnHeader(column);
                                        return col == null || col.getColumns().isEmpty() || col == getTableColumn() ?
                                                tableColumnHeader :
                                                new NestedTableColumnHeader(col);

                                    }
                                };
                            }
                        };
                    }
                };
            }
        };

        tableView.getColumns().add(column);

        Toolkit tk = Toolkit.getToolkit();

        Scene scene = new Scene(tableView);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(500);
        stage.setHeight(400);
        stage.centerOnScreen();
        stage.show();

        tk.firePulse();

        double width = column.getWidth();
        tableColumnHeader.resizeCol();
        assertEquals("Width must be the same",
                width, column.getWidth(), 0.001);

        EventType<TableColumn.CellEditEvent<Person, String>> eventType = TableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                tableView, new TablePosition<Person, String>(tableView, 0, column), (EventType) eventType, "This is a big text inside that column"));
        tableColumnHeader.resizeCol();
        assertTrue("Column width must be greater",
                width < column.getWidth());

        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                tableView, new TablePosition<Person, String>(tableView, 0, column), (EventType) eventType, "small"));
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                tableView, new TablePosition<Person, String>(tableView, 1, column), (EventType) eventType, "small"));
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                tableView, new TablePosition<Person, String>(tableView, 2, column), (EventType) eventType, "small"));
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                tableView, new TablePosition<Person, String>(tableView, 3, column), (EventType) eventType, "small"));

        tableColumnHeader.resizeCol();
        assertTrue("Column width must be smaller",
                width > column.getWidth());
    }

    private class MyTableColumnHeader extends TableColumnHeader {

        public MyTableColumnHeader(final TableColumnBase tc) {
            super(tc);
        }

        public void resizeCol() {
            resizeColumnToFitContent(-1);
        }
    }
}
