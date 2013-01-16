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
import java.util.HashMap;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;

import com.sun.javafx.css.StyleManager;
import javafx.css.StyleableBooleanProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.input.KeyEvent;


public class FXVK extends Control {

    public enum Type {
        TEXT,
        NUMERIC,
        EMAIL,
    }

    private final ObjectProperty<Type> type = new SimpleObjectProperty<Type>(this, "type");
    public final Type getType() { return type.get(); }
    public final void setType(Type value) { type.set(value); }
    public final ObjectProperty<Type> typeProperty() { return type; }


    private final ObjectProperty<EventHandler<KeyEvent>> onAction =
            new SimpleObjectProperty<EventHandler<KeyEvent>>(this, "onAction");
    public final void setOnAction(EventHandler<KeyEvent> value) { onAction.set(value); }
    public final EventHandler<KeyEvent> getOnAction() { return onAction.get(); }
    public final ObjectProperty<EventHandler<KeyEvent>> onActionProperty() { return onAction; }


    final static String[] VK_TYPE_NAMES = new String[] { "text", "numeric", "url", "email" };
    public final static String VK_TYPE_PROP_KEY = "vkType";

    String[] chars;

    public FXVK() {
        this(Type.TEXT);
    }

    public FXVK(Type type) {
        this.type.set(type);
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }

    final ObjectProperty<Node> attachedNodeProperty() {
        if (attachedNode == null) {
            attachedNode = new ObjectPropertyBase<Node>() {
                @Override public Object getBean() {
                    return FXVK.this;
                }

                @Override public String getName() {
                    return "attachedNode";
                }
            };
        }
        return attachedNode;
    }

    private ObjectProperty<Node> attachedNode;
    final void setAttachedNode(Node value) { attachedNodeProperty().setValue(value); }
    final Node getAttachedNode() { return attachedNode == null ? null : attachedNode.getValue(); }
    static FXVK vk;

    public static void attach(final Node textInput) {
        int type = 0;
        Object typeValue = textInput.getProperties().get(VK_TYPE_PROP_KEY);
        String typeStr = "";
        if (typeValue instanceof String) {
            typeStr = ((String)typeValue).toLowerCase();
        }

        if (vk == null) {
            vk = new FXVK(Type.TEXT);
            vk.setSkin(new FXVKSkin(vk));
        }
        vk.setAttachedNode(textInput);
    }

    public static void detach() {
        if (vk != null) {
            vk.setAttachedNode(null);
        }
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new FXVKSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "fxvk";
}

