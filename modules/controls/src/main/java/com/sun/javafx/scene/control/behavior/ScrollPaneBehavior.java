/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.control.skin.ScrollPaneSkin;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.TAB;


/**
 * Behavior for ScrollPane.
 *
 * TODO: the function variables are a poor way to couple to the rest of
 * the system. This technique avoids a direct dependency on the skin class.
 * However, this should really be coupled through the control itself instead
 * of directly to the skin.
 */
public class ScrollPaneBehavior extends BehaviorBase<ScrollPane> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ScrollPaneBehavior(ScrollPane scrollPane) {
        super(scrollPane, SCROLL_PANE_BINDINGS);
    }

    /***************************************************************************
     *                                                                         *
     * Functions                                                               *
     *                                                                         *
     **************************************************************************/

    public void horizontalUnitIncrement() {
        ((ScrollPaneSkin)getControl().getSkin()).hsbIncrement();
    }
    public void horizontalUnitDecrement() {
        ((ScrollPaneSkin)getControl().getSkin()).hsbDecrement();
    }
    public void verticalUnitIncrement() {
        ((ScrollPaneSkin)getControl().getSkin()).vsbIncrement();
    }
    void verticalUnitDecrement() {
        ((ScrollPaneSkin)getControl().getSkin()).vsbDecrement();
    }
    void horizontalPageIncrement() {
        ((ScrollPaneSkin)getControl().getSkin()).hsbPageIncrement();
    }
    void horizontalPageDecrement() {
        ((ScrollPaneSkin)getControl().getSkin()).hsbPageDecrement();
    }
    void verticalPageIncrement() {
        ((ScrollPaneSkin)getControl().getSkin()).vsbPageIncrement();
    }
    void verticalPageDecrement() {
        ((ScrollPaneSkin)getControl().getSkin()).vsbPageDecrement();
    }
    void verticalHome() {
        ScrollPane sp = getControl();
        sp.setHvalue(sp.getHmin());
        sp.setVvalue(sp.getVmin());
    }
    void verticalEnd() {
        ScrollPane sp = getControl();
        sp.setHvalue(sp.getHmax());
        sp.setVvalue(sp.getVmax());
    }


    public void contentDragged(double deltaX, double deltaY) {
        // negative when dragged to the right/bottom
        ScrollPane scroll = getControl();
        if (!scroll.isPannable()) return;
        if (deltaX < 0 && scroll.getHvalue() != 0 || deltaX > 0 && scroll.getHvalue() != scroll.getHmax()) {
            scroll.setHvalue(scroll.getHvalue() + deltaX);
        }
        if (deltaY < 0 && scroll.getVvalue() != 0 || deltaY > 0 && scroll.getVvalue() != scroll.getVmax()) {
            scroll.setVvalue(scroll.getVvalue() + deltaY);
        }
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    static final String TRAVERSE_DEBUG = "TraverseDebug";
    static final String HORIZONTAL_UNITDECREMENT = "HorizontalUnitDecrement";
    static final String HORIZONTAL_UNITINCREMENT = "HorizontalUnitIncrement";
    static final String VERTICAL_UNITDECREMENT = "VerticalUnitDecrement";
    static final String VERTICAL_UNITINCREMENT = "VerticalUnitIncrement";
    static final String VERTICAL_PAGEDECREMENT = "VerticalPageDecrement";
    static final String VERTICAL_PAGEINCREMENT = "VerticalPageIncrement";
    static final String VERTICAL_HOME = "VerticalHome";
    static final String VERTICAL_END = "VerticalEnd";
    static final String TRAVERSE_NEXT = "TraverseNext";
    static final String TRAVERSE_PREVIOUS = "TraversePrevious";

    /**
     * We manually handle focus traversal keys due to the ScrollPane binding
     * the left/right/up/down keys specially.
     */
    protected static final List<KeyBinding> SCROLL_PANE_BINDINGS = new ArrayList<>();
    static {
        // TODO XXX DEBUGGING ONLY
        SCROLL_PANE_BINDINGS.add(new KeyBinding(F4, TRAVERSE_DEBUG).alt().ctrl().shift());

        SCROLL_PANE_BINDINGS.add(new KeyBinding(LEFT, HORIZONTAL_UNITDECREMENT));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(RIGHT, HORIZONTAL_UNITINCREMENT));

        SCROLL_PANE_BINDINGS.add(new KeyBinding(UP, VERTICAL_UNITDECREMENT));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(DOWN, VERTICAL_UNITINCREMENT));

        SCROLL_PANE_BINDINGS.add(new KeyBinding(PAGE_UP, VERTICAL_PAGEDECREMENT));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(PAGE_DOWN, VERTICAL_PAGEINCREMENT));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(SPACE, VERTICAL_PAGEINCREMENT));

        SCROLL_PANE_BINDINGS.add(new KeyBinding(HOME, VERTICAL_HOME));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(END, VERTICAL_END));

        SCROLL_PANE_BINDINGS.add(new KeyBinding(TAB, TRAVERSE_NEXT));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(TAB, TRAVERSE_PREVIOUS).shift());

    }

    protected /*final*/ String matchActionForEvent(KeyEvent e) {
        //TODO - untested code doesn't seem to get triggered (key eaten?)
        String action = super.matchActionForEvent(e);
        if (action != null) {
            if (e.getCode() == LEFT) {
                if (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    action = "HorizontalUnitIncrement";
                }
            } else if (e.getCode() == RIGHT) {
                if (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    action = "HorizontalUnitDecrement";
                }
            }
        }
        return action;
    }

    @Override protected void callAction(String name) {
        switch (name) {
        case HORIZONTAL_UNITDECREMENT:
            horizontalUnitDecrement();
            break;
        case HORIZONTAL_UNITINCREMENT:
            horizontalUnitIncrement();
            break;
        case VERTICAL_UNITDECREMENT:
            verticalUnitDecrement();
            break;
        case VERTICAL_UNITINCREMENT:
            verticalUnitIncrement();
            break;
        case VERTICAL_PAGEDECREMENT:
            verticalPageDecrement();
            break;
        case VERTICAL_PAGEINCREMENT:
            verticalPageIncrement();
            break;
        case VERTICAL_HOME:
            verticalHome();
            break;
        case VERTICAL_END:
            verticalEnd();
            break;
        case TRAVERSE_NEXT:
        case TRAVERSE_PREVIOUS:
            // Is there any focusable node in the ScrollPane content
            if (!getControl().getImpl_traversalEngine().registeredNodes.isEmpty() ||
                TabPaneBehavior.getFirstPopulatedInnerTraversalEngine(getControl().getChildrenUnmodifiable()) != null) {
                /**
                 * if we have the focus owner then traverse from it, otherwise
                 * request focus in the top-left
                 */
                List<Node> children = getControl().getChildrenUnmodifiable();
                Node focusNode = getControl().getScene().getFocusOwner();
                if (focusNode != null && isChildFocused(focusNode, children)) {
                    // This happens if the child doesn't have the key binding
                    // for traversal and the event bubbled up to this class.
                    focusNode.impl_traverse("TraverseNext".equals(name) ? Direction.NEXT : Direction.PREVIOUS);
                } else if ("TraverseNext".equals(name)) {
                    focusFirstChild(children);
                } else {
                    super.callAction(name);
                }
            } else {
                super.callAction(name);
            }
            break;

        default :
         super.callAction(name);
            break;
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

    public static boolean focusFirstChild(List<Node> children) {
        for(int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n.isFocusTraversable() && n.impl_isTreeVisible() && !n.isDisabled()) {
                n.requestFocus();
                return true;
            }
            else if (n instanceof Parent) {
                if (focusFirstChild(((Parent)n).getChildrenUnmodifiable())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean focusLastChild(List<Node> children) {
        for(int i = children.size()-1 ; i > -1; i--) {
            Node n = children.get(i);
            if (n.isFocusTraversable() && n.impl_isTreeVisible() && !n.isDisabled()) {
                n.requestFocus();
                return true;
            }
            else if (n instanceof Parent) {
                if (focusFirstChild(((Parent)n).getChildrenUnmodifiable())) {
                    return true;
                }
            }
        }
        return false;
    }




    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    public void mouseClicked() {
        getControl().requestFocus();
    }

    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        getControl().requestFocus();
    }
}
