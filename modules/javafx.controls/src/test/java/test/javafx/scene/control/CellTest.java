/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
import javafx.scene.control.CellShim;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListCellShim;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableCellShim;
import javafx.scene.control.TableRow;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeCellShim;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableCellShim;
import javafx.scene.control.TreeTableRow;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Tests the behavior that all rows and cells have in common.
 */
public class CellTest {

    private StageLoader stageLoader;

    @AfterEach
    public void afterEach() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    /**
     * All cell implementations, ready to use and not attached to any (virtualized) view.
     */
    private static Stream<Named<Cell<String>>> cells() {
        return Stream.of(
                named("Cell", new Cell<>()),
                named("ListCell", createListCell()),
                named("TableRow", new TableRow<>()),
                named("TableCell", createTableCell()),
                named("TreeCell", createTreeCell()),
                named("TreeTableRow", new TreeTableRow<>()),
                named("TreeTableCell", createTreeTableCell()));
    }

    private static Named<Cell<String>> named(String name, Cell<String> cell) {
        return Named.of(name, cell);
    }

    private static boolean isInsideARow(Cell<String> cell) {
        return cell instanceof TableCell || cell instanceof TreeTableCell;
    }

    private static ListCell<String> createListCell() {
        ListCellShim<String> cell = new ListCellShim<>();
        cell.setLockItemOnStartEdit(true);
        return cell;
    }

    private static TreeCell<String> createTreeCell() {
        TreeCellShim<String> cell = new TreeCellShim<>();
        cell.setLockItemOnStartEdit(true);
        return cell;
    }

    private static TableCell<String, String> createTableCell() {
        TableCellShim<String, String> cell = new TableCellShim<>();
        TableRow<String> tableRow = new TableRow<>();
        CellShim.updateItem(tableRow, "TableRow", false);
        cell.updateTableRow(tableRow);
        cell.setLockItemOnStartEdit(true);
        return cell;
    }

    private static TreeTableCell<String, String> createTreeTableCell() {
        TreeTableCellShim<String, String> cell = new TreeTableCellShim<>();
        TreeTableRow<String> tableRow = new TreeTableRow<>();
        CellShim.updateItem(tableRow, "TableRow", false);
        cell.updateTableRow(tableRow);
        cell.setLockItemOnStartEdit(true);
        return cell;
    }

    /* *********************************************************************
     * Tests for the constructors                                          *
     ***********************************************************************/

    @ParameterizedTest
    @MethodSource("cells")
    public void cellsShouldBeNonFocusableByDefault(Cell<String> cell) {
        // Cells are non-focusable because we manually position the focus from
        // the ListView / TableView / TreeView skin, rather than making them
        // focus traversable and having directional focus work etc. We must
        // keep the focus on the actual table component UNLESS we are
        // editing, in which case it is set on the cell itself.
        assertFalse(cell.isFocusTraversable());
        assertFalse(cell.isFocused());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void styleClassShouldDefaultTo_cell(Cell<String> cell) {
        ControlTestUtils.assertStyleClassContains(cell, "cell");
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void pseudoClassStateShouldBe_empty_ByDefault(Cell<String> cell) {
        ControlTestUtils.assertPseudoClassExists(cell, "empty");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "filled");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "selected");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "focused");
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void testFocusedPseudoClassIsSetWhenFocused(Cell<String> cell) {
        Button button = new Button();
        stageLoader = new StageLoader(new HBox(button, cell));

        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "focused");
        cell.requestFocus();
        ControlTestUtils.assertPseudoClassExists(cell, "focused");

        button.requestFocus();
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "focused");
    }

    /* *********************************************************************
     * Tests for updating the item, selection, editable                    *
     ***********************************************************************/

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingItemAffectsBothItemAndEmpty(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        assertEquals("Apples", cell.getItem());
        assertFalse(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingItemWithEmptyTrueAndItemNotNullIsWeirdButOK(Cell<String> cell) {
        CellShim.updateItem(cell, "Weird!", true);
        assertEquals("Weird!", cell.getItem());
        assertTrue(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingItemWithEmptyFalseAndNullItemIsOK(Cell<String> cell) {
        CellShim.updateItem(cell, null, false);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void selectingANonEmptyCellIsOK(Cell<String> cell) {
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        assertTrue(cell.isSelected());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void unSelectingANonEmptyCellIsOK(Cell<String> cell) {
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        cell.updateSelected(false);
        assertFalse(cell.isSelected());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void selectingAnEmptyCellResultsInNoChange(Cell<String> cell) {
        CellShim.updateItem(cell, null, true);
        cell.updateSelected(true);

        if (isInsideARow(cell)) {
            assertTrue(cell.isSelected());
        } else {
            assertFalse(cell.isSelected());
        }
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingASelectedCellToBeEmptyClearsSelection(Cell<String> cell) {
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        CellShim.updateItem(cell, null, true);
        assertFalse(cell.isSelected());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingItemWithEmptyTrueResultsIn_empty_pseudoClassAndNot_filled(Cell<String> cell) {
        CellShim.updateItem(cell, null, true);
        ControlTestUtils.assertPseudoClassExists(cell, "empty");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "filled");
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingItemWithEmptyFalseResultsIn_filled_pseudoClassAndNot_empty(Cell<String> cell) {
        CellShim.updateItem(cell, null, false);
        ControlTestUtils.assertPseudoClassExists(cell, "filled");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "empty");
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingSelectedToTrueResultsIn_selected_pseudoClass(Cell<String> cell) {
        CellShim.updateItem(cell, "Pears", false);
        cell.updateSelected(true);
        ControlTestUtils.assertPseudoClassExists(cell, "selected");
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingSelectedToFalseResultsInNo_selected_pseudoClass(Cell<String> cell) {
        CellShim.updateItem(cell, "Pears", false);
        cell.updateSelected(true);
        cell.updateSelected(false);
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "selected");
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void editableIsTrueByDefault(Cell<String> cell) {
        assertTrue(cell.isEditable());
        assertTrue(cell.editableProperty().get());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void editableCanBeSet(Cell<String> cell) {
        cell.setEditable(false);
        assertFalse(cell.isEditable());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void editableSetToNonDefaultValueIsReflectedInModel(Cell<String> cell) {
        cell.setEditable(false);
        assertFalse(cell.editableProperty().get());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void editableCanBeCleared(Cell<String> cell) {
        cell.setEditable(false);
        cell.setEditable(true);
        assertTrue(cell.isEditable());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void editableCanBeBound(Cell<String> cell) {
        BooleanProperty other = new SimpleBooleanProperty(false);
        cell.editableProperty().bind(other);
        assertFalse(cell.isEditable());
        other.set(true);
        assertTrue(cell.isEditable());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void cannotSpecifyEditableViaCSS(Cell<String> cell) {
        cell.setStyle("-fx-editable: false;");
        cell.applyCss();
        assertTrue(cell.isEditable());

        cell.setEditable(false);
        assertFalse(cell.isEditable());

        cell.setStyle("-fx-editable: true;");
        cell.applyCss();
        assertFalse(cell.isEditable());
    }

    /* *********************************************************************
     * Tests for editing                                                   *
     ***********************************************************************/

    @ParameterizedTest
    @MethodSource("cells")
    public void editingAnEmptyCellResultsInNoChange(Cell<String> cell) {
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void editingAnEmptyCellResultsInNoChange2(Cell<String> cell) {
        CellShim.updateItem(cell, null, false);
        CellShim.updateItem(cell, null, true);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingACellBeingEditedDoesNotResultInACancelOfEdit(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertFalse(cell.isEmpty());
        assertTrue(cell.isEditing());
        CellShim.updateItem(cell, "Oranges", false);
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void updatingACellBeingEditedDoesNotResultInACancelOfEdit2(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertFalse(cell.isEmpty());
        assertTrue(cell.isEditing());
        CellShim.updateItem(cell, null, true);
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void startEditWhenEditableIsTrue(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void startEditWhenEditableIsFalse(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void startEditWhileAlreadyEditingIsIgnored(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.startEdit();
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void cancelEditWhenEditableIsTrue(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.cancelEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void cancelEditWhenEditableIsFalse(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        cell.cancelEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void commitEditWhenEditableIsTrue(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.commitEdit("Oranges");
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void commitEditWhenEditableIsFalse(Cell<String> cell) {
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        cell.commitEdit("Oranges");
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getBeanIsCorrectForItemProperty(Cell<String> cell) {
        assertSame(cell, cell.itemProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getNameIsCorrectForItemProperty(Cell<String> cell) {
        assertEquals("item", cell.itemProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getBeanIsCorrectForEmptyProperty(Cell<String> cell) {
        assertSame(cell, cell.emptyProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getNameIsCorrectForEmptyProperty(Cell<String> cell) {
        assertEquals("empty", cell.emptyProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getBeanIsCorrectForSelectedProperty(Cell<String> cell) {
        assertSame(cell, cell.selectedProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getNameIsCorrectForSelectedProperty(Cell<String> cell) {
        assertEquals("selected", cell.selectedProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getBeanIsCorrectForEditingProperty(Cell<String> cell) {
        assertSame(cell, cell.editingProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getNameIsCorrectForEditingProperty(Cell<String> cell) {
        assertEquals("editing", cell.editingProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getBeanIsCorrectForEditableProperty(Cell<String> cell) {
        assertSame(cell, cell.editableProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void getNameIsCorrectForEditableProperty(Cell<String> cell) {
        assertEquals("editable", cell.editableProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void loseFocusWhileEditing(Cell<String> cell) {
        Button other = new Button();
        Group root = new Group(other, cell);
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        stage.requestFocus();
        Toolkit.getToolkit().firePulse();

        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertTrue(cell.isEditing());

        other.requestFocus();
        Toolkit.getToolkit().firePulse();

        assertFalse(cell.isEditing());
        stage.hide();
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void settingAnIndexWithoutAViewEmptiesTheCell(Cell<String> cell) {
        assumeTrue(cell instanceof IndexedCell<String>, "the plain Cell has no index");
        CellShim.updateItem(cell, "Apples", false);

        ((IndexedCell<String>) cell).updateIndex(0);

        assertNull(cell.getItem());
        assertTrue(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("cells")
    public void settingANegativeIndexWithoutAViewEmptiesTheCell(Cell<String> cell) {
        assumeTrue(cell instanceof IndexedCell<String>, "the plain Cell has no index");
        CellShim.updateItem(cell, "Apples", false);

        ((IndexedCell<String>) cell).updateIndex(-1);

        assertNull(cell.getItem());
        assertTrue(cell.isEmpty());
    }
}
