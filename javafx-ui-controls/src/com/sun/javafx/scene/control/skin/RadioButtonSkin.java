/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.control.behavior.ButtonBehavior;

public class RadioButtonSkin extends LabeledSkinBase<RadioButton, ButtonBehavior<RadioButton>> {

    /** The radio contains the "dot", which is usually a circle */
    private StackPane radio;

    /**
     * Used for laying out the label + radio together as a group
     *
     * NOTE: This extra node should be eliminated in the future.
     * Instead, position inner nodes directly with the utility
     * functions in Pane (computeXOffset()/computeYOffset()).
     */
    public RadioButtonSkin(RadioButton radioButton) {
        super(radioButton, new ButtonBehavior<RadioButton>(radioButton));

        radio = createRadio();        
        updateChildren();
    }

    @Override protected void updateChildren() {
        super.updateChildren();
        if (radio != null) {
            getChildren().add(radio);
        }
    }

    private static StackPane createRadio() {
        StackPane radio = new StackPane();
        radio.getStyleClass().setAll("radio");
        radio.setSnapToPixel(false);
        StackPane region = new StackPane();
        region.getStyleClass().setAll("dot");
        radio.getChildren().clear();
        radio.getChildren().addAll(region);
        return radio;
    }


    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/
    
    @Override protected double computeMinWidth(double height, int topInset, int rightInset, int bottomInset, int leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset) + snapSize(radio.minWidth(-1));
    }
    
    @Override protected double computeMinHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        return Math.max(snapSize(super.computeMinHeight(width - radio.minWidth(-1), topInset, rightInset, bottomInset, leftInset)),
                topInset + radio.minHeight(-1) + bottomInset);
    }

    @Override protected double computePrefWidth(double height, int topInset, int rightInset, int bottomInset, int leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset) + snapSize(radio.prefWidth(-1));
    }

    @Override protected double computePrefHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        return Math.max(snapSize(super.computePrefHeight(width - radio.prefWidth(-1), topInset, rightInset, bottomInset, leftInset)),
                        topInset + radio.prefHeight(-1) + bottomInset);
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {

        final double radioWidth = radio.prefWidth(-1);
        final double radioHeight = radio.prefHeight(-1);
        final double labelWidth = Math.min(getSkinnable().prefWidth(-1) - radioWidth, w - snapSize(radioWidth));
        final double labelHeight = Math.min(getSkinnable().prefHeight(labelWidth), h);
        final double maxHeight = Math.max(radioHeight, labelHeight);
        final double xOffset = Utils.computeXOffset(w, labelWidth + radioWidth, getSkinnable().getAlignment().getHpos()) + x;
        final double yOffset = Utils.computeYOffset(h, maxHeight, getSkinnable().getAlignment().getVpos()) + y;

        layoutLabelInArea(xOffset + radioWidth, yOffset, labelWidth, maxHeight, Pos.CENTER_LEFT);
        radio.resize(snapSize(radioWidth), snapSize(radioHeight));
        positionInArea(radio, xOffset, yOffset, radioWidth, maxHeight, 0, HPos.CENTER, VPos.CENTER);
    }
}
