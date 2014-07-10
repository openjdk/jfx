/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComboBoxTableCellTest {
    
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
     * Test for public static Callback<TableColumn<T>, TableCell<T>> forTableColumn(T... items)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTableColumn_noArgs_ensureCellFactoryIsNotNull() {
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn();
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTableColumn_noArgs_ensureCellFactoryCreatesCells() {
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn();
        
        TableColumn<String,String> tableColumn = new TableColumn<>();
        ComboBoxTableCell<String,String> cell = (ComboBoxTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    @Test public void testStatic_forTableColumn_noArgs_ensureCellHasNonNullStringConverter() {
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn();

        TableColumn<String,String> tableColumn = new TableColumn<>();
        ComboBoxTableCell<String,String> cell = (ComboBoxTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TableColumn<T>, TableCell<T>> forTableColumn(
     *       final ObservableList<T> items)
     * 
     **************************************************************************/

    @Test public void testStatic_forTableColumn_items_ensureSuccessWhenItemsIsNull() {
        ObservableList<String> items = null;
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn(items);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forTableColumn_items_ensureCellFactoryIsNotNull() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn(items);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forTableColumn_items_ensureCellFactoryCreatesCells() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn(items);

        TableColumn<String,String> tableColumn = new TableColumn<>();
        ComboBoxTableCell<String,String> cell = (ComboBoxTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    @Test public void testStatic_forTableColumn_items_ensureCellHasNonNullStringConverter() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<TableColumn<String,String>, TableCell<String,String>> cellFactory = ComboBoxTableCell.forTableColumn(items);

        TableColumn<String,String> tableColumn = new TableColumn<>();
        ComboBoxTableCell<String,String> cell = (ComboBoxTableCell<String,String>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
    }

    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    @Test public void testConstructor_noArgs_defaultStringConverterIsNotNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        assertTrue(cell.getStyleClass().contains("combo-box-table-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        assertNull(cell.getGraphic());
    }

    @Test public void testConstructor_noArgs_itemsListIsNotNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        assertNotNull(cell.getItems());
        assertTrue(cell.getItems().isEmpty());
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for one-arg (converter, no varargs items) constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_converter_defaultStringConverterIsNotNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(converter);
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_converter_defaultStyleClass() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(converter);
        assertTrue(cell.getStyleClass().contains("combo-box-table-cell"));
    }
    
    @Test public void testConstructor_converter_defaultGraphicIsACheckBox() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(converter);
        assertNull(cell.getGraphic());
    }

    @Test public void testConstructor_converter_itemsListIsNotNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(converter);
        assertNotNull(cell.getItems());
        assertTrue(cell.getItems().isEmpty());
    }

    /**************************************************************************
     *
     * Constructor tests for one-arg (varargs items) constructor
     *
     **************************************************************************/

    @Test public void testConstructor_varargs_defaultStringConverterIsNotNull() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(items);
        assertNotNull(cell.getConverter());
    }

    @Test public void testConstructor_varargs_defaultStyleClass() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(items);
        assertTrue(cell.getStyleClass().contains("combo-box-table-cell"));
    }

    @Test public void testConstructor_varargs_defaultGraphicIsACheckBox() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(items);
        assertNull(cell.getGraphic());
    }

    @Test public void testConstructor_varargs_itemsListIsNotNullOrEmpty() {
        Object[] items = new Object[] { "Item 1", "Item 2", "Item 3" };
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>(items);
        assertNotNull(cell.getItems());
        assertEquals(3, cell.getItems().size());
    }


    /**************************************************************************
     *
     * Property tests
     *
     **************************************************************************/

    @Test public void testComboBoxEditable_falseByDefault() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        assertFalse(cell.isComboBoxEditable());
    }

    @Test public void testComboBoxEditable_setter() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();

        cell.setComboBoxEditable(true);
        assertTrue(cell.isComboBoxEditable());

        cell.setComboBoxEditable(false);
        assertFalse(cell.isComboBoxEditable());
    }
    
    
    /**************************************************************************
     * 
     * updateItem tests
     * 
     **************************************************************************/

    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getGraphic());
    }
    
    @Test public void test_updateItem_isEmpty_textIsNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nullConverter() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        cell.setConverter(null);
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nonNullConverter() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
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
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsFalse_isEmpty() {
        TableView tableView = new TableView();
        tableView.setEditable(false);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test(expected = NullPointerException.class)
    public void test_startEdit_cellEditableIsTrue_tableViewIsNull_isEmpty() {
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.setEditable(true);
        cell.startEdit();
    }

    @Test(expected = NullPointerException.class)
    public void test_startEdit_tableViewEditableIsTrue_tableColumnIsNull() {
        TableView tableView = new TableView();
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.startEdit();
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_isEmpty() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView();
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateTableColumn(tc);

        tableView.edit(0, tc);
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_cellEditableIsTrue_isEmpty() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView();
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.setEditable(true);
        cell.updateTableView(tableView);
        cell.updateTableColumn(tc);

        tableView.edit(0, tc);
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    // --- is Not Empty
    @Test public void test_startEdit_cellEditableIsFalse_isNotEmpty() {
        ComboBoxTableCell<Object, Object> cell = new ComboBoxTableCell<>();
        cell.updateItem("TEST", false);
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsFalse_isNotEmpty() {
        TableView tableView = new TableView();
        tableView.setEditable(false);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateItem("TEST", false);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test(expected = NullPointerException.class)
    public void test_startEdit_cellEditableIsTrue_tableViewIsNull_isNotEmpty() {
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateItem("TEST", false);
        cell.setEditable(true);
        cell.startEdit();
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_isNotEmpty() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView(FXCollections.observableArrayList("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateIndex(0);
        cell.updateTableColumn(tc);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());
    }

    @Test public void test_startEdit_tableViewEditableIsTrue_cellEditableIsTrue_isNotEmpty() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView(FXCollections.observableArrayList("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateIndex(0);
        cell.updateTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());
    }

    // --- cancel edit
    @Test public void test_cancelEdit_usingCellCancelEdit() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView(FXCollections.observableArrayList("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateIndex(0);
        cell.updateTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());

        cell.cancelEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_cancelEdit_usingTableCancelEdit() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView(FXCollections.observableArrayList("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateIndex(0);
        cell.updateTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());

        tableView.edit(-1, null);
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_rt_29320() {
        TableColumn tc = new TableColumn();
        TableView tableView = new TableView(FXCollections.observableArrayList("TEST"));
        tableView.getColumns().add(tc);
        tableView.setEditable(true);
        ComboBoxTableCell<Object,Object> cell = new ComboBoxTableCell<>();
        cell.updateTableView(tableView);
        cell.updateIndex(0);
        cell.updateTableColumn(tc);
        cell.setEditable(true);

        tableView.edit(0, tc);
        ComboBox cb = (ComboBox) cell.getGraphic();

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
