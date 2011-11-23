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

import com.javafx.preview.control.ComboBoxBase;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;

public abstract class ComboBoxPopupControl<T> extends ComboBoxBaseSkin<T> {
    
    private PopupControl popup;

    public ComboBoxPopupControl(ComboBoxBase<T> comboBox) {
        super(comboBox);
    }
    
    /**
     * This method should return the Node that will be displayed when the user
     * clicks on the ComboBox 'button' area.
     */
    protected abstract Node getPopupContent();
    
    protected PopupControl getPopup() {
        if (popup == null) {
            createPopup();
        }
        return popup;
    }

    @Override public void show() {
        if (getSkinnable() == null) {
            throw new IllegalStateException("ComboBox is null");
        }
        if (getPopupContent() == null) {
            throw new IllegalStateException("Popup node is null");
        }
        
        if (! getPopup().isShowing()) {
            if (getPopup().getSkin() == null) {
                getScene().getRoot().impl_processCSS(true);
            }

            Point2D p = com.sun.javafx.Utils.pointRelativeTo(getSkinnable(), getPopupContent(), HPos.CENTER, VPos.BOTTOM, -7, -10, false);
            getPopup().show(getSkinnable().getScene().getWindow(), p.getX(), p.getY());
        }
    }

    @Override public void hide() {
        if (popup != null && popup.isShowing()) {
            popup.hide();
        }
    }
    
    private void createPopup() {
        popup = new PopupControl() {
            {
                setSkin(new Skin() {
                    @Override public Skinnable getSkinnable() { return ComboBoxPopupControl.this.getSkinnable(); }
                    @Override public Node getNode() { return getPopupContent(); }
                    @Override public void dispose() { }
                });
            }
        };
        popup.getStyleClass().add("combo-box-popup");
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.setOnAutoHide(new EventHandler<Event>() {
            @Override public void handle(Event e) {
                getBehavior().onAutoHide();
            }
        });
    }
}
