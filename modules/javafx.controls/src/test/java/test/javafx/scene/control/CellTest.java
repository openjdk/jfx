/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
import javafx.scene.control.CellShim;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableCellShim;
import javafx.scene.control.TableRow;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableCellShim;
import javafx.scene.control.TreeTableRow;
import javafx.stage.Stage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;

/**
 */
public class CellTest {
    private static Stream<Class> parameters() {
        return Stream.of(
                Cell.class,
                ListCell.class,
                TableRow.class,
                // Note: We use the shim here, so we can lock the item. The behaviour is the same otherwise.
                TableCellShim.class,
                TreeCell.class,
                TreeTableRow.class,
                // Note: We use the shim here, so we can lock the item.  The behaviour is the same otherwise.
                TreeTableCellShim.class
        );
    }

    private Cell<String> cell;

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    private void setup(Class<?> type) {
        try {
            cell = (Cell<String>)type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Empty TableCells can be selected, as long as the row they exist in
        // is not empty, so here we set a TableRow to ensure testing works
        // properly
        if (cell instanceof TableCell) {
            TableRow tableRow = new TableRow();
            CellShim.updateItem(tableRow, "TableRow", false);
            ((TableCell)cell).updateTableRow(tableRow);
            ((TableCellShim)cell).setLockItemOnStartEdit(true);
        } else if (cell instanceof TreeTableCell) {
            TreeTableRow tableRow = new TreeTableRow();
            CellShim.updateItem(tableRow, "TableRow", false);
            ((TreeTableCell)cell).updateTableRow(tableRow);
            ((TreeTableCellShim)cell).setLockItemOnStartEdit(true);
        }
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @ParameterizedTest
    @MethodSource("parameters")
    public void cellsShouldBeNonFocusableByDefault(Class<?> c) {
        setup(c);
        // Cells are non-focusable because we manually position the focus from
        // the ListView / TableView / TreeView skin, rather than making them
        // focus traversable and having directional focus work etc. We must
        // keep the focus on the actual table component UNLESS we are
        // editing, in which case it is set on the cell itself.
        assertFalse(cell.isFocusTraversable());
        assertFalse(cell.isFocused());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void styleClassShouldDefaultTo_cell(Class<?> c) {
        setup(c);
        ControlTestUtils.assertStyleClassContains(cell, "cell");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void pseudoClassStateShouldBe_empty_ByDefault(Class<?> c) {
        setup(c);
        ControlTestUtils.assertPseudoClassExists(cell, "empty");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "filled");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "selected");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "focused");
    }

    /*********************************************************************
     * Tests for updating the item, selection, editable                  *
     ********************************************************************/

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingItemAffectsBothItemAndEmpty(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        assertEquals("Apples", cell.getItem());
        assertFalse(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingItemWithEmptyTrueAndItemNotNullIsWeirdButOK(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Weird!", true);
        assertEquals("Weird!", cell.getItem());
        assertTrue(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingItemWithEmptyFalseAndNullItemIsOK(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, null, false);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void selectingANonEmptyCellIsOK(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        assertTrue(cell.isSelected());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void unSelectingANonEmptyCellIsOK(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        cell.updateSelected(false);
        assertFalse(cell.isSelected());
    }

    public void selectingAnEmptyCellResultsInNoChange(Class<?> c) {
        CellShim.updateItem(cell, null, true);
        cell.updateSelected(true);
        assertFalse(cell.isSelected());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingASelectedCellToBeEmptyClearsSelection(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        CellShim.updateItem(cell, null, true);
        assertFalse(cell.isSelected());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingItemWithEmptyTrueResultsIn_empty_pseudoClassAndNot_filled(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, null, true);
        ControlTestUtils.assertPseudoClassExists(cell, "empty");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "filled");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingItemWithEmptyFalseResultsIn_filled_pseudoClassAndNot_empty(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, null, false);
        ControlTestUtils.assertPseudoClassExists(cell, "filled");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "empty");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingSelectedToTrueResultsIn_selected_pseudoClass(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Pears", false);
        cell.updateSelected(true);
        ControlTestUtils.assertPseudoClassExists(cell, "selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingSelectedToFalseResultsInNo_selected_pseudoClass(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Pears", false);
        cell.updateSelected(true);
        cell.updateSelected(false);
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void editableIsTrueByDefault(Class<?> c) {
        setup(c);
        assertTrue(cell.isEditable());
        assertTrue(cell.editableProperty().get());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void editableCanBeSet(Class<?> c) {
        setup(c);
        cell.setEditable(false);
        assertFalse(cell.isEditable());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void editableSetToNonDefaultValueIsReflectedInModel(Class<?> c) {
        setup(c);
        cell.setEditable(false);
        assertFalse(cell.editableProperty().get());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void editableCanBeCleared(Class<?> c) {
        setup(c);
        cell.setEditable(false);
        cell.setEditable(true);
        assertTrue(cell.isEditable());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void editableCanBeBound(Class<?> c) {
        setup(c);
        BooleanProperty other = new SimpleBooleanProperty(false);
        cell.editableProperty().bind(other);
        assertFalse(cell.isEditable());
        other.set(true);
        assertTrue(cell.isEditable());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void cannotSpecifyEditableViaCSS(Class<?> c) {
        setup(c);
        cell.setStyle("-fx-editable: false;");
        cell.applyCss();
        assertTrue(cell.isEditable());

        cell.setEditable(false);
        assertFalse(cell.isEditable());

        cell.setStyle("-fx-editable: true;");
        cell.applyCss();
        assertFalse(cell.isEditable());
    }

    /*********************************************************************
     * Tests for editing                                                 *
     ********************************************************************/

    @ParameterizedTest
    @MethodSource("parameters")
    public void editingAnEmptyCellResultsInNoChange(Class<?> c) {
        setup(c);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void editingAnEmptyCellResultsInNoChange2(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, null, false);
        CellShim.updateItem(cell, null, true);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingACellBeingEditedDoesNotResultInACancelOfEdit(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertFalse(cell.isEmpty());
        assertTrue(cell.isEditing());
        CellShim.updateItem(cell, "Oranges", false);
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void updatingACellBeingEditedDoesNotResultInACancelOfEdit2(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertFalse(cell.isEmpty());
        assertTrue(cell.isEditing());
        CellShim.updateItem(cell, null, true);
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void startEditWhenEditableIsTrue(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void startEditWhenEditableIsFalse(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void startEditWhileAlreadyEditingIsIgnored(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.startEdit();
        assertTrue(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void cancelEditWhenEditableIsTrue(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.cancelEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void cancelEditWhenEditableIsFalse(Class<?> c) {
        setup(c);
       CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        cell.cancelEdit();
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void commitEditWhenEditableIsTrue(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.commitEdit("Oranges");
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void commitEditWhenEditableIsFalse(Class<?> c) {
        setup(c);
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        cell.commitEdit("Oranges");
        assertFalse(cell.isEditing());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getBeanIsCorrectForItemProperty(Class<?> c) {
        setup(c);
        assertSame(cell, cell.itemProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getNameIsCorrectForItemProperty(Class<?> c) {
        setup(c);
        assertEquals("item", cell.itemProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getBeanIsCorrectForEmptyProperty(Class<?> c) {
        setup(c);
        assertSame(cell, cell.emptyProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getNameIsCorrectForEmptyProperty(Class<?> c) {
        setup(c);
        assertEquals("empty", cell.emptyProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getBeanIsCorrectForSelectedProperty(Class<?> c) {
        setup(c);
        assertSame(cell, cell.selectedProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getNameIsCorrectForSelectedProperty(Class<?> c) {
        setup(c);
        assertEquals("selected", cell.selectedProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getBeanIsCorrectForEditingProperty(Class<?> c) {
        setup(c);
        assertSame(cell, cell.editingProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getNameIsCorrectForEditingProperty(Class<?> c) {
        setup(c);
        assertEquals("editing", cell.editingProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getBeanIsCorrectForEditableProperty(Class<?> c) {
        setup(c);
        assertSame(cell, cell.editableProperty().getBean());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getNameIsCorrectForEditableProperty(Class<?> c) {
        setup(c);
        assertEquals("editable", cell.editableProperty().getName());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void loseFocusWhileEditing(Class<?> c) {
        setup(c);
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
}
