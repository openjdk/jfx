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

import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.control.skin.ScrollPaneSkin;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.UP;


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

    /**
     * We manually handle focus traversal keys due to the ScrollPane binding
     * the left/right/up/down keys specially.
     */
    protected static final List<KeyBinding> SCROLL_PANE_BINDINGS = new ArrayList<>();
    static {
        // TODO XXX DEBUGGING ONLY
        SCROLL_PANE_BINDINGS.add(new KeyBinding(F4, "TraverseDebug").alt().ctrl().shift());

        SCROLL_PANE_BINDINGS.add(new ScrollViewKeyBinding(LEFT, "HorizontalUnitDecrement"));
        SCROLL_PANE_BINDINGS.add(new ScrollViewKeyBinding(RIGHT, "HorizontalUnitIncrement"));

        SCROLL_PANE_BINDINGS.add(new ScrollViewKeyBinding(UP, "VerticalUnitDecrement"));
        SCROLL_PANE_BINDINGS.add(new ScrollViewKeyBinding(DOWN, "VerticalUnitIncrement"));

        SCROLL_PANE_BINDINGS.add(new ScrollViewKeyBinding(PAGE_UP, "VerticalPageDecrement"));
        SCROLL_PANE_BINDINGS.add(new ScrollViewKeyBinding(PAGE_DOWN, "VerticalPageIncrement"));
        SCROLL_PANE_BINDINGS.add(new KeyBinding(SPACE, "VerticalPageIncrement"));
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
        if ("HorizontalUnitDecrement".equals(name)) horizontalUnitDecrement();
        else if ("HorizontalUnitIncrement".equals(name)) horizontalUnitIncrement();
        else if ("VerticalUnitDecrement".equals(name)) verticalUnitDecrement();
        else if ("VerticalUnitIncrement".equals(name)) verticalUnitIncrement();
        else if ("VerticalPageDecrement".equals(name)) verticalPageDecrement();
        else if ("VerticalPageIncrement".equals(name)) verticalPageIncrement();
        else super.callAction(name);
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

    /**
     * Class to handle key bindings based upon the orientation of the control.
     */
    public static class ScrollViewKeyBinding extends OrientedKeyBinding {
        public ScrollViewKeyBinding(KeyCode code, String action) {
            super(code, action);
        }

        public ScrollViewKeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
            super(code, type, action);
        }
        public @Override boolean getVertical(Control control) {
            return true;
        }
    }
}
