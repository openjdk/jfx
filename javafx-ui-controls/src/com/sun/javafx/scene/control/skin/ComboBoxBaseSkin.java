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

import javafx.scene.control.ComboBoxBase;
import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public abstract class ComboBoxBaseSkin<T> extends BehaviorSkinBase<ComboBoxBase<T>, ComboBoxBaseBehavior<T>> {
    
    private Node displayNode; // this is normally either label or textField
    
    protected StackPane arrowButton;
    protected StackPane arrow;
    
    /** The mode in which this control will be represented. */
    private ComboBoxMode mode = ComboBoxMode.COMBOBOX;
    protected final ComboBoxMode getMode() { return mode; }
    protected final void setMode(ComboBoxMode value) { mode = value; }
    
    public ComboBoxBaseSkin(final ComboBoxBase<T> comboBox, final ComboBoxBaseBehavior behavior) {
        // Call the super method with the ComboBox we were just given in the 
        // constructor, as well as an instance of the behavior class.
        super(comboBox, behavior);
        
        // open button / arrow
        arrow = new StackPane();
        arrow.setFocusTraversable(false);
        arrow.getStyleClass().setAll("arrow");
        arrow.setMaxWidth(Region.USE_PREF_SIZE);
        arrow.setMaxHeight(Region.USE_PREF_SIZE);
        arrowButton = new StackPane();
        arrowButton.setFocusTraversable(false);
        arrowButton.setId("arrow-button");
        arrowButton.getStyleClass().setAll("arrow-button");
        arrowButton.getChildren().add(arrow);
        getChildren().add(arrowButton);
        
        // When ComboBoxBase focus shifts to another node, it should hide.
        getSkinnable().focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    focusLost();
                }
            }
        });
        
        // Register listeners
        registerChangeListener(comboBox.editableProperty(), "EDITABLE");
        registerChangeListener(comboBox.showingProperty(), "SHOWING");
        registerChangeListener(comboBox.focusedProperty(), "FOCUSED");
        registerChangeListener(comboBox.valueProperty(), "VALUE");
    }
    
    protected void focusLost() {
        ((ComboBoxBase)getSkinnable()).hide();
    }
    /**
     * This method should return a Node that will be positioned within the
     * ComboBox 'button' area.
     */
    public abstract Node getDisplayNode();

    /**
     * This method will be called when the ComboBox popup should be displayed.
     * It is up to specific skin implementations to determine how this is handled.
     */
    public abstract void show();
 
    /**
     * This method will be called when the ComboBox popup should be hidden.
     * It is up to specific skin implementations to determine how this is handled.
     */
    public abstract void hide();
    
     /**
     * Handles changes to properties of the MenuButton.
     */
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if ("SHOWING".equals(p)) {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                hide();
            }
        } else if ("EDITABLE".equals(p)) {
            updateDisplayArea();
        } else if ("VALUE".equals(p)) {
            updateDisplayArea();
        } 
    }
    
    private void updateDisplayArea() {
        displayNode = getDisplayNode();
        getChildren().setAll(displayNode, arrowButton);
    }
    
    private boolean isButton() {
        return getMode() == ComboBoxMode.BUTTON;
    }
    
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        if (displayNode == null) {
            updateDisplayArea();
        }
        
        final Insets padding = getSkinnable().getInsets();
        final Insets arrowButtonPadding = arrowButton.getInsets();

        final double arrowWidth = snapSize(arrow.prefWidth(-1));
        final double arrowButtonWidth = (isButton()) ? 0 :
                (snapSpace(arrowButtonPadding.getLeft()) + arrowWidth + 
                snapSpace(arrowButtonPadding.getRight()));
        
        if (displayNode != null) {
            displayNode.resizeRelocate(x, y, w - arrowButtonWidth, h);
        }
        
        if (isButton()) return;
        
        arrowButton.resize(arrowButtonWidth, h);
        positionInArea(arrowButton, 
                getSkinnable().getWidth() - padding.getRight() - arrowButtonWidth, 0, 
                arrowButtonWidth, h, 0, HPos.CENTER, VPos.CENTER);
    }
    
    @Override protected double computePrefWidth(double height) {
        if (displayNode == null) updateDisplayArea();
        final Insets arrowButtonPadding = arrowButton.getInsets();
        final double arrowWidth = snapSize(arrow.prefWidth(-1));
        final double arrowButtonWidth = (isButton()) ? 0 : 
                                        (snapSpace(arrowButtonPadding.getLeft()) + 
                                        arrowWidth + 
                                        snapSpace(arrowButtonPadding.getRight()));
        
        final double totalWidth = (displayNode == null) ? 0 : (displayNode.prefWidth(height) 
                + arrowButtonWidth);
        final Insets padding = getSkinnable().getInsets();
        return padding.getLeft() + totalWidth + padding.getRight();
    }
    
    @Override protected double computePrefHeight(double width) {
        if (displayNode == null) {
            updateDisplayArea();
            
            // RT-20575: The display node is being brought into the scenegraph
            // early so we get the correct prefHeight, but at this point it
            // may not have had a layout pass run over it itself, so the 
            // displayNode will return a prefHeight of 0. Here we are forcing
            // a one-off run of the layout over the displayNode.
            displayNode.impl_processCSS(true);
        }

        double ph;
        if (displayNode == null) {
            final int DEFAULT_HEIGHT = 21;
            final Insets arrowButtonPadding = arrowButton.getInsets();
            double arrowHeight = (isButton()) ? 0 : 
                    (arrowButtonPadding.getTop() + arrow.prefHeight(-1) + arrowButtonPadding.getBottom());
            ph = Math.max(DEFAULT_HEIGHT, arrowHeight);
        } else {
            ph = displayNode.prefHeight(width);
        }

        final Insets padding = getSkinnable().getInsets();
        return padding.getTop()+ ph + padding.getBottom();
    }

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }
}
