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

import javafx.scene.control.SplitMenuButton;
import com.sun.javafx.scene.control.inputmap.InputMap;

import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

/**
 * Behavior for SplitMenuButton.
 */
public class SplitMenuButtonBehavior extends MenuButtonBehaviorBase<SplitMenuButton> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SplitMenuButtonBehavior for the given SplitMenuButton.
     *
     * @param splitMenuButton the SplitMenuButton
     */
    public SplitMenuButtonBehavior(final SplitMenuButton splitMenuButton) {
        super(splitMenuButton);

        /**
         * The key bindings for the SplitMenuButton. Sets the Enter key as the means
         * to open the menu and the space key as the means to activate the action.
         */
        addDefaultMapping(new InputMap.KeyMapping(SPACE, KEY_PRESSED, this::keyPressed));
        addDefaultMapping(new InputMap.KeyMapping(SPACE, KEY_RELEASED, this::keyReleased));
        addDefaultMapping(new InputMap.KeyMapping(ENTER, KEY_PRESSED, this::keyPressed));
        addDefaultMapping(new InputMap.KeyMapping(ENTER, KEY_RELEASED, this::keyReleased));
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

//    /**
//     * The key bindings for the SplitMenuButton. Sets the Enter key as the means
//     * to open the menu and the space key as the means to activate the action.
//     */
//    protected static final List<KeyBinding> SPLIT_MENU_BUTTON_BINDINGS = new ArrayList<KeyBinding>();
//    static {
//        SPLIT_MENU_BUTTON_BINDINGS.addAll(BASE_MENU_BUTTON_BINDINGS);
//
//        SPLIT_MENU_BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, "Press"));
//        SPLIT_MENU_BUTTON_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, "Release"));
//
//        SPLIT_MENU_BUTTON_BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED,  "Press"));
//        SPLIT_MENU_BUTTON_BINDINGS.add(new KeyBinding(ENTER, KEY_RELEASED, "Release"));
//    }
}
