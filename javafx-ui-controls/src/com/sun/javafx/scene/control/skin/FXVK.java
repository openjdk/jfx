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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Popup;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import javafx.beans.DefaultProperty;

public class FXVK extends Control {

    private Node attachedNode;

    public String[] chars;

    public FXVK() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    }


    public void setAttachedNode(Node node) {
        Node oldNode = attachedNode;
        attachedNode = node;

        if (oldNode != null && node != null && oldNode.getScene() == node.getScene()) {
            return;
        }

        if (node != null) {
            setPrefWidth(node.getScene().getWidth());
            setMaxWidth(node.getScene().getWidth());
            setPrefHeight(200);
        }
    }

    public Node getAttachedNode() {
        return attachedNode;
    }




    private static Popup vkPopup;
    private static FXVK vk;

    public static void attach(final TextInputControl textInput) {
        if (vkPopup == null) {
            vk = new FXVK();
            vkPopup = new Popup();
            vkPopup.getContent().add(vk);
        }

        Scene scene = null;
        if (vk.attachedNode != null) {
            scene = vk.attachedNode.getScene();
        }
        vk.setAttachedNode(textInput);

        if (scene != vk.attachedNode.getScene() || !vkPopup.isShowing()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    Point2D nodePoint =
                        com.sun.javafx.Utils.pointRelativeTo(textInput,
                                      vkPopup.getWidth(), vkPopup.getHeight(),
                                      HPos.CENTER, VPos.BOTTOM, 0, 2, true);

                    Point2D point =
                        com.sun.javafx.Utils.pointRelativeTo(textInput.getScene().getRoot(),
                                      vkPopup.getWidth(), vkPopup.getHeight(),
                                      HPos.CENTER, VPos.BOTTOM, 0, 0, true);

//                     vkPopup.show(textInput, point.getX(), point.getY() - vkPopup.getHeight());
                    vkPopup.show(textInput, point.getX(), point.getY() - vk.prefHeight(-1));
                }
            });
        }
    }

    public static void detach() {
        if (vk != null) {
            vk.setAttachedNode(null);
            vkPopup.hide();
        }
    }










    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "fxvk";
}

