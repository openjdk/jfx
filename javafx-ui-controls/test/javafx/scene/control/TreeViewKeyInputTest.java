/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import com.sun.javafx.scene.control.behavior.ListViewAnchorRetriever;
import com.sun.javafx.scene.control.behavior.TreeViewAnchorRetriever;
import static org.junit.Assert.*;

import javafx.scene.control.KeyEventFirer;
import javafx.scene.control.KeyModifier;

import java.util.Arrays;
import java.util.List;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Scene;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.After;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Not complete yet")
public class TreeViewKeyInputTest {
    private TreeView<String> treeView;
    private MultipleSelectionModel<TreeItem<String>> sm;
    private FocusModel<TreeItem<String>> fm;
    
    private KeyEventFirer keyboard;
    
    private Stage stage;
    private Scene scene;
    private Group group;
    
    private final TreeItem<String> root = new TreeItem<String>("Root");
    private final TreeItem<String> child1 = new TreeItem<String>("Child 1");
    private final TreeItem<String> child2 = new TreeItem<String>("Child 2");
    private final TreeItem<String> child3 = new TreeItem<String>("Child 3");
    private final TreeItem<String> child4 = new TreeItem<String>("Child 4");
    private final TreeItem<String> child5 = new TreeItem<String>("Child 5");
    private final TreeItem<String> child6 = new TreeItem<String>("Child 6");
    private final TreeItem<String> child7 = new TreeItem<String>("Child 7");
    private final TreeItem<String> child8 = new TreeItem<String>("Child 8");
    private final TreeItem<String> child9 = new TreeItem<String>("Child 9");
    private final TreeItem<String> child10 = new TreeItem<String>("Child 10");

    public TreeViewKeyInputTest() {
        root.getChildren().setAll(child1, child2, child3, child4, child5, child6, 
                                    child7, child8, child9, child10 );
    }
    
    @Before public void setup() {
        treeView = new TreeView<String>();
        sm = treeView.getSelectionModel();
        fm = treeView.getFocusModel();
        
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        keyboard = new KeyEventFirer(treeView);
        
        group = new Group();
        scene = new Scene(group);
        
        stage = new Stage();
        stage.setScene(scene);
        
        group.getChildren().setAll(treeView);
        stage.show();

        treeView.setRoot(root);
        sm.clearAndSelect(0);
    }
    
    @After public void tearDown() {
        stage.hide();
    }
    
    
    /***************************************************************************
     * Util methods
     **************************************************************************/
    
    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Indices: ");
        
        List<Integer> indices = sm.getSelectedIndices();
        for (Integer index : indices) {
            sb.append(index);
            sb.append(", ");
        }
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
    
    
    /***************************************************************************
     * General tests
     **************************************************************************/    
    
    @Test public void testInitialState() {
        assertTrue(sm.isSelected(0));
        assertEquals(1, sm.getSelectedIndices().size());
        assertEquals(1, sm.getSelectedItems().size());
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
    
    @Test public void testRightArrowDoesNotChangeState() {
        keyboard.doRightArrowPress();
        testInitialState();
    }
    
    @Test public void testShiftDownArrowIncreasesSelection() {
        sm.clearAndSelect(0);
        keyboard.doDownArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
    }
    
    @Test public void testShiftUpArrowIncreasesSelection() {
        sm.clearAndSelect(1);
        keyboard.doUpArrowPress(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0));
        assertTrue(sm.isSelected(1));
    }
    
    /***************************************************************************
     * Tests for row-based multiple selection
     **************************************************************************/
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
    @Test public void test_rt18642() {
        sm.clearAndSelect(1);                          // select 1
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // shift focus to 2
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // shift focus to 3
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.CTRL); // set anchor, and also select, 3
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // shift focus to 4
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // shift focus to 5
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.CTRL); // set anchor, and also select, 5
        
        assertTrue(isSelected(1, 3, 5));
        assertTrue(isNotSelected(0, 2, 4));
        
        // anchor is at 5, so shift+UP should select rows 4 and 5 only
        keyboard.doUpArrowPress(KeyModifier.SHIFT);   
        assertTrue(isSelected(4, 5));
        assertTrue(isNotSelected(0, 1, 2, 3));
    }
}
