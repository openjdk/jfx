/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableColumnHeaderShim;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import test.com.sun.javafx.scene.control.test.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableColumnHeaderTreeTableTest {

    private TableColumnHeader firstColumnHeader;
    private TreeTableView<Person> tableView;
    private StageLoader sl;
    private static String NAME0 = "Humphrey McPhee";
    private static String NAME1 = "Justice Caldwell";
    private static String NAME2 = "Orrin Davies";
    private static String NAME3 = "Emma Wilson";

    @BeforeEach
    public void before() {
        ObservableList<Person> model = FXCollections.observableArrayList(
                new Person(NAME0, 76),
                new Person(NAME1, 30),
                new Person(NAME2, 30),
                new Person(NAME3, 8)
        );
        TreeTableColumn<Person, String> column = new TreeTableColumn<>("Col ");
        column.setCellValueFactory(new TreeItemPropertyValueFactory<Person, String>("firstName"));

        TreeItem<Person> root = new TreeItem<>();
        for (Person person : model) {
            root.getChildren().add(new TreeItem<>(person));
        }

        tableView = new TreeTableView<>(root);
        tableView.setShowRoot(false);

        tableView.getColumns().add(column);

        sl = new StageLoader(tableView);
        Toolkit tk = Toolkit.getToolkit();

        tk.firePulse();
        firstColumnHeader = VirtualFlowTestUtils.getTableColumnHeader(tableView, column);
    }

    @AfterEach
    public void after() {
        sl.dispose();
    }

    /**
     * When a right click is done on a table column header, the column drag lock should be set to true in the
     * pressed handler, but eventually to false again in the released handler.<br>
     * By that we guarantee, that the column resizing still works.
     */
    @Test
    public void testColumnRightClickDoesAllowResizing() {
        MouseEventFirer firer = new MouseEventFirer(firstColumnHeader);

        assertFalse(TableColumnHeaderShim.getTableHeaderRowColumnDragLock(firstColumnHeader));

        firer.fireMousePressed(MouseButton.SECONDARY);
        assertTrue(TableColumnHeaderShim.getTableHeaderRowColumnDragLock(firstColumnHeader));

        firer.fireMouseReleased(MouseButton.SECONDARY);
        assertFalse(TableColumnHeaderShim.getTableHeaderRowColumnDragLock(firstColumnHeader));
    }

    /**
     * When a right click is done on a table column header and consumed by a self added event handler, the column
     * drag lock should be set to true in the pressed handler, but still to false again in the released handler.<br>
     * By that we guarantee, that the column resizing still works.
     */
    @Test
    public void testColumnRightClickDoesAllowResizingWhenConsumed() {
        firstColumnHeader.addEventHandler(MouseEvent.MOUSE_RELEASED, Event::consume);

        MouseEventFirer firer = new MouseEventFirer(firstColumnHeader);

        assertFalse(TableColumnHeaderShim.getTableHeaderRowColumnDragLock(firstColumnHeader));

        firer.fireMousePressed(MouseButton.SECONDARY);
        assertTrue(TableColumnHeaderShim.getTableHeaderRowColumnDragLock(firstColumnHeader));

        firer.fireMouseReleased(MouseButton.SECONDARY);
        assertFalse(TableColumnHeaderShim.getTableHeaderRowColumnDragLock(firstColumnHeader));
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header without modifications
     */
    @Test
    public void test_resizeColumnToFitContent() {
        TreeTableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        assertEquals(width, column.getWidth(), 0.001, "Width must be the same");
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header with first column increase
     */
    @Test
    public void test_resizeColumnToFitContentIncrease() {
        TreeTableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        tableView.getRoot().getChildren().get(0).getValue().setFirstName("This is a big text inside that column");

        assertEquals(
                width, column.getWidth(), 0.001, "Width must be the same");

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue(
                width < column.getWidth(), "Column width must be greater");

        //Back to initial value
        tableView.getRoot().getChildren().get(0).getValue().setFirstName(NAME0);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertEquals(width, column.getWidth(), 0.001, "Width must be equal to initial value");
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header with first column decrease
     */
    @Test
    public void test_resizeColumnToFitContentDecrease() {
        TreeTableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        tableView.getRoot().getChildren().get(0).getValue().setFirstName("small");
        tableView.getRoot().getChildren().get(1).getValue().setFirstName("small");
        tableView.getRoot().getChildren().get(2).getValue().setFirstName("small");
        tableView.getRoot().getChildren().get(3).getValue().setFirstName("small");

        assertEquals(
                width, column.getWidth(), 0.001, "Width must be the same");

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue(
                width > column.getWidth(), "Column width must be smaller");

        //Back to initial value
        tableView.getRoot().getChildren().get(0).getValue().setFirstName(NAME0);
        tableView.getRoot().getChildren().get(1).getValue().setFirstName(NAME1);
        tableView.getRoot().getChildren().get(2).getValue().setFirstName(NAME2);
        tableView.getRoot().getChildren().get(3).getValue().setFirstName(NAME3);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertEquals(width, column.getWidth(), 0.001, "Width must be equal to initial value");
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header itself
     */
    @Test
    public void test_resizeColumnToFitContentHeader() {
        TreeTableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        column.setText("This is a big text inside that column");

        assertEquals(width, column.getWidth(), 0.001, "Width must be the same");

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue(width < column.getWidth(), "Column width must be greater");

        //Back to initial value
        column.setText("Col");
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, 3);
        assertEquals(width, column.getWidth(), 0.001, "Width must be equal to initial value");
    }

    /**
     * @test
     * @bug 8207957
     * Resize the column header with only 3 first rows
     */
    @Test
    public void test_resizeColumnToFitContentMaxRow() {
        TreeTableColumn column = tableView.getColumns().get(0);
        double width = column.getWidth();

        tableView.getRoot().getChildren().get(3).getValue().setFirstName("This is a big text inside that column");

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, 3);
        assertEquals(width, column.getWidth(), 0.001, "Width must be the same");

        //Back to initial value
        tableView.getRoot().getChildren().get(3).getValue().setFirstName(NAME3);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, 3);
        assertEquals(width, column.getWidth(), 0.001, "Width must be equal to initial value");
    }

    /** Row style must affect the required column width */
    @Test
    public void test_resizeColumnToFitContentRowStyle() {
        TreeTableColumn column = tableView.getColumns().get(0);

        tableView.setRowFactory(this::createSmallRow);
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        double width = column.getWidth();

        tableView.setRowFactory(this::createLargeRow);
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        assertTrue(width < column.getWidth(), "Column width must be greater");
    }

    /** Test resizeColumnToFitContent in the presence of a non-standard row skin */
    @Test
    public void test_resizeColumnToFitContentCustomRowSkin() {
        TreeTableColumn column = tableView.getColumns().get(0);

        tableView.setRowFactory(this::createCustomRow);
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);
        double width = column.getWidth();
        assertTrue(width > 0);
    }

    /**
     * We expect that the css of the label is processed after the resizing took place,
     * since it is needed to correctly measure the size.
     */
    @Test
    public void testResizeColumnToFitContentCssIsApplied() {
        Label label = TableColumnHeaderShim.getLabel(firstColumnHeader);
        label.setStyle("-fx-font-size: 24px;");
        firstColumnHeader.getTableColumn().setText("longlonglonglong");

        assertEquals(12, label.getFont().getSize(), 0);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        assertEquals(24, label.getFont().getSize(), 0);
    }

    /**
     * A table column with a graphic {@link Text} should be bigger than without.
     */
    @Test
    public void testResizeColumnToFitContentWithGraphicText() {
        TableColumnBase<?, ?> tableColumn = firstColumnHeader.getTableColumn();

        tableColumn.setText("longlonglonglonglonglonglonglong");
        tableColumn.setGraphic(new Text("longlonglonglong"));
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        double widthWithGraphic = tableColumn.getWidth();

        tableColumn.setGraphic(null);
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        double width = tableColumn.getWidth();

        assertTrue(widthWithGraphic > width);
    }

    /**
     * A table column with a graphic {@link Label} should be bigger than without.
     */
    @Test
    public void testResizeColumnToFitContentWithGraphicLabel() {
        TableColumnBase<?, ?> tableColumn = firstColumnHeader.getTableColumn();

        tableColumn.setText("longlonglonglonglonglonglonglong");
        tableColumn.setGraphic(new Label("longlonglonglong"));
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        double widthWithGraphic = tableColumn.getWidth();

        tableColumn.setGraphic(null);
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        double width = tableColumn.getWidth();

        assertTrue(widthWithGraphic > width);
    }

    /**
     * The content display should also be taken into consideration when measuring the width.
     * See also: <a href="https://bugs.openjdk.org/browse/JDK-8186188">JDK-8186188</a>
     */
    @Test
    public void testResizeColumnToFitContentWithGraphicAlignment() {
        TableColumnBase<?, ?> tableColumn = firstColumnHeader.getTableColumn();

        tableColumn.setText("longlonglonglonglonglonglonglong");
        tableColumn.setGraphic(new Text("longlonglonglong"));

        Label label = TableColumnHeaderShim.getLabel(firstColumnHeader);

        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        double widthWithGraphic = tableColumn.getWidth();

        label.setContentDisplay(ContentDisplay.BOTTOM);
        TableColumnHeaderShim.resizeColumnToFitContent(firstColumnHeader, -1);

        double width = tableColumn.getWidth();

        assertTrue(widthWithGraphic > width);
    }

    private TreeTableRow<Person> createCustomRow(TreeTableView<Person> tableView) {
        return new TreeTableRow<>() {
            protected Skin<?> createDefaultSkin() {
                return new CustomSkin(this);
            }
        };
    }

    private static class CustomSkin implements Skin<TreeTableRow<?>> {

        private TreeTableRow<?> row;
        private Node node = new HBox();

        CustomSkin(TreeTableRow<?> row) {
            this.row = row;
        }

        @Override
        public TreeTableRow<?> getSkinnable() {
            return row;
        }

        @Override
        public Node getNode() {
            return node;
        }

        @Override
        public void dispose() {
            node = null;
        }
    }

    private TreeTableRow<Person> createSmallRow(TreeTableView<Person> tableView) {
        TreeTableRow<Person> row = new TreeTableRow<>();
        row.setStyle("-fx-font: 24 System");
        return row;
    }

    private TreeTableRow<Person> createLargeRow(TreeTableView<Person> param) {
        TreeTableRow<Person> row = new TreeTableRow<>();
        row.setStyle("-fx-font: 48 System");
        return row;
    }

}
