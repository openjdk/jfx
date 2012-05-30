/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PopupControl;
import javafx.stage.Stage;


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
    
    public static Hyperlink getCustomColorLink(ColorPalette cp) {
        return cp.customColorLink;
    }
    
    public static Stage getCustomColorDialog(ColorPalette cp) {
        if (cp.customColorDialog != null) return cp.customColorDialog.dialog;
        return null;
    }
}
