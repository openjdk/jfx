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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.Before;
import org.junit.Test;

public class CheckBoxTreeTableCellTest {
    
    private SimpleBooleanProperty booleanProperty;
    private Callback<Integer, ObservableValue<Boolean>> callback;
    private StringConverter<Object> converter;
    private TreeTableView<Object> tableView;
    private TreeTableColumn<Object, Object> tableColumn;
    
    @Before public void setup() {
        tableView = new TreeTableView<>();
        tableColumn = new TreeTableColumn<>();
        booleanProperty = new SimpleBooleanProperty(false);
        callback = param -> booleanProperty;
        converter = new StringConverter<Object>() {
            @Override public String toString(Object object) {
                return null;
            }
            
            @Override public Object fromString(String string) {
                return null;
            }
        };
    }
    
    private void setTableViewAndTableColumn(TreeTableCell cell) {
        cell.updateTreeTableView(tableView);
        cell.updateTreeTableColumn(tableColumn);
    }
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TableView<T>, TreeTableCell<T>> forTreeTableColumn(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellFactoryIsNotNull() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellFactoryCreatesCells() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellHasNullStringConverter() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNull(cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TableView<T>, TreeTableCell<T>> forTreeTableColumn(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
     *       final StringConverter<T> converter)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellFactoryIsNotNull() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellFactoryCreatesCells() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellHasSetStringConverter() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    
    @Test public void testConstructor_noArgs_defaultCallbackIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        assertNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_noArgs_defaultStringConverterIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        assertNull(cell.getConverter());
    }
    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        assertTrue(cell.getStyleClass().contains("check-box-tree-table-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        assertNull(cell.getGraphic());
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for one-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_getSelectedProperty_selectedPropertyIsNotNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        assertEquals(callback, cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultStringConverterIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        assertNull(cell.getConverter());
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultStyleClass() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        assertTrue(cell.getStyleClass().contains("check-box-tree-table-cell"));
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultGraphicIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        assertNull(cell.getGraphic());
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for two-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_getSelectedProperty_converter_selectedPropertyIsNotNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback, converter);
        assertEquals(callback, cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultStringConverterIsNotNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback, converter);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultStyleClass() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback, converter);
        assertTrue(cell.getStyleClass().contains("check-box-tree-table-cell"));
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultGraphicIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback, converter);
        assertNull(cell.getGraphic());
    }
    
    
    /**************************************************************************
     * 
     * updateItem tests
     * 
     **************************************************************************/

    @Test(expected=NullPointerException.class)
    public void test_getSelectedPropertyIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        cell.updateItem("TEST", false);
    }
    
    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getGraphic());
    }
    
    @Test public void test_updateItem_isEmpty_textIsNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_graphicIsNotNull() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        setTableViewAndTableColumn(cell);
        cell.updateItem("TEST", false);
        assertNotNull(cell.getGraphic());
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNullBecauseOfNullConverter_1() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        setTableViewAndTableColumn(cell);
        cell.updateItem("TEST", false);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNullBecauseOfNullConverter_2() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        setTableViewAndTableColumn(cell);
        cell.setConverter(null);
        cell.updateItem("TEST", false);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nonNullConverter() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        setTableViewAndTableColumn(cell);
        cell.setConverter(new StringConverter<Object>() {
            @Override public Object fromString(String string) {
                return "ERROR";
            }
            
            @Override public String toString(Object object) {
                return "CONVERTED";
            }
        });
        cell.updateItem("TEST", false);
        assertEquals("CONVERTED", cell.getText());
    }
    
    
    /**************************************************************************
     * 
     * test checkbox selection state is bound
     * 
     **************************************************************************/
    
    @Test public void test_booleanPropertyChangeUpdatesCheckBoxSelection() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        setTableViewAndTableColumn(cell);
        cell.updateItem("TEST", false);
        CheckBox cb = (CheckBox)cell.getGraphic();
        
        assertFalse(cb.isSelected());
        booleanProperty.set(true);
        assertTrue(cb.isScaleShape());

        booleanProperty.set(false);
        assertFalse(cb.isSelected());
    }
    
    @Test public void test_checkBoxSelectionUpdatesBooleanProperty() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        setTableViewAndTableColumn(cell);
        cell.updateItem("TEST", false);
        CheckBox cb = (CheckBox)cell.getGraphic();
        
        assertFalse(booleanProperty.get());
        cb.setSelected(true);
        assertTrue(booleanProperty.get());

        cb.setSelected(false);
        assertFalse(booleanProperty.get());
    }
}
