/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;

public class ButtonBarSkin extends BehaviorSkinBase<ButtonBar, BehaviorBase<ButtonBar>> {
    
    /**************************************************************************
     * 
     * Static fields
     * 
     **************************************************************************/

    private static final double GAP_SIZE = 10; 
    
    private static final String CATEGORIZED_TYPES = "LRHEYNXBIACO"; //$NON-NLS-1$
    
    // represented as a ButtonData
    public static final String BUTTON_DATA_PROPERTY  = "javafx.scene.control.ButtonBar.ButtonData"; //$NON-NLS-1$
    
    // allows to exclude button from uniform resizing
    public static final String BUTTON_SIZE_INDEPENDENCE = "javafx.scene.control.ButtonBar.independentSize"; //$NON-NLS-1$
    
    // pick an arbitrary number
    private static final double DO_NOT_CHANGE_SIZE = Double.MAX_VALUE - 100;
    
    
    /**************************************************************************
     * 
     * fields
     * 
     **************************************************************************/
    
    private HBox layout;
    
    
    
    /**************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/

    public ButtonBarSkin(final ButtonBar control) {
        super(control, new BehaviorBase<>(control, Collections.<KeyBinding> emptyList()));
        
        this.layout = new HBox(GAP_SIZE) {
            @Override
            protected void layoutChildren() {
                // has to be called first or layout is not correct sometimes
                resizeButtons();
                super.layoutChildren();
            }
        };
        this.layout.setAlignment(Pos.CENTER);
        this.layout.getStyleClass().add("container");
        getChildren().add(layout);
        
        layoutButtons();
        
        control.getButtons().addListener((ListChangeListener<Node>) c -> layoutButtons());
        
        registerChangeListener(control.buttonOrderProperty(), "BUTTON_ORDER"); //$NON-NLS-1$
        registerChangeListener(control.buttonMinWidthProperty(), "BUTTON_MIN_WIDTH"); //$NON-NLS-1$
    }
    
    
    /**************************************************************************
     * 
     * Overriding public API
     * 
     **************************************************************************/
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        
        if ("BUTTON_ORDER".equals(p)) { //$NON-NLS-1$
            layoutButtons();
        } else if ("BUTTON_MIN_WIDTH".equals(p)) { //$NON-NLS-1$
//            layoutButtons();
            resizeButtons();
        }
    }
    
    
    
    /**************************************************************************
     * 
     * Implementation
     * 
     **************************************************************************/
    
    private void layoutButtons() {
        final ButtonBar buttonBar = getSkinnable();
        final List<? extends Node> buttons = buttonBar.getButtons();
        final double buttonMinWidth = buttonBar.getButtonMinWidth();
        
        String buttonOrder = getSkinnable().getButtonOrder();

        layout.getChildren().clear();

        // empty is valid, because it is BUTTON_ORDER_NONE
        if (buttonOrder == null) {
            throw new IllegalStateException("ButtonBar buttonOrder string can not be null"); //$NON-NLS-1$
        }

        if (buttonOrder == ButtonBar.BUTTON_ORDER_NONE) {
            // when using BUTTON_ORDER_NONE, we just lay out the buttons in the
            // order they are specified, but we do right-align the buttons by
            // inserting a dynamic spacer.
            Spacer.DYNAMIC.add(layout, true);
            for (Node btn: buttons) {
                sizeButton(btn, buttonMinWidth, DO_NOT_CHANGE_SIZE, Double.MAX_VALUE);
                layout.getChildren().add(btn);
                HBox.setHgrow(btn, Priority.NEVER);
            }
        } else {
            doButtonOrderLayout(buttonOrder);
        }
    }

    private void doButtonOrderLayout(String buttonOrder) {
        final ButtonBar buttonBar = getSkinnable();
        final List<? extends Node> buttons = buttonBar.getButtons();
        final double buttonMinWidth = buttonBar.getButtonMinWidth();
        Map<String, List<Node>> buttonMap = buildButtonMap(buttons);

        char[] buttonOrderArr = buttonOrder.toCharArray();

        int buttonIndex = 0; // to determine edge cases
        Spacer spacer = Spacer.NONE;

        for (int i = 0; i < buttonOrderArr.length; i++) {
            char type = buttonOrderArr[i];
            boolean edgeCase = buttonIndex <= 0 && buttonIndex >= buttons.size()-1;
            boolean hasChildren = ! layout.getChildren().isEmpty();
            if (type == '+') {
                spacer = spacer.replace(Spacer.DYNAMIC);
            } else if (type == '_' && hasChildren) {
                spacer = spacer.replace(Spacer.FIXED);
            } else {
                List<Node> buttonList = buttonMap.get(String.valueOf(type).toUpperCase());
                if (buttonList != null) {
                    spacer.add(layout,edgeCase);

                    for (Node btn: buttonList) {
                        sizeButton(btn, buttonMinWidth, DO_NOT_CHANGE_SIZE, Double.MAX_VALUE);

                        layout.getChildren().add(btn);
                        HBox.setHgrow(btn, Priority.NEVER);
                        buttonIndex++;
                    }
                    spacer = spacer.replace(Spacer.NONE);
                }
            }
        }
    }
    
    // Button sizing. If buttonUniformSize is true button size = max(buttonMinSize, max(all button pref sizes))
    // otherwise button size = max(buttonBar.buttonMinSize, button pref size)
    private void resizeButtons() {
        final ButtonBar buttonBar = getSkinnable();
        double buttonMinWidth = buttonBar.getButtonMinWidth();
        final List<? extends Node> buttons = buttonBar.getButtons();

        // determine the widest button
        double widest = buttonMinWidth;
        for (Node button : buttons) {
            if (!ButtonBar.isButtonUniformSize(button)) {
               widest = Math.max(button.prefWidth(-1), widest);
            }
        }
        
        // set the width of all buttons
        for (Node button : buttons) {
            if (!ButtonBar.isButtonUniformSize(button)) {
                sizeButton(button, DO_NOT_CHANGE_SIZE, widest, DO_NOT_CHANGE_SIZE);
            }
        }
    }
    
    private void sizeButton(Node btn, double min, double pref, double max) {
        if (btn instanceof Region) {
            Region regionBtn = (Region)btn;
            
            if (min != DO_NOT_CHANGE_SIZE) {
                regionBtn.setMinWidth(min);
            }
            if (pref != DO_NOT_CHANGE_SIZE) {
                regionBtn.setPrefWidth(pref);
            }
            if (max != DO_NOT_CHANGE_SIZE)
            regionBtn.setMaxWidth(max);
        }
    }
    
    private String getButtonType(Node btn) {
        ButtonData buttonType =  (ButtonData) btn.getProperties().get(BUTTON_DATA_PROPERTY);
        
        if (buttonType == null) {
            // just assume it is ButtonType.OTHER
            buttonType = ButtonData.OTHER;
        }
        
        String typeCode = buttonType.getTypeCode();
        typeCode = typeCode.length() > 0? typeCode.substring(0,1): ""; //$NON-NLS-1$
        return CATEGORIZED_TYPES.contains(typeCode.toUpperCase())? typeCode : ButtonData.OTHER.getTypeCode(); 
    }
    
    private Map<String, List<Node>> buildButtonMap( List<? extends Node> buttons ) {
        Map<String, List<Node>> buttonMap = new HashMap<>();
        for (Node btn : buttons) {
            if ( btn == null ) continue;
            String type =  getButtonType(btn); 
            List<Node> typedButtons = buttonMap.get(type);
            if ( typedButtons == null ) {
                typedButtons = new ArrayList<Node>();
                buttonMap.put(type, typedButtons);
            }
            typedButtons.add( btn );
        }
        return buttonMap;
    }
    
    
    
    /**************************************************************************
     * 
     * Support classes / enums
     * 
     **************************************************************************/
    
    private enum Spacer {
        FIXED {
            @Override protected Node create(boolean edgeCase) {
                if ( edgeCase ) return null;
                Region spacer = new Region();
                spacer.setMinWidth(GAP_SIZE);
                HBox.setHgrow(spacer, Priority.NEVER);
                return spacer;
            }
        },
        DYNAMIC {
            @Override protected Node create(boolean edgeCase) {
                Region spacer = new Region();
                spacer.setMinWidth(edgeCase ? 0 : GAP_SIZE);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                return spacer;
            }

            @Override public Spacer replace(Spacer spacer) {
                return FIXED == spacer? this: spacer;
            }
        },
        NONE;
        
        protected Node create(boolean edgeCase) {
            return null;
        }
        
        public Spacer replace(Spacer spacer) {
            return spacer;
        }
        
        public void add(Pane pane, boolean edgeCase) {
            Node spacer = create(edgeCase);
            if (spacer != null) {
                pane.getChildren().add(spacer);
            }
        }
    }
}
