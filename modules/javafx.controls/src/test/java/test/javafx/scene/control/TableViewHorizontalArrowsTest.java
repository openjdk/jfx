/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.BiConsumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.KeyModifier.*;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Test basic horizontal navigation mappings for TableView. It's parametrized on NodeOrientation
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
        tableView = new TableView<String>();
        tableView.setNodeOrientation(orientation);
        sm = tableView.getSelectionModel();
        fm = tableView.getFocusModel();

        sm.setSelectionMode(SelectionMode.MULTIPLE);
        sm.setCellSelectionEnabled(false);

        tableView.getItems().setAll("1", "2", "3", "4", "5", "6", "7", "8", "9",
                                    "10", "11", "12");

        col0 = new TableColumn<String, String>("col0");
        col1 = new TableColumn<String, String>("col1");
        col2 = new TableColumn<String, String>("col2");
        col3 = new TableColumn<String, String>("col3");
        col4 = new TableColumn<String, String>("col4");
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
     * Toggles the nodeOrientation of tableView.
     */
    private void changeNodeOrientation() {
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


    // ----------------------- Tests ----------------------------

    @Test
    public void testForwardSelect() {
        sm.setCellSelectionEnabled(true);
        sm.select(0, col0);
        forward();
        assertTrue("next cell must be selected", sm.isSelected(0, col1));
        assertFalse("old cell not be selected", sm.isSelected(0, col0));
    }

    @Test
    public void testBackwardSelect() {
        sm.setCellSelectionEnabled(true);
        sm.select(0, col4);
        backward();
        assertTrue("next cell must be selected", sm.isSelected(0, col3));
        assertFalse("old cell not be selected", sm.isSelected(0, col4));
    }

    @Test
    public void testForwardFocus() {
        sm.setCellSelectionEnabled(true);
        sm.select(0, col0);
        forward(getShortcutKey());
        assertTrue("selected cell must still be selected", sm.isSelected(0, col0));
        assertFalse("next cell must not be selected", sm.isSelected(0, col1));
        TablePosition<?, ?> focusedCell = fm.getFocusedCell();
        assertEquals("focused cell must moved to next", col1, focusedCell.getTableColumn());
    }

    @Test
    public void testBackwardFocus() {
        sm.setCellSelectionEnabled(true);
        sm.select(0, col4);
        backward(getShortcutKey());
        assertTrue("selected cell must still be selected", sm.isSelected(0, col4));
        assertFalse("previous cell must not be selected", sm.isSelected(0, col3));
        TablePosition<?, ?> focusedCell = fm.getFocusedCell();
        assertEquals("focused cell must moved to prev", col3, focusedCell.getTableColumn());
    }



    @Test
    public void testChangeOrientationSimpleForwardSelect() {
        sm.setCellSelectionEnabled(true);
        sm.select(0, col0);
        forward();
        assertTrue(sm.isSelected(0, col1));
        assertFalse(sm.isSelected(0, col0));

        changeNodeOrientation();

        // Now, test that the forward select resprects change in NodeOrientation
        forward();

        assertFalse(sm.isSelected(0, col1));
        assertTrue(sm.isSelected(0, col2));
    }

    @Test
    public void testChangeOrientationSimpleBackwardSelect() {
        sm.setCellSelectionEnabled(true);
        sm.select(0, col4);
        backward();
        assertTrue(sm.isSelected(0, col3));
        assertFalse(sm.isSelected(0, col4));

        changeNodeOrientation();

        // Now, test that the backward select resprects change in NodeOrientation
        backward();
        assertFalse(sm.isSelected(0, col3));
        assertTrue(sm.isSelected(0, col2));
    }

    // TBD: add tests for all keyMappings with modifiers
}
