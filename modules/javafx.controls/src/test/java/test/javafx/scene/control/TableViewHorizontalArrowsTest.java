/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.KeyModifier.getShortcutKey;
import java.util.Collection;
import java.util.List;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.util.Utils;
import test.com.sun.javafx.scene.control.behavior.TableViewAnchorRetriever;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Test basic horizontal navigation mappings for TableView.
 * It is parameterized on NodeOrientation
 */
public class TableViewHorizontalArrowsTest {
    private static Collection<NodeOrientation> parameters() {
        return List.of(
            NodeOrientation.LEFT_TO_RIGHT,
            NodeOrientation.RIGHT_TO_LEFT
        );
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

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(NodeOrientation orientation) {
        this.orientation = orientation;
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

    @AfterEach
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
        orientation = (orientation == NodeOrientation.LEFT_TO_RIGHT ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void testForwardSelect(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col0);
        forward();
        assertTrue(sm.isSelected(0, col1), "next cell must be selected");
        assertFalse(sm.isSelected(0, col0), "old cell not be selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBackwardSelect(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col4);
        backward();
        assertTrue(sm.isSelected(0, col3), "next cell must be selected");
        assertFalse(sm.isSelected(0, col4), "old cell not be selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testForwardFocus(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col0);
        forward(getShortcutKey());
        assertTrue(sm.isSelected(0, col0), "selected cell must still be selected");
        assertFalse(sm.isSelected(0, col1), "next cell must not be selected");
        TablePosition<?, ?> focusedCell = fm.getFocusedCell();
        assertEquals(col1, focusedCell.getTableColumn(), "focused cell must moved to next");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testBackwardFocus(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col4);
        backward(getShortcutKey());
        assertTrue(sm.isSelected(0, col4), "selected cell must still be selected");
        assertFalse(sm.isSelected(0, col3), "previous cell must not be selected");
        TablePosition<?, ?> focusedCell = fm.getFocusedCell();
        assertEquals(col3, focusedCell.getTableColumn(), "focused cell must moved to prev");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeOrientationSimpleForwardSelect(NodeOrientation orientation) {
        setup(orientation);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeOrientationSimpleBackwardSelect(NodeOrientation orientation) {
        setup(orientation);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void testShiftBackwardWhenAtFirstCol(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col0);
        backward(KeyModifier.SHIFT);

        assertTrue(sm.isSelected(0, col0), "Selected cell remains selected");

        // We are at the first colum, there is no backward cell
        assertFalse(sm.isSelected(0, col1), "sanity - forward cell must not be selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testShiftForwardWhenAtFirstCol(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col0);
        forward(KeyModifier.SHIFT);

        assertTrue(sm.isSelected(0, col0), "Selected cell remains selected");
        assertTrue(sm.isSelected(0, col1), "forward cell must also be selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testShiftBackwardWhenAtLastCol(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col4);
        backward(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0, col4), "Selected cell remains selected");
        assertTrue(sm.isSelected(0, col3), "backward cell must also be selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testShiftForwardWhenAtLastCol(NodeOrientation orientation) {
        setup(orientation);
        sm.select(0, col4);
        forward(KeyModifier.SHIFT);
        assertTrue(sm.isSelected(0, col4), "Selected cell remains selected");

        // We are at the last colum, there is no forward cell
        assertFalse(sm.isSelected(0, col3), "sanity - backward cell must not be selected");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCtrlBackwardDoesNotMoveRowFocus(NodeOrientation orientation) {
        setup(orientation);
        // Select first row
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));

        backward(KeyModifier.getShortcutKey());

        assertTrue(fm.isFocused(0), "Focus should not change");
        assertTrue(sm.isSelected(0), "Selection should not change");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCtrlForwardDoesNotMoveRowFocus(NodeOrientation orientation) {
        setup(orientation);
        // Select first row
        sm.clearAndSelect(0);
        assertTrue(fm.isFocused(0));

        forward(KeyModifier.getShortcutKey());

        assertTrue(fm.isFocused(0), "Focus should not change");
        assertTrue(sm.isSelected(0), "Selection should not change");
    }

    // Tests for discontinuous multiple cell selection (JDK-8120523)
    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt18591_select_forward_then_backward(NodeOrientation orientation) {
        setup(orientation);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt18591_select_backward_then_forward(NodeOrientation orientation) {
        setup(orientation);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt18536_forward_focus_and_selectAll(NodeOrientation orientation) {
        setup(orientation);
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void test_rt18536_backward_focus_and_selectAll(NodeOrientation orientation) {
        setup(orientation);
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
