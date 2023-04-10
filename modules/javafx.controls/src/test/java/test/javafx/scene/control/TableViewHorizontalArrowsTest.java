/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.KeyModifier.*;

import com.sun.javafx.util.Utils;
import javafx.geometry.NodeOrientation;
import javafx.scene.input.KeyCode;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import test.com.sun.javafx.scene.control.behavior.TableViewAnchorRetriever;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;


/**
 * Test basic horizontal navigation mappings for TableView.
 * It is parameterized on NodeOrientation
 */
@RunWith(Parameterized.class)
public class TableViewHorizontalArrowsTest {
    @Parameterized.Parameters
    public static Collection<?> implementations() {
        return Arrays.asList(new Object[][] {
            {NodeOrientation.LEFT_TO_RIGHT},
            {NodeOrientation.RIGHT_TO_LEFT}
        });
    }

    private TableView<String> tableView;
    private TableView.TableViewSelectionModel<String> sm;
    private TableView.TableViewFocusModel<String> fm;

    private TableColumn<String, String> col0;
    private TableColumn<String, String> col1;
    private TableColumn<String, String> col2;
    private TableColumn<String, String> col3;
    private TableColumn<String, String> col4;

    private KeyEventFirer keyboard;
    private StageLoader stageLoader;
    private NodeOrientation orientation;

    public TableViewHorizontalArrowsTest(NodeOrientation val) {
        orientation = val;
    }

    @Before
    public void setup() {
        tableView = new TableView<>();
        tableView.setNodeOrientation(orientation);
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(true);

        tableView.getItems().setAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

        col0 = new TableColumn<>("col0");
        col1 = new TableColumn<>("col1");
        col2 = new TableColumn<>("col2");
        col3 = new TableColumn<>("col3");
        col4 = new TableColumn<>("col4");
        tableView.getColumns().setAll(col0, col1, col2, col3, col4);

        keyboard = new KeyEventFirer(tableView);

        stageLoader = new StageLoader(tableView);
        stageLoader.getStage().show();
    }

    @After
    public void tearDown() {
        tableView.getSkin().dispose();
        stageLoader.dispose();
    }

    // ---------------- Helper methods -------------------------
    /**
     * Toggles the parameter nodeOrientation and
     * sets the tableView's orientation to the new toggled value
     */
    private void toggleNodeOrientation() {
        orientation = (orientation == NodeOrientation.LEFT_TO_RIGHT?
            NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        tableView.setNodeOrientation(orientation);
    }

    /**
     * Orientation-aware forward horizontal navigation with arrow keys.
     * @param modifiers the modifiers to use on keyboard
     */
    private void forward(KeyModifier... modifiers) {
        if (orientation == NodeOrientation.LEFT_TO_RIGHT) {
            keyboard.doRightArrowPress(modifiers);
        } else if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
            keyboard.doLeftArrowPress(modifiers);
        }
    }

    /**
     * Orientation-aware backward horizontal navigation with arrow keys.
     * @param modifiers the modifiers to use on keyboard
     */
    private void backward(KeyModifier... modifiers) {
        if (orientation == NodeOrientation.LEFT_TO_RIGHT) {
            keyboard.doLeftArrowPress(modifiers);
        } else if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
            keyboard.doRightArrowPress(modifiers);
        }
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


    // ----------------------- Tests ----------------------------

    @Test
    public void testForwardSelect() {
        sm.select(0, col0);
        forward();
        assertTrue("next cell must be selected", sm.isSelected(0, col1));
        assertFalse("old cell not be selected", sm.isSelected(0, col0));
    }

    @Test
    public void testBackwardSelect() {
        sm.select(0, col4);
        backward();
        assertTrue("next cell must be selected", sm.isSelected(0, col3));
        assertFalse("old cell not be selected", sm.isSelected(0, col4));
    }

    @Test
    public void testForwardFocus() {
        sm.select(0, col0);
        forward(getShortcutKey());
        assertTrue("selected cell must still be selected", sm.isSelected(0, col0));
        assertFalse("next cell must not be selected", sm.isSelected(0, col1));
        TablePosition<?, ?> focusedCell = fm.getFocusedCell();
        assertEquals("focused cell must moved to next", col1, focusedCell.getTableColumn());
    }

    @Test
    public void testBackwardFocus() {
        sm.select(0, col4);
        backward(getShortcutKey());
        assertTrue("selected cell must still be selected", sm.isSelected(0, col4));
        assertFalse("previous cell must not be selected", sm.isSelected(0, col3));
        TablePosition<?, ?> focusedCell = fm.getFocusedCell();
        assertEquals("focused cell must moved to prev", col3, focusedCell.getTableColumn());
    }

    @Test
    public void testChangeOrientationSimpleForwardSelect() {
        sm.select(0, col0);
        forward();
        assertTrue(sm.isSelected(0, col1));
        assertFalse(sm.isSelected(0, col0));

        toggleNodeOrientation();

        // Now, test that the forward select respects change in NodeOrientation
        forward();

        assertFalse(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
    }

    @Test
    public void testChangeOrientationSimpleBackwardSelect() {
        sm.select(0, col4);
        backward();
        assertTrue(sm.isSelected(0, col3));
        assertFalse(sm.isSelected(0, col4));

        toggleNodeOrientation();

        // Now, test that the backward select respects change in NodeOrientation
        backward();
        assertFalse(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col2));
    }

    @Test public void testShiftBackwardWhenAtFirstCol() {
        sm.select(0, col0);
        backward(KeyModifier.SHIFT);

        assertTrue("Selected cell remains selected", sm.isSelected(0, col0));

        // We are at the first colum, there is no backward cell
        assertFalse("sanity - forward cell must not be selected", sm.isSelected(0, col1));
    }

    @Test public void testShiftForwardWhenAtFirstCol() {
        sm.select(0, col0);
        forward(KeyModifier.SHIFT);

        assertTrue("Selected cell remains selected", sm.isSelected(0, col0));
        assertTrue("forward cell must also be selected", sm.isSelected(0, col1));
    }

    @Test public void testShiftBackwardWhenAtLastCol() {
        sm.select(0, col4);
        backward(KeyModifier.SHIFT);
        assertTrue("Selected cell remains selected", sm.isSelected(0, col4));
        assertTrue("backward cell must also be selected", sm.isSelected(0, col3));
    }

    @Test public void testShiftForwardWhenAtLastCol() {
        sm.select(0, col4);
        forward(KeyModifier.SHIFT);
        assertTrue("Selected cell remains selected", sm.isSelected(0, col4));

        // We are at the last colum, there is no forward cell
        assertFalse("sanity - backward cell must not be selected", sm.isSelected(0, col3));
    }

    @Test public void testCtrlBackwardDoesNotMoveRowFocus() {
        // Select first row
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));

        backward(KeyModifier.getShortcutKey());

        assertTrue("Focus should not change", fm.isFocused(0));
        assertTrue("Selection should not change", sm.isSelected(0));
    }

    @Test public void testCtrlForwardDoesNotMoveRowFocus() {
        // Select first row
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));

        forward(KeyModifier.getShortcutKey());

        assertTrue("Focus should not change", fm.isFocused(0));
        assertTrue("Selection should not change", sm.isSelected(0));
    }

    // Tests for discontinuous multiple cell selection (RT-18951)
    @Test public void test_rt18591_select_forward_then_backward() {
        sm.select(0, col0);

        forward(KeyModifier.getShortcutKey());
        forward(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE,
                KeyModifier.getShortcutKey(),
                (Utils.isMac()? KeyModifier.CTRL : null));
        assertTrue(sm.isSelected(0, col0));
        assertFalse(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(isAnchor(0, 2));

        forward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        forward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());

        assertTrue(sm.isSelected(0, col0));
        assertFalse(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
        assertTrue(isAnchor(0,2));


        backward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        backward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        backward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());

        assertTrue(sm.isSelected(0, col0));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col4));
    }

    @Test public void test_rt18591_select_backward_then_forward() {
        sm.select(0, col4);

        backward(KeyModifier.getShortcutKey());
        backward(KeyModifier.getShortcutKey());
        keyboard.doKeyPress(KeyCode.SPACE,
                KeyModifier.getShortcutKey(),
                (Utils.isMac()? KeyModifier.CTRL : null));

        assertTrue(sm.isSelected(0, col4));
        assertFalse(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(isAnchor(0, 2));

        backward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        backward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());

        assertTrue(sm.isSelected(0, col4));
        assertFalse(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col0));
        assertTrue(isAnchor(0,2));

        forward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        forward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());
        forward(KeyModifier.SHIFT, KeyModifier.getShortcutKey());

        assertTrue(sm.isSelected(0, col4));
        assertTrue(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col2));
        assertTrue(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col0));
    }

    @Test public void test_rt18536_forward_focus_and_selectAll() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.clearAndSelect(1, col0);

        // move focus by holding down ctrl button
        forward(KeyModifier.getShortcutKey());   // move focus to (1, col1)
        forward(KeyModifier.getShortcutKey());   // move focus to (1, col2)
        forward(KeyModifier.getShortcutKey());   // move focus to (1, col3)
        forward(KeyModifier.getShortcutKey());   // move focus to (1, col4)
        assertTrue(fm.isFocused(1, col4));

        // press shift + space to select all cells between (1, col0) and (1, col4)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col0));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col4));
    }

    @Test public void test_rt18536_backward_focus_and_selectAll() {
        // Test shift selection when focus is elsewhere (so as to select a range)
        sm.clearAndSelect(1, col4);

        // move focus by holding down ctrl button
        backward(KeyModifier.getShortcutKey());   // move focus to (1, col3)
        backward(KeyModifier.getShortcutKey());   // move focus to (1, col2)
        backward(KeyModifier.getShortcutKey());   // move focus to (1, col1)
        backward(KeyModifier.getShortcutKey());   // move focus to (1, col0)
        assertTrue(fm.isFocused(1, col0));

        // press shift + space to select all cells between (1, col0) and (1, col4)
        keyboard.doKeyPress(KeyCode.SPACE, KeyModifier.SHIFT);
        assertTrue(sm.isSelected(1, col0));
        assertTrue(sm.isSelected(1, col1));
        assertTrue(sm.isSelected(1, col2));
        assertTrue(sm.isSelected(1, col3));
        assertTrue(sm.isSelected(1, col4));
    }
}
