/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.beans.Observable;
import javafx.scene.control.ButtonBase;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import static com.sun.javafx.scene.control.inputmap.InputMap.*;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.SPACE;

/**
 * All of the "button" types (CheckBox, RadioButton, ToggleButton, and Button)
 * and also maybe some other types like hyperlinks operate on the "armed"
 * selection strategy, just like JButton. This behavior class encapsulates that
 * logic in a way that can be reused and extended by each of the individual
 * class behaviors.
 *
 */
public class ButtonBehavior<C extends ButtonBase> extends BehaviorBase<C> {
    private final InputMap<C> buttonInputMap;

    /**
     * Indicates that a keyboard key has been pressed which represents the
     * event (this could be space bar for example). As long as keyDown is true,
     * we are also armed, and will ignore mouse events related to arming.
     * Note this is made package private solely for the sake of testing.
     */
    private boolean keyDown;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ButtonBehavior(C control) {
        super(control);

        // create a map for button-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        buttonInputMap = createInputMap();

        // add focus traversal mappings
        addDefaultMapping(buttonInputMap, FocusTraversalInputMap.getFocusTraversalMappings());

        // then button-specific mappings for key and mouse input
        addDefaultMapping(buttonInputMap,
            new KeyMapping(SPACE, KeyEvent.KEY_PRESSED, this::keyPressed),
            new KeyMapping(SPACE, KeyEvent.KEY_RELEASED, this::keyReleased),
            new MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed),
            new MouseMapping(MouseEvent.MOUSE_RELEASED, this::mouseReleased),
            new MouseMapping(MouseEvent.MOUSE_ENTERED, this::mouseEntered),
            new MouseMapping(MouseEvent.MOUSE_EXITED, this::mouseExited),

            // on non-Mac OS platforms, we support pressing the ENTER key to activate the button
            new KeyMapping(new KeyBinding(ENTER, KeyEvent.KEY_PRESSED), this::keyPressed, event -> PlatformUtil.isMac()),
            new KeyMapping(new KeyBinding(ENTER, KeyEvent.KEY_RELEASED), this::keyReleased, event -> PlatformUtil.isMac())
        );

        // Button also cares about focus
        control.focusedProperty().addListener(this::focusChanged);
    }



    /***************************************************************************
     *                                                                         *
     * Implementation of BehaviorBase API                                      *
     *                                                                         *
     **************************************************************************/

    @Override public InputMap<C> getInputMap() {
        return buttonInputMap;
    }

    @Override public void dispose() {
        super.dispose();

        // TODO
        getNode().focusedProperty().removeListener(this::focusChanged);
    }



    /***************************************************************************
     *                                                                         *
     * Focus change handling                                                   *
     *                                                                         *
     **************************************************************************/

    private void focusChanged(Observable o) {
        // If we did have the key down, but are now not focused, then we must
        // disarm the button.
        final ButtonBase button = getNode();
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
     * This function is invoked when an appropriate keystroke occurs which
     * causes this button to be armed if it is not already armed by a mouse
     * press.
     */
    protected void keyPressed(KeyEvent e) {
        if (! getNode().isPressed() && ! getNode().isArmed()) {
            keyDown = true;
            getNode().arm();
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    protected void keyReleased(KeyEvent e) {
        if (keyDown) {
            keyDown = false;
            if (getNode().isArmed()) {
                getNode().disarm();
                getNode().fire();
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
    protected void mousePressed(MouseEvent e) {
        // if the button is not already focused, then request the focus
        if (! getNode().isFocused() && getNode().isFocusTraversable()) {
            getNode().requestFocus();
        }

        // arm the button if it is a valid mouse event
        // Note there appears to be a bug where if I press and hold and release
        // then there is a clickCount of 0 on the release, whereas a quick click
        // has a release clickCount of 1. So here I'll check clickCount <= 1,
        // though it should really be == 1 I think.
        boolean valid = (e.getButton() == MouseButton.PRIMARY &&
                ! (e.isMiddleButtonDown() || e.isSecondaryButtonDown() ||
                        e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()));

        if (! getNode().isArmed() && valid) {
            getNode().arm();
        }
    }

    /**
     * Invoked when a mouse release has occurred. We determine whether this
     * was done in a manner that would fire the button's action. This happens
     * only if the button was armed by a corresponding mouse press.
     */
    protected void mouseReleased(MouseEvent e) {
        // if armed by a mouse press instead of key press, then fire!
        if (! keyDown && getNode().isArmed()) {
            getNode().fire();
            getNode().disarm();
        }
    }

    /**
     * Invoked when the mouse enters the Button. If the Button had been armed
     * by a mouse press and the mouse is still pressed, then this will cause
     * the button to be rearmed.
     */
    protected void mouseEntered(MouseEvent e) {
        // rearm if necessary
        if (! keyDown && getNode().isPressed()) {
            getNode().arm();
        }
    }

    /**
     * Invoked when the mouse exits the Button. If the Button is armed due to
     * a mouse press, then this function will disarm the button upon the mouse
     * exiting it.
     */
    protected void mouseExited(MouseEvent e) {
        // Disarm if necessary
        if (! keyDown && getNode().isArmed()) {
            getNode().disarm();
        }
    }
}
