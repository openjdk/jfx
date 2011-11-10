/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.skin;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

/**
 * Region/css based skin for Tooltip. It deals mostly with show hide logic for
 * Popup based controls, and specifically in this case for tooltip. It also
 * implements some of the Skin interface methods.
 *
 * TooltipContent class is the actual skin implementation of the tooltip.
 */
public class TooltipSkin implements Skin<Tooltip> { //extends PopupControlSkin<Tooltip> {
    private StackPane root;
    private Label tipLabel;
    private StackPane pageCorner;

    private Tooltip tooltip;

    public TooltipSkin(Tooltip t) {
        this.tooltip = t;
//        setEffect(t.getEffect());
        tipLabel = new Label();
        tipLabel.contentDisplayProperty().bind(t.contentDisplayProperty());
        tipLabel.fontProperty().bind(t.fontProperty());
        tipLabel.graphicProperty().bind(t.graphicProperty());
        tipLabel.textAlignmentProperty().bind(t.textAlignmentProperty());
        tipLabel.textOverrunProperty().bind(t.textOverrunProperty());
        tipLabel.textProperty().bind(t.textProperty());
        tipLabel.wrapTextProperty().bind(t.wrapTextProperty());
        pageCorner = new StackPane();
        pageCorner.getStyleClass().setAll("page-corner");
      
        root = new StackPane() {
            @Override protected void layoutChildren() {
                tipLabel.resizeRelocate(getInsets().getLeft(), getInsets().getTop(),
                        getWidth() - getInsets().getLeft() - getInsets().getRight(),
                        getHeight() - getInsets().getTop() - getInsets().getBottom());
                double pw = pageCorner.prefWidth(-1);
                double ph = pageCorner.prefHeight(-1);
                pageCorner.resizeRelocate((getWidth() - pw), (getHeight() - ph), pw, ph);
            }

            @Override protected double computeMinWidth(double width) {
                return (tooltip.getMinWidth() != -1) ? tooltip.getMinWidth() : computePrefWidth(width);
            }

             @Override protected double computeMinHeight(double height) {
                return (tooltip.getMinHeight() != -1) ? tooltip.getMinHeight() : computePrefHeight(height);
            }

            @Override protected double computePrefWidth(double width) {
                 if(tooltip.getPrefWidth() != -1 ) {
                    return tooltip.getPrefWidth();
                } else {
                     return (tooltip.isWrapText()) ? tipLabel.prefWidth(width) :
                         getInsets().getLeft() + tipLabel.prefWidth(-1) + getInsets().getRight();
                }
            }

            @Override protected double computePrefHeight(double height) {
                 if (tooltip.getPrefWidth() != -1) {
                     return getInsets().getTop() +
                         tipLabel.prefHeight(tooltip.getPrefWidth() - getInsets().getLeft() - getInsets().getRight()) +
                         getInsets().getBottom();
                 } else {
                    return getInsets().getTop() + tipLabel.prefHeight(-1) + getInsets().getBottom();
                 }

            }
            @Override protected double computeMaxWidth(double width) {
                return (tooltip.getMaxWidth() != -1) ? tooltip.getMaxWidth() : computePrefWidth(width);
            }

            @Override protected double computeMaxHeight(double height) {
               return (tooltip.getMaxHeight() != -1) ? tooltip.getMaxHeight() : computePrefHeight(height);
             }

        };

        root.getChildren().addAll(tipLabel, pageCorner);
        // RT-7512 - skin needs to have styleClass of the control
        // TODO - This needs to be bound together, not just set! Probably should
        // do the same for id and style as well.
        root.getStyleClass().setAll(t.getStyleClass());
        root.setStyle(t.getStyle());
        root.setId(t.getId());
    }
    
    
    @Override public Tooltip getSkinnable() {
        return tooltip;
    }

    @Override public Node getNode() {
        return root;
    }

    @Override public void dispose() {
        tooltip = null;
    }
}
