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

import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;


/**
 * All of the "button" types (CheckBox, RadioButton, ToggleButton, and Button)
 * and also maybe some other types like hyperlinks operate on the "armed"
 * selection strategy, just like JButton. This behavior class encapsulates that
 * logic in a way that can be reused and extended by each of the individual
 * class behaviors.
 *
 */
public class ButtonBehavior<C extends ButtonBase> extends BehaviorBase<C> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ButtonBehavior(final C button) {
        super(button, BUTTON_BINDINGS);
    }

    public ButtonBehavior(final C button, final List<KeyBinding> bindings) {
        super(button, bindings);
    }

    /***************************************************************************
     *                                                                         *
     * Focus change handling                                                   *
     *                                                                         *
     **************************************************************************/

    @Override protected void focusChanged() {
        // If we did have the key down, but are now not focused, then we must
        // disarm the button.
        final ButtonBase button = getControl();
        if (keyDown && !button.isFocused()) {
            keyDown = false;
            button.disarm();
        }
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * Indicates that a keyboard key has been pressed which represents the
     * event (this could be space bar for example). As long as keyDown is true,
     * we are also armed, and will ignore mouse events related to arming.
     * Note this is made package private solely for the sake of testing.
     */
    private boolean keyDown;

    private static final String PRESS_ACTION = "Press";
    private static final String RELEASE_ACTION = "Release";

    protected static final List<KeyBinding> BUTTON_BINDINGS = new ArrayList<KeyBinding>();
    static {
            BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, PRESS_ACTION));
            BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, RELEASE_ACTION));
    }

    @Override protected void callAction(String name) {
        if (!getControl().isDisabled()) {
            if (PRESS_ACTION.equals(name)) {
                keyPressed();
            } else if (RELEASE_ACTION.equals(name)) {
                keyReleased();
            } else {
                super.callAction(name);
            }
        }
    }

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this button to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed() {
        final ButtonBase button = getControl();
        if (! button.isPressed() && ! button.isArmed()) {
            keyDown = true;
            button.arm();
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased() {
        final ButtonBase button = getControl();
        if (keyDown) {
            keyDown = false;
            if (button.isArmed()) {
                button.disarm();
                button.fire();
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    /**
     * Invoked when a mouse press has occurred over the button. In addition to
     * potentially arming the Button, this will transfer focus to the button
     */
    @Override public void mousePressed(MouseEvent e) {
        final ButtonBase button = getControl();
        super.mousePressed(e);
        // if the button is not already focused, then request the focus
        if (! button.isFocused() && button.isFocusTraversable()) {
            button.requestFocus();
        }

        // arm the button if it is a valid mouse event
        // Note there appears to be a bug where if I press and hold and release
        // then there is a clickCount of 0 on the release, whereas a quick click
        // has a release clickCount of 1. So here I'll check clickCount <= 1,
        // though it should really be == 1 I think.
        boolean valid = (e.getButton() == MouseButton.PRIMARY &&
            ! (e.isMiddleButtonDown() || e.isSecondaryButtonDown() ||
             e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()));

        if (! button.isArmed() && valid) {
            button.arm();
        }
    }

    /**
     * Invoked when a mouse release has occurred. We determine whether this
     * was done in a manner that would fire the button's action. This happens
     * only if the button was armed by a corresponding mouse press.
     */
    @Override public void mouseReleased(MouseEvent e) {
        // if armed by a mouse press instead of key press, then fire!
        final ButtonBase button = getControl();
        if (! keyDown && button.isArmed()) {
            button.fire();
            button.disarm();
        }
    }

    /**
     * Invoked when the mouse enters the Button. If the Button had been armed
     * by a mouse press and the mouse is still pressed, then this will cause
     * the button to be rearmed.
     */
    @Override public void mouseEntered(MouseEvent e) {
        // rearm if necessary
        final ButtonBase button = getControl();
        super.mouseEntered(e);
        if (! keyDown && button.isPressed()) {
            button.arm();
        }
    }

    /**
     * Invoked when the mouse exits the Button. If the Button is armed due to
     * a mouse press, then this function will disarm the button upon the mouse
     * exiting it.
     */
    @Override public void mouseExited(MouseEvent e) {
        // Disarm if necessary
        final ButtonBase button = getControl();
        super.mouseExited(e);
        if (! keyDown && button.isArmed()) {
            button.disarm();
        }
    }
}
