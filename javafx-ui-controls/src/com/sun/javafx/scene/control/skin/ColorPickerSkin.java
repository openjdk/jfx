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
package com.sun.javafx.scene.control.skin;

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
                    imageView.setImage(new Image(v));
                } else {
                    pickerColorBox.getChildren().add(new ImageView(new Image(v)));
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
        colorNameMap.put(Color.ALICEBLUE, "Aliceblue");
        colorNameMap.put(Color.ANTIQUEWHITE, "Antiquewhite");
        colorNameMap.put(Color.AQUA, "Aqua");
        colorNameMap.put(Color.AQUAMARINE, "Aquamarine");
        colorNameMap.put(Color.AZURE, "Azure");
        colorNameMap.put(Color.BEIGE, "Beige");
        colorNameMap.put(Color.BISQUE, "Bisque");
        colorNameMap.put(Color.BLACK, "Black");
        colorNameMap.put(Color.BLANCHEDALMOND, "Blanchedalmond");
        colorNameMap.put(Color.BLUE, "Blue");
        colorNameMap.put(Color.BLUEVIOLET, "Blueviolet");
        colorNameMap.put(Color.BROWN, "Brown");
        colorNameMap.put(Color.BURLYWOOD, "Burlywood");
        colorNameMap.put(Color.CADETBLUE, "Cadetblue");
        colorNameMap.put(Color.CHARTREUSE, "Chartreuse");
        colorNameMap.put(Color.CHOCOLATE, "Chocolate");
        colorNameMap.put(Color.CORAL, "Coral");
        colorNameMap.put(Color.CORNFLOWERBLUE, "Cornflowerblue");
        colorNameMap.put(Color.CORNSILK, "Cornsilk");
        colorNameMap.put(Color.CRIMSON, "Crimson");
        colorNameMap.put(Color.CYAN, "Cyan");
        colorNameMap.put(Color.DARKBLUE, "Darkblue");
        colorNameMap.put(Color.DARKCYAN, "Darkcyan");
        colorNameMap.put(Color.DARKGOLDENROD, "Darkgoldenrod");
        colorNameMap.put(Color.DARKGRAY, "Darkgray");
        colorNameMap.put(Color.DARKGREEN, "Darkgreen");
        colorNameMap.put(Color.DARKGREY, "Darkgrey");
        colorNameMap.put(Color.DARKKHAKI, "Darkkhaki");
        colorNameMap.put(Color.DARKMAGENTA, "Darkmagenta");
        colorNameMap.put(Color.DARKOLIVEGREEN, "Darkolivegreen");
        colorNameMap.put(Color.DARKORANGE, "Darkorange");
        colorNameMap.put(Color.DARKORCHID, "Darkorchid");
        colorNameMap.put(Color.DARKRED, "Darkred");
        colorNameMap.put(Color.DARKSALMON, "Darksalmon");
        colorNameMap.put(Color.DARKSEAGREEN, "Darkseagreen");
        colorNameMap.put(Color.DARKSLATEBLUE, "Darkslateblue");
        colorNameMap.put(Color.DARKSLATEGRAY, "Darkslategray");
        colorNameMap.put(Color.DARKSLATEGREY, "Darkslategrey");
        colorNameMap.put(Color.DARKTURQUOISE, "Darkturquoise");
        colorNameMap.put(Color.DARKVIOLET, "Darkviolet");
        colorNameMap.put(Color.DEEPPINK, "Deeppink");
        colorNameMap.put(Color.DEEPSKYBLUE, "Deepskyblue");
        colorNameMap.put(Color.DIMGRAY, "Dimgray");
        colorNameMap.put(Color.DIMGREY, "Dimgrey");
        colorNameMap.put(Color.DODGERBLUE, "Dodgerblue");
        colorNameMap.put(Color.FIREBRICK, "Firebrick");
        colorNameMap.put(Color.FLORALWHITE, "Floralwhite");
        colorNameMap.put(Color.FORESTGREEN, "Forestgreen");
        colorNameMap.put(Color.FUCHSIA, "Fuchsia");
        colorNameMap.put(Color.GAINSBORO, "Gainsboro");
        colorNameMap.put(Color.GHOSTWHITE, "Ghostwhite");
        colorNameMap.put(Color.GOLD, "Gold");
        colorNameMap.put(Color.GOLDENROD, "Goldenrod");
        colorNameMap.put(Color.GRAY, "Gray");
        colorNameMap.put(Color.GREEN, "Green");
        colorNameMap.put(Color.GREENYELLOW, "Greenyellow");
        colorNameMap.put(Color.GREY, "Grey");
        colorNameMap.put(Color.HONEYDEW, "Honeydew");
        colorNameMap.put(Color.HOTPINK, "Hotpink");
        colorNameMap.put(Color.INDIANRED, "Indianred");
        colorNameMap.put(Color.INDIGO, "Indigo");
        colorNameMap.put(Color.IVORY, "Ivory");
        colorNameMap.put(Color.KHAKI, "Khaki");
        colorNameMap.put(Color.LAVENDER, "Lavender");
        colorNameMap.put(Color.LAVENDERBLUSH, "Lavenderblush");
        colorNameMap.put(Color.LAWNGREEN, "Lawngreen");
        colorNameMap.put(Color.LEMONCHIFFON, "Lemonchiffon");
        colorNameMap.put(Color.LIGHTBLUE, "Lightblue");
        colorNameMap.put(Color.LIGHTCORAL, "Lightcoral");
        colorNameMap.put(Color.LIGHTCYAN, "Lightcyan");
        colorNameMap.put(Color.LIGHTGOLDENRODYELLOW, "Lightgoldenrodyellow");
        colorNameMap.put(Color.LIGHTGRAY, "Lightgray");
        colorNameMap.put(Color.LIGHTGREEN, "Lightgreen");
        colorNameMap.put(Color.LIGHTGREY, "Lightgrey");
        colorNameMap.put(Color.LIGHTPINK, "Lightpink");
        colorNameMap.put(Color.LIGHTSALMON, "Lightsalmon");
        colorNameMap.put(Color.LIGHTSEAGREEN, "Lightseagreen");
        colorNameMap.put(Color.LIGHTSKYBLUE, "Lightskyblue");
        colorNameMap.put(Color.LIGHTSLATEGRAY, "Lightslategray");
        colorNameMap.put(Color.LIGHTSLATEGREY, "Lightslategrey");
        colorNameMap.put(Color.LIGHTSTEELBLUE, "Lightsteelblue");
        colorNameMap.put(Color.LIGHTYELLOW, "Lightyellow");
        colorNameMap.put(Color.LIME, "Lime");
        colorNameMap.put(Color.LIMEGREEN, "Limegreen");
        colorNameMap.put(Color.LINEN, "Linen");
        colorNameMap.put(Color.MAGENTA, "Magenta");
        colorNameMap.put(Color.MAROON, "Maroon");
        colorNameMap.put(Color.MEDIUMAQUAMARINE, "Mediumaquamarine");
        colorNameMap.put(Color.MEDIUMBLUE, "Mediumblue");
        colorNameMap.put(Color.MEDIUMORCHID, "Mediumorchid");
        colorNameMap.put(Color.MEDIUMPURPLE, "Mediumpurple");
        colorNameMap.put(Color.MEDIUMSEAGREEN, "Mediumseagreen");
        colorNameMap.put(Color.MEDIUMSLATEBLUE, "Mediumslateblue");
        colorNameMap.put(Color.MEDIUMSPRINGGREEN, "Mediumspringgreen");
        colorNameMap.put(Color.MEDIUMTURQUOISE, "Mediumturquoise");
        colorNameMap.put(Color.MEDIUMVIOLETRED, "Mediumvioletred");
        colorNameMap.put(Color.MIDNIGHTBLUE, "Midnightblue");
        colorNameMap.put(Color.MINTCREAM, "Mintcream");
        colorNameMap.put(Color.MISTYROSE, "Mistyrose");
        colorNameMap.put(Color.MOCCASIN, "Moccasin");
        colorNameMap.put(Color.NAVAJOWHITE, "Navajowhite");
        colorNameMap.put(Color.NAVY, "Navy");
        colorNameMap.put(Color.OLDLACE, "Oldlace");
        colorNameMap.put(Color.OLIVE, "Olive");
        colorNameMap.put(Color.OLIVEDRAB, "Olivedrab");
        colorNameMap.put(Color.ORANGE, "Orange");
        colorNameMap.put(Color.ORANGERED, "Orangered");
        colorNameMap.put(Color.ORCHID, "Orchid");
        colorNameMap.put(Color.PALEGOLDENROD, "Palegoldenrod");
        colorNameMap.put(Color.PALEGREEN, "Palegreen");
        colorNameMap.put(Color.PALETURQUOISE, "Paleturquoise");
        colorNameMap.put(Color.PALEVIOLETRED, "Palevioletred");
        colorNameMap.put(Color.PAPAYAWHIP, "Papayawhip");
        colorNameMap.put(Color.PEACHPUFF, "Peachpuff");
        colorNameMap.put(Color.PERU, "Peru");
        colorNameMap.put(Color.PINK, "Pink");
        colorNameMap.put(Color.PLUM, "Plum");
        colorNameMap.put(Color.POWDERBLUE, "Powderblue");
        colorNameMap.put(Color.PURPLE, "Purple");
        colorNameMap.put(Color.RED, "Red");
        colorNameMap.put(Color.ROSYBROWN, "Rosybrown");
        colorNameMap.put(Color.ROYALBLUE, "Royalblue");
        colorNameMap.put(Color.SADDLEBROWN, "Saddlebrown");
        colorNameMap.put(Color.SALMON, "Salmon");
        colorNameMap.put(Color.SANDYBROWN, "Sandybrown");
        colorNameMap.put(Color.SEAGREEN, "Seagreen");
        colorNameMap.put(Color.SEASHELL, "Seashell");
        colorNameMap.put(Color.SIENNA, "Sienna");
        colorNameMap.put(Color.SILVER, "Silver");
        colorNameMap.put(Color.SKYBLUE, "Skyblue");
        colorNameMap.put(Color.SLATEBLUE, "Slateblue");
        colorNameMap.put(Color.SLATEGRAY, "Slategray");
        colorNameMap.put(Color.SLATEGREY, "Slategrey");
        colorNameMap.put(Color.SNOW, "Snow");
        colorNameMap.put(Color.SPRINGGREEN, "Springgreen");
        colorNameMap.put(Color.STEELBLUE, "Steelblue");
        colorNameMap.put(Color.TAN, "Tan");
        colorNameMap.put(Color.TEAL, "Teal");
        colorNameMap.put(Color.THISTLE, "Thistle");
        colorNameMap.put(Color.TOMATO, "Tomato");
        colorNameMap.put(Color.TURQUOISE, "Turquoise");
        colorNameMap.put(Color.VIOLET, "Violet");
        colorNameMap.put(Color.WHEAT, "Wheat");
        colorNameMap.put(Color.WHITE, "White");
        colorNameMap.put(Color.WHITESMOKE, "Whitesmoke");
        colorNameMap.put(Color.YELLOW, "Yellow");
        colorNameMap.put(Color.YELLOWGREEN, "Yellowgreen");
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
