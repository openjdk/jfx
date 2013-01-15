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

import static javafx.scene.input.KeyCode.CANCEL;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.KeyCode.DOWN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.MouseEvent;

import javafx.css.PseudoClass;
import com.sun.javafx.scene.control.skin.Utils;

/**
 * ChoiceBoxBehavior - default implementation
 *
 * @profile common
 */
public class ChoiceBoxBehavior<T> extends BehaviorBase<ChoiceBox<T>> {
    /**
     * The key bindings for the ChoiceBox. It seems this should really be the
     * same as with the ButtonBehavior super class, but it doesn't handle ENTER
     * events on desktop, whereas this does. It may be a proper analysis of the
     * interaction logic would allow us to share bindings, but for now, we simply
     * build it up specially here.
     */
    protected static final List<KeyBinding> CHOICE_BUTTON_BINDINGS = new ArrayList<KeyBinding>();
    static {
        CHOICE_BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, "Press"));
        CHOICE_BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, "Release"));

        if (Utils.isEmbeddedNonTouch()) {
            CHOICE_BUTTON_BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED, "Press"));
            CHOICE_BUTTON_BINDINGS.add(new KeyBinding(ENTER, KEY_RELEASED, "Release"));
        }

        CHOICE_BUTTON_BINDINGS.add(new KeyBinding(ESCAPE, KEY_RELEASED, "Cancel"));
        CHOICE_BUTTON_BINDINGS.add(new KeyBinding(DOWN, KEY_RELEASED, "Down"));
        CHOICE_BUTTON_BINDINGS.add(new KeyBinding(CANCEL, KEY_RELEASED, "Cancel"));

    }

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    @Override protected void callAction(String name) {
        if (name.equals("Cancel")) cancel();
        else if (name.equals("Press")) keyPressed();
        else if (name.equals("Release")) keyReleased();
        else if (name.equals("Down")) showPopup();
        else super.callAction(name);
    }

    private TwoLevelFocusComboBehavior tlFocus;

    public ChoiceBoxBehavior(ChoiceBox control) {
        super(control);
        /*
        ** only add this if we're on an embedded
        ** platform that supports 5-button navigation 
        */
        if (Utils.isEmbeddedNonTouch()) {
            tlFocus = new TwoLevelFocusComboBehavior(control); // needs to be last.
        }
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return CHOICE_BUTTON_BINDINGS;
    }

    public void select(int index) {
        SelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        sm.select(index);
    }

    public void close() {
        getControl().hide();
    }

    public void showPopup() {
        getControl().show();
    }

    /**
     * Invoked when a mouse press has occurred over the box. In addition to
     * potentially arming the Button, this will transfer focus to the box
     */
    @Override public void mousePressed(MouseEvent e) {
        ChoiceBox choiceButton = getControl();
        super.mousePressed(e);
        if (choiceButton.isFocusTraversable()) choiceButton.requestFocus();
    }

    /**
     * Invoked when a mouse release has occurred. We determine whether this
     * was done in a manner that would fire the box's action. This happens
     * only if the box was armed by a corresponding mouse press.
     */
    @Override public void mouseReleased(MouseEvent e) {
        ChoiceBox choiceButton = getControl();
        super.mouseReleased(e);
        if (choiceButton.isShowing() || !choiceButton.contains(e.getX(), e.getY())) {
            choiceButton.hide(); // hide if already showing 
        }
        else {
            choiceButton.show();
        }
    }

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this box to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed() {
        ChoiceBox choiceButton = getControl();
        if (!choiceButton.isShowing()) {
            choiceButton.show();
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the box
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased() {
    }

    // no-op
    /**
     * Invoked when "escape" key is released
     */
    public void cancel() {
        ChoiceBox choiceButton = getControl();
        choiceButton.hide();
    }

}
