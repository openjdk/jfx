/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.test.Person;

import static org.junit.Assert.assertEquals;

public class TableRowSkinTest {

    private TableView<Person> tableView;
    private StageLoader stageLoader;

    @Before
    public void before() {
        tableView = new TableView<>();

        TableColumn<Person, String> firstNameCol = new TableColumn<>("Firstname");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        TableColumn<Person, String> lastNameCol = new TableColumn<>("Lastname");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        TableColumn<Person, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableColumn<Person, Integer> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));

        tableView.getColumns().addAll(firstNameCol, lastNameCol, emailCol, ageCol);

        ObservableList<Person> items = FXCollections.observableArrayList(
                new Person("firstName1", "lastName1", "email1@javafx.com", 1),
                new Person("firstName2", "lastName2", "email2@javafx.com", 2),
                new Person("firstName3", "lastName3", "email3@javafx.com", 3),
                new Person("firstName4", "lastName4", "email4@javafx.com", 4)
        );

        tableView.setItems(items);

        stageLoader = new StageLoader(tableView);
    }

    @Test
    public void removedColumnsShouldRemoveCorrespondingCellsInRowFixedCellSize() {
        tableView.setFixedCellSize(24);
        removedColumnsShouldRemoveCorrespondingCellsInRowImpl();
    }

    @Test
    public void removedColumnsShouldRemoveCorrespondingCellsInRow() {
        removedColumnsShouldRemoveCorrespondingCellsInRowImpl();
    }

    @Test
    public void invisibleColumnsShouldRemoveCorrespondingCellsInRowFixedCellSize() {
        tableView.setFixedCellSize(24);
        invisibleColumnsShouldRemoveCorrespondingCellsInRowImpl();
    }

    @Test
    public void invisibleColumnsShouldRemoveCorrespondingCellsInRow() {
        invisibleColumnsShouldRemoveCorrespondingCellsInRowImpl();
    }

    @After
    public void after() {
        stageLoader.dispose();
    }

    private void invisibleColumnsShouldRemoveCorrespondingCellsInRowImpl() {
        // Set the last 2 columns invisible.
        tableView.getColumns().get(tableView.getColumns().size() - 1).setVisible(false);
        tableView.getColumns().get(tableView.getColumns().size() - 2).setVisible(false);

        Toolkit.getToolkit().firePulse();

        // We set 2 columns to invisible, so the cell count should be decremented by 2 as well.
        assertEquals(tableView.getColumns().size() - 2,
                VirtualFlowTestUtils.getCell(tableView, 0).getChildrenUnmodifiable().size());
    }

    private void removedColumnsShouldRemoveCorrespondingCellsInRowImpl() {
        // Remove the last 2 columns.
        tableView.getColumns().remove(tableView.getColumns().size() - 1, tableView.getColumns().size());

        Toolkit.getToolkit().firePulse();

        // We removed 2 columns, so the cell count should be decremented by 2 as well.
        assertEquals(tableView.getColumns().size(),
                VirtualFlowTestUtils.getCell(tableView, 0).getChildrenUnmodifiable().size());
    }

}
