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

import static org.junit.Assert.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.Before;
import org.junit.Test;

public class CheckBoxListCellTest {
    
    private ReadOnlyObjectWrapper<Boolean> roBooleanProperty = new ReadOnlyObjectWrapper<Boolean>(false);
    private Callback<Object, ObservableValue<Boolean>> callback = new Callback<Object, ObservableValue<Boolean>>() {
        public ObservableValue<Boolean> call(Object param) {
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
     * Test for public static <T> Callback<ListView<T>, ListCell<T>> forListView(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forListView_callback_ensureCellFactoryIsNotNull() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forListView_callback_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback);
        
        ListView<Object> listView = new ListView();
        CheckBoxListCell<Object> cell = (CheckBoxListCell<Object>)cellFactory.call(listView);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forListView_callback_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback);
        
        ListView<Object> listView = new ListView();
        CheckBoxListCell<Object> cell = (CheckBoxListCell<Object>)cellFactory.call(listView);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forListView_callback_ensureCellHasNonNullStringConverter() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback);
        
        ListView<Object> listView = new ListView();
        CheckBoxListCell<Object> cell = (CheckBoxListCell<Object>)cellFactory.call(listView);
        assertNotNull(cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<ListView<T>, ListCell<T>> forListView(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
     *       final StringConverter<T> converter)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forListView_callback_2_ensureCellFactoryIsNotNull() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback, converter);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forListView_callback_2_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback, converter);
        
        ListView<Object> listView = new ListView();
        CheckBoxListCell<Object> cell = (CheckBoxListCell<Object>)cellFactory.call(listView);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forListView_callback_2_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback, converter);
        
        ListView<Object> listView = new ListView();
        CheckBoxListCell<Object> cell = (CheckBoxListCell<Object>)cellFactory.call(listView);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forListView_callback_2_ensureCellHasSetStringConverter() {
        assertFalse(roBooleanProperty.get());
        Callback<ListView<Object>, ListCell<Object>> cellFactory = CheckBoxListCell.forListView(callback, converter);
        
        ListView<Object> listView = new ListView();
        CheckBoxListCell<Object> cell = (CheckBoxListCell<Object>)cellFactory.call(listView);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    
    @Test public void testConstructor_noArgs_defaultCallbackIsNull() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell();
        assertNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_noArgs_defaultStringConverterIsNotNull() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell();
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell();
        assertTrue(cell.getStyleClass().contains("check-box-list-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsACheckBox() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell();
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for one-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_getSelectedProperty_selectedPropertyIsNotNull() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback);
        assertEquals(callback, cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultStringConverterIsNotNull() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback);
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultStyleClass() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback);
        assertTrue(cell.getStyleClass().contains("check-box-list-cell"));
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultGraphicIsACheckBox() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback);
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for two-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_getSelectedProperty_converter_selectedPropertyIsNotNull() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback, converter);
        assertEquals(callback, cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultStringConverterIsNotNull() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback, converter);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultStyleClass() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback, converter);
        assertTrue(cell.getStyleClass().contains("check-box-list-cell"));
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultGraphicIsACheckBox() {
        CheckBoxListCell<Object> cell = new CheckBoxListCell(callback, converter);
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
}
