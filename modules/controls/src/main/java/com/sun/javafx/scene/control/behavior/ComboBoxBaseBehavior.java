/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

public class ComboBoxBaseBehavior<T> extends BehaviorBase<ComboBoxBase<T>> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    private TwoLevelFocusComboBehavior tlFocus;
    
    /**
     * 
     */
    public ComboBoxBaseBehavior(final ComboBoxBase<T> comboBox, final List<KeyBinding> bindings) {
        super(comboBox, bindings);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (com.sun.javafx.scene.control.skin.Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusComboBehavior(comboBox); // needs to be last.
        }
    }

    @Override public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        super.dispose();
    }

    /***************************************************************************
     *                                                                         *
     * Focus change handling                                                   *
     *                                                                         *
     **************************************************************************/

    @Override protected void focusChanged() {
        // If we did have the key down, but are now not focused, then we must
        // disarm the box.
        final ComboBoxBase<T> box = getControl();
        if (keyDown && !box.isFocused()) {
            keyDown = false;
            box.disarm();
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

    protected static final List<KeyBinding> COMBO_BOX_BASE_BINDINGS = new ArrayList<KeyBinding>();
    static {
        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(F4, KEY_RELEASED, "togglePopup"));
        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(UP, "togglePopup").alt());
        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(DOWN, "togglePopup").alt());

        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, PRESS_ACTION));
        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, RELEASE_ACTION));

        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED, PRESS_ACTION));
        COMBO_BOX_BASE_BINDINGS.add(new KeyBinding(ENTER, KEY_RELEASED, RELEASE_ACTION));
    }

    @Override protected void callActionForEvent(KeyEvent e) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;
        super.callActionForEvent(e);
    }

    @Override protected void callAction(String name) {
        if (PRESS_ACTION.equals(name)) {
            keyPressed();
        } else if (RELEASE_ACTION.equals(name)) {
            keyReleased();
        } else if ("showPopup".equals(name)) {
            show();
        } else if ("togglePopup".equals(name)) {
            if (getControl().isShowing()) hide();
            else show();
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
        if (com.sun.javafx.scene.control.skin.Utils.isTwoLevelFocus()) {
            show();
            if (tlFocus != null) {
                tlFocus.setExternalFocus(false);
            }
        }
        else {
            if (! getControl().isPressed() && ! getControl().isArmed()) {
                keyDown = true;
                getControl().arm();
            }
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased() {
        if (!com.sun.javafx.scene.control.skin.Utils.isTwoLevelFocus()) {
            if (keyDown) {
                keyDown = false;
                if (getControl().isArmed()) {
                    getControl().disarm();
                }
            }
        }
    }
    
    
    
    /**************************************************************************
     *                                                                        *
     * Mouse Events                                                           *
     *                                                                        *
     *************************************************************************/

    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        arm(e);
    }

    @Override public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);

        disarm();

        // The showPopupOnMouseRelease boolean was added to resolve
        // RT-18151: namely, clicking on the comboBox button shouldn't hide, 
        // and then immediately show the popup, which was occurring because we
        // can't know whether the popup auto-hide was coming because of a MOUSE_PRESS
        // since PopupWindow calls hide() before it calls onAutoHide().
        if (showPopupOnMouseRelease) {
            show();
        } else {
            showPopupOnMouseRelease = true;
            hide();
        }
    }

    @Override public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);

        if (!getControl().isEditable()) {
            mouseInsideButton = true;
        } else {
            // This is strongly tied to ComboBoxBaseSkin
            final EventTarget target = e.getTarget();
            mouseInsideButton = (target instanceof Node && "arrow-button".equals(((Node) target).getId()));
        }
        arm();
    }

    @Override public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        mouseInsideButton = false;
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
    
    public void show() {
        if (! getControl().isShowing()) {
            getControl().requestFocus();
            getControl().show();
        }
    }
    
    public void hide() {
        if (getControl().isShowing()) {
            getControl().hide();
        }
    }

    private boolean showPopupOnMouseRelease = true;
    private boolean mouseInsideButton = false;
    public void onAutoHide() {
        // RT-18151: if the ComboBox button was clicked, and it was this that forced the
        // popup to disappear, we don't want the popup to immediately reappear.
        // If the mouse was not within the comboBox button at the time of the auto-hide occurring,
        // then showPopupOnMouseRelease returns to its default of true; otherwise, it toggles.
        // Note that this logic depends on popup.setAutoHide(true) in ComboBoxPopupControl
        hide();
        showPopupOnMouseRelease = mouseInsideButton ? !showPopupOnMouseRelease : true;
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

}
