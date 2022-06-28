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

package test.javafx.scene.control.skin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Skin;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableColumnHeaderShim;
import javafx.scene.control.skin.TreeTableRowSkin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.test.Person;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TreeTableRowSkinTest {

    private TreeTableView<Person> treeTableView;
    private StageLoader stageLoader;
    private TableColumnHeader firstColumnHeader;

    @BeforeEach
    public void before() {
        treeTableView = new TreeTableView<>();

        TreeTableColumn<Person, String> firstNameCol = new TreeTableColumn<>("Firstname");
        firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("firstName"));
        TreeTableColumn<Person, String> lastNameCol = new TreeTableColumn<>("Lastname");
        lastNameCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("lastName"));
        TreeTableColumn<Person, String> emailCol = new TreeTableColumn<>("Email");
        emailCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("email"));
        TreeTableColumn<Person, Integer> ageCol = new TreeTableColumn<>("Age");
        ageCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("age"));

        treeTableView.getColumns().addAll(firstNameCol, lastNameCol, emailCol, ageCol);

        ObservableList<TreeItem<Person>> items = FXCollections.observableArrayList(
                new TreeItem<>(new Person("firstName1", "lastName1", "email1@javafx.com", 1)),
                new TreeItem<>(new Person("firstName2", "lastName2", "email2@javafx.com", 2)),
                new TreeItem<>(new Person("firstName3", "lastName3", "email3@javafx.com", 3)),
                new TreeItem<>(new Person("firstName4", "lastName4", "email4@javafx.com", 4))
        );

        TreeItem<Person> root = new TreeItem<>();
        root.getChildren().addAll(items);
        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);

        stageLoader = new StageLoader(treeTableView);
        firstColumnHeader = VirtualFlowTestUtils.getTableColumnHeader(treeTableView, firstNameCol);
    }

    /**
     * The {@link TreeTableView} should never be null inside the {@link TreeTableRowSkin} during auto sizing.
     * See also: JDK-8289357
     */
    @Test
    public void testTreeTableViewInRowSkinIsNotNullWhenAutoSizing() {
        treeTableView.setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected Skin<?> createDefaultSkin() {
                return new ThrowingTreeTableRowSkin<>(this);
            }
        });
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
    }

    /**
     * The {@link TreeTableView} should not have any {@link TreeTableRow} as children.
     * {@link TreeTableRow}s are added temporary as part of the auto sizing, but should never remain after.
     * See also: JDK-8289357
     */
    @Test
    public void testTreeTableViewChildrenCount() {
        assertTrue(treeTableView.getChildrenUnmodifiable().stream().noneMatch(node -> node instanceof TreeTableRow));
    }

    @AfterEach
    public void after() {
        stageLoader.dispose();
    }

    private static class ThrowingTreeTableRowSkin<T> extends TreeTableRowSkin<T> {
        public ThrowingTreeTableRowSkin(TreeTableRow<T> treeTableRow) {
            super(treeTableRow);
            assertNotNull(treeTableRow.getTreeTableView());
        }
    }

}
