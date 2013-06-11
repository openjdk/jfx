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

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextFieldTreeTableCellTest {

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
     * Test for public static Callback<TreeTableColumn<String>, TreeTableCell<String>> forTreeTableColumn()
     *
     **************************************************************************/


    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellFactoryIsNotNull() {
        Callback<TreeTableColumn<Object,String>, TreeTableCell<Object,String>> cellFactory = TextFieldTreeTableCell.forTreeTableColumn();
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellFactoryCreatesCells() {
        Callback<TreeTableColumn<Object,String>, TreeTableCell<Object,String>> cellFactory = TextFieldTreeTableCell.forTreeTableColumn();

        TreeTableColumn<Object,String> tableColumn = new TreeTableColumn<>();
        TextFieldTreeTableCell<Object,String> cell = (TextFieldTreeTableCell<Object,String>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    @Test public void testStatic_forTreeTableColumn_callback_ensureCellHasNonNullStringConverter() {
        Callback<TreeTableColumn<Object,String>, TreeTableCell<Object,String>> cellFactory = TextFieldTreeTableCell.forTreeTableColumn();

        TreeTableColumn<Object,String> tableColumn = new TreeTableColumn<>();
        TextFieldTreeTableCell<Object,String> cell = (TextFieldTreeTableCell<Object,String>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
    }



    /**************************************************************************
     *
     * Test for public static <T> Callback<TreeTableColumn<T>, TreeTableCell<T>> forTreeTableColumn(
     *       final StringConverter<T> converter)
     *
     **************************************************************************/


    @Test public void testStatic_forTreeTableColumn_converter_ensureCellFactoryIsNotNull() {
        Callback<TreeTableColumn<Object,Object>, TreeTableCell<Object,Object>> cellFactory = TextFieldTreeTableCell.forTreeTableColumn(converter);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forTreeTableColumn_converter_ensureCellFactoryCreatesCells() {
        Callback<TreeTableColumn<Object,Object>, TreeTableCell<Object,Object>> cellFactory = TextFieldTreeTableCell.forTreeTableColumn(converter);

        TreeTableColumn<Object,Object> tableColumn = new TreeTableColumn<>();
        TextFieldTreeTableCell<Object,Object> cell = (TextFieldTreeTableCell<Object,Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    @Test public void testStatic_forTreeTableColumn_converter_ensureCellHasSetStringConverter() {
        Callback<TreeTableColumn<Object,Object>, TreeTableCell<Object,Object>> cellFactory = TextFieldTreeTableCell.forTreeTableColumn(converter);

        TreeTableColumn<Object,Object> tableColumn = new TreeTableColumn<>();
        TextFieldTreeTableCell<Object,Object> cell = (TextFieldTreeTableCell<Object,Object>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }



    /**************************************************************************
     *
     * Constructor tests for default constructor
     *
     **************************************************************************/

    @Test public void testConstructor_noArgs_defaultStringConverterIsNull() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        assertNull(cell.getConverter());
    }

    @Test public void testConstructor_noArgs_defaultStyleClass() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        assertTrue(cell.getStyleClass().contains("text-field-tree-table-cell"));
    }

    @Test public void testConstructor_noArgs_defaultGraphicIsNull() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        assertNull(cell.getGraphic());
    }


    /**************************************************************************
     *
     * Constructor tests for one-arg constructor
     *
     **************************************************************************/

    @Test public void testConstructor_converter_defaultStringConverterIsNotNull() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>(converter);
        assertNotNull(cell.getConverter());
    }

    @Test public void testConstructor_converter_defaultStyleClass() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>(converter);
        assertTrue(cell.getStyleClass().contains("text-field-tree-table-cell"));
    }

    @Test public void testConstructor_converter_defaultGraphicIsACheckBox() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>(converter);
        assertNull(cell.getGraphic());
    }


    /**************************************************************************
     *
     * updateItem tests
     *
     **************************************************************************/

    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getGraphic());
    }

    @Test public void test_updateItem_isEmpty_textIsNull() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getText());
    }

    @Test public void test_updateItem_isNotEmpty_textIsNotNull() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }

    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nullConverter() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
        cell.setConverter(null);
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }

    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nonNullConverter() {
        TextFieldTreeTableCell<?,Object> cell = new TextFieldTreeTableCell<>();
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
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.setEditable(false);
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_tableColumnEditableIsFalse_isEmpty() {
//        TreeItem root = new TreeItem("Root");
//        TreeTableView treeView = new TreeTableView(root);
//
//        TreeTableColumn<Object,Object> tableColumn = new TreeTableColumn<>();
//        tableColumn.setEditable(false);
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateTreeTableColumn(tableColumn);
//        cell.updateTreeTableView(treeView);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void test_startEdit_cellEditableIsTrue_tableColumnIsNull_isEmpty() {
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.setEditable(true);
//        cell.startEdit();
//    }
//
//    @Test public void test_startEdit_tableColumnEditableIsTrue_isEmpty() {
//        TreeTableColumn<Object,Object> tableColumn = new TreeTableColumn<>();
//        tableColumn.setEditable(true);
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateTreeTableColumn(tableColumn);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_tableColumnEditableIsTrue_cellEditableIsTrue_isEmpty() {
//        TreeTableColumn<Object,Object> tableColumn = new TreeTableColumn<>();
//        tableColumn.setEditable(true);
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.setEditable(true);
//        cell.updateTreeTableColumn(tableColumn);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    // --- is Not Empty
//    @Test public void test_startEdit_cellEditableIsFalse_isNotEmpty() {
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateItem("TEST", false);
//        cell.setEditable(false);
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_tableColumnEditableIsFalse_isNotEmpty() {
//        TreeItem root = new TreeItem("Root");
//        TreeTableView treeView = new TreeTableView(root);
//
//        TreeTableColumn<Object,Object> tableColumn = new TreeTableColumn<>();
//        tableColumn.setEditable(false);
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateTreeTableView(treeView);
//        cell.updateTreeTableColumn(tableColumn);
//        cell.updateItem("TEST", false);
//
//        cell.startEdit();
//        assertFalse(cell.isEditing());
//        assertNull(cell.getGraphic());
//    }
//
//    @Test(expected = NullPointerException.class)
//    public void test_startEdit_cellEditableIsTrue_tableColumnIsNull_isNotEmpty() {
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateItem("TEST", false);
//        cell.setEditable(true);
//        cell.startEdit();
//    }
//
//    @Test public void test_startEdit_treeViewEditableIsTrue_isNotEmpty() {
//        TreeItem root = new TreeItem("Root");
//        TreeTableView treeView = new TreeTableView(root);
//        treeView.setEditable(true);
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateTreeTableView(treeView);
//        cell.updateIndex(0);
//
//        cell.startEdit();
//        assertTrue(cell.isEditing());
//        assertNotNull(cell.getGraphic());
//    }
//
//    @Test public void test_startEdit_treeViewEditableIsTrue_cellEditableIsTrue_isNotEmpty() {
//        TreeItem root = new TreeItem("Root");
//        TreeTableView treeView = new TreeTableView(root);
//        treeView.setEditable(true);
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateTreeTableView(treeView);
//        cell.setEditable(true);
//        cell.updateIndex(0);
//
//        cell.startEdit();
//        assertTrue(cell.isEditing());
//        assertNotNull(cell.getGraphic());
//    }
//
//    // --- cancel edit
//    @Test public void test_cancelEdit() {
//        TreeItem root = new TreeItem("Root");
//        TreeTableView treeView = new TreeTableView(root);
//        treeView.setEditable(true);
//        TreeTableColumn col = new TreeTableColumn();
//        TextFieldTreeTableCell<Object,Object> cell = new TextFieldTreeTableCell<>();
//        cell.updateTreeTableView(treeView);
//        cell.updateTreeTableColumn(col);
//        cell.updateIndex(0);
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
