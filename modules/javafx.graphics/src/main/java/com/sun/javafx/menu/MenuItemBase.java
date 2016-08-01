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

package com.sun.javafx.menu;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCombination;
import javafx.scene.Node;


public interface MenuItemBase {

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    public void setId(String value);
    public String getId();
    public StringProperty idProperty();

    /**
     * The text to display in the menu item.
     */
    public void setText(String value);
    public String getText();
    public StringProperty textProperty();

    /**
     * An optional graphic for the menu item This will normally be
     * an {@link javafx.scene.image.ImageView} node, but there is no requirement for this to be
     * the case.
     */
    public void setGraphic(Node value);
    public Node getGraphic();
    public ObjectProperty<Node> graphicProperty();

    /**
     * The action, which is invoked whenever the MenuItemBase is fired. This
     * may be due to the user clicking on the button with the mouse, or by
     * a touch event, or by a key press, or if the developer programatically
     * invokes the {@link #fire()} method.
     */
    public void setOnAction(EventHandler<ActionEvent> value);
    public EventHandler<ActionEvent> getOnAction();
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty();


    // --- Disable
    public void setDisable(boolean value);
    public boolean isDisable();
    public BooleanProperty disableProperty();

    // --- Visible
    public void setVisible(boolean value);
    public boolean isVisible();
    public BooleanProperty visibleProperty();


    // --- Accelerator
    public void setAccelerator(KeyCombination value);
    public KeyCombination getAccelerator();
    public ObjectProperty<KeyCombination> acceleratorProperty();

    /**
     * MnemonicParsing property to enable/disable text parsing.
     * If this is set to true, then the MenuItemBase text will be
     * parsed to see if it contains the mnemonic parsing character '_'.
     * When a mnemonic is detected the key combination will
     * be determined based on the succeeding character, and the mnemonic
     * added.
     *
     * <p>
     * The default value for MenuItemBase is true.
     * </p>
     */
    public void setMnemonicParsing(boolean value);
    public boolean isMnemonicParsing();
    public BooleanProperty mnemonicParsingProperty();


    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Fires a new ActionEvent.
     */
    public void fire();

    /**
     * Fires when the accelerator for this MenuItem is invoked.
     */
    public void fireValidation();

}
