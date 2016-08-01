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

import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.skin.ScrollPaneSkin;

import java.util.Optional;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.END;


/**
 * Behavior for ScrollPane.
 *
 * TODO: the function variables are a poor way to couple to the rest of
 * the system. This technique avoids a direct dependency on the skin class.
 * However, this should really be coupled through the control itself instead
 * of directly to the skin.
 */
public class ScrollPaneBehavior extends BehaviorBase<ScrollPane> {

    private final InputMap<ScrollPane> inputMap;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ScrollPaneBehavior(ScrollPane scrollPane) {
        super(scrollPane);

        // create a map for scrollpane-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap();

        // scrollpane-specific mappings for key and mouse input
        addDefaultMapping(inputMap,
            new InputMap.KeyMapping(LEFT, e -> rtl(scrollPane, this::horizontalUnitIncrement, this::horizontalUnitDecrement)),
            new InputMap.KeyMapping(RIGHT, e -> rtl(scrollPane, this::horizontalUnitDecrement, this::horizontalUnitIncrement)),

            new InputMap.KeyMapping(UP, e -> verticalUnitDecrement()),
            new InputMap.KeyMapping(DOWN, e -> verticalUnitIncrement()),

            new InputMap.KeyMapping(PAGE_UP, e -> verticalPageDecrement()),
            new InputMap.KeyMapping(PAGE_DOWN, e -> verticalPageIncrement()),
            new InputMap.KeyMapping(SPACE, e -> verticalPageIncrement()),

            new InputMap.KeyMapping(HOME, e -> verticalHome()),
            new InputMap.KeyMapping(END, e -> verticalEnd()),

            new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed)
        );
    }



    /***************************************************************************
     *                                                                         *
     * Functions                                                               *
     *                                                                         *
     **************************************************************************/

    @Override public InputMap<ScrollPane> getInputMap() {
        return inputMap;
    }

    public void horizontalUnitIncrement() {
        getHorizontalScrollBar().ifPresent(ScrollBar::increment);
    }
    public void horizontalUnitDecrement() {
        getHorizontalScrollBar().ifPresent(ScrollBar::decrement);
    }
    public void verticalUnitIncrement() {
        getVerticalScrollBar().ifPresent(ScrollBar::increment);
    }
    void verticalUnitDecrement() {
        getVerticalScrollBar().ifPresent(ScrollBar::decrement);
    }
    void horizontalPageIncrement() {
        getHorizontalScrollBar().ifPresent(ScrollBar::increment);
    }
    void horizontalPageDecrement() {
        getHorizontalScrollBar().ifPresent(ScrollBar::decrement);
    }
    void verticalPageIncrement() {
        getVerticalScrollBar().ifPresent(ScrollBar::increment);
    }
    void verticalPageDecrement() {
        getVerticalScrollBar().ifPresent(ScrollBar::decrement);
    }
    void verticalHome() {
        ScrollPane sp = getNode();
        sp.setHvalue(sp.getHmin());
        sp.setVvalue(sp.getVmin());
    }
    void verticalEnd() {
        ScrollPane sp = getNode();
        sp.setHvalue(sp.getHmax());
        sp.setVvalue(sp.getVmax());
    }

    private Optional<ScrollBar> getVerticalScrollBar() {
        return Optional.ofNullable(((ScrollPaneSkin)getNode().getSkin()).getVerticalScrollBar());
    }

    private Optional<ScrollBar> getHorizontalScrollBar() {
        return Optional.ofNullable(((ScrollPaneSkin)getNode().getSkin()).getHorizontalScrollBar());
    }


    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    public void mousePressed(MouseEvent e) {
        getNode().requestFocus();
    }
}
