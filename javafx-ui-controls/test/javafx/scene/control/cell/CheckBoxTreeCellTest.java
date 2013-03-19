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
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.junit.Test;

public class CheckBoxTreeCellTest {
    
    private ReadOnlyObjectWrapper<Boolean> roBooleanProperty = new ReadOnlyObjectWrapper<Boolean>(false);
    private Callback<TreeItem<Object>, ObservableValue<Boolean>> callback = new Callback<TreeItem<Object>, ObservableValue<Boolean>>() {
        public ObservableValue<Boolean> call(TreeItem<Object> param) {
            return roBooleanProperty;
        }
    };
    
    private StringConverter<TreeItem<Object>> converter = new StringConverter<TreeItem<Object>>() {
        @Override public String toString(TreeItem<Object> object) {
            return null;
        }
        
        @Override public TreeItem<Object> fromString(String string) {
            return null;
        }
    };
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView()
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeView_callback_ensureCellFactoryIsNotNull() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView();
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeView_callback_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView();
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeView_callback_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView();
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeView_callback_ensureCellHasNonNullStringConverter() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView();
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell.getConverter());
    }
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeView_callback1_ensureCellFactoryIsNotNull() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeView_callback1_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback);
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeView_callback1_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback);
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeView_callback1_ensureCellHasNonNullStringConverter() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback);
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TreeView<T>, TreeCell<T>> forTreeView(
     *       final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
     *       final StringConverter<T> converter)
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeView_callback_2_ensureCellFactoryIsNotNull() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback, converter);
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeView_callback_2_ensureCellFactoryCreatesCells() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback, converter);
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell);
    }
    
    @Test public void testStatic_forTreeView_callback_2_ensureCellHasNonNullSelectedStateCallback() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback, converter);
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testStatic_forTreeView_callback_2_ensureCellHasSetStringConverter() {
        assertFalse(roBooleanProperty.get());
        Callback<TreeView<Object>, TreeCell<Object>> cellFactory = CheckBoxTreeCell.forTreeView(callback, converter);
        
        TreeView<Object> treeView = new TreeView<>();
        CheckBoxTreeCell<Object> cell = (CheckBoxTreeCell<Object>)cellFactory.call(treeView);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }
    
    
    
    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    
    @Test public void testConstructor_noArgs_defaultCallbackIsNull() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>();
        assertNotNull(cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_noArgs_defaultStringConverterIsNotNull() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>();
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>();
        assertTrue(cell.getStyleClass().contains("check-box-tree-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsACheckBox() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>();
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for one-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_getSelectedProperty_selectedPropertyIsNotNull() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback);
        assertEquals(callback, cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultStringConverterIsNotNull() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback);
        assertNotNull(cell.getConverter());
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultStyleClass() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback);
        assertTrue(cell.getStyleClass().contains("check-box-tree-cell"));
    }
    
    @Test public void testConstructor_getSelectedProperty_defaultGraphicIsACheckBox() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback);
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
    
    @Test(expected=NullPointerException.class)
    public void testConstructor_getSelectedProperty_passInNullCallback() {
        new CheckBoxTreeCell<>(null);
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests for two-arg constructor
     * 
     **************************************************************************/
    
    @Test public void testConstructor_getSelectedProperty_converter_selectedPropertyIsNotNull() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback, converter);
        assertEquals(callback, cell.getSelectedStateCallback());
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultStringConverterIsNotNull() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback, converter);
        assertNotNull(cell.getConverter());
        assertEquals(converter, cell.getConverter());
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultStyleClass() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback, converter);
        assertTrue(cell.getStyleClass().contains("check-box-tree-cell"));
    }
    
    @Test public void testConstructor_getSelectedProperty_converter_defaultGraphicIsACheckBox() {
        CheckBoxTreeCell<Object> cell = new CheckBoxTreeCell<>(callback, converter);
        assertTrue(cell.getGraphic() instanceof CheckBox);
    }
    
    @Test(expected=NullPointerException.class)
    public void testConstructor_getSelectedProperty_converter_passInNullCallback() {
        new CheckBoxTreeCell<>(null, converter);
    }
}
