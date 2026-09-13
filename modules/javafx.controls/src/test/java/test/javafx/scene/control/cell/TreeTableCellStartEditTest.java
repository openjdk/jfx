/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.cell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.ChoiceBoxTreeTableCell;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.control.cell.ProgressBarTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized tests for the {@link TreeTableCell#startEdit()} method of {@link TreeTableCell} and all sub
 * implementations. The {@link CheckBoxTreeTableCell} is special as in there the checkbox will be disabled based of the
 * editability.
 */
public class TreeTableCellStartEditTest {

    private static final boolean[] EDITABLE_STATES = { true, false };

    private TreeTableView<String> treeTable;
    private TreeTableRow<String> treeTableRow;
    private TreeTableColumn<String, ?> treeTableColumn;
    private TreeTableCell<String, ?> treeTableCell;

    private static Collection<Supplier<TreeTableCell<String, ?>>> parameters() {
        return List.of(
            TreeTableCell::new,
            ComboBoxTreeTableCell::new,
            TextFieldTreeTableCell::new,
            ChoiceBoxTreeTableCell::new,
            CheckBoxTreeTableCell::new,
            ProgressBarTreeTableCell::new
        );
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Supplier<TreeTableCell<String, ?>> treeTableCellSupplier) {
        TreeItem<String> root = new TreeItem<>("1");
        root.getChildren().addAll(List.of(new TreeItem<>("2"), new TreeItem<>("3")));
        treeTable = new TreeTableView<>(root);

        treeTableColumn = new TreeTableColumn<>();
        treeTable.getColumns().add(treeTableColumn);

        treeTableRow = new TreeTableRow<>();

        treeTableCell = treeTableCellSupplier.get();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEditMustNotThrowNPE(Supplier<TreeTableCell<String, ?>> treeTableCellSupplier) {
        setup(treeTableCellSupplier);
        // A tree table cell without anything attached should not throw a NPE.
        treeTableCell.startEdit();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEditRespectsEditable(Supplier<TreeTableCell<String, ?>> treeTableCellSupplier) {
        setup(treeTableCellSupplier);
        treeTableCell.updateIndex(0);

        treeTableCell.updateTableColumn((TreeTableColumn) treeTableColumn);
        treeTableCell.updateTableRow(treeTableRow);
        treeTableCell.updateTreeTableView(treeTable);

        for (boolean isTableEditable : EDITABLE_STATES) {
            for (boolean isColumnEditable : EDITABLE_STATES) {
                for (boolean isRowEditable : EDITABLE_STATES) {
                    for (boolean isCellEditable : EDITABLE_STATES) {
                        testStartEditImpl(isTableEditable, isColumnEditable, isRowEditable, isCellEditable);
                    }
                }
            }
        }
    }

    /**
     * A {@link TreeTableCell} (or sub implementation) should be editable (thus, can be in editing state), if the
     * corresponding tree table, column, row and cell is editable.
     *
     * @param isTreeTableEditable true, when the tree table should be editable, false otherwise
     * @param isColumnEditable true, when the column should be editable, false otherwise
     * @param isRowEditable true, when the row should be editable, false otherwise
     * @param isCellEditable true, when the cell should be editable, false otherwise
     */
    private void testStartEditImpl(boolean isTreeTableEditable, boolean isColumnEditable, boolean isRowEditable,
            boolean isCellEditable) {
        assertFalse(treeTableCell.isEditing());

        treeTable.setEditable(isTreeTableEditable);
        treeTableColumn.setEditable(isColumnEditable);
        treeTableRow.setEditable(isRowEditable);
        treeTableCell.setEditable(isCellEditable);

        treeTableCell.startEdit();

        boolean expectedEditingState = isTreeTableEditable && isColumnEditable && isRowEditable && isCellEditable;
        assertEquals(expectedEditingState, treeTableCell.isEditing());

        if (treeTableCell instanceof CheckBoxTreeTableCell) {
            assertNotNull(treeTableCell.getGraphic());
            // Ignored until https://bugs.openjdk.org/browse/JDK-8270042 is resolved.
            // Check if the checkbox is disabled when not editable.
            // assertEquals(expectedEditingState, !treeTableCell.getGraphic().isDisabled());
        } else if (treeTableCell instanceof ProgressBarTreeTableCell) {
            // The progress bar is always shown.
            assertNotNull(treeTableCell.getGraphic());
        } else if (!treeTableCell.getClass().equals(TreeTableCell.class)) {
            // All other sub implementation should show a graphic when editable.
            assertEquals(expectedEditingState, treeTableCell.getGraphic() != null);
        }

        // Restore the editing state.
        treeTableCell.cancelEdit();
    }

}
