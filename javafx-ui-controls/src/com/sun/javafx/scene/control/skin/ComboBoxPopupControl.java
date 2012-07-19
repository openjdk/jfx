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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;
import javafx.scene.input.MouseEvent;

public abstract class ComboBoxPopupControl<T> extends ComboBoxBaseSkin<T> {
    
    private PopupControl popup;

    public ComboBoxPopupControl(ComboBoxBase<T> comboBox, final ComboBoxBaseBehavior behavior) {
        super(comboBox, behavior);
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
        
        Node content = getPopupContent();
        if (content == null) {
            throw new IllegalStateException("Popup node is null");
        }
        
        if (getPopup().isShowing()) return;
        
        positionAndShowPopup();
    }

    @Override public void hide() {
        if (popup != null && popup.isShowing()) {
            popup.hide();
        }
    }
    
    private Point2D getPrefPopupPosition() {
        return com.sun.javafx.Utils.pointRelativeTo(getSkinnable(), getPopupContent(), HPos.CENTER, VPos.BOTTOM, -7, -10, false);
    }
    
    private void positionAndShowPopup() {
        if (getPopup().getSkin() == null) {
            getSkinnable().getScene().getRoot().impl_processCSS(true);
        }
        
        Point2D p = getPrefPopupPosition();
        getPopup().show(getSkinnable().getScene().getWindow(), p.getX(), p.getY());
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
        popup.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent t) {
                // RT-18529: We listen to mouse input that is received by the popup
                // but that is not consumed, and assume that this is due to the mouse
                // clicking outside of the node, but in areas such as the 
                // dropshadow.
                getBehavior().onAutoHide();
            }
        });
        
        // Fix for RT-21207
        InvalidationListener layoutPosListener = new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                reconfigurePopup();
            }
        };
        getSkinnable().layoutXProperty().addListener(layoutPosListener);
        getSkinnable().layoutYProperty().addListener(layoutPosListener);
        getSkinnable().widthProperty().addListener(layoutPosListener);
    }
    
    void reconfigurePopup() {
        if (! getPopup().isShowing()) return;
                
        Point2D p = getPrefPopupPosition();
        reconfigurePopup(p.getX(), p.getY(), 
                getPopupContent().prefWidth(1), getPopupContent().prefHeight(1));
    }   
    
    void reconfigurePopup(double x, double y, double minWidth, double minHeight) {
        if (! getPopup().isShowing()) return;
        
        if (x > -1) getPopup().setX(x);
        if (y > -1) getPopup().setY(y);
        if (minWidth > -1) getPopup().setMinWidth(minWidth);
        if (minHeight > -1) getPopup().setMinHeight(minHeight);
    }
}
