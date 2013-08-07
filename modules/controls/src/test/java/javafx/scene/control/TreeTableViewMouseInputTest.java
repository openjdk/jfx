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

import com.sun.javafx.Utils;
import com.sun.javafx.scene.control.behavior.TreeTableViewAnchorRetriever;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.scene.control.skin.TreeTableViewSkin;
import com.sun.javafx.scene.control.test.Person;

import static org.junit.Assert.*;

import java.util.List;

import com.sun.javafx.tk.Toolkit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.KeyCode;

import javafx.stage.Stage;
import org.junit.After;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class TreeTableViewMouseInputTest {
    private TreeTableView<String> tableView;
    private TreeTableView.TreeTableViewSelectionModel<String> sm;
    private TreeTableView.TreeTableViewFocusModel<String> fm;
    
    private KeyEventFirer keyboard;
    
    private StageLoader stageLoader;
    
    private final TreeTableColumn<String, String> col0 = new TreeTableColumn<String, String>("col0");
    private final TreeTableColumn<String, String> col1 = new TreeTableColumn<String, String>("col1");
    private final TreeTableColumn<String, String> col2 = new TreeTableColumn<String, String>("col2");
    private final TreeTableColumn<String, String> col3 = new TreeTableColumn<String, String>("col3");
    private final TreeTableColumn<String, String> col4 = new TreeTableColumn<String, String>("col4");
    
    private final TreeItem<String> root = new TreeItem<String>("Root");                     // 0
        private final TreeItem<String> child1 = new TreeItem<String>("Child 1");            // 1
        private final TreeItem<String> child2 = new TreeItem<String>("Child 2");            // 2
        private final TreeItem<String> child3 = new TreeItem<String>("Child 3");            // 3
            private final TreeItem<String> subchild1 = new TreeItem<String>("Subchild 1");  // 4
            private final TreeItem<String> subchild2 = new TreeItem<String>("Subchild 2");  // 5
            private final TreeItem<String> subchild3 = new TreeItem<String>("Subchild 3");  // 6
        private final TreeItem<String> child4 = new TreeItem<String>("Child 4");            // 7
        private final TreeItem<String> child5 = new TreeItem<String>("Child 5");            // 8
        private final TreeItem<String> child6 = new TreeItem<String>("Child 6");            // 9
        private final TreeItem<String> child7 = new TreeItem<String>("Child 7");            // 10
        private final TreeItem<String> child8 = new TreeItem<String>("Child 8");            // 11
        private final TreeItem<String> child9 = new TreeItem<String>("Child 9");            // 12
        private final TreeItem<String> child10 = new TreeItem<String>("Child 10");          // 13
    
    @Before public void setup() {
        // reset tree structure
        root.getChildren().clear();
        root.setExpanded(true);
        root.getChildren().setAll(child1, child2, child3, child4, child5, child6, child7, child8, child9, child10 );
        child1.getChildren().clear();
        child1.setExpanded(false);
        child2.getChildren().clear();
        child2.setExpanded(false);
        child3.getChildren().clear();
        child3.setExpanded(true);
        child3.getChildren().setAll(subchild1, subchild2, subchild3);
        child4.getChildren().clear();
        child4.setExpanded(false);
        child5.getChildren().clear();
        child5.setExpanded(false);
        child6.getChildren().clear();
        child6.setExpanded(false);
        child7.getChildren().clear();
        child7.setExpanded(false);
        child8.getChildren().clear();
        child8.setExpanded(false);
        child9.getChildren().clear();
        child9.setExpanded(false);
        child10.getChildren().clear();
        child10.setExpanded(false);
        
        tableView = new TreeTableView<String>();
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();
        
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        
        tableView.setRoot(root);
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
        
        List<TreeTablePosition<String,?>> cells = sm.getSelectedCells();
        for (TreeTablePosition<String,?> tp : cells) {
            sb.append("(");
            sb.append(tp.getRow());
            sb.append(",");
            sb.append(tp.getColumn());
            sb.append("), ");
        }
        
        sb.append("] \nFocus: (" + fm.getFocusedCell().getRow() + ", " + fm.getFocusedCell().getColumn() + ")");
        
        TreeTablePosition anchor = getAnchor();
        sb.append(" \nAnchor: (" + (anchor == null ? "null" : anchor.getRow()) + 
                ", " + (anchor == null ? "null" : anchor.getColumn()) + ")");
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
    
    private TreeTablePosition getAnchor() {
        return TreeTableViewAnchorRetriever.getAnchor(tableView);
    }
    
    private boolean isAnchor(int row) {
        TreeTablePosition tp = new TreeTablePosition(tableView, row, null);
        return getAnchor() != null && getAnchor().equals(tp);
    }
    
    private boolean isAnchor(int row, int col) {
        TreeTablePosition tp = new TreeTablePosition(tableView, row, tableView.getColumns().get(col));
        return getAnchor() != null && getAnchor().equals(tp);
    }
    
    private int getItemCount() {
        return root.getChildren().size() + child3.getChildren().size();
    }
    
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
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

    private int rt30394_count = 0;
    @Test public void test_rt30394() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        final TableFocusModel fm = tableView.getFocusModel();
        fm.focus(-1);

        fm.focusedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                rt30394_count++;
                assertEquals(0, fm.getFocusedIndex());
            }
        });

        // test pre-conditions
        assertEquals(0,rt30394_count);
        assertFalse(fm.isFocused(0));

        // select the first row with the shift key held down. The focus event
        // should only fire once - for focus on 0 (never -1 as this bug shows).
        VirtualFlowTestUtils.clickOnRow(tableView, 0, KeyModifier.SHIFT);
        assertEquals(1, rt30394_count);
        assertTrue(fm.isFocused(0));
    }

    @Test public void test_rt32119() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearSelection();

        // select rows 2, 3, and 4
        VirtualFlowTestUtils.clickOnRow(tableView, 2);
        VirtualFlowTestUtils.clickOnRow(tableView, 4, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertTrue(sm.isSelected(3));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));

        // now shift click on the 2nd row - this should make only row 2 be
        // selected. The bug is that row 4 remains selected also.
        VirtualFlowTestUtils.clickOnRow(tableView, 2, KeyModifier.SHIFT);
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
        assertFalse(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }
}
