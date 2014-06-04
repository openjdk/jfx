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

package com.sun.javafx.scene.control.behavior;

import javafx.event.Event;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TabPaneBehavior extends BehaviorBase<TabPane> {

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    private static final String HOME = "Home";
    private static final String END = "End";
    private static final String CTRL_PAGE_UP = "Ctrl_Page_Up";
    private static final String CTRL_PAGE_DOWN = "Ctrl_Page_Down";
    private static final String CTRL_TAB = "Ctrl_Tab";
    private static final String CTRL_SHIFT_TAB = "Ctrl_Shift_Tab";

    protected static final List<KeyBinding> TAB_PANE_BINDINGS = new ArrayList<>();
    static {
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.UP, "TraverseUp"));
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.DOWN, "TraverseDown"));
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.LEFT, "TraverseLeft"));
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.RIGHT, "TraverseRight"));
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.HOME, HOME));
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.END, END));
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.PAGE_UP, CTRL_PAGE_UP).ctrl());
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.PAGE_DOWN, CTRL_PAGE_DOWN).ctrl());
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.TAB, CTRL_TAB).ctrl());
        TAB_PANE_BINDINGS.add(new KeyBinding(KeyCode.TAB, CTRL_SHIFT_TAB).shift().ctrl());
    }

    @Override protected void callAction(String name) {
        boolean rtl = (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);

        if (("TraverseLeft".equals(name) && !rtl) ||
            ("TraverseRight".equals(name) && rtl) ||
            "TraverseUp".equals(name)) {
            if (getControl().isFocused()) {
                selectPreviousTab();
            }
        } else if (("TraverseRight".equals(name) && !rtl) || 
                   ("TraverseLeft".equals(name) && rtl) ||
                   "TraverseDown".equals(name)) {
            if (getControl().isFocused()) {
                selectNextTab();
            }
        } else if (CTRL_TAB.equals(name) || CTRL_PAGE_DOWN.equals(name)) {
            TabPane tp = getControl();
            if (tp.getSelectionModel().getSelectedIndex() == (tp.getTabs().size() - 1)) {
                tp.getSelectionModel().selectFirst();
            } else {
                selectNextTab();
            }
            tp.requestFocus();
        } else if (CTRL_SHIFT_TAB.equals(name) || CTRL_PAGE_UP.equals(name)) {
            TabPane tp = getControl();
            if (tp.getSelectionModel().getSelectedIndex() == 0) {
                tp.getSelectionModel().selectLast();
            } else {
                selectPreviousTab();
            }
            tp.requestFocus();
        } else if (HOME.equals(name)) {
            if (getControl().isFocused()) {
                getControl().getSelectionModel().selectFirst();
            }
        } else if (END.equals(name)) {
            if (getControl().isFocused()) {
                getControl().getSelectionModel().selectLast();
            }
        } else {
            super.callAction(name);
        }
    }

    public static boolean isChildFocused(Node focusedNode, List<Node> children) {
        boolean answer = false;
        for(int i = 0; i < children.size(); i++) {
            if (children.get(i) == focusedNode) {
                answer = true;
                break;
            }
            if (children.get(i) instanceof Parent) {
                if (isChildFocused(focusedNode, ((Parent)children.get(i)).getChildrenUnmodifiable())) {
                    return true;
                }
            }
        }
        return answer;
    }

    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        TabPane tp = getControl();
        tp.requestFocus();
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    public TabPaneBehavior(TabPane tabPane) {
        super(tabPane, TAB_PANE_BINDINGS);
    }

    public void selectTab(Tab tab) {
        getControl().getSelectionModel().select(tab);
    }

    public boolean canCloseTab(Tab tab) {
        Event event = new Event(tab,tab,Tab.TAB_CLOSE_REQUEST_EVENT);
        Event.fireEvent(tab, event);
        return ! event.isConsumed();
    }
    
    public void closeTab(Tab tab) {
        TabPane tabPane = getControl();
        // only switch to another tab if the selected tab is the one we're closing
        int index = tabPane.getTabs().indexOf(tab);
        if (index != -1) {
            tabPane.getTabs().remove(index);
        }                
        if (tab.getOnClosed() != null) {
            Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
        }
    }

    // Find a tab after the currently selected that is not disabled.
    public void selectNextTab() {
        SingleSelectionModel<Tab> selectionModel = getControl().getSelectionModel();
        int current = selectionModel.getSelectedIndex();
        int index = current;
        while (index < getControl().getTabs().size()) {
            selectionModel.selectNext();
            index++;
            if (!selectionModel.getSelectedItem().isDisable()) {
                return;
            }
        }
        selectionModel.select(current);
    }

    // Find a tab before the currently selected that is not disabled.
    public void selectPreviousTab() {       
        SingleSelectionModel<Tab> selectionModel = getControl().getSelectionModel();
        int current = selectionModel.getSelectedIndex();
        int index = current;
        while (index > 0) {
            selectionModel.selectPrevious();
            index--;
            if (!selectionModel.getSelectedItem().isDisable()) {
                return;
            }
        }
        selectionModel.select(current);
    }
}
