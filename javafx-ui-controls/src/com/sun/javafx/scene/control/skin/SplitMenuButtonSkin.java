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

import javafx.event.EventHandler;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.scene.control.behavior.SplitMenuButtonBehavior;

/**
 * Skin for SplitMenuButton Control.
 */
public class SplitMenuButtonSkin extends MenuButtonSkinBase<SplitMenuButton, SplitMenuButtonBehavior> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Creates a new SplitMenuButtonSkin for the given SplitMenu.
     * 
     * @param splitMenuButton the SplitMenuButton
     */
    public SplitMenuButtonSkin(final SplitMenuButton splitMenuButton) {
        super(splitMenuButton, new SplitMenuButtonBehavior(splitMenuButton));

        /*
         * The arrow button is the only thing that acts like a MenuButton on
         * this control.
         */
        behaveLikeButton = true;
        // TODO: do we need to consume all mouse events?
        // they only bubble to the skin which consumes them by default
        arrowButton.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    event.consume();
                }
            });
        arrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                getBehavior().mousePressed(e, false);
                e.consume();
            }
        });
        arrowButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                getBehavior().mouseReleased(e, false);
                e.consume();
            }
        });

        label.setLabelFor(splitMenuButton);
    }
}
