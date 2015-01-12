/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.TableCellBehavior;
import com.sun.javafx.scene.control.behavior.TreeTableCellBehavior;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import java.util.List;
import java.util.function.Function;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.Utils;
import com.sun.javafx.scene.control.behavior.TreeTableViewAnchorRetriever;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.tk.Toolkit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TreeTableViewKeyInputTest {
    private TreeTableView<String> tableView;
    private TreeTableView.TreeTableViewSelectionModel<String> sm;
    private TreeTableView.TreeTableViewFocusModel<String> fm;
    
    private KeyEventFirer keyboard;
    
    private StageLoader stageLoader;
    
    private TreeTableColumn<String, String> col0;
    private TreeTableColumn<String, String> col1;
    private TreeTableColumn<String, String> col2;
    private TreeTableColumn<String, String> col3;
    private TreeTableColumn<String, String> col4;
    
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

        col0 = new TreeTableColumn<String, String>("col0");
        col1 = new TreeTableColumn<String, String>("col1");
        col2 = new TreeTableColumn<String, String>("col2");
        col3 = new TreeTableColumn<String, String>("col3");
        col4 = new TreeTableColumn<String, String>("col4");
        tableView.getColumns().setAll(col0, col1, col2, col3, col4);
        
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
            if (! sm.isSelected(index)) {
                System.out.println("Index " + index + " is not selected, but it is expected to be (selected indices: " + sm.getSelectedIndices() + ")");
                return false;
            }
        }
        return true;
    }
    
    // Returns true if ALL indices are NOT selected
    private boolean isNotSelected(int... indices) {
        for (int index : indices) {
            if (sm.isSelected(index)) {
                System.out.println("Index " + index + " is selected, but it is not expected to be");
                return false;
            }
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
     * 
     * 
     * Tests taken from TableViewKeyInputTest
     * (scroll down further for the TreeViewKeyInputTests)
     * 
     * 
     **************************************************************************/    
    
    
    /***************************************************************************
     * General tests
     **************************************************************************/    
    
    @Test public void testInitialState() {
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(0, sm.getSelectedCells().size());
        assertEquals(0, sm.getSelectedIndices().size());
        assertEquals(0, sm.getSelectedItems().size());
    }
    
    
    /***************************************************************************
     * Tests for row-based single selection
     **************************************************************************/
    
    @Test public void testDownArrowChangesSelection() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress();
        assertFalse(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
    }
    
    @Test public void testDownArrowDoesNotChangeSelectionWhenAtLastIndex() {
        int endIndex = tableView.getExpandedItemCount() - 1;
        sm.clearAndSelect(endIndex);
        assertTrue(sm.isSelected(endIndex));
        keyboard.doDownArrowPress();
        assertTrue(sm.isSelected(endIndex));
    }
    
    @Test public void testUpArrowDoesNotChangeSelectionWhenAt0Index() {
        sm.clearAndSelect(0);
        keyboard.doUpArrowPress();
        assertTrue(sm.isSelected(0));
        assertEquals(1, sm.getSelectedCells().size());
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, sm.getSelectedItems().size());
    }
    
    @Test public void testUpArrowChangesSelection() {
        sm.clearAndSelect(1);
        keyboard.doUpArrowPress();
        assertFalse(sm.isSelected(1));
        assertTrue(sm.isSelected(0));
    }
    
    @Test public void testLeftArrowDoesNotChangeState() {
        keyboard.doLeftArrowPress();
        testInitialState();
    }
    
//    @Test public void testRightArrowDoesNotChangeState() {
//        keyboard.doRightArrowPress();
//        testInitialState();
//    }
    
    /* test 19
    @Test public void testCtrlDownMovesFocusButLeavesSelectionAlone() {
        assertTrue(fm.isFocused(0));
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
    } */
    
    // test 20
    @Test public void testCtrlUpDoesNotMoveFocus() {
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
    }
    
    // test 21
    @Test public void testCtrlLeftDoesNotMoveFocus() {
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
    }
    
    // test 22
    @Test public void testCtrlRightDoesNotMoveFocus() {
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
    }
    
    /* test 23
    @Test public void testCtrlUpMovesFocus() {
        sm.clearAndSelect(1);
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(1));
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(1));
    } */
    
    // test 24
    @Test public void testCtrlDownDoesNotMoveFocusWhenAtLastIndex() {
        int endIndex = tableView.getExpandedItemCount() - 1;
        sm.clearAndSelect(endIndex);
        assertTrue(fm.isFocused(endIndex));
        assertTrue(sm.isSelected(endIndex));
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(endIndex));
        assertTrue(sm.isSelected(endIndex));
    }
    
    /* test 25
    @Test public void testCtrlDownArrowWithSpaceChangesAnchor() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey());  // select 2
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1));
        assertTrue(isAnchor(2));
    } */
    
    /* test 26
    @Test public void testCtrlUpArrowWithSpaceChangesAnchor() {
        sm.clearAndSelect(2);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey());  // select 0
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1));
        assertTrue(isAnchor(0));
    } */
    
    // test 44
    @Test public void testHomeKey() {
        sm.clearAndSelect(3);
        keyboard.doKeyPress(KeyCode.HOME);
        assertTrue(isSelected(0));
        assertTrue(isNotSelected(1,2,3));
    }
    
    // test 45
    @Test public void testEndKey() {
        sm.clearAndSelect(3);
        keyboard.doKeyPress(KeyCode.END);
        assertTrue(isSelected(tableView.getExpandedItemCount() - 1));
        assertTrue(isNotSelected(1,2,3));
    }
    
    /* test 53
    @Test public void testCtrlHome() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey());
        assertTrue(isSelected(5));
        assertTrue(fm.isFocused(0));
    } */
    
    /* test 54
    @Test public void testCtrlEnd() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey());
        assertTrue(isSelected(5));
        assertTrue(fm.isFocused(tableView.getItems().size() - 1));
    } */
    
    /* test 68
    @Test public void testCtrlSpaceToClearSelection() {
        sm.clearAndSelect(5);
        assertTrue(isSelected(5));
        assertTrue(fm.isFocused(5));
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey());
        assertTrue(isNotSelected(5));
        assertTrue(debug(), fm.isFocused(5));
        assertTrue(isAnchor(5));
    } */
    
    
    
    /***************************************************************************
     * Tests for row-based multiple selection
     **************************************************************************/
    
    @Test public void testShiftDownArrowIncreasesSelection() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
    }
    
    @Test public void testShiftDownArrowDoesNotChangeSelectionWhenAtLastIndex() {
        int endIndex = tableView.getExpandedItemCount() - 1;
        sm.clearAndSelect(endIndex);
        assertTrue(sm.isSelected(endIndex));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(endIndex));
    }
    
    @Test public void testShiftUpArrowIncreasesSelection() {
        sm.clearAndSelect(1);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
    }
    
    @Test public void testShiftUpArrowWhenAt0Index() {
        sm.clearAndSelect(0);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
    }
    
    @Test public void testShiftLeftArrowWhenAt0Index() {
        sm.clearAndSelect(0);
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
    }
    
    @Test public void testShiftRightArrowWhenAt0Index() {
        sm.clearAndSelect(0);
        keyboard.doRightArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
    }
    
    @Test public void testShiftDownTwiceThenShiftUp() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(0, 1));
        assertTrue(debug(), isNotSelected(2));
    }
    
    @Test public void testShiftUpTwiceThenShiftDownFrom0Index() {
        sm.clearAndSelect(0);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
        assertFalse(sm.isSelected(2));
    }
    
    @Test public void testShiftLeftTwiceThenShiftRight() {
        sm.clearAndSelect(0);
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);
        keyboard.doRightArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
        assertFalse(sm.isSelected(2));
    }
    
    @Test public void testShiftRightTwiceThenShiftLeft() {
        sm.clearAndSelect(0);
        keyboard.doRightArrowPress(KeyModifier.SHIFT);
        keyboard.doRightArrowPress(KeyModifier.SHIFT);
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
        assertFalse(sm.isSelected(2));
    }
    
    @Test public void testShiftUpTwiceThenShiftDown() {
        sm.clearAndSelect(2);                           // select 2
        keyboard.doUpArrowPress(KeyModifier.SHIFT);     // also select 1
        keyboard.doUpArrowPress(KeyModifier.SHIFT);     // also select 0
        keyboard.doDownArrowPress(KeyModifier.SHIFT);   // deselect 0
        assertFalse(debug(), sm.isSelected(0));
        assertTrue(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
    }
    
    // test 18 from Jindra's testcases.rtf file
    @Test public void testShiftDownTwiceThenShiftUpWhenAtLastIndex() {
        int endIndex = tableView.getExpandedItemCount() - 1;
        sm.clearAndSelect(endIndex);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(endIndex));
        assertTrue(sm.isSelected(endIndex - 1));
        assertFalse(sm.isSelected(endIndex - 2));
    }
    
    // test 27
    @Test public void testCtrlDownArrowWithSpaceChangesAnchor_extended() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // deselect 0
        assertTrue(isSelected(2));
        assertTrue(isNotSelected(0, 1));
        assertTrue(isAnchor(0));
    } 
    
    // test 28
    @Test public void testCtrlUpArrowWithSpaceChangesAnchor_extended() {
        sm.clearAndSelect(2);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // deselect 2
        assertTrue(isSelected(0));
        assertTrue(isNotSelected(1, 2));
        assertTrue(isAnchor(2));
    }
    
    // test 29
    @Test public void testCtrlDownArrowWithSpaceChangesAnchor_extended2() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 4
        assertTrue(isSelected(0, 2, 4));
        assertTrue(isNotSelected(1, 3, 5));
        assertTrue(isAnchor(4));
    }
    
    // test 30
    @Test public void testCtrlUpArrowWithSpaceChangesAnchor_extended2() {
        sm.clearAndSelect(4);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0
        assertTrue(isSelected(0, 2, 4));
        assertTrue(isNotSelected(1, 3));
        assertTrue(isAnchor(0));
    }
    
    // test 31
    @Test public void testCtrlDownArrowThenShiftSpaceToSelectRange() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);  // select 0,1,2
        assertTrue(isSelected(0, 1, 2));
        assertTrue(isNotSelected(3));
        assertTrue(isAnchor(0));
    }
    
    // test 32
    @Test public void testCtrlUpArrowThenShiftSpaceToSelectRange() {
        sm.clearAndSelect(2);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);  // select 0,1,2
        assertTrue(isSelected(0, 1, 2));
        assertTrue(isNotSelected(3));
        assertTrue(debug(), isAnchor(2));
    }
    
    // test 33
    @Test public void testCtrlDownArrowThenSpaceToChangeSelection() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2, keeping 0 selected
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1, 3));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);  // select 2,3,4
        assertTrue(isSelected(2, 3, 4));
        assertTrue(isNotSelected(0, 1));
        assertTrue(isAnchor(2));
    }
    
    // test 34
    @Test public void testCtrlUpArrowThenSpaceToChangeSelection() {
        sm.clearAndSelect(4);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2, keeping 4 selected
        assertTrue(isSelected(2, 4));
        assertTrue(isNotSelected(0, 1, 3));
        assertTrue(isAnchor(2));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);  // select 0,1,2
        assertTrue(isSelected(0, 1, 2));
        assertTrue(isNotSelected(3, 4));
        assertTrue(debug(), isAnchor(2));
    }
    
    // test 35
    @Test public void testCtrlDownTwiceThenShiftDown() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT);  // select 0,1,2,3
        assertTrue(isSelected(0, 1, 2, 3));
    }
    
    // test 36
    @Test public void testCtrlUpTwiceThenShiftDown() {
        sm.clearAndSelect(3);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT);  // select 1,2,3
        assertTrue(isSelected(1, 2, 3));
        assertTrue(isNotSelected(0));
    }
    
    // test 37
    @Test public void testCtrlDownThriceThenShiftUp() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);  // select 0,1,2
        assertTrue(isSelected(0, 1, 2));
        assertTrue(isNotSelected(3, 4));
    }
    
    // test 38
    @Test public void testCtrlUpTwiceThenShiftUp() {
        sm.clearAndSelect(3);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);  // select 0,1,2,3
        assertTrue(isSelected(0, 1, 2, 3));
        assertTrue(isNotSelected(4));
    }
    
    // test 39
    @Test public void testCtrlDownTwiceThenSpace_extended() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0,2
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1, 3));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doDownArrowPress(KeyModifier.SHIFT);   // select 2,3,4,5
        assertTrue(isSelected(2, 3, 4, 5));
        assertTrue(isNotSelected(0, 1));
        assertTrue(isAnchor(2));
    }
    
    // test 40
    @Test public void testCtrlUpTwiceThenSpace_extended() {
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 3,5
        assertTrue(isSelected(3,5));
        assertTrue(isNotSelected(0,1,2,4));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doDownArrowPress(KeyModifier.SHIFT);   // select 1,2,3
        assertTrue(isSelected(1,2,3));
        assertTrue(isNotSelected(0,4,5));
        assertTrue(isAnchor(3));
    }
    
    // test 41
    @Test public void testCtrlDownTwiceThenSpace_extended2() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0,2
        assertTrue(isSelected(0,2));
        assertTrue(isNotSelected(1,3,4));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 5
        keyboard.doUpArrowPress(KeyModifier.SHIFT);     // select 2,3,4
        assertTrue(isSelected(2,3,4));
        assertTrue(isNotSelected(0,1,5));
        assertTrue(isAnchor(2));
    }
    
    // test 50
    @Test public void testCtrlDownThenShiftHome() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0,2
        assertTrue(isSelected(0,2));
        assertTrue(isNotSelected(1,3,4));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2));
        assertTrue(isNotSelected(3,4));
        assertTrue(debug(),isAnchor(2));
    }
    
    // test 51
    @Test public void testCtrlUpThenShiftEnd() {
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 3,5
        assertTrue(isSelected(3,5));
        assertTrue(isNotSelected(1,2,4));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertTrue(isSelected(3,4,5,6,7,8,9));
        assertTrue(isNotSelected(0,1,2));
        assertTrue(debug(),isAnchor(3));
    }
    
    // test 42
    @Test public void testCtrlUpTwiceThenSpace_extended2() {
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 3,5
        assertTrue(isSelected(3,5));
        assertTrue(isNotSelected(0,1,2,4));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.SHIFT);     // select 0,1,2,3
        assertTrue(isSelected(0,1,2,3));
        assertTrue(isNotSelected(4,5));
        assertTrue(isAnchor(3));
    }
    
    // test 46
    @Test public void testHomeKey_withSelectedItems() {
        sm.clearSelection();
        sm.selectRange(4, 11);
        keyboard.doKeyPress(KeyCode.HOME);
        assertTrue(isSelected(0));
        assertTrue(isNotSelected(1,2,3,4,5,6,7,8,9,10,11));
    }
    
    // test 47
    @Test public void testEndKey_withSelectedItems() {
        sm.clearSelection();
        sm.selectRange(4, 11);
        keyboard.doKeyPress(KeyCode.END);
        assertTrue(isSelected(tableView.getExpandedItemCount() - 1));
        assertTrue(isNotSelected(1,2,3,4,5,6,7,8));
    }
    
    // test 48
    @Test public void testShiftHome() {
        sm.clearAndSelect(3);
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3));
        assertTrue(isNotSelected(4,5));
        assertTrue(debug(), isAnchor(3));
    }
    
    // test 49
    @Test public void testShiftEnd() {
        sm.clearAndSelect(3);
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertTrue(isSelected(3,4,5,6,7,8,9));
        assertTrue(isNotSelected(0,1,2));
        assertTrue(debug(), isAnchor(3));
    }
    
    // test 52
    @Test public void testShiftHomeThenShiftEnd() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4,5));
        assertTrue(isAnchor(5));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertTrue(isSelected(5,6,7,8,9));
        assertTrue(isAnchor(5));
    }
    
    // test 65
    @Test public void testShiftPageUp() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2));
        assertTrue(isAnchor(2));
    }
    
    // test 67
    @Test public void testCtrlAToSelectAll() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4,5,6,7,8,9));
    }
    
    
    /***************************************************************************
     * Tests for cell-based multiple selection
     **************************************************************************/    
    
    @Ignore("Bug persists")
    @Test public void testSelectionPathDeviationWorks1() {
        // select horizontally, then select two items vertically, then go back
        // in opposite direction
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col0);
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doDownArrowPress(KeyModifier.SHIFT);    // select (2, col3)
        keyboard.doDownArrowPress(KeyModifier.SHIFT);    // select (3, col3)
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(2, col2));
        assertTrue(sm.isSelected(3, col2));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);    // deselect (3, col3)
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(2, col2));
        assertFalse(sm.isSelected(3, col2));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);    // deselect (2, col3)
        assertTrue(sm.isSelected(1, col2));
        assertFalse(sm.isSelected(2, col2));
        assertFalse(sm.isSelected(3, col2));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);    // deselect (1, col3)
        assertFalse(debug(), sm.isSelected(1, col2));
        assertFalse(sm.isSelected(2, col2));
        assertFalse(sm.isSelected(3, col2));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);    // deselect (1, col2)
        assertFalse(sm.isSelected(1, col1));
    }
    
    
    /***************************************************************************
     * Tests for discontinuous multiple row selection (RT-18951)
     **************************************************************************/    
    
    // Test 1
    @Test public void test_rt18591_row_1() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,2,3,4));
        assertTrue(isAnchor(2));
    }
    
    // Test 2
    @Test public void test_rt18591_row_2() {
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(3,5));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(1,2,3,5));
        assertTrue(isAnchor(3));
    }
    
    // Test 3
    @Test public void test_rt18591_row_3() {
        // same as test 1 above
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,2,3,4));
        assertTrue(isAnchor(2));
        // end of similarities
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4));
        assertTrue(isAnchor(2));
    }
    
    // Test 4
    @Test public void test_rt18591_row_4() {
        // same as test 2 above
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(3,5));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(1,2,3,5));
        assertTrue(isAnchor(3));
        // end of similarities
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(1,2,3,4,5));
        assertTrue(isAnchor(3));
    }
    
    // Test 5 (need Page down support)
//    @Test public void test_5() {
//        // same as test 1 above
//        sm.clearAndSelect(0);
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
//        keyboard.doKeyPress(KeyCode.SPACE, 
//                KeyModifier.getShortcutKey(),
//                (Utils.isMac()  ? KeyModifier.CTRL : null));
//        assertTrue(isSelected(0,2));
//        assertTrue(isAnchor(2));
//        // end of similarities
//        
//        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
//        assertTrue(isSelected(0,2,/*until end of page */));
//        assertTrue(isAnchor(2));
//    }
    
    // Test 6
    @Test public void test_rt18591_row_6() {
        sm.clearAndSelect(10);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(debug(), isSelected(8,10));
        assertTrue(isAnchor(8));
        
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4,5,6,7,8,10));
        assertTrue(isAnchor(8));
    }
    
//    // Test 7
//    @Test public void test_rt18591_row_7() {
//        sm.clearAndSelect(0);
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
//        keyboard.doKeyPress(KeyCode.SPACE, 
//                KeyModifier.getShortcutKey(),
//                (Utils.isMac()  ? KeyModifier.CTRL : null));
//        assertTrue(isSelected(0,2));
//        assertTrue(isAnchor(2));
//        
//        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
//        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
//        assertTrue(isSelected(0,2,3,4,5,6,7,8,10)); // this isn't right
//        assertTrue(isAnchor(8));
//        
//        // NOT COMPLETE
//    }
//    
//    // Test 8
//    @Test public void test_rt18591_row_8() {
//        // NOT COMPLETE
//    }
    
    // Test 9
    @Test public void test_rt18591_row_9() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,2,3,4,5,6,7,8,9));
        assertTrue(isAnchor(2));
    }
    
    // Test 10
    @Test public void test_rt18591_row_10() {
        sm.clearAndSelect(8);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(6,8));
        assertTrue(isAnchor(6));
        
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4,5,6,8));
        assertTrue(isAnchor(6));
    }
    
    // Test 11
    @Test public void test_rt18591_row_11() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4,5));
        assertTrue(isAnchor(5));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4,5,6,7,8,9));
        assertTrue(isAnchor(5));
    }
    
    // Test 12
    @Test public void test_rt18591_row_12() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,2,3,4));
        assertTrue(isAnchor(2));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(isSelected(0,1,2,3,4));
        assertTrue(isAnchor(2));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(1,2,3,4));
        assertTrue(isAnchor(0));
        assertTrue(fm.isFocused(0));
    }
    
    
    /***************************************************************************
     * Tests for discontinuous multiple cell selection (RT-18951)
     **************************************************************************/  
    
    // Test 1
    @Test public void test_rt18591_cell_1() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col0);
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(isAnchor(0,2));
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(sm.isSelected(0,col3));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(isAnchor(0,2));
    }
    
    // Test 2
    @Test public void test_rt18591_cell_2() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col4);
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(isAnchor(0,2));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(isAnchor(0,2));
    }
    
    // Test 3
    @Test public void test_rt18591_cell_3() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col0);
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(isAnchor(0,2));
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(sm.isSelected(0,col3));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(isAnchor(0,2));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(sm.isSelected(0,col3));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(isAnchor(0,2));
    }
    
    // Test 4
    @Test public void test_rt18591_cell_4() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col4);
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(isAnchor(0,2));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doLeftArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(isAnchor(0,2));
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col0));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(0,col2));
        assertTrue(sm.isSelected(0,col3));
        assertTrue(sm.isSelected(0,col4));
        assertTrue(isAnchor(0,2));
    }
    
    // Test 5
    @Test public void test_rt18591_cell_5() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col1);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(isAnchor(2,1));
    }
    
    // Test 6
    @Test public void test_rt18591_cell_6() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(5, col1);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(5,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(isAnchor(3,1));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(1,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(5,col1));
        assertTrue(isAnchor(3,1));
    }
    
    // Test 7
    @Test public void test_rt18591_cell_7() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col1);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(1,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(isAnchor(2,1));
    }
    
    // Test 8
    @Test public void test_rt18591_cell_8() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(5, col1);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(5,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(isAnchor(3,1));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(1,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(5,col1));
        assertTrue(isAnchor(3,1));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(1,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(sm.isSelected(5,col1));
        assertTrue(isAnchor(3,1));
    }
    
    // Skipped tests 9 - 12 as they require Page Up/Down support
    
    // Test 13
    @Test public void test_rt18591_cell_13() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col1);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col1));
        for (int i = 2; i < tableView.getExpandedItemCount(); i++) {
            assertTrue(debug(),sm.isSelected(i,col1));
        }
        assertTrue(isAnchor(2,1));
    }
    
    // Test 14
    @Test public void test_rt18591_cell_14() {
        int n = tableView.getExpandedItemCount() - 1;
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(n, col1);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(n,col1));
        assertTrue(sm.isSelected(n - 2,col1));
        assertTrue(isAnchor(n - 2,1));
        
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(n,col1));
        for (int i = 0; i < n - 2; i++) {
            assertTrue(sm.isSelected(i,col1));
        }
        assertTrue(isAnchor(n - 2,1));
    }
    
    // Test 15
    @Test public void test_rt18591_cell_15() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(5, col1);

        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        for (int i = 0; i <= 5; i++) {
            assertTrue(sm.isSelected(i,col1));
        }
        assertTrue(debug(), isAnchor(5,1));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        for (int i = 0; i < tableView.getExpandedItemCount() - 1; i++) {
            assertTrue(sm.isSelected(i,col1));
        }
        assertTrue(isAnchor(5,1));
    }
    
    // Test 16
    @Test public void test_rt18591_cell_16() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);
        sm.select(0, col1);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        assertTrue(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(1,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(isAnchor(2,1));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertFalse(sm.isSelected(0,col1));
        assertTrue(sm.isSelected(1,col1));
        assertTrue(sm.isSelected(2,col1));
        assertTrue(sm.isSelected(3,col1));
        assertTrue(sm.isSelected(4,col1));
        assertTrue(isAnchor(0,1));
        assertTrue(fm.isFocused(0,col1));
    }
    
//    // Test 17
//    @Test public void test_rt18591_cell_17() {
//        sm.setSelectionMode(SelectionMode.MULTIPLE);
//        sm.setCellSelectionEnabled(true);
//        sm.select(3, col1);
//        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
//        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
//        keyboard.doKeyPress(KeyCode.SPACE, 
//                KeyModifier.getShortcutKey(),
//                (Utils.isMac()  ? KeyModifier.CTRL : null));
//        assertTrue(sm.isSelected(3,col1));
//        assertTrue(sm.isSelected(3,col3));
//        assertTrue(isAnchor(3,3));
//        
//        keyboard.doRightArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
//        assertTrue(sm.isSelected(3,col1));
//        assertTrue(sm.isSelected(3,col3));
//        assertTrue(sm.isSelected(3,col4));
//        assertTrue(isAnchor(3,3));
//        
//        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
//        keyboard.doDownArrowPress(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
//        assertTrue(sm.isSelected(3,col1));
//        assertTrue(sm.isSelected(3,col3));
//        assertTrue(sm.isSelected(3,col4));
//        assertTrue(sm.isSelected(4,col3));
//        assertTrue(sm.isSelected(5,col3));
//        assertTrue(isAnchor(3,3));
//    }
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
    @Test public void test_rt18488_selectToLeft() {
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col4);
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col4)
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col1)
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // deselect (1, col1)
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(debug(), sm.isSelected(1, col1));
        assertFalse(sm.isSelected(1, col0));
    }
    
    @Test public void test_rt18488_selectToRight() {
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col0);
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col4)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col5)
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // deselect (1, col5)
        assertFalse(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
    }
    
    @Test public void test_rt18488_comment1() {
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col0);
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col4)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col5)
        keyboard.doDownArrowPress(KeyModifier.SHIFT);    // select (2, col5)
        
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);     // deselect (2, col5)
        assertFalse(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
    }
    
    @Test public void test_rt18536_positive_horizontal() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col0);
        
        // move focus by holding down ctrl button
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col2)
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col3)
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col4)
        keyboard.doRightArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col5)
        assertTrue(fm.isFocused(1, col4));
        
        // press shift + space to select all cells between (1, col1) and (1, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col4));
        assertTrue(debug(), sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
    }
    
    @Test public void test_rt18536_negative_horizontal() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col4);
        
        // move focus by holding down ctrl button
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col4)
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col3)
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col2)
        keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col1)
        assertTrue(fm.isFocused(1, col0));
        
        // press shift + space to select all cells between (1, col1) and (1, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(debug(), sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col0));
    }

    //
    @Test public void test_rt18536_positive_vertical() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col4);
        
        // move focus by holding down ctrl button
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // move focus to (2, col5)
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // move focus to (3, col5)
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // move focus to (4, col5)
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // move focus to (5, col5)
        assertTrue(fm.isFocused(5, col4));
        
        // press shift + space to select all cells between (1, col5) and (5, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(3, col4));
        assertTrue(sm.isSelected(4, col4));
        assertTrue(sm.isSelected(5, col4));
    }
    
    //
    @Test public void test_rt18536_negative_vertical() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(5, col4);
        
        // move focus by holding down ctrl button
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());   // move focus to (4, col5)
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());   // move focus to (3, col5)
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());   // move focus to (2, col5)
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());   // move focus to (1, col5)
        assertTrue(fm.isFocused(1, col4));
        
        // press shift + space to select all cells between (1, col5) and (5, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(3, col4));
        assertTrue(sm.isSelected(4, col4));
        assertTrue(sm.isSelected(5, col4));
    }
    
    //
    @Test public void test_rt18642() {
        sm.setCellSelectionEnabled(false);
        sm.clearAndSelect(1);                          // select 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 2
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 3
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set anchor, and also select, 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 4
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 5
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set anchor, and also select, 5
        
        assertTrue(isSelected(1, 3, 5));
        assertTrue(isNotSelected(0, 2, 4));
        
        // anchor is at 5, so shift+UP should select rows 4 and 5 only
        keyboard.doUpArrowPress(KeyModifier.SHIFT);   
        assertTrue(isSelected(4, 5));
        assertTrue(isNotSelected(0, 1, 2, 3));
    }
    
    
    
    
    
    /***************************************************************************
     * 
     * 
     * Tests taken from TreeViewKeyInputTest
     * 
     * 
     **************************************************************************/  
    
    /***************************************************************************
     * Tests for row-based single selection
     **************************************************************************/
    
    // test 19
    @Test public void testCtrlDownMovesFocusButLeavesSelectionAlone() {
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
    }
    
    // test 23
    @Test public void testCtrlUpMovesFocus() {
        sm.clearAndSelect(1);
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(1));
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(1));
    }
    
    // test 25
    @Test public void testCtrlDownArrowWithSpaceChangesAnchor() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1));
        assertTrue(isAnchor(2));
    }
    
    // test 26
    @Test public void testCtrlUpArrowWithSpaceChangesAnchor() {
        sm.clearAndSelect(2);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1));
        assertTrue(isAnchor(0));
    }
    
    // test 53
    @Test public void testCtrlHome() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey());
        assertTrue(isSelected(5));
        assertTrue(fm.isFocused(0));
    }
    
    // test 54
    @Test public void testCtrlEnd() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey());
        assertTrue(isSelected(5));
        assertTrue(fm.isFocused(getItemCount()));
    }
    
    // test 68
    @Test public void testCtrlSpaceToClearSelection() {
        sm.clearAndSelect(5);
        assertTrue(isSelected(5));
        assertTrue(fm.isFocused(5));
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isNotSelected(5));
        assertTrue(debug(), fm.isFocused(5));
        assertTrue(isAnchor(5));
    }
    
    /***************************************************************************
     * Tests for discontinuous multiple selection (RT-18952)
     **************************************************************************/  
    
    // Test 1
    @Test public void test_rt18952_1() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(0,2,3,4));
        assertTrue(isAnchor(2));
    }
    
    // Test 2
    @Test public void test_rt18952_2() {
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(3,5));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(1,2,3,5));
        assertTrue(isAnchor(3));
    }
    
    // Test 3
    @Test public void test_rt18952_3() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,2,3,4));
        assertTrue(isAnchor(2));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4));
        assertTrue(isAnchor(2));
    }
    
    // Test 4
    @Test public void test_rt18952_4() {
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(3,5));
        assertTrue(isAnchor(3));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(1,2,3,5));
        assertTrue(isAnchor(3));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(1,2,3,4,5));
        assertTrue(isAnchor(3));
    }
    
    // TODO skipped some tests here (5-8)
    
    // Test 9
    @Test public void test_rt18952_9() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,2,3,4,5,6,7,8,9));
        assertTrue(isAnchor(2));
    }
    
    // Test 10
    @Test public void test_rt18952_10() {
        sm.clearAndSelect(9);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(7,9));
        assertTrue(isAnchor(7));
        
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4,5,6,7,9));
        assertTrue(isAnchor(7));
    }
    
    // Test 11
    @Test public void test_rt18952_11() {
        sm.clearAndSelect(5);
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4,5));
        assertTrue(isAnchor(5));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4,5,6,7,8,9));
        assertTrue(isAnchor(5));
    }
    
    // Test 12
    @Test public void test_rt18952_12() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE,
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(0,2));
        assertTrue(isAnchor(2));
        
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,2,3,4));
        assertTrue(isAnchor(2));
        
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4));
        assertTrue(isAnchor(2));

        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, 
                KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(1,2,3,4));
        assertTrue(fm.isFocused(0));
        assertTrue(isAnchor(0));
    }
    
    
    
    /***************************************************************************
     * Tests for editing
     **************************************************************************/
    
//    // test 43 (part 1)
//    @Test public void testF2EntersEditModeAndEscapeCancelsEdit_part1() {
//        tableView.setEditable(true);
//        
//        sm.clearAndSelect(0);
//        assertNull(tableView.getEditingItem());
//        keyboard.doKeyPress(KeyCode.F2);
//        assertEquals(root, tableView.getEditingItem());
//        
//        keyboard.doKeyPress(KeyCode.ESCAPE);
//        assertNull(tableView.getEditingItem());
//    }
//    
//    // test 43 (part 2)
//    @Test public void testF2EntersEditModeAndEscapeCancelsEdit_part2() {
//        tableView.setEditable(true);
//        
//        sm.clearAndSelect(0);
//        keyboard.doKeyPress(KeyCode.F2);
//        
//        
//    }
    
    
    /***************************************************************************
     * Tests for Tree(Table)View-specific functionality
     **************************************************************************/ 
    
    // Test 1 (TreeView test cases)
    @Test public void testRightArrowExpandsBranch() {
        sm.clearAndSelect(0);
        root.setExpanded(false);
        assertFalse(root.isExpanded());
        keyboard.doRightArrowPress();
        assertTrue(root.isExpanded());
    }
    
    // Test 2 (TreeView test cases)
    @Test public void testRightArrowOnExpandedBranch() {
        sm.clearAndSelect(0);
        keyboard.doRightArrowPress();
        assertTrue(isNotSelected(0));
        assertTrue(isSelected(1));
    }
    
    // Test 3 (TreeView test cases)
    @Test public void testRightArrowOnLeafNode() {
        sm.clearAndSelect(1);
        keyboard.doRightArrowPress();
        assertTrue(isNotSelected(0));
        assertTrue(isSelected(1));
        assertTrue(isNotSelected(2));
    }
    
    // Test 4 (TreeView test cases)
    @Test public void testLeftArrowCollapsesBranch() {
        sm.clearAndSelect(0);
        assertTrue(root.isExpanded());
        keyboard.doLeftArrowPress();
        assertFalse(root.isExpanded());
    }
    
    // Test 5 (TreeView test cases)
    @Test public void testLeftArrowOnLeafMovesSelectionToParent() {
        sm.clearAndSelect(2);
        assertTrue(root.isExpanded());
        keyboard.doLeftArrowPress();
        assertTrue(root.isExpanded());
        assertTrue(isSelected(0));
        assertTrue(isNotSelected(2));
    }
    
    // Test 6 (TreeView test cases)
    @Test public void testLeftArrowMultipleTimes() {
        sm.clearAndSelect(5);
        keyboard.doLeftArrowPress();
        assertTrue(child3.isExpanded());
        assertTrue(isSelected(3));
        assertTrue(isNotSelected(5));
        
        keyboard.doLeftArrowPress();
        assertFalse(child3.isExpanded());
        assertTrue(isSelected(3));
        
        keyboard.doLeftArrowPress();
        assertTrue(isSelected(0));
        assertTrue(root.isExpanded());
        
        keyboard.doLeftArrowPress();
        assertTrue(isSelected(0));
        assertFalse(root.isExpanded());
    }
    
    // Test 7 (TreeView test cases)
    @Test public void testDownArrowTwice() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress();
        keyboard.doDownArrowPress();
        assertTrue(isSelected(2));
        assertTrue(isNotSelected(0));
    }
    
    // Test 8 (TreeView test cases)
    @Test public void testDownArrowFourTimes() {
        // adding children to child2, but not expanding it
        child2.getChildren().addAll(new TreeItem("another child"), new TreeItem("And another!"));
        child2.setExpanded(false);
        
        child3.setExpanded(true);
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress();
        keyboard.doDownArrowPress();
        keyboard.doDownArrowPress();
        keyboard.doDownArrowPress();
        assertTrue(isSelected(4));
        assertTrue(isNotSelected(0));
    }
    
    // Test 9 (TreeView test cases)
    @Test public void testUpArrow() {
        sm.clearAndSelect(1);
        keyboard.doUpArrowPress();
        assertTrue(isSelected(0));
        assertTrue(isNotSelected(1));
    }
    
    // Test 9 (TreeView test cases)
    @Test public void testUpArrowFourTimes() {
        // adding children to child2, but not expanding it
        child2.getChildren().addAll(new TreeItem("another child"), new TreeItem("And another!"));
        child2.setExpanded(false);
        
        sm.clearAndSelect(5);
        keyboard.doUpArrowPress();
        keyboard.doUpArrowPress();
        keyboard.doUpArrowPress();
        keyboard.doUpArrowPress();
        
        assertTrue(isSelected(1));
        assertTrue(isNotSelected(5));
    }
    
    // Test 20 (TreeView test cases)
    // NOTE: this used to be isSelected but changed when we removed functionality
    // for KeyCode.SLASH. Rather than remove the test I'm now testing to make
    // sure it does nothing.
    @Test public void testCtrlForwardSlashToSelectAll() {
        sm.clearAndSelect(1);
        keyboard.doKeyPress(KeyCode.SLASH, KeyModifier.getShortcutKey());
        assertTrue(isSelected(1));
        assertTrue(isNotSelected(0,2,3,4,5,6,7,8,9));
    }
    
    // Test 21 (TreeView test cases)
    // NOTE: this used to be isNotSelected but changed when we removed functionality
    // for KeyCode.BACK_SLASH. Rather than remove the test I'm now testing to make
    // sure it does nothing.
    @Test public void testCtrlBackSlashToClearSelection() {
        sm.selectAll();
        fm.focus(1);
        keyboard.doKeyPress(KeyCode.BACK_SLASH, KeyModifier.getShortcutKey());
        assertTrue(debug(), isSelected(0,1,2,3,4,5,6,7,8,9));
        assertTrue(fm.isFocused(1));
    }
    
    // Test 24 (TreeView test cases)
    @Ignore("Not yet working")
    @Test public void testExpandCollapseImpactOnSelection() {
        sm.clearAndSelect(5);
        assertTrue(child3.isExpanded());
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(isSelected(3,4,5));
        
        keyboard.doLeftArrowPress();
        assertFalse(child3.isExpanded());
        assertTrue(isSelected(3));
        
        keyboard.doRightArrowPress();
        assertTrue(child3.isExpanded());
        assertTrue(isSelected(3,4,5));
    }
    
    // Test 54 (TreeView test cases)
    @Test public void testAsteriskExpandsAllBranchesFromRoot() {
        // adding children to child2, but not expanding it
        child2.getChildren().addAll(new TreeItem("another child"), new TreeItem("And another!"));
        child2.setExpanded(false);
        
        sm.clearAndSelect(0);
        assertFalse(child2.isExpanded());
        assertTrue(child3.isExpanded());
        keyboard.doKeyPress(KeyCode.MULTIPLY);
        
        assertTrue(child2.isExpanded());
        assertTrue(child3.isExpanded());
    }
    
    // Test 57 (TreeView test cases)
    @Test public void testMinusCollapsesBranch() {
        sm.clearAndSelect(3);
        assertTrue(child3.isExpanded());
        keyboard.doKeyPress(KeyCode.SUBTRACT);
        assertFalse(child3.isExpanded());
    }
    
    // Test 58 (TreeView test cases)
    @Test public void testPlusCollapsesBranch() {
        sm.clearAndSelect(3);
        child3.setExpanded(false);
        assertFalse(child3.isExpanded());
        keyboard.doKeyPress(KeyCode.ADD);
        assertTrue(child3.isExpanded());
    }
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
//    @Test public void test_rt18642() {
//        sm.clearAndSelect(1);                          // select 1
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 2
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 3
//        keyboard.doKeyPress(KeyCode.SPACE, 
//                KeyModifier.getShortcutKey(),
//                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set anchor, and also select, 3
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 4
//        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 5
//        keyboard.doKeyPress(KeyCode.SPACE, 
//                KeyModifier.getShortcutKey(),
//                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set anchor, and also select, 5
//        
//        assertTrue(isSelected(1, 3, 5));
//        assertTrue(isNotSelected(0, 2, 4));
//        
//        // anchor is at 5, so shift+UP should select rows 4 and 5 only
//        keyboard.doUpArrowPress(KeyModifier.SHIFT);   
//        assertTrue(isSelected(4, 5));
//        assertTrue(isNotSelected(0, 1, 2, 3));
//    }
    
    @Test public void test_rt14451_1() {
        sm.clearAndSelect(5);                          

        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT); 
        assertTrue(debug(), isSelected(0,1,2,3,4,5));
        assertTrue(isNotSelected(6,7,8,9));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT); 
        assertTrue(isNotSelected(0,1,2,3,4));
        assertTrue(isSelected(5,6,7,8,9));
        
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT); 
        assertTrue(isSelected(0,1,2,3,4,5));
        assertTrue(debug(), isNotSelected(6,7,8,9));
    } 
    
    @Test public void test_rt14451_2() {
        sm.clearAndSelect(5);                          

        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT); 
        assertTrue(isNotSelected(0,1,2,3,4));
        assertTrue(isSelected(5,6,7,8,9));
        
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT); 
        assertTrue(isSelected(0,1,2,3,4,5));
        assertTrue(debug(), isNotSelected(6,7,8,9));
        
        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT); 
        assertTrue(isNotSelected(0,1,2,3,4));
        assertTrue(isSelected(5,6,7,8,9));
    } 
    
    @Test public void test_rt26835_1() {
        sm.clearAndSelect(5);                          
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.getShortcutKey()); 
        assertTrue(fm.isFocused(0));
    } 
    
    @Test public void test_rt26835_2() {
        sm.clearAndSelect(5);                          
        keyboard.doKeyPress(KeyCode.END, KeyModifier.getShortcutKey()); 
        assertTrue(debug(), fm.isFocused(getItemCount()));
    } 
    
    @Test public void test_rt27175() {
        sm.clearAndSelect(5);                          
        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT, KeyModifier.getShortcutKey()); 
        assertTrue(debug(), fm.isFocused(0));
        assertTrue(isSelected(0,1,2,3,4,5));
    } 
    
    @Test public void test_rt28065() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        tableView.getSelectionModel().select(0);
        assertEquals(0, tableView.getSelectionModel().getSelectedIndex());
        assertEquals(root, tableView.getSelectionModel().getSelectedItem());
        assertEquals(0, tableView.getFocusModel().getFocusedIndex());
        assertEquals(root, tableView.getFocusModel().getFocusedItem());
        
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(0, tableView.getSelectionModel().getSelectedIndex());
        assertEquals(root, tableView.getSelectionModel().getSelectedItem());
        assertEquals(0, tableView.getFocusModel().getFocusedIndex());
        assertEquals(root, tableView.getFocusModel().getFocusedItem());
    }
    
    @Test public void test_rt27583_cellSelection_1() {
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.select(0, col0);
        assertTrue(fm.isFocused(0, col0));
        
        // focus should not go out the top of the table
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(1, col0));
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(0, col0));
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(debug(), fm.isFocused(0, col0));
        
    }
    
    @Test public void test_rt27583_cellSelection_2() {
        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.select(10, col0);
        assertTrue(fm.isFocused(10, col0));
        
        // focus should not go out the bottom of the table
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(11, col0));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(12, col0));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(13, col0));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(debug(), fm.isFocused(13, col0));
    }
    
    @Test public void test_rt27583_rowSelection_1() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.select(0);
        assertTrue(fm.isFocused(0));
        
        // focus should not go out the top of the table
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(1));
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(0));
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(debug(), fm.isFocused(0));
        
    }
    
    @Test public void test_rt27583_rowSelection_2() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.select(10);
        assertTrue(fm.isFocused(10));
        
        // focus should not go out the bottom of the table
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(11));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(12));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(fm.isFocused(13));
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(debug(), fm.isFocused(13));
    }
    
    @Test public void test_rt29930() {
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(0);
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT); // select rows [0,1]
        keyboard.doDownArrowPress(KeyModifier.SHIFT); // select rows [0,1,2]
        assertTrue(isSelected(0,1,2));
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(0, getAnchor().getRow());
        
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), PlatformUtil.isMac() ? KeyModifier.CTRL : null); // set new anchor point
        assertTrue(isSelected(0,1));
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(2, getAnchor().getRow());
        
        keyboard.doDownArrowPress(KeyModifier.SHIFT); // select rows [2,3]
        assertTrue(isSelected(2,3));
        assertTrue(debug(), isNotSelected(0,1));
        assertEquals(3, fm.getFocusedIndex());
        assertEquals(2, getAnchor().getRow());
    }

    private int rt29849_start_count = 0;
    private int rt29849_cancel_count = 0;
    @Test public void test_rt29849() {
        tableView.setEditable(true);
        col0.setEditable(true);

        col0.setCellValueFactory(param -> new ReadOnlyStringWrapper("DUMMY TEXT"));

        col0.setOnEditStart(t -> {
            rt29849_start_count++;
        });
        col0.setOnEditCancel(t -> {
            rt29849_cancel_count++;
        });

        // initially the counts should be zero
        assertEquals(0, rt29849_start_count);
        assertEquals(0, rt29849_cancel_count);

        IndexedCell cell = VirtualFlowTestUtils.getCell(tableView, 0, 0);
        assertTrue(cell.isEditable());
        assertFalse(cell.isEditing());
        assertEquals(0, cell.getIndex());

        // do an edit, start count should be one, cancel still zero
        tableView.edit(0, col0);
        assertTrue(cell.isEditing());
        assertEquals(1, rt29849_start_count);
        assertEquals(0, rt29849_cancel_count);

        // cancel edit, now both counts should be 1
        keyboard.doKeyPress(KeyCode.ESCAPE);
        assertFalse(cell.isEditing());
        assertEquals(1, rt29849_start_count);
        assertEquals(1, rt29849_cancel_count);
    }

    private int rt31577_count = 0;
    @Test public void test_rt31577() {
        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearSelection();

        // the actual bug is that the selectedItem property does not fire an
        // event when the selected items list changes (due to deselection).
        // It actually does always contain the right value - it just doesn't
        // let anyone know it!
        sm.selectedItemProperty().addListener(observable -> {
            rt31577_count++;
        });

        assertTrue(sm.getSelectedItems().isEmpty());
        assertFalse(sm.isSelected(1));
        assertEquals(0, rt31577_count);

        // select the first row
        keyboard.doKeyPress(KeyCode.KP_DOWN);
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(sm.isSelected(0));
        assertTrue(sm.getSelectedItems().contains(root));
        assertEquals(root, sm.getSelectedItem());
        assertEquals(1, rt31577_count);

        // deselect the row
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.CTRL,
                Utils.isMac() ? KeyModifier.getShortcutKey() : null);
        assertTrue(sm.getSelectedItems().isEmpty());
        assertFalse(sm.isSelected(1));
        assertNull(sm.getSelectedItem());
        assertEquals(2, rt31577_count);
    }

    @Test public void test_rt32383_pageDown() {
        // this test requires a lot of data
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<String>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(0);

        final TreeItem<String> initialFocusOwner = fm.getFocusedItem();

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> newFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, newFocusOwner);

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> nextFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, nextFocusOwner);
        assertNotSame(newFocusOwner, nextFocusOwner);
    }

    @Test public void test_rt32383_pageUp() {
        // this test requires a lot of data
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<String>("Row " + i));
        }

        final int lastIndex = 99;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(lastIndex);

        // need to make sure we scroll down to the bottom!
        tableView.scrollTo(lastIndex);
        Toolkit.getToolkit().firePulse();

        final TreeItem<String> initialFocusOwner = fm.getFocusedItem();

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> newFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, newFocusOwner);

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> nextFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, nextFocusOwner);
        assertNotSame(newFocusOwner, nextFocusOwner);
    }

    @Test public void test_rt27710_pageDown_singleSelection_cell() {
        // this test requires a lot of data
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<String>("Row " + i));
        }

        col0.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(0, col0);

        final TreeItem<String> initialFocusOwner = fm.getFocusedItem();

        // because single selection is enabled, shift+pgDown should result in
        // the same result as ctrl+pgDown
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> newFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, newFocusOwner);

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> nextFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, nextFocusOwner);
        assertNotSame(newFocusOwner, nextFocusOwner);
    }

    @Test public void test_rt27710_pageUp_singleSelection_cell() {
        // this test requires a lot of data
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<String>("Row " + i));
        }

        col0.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

        final int lastIndex = 99;

        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(lastIndex, col0);

        // need to make sure we scroll down to the bottom!
        tableView.scrollTo(lastIndex);
        Toolkit.getToolkit().firePulse();

        final TreeItem<String> initialFocusOwner = fm.getFocusedItem();

        // because single selection is enabled, shift+pgUp should result in
        // the same result as ctrl+pgUp
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> newFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, newFocusOwner);

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final TreeItem<String> nextFocusOwner = fm.getFocusedItem();
        assertNotSame(initialFocusOwner, nextFocusOwner);
        assertNotSame(newFocusOwner, nextFocusOwner);
    }

    @Test public void test_rt19053_pageUp() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int middleIndex = items / 2;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        tableView.setShowRoot(false);
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(middleIndex);

        assertEquals(middleIndex, sm.getSelectedIndex());

        final Object initialSelectionOwner = sm.getSelectedItem();

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final Object newSelectionOwner = sm.getSelectedItem();
        assertNotSame(initialSelectionOwner + " == " + newSelectionOwner, initialSelectionOwner, newSelectionOwner);

        // selection should go all the way to the top, but this bug
        // shows that instead it seems to stop midway - where the anchor is
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(0, sm.getSelectedIndex());
        final Object nextSelectionOwner =  sm.getSelectedItem();
        assertNotSame(initialSelectionOwner, nextSelectionOwner);
        assertNotSame(newSelectionOwner, nextSelectionOwner);
    }

    @Test public void test_rt19053_pageDown() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int middleIndex = items / 2;

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        tableView.setShowRoot(false);
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(middleIndex);

        assertEquals(middleIndex, sm.getSelectedIndex());

        final Object initialSelectionOwner = sm.getSelectedItem();

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final Object newSelectionOwner = sm.getSelectedItem();
        assertNotSame(initialSelectionOwner, newSelectionOwner);

        // selection should go all the way to the bottom, but this bug
        // shows that instead it seems to stop midway - where the anchor is
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(items - 1, fm.getFocusedIndex());
        assertEquals(items - 1, sm.getSelectedIndex());
        final Object nextSelectionOwner =  sm.getSelectedItem();
        assertNotSame(initialSelectionOwner, nextSelectionOwner);
        assertNotSame(newSelectionOwner, nextSelectionOwner);
    }

    @Test public void test_rt21375_scenario_1a_down() {
        sm.clearSelection();

        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(0,1,2,3));
        assertEquals(4, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_1b_down() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(0,1,2,3));
        assertEquals(4, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_2_down() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), PlatformUtil.isMac() ? KeyModifier.CTRL : null);
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(2,3,4));
        assertEquals(3, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_3_down() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(0);

        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), PlatformUtil.isMac() ? KeyModifier.CTRL : null);
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(0,2,3,4));
        assertEquals(4, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_1a_up() {
        sm.clearSelection();

        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(7);

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(7,6,5,4));
        assertEquals(4, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_1b_up() {
        sm.clearSelection();

        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(7);

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(7,6,5,4));
        assertEquals(4, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_2_up() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(7);

        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), PlatformUtil.isMac() ? KeyModifier.CTRL : null);
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(5,4,3));
        assertEquals(3, sm.getSelectedItems().size());
    }

    @Test public void test_rt21375_scenario_3_up() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.clearAndSelect(7);

        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), PlatformUtil.isMac() ? KeyModifier.CTRL : null);
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(7,5,4,3));
        assertEquals(4, sm.getSelectedItems().size());
    }

    @Test public void test_rt33301_multipleSelection_down() {
        final int items = 4;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final TableFocusModel fm = tableView.getFocusModel();
        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        sm.clearAndSelect(2);

        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 3
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 4
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(0,1));
        assertTrue(isSelected(2,3,4));
        assertEquals(3, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(4));

        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 4
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 4
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 4
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(0,1));
        assertTrue(isSelected(2,3,4));
        assertEquals(3, sm.getSelectedItems().size());
        assertTrue("Focus index incorrectly at: " + fm.getFocusedIndex(), fm.isFocused(4));
    }

    @Test public void test_rt33301_multipleSelection_up() {
        final int items = 4;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final TableFocusModel fm = tableView.getFocusModel();
        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);
        sm.clearAndSelect(2);

        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 0
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(3,4));
        assertTrue(isSelected(0,1,2));
        assertEquals(3, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(0));

        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 0
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 0
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 0
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(3,4));
        assertTrue(isSelected(0,1,2));
        assertEquals(3, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(0));
    }

    @Test public void test_rt33301_singleSelection_down() {
        final int items = 4;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final TableFocusModel fm = tableView.getFocusModel();
        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(false);
        sm.clearAndSelect(2);

        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 3
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 4
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(0,1,2,3));
        assertTrue(isSelected(4));
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(4));

        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 4
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 4
        keyboard.doKeyPress(KeyCode.DOWN,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 4
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(0,1,2,3));
        assertTrue(isSelected(4));
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(4));
    }

    @Test public void test_rt33301_singleSelection_up() {
        final int items = 4;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final TableFocusModel fm = tableView.getFocusModel();
        final TableSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(false);
        sm.clearAndSelect(2);

        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // row 0
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(1,2,3,4));
        assertTrue(isSelected(0));
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(0));

        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 0
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 0
        keyboard.doKeyPress(KeyCode.UP,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT); // should stay at row 0
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(1,2,3,4));
        assertTrue(isSelected(0));
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(0));
    }

    private int rt_33559_count = 0;
    @Test public void test_rt33559() {
        final int items = 4;
        root.getChildren().clear();
        root.setExpanded(false);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(0);

        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c -> {
            while (c.next()) {
                rt_33559_count++;
            }
        });

        assertEquals(0, rt_33559_count);
        keyboard.doKeyPress(KeyCode.RIGHT); // expand root
        assertEquals(0, rt_33559_count);
    }

    @Test public void test_rt20915() {
        final FocusModel fm = tableView.getFocusModel();
        final MultipleSelectionModel sm = tableView.getSelectionModel();
        sm.clearAndSelect(0);

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertTrue(isNotSelected(1,2,3));
        assertTrue(isSelected(0));
        assertEquals(1, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(3));

        keyboard.doKeyPress(KeyCode.SPACE,  KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertTrue(isSelected(0,1,2,3));
        assertEquals(4, sm.getSelectedItems().size());
        assertTrue(fm.isFocused(3));
    }

    @Test public void test_rt34200() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.clearAndSelect(99);
        tableView.scrollTo(99);
        assertEquals(99, getAnchor().getRow());
        assertEquals(99, fm.getFocusedIndex());

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(99, getAnchor().getRow());
        assertTrue(fm.getFocusedIndex() < 99);
    }

    @Test public void test_rt34369_cellSelection() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(99, col0);
        tableView.scrollTo(99);
        assertEquals(99, getAnchor().getRow());
        assertEquals(col0, getAnchor().getTableColumn());
        assertEquals(99, fm.getFocusedIndex());

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(99, getAnchor().getRow());
        assertEquals(col0, getAnchor().getTableColumn());
        assertTrue(fm.getFocusedIndex() < 99);
    }

    @Test public void test_rt34369_rowSelection() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(false);

        sm.clearAndSelect(99);
        tableView.scrollTo(99);
        assertEquals(99, getAnchor().getRow());
        assertEquals(99, fm.getFocusedIndex());

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(99, getAnchor().getRow());
        assertTrue(fm.getFocusedIndex() < 99);
    }

    @Test public void test_rt33894() {
        final int items = 5;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.clearAndSelect(1);
        assertEquals(1, getAnchor().getRow());
        assertEquals(1, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(1, getAnchor().getRow());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(2, getAnchor().getRow());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(2, sm.getSelectedIndex());
        assertTrue(isSelected(1, 2));

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(2, getAnchor().getRow());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(2, sm.getSelectedIndex());
        assertTrue(isSelected(1, 2));

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(0, sm.getSelectedIndex());
        assertTrue(isSelected(0, 1, 2));
    }

    @Test public void test_rt34425() {
        final int items = 5;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.clearAndSelect(1);
        assertEquals(1, getAnchor().getRow());
        assertEquals(1, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(1, getAnchor().getRow());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.SPACE);
        Toolkit.getToolkit().firePulse();
        assertEquals(2, getAnchor().getRow());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(2, sm.getSelectedIndex());
        assertTrue(isSelected(1, 2));
    }

    @Test public void test_rt33613_up_oneColumn() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(6, col0);
        assertEquals(6, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertEquals(6, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(6, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertEquals(3, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(6, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertEquals(3, fm.getFocusedIndex());
        assertEquals(3, sm.getSelectedIndex());
    }

    @Test public void test_rt33613_up_multipleColumn_right() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(6, col0);
        assertEquals(6, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(6, col0));
        assertTrue(sm.isSelected(6, col0));

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(6, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(3, col1));
        assertTrue(sm.isSelected(6, col0));
        assertFalse(sm.isSelected(3, col1));

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(6, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(3, col1));
        assertTrue(sm.isSelected(3, col1));
        assertTrue(sm.isSelected(6, col0));
    }

    @Test public void test_rt33613_up_multipleColumn_left() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(6, col1);
        assertEquals(6, getAnchor().getRow());
        assertEquals(1, getAnchor().getColumn());
        assertTrue(fm.isFocused(6, col1));
        assertTrue(sm.isSelected(6, col1));

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(6, getAnchor().getRow());
        assertEquals(1, getAnchor().getColumn());
        assertTrue(fm.isFocused(3, col0));
        assertTrue(sm.isSelected(6, col1));
        assertFalse(sm.isSelected(3, col0));

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(6, getAnchor().getRow());
        assertEquals(1, getAnchor().getColumn());
        assertTrue(fm.isFocused(3, col0));
        assertTrue(sm.isSelected(3, col0));
        assertTrue(sm.isSelected(6, col1));
    }

    @Test public void test_rt33613_down_oneColumn() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(3, col0);
        assertEquals(3, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertEquals(3, fm.getFocusedIndex());
        assertEquals(3, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(3, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertEquals(6, fm.getFocusedIndex());
        assertEquals(3, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(3, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertEquals(6, fm.getFocusedIndex());
        assertEquals(6, sm.getSelectedIndex());
    }

    @Test public void test_rt33613_down_multipleColumn_right() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(3, col0);
        assertEquals(3, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(3, col0));
        assertTrue(sm.isSelected(3, col0));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(3, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(6, col1));
        assertTrue(sm.isSelected(3, col0));
        assertFalse(sm.isSelected(6, col1));

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(3, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(6, col1));
        assertTrue(sm.isSelected(6, col1));
        assertTrue(sm.isSelected(3, col0));
    }

    @Test public void test_rt33613_down_multipleColumn_left() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(3, col1);
        assertEquals(3, getAnchor().getRow());
        assertEquals(1, getAnchor().getColumn());
        assertTrue(fm.isFocused(3, col1));
        assertTrue(sm.isSelected(3, col1));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(3, getAnchor().getRow());
        assertEquals(1, getAnchor().getColumn());
        assertTrue(fm.isFocused(6, col0));
        assertTrue(sm.isSelected(3, col1));
        assertFalse(sm.isSelected(6, col0));

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(3, getAnchor().getRow());
        assertEquals(1, getAnchor().getColumn());
        assertTrue(fm.isFocused(6, col0));
        assertTrue(sm.isSelected(6, col0));
        assertTrue(sm.isSelected(3, col1));
    }

    @Test public void test_rt18439() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0, col0);

        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 4
        assertEquals(0, getAnchor().getRow());
        assertEquals(debug(), 0, getAnchor().getColumn());              // anchor does not move
        assertTrue(fm.isFocused(0, col4));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 2
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());             // anchor does not move
        assertTrue(fm.isFocused(2, col4));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));

        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 0
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());             // anchor does not move
        assertTrue(fm.isFocused(2, col0));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(2, col3));
        assertTrue(sm.isSelected(2, col2));
        assertTrue(sm.isSelected(2, col1));
        assertTrue(sm.isSelected(2, col0));

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 0
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());           // anchor does not move
        assertTrue(fm.isFocused(0, col0));
        assertFalse(sm.isSelected(0, col0));                // we've gone right around - this cell is now unselected
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(2, col3));
        assertTrue(sm.isSelected(2, col2));
        assertTrue(sm.isSelected(2, col1));
        assertTrue(sm.isSelected(2, col0));
        assertTrue(sm.isSelected(1, col0));
    }

    // this is an extension of the previous test, where we had a bug where going up resulted in all cells between the
    // anchor (at (0,0)) and the first selected cell in column 0 were being selected. This wasn't visible in the previous
    // test as we only went down two rows, so when we went up everything looked as expected
    @Test public void test_rt18439_startAt_row0_col0_clockwise() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0, col0);

        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 4

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 2
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 3
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 4

        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 0

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 3
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());           // anchor does not move
        assertTrue(fm.isFocused(3, col0));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(3, col4));
        assertTrue(sm.isSelected(4, col4));
        assertTrue(sm.isSelected(4, col3));
        assertTrue(sm.isSelected(4, col2));
        assertTrue(sm.isSelected(4, col1));
        assertTrue(sm.isSelected(4, col0));
        assertTrue(sm.isSelected(3, col0));

        // critical part - these cells should not be selected!
        assertFalse(sm.isSelected(1, col0));
        assertFalse(sm.isSelected(2, col0));
    }

    @Test public void test_rt18439_startAt_row0_col4_clockwise() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0, col4);

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 2
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 3
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 4

        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 0

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 3
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 2
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 0

        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 1
        assertEquals(0, getAnchor().getRow());
        assertEquals(4, getAnchor().getColumn());           // anchor does not move
        assertTrue(fm.isFocused(0, col1));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(3, col4));
        assertTrue(sm.isSelected(4, col4));
        assertTrue(sm.isSelected(4, col3));
        assertTrue(sm.isSelected(4, col2));
        assertTrue(sm.isSelected(4, col1));
        assertTrue(sm.isSelected(4, col0));
        assertTrue(sm.isSelected(3, col0));
        assertTrue(sm.isSelected(2, col0));
        assertTrue(sm.isSelected(1, col0));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));

        // critical part - these cells should not be selected!
        assertFalse(sm.isSelected(0, col2));
        assertFalse(sm.isSelected(0, col3));
    }

    @Test public void test_rt18439_startAt_row4_col4_clockwise() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(4, col4);

        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 0

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 3
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 2
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);   // row 0

        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 1
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 2
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 3
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT); // col 4

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 1
        assertEquals(4, getAnchor().getRow());
        assertEquals(4, getAnchor().getColumn());           // anchor does not move
        assertTrue(fm.isFocused(1, col4));
        assertTrue(sm.isSelected(4, col4));
        assertTrue(sm.isSelected(4, col2));
        assertTrue(sm.isSelected(4, col2));
        assertTrue(sm.isSelected(4, col1));
        assertTrue(sm.isSelected(4, col0));
        assertTrue(sm.isSelected(3, col0));
        assertTrue(sm.isSelected(2, col0));
        assertTrue(sm.isSelected(1, col0));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));

        // critical part - these cells should not be selected!
        assertFalse(sm.isSelected(2, col4));
        assertFalse(sm.isSelected(3, col4));
    }

    @Test public void test_rt18439_startAt_row4_col0_clockwise() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(4, col0);

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 3
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 2
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // row 0

        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT);   // col 1
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT);   // col 2
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT);   // col 3
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.SHIFT);   // col 4

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 1
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 2
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 3
        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT); // row 4

        keyboard.doKeyPress(KeyCode.LEFT, KeyModifier.SHIFT); // col 3
        assertEquals(4, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());           // anchor does not move
        assertTrue(fm.isFocused(4, col3));
        assertTrue(sm.isSelected(4, col0));
        assertTrue(sm.isSelected(3, col0));
        assertTrue(sm.isSelected(2, col0));
        assertTrue(sm.isSelected(1, col0));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(2, col4));
        assertTrue(sm.isSelected(3, col4));
        assertTrue(sm.isSelected(4, col4));
        assertTrue(sm.isSelected(4, col3));

        // critical part - these cells should not be selected!
        assertFalse(sm.isSelected(4, col2));
        assertFalse(sm.isSelected(4, col1));
    }

    @Test public void test_rt34461_cellSelection() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(true);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0, col0);
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(0, col0));
        assertTrue(sm.isSelected(0, col0));
        assertFalse(sm.isSelected(1, col0));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        assertEquals(0, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(1, col0));
        assertTrue(sm.isSelected(0, col0));
        assertFalse(sm.isSelected(1, col0));

        keyboard.doKeyPress(KeyCode.SPACE);
        assertEquals(1, getAnchor().getRow());      // new anchor point
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(1, col0));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(1, col0));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT);
        assertEquals(1, getAnchor().getRow());
        assertEquals(0, getAnchor().getColumn());
        assertTrue(fm.isFocused(2, col0));
        assertFalse(sm.isSelected(0, col0));    // selection moves off here as the anchor point moved with the space
        assertTrue(sm.isSelected(1, col0));
        assertTrue(sm.isSelected(2, col0));
    }

    @Test public void test_rt34461_rowSelection() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0);
        assertEquals(0, getAnchor().getRow());
        assertEquals(-1, getAnchor().getColumn());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        assertEquals(0, getAnchor().getRow());
        assertEquals(-1, getAnchor().getColumn());
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));

        keyboard.doKeyPress(KeyCode.SPACE);
        assertEquals(1, getAnchor().getRow());      // new anchor point
        assertEquals(-1, getAnchor().getColumn());
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(0));
        assertTrue(sm.isSelected(1));

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.SHIFT);
        assertEquals(1, getAnchor().getRow());
        assertEquals(-1, getAnchor().getColumn());
        assertTrue(fm.isFocused(2));
        assertFalse(sm.isSelected(0));    // selection moves off here as the anchor point moved with the space
        assertTrue(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
    }

    @Test public void test_rt34407_down_down_up() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setPrefHeight(130); // roughly room for four rows

        StageLoader sl = new StageLoader(tableView);
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0);
        fm.focus(0);
        assertEquals(0, getAnchor().getRow());
        assertEquals(-1, getAnchor().getColumn());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));

        // we expect the final Page-up to return us back to this selected index and with the same number of selected indices
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        final int leadSelectedIndex = sm.getSelectedIndex();
        final int selectedIndicesCount = sm.getSelectedIndices().size();
        assertEquals(3, leadSelectedIndex);
        assertEquals(3, fm.getFocusedIndex());
        assertEquals(4, selectedIndicesCount);

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        assertEquals(leadSelectedIndex * 2, sm.getSelectedIndex());
        assertEquals(leadSelectedIndex * 2, fm.getFocusedIndex());
        assertEquals(selectedIndicesCount * 2 - 1, sm.getSelectedIndices().size());

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        assertEquals(leadSelectedIndex, sm.getSelectedIndex());
        assertEquals(leadSelectedIndex, fm.getFocusedIndex());
        assertEquals(selectedIndicesCount, sm.getSelectedIndices().size());

        sl.dispose();
    }

    @Test public void test_rt34407_up_up_down() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        tableView.setPrefHeight(160); // roughly room for four rows

        StageLoader sl = new StageLoader(tableView);
        sm.setCellSelectionEnabled(false);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(99);
        fm.focus(99);
        tableView.scrollTo(99);
        Toolkit.getToolkit().firePulse();

        assertEquals(99, getAnchor().getRow());
        assertEquals(-1, getAnchor().getColumn());
        assertTrue(fm.isFocused(99));
        assertTrue(sm.isSelected(99));
        assertFalse(sm.isSelected(98));

        // we expect the final Page-down to return us back to this selected index and with the same number of selected indices
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        final int leadSelectedIndex = sm.getSelectedIndex();
        final int selectedIndicesCount = sm.getSelectedIndices().size();
        final int diff = 99 - leadSelectedIndex;
        assertEquals(99 - diff, leadSelectedIndex);
        assertEquals(99 - diff, fm.getFocusedIndex());
        assertEquals(4, selectedIndicesCount);

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        assertEquals(99 - diff * 2 - 1, sm.getSelectedIndex());
        assertEquals(selectedIndicesCount * 2, sm.getSelectedIndices().size());

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        assertEquals(leadSelectedIndex, sm.getSelectedIndex());
        assertEquals(selectedIndicesCount, sm.getSelectedIndices().size());

        sl.dispose();
    }

    @Test public void test_rt34768() {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TreeTableColumn<String, String> firstNameCol = new TreeTableColumn<>("First Name");
        tableView.getColumns().setAll(firstNameCol);
        tableView.setRoot(null);

        // no need for an assert here - we're testing for an AIOOBE
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
    }

    @Test public void test_rt35853_multipleSelection_rowSelection_shiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertTrue(sm.isSelected(5));
    }

    @Test public void test_rt35853_multipleSelection_rowSelection_noShiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP);
        assertEquals(4, getAnchor().getRow());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt35853_singleSelection_rowSelection_shiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.SINGLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(4, getAnchor().getRow());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt35853_singleSelection_rowSelection_noShiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.SINGLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP);
        assertEquals(4, getAnchor().getRow());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt35853_multipleSelection_cellSelection_shiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(5, col);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5, col));
        assertTrue(sm.isSelected(5, col));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(4, col));
        assertTrue(sm.isSelected(4, col));
        assertTrue(sm.isSelected(5, col));
    }

    @Test public void test_rt35853_multipleSelection_cellSelection_noShiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(5, col);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5, col));
        assertTrue(sm.isSelected(5, col));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP);
        assertEquals(4, getAnchor().getRow());
        assertTrue(fm.isFocused(4, col));
        assertTrue(sm.isSelected(4, col));
        assertFalse(sm.isSelected(5, col));
    }

    @Test public void test_rt35853_singleSelection_cellSelection_shiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(5, col);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5, col));
        assertTrue(sm.isSelected(5, col));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(4, getAnchor().getRow());
        assertTrue(fm.isFocused(4, col));
        assertTrue(sm.isSelected(4, col));
        assertFalse(sm.isSelected(5, col));
    }

    @Test public void test_rt35853_singleSelection_cellSelection_noShiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(true);

        sm.clearAndSelect(5, col);
        assertEquals(5, getAnchor().getRow());
        assertTrue(fm.isFocused(5, col));
        assertTrue(sm.isSelected(5, col));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP);
        assertEquals(4, getAnchor().getRow());
        assertTrue(fm.isFocused(4, col));
        assertTrue(sm.isSelected(4, col));
        assertFalse(sm.isSelected(5, col));
    }

    @Test public void test_rt36800_rowSelection() {
        test_rt36800(false);
    }

    @Test public void test_rt36800_cellSelection() {
        test_rt36800(true);
    }

    private void test_rt36800(boolean cellSelection) {
        // get the current exception handler before replacing with our own,
        // as ListListenerHelp intercepts the exception otherwise
        final Thread.UncaughtExceptionHandler exceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            fail("We don't expect any exceptions in this test!");
        });

        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.setCellSelectionEnabled(cellSelection);

        if (cellSelection) {
            sm.clearAndSelect(5, col);
            assertEquals(5, getAnchor().getRow());
            assertEquals(col, getAnchor().getTableColumn());
            assertTrue(fm.isFocused(5, col));
            assertTrue(sm.isSelected(5, col));
        } else {
            sm.clearAndSelect(5);
            assertEquals(5, getAnchor().getRow());
            assertTrue(fm.isFocused(5));
            assertTrue(sm.isSelected(5));
        }

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 4
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 3
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 2
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 0
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // bug time?

        if (cellSelection) {
            assertEquals(0, getAnchor().getRow());
            assertEquals(col, getAnchor().getTableColumn());
            assertTrue(fm.isFocused(0, col));
            assertTrue(sm.isSelected(0, col));
            assertFalse(sm.isSelected(1, col));
            assertFalse(sm.isSelected(2, col));
            assertFalse(sm.isSelected(3, col));
            assertFalse(sm.isSelected(4, col));
            assertFalse(sm.isSelected(5, col));
        } else {
            assertEquals(0, getAnchor().getRow());
            assertTrue(fm.isFocused(0));
            assertTrue(sm.isSelected(0));
            assertFalse(sm.isSelected(1));
            assertFalse(sm.isSelected(2));
            assertFalse(sm.isSelected(3));
            assertFalse(sm.isSelected(4));
            assertFalse(sm.isSelected(5));
        }

        // reset the exception handler
        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
    }

    @Test public void test_rt_37130_pageUpAtTop() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        StageLoader sl = new StageLoader(tableView);

        sm.select(5, col);
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);

        sl.dispose();
    }

    @Test public void test_rt_37130_pageUpAtBottom() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        StageLoader sl = new StageLoader(tableView);

        sm.select(95, col);
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);

        sl.dispose();
    }

    @Test public void test_rt_37130_pageDownAtTop() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        StageLoader sl = new StageLoader(tableView);

        sm.select(5, col);
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);

        sl.dispose();
    }

    @Test public void test_rt_37130_pageDownAtBottom() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
        tableView.getColumns().setAll(col);

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        StageLoader sl = new StageLoader(tableView);

        sm.select(95, col);
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);

        sl.dispose();
    }

//    @Test public void test_rt_38326() {
//        int argCount = 4;
//        int testCount = (int) Math.pow(2, argCount);
//        for (int test = 0; test < testCount; test++) {
//            boolean moveUp                                      = (test & 0b1000) == 0b1000;
//            boolean singleSelection                             = (test & 0b0100) == 0b0100;
//            boolean cellSelection                               = (test & 0b0010) == 0b0010;
//            boolean updateItemsListBeforeSelectionModelChanges  = (test & 0b0001) == 0b0001;
//
//            StringBuilder sb = new StringBuilder("@Test public void test_rt_38326_focusLostOnShortcutKeyNav_");
//            sb.append(moveUp ? "moveUp_" : "moveDown_");
//            sb.append(singleSelection ? "singleSelection_" : "multipleSelection_");
//            sb.append(cellSelection ? "cellSelection_" : "rowSelection_");
//            sb.append(updateItemsListBeforeSelectionModelChanges ? "updateItemsListBeforeSelectionModelChanges" : "updateItemsListAfterSelectionModelChanges");
//            sb.append("() {\n    ");
//            sb.append("test_rt_38326(");
//            sb.append(moveUp + ", ");
//            sb.append(singleSelection + ", ");
//            sb.append(cellSelection + ", ");
//            sb.append(updateItemsListBeforeSelectionModelChanges);
//            sb.append(");\n}");
//
//            System.out.println(sb);
//        }
//    }

    // -- tests generated by above commented out code
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_multipleSelection_rowSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(false, false, false, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_multipleSelection_rowSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(false, false, false, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_multipleSelection_cellSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(false, false, true, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_multipleSelection_cellSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(false, false, true, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_singleSelection_rowSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(false, true, false, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_singleSelection_rowSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(false, true, false, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_singleSelection_cellSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(false, true, true, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveDown_singleSelection_cellSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(false, true, true, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_multipleSelection_rowSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(true, false, false, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_multipleSelection_rowSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(true, false, false, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_multipleSelection_cellSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(true, false, true, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_multipleSelection_cellSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(true, false, true, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_singleSelection_rowSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(true, true, false, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_singleSelection_rowSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(true, true, false, true);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_singleSelection_cellSelection_updateItemsListAfterSelectionModelChanges() {
        test_rt_38326(true, true, true, false);
    }
    @Test public void test_rt_38326_focusLostOnShortcutKeyNav_moveUp_singleSelection_cellSelection_updateItemsListBeforeSelectionModelChanges() {
        test_rt_38326(true, true, true, true);
    }

    private void test_rt_38326(boolean moveUp, boolean singleSelection, boolean cellSelection, boolean updateItemsListBeforeSelectionModelChanges) {
        final int items = 10;
        ObservableList<TreeItem<String>> itemsList = FXCollections.observableArrayList();
        for (int i = 0; i < items; i++) {
            itemsList.add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);

        if (updateItemsListBeforeSelectionModelChanges) {
            root.getChildren().clear();
            root.getChildren().addAll(itemsList);
        }

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

        TreeTableColumn<String, String> col2 = new TreeTableColumn<>("Column 2");
        col2.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

        tableView.getColumns().setAll(col, col2);

        TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(singleSelection ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(cellSelection);

        if (! updateItemsListBeforeSelectionModelChanges) {
            root.getChildren().clear();
            root.getChildren().addAll(itemsList);
        }

        StageLoader sl = new StageLoader(tableView);

        // test the initial state to ensure it is as we expect
        assertFalse(sm.isSelected(0));
        assertFalse(sm.isSelected(0, col));
        assertFalse(sm.isSelected(0, col2));
        assertEquals(0, sm.getSelectedIndices().size());
        assertEquals(0, sm.getSelectedItems().size());
        assertEquals(0, sm.getSelectedCells().size());

        final int startRow = 5;
        sm.clearSelection();
        sm.select(startRow, col);
        assertEquals(1, sm.getSelectedCells().size());
        assertEquals(startRow, sm.getSelectedCells().get(0).getRow());
        assertEquals(col, sm.getSelectedCells().get(0).getTableColumn());
        assertEquals(startRow, tableView.getFocusModel().getFocusedCell().getRow());
        assertEquals(col, tableView.getFocusModel().getFocusedCell().getTableColumn());

        keyboard.doKeyPress(moveUp ? KeyCode.UP : KeyCode.DOWN, KeyModifier.getShortcutKey());
        assertEquals(moveUp ? startRow-1 : startRow+1, tableView.getFocusModel().getFocusedCell().getRow());
        assertEquals(col, tableView.getFocusModel().getFocusedCell().getTableColumn());

        sl.dispose();
    }

    private int rt_39088_indices_event_count = 0;
    private int rt_39088_items_event_count = 0;
    @Test public void test_rt_39088() {
        ObservableList<TreeItem<String>> itemsList = FXCollections.observableArrayList();
        for (int i = 0; i < 4; i++) {
            itemsList.add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        root.getChildren().setAll(itemsList);

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

        tableView.getColumns().setAll(col);

        TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);

        ObservableList<Integer> indices = sm.getSelectedIndices();
        ObservableList<TreeItem<String>> items = sm.getSelectedItems();

        indices.addListener((ListChangeListener<Integer>) change -> rt_39088_indices_event_count++);
        items.addListener((ListChangeListener<TreeItem<String>>) change -> rt_39088_items_event_count++);

        StageLoader sl = new StageLoader(tableView);

        assertEquals(0, rt_39088_indices_event_count);
        assertEquals(0, rt_39088_items_event_count);
        assertEquals(0, indices.size());
        assertEquals(0, items.size());

        sm.select(3);
        assertEquals(1, rt_39088_indices_event_count);
        assertEquals(1, rt_39088_items_event_count);
        assertEquals(1, indices.size());
        assertEquals(1, items.size());

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(2, rt_39088_indices_event_count);
        assertEquals(2, rt_39088_items_event_count);
        assertEquals(2, indices.size());
        assertEquals(2, items.size());

        // this is where the test fails...
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(3, rt_39088_indices_event_count);
        assertEquals(3, rt_39088_items_event_count);
        assertEquals(3, indices.size());
        assertEquals(3, items.size());

        sl.dispose();
    }

    @Test public void test_rt_27709_singleSelection_cellSelection() {
        test_rt_27709(SelectionMode.SINGLE, true, false);
    }

    @Test public void test_rt_27709_multipleSelection_cellSelection() {
        test_rt_27709(SelectionMode.MULTIPLE, true, false);
    }

    @Test public void test_rt_27709_singleSelection_rowSelection() {
        test_rt_27709(SelectionMode.SINGLE, false, false);
    }

    @Test public void test_rt_27709_multipleSelection_rowSelection() {
        test_rt_27709(SelectionMode.MULTIPLE, false, false);
    }

    @Test public void test_rt_27709_singleSelection_cellSelection_resetSelection() {
        test_rt_27709(SelectionMode.SINGLE, true, true);
    }

    @Test public void test_rt_27709_multipleSelection_cellSelection_resetSelection() {
        test_rt_27709(SelectionMode.MULTIPLE, true, true);
    }

    @Test public void test_rt_27709_singleSelection_rowSelection_resetSelection() {
        test_rt_27709(SelectionMode.SINGLE, false, true);
    }

    @Test public void test_rt_27709_multipleSelection_rowSelection_resetSelection() {
        test_rt_27709(SelectionMode.MULTIPLE, false, true);
    }

    private void test_rt_27709(SelectionMode mode, boolean cellSelectionMode, boolean resetSelection) {
        root.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        tableView.setShowRoot(false);

        TreeTableColumn<String, String> col = new TreeTableColumn<>("Column");
        col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));

        tableView.getColumns().setAll(col);

        TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(mode);
        sm.setCellSelectionEnabled(cellSelectionMode);

        ObservableList<Integer> indices = sm.getSelectedIndices();
        ObservableList<TreeTablePosition<String,?>> cells = sm.getSelectedCells();

        StageLoader sl = new StageLoader(tableView);

        int expectedSize = mode == SelectionMode.SINGLE ? 1 : 10;
        int lookupIndex = mode == SelectionMode.SINGLE ? 0 : 9;

        sm.select(0, col);
        assertEquals(1, indices.size());
        assertEquals(1, cells.size());

        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertEquals(expectedSize, indices.size());
        assertEquals(expectedSize, cells.size());
        assertEquals(9, (int) indices.get(lookupIndex));
        assertEquals(9, cells.get(lookupIndex).getRow());

        if (resetSelection) {
            sm.clearAndSelect(9, col);
            TreeTablePosition<?,?> anchor = TreeTableCellBehavior.getAnchor(tableView, null);
            assertEquals(9, anchor.getRow());
            assertEquals(col, anchor.getTableColumn());
        } else {
            expectedSize = 1;
        }

        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertEquals(expectedSize, indices.size());
        assertEquals(expectedSize, cells.size());
        assertTrue(sm.isSelected(0, col));

        if (resetSelection) {
            sm.clearAndSelect(0, col);

            TreeTablePosition<?,?> anchor = TreeTableCellBehavior.getAnchor(tableView, null);
            assertEquals(0, anchor.getRow());
            assertEquals(col, anchor.getTableColumn());
        } else {
            expectedSize = mode == SelectionMode.SINGLE ? 1 : 10;
        }

        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertEquals(expectedSize, indices.size());
        assertEquals(expectedSize, cells.size());
        assertTrue(sm.isSelected(9, col));

        sl.dispose();
    }

    @Test public void test_rt_18440_goLeft() {
        test_rt_18440(KeyCode.LEFT, 3, false, colIndex -> {
            keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
            return colIndex - 1;
        });
    }

    @Test public void test_rt_18440_goLeft_toEnd() {
        test_rt_18440(KeyCode.LEFT, 3, true, colIndex -> {
            keyboard.doLeftArrowPress(KeyModifier.getShortcutKey());
            return colIndex - 1;
        });
    }

    @Test public void test_rt_18440_goRight() {
        test_rt_18440(KeyCode.RIGHT, 0, false, colIndex -> {
            keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
            return colIndex + 1;
        });
    }

    @Test public void test_rt_18440_goRight_toEnd() {
        test_rt_18440(KeyCode.RIGHT, 0, true, colIndex -> {
            keyboard.doRightArrowPress(KeyModifier.getShortcutKey());
            return colIndex + 1;
        });
    }

    private void test_rt_18440(KeyCode direction, int startColumn, boolean goToEnd, Function<Integer, Integer> r) {
        root.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        tableView.setShowRoot(false);
        tableView.setRoot(root);

        tableView.getColumns().clear();
        for (int i = 0; i < 4; i++) {
            TreeTableColumn<String, String> col = new TreeTableColumn<>("Column " + i);
            col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
            tableView.getColumns().add(col);
        }

        TreeTableView.TreeTableViewFocusModel fm = tableView.getFocusModel();
        TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        ObservableList<Integer> indices = sm.getSelectedIndices();
        ObservableList<TreeItem<String>> items = sm.getSelectedItems();

        StageLoader sl = new StageLoader(tableView);

        assertEquals(0, indices.size());
        assertEquals(0, items.size());

        sm.select(0, tableView.getColumns().get(startColumn));
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(tableView.getColumns().get(startColumn), sm.getSelectedCells().get(0).getTableColumn());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(tableView.getColumns().get(startColumn), fm.getFocusedCell().getTableColumn());

        int expectedColumn = r.apply(startColumn);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(tableView.getColumns().get(startColumn), sm.getSelectedCells().get(0).getTableColumn());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(tableView.getColumns().get(expectedColumn), fm.getFocusedCell().getTableColumn());

        expectedColumn = r.apply(expectedColumn);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(tableView.getColumns().get(startColumn), sm.getSelectedCells().get(0).getTableColumn());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(tableView.getColumns().get(expectedColumn), fm.getFocusedCell().getTableColumn());

        if (goToEnd) {
            expectedColumn = r.apply(expectedColumn);
            assertEquals(0, sm.getSelectedIndex());
            assertEquals(tableView.getColumns().get(startColumn), sm.getSelectedCells().get(0).getTableColumn());
            assertEquals(0, fm.getFocusedIndex());
            assertEquals(tableView.getColumns().get(expectedColumn), fm.getFocusedCell().getTableColumn());
        }

        expectedColumn = direction == KeyCode.RIGHT ? 3 : 0;
        keyboard.doKeyPress(direction, KeyModifier.SHIFT);
        assertEquals(0, sm.getSelectedIndex());
        assertEquals(debug(), 4, sm.getSelectedCells().size());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(tableView.getColumns().get(expectedColumn), fm.getFocusedCell().getTableColumn());

        sl.dispose();
    }

    @Test public void test_rt_24865_moveDownwards() {
        root.getChildren().clear();
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        tableView.setShowRoot(false);
        tableView.setRoot(root);

        Toolkit.getToolkit().firePulse();

        ObservableList<Integer> indices = sm.getSelectedIndices();

        sm.select(0);
        assertTrue(isSelected(0));
        assertTrue(fm.isFocused(0));
        assertEquals(1, indices.size());
        assertEquals(0, ((TreeTablePosition) TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(isSelected(0, 1, 2, 3));
        assertTrue(fm.isFocused(3));
        assertEquals(4, indices.size());
        assertEquals(0, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(isSelected(0, 1, 2, 3));
        assertTrue(isNotSelected(4, 5, 6, 7, 8, 9));
        assertTrue(fm.isFocused(6));
        assertEquals(4, indices.size());
        assertEquals(0, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        // main point of test: selection between the last index (3) and the focus
        // index (6) should now be true
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        final int selectedRowCount = indices.size();
        for (int i = 0; i < selectedRowCount; i++) {
            assertTrue(isSelected(i));
        }
        assertTrue(fm.isFocused(selectedRowCount - 1));
        assertEquals(0, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        int newSelectedRowCount = selectedRowCount + 1;
        for (int i = 0; i < newSelectedRowCount; i++) {
            assertTrue(isSelected(i));
        }
        assertTrue(fm.isFocused(newSelectedRowCount - 1));
        assertEquals(0, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());
    }

    @Test public void test_rt_24865_moveUpwards() {
        root.getChildren().clear();
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        tableView.setShowRoot(false);
        tableView.setRoot(root);

        Toolkit.getToolkit().firePulse();

        ObservableList<Integer> indices = sm.getSelectedIndices();

        sm.select(50);
        tableView.scrollTo(50);

        Toolkit.getToolkit().firePulse();

        assertTrue(isSelected(50));
        assertTrue(fm.isFocused(50));
        assertEquals(1, indices.size());
        assertEquals(50, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(isSelected(50, 49, 48, 47));
        assertTrue(fm.isFocused(47));
        assertEquals(4, indices.size());
        assertEquals(50, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        assertTrue(isSelected(50, 49, 48, 47));
        assertTrue(isNotSelected(46, 45, 44, 43, 42, 41));
        assertTrue(fm.isFocused(44));
        assertEquals(4, indices.size());
        assertEquals(50, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        // main point of test: selection between the last index (47) and the focus
        // index (44) should now be true
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        final int selectedRowCount = indices.size();
        for (int i = 0; i < selectedRowCount; i++) {
            assertTrue(isSelected(50 - i));
        }
        assertTrue(fm.isFocused(50 - selectedRowCount + 1));
        assertEquals(50, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());

        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        int newSelectedRowCount = selectedRowCount + 1;
        for (int i = 0; i < newSelectedRowCount; i++) {
            assertTrue(isSelected(50 - i));
        }
        assertTrue(fm.isFocused(50 - newSelectedRowCount + 1));
        assertEquals(50, ((TreeTablePosition)TreeTableCellBehavior.getAnchor(tableView, null)).getRow());
    }

    @Test public void test_rt_39792_goLeft_goPastEnd() {
        test_rt_39792(3, colIndex -> {
            keyboard.doLeftArrowPress(KeyModifier.SHIFT);
            return colIndex - 1;
        });
    }

    @Test public void test_rt_39792_goRight_goPastEnd() {
        test_rt_39792(0, colIndex -> {
            keyboard.doRightArrowPress(KeyModifier.SHIFT);
            return colIndex + 1;
        });
    }

    private void test_rt_39792(int startColumn, Function<Integer, Integer> r) {
        root.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        tableView.setShowRoot(false);
        tableView.setRoot(root);

        tableView.getColumns().clear();
        for (int i = 0; i < 4; i++) {
            TreeTableColumn<String, String> col = new TreeTableColumn<>("Column " + i);
            col.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue()));
            tableView.getColumns().add(col);
        }

        TreeTableView.TreeTableViewSelectionModel<String> sm = tableView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        ObservableList<Integer> indices = sm.getSelectedIndices();
        ObservableList<TreeItem<String>> items = sm.getSelectedItems();

        StageLoader sl = new StageLoader(tableView);

        assertEquals(0, indices.size());
        assertEquals(0, items.size());

        sm.select(0, tableView.getColumns().get(startColumn));
        assertEquals(1, sm.getSelectedCells().size());

        int expectedColumn = r.apply(startColumn);
        assertEquals(2, sm.getSelectedCells().size());

        expectedColumn = r.apply(expectedColumn);
        assertEquals(3, sm.getSelectedCells().size());

        expectedColumn = r.apply(expectedColumn);
        assertEquals(4, sm.getSelectedCells().size());

        // this should not cause any issue, but it does - as noted in RT-39792
        /*expectedColumn = */r.apply(expectedColumn);
        assertEquals(4, sm.getSelectedCells().size());

        sl.dispose();
    }
}
