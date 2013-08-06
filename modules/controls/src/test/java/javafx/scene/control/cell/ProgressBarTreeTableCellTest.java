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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressBarTreeTableCellTest {
    
    private SimpleBooleanProperty booleanProperty;
    private Callback<Integer, ObservableValue<Boolean>> callback;
    private StringConverter<Object> converter;
    private TreeTableView<Object> tableView;
    private TreeTableColumn<Object, Object> tableColumn;
    
    @Before public void setup() {
        tableView = new TreeTableView<>();
        tableColumn = new TreeTableColumn<>();
        booleanProperty = new SimpleBooleanProperty(false);
        callback = new Callback<Integer, ObservableValue<Boolean>>() {
            public ObservableValue<Boolean> call(Integer param) {
                return booleanProperty;
            }
        };
        converter = new StringConverter<Object>() {
            @Override public String toString(Object object) {
                return null;
            }
            
            @Override public Object fromString(String string) {
                return null;
            }
        };
    }
    
    private void setTableViewAndTreeTableColumn(TreeTableCell cell) {
        cell.updateTreeTableView(tableView);
        cell.updateTreeTableColumn(tableColumn);
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TreeTableColumn<S,Double>, TreeTableCell<T,Double>> forTreeTableColumn()
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellFactoryIsNotNull() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Double>, TreeTableCell<Object, Double>> cellFactory = ProgressBarTreeTableCell.forTreeTableColumn();
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTreeTableColumn_noArgs_ensureCellFactoryCreatesCells() {
        assertFalse(booleanProperty.get());
        Callback<TreeTableColumn<Object, Double>, TreeTableCell<Object, Double>> cellFactory = ProgressBarTreeTableCell.forTreeTableColumn();
        
        TreeTableColumn tableColumn = new TreeTableColumn<>();
        ProgressBarTreeTableCell<Object> cell = (ProgressBarTreeTableCell<Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    
    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        ProgressBarTreeTableCell<Object> cell = new ProgressBarTreeTableCell<>();
        assertTrue(cell.getStyleClass().contains("progress-bar-tree-table-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsNull() {
        ProgressBarTreeTableCell<Object> cell = new ProgressBarTreeTableCell<>();
        assertNull(cell.getGraphic());
    }



    /**************************************************************************
     * 
     * updateItem tests
     * 
     **************************************************************************/

    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        ProgressBarTreeTableCell<Object> cell = new ProgressBarTreeTableCell<>();
        cell.updateItem(0.5, true);
        assertNull(cell.getGraphic());
    }
    
    @Test public void test_updateItem_isEmpty_textIsNull() {
        ProgressBarTreeTableCell<Object> cell = new ProgressBarTreeTableCell<>();
        cell.updateItem(0.5, true);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_graphicIsNotNull() {
        ProgressBarTreeTableCell<Object> cell = new ProgressBarTreeTableCell<>();
        setTableViewAndTreeTableColumn(cell);
        cell.updateItem(0.5, false);
        assertNotNull(cell.getGraphic());
        assertTrue(cell.getGraphic() instanceof ProgressBar);
    }
    

    
//    /**************************************************************************
//     * 
//     * test checkbox selection state is bound
//     * 
//     **************************************************************************/
//    
//    @Test public void test_booleanPropertyChangeUpdatesCheckBoxSelection() {
//        ProgressBarTreeTableCell<Object, Object> cell = new ProgressBarTreeTableCell<>(callback);
//        setTableViewAndTreeTableColumn(cell);
//        cell.updateItem("TEST", false);
//        CheckBox cb = (CheckBox)cell.getGraphic();
//        
//        assertFalse(cb.isSelected());
//        booleanProperty.set(true);
//        assertTrue(cb.isScaleShape());
//
//        booleanProperty.set(false);
//        assertFalse(cb.isSelected());
//    }
//    
//    @Test public void test_checkBoxSelectionUpdatesBooleanProperty() {
//        ProgressBarTreeTableCell<Object, Object> cell = new ProgressBarTreeTableCell<>(callback);
//        setTableViewAndTreeTableColumn(cell);
//        cell.updateItem("TEST", false);
//        CheckBox cb = (CheckBox)cell.getGraphic();
//        
//        assertFalse(booleanProperty.get());
//        cb.setSelected(true);
//        assertTrue(booleanProperty.get());
//
//        cb.setSelected(false);
//        assertFalse(booleanProperty.get());
//    }
}
