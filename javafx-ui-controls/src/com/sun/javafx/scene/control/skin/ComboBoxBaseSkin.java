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
import javafx.scene.layout.StackPane;

public abstract class ComboBoxBaseSkin<T> extends SkinBase<ComboBoxBase<T>, ComboBoxBaseBehavior<T>> {
    
    private Node displayNode; // this is normally either label or textField
    
    protected StackPane arrowButton;
    protected StackPane arrow;
    
    public ComboBoxBaseSkin(final ComboBoxBase<T> comboBox, final ComboBoxBaseBehavior behavior) {
        // Call the super method with the button we were just given in the 
        // constructor, as well as an instance of the behavior class.
        super(comboBox, behavior);//new ComboBoxBaseBehavior(comboBox));
        
        // open button / arrow
        arrow = new StackPane();
        arrow.setFocusTraversable(false);
        arrow.getStyleClass().setAll("arrow");
        arrow.setMaxWidth(USE_PREF_SIZE);
        arrow.setMaxHeight(USE_PREF_SIZE);
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
                    ((ComboBoxBase)getSkinnable()).hide();
                }
            }
        });
        
        // Register listeners
        registerChangeListener(comboBox.editableProperty(), "EDITABLE");
        registerChangeListener(comboBox.showingProperty(), "SHOWING");
        registerChangeListener(comboBox.focusedProperty(), "FOCUSED");
        registerChangeListener(comboBox.valueProperty(), "VALUE");
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
        }
    }
    
    private void updateDisplayArea() {
        if (displayNode != null) {
            getChildren().remove(displayNode);
            displayNode = null;
        }
        displayNode = getDisplayNode();
        getChildren().add(0, displayNode);
    }
    
    @Override protected void layoutChildren() {
        if (displayNode == null) {
            updateDisplayArea();
        }
        
        final Insets padding = getInsets();
        final Insets arrowButtonPadding = arrowButton.getInsets();

        // x, y, w, h are the content area that will hold the label and arrow */
        final double x = padding.getLeft();
        final double y = padding.getTop();
        final double w = getSkinnable().getWidth() - (padding.getLeft() + padding.getRight());
        final double h = getSkinnable().getHeight() - (padding.getTop() + padding.getBottom());

        final double arrowWidth = snapSize(arrow.prefWidth(-1));
        final double arrowButtonWidth = snapSpace(arrowButtonPadding.getLeft()) + 
                                        arrowWidth + 
                                        snapSpace(arrowButtonPadding.getRight());
        
        if (displayNode != null) {
            displayNode.resizeRelocate(x, y, w, h);
        }
        
        arrowButton.resize(arrowButtonWidth, getHeight());
        positionInArea(arrowButton, getWidth() - padding.getRight() - arrowButtonWidth, 0, 
                arrowButtonWidth, getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }
    
    @Override protected double computePrefWidth(double height) {
        return displayNode == null ? 100 : displayNode.prefWidth(height);
    }
    
    @Override protected double computePrefHeight(double width) {
        final Insets padding = getInsets();

        if (displayNode == null) {
            final int DEFAULT_HEIGHT = 20;
            final Insets arrowButtonPadding = arrowButton.getInsets();
            double arrowHeight = arrowButtonPadding.getTop() + arrow.prefHeight(-1) + arrowButtonPadding.getBottom();
            return padding.getTop() + Math.max(DEFAULT_HEIGHT, arrowHeight) + padding.getBottom();
        } else {
            return displayNode.prefHeight(width);
        }
    }

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }
}
