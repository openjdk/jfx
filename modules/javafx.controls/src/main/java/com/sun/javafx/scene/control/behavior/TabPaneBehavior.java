/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import java.util.List;
import javafx.event.Event;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.input.KeyBinding;
import javafx.scene.control.input.SkinInputMap;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

/**
 * The TabPaneBehavior is stateless.
 */
// The amount of memory saved with stateless (static) behaviors would likely not justify
// more extensive changes that are required to convert the legacy behavior to a stateless one:
// now we have to manually drag the control instance everywhere.
// I don't think this is worth it.
public class TabPaneBehavior {
    private static final SkinInputMap<TabPane> inputMap = createInputMap();

    // stateless behavior: one SkinInputMap for all TabPanes
    private TabPaneBehavior() { }

    private static SkinInputMap<TabPane> createInputMap() {
        SkinInputMap<TabPane> m = new SkinInputMap<>();

        m.registerFunction(TabPane.Tag.SELECT_FIRST_TAB, TabPaneBehavior::selectFirstTab);
        m.registerFunction(TabPane.Tag.SELECT_LAST_TAB, TabPaneBehavior::selectLastTab);
        m.registerFunction(TabPane.Tag.SELECT_LEFT_TAB, TabPaneBehavior::selectLeftTab);
        m.registerFunction(TabPane.Tag.SELECT_NEXT_TAB, TabPaneBehavior::selectNextTab);
        m.registerFunction(TabPane.Tag.SELECT_PREV_TAB, TabPaneBehavior::selectPreviousTab);
        m.registerFunction(TabPane.Tag.SELECT_RIGHT_TAB, TabPaneBehavior::selectRightTab);

        m.registerKey(KeyBinding.of(KeyCode.DOWN), TabPane.Tag.SELECT_NEXT_TAB);
        m.registerKey(KeyBinding.of(KeyCode.HOME), TabPane.Tag.SELECT_FIRST_TAB);
        m.registerKey(KeyBinding.of(KeyCode.END), TabPane.Tag.SELECT_LAST_TAB);
        m.registerKey(KeyBinding.of(KeyCode.LEFT), TabPane.Tag.SELECT_LEFT_TAB);
        m.registerKey(KeyBinding.of(KeyCode.RIGHT), TabPane.Tag.SELECT_RIGHT_TAB);
        m.registerKey(KeyBinding.of(KeyCode.UP), TabPane.Tag.SELECT_PREV_TAB);

        m.registerKey(KeyBinding.ctrl(KeyCode.PAGE_DOWN), TabPane.Tag.SELECT_NEXT_TAB);
        m.registerKey(KeyBinding.ctrl(KeyCode.PAGE_UP), TabPane.Tag.SELECT_PREV_TAB);
        m.registerKey(KeyBinding.ctrl(KeyCode.TAB), TabPane.Tag.SELECT_NEXT_TAB);
        m.registerKey(KeyBinding.ctrlShift(KeyCode.TAB), TabPane.Tag.SELECT_PREV_TAB);

        m.addHandler(MouseEvent.MOUSE_PRESSED, true, TabPaneBehavior::requestFocus);

        return m;
    }

    public static void install(TabPane control) {
        control.getInputMap().setSkinInputMap(inputMap);
    }

    public static void selectTab(TabPane c, Tab tab) {
        c.getSelectionModel().select(tab);
    }

    public static boolean canCloseTab(Tab tab) {
        Event ev = new Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT);
        Event.fireEvent(tab, ev);
        return !ev.isConsumed();
    }

    public static void closeTab(TabPane c, Tab tab) {
        // only switch to another tab if the selected tab is the one we're closing
        int index = c.getTabs().indexOf(tab);
        if (index != -1) {
            c.getTabs().remove(index);
        }
        if (tab.getOnClosed() != null) {
            Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
        }
    }

    // Find a tab after the currently selected that is not disabled. Loop around
    // if no tabs are found after currently selected tab.
    public static void selectNextTab(TabPane c) {
        moveSelection(c, 1);
    }

    // Find a tab before the currently selected that is not disabled.
    public static void selectPreviousTab(TabPane c) {
        moveSelection(c, -1);
    }

    private static void selectLeftTab(TabPane c) {
        if (isRTL(c)) {
            selectNextTab(c);
        } else {
            selectPreviousTab(c);
        }
    }

    private static void selectRightTab(TabPane c) {
        if (isRTL(c)) {
            selectPreviousTab(c);
        } else {
            selectNextTab(c);
        }
    }

    private static boolean isRTL(TabPane c) {
        return c.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;
    }

    // TODO a bit of controversy: should this method return boolean to avoid consuming the key event
    // when the control is not focused?
    private static void selectFirstTab(TabPane c) {
        if (c.isFocused()) {
            moveSelection(c, -1, 1);
        }
    }

    // TODO a bit of controversy: should this method return boolean to avoid consuming the key event
    // when the control is not focused?
    private static void selectLastTab(TabPane c) {
        if (c.isFocused()) {
            int sz = c.getTabs().size();
            moveSelection(c, sz, -1);
        }
    }

    private static void moveSelection(TabPane c, int delta) {
        int ix = c.getSelectionModel().getSelectedIndex();
        moveSelection(c, ix, delta);
    }

    private static void moveSelection(TabPane c, int startIndex, int delta) {
        if (c.getTabs().isEmpty()) {
            return;
        }

        int tabIndex = findValidTab(c, startIndex, delta);
        if (tabIndex > -1) {
            final SelectionModel<Tab> selectionModel = c.getSelectionModel();
            selectionModel.select(tabIndex);
        }
        c.requestFocus();
    }

    private static int findValidTab(TabPane c, int startIndex, int delta) {
        final List<Tab> tabs = c.getTabs();
        final int max = tabs.size();

        int index = startIndex;
        do {
            index = nextIndex(index + delta, max);
            Tab tab = tabs.get(index);
            if (tab != null && !tab.isDisable()) {
                return index;
            }
        } while (index != startIndex);

        return -1;
    }

    private static int nextIndex(int value, int max) {
        final int min = 0;
        int r = value % max;
        if (r > min && max < min) {
            r = r + max - min;
        } else if (r < min && max > min) {
            r = r + max - min;
        }
        return r;
    }

    private static void requestFocus(MouseEvent ev) {
        ((Node)ev.getSource()).requestFocus();
    }
}
