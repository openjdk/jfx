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

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SelectionModel;
import com.sun.javafx.scene.control.skin.Utils;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.KeyCode.*;

import static com.sun.javafx.scene.control.inputmap.InputMap.*;

/**
 * ChoiceBoxBehavior - default implementation
 */
public class ChoiceBoxBehavior<T> extends BehaviorBase<ChoiceBox<T>> {

    private final InputMap<ChoiceBox<T>> choiceBoxInputMap;

    private TwoLevelFocusComboBehavior tlFocus;

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/

    public ChoiceBoxBehavior(ChoiceBox<T> control) {
        super(control);

        // create a map for choiceBox-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        choiceBoxInputMap = createInputMap();

        // choiceBox-specific mappings for key and mouse input
        addDefaultMapping(choiceBoxInputMap,
            new KeyMapping(SPACE, KeyEvent.KEY_PRESSED, this::keyPressed),
            new KeyMapping(SPACE, KeyEvent.KEY_RELEASED, this::keyReleased),

            new KeyMapping(ESCAPE, KeyEvent.KEY_RELEASED, e -> cancel()),
            new KeyMapping(DOWN, KeyEvent.KEY_RELEASED, e -> showPopup()),
            new KeyMapping(CANCEL, KeyEvent.KEY_RELEASED, e -> cancel()),

            new MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed),
            new MouseMapping(MouseEvent.MOUSE_RELEASED, this::mouseReleased)
        );

        // add some special two-level focus mappings
        InputMap<ChoiceBox<T>> twoLevelFocusInputMap = new InputMap<>(control);
        twoLevelFocusInputMap.setInterceptor(e -> !Utils.isTwoLevelFocus());
        twoLevelFocusInputMap.getMappings().addAll(
            new KeyMapping(ENTER, KeyEvent.KEY_PRESSED, this::keyPressed),
            new KeyMapping(ENTER, KeyEvent.KEY_RELEASED, this::keyReleased)
        );
        addDefaultChildMap(choiceBoxInputMap, twoLevelFocusInputMap);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusComboBehavior(control); // needs to be last.
        }
    }

    @Override public InputMap<ChoiceBox<T>> getInputMap() {
        return choiceBoxInputMap;
    }

    @Override public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        super.dispose();
    }

    public void select(int index) {
        SelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        sm.select(index);
    }

    public void close() {
        getNode().hide();
    }

    public void showPopup() {
        getNode().show();
    }

    /**
     * Invoked when a mouse press has occurred over the box. In addition to
     * potentially arming the Button, this will transfer focus to the box
     */
    public void mousePressed(MouseEvent e) {
        ChoiceBox<T> choiceButton = getNode();
        if (choiceButton.isFocusTraversable()) choiceButton.requestFocus();
    }

    /**
     * Invoked when a mouse release has occurred. We determine whether this
     * was done in a manner that would fire the box's action. This happens
     * only if the box was armed by a corresponding mouse press.
     */
    public void mouseReleased(MouseEvent e) {
        ChoiceBox<T> choiceButton = getNode();
        if (choiceButton.isShowing() || !choiceButton.contains(e.getX(), e.getY())) {
            choiceButton.hide(); // hide if already showing
        }
        else if (e.getButton() == MouseButton.PRIMARY) {
            choiceButton.show();
        }
    }

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this box to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed(KeyEvent e) {
        ChoiceBox<T> choiceButton = getNode();
        if (!choiceButton.isShowing()) {
            choiceButton.show();
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the box
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased(KeyEvent e) {
    }

    // no-op
    /**
     * Invoked when "escape" key is released
     */
    public void cancel() {
        ChoiceBox<T> choiceButton = getNode();
        choiceButton.hide();
    }

}
