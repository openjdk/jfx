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

package test.javafx.scene.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Parameterized tests for the {@link TableCell#startEdit()} method of {@link TableCell} and all sub implementations.
 * The {@link CheckBoxTableCell} is special as in there the checkbox will be disabled based of the editability.
 */
@RunWith(Parameterized.class)
public class TableCellStartEditTest {

    private static final boolean[] EDITABLE_STATES = { true, false };

    private TableView<String> table;
    private TableRow<String> tableRow;
    private TableColumn<String, ?> tableColumn;

    private final Supplier<TableCell<String, ?>> tableCellSupplier;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return wrapAsObjectArray(List.of(TableCell::new, ComboBoxTableCell::new, TextFieldTableCell::new,
                ChoiceBoxTableCell::new, CheckBoxTableCell::new, ProgressBarTableCell::new));
    }

    private static Collection<Object[]> wrapAsObjectArray(List<Supplier<TableCell<Object, ?>>> tableCells) {
        return tableCells.stream().map(tc -> new Object[] { tc }).collect(toList());
    }

    public TableCellStartEditTest(Supplier<TableCell<String, ?>> tableCellSupplier) {
        this.tableCellSupplier = tableCellSupplier;
    }

    @Before
    public void setup() {
        ObservableList<String> items = FXCollections.observableArrayList("1", "2", "3");
        table = new TableView<>(items);

        tableColumn = new TableColumn<>();
        table.getColumns().add(tableColumn);

        tableRow = new TableRow<>();
    }

    @Test
    public void testStartEditMustNotThrowNPE() {
        TableCell<String, ?> tableCell = tableCellSupplier.get();
        // A table cell without anything attached should not throw a NPE.
        tableCell.startEdit();
    }

    @Test
    public void testStartEditRespectsEditable() {
        TableCell<String, ?> tableCell = tableCellSupplier.get();
        tableCell.updateIndex(0);

        tableCell.updateTableColumn(tableColumn);
        tableCell.updateTableRow(tableRow);
        tableCell.updateTableView(table);

        for (boolean isTableEditable : EDITABLE_STATES) {
            for (boolean isColumnEditable : EDITABLE_STATES) {
                for (boolean isRowEditable : EDITABLE_STATES) {
                    for (boolean isCellEditable : EDITABLE_STATES) {
                        testStartEditImpl(tableCell, isTableEditable, isColumnEditable, isRowEditable, isCellEditable);
                    }
                }
            }
        }
    }

    /**
     * A {@link TableCell} (or sub implementation) should be editable (thus, can be in editing state), if the
     * corresponding table, column, row  and cell is editable.
     *
     * @param tableCell the {@link TableCell} where the <code>startEdit</code> method is tested
     * @param isTableEditable true, when the table should be editable, false otherwise
     * @param isColumnEditable true, when the column should be editable, false otherwise
     * @param isRowEditable true, when the row should be editable, false otherwise
     * @param isCellEditable true, when the cell should be editable, false otherwise
     */
    private void testStartEditImpl(TableCell<String, ?> tableCell, boolean isTableEditable, boolean isColumnEditable, boolean isRowEditable,
            boolean isCellEditable) {
        assertFalse(tableCell.isEditing());

        table.setEditable(isTableEditable);
        tableColumn.setEditable(isColumnEditable);
        tableRow.setEditable(isRowEditable);
        tableCell.setEditable(isCellEditable);

        tableCell.startEdit();

        // Only when the table, column, row and the cell itself is editable, it can get in editing state.
        boolean expectedEditingState = isTableEditable && isColumnEditable && isRowEditable && isCellEditable;
        assertEquals(expectedEditingState, tableCell.isEditing());

        // Ignored until https://bugs.openjdk.java.net/browse/JDK-8270042 is resolved.
        // Special check for CheckBoxTableCell.
//        if (tableCell instanceof CheckBoxTableCell) {
//            assertEquals(expectedEditingState, !tableCell.getGraphic().isDisabled());
//        }

        // Restore the editing state.
        tableCell.cancelEdit();
    }

}
