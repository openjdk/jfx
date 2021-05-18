/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.Collection;
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;

import static org.junit.Assert.*;

/**
 */
@RunWith(Parameterized.class)
public class CellTest {
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                {Cell.class},
                {ListCell.class},
                {TableRow.class},
                {TableCell.class},
                {TreeCell.class},
                {TreeTableRow.class},
                {TreeTableCell.class}
        });
    }

    private Cell<String> cell;
    private Class type;

    public CellTest(Class type) {
        this.type = type;
    }

    @Before public void setup() throws Exception {
        cell = (Cell<String>) type.getDeclaredConstructor().newInstance();

        // Empty TableCells can be selected, as long as the row they exist in
        // is not empty, so here we set a TableRow to ensure testing works
        // properly
        if (cell instanceof TableCell) {
            TableRow tableRow = new TableRow();
            CellShim.updateItem(tableRow, "TableRow", false);
            ((TableCell)cell).updateTableRow(tableRow);
            TableCellShim.set_lockItemOnEdit((TableCell)cell, true);
        } else if (cell instanceof TreeTableCell) {
            TreeTableRow tableRow = new TreeTableRow();
            CellShim.updateItem(tableRow, "TableRow", false);
            ((TreeTableCell)cell).updateTreeTableRow(tableRow);
            TreeTableCellShim.set_lockItemOnEdit((TreeTableCell)cell, true);
        }
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void cellsShouldBeNonFocusableByDefault() {
        // Cells are non-focusable because we manually position the focus from
        // the ListView / TableView / TreeView skin, rather than making them
        // focus traversable and having directional focus work etc. We must
        // keep the focus on the actual table component UNLESS we are
        // editing, in which case it is set on the cell itself.
        assertFalse(cell.isFocusTraversable());
        assertFalse(cell.isFocused());
    }

    @Test public void styleClassShouldDefaultTo_cell() {
        ControlTestUtils.assertStyleClassContains(cell, "cell");
    }

    @Test public void pseudoClassStateShouldBe_empty_ByDefault() {
        ControlTestUtils.assertPseudoClassExists(cell, "empty");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "filled");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "selected");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "focused");
    }

    /*********************************************************************
     * Tests for updating the item, selection, editable                  *
     ********************************************************************/

    @Test public void updatingItemAffectsBothItemAndEmpty() {
        CellShim.updateItem(cell, "Apples", false);
        assertEquals("Apples", cell.getItem());
        assertFalse(cell.isEmpty());
    }

    @Test public void updatingItemWithEmptyTrueAndItemNotNullIsWeirdButOK() {
        CellShim.updateItem(cell, "Weird!", true);
        assertEquals("Weird!", cell.getItem());
        assertTrue(cell.isEmpty());
    }

    @Test public void updatingItemWithEmptyFalseAndNullItemIsOK() {
        CellShim.updateItem(cell, null, false);
        assertNull(cell.getItem());
        assertFalse(cell.isEmpty());
    }

    @Test public void selectingANonEmptyCellIsOK() {
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        assertTrue(cell.isSelected());
    }

    @Test public void unSelectingANonEmptyCellIsOK() {
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        cell.updateSelected(false);
        assertFalse(cell.isSelected());
    }

    public void selectingAnEmptyCellResultsInNoChange() {
        CellShim.updateItem(cell, null, true);
        cell.updateSelected(true);
        assertFalse(cell.isSelected());
    }

    @Test public void updatingASelectedCellToBeEmptyClearsSelection() {
        CellShim.updateItem(cell, "Oranges", false);
        cell.updateSelected(true);
        CellShim.updateItem(cell, null, true);
        assertFalse(cell.isSelected());
    }

    @Test public void updatingItemWithEmptyTrueResultsIn_empty_pseudoClassAndNot_filled() {
        CellShim.updateItem(cell, null, true);
        ControlTestUtils.assertPseudoClassExists(cell, "empty");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "filled");
    }

    @Test public void updatingItemWithEmptyFalseResultsIn_filled_pseudoClassAndNot_empty() {
        CellShim.updateItem(cell, null, false);
        ControlTestUtils.assertPseudoClassExists(cell, "filled");
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "empty");
    }

    @Test public void updatingSelectedToTrueResultsIn_selected_pseudoClass() {
        CellShim.updateItem(cell, "Pears", false);
        cell.updateSelected(true);
        ControlTestUtils.assertPseudoClassExists(cell, "selected");
    }

    @Test public void updatingSelectedToFalseResultsInNo_selected_pseudoClass() {
        CellShim.updateItem(cell, "Pears", false);
        cell.updateSelected(true);
        cell.updateSelected(false);
        ControlTestUtils.assertPseudoClassDoesNotExist(cell, "selected");
    }

    @Test public void editableIsTrueByDefault() {
        assertTrue(cell.isEditable());
        assertTrue(cell.editableProperty().get());
    }

    @Test public void editableCanBeSet() {
        cell.setEditable(false);
        assertFalse(cell.isEditable());
    }

    @Test public void editableSetToNonDefaultValueIsReflectedInModel() {
        cell.setEditable(false);
        assertFalse(cell.editableProperty().get());
    }

    @Test public void editableCanBeCleared() {
        cell.setEditable(false);
        cell.setEditable(true);
        assertTrue(cell.isEditable());
    }

    @Test public void editableCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        cell.editableProperty().bind(other);
        assertFalse(cell.isEditable());
        other.set(true);
        assertTrue(cell.isEditable());
    }

    @Test public void cannotSpecifyEditableViaCSS() {
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

    @Test public void editingAnEmptyCellResultsInNoChange() {
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @Test public void editingAnEmptyCellResultsInNoChange2() {
        CellShim.updateItem(cell, null, false);
        CellShim.updateItem(cell, null, true);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @Test public void updatingACellBeingEditedDoesNotResultInACancelOfEdit() {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertFalse(cell.isEmpty());
        assertTrue(cell.isEditing());
        CellShim.updateItem(cell, "Oranges", false);
        assertTrue(cell.isEditing());
    }

    @Test public void updatingACellBeingEditedDoesNotResultInACancelOfEdit2() {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertFalse(cell.isEmpty());
        assertTrue(cell.isEditing());
        CellShim.updateItem(cell, null, true);
        assertTrue(cell.isEditing());
    }

    @Test public void startEditWhenEditableIsTrue() {
        if ((cell instanceof TableCell)) {
            TableCellShim.set_lockItemOnEdit((TableCell) cell, true);
        }
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        assertTrue(cell.isEditing());
    }

    @Test public void startEditWhenEditableIsFalse() {
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
    }

    @Test public void startEditWhileAlreadyEditingIsIgnored() {
        if (cell instanceof TableCell) {
            TableCellShim.set_lockItemOnEdit((TableCell) cell, true);
        }
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.startEdit();
        assertTrue(cell.isEditing());
    }

    @Test public void cancelEditWhenEditableIsTrue() {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.cancelEdit();
        assertFalse(cell.isEditing());
    }

    @Test public void cancelEditWhenEditableIsFalse() {
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        cell.cancelEdit();
        assertFalse(cell.isEditing());
    }

    @Test public void commitEditWhenEditableIsTrue() {
        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.commitEdit("Oranges");
        assertFalse(cell.isEditing());
    }

    @Test public void commitEditWhenEditableIsFalse() {
        CellShim.updateItem(cell, "Apples", false);
        cell.setEditable(false);
        cell.startEdit();
        cell.commitEdit("Oranges");
        assertFalse(cell.isEditing());
    }

    @Test public void getBeanIsCorrectForItemProperty() {
        assertSame(cell, cell.itemProperty().getBean());
    }

    @Test public void getNameIsCorrectForItemProperty() {
        assertEquals("item", cell.itemProperty().getName());
    }

    @Test public void getBeanIsCorrectForEmptyProperty() {
        assertSame(cell, cell.emptyProperty().getBean());
    }

    @Test public void getNameIsCorrectForEmptyProperty() {
        assertEquals("empty", cell.emptyProperty().getName());
    }

    @Test public void getBeanIsCorrectForSelectedProperty() {
        assertSame(cell, cell.selectedProperty().getBean());
    }

    @Test public void getNameIsCorrectForSelectedProperty() {
        assertEquals("selected", cell.selectedProperty().getName());
    }

    @Test public void getBeanIsCorrectForEditingProperty() {
        assertSame(cell, cell.editingProperty().getBean());
    }

    @Test public void getNameIsCorrectForEditingProperty() {
        assertEquals("editing", cell.editingProperty().getName());
    }

    @Test public void getBeanIsCorrectForEditableProperty() {
        assertSame(cell, cell.editableProperty().getBean());
    }

    @Test public void getNameIsCorrectForEditableProperty() {
        assertEquals("editable", cell.editableProperty().getName());
    }

    // When the cell was focused, but is no longer focused, we should cancel editing
    // Check for focused pseudoClass state change?
    @Ignore(value = "I'm not sure how to test this, since I need a scene & such to move focus around")
    @Test public void loseFocusWhileEditing() {
        Button other = new Button();
        Group root = new Group(other, cell);
        Scene scene = new Scene(root);

        CellShim.updateItem(cell, "Apples", false);
        cell.startEdit();
        cell.requestFocus();

        other.requestFocus();

        assertFalse(cell.isEditing());
    }
}
