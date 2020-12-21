/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import com.sun.javafx.scene.control.behavior.TreeCellBehavior;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import java.util.List;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.util.Utils;
import test.com.sun.javafx.scene.control.behavior.TreeViewAnchorRetriever;
import test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.control.FocusModel;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeView;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TreeViewKeyInputTest {
    private TreeView<String> treeView;
    private MultipleSelectionModel<TreeItem<String>> sm;
    private FocusModel<TreeItem<String>> fm;

    private KeyEventFirer keyboard;
    private StageLoader stageLoader;

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

        // recreate treeview and gather models
        treeView = new TreeView<String>();
        treeView.setRoot(root);
        sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        fm = treeView.getFocusModel();

        // set up keyboard event firer
        keyboard = new KeyEventFirer(treeView);

        // create a simple UI that will be shown (to send the keyboard events to)
        stageLoader = new StageLoader(treeView);
        stageLoader.getStage().show();
    }

    @After public void tearDown() {
        treeView.getSkin().dispose();
        stageLoader.dispose();
    }


    /***************************************************************************
     * Util methods
     **************************************************************************/

    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Indices: [");

        List<Integer> indices = sm.getSelectedIndices();
        for (Integer index : indices) {
            sb.append(index);
            sb.append(", ");
        }

        sb.append("] \nFocus: " + fm.getFocusedIndex());
        sb.append(" \nAnchor: " + getAnchor());
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

    private int getAnchor() {
        return TreeViewAnchorRetriever.getAnchor(treeView);
    }

    private boolean isAnchor(int index) {
        return getAnchor() == index;
    }

    private int getItemCount() {
        return root.getChildren().size() + child3.getChildren().size();
    }


    /***************************************************************************
     * General tests
     **************************************************************************/

    @Test public void testInitialState() {
        assertEquals(-1, sm.getSelectedIndex());
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
        int endIndex = getItemCount();
        sm.clearAndSelect(endIndex);
        assertTrue(debug(), sm.isSelected(endIndex));
        keyboard.doDownArrowPress();
        assertTrue(sm.isSelected(endIndex));
    }

    @Test public void testUpArrowDoesNotChangeSelectionWhenAt0Index() {
        sm.clearAndSelect(0);
        keyboard.doUpArrowPress();
        assertTrue(sm.isSelected(0));
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
        sm.clearAndSelect(0);
        keyboard.doLeftArrowPress();
        assertTrue(sm.isSelected(0));
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, sm.getSelectedItems().size());
    }

    // test 19
    @Test public void testCtrlDownMovesFocusButLeavesSelectionAlone() {
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
    }

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

    // test 23
    @Test public void testCtrlUpMovesFocus() {
        sm.clearAndSelect(1);
        assertTrue(fm.isFocused(1));
        assertTrue(sm.isSelected(1));
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(1));
    }

    // test 24
    @Test public void testCtrlDownDoesNotMoveFocusWhenAtLastIndex() {
        int endIndex = getItemCount();
        sm.clearAndSelect(endIndex);
        assertTrue(fm.isFocused(endIndex));
        assertTrue(sm.isSelected(endIndex));
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(fm.isFocused(endIndex));
        assertTrue(sm.isSelected(endIndex));
    }

    // test 25
    @Test public void testCtrlDownArrowWithSpaceChangesAnchor() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0
        assertTrue(isSelected(0, 2));
        assertTrue(isNotSelected(1));
        assertTrue(isAnchor(0));
    }

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
        assertTrue(debug(), isSelected(getItemCount()));
        assertTrue(isNotSelected(1,2,3));
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isNotSelected(5));
        assertTrue(debug(), fm.isFocused(5));
        assertTrue(isAnchor(5));
    }



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
        int endIndex = getItemCount() - 1;
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
        assertTrue(debug(), sm.isSelected(0));
        assertTrue(sm.isSelected(1));
        assertFalse(sm.isSelected(2));
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
        sm.clearAndSelect(2);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertFalse(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
        assertTrue(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
    }

    // test 18 from Jindra's testcases.rtf file
    @Test public void testShiftDownTwiceThenShiftUpWhenAtLastIndex() {
        int endIndex = getItemCount();
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2

        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 0

        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 2
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2

        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());    // move focus to 4
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));  // select 2

        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 1
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());    // move focus to 0
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        assertTrue(isSelected(getItemCount()));
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
        assertTrue(isAnchor(3));
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
     * Tests for discontinuous multiple selection (RT-18952)
     **************************************************************************/

    // Test 1
    @Test public void test_rt18952_1() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
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
        keyboard.doKeyPress(KeyCode.SPACE,KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null));
        assertTrue(isSelected(1,2,3,4));
        assertTrue(fm.isFocused(0));
        assertTrue(isAnchor(0));
    }



    /***************************************************************************
     * Tests for editing
     **************************************************************************/

    // test 43 (part 1)
    @Test public void testF2EntersEditModeAndEscapeCancelsEdit_part1() {
        treeView.setEditable(true);

        sm.clearAndSelect(0);
        assertNull(treeView.getEditingItem());
        keyboard.doKeyPress(KeyCode.F2);
        assertEquals(root, treeView.getEditingItem());

        keyboard.doKeyPress(KeyCode.ESCAPE);
        assertNull(treeView.getEditingItem());
    }

//    // test 43 (part 2)
//    @Test public void testF2EntersEditModeAndEscapeCancelsEdit_part2() {
//        treeView.setEditable(true);
//
//        sm.clearAndSelect(0);
//        keyboard.doKeyPress(KeyCode.F2);
//
//
//    }


    /***************************************************************************
     * Tests for TreeView-specific functionality
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
        assertTrue(isSelected(0,1,2,3,4,5,6,7,8,9));
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

    @Test public void test_rt18642() {
        sm.clearAndSelect(1);                          // select 1
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 2
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 3
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set anchor, and also select, 3
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 4
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());   // shift focus to 5
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set anchor, and also select, 5

        assertTrue(isSelected(1, 3, 5));
        assertTrue(isNotSelected(0, 2, 4));

        // anchor is at 5, so shift+UP should select rows 4 and 5 only
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(isSelected(4, 5));
        assertTrue(isNotSelected(0, 1, 2, 3));
    }

    @Test public void test_rt14451_1() {
        sm.clearAndSelect(5);

        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertTrue(isSelected(0,1,2,3,4,5));
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

        treeView.getSelectionModel().select(0);
        assertEquals(0, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(root, treeView.getSelectionModel().getSelectedItem());
        assertEquals(0, treeView.getFocusModel().getFocusedIndex());
        assertEquals(root, treeView.getFocusModel().getFocusedItem());

        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
        assertEquals(0, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(root, treeView.getSelectionModel().getSelectedItem());
        assertEquals(0, treeView.getFocusModel().getFocusedIndex());
        assertEquals(root, treeView.getFocusModel().getFocusedItem());
    }

    @Test public void test_rt29930() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0);

        keyboard.doDownArrowPress(KeyModifier.SHIFT); // select rows [0,1]
        keyboard.doDownArrowPress(KeyModifier.SHIFT); // select rows [0,1,2]
        assertTrue(isSelected(0,1,2));
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(0, getAnchor());

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(),
                (Utils.isMac()  ? KeyModifier.CTRL : null)); // set new anchor point
        assertTrue(isSelected(0,1));
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(2, getAnchor());

        keyboard.doDownArrowPress(KeyModifier.SHIFT); // select rows [2,3]
        assertTrue(isSelected(2,3));
        assertTrue(isNotSelected(0,1));
        assertEquals(3, fm.getFocusedIndex());
        assertEquals(2, getAnchor());
    }

    private int rt29849_start_count = 0;
    private int rt29849_cancel_count = 0;
    @Test public void test_rt29849() {
        treeView.setEditable(true);

        treeView.setOnEditStart(event -> {
            rt29849_start_count++;
        });
        treeView.setOnEditCancel(event -> {
            rt29849_cancel_count++;
        });

        // initially the counts should be zero
        assertEquals(0, rt29849_start_count);
        assertEquals(0, rt29849_cancel_count);

        IndexedCell cell = VirtualFlowTestUtils.getCell(treeView, 0);
        assertTrue(cell.isEditable());
        assertFalse(cell.isEditing());
        assertEquals(0, cell.getIndex());

        // do an edit, start count should be one, cancel still zero
        treeView.edit(root);
        assertTrue(cell.isEditing());
        assertEquals(1, rt29849_start_count);
        assertEquals(0, rt29849_cancel_count);

        // cancel edit, now both counts should be 1
//        keyboard.doKeyPress(KeyCode.ESCAPE);
        treeView.edit(null);
        assertFalse(cell.isEditing());
        assertEquals(1, rt29849_start_count);
        assertEquals(1, rt29849_cancel_count);
    }

    private int rt31577_count = 0;
    @Test public void test_rt31577() {
        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(lastIndex);

        // need to make sure we scroll down to the bottom!
        treeView.scrollTo(lastIndex);
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

    @Test public void test_rt19053_pageUp() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final int middleIndex = items / 2;

        treeView.setShowRoot(false);
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(middleIndex);

        assertEquals(middleIndex, sm.getSelectedIndex());

        final Object initialSelectionOwner = sm.getSelectedItem();

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        final Object newSelectionOwner = sm.getSelectedItem();
        assertNotSame(initialSelectionOwner, newSelectionOwner);

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

        treeView.setShowRoot(false);
        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

    private int rt32783_count_start = 0;
    private int rt32783_count_commit = 0;
    private int rt32783_count_cancel = 0;
    private int rt32783_count = 0;
    @Test public void test_rt32683() {
        // set up test
        final int items = 8;
        TreeItem<String> newRoot = new TreeItem<>("New root");
        newRoot.setExpanded(false);
        for (int i = 0; i < items; i++) {
            newRoot.getChildren().add(new TreeItem<>("Row " + i));
        }

        treeView.setRoot(newRoot);
        treeView.setEditable(true);
        treeView.setOnEditStart(t -> {
            rt32783_count_start++;
        });

        treeView.setOnEditCommit(t -> {
            rt32783_count_commit++;
        });

        treeView.setOnEditCancel(t -> {
            rt32783_count_cancel++;
        });

        treeView.editingItemProperty().addListener(observable -> {
            assertNotNull(treeView.getEditingItem());
            //System.out.println("editing item: " + treeView.getEditingItem());
            rt32783_count++;
        });

        // start test
//        final int middleIndex = items / 2;

        newRoot.setExpanded(true);
        Toolkit.getToolkit().firePulse();
        newRoot.setExpanded(false);
        Toolkit.getToolkit().firePulse();

        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.clearAndSelect(0);

        // need to get the cell before the editing starts
//        TreeCell cell = (TreeCell)VirtualFlowTestUtils.getCell(treeView, 0);

        // this forces the selected cell to go into editing mode
        keyboard.doKeyPress(KeyCode.F2);
//        Toolkit.getToolkit().firePulse();

        assertEquals(1, rt32783_count_start);
        assertEquals(0, rt32783_count_commit);
        assertEquals(0, rt32783_count_cancel);
//        assertTrue(cell.isEditing());
        assertEquals(newRoot, treeView.getEditingItem());
    }

    @Test public void test_rt21375_scenario_1a_down() {
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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
        final int items = 8;
        root.getChildren().clear();
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
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

        final FocusModel fm = treeView.getFocusModel();
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
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

        final FocusModel fm = treeView.getFocusModel();
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);
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

        final FocusModel fm = treeView.getFocusModel();
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
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

        final FocusModel fm = treeView.getFocusModel();
        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
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

        final MultipleSelectionModel sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.SINGLE);
        sm.clearAndSelect(0);

        treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener) c -> {
            while (c.next()) {
                rt_33559_count++;
            }
        });

        assertEquals(0, rt_33559_count);
        keyboard.doKeyPress(KeyCode.RIGHT); // expand root
        assertEquals(0, rt_33559_count);
    }

    @Test public void test_rt20915() {
        final FocusModel fm = treeView.getFocusModel();
        final MultipleSelectionModel sm = treeView.getSelectionModel();

        sm.clearAndSelect(0);
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(0, getAnchor());

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
        assertTrue(debug(), isSelected(0,1,2,3));
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
        treeView.scrollTo(99);
        assertEquals(99, getAnchor());
        assertEquals(99, fm.getFocusedIndex());

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(99, getAnchor());
        assertTrue(fm.getFocusedIndex() < 99);
    }

    @Test public void test_rt4369() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.clearAndSelect(99);
        treeView.scrollTo(99);
        assertEquals(99, getAnchor());
        assertEquals(99, fm.getFocusedIndex());

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(99, getAnchor());
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
        assertEquals(1, getAnchor());
        assertEquals(1, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(1, getAnchor());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(2, getAnchor());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(2, sm.getSelectedIndex());
        assertTrue(isSelected(1, 2));

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(2, getAnchor());
        assertEquals(0, fm.getFocusedIndex());
        assertEquals(2, sm.getSelectedIndex());
        assertTrue(isSelected(1, 2));

        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        Toolkit.getToolkit().firePulse();
        assertEquals(0, getAnchor());
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
        assertEquals(1, getAnchor());
        assertEquals(1, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.DOWN, KeyModifier.getShortcutKey());
        Toolkit.getToolkit().firePulse();
        assertEquals(1, getAnchor());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(1, sm.getSelectedIndex());

        keyboard.doKeyPress(KeyCode.SPACE);
        Toolkit.getToolkit().firePulse();
        assertEquals(2, getAnchor());
        assertEquals(2, fm.getFocusedIndex());
        assertEquals(2, sm.getSelectedIndex());
        assertTrue(isSelected(1, 2));
    }

    @Test public void test_rt34407_down_down_up() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }
        treeView.setPrefHeight(130); // roughly room for four rows

        StageLoader sl = new StageLoader(treeView);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(0);
        fm.focus(0);
        assertEquals(0, getAnchor());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));

        // we expect the final Page-up to return us back to this selected index and with the same number of selected indices
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        final int leadSelectedIndex = sm.getSelectedIndex();
        final int selectedIndicesCount = sm.getSelectedIndices().size();
        assertEquals(6, leadSelectedIndex);
        assertEquals(6, fm.getFocusedIndex());
        assertEquals(7, selectedIndicesCount);

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
        treeView.setPrefHeight(120); // roughly room for four rows

        StageLoader sl = new StageLoader(treeView);
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(99);
        fm.focus(99);
        treeView.scrollTo(99);
        Toolkit.getToolkit().firePulse();

        assertEquals(99, getAnchor());
        assertTrue(fm.isFocused(99));
        assertTrue(sm.isSelected(99));
        assertFalse(sm.isSelected(98));

        // we expect the final Page-down to return us back to this selected index and with the same number of selected indices
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        final int leadSelectedIndex = sm.getSelectedIndex();
        final int selectedIndicesCount = sm.getSelectedIndices().size();
        final int diff = 4;//99 - leadSelectedIndex;
        assertEquals(99 - diff, leadSelectedIndex);
        assertEquals(99 - diff, fm.getFocusedIndex());
        assertEquals(5, selectedIndicesCount);

        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.SHIFT);
        assertEquals(99 - diff * 2 - 1, sm.getSelectedIndex());
        assertEquals(selectedIndicesCount * 2, sm.getSelectedIndices().size());

        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        assertEquals(leadSelectedIndex, sm.getSelectedIndex());
        assertEquals(selectedIndicesCount, sm.getSelectedIndices().size());

        sl.dispose();
    }

    @Test public void test_rt34768() {
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TreeTableColumn<String, String> firstNameCol = new TreeTableColumn<>("First Name");
        treeView.setRoot(null);

        // no need for an assert here - we're testing for an AIOOBE
        keyboard.doKeyPress(KeyCode.A, KeyModifier.getShortcutKey());
    }

    @Test public void test_rt35853_multipleSelection_shiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(5, getAnchor());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertTrue(sm.isSelected(5));
    }

    @Test public void test_rt35853_multipleSelection_noShiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP);
        assertEquals(4, getAnchor());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt35853_singleSelection_shiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.SINGLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT);
        assertEquals(4, getAnchor());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt35853_singleSelection_noShiftDown() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.SINGLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        sm.selectedIndexProperty().addListener(observable -> {
            // we expect only one selected index change event, from 5 to 4
            assertEquals(4, sm.getSelectedIndex());
        });

        keyboard.doKeyPress(KeyCode.UP);
        assertEquals(4, getAnchor());
        assertTrue(fm.isFocused(4));
        assertTrue(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt36800() {
        final int items = 10;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.SINGLE);

        sm.clearAndSelect(5);
        assertEquals(5, getAnchor());
        assertTrue(fm.isFocused(5));
        assertTrue(sm.isSelected(5));

        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 4
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 3
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 2
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 1
        keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // 0

        ControlTestUtils.runWithExceptionHandler(() -> {
            keyboard.doKeyPress(KeyCode.UP, KeyModifier.SHIFT); // bug time?
        });

        assertEquals(0, getAnchor());
        assertTrue(fm.isFocused(0));
        assertTrue(sm.isSelected(0));
        assertFalse(sm.isSelected(1));
        assertFalse(sm.isSelected(2));
        assertFalse(sm.isSelected(3));
        assertFalse(sm.isSelected(4));
        assertFalse(sm.isSelected(5));
    }

    @Test public void test_rt_37130_pageUpAtTop() {
        final int items = 100;
        root.getChildren().clear();
        root.setExpanded(true);
        for (int i = 0; i < items; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        StageLoader sl = new StageLoader(treeView);

        sm.select(5);
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

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        StageLoader sl = new StageLoader(treeView);

        sm.select(95);
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

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        StageLoader sl = new StageLoader(treeView);

        sm.select(5);
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

        sm.setSelectionMode(SelectionMode.MULTIPLE);

        StageLoader sl = new StageLoader(treeView);

        sm.select(95);
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.SHIFT);

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

        MultipleSelectionModel<TreeItem<String>> sm = treeView.getSelectionModel();
        sm.setSelectionMode(SelectionMode.MULTIPLE);

        ObservableList<Integer> indices = sm.getSelectedIndices();
        ObservableList<TreeItem<String>> items = sm.getSelectedItems();

        indices.addListener((ListChangeListener<Integer>) change -> rt_39088_indices_event_count++);
        items.addListener((ListChangeListener<TreeItem<String>>) change -> rt_39088_items_event_count++);

        StageLoader sl = new StageLoader(treeView);

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

    @Test public void test_rt_27709_singleSelection_rowSelection() {
        test_rt_27709(SelectionMode.SINGLE, false);
    }

    @Test public void test_rt_27709_multipleSelection_rowSelection() {
        test_rt_27709(SelectionMode.MULTIPLE, false);
    }

    @Test public void test_rt_27709_singleSelection_rowSelection_resetSelection() {
        test_rt_27709(SelectionMode.SINGLE, true);
    }

    @Test public void test_rt_27709_multipleSelection_rowSelection_resetSelection() {
        test_rt_27709(SelectionMode.MULTIPLE, true);
    }

    private void test_rt_27709(SelectionMode mode, boolean resetSelection) {
        root.getChildren().clear();
        for (int i = 0; i < 10; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        treeView.setShowRoot(false);

        MultipleSelectionModel<TreeItem<String>> sm = treeView.getSelectionModel();
        sm.setSelectionMode(mode);

        ObservableList<Integer> indices = sm.getSelectedIndices();

        int expectedSize = mode == SelectionMode.SINGLE ? 1 : 10;
        int lookupIndex = mode == SelectionMode.SINGLE ? 0 : 9;

        sm.select(0);
        assertEquals(1, indices.size());

        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertEquals(debug(), expectedSize, indices.size());
        assertEquals(9, (int) indices.get(lookupIndex));

        if (resetSelection) {
            sm.clearAndSelect(9);
            int anchor = TreeCellBehavior.getAnchor(treeView, null);
            assertEquals(9, anchor);
        } else {
            expectedSize = 1;
        }

        keyboard.doKeyPress(KeyCode.HOME, KeyModifier.SHIFT);
        assertEquals(expectedSize, indices.size());
        assertTrue(debug(),sm.isSelected(0));

        if (resetSelection) {
            sm.clearAndSelect(0);

            int anchor = TreeCellBehavior.getAnchor(treeView, null);
            assertEquals(0, anchor);
        } else {
            expectedSize = mode == SelectionMode.SINGLE ? 1 : 10;
        }

        keyboard.doKeyPress(KeyCode.END, KeyModifier.SHIFT);
        assertEquals(expectedSize, indices.size());
        assertTrue(sm.isSelected(9));
    }

    @Test public void test_rt_24865_moveDownwards() {
        root.getChildren().clear();
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        treeView.setShowRoot(false);

        Toolkit.getToolkit().firePulse();

        ObservableList<Integer> indices = sm.getSelectedIndices();

        sm.select(0);
        assertTrue(isSelected(0));
        assertTrue(fm.isFocused(0));
        assertEquals(1, indices.size());
        assertEquals(0, (int) TreeCellBehavior.getAnchor(treeView, -1));

        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(isSelected(0, 1, 2, 3));
        assertTrue(fm.isFocused(3));
        assertEquals(4, indices.size());
        assertEquals(0, (int) TreeCellBehavior.getAnchor(treeView, -1));

        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        keyboard.doDownArrowPress(KeyModifier.getShortcutKey());
        assertTrue(isSelected(0, 1, 2, 3));
        assertTrue(isNotSelected(4, 5, 6, 7, 8, 9));
        assertTrue(fm.isFocused(6));
        assertEquals(4, indices.size());
        assertEquals(0, (int) TreeCellBehavior.getAnchor(treeView, -1));

        // main point of test: selection between the last index (3) and the focus
        // index (6) should now be true
        keyboard.doKeyPress(KeyCode.PAGE_DOWN, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        final int selectedRowCount = indices.size();
        for (int i = 0; i < selectedRowCount; i++) {
            assertTrue(isSelected(i));
        }
        assertTrue(fm.isFocused(selectedRowCount - 1));
        assertEquals(0, (int) TreeCellBehavior.getAnchor(treeView, -1));

        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        int newSelectedRowCount = selectedRowCount + 1;
        for (int i = 0; i < newSelectedRowCount; i++) {
            assertTrue(isSelected(i));
        }
        assertTrue(fm.isFocused(newSelectedRowCount - 1));
        assertEquals(0, (int) TreeCellBehavior.getAnchor(treeView, -1));
    }

    @Test public void test_rt_24865_moveUpwards() {
        root.getChildren().clear();
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        treeView.setShowRoot(false);

        Toolkit.getToolkit().firePulse();

        ObservableList<Integer> indices = sm.getSelectedIndices();

        sm.select(50);
        treeView.scrollTo(50);

        Toolkit.getToolkit().firePulse();

        assertTrue(isSelected(50));
        assertTrue(fm.isFocused(50));
        assertEquals(1, indices.size());
        assertEquals(50, (int) TreeCellBehavior.getAnchor(treeView, -1));

        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(isSelected(50, 49, 48, 47));
        assertTrue(fm.isFocused(47));
        assertEquals(4, indices.size());
        assertEquals(50, (int) TreeCellBehavior.getAnchor(treeView, -1));

        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        keyboard.doUpArrowPress(KeyModifier.getShortcutKey());
        assertTrue(isSelected(50, 49, 48, 47));
        assertTrue(isNotSelected(46, 45, 44, 43, 42, 41));
        assertTrue(fm.isFocused(44));
        assertEquals(4, indices.size());
        assertEquals(50, (int) TreeCellBehavior.getAnchor(treeView, -1));

        // main point of test: selection between the last index (47) and the focus
        // index (44) should now be true
        keyboard.doKeyPress(KeyCode.PAGE_UP, KeyModifier.getShortcutKey(), KeyModifier.SHIFT);
        final int selectedRowCount = indices.size();
        for (int i = 0; i < selectedRowCount; i++) {
            assertTrue(isSelected(50 - i));
        }
        assertTrue(fm.isFocused(50 - selectedRowCount + 1));
        assertEquals(50, (int) TreeCellBehavior.getAnchor(treeView, -1));

        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        int newSelectedRowCount = selectedRowCount + 1;
        for (int i = 0; i < newSelectedRowCount; i++) {
            assertTrue(isSelected(50 - i));
        }
        assertTrue(fm.isFocused(50 - newSelectedRowCount + 1));
        assertEquals(50, (int) TreeCellBehavior.getAnchor(treeView, -1));
    }

    @Test public void test_jdk_8160858() {
        root.getChildren().clear();
        for (int i = 0; i < 100; i++) {
            root.getChildren().add(new TreeItem<>("Row " + i));
        }

        root.setExpanded(true);
        treeView.setShowRoot(false);

        // create a button to move focus over to
        Button btn = new Button("Button");
        ((Group)treeView.getScene().getRoot()).getChildren().add(btn);

        treeView.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertEquals(stageLoader.getStage().getScene().getFocusOwner(), treeView);

        // we expect initially that selection is on -1, and focus is on 0
        assertEquals(-1, sm.getSelectedIndex());
        assertEquals(0, fm.getFocusedIndex());

        keyboard.doDownArrowPress();
        assertEquals(1, sm.getSelectedIndex());
        assertEquals(1, fm.getFocusedIndex());

        btn.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertEquals(stageLoader.getStage().getScene().getFocusOwner(), btn);

        treeView.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertEquals(stageLoader.getStage().getScene().getFocusOwner(), treeView);

        assertEquals(1, sm.getSelectedIndex());
        assertEquals(1, fm.getFocusedIndex());
    }
}
