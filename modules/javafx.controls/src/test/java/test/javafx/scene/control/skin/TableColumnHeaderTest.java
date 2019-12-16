/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableColumnHeader;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.test.Person;
import javafx.scene.control.skin.TableColumnHeaderShim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TableColumnHeaderTest {

    private TableColumnHeader firstColumnHeader;
    private TableView<Person> tableView;
    private StageLoader sl;
    private static String NAME0 = "Humphrey McPhee";
    private static String NAME1 = "Justice Caldwell";
    private static String NAME2 = "Orrin Davies";
    private static String NAME3 = "Emma Wilson";

    @Before
    public void before() {
        ObservableList<Person> model = FXCollections.observableArrayList(
                new Person(NAME0, 76),
                new Person(NAME1, 30),
                new Person(NAME2, 30),
                new Person(NAME3, 8)
        );
        TableColumn<Person, String> column = new TableColumn<>("Col ");
        column.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));

        tableView = new TableView<>(model);

        tableView.getColumns().add(column);

        sl = new StageLoader(tableView);
        Toolkit tk = Toolkit.getToolkit();

        tk.firePulse();
        //Force the column to have default font, otherwise font Amble is applied and mess with header width size
        column.setStyle("-fx-font: System;");
        firstColumnHeader = VirtualFlowTestUtils.getTableColumnHeader(tableView, column);
    }

    @After
    public void after() {
        sl.dispose();
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header without modifications
     */
    @Test
    public void test_resizeColumnToFitContent() {
        TableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        assertEquals("Width must be the same",
                width, column.getWidth(), 0.001);
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header with first column increase
     */
    @Test
    public void test_resizeColumnToFitContentIncrease() {
        TableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        tableView.getItems().get(0).setFirstName("This is a big text inside that column");

        assertEquals("Width must be the same",
                width, column.getWidth(), 0.001);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue("Column width must be greater",
                width < column.getWidth());

        //Back to initial value
        tableView.getItems().get(0).setFirstName(NAME0);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertEquals("Width must be equal to initial value",
                width, column.getWidth(), 0.001);
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header with first column decrease
     */
    @Test
    public void test_resizeColumnToFitContentDecrease() {
        TableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        tableView.getItems().get(0).setFirstName("small");
        tableView.getItems().get(1).setFirstName("small");
        tableView.getItems().get(2).setFirstName("small");
        tableView.getItems().get(3).setFirstName("small");

        assertEquals("Width must be the same",
                width, column.getWidth(), 0.001);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue("Column width must be smaller",
                width > column.getWidth());

        //Back to initial value
        tableView.getItems().get(0).setFirstName(NAME0);
        tableView.getItems().get(1).setFirstName(NAME1);
        tableView.getItems().get(2).setFirstName(NAME2);
        tableView.getItems().get(3).setFirstName(NAME3);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertEquals("Width must be equal to initial value",
                width, column.getWidth(), 0.001);
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header itself
     */
    @Test
    public void test_resizeColumnToFitContentHeader() {
        TableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        column.setText("This is a big text inside that column");

        assertEquals("Width must be the same",
                width, column.getWidth(), 0.001);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue("Column width must be greater",
                width < column.getWidth());

        //Back to initial value
        column.setText("Col");
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, 3);
        assertEquals("Width must be equal to initial value",
                width, column.getWidth(), 0.001);
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header with only 3 first rows
     */
    @Test
    public void test_resizeColumnToFitContentMaxRow() {
        TableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        tableView.getItems().get(3).setFirstName("This is a big text inside that column");


        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, 3);
        assertEquals("Width must be the same",
                width, column.getWidth(), 0.001);


        //Back to initial value
        tableView.getItems().get(3).setFirstName(NAME3);


        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, 3);
        assertEquals("Width must be equal to initial value",
                width, column.getWidth(), 0.001);
    }
}
