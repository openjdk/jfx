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
    
    
    /**************************************************************************
     * 
     * Constructor tests for one-arg constructor
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


//    /**************************************************************************
//     *
//     * editing tests
//     *
//     **************************************************************************/
//
//    // --- is Empty
//    @Test public void test_startEdit_cellEditableIsFalse_isEmpty() {
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.setEditable(false);
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_listViewEditableIsFalse_isEmpty() {
//        TableView listView = new TableView();
//        listView.setEditable(false);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateTableView(listView);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void test_startEdit_cellEditableIsTrue_listViewIsNull_isEmpty() {
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.setEditable(true);
//        cell.startEdit();
//    }
//
//    @Test public void test_startEdit_listViewEditableIsTrue_isEmpty() {
//        TableView listView = new TableView();
//        listView.setEditable(true);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateTableView(listView);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_listViewEditableIsTrue_cellEditableIsTrue_isEmpty() {
//        TableView listView = new TableView();
//        listView.setEditable(true);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.setEditable(true);
//        cell.updateTableView(listView);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    // --- is Not Empty
//    @Test public void test_startEdit_cellEditableIsFalse_isNotEmpty() {
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateItem("TEST", false);
//        cell.setEditable(false);
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_listViewEditableIsFalse_isNotEmpty() {
//        TableView listView = new TableView();
//        listView.setEditable(false);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateTableView(listView);
//        cell.updateItem("TEST", false);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void test_startEdit_cellEditableIsTrue_listViewIsNull_isNotEmpty() {
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateItem("TEST", false);
//        cell.setEditable(true);
//        cell.startEdit();
//    }
//
//    @Test public void test_startEdit_listViewEditableIsTrue_isNotEmpty() {
//        TableView listView = new TableView();
//        listView.setEditable(true);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateTableView(listView);
//        cell.updateItem("TEST", false);
//
//        cell.startEdit();
//        assertTrue(cell.isEditing());
//        assertNotNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_listViewEditableIsTrue_cellEditableIsTrue_isNotEmpty() {
//        TableView listView = new TableView();
//        listView.setEditable(true);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.setEditable(true);
//        cell.updateTableView(listView);
//        cell.updateItem("TEST", false);
//
//        cell.startEdit();
//        assertTrue(cell.isEditing());
//        assertNotNull(cell.getGraphic());
//    }
//
//    // --- cancel edit
//    @Test public void test_cancelEdit() {
//        TableView listView = new TableView();
//        listView.setEditable(true);
//        ComboBoxTableCell<Object> cell = new ComboBoxTableCell<>();
//        cell.updateTableView(listView);
//        cell.updateItem("TEST", false);
//
//        cell.startEdit();
//        assertTrue(cell.isEditing());
//        assertNotNull(cell.getGraphic());
//
//        cell.cancelEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
}
