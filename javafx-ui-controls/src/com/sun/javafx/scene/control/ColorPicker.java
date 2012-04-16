/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBoxBase;

/**
 *
 * @author paru
 */
public class ColorPicker<Paint> extends ComboBoxBase<Paint> {

    public static final String STYLE_CLASS_BUTTON = "button";
    public static final String STYLE_CLASS_SPLIT_BUTTON = "split-button";
    
    /**
     * The ColorPicker's currently selected color.  
     */
    private ObjectProperty<Paint> color = new SimpleObjectProperty<Paint>();
    public ObjectProperty<Paint> colorProperty() { return color; }
    public Paint getColor() { return color.get(); }
    public void setColor(Paint newColor) { color.set(newColor); }
    
    // Need API to turn off Color Label text.
    // API to list custom colors 
    // API to save custom colors
    
    public ColorPicker() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }
    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "color-picker";
}
