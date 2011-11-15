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

import com.javafx.preview.control.ComboBox;
import javafx.beans.Observable;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;

import com.sun.javafx.Utils;

import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;

public class ComboBoxBehavior<T> extends BehaviorBase<ComboBox<T>> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    public ComboBoxBehavior(final ComboBox<T> comboBox) {
        super(comboBox);
        InvalidationListener focusListener = new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                // If we did have the key down, but are now not focused, then we must
                // disarm the button.
                if (keyDown && !getControl().isFocused()) {
                    keyDown = false;
                    getControl().disarm();
                }
            }
        };
        getControl().focusedProperty().addListener(focusListener);
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
        BUTTON_BINDINGS.addAll(TRAVERSAL_BINDINGS);
        if (Utils.isWindows()) {
            BUTTON_BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED, PRESS_ACTION));
            BUTTON_BINDINGS.add(new KeyBinding(ENTER, KEY_RELEASED, RELEASE_ACTION));
            BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, PRESS_ACTION));
            BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, RELEASE_ACTION));
        } else {
            BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, PRESS_ACTION));
            BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, RELEASE_ACTION));
        }
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return BUTTON_BINDINGS;
    }

    @Override protected void callAction(String name) {
        if (PRESS_ACTION.equals(name)) {
            keyPressed();
        } else if (RELEASE_ACTION.equals(name)) {
            keyReleased();
        } else {
            super.callAction(name);
        }
    }

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this button to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed() {
        if (! getControl().isPressed() && ! getControl().isArmed()) {
            keyDown = true;
            getControl().arm();
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased() {
        if (keyDown) {
            keyDown = false;
            if (getControl().isArmed()) {
                getControl().disarm();
            }
        }
    }
    
    
    
    /**************************************************************************
     *                                                                        *
     * Mouse Events                                                           *
     *                                                                        *
     *************************************************************************/
 
    public void arrowPressed(MouseEvent e) {
        getFocus();
        arm(e);
    }
    
    public void arrowReleased(MouseEvent e) {
        disarm();
        
        Point2D p = ((Node)e.getSource()).localToParent(e.getX(), e.getY());
        if (getControl().isShowing()) {
            getControl().hide();
        } else if (getControl().contains(p.getX(), p.getY())) {
            getControl().show();
        }
    }
    
    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        getFocus();
        arm(e);
    }
    
    @Override public void mouseReleased(MouseEvent e) {
        super.mousePressed(e);
        
        disarm();
        
        if (getControl().isShowing()) {
            hide();
        } else if (getControl().contains(e.getX(), e.getY())) {
            getControl().show();
        }
    }

    @Override public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        arm();
    }

    @Override public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        disarm();
    }
    
    private void getFocus() {
        if (! getControl().isFocused() && getControl().isFocusTraversable()) {
            getControl().requestFocus();
        }
    }
    
    private void arm(MouseEvent e) {
        boolean valid = (e.getButton() == MouseButton.PRIMARY &&
            ! (e.isMiddleButtonDown() || e.isSecondaryButtonDown() ||
             e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()));
        
        if (! getControl().isArmed() && valid) {
            getControl().arm();
        }
    }
    
    public void hide() {
        if (getControl().isShowing()) {
            getControl().hide();
        }
    }
    
    public void arm() {
        if (getControl().isPressed()) {
            getControl().arm();
        }
    }
    
    public void disarm() {
        if (! keyDown && getControl().isArmed()) {
            getControl().disarm();
        }
    }
    
    
    /**************************************************************************
     *                                                                        *
     * Key Events                                                             *
     *                                                                        *
     *************************************************************************/
//    protected static final List<KeyBinding> BUTTON_BINDINGS = new ArrayList<KeyBinding>();
//    static {
//        BUTTON_BINDINGS.addAll(TRAVERSAL_BINDINGS);
//        
//        BUTTON_BINDINGS.add(new KeyBinding(KeyCode.ENTER, "EnterPressed"));
//    }
//
//    @Override protected List<KeyBinding> createKeyBindings() {
//        return BUTTON_BINDINGS;
//    }
//
//    @Override protected void callAction(String name) {
//        if ("EnterPressed".equals(name)) {
//            enterPressed();
//        } else {
//            super.callAction(name);
//        }
//    }
//    
//    private void enterPressed() {
//        getControl().fire();
//    }
}
