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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.scene.control.behavior.TreeViewAnchorRetriever;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.KeyModifier;
import com.sun.javafx.scene.control.infrastructure.StageLoader;
import com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import com.sun.javafx.scene.control.skin.TreeViewSkin;

//@Ignore("Disabling tests as they fail with OOM in continuous builds")
public class TreeViewMouseInputTest {
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
        sm.clearAndSelect(0);
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
     * Tests for specific bug reports
     **************************************************************************/
    
    @Ignore("Bug is not yet fixed")
    @Test public void test_rt29833_mouse_select_upwards() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(9);
        
        // select all from 9 - 7
        VirtualFlowTestUtils.clickOnRow(treeView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(7,8,9));
        
        // select all from 9 - 7 - 5
        VirtualFlowTestUtils.clickOnRow(treeView, 5, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }
    
    @Ignore("Bug is not yet fixed")
    @Test public void test_rt29833_mouse_select_downwards() {
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        sm.clearAndSelect(5);
        
        // select all from 5 - 7
        VirtualFlowTestUtils.clickOnRow(treeView, 7, KeyModifier.SHIFT);
        assertTrue(debug(), isSelected(5,6,7));
        
        // select all from 5 - 7 - 9
        VirtualFlowTestUtils.clickOnRow(treeView, 9, KeyModifier.SHIFT);
        assertTrue(debug(),isSelected(5,6,7,8,9));
    }
}
