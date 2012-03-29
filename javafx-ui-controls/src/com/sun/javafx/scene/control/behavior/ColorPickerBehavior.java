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

import javafx.scene.control.ComboBox;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;


import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.control.ColorPicker;

import javafx.scene.control.SelectionModel;
import javafx.scene.input.MouseEvent;

public class ColorPickerBehavior<T> extends ComboBoxBaseBehavior<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     * 
     */
    public ColorPickerBehavior(final ColorPicker<T> colorPicker) {
        super(colorPicker);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    protected static final List<KeyBinding> COMBO_BOX_BINDINGS = new ArrayList<KeyBinding>();
    static {
        COMBO_BOX_BINDINGS.addAll(COMBO_BOX_BASE_BINDINGS);
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return COMBO_BOX_BINDINGS;
    }

    @Override protected void callAction(String name) {
        super.callAction(name);
    }
    
    private ColorPicker<T> getColorPicker() {
        return (ColorPicker<T>) getControl();
    }
    
     /**************************************************************************
     *                                                                        *
     * Mouse Events                                                           *
     *                                                                        *
     *************************************************************************/
     /**
     * When a mouse button is pressed, we either want to behave like a button or
     * show the popup.  This will be called by the skin.
     *
     * @param e the mouse press event
     * @param behaveLikeButton if true, this should act just like a button
     */
//    public void mousePressed(MouseEvent e) {
//        super.mousePressed(e);
//    }
    
    @Override public void mouseReleased(MouseEvent e) {
        // Overriding to not do the usual on mouseReleased.
        // The event is handled by the skin instead, which calls
        // the method below.
    }
//    
    /**
     * Handles mouse release events.  This will be called by the skin.
     *
     * @param e the mouse press event
     * @param behaveLikeButton if true, this should act just like a button
     */
    public void mouseReleased(MouseEvent e, boolean behaveLikeSplitButton) {
        if (behaveLikeSplitButton) {
            super.mouseReleased(e);
        } else {
            disarm();
        }
    }
}
