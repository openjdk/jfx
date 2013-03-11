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
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.scene.control.behavior.ColorPickerBehavior;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.ColorPicker;
import javafx.beans.property.BooleanProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

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
                    displayNode.setText(colorValueToWeb(((ColorPicker)getSkinnable()).getValue()));
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
        if (getMode() == ComboBoxMode.BUTTON || getMode() == ComboBoxMode.COMBOBOX) {
             if (arrowButton.getOnMouseReleased() == null) {
                arrowButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent e) {
                        ((ColorPickerBehavior)getBehavior()).mouseReleased(e, true);
                        e.consume();
                    }
                });
            }
        } else if (getMode() == ComboBoxMode.SPLITBUTTON) {
            if (arrowButton.getOnMouseReleased() == null) {
                arrowButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent e) {
                        ((ColorPickerBehavior)getBehavior()).mouseReleased(e, true);
                        e.consume();
                    }
                });
            }
        }
        registerChangeListener(colorPicker.valueProperty(), "VALUE");

        // create displayNode
        displayNode = new Label();
        displayNode.getStyleClass().add("color-picker-label");
        if (getMode() == ComboBoxMode.BUTTON || getMode() == ComboBoxMode.COMBOBOX) {
            if (displayNode.getOnMouseReleased() == null) {
                displayNode.setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent e) {
                        ((ColorPickerBehavior)getBehavior()).mouseReleased(e, true);
                    }
                });
            }
        } else {
            if (displayNode.getOnMouseReleased() == null) {
                displayNode.setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent e) {
                        ((ColorPickerBehavior)getBehavior()).mouseReleased(e, false);
                        e.consume();
                    }
                });
            }
        }
        // label graphic
        pickerColorBox = new PickerColorBox();
        pickerColorBox.getStyleClass().add("picker-color");
        colorRect = new Rectangle(12, 12);
        colorRect.getStyleClass().add("picker-color-rect");

        updateColor();
        colorPicker.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                updateColor();
            }
        });

        pickerColorBox.getChildren().add(colorRect);
        displayNode.setGraphic(pickerColorBox);
        if (displayNode.getOnMouseReleased() == null) {
            displayNode.setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    ((ColorPickerBehavior)getBehavior()).mouseReleased(e, false);
                    e.consume();
                }
            });
        }
    }



    private void updateComboBoxMode() {
        if (getSkinnable().getStyleClass().contains(ColorPicker.STYLE_CLASS_BUTTON)) {
            setMode(ComboBoxMode.BUTTON);
        }
        else if (getSkinnable().getStyleClass().contains(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)) {
            setMode(ComboBoxMode.SPLITBUTTON);
        }
    }
    
    private static final Map<Color, String> colorNameMap = new HashMap<Color, String>(147);
    static {
        // The following code is used to generate the hard-coded colorNameMap
        // below, but it is then hand tweaked to proper represent the colour
        // names.
        
//        // Initializes the namedColors map
//        Color.web("white", 1.0);
//        for (Field f : Color.class.getDeclaredFields()) {
//            int modifier = f.getModifiers();
//            if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier) && f.getType().equals(Color.class)) {
//                colorNames.add(f.getName());
//                
//                String name = f.toString();
//                name = name.substring(name.lastIndexOf("Color."));
//                
//                String displayName = f.getName();
//                displayName = displayName.substring(0, 1) + displayName.substring(1).toLowerCase();
//                
//                System.out.println("colorNameMap.put(" + name + ", \"" + displayName + "\");");
//            }
//        }
        
        colorNameMap.put(Color.TRANSPARENT, "Transparent");
        colorNameMap.put(Color.ALICEBLUE, "Alice Blue");
        colorNameMap.put(Color.ANTIQUEWHITE, "Antique White");
        colorNameMap.put(Color.AQUA, "Aqua");
        colorNameMap.put(Color.AQUAMARINE, "Aquamarine");
        colorNameMap.put(Color.AZURE, "Azure");
        colorNameMap.put(Color.BEIGE, "Beige");
        colorNameMap.put(Color.BISQUE, "Bisque");
        colorNameMap.put(Color.BLACK, "Black");
        colorNameMap.put(Color.BLANCHEDALMOND, "Blanched Almond");
        colorNameMap.put(Color.BLUE, "Blue");
        colorNameMap.put(Color.BLUEVIOLET, "Blue Violet");
        colorNameMap.put(Color.BROWN, "Brown");
        colorNameMap.put(Color.BURLYWOOD, "Burlywood");
        colorNameMap.put(Color.CADETBLUE, "Cadet Blue");
        colorNameMap.put(Color.CHARTREUSE, "Chartreuse");
        colorNameMap.put(Color.CHOCOLATE, "Chocolate");
        colorNameMap.put(Color.CORAL, "Coral");
        colorNameMap.put(Color.CORNFLOWERBLUE, "Cornflower Blue");
        colorNameMap.put(Color.CORNSILK, "Cornsilk");
        colorNameMap.put(Color.CRIMSON, "Crimson");
        colorNameMap.put(Color.CYAN, "Cyan");
        colorNameMap.put(Color.DARKBLUE, "Dark Blue");
        colorNameMap.put(Color.DARKCYAN, "Dark Cyan");
        colorNameMap.put(Color.DARKGOLDENROD, "Dark Goldenrod");
        colorNameMap.put(Color.DARKGRAY, "Dark Gray");
        colorNameMap.put(Color.DARKGREEN, "Dark Green");
        colorNameMap.put(Color.DARKGREY, "Dark Grey");
        colorNameMap.put(Color.DARKKHAKI, "Dark Khaki");
        colorNameMap.put(Color.DARKMAGENTA, "Dark Magenta");
        colorNameMap.put(Color.DARKOLIVEGREEN, "Dark Olive Green");
        colorNameMap.put(Color.DARKORANGE, "Dark Orange");
        colorNameMap.put(Color.DARKORCHID, "Dark Orchid");
        colorNameMap.put(Color.DARKRED, "Dark Red");
        colorNameMap.put(Color.DARKSALMON, "Dark Salmon");
        colorNameMap.put(Color.DARKSEAGREEN, "Dark Sea Green");
        colorNameMap.put(Color.DARKSLATEBLUE, "Dark Slate Blue");
        colorNameMap.put(Color.DARKSLATEGRAY, "Dark Slate Gray");
        colorNameMap.put(Color.DARKSLATEGREY, "Dark Slate Grey");
        colorNameMap.put(Color.DARKTURQUOISE, "Dark Turquoise");
        colorNameMap.put(Color.DARKVIOLET, "Dark Violet");
        colorNameMap.put(Color.DEEPPINK, "Deep Pink");
        colorNameMap.put(Color.DEEPSKYBLUE, "Deep Sky Blue");
        colorNameMap.put(Color.DIMGRAY, "Dim Gray");
        colorNameMap.put(Color.DIMGREY, "Dim Grey");
        colorNameMap.put(Color.DODGERBLUE, "Dodger Blue");
        colorNameMap.put(Color.FIREBRICK, "Firebrick");
        colorNameMap.put(Color.FLORALWHITE, "Floral White");
        colorNameMap.put(Color.FORESTGREEN, "Forest Green");
        colorNameMap.put(Color.FUCHSIA, "Fuchsia");
        colorNameMap.put(Color.GAINSBORO, "Gainsboro");
        colorNameMap.put(Color.GHOSTWHITE, "Ghost White");
        colorNameMap.put(Color.GOLD, "Gold");
        colorNameMap.put(Color.GOLDENROD, "Goldenrod");
        colorNameMap.put(Color.GRAY, "Gray");
        colorNameMap.put(Color.GREEN, "Green");
        colorNameMap.put(Color.GREENYELLOW, "Green Yellow");
        colorNameMap.put(Color.GREY, "Grey");
        colorNameMap.put(Color.HONEYDEW, "Honeydew");
        colorNameMap.put(Color.HOTPINK, "Hot Pink");
        colorNameMap.put(Color.INDIANRED, "Indian Red");
        colorNameMap.put(Color.INDIGO, "Indigo");
        colorNameMap.put(Color.IVORY, "Ivory");
        colorNameMap.put(Color.KHAKI, "Khaki");
        colorNameMap.put(Color.LAVENDER, "Lavender");
        colorNameMap.put(Color.LAVENDERBLUSH, "Lavender Blush");
        colorNameMap.put(Color.LAWNGREEN, "Lawn Green");
        colorNameMap.put(Color.LEMONCHIFFON, "Lemon Chiffon");
        colorNameMap.put(Color.LIGHTBLUE, "Light Blue");
        colorNameMap.put(Color.LIGHTCORAL, "Light Coral");
        colorNameMap.put(Color.LIGHTCYAN, "Light Cyan");
        colorNameMap.put(Color.LIGHTGOLDENRODYELLOW, "Light Goldenrod Yellow");
        colorNameMap.put(Color.LIGHTGRAY, "Light Gray");
        colorNameMap.put(Color.LIGHTGREEN, "Light Green");
        colorNameMap.put(Color.LIGHTGREY, "Light Grey");
        colorNameMap.put(Color.LIGHTPINK, "Light Pink");
        colorNameMap.put(Color.LIGHTSALMON, "Light Salmon");
        colorNameMap.put(Color.LIGHTSEAGREEN, "Light Sea Green");
        colorNameMap.put(Color.LIGHTSKYBLUE, "Light Sky Blue");
        colorNameMap.put(Color.LIGHTSLATEGRAY, "Light Slate Gray");
        colorNameMap.put(Color.LIGHTSLATEGREY, "Light Slate Grey");
        colorNameMap.put(Color.LIGHTSTEELBLUE, "Light Steel Blue");
        colorNameMap.put(Color.LIGHTYELLOW, "Light Yellow");
        colorNameMap.put(Color.LIME, "Lime");
        colorNameMap.put(Color.LIMEGREEN, "Lime Green");
        colorNameMap.put(Color.LINEN, "Linen");
        colorNameMap.put(Color.MAGENTA, "Magenta");
        colorNameMap.put(Color.MAROON, "Maroon");
        colorNameMap.put(Color.MEDIUMAQUAMARINE, "Medium Aquamarine");
        colorNameMap.put(Color.MEDIUMBLUE, "Medium Blue");
        colorNameMap.put(Color.MEDIUMORCHID, "Medium Orchid");
        colorNameMap.put(Color.MEDIUMPURPLE, "Medium Purple");
        colorNameMap.put(Color.MEDIUMSEAGREEN, "Medium Sea Green");
        colorNameMap.put(Color.MEDIUMSLATEBLUE, "Medium Slate Blue");
        colorNameMap.put(Color.MEDIUMSPRINGGREEN, "Medium Spring Green");
        colorNameMap.put(Color.MEDIUMTURQUOISE, "Medium Turquoise");
        colorNameMap.put(Color.MEDIUMVIOLETRED, "Medium Violet Red");
        colorNameMap.put(Color.MIDNIGHTBLUE, "Midnight Blue");
        colorNameMap.put(Color.MINTCREAM, "Mint Cream");
        colorNameMap.put(Color.MISTYROSE, "Misty Rose");
        colorNameMap.put(Color.MOCCASIN, "Moccasin");
        colorNameMap.put(Color.NAVAJOWHITE, "Navajo White");
        colorNameMap.put(Color.NAVY, "Navy");
        colorNameMap.put(Color.OLDLACE, "Old Lace");
        colorNameMap.put(Color.OLIVE, "Olive");
        colorNameMap.put(Color.OLIVEDRAB, "Olive Drab");
        colorNameMap.put(Color.ORANGE, "Orange");
        colorNameMap.put(Color.ORANGERED, "Orange Red");
        colorNameMap.put(Color.ORCHID, "Orchid");
        colorNameMap.put(Color.PALEGOLDENROD, "Pale Goldenrod");
        colorNameMap.put(Color.PALEGREEN, "Pale Green");
        colorNameMap.put(Color.PALETURQUOISE, "Pale Turquoise");
        colorNameMap.put(Color.PALEVIOLETRED, "Pale Violet Red");
        colorNameMap.put(Color.PAPAYAWHIP, "Papaya Whip");
        colorNameMap.put(Color.PEACHPUFF, "Peach Puff");
        colorNameMap.put(Color.PERU, "Peru");
        colorNameMap.put(Color.PINK, "Pink");
        colorNameMap.put(Color.PLUM, "Plum");
        colorNameMap.put(Color.POWDERBLUE, "Powder Blue");
        colorNameMap.put(Color.PURPLE, "Purple");
        colorNameMap.put(Color.RED, "Red");
        colorNameMap.put(Color.ROSYBROWN, "Rosy Brown");
        colorNameMap.put(Color.ROYALBLUE, "Royal Blue");
        colorNameMap.put(Color.SADDLEBROWN, "Saddle Brown");
        colorNameMap.put(Color.SALMON, "Salmon");
        colorNameMap.put(Color.SANDYBROWN, "Sandy Brown");
        colorNameMap.put(Color.SEAGREEN, "Sea Green");
        colorNameMap.put(Color.SEASHELL, "Seashell");
        colorNameMap.put(Color.SIENNA, "Sienna");
        colorNameMap.put(Color.SILVER, "Silver");
        colorNameMap.put(Color.SKYBLUE, "Sky Blue");
        colorNameMap.put(Color.SLATEBLUE, "Slate Blue");
        colorNameMap.put(Color.SLATEGRAY, "Slate Gray");
        colorNameMap.put(Color.SLATEGREY, "Slate Grey");
        colorNameMap.put(Color.SNOW, "Snow");
        colorNameMap.put(Color.SPRINGGREEN, "Spring Green");
        colorNameMap.put(Color.STEELBLUE, "Steel Blue");
        colorNameMap.put(Color.TAN, "Tan");
        colorNameMap.put(Color.TEAL, "Teal");
        colorNameMap.put(Color.THISTLE, "Thistle");
        colorNameMap.put(Color.TOMATO, "Tomato");
        colorNameMap.put(Color.TURQUOISE, "Turquoise");
        colorNameMap.put(Color.VIOLET, "Violet");
        colorNameMap.put(Color.WHEAT, "Wheat");
        colorNameMap.put(Color.WHITE, "White");
        colorNameMap.put(Color.WHITESMOKE, "White Smoke");
        colorNameMap.put(Color.YELLOW, "Yellow");
        colorNameMap.put(Color.YELLOWGREEN, "Yellow Green");
    }
    
    static String colorValueToWeb(Color c) {
        if (c == null) return null;
        String web = colorNameMap.get(c);
        if (web == null) {
            web = String.format((Locale) null, "%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
        }
        return web;
    }
 
    @Override protected Node getPopupContent() {
        if (popupContent == null) {
//            popupContent = new ColorPalette(colorPicker.getValue(), colorPicker);
            popupContent = new ColorPalette(getSkinnable().getValue(), (ColorPicker)getSkinnable());
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
        popupContent.setDialogLocation(getPopup().getX()+getPopup().getWidth(), getPopup().getY());
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
            displayNode.setText(colorValueToWeb(colorPicker.getValue()));
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

    /***************************************************************************
    *                                                                         *
    *                         picker-color-cell                               *
    *                                                                         *
    **************************************************************************/

    private class PickerColorBox extends StackPane {
        @Override protected void layoutChildren() {
            final double top = getInsets().getTop();
            final double left = getInsets().getLeft();
            final double width = getWidth();
            final double height = getHeight();
            final double right = getInsets().getRight();
            final double bottom = getInsets().getBottom();
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
                return (StyleableProperty<Boolean>)skin.colorLabelVisible;
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
