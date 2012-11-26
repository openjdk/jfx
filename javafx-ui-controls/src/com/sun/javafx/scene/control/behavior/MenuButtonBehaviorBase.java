/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.CANCEL;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Side;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;

/**
 * The base behavior for a MenuButton.
 */
public abstract class MenuButtonBehaviorBase<C extends MenuButton> extends ButtonBehavior<C> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public MenuButtonBehaviorBase(final C menuButton) {
        super(menuButton);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * Opens the popup menu.
     */
    protected static final String OPEN_ACTION = "Open";

    /**
     * Closes the popup menu.
     */
    protected static final String CLOSE_ACTION = "Close";

    /**
     * The base key bindings for a MenuButton. These basically just define the
     * bindings to close an open menu. Subclasses will tell you what can be done
     * to open it.
     */
    protected static final List<KeyBinding> BASE_MENU_BUTTON_BINDINGS = new ArrayList<KeyBinding>();
    static {
        BASE_MENU_BUTTON_BINDINGS.add(new KeyBinding(UP, "TraverseUp"));
        BASE_MENU_BUTTON_BINDINGS.add(new KeyBinding(DOWN, "TraverseDown"));
        BASE_MENU_BUTTON_BINDINGS.add(new KeyBinding(LEFT, "TraverseLeft"));
        BASE_MENU_BUTTON_BINDINGS.add(new KeyBinding(RIGHT, "TraverseRight"));
        BASE_MENU_BUTTON_BINDINGS.add(new KeyBinding(ESCAPE, KEY_PRESSED, CLOSE_ACTION));
        BASE_MENU_BUTTON_BINDINGS.add(new KeyBinding(CANCEL, KEY_PRESSED, CLOSE_ACTION));
    }

    /**
     * Invokes the given named action.
     *
     * @param name the name of the action to invoke
     */
    @Override protected void callAction(String name) {
        MenuButton button = getControl();
        Side popupSide = button.getPopupSide();
        if (CLOSE_ACTION.equals(name)) {
            button.hide();
        } else if (OPEN_ACTION.equals(name)) {
            if (button.isShowing()) {
                button.hide();
            } else {
                button.show();
            }
        } else if (!button.isShowing() &&
                   ("TraverseUp".equals(name)    && popupSide == Side.TOP) ||
                   ("TraverseDown".equals(name)  && (popupSide == Side.BOTTOM || popupSide == Side.TOP)) ||
                   ("TraverseLeft".equals(name)  && popupSide == Side.LEFT) ||
                   ("TraverseRight".equals(name) && (popupSide == Side.RIGHT || popupSide == Side.LEFT))) {
            // Show the menu when arrow key matches the popupSide
            // direction -- but also allow RIGHT key for LEFT position and
            // DOWN key for TOP position. This is needed because the skin
            // only paints right- and down-facing arrows in these cases.
            button.show();
        } else {
            super.callAction(name);
        }
    }

    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    /**
     * When a mouse button is pressed, we either want to behave like a button or
     * show the popup.  This will be called by the skin.
     *
     * @param e the mouse press event
     * @param behaveLikeButton if true, this should act just like a button
     */
    public void mousePressed(MouseEvent e, boolean behaveLikeButton) {
        final C control = getControl();

        /*
         * Behaving like a button is easy - we just call super. But, we cannot
         * call super if all we want to do is show the popup. The reason for
         * this is that super also handles all the arm/disarm/fire logic, and
         * this can inadvertently cause actions to fire when we don't want them
         * to fire. So, we unfortunately need to duplicate the focus
         * handling code here.
         */
        if (behaveLikeButton) {
            if (control.isShowing()) {
                control.hide();
            }
            super.mousePressed(e);
        } else {
            if (!control.isFocused() && control.isFocusTraversable()) {
                control.requestFocus();
            }
            if (control.isShowing()) {
                control.hide();
            } else {
                control.show();
            }
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        // Overriding to not call fire() on mouseReleased.
        // The event is handled by the skin instead, which calls
        // the method below.
    }

    /**
     * Handles mouse release events.  This will be called by the skin.
     *
     * @param e the mouse press event
     * @param behaveLikeButton if true, this should act just like a button
     */
    public void mouseReleased(MouseEvent e, boolean behaveLikeButton) {
        if (behaveLikeButton) {
            super.mouseReleased(e);
        } else {
            if (getControl().isShowing() && !getControl().contains(e.getX(), e.getY())) {
                getControl().hide();
            }
            getControl().disarm();
        }
    }
}
