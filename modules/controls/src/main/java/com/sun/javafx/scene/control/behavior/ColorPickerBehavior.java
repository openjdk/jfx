/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import static javafx.scene.input.KeyEvent.*;


import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ColorPicker;
import com.sun.javafx.scene.control.skin.ColorPickerSkin;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class ColorPickerBehavior extends ComboBoxBaseBehavior<Color> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     * 
     */
    public ColorPickerBehavior(final ColorPicker colorPicker) {
        super(colorPicker);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Opens the Color Picker Palette.
     */
    protected static final String OPEN_ACTION = "Open";

    /**
     * Closes the Color Picker Palette.
     */
    protected static final String CLOSE_ACTION = "Close";
    

    protected static final List<KeyBinding> COLOR_PICKER_BINDINGS = new ArrayList<KeyBinding>();
    static {
//        COLOR_PICKER_BINDINGS.addAll(COMBO_BOX_BASE_BINDINGS);
        COLOR_PICKER_BINDINGS.add(new KeyBinding(ESCAPE, KEY_PRESSED, CLOSE_ACTION));
        COLOR_PICKER_BINDINGS.add(new KeyBinding(SPACE, KEY_PRESSED, OPEN_ACTION));
        COLOR_PICKER_BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED, OPEN_ACTION));
        
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return COLOR_PICKER_BINDINGS;
    }

    @Override protected void callAction(String name) {
        if (OPEN_ACTION.equals(name)) {
            show();
        } else if(CLOSE_ACTION.equals(name)) {
            hide();
        }
        else super.callAction(name);
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
     * @param showHidePopup if true, this should show or hide Popup.
     */
//    public void mousePressed(MouseEvent e) {
//        super.mousePressed(e);
//    }
    
    @Override public void onAutoHide() {
        // when we click on some non  interactive part of the 
        // Color Palette - we do not want to hide.
        wasComboBoxButtonClickedForAutoHide = mouseInsideButton;
        ColorPicker colorPicker = (ColorPicker)getControl();
        ColorPickerSkin cpSkin = (ColorPickerSkin)colorPicker.getSkin();
        cpSkin.syncWithAutoUpdate();
    }
    
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
    public void mouseReleased(MouseEvent e, boolean showHidePopup) {
        if (showHidePopup) {
            super.mouseReleased(e);
        } else {
            disarm();
        }
    }
}
