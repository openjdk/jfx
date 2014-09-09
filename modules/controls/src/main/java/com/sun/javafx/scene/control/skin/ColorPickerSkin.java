/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleManager;

import javafx.beans.property.StringProperty;
import javafx.css.StyleOrigin;
import javafx.css.StyleableBooleanProperty;
import javafx.css.CssMetaData;

import com.sun.javafx.css.converters.BooleanConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.scene.control.behavior.ColorPickerBehavior;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.control.ColorPicker;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.WritableValue;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import com.sun.javafx.scene.control.skin.resources.ControlResources;

import static javafx.scene.paint.Color.*;

/**
 *
 */
public class ColorPickerSkin extends ComboBoxPopupControl<Color> {

    private Label displayNode; 
    private StackPane pickerColorBox;
    private Rectangle colorRect; 
    private ColorPalette popupContent;
    BooleanProperty colorLabelVisible = new StyleableBooleanProperty(true) {
        @Override public void invalidated() {
            if (displayNode != null) {
                if (colorLabelVisible.get()) {
                    displayNode.setText(colorDisplayName(((ColorPicker)getSkinnable()).getValue()));
                } else {
                    displayNode.setText("");
                }
            }
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorLabelVisible";
        }
        @Override public CssMetaData<ColorPicker,Boolean> getCssMetaData() {
            return StyleableProperties.COLOR_LABEL_VISIBLE;
        }
    };
    public StringProperty imageUrlProperty() { return imageUrl; }
    private final StyleableStringProperty imageUrl = new StyleableStringProperty() {
        @Override public void applyStyle(StyleOrigin origin, String v) {
            super.applyStyle(origin, v);
            if (v == null) {
                // remove old image view
                if (pickerColorBox.getChildren().size() == 2) pickerColorBox.getChildren().remove(1);
            } else {
                if (pickerColorBox.getChildren().size() == 2) {
                    ImageView imageView = (ImageView)pickerColorBox.getChildren().get(1);
                    imageView.setImage(StyleManager.getInstance().getCachedImage(v));
                } else {
                    pickerColorBox.getChildren().add(new ImageView(StyleManager.getInstance().getCachedImage(v)));
                }
            }
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "imageUrl";
        }
        @Override public CssMetaData<ColorPicker,String> getCssMetaData() {
            return StyleableProperties.GRAPHIC;
        }
    };
    private final StyleableDoubleProperty colorRectWidth =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_WIDTH;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectWidth";
        }
    };
    private final StyleableDoubleProperty colorRectHeight =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_HEIGHT;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectHeight";
        }
    };
    private final StyleableDoubleProperty colorRectX =  new StyleableDoubleProperty(0) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_X;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectX";
        }
    };
    private final StyleableDoubleProperty colorRectY =  new StyleableDoubleProperty(0) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_Y;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectY";
        }
    };

    public ColorPickerSkin(final ColorPicker colorPicker) {
        super(colorPicker, new ColorPickerBehavior(colorPicker));
        updateComboBoxMode();
        registerChangeListener(colorPicker.valueProperty(), "VALUE");

        // create displayNode
        displayNode = new Label();
        displayNode.getStyleClass().add("color-picker-label");
        displayNode.setManaged(false);

        // label graphic
        pickerColorBox = new PickerColorBox();
        pickerColorBox.getStyleClass().add("picker-color");
        colorRect = new Rectangle(12, 12);
        colorRect.getStyleClass().add("picker-color-rect");

        updateColor();

        pickerColorBox.getChildren().add(colorRect);
        displayNode.setGraphic(pickerColorBox);
    }


    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (!colorLabelVisible.get()) {
            return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        }
        String displayNodeText = displayNode.getText();
        double width = 0;
        for (String name : colorNameMap.values()) {
            displayNode.setText(name);
            width = Math.max(width, super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
        }
        displayNode.setText(formatHexString(Color.BLACK)); // #000000
        width = Math.max(width, super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
        displayNode.setText(displayNodeText);
        return width;
    }

    private void updateComboBoxMode() {
        List<String> styleClass = getSkinnable().getStyleClass();
        if (styleClass.contains(ColorPicker.STYLE_CLASS_BUTTON)) {
            setMode(ComboBoxMode.BUTTON);
        } else if (styleClass.contains(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)) {
            setMode(ComboBoxMode.SPLITBUTTON);
        }
    }

    private static final Map<Color, String> colorNameMap = new HashMap<Color, String>(24);
    private static final Map<Color, String> cssNameMap = new HashMap<Color, String>(139);
    static {
        // Translatable display names for the most common colors
        colorNameMap.put(TRANSPARENT, getString("colorName.transparent"));
        colorNameMap.put(BLACK,       getString("colorName.black"));
        colorNameMap.put(BLUE,        getString("colorName.blue"));
        colorNameMap.put(CYAN,        getString("colorName.cyan"));
        colorNameMap.put(DARKBLUE,    getString("colorName.darkblue"));
        colorNameMap.put(DARKCYAN,    getString("colorName.darkcyan"));
        colorNameMap.put(DARKGRAY,    getString("colorName.darkgray"));
        colorNameMap.put(DARKGREEN,   getString("colorName.darkgreen"));
        colorNameMap.put(DARKMAGENTA, getString("colorName.darkmagenta"));
        colorNameMap.put(DARKRED,     getString("colorName.darkred"));
        colorNameMap.put(GRAY,        getString("colorName.gray"));
        colorNameMap.put(GREEN,       getString("colorName.green"));
        colorNameMap.put(LIGHTBLUE,   getString("colorName.lightblue"));
        colorNameMap.put(LIGHTCYAN,   getString("colorName.lightcyan"));
        colorNameMap.put(LIGHTGRAY,   getString("colorName.lightgray"));
        colorNameMap.put(LIGHTGREEN,  getString("colorName.lightgreen"));
        colorNameMap.put(LIGHTYELLOW, getString("colorName.lightyellow"));
        colorNameMap.put(MAGENTA,     getString("colorName.magenta"));
        colorNameMap.put(MEDIUMBLUE,  getString("colorName.mediumblue"));
        colorNameMap.put(ORANGE,      getString("colorName.orange"));
        colorNameMap.put(PINK,        getString("colorName.pink"));
        colorNameMap.put(RED,         getString("colorName.red"));
        colorNameMap.put(WHITE,       getString("colorName.white"));
        colorNameMap.put(YELLOW,      getString("colorName.yellow"));

        // CSS names.
        // Note that synonyms (such as "grey") have been removed here,
        // since a color can be presented with only one name in this
        // skin. If a reverse map is created for parsing names in the
        // future, then the synonyms should be included there. For a
        // full list of CSS names, see Color.java.
        cssNameMap.put(ALICEBLUE,            "aliceblue");
        cssNameMap.put(ANTIQUEWHITE,         "antiquewhite");
        cssNameMap.put(AQUAMARINE,           "aquamarine");
        cssNameMap.put(AZURE,                "azure");
        cssNameMap.put(BEIGE,                "beige");
        cssNameMap.put(BISQUE,               "bisque");
        cssNameMap.put(BLACK,                "black");
        cssNameMap.put(BLANCHEDALMOND,       "blanchedalmond");
        cssNameMap.put(BLUE,                 "blue");
        cssNameMap.put(BLUEVIOLET,           "blueviolet");
        cssNameMap.put(BROWN,                "brown");
        cssNameMap.put(BURLYWOOD,            "burlywood");
        cssNameMap.put(CADETBLUE,            "cadetblue");
        cssNameMap.put(CHARTREUSE,           "chartreuse");
        cssNameMap.put(CHOCOLATE,            "chocolate");
        cssNameMap.put(CORAL,                "coral");
        cssNameMap.put(CORNFLOWERBLUE,       "cornflowerblue");
        cssNameMap.put(CORNSILK,             "cornsilk");
        cssNameMap.put(CRIMSON,              "crimson");
        cssNameMap.put(CYAN,                 "cyan");
        cssNameMap.put(DARKBLUE,             "darkblue");
        cssNameMap.put(DARKCYAN,             "darkcyan");
        cssNameMap.put(DARKGOLDENROD,        "darkgoldenrod");
        cssNameMap.put(DARKGRAY,             "darkgray");
        cssNameMap.put(DARKGREEN,            "darkgreen");
        cssNameMap.put(DARKKHAKI,            "darkkhaki");
        cssNameMap.put(DARKMAGENTA,          "darkmagenta");
        cssNameMap.put(DARKOLIVEGREEN,       "darkolivegreen");
        cssNameMap.put(DARKORANGE,           "darkorange");
        cssNameMap.put(DARKORCHID,           "darkorchid");
        cssNameMap.put(DARKRED,              "darkred");
        cssNameMap.put(DARKSALMON,           "darksalmon");
        cssNameMap.put(DARKSEAGREEN,         "darkseagreen");
        cssNameMap.put(DARKSLATEBLUE,        "darkslateblue");
        cssNameMap.put(DARKSLATEGRAY,        "darkslategray");
        cssNameMap.put(DARKTURQUOISE,        "darkturquoise");
        cssNameMap.put(DARKVIOLET,           "darkviolet");
        cssNameMap.put(DEEPPINK,             "deeppink");
        cssNameMap.put(DEEPSKYBLUE,          "deepskyblue");
        cssNameMap.put(DIMGRAY,              "dimgray");
        cssNameMap.put(DODGERBLUE,           "dodgerblue");
        cssNameMap.put(FIREBRICK,            "firebrick");
        cssNameMap.put(FLORALWHITE,          "floralwhite");
        cssNameMap.put(FORESTGREEN,          "forestgreen");
        cssNameMap.put(GAINSBORO,            "gainsboro");
        cssNameMap.put(GHOSTWHITE,           "ghostwhite");
        cssNameMap.put(GOLD,                 "gold");
        cssNameMap.put(GOLDENROD,            "goldenrod");
        cssNameMap.put(GRAY,                 "gray");
        cssNameMap.put(GREEN,                "green");
        cssNameMap.put(GREENYELLOW,          "greenyellow");
        cssNameMap.put(HONEYDEW,             "honeydew");
        cssNameMap.put(HOTPINK,              "hotpink");
        cssNameMap.put(INDIANRED,            "indianred");
        cssNameMap.put(INDIGO,               "indigo");
        cssNameMap.put(IVORY,                "ivory");
        cssNameMap.put(KHAKI,                "khaki");
        cssNameMap.put(LAVENDER,             "lavender");
        cssNameMap.put(LAVENDERBLUSH,        "lavenderblush");
        cssNameMap.put(LAWNGREEN,            "lawngreen");
        cssNameMap.put(LEMONCHIFFON,         "lemonchiffon");
        cssNameMap.put(LIGHTBLUE,            "lightblue");
        cssNameMap.put(LIGHTCORAL,           "lightcoral");
        cssNameMap.put(LIGHTCYAN,            "lightcyan");
        cssNameMap.put(LIGHTGOLDENRODYELLOW, "lightgoldenrodyellow");
        cssNameMap.put(LIGHTGRAY,            "lightgray");
        cssNameMap.put(LIGHTGREEN,           "lightgreen");
        cssNameMap.put(LIGHTPINK,            "lightpink");
        cssNameMap.put(LIGHTSALMON,          "lightsalmon");
        cssNameMap.put(LIGHTSEAGREEN,        "lightseagreen");
        cssNameMap.put(LIGHTSKYBLUE,         "lightskyblue");
        cssNameMap.put(LIGHTSLATEGRAY,       "lightslategray");
        cssNameMap.put(LIGHTSTEELBLUE,       "lightsteelblue");
        cssNameMap.put(LIGHTYELLOW,          "lightyellow");
        cssNameMap.put(LIME,                 "lime");
        cssNameMap.put(LIMEGREEN,            "limegreen");
        cssNameMap.put(LINEN,                "linen");
        cssNameMap.put(MAGENTA,              "magenta");
        cssNameMap.put(MAROON,               "maroon");
        cssNameMap.put(MEDIUMAQUAMARINE,     "mediumaquamarine");
        cssNameMap.put(MEDIUMBLUE,           "mediumblue");
        cssNameMap.put(MEDIUMORCHID,         "mediumorchid");
        cssNameMap.put(MEDIUMPURPLE,         "mediumpurple");
        cssNameMap.put(MEDIUMSEAGREEN,       "mediumseagreen");
        cssNameMap.put(MEDIUMSLATEBLUE,      "mediumslateblue");
        cssNameMap.put(MEDIUMSPRINGGREEN,    "mediumspringgreen");
        cssNameMap.put(MEDIUMTURQUOISE,      "mediumturquoise");
        cssNameMap.put(MEDIUMVIOLETRED,      "mediumvioletred");
        cssNameMap.put(MIDNIGHTBLUE,         "midnightblue");
        cssNameMap.put(MINTCREAM,            "mintcream");
        cssNameMap.put(MISTYROSE,            "mistyrose");
        cssNameMap.put(MOCCASIN,             "moccasin");
        cssNameMap.put(NAVAJOWHITE,          "navajowhite");
        cssNameMap.put(NAVY,                 "navy");
        cssNameMap.put(OLDLACE,              "oldlace");
        cssNameMap.put(OLIVE,                "olive");
        cssNameMap.put(OLIVEDRAB,            "olivedrab");
        cssNameMap.put(ORANGE,               "orange");
        cssNameMap.put(ORANGERED,            "orangered");
        cssNameMap.put(ORCHID,               "orchid");
        cssNameMap.put(PALEGOLDENROD,        "palegoldenrod");
        cssNameMap.put(PALEGREEN,            "palegreen");
        cssNameMap.put(PALETURQUOISE,        "paleturquoise");
        cssNameMap.put(PALEVIOLETRED,        "palevioletred");
        cssNameMap.put(PAPAYAWHIP,           "papayawhip");
        cssNameMap.put(PEACHPUFF,            "peachpuff");
        cssNameMap.put(PERU,                 "peru");
        cssNameMap.put(PINK,                 "pink");
        cssNameMap.put(PLUM,                 "plum");
        cssNameMap.put(POWDERBLUE,           "powderblue");
        cssNameMap.put(PURPLE,               "purple");
        cssNameMap.put(RED,                  "red");
        cssNameMap.put(ROSYBROWN,            "rosybrown");
        cssNameMap.put(ROYALBLUE,            "royalblue");
        cssNameMap.put(SADDLEBROWN,          "saddlebrown");
        cssNameMap.put(SALMON,               "salmon");
        cssNameMap.put(SANDYBROWN,           "sandybrown");
        cssNameMap.put(SEAGREEN,             "seagreen");
        cssNameMap.put(SEASHELL,             "seashell");
        cssNameMap.put(SIENNA,               "sienna");
        cssNameMap.put(SILVER,               "silver");
        cssNameMap.put(SKYBLUE,              "skyblue");
        cssNameMap.put(SLATEBLUE,            "slateblue");
        cssNameMap.put(SLATEGRAY,            "slategray");
        cssNameMap.put(SNOW,                 "snow");
        cssNameMap.put(SPRINGGREEN,          "springgreen");
        cssNameMap.put(STEELBLUE,            "steelblue");
        cssNameMap.put(TAN,                  "tan");
        cssNameMap.put(TEAL,                 "teal");
        cssNameMap.put(THISTLE,              "thistle");
        cssNameMap.put(TOMATO,               "tomato");
        cssNameMap.put(TRANSPARENT,          "transparent");
        cssNameMap.put(TURQUOISE,            "turquoise");
        cssNameMap.put(VIOLET,               "violet");
        cssNameMap.put(WHEAT,                "wheat");
        cssNameMap.put(WHITE,                "white");
        cssNameMap.put(WHITESMOKE,           "whitesmoke");
        cssNameMap.put(YELLOW,               "yellow");
        cssNameMap.put(YELLOWGREEN,          "yellowgreen");
    }

    static String colorDisplayName(Color c) {
        if (c != null) {
            String displayName = colorNameMap.get(c);
            if (displayName == null) {
                displayName = formatHexString(c);
            }
            return displayName;
        } else {
            return null;
        }
    }

    static String tooltipString(Color c) {
        if (c != null) {
            String tooltipStr = "";
            String displayName = colorNameMap.get(c);
            if (displayName != null) {
                tooltipStr += displayName + " ";
            }

            tooltipStr += formatHexString(c);

            String cssName = cssNameMap.get(c);
            if (cssName != null) {
                tooltipStr += " (css: " + cssName + ")";
            }
            return tooltipStr;
        } else {
            return null;
        }
    }

    static String formatHexString(Color c) {
        if (c != null) {
            return String.format((Locale) null, "#%02x%02x%02x",
                                 Math.round(c.getRed() * 255),
                                 Math.round(c.getGreen() * 255),
                                 Math.round(c.getBlue() * 255));
        } else {
            return null;
        }
    }

    @Override protected Node getPopupContent() {
        if (popupContent == null) {
//            popupContent = new ColorPalette(colorPicker.getValue(), colorPicker);
            popupContent = new ColorPalette((ColorPicker)getSkinnable());
            popupContent.setPopupControl(getPopup());
        }
       return popupContent;
    }
    
    @Override protected void focusLost() {
        // do nothing
    }
    
    @Override public void show() {
        super.show();
        final ColorPicker colorPicker = (ColorPicker)getSkinnable();
        popupContent.updateSelection(colorPicker.getValue());
        popupContent.clearFocus();
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
    
        if ("SHOWING".equals(p)) {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                if (!popupContent.isCustomColorDialogShowing()) hide();
            }
        } else if ("VALUE".equals(p)) {
            updateColor();
           // Change the current selected color in the grid if ColorPicker value changes
            if (popupContent != null) {
//                popupContent.updateSelection(getSkinnable().getValue());
            }
        }
    }
    @Override public Node getDisplayNode() {
        return displayNode;
    }
    
    private void updateColor() {
        final ColorPicker colorPicker = (ColorPicker)getSkinnable();
        colorRect.setFill(colorPicker.getValue());
        if (colorLabelVisible.get()) {
            displayNode.setText(colorDisplayName(colorPicker.getValue()));
        } else {
            displayNode.setText("");
        }
    }
    public void syncWithAutoUpdate() {
        if (!getPopup().isShowing() && getSkinnable().isShowing()) {
            // Popup was dismissed. Maybe user clicked outside or typed ESCAPE.
            // Make sure ColorPicker button is in sync.
            getSkinnable().hide();
        }
    }
    
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        updateComboBoxMode();
        super.layoutChildren(x,y,w,h);
    }

    static String getString(String key) {
        return ControlResources.getString("ColorPicker."+key);
    }

    /***************************************************************************
    *                                                                         *
    *                         picker-color-cell                               *
    *                                                                         *
    **************************************************************************/

    private class PickerColorBox extends StackPane {
        @Override protected void layoutChildren() {
            final double top = snappedTopInset();
            final double left = snappedLeftInset();
            final double width = getWidth();
            final double height = getHeight();
            final double right = snappedRightInset();
            final double bottom = snappedBottomInset();
            colorRect.setX(snapPosition(colorRectX.get()));
            colorRect.setY(snapPosition(colorRectY.get()));
            colorRect.setWidth(snapSize(colorRectWidth.get()));
            colorRect.setHeight(snapSize(colorRectHeight.get()));
            if (getChildren().size() == 2) {
                final ImageView icon = (ImageView) getChildren().get(1);
                Pos childAlignment = StackPane.getAlignment(icon);
                layoutInArea(icon, left, top,
                             width - left - right, height - top - bottom,
                             0, getMargin(icon),
                             childAlignment != null? childAlignment.getHpos() : getAlignment().getHpos(),
                             childAlignment != null? childAlignment.getVpos() : getAlignment().getVpos());
                colorRect.setLayoutX(icon.getLayoutX());
                colorRect.setLayoutY(icon.getLayoutY());
            } else {
                Pos childAlignment = StackPane.getAlignment(colorRect);
                layoutInArea(colorRect, left, top,
                             width - left - right, height - top - bottom,
                             0, getMargin(colorRect),
                             childAlignment != null? childAlignment.getHpos() : getAlignment().getHpos(),
                             childAlignment != null? childAlignment.getVpos() : getAlignment().getVpos());
            }
        }
    }

    /***************************************************************************
    *                                                                         *
    *                         Stylesheet Handling                             *
    *                                                                         *
    **************************************************************************/

     private static class StyleableProperties {
        private static final CssMetaData<ColorPicker,Boolean> COLOR_LABEL_VISIBLE = 
                new CssMetaData<ColorPicker,Boolean>("-fx-color-label-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override public boolean isSettable(ColorPicker n) {
                final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                return skin.colorLabelVisible == null || !skin.colorLabelVisible.isBound();
            }
            
            @Override public StyleableProperty<Boolean> getStyleableProperty(ColorPicker n) {
                final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)skin.colorLabelVisible;
            }
        };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_WIDTH =
                new CssMetaData<ColorPicker,Number>("-fx-color-rect-width", SizeConverter.getInstance(), 12d) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectWidth.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectWidth;
                    }
                };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_HEIGHT =
                new CssMetaData<ColorPicker,Number>("-fx-color-rect-height", SizeConverter.getInstance(), 12d) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectHeight.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectHeight;
                    }
                };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_X =
                new CssMetaData<ColorPicker,Number>("-fx-color-rect-x", SizeConverter.getInstance(), 0) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectX.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectX;
                    }
                };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_Y =
                new CssMetaData<ColorPicker,Number>("-fx-color-rect-y", SizeConverter.getInstance(), 0) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectY.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectY;
                    }
                };
        private static final CssMetaData<ColorPicker,String> GRAPHIC =
            new CssMetaData<ColorPicker,String>("-fx-graphic", StringConverter.getInstance()) {
                @Override public boolean isSettable(ColorPicker n) {
                    final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                    return !skin.imageUrl.isBound();
                }
                @Override public StyleableProperty<String> getStyleableProperty(ColorPicker n) {
                    final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                    return skin.imageUrl;
                }
            };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(ComboBoxBaseSkin.getClassCssMetaData());
            styleables.add(COLOR_LABEL_VISIBLE);
            styleables.add(COLOR_RECT_WIDTH);
            styleables.add(COLOR_RECT_HEIGHT);
            styleables.add(COLOR_RECT_X);
            styleables.add(COLOR_RECT_Y);
            styleables.add(GRAPHIC);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
     
    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

}
