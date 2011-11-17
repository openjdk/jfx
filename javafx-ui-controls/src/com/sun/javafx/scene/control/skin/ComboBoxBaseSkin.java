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

package com.sun.javafx.scene.control.skin;

import com.javafx.preview.control.ComboBox;
import com.javafx.preview.control.ComboBoxBase;
import com.javafx.preview.control.ComboBoxContent;
import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.scene.control.Logging;
import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class ComboBoxBaseSkin<T> extends SkinBase<ComboBoxBase<T>, ComboBoxBaseBehavior<T>> {
    
    private Node displayNode; // this is always either label or textField
    
    private StackPane arrowButton;
    private StackPane arrow;
    
    private ComboBoxContent popup;
    
    public ComboBoxBaseSkin(final ComboBoxBase<T> comboBox) {
        // Call the super method with the button we were just given in the 
        // constructor, as well as an instance of the behavior class.
        super(comboBox, new ComboBoxBaseBehavior(comboBox));
        
        // open button / arrow
        arrow = new StackPane();
        arrow.getStyleClass().setAll("arrow");
        arrow.setMaxWidth(USE_PREF_SIZE);
        arrow.setMaxHeight(USE_PREF_SIZE);
        arrowButton = new StackPane();
        arrowButton.getStyleClass().setAll("arrow-button");
        arrowButton.getChildren().add(arrow);
        getChildren().add(arrowButton);
        
        arrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                getBehavior().arrowPressed(e);
                e.consume();
            }
        });
        arrowButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                getBehavior().arrowReleased(e);
                e.consume();
            }
        });
        
        // Register listeners
        registerChangeListener(comboBox.editableProperty(), "EDITABLE");
        registerChangeListener(comboBox.showingProperty(), "SHOWING");
        registerChangeListener(comboBox.focusedProperty(), "FOCUSED");
        registerChangeListener(comboBox.valueProperty(), "VALUE");
//        registerChangeListener(comboBox.cellFactoryProperty(), "CELL_FACTORY");
//        registerChangeListener(comboBox.itemsProperty(), "ITEMS");
        
//        registerChangeListener(comboBox.selectionModelProperty(), "SELECTION_MODEL");
//        if (comboBox.getSelectionModel() != null) {
//            registerChangeListener(comboBox.getSelectionModel().selectedItemProperty(), "SELECTED_ITEM");
//        }
    }

     /**
     * Handles changes to properties of the MenuButton.
     */
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if (p == "SHOWING") {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                hide();
            }
        } else if (p == "EDITABLE") {
            updateDisplayArea();
        } else if (p == "VALUE") {
            updateDisplayArea();
//        } else if (p == "SELECTION_MODEL") {
//            
        }
    }
    
    private void updateDisplayArea() {
        if (displayNode != null) {
            getChildren().remove(displayNode);
            displayNode = null;
        }
        displayNode = popup.getDisplayNode();
        getChildren().add(0, displayNode);
    }
    
    private void show() {
        popup.showPopup();
    }

    private void hide() {
        popup.hidePopup();
    }
    
    @Override protected void layoutChildren() {
        final Insets padding = getPadding();
        final Insets arrowButtonPadding = arrowButton.getPadding();

        // x, y, w, h are the content area that will hold the label and arrow */
        final double x = padding.getLeft();
        final double y = padding.getTop();
        final double w = getWidth() - (padding.getLeft() + padding.getRight());
        final double h = getHeight() - (padding.getTop() + padding.getBottom());

        final double arrowWidth = arrow.prefWidth(-1);
        final double arrowButtonWidth = arrowButtonPadding.getLeft() + arrowWidth + arrowButtonPadding.getRight();
        
        displayNode.resizeRelocate(x, y, w, h);
        
        arrowButton.resize(arrowButtonWidth, getHeight());
        positionInArea(arrowButton, getWidth() - padding.getRight() - arrowButtonWidth, 0, 
                arrowButtonWidth, getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }
    
    @Override protected double computePrefWidth(double height) {
        if (popup == null) return 100;
        return popup.computePrefWidth(height);
    }

    @Override protected double computePrefHeight(double width) {
        final Insets padding = getPadding();
        final Insets arrowButtonPadding = arrowButton.getPadding();
        double arrowHeight = arrowButtonPadding.getTop() + arrow.prefHeight(-1) + arrowButtonPadding.getBottom();
        
        return padding.getTop()
                + Math.max(displayNode.prefHeight(width), arrowHeight)
                + padding.getBottom();
    }

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }
    
    
    private void loadPopupClass(String className) {
        // we don't reload the skin class if it is the same as the last class name
        if (popupClass != null && popupClass.equals(className)) {
            return;
        }
        popupClass = className;

        if (className == null || className.isEmpty()) {
            Logging.getControlsLogger().severe("Empty -fx-popup-class property specified for ComboBox: " + this);
            return;
        }

        try {
            Class<?> skinClass;
            // RT-17525 : Use context class loader only if Class.forName fails.
            try {
                skinClass = Class.forName(className);
            } catch (ClassNotFoundException clne) {
                if (Thread.currentThread().getContextClassLoader() != null) {
                    skinClass = Thread.currentThread().getContextClassLoader().loadClass(className);
                } else {
                    throw clne;
                }
            }
            
            Constructor<?>[] constructors = skinClass.getConstructors();
            Constructor<?> constructor = null;
            for (Constructor<?> c : constructors) {
                Class<?>[] parameterTypes = c.getParameterTypes();
                if (parameterTypes.length == 1 && ComboBoxBase.class.isAssignableFrom(parameterTypes[0])) {
                    constructor = c;
                    break;
                }
            }

            if (constructor == null) {
                Logging.getControlsLogger().severe(
                "No valid constructor defined in '" + className + "' for control " + this +
                         ".\r\nYou must provide a constructor that accepts a single "
                         + "ComboBoxBase parameter in " + className + ".",
                new NullPointerException());
            } else {
                popup = (ComboBoxContent) constructor.newInstance(getSkinnable());
                updateDisplayArea();
            }
        } catch (InvocationTargetException e) {
            Logging.getControlsLogger().severe(
                "Failed to load ComboBoxContent class '" + className + "' for control " + this,
                e.getCause());
        } catch (Exception e) {
            Logging.getControlsLogger().severe(
                "Failed to load ComboBoxContent class '" + className + "' for control " + this, e);
        }
}
    
    
    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/
    
    @Styleable(property="-fx-popup-class")
    private String popupClass;

    /** @treatasprivate */
    private static class StyleableProperties {
        private static final StyleableProperty POPUP_CLASS = new StyleableProperty(ComboBoxBaseSkin.class, "popupClass");
            
        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(CellSkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                POPUP_CLASS
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
            
            bitIndices = new int[StyleableProperty.getMaxIndex()];
            java.util.Arrays.fill(bitIndices, -1);
            for(int bitIndex=0; bitIndex<STYLEABLES.size(); bitIndex++) {
                bitIndices[STYLEABLES.get(bitIndex).getIndex()] = bitIndex;
            }
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected int[] impl_cssStyleablePropertyBitIndices() {
        return ComboBoxBaseSkin.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return ComboBoxBaseSkin.StyleableProperties.STYLEABLES;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-popup-class".equals(property)) {
            loadPopupClass((String)value);
        }
        return super.impl_cssSet(property, value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-popup-class".equals(property)) {
            return true;
        }

        return super.impl_cssSettable(property);
    }
}
