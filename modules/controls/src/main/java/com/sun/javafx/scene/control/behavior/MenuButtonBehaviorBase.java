/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Side;
import javafx.scene.control.MenuButton;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import static javafx.scene.input.KeyCode.*;

/**
 * The base behavior for a MenuButton.
 */
public abstract class MenuButtonBehaviorBase<C extends MenuButton> extends ButtonBehavior<C> {

    private final InputMap<C> buttonInputMap;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public MenuButtonBehaviorBase(final C menuButton) {
        super(menuButton);

        // pull down the parent input map, no need to add focus traversal
        // mappings - added in ButtonBehavior.
        buttonInputMap = super.getInputMap();

        // We want to remove the maping for MOUSE_RELEASED, as the event is
        // handled by the skin instead, which calls the mouseReleased method below.
        removeMapping(MouseEvent.MOUSE_RELEASED);

        /**
         * The base key bindings for a MenuButton. These basically just define the
         * bindings to close an open menu. Subclasses will tell you what can be done
         * to open it.
         */
        addDefaultMapping(new KeyMapping(ESCAPE, e -> getNode().hide()));
        addDefaultMapping(new KeyMapping(CANCEL, e -> getNode().hide()));

        // we create a child input map, as we want to override some of the
        // focus traversal behaviors (and child maps take precedence over parent maps)
        InputMap<C> customFocusInputMap = new InputMap<>(menuButton);
        addDefaultMapping(customFocusInputMap, new KeyMapping(UP, this::overrideTraversalInput));
        addDefaultMapping(customFocusInputMap, new KeyMapping(DOWN, this::overrideTraversalInput));
        addDefaultMapping(customFocusInputMap, new KeyMapping(LEFT, this::overrideTraversalInput));
        addDefaultMapping(customFocusInputMap, new KeyMapping(RIGHT, this::overrideTraversalInput));
        addDefaultChildMap(buttonInputMap, customFocusInputMap);
    }


    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    private void overrideTraversalInput(KeyEvent event) {
        final MenuButton button = getNode();
        final Side popupSide = button.getPopupSide();
        if (!button.isShowing() &&
                (event.getCode() == UP    && popupSide == Side.TOP)    ||
                (event.getCode() == DOWN  && (popupSide == Side.BOTTOM || popupSide == Side.TOP))  ||
                (event.getCode() == LEFT  && (popupSide == Side.RIGHT  || popupSide == Side.LEFT)) ||
                (event.getCode() == RIGHT && (popupSide == Side.RIGHT  || popupSide == Side.LEFT))) {
            // Show the menu when arrow key matches the popupSide
            // direction -- but also allow RIGHT key for LEFT position and
            // DOWN key for TOP position. To be symmetrical, we also allow for
            // the LEFT key to work when in the RIGHT position. This is needed
            // because the skin only paints right- and down-facing arrows in
            // these cases.
            button.show();
        }
    }

    protected void openAction() {
        if (getNode().isShowing()) {
            getNode().hide();
        } else {
            getNode().show();
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
        final C control = getNode();

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
                if (e.getButton() == MouseButton.PRIMARY) {
                    control.show();
                }
            }
        }
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
            if (getNode().isShowing() && !getNode().contains(e.getX(), e.getY())) {
                getNode().hide();
            }
            getNode().disarm();
        }
    }
}
