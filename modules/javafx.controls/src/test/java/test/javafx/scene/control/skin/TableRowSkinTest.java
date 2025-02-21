/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Skin;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableColumnHeaderShim;
import javafx.scene.control.skin.TableRowSkin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.test.Person;

public class TableRowSkinTest {

    private TableView<Person> tableView;
    private StageLoader stageLoader;
    private TableColumnHeader firstColumnHeader;

    @BeforeEach
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
        firstColumnHeader = VirtualFlowTestUtils.getTableColumnHeader(tableView, firstNameCol);
    }

    /**
     * The {@link TableView} should never be null inside the {@link TableRowSkin} during auto sizing.
     * See also: JDK-8289357
     */
    @Test
    public void testTableViewInRowSkinIsNotNullWhenAutoSizing() {
        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected Skin<?> createDefaultSkin() {
                return new ThrowingTableRowSkin<>(this);
            }
        });
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
    }

    /**
     * The {@link TableView} should not have any {@link TableRow} as children.
     * {@link TableRow}s are added temporary as part of the auto sizing, but should never remain after.
     * See also: JDK-8289357 and JDK-8292009
     */
    @Test
    public void testTableViewChildrenCount() {
        assertTrue(tableView.getChildrenUnmodifiable().stream().noneMatch(node -> node instanceof TableRow));
    }

    @Test
    public void tableRowShouldHonorPadding() {
        int top = 10;
        int right = 20;
        int bottom = 30;
        int left = 40;

        int horizontalPadding = left + right;
        int verticalPadding = top + bottom;

        IndexedCell cell = VirtualFlowTestUtils.getCell(tableView, 0);

        double minWidth = cell.minWidth(-1);
        double prefWidth = cell.prefWidth(-1);
        double maxWidth = cell.maxWidth(-1);
        double width = cell.getWidth();

        double minHeight = cell.minHeight(-1);
        double prefHeight = cell.prefHeight(-1);
        double maxHeight = cell.maxHeight(-1);
        double height = cell.getHeight();

        tableView.setRowFactory(tableView -> {
            TableRow<Person> row = new TableRow<>();
            row.setPadding(new Insets(top, right, bottom, left));
            return row;
        });

        tableView.refresh();
        Toolkit.getToolkit().firePulse();

        cell = VirtualFlowTestUtils.getCell(tableView, 0);

        assertEquals(minWidth + horizontalPadding, cell.minWidth(-1), 0);
        assertEquals(prefWidth + horizontalPadding, cell.prefWidth(-1), 0);
        assertEquals(maxWidth + horizontalPadding, cell.maxWidth(-1), 0);
        assertEquals(width + horizontalPadding, cell.getWidth(), 0);

        assertEquals(minHeight + verticalPadding, cell.minHeight(-1), 0);
        assertEquals(prefHeight + verticalPadding, cell.prefHeight(-1), 0);
        assertEquals(maxHeight + verticalPadding, cell.maxHeight(-1), 0);
        assertEquals(height + verticalPadding, cell.getHeight(), 0);
    }

    @Test
    public void tableRowWithCellSizeShouldHonorPadding() {
        int top = 10;
        int right = 20;
        int bottom = 30;
        int left = 40;

        int horizontalPadding = left + right;
        int verticalPadding = top + bottom;
        int verticalPaddingWithCellSize = top + bottom + 10;

        IndexedCell cell = VirtualFlowTestUtils.getCell(tableView, 0);

        double minWidth = cell.minWidth(-1);
        double prefWidth = cell.prefWidth(-1);
        double maxWidth = cell.maxWidth(-1);
        double width = cell.getWidth();

        double minHeight = cell.minHeight(-1);
        double prefHeight = cell.prefHeight(-1);
        double maxHeight = cell.maxHeight(-1);
        double height = cell.getHeight();

        tableView.setRowFactory(tableView -> {
            TableRow<Person> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 34px");
            row.setPadding(new Insets(top, right, bottom, left));
            return row;
        });

        tableView.refresh();
        Toolkit.getToolkit().firePulse();

        cell = VirtualFlowTestUtils.getCell(tableView, 0);

        assertEquals(minWidth + horizontalPadding, cell.minWidth(-1), 0);
        assertEquals(prefWidth + horizontalPadding, cell.prefWidth(-1), 0);
        assertEquals(maxWidth + horizontalPadding, cell.maxWidth(-1), 0);
        assertEquals(width + horizontalPadding, cell.getWidth(), 0);

        // minHeight will take the lowest height - which are the cells (24px)
        assertEquals(minHeight + verticalPadding, cell.minHeight(-1), 0);
        assertEquals(prefHeight + verticalPaddingWithCellSize, cell.prefHeight(-1), 0);
        assertEquals(maxHeight + verticalPaddingWithCellSize, cell.maxHeight(-1), 0);
        assertEquals(height + verticalPaddingWithCellSize, cell.getHeight(), 0);
    }

    @Test
    public void tableRowWithFixedSizeShouldIgnoreVerticalPadding() {
        int top = 10;
        int right = 20;
        int bottom = 30;
        int left = 40;

        int horizontalPadding = left + right;

        tableView.setFixedCellSize(24);

        IndexedCell cell = VirtualFlowTestUtils.getCell(tableView, 0);

        double minWidth = cell.minWidth(-1);
        double prefWidth = cell.prefWidth(-1);
        double maxWidth = cell.maxWidth(-1);
        double width = cell.getWidth();

        double minHeight = cell.minHeight(-1);
        double prefHeight = cell.prefHeight(-1);
        double maxHeight = cell.maxHeight(-1);
        double height = cell.getHeight();

        tableView.setRowFactory(tableView -> {
            TableRow<Person> row = new TableRow<>();
            row.setPadding(new Insets(top, right, bottom, left));
            return row;
        });

        tableView.refresh();
        Toolkit.getToolkit().firePulse();

        cell = VirtualFlowTestUtils.getCell(tableView, 0);

        assertEquals(minWidth + horizontalPadding, cell.minWidth(-1), 0);
        assertEquals(prefWidth + horizontalPadding, cell.prefWidth(-1), 0);
        assertEquals(maxWidth + horizontalPadding, cell.maxWidth(-1), 0);
        assertEquals(width + horizontalPadding, cell.getWidth(), 0);

        assertEquals(minHeight, cell.minHeight(-1), 0);
        assertEquals(prefHeight, cell.prefHeight(-1), 0);
        assertEquals(maxHeight, cell.maxHeight(-1), 0);
        assertEquals(height, cell.getHeight(), 0);
    }

    /**
     * When we set a fixed cell size and make an invisible column visible we expect the underlying cells to be visible,
     * e.g. width > 0.
     * See also: JDK-8305248
     */
    @Test
    public void testMakeInvisibleColumnVisible() {
        tableView.setFixedCellSize(24);
        TableColumn<Person, ?> firstColumn = tableView.getColumns().get(0);
        firstColumn.setVisible(false);

        tableView.refresh();
        Toolkit.getToolkit().firePulse();

        firstColumn.setVisible(true);
        Toolkit.getToolkit().firePulse();

        IndexedCell<?> row = VirtualFlowTestUtils.getCell(tableView, 0);
        for (Node node : row.getChildrenUnmodifiable()) {
            if (node instanceof TableCell<?, ?> cell) {
                double width = cell.getWidth();
                assertNotEquals(0.0, width);
            }
        }
    }

    @Test
    public void testMakeVisibleColumnInvisible() {
        tableView.setFixedCellSize(24);
        TableColumn<Person, ?> firstColumn = tableView.getColumns().get(0);
        assertTrue(firstColumn.isVisible());

        tableView.refresh();
        Toolkit.getToolkit().firePulse();

        firstColumn.setVisible(false);
        Toolkit.getToolkit().firePulse();

        IndexedCell<?> row = VirtualFlowTestUtils.getCell(tableView, 0);
        for (Node node : row.getChildrenUnmodifiable()) {
            if (node instanceof TableCell<?, ?> cell) {
                double width = cell.getWidth();
                assertNotEquals(0.0, width);
            }
        }
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

    /**
     * The {@link TableRowSkin} should add new cells after new columns are added.
     * See: JDK-8321970
     */
    @Test
    public void cellsShouldBeAddedInRowFixedCellSize() {
        tableView.setPrefWidth(800);
        tableView.setFixedCellSize(24);

        TableColumn<Person, String> otherColumn = new TableColumn<>("other");
        otherColumn.setPrefWidth(100);
        otherColumn.setCellValueFactory(value -> new SimpleStringProperty("other"));
        tableView.getColumns().add(otherColumn);

        Toolkit.getToolkit().firePulse();
        assertEquals(5, tableView.getColumns().size());

        Toolkit.getToolkit().firePulse();
        IndexedCell<?> row = VirtualFlowTestUtils.getCell(tableView, 1);
        assertEquals(5, row.getChildrenUnmodifiable().stream().filter(TableCell.class::isInstance).count());
    }

    @AfterEach
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
        tableView.getColumns().remove(tableView.getColumns().size() - 2, tableView.getColumns().size());

        Toolkit.getToolkit().firePulse();

        // We removed 2 columns, so the cell count should be decremented by 2 as well.
        assertEquals(tableView.getColumns().size(),
                VirtualFlowTestUtils.getCell(tableView, 0).getChildrenUnmodifiable().size());
    }

    private static class ThrowingTableRowSkin<T> extends TableRowSkin<T> {
        public ThrowingTableRowSkin(TableRow<T> tableRow) {
            super(tableRow);
            assertNotNull(tableRow.getTableView());
        }
    }

}
