/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.cell;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChoiceBoxTreeTableCellTest {
    
    private StringConverter<Object> converter;
    
    @Before public void setup() {
        converter = new StringConverter<Object>() {
            @Override public String toString(Object object) {
                return null;
            }

            @Override public Object fromString(String string) {
                return null;
            }
        };
    }
    
    /**************************************************************************
     * 
     * Test for public static Callback<TreeTableColumn<T>, TreeTableCell<T>> forTreeTableColumn(T... items)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellFactoryIsNotNull() {
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn();
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellFactoryCreatesCells() {
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn();
        
        TreeTableColumn<String,String> tableColumn = new TreeTableColumn<>();
        ChoiceBoxTreeTableCell<String,String> cell = (ChoiceBoxTreeTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellHasNonNullStringConverter() {
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn();

        TreeTableColumn<String,String> tableColumn = new TreeTableColumn<>();
        ChoiceBoxTreeTableCell<String,String> cell = (ChoiceBoxTreeTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TreeTableColumn<T>, TreeTableCell<T>> forTreeTableColumn(
     *       final ObservableList<T> items)
     * 
     **************************************************************************/

    @Test public void testStatic_forTreeTableColumn_items_ensureSuccessWhenItemsIsNull() {
        ObservableList<String> items = null;
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn(items);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forTreeTableColumn_items_ensureCellFactoryIsNotNull() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn(items);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forTreeTableColumn_items_ensureCellFactoryCreatesCells() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn(items);

        TreeTableColumn<String,String> tableColumn = new TreeTableColumn<>();
        ChoiceBoxTreeTableCell<String,String> cell = (ChoiceBoxTreeTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    @Test public void testStatic_forTreeTableColumn_items_ensureCellHasNonNullStringConverter() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<TreeTableColumn<String,String>, TreeTableCell<String,String>> cellFactory = ChoiceBoxTreeTableCell.forTreeTableColumn(items);

        TreeTableColumn<String,String> tableColumn = new TreeTableColumn<>();
        ChoiceBoxTreeTableCell<String,String> cell = (ChoiceBoxTreeTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
    }

    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    @Test public void testConstructor_noArgs_defaultStringConverterIsNotNull() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        assertTrue(cell.getStyleClass().contains("choice-box-tree-table-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsNull() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        assertNull(cell.getGraphic());
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for one-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_converter_defaultStringConverterIsNotNull() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(converter);
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_converter_defaultStyleClass() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(converter);
        assertTrue(cell.getStyleClass().contains("choice-box-tree-table-cell"));
    }
    
    @Test public void testConstructor_converter_defaultGraphicIsACheckBox() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(converter);
        assertNull(cell.getGraphic());
    }


    /**************************************************************************
     *
     * Constructor tests for one-arg (varargs items) constructor
     *
     **************************************************************************/

    @Test public void testConstructor_varargs_defaultStringConverterIsNotNull() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(items);
        assertNotNull(cell.getConverter());
    }

    @Test public void testConstructor_varargs_defaultStyleClass() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(items);
        assertTrue(cell.getStyleClass().contains("choice-box-tree-table-cell"));
    }

    @Test public void testConstructor_varargs_defaultGraphicIsACheckBox() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(items);
        assertNull(cell.getGraphic());
    }

    @Test public void testConstructor_varargs_itemsListIsNotNullOrEmpty() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>(items);
        assertNotNull(cell.getItems());
        assertEquals(3, cell.getItems().size());
    }
    
    
    /**************************************************************************
     * 
     * updateItem tests
     * 
     **************************************************************************/

    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getGraphic());
    }
    
    @Test public void test_updateItem_isEmpty_textIsNull() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nullConverter() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.setConverter(null);
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nonNullConverter() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.setConverter(
                new StringConverter<Object>() {
                    @Override public Object fromString(String string) {
                        return null;
                    }

                    @Override public String toString(Object object) {
                        return "CONVERTED";
                    }
                });
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("CONVERTED", cell.getText());
    }


    /**************************************************************************
     *
     * editing tests
     *
     **************************************************************************/

    // --- is Empty
    @Test public void test_startEdit_cellEditableIsFalse_isEmpty() {
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsFalse_isEmpty() {
        TreeTableView tableView = new TreeTableView();
        tableView.setEditable(false);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test(expected = NullPointerException.class)
    public void test_startEdit_cellEditableIsTrue_tableViewIsNull_isEmpty() {
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.setEditable(true);
        cell.startEdit();
    }

    @Test(expected = NullPointerException.class)
    public void test_startEdit_tableViewEditableIsTrue_tableColumnIsNull() {
        TreeTableView tableView = new TreeTableView();
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.startEdit();
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_isEmpty() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView();
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateTreeTableColumn(tc);

        tableView.edit(0, tc);
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_cellEditableIsTrue_isEmpty() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView();
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.setEditable(true);
        cell.updateTreeTableView(tableView);
        cell.updateTreeTableColumn(tc);

        tableView.edit(0, tc);
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    // --- is Not Empty
    @Test public void test_startEdit_cellEditableIsFalse_isNotEmpty() {
        ChoiceBoxTreeTableCell<Object, Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateItem("TEST", false);
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsFalse_isNotEmpty() {
        TreeTableView tableView = new TreeTableView();
        tableView.setEditable(false);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateItem("TEST", false);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test(expected = NullPointerException.class)
    public void test_startEdit_cellEditableIsTrue_tableViewIsNull_isNotEmpty() {
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateItem("TEST", false);
        cell.setEditable(true);
        cell.startEdit();
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_isNotEmpty() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView(new TreeItem("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateIndex(0);
        cell.updateTreeTableColumn(tc);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_cellEditableIsTrue_isNotEmpty() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView(new TreeItem("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateIndex(0);
        cell.updateTreeTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());
    }

    // --- cancel edit
    @Test public void test_cancelEdit_usingCellCancelEdit() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView(new TreeItem("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateIndex(0);
        cell.updateTreeTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());

        cell.cancelEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_cancelEdit_usingTableCancelEdit() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView(new TreeItem("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateIndex(0);
        cell.updateTreeTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());

        tableView.edit(-1, null);
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_rt_29320() {
        TreeTableColumn tc = new TreeTableColumn();
        TreeTableView tableView = new TreeTableView(new TreeItem("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ChoiceBoxTreeTableCell<Object,Object> cell = new ChoiceBoxTreeTableCell<>();
        cell.updateTreeTableView(tableView);
        cell.updateIndex(0);
        cell.updateTreeTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        ChoiceBox cb = (ChoiceBox) cell.getGraphic();

        // initially the choiceBox converter should equal the cell converter
        assertNotNull(cell.getConverter());
        assertNotNull(cb.getConverter());
        assertEquals(cell.getConverter(), cb.getConverter());

        // and if the cell changes the choicebox should follow
        cell.setConverter(null);
        assertNull(cb.getConverter());

        StringConverter<Object> customConverter = new StringConverter<Object>() {
            @Override public String toString(Object object) { return null; }
            @Override public Object fromString(String string) { return null; }
        };
        cell.setConverter(customConverter);
        assertEquals(customConverter, cb.getConverter());
    }
}
