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

import javafx.scene.control.TitledPane;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.MouseEvent;

import static javafx.scene.input.KeyCode.SPACE;

public class TitledPaneBehavior extends BehaviorBase<TitledPane> {

    private final TitledPane titledPane;
    private final InputMap<TitledPane> inputMap;

    public TitledPaneBehavior(TitledPane pane) {
        super(pane);
        this.titledPane = pane;

        // create a map for titledPane-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap();

        // add focus traversal mappings
        addDefaultMapping(inputMap, FocusTraversalInputMap.getFocusTraversalMappings());

        // ENTER should not be a key binding for TitledPane, as this is the
        // key reserved for the default button. See RT-40166 for more detail.
        addDefaultMapping(
            new InputMap.KeyMapping(SPACE, e -> {
                if (titledPane.isCollapsible() && titledPane.isFocused()) {
                    titledPane.setExpanded(!titledPane.isExpanded());
                    titledPane.requestFocus();
                }
            }),
            new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed)
        );
    }

    @Override public InputMap<TitledPane> getInputMap() {
        return inputMap;
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

//    private static final String PRESS_ACTION = "Press";
//
//    protected static final List<KeyBinding> TITLEDPANE_BINDINGS = new ArrayList<KeyBinding>();
//    static {
//        // ENTER should not be a key binding for TitledPane, as this is the
//        // key reserved for the default button. See RT-40166 for more detail.
//        // TITLEDPANE_BINDINGS.add(new KeyBinding(ENTER, PRESS_ACTION));
//        TITLEDPANE_BINDINGS.add(new KeyBinding(SPACE, PRESS_ACTION));
//    }
//
//    @Override protected void callAction(String name) {
//        switch (name) {
//          case PRESS_ACTION:
//            if (titledPane.isCollapsible() && titledPane.isFocused()) {
//                titledPane.setExpanded(!titledPane.isExpanded());
//                titledPane.requestFocus();
//            }
//            break;
//          default:
//            super.callAction(name);
//        }
//    }

    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    public void mousePressed(MouseEvent e) {
        getNode().requestFocus();
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    public void expand() {
        titledPane.setExpanded(true);
    }

    public void collapse() {
        titledPane.setExpanded(false);
    }

    public void toggle() {
        titledPane.setExpanded(!titledPane.isExpanded());
    }

}

