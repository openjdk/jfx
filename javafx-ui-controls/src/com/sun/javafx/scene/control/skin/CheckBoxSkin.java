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
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.control.behavior.ButtonBehavior;
import javafx.geometry.NodeOrientation;

/**
 * Skin for tri state selection Control.
 */
public class CheckBoxSkin extends LabeledSkinBase<CheckBox, ButtonBehavior<CheckBox>> {

    private final StackPane box = new StackPane();
    private StackPane innerbox;

    public CheckBoxSkin(CheckBox checkbox) {
        super(checkbox, new ButtonBehavior<CheckBox>(checkbox));

        box.getStyleClass().setAll("box");
        innerbox = new StackPane();
        innerbox.getStyleClass().setAll("mark");
        innerbox.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        box.getChildren().add(innerbox);
        updateChildren();
    }

    @Override protected void updateChildren() {
        super.updateChildren();
        if (box != null) {
            getChildren().add(box);
        }
    }
    
    @Override protected double computeMinWidth(double height, int topInset, int rightInset, int bottomInset, int leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset) + snapSize(box.minWidth(-1));
    }
    
    @Override protected double computeMinHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        return Math.max(super.computeMinHeight(width - box.minWidth(-1), topInset, rightInset, bottomInset, leftInset),
                topInset + box.minHeight(-1) + bottomInset);
    }

    @Override protected double computePrefWidth(double height, int topInset, int rightInset, int bottomInset, int leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset) + snapSize(box.prefWidth(-1));
    }

    @Override protected double computePrefHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        return Math.max(super.computePrefHeight(width - box.prefWidth(-1), topInset, rightInset, bottomInset, leftInset),
                        topInset + box.prefHeight(-1) + bottomInset);
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {

        final double boxWidth = snapSize(box.prefWidth(-1));
        final double boxHeight = snapSize(box.prefHeight(-1));
        final double labelWidth = Math.min(getSkinnable().prefWidth(-1) - boxWidth, w - snapSize(boxWidth));
        final double labelHeight = Math.min(getSkinnable().prefHeight(labelWidth), h);
        final double maxHeight = Math.max(boxHeight, labelHeight);
        final double xOffset = Utils.computeXOffset(w, labelWidth + boxWidth, getSkinnable().getAlignment().getHpos()) + x;
        final double yOffset = Utils.computeYOffset(h, maxHeight, getSkinnable().getAlignment().getVpos()) + x;

        layoutLabelInArea(xOffset + boxWidth, yOffset, labelWidth, maxHeight, Pos.CENTER_LEFT);
        box.resize(boxWidth, boxHeight);
        positionInArea(box, xOffset, yOffset, boxWidth, maxHeight, 0, HPos.CENTER, VPos.CENTER);
    }
}
