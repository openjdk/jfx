/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.scene.control.input.BehaviorBase;
import javafx.scene.control.input.KeyBinding;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import com.sun.javafx.scene.control.skin.Utils;

public class ComboBoxBaseBehavior<T> extends BehaviorBase<ComboBoxBase<T>> {

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
    public ComboBoxBaseBehavior(ComboBoxBase<T> c) {
        super(c);
    }

    @Override
    protected void populateSkinInputMap() {
        // ComboBoxBase also cares about focus
        getControl().focusedProperty().addListener(focusListener);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusComboBehavior(getControl()); // needs to be last.
        }

        registerFunction(ComboBoxBase.TOGGLE_POPUP, this::togglePopup);

        registerKey(KeyBinding.builder(KeyCode.F4).keyReleased().build(), ComboBoxBase.TOGGLE_POPUP);
        registerKey(KeyBinding.alt(KeyCode.DOWN), ComboBoxBase.TOGGLE_POPUP);
        registerKey(KeyBinding.alt(KeyCode.UP), ComboBoxBase.TOGGLE_POPUP);

        addHandler(KeyBinding.of(KeyCode.SPACE), (ev) -> keyPressed(ev, true));
        addHandler(KeyBinding.builder(KeyCode.SPACE).keyReleased().build(), (ev) -> keyReleased(ev, true));

        // these two should not consume the event
        addHandler(KeyBinding.of(KeyCode.ENTER), (ev) -> keyPressed(ev, false));
        addHandler(KeyBinding.builder(KeyCode.ENTER).keyReleased().build(), (ev) -> keyReleased(ev, false));

        addHandler(KeyBinding.of(KeyCode.ESCAPE), this::cancelEdit);
        addHandler(KeyBinding.of(KeyCode.F10), this::forwardToParent);

        addHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        addHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
        addHandler(MouseEvent.MOUSE_ENTERED, this::mouseEntered);
        addHandler(MouseEvent.MOUSE_EXITED, this::mouseExited);
    }

    public void dispose() {
        if (tlFocus != null) {
            tlFocus.dispose();
        }
        getControl().focusedProperty().removeListener(focusListener);
    }

    /***************************************************************************
     *                                                                         *
     * Focus change handling                                                   *
     *                                                                         *
     **************************************************************************/

    protected void focusChanged(Observable o) {
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

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this button to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed(KeyEvent e, boolean consume) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (Utils.isTwoLevelFocus()) {
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
        if (consume) {
            e.consume();
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased(KeyEvent e, boolean consume) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (!Utils.isTwoLevelFocus()) {
            if (keyDown) {
                keyDown = false;
                if (getControl().isArmed()) {
                    getControl().disarm();
                }
            }
        }
        if (consume) {
            e.consume();
        }
    }

    private void forwardToParent(KeyEvent event) {
        try {
            if (getControl().getParent() != null) {
                getControl().getParent().fireEvent(event);
            }
        } finally {
            // TODO original logic is to always consume the event.
            // we may want to change that
            event.consume();
        }
    }

    private void cancelEdit(KeyEvent event) {
        /**
         * This can be cleaned up if the editor property is moved up
         * to ComboBoxBase.
         */
        ComboBoxBase comboBoxBase = getControl();
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
        event.consume();
    }


    /**************************************************************************
     *                                                                        *
     * Mouse Events                                                           *
     *                                                                        *
     *************************************************************************/

    public void mousePressed(MouseEvent e) {
        arm(e);
        e.consume();
    }

    public void mouseReleased(MouseEvent e) {
        disarm();

        // The showPopupOnMouseRelease boolean was added to resolve
        // JDK-8118434: namely, clicking on the comboBox button shouldn't hide,
        // and then immediately show the popup, which was occurring because we
        // can't know whether the popup auto-hide was coming because of a MOUSE_PRESS
        // since PopupWindow calls hide() before it calls onAutoHide().
        if (showPopupOnMouseRelease) {
            show();
        } else {
            showPopupOnMouseRelease = true;
            hide();
        }
        e.consume();
    }

    public void mouseEntered(MouseEvent e) {
        if (!getControl().isEditable()) {
            mouseInsideButton = true;
        } else {
            // This is strongly tied to ComboBoxBaseSkin
            final EventTarget target = e.getTarget();
            mouseInsideButton = (target instanceof Node && "arrow-button".equals(((Node) target).getId()));
        }
        arm();
        e.consume();
    }

    public void mouseExited(MouseEvent e) {
        mouseInsideButton = false;
        disarm();
        e.consume();
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

        if (! getControl().isArmed() && valid) {
            getControl().arm();
        }
    }

    public void show() {
        if (! getControl().isShowing()) {
            if (getControl().isFocusTraversable()) {
                getControl().requestFocus();
            }
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
    public void onAutoHide(PopupControl popup) {
        // JDK-8118434: if the ComboBox button was clicked, and it was this that forced the
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

    private void togglePopup() {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (getControl().isShowing()) {
            hide();
        } else {
            show();
        }
    }
}
