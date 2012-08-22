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

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 * PoppedUp Controls are different from other controls in that some, or all, of
 * their visuals are hosted in a Popup. Because the Popup is not itself a member
 * of the scenegraph, we need to manage sending certain messages such as
 * processing of CSS and layout between the "base" or "anchor" scenegraph and
 * the popup scenegraph. This class manages these details.
 * <p>
 * Subclasses of PopupControlSkin may want to provide a Region which represents
 * the Control in the base scenegraph, and must provide a Region which
 * represents this control in the popup. These regions will be mapped in
 * as appropriate by this class.
 * <p>
 * The popup associated with this skin is defined as a protected variable. By
 * default a new Popup instance is created for each PopupControlSkin instance,
 * though subclasses may reuse popups (c.f. TooltipSkin).
 * <p>
 * The popup is shown or hidden based on the {@code showing} variable. It is
 * the responsibility of the subclass to make sure this variable is updated
 * as appropriate based on state on the Control or on other state.
 * <p>
 * When {@code showing} changes to true, the showPopup() function will be
 * called. The implementation of that function relies on the {@code screenX} and
 * {@code screenY} variables for positioning of the popup. Subclasses should
 * ensure that these variables are setup correctly.
 * <p>
 * When {@code showing) changes to false, then the hidePopup() function will
 * be called.
 */
public class PopupControlSkin<C extends PopupControl> implements Skin<C>{

    /**
    * This var keeps a count of visible Popup controls other than the tooltip
    * There should only be at most one visible popup control at any given time
    */
    private static int visiblePopupControlCount=0;

    /**
     * The visual representation for the Control in the "base" scenegraph. This
     * may be null if there is no base representation (such as with a Tooltip).
     */
    protected Node content;

    /**
     * The visual representation for the Control in the popup. This must be
     * specified.
     */
    protected Node popupContent;

    /**
     * Indicates whether the popup should be showing or hidden. It is the
     * responsibility of subclasses to bind this showing variable to the
     * appropriate var on the control
     */
    protected boolean showing = false;

    protected void setShowing(boolean value) {
       boolean oldValue = showing;
       showing = value;
       showHidePopup(oldValue);
    }

    protected void showHidePopup(boolean oldValue) {
        if (showing) {
            visiblePopupControlCount++;
            showPopup();
        } else if (oldValue) {
            visiblePopupControlCount--;
            hidePopup();
        }
    }

    /**
     * The popup menu which will be used. The subclass generally doesn't need
     * to access this variable, except for Tooltip, which does so to make sure
     * that the shared popup is used.
     */
    protected Popup popup;

    /**
     * The screenX position at which to show the popup.
     */
    protected double screenX;

    /**
     * The screenY position at which to show the popup.
     */
    protected double screenY;

    protected void onAutoHide() {
        // do nothing subclasses can override. 
    }
    protected void showPopup() {
        if (popup == null) {
            popup = new Popup();
            popup.setAutoHide(true);
            popup.setAutoFix(true);
            popup.setOnAutoHide(new EventHandler<Event>() {
                @Override public void handle(Event evt) {
                    onAutoHide();
                }
            });
        }

        // Call show before setting the content. In order for popupContent
        // to be styled, the popupContent must be in a Scene and the Scene
        // must be in a Window.
        final Scene scene = content == null ? null : content.getScene();
        final Window win = scene == null ? null : scene.getWindow();
        // TODO: do we need this test?
        if (win == null) return;
        popup.show(content, screenX, screenY);

        // Adding content here triggers Popup.invokeDimensions()
        popup.getContent().setAll(popupContent);

        if (popupContent instanceof Parent) {
            ((Parent) popupContent).requestLayout();
        }
//        getSkinnable().autosize();
        popupContent.autosize();
    }

    protected void hidePopup() {
        if (popup != null) {
            popup.hide();
            // null the popup to reclaim memory
            popup.getContent().clear();
            popup = null;
        }
    }

    /* ************************************************************************
     *                    Skin Implementation                                 *
     **************************************************************************/

    @Override public Node getNode() {
        // subclasses should override
        return null;
    }

    @Override public void dispose() {
       // do nothing subclasses should override
    }

    @Override public C getSkinnable() {
        // subclasses should override
        return null;
    }
}
