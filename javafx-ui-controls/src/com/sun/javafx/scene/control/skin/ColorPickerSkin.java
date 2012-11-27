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

import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javafx.scene.Node;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import com.sun.javafx.scene.control.behavior.ColorPickerBehavior;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.ColorPicker;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.paint.Color;

/**
 *
 * @author paru
 */
public class ColorPickerSkin extends ComboBoxPopupControl<Color> {

    private Label displayNode; 
    private StackPane icon; 
    private Rectangle colorRect; 
    private ColorPalette popupContent;
//    private ColorPickerPanel popup = new ColorPickerPanel(Color.WHITE);
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
        
        @Override
        public Object getBean() {
            return ColorPickerSkin.this;
        }

        @Override
        public String getName() {
            return "colorLabelVisible";
        }
        
        @Override public StyleableProperty getStyleableProperty() {
            return StyleableProperties.COLOR_LABEL_VISIBLE;
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
    }
    
    private void updateComboBoxMode() {
        if (getSkinnable().getStyleClass().contains(ColorPicker.STYLE_CLASS_BUTTON)) {
            setMode(ComboBoxMode.BUTTON);
        }
        else if (getSkinnable().getStyleClass().contains(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)) {
            setMode(ComboBoxMode.SPLITBUTTON);
        }
    }
    
    private static final List<String> colorNames = new ArrayList<String>();
    static {
        // Initializes the namedColors map
        Color.web("white", 1.0);
        for (Field f : Color.class.getDeclaredFields()) {
            int modifier = f.getModifiers();
            if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier) && f.getType().equals(Color.class)) {
                colorNames.add(f.getName());
            }
        }
        Collections.sort(colorNames);
    }
    
    static String colorValueToWeb(Color c) {
        String web = null;
        if (c == null) return null;
        if (colorNames != null) {
            // Find a name for the color. Note that there can
            // be more than one name for a color, e.g. #ff0ff
            // is named both "fuchsia" and "magenta".
            // We return the first name encountered (alphabetically).

            // TODO: Use a hash map for performance
            for (String name : colorNames) {
                if (Color.web(name).equals(c)) {
                    web = name;
                    break;
                }
            }
        }
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
        final ColorPicker colorPicker = (ColorPicker)getSkinnable();
        if (displayNode == null) {
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
            icon = new StackPane();
            icon.getStyleClass().add("picker-color");
            colorRect = new Rectangle(12, 12);
            colorRect.getStyleClass().add("picker-color-rect");
            
            updateColor();
            colorPicker.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    updateColor();
                }
            });
            
            icon.getChildren().add(colorRect);
            displayNode.setGraphic(icon);
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
    *                         Stylesheet Handling                             *
    *                                                                         *
    **************************************************************************/
    
     private static class StyleableProperties {
        private static final StyleableProperty<ColorPicker,Boolean> COLOR_LABEL_VISIBLE = 
                new StyleableProperty<ColorPicker,Boolean>("-fx-color-label-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override public boolean isSettable(ColorPicker n) {
                final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                return skin.colorLabelVisible == null || !skin.colorLabelVisible.isBound();
            }
            
            @Override public WritableValue<Boolean> getWritableValue(ColorPicker n) {
                final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                return skin.colorLabelVisible;
            }
        };
        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(ComboBoxBaseSkin.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                COLOR_LABEL_VISIBLE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
     
     /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return StyleableProperties.STYLEABLES;
     }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

    
}
