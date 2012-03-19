/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control;

import javafx.scene.control.ComboBoxBase;

/**
 *
 * @author paru
 */
public class ColorPicker<Color> extends ComboBoxBase<Color> {

    public static final String STYLE_CLASS_BUTTON = "button";
    public static final String STYLE_CLASS_SPLIT_BUTTON = "split-button";
    
    // Need API to turn off Color Label text.
    
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
