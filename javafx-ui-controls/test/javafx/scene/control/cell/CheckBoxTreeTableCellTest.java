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
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.Test;

public class CheckBoxTreeTableCellTest {
    
    private ReadOnlyObjectWrapper<Boolean> roBooleanProperty = new ReadOnlyObjectWrapper<Boolean>(false);
    private Callback<Integer, ObservableValue<Boolean>> callback = new Callback<Integer, ObservableValue<Boolean>>() {
        public ObservableValue<Boolean> call(Integer param) {
            return roBooleanProperty;
        }
    };
    
    private StringConverter<Object> converter = new StringConverter<Object>() {
        @Override public String toString(Object object) {
            return null;
        }
        
        @Override public Object fromString(String string) {
            return null;
        }
    };
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TableView<T>, TreeTableCell<T>> forTreeTableColumn(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellFactoryIsNotNull() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        
        TreeTableColumn tableColumn = new TreeTableColumn();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        
        TreeTableColumn tableColumn = new TreeTableColumn();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_ensureCellHasNullStringConverter() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback);
        
        TreeTableColumn tableColumn = new TreeTableColumn();
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
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeTableColumn<Object, Object>, TreeTableCell<Object, Object>> cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(callback, converter);
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        CheckBoxTreeTableCell<Object, Object> cell = (CheckBoxTreeTableCell<Object, Object>)cellFactory.call(tableColumn);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeTableColumn_callback_2_ensureCellHasSetStringConverter() {
        assertFalse(roBooleanProperty.get());
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
    
    @Test public void testConstructor_noArgs_defaultGraphicIsACheckBox() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>();
        assertTrue(cell.getGraphic() instanceof CheckBox);
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
    
    @Test public void testConstructor_getSelectedProperty_defaultGraphicIsACheckBox() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback);
        assertTrue(cell.getGraphic() instanceof CheckBox);
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
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultGraphicIsACheckBox() {
        CheckBoxTreeTableCell<Object, Object> cell = new CheckBoxTreeTableCell<>(callback, converter);
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
}
