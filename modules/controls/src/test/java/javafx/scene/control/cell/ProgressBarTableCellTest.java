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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressBarTableCellTest {
    
    private SimpleBooleanProperty booleanProperty;
    private Callback<Integer, ObservableValue<Boolean>> callback;
    private StringConverter<Object> converter;
    private TableView<Object> tableView;
    private TableColumn<Object, Object> tableColumn;
    
    @Before public void setup() {
        tableView = new TableView<>();
        tableColumn = new TableColumn<>();
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
    
    private void setTableViewAndTableColumn(TableCell cell) {
        cell.updateTableView(tableView);
        cell.updateTableColumn(tableColumn);
    }
    
    
    
    /**************************************************************************
     * 
     * Test for public static <T> Callback<TableColumn<S,Double>, TableCell<T,Double>> forTableColumn()
     * 
     **************************************************************************/

    
    @Test public void testStatic_forTableColumn_noArgs_ensureCellFactoryIsNotNull() {
        assertFalse(booleanProperty.get());
        Callback<TableColumn<Object, Double>, TableCell<Object, Double>> cellFactory = ProgressBarTableCell.forTableColumn();
        assertNotNull(cellFactory);
    }
    
    @Test public void testStatic_forTableColumn_noArgs_ensureCellFactoryCreatesCells() {
        assertFalse(booleanProperty.get());
        Callback<TableColumn<Object, Double>, TableCell<Object, Double>> cellFactory = ProgressBarTableCell.forTableColumn();
        
        TableColumn tableColumn = new TableColumn<>();
        ProgressBarTableCell<Object> cell = (ProgressBarTableCell<Object>)cellFactory.call(tableColumn);
        assertNotNull(cell);
    }

    
    /**************************************************************************
     * 
     * Constructor tests for default constructor
     * 
     **************************************************************************/

    
    @Test public void testConstructor_noArgs_defaultStyleClass() {
        ProgressBarTableCell<Object> cell = new ProgressBarTableCell<>();
        assertTrue(cell.getStyleClass().contains("progress-bar-table-cell"));
    }
    
    @Test public void testConstructor_noArgs_defaultGraphicIsAProgressBar() {
        ProgressBarTableCell<Object> cell = new ProgressBarTableCell<>();
        assertTrue(cell.getGraphic() instanceof ProgressBar);
    }



    /**************************************************************************
     * 
     * updateItem tests
     * 
     **************************************************************************/

    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        ProgressBarTableCell<Object> cell = new ProgressBarTableCell<>();
        cell.updateItem(0.5, true);
        assertNull(cell.getGraphic());
    }
    
    @Test public void test_updateItem_isEmpty_textIsNull() {
        ProgressBarTableCell<Object> cell = new ProgressBarTableCell<>();
        cell.updateItem(0.5, true);
        assertNull(cell.getText());
    }
    
    @Test public void test_updateItem_isNotEmpty_graphicIsNotNull() {
        ProgressBarTableCell<Object> cell = new ProgressBarTableCell<>();
        setTableViewAndTableColumn(cell);
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
//        ProgressBarTableCell<Object, Object> cell = new ProgressBarTableCell<>(callback);
//        setTableViewAndTableColumn(cell);
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
//        ProgressBarTableCell<Object, Object> cell = new ProgressBarTableCell<>(callback);
//        setTableViewAndTableColumn(cell);
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
