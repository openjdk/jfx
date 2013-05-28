/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.scene.control.behavior.TableViewAnchorRetriever;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class TableViewMouseInputTest {
    private TableView<String> tableView;
    private TableView.TableViewSelectionModel<String> sm;
    private TableView.TableViewFocusModel<String> fm;
    
    private KeyEventFirer keyboard;
    
    private StageLoader stageLoader;
    
    private final TableColumn<String, String> col0 = new TableColumn<String, String>("col0");
    private final TableColumn<String, String> col1 = new TableColumn<String, String>("col1");
    private final TableColumn<String, String> col2 = new TableColumn<String, String>("col2");
    private final TableColumn<String, String> col3 = new TableColumn<String, String>("col3");
    private final TableColumn<String, String> col4 = new TableColumn<String, String>("col4");
    
    @Before public void setup() {
        tableView = new TableView<String>();
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();
        
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        
        tableView.getItems().setAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        tableView.getColumns().setAll(col0, col1, col2, col3, col4);
        
        sm.clearAndSelect(0);
        
        keyboard = new KeyEventFirer(tableView);
        
        stageLoader = new StageLoader(tableView);
        stageLoader.getStage().show();
    }
    
    @After public void tearDown() {
        tableView.getSkin().dispose();
        stageLoader.dispose();
    }
    
    /***************************************************************************
     * Util methods
     **************************************************************************/
    
    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Cells: [");
        
        List<TablePosition> cells = sm.getSelectedCells();
        for (TablePosition<String,?> tp : cells) {
            sb.append("(");
            sb.append(tp.getRow());
            sb.append(",");
            sb.append(tp.getColumn());
            sb.append("), ");
        }
        
        sb.append("] \nFocus: (" + fm.getFocusedCell().getRow() + ", " + fm.getFocusedCell().getColumn() + ")");
        sb.append(" \nAnchor: (" + getAnchor().getRow() + ", " + getAnchor().getColumn() + ")");
        return sb.toString();
    }
    
    // Returns true if ALL indices are selected
    private boolean isSelected(int... indices) {
        for (int index : indices) {
            if (! sm.isSelected(index)) return false;
        }
        return true;
    }
    
    // Returns true if ALL indices are NOT selected
    private boolean isNotSelected(int... indices) {
        for (int index : indices) {
            if (sm.isSelected(index)) return false;
        }
        return true;
    }
    
    private TablePosition getAnchor() {
        return TableViewAnchorRetriever.getAnchor(tableView);
    }
    
    private boolean isAnchor(int row) {
        TablePosition tp = new TablePosition(tableView, row, null);
        return getAnchor() != null && getAnchor().equals(tp);
    }
    
    private boolean isAnchor(int row, int col) {
        TablePosition tp = new TablePosition(tableView, row, tableView.getColumns().get(col));
        return getAnchor() != null && getAnchor().equals(tp);
    }
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
    @Ignore("Bug is not yet fixed")
    @Test public void test_rt29833_mouse_select_upwards() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(9);
        
        // select all from 9 - 7
        VirtualFlowTestUtils.clickOnRow(tableView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(7,8,9));
        
        // select all from 9 - 7 - 5
        VirtualFlowTestUtils.clickOnRow(tableView, 5, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }
    
    @Test public void test_rt29833_mouse_select_downwards() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(5);
        
        // select all from 5 - 7
        VirtualFlowTestUtils.clickOnRow(tableView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(5,6,7));
        
        // select all from 5 - 7 - 9
        VirtualFlowTestUtils.clickOnRow(tableView, 9, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }
}
