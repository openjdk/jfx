/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl.Cmd;
import javafx.scene.control.input.FunctionTag;
import javafx.scene.control.input.KeyBinding2;
import javafx.scene.control.input.KeyMap;
import javafx.scene.control.skin.ComboBoxBaseSkin;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import com.sun.javafx.scene.control.inputmap.InputMap.MouseMapping;
import com.sun.javafx.scene.control.skin.Utils;

public class ComboBoxBaseBehavior<T> extends BehaviorBase<ComboBoxBase<T>> {

    private final InputMap<ComboBoxBase<T>> inputMap;
    private InvalidationListener focusListener = this::focusChanged;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    private TwoLevelFocusComboBehavior tlFocus;

    /**
     *
     */
    public ComboBoxBaseBehavior(final ComboBoxBase<T> comboBox) {
        super(comboBox);

        // create a map for comboBox-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap();

        addKeyMap(comboBox);

        // comboBox-specific mappings for key and mouse input
        KeyMapping enterPressed, enterReleased;
        addDefaultMapping(inputMap,
            new KeyMapping(SPACE, KEY_PRESSED, this::keyPressed),
            new KeyMapping(SPACE, KEY_RELEASED, this::keyReleased),

            enterPressed = new KeyMapping(ENTER, KEY_PRESSED, this::keyPressed),
            enterReleased = new KeyMapping(ENTER, KEY_RELEASED, this::keyReleased),

            // The following keys are forwarded to the parent container
            new KeyMapping(ESCAPE, KEY_PRESSED, this::cancelEdit),
            new KeyMapping(F10,    KEY_PRESSED, this::forwardToParent),

            new MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed),
            new MouseMapping(MouseEvent.MOUSE_RELEASED, this::mouseReleased),
            new MouseMapping(MouseEvent.MOUSE_ENTERED, this::mouseEntered),
            new MouseMapping(MouseEvent.MOUSE_EXITED, this::mouseExited)
        );

        // we don't want to consume events on enter press - let them carry on through
        enterPressed.setAutoConsume(false);
        enterReleased.setAutoConsume(false);

        // ComboBoxBase also cares about focus
        comboBox.focusedProperty().addListener(focusListener);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusComboBehavior(comboBox); // needs to be last.
        }
    }

    // TODO move to control
    enum Cmd implements FunctionTag {
        //CANCEL_EDIT, // TODO forwards to parent, child class logic in the base class, looks poorly thought out
        TOGGLE_POPUP
    }
    
    public void install() {
        KeyMap m = getNode().getKeyMap();
        
        //m.func(s, Cmd.CANCEL_EDIT, this::cancelEdit);
        m.func(this, Cmd.TOGGLE_POPUP, this::togglePopup);
        
        m.key(this, KeyBinding2.withRelease(F4).build(), Cmd.TOGGLE_POPUP);
        m.key(this, KeyBinding2.alt(DOWN), Cmd.TOGGLE_POPUP);
        m.key(this, KeyBinding2.alt(UP), Cmd.TOGGLE_POPUP);
    }

    @Override public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        getNode().focusedProperty().removeListener(focusListener);
        super.dispose();
    }

    @Override public InputMap<ComboBoxBase<T>> getInputMap() {
        return inputMap;
    }

    /***************************************************************************
     *                                                                         *
     * Focus change handling                                                   *
     *                                                                         *
     **************************************************************************/

    protected void focusChanged(Observable o) {
        // If we did have the key down, but are now not focused, then we must
        // disarm the box.
        final ComboBoxBase<T> box = getNode();
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

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this button to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed(KeyEvent e) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (Utils.isTwoLevelFocus()) {
            show();
            if (tlFocus != null) {
                tlFocus.setExternalFocus(false);
            }
        }
        else {
            if (! getNode().isPressed() && ! getNode().isArmed()) {
                keyDown = true;
                getNode().arm();
            }
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased(KeyEvent e) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (!Utils.isTwoLevelFocus()) {
            if (keyDown) {
                keyDown = false;
                if (getNode().isArmed()) {
                    getNode().disarm();
                }
            }
        }
    }

    private void forwardToParent(KeyEvent event) {
        if (getNode().getParent() != null) {
            getNode().getParent().fireEvent(event);
        }
    }

    private void cancelEdit(KeyEvent event) {
        /**
         * This can be cleaned up if the editor property is moved up
         * to ComboBoxBase.
         */
        ComboBoxBase comboBoxBase = getNode();
        TextField textField = null;
        if (comboBoxBase instanceof DatePicker) {
            textField = ((DatePicker)comboBoxBase).getEditor();
        } else if (comboBoxBase instanceof ComboBox) {
            textField = comboBoxBase.isEditable() ? ((ComboBox)comboBoxBase).getEditor() : null;
        }

        if (textField != null && textField.getTextFormatter() != null) {
            textField.cancelEdit();
        } else {
            forwardToParent(event);
        }
    }


    /**************************************************************************
     *                                                                        *
     * Mouse Events                                                           *
     *                                                                        *
     *************************************************************************/

    public void mousePressed(MouseEvent e) {
        arm(e);
    }

    public void mouseReleased(MouseEvent e) {
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

    public void mouseEntered(MouseEvent e) {
        if (!getNode().isEditable()) {
            mouseInsideButton = true;
        } else {
            // This is strongly tied to ComboBoxBaseSkin
            final EventTarget target = e.getTarget();
            mouseInsideButton = (target instanceof Node && "arrow-button".equals(((Node) target).getId()));
        }
        arm();
    }

    public void mouseExited(MouseEvent e) {
        mouseInsideButton = false;
        disarm();
    }

//    private void getFocus() {
//        if (! getNode().isFocused() && getNode().isFocusTraversable()) {
//            getNode().requestFocus();
//        }
//    }

    private void arm(MouseEvent e) {
        boolean valid = (e.getButton() == MouseButton.PRIMARY &&
            ! (e.isMiddleButtonDown() || e.isSecondaryButtonDown() ||
             e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()));

        if (! getNode().isArmed() && valid) {
            getNode().arm();
        }
    }

    public void show() {
        if (! getNode().isShowing()) {
            if (getNode().isFocusTraversable()) {
                getNode().requestFocus();
            }
            getNode().show();
        }
    }

    public void hide() {
        if (getNode().isShowing()) {
            getNode().hide();
        }
    }

    private boolean showPopupOnMouseRelease = true;
    private boolean mouseInsideButton = false;
    public void onAutoHide(PopupControl popup) {
        // RT-18151: if the ComboBox button was clicked, and it was this that forced the
        // popup to disappear, we don't want the popup to immediately reappear.
        // If the mouse was not within the comboBox button at the time of the auto-hide occurring,
        // then showPopupOnMouseRelease returns to its default of true; otherwise, it toggles.
        // Note that this logic depends on popup.setAutoHide(true) in ComboBoxPopupControl
        hide();
        showPopupOnMouseRelease = mouseInsideButton ? !showPopupOnMouseRelease : true;
    }

    public void arm() {
        if (getNode().isPressed()) {
            getNode().arm();
        }
    }

    public void disarm() {
        if (! keyDown && getNode().isArmed()) {
            getNode().disarm();
        }
    }

    private void togglePopup() {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (getNode().isShowing()) {
            hide();
        } else {
            show();
        }
    }
}
