/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import com.sun.javafx.scene.control.behavior.ListViewAnchorRetriever;
import com.sun.javafx.scene.control.behavior.TableViewAnchorRetriever;
import static org.junit.Assert.*;

import java.util.List;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import javafx.stage.Stage;
import org.junit.After;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TableViewKeyInputTest {
    private TableView<String> tableView;
    private TableView.TableViewSelectionModel<String> sm;
    private TableView.TableViewFocusModel<String> fm;
    
    private KeyEventFirer keyboard;
    
    private Stage stage;
    private Scene scene;
    private Group group;
    
    private final TableColumn<String, String> col1 = new TableColumn<String, String>();
    private final TableColumn<String, String> col2 = new TableColumn<String, String>();
    private final TableColumn<String, String> col3 = new TableColumn<String, String>();
    private final TableColumn<String, String> col4 = new TableColumn<String, String>();
    private final TableColumn<String, String> col5 = new TableColumn<String, String>();
    
    @Before public void setup() {
        tableView = new TableView<String>();
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();
        
        sm.setSelectionMode(SelectionMode.MULTIPLE);
        
        keyboard = new KeyEventFirer(tableView);
        
        group = new Group();
        scene = new Scene(group);
        
        stage = new Stage();
        stage.setScene(scene);
        
        group.getChildren().setAll(tableView);
        stage.show();

        tableView.getItems().setAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        tableView.getColumns().setAll(col1, col2, col3, col4, col5);
        
        sm.clearAndSelect(0);
    }
    
    @After public void tearDown() {
        stage.hide();
    }
    
    /***************************************************************************
     * Util methods
     **************************************************************************/
    
    private String debug() {
        StringBuilder sb = new StringBuilder("Selected Cells: ");
        
        List<TablePosition> cells = sm.getSelectedCells();
        for (TablePosition tp : cells) {
            sb.append("(");
            sb.append(tp.getRow());
            sb.append(",");
            sb.append(tp.getColumn());
            sb.append("), ");
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
    
    private TablePosition getAnchor() {
        return TableViewAnchorRetriever.getAnchor(tableView);
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
     * Tests for cell-based multiple selection
     **************************************************************************/    
    
    @Ignore("This bug still exists")
    @Test public void testSelectionPathDeviationWorks1() {
        // select horizontally, then select two items vertically, then go back
        // in opposite direction
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col1);
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doDownArrowPress(KeyModifier.SHIFT);    // select (2, col3)
        keyboard.doDownArrowPress(KeyModifier.SHIFT);    // select (3, col3)
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(2, col3));
        assertTrue(sm.isSelected(3, col3));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);    // deselect (3, col3)
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(2, col3));
        assertFalse(sm.isSelected(3, col3));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);    // deselect (2, col3)
        assertTrue(sm.isSelected(1, col3));
        assertFalse(sm.isSelected(2, col3));
        assertFalse(sm.isSelected(3, col3));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);    // deselect (1, col3)
        assertFalse(sm.isSelected(1, col3));
        assertFalse(sm.isSelected(2, col3));
        assertFalse(sm.isSelected(3, col3));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);    // deselect (1, col2)
        assertFalse(sm.isSelected(1, col2));
    }
    
    
    /***************************************************************************
     * Tests for specific bug reports
     **************************************************************************/
    
    @Test public void test_rt18488_selectToLeft() {
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col5);
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col4)
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // select (1, col1)
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // deselect (1, col1)
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(debug(), sm.isSelected(1, col2));
        assertFalse(sm.isSelected(1, col1));
    }
    
    @Test public void test_rt18488_selectToRight() {
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col1);
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col4)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col5)
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        
        keyboard.doLeftArrowPress(KeyModifier.SHIFT);   // deselect (1, col5)
        assertFalse(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
    }
    
    @Ignore("This bug still exists")
    @Test public void test_rt18488_comment1() {
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col1);
        
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col2)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col3)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col4)
        keyboard.doRightArrowPress(KeyModifier.SHIFT);   // select (1, col5)
        keyboard.doDownArrowPress(KeyModifier.SHIFT);    // select (2, col5)
        
        assertTrue(sm.isSelected(2, col5));
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
        
        keyboard.doUpArrowPress(KeyModifier.SHIFT);     // deselect (2, col5)
        assertFalse(sm.isSelected(2, col5));
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
    }
    
    @Ignore("This bug still exists")
    @Test public void test_rt18536_positive_horizontal() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col1);
        
        // move focus by holding down ctrl button
        keyboard.doRightArrowPress(KeyModifier.CTRL);   // move focus to (1, col2)
        keyboard.doRightArrowPress(KeyModifier.CTRL);   // move focus to (1, col3)
        keyboard.doRightArrowPress(KeyModifier.CTRL);   // move focus to (1, col4)
        keyboard.doRightArrowPress(KeyModifier.CTRL);   // move focus to (1, col5)
        assertTrue(fm.isFocused(1, col5));
        
        // press shift + space to select all cells between (1, col1) and (1, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
    }
    
    @Ignore("This bug still exists")
    @Test public void test_rt18536_negative_horizontal() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col5);
        
        // move focus by holding down ctrl button
        keyboard.doLeftArrowPress(KeyModifier.CTRL);   // move focus to (1, col4)
        keyboard.doLeftArrowPress(KeyModifier.CTRL);   // move focus to (1, col3)
        keyboard.doLeftArrowPress(KeyModifier.CTRL);   // move focus to (1, col2)
        keyboard.doLeftArrowPress(KeyModifier.CTRL);   // move focus to (1, col1)
        assertTrue(fm.isFocused(1, col1));
        
        // press shift + space to select all cells between (1, col1) and (1, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(1, col4));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col1));
    }
    
    @Ignore("This bug still exists")
    @Test public void test_rt18536_positive_vertical() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(1, col5);
        
        // move focus by holding down ctrl button
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // move focus to (2, col5)
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // move focus to (3, col5)
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // move focus to (4, col5)
        keyboard.doDownArrowPress(KeyModifier.CTRL);   // move focus to (5, col5)
        assertTrue(fm.isFocused(5, col5));
        
        // press shift + space to select all cells between (1, col5) and (5, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(2, col5));
        assertTrue(sm.isSelected(3, col5));
        assertTrue(sm.isSelected(4, col5));
        assertTrue(sm.isSelected(5, col5));
    }
    
    @Ignore("This bug still exists")
    @Test public void test_rt18536_negative_vertical() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.setCellSelectionEnabled(true);
        sm.clearAndSelect(5, col5);
        
        // move focus by holding down ctrl button
        keyboard.doUpArrowPress(KeyModifier.CTRL);   // move focus to (4, col5)
        keyboard.doUpArrowPress(KeyModifier.CTRL);   // move focus to (3, col5)
        keyboard.doUpArrowPress(KeyModifier.CTRL);   // move focus to (2, col5)
        keyboard.doUpArrowPress(KeyModifier.CTRL);   // move focus to (1, col5)
        assertTrue(fm.isFocused(1, col5));
        
        // press shift + space to select all cells between (1, col5) and (5, col5)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col5));
        assertTrue(sm.isSelected(2, col5));
        assertTrue(sm.isSelected(3, col5));
        assertTrue(sm.isSelected(4, col5));
        assertTrue(sm.isSelected(5, col5));
    }
    
    @Test public void test_rt18642() {
        sm.setCellSelectionEnabled(false);
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
