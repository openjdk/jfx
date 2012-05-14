/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.PopupControl;


/**
 *
 * @author paru
 */
public class ColorPickerPaletteRetriever {
    
    public static ColorPalette getColorPalette(ColorPicker cp) {
        ColorPickerSkin cpSkin = (ColorPickerSkin)cp.getSkin();
        return (ColorPalette)cpSkin.getPopupContent();
    }
    
    public static ColorPalette.ColorPickerGrid getColorGrid(ColorPalette colorPalette) {
        return colorPalette.colorPickerGrid;
    }
    
    public static PopupControl getPopup(ColorPicker cp) {
        ColorPickerSkin cpSkin = (ColorPickerSkin)cp.getSkin();
        return cpSkin.getPopup();
    }
}
